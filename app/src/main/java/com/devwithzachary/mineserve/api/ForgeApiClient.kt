package com.devwithzachary.mineserve.api

import android.util.Log
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class ForgeApiClient(
    private val client: OkHttpClient = MineServeHttpClient.client,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "ForgeApiClient"
        private const val PROMOTIONS_URL = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json"
        private const val MAVEN_BASE = "https://maven.minecraftforge.net/net/minecraftforge/forge"

        @Volatile
        private var cachedPromotions: Map<String, String>? = null
    }

    suspend fun getVersions(): List<String> = withContext(Dispatchers.IO) {
        try {
            val promos = fetchPromotions()
            if (promos.isEmpty()) return@withContext defaultFallbackVersions()

            val mcVersions = mutableSetOf<String>()
            for (key in promos.keys) {
                val mc = key.removeSuffix("-recommended").removeSuffix("-latest")
                if (mc.isNotBlank() && !mc.contains("craftmine")) {
                    mcVersions.add(mc)
                }
            }

            val sorted = mcVersions.toList().sortedMinecraftVersionsDescending()
            if (sorted.isNotEmpty()) sorted else defaultFallbackVersions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Forge versions", e)
            defaultFallbackVersions()
        }
    }

    suspend fun getPromoVersion(mcVersion: String): String? = withContext(Dispatchers.IO) {
        val promos = fetchPromotions()
        promos["$mcVersion-recommended"] ?: promos["$mcVersion-latest"]
    }

    suspend fun getDownloadUrl(mcVersion: String): String? = withContext(Dispatchers.IO) {
        try {
            val promos = fetchPromotions()
            val promoVersion = promos["$mcVersion-recommended"] ?: promos["$mcVersion-latest"]
                ?: return@withContext null

            // Build candidate URLs to handle Forge maven naming variations across historical releases
            val candidates = mutableListOf<String>()

            // 1. Known historical Forge naming quirks
            when {
                mcVersion == "1.7.10" && promoVersion.contains("1614") -> {
                    candidates.add("$MAVEN_BASE/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-installer.jar")
                }
                mcVersion == "1.8.9" && promoVersion.contains("2318") -> {
                    candidates.add("$MAVEN_BASE/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar")
                }
                mcVersion == "1.10" && promoVersion.contains("2000") -> {
                    candidates.add("$MAVEN_BASE/1.10-12.18.0.2000-1.10.0/forge-1.10-12.18.0.2000-1.10.0-installer.jar")
                }
                mcVersion == "1.7.2" && promoVersion.contains("1161") -> {
                    candidates.add("$MAVEN_BASE/1.7.2-10.12.2.1161-mc172/forge-1.7.2-10.12.2.1161-mc172-installer.jar")
                }
            }

            val fullVersion = "$mcVersion-$promoVersion"

            // 2. Early archive distributions (1.1 - 1.4.7)
            if (mcVersion in setOf("1.1", "1.2.3", "1.2.4", "1.2.5")) {
                candidates.add("$MAVEN_BASE/$fullVersion/forge-$fullVersion-server.zip")
            }
            if (mcVersion in setOf("1.3.2", "1.4.0", "1.4.1", "1.4.2", "1.4.3", "1.4.4", "1.4.5", "1.4.6", "1.4.7")) {
                candidates.add("$MAVEN_BASE/$fullVersion/forge-$fullVersion-universal.zip")
            }

            // 3. Standard installer pattern (used for 1.5.2 up to latest)
            candidates.add("$MAVEN_BASE/$fullVersion/forge-$fullVersion-installer.jar")
            candidates.add("$MAVEN_BASE/$fullVersion-$mcVersion/forge-$fullVersion-$mcVersion-installer.jar")

            // Test candidate URLs with HEAD request to guarantee a reachable artifact
            for (url in candidates) {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .head()
                        .header("User-Agent", MineServeHttpClient.USER_AGENT)
                        .build()
                    client.newCall(req).execute().use { res ->
                        if (res.isSuccessful) {
                            return@withContext url
                        }
                    }
                } catch (_: Exception) {}
            }

            // Default fallback to first candidate if probe was blocked or offline
            candidates.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed resolving Forge download URL for $mcVersion", e)
            null
        }
    }

    private suspend fun fetchPromotions(): Map<String, String> = withContext(Dispatchers.IO) {
        cachedPromotions?.let { return@withContext it }
        try {
            val req = Request.Builder()
                .url(PROMOTIONS_URL)
                .header("User-Agent", MineServeHttpClient.USER_AGENT)
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyMap()
                val body = resp.body?.string() ?: return@withContext emptyMap()
                val obj = json.parseToJsonElement(body).jsonObject
                val promosObj = obj["promos"]?.jsonObject ?: return@withContext emptyMap()
                val map = promosObj.mapValues { it.value.jsonPrimitive.content }
                cachedPromotions = map
                map
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Forge promotions", e)
            emptyMap()
        }
    }

    private fun defaultFallbackVersions(): List<String> = listOf(
        "1.20.4", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.16.5", "1.12.2", "1.8.9", "1.7.10"
    )
}
