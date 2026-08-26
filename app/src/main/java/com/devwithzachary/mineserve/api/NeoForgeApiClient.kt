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

class NeoForgeApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "NeoForgeApiClient"
        private const val BASE_URL = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"
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
            val array = obj["versions"]?.jsonArray
            if (array != null) {
                val rawList = array.map { it.jsonPrimitive.content }.filter { !it.contains("craftmine") }

                // Group by Minecraft version key to only keep the single latest build per MC version
                val latestByKey = mutableMapOf<String, String>()
                for (v in rawList) {
                    val key = getGameVersionKey(v)
                    latestByKey[key] = v
                }

                val list = latestByKey.values.toList().sortedMinecraftVersionsDescending()
                if (list.isNotEmpty()) return@withContext list
            }
            defaultFallbackVersions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch NeoForge versions", e)
            defaultFallbackVersions()
        }
    }

    private fun getGameVersionKey(v: String): String {
        val clean = v.split('-', '_')[0]
        val parts = clean.split('.')
        return when {
            parts.size >= 4 -> {
                if (parts[2] == "0") "${parts[0]}.${parts[1]}"
                else "${parts[0]}.${parts[1]}.${parts[2]}"
            }
            parts.size == 3 -> "${parts[0]}.${parts[1]}"
            else -> clean
        }
    }

    fun getDownloadUrl(version: String): String {
        return "https://maven.neoforged.net/releases/net/neoforged/neoforge/$version/neoforge-$version-installer.jar"
    }

    private fun defaultFallbackVersions(): List<String> = listOf(
        "26.2.0.68", "26.1.2.98", "21.11.45", "21.4.157", "21.1.248", "20.6.139", "20.4.251", "20.2.93"
    )
}
