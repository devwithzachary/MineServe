package com.devwithzachary.mineserve.cloud

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.api.MineServeHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class CloudSubdomainInfo(
    val subdomain: String = "",
    val region: String = "us-east",
    val domain: String = "play.mineserve.app"
)

@Serializable
data class CloudAccountProfile(
    val userId: String = "",
    val email: String = "",
    val tier: String = "FREE",
    val stripeCustomerId: String? = null,
    val storageUsedBytes: Long = 0L,
    val activeSubdomains: List<String> = emptyList(),
    val activeSubdomainDetails: List<CloudSubdomainInfo> = emptyList()
) {
    val storageQuotaBytes: Long
        get() = when (tier.uppercase()) {
            "PLUS" -> 10L * 1024L * 1024L * 1024L
            "PRO" -> 50L * 1024L * 1024L * 1024L
            else -> 0L
        }

    val maxSubdomains: Int
        get() = when (tier.uppercase()) {
            "PLUS" -> 1
            "PRO" -> 3
            else -> 0
        }
}

@Serializable
data class CloudBackupItem(
    val backupId: String = "",
    val userId: String = "",
    val serverId: String = "",
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val r2Key: String = "",
    val status: String = "PENDING",
    val createdAtEpochMs: Long = 0L
)

@Serializable
private data class UploadUrlRequest(
    val serverId: String,
    val fileName: String,
    val sizeBytes: Long
)

@Serializable
private data class UploadUrlResponse(
    val backupId: String,
    val uploadUrl: String,
    val expiresInSeconds: Long = 900
)

@Serializable
private data class ListBackupsResponse(
    val backups: List<CloudBackupItem> = emptyList()
)

@Serializable
private data class DownloadUrlResponse(
    val backupId: String,
    val downloadUrl: String,
    val expiresInSeconds: Long = 900
)

@Serializable
private data class CloudErrorResponse(
    val error: String? = null
)

@Serializable
private data class DeviceLinkRequest(
    val deviceName: String,
    val platform: String = "android"
)

@Serializable
private data class DeviceLinkResponse(
    val token: String,
    val user: CloudAccountProfile
)

@Serializable
private data class OtpRequestPayload(
    val email: String
)

@Serializable
private data class OtpVerifyPayload(
    val email: String,
    val code: String
)

@Serializable
data class CloudTunnelAllocation(
    val tunnelId: String = "",
    val hostname: String = "",
    val panelHostname: String = "",
    val relayHost: String = "",
    val relayPort: Int = 0,
    val region: String = "us-east",
    val protocol: String = "tcp",
    val clientToken: String = ""
)

@Serializable
private data class TunnelAllocateRequest(
    val serverId: String,
    val subdomain: String,
    val region: String = "us-east",
    val protocol: String = "tcp",
    val domain: String = "play.mineserve.app"
)

data class CloudAccountState(
    val isLinked: Boolean = false,
    val isLoading: Boolean = false,
    val profile: CloudAccountProfile? = null,
    val errorMessage: String? = null
)

class MineServeCloudManager(context: Context) {

