package com.devwithzachary.mineserve.desktop.ui

import com.devwithzachary.mineserve.api.ModrinthApiClient
import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.designsystem.theme.ThemeMode
import com.devwithzachary.mineserve.desktop.DesktopCloudManager
import com.devwithzachary.mineserve.desktop.DesktopDataPaths
import com.devwithzachary.mineserve.desktop.DesktopDiagnosticsManager
import com.devwithzachary.mineserve.desktop.DesktopFirewallChecker
import com.devwithzachary.mineserve.desktop.DesktopHostTelemetry
import com.devwithzachary.mineserve.desktop.DesktopNetworkManager
import com.devwithzachary.mineserve.desktop.DesktopPortForwardState
import com.devwithzachary.mineserve.desktop.DesktopProcessManager
import com.devwithzachary.mineserve.desktop.DesktopServerRepository
import com.devwithzachary.mineserve.desktop.DesktopServerSoftwareManager
import com.devwithzachary.mineserve.desktop.DesktopTunnelState
import com.devwithzachary.mineserve.desktop.DesktopWebServer
import com.devwithzachary.mineserve.desktop.PortableJavaManager
import com.devwithzachary.mineserve.engine.ChunkOptimizer
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.BannedIpEntry
import com.devwithzachary.mineserve.model.BannedPlayerEntry
import com.devwithzachary.mineserve.model.ChunkPruneOptions
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.OpEntry
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ServerPlayerLists
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.model.WhitelistEntry
import com.devwithzachary.mineserve.net.portforward.PortForwardProtocol
import com.devwithzachary.mineserve.util.FileStorageUtils
import com.devwithzachary.mineserve.util.ServerModFilter
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class DesktopTab(val title: String) {
    CONSOLE("Console"),
    PERFORMANCE("Performance"),
    PLAYERS("Players"),
    CONTENT("Content"),
    WORLD_MAP("World & Map"),
    MANAGEMENT("Management")
}

enum class DesktopContentSection(val title: String) {
    PLUGINS("Plugins / Mods"),
    FILES("Files")
}

enum class DesktopWorldSection(val title: String) {
    WORLD("World"),
    LIVE_MAP("Live Map")
}

enum class DesktopManagementSection(val title: String) {
    SETTINGS("Network & JVM"),
    BACKUPS("Backups"),
    AUTOMATION("Automation")
}

enum class DesktopModal {
    NONE,
    CREATE_SERVER,
    IMPORT_SERVER,
    CLOUD,
    HOST_TOOLS,
    USERS_AUDIT,
    ABOUT_CREDITS,
    WHATS_NEW
}

data class DesktopRegionFileEntry(
    val dimension: DimensionType,
    val fileName: String,
    val sizeBytes: Long,
    val estimatedChunks: Int
)

data class DesktopDimensionSummary(
    val dimension: DimensionType,
    val exists: Boolean,
    val sizeBytes: Long,
    val regionCount: Int,
    val estimatedChunks: Int
)

