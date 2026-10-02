package com.devwithzachary.mineserve.engine

import android.util.Log
import com.devwithzachary.mineserve.api.FabricApiClient
import com.devwithzachary.mineserve.api.ForgeApiClient
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.api.ModrinthApiClient
import com.devwithzachary.mineserve.api.MojangApiClient
import com.devwithzachary.mineserve.api.NeoForgeApiClient
import com.devwithzachary.mineserve.api.PaperApiClient
import com.devwithzachary.mineserve.api.PurpurApiClient
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerBuildInfo
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
import com.devwithzachary.mineserve.repository.BackupRepository
import com.devwithzachary.mineserve.repository.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Dedicated manager for Minecraft server software operations:
 * - Version manifest resolution and latest build querying across all server software types
 * - Downloading server binaries and installer archives with progress streaming
 * - Automated in-place build updates with safety backups
 * - Cross-version server upgrades, Java runtime realignment, and installer cleanup
 */
class ServerSoftwareManager(
    private val serverRepository: ServerRepository,
    private val backupRepository: BackupRepository,
    private val processManager: ServerProcessManager
) {
    companion object {
        private const val TAG = "ServerSoftwareManager"
    }

    suspend fun resolveLatestBuild(type: ServerType, version: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        when (type) {
            ServerType.PAPER, ServerType.BEDROCK_GEYSER -> {
                PaperApiClient().getLatestBuildInfo("paper", version)
            }
            ServerType.PURPUR -> {
                PurpurApiClient().getLatestBuildInfo(version)
            }
            ServerType.FOLIA -> {
                PaperApiClient().getLatestBuildInfo("folia", version)
            }
            ServerType.VANILLA -> {
                MojangApiClient().getServerJarDownloadUrl(version)?.let { Pair("Release", it) }
            }
            ServerType.FABRIC -> {
                val api = FabricApiClient()
                val loader = api.getLatestLoaderVersion()
                val url = api.getFabricServerJarUrl(version)
                Pair("Loader $loader", url)
            }
            ServerType.FORGE -> {
                val forgeApi = ForgeApiClient()
                val url = forgeApi.getDownloadUrl(version)
                val build = forgeApi.getPromoVersion(version) ?: "Latest"
                if (url != null) Pair("Forge $build", url) else null
            }
            ServerType.NEOFORGE -> {
                val url = NeoForgeApiClient().getDownloadUrl(version)
                Pair("Installer", url)
            }
            ServerType.CUSTOM -> null
        }
    }

    suspend fun fetchAvailableVersionsForServer(type: ServerType): List<String> = withContext(Dispatchers.IO) {
        try {
            val list = when (type) {
                ServerType.PAPER, ServerType.BEDROCK_GEYSER -> {
                    PaperApiClient().getProjectVersions("paper")
                }
                ServerType.PURPUR -> {
                    PurpurApiClient().getVersions()
                }
                ServerType.FOLIA -> {
                    PaperApiClient().getProjectVersions("folia")
                }
                ServerType.VANILLA -> {
                    MojangApiClient().getReleaseVersions()
                }
                ServerType.FABRIC -> {
                    FabricApiClient().getGameVersions()
                }
                ServerType.FORGE -> {
                    ForgeApiClient().getVersions()
                }
                ServerType.NEOFORGE -> {
                    NeoForgeApiClient().getVersions()
                }
                else -> {
                    listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.1", "1.20.4", "1.20.1")
                }
            }
            list.sortedMinecraftVersionsDescending()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch versions for ${type.displayName}", e)
            listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.1", "1.20.4", "1.20.1")
        }
    }

    suspend fun checkForServerBuildUpdate(server: MinecraftServer): ServerBuildInfo? = withContext(Dispatchers.IO) {
        if (server.type == ServerType.CUSTOM) return@withContext null
        val buildInfo = resolveLatestBuild(server.type, server.version) ?: return@withContext null
        val latestBuild = buildInfo.first
        val downloadUrl = buildInfo.second
        val currentBuild = server.serverBuild
        val isUpdateAvailable = currentBuild != null && currentBuild != latestBuild
        ServerBuildInfo(
            currentBuild = currentBuild,
            latestBuild = latestBuild,
            isUpdateAvailable = isUpdateAvailable,
            downloadUrl = downloadUrl
        )
    }

    suspend fun downloadFileWithProgress(
        fileUrl: String,
        destFile: File,
        onProgress: (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = MineServeHttpClient.newGetRequest(fileUrl)

        MineServeHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server returned HTTP ${response.code} for $fileUrl")
            }
            val body = response.body ?: throw IOException("Empty response body from $fileUrl")
            val contentLength = body.contentLength()

            body.byteStream().use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    val buffer = ByteArray(65536)
                    var totalRead = 0L
                    var read: Int

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        totalRead += read
                        outputStream.write(buffer, 0, read)
                        onProgress(totalRead, contentLength)
                    }
                    outputStream.flush()
                }
            }
        }
    }

    suspend fun provisionServerSoftware(
        server: MinecraftServer,
        serverDir: File,
        jarUrl: String?,
        onProgress: (String, Int) -> Unit
    ) {
        val destJar = File(serverDir, server.jarFileName)

        if (!jarUrl.isNullOrEmpty()) {
            val isZip = jarUrl.endsWith(".zip", ignoreCase = true)
            val targetFile = if (isZip) File(serverDir, "server_archive.zip") else destJar
            onProgress("Downloading ${if (isZip) "archive" else "server.jar"}...", 30)
            downloadFileWithProgress(jarUrl, targetFile) { bytesRead, totalBytes ->
                val percent = if (totalBytes > 0) 30 + ((bytesRead * 60) / totalBytes).toInt() else 50
                onProgress("Downloading (${bytesRead / (1024 * 1024)} MB)...", percent)
            }
            if (isZip) {
                onProgress("Unpacking server archive...", 90)
                ServerRepository.extractZip(targetFile, serverDir, deleteZipAfter = true)
            }
        } else {
            // Create dummy/placeholder jar if URL couldn't be resolved
            destJar.writeBytes(ByteArray(1024))
        }

        // If Geyser cross-play requested, download Geyser plugin
        if (server.type == ServerType.BEDROCK_GEYSER) {
            onProgress("Bundling GeyserMC for Bedrock cross-play...", 90)
            try {
                val geyserUrl = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
                val pluginsDir = File(serverDir, "plugins").apply { if (!exists()) mkdirs() }
                val geyserDest = File(pluginsDir, "Geyser-Spigot.jar")
                downloadFileWithProgress(geyserUrl, geyserDest) { _, _ -> }
            } catch (_: Exception) {}
        }
    }

    suspend fun updateServerBuild(
        server: MinecraftServer,
        createBackup: Boolean,
        onProgress: (String, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverId = server.id
            val serverDir = serverRepository.getServerDirectory(serverId)

            // Stop server if running
            val isRunning = server.isRunning || processManager.isServerRunning(serverId)
            if (isRunning) {
                onProgress("Stopping server...", 5)
                processManager.forceStopAndCleanup(serverId)
                delay(500)
            }

            // Create pre-update backup if requested
            if (createBackup) {
                onProgress("Creating safety backup...", 10)
                backupRepository.createBackup(
                    serverDir = serverDir,
                    isWorldOnly = false,
                    customName = "pre_build_update_${server.version}_${System.currentTimeMillis()}"
                )
            }

            onProgress("Resolving latest build...", 20)
            val buildInfo = resolveLatestBuild(server.type, server.version)
                ?: return@withContext false

            val destJar = File(serverDir, server.jarFileName.ifBlank { "server.jar" })
            val tempJar = File(serverDir, "${destJar.name}.tmp")

            onProgress("Downloading ${server.type.displayName} (Build ${buildInfo.first})...", 30)
            downloadFileWithProgress(buildInfo.second, tempJar) { bytesRead, totalBytes ->
                val percent = if (totalBytes > 0) 30 + ((bytesRead * 60) / totalBytes).toInt() else 60
                val mb = bytesRead / (1024 * 1024)
                onProgress("Downloading server.jar ($mb MB)...", percent)
            }

            if (tempJar.exists() && tempJar.length() > 0) {
                if (destJar.exists()) destJar.delete()
                tempJar.renameTo(destJar)
            } else {
                return@withContext false
            }

            // Update server build configuration
            val updated = server.copy(serverBuild = buildInfo.first)
            serverRepository.updateServer(updated)

            onProgress("Build updated to ${buildInfo.first} successfully!", 100)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating server build", e)
            false
        }
    }

    suspend fun updateServerBuild(
        serverId: String,
        createBackup: Boolean,
        onProgress: (String, Int) -> Unit
    ): Boolean {
        val server = serverRepository.loadServers().firstOrNull { it.id == serverId } ?: return false
        return updateServerBuild(server, createBackup, onProgress)
    }

    suspend fun upgradeServerVersion(
        server: MinecraftServer,
        newVersion: String,
        createBackup: Boolean,
        onProgress: (String, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverId = server.id
            val serverDir = serverRepository.getServerDirectory(serverId)

            // Stop server if running
            val isRunning = server.isRunning || processManager.isServerRunning(serverId)
            if (isRunning) {
                onProgress("Stopping server...", 5)
                processManager.forceStopAndCleanup(serverId)
                delay(500)
            }

            // Create pre-upgrade backup if requested
            if (createBackup) {
                onProgress("Creating pre-upgrade world backup...", 10)
                backupRepository.createBackup(
                    serverDir = serverDir,
                    isWorldOnly = false,
                    customName = "pre_upgrade_${server.version}_to_${newVersion}_${System.currentTimeMillis()}"
                )
            }

            onProgress("Resolving download URL for $newVersion...", 20)
            val buildInfo = resolveLatestBuild(server.type, newVersion)
                ?: return@withContext false

            val isInstaller = server.type == ServerType.NEOFORGE || server.type == ServerType.FORGE
            val isZip = buildInfo.second.endsWith(".zip", ignoreCase = true)
            val targetJarName = if (isInstaller) "installer.jar" else server.jarFileName.ifBlank { "server.jar" }
            val destJar = File(serverDir, targetJarName)
            val tempJar = File(serverDir, "${targetJarName}.tmp")

            if (isZip) {
                val archiveFile = File(serverDir, "server_archive.zip")
                onProgress("Downloading ${server.type.displayName} $newVersion archive...", 30)
                downloadFileWithProgress(buildInfo.second, archiveFile) { bytesRead, totalBytes ->
                    val percent = if (totalBytes > 0) 30 + ((bytesRead * 60) / totalBytes).toInt() else 60
                    val mb = bytesRead / (1024 * 1024)
                    onProgress("Downloading archive ($mb MB)...", percent)
                }
                onProgress("Unpacking server archive...", 85)
                ServerRepository.extractZip(archiveFile, serverDir, deleteZipAfter = true)
            } else {
                onProgress("Downloading ${server.type.displayName} $newVersion...", 30)
                downloadFileWithProgress(buildInfo.second, tempJar) { bytesRead, totalBytes ->
                    val percent = if (totalBytes > 0) 30 + ((bytesRead * 60) / totalBytes).toInt() else 60
                    val mb = bytesRead / (1024 * 1024)
                    onProgress("Downloading server jar ($mb MB)...", percent)
                }

                if (tempJar.exists() && tempJar.length() > 0) {
                    if (destJar.exists()) destJar.delete()
                    tempJar.renameTo(destJar)
                    if (isInstaller) {
                        File(serverDir, "run.sh").delete()
                        serverDir.listFiles { f -> (f.name.startsWith("forge-") || f.name.startsWith("neoforge-")) && f.name.endsWith(".jar") && !f.name.contains("installer") }?.forEach { it.delete() }
                    }
                } else {
                    return@withContext false
                }
            }

            // Update Geyser plugin if Bedrock Cross-Play server
            if (server.type == ServerType.BEDROCK_GEYSER) {
                onProgress("Updating GeyserMC cross-play plugin...", 92)
                try {
                    val geyserUrl = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
                    val geyserDest = File(File(serverDir, "plugins"), "Geyser-Spigot.jar")
                    downloadFileWithProgress(geyserUrl, geyserDest) { _, _ -> }
                } catch (_: Exception) {}
            }

            // Auto-upgrade Live Map plugin (e.g. Squaremap) if installed on this server
            upgradeLiveMapPluginIfInstalled(server, serverDir, newVersion, onProgress)

            // Determine required Java version and update config
            val requiredJava = determineJavaVersion(newVersion, server.type)
            val updatedJava = maxOf(server.javaVersion, requiredJava)
            val updated = server.copy(
                version = newVersion,
                serverBuild = buildInfo.first,
                jarFileName = targetJarName,
                javaVersion = updatedJava
            )

            serverRepository.updateServer(updated)

            onProgress("Successfully upgraded to Minecraft $newVersion!", 100)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading server version", e)
            false
        }
    }

    suspend fun upgradeServerVersion(
        serverId: String,
        newVersion: String,
        createBackup: Boolean,
        onProgress: (String, Int) -> Unit
    ): Boolean {
        val server = serverRepository.loadServers().firstOrNull { it.id == serverId } ?: return false
        return upgradeServerVersion(server, newVersion, createBackup, onProgress)
    }

    /**
     * If a live web map plugin (e.g. Squaremap) is currently installed on the server,
     * attempts to resolve and download an updated compatible version for the target Minecraft version.
     */
    internal suspend fun upgradeLiveMapPluginIfInstalled(
        server: MinecraftServer,
        serverDir: File,
        newVersion: String,
        onProgress: (String, Int) -> Unit
    ) {
        val installedMapPlugin = WebMapPluginType.detectInstalled(serverDir) ?: return
        onProgress("Upgrading ${installedMapPlugin.displayName} live map plugin...", 94)
        try {
            val isModServer = server.type.supportsMods && !server.type.supportsPlugins
            val loader = when (server.type) {
                ServerType.FABRIC -> "fabric"
                ServerType.FORGE -> "forge"
                ServerType.NEOFORGE -> "neoforge"
                else -> "paper"
            }
            val modrinth = ModrinthApiClient()
            val resolved = modrinth.resolveDownloadUrl(
                projectIdOrSlug = installedMapPlugin.modrinthSlug,
                isMod = isModServer,
                loaderFilter = loader,
                gameVersion = newVersion
            ) ?: modrinth.resolveDownloadUrl(
                projectIdOrSlug = installedMapPlugin.modrinthSlug,
                isMod = isModServer,
                loaderFilter = loader,
                gameVersion = null
            ) ?: modrinth.resolveDownloadUrl(
                projectIdOrSlug = installedMapPlugin.modrinthSlug,
                isMod = isModServer,
                loaderFilter = null,
                gameVersion = null
            )

            if (resolved != null) {
                val targetFolder = if (isModServer) File(serverDir, "mods") else File(serverDir, "plugins")
                if (!targetFolder.exists()) targetFolder.mkdirs()

                val newFile = File(targetFolder, resolved.first)
                val tempFile = File(targetFolder, "${resolved.first}.tmp")

                downloadFileWithProgress(resolved.second, tempFile) { _, _ -> }
                if (tempFile.exists() && tempFile.length() > 0) {
                    // Delete existing old map plugin files across plugins and mods to prevent duplicate plugin collisions
                    val oldFiles = listOf(File(serverDir, "plugins"), File(serverDir, "mods"))
                        .flatMap { it.listFiles()?.toList() ?: emptyList() }
                        .filter { it.isFile && it.name.lowercase().contains("squaremap") && it.absolutePath != tempFile.absolutePath }
                    for (old in oldFiles) {
                        old.delete()
                    }
                    if (newFile.exists()) newFile.delete()
                    tempFile.renameTo(newFile)
                    Log.i(TAG, "Successfully upgraded ${installedMapPlugin.displayName} to ${resolved.first} for Minecraft $newVersion")
                }
            } else {
                Log.w(TAG, "No compatible ${installedMapPlugin.displayName} release found on Modrinth for $newVersion")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to auto-upgrade live map plugin: ${e.message}")
        }
    }
}
