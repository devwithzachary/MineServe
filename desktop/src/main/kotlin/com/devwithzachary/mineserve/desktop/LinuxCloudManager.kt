package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.core.CoreLog
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class LinuxCloudTunnelAllocation(
    val tunnelId: String,
    val hostname: String,
    val relayHost: String,
    val relayPort: Int,
    val region: String,
    val clientToken: String
)

class LinuxCloudManager(
    private val paths: LinuxDataPaths,
    private val apiBaseUrl: String = "https://api.mineserve.app"
) {
    companion object {
        private const val TAG = "LinuxCloudManager"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val accountFile: File get() = File(paths.rootDir, "cloud_account.json")
    private val lock = Any()

    @Volatile private var authToken: String? = null
    @Volatile private var cachedProfile: JSONObject? = null

    init {
        loadFromDisk()
    }

    val isLinked: Boolean get() = !authToken.isNullOrBlank()

    private fun loadFromDisk() = synchronized(lock) {
        try {
            if (accountFile.exists()) {
                val obj = JSONObject(accountFile.readText())
                authToken = obj.optString("token", "").takeIf { it.isNotBlank() }
                cachedProfile = obj.optJSONObject("profile")
            }
        } catch (e: Exception) {
            CoreLog.w(TAG, "Failed loading cloud_account.json: ${e.message}")
        }
    }

    private fun saveToDisk(token: String?, profile: JSONObject?) = synchronized(lock) {
        authToken = token
        cachedProfile = profile
        try {
            if (token.isNullOrBlank()) {
                if (accountFile.exists()) accountFile.delete()
            } else {
                val obj = JSONObject().put("token", token)
                if (profile != null) obj.put("profile", profile)
                accountFile.writeText(obj.toString(2))
            }
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed saving cloud_account.json", e)
        }
    }

    fun signOut() {
        saveToDisk(null, null)
    }

    fun toStatusJson(): JSONObject {
        val p = cachedProfile
        if (!isLinked || p == null) {
            return JSONObject()
                .put("isLinked", false)
                .put("connected", false)
                .put("tier", "FREE")
        }
        return JSONObject()
            .put("isLinked", true)
            .put("connected", true)
            .put("email", p.optString("email", ""))
            .put("tier", p.optString("tier", "FREE"))
            .put("storageUsedBytes", p.optLong("storageUsedBytes", 0L))
            .put("storageQuotaBytes", p.optLong("storageQuotaBytes", 0L))
            .put("activeSubdomains", p.optJSONArray("activeSubdomains") ?: JSONArray())
    }

    suspend fun requestEmailOtp(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clean = email.trim().lowercase()
        if (clean.isBlank() || !clean.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        try {
            val payload = JSONObject().put("email", clean).toString()
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/auth/otp/request")
                .post(payload.toRequestBody(JSON_MEDIA))
                .build()
            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = parseError(resp.body?.string().orEmpty(), resp.code)
                    return@withContext Result.failure(IllegalStateException(err))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            CoreLog.e(TAG, "OTP request failed", e)
            Result.failure(IllegalStateException("Unable to reach MineServe Cloud: ${e.message}"))
        }
    }

    suspend fun verifyEmailOtp(email: String, code: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanCode = code.filter { it.isDigit() }
        if (cleanCode.length != 6) {
            return@withContext Result.failure(IllegalArgumentException("Enter the 6-digit verification code from your email."))
        }
        try {
            val payload = JSONObject().put("email", cleanEmail).put("code", cleanCode).toString()
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/auth/otp/verify")
                .post(payload.toRequestBody(JSON_MEDIA))
                .build()
            val verifyJson = MineServeHttpClient.client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseError(body, resp.code)))
                }
                JSONObject(body)
            }

            var finalToken = verifyJson.optString("token", "")
            var finalUser = verifyJson.optJSONObject("user")

            // Exchange for dedicated per-device token
            runCatching {
                val linkBody = JSONObject().put("deviceName", LinuxHostTelemetry.getHostDisplayName().take(60)).toString()
                val linkReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/devices/link")
                    .header("Authorization", "Bearer $finalToken")
                    .post(linkBody.toRequestBody(JSON_MEDIA))
                    .build()
                MineServeHttpClient.client.newCall(linkReq).execute().use { linkResp ->
                    if (linkResp.isSuccessful) {
                        val linked = JSONObject(linkResp.body?.string().orEmpty())
                        linked.optString("token", "").takeIf { it.isNotBlank() }?.let { finalToken = it }
                        linked.optJSONObject("user")?.let { finalUser = it }
                    }
                }
            }

            saveToDisk(finalToken, finalUser)
            refreshProfile()
            Result.success(toStatusJson())
        } catch (e: Exception) {
            CoreLog.e(TAG, "OTP verify failed", e)
            Result.failure(IllegalStateException("Unable to verify code: ${e.message}"))
        }
    }

    suspend fun refreshProfile(): Boolean = withContext(Dispatchers.IO) {
        val token = authToken ?: return@withContext false
        try {
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext false
                }
                if (!resp.isSuccessful) return@withContext false
                val parsed = JSONObject(body)
                val userObj = parsed.optJSONObject("user") ?: parsed
                saveToDisk(token, userObj)
                true
            }
        } catch (e: Exception) {
            CoreLog.w(TAG, "Cloud profile refresh failed: ${e.message}")
            false
        }
    }

    suspend fun allocateCloudTunnel(
        serverId: String,
        subdomain: String,
        region: String = "us-east",
        protocol: String = "tcp",
        domain: String = "play.mineserve.app"
    ): Result<LinuxCloudTunnelAllocation> = withContext(Dispatchers.IO) {
        val token = authToken
            ?: return@withContext Result.failure(IllegalStateException("Sign in to MineServe Cloud in Host Tools first."))
        val cleanSub = subdomain.trim().lowercase()
        if (cleanSub.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Enter a subdomain (e.g. my-smp) in Settings & Network."))
        }
        try {
            val payload = JSONObject()
                .put("serverId", serverId)
                .put("subdomain", cleanSub)
                .put("region", region.ifBlank { "us-east" })
                .put("protocol", protocol)
                .put("domain", domain.ifBlank { "play.mineserve.app" })
                .toString()
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/tunnels/allocate")
                .header("Authorization", "Bearer $token")
                .post(payload.toRequestBody(JSON_MEDIA))
                .build()
            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired. Please sign in again."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseError(body, resp.code)))
                }
                val j = JSONObject(body)
                refreshProfile()
                Result.success(
                    LinuxCloudTunnelAllocation(
                        tunnelId = j.optString("tunnelId", ""),
                        hostname = j.optString("hostname", "$cleanSub.$domain"),
                        relayHost = j.optString("relayHost", ""),
                        relayPort = j.optInt("relayPort", 0),
                        region = j.optString("region", region),
                        clientToken = j.optString("clientToken", "")
                    )
                )
            }
        } catch (e: Exception) {
            CoreLog.e(TAG, "Cloud tunnel allocation failed", e)
            Result.failure(e)
        }
    }

    private fun parseError(body: String, code: Int): String {
        return runCatching {
            val j = JSONObject(body)
            j.optString("error", "").ifBlank { j.optString("message", "") }
        }.getOrDefault("").ifBlank { "MineServe Cloud returned HTTP $code" }
    }

    suspend fun listCloudBackups(serverId: String): Result<JSONArray> = withContext(Dispatchers.IO) {
        val token = authToken
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.code == 401) {
                    signOut()
                    return@withContext Result.failure(IllegalStateException("MineServe Cloud session expired."))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException(parseError(body, resp.code)))
                }
                val rawArr = JSONObject(body).optJSONArray("backups") ?: JSONArray()
                val filtered = JSONArray()
                for (i in 0 until rawArr.length()) {
                    val item = rawArr.optJSONObject(i) ?: continue
                    val st = item.optString("status", "")
                    val sid = item.optString("serverId", "")
                    if (st.equals("READY", ignoreCase = true) && (serverId.isBlank() || sid == serverId)) {
                        filtered.put(item)
                    }
                }
                Result.success(filtered)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadBackupToCloud(serverId: String, file: File): Result<String> = withContext(Dispatchers.IO) {
        val token = authToken
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            val initPayload = JSONObject()
                .put("serverId", serverId)
                .put("fileName", file.name)
                .put("sizeBytes", file.length())
                .toString()
            val initReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/upload-url")
                .header("Authorization", "Bearer $token")
                .post(initPayload.toRequestBody(JSON_MEDIA))
                .build()
            val uploadMeta = MineServeHttpClient.client.newCall(initReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(IllegalStateException(parseError(body, resp.code)))
                JSONObject(body)
            }
            val backupId = uploadMeta.getString("backupId")
            val uploadUrl = uploadMeta.getString("uploadUrl")

            val putReq = Request.Builder()
                .url(uploadUrl)
                .put(file.asRequestBody("application/zip".toMediaType()))
                .build()
            MineServeHttpClient.client.newCall(putReq).execute().use { putResp ->
                if (!putResp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Cloud upload failed (HTTP ${putResp.code})"))
                }
            }

            val confirmReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/$backupId/confirm")
                .header("Authorization", "Bearer $token")
                .post("{}".toRequestBody(JSON_MEDIA))
                .build()
            MineServeHttpClient.client.newCall(confirmReq).execute().use { confirmResp ->
                val body = confirmResp.body?.string().orEmpty()
                if (!confirmResp.isSuccessful) return@withContext Result.failure(IllegalStateException(parseError(body, confirmResp.code)))
            }
            refreshProfile()
            Result.success("Uploaded ${file.name} to MineServe Cloud")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadCloudBackup(backupId: String, destFile: File): Result<File> = withContext(Dispatchers.IO) {
        val token = authToken
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            val urlReq = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/$backupId/download-url")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            val dlUrl = MineServeHttpClient.client.newCall(urlReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(IllegalStateException(parseError(body, resp.code)))
                JSONObject(body).getString("downloadUrl")
            }
            val dlReq = Request.Builder().url(dlUrl).get().build()
            MineServeHttpClient.client.newCall(dlReq).execute().use { dlResp ->
                if (!dlResp.isSuccessful) return@withContext Result.failure(IllegalStateException("Cloud download failed (HTTP ${dlResp.code})"))
                val body = dlResp.body ?: return@withContext Result.failure(IllegalStateException("Empty cloud backup body"))
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { out -> body.byteStream().copyTo(out) }
            }
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = authToken
            ?: return@withContext Result.failure(IllegalStateException("Not signed in to MineServe Cloud."))
        try {
            val req = MineServeHttpClient.newRequestBuilder("$apiBaseUrl/v1/backups/$backupId")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@withContext Result.failure(IllegalStateException(parseError(body, resp.code)))
            }
            refreshProfile()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
