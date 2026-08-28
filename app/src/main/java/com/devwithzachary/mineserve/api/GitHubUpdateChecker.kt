package com.devwithzachary.mineserve.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GitHubUpdateChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "GitHubUpdateChecker"
        private const val RELEASES_API_URL = "https://api.github.com/repos/devwithzachary/mineserve/releases/latest"
        private const val USER_AGENT = "MineServe-Android (https://github.com/devwithzachary/mineserve)"
    }

    suspend fun checkLatestRelease(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(RELEASES_API_URL)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                if (resp.code == 404) {
                    return@withContext UpdateCheckResult.UpToDate(currentVersionName, currentVersionName)
                }
                return@withContext UpdateCheckResult.Error("GitHub API HTTP error: ${resp.code}")
            }

            val body = resp.body?.string() ?: return@withContext UpdateCheckResult.Error("Empty response from GitHub")
            val obj = json.parseToJsonElement(body).jsonObject

            val tagName = obj["tag_name"]?.jsonPrimitive?.content ?: ""
            val name = obj["name"]?.jsonPrimitive?.content ?: tagName
            val releaseNotes = obj["body"]?.jsonPrimitive?.content ?: ""
            val htmlUrl = obj["html_url"]?.jsonPrimitive?.content ?: "https://github.com/devwithzachary/mineserve/releases"
            val publishedAt = obj["published_at"]?.jsonPrimitive?.content ?: ""

            // Find direct APK download asset if present
            var apkUrl: String? = null
            val assetsArr = obj["assets"]?.jsonArray
            if (assetsArr != null) {
                for (assetElement in assetsArr) {
                    val assetObj = assetElement.jsonObject
                    val assetName = assetObj["name"]?.jsonPrimitive?.content ?: ""
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content
                        break
                    }
                }
            }

            val isNewer = isVersionNewer(remoteTag = tagName, currentVersion = currentVersionName)
            val release = GitHubRelease(
                tagName = tagName,
                name = name,
                body = releaseNotes,
                htmlUrl = htmlUrl,
                publishedAt = publishedAt,
                apkDownloadUrl = apkUrl,
                isNewer = isNewer
            )

            Log.d(TAG, "Fetched latest release: $tagName (isNewer=$isNewer vs current=$currentVersionName)")

            if (isNewer) {
                UpdateCheckResult.UpdateAvailable(release)
            } else {
                UpdateCheckResult.UpToDate(currentVersionName, tagName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed checking GitHub release: ${e.message}", e)
            UpdateCheckResult.Error(e.message ?: "Failed to connect to GitHub")
        }
    }

    fun isVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

        if (cleanRemote.isBlank()) return false
        if (cleanRemote.equals(cleanCurrent, ignoreCase = true)) return false

        val remoteParts = cleanRemote.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".", "-", "_").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
