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
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending

class PaperApiClient(
    private val client: OkHttpClient = MineServeHttpClient.client,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "PaperApiClient"
        private const val V3_BASE_URL = "https://fill.papermc.io/v3"
        private const val USER_AGENT = MineServeHttpClient.USER_AGENT
    }

    suspend fun getProjectVersions(project: String = "paper"): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$V3_BASE_URL/projects/$project")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext MineServeHttpClient.DEFAULT_FALLBACK_VERSIONS
                val body = resp.body?.string() ?: return@withContext MineServeHttpClient.DEFAULT_FALLBACK_VERSIONS
                val obj = json.parseToJsonElement(body).jsonObject

                // In v3, "versions" is a map of major version to list of patch versions
                val versionsObj = obj["versions"]?.jsonObject
                if (versionsObj != null) {
                    val list = mutableListOf<String>()
                    versionsObj.values.forEach { arr ->
                        arr.jsonArray.forEach { item ->
                            val ver = item.jsonPrimitive.content
                            if (!ver.contains("-rc") && !ver.contains("-pre") && !ver.contains("-snapshot")) {
                                list.add(ver)
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        return@withContext list.sortedMinecraftVersionsDescending()
                    }
                }

                // Fallback v2 array
                val versionsArray = obj["versions"]?.jsonArray
                if (versionsArray != null) {
                    return@withContext versionsArray.map { it.jsonPrimitive.content }.sortedMinecraftVersionsDescending()
                }

                MineServeHttpClient.DEFAULT_FALLBACK_VERSIONS
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch $project versions", e)
            MineServeHttpClient.DEFAULT_FALLBACK_VERSIONS
        }
    }

    suspend fun getLatestBuildInfo(project: String = "paper", version: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$V3_BASE_URL/projects/$project/versions/$version")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val obj = json.parseToJsonElement(body).jsonObject
                        val buildsArray = obj["builds"]?.jsonArray
                        val latestBuild = buildsArray?.firstOrNull()?.jsonPrimitive?.content?.toIntOrNull()
                            ?: buildsArray?.lastOrNull()?.jsonPrimitive?.content?.toIntOrNull()

                        if (latestBuild != null) {
                            val buildReq = Request.Builder()
                                .url("$V3_BASE_URL/projects/$project/versions/$version/builds/$latestBuild")
                                .header("User-Agent", USER_AGENT)
                                .build()
                            client.newCall(buildReq).execute().use { buildResp ->
                                if (buildResp.isSuccessful) {
                                    val buildBody = buildResp.body?.string()
                                    if (!buildBody.isNullOrEmpty()) {
                                        val buildObj = json.parseToJsonElement(buildBody).jsonObject
                                        val downloads = buildObj["downloads"]?.jsonObject
                                        if (downloads != null) {
                                            for ((_, dlValue) in downloads) {
                                                val dlObj = dlValue.jsonObject
                                                val directUrl = dlObj["url"]?.jsonPrimitive?.content
                                                if (!directUrl.isNullOrEmpty()) {
                                                    Log.d(TAG, "Resolved PaperMC direct v3 URL for build $latestBuild: $directUrl")
                                                    return@withContext Pair(latestBuild.toString(), directUrl)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving build info for $project $version: ${e.message}")
        }
        null
    }

    suspend fun getLatestBuildDownloadUrl(project: String, version: String): String? = withContext(Dispatchers.IO) {
        val info = getLatestBuildInfo(project, version)
        if (info != null) return@withContext info.second

        // Fallback: Mojang Vanilla JAR if Paper fails
        try {
            return@withContext MojangApiClient().getServerJarDownloadUrl(version)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback download URL failed", e)
            return@withContext null
        }
    }
}
