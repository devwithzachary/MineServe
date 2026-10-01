package com.devwithzachary.mineserve.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.api.GitHubRelease
import com.devwithzachary.mineserve.api.GitHubUpdateChecker
import com.devwithzachary.mineserve.api.UpdateCheckResult
import com.devwithzachary.mineserve.engine.JavaInstallState
import com.devwithzachary.mineserve.engine.JavaRuntimeManager
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.engine.RootfsManager
import com.devwithzachary.mineserve.engine.RootfsSetupState
import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.engine.ServerSchedulerManager
import com.devwithzachary.mineserve.engine.ServerSoftwareManager
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ServerBuildInfo
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelConfig
import com.devwithzachary.mineserve.repository.BackupRepository
import com.devwithzachary.mineserve.repository.PluginRepository
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.repository.UpdatePreferences
import com.devwithzachary.mineserve.service.MineServeForegroundService
import com.devwithzachary.mineserve.tunnel.TunnelManager
import com.devwithzachary.mineserve.tunnel.TunnelState
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    val pRootEngine = PRootEngine(application)
    val rootfsManager = RootfsManager(application, pRootEngine)
    val javaRuntimeManager = JavaRuntimeManager(application, pRootEngine)
    val processManager = ServerProcessManager.getInstance(application, pRootEngine, javaRuntimeManager)
    val serverRepository = ServerRepository(application, pRootEngine)
    val backupRepository = BackupRepository(application)
    val pluginRepository = PluginRepository()
    val serverSoftwareManager = ServerSoftwareManager(serverRepository, backupRepository, processManager)
    val tunnelManager = TunnelManager.getInstance(application)
    val updateChecker = GitHubUpdateChecker()
    val updatePreferences = UpdatePreferences(application)
    val schedulerManager = ServerSchedulerManager.getInstance(application, serverRepository, backupRepository)

    val servers: StateFlow<List<MinecraftServer>> = serverRepository.servers
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = processManager.serverStatuses
    val serverMetrics: StateFlow<Map<String, ServerMetrics>> = processManager.serverMetrics
    val refreshTriggers: StateFlow<Map<String, Long>> = processManager.refreshTriggers
    val tunnelStates: StateFlow<Map<String, TunnelState>> = tunnelManager.tunnelStates

    private val _isCheckGitHubUpdatesEnabled = MutableStateFlow(updatePreferences.isCheckGitHubUpdatesEnabled)
    val isCheckGitHubUpdatesEnabled: StateFlow<Boolean> = _isCheckGitHubUpdatesEnabled.asStateFlow()

    private val _availableUpdate = MutableStateFlow<GitHubRelease?>(null)
    val availableUpdate: StateFlow<GitHubRelease?> = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateCheckStatus = MutableStateFlow<String?>(null)
    val updateCheckStatus: StateFlow<String?> = _updateCheckStatus.asStateFlow()

    val isRootfsInstalled: StateFlow<Boolean> = rootfsManager.isInstalledState
    val rootfsSetupState: StateFlow<RootfsSetupState> = rootfsManager.setupState

    private val _storageUsedMb = MutableStateFlow(0L)
    val storageUsedMb: StateFlow<Long> = _storageUsedMb.asStateFlow()

    private val _serverPropertiesMap = MutableStateFlow<Map<String, ServerProperties>>(emptyMap())
    val serverPropertiesMap: StateFlow<Map<String, ServerProperties>> = _serverPropertiesMap.asStateFlow()

    private val _serverBackupsMap = MutableStateFlow<Map<String, List<BackupEntry>>>(emptyMap())
    val serverBackupsMap: StateFlow<Map<String, List<BackupEntry>>> = _serverBackupsMap.asStateFlow()

    private val _serverPluginsMap = MutableStateFlow<Map<String, List<PluginModEntry>>>(emptyMap())
    val serverPluginsMap: StateFlow<Map<String, List<PluginModEntry>>> = _serverPluginsMap.asStateFlow()

    private val _serverStorageMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val serverStorageMap: StateFlow<Map<String, Long>> = _serverStorageMap.asStateFlow()

    init {
        // Connect background foreground service callbacks
        MineServeForegroundService.onStopAllServersRequested = {
            for ((id, status) in processManager.serverStatuses.value) {
                if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING) {
                    processManager.stopServer(id)
                }
            }
        }

        MineServeForegroundService.activeServerInfoProvider = {
            val count = processManager.getAnyRunningServerCount()
            if (count > 0) "$count Minecraft server(s) running" else "Server engine idle"
        }

        schedulerManager.processManagerProvider = { processManager }
        schedulerManager.startServerCallback = { server: MinecraftServer -> startServer(server) }
        schedulerManager.start()

        processManager.onServerWakeRequested = { server: MinecraftServer ->
            startServer(server)
        }

        refreshData()

        if (updatePreferences.isCheckGitHubUpdatesEnabled) {
            checkForGitHubUpdates(isManual = false)
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val loaded = serverRepository.loadServers()
            for (s in loaded) {
                loadServerDetails(s.id)
            }
            rootfsManager.refreshInstalledState()
            _storageUsedMb.value = rootfsManager.getStorageUsedMb()
        }
    }

    fun loadServerDetails(serverId: String) {
        viewModelScope.launch {
            val serverDir = serverRepository.getServerDirectory(serverId)
            val props = serverRepository.loadServerProperties(serverId)
            val backups = backupRepository.listBackups(serverDir)
            val plugins = pluginRepository.listPluginsAndMods(serverDir)
            val storageBytes = serverRepository.getServerStorageBytes(serverId)

            val pMap = _serverPropertiesMap.value.toMutableMap()
            pMap[serverId] = props
            _serverPropertiesMap.value = pMap

            val bMap = _serverBackupsMap.value.toMutableMap()
            bMap[serverId] = backups
            _serverBackupsMap.value = bMap

            val plMap = _serverPluginsMap.value.toMutableMap()
            plMap[serverId] = plugins
            _serverPluginsMap.value = plMap

            val sMap = _serverStorageMap.value.toMutableMap()
            sMap[serverId] = storageBytes
            _serverStorageMap.value = sMap
        }
    }

    fun startRootfsSetup() {
        viewModelScope.launch {
            rootfsManager.performRootfsSetup(javaRuntimeManager) {
                refreshData()
            }
        }
    }

    fun installJava(version: Int = 21) {
        viewModelScope.launch {
            javaRuntimeManager.installJava(version).collect { state ->
                when (state) {
                    is JavaInstallState.Success -> {
                        Log.d(TAG, "Java $version installed successfully")
                        refreshData()
                    }
                    is JavaInstallState.Error -> {
                        Log.e(TAG, "Java $version install failed: ${state.errorMessage}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun startServer(server: MinecraftServer) {
        val freshServer = servers.value.firstOrNull { it.id == server.id } ?: server
        Log.d(TAG, "startServer: requested start for server ${freshServer.name} (${freshServer.id})")
        val serverDir = serverRepository.getServerDirectory(freshServer.id)
        MineServeForegroundService.start(getApplication())
        processManager.startServer(freshServer, serverDir)
    }

    fun stopServer(serverId: String) {
        processManager.stopServer(serverId)
    }

    fun sendCommand(serverId: String, command: String) {
        processManager.sendCommand(serverId, command)
    }

    fun resizeTerminal(serverId: String, cols: Int, rows: Int) {
        processManager.resizeTerminal(serverId, cols, rows)
    }

    suspend fun downloadAndCreateServer(
        name: String,
        type: ServerType,
        version: String,
        port: Int,
        ramMb: Int,
        motd: String,
        onProgress: (String, Int) -> Unit
    ): MinecraftServer? = withContext(Dispatchers.IO) {
        try {
            onProgress("Resolving download URL for ${type.displayName} $version...", 10)
            val buildInfo = serverSoftwareManager.resolveLatestBuild(type, version)
            val jarUrl = buildInfo?.second

            val isInstaller = type == ServerType.NEOFORGE || type == ServerType.FORGE
            val server = serverRepository.createServer(
                name = name,
                type = type,
                version = version,
                port = port,
                ramMb = ramMb,
                motd = motd,
                jarFileName = if (isInstaller) "installer.jar" else "server.jar",
                serverBuild = buildInfo?.first
            )

            val serverDir = serverRepository.getServerDirectory(server.id)
            serverSoftwareManager.provisionServerSoftware(server, serverDir, jarUrl, onProgress)

            onProgress("Server created successfully!", 100)
            loadServerDetails(server.id)
            server
        } catch (e: Exception) {
            Log.e(TAG, "Error creating server", e)
            null
        }
    }

    suspend fun resolveLatestBuild(type: ServerType, version: String): Pair<String, String>? =
        serverSoftwareManager.resolveLatestBuild(type, version)

    suspend fun fetchAvailableVersionsForServer(type: ServerType): List<String> =
        serverSoftwareManager.fetchAvailableVersionsForServer(type)

    suspend fun checkForServerBuildUpdate(server: MinecraftServer): ServerBuildInfo? =
        serverSoftwareManager.checkForServerBuildUpdate(server)

    suspend fun updateServerBuild(
        serverId: String,
        createBackup: Boolean,
        onProgress: (String, Int) -> Unit
    ): Boolean {
        val server = servers.value.firstOrNull { it.id == serverId } ?: return false
        val success = serverSoftwareManager.updateServerBuild(server, createBackup, onProgress)
        if (success) {
            loadServerDetails(serverId)
            refreshData()
        }
        return success
    }

    suspend fun upgradeServerVersion(
        serverId: String,
        newVersion: String,
        createBackup: Boolean,
        onProgress: (String, Int) -> Unit
    ): Boolean {
        val server = servers.value.firstOrNull { it.id == serverId } ?: return false
        val success = serverSoftwareManager.upgradeServerVersion(server, newVersion, createBackup, onProgress)
        if (success) {
            loadServerDetails(serverId)
            refreshData()
        }
        return success
    }

    fun deleteServer(serverId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            processManager.forceStopAndCleanup(serverId)
            serverRepository.deleteServer(serverId)
            refreshData()
            withContext(Dispatchers.Main) {
                onDeleted()
            }
        }
    }

    fun saveProperties(serverId: String, properties: ServerProperties) {
        viewModelScope.launch {
            serverRepository.saveServerProperties(serverId, properties)
            loadServerDetails(serverId)
        }
    }

    suspend fun readRawConfigFile(serverId: String, fileName: String): String {
        return serverRepository.readRawConfigFile(serverId, fileName)
    }

    suspend fun saveRawConfigFile(serverId: String, fileName: String, content: String): Boolean {
        val success = serverRepository.saveRawConfigFile(serverId, fileName, content)
        if (success && fileName == "server.properties") {
            loadServerDetails(serverId)
        }
        return success
    }

    suspend fun listEditableConfigFiles(serverId: String): List<String> {
        return serverRepository.listEditableConfigFiles(serverId)
    }

    suspend fun listDirectory(serverId: String, relativePath: String = ""): List<com.devwithzachary.mineserve.model.FileEntry> {
        return serverRepository.listDirectory(serverId, relativePath)
    }

    suspend fun createFile(serverId: String, relativePath: String, fileName: String, content: String = ""): Boolean {
        val success = serverRepository.createFile(serverId, relativePath, fileName, content)
        if (success) loadServerDetails(serverId)
        return success
    }

    suspend fun createDirectory(serverId: String, relativePath: String, dirName: String): Boolean {
        val success = serverRepository.createDirectory(serverId, relativePath, dirName)
        if (success) loadServerDetails(serverId)
        return success
    }

    suspend fun deleteFile(serverId: String, relativePath: String): Boolean {
        val success = serverRepository.deleteFileOrDirectory(serverId, relativePath)
        if (success) loadServerDetails(serverId)
        return success
    }

    suspend fun renameFile(serverId: String, relativePath: String, newName: String): Boolean {
        val success = serverRepository.renameFileOrDirectory(serverId, relativePath, newName)
        if (success) loadServerDetails(serverId)
        return success
    }

    suspend fun duplicateFile(serverId: String, relativePath: String): Boolean {
        val success = serverRepository.duplicateFile(serverId, relativePath)
        if (success) loadServerDetails(serverId)
        return success
    }

    suspend fun readFile(serverId: String, relativePath: String): String {
        return serverRepository.readFile(serverId, relativePath)
    }

    suspend fun writeFile(serverId: String, relativePath: String, content: String): Boolean {
        val success = serverRepository.writeFile(serverId, relativePath, content)
        if (success && (relativePath == "server.properties" || relativePath.endsWith("/server.properties"))) {
            loadServerDetails(serverId)
        }
        return success
    }

    suspend fun importFile(serverId: String, relativePath: String, uri: android.net.Uri): Boolean {
        val success = serverRepository.importFile(serverId, relativePath, uri)
        if (success) loadServerDetails(serverId)
        return success
    }

    suspend fun exportFile(serverId: String, relativePath: String): Boolean {
        return serverRepository.exportFileToDownloads(serverId, relativePath)
    }

    suspend fun unzipFile(
        serverId: String,
        relativePath: String,
        destinationPath: String = "",
        deleteZipAfter: Boolean = false
    ): Result<Int> {
        val result = serverRepository.unzipFile(serverId, relativePath, destinationPath, deleteZipAfter)
        if (result.isSuccess) {
            loadServerDetails(serverId)
        }
        return result
    }

    suspend fun searchFiles(serverId: String, query: String): List<com.devwithzachary.mineserve.model.FileEntry> {
        return serverRepository.searchFiles(serverId, query)
    }

    suspend fun analyzeCrash(serverId: String): com.devwithzachary.mineserve.model.CrashDiagnosticReport? {
        val serverDir = serverRepository.getServerDirectory(serverId)
        return com.devwithzachary.mineserve.engine.CrashLogAnalyzer.analyzeServer(serverDir)
    }

    suspend fun applyQuickFix(serverId: String, action: com.devwithzachary.mineserve.model.QuickFixAction): Boolean {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val currentServer = servers.value.firstOrNull { it.id == serverId } ?: return false

        return when (action.actionType) {
            com.devwithzachary.mineserve.model.QuickFixType.ACCEPT_EULA -> {
                val eulaFile = File(serverDir, "eula.txt")
                try {
                    eulaFile.writeText("eula=true\n# Accepted via MineServe Quick Fix\n")
                    true
                } catch (e: Exception) {
                    false
                }
            }
            com.devwithzachary.mineserve.model.QuickFixType.CHANGE_JAVA_VERSION -> {
                val newVer = action.payload.toIntOrNull() ?: 21
                updateServer(currentServer.copy(javaVersion = newVer))
                true
            }
            com.devwithzachary.mineserve.model.QuickFixType.INCREASE_RAM -> {
                val newRam = action.payload.toIntOrNull() ?: 3072
                updateServer(currentServer.copy(allocatedRamMb = newRam))
                true
            }
            com.devwithzachary.mineserve.model.QuickFixType.CHANGE_PORT -> {
                val candidate = com.devwithzachary.mineserve.model.findNextAvailablePort(servers.value)
                updateServer(currentServer.copy(port = candidate))
                true
            }
            com.devwithzachary.mineserve.model.QuickFixType.DELETE_FILE -> {
                if (action.payload.isNotBlank()) {
                    deleteFile(serverId, action.payload)
                } else false
            }
            com.devwithzachary.mineserve.model.QuickFixType.OPEN_FILE_EDITOR -> true
        }
    }

    fun createBackup(serverId: String, isWorldOnly: Boolean, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val serverDir = serverRepository.getServerDirectory(serverId)
            val result = backupRepository.createBackup(serverDir, isWorldOnly)
            loadServerDetails(serverId)
            withContext(Dispatchers.Main) {
                onResult(result != null)
            }
        }
    }

    fun restoreBackup(serverId: String, backup: BackupEntry, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val serverDir = serverRepository.getServerDirectory(serverId)
            val backupFile = File(File(serverDir, "backups"), backup.fileName)
            val success = backupRepository.restoreBackup(serverDir, backupFile)
            loadServerDetails(serverId)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun exportBackup(backup: BackupEntry, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val serverDir = serverRepository.getServerDirectory(backup.serverId)
            val backupFile = File(File(serverDir, "backups"), backup.fileName)
            val path = backupRepository.exportBackupToDownloads(backupFile)
            withContext(Dispatchers.Main) {
                onResult(path)
            }
        }
    }

    fun getBackupShareIntent(backup: BackupEntry): Intent? {
        val serverDir = serverRepository.getServerDirectory(backup.serverId)
        val backupFile = File(File(serverDir, "backups"), backup.fileName)
        return backupRepository.getShareIntent(backupFile)
    }

    fun deleteBackup(serverId: String, backup: BackupEntry, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val serverDir = serverRepository.getServerDirectory(serverId)
            val backupFile = File(File(serverDir, "backups"), backup.fileName)
            val success = backupRepository.deleteBackup(backupFile)
            loadServerDetails(serverId)
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    fun togglePlugin(serverId: String, entry: PluginModEntry) {
        viewModelScope.launch {
            val serverDir = serverRepository.getServerDirectory(serverId)
            pluginRepository.togglePlugin(serverDir, entry)
            loadServerDetails(serverId)
        }
    }

    fun deletePlugin(serverId: String, entry: PluginModEntry) {
        viewModelScope.launch {
            val serverDir = serverRepository.getServerDirectory(serverId)
            pluginRepository.deletePlugin(serverDir, entry)
            loadServerDetails(serverId)
        }
    }

    fun installPluginOrMod(
        serverId: String,
        fileName: String,
        downloadUrl: String,
        isMod: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val serverDir = serverRepository.getServerDirectory(serverId)
                val targetFolder = if (isMod) File(serverDir, "mods") else File(serverDir, "plugins")
                if (!targetFolder.exists()) targetFolder.mkdirs()
                val dest = File(targetFolder, fileName)
                serverSoftwareManager.downloadFileWithProgress(downloadUrl, dest) { _, _ -> }
                loadServerDetails(serverId)
                onResult(true)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed installing plugin/mod", e)
                onResult(false)
            }
        }
    }

    fun importPluginOrMod(
        serverId: String,
        uri: android.net.Uri,
        isMod: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val serverDir = serverRepository.getServerDirectory(serverId)
                val ok = pluginRepository.importJarFromUri(
                    serverDir = serverDir,
                    uri = uri,
                    isMod = isMod,
                    contentResolver = getApplication<Application>().contentResolver
                )
                loadServerDetails(serverId)
                onResult(ok)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed importing JAR", e)
                onResult(false)
            }
        }
    }

    fun toggleTunnel(server: MinecraftServer) {
        tunnelManager.toggleTunnel(server)
    }

    fun updateTunnelConfig(server: MinecraftServer, config: TunnelConfig) {
        viewModelScope.launch {
            val updated = server.copy(tunnelConfig = config)
            serverRepository.updateServer(updated)
            if (config.enabled && !tunnelManager.isTunnelActive(server.id)) {
                tunnelManager.startTunnel(updated)
            } else if (!config.enabled && tunnelManager.isTunnelActive(server.id)) {
                tunnelManager.stopTunnel(server.id)
            }
        }
    }

    fun updateServer(server: MinecraftServer) {
        viewModelScope.launch {
            serverRepository.updateServer(server)
        }
    }

    fun toggleCheckGitHubUpdates(enabled: Boolean) {
        updatePreferences.isCheckGitHubUpdatesEnabled = enabled
        _isCheckGitHubUpdatesEnabled.value = enabled
        if (!enabled) {
            _availableUpdate.value = null
        }
    }

    fun checkForGitHubUpdates(isManual: Boolean = false) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            if (isManual) {
                _updateCheckStatus.value = "Checking GitHub for updates..."
            }
            val result = updateChecker.checkLatestRelease(BuildConfig.VERSION_NAME)
            _isCheckingUpdate.value = false

            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    if (isManual || updatePreferences.ignoredVersion != result.release.tagName) {
                        _availableUpdate.value = result.release
                    }
                    _updateCheckStatus.value = "Update available: ${result.release.tagName}"
                }
                is UpdateCheckResult.UpToDate -> {
                    _updateCheckStatus.value = "You are on the latest version (v${BuildConfig.VERSION_NAME})"
                }
                is UpdateCheckResult.Error -> {
                    if (isManual) {
                        _updateCheckStatus.value = "Check failed: ${result.message}"
                    }
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
    }

    fun disableGitHubUpdatePrompts() {
        toggleCheckGitHubUpdates(false)
        _availableUpdate.value = null
    }

    fun updateAutomationConfig(serverId: String, config: com.devwithzachary.mineserve.model.ServerAutomationConfig) {
        viewModelScope.launch {
            val currentServer = servers.value.firstOrNull { it.id == serverId } ?: return@launch
            val updated = currentServer.copy(automationConfig = config)
            serverRepository.updateServer(updated)
            if (!config.autoWakeOnPing && processManager.isServerInStandby(serverId)) {
                processManager.exitStandby(serverId)
            } else if (config.autoWakeOnPing && !processManager.isServerRunning(serverId) && !processManager.isServerInStandby(serverId)) {
                enterStandby(updated)
            }
        }
    }

    fun enterStandby(server: MinecraftServer) {
        processManager.enterStandby(server) { wokeServer ->
            startServer(wokeServer)
        }
    }

    fun exitStandby(serverId: String) {
        processManager.exitStandby(serverId)
    }

    fun isServerInStandby(serverId: String): Boolean {
        return processManager.isServerInStandby(serverId)
    }

    // World & Map Management

    suspend fun getWorldSummary(serverId: String): com.devwithzachary.mineserve.model.WorldSummary = withContext(Dispatchers.IO) {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val levelName = _serverPropertiesMap.value[serverId]?.levelName
            ?: serverRepository.loadServerProperties(serverId).levelName
        com.devwithzachary.mineserve.engine.WorldManager.getWorldSummary(serverDir, levelName)
    }

    suspend fun importWorld(
        serverId: String,
        uri: android.net.Uri,
        createBackup: Boolean,
        onProgress: (String, Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val levelName = _serverPropertiesMap.value[serverId]?.levelName
            ?: serverRepository.loadServerProperties(serverId).levelName

        // Stop server if running
        if (serverStatuses.value[serverId] == ServerStatus.RUNNING) {
            onProgress("Stopping server to safely import world...", 5)
            stopServer(serverId)
            while (serverStatuses.value[serverId] == ServerStatus.STOPPING) {
                delay(200)
            }
        }

        // Safety backup
        if (createBackup) {
            onProgress("Creating safety backup of current world...", 10)
            backupRepository.createBackup(
                serverDir = serverDir,
                isWorldOnly = true,
                customName = "pre_import_world_${System.currentTimeMillis()}"
            )
        }

        com.devwithzachary.mineserve.engine.WorldManager.importWorld(
            serverDir = serverDir,
            uri = uri,
            context = getApplication(),
            levelName = levelName,
            onProgress = onProgress
        )
    }

    suspend fun exportWorld(
        serverId: String,
        outputStream: java.io.OutputStream,
        onProgress: (String, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val levelName = _serverPropertiesMap.value[serverId]?.levelName
            ?: serverRepository.loadServerProperties(serverId).levelName
        com.devwithzachary.mineserve.engine.WorldManager.exportWorld(
            serverDir = serverDir,
            outputStream = outputStream,
            levelName = levelName,
            onProgress = onProgress
        )
    }

    suspend fun resetDimension(
        serverId: String,
        dimension: com.devwithzachary.mineserve.model.DimensionType,
        createBackup: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val levelName = _serverPropertiesMap.value[serverId]?.levelName
            ?: serverRepository.loadServerProperties(serverId).levelName

        // Stop server if running
        if (serverStatuses.value[serverId] == ServerStatus.RUNNING) {
            stopServer(serverId)
            while (serverStatuses.value[serverId] == ServerStatus.STOPPING) {
                delay(200)
            }
        }

        // Safety backup
        if (createBackup) {
            backupRepository.createBackup(
                serverDir = serverDir,
                isWorldOnly = true,
                customName = "pre_reset_${dimension.name.lowercase()}_${System.currentTimeMillis()}"
            )
        }

        com.devwithzachary.mineserve.engine.WorldManager.resetDimension(
            serverDir = serverDir,
            dimension = dimension,
            levelName = levelName
        )
    }

    suspend fun pruneChunks(
        serverId: String,
        options: com.devwithzachary.mineserve.model.ChunkPruneOptions,
        onProgress: (String, Int) -> Unit
    ): com.devwithzachary.mineserve.model.ChunkPruneResult = withContext(Dispatchers.IO) {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val levelName = _serverPropertiesMap.value[serverId]?.levelName
            ?: serverRepository.loadServerProperties(serverId).levelName

        // Stop server if running
        if (serverStatuses.value[serverId] == ServerStatus.RUNNING) {
            onProgress("Stopping server to safely prune chunks...", 2)
            stopServer(serverId)
            while (serverStatuses.value[serverId] == ServerStatus.STOPPING) {
                delay(200)
            }
        }

        // Safety backup
        if (options.createBackup) {
            onProgress("Creating safety backup before chunk pruning...", 5)
            backupRepository.createBackup(
                serverDir = serverDir,
                isWorldOnly = true,
                customName = "pre_prune_chunks_${System.currentTimeMillis()}"
            )
        }

        com.devwithzachary.mineserve.engine.ChunkOptimizer.optimizeWorld(
            serverDir = serverDir,
            options = options,
            levelName = levelName,
            onProgress = onProgress
        )
    }

    private val _webMapPorts = mutableMapOf<String, Int>()

    fun getWebMapState(serverId: String): com.devwithzachary.mineserve.model.WebMapState {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val installed = com.devwithzachary.mineserve.model.WebMapPluginType.detectInstalled(serverDir)
        val isRunning = serverStatuses.value[serverId] == ServerStatus.RUNNING
        val defaultPort = installed?.defaultPort ?: 8080
        val port = _webMapPorts[serverId] ?: defaultPort
        return com.devwithzachary.mineserve.model.WebMapState(
            installedPlugin = installed,
            port = port,
            isServerRunning = isRunning
        )
    }

    fun setWebMapPort(serverId: String, port: Int) {
        _webMapPorts[serverId] = port
    }

    fun installWebMapPlugin(
        serverId: String,
        pluginType: com.devwithzachary.mineserve.model.WebMapPluginType,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentServer = servers.value.firstOrNull { it.id == serverId }
                val isModServer = currentServer?.type?.supportsMods == true && currentServer.type.supportsPlugins != true
                val loader = when (currentServer?.type) {
                    ServerType.FABRIC -> "fabric"
                    ServerType.FORGE -> "forge"
                    ServerType.NEOFORGE -> "neoforge"
                    else -> "paper"
                }
                val modrinth = com.devwithzachary.mineserve.api.ModrinthApiClient()
                val resolved = modrinth.resolveDownloadUrl(
                    projectIdOrSlug = pluginType.modrinthSlug,
                    isMod = isModServer,
                    loaderFilter = loader,
                    gameVersion = currentServer?.version
                ) ?: modrinth.resolveDownloadUrl(
                    projectIdOrSlug = pluginType.modrinthSlug,
                    isMod = isModServer,
                    loaderFilter = null,
                    gameVersion = null
                )

                if (resolved == null) {
                    onResult(false)
                    return@launch
                }

                installPluginOrMod(
                    serverId = serverId,
                    fileName = resolved.first,
                    downloadUrl = resolved.second,
                    isMod = isModServer,
                    onResult = onResult
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed installing web map plugin", e)
                onResult(false)
            }
        }
    }

    fun uninstallWebMapPlugin(serverId: String): Boolean {
        val serverDir = serverRepository.getServerDirectory(serverId)
        val file = com.devwithzachary.mineserve.model.WebMapPluginType.findInstalledFile(serverDir)
        val deleted = file != null && file.exists() && file.delete()
        if (deleted) {
            loadServerDetails(serverId)
        }
        return deleted
    }
}
