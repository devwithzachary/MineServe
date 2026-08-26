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
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "PaperApiClient"
        private const val V3_BASE_URL = "https://fill.papermc.io/v3"
        private const val USER_AGENT = "MineServe-Android (https://github.com/devwithzachary/mineserve)"
    }

    suspend fun getProjectVersions(project: String = "paper"): List<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$V3_BASE_URL/projects/$project")
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext defaultFallbackVersions()
            val body = resp.body?.string() ?: return@withContext defaultFallbackVersions()
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

            defaultFallbackVersions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch $project versions", e)
            defaultFallbackVersions()
        }
    }

    suspend fun getLatestBuildDownloadUrl(project: String, version: String): String? = withContext(Dispatchers.IO) {
        try {
            // 1. Try v3 API
            val req = Request.Builder()
                .url("$V3_BASE_URL/projects/$project/versions/$version")
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
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
                        val buildResp = client.newCall(buildReq).execute()
                        if (buildResp.isSuccessful) {
                            val buildBody = buildResp.body?.string()
                            if (!buildBody.isNullOrEmpty()) {
                                val buildObj = json.parseToJsonElement(buildBody).jsonObject
                                val downloads = buildObj["downloads"]?.jsonObject
                                if (downloads != null) {
                                    // Check server:default, application, or any download entry
                                    for ((_, dlValue) in downloads) {
                                        val dlObj = dlValue.jsonObject
                                        val directUrl = dlObj["url"]?.jsonPrimitive?.content
                                        if (!directUrl.isNullOrEmpty()) {
                                            Log.d(TAG, "Resolved PaperMC direct v3 URL: $directUrl")
                                            return@withContext directUrl
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving v3 URL for $project $version: ${e.message}")
        }

        // Fallback: Mojang Vanilla JAR if Paper fails
        try {
            return@withContext MojangApiClient().getServerJarDownloadUrl(version)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback download URL failed", e)
            return@withContext null
        }
    }

    private fun defaultFallbackVersions(): List<String> = listOf(
        "26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"
    )
}