    companion object {
        private const val TAG = "MineServeCloudManager"
        private const val PREFS_NAME = "mineserve_cloud_prefs"
        private const val KEY_AUTH_TOKEN = "cloud_auth_token"
        private const val KEY_CACHED_PROFILE = "cloud_cached_profile"

        private val useStagingCloud: Boolean
            get() = BuildConfig.DEBUG && !BuildConfig.FORCE_PROD_CLOUD

        val apiBaseUrl: String
            get() = if (useStagingCloud) "https://api-staging.mineserve.app" else "https://api.mineserve.app"

        val portalBaseUrl: String
            get() = if (useStagingCloud) "https://cloud-staging.mineserve.app" else "https://cloud.mineserve.app"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val zipMediaType = "application/zip".toMediaType()
    private val longTransferClient by lazy {
        MineServeHttpClient.client.newBuilder()
            .writeTimeout(10, TimeUnit.MINUTES)
            .readTimeout(10, TimeUnit.MINUTES)
            .build()
    }

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<CloudAccountState> = _state.asStateFlow()

    private fun loadInitialState(): CloudAccountState {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        val cachedJson = prefs.getString(KEY_CACHED_PROFILE, null)
        val cachedProfile = if (!cachedJson.isNullOrBlank()) {
            runCatching { json.decodeFromString<CloudAccountProfile>(cachedJson) }.getOrNull()
        } else {
            null
        }
        return CloudAccountState(
            isLinked = !token.isNullOrBlank(),
            profile = cachedProfile
        )
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)?.takeIf { it.isNotBlank() }

    suspend fun handleAuthDeepLink(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (uri.scheme != "mineserve" || uri.host != "auth") return@withContext false
        val token = uri.getQueryParameter("token")?.trim()
        if (token.isNullOrBlank()) return@withContext false

        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        _state.value = _state.value.copy(isLinked = true, isLoading = true, errorMessage = null)

        val linked = runCatching {
            val reqJson = json.encodeToString(DeviceLinkRequest(deviceName = formatDeviceName()))
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/devices/link")
                .header("Authorization", "Bearer $token")
                .post(reqJson.toRequestBody(jsonMediaType))
                .build()
            MineServeHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val res = json.decodeFromString<DeviceLinkResponse>(body)
                    prefs.edit()
                        .putString(KEY_AUTH_TOKEN, res.token)
                        .putString(KEY_CACHED_PROFILE, json.encodeToString(res.user))
                        .apply()
                    _state.value = CloudAccountState(
                        isLinked = true,
                        isLoading = false,
                        profile = res.user,
                        errorMessage = null
                    )
                    true
                } else {
                    false
                }
            }
        }.getOrDefault(false)

