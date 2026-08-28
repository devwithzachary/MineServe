package com.devwithzachary.mineserve

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.MainViewModel
import com.devwithzachary.mineserve.ui.screens.about.AboutScreen
import com.devwithzachary.mineserve.ui.screens.credits.CreditsScreen
import com.devwithzachary.mineserve.ui.screens.dashboard.DashboardScreen
import com.devwithzachary.mineserve.ui.screens.detail.ServerDetailScreen
import com.devwithzachary.mineserve.ui.screens.settings.AppSettingsScreen
import com.devwithzachary.mineserve.ui.screens.splash.SplashScreen
import com.devwithzachary.mineserve.ui.screens.wizard.CreateServerWizardScreen
import com.devwithzachary.mineserve.ui.theme.MineServeTheme
import com.devwithzachary.mineserve.ui.theme.Slate950

sealed class Screen {
    data object Splash : Screen()
    data object Dashboard : Screen()
    data object Wizard : Screen()
    data class Detail(val serverId: String) : Screen()
    data object Settings : Screen()
    data object About : Screen()
    data object Credits : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MineServeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Slate950
                ) {
                    MineServeApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MineServeApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val serverStatuses by viewModel.serverStatuses.collectAsStateWithLifecycle()
    val serverMetrics by viewModel.serverMetrics.collectAsStateWithLifecycle()
    val refreshTriggers by viewModel.refreshTriggers.collectAsStateWithLifecycle()
    val isRootfsInstalled by viewModel.isRootfsInstalled.collectAsStateWithLifecycle()
    val rootfsSetupState by viewModel.rootfsSetupState.collectAsStateWithLifecycle()
    val storageUsedMb by viewModel.storageUsedMb.collectAsStateWithLifecycle()
    val serverPropertiesMap by viewModel.serverPropertiesMap.collectAsStateWithLifecycle()
    val serverBackupsMap by viewModel.serverBackupsMap.collectAsStateWithLifecycle()
    val serverPluginsMap by viewModel.serverPluginsMap.collectAsStateWithLifecycle()
    val serverStorageMap by viewModel.serverStorageMap.collectAsStateWithLifecycle()
    val tunnelStates by viewModel.tunnelStates.collectAsStateWithLifecycle()

    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (viewModel.rootfsManager.isInstalled()) Screen.Dashboard else Screen.Splash
        )
    }

    // Permission launcher for Android 13+ Notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "POST_NOTIFICATIONS granted: $isGranted")
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Splash -> {
                SplashScreen(
                    isRootfsInstalled = isRootfsInstalled,
                    setupState = rootfsSetupState,
                    onStartSetup = { viewModel.startRootfsSetup() },
                    onContinueToDashboard = { currentScreen = Screen.Dashboard }
                )
            }

            is Screen.Dashboard -> {
                DashboardScreen(
                    servers = servers,
                    serverStatuses = serverStatuses,
                    serverMetrics = serverMetrics,
                    serverStorageMap = serverStorageMap,
                    tunnelStates = tunnelStates,
                    onServerClick = { server ->
                        Log.d("MainActivity", "onServerClick: ${server.id}")
                        viewModel.loadServerDetails(server.id)
                        currentScreen = Screen.Detail(server.id)
                    },
                    onStartServer = { server ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        viewModel.startServer(server)
                    },
                    onStopServer = { serverId ->
                        viewModel.stopServer(serverId)
                    },
                    onConsoleClick = { server ->
                        Log.d("MainActivity", "onConsoleClick: ${server.id}")
                        viewModel.loadServerDetails(server.id)
                        currentScreen = Screen.Detail(server.id)
                    },
                    onCreateServerClick = { currentScreen = Screen.Wizard },
                    onSettingsClick = { currentScreen = Screen.Settings },
                    onAboutClick = { currentScreen = Screen.About },
                    onCreditsClick = { currentScreen = Screen.Credits }
                )
            }

            is Screen.Wizard -> {
                CreateServerWizardScreen(
                    onServerCreated = { createdServer ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        viewModel.refreshData()
                        viewModel.loadServerDetails(createdServer.id)
                        viewModel.startServer(createdServer)
                        currentScreen = Screen.Detail(createdServer.id)
                    },
                    onCancel = { currentScreen = Screen.Dashboard },
                    existingServers = servers,
                    onDownloadAndCreateServer = { name, type, version, port, ramMb, motd, onProgress ->
                        viewModel.downloadAndCreateServer(name, type, version, port, ramMb, motd, onProgress)
                    }
                )
            }

            is Screen.Detail -> {
                Log.d("MainActivity", "Screen.Detail: target serverId=${screen.serverId}, available servers=${servers.map { it.id }}")
                val server = servers.firstOrNull { it.id == screen.serverId }
                if (server != null) {
                    val status = serverStatuses[server.id] ?: ServerStatus.STOPPED
                    val metrics = serverMetrics[server.id]
                    val emulator = viewModel.processManager.getEmulator(server.id)
                    val trigger = refreshTriggers[server.id] ?: 0L
                    val props = serverPropertiesMap[server.id] ?: ServerProperties()
                    val backups = serverBackupsMap[server.id] ?: emptyList()
                    val plugins = serverPluginsMap[server.id] ?: emptyList()
                    val storageBytes = serverStorageMap[server.id] ?: 0L
                    val tunnelState = tunnelStates[server.id] ?: com.devwithzachary.mineserve.tunnel.TunnelState.Disconnected

                    ServerDetailScreen(
                        server = server,
                        status = status,
                        metrics = metrics,
                        emulator = emulator,
                        refreshTrigger = trigger,
                        properties = props,
                        backups = backups,
                        plugins = plugins,
                        storageBytes = storageBytes,
                        tunnelState = tunnelState,
                        onBack = { currentScreen = Screen.Dashboard },
                        onStartServer = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            viewModel.startServer(server)
                        },
                        onStopServer = { viewModel.stopServer(server.id) },
                        onToggleTunnel = { viewModel.toggleTunnel(server) },
                        onSaveServer = { updatedServer -> viewModel.updateServer(updatedServer) },
                        onSendCommand = { cmd -> viewModel.sendCommand(server.id, cmd) },
                        onResizeTerminal = { cols, rows -> viewModel.resizeTerminal(server.id, cols, rows) },
                        onSaveProperties = { updatedProps -> viewModel.saveProperties(server.id, updatedProps) },
                        onReadRawConfigFile = { fileName -> viewModel.readRawConfigFile(server.id, fileName) },
                        onSaveRawConfigFile = { fileName, content -> viewModel.saveRawConfigFile(server.id, fileName, content) },
                        onListConfigFiles = { viewModel.listEditableConfigFiles(server.id) },
                        onCreateBackup = { onResult -> viewModel.createBackup(server.id, true, onResult) },
                        onRestoreBackup = { b, onResult -> viewModel.restoreBackup(server.id, b, onResult) },
                        onExportBackup = { b, onResult -> viewModel.exportBackup(b, onResult) },
                        onGetShareIntent = { b -> viewModel.getBackupShareIntent(b) },
                        onTogglePlugin = { p -> viewModel.togglePlugin(server.id, p) },
                        onDeletePlugin = { p -> viewModel.deletePlugin(server.id, p) },
                        onInstallPluginOrMod = { fileName, url, isMod, onResult ->
                            viewModel.installPluginOrMod(server.id, fileName, url, isMod, onResult)
                        },
                        onImportJar = { uri, isMod, onResult ->
                            viewModel.importPluginOrMod(server.id, uri, isMod, onResult)
                        },
                        onDeleteServer = {
                            currentScreen = Screen.Dashboard
                            viewModel.deleteServer(server.id)
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        currentScreen = Screen.Dashboard
                    }
                }
            }

            is Screen.Settings -> {
                AppSettingsScreen(
                    storageUsedMb = storageUsedMb,
                    isJavaVersionInstalled = { ver -> viewModel.javaRuntimeManager.isJavaInstalled(ver) },
                    onInstallJava = { ver -> viewModel.installJava(ver) },
                    onReinstallRuntime = {
                        viewModel.startRootfsSetup()
                        currentScreen = Screen.Splash
                    },
                    onBack = { currentScreen = Screen.Dashboard },
                    onAboutClick = { currentScreen = Screen.About }
                )
            }

            is Screen.About -> {
                AboutScreen(
                    onBack = { currentScreen = Screen.Dashboard }
                )
            }

            is Screen.Credits -> {
                CreditsScreen(
                    onBack = { currentScreen = Screen.Dashboard }
                )
            }
        }
    }
}
