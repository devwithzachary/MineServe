package com.devwithzachary.mineserve.admin.web

import com.devwithzachary.mineserve.api.ModrinthApiClient
import com.devwithzachary.mineserve.engine.ChunkOptimizer
import com.devwithzachary.mineserve.engine.ProxyNetworkManager
import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.engine.ServerSchedulerManager
import com.devwithzachary.mineserve.engine.ServerSoftwareManager
import com.devwithzachary.mineserve.engine.WorldManager
import com.devwithzachary.mineserve.model.ChunkPruneOptions
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.DiscordWebhookConfig
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.net.discord.DiscordWebhookClient
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ProxyRoute
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.modpack.ModpackInstaller
import com.devwithzachary.mineserve.net.portforward.PortForwardManager
import com.devwithzachary.mineserve.net.portforward.PortForwardProtocol
import com.devwithzachary.mineserve.net.portforward.PortForwardState
import com.devwithzachary.mineserve.repository.BackupRepository
import com.devwithzachary.mineserve.repository.PluginRepository
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.tunnel.TunnelManager
import com.devwithzachary.mineserve.tunnel.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private data class ModpackImportJob(
    val id: String,
    @Volatile var status: String = "INSTALLING",
    @Volatile var message: String = "Starting modpack import...",
    @Volatile var currentFile: String = "",
    @Volatile var progressPercent: Int = 0,
    @Volatile var error: String? = null
)

/**
 * Handles extended Web Admin API endpoints for Performance, Automation, World,
 * Live Map, Tunneling & Port Forwarding, Backups, and Plugins/Mods.
 */