class DesktopViewModel(
    val paths: DesktopDataPaths,
    val serverRepository: DesktopServerRepository,
    val processManager: DesktopProcessManager,
    val softwareManager: DesktopServerSoftwareManager,
    val javaManager: PortableJavaManager,
    val networkManager: DesktopNetworkManager,
    val cloudManager: DesktopCloudManager,
    val userRepository: UserRepository,
    val webServer: DesktopWebServer
) {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val modrinthApi = ModrinthApiClient()

    private val _selectedServerId = MutableStateFlow<String?>(serverRepository.servers.value.firstOrNull()?.id)
    val selectedServerId: StateFlow<String?> = _selectedServerId.asStateFlow()

    private val _activeTab = MutableStateFlow(DesktopTab.CONSOLE)
    val activeTab: StateFlow<DesktopTab> = _activeTab.asStateFlow()

    private val _contentSection = MutableStateFlow(DesktopContentSection.PLUGINS)
    val contentSection: StateFlow<DesktopContentSection> = _contentSection.asStateFlow()

    private val _worldSection = MutableStateFlow(DesktopWorldSection.WORLD)
    val worldSection: StateFlow<DesktopWorldSection> = _worldSection.asStateFlow()

    private val _managementSection = MutableStateFlow(DesktopManagementSection.SETTINGS)
    val managementSection: StateFlow<DesktopManagementSection> = _managementSection.asStateFlow()

    private val _activeModal = MutableStateFlow(DesktopModal.NONE)
    val activeModal: StateFlow<DesktopModal> = _activeModal.asStateFlow()

    private val _sidebarCollapsed = MutableStateFlow(false)
    val sidebarCollapsed: StateFlow<Boolean> = _sidebarCollapsed.asStateFlow()

    private val uiSettingsFile = File(paths.rootDir, "desktop_ui.json")
    private val _uiScale = MutableStateFlow(loadInitialUiScale())
    val uiScale: StateFlow<Float> = _uiScale.asStateFlow()

    private val _themeMode = MutableStateFlow(loadInitialThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showWhatsNewOnUpdate = MutableStateFlow(DesktopDiagnosticsManager.isShowWhatsNewOnUpdateEnabled(paths))
    val showWhatsNewOnUpdate: StateFlow<Boolean> = _showWhatsNewOnUpdate.asStateFlow()

    private val _webAdminRunning = MutableStateFlow(webServer.isRunning)
    val webAdminRunning: StateFlow<Boolean> = _webAdminRunning.asStateFlow()

    private val _serversDirPath = MutableStateFlow(paths.serversDir.absolutePath)
    val serversDirPath: StateFlow<String> = _serversDirPath.asStateFlow()

    private val _runtimesDirPath = MutableStateFlow(paths.runtimesDir.absolutePath)
    val runtimesDirPath: StateFlow<String> = _runtimesDirPath.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // File Manager State
    private val _currentFilePath = MutableStateFlow("")
    val currentFilePath: StateFlow<String> = _currentFilePath.asStateFlow()

    private val _directoryEntries = MutableStateFlow<List<FileEntry>>(emptyList())
    val directoryEntries: StateFlow<List<FileEntry>> = _directoryEntries.asStateFlow()

    private val _editingFilePath = MutableStateFlow<String?>(null)
    val editingFilePath: StateFlow<String?> = _editingFilePath.asStateFlow()

    private val _editingFileContent = MutableStateFlow("")
    val editingFileContent: StateFlow<String> = _editingFileContent.asStateFlow()

    // Backups State
    private val _localBackups = MutableStateFlow<List<BackupEntry>>(emptyList())
    val localBackups: StateFlow<List<BackupEntry>> = _localBackups.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<JSONObject>>(emptyList())
    val cloudBackups: StateFlow<List<JSONObject>> = _cloudBackups.asStateFlow()

    // Plugins / Mods State
    private val _installedPlugins = MutableStateFlow<List<File>>(emptyList())
    val installedPlugins: StateFlow<List<File>> = _installedPlugins.asStateFlow()

    private val _modrinthResults = MutableStateFlow<List<PluginModEntry>>(emptyList())
    val modrinthResults: StateFlow<List<PluginModEntry>> = _modrinthResults.asStateFlow()

    private val _modrinthBusy = MutableStateFlow(false)
    val modrinthBusy: StateFlow<Boolean> = _modrinthBusy.asStateFlow()

    // World & Live Map State
    private val _worldDimensions = MutableStateFlow<List<DesktopDimensionSummary>>(emptyList())
    val worldDimensions: StateFlow<List<DesktopDimensionSummary>> = _worldDimensions.asStateFlow()

    private val _regionFiles = MutableStateFlow<List<DesktopRegionFileEntry>>(emptyList())
    val regionFiles: StateFlow<List<DesktopRegionFileEntry>> = _regionFiles.asStateFlow()

    private val _squaremapInstalled = MutableStateFlow(false)
    val squaremapInstalled: StateFlow<Boolean> = _squaremapInstalled.asStateFlow()

    private val _squaremapPort = MutableStateFlow(WebMapPluginType.SQUAREMAP.defaultPort)
    val squaremapPort: StateFlow<Int> = _squaremapPort.asStateFlow()

    // Network State Snapshot
    private val _tunnelState = MutableStateFlow<DesktopTunnelState>(DesktopTunnelState.Disconnected)
    val tunnelState: StateFlow<DesktopTunnelState> = _tunnelState.asStateFlow()

    private val _portForwardState = MutableStateFlow<DesktopPortForwardState>(DesktopPortForwardState.Idle)
    val portForwardState: StateFlow<DesktopPortForwardState> = _portForwardState.asStateFlow()

    // Async Operation Status (Create / Upgrade / Modpack / Java)
    private val _operationBusy = MutableStateFlow(false)
    val operationBusy: StateFlow<Boolean> = _operationBusy.asStateFlow()

    private val _operationProgress = MutableStateFlow(0)
    val operationProgress: StateFlow<Int> = _operationProgress.asStateFlow()

    private val _operationStatus = MutableStateFlow("")
    val operationStatus: StateFlow<String> = _operationStatus.asStateFlow()

    private var refreshJob: Job? = null

    private fun isPathInside(target: File, root: File): Boolean {
        val rootPath = root.canonicalPath
        val targetPath = target.canonicalPath
        return targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
    }

    init {
        _selectedServerId.value?.let { refreshSelectedServerData(it) }
        val hasExistingServers = serverRepository.servers.value.isNotEmpty()
        if (DesktopDiagnosticsManager.shouldAutoShowWhatsNew(paths, hasExistingServers)) {
            DesktopDiagnosticsManager.markWhatsNewSeen(paths)
            _activeModal.value = DesktopModal.WHATS_NEW
        }
        scope.launch {
            serverRepository.servers.collect { list ->
                val cur = _selectedServerId.value
                if (list.isEmpty()) {
                    if (cur != null) _selectedServerId.value = null
                    _currentFilePath.value = ""
                    _editingFilePath.value = null
                    _directoryEntries.value = emptyList()
                    _localBackups.value = emptyList()
                    _installedPlugins.value = emptyList()
                    _worldDimensions.value = emptyList()
                    _regionFiles.value = emptyList()
                    _squaremapInstalled.value = false
                } else if (cur == null || list.none { it.id == cur }) {
                    val nextId = list.first().id
                    _selectedServerId.value = nextId
                    _currentFilePath.value = ""
                    _editingFilePath.value = null
                    refreshSelectedServerData(nextId)
                }
            }
        }
        scope.launch {
            while (true) {
                _selectedServerId.value?.let { sid ->
                    _tunnelState.value = networkManager.getTunnelState(sid)
                    _portForwardState.value = networkManager.getPortForwardState(sid)
                }
                _showWhatsNewOnUpdate.value = DesktopDiagnosticsManager.isShowWhatsNewOnUpdateEnabled(paths)
                delay(1200)
            }
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
        scope.launch {
            delay(3500)
            if (_toastMessage.value == message) _toastMessage.value = null
        }
    }

    fun toggleSidebar() {
        _sidebarCollapsed.value = !_sidebarCollapsed.value
    }

    private fun loadInitialUiScale(): Float {
        runCatching {
            if (uiSettingsFile.exists()) {
                val obj = JSONObject(uiSettingsFile.readText())
                val saved = obj.optDouble("uiScale", Double.NaN)
                if (!saved.isNaN()) {
                    return saved.toFloat().coerceIn(0.85f, 2.0f)
                }
            }
        }
        // Auto-detect HiDPI / 2560x1600+ displays on Linux Wayland/XWayland where AWT defaults to 1.0x
        val screenWidth = runCatching { Toolkit.getDefaultToolkit().screenSize.width }.getOrDefault(1920)
        return when {
            screenWidth >= 2400 -> 1.30f
            screenWidth >= 1900 -> 1.15f
            else -> 1.0f
        }
    }

    fun setUiScale(newScale: Float) {
        val clamped = ((newScale * 20f).toInt() / 20f).coerceIn(0.85f, 2.0f)
        _uiScale.value = clamped
        runCatching {
            val obj = if (uiSettingsFile.exists()) JSONObject(uiSettingsFile.readText()) else JSONObject()
            obj.put("uiScale", clamped.toDouble())
            uiSettingsFile.writeText(obj.toString(2))
        }
    }

    private fun loadInitialThemeMode(): ThemeMode {
        return runCatching {
            if (uiSettingsFile.exists()) {
                val obj = JSONObject(uiSettingsFile.readText())
                val raw = obj.optString("themeMode", "")
                ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.ObsidianDark
            } else {
                ThemeMode.ObsidianDark
            }
        }.getOrDefault(ThemeMode.ObsidianDark)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        runCatching {
            val obj = if (uiSettingsFile.exists()) JSONObject(uiSettingsFile.readText()) else JSONObject()
            obj.put("themeMode", mode.name)
            uiSettingsFile.writeText(obj.toString(2))
        }
    }

    fun updateHostDirectories(serversPath: String?, runtimesPath: String?) {
        val anyActive = processManager.serverStatuses.value.values.any {
            it == com.devwithzachary.mineserve.model.ServerStatus.RUNNING ||
                it == com.devwithzachary.mineserve.model.ServerStatus.STARTING
        }
        if (anyActive) {
            showToast("Stop all running servers before changing host directories.")
            return
        }
        paths.updateDirectories(serversPath, runtimesPath).fold(
            onSuccess = { (srvDir, rtDir) ->
                _serversDirPath.value = srvDir.absolutePath
                _runtimesDirPath.value = rtDir.absolutePath
                showToast("Updated host directories.")
            },
            onFailure = { err ->
                showToast(err.message ?: "Failed to update host directories.")
            }
        )
    }

    fun setShowWhatsNewOnUpdate(enabled: Boolean) {
        _showWhatsNewOnUpdate.value = enabled
        DesktopDiagnosticsManager.setShowWhatsNewOnUpdateEnabled(paths, enabled)
        DesktopDiagnosticsManager.markWhatsNewSeen(paths)
    }

    fun openWhatsNewGuide() {
        DesktopDiagnosticsManager.markWhatsNewSeen(paths)
        _activeModal.value = DesktopModal.WHATS_NEW
    }

    fun adjustUiScale(delta: Float) {
        setUiScale(_uiScale.value + delta)
    }

    fun setWebAdminEnabled(enabled: Boolean) {
        if (enabled) {
            val ok = webServer.start()
            _webAdminRunning.value = webServer.isRunning
            showToast(if (ok) "Web Remote Admin started on port ${webServer.actualPort}" else "Failed to start Web Remote Admin")
        } else {
            webServer.stop()
            _webAdminRunning.value = webServer.isRunning
            showToast("Web Remote Admin stopped")
        }
    }

    fun openModal(modal: DesktopModal) {
        _activeModal.value = modal
    }

    fun selectServer(serverId: String) {
        _selectedServerId.value = serverId
        _currentFilePath.value = ""
        _editingFilePath.value = null
        refreshSelectedServerData(serverId)
    }

    fun selectTab(tab: DesktopTab) {
        _activeTab.value = tab
        _selectedServerId.value?.let { refreshSelectedServerData(it) }
    }

    fun selectContentSection(section: DesktopContentSection) {
        _contentSection.value = section
    }

    fun selectWorldSection(section: DesktopWorldSection) {
        _worldSection.value = section
    }

    fun selectManagementSection(section: DesktopManagementSection) {
        _managementSection.value = section
    }

    fun openServerSettings() {
        _activeTab.value = DesktopTab.MANAGEMENT
        _managementSection.value = DesktopManagementSection.SETTINGS
        _selectedServerId.value?.let { refreshSelectedServerData(it) }
    }

    fun getSelectedServer(): MinecraftServer? {
        val id = _selectedServerId.value ?: return null
        return serverRepository.getServer(id)
    }

    fun refreshSelectedServerData(serverId: String = _selectedServerId.value.orEmpty()) {
        if (serverId.isBlank()) return
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val srv = serverRepository.getServer(serverId) ?: return@launch
            _tunnelState.value = networkManager.getTunnelState(serverId)
            _portForwardState.value = networkManager.getPortForwardState(serverId)
            loadDirectory(serverId, _currentFilePath.value)
            _localBackups.value = serverRepository.listBackups(serverId)
            refreshInstalledPlugins(srv)
            refreshWorldSummary(srv)
            refreshLiveMap(srv)
        }
    }

    // --- File Manager Actions ---

    fun loadDirectory(serverId: String, relativePath: String) {
        _currentFilePath.value = relativePath
        _directoryEntries.value = serverRepository.listDirectory(serverId, relativePath)
    }

    fun importServerFile(serverId: String, relativeDir: String, sourceFile: File): Boolean {
        if (!sourceFile.exists() || !sourceFile.isFile) return false
        val serverDir = serverRepository.getServerDirectory(serverId)
        val targetDir = if (relativeDir.isBlank()) serverDir else File(serverDir, relativeDir)
        if (!isPathInside(targetDir, serverDir)) return false
        targetDir.mkdirs()
        val destFile = File(targetDir, sourceFile.name)
        if (!isPathInside(destFile, serverDir)) return false
        return runCatching {
            sourceFile.copyTo(destFile, overwrite = true)
            loadDirectory(serverId, relativeDir)
            showToast("Uploaded ${sourceFile.name}")
            true
        }.getOrDefault(false)
    }

    fun loadPlayerLists(serverId: String): ServerPlayerLists {
        val dir = serverRepository.getServerDirectory(serverId)
        val whitelist = runCatching {
            val f = File(dir, "whitelist.json")
            if (!f.exists()) emptyList() else {
                val arr = JSONArray(f.readText())
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    WhitelistEntry(uuid = o.optString("uuid", ""), name = o.optString("name", ""))
                }.filter { it.name.isNotBlank() }
            }
        }.getOrDefault(emptyList())

        val ops = runCatching {
            val f = File(dir, "ops.json")
            if (!f.exists()) emptyList() else {
                val arr = JSONArray(f.readText())
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    OpEntry(
                        uuid = o.optString("uuid", ""),
                        name = o.optString("name", ""),
                        level = o.optInt("level", 4),
                        bypassesPlayerLimit = o.optBoolean("bypassesPlayerLimit", false)
                    )
                }.filter { it.name.isNotBlank() }
            }
        }.getOrDefault(emptyList())

        val bannedPlayers = runCatching {
            val f = File(dir, "banned-players.json")
            if (!f.exists()) emptyList() else {
                val arr = JSONArray(f.readText())
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    BannedPlayerEntry(
                        uuid = o.optString("uuid", ""),
                        name = o.optString("name", ""),
                        created = o.optString("created", ""),
                        source = o.optString("source", "Server"),
                        expires = o.optString("expires", "forever"),
                        reason = o.optString("reason", "Banned by an operator.")
                    )
                }.filter { it.name.isNotBlank() }
            }
        }.getOrDefault(emptyList())

        val bannedIps = runCatching {
            val f = File(dir, "banned-ips.json")
            if (!f.exists()) emptyList() else {
                val arr = JSONArray(f.readText())
                (0 until arr.length()).mapNotNull { i ->
                    val o = arr.optJSONObject(i) ?: return@mapNotNull null
                    BannedIpEntry(
                        ip = o.optString("ip", ""),
                        created = o.optString("created", ""),
                        source = o.optString("source", "Server"),
                        expires = o.optString("expires", "forever"),
                        reason = o.optString("reason", "Banned by an operator.")
                    )
                }.filter { it.ip.isNotBlank() }
            }
        }.getOrDefault(emptyList())

        return ServerPlayerLists(
            whitelist = whitelist,
            ops = ops,
            bannedPlayers = bannedPlayers,
            bannedIps = bannedIps
        )
    }

    fun openFileForEditing(serverId: String, relativePath: String) {
        _editingFilePath.value = relativePath
        _editingFileContent.value = serverRepository.readFile(serverId, relativePath)
    }

    fun updateEditingContent(newContent: String) {
        _editingFileContent.value = newContent
    }

    fun saveEditingFile() {
        val sid = _selectedServerId.value ?: return
        val path = _editingFilePath.value ?: return
        val ok = serverRepository.writeFile(sid, path, _editingFileContent.value)
        showToast(if (ok) "Saved $path" else "Failed to save $path")
    }

    fun createNewFile(name: String) {
        val sid = _selectedServerId.value ?: return
        val ok = serverRepository.createFile(sid, _currentFilePath.value, name, "")
        if (ok) {
            loadDirectory(sid, _currentFilePath.value)
            val rel = if (_currentFilePath.value.isBlank()) name else "${_currentFilePath.value}/$name"
            openFileForEditing(sid, rel)
            showToast("Created file $name")
        } else {
            showToast("Could not create file $name")
        }
    }

    fun createNewFolder(name: String) {
        val sid = _selectedServerId.value ?: return
        val ok = serverRepository.createDirectory(sid, _currentFilePath.value, name)
        if (ok) {
            loadDirectory(sid, _currentFilePath.value)
            showToast("Created folder $name")
        } else {
            showToast("Could not create folder $name")
        }
    }

    fun renameEntry(relativePath: String, newName: String) {
        val sid = _selectedServerId.value ?: return
        if (serverRepository.renameFileOrDirectory(sid, relativePath, newName)) {
            loadDirectory(sid, _currentFilePath.value)
            showToast("Renamed to $newName")
        } else {
            showToast("Rename failed")
        }
    }

    fun deleteEntry(relativePath: String) {
        val sid = _selectedServerId.value ?: return
        if (serverRepository.deleteFileOrDirectory(sid, relativePath)) {
            if (_editingFilePath.value == relativePath) _editingFilePath.value = null
            loadDirectory(sid, _currentFilePath.value)
            showToast("Deleted $relativePath")
        }
    }

    fun unzipEntry(relativePath: String) {
        val sid = _selectedServerId.value ?: return
        serverRepository.unzipFile(sid, relativePath, deleteZipAfter = false).fold(
            onSuccess = { count ->
                loadDirectory(sid, _currentFilePath.value)
                showToast("Extracted $count files")
            },
            onFailure = { err -> showToast(err.message ?: "Unzip failed") }
        )
    }

    // --- Backups Actions ---

    fun createBackup(worldOnly: Boolean) {
        val sid = _selectedServerId.value ?: return
        scope.launch {
            showToast("Creating ${if (worldOnly) "world" else "full"} backup...")
            serverRepository.createBackup(sid, worldOnly).fold(
                onSuccess = { b ->
                    _localBackups.value = serverRepository.listBackups(sid)
                    serverRepository.getServer(sid)?.let { srv ->
                        com.devwithzachary.mineserve.net.discord.DiscordWebhookClient.notifyBackupCompleted(
                            srv,
                            if (worldOnly) "World Backup" else "Full Server Backup",
                            b.fileName
                        )
                    }
                    showToast("Backup created: ${b.fileName}")
                },
                onFailure = { err -> showToast("Backup failed: ${err.message}") }
            )
        }
    }

    fun restoreBackup(fileName: String) {
        val sid = _selectedServerId.value ?: return
        if (processManager.isServerRunning(sid)) {
            showToast("Stop the server before restoring a backup.")
            return
        }
        scope.launch {
            serverRepository.restoreBackup(sid, fileName).fold(
                onSuccess = { showToast("Restored backup $fileName") },
                onFailure = { err -> showToast("Restore failed: ${err.message}") }
            )
        }
    }

    fun deleteBackup(fileName: String) {
        val sid = _selectedServerId.value ?: return
        if (serverRepository.deleteBackup(sid, fileName)) {
            _localBackups.value = serverRepository.listBackups(sid)
            showToast("Deleted backup $fileName")
        }
    }

    fun refreshCloudBackups() {
        val sid = _selectedServerId.value ?: return
        if (!cloudManager.isLinked) {
            _cloudBackups.value = emptyList()
            return
        }
        scope.launch {
            cloudManager.listCloudBackups(sid).onSuccess { arr ->
                val list = mutableListOf<JSONObject>()
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { list.add(it) }
                }
                _cloudBackups.value = list
            }
        }
    }

    fun uploadBackupToCloud(fileName: String) {
        val sid = _selectedServerId.value ?: return
        val file = File(serverRepository.getBackupsDirectory(sid), fileName)
        if (!file.exists()) return
        scope.launch {
            showToast("Uploading $fileName to MineServe Cloud...")
            cloudManager.uploadBackupToCloud(sid, file).fold(
                onSuccess = { msg ->
                    refreshCloudBackups()
                    showToast(msg)
                },
                onFailure = { err -> showToast(err.message ?: "Cloud upload failed") }
            )
        }
    }

    // --- Plugins / Mods / Modrinth / .mrpack ---

    fun refreshInstalledPlugins(server: MinecraftServer) {
        val folder = if (server.type.supportsMods && !server.type.supportsPlugins) "mods" else "plugins"
        val dir = File(serverRepository.getServerDirectory(server.id), folder).apply { mkdirs() }
        _installedPlugins.value = (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && (it.name.endsWith(".jar", true) || it.name.endsWith(".jar.disabled", true)) }
            .sortedBy { it.name.lowercase() }
    }

    fun togglePlugin(file: File) {
        val srv = getSelectedServer() ?: return
        val dst = if (file.name.endsWith(".disabled")) {
            File(file.parentFile, file.name.removeSuffix(".disabled"))
        } else {
            File(file.parentFile, "${file.name}.disabled")
        }
        if (file.renameTo(dst)) {
            refreshInstalledPlugins(srv)
        }
    }

    fun deletePlugin(file: File) {
        val srv = getSelectedServer() ?: return
        if (file.delete()) {
            refreshInstalledPlugins(srv)
            showToast("Deleted ${file.name}")
        }
    }

    fun searchModrinth(query: String) {
        val srv = getSelectedServer() ?: return
        val isMod = srv.type.supportsMods && !srv.type.supportsPlugins
        _modrinthBusy.value = true
        scope.launch {
            try {
                _modrinthResults.value = modrinthApi.search(query, isMod = isMod, loaderFilter = srv.type.name.lowercase(), gameVersion = srv.version)
            } finally {
                _modrinthBusy.value = false
            }
        }
    }

    fun installModrinthProject(projectId: String) {
        val srv = getSelectedServer() ?: return
        val isMod = srv.type.supportsMods && !srv.type.supportsPlugins
        val folder = if (isMod) "mods" else "plugins"
        val dir = File(serverRepository.getServerDirectory(srv.id), folder).apply { mkdirs() }
        _modrinthBusy.value = true
        scope.launch {
            try {
                val resolved = modrinthApi.resolveDownloadUrl(projectId, isMod, srv.type.name.lowercase(), srv.version)
                if (resolved == null) {
                    showToast("No compatible version found on Modrinth.")
                } else {
                    val (fileName, url) = resolved
                    if (softwareManager.downloadFile(url, File(dir, fileName))) {
                        refreshInstalledPlugins(srv)
                        showToast("Installed $fileName")
                    }
                }
            } finally {
                _modrinthBusy.value = false
            }
        }
    }

    fun importMrpackFile(mrpackFile: File) {
        val srv = getSelectedServer() ?: return
        if (processManager.isServerRunning(srv.id)) {
            showToast("Stop the server before importing a .mrpack modpack.")
            return
        }
        val serverDir = serverRepository.getServerDirectory(srv.id)
        _operationBusy.value = true
        _operationProgress.value = 5
        _operationStatus.value = "Reading ${mrpackFile.name}..."
        scope.launch {
            try {
                var indexJsonStr: String? = null
                ZipInputStream(BufferedInputStream(FileInputStream(mrpackFile))).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/')
                        if (name == "modrinth.index.json") {
                            indexJsonStr = zis.bufferedReader(Charsets.UTF_8).readText()
                        } else if ((name.startsWith("overrides/") || name.startsWith("server-overrides/")) && !entry.isDirectory) {
                            val rel = if (name.startsWith("server-overrides/")) name.removePrefix("server-overrides/") else name.removePrefix("overrides/")
                            val target = File(serverDir, rel)
                            if (isPathInside(target, serverDir)) {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { zis.copyTo(it) }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                val rawIndex = indexJsonStr ?: throw IllegalArgumentException("modrinth.index.json missing in .mrpack")
                val indexObj = JSONObject(rawIndex)
                val packName = indexObj.optString("name", "Modpack")
                val filesArr = indexObj.optJSONArray("files") ?: JSONArray()
                val toDownload = mutableListOf<Pair<String, String>>()
                for (i in 0 until filesArr.length()) {
                    val fObj = filesArr.optJSONObject(i) ?: continue
                    val serverEnv = fObj.optJSONObject("env")?.optString("server", "required") ?: "required"
                    if (serverEnv.equals("unsupported", ignoreCase = true)) continue
                    val path = fObj.optString("path", "").trim()
                    val url = fObj.optJSONArray("downloads")?.optString(0, "").orEmpty()
                    if (path.isNotBlank() && url.isNotBlank()) toDownload.add(path to url)
                }
                val total = toDownload.size.coerceAtLeast(1)
                for ((idx, pair) in toDownload.withIndex()) {
                    val dest = File(serverDir, pair.first)
                    if (!isPathInside(dest, serverDir)) continue
                    dest.parentFile?.mkdirs()
                    _operationStatus.value = "Downloading ${dest.name} (${idx + 1}/${toDownload.size})..."
                    _operationProgress.value = (((idx + 1) * 95) / total).coerceIn(5, 95)
                    runCatching { softwareManager.downloadFile(pair.second, dest) }
                }
                val disabled = ServerModFilter.disableClientOnlyMods(serverDir)
                refreshInstalledPlugins(srv)
                showToast("Imported $packName (${toDownload.size} files, auto-disabled ${disabled.size} client-only mods)")
            } catch (e: Exception) {
                showToast("Modpack import failed: ${e.message}")
            } finally {
                _operationBusy.value = false
                _operationStatus.value = ""
            }
        }
    }

    // --- World & Live Map ---

    fun refreshWorldSummary(server: MinecraftServer) {
        val serverDir = serverRepository.getServerDirectory(server.id)
        val levelName = serverRepository.loadServerProperties(server.id).levelName.ifBlank { "world" }
        val allRegions = mutableListOf<DesktopRegionFileEntry>()
        _worldDimensions.value = DimensionType.entries.map { dim ->
            val dDir = dim.getDimensionDir(serverDir, levelName)
            val rDir = when (dim) {
                DimensionType.OVERWORLD -> File(dDir, "region")
                DimensionType.NETHER -> File(dDir, "DIM-1/region").takeIf { it.exists() } ?: File(dDir, "region")
                DimensionType.THE_END -> File(dDir, "DIM1/region").takeIf { it.exists() } ?: File(dDir, "region")
            }
            val exists = dDir.exists() && dDir.isDirectory
            val size = if (exists) FileStorageUtils.calculateDirectorySize(dDir) else 0L
            val mca = if (rDir.exists()) (rDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mca") } ?: emptyList()) else emptyList()
            var dimChunks = 0
            mca.forEach { f ->
                val c = ((f.length() / 4096L).toInt()).coerceIn(0, 1024)
                dimChunks += c
                allRegions.add(DesktopRegionFileEntry(dim, f.name, f.length(), c))
            }
            DesktopDimensionSummary(dim, exists, size, mca.size, dimChunks)
        }
        _regionFiles.value = allRegions.sortedByDescending { it.sizeBytes }
    }

    fun deleteRegionFile(entry: DesktopRegionFileEntry) {
        val srv = getSelectedServer() ?: return
        if (processManager.isServerRunning(srv.id)) {
            showToast("Stop the server before deleting region files.")
            return
        }
        val serverDir = serverRepository.getServerDirectory(srv.id)
        val levelName = serverRepository.loadServerProperties(srv.id).levelName.ifBlank { "world" }
        val dDir = entry.dimension.getDimensionDir(serverDir, levelName)
        val rDir = when (entry.dimension) {
            DimensionType.OVERWORLD -> File(dDir, "region")
            DimensionType.NETHER -> File(dDir, "DIM-1/region").takeIf { it.exists() } ?: File(dDir, "region")
            DimensionType.THE_END -> File(dDir, "DIM1/region").takeIf { it.exists() } ?: File(dDir, "region")
        }
        val target = File(rDir, File(entry.fileName).name)
        if (target.exists() && target.delete()) {
            refreshWorldSummary(srv)
            showToast("Deleted ${entry.fileName}")
        }
    }

    fun exportWorldZip(destZip: File) {
        val srv = getSelectedServer() ?: return
        if (processManager.isServerRunning(srv.id)) {
            showToast("Stop the server before exporting the world.")
            return
        }
        scope.launch {
            val serverDir = serverRepository.getServerDirectory(srv.id)
            val levelName = serverRepository.loadServerProperties(srv.id).levelName.ifBlank { "world" }
            val worldDir = File(serverDir, levelName)
            if (!worldDir.exists()) {
                showToast("World directory '$levelName' not found.")
                return@launch
            }
            runCatching {
                java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(FileOutputStream(destZip))).use { zos ->
                    FileStorageUtils.zipDirectoryToStream(worldDir, zos)
                }
            }.fold(
                onSuccess = {
                    showToast("Exported world to ${destZip.name}")
                    openInFileManager(destZip.parentFile ?: destZip)
                },
                onFailure = { err -> showToast("Export failed: ${err.message}") }
            )
        }
    }

    fun importWorldArchive(archiveFile: File) {
        val srv = getSelectedServer() ?: return
        if (processManager.isServerRunning(srv.id)) {
            showToast("Stop the server before importing a world.")
            return
        }
        scope.launch {
            showToast("Backing up and importing ${archiveFile.name}...")
            serverRepository.createBackup(srv.id, worldOnly = true)
            val serverDir = serverRepository.getServerDirectory(srv.id)
            val levelName = serverRepository.loadServerProperties(srv.id).levelName.ifBlank { "world" }
            val worldDir = File(serverDir, levelName).apply { mkdirs() }
            FileStorageUtils.extractZipSafe(archiveFile, worldDir, deleteZipAfter = false).fold(
                onSuccess = { count ->
                    refreshWorldSummary(srv)
                    showToast("Imported world ($count files extracted)")
                },
                onFailure = { err -> showToast("World import failed: ${err.message}") }
            )
        }
    }

    fun resetDimension(dim: DimensionType) {
        val srv = getSelectedServer() ?: return
        if (processManager.isServerRunning(srv.id)) {
            showToast("Stop the server before resetting ${dim.displayName}.")
            return
        }
        scope.launch {
            serverRepository.createBackup(srv.id, worldOnly = true)
            val serverDir = serverRepository.getServerDirectory(srv.id)
            val levelName = serverRepository.loadServerProperties(srv.id).levelName.ifBlank { "world" }
            val dir = dim.getDimensionDir(serverDir, levelName)
            if (dir.exists()) dir.deleteRecursively()
            refreshWorldSummary(srv)
            showToast("Reset ${dim.displayName} (backup saved)")
        }
    }

    fun pruneChunks(threshold: InhabitedTimeThreshold) {
        val srv = getSelectedServer() ?: return
        if (processManager.isServerRunning(srv.id)) {
            showToast("Stop the server before pruning chunks.")
            return
        }
        scope.launch {
            showToast("Pruning unused chunks...")
            serverRepository.createBackup(srv.id, worldOnly = true)
            val serverDir = serverRepository.getServerDirectory(srv.id)
            val levelName = serverRepository.loadServerProperties(srv.id).levelName.ifBlank { "world" }
            val res = ChunkOptimizer.optimizeWorld(serverDir, ChunkPruneOptions(threshold = threshold), levelName)
            refreshWorldSummary(srv)
            showToast("Pruned ${res.prunedChunks} chunks (freed ${FileStorageUtils.formatBytes(res.bytesFreed)})")
        }
    }

    fun refreshLiveMap(server: MinecraftServer) {
        val serverDir = serverRepository.getServerDirectory(server.id)
        _squaremapInstalled.value = WebMapPluginType.detectInstalled(serverDir) != null
        _squaremapPort.value = WebMapPluginType.detectSquaremapPort(serverDir)
    }

    fun installSquaremap() {
        val srv = getSelectedServer() ?: return
        val serverDir = serverRepository.getServerDirectory(srv.id)
        val isMod = srv.type.supportsMods && !srv.type.supportsPlugins
        scope.launch {
            showToast("Downloading Squaremap from Modrinth...")
            val resolved = modrinthApi.resolveDownloadUrl(WebMapPluginType.SQUAREMAP.modrinthSlug, isMod, srv.type.name.lowercase(), srv.version)
                ?: modrinthApi.resolveDownloadUrl(WebMapPluginType.SQUAREMAP.modrinthSlug, isMod, null, null)
            if (resolved == null) {
                showToast("Could not resolve Squaremap from Modrinth.")
                return@launch
            }
            val folder = File(serverDir, if (isMod) "mods" else "plugins").apply { mkdirs() }
            if (softwareManager.downloadFile(resolved.second, File(folder, resolved.first))) {
                WebMapPluginType.setSquaremapPort(serverDir, isMod, 8081)
                refreshLiveMap(srv)
                refreshInstalledPlugins(srv)
                showToast("Squaremap installed on port 8081!")
            }
        }
    }

    fun uninstallSquaremap() {
        val srv = getSelectedServer() ?: return
        val serverDir = serverRepository.getServerDirectory(srv.id)
        if (WebMapPluginType.findInstalledFile(serverDir)?.delete() == true) {
            refreshLiveMap(srv)
            refreshInstalledPlugins(srv)
            showToast("Uninstalled Squaremap")
        }
    }

    fun updateSquaremapPort(newPort: Int) {
        val srv = getSelectedServer() ?: return
        val serverDir = serverRepository.getServerDirectory(srv.id)
        val isMod = srv.type.supportsMods && !srv.type.supportsPlugins
        WebMapPluginType.setSquaremapPort(serverDir, isMod, newPort.coerceIn(1024, 65535))
        refreshLiveMap(srv)
        showToast("Updated Squaremap port to $newPort")
    }

    // --- OS Integration Helpers ---

    fun openInFileManager(file: File) {
        if (!com.devwithzachary.mineserve.desktop.DesktopPlatform.openFile(file)) {
            showToast("Path: ${file.absolutePath}")
        }
    }

    fun openUrlInBrowser(url: String) {
        if (!com.devwithzachary.mineserve.desktop.DesktopPlatform.openUrl(url)) {
            copyToClipboard(url)
            showToast("Copied URL: $url")
        }
    }

    fun copyToClipboard(text: String, toastLabel: String = "Copied to clipboard") {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            showToast(toastLabel)
        }
    }
}
