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
            val req = MineServeHttpClient.newGetRequest(MANIFEST_URL)
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
            return@withContext DEFAULT_MINECRAFT_FALLBACK_VERSIONS
        }
    }

    suspend fun getServerJarDownloadUrl(version: String): String? = withContext(Dispatchers.IO) {
        try {
            val req = MineServeHttpClient.newGetRequest(MANIFEST_URL)
            val versionUrl = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val obj = json.parseToJsonElement(body).jsonObject
                val versions = obj["versions"]?.jsonArray ?: return@use null

                val versionEntry = versions.firstOrNull {
                    val id = it.jsonObject["id"]?.jsonPrimitive?.content
                    id == version || (version == "1.0.0" && id == "1.0")
                }?.jsonObject ?: return@use null

                versionEntry["url"]?.jsonPrimitive?.content
            }

            if (!versionUrl.isNullOrBlank()) {
                val detailReq = MineServeHttpClient.newGetRequest(versionUrl)
                val officialUrl = client.newCall(detailReq).execute().use { detailResp ->
                    if (!detailResp.isSuccessful) return@use null
                    val detailBody = detailResp.body?.string() ?: return@use null
                    val detailObj = json.parseToJsonElement(detailBody).jsonObject

                    detailObj["downloads"]?.jsonObject?.get("server")?.jsonObject?.get("url")?.jsonPrimitive?.content
                }
                if (!officialUrl.isNullOrBlank()) {
                    return@withContext officialUrl
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mojang manifest server URL lookup failed for $version: ${e.message}")
        }

        null
    }
}
