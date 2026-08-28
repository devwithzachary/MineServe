package com.devwithzachary.mineserve.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devwithzachary.mineserve.api.FabricApiClient
import com.devwithzachary.mineserve.api.MojangApiClient
import com.devwithzachary.mineserve.api.PaperApiClient
import com.devwithzachary.mineserve.engine.JavaInstallState
import com.devwithzachary.mineserve.engine.JavaRuntimeManager
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.engine.RootfsManager
import com.devwithzachary.mineserve.engine.RootfsSetupState
import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelConfig
import com.devwithzachary.mineserve.repository.BackupRepository
import com.devwithzachary.mineserve.repository.PluginRepository
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.service.MineServeForegroundService
import com.devwithzachary.mineserve.tunnel.TunnelManager
import com.devwithzachary.mineserve.tunnel.TunnelState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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
    val tunnelManager = TunnelManager.getInstance(application)

    val servers: StateFlow<List<MinecraftServer>> = serverRepository.servers
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = processManager.serverStatuses
    val serverMetrics: StateFlow<Map<String, ServerMetrics>> = processManager.serverMetrics
    val refreshTriggers: StateFlow<Map<String, Long>> = processManager.refreshTriggers
    val tunnelStates: StateFlow<Map<String, TunnelState>> = tunnelManager.tunnelStates

    private val _isRootfsInstalled = MutableStateFlow(rootfsManager.isInstalled())
    val isRootfsInstalled: StateFlow<Boolean> = _isRootfsInstalled.asStateFlow()

    private val _rootfsSetupState = MutableStateFlow<RootfsSetupState>(RootfsSetupState.Idle)
    val rootfsSetupState: StateFlow<RootfsSetupState> = _rootfsSetupState.asStateFlow()

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

        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRootfsInstalled.value = rootfsManager.isInstalled()
            _storageUsedMb.value = rootfsManager.getStorageUsedMb()
            val loaded = serverRepository.loadServers()
            for (s in loaded) {
                loadServerDetails(s.id)
            }
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
            _isRootfsInstalled.value = false
            rootfsManager.setupRootfs().collect { state ->
                _rootfsSetupState.value = state
                if (state is RootfsSetupState.Success) {
                    _isRootfsInstalled.value = true
                    // Install Java automatically
                    installJava(21)
                    refreshData()
                } else if (state is RootfsSetupState.Error) {
                    _isRootfsInstalled.value = false
                }
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
        Log.d(TAG, "startServer: requested start for server ${server.name} (${server.id})")
        val serverDir = serverRepository.getServerDirectory(server.id)
        MineServeForegroundService.start(getApplication())
        processManager.startServer(server, serverDir)
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
            val jarUrl = when (type) {
                ServerType.PAPER, ServerType.BEDROCK_GEYSER -> {
                    com.devwithzachary.mineserve.api.PaperApiClient().getLatestBuildDownloadUrl("paper", version)
                }
                ServerType.PURPUR -> {
                    com.devwithzachary.mineserve.api.PurpurApiClient().getDownloadUrl(version)
                }
                ServerType.FOLIA -> {
                    com.devwithzachary.mineserve.api.PaperApiClient().getLatestBuildDownloadUrl("folia", version)
                }
                ServerType.VANILLA -> {
                    com.devwithzachary.mineserve.api.MojangApiClient().getServerJarDownloadUrl(version)
                }
                ServerType.FABRIC -> {
                    com.devwithzachary.mineserve.api.FabricApiClient().getFabricServerJarUrl(version)
                }
                ServerType.NEOFORGE -> {
                    com.devwithzachary.mineserve.api.NeoForgeApiClient().getDownloadUrl(version)
                }
                else -> null
            }

            val server = serverRepository.createServer(
                name = name,
                type = type,
                version = version,
                port = port,
                ramMb = ramMb,
                motd = motd,
                jarFileName = "server.jar"
            )

            val serverDir = serverRepository.getServerDirectory(server.id)
            val destJar = File(serverDir, "server.jar")

            if (!jarUrl.isNullOrEmpty()) {
                onProgress("Downloading server.jar...", 30)
                downloadFileWithProgress(jarUrl, destJar) { bytesRead, totalBytes ->
                    val percent = if (totalBytes > 0) 30 + ((bytesRead * 60) / totalBytes).toInt() else 50
                    onProgress("Downloading server.jar (${bytesRead / (1024 * 1024)} MB)...", percent)
                }
            } else {
                // Create dummy/placeholder jar if URL couldn't be resolved
                destJar.writeBytes(ByteArray(1024))
            }

            // If Geyser cross-play requested, download Geyser plugin
            if (type == ServerType.BEDROCK_GEYSER) {
                onProgress("Bundling GeyserMC for Bedrock cross-play...", 90)
                try {
                    val geyserUrl = "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
                    val geyserDest = File(File(serverDir, "plugins"), "Geyser-Spigot.jar")
                    downloadFileWithProgress(geyserUrl, geyserDest) { _, _ -> }
                } catch (_: Exception) {}
            }

            onProgress("Server created successfully!", 100)
            loadServerDetails(server.id)
            server
        } catch (e: Exception) {
            Log.e(TAG, "Error creating server", e)
            null
        }
    }

    private suspend fun downloadFileWithProgress(
        fileUrl: String,
        destFile: File,
        onProgress: (Long, Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        var currentUrl = fileUrl
        var redirects = 0
        while (redirects < 5) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15000
            conn.readTimeout = 60000
            conn.setRequestProperty("User-Agent", "MineServe-Android (https://github.com/devwithzachary/mineserve)")
            conn.connect()

            val responseCode = conn.responseCode
            if (responseCode in 300..399) {
                val newUrl = conn.getHeaderField("Location")
                if (!newUrl.isNullOrEmpty()) {
                    currentUrl = newUrl
                    redirects++
                    continue
                }
            }

            if (responseCode !in 200..299) {
                throw IOException("Server returned HTTP $responseCode for $currentUrl")
            }

            val contentLength = conn.contentLength.toLong()
            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(65536)
            var totalRead = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                totalRead += read
                outputStream.write(buffer, 0, read)
                onProgress(totalRead, contentLength)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            break
        }
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
                downloadFileWithProgress(downloadUrl, dest) { _, _ -> }
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
}
