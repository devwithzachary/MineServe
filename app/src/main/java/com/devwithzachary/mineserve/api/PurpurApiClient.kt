package com.devwithzachary.mineserve.api

import android.util.Log
import com.devwithzachary.mineserve.model.DEFAULT_MINECRAFT_FALLBACK_VERSIONS
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

class PurpurApiClient(
    private val client: OkHttpClient = MineServeHttpClient.client,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "PurpurApiClient"
        private const val BASE_URL = "https://api.purpurmc.org/v2/purpur"
    }

    suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = MineServeHttpClient.newGetRequest(BASE_URL)
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext DEFAULT_MINECRAFT_FALLBACK_VERSIONS
                val body = resp.body?.string() ?: return@withContext DEFAULT_MINECRAFT_FALLBACK_VERSIONS
                val obj = json.parseToJsonElement(body).jsonObject
                val versionsArray = obj["versions"]?.jsonArray
                if (versionsArray != null) {
                    return@withContext versionsArray.map { it.jsonPrimitive.content }.sortedMinecraftVersionsDescending()
                }
                DEFAULT_MINECRAFT_FALLBACK_VERSIONS
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Purpur versions", e)
            DEFAULT_MINECRAFT_FALLBACK_VERSIONS
        }
    }

    fun getDownloadUrl(version: String): String {
        return "$BASE_URL/$version/latest/download"
    }

    suspend fun getLatestBuildInfo(version: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val req = MineServeHttpClient.newGetRequest("$BASE_URL/$version")
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Pair("latest", getDownloadUrl(version))
                val body = resp.body?.string() ?: return@withContext Pair("latest", getDownloadUrl(version))
                val obj = json.parseToJsonElement(body).jsonObject
                val latestBuild = obj["builds"]?.jsonObject?.get("latest")?.jsonPrimitive?.content
                if (!latestBuild.isNullOrBlank()) {
                    return@withContext Pair(latestBuild, getDownloadUrl(version))
                }
                Pair("latest", getDownloadUrl(version))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Purpur latest build for $version", e)
            Pair("latest", getDownloadUrl(version))
        }
    }
}
