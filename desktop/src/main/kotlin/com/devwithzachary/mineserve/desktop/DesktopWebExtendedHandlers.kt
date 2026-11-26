package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.api.ModrinthApiClient
import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.Permission
import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.engine.ChunkOptimizer
import com.devwithzachary.mineserve.model.ChunkPruneOptions
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.DiscordWebhookConfig
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ProxyRoute
import com.devwithzachary.mineserve.model.ProxyServerConfig
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.net.http.WebHttpUtils
import com.devwithzachary.mineserve.net.portforward.PortForwardProtocol
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal class DesktopWebExtendedHandlers(
    private val paths: DesktopDataPaths,
    private val serverRepository: DesktopServerRepository,
    private val processManager: DesktopProcessManager,
    private val softwareManager: DesktopServerSoftwareManager,
    private val networkManager: DesktopNetworkManager,
    private val userRepository: UserRepository,
    private val actualWebPortProvider: () -> Int
) {
    private val modrinthApi = ModrinthApiClient()

    fun handlePerformanceRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        currentUser: PanelUser,
        effectiveIp: String,
        requirePerm: (Permission) -> Boolean
    ) {
        if (sub == "aikar" && method == "POST") {
            if (!requirePerm(Permission.SERVER_SETTINGS_WRITE)) return
            val aikar = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"
            serverRepository.updateServer(server.copy(jvmArgs = aikar))
            userRepository.recordAudit(currentUser, "AIKAR_APPLY", server.name, "Applied Aikar's G1GC flags", effectiveIp)
            sendJson(out, 200, "OK", JSONObject().put("success", true))
        } else {
            val m = processManager.serverMetrics.value[server.id]
            val serverDir = serverRepository.getServerDirectory(server.id)
            val diskBytes = FileStorageUtils.calculateDirectorySize(serverDir)
            val freeBytes = paths.rootDir.usableSpace
            sendJson(
                out, 200, "OK",
                JSONObject()
                    .put("status", processManager.getServerStatus(server.id).name)
                    .put("cpuPercentage", m?.cpuPercentage?.toDouble() ?: 0.0)
                    .put("ramUsedMb", m?.ramUsedMb ?: 0L)
                    .put("ramMaxMb", server.allocatedRamMb)
                    .put("allocatedRamMb", server.allocatedRamMb)
                    .put("tps", m?.tps ?: 20.0)
                    .put("mspt", m?.mspt ?: 0.0)
                    .put("uptimeSeconds", m?.uptimeSeconds ?: 0L)
                    .put("serverStorageBytes", diskBytes)
                    .put("freeDeviceStorageBytes", freeBytes)
                    .put("aikarApplied", server.jvmArgs.contains("G1NewSizePercent"))
            )
        }
    }

    suspend fun handlePluginsRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        queryParams: Map<String, String>,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        val isMod = server.type.supportsMods && !server.type.supportsPlugins
        val folderName = if (isMod) "mods" else "plugins"
        val dir = File(serverRepository.getServerDirectory(server.id), folderName).apply { mkdirs() }

        when {
            sub == null && method == "GET" -> {
                val arr = JSONArray()
                (dir.listFiles() ?: emptyArray())
                    .filter { it.isFile && (it.name.endsWith(".jar", true) || it.name.endsWith(".jar.disabled", true)) }
                    .sortedBy { it.name.lowercase() }
                    .forEach { f ->
                        val enabled = f.name.endsWith(".jar", true)
                        val cleanName = f.name.removeSuffix(".disabled").removeSuffix(".jar")
                        arr.put(
                            JSONObject()
                                .put("id", f.name)
                                .put("fileName", f.name)
                                .put("name", cleanName)
                                .put("displayName", cleanName)
                                .put("enabled", enabled)
                                .put("isMod", isMod)
                                .put("sizeBytes", f.length())
                        )
                    }
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("folder", folderName)
                        .put("supportsPlugins", server.type.supportsPlugins)
                        .put("supportsMods", server.type.supportsMods)
                        .put("items", arr)
                )
            }
            sub == "toggle" && method == "POST" -> {
                val fname = File(parseJson(body).optString("fileName", "")).name
                val src = File(dir, fname)
                if (!src.exists()) {
                    sendJson(out, 404, "Not Found", JSONObject().put("error", "File not found"))
                    return
                }
                val dst = if (fname.endsWith(".disabled")) File(dir, fname.removeSuffix(".disabled")) else File(dir, "$fname.disabled")
                val ok = src.renameTo(dst)
                if (ok) userRepository.recordAudit(currentUser, "PLUGIN_TOGGLE", server.name, dst.name, effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", ok))
            }
            sub == "delete" && method == "POST" -> {
                val fname = File(parseJson(body).optString("fileName", "")).name
                val ok = File(dir, fname).delete()
                if (ok) userRepository.recordAudit(currentUser, "PLUGIN_DELETE", server.name, fname, effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", ok))
            }
            sub == "search" && method == "GET" -> {
                val query = queryParams["q"] ?: ""
                val loaderFilter = server.type.name.lowercase()
                val results = runCatching {
                    modrinthApi.search(query = query, isMod = isMod, loaderFilter = loaderFilter, gameVersion = server.version)
                }.getOrDefault(emptyList())
                val arr = JSONArray()
                results.forEach { p ->
                    arr.put(
                        JSONObject()
                            .put("id", p.id)
                            .put("projectId", p.id)
                            .put("slug", p.slug)
                            .put("name", p.name)
                            .put("title", p.name)
                            .put("description", p.description)
                            .put("downloads", p.downloads)
                            .put("author", p.author)
                            .put("iconUrl", p.iconUrl ?: "")
                    )
                }
                sendJson(out, 200, "OK", JSONObject().put("results", arr))
            }
            sub == "install" && method == "POST" -> {
                val j = parseJson(body)
                val projectId = j.optString("projectId", "").ifBlank { j.optString("id", "") }.trim()
                val loaderFilter = server.type.name.lowercase()
                val fileInfo = runCatching {
                    modrinthApi.resolveDownloadUrl(projectId, isMod, loaderFilter, server.version)
                }.getOrNull()
                if (fileInfo == null) {
                    sendJson(out, 404, "Not Found", JSONObject().put("error", "No compatible version found on Modrinth."))
                    return
                }
                val (fileName, downloadUrl) = fileInfo
                val dest = File(dir, fileName)
                val ok = softwareManager.downloadFile(downloadUrl, dest)
                if (ok) userRepository.recordAudit(currentUser, "PLUGIN_INSTALL", server.name, fileName, effectiveIp)
                sendJson(out, if (ok) 200 else 500, if (ok) "OK" else "Internal Server Error", JSONObject().put("success", ok).put("fileName", fileName))
            }
            else -> sendJson(out, 404, "Not Found", JSONObject().put("error", "Unknown plugins route"))
        }
    }

    fun handlePluginUpload(
        out: OutputStream,
        serverId: String,
        queryParams: Map<String, String>,
        tmpFile: File,
        user: PanelUser,
        effectiveIp: String
    ) {
        val srv = serverRepository.getServer(serverId)
        if (srv == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Server not found"))
            return
        }
        val safeName = File(queryParams["name"] ?: "").name.trim()
        if (safeName.isBlank() || !safeName.endsWith(".jar", ignoreCase = true)) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Only .jar files are supported."))
            return
        }
        val folder = if (srv.type.supportsMods && !srv.type.supportsPlugins) "mods" else "plugins"
        val dest = File(File(serverRepository.getServerDirectory(srv.id), folder).apply { mkdirs() }, safeName)
        tmpFile.copyTo(dest, overwrite = true)
        userRepository.recordAudit(user, "PLUGIN_UPLOAD", srv.name, safeName, effectiveIp)
        sendJson(out, 200, "OK", JSONObject().put("success", true).put("fileName", safeName).put("folder", folder))
    }

    suspend fun handleWorldRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        subAction: String?,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        val serverDir = serverRepository.getServerDirectory(server.id)
        val levelName = serverRepository.loadServerProperties(server.id).levelName.ifBlank { "world" }

        fun resolveRegionDir(dim: DimensionType): File {
            val dDir = dim.getDimensionDir(serverDir, levelName)
            return when (dim) {
                DimensionType.OVERWORLD -> File(dDir, "region")
                DimensionType.NETHER -> File(dDir, "DIM-1/region").takeIf { it.exists() } ?: File(dDir, "region")
                DimensionType.THE_END -> File(dDir, "DIM1/region").takeIf { it.exists() } ?: File(dDir, "region")
            }
        }

        when {
            sub == null && method == "GET" -> {
                val dimsArr = JSONArray()
                var totalBytes = 0L
                var totalChunks = 0
                for (dim in DimensionType.entries) {
                    val dDir = dim.getDimensionDir(serverDir, levelName)
                    val rDir = resolveRegionDir(dim)
                    val exists = dDir.exists() && dDir.isDirectory
                    val size = if (exists) FileStorageUtils.calculateDirectorySize(dDir) else 0L
                    val mcaFiles = if (rDir.exists()) (rDir.listFiles()?.filter { it.name.endsWith(".mca") } ?: emptyList()) else emptyList()
                    val estChunks = mcaFiles.sumOf { ((it.length() / 4096L).toInt()).coerceIn(0, 1024) }
                    totalBytes += size
                    totalChunks += estChunks
                    dimsArr.put(
                        JSONObject()
                            .put("type", dim.name)
                            .put("displayName", dim.displayName)
                            .put("exists", exists)
                            .put("sizeBytes", size)
                            .put("regionFileCount", mcaFiles.size)
                            .put("estimatedChunks", estChunks)
                    )
                }
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("levelName", levelName)
                        .put("totalSizeBytes", totalBytes)
                        .put("totalEstimatedChunks", totalChunks)
                        .put("dimensions", dimsArr)
                )
            }
            sub == "reset" && method == "POST" -> {
                if (processManager.isServerRunning(server.id)) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before resetting a dimension."))
                    return
                }
                val j = parseJson(body)
                val dim = runCatching { DimensionType.valueOf(j.optString("dimension", "")) }.getOrNull()
                if (dim == null) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Invalid dimension"))
                    return
                }
                if (j.optBoolean("createBackup", true)) {
                    serverRepository.createBackup(server.id, worldOnly = true)
                }
                val dir = dim.getDimensionDir(serverDir, levelName)
                val ok = if (dir.exists()) dir.deleteRecursively() else true
                userRepository.recordAudit(currentUser, "WORLD_RESET", server.name, dim.displayName, effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", ok))
            }
            sub == "optimize" && method == "POST" -> {
                if (processManager.isServerRunning(server.id)) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before pruning chunks."))
                    return
                }
                val j = parseJson(body)
                val createBackup = j.optBoolean("createBackup", true)
                if (createBackup) serverRepository.createBackup(server.id, worldOnly = true)
                val thresholdName = j.optString("threshold", "UNTOUCHED")
                val threshold = InhabitedTimeThreshold.entries.firstOrNull { it.name == thresholdName } ?: InhabitedTimeThreshold.UNTOUCHED
                val opts = ChunkPruneOptions(
                    threshold = threshold,
                    pruneNether = j.optBoolean("pruneNether", true),
                    pruneEnd = j.optBoolean("pruneEnd", true),
                    createBackup = createBackup
                )
                val res = ChunkOptimizer.optimizeWorld(serverDir, opts, levelName)
                userRepository.recordAudit(currentUser, "WORLD_OPTIMIZE", server.name, "Pruned ${res.prunedChunks} chunks", effectiveIp)
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("success", true)
                        .put("regionsScanned", res.scannedRegions)
                        .put("scannedChunks", res.scannedChunks)
                        .put("chunksPruned", res.prunedChunks)
                        .put("deletedRegions", res.deletedRegions)
                        .put("bytesBefore", res.bytesBefore)
                        .put("bytesAfter", res.bytesAfter)
                        .put("bytesSaved", res.bytesFreed)
                        .put("percentFreed", res.percentFreed)
                )
            }
            sub == "export" && method == "GET" -> {
                if (processManager.isServerRunning(server.id)) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before exporting the world."))
                    return
                }
                val worldDir = File(serverDir, levelName)
                if (!worldDir.exists()) {
                    sendJson(out, 404, "Not Found", JSONObject().put("error", "World directory not found."))
                    return
                }
                val tmpZip = File(paths.tmpDir, "world_export_${server.id}_${System.currentTimeMillis()}.zip")
                try {
                    ZipOutputStream(BufferedOutputStream(FileOutputStream(tmpZip))).use { zos ->
                        FileStorageUtils.zipDirectoryToStream(worldDir, zos)
                    }
                    WebHttpUtils.sendFileStreamResponse(out, "application/zip", tmpZip, mapOf("Content-Disposition" to "attachment; filename=\"${server.name.replace(" ", "_")}_world.zip\""))
                } finally {
                    tmpZip.delete()
                }
            }
            sub == "regions" -> {
                if (subAction == "delete" && method == "POST") {
                    if (processManager.isServerRunning(server.id)) {
                        sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before deleting a region file."))
                        return
                    }
                    val j = parseJson(body)
                    val dim = runCatching { DimensionType.valueOf(j.optString("dimension", "OVERWORLD")) }.getOrDefault(DimensionType.OVERWORLD)
                    val fname = File(j.optString("fileName", "")).name
                    val f = File(resolveRegionDir(dim), fname)
                    val ok = f.exists() && f.name.endsWith(".mca") && f.delete()
                    sendJson(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", JSONObject().put("success", ok))
                } else {
                    val arr = JSONArray()
                    val allRegions = mutableListOf<JSONObject>()
                    for (dim in DimensionType.entries) {
                        val rDir = resolveRegionDir(dim)
                        if (!rDir.exists()) continue
                        rDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mca") }?.forEach { f ->
                            val parts = f.name.split(".")
                            val rx = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            val rz = parts.getOrNull(2)?.toIntOrNull() ?: 0
                            val chunks = ((f.length() / 4096L).toInt()).coerceIn(1, 1024)
                            allRegions.add(
                                JSONObject()
                                    .put("dimension", dim.name)
                                    .put("dimensionName", dim.displayName)
                                    .put("fileName", f.name)
                                    .put("regionX", rx)
                                    .put("regionZ", rz)
                                    .put("sizeBytes", f.length())
                                    .put("chunkCount", chunks)
                            )
                        }
                    }
                    allRegions.sortedByDescending { it.optLong("sizeBytes", 0L) }.take(50).forEach { arr.put(it) }
                    sendJson(out, 200, "OK", JSONObject().put("regions", arr).put("totalCount", allRegions.size))
                }
            }
            sub == "pregen" -> {
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("isRunning", false)
                        .put("dimension", "OVERWORLD")
                        .put("radiusBlocks", 500)
                        .put("completedBatches", 0)
                        .put("totalBatches", 0)
                        .put("percent", 0)
                        .put("statusMessage", "Idle")
                )
            }
            else -> sendJson(out, 200, "OK", JSONObject().put("success", true))
        }
    }

    suspend fun handleWorldImport(
        out: OutputStream,
        serverId: String,
        queryParams: Map<String, String>,
        tmpFile: File,
        user: PanelUser,
        effectiveIp: String
    ) {
        val srv = serverRepository.getServer(serverId)
        if (srv == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Server not found"))
            return
        }
        if (processManager.isServerRunning(srv.id)) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before importing a world."))
            return
        }
        if (queryParams["createBackup"]?.toBooleanStrictOrNull() ?: true) {
            serverRepository.createBackup(srv.id, worldOnly = true)
        }
        val levelName = serverRepository.loadServerProperties(srv.id).levelName.ifBlank { "world" }
        val worldDir = File(serverRepository.getServerDirectory(srv.id), levelName).apply { mkdirs() }
        val res = FileStorageUtils.extractZipSafe(tmpFile, worldDir, deleteZipAfter = false)
        if (res.isFailure) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", res.exceptionOrNull()?.message ?: "World import failed"))
        } else {
            userRepository.recordAudit(user, "WORLD_IMPORT", srv.name, "Imported world archive", effectiveIp)
            sendJson(out, 200, "OK", JSONObject().put("success", true))
        }
    }

    suspend fun handleLiveMapRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String,
        requirePerm: (Permission) -> Boolean
    ) {
        val perm = if (method == "GET") Permission.SERVER_VIEW else Permission.SERVER_SETTINGS_WRITE
        if (!requirePerm(perm)) return
        val serverDir = serverRepository.getServerDirectory(server.id)
        val isModServer = server.type.supportsMods && !server.type.supportsPlugins
        val actualPort = actualWebPortProvider()

        when {
            sub == null && method == "GET" -> {
                val installed = WebMapPluginType.detectInstalled(serverDir)
                val mapPort = WebMapPluginType.detectSquaremapPort(serverDir)
                val localIp = DesktopHostTelemetry.getLocalIpAddress() ?: "127.0.0.1"
                val suggested = if (actualPort == WebMapPluginType.SQUAREMAP.defaultPort) {
                    WebMapPluginType.SQUAREMAP.defaultPort + 1
                } else {
                    WebMapPluginType.SQUAREMAP.defaultPort
                }
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("installed", installed != null)
                        .put("pluginName", installed?.displayName ?: "")
                        .put("port", mapPort)
                        .put("webAdminPort", actualPort)
                        .put("portConflict", mapPort == actualPort)
                        .put("suggestedPort", suggested)
                        .put("isServerRunning", processManager.isServerRunning(server.id))
                        .put("supportsPluginsOrMods", server.type.supportsPlugins || server.type.supportsMods)
                        .put("localMapUrl", "http://$localIp:$mapPort")
                )
            }
            sub == "install" && method == "POST" -> {
                val loader = server.type.name.lowercase()
                val resolved = modrinthApi.resolveDownloadUrl(WebMapPluginType.SQUAREMAP.modrinthSlug, isModServer, loader, server.version)
                    ?: modrinthApi.resolveDownloadUrl(WebMapPluginType.SQUAREMAP.modrinthSlug, isModServer, null, null)
                if (resolved == null) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Could not resolve Squaremap download from Modrinth."))
                    return
                }
                val (fileName, downloadUrl) = resolved
                val folder = File(serverDir, if (isModServer) "mods" else "plugins").apply { mkdirs() }
                val dest = File(folder, fileName)
                val ok = softwareManager.downloadFile(downloadUrl, dest)
                if (ok) {
                    val safePort = if (actualPort == WebMapPluginType.SQUAREMAP.defaultPort) WebMapPluginType.SQUAREMAP.defaultPort + 1 else WebMapPluginType.SQUAREMAP.defaultPort
                    WebMapPluginType.setSquaremapPort(serverDir, isModServer, safePort)
                    userRepository.recordAudit(currentUser, "LIVEMAP_INSTALL", server.name, "$fileName (port $safePort)", effectiveIp)
                }
                sendJson(out, if (ok) 200 else 500, if (ok) "OK" else "Internal Server Error", JSONObject().put("success", ok).put("fileName", fileName))
            }
            sub == "uninstall" && method == "POST" -> {
                val installedFile = WebMapPluginType.findInstalledFile(serverDir)
                val deleted = installedFile?.delete() ?: false
                if (deleted) userRepository.recordAudit(currentUser, "LIVEMAP_UNINSTALL", server.name, installedFile.name, effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", deleted))
            }
            sub == "port" && method == "POST" -> {
                val newPort = parseJson(body).optInt("port", WebMapPluginType.SQUAREMAP.defaultPort)
                if (newPort !in 1024..65535) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Port must be between 1024 and 65535."))
                    return
                }
                WebMapPluginType.setSquaremapPort(serverDir, isModServer, newPort)
                userRepository.recordAudit(currentUser, "LIVEMAP_PORT", server.name, "Port $newPort", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true).put("port", newPort))
            }
            else -> sendJson(out, 404, "Not Found", JSONObject().put("error", "Unknown livemap route"))
        }
    }

    fun handleNetworkRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        when {
            sub == null && method == "GET" -> {
                val tCfg = server.tunnelConfig
                val pfCfg = server.portForwardConfig
                val tState = networkManager.getTunnelState(server.id)
                val pfState = networkManager.getPortForwardState(server.id)

                val tStateObj = when (tState) {
                    is DesktopTunnelState.Disconnected -> JSONObject().put("status", "disconnected")
                    is DesktopTunnelState.Connecting -> JSONObject()
                        .put("status", "connecting")
                        .put("message", tState.message)
                        .put("claimUrl", tState.claimUrl ?: "")
                    is DesktopTunnelState.Connected -> JSONObject()
                        .put("status", "connected")
                        .put("fullAddress", tState.fullAddress)
                        .put("publicHost", tState.publicHost)
                        .put("publicPort", tState.publicPort)
                    is DesktopTunnelState.Error -> JSONObject()
                        .put("status", "error")
                        .put("errorMessage", tState.errorMessage)
                }

                val pfStateObj = when (pfState) {
                    is DesktopPortForwardState.Idle -> JSONObject().put("status", "idle")
                    is DesktopPortForwardState.Discovering -> JSONObject().put("status", "discovering").put("message", pfState.message)
                    is DesktopPortForwardState.Mapped -> JSONObject()
                        .put("status", "mapped")
                        .put("directAddress", pfState.directAddress)
                        .put("externalIp", pfState.externalIp)
                        .put("externalPort", pfState.externalPort)
                        .put("gatewayName", pfState.gatewayName)
                    is DesktopPortForwardState.Failed -> JSONObject().put("status", "failed").put("reason", pfState.reason)
                }

                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("localIp", DesktopHostTelemetry.getLocalIpAddress() ?: "127.0.0.1")
                        .put("tunnelConfig", JSONObject()
                            .put("provider", tCfg.provider.name)
                            .put("autoStart", tCfg.autoStart)
                            .put("customRelayHost", tCfg.customRelayHost)
                            .put("customRelayPort", tCfg.customRelayPort)
                            .put("playitSecretSet", tCfg.playitSecret.isNotBlank())
                            .put("cloudSubdomain", tCfg.cloudSubdomain)
                            .put("cloudRegion", tCfg.cloudRegion)
                            .put("cloudDomain", tCfg.cloudDomain))
                        .put("tunnelState", tStateObj)
                        .put("portForwardConfig", JSONObject()
                            .put("autoForward", pfCfg.autoForward)
                            .put("protocol", pfCfg.protocol.name)
                            .put("leaseDurationSeconds", pfCfg.leaseDurationSeconds))
                        .put("portForwardState", pfStateObj)
                )
            }
            sub == "tunnel" && method == "POST" -> {
                val j = parseJson(body)
                val act = j.optString("action", "save").lowercase()
                val provider = runCatching { TunnelProvider.valueOf(j.optString("provider", server.tunnelConfig.provider.name)) }
                    .getOrDefault(server.tunnelConfig.provider)
                val updatedCfg = server.tunnelConfig.copy(
                    provider = provider,
                    autoStart = j.optBoolean("autoStart", server.tunnelConfig.autoStart),
                    customRelayHost = j.optString("customRelayHost", server.tunnelConfig.customRelayHost).trim().ifBlank { "bore.pub" },
                    customRelayPort = j.optInt("customRelayPort", server.tunnelConfig.customRelayPort).coerceIn(1, 65535),
                    playitSecret = j.optString("playitSecret", "").takeIf { it.isNotBlank() } ?: server.tunnelConfig.playitSecret,
                    cloudSubdomain = j.optString("cloudSubdomain", server.tunnelConfig.cloudSubdomain).trim().lowercase(),
                    cloudRegion = j.optString("cloudRegion", server.tunnelConfig.cloudRegion).trim().ifBlank { "us-east" },
                    cloudDomain = j.optString("cloudDomain", server.tunnelConfig.cloudDomain).trim().ifBlank { "play.mineserve.app" }
                )
                val updated = server.copy(tunnelConfig = updatedCfg)
                serverRepository.updateServer(updated)
                when (act) {
                    "start" -> networkManager.startTunnel(updated)
                    "stop" -> networkManager.stopTunnel(server.id)
                }
                userRepository.recordAudit(currentUser, "TUNNEL_${act.uppercase()}", server.name, provider.name, effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            sub == "portforward" && method == "POST" -> {
                val j = parseJson(body)
                val act = j.optString("action", "save").lowercase()
                val protocol = runCatching { PortForwardProtocol.valueOf(j.optString("protocol", server.portForwardConfig.protocol.name)) }
                    .getOrDefault(server.portForwardConfig.protocol)
                val updatedPf = server.portForwardConfig.copy(
                    autoForward = j.optBoolean("autoForward", server.portForwardConfig.autoForward),
                    protocol = protocol,
                    leaseDurationSeconds = j.optInt("leaseDurationSeconds", server.portForwardConfig.leaseDurationSeconds).coerceAtLeast(0)
                )
                val updated = server.copy(portForwardConfig = updatedPf)
                serverRepository.updateServer(updated)
                when (act) {
                    "start" -> networkManager.startPortForward(updated)
                    "stop" -> networkManager.releasePortForward(server.id)
                }
                userRepository.recordAudit(currentUser, "PORTFORWARD_${act.uppercase()}", server.name, protocol.name, effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            else -> sendJson(out, 404, "Not Found", JSONObject().put("error", "Unknown network route"))
        }
    }

    fun handleAutomationRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        subAction: String? = null,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        val cfg = server.automationConfig
        when {
            sub == null && method == "GET" -> {
                val arr = JSONArray()
                cfg.scheduledTasks.forEach { t ->
                    arr.put(
                        JSONObject()
                            .put("id", t.id)
                            .put("name", t.name)
                            .put("enabled", t.enabled)
                            .put("taskType", t.taskType.name)
                            .put("taskTypeDisplay", t.taskType.displayName)
                            .put("frequency", t.frequency.name)
                            .put("frequencyDescription", t.frequencyDescription)
                            .put("intervalMinutes", t.intervalMinutes)
                            .put("dailyHour", t.dailyHour)
                            .put("dailyMinute", t.dailyMinute)
                            .put("cronExpression", t.cronExpression)
                            .put("command", t.command)
                            .put("backupRetentionCount", t.backupRetentionCount)
                            .put("lastRunTimestamp", t.lastRunTimestamp)
                    )
                }
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("idleSleepEnabled", cfg.idleSleepEnabled)
                        .put("idleSleepTimeoutMinutes", cfg.idleSleepTimeoutMinutes)
                        .put("autoWakeOnPing", cfg.autoWakeOnPing)
                        .put("scheduledTasks", arr)
                )
            }
            sub == null && method == "POST" -> {
                val j = parseJson(body)
                val updated = cfg.copy(
                    idleSleepEnabled = j.optBoolean("idleSleepEnabled", cfg.idleSleepEnabled),
                    idleSleepTimeoutMinutes = j.optInt("idleSleepTimeoutMinutes", cfg.idleSleepTimeoutMinutes).coerceIn(1, 240),
                    autoWakeOnPing = j.optBoolean("autoWakeOnPing", cfg.autoWakeOnPing)
                )
                serverRepository.updateServer(server.copy(automationConfig = updated))
                userRepository.recordAudit(currentUser, "AUTOMATION_SAVE", server.name, "Updated automation settings", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            sub == "tasks" && method == "POST" -> {
                val j = parseJson(body)
                val act = (subAction?.takeIf { it.isNotBlank() } ?: j.optString("action", "add")).lowercase()
                val tasks = cfg.scheduledTasks.toMutableList()
                when (act) {
                    "add" -> {
                        val tType = runCatching { ScheduledTaskType.valueOf(j.optString("taskType", "WORLD_BACKUP")) }.getOrDefault(ScheduledTaskType.WORLD_BACKUP)
                        val freq = runCatching { ScheduleFrequency.valueOf(j.optString("frequency", "INTERVAL")) }.getOrDefault(ScheduleFrequency.INTERVAL)
                        tasks.add(
                            ScheduledTask(
                                id = UUID.randomUUID().toString().take(8),
                                serverId = server.id,
                                name = j.optString("name", tType.displayName).trim().ifBlank { tType.displayName },
                                enabled = true,
                                taskType = tType,
                                frequency = freq,
                                intervalMinutes = j.optInt("intervalMinutes", 360).coerceAtLeast(1),
                                dailyHour = j.optInt("dailyHour", 4).coerceIn(0, 23),
                                dailyMinute = j.optInt("dailyMinute", 0).coerceIn(0, 59),
                                cronExpression = j.optString("cronExpression", ""),
                                command = j.optString("command", ""),
                                backupRetentionCount = j.optInt("backupRetentionCount", 5).coerceIn(1, 50)
                            )
                        )
                    }
                    "toggle" -> {
                        val tid = j.optString("taskId", "")
                        val idx = tasks.indexOfFirst { it.id == tid }
                        if (idx >= 0) tasks[idx] = tasks[idx].copy(enabled = !tasks[idx].enabled)
                    }
                    "delete" -> {
                        val tid = j.optString("taskId", "")
                        tasks.removeAll { it.id == tid }
                    }
                }
                serverRepository.updateServer(server.copy(automationConfig = cfg.copy(scheduledTasks = tasks)))
                userRepository.recordAudit(currentUser, "AUTOMATION_TASK_${act.uppercase()}", server.name, act, effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            else -> sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
        }
    }

    fun handleDiscordRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        val cfg = server.discordWebhookConfig
        when {
            sub == null && method == "GET" -> {
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("enabled", cfg.enabled)
                        .put("webhookUrl", cfg.webhookUrl)
                        .put("notifyServerStart", cfg.notifyServerStart)
                        .put("notifyServerStop", cfg.notifyServerStop)
                        .put("notifyServerCrash", cfg.notifyServerCrash)
                        .put("notifyPlayerJoin", cfg.notifyPlayerJoin)
                        .put("notifyPlayerLeave", cfg.notifyPlayerLeave)
                        .put("notifyBackupComplete", cfg.notifyBackupComplete)
                        .put("includePlayerSkins", cfg.includePlayerSkins)
                )
            }
            sub == null && method == "POST" -> {
                val j = parseJson(body)
                val updated = DiscordWebhookConfig(
                    enabled = j.optBoolean("enabled", cfg.enabled),
                    webhookUrl = j.optString("webhookUrl", cfg.webhookUrl).trim(),
                    notifyServerStart = j.optBoolean("notifyServerStart", cfg.notifyServerStart),
                    notifyServerStop = j.optBoolean("notifyServerStop", cfg.notifyServerStop),
                    notifyServerCrash = j.optBoolean("notifyServerCrash", cfg.notifyServerCrash),
                    notifyPlayerJoin = j.optBoolean("notifyPlayerJoin", cfg.notifyPlayerJoin),
                    notifyPlayerLeave = j.optBoolean("notifyPlayerLeave", cfg.notifyPlayerLeave),
                    notifyBackupComplete = j.optBoolean("notifyBackupComplete", cfg.notifyBackupComplete),
                    includePlayerSkins = j.optBoolean("includePlayerSkins", cfg.includePlayerSkins)
                )
                serverRepository.updateServer(server.copy(discordWebhookConfig = updated))
                userRepository.recordAudit(currentUser, "DISCORD_SAVE", server.name, "Updated Discord webhook", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            sub == "test" && method == "POST" -> {
                val url = parseJson(body).optString("webhookUrl", cfg.webhookUrl).trim()
                if (!url.startsWith("https://discord.com/api/webhooks/") && !url.startsWith("https://discordapp.com/api/webhooks/")) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Enter a valid Discord Webhook URL."))
                    return
                }
                val payload = JSONObject()
                    .put("username", "MineServe")
                    .put("content", "MineServe ${DesktopPlatform.shortName} test notification for **${server.name}**.")
                    .toString()
                val ok = runCatching {
                    val req = MineServeHttpClient.newRequestBuilder(url)
                        .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    MineServeHttpClient.client.newCall(req).execute().use { it.isSuccessful }
                }.getOrDefault(false)
                if (ok) {
                    sendJson(out, 200, "OK", JSONObject().put("success", true))
                } else {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Discord webhook test failed."))
                }
            }
            else -> sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
        }
    }

    private fun sanitizeProxyAlias(raw: String): String {
        val cleaned = raw.lowercase().replace(Regex("[^a-z0-9_-]"), "-").trim('-')
        return cleaned.ifBlank { "server" }
    }

    private fun applyLinuxProxyConfig(
        proxyServer: MinecraftServer,
        proxyDir: File,
        allServers: List<MinecraftServer>
    ): ProxyServerConfig {
        proxyDir.mkdirs()
        val cfg = proxyServer.proxyConfig
        val secret = cfg.forwardingSecret.ifBlank { ProxyServerConfig.generateSecret() }
        val resolvedRoutes = cfg.routes.map { r ->
            val target = allServers.firstOrNull { it.id == r.targetServerId }
            r.copy(
                alias = sanitizeProxyAlias(r.alias),
                port = target?.port ?: r.port
            )
        }
        val tryList = if (cfg.tryOrder.isNotEmpty()) {
            cfg.tryOrder.filter { alias -> resolvedRoutes.any { it.alias == alias } }
        } else {
            resolvedRoutes.map { it.alias }
        }.ifEmpty { resolvedRoutes.map { it.alias } }

        when (proxyServer.type) {
            ServerType.VELOCITY -> {
                File(proxyDir, "forwarding.secret").writeText(secret)
                val serversBlock = if (resolvedRoutes.isEmpty()) {
                    "lobby = \"127.0.0.1:25566\""
                } else {
                    resolvedRoutes.joinToString("\n") { "${it.alias} = \"${it.host}:${it.port}\"" }
                }
                val tryArray = if (tryList.isEmpty()) {
                    "[\"lobby\"]"
                } else {
                    tryList.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }
                }
                val escapedMotd = proxyServer.motd.replace("\"", "\\\"")
                val toml = """
                    config-version = "2.7"
                    bind = "0.0.0.0:${proxyServer.port}"
                    motd = "<green>$escapedMotd</green>"
                    show-max-players = 100
                    online-mode = ${cfg.onlineMode}
                    force-key-authentication = true
                    prevent-client-proxy-connections = false
                    player-info-forwarding-mode = "modern"
                    forwarding-secret-file = "forwarding.secret"
                    announce-forge = false

                    [servers]
                    $serversBlock
                    try = $tryArray

                    [forced-hosts]

                    [advanced]
                    compression-threshold = 256
                    compression-level = -1
                    login-ratelimit = 3000
                    connection-timeout = 5000
                    read-timeout = 30000
                    haproxy-protocol = false
                    tcp-fast-open = false
                    bungee-plugin-message-channel = true
                    show-ping-requests = false
                    failover-on-unexpected-server-disconnect = true
                    announce-proxy-commands = true
                    log-command-executions = false
                    log-player-connections = true
                """.trimIndent() + "\n"
                File(proxyDir, "velocity.toml").writeText(toml)
            }
            ServerType.BUNGEECORD -> {
                val serversYaml = if (resolvedRoutes.isEmpty()) {
                    """
                    |  lobby:
                    |    motd: '${proxyServer.motd.replace("'", "''")}'
                    |    address: 127.0.0.1:25566
                    |    restricted: false
                    """.trimMargin()
                } else {
                    resolvedRoutes.joinToString("\n") { r ->
                        """
                        |  ${r.alias}:
                        |    motd: '${proxyServer.motd.replace("'", "''")}'
                        |    address: ${r.host}:${r.port}
                        |    restricted: false
                        """.trimMargin()
                    }
                }
                val prioritiesYaml = if (tryList.isEmpty()) {
                    "  - lobby"
                } else {
                    tryList.joinToString("\n") { "  - $it" }
                }
                val yml = """
                    forge_support: true
                    connection_throttle_limit: 3
                    timeout: 30000
                    online_mode: ${cfg.onlineMode}
                    log_commands: false
                    network_compression_threshold: 256
                    listeners:
                    - query_port: ${proxyServer.port}
                      motd: '${proxyServer.motd.replace("'", "''")}'
                      tab_list: GLOBAL_PING
                      query_enabled: false
                      proxy_protocol: false
                      forced_hosts: {}
                      ping_passthrough: false
                      priorities:
                    $prioritiesYaml
                      bind_local_address: true
                      host: 0.0.0.0:${proxyServer.port}
                      max_players: 100
                      tab_size: 60
                      force_default_server: true
                    connection_throttle: 4000
                    groups: {}
                    log_pings: true
                    ip_forward: true
                    prevent_proxy_connections: false
                    servers:
                    $serversYaml
                    stats: ${UUID.randomUUID()}
                """.trimIndent() + "\n"
                File(proxyDir, "config.yml").writeText(yml)
            }
            else -> Unit
        }

        return cfg.copy(
            routes = resolvedRoutes,
            tryOrder = tryList,
            forwardingSecret = secret
        )
    }

    fun handleProxyRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String,
        accessibleServerIds: Set<String>? = null
    ) {
        if (method == "GET") {
            if (server.type.isProxy) {
                val candidates = JSONArray()
                serverRepository.servers.value
                    .filter { it.id != server.id && !it.type.isProxy && (accessibleServerIds == null || accessibleServerIds.contains(it.id)) }
                    .forEach { b ->
                        val existing = server.proxyConfig.routes.firstOrNull { it.targetServerId == b.id }
                        candidates.put(
                            JSONObject()
                                .put("id", b.id)
                                .put("name", b.name)
                                .put("type", b.type.displayName)
                                .put("port", b.port)
                                .put("linked", existing != null)
                                .put("alias", existing?.alias ?: sanitizeProxyAlias(b.name))
                        )
                    }
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("isProxy", true)
                        .put("proxyType", server.type.displayName)
                        .put("onlineMode", server.proxyConfig.onlineMode)
                        .put("candidates", candidates)
                )
            } else {
                val props = serverRepository.loadServerProperties(server.id)
                sendJson(out, 200, "OK", JSONObject().put("isProxy", false).put("loopbackOnly", props.serverIp.trim() == "127.0.0.1"))
            }
        } else if (method == "POST") {
            val j = parseJson(body)
            if (server.type.isProxy) {
                val allServers = serverRepository.servers.value
                val targetIds = j.optJSONArray("linkedServerIds") ?: JSONArray()
                val routes = mutableListOf<ProxyRoute>()
                for (i in 0 until targetIds.length()) {
                    val tid = targetIds.optString(i, "")
                    if (accessibleServerIds != null && !accessibleServerIds.contains(tid)) {
                        sendJson(out, 403, "Forbidden", JSONObject().put("error", "Access denied to backend server $tid"))
                        return
                    }
                    val backend = allServers.firstOrNull { it.id == tid && !it.type.isProxy } ?: continue
                    routes.add(
                        ProxyRoute(
                            alias = sanitizeProxyAlias(backend.name),
                            targetServerId = backend.id,
                            host = "127.0.0.1",
                            port = backend.port
                        )
                    )
                }
                val onlineMode = j.optBoolean("onlineMode", server.proxyConfig.onlineMode)
                val draft = server.copy(
                    proxyConfig = server.proxyConfig.copy(
                        routes = routes,
                        tryOrder = routes.map { it.alias },
                        onlineMode = onlineMode
                    )
                )
                val appliedCfg = applyLinuxProxyConfig(
                    proxyServer = draft,
                    proxyDir = serverRepository.getServerDirectory(server.id),
                    allServers = allServers
                )
                serverRepository.updateServer(draft.copy(proxyConfig = appliedCfg))

                if (j.optBoolean("bindBackendsLoopback", false)) {
                    routes.forEach { r ->
                        val bProps = serverRepository.loadServerProperties(r.targetServerId)
                        serverRepository.saveServerProperties(
                            r.targetServerId,
                            bProps.copy(serverIp = "127.0.0.1", onlineMode = false)
                        )
                    }
                }
                userRepository.recordAudit(currentUser, "PROXY_CONFIG", server.name, "Linked ${routes.size} backend(s)", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true).put("linkedCount", routes.size))
            } else {
                val loopback = j.optBoolean("loopbackOnly", false)
                val props = serverRepository.loadServerProperties(server.id)
                serverRepository.saveServerProperties(
                    server.id,
                    props.copy(
                        serverIp = if (loopback) "127.0.0.1" else "",
                        onlineMode = if (loopback) false else props.onlineMode
                    )
                )
                userRepository.recordAudit(currentUser, "PROXY_LOOPBACK", server.name, "loopbackOnly=$loopback", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true).put("loopbackOnly", loopback))
            }
        } else {
            sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
        }
    }

    fun buildNetworkSummary(server: MinecraftServer): JSONObject {
        val localIp = DesktopHostTelemetry.getLocalIpAddress() ?: "127.0.0.1"
        val tState = networkManager.getTunnelState(server.id)
        val pfState = networkManager.getPortForwardState(server.id)
        val providerLabel = when (server.tunnelConfig.provider) {
            TunnelProvider.MINESERVE_CLOUD -> "MineServe Cloud"
            TunnelProvider.BORE -> "Bore"
            TunnelProvider.CUSTOM_BORE -> "Custom Bore"
            TunnelProvider.PLAYIT -> "Playit.gg"
        }
        val tunnelStatus = when (tState) {
            is DesktopTunnelState.Connected -> "connected"
            is DesktopTunnelState.Connecting -> "connecting"
            is DesktopTunnelState.Error -> "error"
            is DesktopTunnelState.Disconnected -> "disconnected"
        }
        val tunnelAddress = (tState as? DesktopTunnelState.Connected)?.fullAddress.orEmpty()
        val tunnelMessage = when (tState) {
            is DesktopTunnelState.Connecting -> tState.claimUrl ?: tState.message
            is DesktopTunnelState.Error -> tState.errorMessage
            else -> ""
        }
        val pfStatus = when (pfState) {
            is DesktopPortForwardState.Mapped -> "mapped"
            is DesktopPortForwardState.Discovering -> "discovering"
            is DesktopPortForwardState.Failed -> "failed"
            is DesktopPortForwardState.Idle -> "idle"
        }
        val directAddress = (pfState as? DesktopPortForwardState.Mapped)?.directAddress.orEmpty()
        val pfMessage = when (pfState) {
            is DesktopPortForwardState.Discovering -> pfState.message
            is DesktopPortForwardState.Failed -> pfState.reason
            else -> ""
        }
        val publicAddress = when {
            tunnelAddress.isNotBlank() -> tunnelAddress
            directAddress.isNotBlank() -> directAddress
            else -> ""
        }
        val activeMethod = when {
            tunnelAddress.isNotBlank() -> providerLabel
            directAddress.isNotBlank() -> "UPnP / NAT-PMP"
            tunnelStatus == "connecting" -> providerLabel
            pfStatus == "discovering" -> "UPnP / NAT-PMP"
            else -> ""
        }
        val fwCheck = if (tunnelStatus != "connected") {
            DesktopFirewallChecker.checkInboundPort(server.port, isUdp = server.type == ServerType.BEDROCK_BDS)
        } else null
        return JSONObject()
            .put("localAddress", "$localIp:${server.port}")
            .put("publicAddress", publicAddress)
            .put("activeMethod", activeMethod)
            .put("tunnelProvider", server.tunnelConfig.provider.name)
            .put("tunnelProviderLabel", providerLabel)
            .put("tunnelStatus", tunnelStatus)
            .put("tunnelAddress", tunnelAddress)
            .put("tunnelMessage", tunnelMessage)
            .put("portForwardStatus", pfStatus)
            .put("directAddress", directAddress)
            .put("portForwardMessage", pfMessage)
            .put("firewallName", fwCheck?.firewallName ?: "")
            .put("firewallWarning", if (fwCheck != null && fwCheck.isActive && !fwCheck.isPortConfirmedOpen) (fwCheck.warningMessage ?: "") else "")
            .put("firewallFixCommand", if (fwCheck != null && fwCheck.isActive && !fwCheck.isPortConfirmedOpen) (fwCheck.fixCommand ?: "") else "")
    }

    private fun parseJson(body: String): JSONObject =
        runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())

    private fun sendJson(out: OutputStream, code: Int, text: String, obj: JSONObject) {
        WebHttpUtils.sendResponse(out, code, text, "application/json; charset=utf-8", obj.toString().toByteArray(Charsets.UTF_8))
    }
}
