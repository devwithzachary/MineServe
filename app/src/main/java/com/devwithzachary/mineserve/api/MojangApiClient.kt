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

class MojangApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "MojangApiClient"
        private const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    }

    suspend fun getReleaseVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(MANIFEST_URL)
                .header("User-Agent", "MineServe-Android")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val body = resp.body?.string() ?: return@withContext emptyList()
            val obj = json.parseToJsonElement(body).jsonObject
            val versions = obj["versions"]?.jsonArray ?: return@withContext emptyList()

            val list = versions.mapNotNull {
                val vObj = it.jsonObject
                val type = vObj["type"]?.jsonPrimitive?.content
                if (type == "release") vObj["id"]?.jsonPrimitive?.content else null
            }
            return@withContext list.sortedMinecraftVersionsDescending()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Mojang release versions", e)
            return@withContext listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5")
        }
    }

    suspend fun getServerJarDownloadUrl(version: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(MANIFEST_URL)
                .header("User-Agent", "MineServe-Android")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            val obj = json.parseToJsonElement(body).jsonObject
            val versions = obj["versions"]?.jsonArray ?: return@withContext null

            val versionEntry = versions.firstOrNull {
                it.jsonObject["id"]?.jsonPrimitive?.content == version
            }?.jsonObject ?: return@withContext null

            val versionUrl = versionEntry["url"]?.jsonPrimitive?.content ?: return@withContext null
            val detailReq = Request.Builder().url(versionUrl).header("User-Agent", "MineServe-Android").build()
            val detailResp = client.newCall(detailReq).execute()
            if (!detailResp.isSuccessful) return@withContext null
            val detailBody = detailResp.body?.string() ?: return@withContext null
            val detailObj = json.parseToJsonElement(detailBody).jsonObject

            return@withContext detailObj["downloads"]?.jsonObject?.get("server")?.jsonObject?.get("url")?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Mojang server URL for $version", e)
            return@withContext null
        }
    }
}