        if (linked) true else refreshProfile()
    }

    suspend fun requestEmailOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val normalized = email.trim().lowercase()
        if (normalized.isBlank() || !normalized.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        try {
            val reqJson = json.encodeToString(OtpRequestPayload(email = normalized))
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/auth/otp/request")
                .post(reqJson.toRequestBody(jsonMediaType))
                .build()
            MineServeHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = parseErrorMessage(resp.body?.string().orEmpty(), resp.code)
                    return@withContext Result.failure(IllegalStateException(err))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request MineServe Cloud OTP", e)
            Result.failure(IllegalStateException("Unable to reach MineServe Cloud. Please check your connection."))
        }
    }

    suspend fun verifyEmailOtp(email: String, code: String): Result<CloudAccountProfile> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val normalizedCode = code.filter { it.isDigit() }
        if (normalizedCode.length != 6) {
            return@withContext Result.failure(IllegalArgumentException("Enter the 6-digit verification code from your email."))
        }
        try {
            val reqJson = json.encodeToString(OtpVerifyPayload(email = normalizedEmail, code = normalizedCode))
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/auth/otp/verify")
                .post(reqJson.toRequestBody(jsonMediaType))
                .build()
            val authRes = MineServeHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val err = parseErrorMessage(body, resp.code)
                    return@withContext Result.failure(IllegalStateException(err))
                }
                json.decodeFromString<DeviceLinkResponse>(body)
            }

            // Exchange temporary session JWT for a dedicated per-device token
            val finalRes = runCatching {
                val linkReqJson = json.encodeToString(DeviceLinkRequest(deviceName = formatDeviceName()))
                val linkReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/devices/link")
                    .header("Authorization", "Bearer ${authRes.token}")
                    .post(linkReqJson.toRequestBody(jsonMediaType))
                    .build()
                MineServeHttpClient.newCall(linkReq).execute().use { linkResp ->
                    if (linkResp.isSuccessful) {
                        json.decodeFromString<DeviceLinkResponse>(linkResp.body?.string().orEmpty())
                    } else {
                        authRes
                    }
                }
            }.getOrDefault(authRes)

            prefs.edit()
                .putString(KEY_AUTH_TOKEN, finalRes.token)
                .putString(KEY_CACHED_PROFILE, json.encodeToString(finalRes.user))
                .apply()
            _state.value = CloudAccountState(
                isLinked = true,
                isLoading = false,
                profile = finalRes.user,
                errorMessage = null
            )
            Result.success(finalRes.user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify MineServe Cloud OTP", e)
            Result.failure(IllegalStateException("Unable to verify code. Please check your connection and try again."))
        }
    }

    private fun formatDeviceName(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val raw = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }.trim()
        return raw.replaceFirstChar { it.uppercase() }.ifBlank { "Android Device" }
    }

    suspend fun refreshProfile(): Boolean = withContext(Dispatchers.IO) {
        val token = getAuthToken()
        if (token == null) {
            _state.value = CloudAccountState(isLinked = false)
            return@withContext false
        }

        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        try {
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            MineServeHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    _state.value = CloudAccountState(
                        isLinked = false,
                        errorMessage = "MineServe Cloud session expired. Please sign in again."
                    )
                    return@withContext false
                }
                if (!resp.isSuccessful) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Cloud sync failed (HTTP ${resp.code})"
                    )
                    return@withContext false
                }
                val profile = json.decodeFromString<CloudAccountProfile>(body)
                prefs.edit().putString(KEY_CACHED_PROFILE, json.encodeToString(profile)).apply()
                _state.value = CloudAccountState(
                    isLinked = true,
                    isLoading = false,
                    profile = profile,
                    errorMessage = null
                )
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh MineServe Cloud profile", e)
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = "Unable to reach MineServe Cloud."
            )
            false
        }
    }

    fun signOut() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_CACHED_PROFILE)
            .apply()
        _state.value = CloudAccountState(isLinked = false)
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        val parsed = runCatching { json.decodeFromString<CloudErrorResponse>(body).error }.getOrNull()
        return parsed?.takeIf { it.isNotBlank() } ?: "MineServe Cloud request failed (HTTP $code)"
    }

    suspend fun uploadBackupToCloud(
        serverId: String,
        file: File,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = getAuthToken()
            ?: return@withContext Result.failure(IllegalStateException("Sign in to MineServe Cloud in Settings first."))
        if (!file.exists()) {
            return@withContext Result.failure(IllegalArgumentException("Backup file not found: ${file.name}"))
        }

        try {
            onProgress(5)
            val totalBytes = file.length()
            val reqJson = json.encodeToString(
                UploadUrlRequest(
                    serverId = serverId,
                    fileName = file.name,
                    sizeBytes = totalBytes
                )
            )
            val urlReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/upload-url")
                .header("Authorization", "Bearer $token")
                .post(reqJson.toRequestBody(jsonMediaType))
                .build()

            val uploadMeta = MineServeHttpClient.newCall(urlReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired. Please sign in again."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseErrorMessage(body, resp.code)))
                }
                json.decodeFromString<UploadUrlResponse>(body)
            }

            onProgress(10)
            val streamingBody = object : RequestBody() {
                override fun contentType() = zipMediaType
                override fun contentLength() = totalBytes
                override fun writeTo(sink: BufferedSink) {
                    file.source().use { source ->
                        val buffer = okio.Buffer()
                        var uploaded = 0L
                        var lastPct = 10
                        while (true) {
                            val read = source.read(buffer, 65536L)
                            if (read == -1L) break
                            sink.write(buffer, read)
                            uploaded += read
                            if (totalBytes > 0) {
                                val pct = (10 + ((uploaded * 80) / totalBytes)).toInt().coerceIn(10, 90)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
            }

            val putReq = Request.Builder()
                .url(uploadMeta.uploadUrl)
                .put(streamingBody)
                .build()

            longTransferClient.newCall(putReq).execute().use { putResp ->
                if (!putResp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Cloud upload failed (HTTP ${putResp.code})")
                    )
                }
            }

            onProgress(95)
            val confirmReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/${uploadMeta.backupId}/confirm")
                .header("Authorization", "Bearer $token")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            MineServeHttpClient.newCall(confirmReq).execute().use { confirmResp ->
                val body = confirmResp.body?.string().orEmpty()
                if (!confirmResp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseErrorMessage(body, confirmResp.code)))
                }
            }

            onProgress(100)
            refreshProfile()
            Result.success("Uploaded ${file.name} to MineServe Cloud")
        } catch (e: Exception) {
            Log.e(TAG, "MineServe Cloud backup upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun listCloudBackups(serverId: String? = null): Result<List<CloudBackupItem>> = withContext(Dispatchers.IO) {
        val token = getAuthToken()
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            MineServeHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired. Please sign in again."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseErrorMessage(body, resp.code)))
                }
                val items = json.decodeFromString<ListBackupsResponse>(body).backups
                    .filter { it.status.equals("READY", ignoreCase = true) }
                    .let { list ->
                        if (serverId.isNullOrBlank()) list else list.filter { it.serverId == serverId }
                    }
                    .sortedByDescending { it.createdAtEpochMs }
                Result.success(items)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list MineServe Cloud backups", e)
            Result.failure(e)
        }
    }

    suspend fun downloadCloudBackup(
        backupId: String,
        destFile: File,
        onProgress: (Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val token = getAuthToken()
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            onProgress(5)
            val urlReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/$backupId/download-url")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            val downloadMeta = MineServeHttpClient.newCall(urlReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired. Please sign in again."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseErrorMessage(body, resp.code)))
                }
                json.decodeFromString<DownloadUrlResponse>(body)
            }

            val dlReq = Request.Builder().url(downloadMeta.downloadUrl).get().build()
            longTransferClient.newCall(dlReq).execute().use { dlResp ->
                if (!dlResp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Cloud download failed (HTTP ${dlResp.code})")
                    )
                }
                val body = dlResp.body
                    ?: return@withContext Result.failure(IllegalStateException("Empty response body from cloud storage"))
                val totalBytes = body.contentLength()
                destFile.parentFile?.mkdirs()
                val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        val buffer = ByteArray(65536)
                        var downloaded = 0L
                        var lastPct = 5
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (totalBytes > 0) {
                                val pct = (5 + ((downloaded * 95) / totalBytes)).toInt().coerceIn(5, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
                if (destFile.exists()) destFile.delete()
                tmpFile.renameTo(destFile)
            }
            onProgress(100)
            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download MineServe Cloud backup", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = getAuthToken()
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/$backupId")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            MineServeHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired. Please sign in again."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseErrorMessage(body, resp.code)))
                }
                refreshProfile()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete MineServe Cloud backup", e)
            Result.failure(e)
        }
    }

    suspend fun allocateCloudTunnel(
        serverId: String,
        subdomain: String,
        region: String = "us-east",
        protocol: String = "tcp",
        domain: String = "play.mineserve.app"
    ): Result<CloudTunnelAllocation> = withContext(Dispatchers.IO) {
        val token = getAuthToken()
            ?: return@withContext Result.failure(IllegalStateException("Sign in to MineServe Cloud in Settings first."))
        val normalized = subdomain.trim().lowercase()
        if (normalized.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Enter a subdomain (e.g. my-smp)."))
        }
        try {
            val reqJson = json.encodeToString(
                TunnelAllocateRequest(
                    serverId = serverId,
                    subdomain = normalized,
                    region = region,
                    protocol = protocol,
                    domain = domain
                )
            )
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/tunnels/allocate")
                .header("Authorization", "Bearer $token")
                .post(reqJson.toRequestBody(jsonMediaType))
                .build()
            MineServeHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired. Please sign in again."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseErrorMessage(body, resp.code)))
                }
                val allocation = json.decodeFromString<CloudTunnelAllocation>(body)
                refreshProfile()
                Result.success(allocation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to allocate MineServe Cloud tunnel", e)
            Result.failure(e)
        }
    }

    suspend fun releaseCloudTunnel(tunnelIdOrSubdomain: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = getAuthToken()
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/tunnels/$tunnelIdOrSubdomain")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            MineServeHttpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired. Please sign in again."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseErrorMessage(body, resp.code)))
                }
                refreshProfile()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release MineServe Cloud tunnel", e)
            Result.failure(e)
        }
    }
}