internal class WebAdminExtendedHandlers(
    private val scope: CoroutineScope,
    private val webAdminPort: Int,
    private val serverRepository: ServerRepository,
    private val processManager: ServerProcessManager,
    private val serverSoftwareManager: ServerSoftwareManager,
    private val backupRepository: BackupRepository,
    private val pluginRepository: PluginRepository,
    private val tunnelManager: TunnelManager,
    private val portForwardManager: PortForwardManager
) {
    private val modrinthClient = ModrinthApiClient()
    private val modpackJobs = ConcurrentHashMap<String, ModpackImportJob>()

    // --- Performance ---

    suspend fun handleGetPerformance(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val metrics = processManager.serverMetrics.value[serverId]
        val serverDir = serverRepository.getServerDirectory(serverId)
        val serverBytes = serverRepository.getServerStorageBytes(serverId)
        val freeBytes = serverDir.usableSpace

        val obj = JSONObject()
            .put("status", processManager.getServerStatus(serverId).name)
            .put("allocatedRamMb", server.allocatedRamMb)
            .put("jvmArgs", server.jvmArgs)
            .put("aikarApplied", server.jvmArgs.contains("G1NewSizePercent"))
            .put("serverStorageBytes", serverBytes)
            .put("freeDeviceStorageBytes", freeBytes)
            .put("cpuPercentage", metrics?.cpuPercentage?.toDouble() ?: 0.0)
            .put("ramUsedMb", metrics?.ramUsedMb ?: 0)
            .put("ramMaxMb", metrics?.ramMaxMb ?: server.allocatedRamMb)
            .put("tps", metrics?.tps ?: 20.0)
            .put("mspt", metrics?.mspt ?: 0.0)
            .put("uptimeSeconds", metrics?.uptimeSeconds ?: 0L)
        return ok(obj)
    }

    suspend fun handleApplyAikarFlags(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val aikarFlags = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 " +
            "-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch " +
            "-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M " +
            "-XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 " +
            "-XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 " +
            "-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem " +
            "-XX:MaxTenuringThreshold=1"
        serverRepository.updateServer(server.copy(jvmArgs = aikarFlags))
        return ok(JSONObject().put("success", true).put("jvmArgs", aikarFlags))
    }

    // --- Automation ---

    fun handleGetAutomation(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val cfg = server.automationConfig
        val tasksArr = JSONArray()
        for (t in cfg.scheduledTasks) {
            tasksArr.put(
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
        return ok(
            JSONObject()
                .put("idleSleepEnabled", cfg.idleSleepEnabled)
                .put("idleSleepTimeoutMinutes", cfg.idleSleepTimeoutMinutes)
                .put("autoWakeOnPing", cfg.autoWakeOnPing)
                .put("scheduledTasks", tasksArr)
        )
    }

    suspend fun handleSaveAutomation(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val updatedCfg = server.automationConfig.copy(
            idleSleepEnabled = json.optBoolean("idleSleepEnabled", server.automationConfig.idleSleepEnabled),
            idleSleepTimeoutMinutes = json.optInt("idleSleepTimeoutMinutes", server.automationConfig.idleSleepTimeoutMinutes).coerceIn(1, 240),
            autoWakeOnPing = json.optBoolean("autoWakeOnPing", server.automationConfig.autoWakeOnPing)
        )
        serverRepository.updateServer(server.copy(automationConfig = updatedCfg))
        return ok(JSONObject().put("success", true))
    }

    suspend fun handleAutomationTask(serverId: String, action: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val tasks = server.automationConfig.scheduledTasks.toMutableList()

        when (action) {
            "add" -> {
                val taskType = runCatching { ScheduledTaskType.valueOf(json.optString("taskType", "WORLD_BACKUP")) }
                    .getOrDefault(ScheduledTaskType.WORLD_BACKUP)
                val frequency = runCatching { ScheduleFrequency.valueOf(json.optString("frequency", "INTERVAL")) }
                    .getOrDefault(ScheduleFrequency.INTERVAL)
                val taskName = json.optString("name", taskType.displayName).trim().ifBlank { taskType.displayName }
                val newTask = ScheduledTask(
                    id = UUID.randomUUID().toString().take(8),
                    serverId = serverId,
                    name = taskName,
                    enabled = true,
                    taskType = taskType,
                    frequency = frequency,
                    intervalMinutes = json.optInt("intervalMinutes", 360).coerceAtLeast(1),
                    dailyHour = json.optInt("dailyHour", 4).coerceIn(0, 23),
                    dailyMinute = json.optInt("dailyMinute", 0).coerceIn(0, 59),
                    cronExpression = json.optString("cronExpression", ""),
                    command = json.optString("command", ""),
                    backupRetentionCount = json.optInt("backupRetentionCount", 5).coerceIn(1, 50)
                )
                tasks.add(newTask)
            }
            "toggle" -> {
                val taskId = json.optString("taskId", "")
                val idx = tasks.indexOfFirst { it.id == taskId }
                if (idx < 0) return notFound("Task not found")
                tasks[idx] = tasks[idx].copy(enabled = !tasks[idx].enabled)
            }
            "delete" -> {
                val taskId = json.optString("taskId", "")
                tasks.removeAll { it.id == taskId }
            }
            else -> return badRequest("Unknown task action: $action")
        }

        val updatedCfg = server.automationConfig.copy(scheduledTasks = tasks)
        serverRepository.updateServer(server.copy(automationConfig = updatedCfg))
        return ok(JSONObject().put("success", true))
    }

    // --- World ---

    suspend fun handleGetWorld(serverId: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val serverDir = serverRepository.getServerDirectory(serverId)
        val props = serverRepository.loadServerProperties(serverId)
        val summary = WorldManager.getWorldSummary(serverDir, props.levelName)

        val dimsArr = JSONArray()
        var totalEstimatedChunks = 0
        for (d in summary.dimensions) {
            totalEstimatedChunks += d.totalChunksCount
            dimsArr.put(
                JSONObject()
                    .put("type", d.dimension.name)
                    .put("displayName", d.dimension.displayName)
                    .put("exists", d.exists)
                    .put("sizeBytes", d.sizeBytes)
                    .put("regionFileCount", d.regionFilesCount)
                    .put("estimatedChunks", d.totalChunksCount)
            )
        }
        return ok(
            JSONObject()
                .put("levelName", summary.levelName)
                .put("totalSizeBytes", summary.totalSizeBytes)
                .put("totalEstimatedChunks", totalEstimatedChunks)
                .put("dimensions", dimsArr)
        )
    }

    suspend fun handleResetDimension(serverId: String, body: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        if (processManager.isServerRunning(serverId)) {
            return badRequest("Stop the server before resetting a dimension.")
        }
        val json = parseJsonObject(body)
        val dimType = runCatching { DimensionType.valueOf(json.optString("dimension", "")) }
            .getOrElse { return badRequest("Invalid dimension type") }
        val createBackup = json.optBoolean("createBackup", true)
        val serverDir = serverRepository.getServerDirectory(serverId)
        val props = serverRepository.loadServerProperties(serverId)

        if (createBackup) {
            backupRepository.createBackup(serverDir, isWorldOnly = true)
        }
        val success = WorldManager.resetDimension(serverDir, dimType, props.levelName)
        return ok(JSONObject().put("success", success))
    }

    suspend fun handleOptimizeWorld(serverId: String, body: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        if (processManager.isServerRunning(serverId)) {
            return badRequest("Stop the server before optimizing chunks.")
        }
        val json = parseJsonObject(body)
        val createBackup = json.optBoolean("createBackup", true)
        val pruneNether = json.optBoolean("pruneNether", true)
        val pruneEnd = json.optBoolean("pruneEnd", true)
        val thresholdName = json.optString("threshold", "").trim()
        val threshold = InhabitedTimeThreshold.entries.firstOrNull { it.name == thresholdName }
            ?: run {
                val thresholdTicks = json.optLong("thresholdTicks", 0L).coerceAtLeast(0L)
                InhabitedTimeThreshold.entries
                    .minByOrNull { kotlin.math.abs(it.ticks - thresholdTicks) }
                    ?: InhabitedTimeThreshold.UNTOUCHED
            }
        val serverDir = serverRepository.getServerDirectory(serverId)
        val props = serverRepository.loadServerProperties(serverId)

        if (createBackup) {
            backupRepository.createBackup(serverDir, isWorldOnly = true)
        }
        val options = ChunkPruneOptions(
            threshold = threshold,
            pruneNether = pruneNether,
            pruneEnd = pruneEnd,
            createBackup = createBackup
        )
        val result = ChunkOptimizer.optimizeWorld(serverDir, options, props.levelName)
        return ok(
            JSONObject()
                .put("success", true)
                .put("regionsScanned", result.scannedRegions)
                .put("scannedChunks", result.scannedChunks)
                .put("chunksPruned", result.prunedChunks)
                .put("deletedRegions", result.deletedRegions)
                .put("bytesBefore", result.bytesBefore)
                .put("bytesAfter", result.bytesAfter)
                .put("bytesSaved", result.bytesFreed)
                .put("percentFreed", result.percentFreed)
        )
    }

    suspend fun exportWorldToTempZip(serverId: String): Pair<File?, WebApiResult?> {
        val server = serverRepository.getServer(serverId) ?: return null to notFound("Server not found")
        if (processManager.isServerRunning(serverId)) {
            return null to badRequest("Stop the server before exporting the world.")
        }
        val serverDir = serverRepository.getServerDirectory(serverId)
        val props = serverRepository.loadServerProperties(serverId)
        val tempFile = File.createTempFile("mineserve_world_${server.id}_", ".zip")
        val success = tempFile.outputStream().use { out ->
            WorldManager.exportWorld(serverDir, out, props.levelName)
        }
        if (!success) {
            tempFile.delete()
            return null to badRequest("Failed to export world archive.")
        }
        return tempFile to null
    }

    suspend fun handleImportWorld(serverId: String, uploadedZip: File, createBackup: Boolean): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        if (processManager.isServerRunning(serverId)) {
            return badRequest("Stop the server before importing a world.")
        }
        val serverDir = serverRepository.getServerDirectory(serverId)
        val props = serverRepository.loadServerProperties(serverId)
        if (createBackup) {
            backupRepository.createBackup(serverDir, isWorldOnly = true)
        }
        val result = WorldManager.importWorldFromZip(serverDir, uploadedZip, props.levelName)
        return result.fold(
            onSuccess = { msg -> ok(JSONObject().put("success", true).put("message", msg)) },
            onFailure = { err -> badRequest(err.message ?: "Failed to import world archive.") }
        )
    }

    // --- Live Map (Squaremap) ---

    fun handleGetLiveMap(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val serverDir = serverRepository.getServerDirectory(serverId)
        val installed = WebMapPluginType.detectInstalled(serverDir)
        val port = detectSquaremapPort(serverDir)
        val isRunning = processManager.isServerRunning(serverId)
        val localIp = portForwardManager.getLocalWifiIpAddress() ?: "127.0.0.1"
        val portConflict = port == webAdminPort
        val suggestedPort = if (webAdminPort == 8080) 8081 else 8080

        return ok(
            JSONObject()
                .put("installed", installed != null)
                .put("pluginName", installed?.displayName ?: "")
                .put("port", port)
                .put("webAdminPort", webAdminPort)
                .put("portConflict", portConflict)
                .put("suggestedPort", suggestedPort)
                .put("isServerRunning", isRunning)
                .put("supportsPluginsOrMods", server.type.supportsPlugins || server.type.supportsMods)
                .put("localMapUrl", "http://$localIp:$port")
        )
    }

    suspend fun handleInstallLiveMap(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val isModServer = server.type.supportsMods && !server.type.supportsPlugins
        val loader = loaderForServer(server)
        val resolved = modrinthClient.resolveDownloadUrl(
            projectIdOrSlug = WebMapPluginType.SQUAREMAP.modrinthSlug,
            isMod = isModServer,
            loaderFilter = loader,
            gameVersion = server.version
        ) ?: modrinthClient.resolveDownloadUrl(
            projectIdOrSlug = WebMapPluginType.SQUAREMAP.modrinthSlug,
            isMod = isModServer,
            loaderFilter = null,
            gameVersion = null
        ) ?: return badRequest("Could not resolve Squaremap download from Modrinth.")

        val serverDir = serverRepository.getServerDirectory(serverId)
        val targetFolder = File(serverDir, if (isModServer) "mods" else "plugins").apply { mkdirs() }
        val destFile = File(targetFolder, resolved.first)
        serverSoftwareManager.downloadFileWithProgress(resolved.second, destFile) { _, _ -> }
        val okDownload = destFile.exists() && destFile.length() > 0L
        if (okDownload && detectSquaremapPort(serverDir) == webAdminPort) {
            val safePort = if (webAdminPort == 8080) 8081 else 8080
            setSquaremapPort(serverDir, server.type, safePort)
        }
        return ok(JSONObject().put("success", okDownload).put("fileName", resolved.first))
    }

    fun handleSetLiveMapPort(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val newPort = json.optInt("port", -1)
        if (newPort !in 1024..65535) {
            return badRequest("Port must be between 1024 and 65535.")
        }
        if (newPort == webAdminPort) {
            return badRequest("Live Map port cannot be the same as Web Admin port ($webAdminPort).")
        }
        val serverDir = serverRepository.getServerDirectory(serverId)
        setSquaremapPort(serverDir, server.type, newPort)
        return ok(JSONObject().put("success", true).put("port", newPort))
    }

    fun handleUninstallLiveMap(serverId: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val serverDir = serverRepository.getServerDirectory(serverId)
        val file = WebMapPluginType.findInstalledFile(serverDir)
        val deleted = file != null && file.exists() && file.delete()
        return ok(JSONObject().put("success", deleted))
    }

    // --- Networking (Tunnels & Port Forwarding) ---

    fun handleGetNetwork(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val tCfg = server.tunnelConfig
        val tState = tunnelManager.getTunnelState(serverId)
        val pfCfg = server.portForwardConfig
        val pfState = portForwardManager.getState(serverId)

        val tStateObj = when (tState) {
            is TunnelState.Disconnected -> JSONObject().put("status", "disconnected")
            is TunnelState.Connecting -> JSONObject()
                .put("status", "connecting")
                .put("message", tState.message)
                .put("claimUrl", tState.claimUrl ?: "")
            is TunnelState.Connected -> JSONObject()
                .put("status", "connected")
                .put("fullAddress", tState.fullAddress)
                .put("publicHost", tState.publicHost)
                .put("publicPort", tState.publicPort)
            is TunnelState.Error -> JSONObject()
                .put("status", "error")
                .put("errorMessage", tState.errorMessage)
        }

        val pfStateObj = when (pfState) {
            is PortForwardState.Idle -> JSONObject().put("status", "idle")
            is PortForwardState.Discovering -> JSONObject().put("status", "discovering").put("message", pfState.message)
            is PortForwardState.Mapped -> JSONObject()
                .put("status", "mapped")
                .put("directAddress", pfState.directAddress)
                .put("externalIp", pfState.externalIp)
                .put("externalPort", pfState.externalPort)
                .put("gatewayName", pfState.gatewayName)
            is PortForwardState.Failed -> JSONObject().put("status", "failed").put("reason", pfState.reason)
        }

        return ok(
            JSONObject()
                .put("localIp", portForwardManager.getLocalWifiIpAddress() ?: "")
                .put("tunnelConfig", JSONObject()
                    .put("provider", tCfg.provider.name)
                    .put("autoStart", tCfg.autoStart)
                    .put("customRelayHost", tCfg.customRelayHost)
                    .put("customRelayPort", tCfg.customRelayPort)
                    .put("playitSecretSet", tCfg.playitSecret.isNotBlank()))
                .put("tunnelState", tStateObj)
                .put("portForwardConfig", JSONObject()
                    .put("autoForward", pfCfg.autoForward)
                    .put("protocol", pfCfg.protocol.name)
                    .put("leaseDurationSeconds", pfCfg.leaseDurationSeconds))
                .put("portForwardState", pfStateObj)
        )
    }

    suspend fun handleTunnelAction(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val action = json.optString("action", "save").lowercase()

        val provider = runCatching { TunnelProvider.valueOf(json.optString("provider", server.tunnelConfig.provider.name)) }
            .getOrDefault(server.tunnelConfig.provider)
        val updatedTunnelCfg = server.tunnelConfig.copy(
            provider = provider,
            autoStart = json.optBoolean("autoStart", server.tunnelConfig.autoStart),
            customRelayHost = json.optString("customRelayHost", server.tunnelConfig.customRelayHost).trim().ifBlank { "bore.pub" },
            customRelayPort = json.optInt("customRelayPort", server.tunnelConfig.customRelayPort).coerceIn(1, 65535),
            playitSecret = json.optString("playitSecret", "").takeIf { it.isNotBlank() } ?: server.tunnelConfig.playitSecret
        )
        val updatedServer = server.copy(tunnelConfig = updatedTunnelCfg)
        serverRepository.updateServer(updatedServer)

        when (action) {
            "start" -> tunnelManager.startTunnel(updatedServer)
            "stop" -> tunnelManager.stopTunnel(serverId)
        }
        return ok(JSONObject().put("success", true))
    }

    suspend fun handlePortForwardAction(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val action = json.optString("action", "save").lowercase()

        val protocol = runCatching { PortForwardProtocol.valueOf(json.optString("protocol", server.portForwardConfig.protocol.name)) }
            .getOrDefault(server.portForwardConfig.protocol)
        val updatedPfCfg = server.portForwardConfig.copy(
            autoForward = json.optBoolean("autoForward", server.portForwardConfig.autoForward),
            protocol = protocol,
            leaseDurationSeconds = json.optInt("leaseDurationSeconds", server.portForwardConfig.leaseDurationSeconds).coerceAtLeast(0)
        )
        val updatedServer = server.copy(portForwardConfig = updatedPfCfg)
        serverRepository.updateServer(updatedServer)

        when (action) {
            "start" -> portForwardManager.forwardPortAsync(updatedServer)
            "stop" -> portForwardManager.releasePort(serverId)
        }
        return ok(JSONObject().put("success", true))
    }

    // --- Backups ---

    suspend fun handleListBackups(serverId: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val serverDir = serverRepository.getServerDirectory(serverId)
        val list = backupRepository.listBackups(serverDir)
        val arr = JSONArray()
        for (b in list) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("name", b.name)
                    .put("fileName", b.fileName)
                    .put("sizeBytes", b.sizeBytes)
                    .put("timestamp", b.timestamp)
                    .put("isWorldOnly", b.isWorldOnly)
            )
        }
        return ok(JSONObject().put("backups", arr))
    }

    suspend fun handleCreateBackup(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val isWorldOnly = json.optBoolean("isWorldOnly", true)
        val serverDir = serverRepository.getServerDirectory(serverId)
        val entry = backupRepository.createBackup(serverDir, isWorldOnly = isWorldOnly)
            ?: return badRequest("Failed to create backup (ensure world directory exists).")
        DiscordWebhookClient.notifyBackupCompleted(
            server,
            if (isWorldOnly) "World Backup" else "Full Server Backup",
            entry.fileName
        )
        return ok(JSONObject().put("success", true).put("fileName", entry.fileName))
    }

    // --- Discord Webhook Bridge ---

    fun handleGetDiscord(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val cfg = server.discordWebhookConfig
        return ok(
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

    suspend fun handleSaveDiscord(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val updatedCfg = DiscordWebhookConfig(
            enabled = json.optBoolean("enabled", server.discordWebhookConfig.enabled),
            webhookUrl = json.optString("webhookUrl", server.discordWebhookConfig.webhookUrl).trim(),
            notifyServerStart = json.optBoolean("notifyServerStart", server.discordWebhookConfig.notifyServerStart),
            notifyServerStop = json.optBoolean("notifyServerStop", server.discordWebhookConfig.notifyServerStop),
            notifyServerCrash = json.optBoolean("notifyServerCrash", server.discordWebhookConfig.notifyServerCrash),
            notifyPlayerJoin = json.optBoolean("notifyPlayerJoin", server.discordWebhookConfig.notifyPlayerJoin),
            notifyPlayerLeave = json.optBoolean("notifyPlayerLeave", server.discordWebhookConfig.notifyPlayerLeave),
            notifyBackupComplete = json.optBoolean("notifyBackupComplete", server.discordWebhookConfig.notifyBackupComplete),
            includePlayerSkins = json.optBoolean("includePlayerSkins", server.discordWebhookConfig.includePlayerSkins)
        )
        val updatedServer = server.copy(discordWebhookConfig = updatedCfg)
        serverRepository.updateServer(updatedServer)
        processManager.updateSessionServerConfig(updatedServer)
        return ok(JSONObject().put("success", true))
    }

    suspend fun handleTestDiscord(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val url = parseJsonObject(body).optString("webhookUrl", server.discordWebhookConfig.webhookUrl).trim()
        val result = DiscordWebhookClient.sendTestMessage(url, server.name)
        return result.fold(
            onSuccess = { ok(JSONObject().put("success", true)) },
            onFailure = { err -> badRequest(err.message ?: "Failed to send Discord test webhook.") }
        )
    }

    // --- Proxy Network & Loopback ---

    suspend fun handleGetProxy(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        if (server.type.isProxy) {
            val allServers = serverRepository.servers.value
            val candidates = JSONArray()
            for (b in allServers) {
                if (b.id == server.id || b.type.isProxy) continue
                val existing = server.proxyConfig.routes.firstOrNull { it.targetServerId == b.id }
                candidates.put(
                    JSONObject()
                        .put("id", b.id)
                        .put("name", b.name)
                        .put("type", b.type.displayName)
                        .put("port", b.port)
                        .put("linked", existing != null)
                        .put("alias", existing?.alias ?: ProxyNetworkManager.sanitizeAlias(b.name))
                )
            }
            return ok(
                JSONObject()
                    .put("isProxy", true)
                    .put("proxyType", server.type.displayName)
                    .put("onlineMode", server.proxyConfig.onlineMode)
                    .put("candidates", candidates)
            )
        } else {
            val props = serverRepository.loadServerProperties(serverId)
            return ok(
                JSONObject()
                    .put("isProxy", false)
                    .put("loopbackOnly", props.serverIp.trim() == "127.0.0.1")
            )
        }
    }

    suspend fun handleSaveProxy(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        if (server.type.isProxy) {
            val allServers = serverRepository.servers.value
            val targetIds = json.optJSONArray("linkedServerIds") ?: JSONArray()
            val routes = mutableListOf<ProxyRoute>()
            for (i in 0 until targetIds.length()) {
                val tid = targetIds.optString(i, "")
                val backend = allServers.firstOrNull { it.id == tid && !it.type.isProxy } ?: continue
                routes.add(
                    ProxyRoute(
                        alias = ProxyNetworkManager.sanitizeAlias(backend.name),
                        targetServerId = backend.id,
                        host = "127.0.0.1",
                        port = backend.port
                    )
                )
            }
            val onlineMode = json.optBoolean("onlineMode", server.proxyConfig.onlineMode)
            val draft = server.copy(
                proxyConfig = server.proxyConfig.copy(
                    routes = routes,
                    tryOrder = routes.map { it.alias },
                    onlineMode = onlineMode
                )
            )
            val appliedCfg = ProxyNetworkManager.applyProxyConfig(
                proxyServer = draft,
                proxyDir = serverRepository.getServerDirectory(serverId),
                allServers = allServers
            )
            serverRepository.updateServer(draft.copy(proxyConfig = appliedCfg))

            if (json.optBoolean("bindBackendsLoopback", false)) {
                routes.forEach { r ->
                    ProxyNetworkManager.setBackendLoopbackMode(serverRepository, r.targetServerId, true)
                }
            }
            return ok(JSONObject().put("success", true).put("linkedCount", routes.size))
        } else {
            val loopbackOnly = json.optBoolean("loopbackOnly", false)
            ProxyNetworkManager.setBackendLoopbackMode(serverRepository, serverId, loopbackOnly)
            return ok(JSONObject().put("success", true).put("loopbackOnly", loopbackOnly))
        }
    }

    suspend fun handleRestoreBackup(serverId: String, body: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        if (processManager.isServerRunning(serverId)) {
            return badRequest("Stop the server before restoring a backup.")
        }
        val fileName = parseJsonObject(body).optString("fileName", "").trim()
        val backupFile = resolveSafeChild(File(serverRepository.getServerDirectory(serverId), "backups"), fileName)
            ?: return badRequest("Invalid backup file name.")
        val success = backupRepository.restoreBackup(serverRepository.getServerDirectory(serverId), backupFile)
        return ok(JSONObject().put("success", success))
    }

    suspend fun handleDeleteBackup(serverId: String, body: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val fileName = parseJsonObject(body).optString("fileName", "").trim()
        val backupFile = resolveSafeChild(File(serverRepository.getServerDirectory(serverId), "backups"), fileName)
            ?: return badRequest("Invalid backup file name.")
        val success = backupRepository.deleteBackup(backupFile)
        return ok(JSONObject().put("success", success))
    }

    fun getBackupFile(serverId: String, fileName: String): File? {
        serverRepository.getServer(serverId) ?: return null
        val backupsDir = File(serverRepository.getServerDirectory(serverId), "backups")
        return resolveSafeChild(backupsDir, fileName)?.takeIf { it.exists() && it.isFile }
    }

    // --- Plugins / Mods ---

    suspend fun handleListPlugins(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val serverDir = serverRepository.getServerDirectory(serverId)
        val items = pluginRepository.listPluginsAndMods(serverDir)
        val arr = JSONArray()
        for (p in items) {
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("fileName", p.fileName)
                    .put("name", p.name)
                    .put("enabled", p.enabled)
                    .put("isMod", p.isMod)
                    .put("sizeBytes", p.fileSizeBytes)
            )
        }
        return ok(
            JSONObject()
                .put("supportsPlugins", server.type.supportsPlugins)
                .put("supportsMods", server.type.supportsMods)
                .put("items", arr)
        )
    }

    suspend fun handlePluginAction(serverId: String, action: String, body: String): WebApiResult {
        serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val serverDir = serverRepository.getServerDirectory(serverId)
        val fileName = parseJsonObject(body).optString("fileName", "").trim()
        val items = pluginRepository.listPluginsAndMods(serverDir)
        val entry = items.firstOrNull { it.fileName == fileName } ?: return notFound("Plugin/Mod not found")

        val success = when (action) {
            "toggle" -> pluginRepository.togglePlugin(serverDir, entry)
            "delete" -> pluginRepository.deletePlugin(serverDir, entry)
            else -> return badRequest("Unknown plugin action: $action")
        }
        return ok(JSONObject().put("success", success))
    }

    suspend fun handleSearchModrinth(serverId: String, query: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val isMod = server.type.supportsMods && !server.type.supportsPlugins
        val loader = loaderForServer(server)
        val results = modrinthClient.search(query, isMod = isMod, loaderFilter = loader, gameVersion = server.version)
        val arr = JSONArray()
        for (r in results) {
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("slug", r.slug)
                    .put("name", r.name)
                    .put("description", r.description)
                    .put("author", r.author)
                    .put("downloads", r.downloads)
                    .put("iconUrl", r.iconUrl ?: "")
            )
        }
        return ok(JSONObject().put("results", arr))
    }

    suspend fun handleInstallModrinth(serverId: String, body: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val json = parseJsonObject(body)
        val projectId = json.optString("projectId", "").trim()
        if (projectId.isBlank()) return badRequest("projectId is required")

        val isMod = server.type.supportsMods && !server.type.supportsPlugins
        val loader = loaderForServer(server)
        val resolved = modrinthClient.resolveDownloadUrl(
            projectIdOrSlug = projectId,
            isMod = isMod,
            loaderFilter = loader,
            gameVersion = server.version
        ) ?: modrinthClient.resolveDownloadUrl(
            projectIdOrSlug = projectId,
            isMod = isMod,
            loaderFilter = null,
            gameVersion = null
        ) ?: return badRequest("No compatible file found on Modrinth.")

        val serverDir = serverRepository.getServerDirectory(serverId)
        val targetDir = File(serverDir, if (isMod) "mods" else "plugins").apply { mkdirs() }
        val destFile = File(targetDir, resolved.first)
        val okDownload = serverSoftwareManager.downloadFileWithProgress(resolved.second, destFile) { _, _ -> }
        return ok(JSONObject().put("success", okDownload).put("fileName", resolved.first))
    }

    fun handleUploadPluginJar(serverId: String, rawFileName: String, uploadedFile: File): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        if (!server.type.supportsPlugins && !server.type.supportsMods) {
            return badRequest("This server software does not support plugins or mods.")
        }
        val cleanName = rawFileName.trim().ifBlank { "uploaded_addon.jar" }
        if (!cleanName.endsWith(".jar", ignoreCase = true)) {
            return badRequest("Only .jar files can be uploaded.")
        }
        val serverDir = serverRepository.getServerDirectory(serverId)
        val isModServer = server.type.supportsMods && !server.type.supportsPlugins
        val targetFolderName = if (isModServer) "mods" else "plugins"
        val targetDir = File(serverDir, targetFolderName).apply { mkdirs() }
        val destFile = resolveSafeChild(targetDir, cleanName)
            ?: return badRequest("Invalid file name.")
        uploadedFile.copyTo(destFile, overwrite = true)
        return ok(
            JSONObject()
                .put("success", true)
                .put("fileName", destFile.name)
                .put("folder", targetFolderName)
        )
    }

    fun handleStartModpackImport(serverId: String, mrpackFile: File): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: run {
            mrpackFile.delete()
            return notFound("Server not found")
        }
        if (processManager.isServerRunning(serverId)) {
            mrpackFile.delete()
            return badRequest("Stop the server before importing a modpack.")
        }
        if (!server.type.supportsMods && !server.type.supportsPlugins) {
            mrpackFile.delete()
            return badRequest("This server software does not support modpacks.")
        }
        val jobId = UUID.randomUUID().toString()
        val job = ModpackImportJob(id = jobId)
        modpackJobs[jobId] = job
        val serverDir = serverRepository.getServerDirectory(serverId)

        scope.launch {
            try {
                val res = ModpackInstaller().installModpackFromFile(mrpackFile, serverDir) { progress ->
                    job.message = progress.stage
                    job.currentFile = progress.currentItem
                    job.progressPercent = (progress.progress * 100f).toInt().coerceIn(0, 100)
                }
                res.fold(
                    onSuccess = { index ->
                        job.status = "DONE"
                        job.progressPercent = 100
                        job.message = "Imported ${index.name} (${index.versionId})"
                    },
                    onFailure = { err ->
                        job.status = "ERROR"
                        job.error = err.message ?: "Modpack import failed."
                    }
                )
            } catch (e: Exception) {
                job.status = "ERROR"
                job.error = e.message ?: "Modpack import failed."
            } finally {
                mrpackFile.delete()
            }
        }
        return ok(JSONObject().put("success", true).put("jobId", jobId))
    }

    fun handleGetModpackJobStatus(jobId: String): WebApiResult {
        val job = modpackJobs[jobId] ?: return notFound("Modpack import job not found")
        val obj = JSONObject()
            .put("jobId", job.id)
            .put("status", job.status)
            .put("message", job.message)
            .put("currentFile", job.currentFile)
            .put("progressPercent", job.progressPercent)
        if (job.error != null) obj.put("error", job.error)
        return ok(obj)
    }

    // --- Helpers ---

    private fun loaderForServer(server: MinecraftServer): String = when (server.type) {
        ServerType.FABRIC -> "fabric"
        ServerType.FORGE -> "forge"
        ServerType.NEOFORGE -> "neoforge"
        ServerType.VELOCITY -> "velocity"
        ServerType.BUNGEECORD -> "bungeecord"
        else -> "paper"
    }

    private fun detectSquaremapPort(serverDir: File): Int {
        val candidates = listOf(
            File(serverDir, "plugins/squaremap/config.yml"),
            File(serverDir, "mods/squaremap/config.yml"),
            File(serverDir, "config/squaremap/config.yml")
        )
        for (cfg in candidates) {
            if (cfg.exists()) {
                val match = Regex("""^\s*port:\s*(\d+)""", RegexOption.MULTILINE).find(cfg.readText())
                val p = match?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (p != null && p in 1..65535) return p
            }
        }
        return WebMapPluginType.SQUAREMAP.defaultPort
    }

    private fun setSquaremapPort(serverDir: File, serverType: ServerType, port: Int) {
        val candidates = listOf(
            File(serverDir, "plugins/squaremap/config.yml"),
            File(serverDir, "mods/squaremap/config.yml"),
            File(serverDir, "config/squaremap/config.yml")
        )
        val existing = candidates.firstOrNull { it.exists() }
        if (existing != null) {
            val content = existing.readText()
            val regex = Regex("""^(\s*port:\s*)\d+""", RegexOption.MULTILINE)
            val updated = if (regex.containsMatchIn(content)) {
                regex.replaceFirst(content, "$1$port")
            } else {
                content.trimEnd() + "\nsettings:\n  internal-webserver:\n    port: $port\n"
            }
            existing.writeText(updated)
            return
        }
        val isMod = serverType.supportsMods && !serverType.supportsPlugins
        val targetCfg = File(serverDir, if (isMod) "config/squaremap/config.yml" else "plugins/squaremap/config.yml")
        targetCfg.parentFile?.mkdirs()
        targetCfg.writeText(
            """
            settings:
              internal-webserver:
                enabled: true
                bind: 0.0.0.0
                port: $port
            """.trimIndent() + "\n"
        )
    }

    private fun resolveSafeChild(parentDir: File, fileName: String): File? {
        if (fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return null
        }
        val child = File(parentDir, fileName)
        return if (child.canonicalPath.startsWith(parentDir.canonicalPath)) child else null
    }

    private fun parseJsonObject(body: String): JSONObject =
        runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrElse { JSONObject() }

    private fun ok(body: JSONObject) = WebApiResult(200, "OK", body)
    private fun badRequest(msg: String) = WebApiResult(400, "Bad Request", JSONObject().put("error", msg))
    private fun notFound(msg: String) = WebApiResult(404, "Not Found", JSONObject().put("error", msg))
}
