package com.devwithzachary.mineserve.api

import android.util.Log
import com.devwithzachary.mineserve.model.PluginModEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
data class ModrinthProjectDetails(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val body: String = "",
    val iconUrl: String? = null,
    val author: String = "",
    val downloads: Int = 0,
    val followers: Int = 0,
    val categories: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val gameVersions: List<String> = emptyList(),
    val sourceUrl: String? = null,
    val wikiUrl: String? = null,
    val issuesUrl: String? = null
)

@Serializable
data class ModrinthVersionFile(
    val url: String,
    val fileName: String,
    val primary: Boolean,
    val sizeBytes: Long
)

@Serializable
data class ModrinthVersionInfo(
    val id: String,
    val name: String,
    val versionNumber: String,
    val gameVersions: List<String>,
    val loaders: List<String>,
    val files: List<ModrinthVersionFile>
)

class ModrinthApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "ModrinthApiClient"
        private const val BASE_URL = "https://api.modrinth.com/v2"
        private const val USER_AGENT = "MineServe-Android/1.0.0 (https://github.com/devwithzachary/mineserve)"
    }

    suspend fun search(
        query: String,
        isMod: Boolean,
        loaderFilter: String? = null,
        gameVersion: String? = null
    ): List<PluginModEntry> = withContext(Dispatchers.IO) {
        try {
            val projectType = if (isMod) "mod" else "plugin"
            val facetList = mutableListOf("[\"project_type:$projectType\"]")

            if (isMod && !loaderFilter.isNullOrBlank()) {
                val cleanLoader = loaderFilter.lowercase()
                if (cleanLoader.contains("fabric")) {
                    facetList.add("[\"categories:fabric\"]")
                } else if (cleanLoader.contains("neoforge") || cleanLoader.contains("forge")) {
                    facetList.add("[\"categories:neoforge\",\"categories:forge\"]")
                }
            } else if (!isMod) {
                // Plugin loaders: paper, spigot, purpur, folia, bukkit
                facetList.add("[\"categories:paper\",\"categories:spigot\",\"categories:purpur\",\"categories:folia\",\"categories:bukkit\"]")
            }

            val facetsParam = "[${facetList.joinToString(",")}]"
            val encodedFacets = URLEncoder.encode(facetsParam, "UTF-8")
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$BASE_URL/search?query=$encodedQuery&facets=$encodedFacets&limit=25"

            Log.d(TAG, "Executing Modrinth search: $url")
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                Log.w(TAG, "Search HTTP error: ${resp.code}")
                return@withContext emptyList()
            }
            val body = resp.body?.string() ?: return@withContext emptyList()
            val obj = json.parseToJsonElement(body).jsonObject
            val hits = obj["hits"]?.jsonArray ?: return@withContext emptyList()

            return@withContext hits.map {
                val hit = it.jsonObject
                val categories = hit["categories"]?.jsonArray?.map { c -> c.jsonPrimitive.content } ?: emptyList()
                val slug = hit["slug"]?.jsonPrimitive?.content ?: "item"
                val downloads = hit["downloads"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val iconUrl = hit["icon_url"]?.jsonPrimitive?.content?.takeIf { u -> u.isNotBlank() && u != "null" }
                val title = hit["title"]?.jsonPrimitive?.content ?: slug
                val desc = hit["description"]?.jsonPrimitive?.content ?: ""
                val author = hit["author"]?.jsonPrimitive?.content ?: ""
                val latestVer = hit["latest_version"]?.jsonPrimitive?.content ?: ""

                PluginModEntry(
                    id = hit["project_id"]?.jsonPrimitive?.content ?: slug,
                    fileName = "$slug.jar",
                    name = title,
                    version = latestVer,
                    description = desc,
                    author = author,
                    enabled = true,
                    isMod = isMod,
                    iconUrl = iconUrl,
                    downloads = downloads,
                    categories = categories,
                    slug = slug
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search Modrinth", e)
            return@withContext emptyList()
        }
    }

    suspend fun getProjectDetails(projectIdOrSlug: String): ModrinthProjectDetails? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/project/$projectIdOrSlug"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            val obj = json.parseToJsonElement(body).jsonObject

            val id = obj["id"]?.jsonPrimitive?.content ?: projectIdOrSlug
            val slug = obj["slug"]?.jsonPrimitive?.content ?: projectIdOrSlug
            val title = obj["title"]?.jsonPrimitive?.content ?: slug
            val desc = obj["description"]?.jsonPrimitive?.content ?: ""
            val fullBody = obj["body"]?.jsonPrimitive?.content ?: ""
            val iconUrl = obj["icon_url"]?.jsonPrimitive?.content?.takeIf { u -> u.isNotBlank() && u != "null" }
            val downloads = obj["downloads"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val followers = obj["followers"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val categories = obj["categories"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val loaders = obj["loaders"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val gameVersions = obj["game_versions"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val sourceUrl = obj["source_url"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val wikiUrl = obj["wiki_url"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            val issuesUrl = obj["issues_url"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

            ModrinthProjectDetails(
                id = id,
                slug = slug,
                title = title,
                description = desc,
                body = fullBody,
                iconUrl = iconUrl,
                downloads = downloads,
                followers = followers,
                categories = categories,
                loaders = loaders,
                gameVersions = gameVersions,
                sourceUrl = sourceUrl,
                wikiUrl = wikiUrl,
                issuesUrl = issuesUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed getting details for $projectIdOrSlug", e)
            null
        }
    }

    suspend fun resolveDownloadUrl(
        projectIdOrSlug: String,
        isMod: Boolean,
        loaderFilter: String? = null,
        gameVersion: String? = null
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        // Returns Pair(fileName, downloadUrl)
        try {
            val url = "$BASE_URL/project/$projectIdOrSlug/version"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null
            val versionsArray = json.parseToJsonElement(body).jsonArray

            val versionList = versionsArray.mapNotNull { vItem ->
                val vObj = vItem.jsonObject
                val vId = vObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val vName = vObj["name"]?.jsonPrimitive?.content ?: ""
                val vNum = vObj["version_number"]?.jsonPrimitive?.content ?: ""
                val loaders = vObj["loaders"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val gVers = vObj["game_versions"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                val files = vObj["files"]?.jsonArray?.mapNotNull { fItem ->
                    val fObj = fItem.jsonObject
                    val fUrl = fObj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val fName = fObj["filename"]?.jsonPrimitive?.content ?: "plugin.jar"
                    val isPrimary = fObj["primary"]?.jsonPrimitive?.content == "true"
                    val fSize = fObj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    ModrinthVersionFile(fUrl, fName, isPrimary, fSize)
                } ?: emptyList()

                ModrinthVersionInfo(vId, vName, vNum, gVers, loaders, files)
            }

            if (versionList.isEmpty()) return@withContext null

            // 1. Try finding version matching loader and gameVersion
            val targetLoader = loaderFilter?.lowercase()
            var matchedVersion: ModrinthVersionInfo? = null

            if (targetLoader != null) {
                matchedVersion = versionList.firstOrNull { ver ->
                    val hasLoader = ver.loaders.any { it.lowercase() == targetLoader || (targetLoader.contains("forge") && it.contains("forge")) }
                    val hasGameVersion = gameVersion == null || ver.gameVersions.contains(gameVersion)
                    hasLoader && hasGameVersion
                }
                if (matchedVersion == null) {
                    matchedVersion = versionList.firstOrNull { ver ->
                        ver.loaders.any { it.lowercase() == targetLoader || (targetLoader.contains("forge") && it.contains("forge")) }
                    }
                }
            }

            // 2. Fallback to first available version
            if (matchedVersion == null) {
                matchedVersion = versionList.firstOrNull()
            }

            val file = matchedVersion?.files?.firstOrNull { it.primary } ?: matchedVersion?.files?.firstOrNull()
            if (file != null) {
                return@withContext Pair(file.fileName, file.url)
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed resolving download for $projectIdOrSlug", e)
            null
        }
    }
}
