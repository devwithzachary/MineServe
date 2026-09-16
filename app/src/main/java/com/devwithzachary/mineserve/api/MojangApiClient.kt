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
    private val client: OkHttpClient = MineServeHttpClient.client,
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
                .header("User-Agent", MineServeHttpClient.USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Mojang release versions", e)
            return@withContext MineServeHttpClient.DEFAULT_FALLBACK_VERSIONS
        }
    }

    suspend fun getServerJarDownloadUrl(version: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(MANIFEST_URL)
                .header("User-Agent", MineServeHttpClient.USER_AGENT)
                .build()
            val versionUrl = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val obj = json.parseToJsonElement(body).jsonObject
                val versions = obj["versions"]?.jsonArray ?: return@withContext null

                val versionEntry = versions.firstOrNull {
                    it.jsonObject["id"]?.jsonPrimitive?.content == version
                }?.jsonObject ?: return@withContext null

                versionEntry["url"]?.jsonPrimitive?.content
            } ?: return@withContext null

            val detailReq = Request.Builder().url(versionUrl).header("User-Agent", MineServeHttpClient.USER_AGENT).build()
            client.newCall(detailReq).execute().use { detailResp ->
                if (!detailResp.isSuccessful) return@withContext null
                val detailBody = detailResp.body?.string() ?: return@withContext null
                val detailObj = json.parseToJsonElement(detailBody).jsonObject

                return@withContext detailObj["downloads"]?.jsonObject?.get("server")?.jsonObject?.get("url")?.jsonPrimitive?.content
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Mojang server URL for $version", e)
            return@withContext null
        }
    }
}
