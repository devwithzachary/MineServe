package com.devwithzachary.mineserve.backup.cloud

import android.util.Log
import com.devwithzachary.mineserve.api.MineServeHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class WebDavClient {
    companion object {
        private const val TAG = "WebDavClient"
    }

    suspend fun testConnection(config: CloudBackupConfig): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl(config.webDavUrl)
            if (url.isBlank()) return@withContext Result.failure(IllegalArgumentException("WebDAV server URL is empty."))

            val credential = Credentials.basic(config.webDavUsername, config.webDavPassword)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .method("PROPFIND", RequestBody.create("application/xml".toMediaType(), "<d:propfind xmlns:d=\"DAV:\"><d:prop><d:resourcetype/></d:prop></d:propfind>"))
                .addHeader("Depth", "0")
                .build()

            MineServeHttpClient.client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 207) {
                    Result.success("Connected successfully to Nextcloud / WebDAV server.")
                } else if (response.code == 401 || response.code == 403) {
                    Result.failure(IOException("Authentication failed (HTTP ${response.code}). Check your username or app password."))
                } else {
                    Result.failure(IOException("Server returned unexpected response (HTTP ${response.code})."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WebDAV test connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadBackup(
        backupFile: File,
        config: CloudBackupConfig,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = normalizeUrl(config.webDavUrl)
            val credential = Credentials.basic(config.webDavUsername, config.webDavPassword)

            // Ensure destination directory exists (MKCOL)
            val remoteFolder = config.webDavRemotePath.trim().removePrefix("/").removeSuffix("/")
            var accumulatedPath = baseUrl
            if (remoteFolder.isNotBlank()) {
                val segments = remoteFolder.split("/")
                for (segment in segments) {
                    if (segment.isBlank()) continue
                    accumulatedPath = if (accumulatedPath.endsWith("/")) "$accumulatedPath$segment/" else "$accumulatedPath/$segment/"
                    ensureDirectoryExists(accumulatedPath, credential)
                }
            } else {
                if (!accumulatedPath.endsWith("/")) accumulatedPath += "/"
            }

            val targetFileUrl = if (accumulatedPath.endsWith("/")) "$accumulatedPath${backupFile.name}" else "$accumulatedPath/${backupFile.name}"
            Log.i(TAG, "Uploading backup ${backupFile.name} to $targetFileUrl")

            val totalBytes = backupFile.length()
            val requestBody = object : RequestBody() {
                override fun contentType() = "application/zip".toMediaType()
                override fun contentLength() = totalBytes

                override fun writeTo(sink: BufferedSink) {
                    FileInputStream(backupFile).use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var uploadedBytes = 0L
                        var lastReportedPct = -1

                        while (input.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                            uploadedBytes += read
                            if (totalBytes > 0) {
                                val pct = ((uploadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastReportedPct) {
                                    lastReportedPct = pct
                                    onProgress?.invoke(pct)
                                }
                            }
                        }
                    }
                }
            }

            val putRequest = Request.Builder()
                .url(targetFileUrl)
                .header("Authorization", credential)
                .put(requestBody)
                .build()

            MineServeHttpClient.client.newCall(putRequest).execute().use { response ->
                if (response.isSuccessful || response.code == 201 || response.code == 204) {
                    Result.success("Uploaded ${backupFile.name} to WebDAV successfully.")
                } else {
                    Result.failure(IOException("WebDAV upload failed with HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading backup to WebDAV: ${e.message}")
            Result.failure(e)
        }
    }

    private fun ensureDirectoryExists(directoryUrl: String, credential: String) {
        try {
            val req = Request.Builder()
                .url(directoryUrl)
                .header("Authorization", credential)
                .method("MKCOL", null)
                .build()

            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                // HTTP 201 Created or 405 Method Not Allowed (already exists) are expected
                Log.d(TAG, "MKCOL $directoryUrl returned ${resp.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "MKCOL check failed for $directoryUrl: ${e.message}")
        }
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (clean.isBlank()) return ""
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return clean
    }
}

