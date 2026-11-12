package com.devwithzachary.mineserve.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

/**
 * Fetches latest BungeeCord build metadata and download URL from md-5 Jenkins CI.
 */
class BungeeCordApiClient(
    private val client: OkHttpClient = MineServeHttpClient.client,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "BungeeCordApiClient"
        private const val JENKINS_API_URL = "https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/api/json"
        private const val DIRECT_JAR_URL =
            "https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar"
    }

    fun getVersions(): List<String> = listOf("Latest")

    suspend fun getLatestBuildInfo(): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            val req = MineServeHttpClient.newGetRequest(JENKINS_API_URL)
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val obj = json.parseToJsonElement(body).jsonObject
                        val buildNum = obj["number"]?.jsonPrimitive?.content
                        if (!buildNum.isNullOrBlank()) {
                            return@withContext Pair("Build #$buildNum", DIRECT_JAR_URL)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query BungeeCord Jenkins API, using direct latest URL", e)
        }
        Pair("Latest", DIRECT_JAR_URL)
    }
}

