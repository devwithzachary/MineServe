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

class FabricApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "FabricApiClient"
        private const val BASE_URL = "https://meta.fabricmc.net/v2"
    }

    suspend fun getGameVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE_URL/versions/game")
                .header("User-Agent", "MineServe-Android")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext defaultFallbackVersions()
            val body = resp.body?.string() ?: return@withContext defaultFallbackVersions()
            val array = json.parseToJsonElement(body).jsonArray
            val stableList = array.mapNotNull {
                val obj = it.jsonObject
                if (obj["stable"]?.jsonPrimitive?.content == "true") {
                    obj["version"]?.jsonPrimitive?.content
                } else null
            }
            if (stableList.isNotEmpty()) return@withContext stableList.sortedMinecraftVersionsDescending()
            defaultFallbackVersions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Fabric game versions", e)
            defaultFallbackVersions()
        }
    }

    suspend fun getLatestLoaderVersion(): String = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE_URL/versions/loader")
                .header("User-Agent", "MineServe-Android")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext "0.16.10"
            val body = resp.body?.string() ?: return@withContext "0.16.10"
            val array = json.parseToJsonElement(body).jsonArray
            return@withContext array.firstOrNull()?.jsonObject?.get("version")?.jsonPrimitive?.content ?: "0.16.10"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Fabric loader version", e)
            return@withContext "0.16.10"
        }
    }

    suspend fun getLatestInstallerVersion(): String = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$BASE_URL/versions/installer")
                .header("User-Agent", "MineServe-Android")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext "1.0.1"
            val body = resp.body?.string() ?: return@withContext "1.0.1"
            val array = json.parseToJsonElement(body).jsonArray
            return@withContext array.firstOrNull()?.jsonObject?.get("version")?.jsonPrimitive?.content ?: "1.0.1"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Fabric installer version", e)
            return@withContext "1.0.1"
        }
    }

    suspend fun getFabricServerJarUrl(gameVersion: String): String = withContext(Dispatchers.IO) {
        val loader = getLatestLoaderVersion()
        val installer = getLatestInstallerVersion()
        return@withContext "$BASE_URL/versions/loader/$gameVersion/$loader/$installer/server/jar"
    }

    private fun defaultFallbackVersions(): List<String> = listOf(
        "26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4"
    )
}
