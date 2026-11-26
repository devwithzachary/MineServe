package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.BedrockApiClient
import com.devwithzachary.mineserve.api.BungeeCordApiClient
import com.devwithzachary.mineserve.api.FabricApiClient
import com.devwithzachary.mineserve.api.ForgeApiClient
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.api.MojangApiClient
import com.devwithzachary.mineserve.api.NeoForgeApiClient
import com.devwithzachary.mineserve.api.PaperApiClient
import com.devwithzachary.mineserve.api.PurpurApiClient
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopServerSoftwareManager(
    private val serverRepository: DesktopServerRepository,
    private val javaManager: PortableJavaManager
) {
    companion object {
        private const val TAG = "DesktopServerSoftwareManager"
    }

    private val paperApi = PaperApiClient()
    private val purpurApi = PurpurApiClient()
    private val mojangApi = MojangApiClient()
    private val fabricApi = FabricApiClient()
    private val forgeApi = ForgeApiClient()
    private val neoForgeApi = NeoForgeApiClient()
    private val bungeeApi = BungeeCordApiClient()
    private val bedrockApi = BedrockApiClient()

    private val downloadClient by lazy {
        MineServeHttpClient.client.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .build()
    }

    suspend fun fetchAvailableVersions(type: ServerType): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (type) {
                ServerType.PAPER, ServerType.BEDROCK_GEYSER -> paperApi.getProjectVersions("paper")
                ServerType.FOLIA -> paperApi.getProjectVersions("folia")
                ServerType.VELOCITY -> paperApi.getProjectVersions("velocity")
                ServerType.PURPUR -> purpurApi.getVersions()
                ServerType.VANILLA -> mojangApi.getReleaseVersions()
                ServerType.FABRIC -> fabricApi.getGameVersions()
                ServerType.FORGE -> forgeApi.getVersions()
                ServerType.NEOFORGE -> neoForgeApi.getVersions()
                ServerType.BUNGEECORD -> bungeeApi.getVersions()
                ServerType.BEDROCK_BDS -> {
                    val downloadType = if (DesktopPlatform.isWindows) "serverBedrockWindows" else "serverBedrockLinux"
                    bedrockApi.getBdsVersions(downloadType)
                }
                ServerType.CUSTOM -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun resolveLatestBuild(type: ServerType, version: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        runCatching {
            when (type) {
                ServerType.PAPER, ServerType.BEDROCK_GEYSER -> paperApi.getLatestBuildInfo("paper", version)
                ServerType.FOLIA -> paperApi.getLatestBuildInfo("folia", version)
                ServerType.VELOCITY -> paperApi.getLatestBuildInfo("velocity", version)
                ServerType.PURPUR -> purpurApi.getLatestBuildInfo(version)
                ServerType.VANILLA -> {
                    val url = mojangApi.getServerJarDownloadUrl(version) ?: return@runCatching null
                    "Release" to url
                }
                ServerType.FABRIC -> {
                    val loader = fabricApi.getLatestLoaderVersion()
                    val url = fabricApi.getFabricServerJarUrl(version)
                    "Loader $loader" to url
                }
                ServerType.FORGE -> {
                    val url = forgeApi.getDownloadUrl(version) ?: return@runCatching null
                    val build = forgeApi.getPromoVersion(version) ?: "Latest"
                    "Forge $build" to url
                }
                ServerType.NEOFORGE -> {
                    val url = neoForgeApi.getDownloadUrl(version)
                    "Installer" to url
                }
                ServerType.BUNGEECORD -> bungeeApi.getLatestBuildInfo()
                ServerType.BEDROCK_BDS -> {
                    val downloadType = if (DesktopPlatform.isWindows) "serverBedrockWindows" else "serverBedrockLinux"
                    bedrockApi.getLatestBdsBuild(downloadType)
                }
                ServerType.CUSTOM -> null
            }
        }.getOrNull()
    }

    suspend fun downloadFile(
        url: String,
        destFile: File,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        destFile.parentFile?.mkdirs()
        val req = MineServeHttpClient.newRequestBuilder(url).build()
        downloadClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Download failed with HTTP ${resp.code} from $url")
            }
            val body = resp.body ?: throw IllegalStateException("Empty download response")
            val total = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buf = ByteArray(65536)
                    var readTotal = 0L
                    var lastPct = -1
                    while (true) {
                        val r = input.read(buf)
                        if (r == -1) break
                        output.write(buf, 0, r)
                        readTotal += r
                        if (total > 0) {
                            val pct = ((readTotal * 100) / total).toInt().coerceIn(0, 100)
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.flush()
                }
            }
        }
        destFile.exists() && destFile.length() > 0L
    }

    suspend fun provisionServer(
        server: MinecraftServer,
        downloadUrl: String?,
        onProgress: (String, Int) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val serverDir = serverRepository.getServerDirectory(server.id)
        val url = downloadUrl ?: resolveLatestBuild(server.type, server.version)?.second
            ?: throw IllegalStateException("Unable to resolve download URL for ${server.type.displayName} ${server.version}")

        if (server.type == ServerType.BEDROCK_BDS) {
            if (DesktopPlatform.isMacOs) {
                throw IllegalStateException("Mojang does not publish a native macOS Bedrock Dedicated Server binary. Use Paper + GeyserMC (Bedrock + Java Crossplay) on macOS instead.")
            }
            val zipFile = File(serverDir, "bedrock-server.zip")
            onProgress("Downloading Bedrock Dedicated Server...", 25)
            downloadFile(url, zipFile) { pct ->
                onProgress("Downloading Bedrock Dedicated Server ($pct%)...", 25 + (pct * 50 / 100))
            }
            onProgress("Extracting Bedrock Dedicated Server...", 80)
            FileStorageUtils.extractZipSafe(zipFile, serverDir, deleteZipAfter = true).getOrThrow()
            if (DesktopPlatform.isLinux) {
                File(serverDir, "bedrock_server").setExecutable(true, false)
            }
            onProgress("Bedrock Dedicated Server ready!", 100)
            return@withContext
        }

        val targetFile = File(serverDir, server.jarFileName)
        onProgress("Downloading ${server.type.displayName} ${server.version}...", 25)
        downloadFile(url, targetFile) { pct ->
            onProgress("Downloading ${server.type.displayName} ($pct%)...", 25 + (pct * 65 / 100))
        }

        if (server.type == ServerType.BEDROCK_GEYSER) {
            onProgress("Bundling GeyserMC for Bedrock cross-play...", 92)
            runCatching {
                val geyserUrl = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
                val pluginsDir = File(serverDir, "plugins").apply { mkdirs() }
                val geyserDest = File(pluginsDir, "Geyser-Spigot.jar")
                downloadFile(geyserUrl, geyserDest) {}
            }.onFailure { err ->
                CoreLog.w(TAG, "Failed bundling Geyser-Spigot.jar: ${err.message}")
            }
        }

        if (server.type == ServerType.FORGE || server.type == ServerType.NEOFORGE) {
            onProgress("Ensuring Java ${server.javaVersion} is installed...", 90)
            val javaBin = javaManager.installJava(server.javaVersion) { msg ->
                onProgress(msg, 92)
            }.getOrThrow()
            onProgress("Running ${server.type.displayName} server installer...", 95)
            val proc = ProcessBuilder(javaBin.absolutePath, "-jar", targetFile.absolutePath, "--installServer")
                .directory(serverDir)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            if (code != 0) {
                CoreLog.e(TAG, "Installer exited $code: ${out.takeLast(500)}")
                throw IllegalStateException("${server.type.displayName} installer failed (exit $code)")
            }
        }
        onProgress("Server provisioned successfully!", 100)
    }

    suspend fun upgradeServerVersion(
        server: MinecraftServer,
        newVersion: String,
        createBackup: Boolean,
        processManager: DesktopProcessManager,
        onProgress: (String, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (processManager.isServerRunning(server.id)) {
                onProgress("Stopping server...", 5)
                processManager.stopServer(server.id)
                var waited = 0
                while (!processManager.isServerFullyStopped(server.id) && waited < 20_000) {
                    kotlinx.coroutines.delay(250)
                    waited += 250
                }
            }
            if (createBackup) {
                onProgress("Creating pre-upgrade backup...", 15)
                serverRepository.createBackup(server.id, worldOnly = false)
            }
            onProgress("Resolving ${server.type.displayName} $newVersion...", 25)
            val buildInfo = resolveLatestBuild(server.type, newVersion)
                ?: return@withContext false
            val updated = server.copy(
                version = newVersion,
                serverBuild = buildInfo.first,
                javaVersion = determineJavaVersion(newVersion, server.type)
            )
            provisionServer(updated, buildInfo.second, onProgress)
            serverRepository.updateServer(updated)
            true
        } catch (e: Exception) {
            CoreLog.e(TAG, "Upgrade failed for ${server.id}", e)
            false
        }
    }
}
