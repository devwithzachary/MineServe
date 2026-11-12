package com.devwithzachary.mineserve.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

/**
 * Resolves download URLs and versions for Mojang Bedrock Dedicated Server (BDS).
 */
class BedrockApiClient(
    private val client: OkHttpClient = MineServeHttpClient.client,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "BedrockApiClient"
        private const val BDS_LINKS_API = "https://net-secondary.web.minecraft-services.net/api/v1.0/download/links"
    }

    suspend fun getBdsVersions(): List<String> = withContext(Dispatchers.IO) {
        val build = getLatestBdsBuild()
        if (build != null) listOf(build.first) else listOf("Latest")
    }

    suspend fun getLatestBdsBuild(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val req = MineServeHttpClient.newGetRequest(BDS_LINKS_API)
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val root = json.parseToJsonElement(body).jsonObject
                val links = root["result"]?.jsonObject?.get("links")?.jsonArray ?: return@withContext null
                for (item in links) {
                    val obj = item.jsonObject
                    if (obj["downloadType"]?.jsonPrimitive?.content == "serverBedrockLinux") {
                        val url = obj["downloadUrl"]?.jsonPrimitive?.content ?: continue
                        val ver = Regex("""bedrock-server-([0-9.]+)\.zip""")
                            .find(url)?.groupValues?.getOrNull(1) ?: "Latest"
                        return@withContext Pair(ver, url)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve BDS download link", e)
        }
        null
    }
}

