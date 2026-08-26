package com.devwithzachary.mineserve.api

import android.util.Log
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PurpurApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "PurpurApiClient"
        private const val BASE_URL = "https://api.purpurmc.org/v2/purpur"
    }

    suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(BASE_URL)
                .header("User-Agent", "MineServe-Android")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext defaultFallbackVersions()
            val body = resp.body?.string() ?: return@withContext defaultFallbackVersions()
            val obj = json.parseToJsonElement(body).jsonObject
            val versionsArray = obj["versions"]?.jsonArray
            if (versionsArray != null) {
                return@withContext versionsArray.map { it.jsonPrimitive.content }.sortedMinecraftVersionsDescending()
            }
            defaultFallbackVersions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Purpur versions", e)
            defaultFallbackVersions()
        }
    }

    fun getDownloadUrl(version: String): String {
        return "$BASE_URL/$version/latest/download"
    }

    private fun defaultFallbackVersions(): List<String> = listOf(
        "26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"
    )
}
