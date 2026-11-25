package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.cloud.CloudAccountState
import com.devwithzachary.mineserve.cloud.CloudBackupItem
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.engine.TerminalEmulator
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.CrashDiagnosticReport
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.QuickFixAction
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.tunnel.TunnelState
import com.devwithzachary.mineserve.ui.components.ServerShareDialog
import com.devwithzachary.mineserve.ui.components.TunnelSecurityWarningCard
import kotlinx.coroutines.launch
import java.net.NetworkInterface

private enum class DetailTab {
    CONSOLE, PERFORMANCE, PLAYERS, SETTINGS
}

private enum class SettingsSubSection {
    GENERAL, PLUGINS_MODS, FILES, WORLD, LIVE_MAP, BACKUPS, AUTOMATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    server: MinecraftServer,
    allServers: List<MinecraftServer> = emptyList(),
    status: ServerStatus,
    metrics: ServerMetrics?,
    emulator: TerminalEmulator,
    refreshTrigger: Long,
    properties: ServerProperties,
    backups: List<BackupEntry>,
    plugins: List<PluginModEntry>,
    storageBytes: Long = 0L,
    tunnelState: TunnelState = TunnelState.Disconnected,
    onBack: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onToggleTunnel: () -> Unit = {},
    onSaveServer: (MinecraftServer) -> Unit = {},
    onSendCommand: (String) -> Unit,
    onResizeTerminal: (Int, Int) -> Unit,
    onSaveProperties: (ServerProperties) -> Unit,
    onListDirectory: suspend (String) -> List<FileEntry> = { emptyList() },
    onCreateFile: suspend (String, String, String) -> Boolean = { _, _, _ -> false },
    onCreateDirectory: suspend (String, String) -> Boolean = { _, _ -> false },
    onDeleteFile: suspend (String) -> Boolean = { false },
    onRenameFile: suspend (String, String) -> Boolean = { _, _ -> false },
    onDuplicateFile: suspend (String) -> Boolean = { false },
    onReadFile: suspend (String) -> String = { "" },
    onWriteFile: suspend (String, String) -> Boolean = { _, _ -> false },
    onImportFile: suspend (String, Uri) -> Boolean = { _, _ -> false },
    onExportFile: suspend (String) -> Boolean = { false },
    onUnzipFile: suspend (String, String, Boolean) -> Result<Int> = { _, _, _ -> Result.success(0) },
    onSearchFiles: suspend (String) -> List<FileEntry> = { emptyList() },
    onAnalyzeCrash: suspend () -> CrashDiagnosticReport? = { null },
    onApplyQuickFix: suspend (QuickFixAction) -> Boolean = { false },
    onCreateBackup: ((Boolean) -> Unit) -> Unit = {},
    onRestoreBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onExportBackup: (BackupEntry, (String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onGetShareIntent: (BackupEntry) -> Intent? = { null },
    onRefreshBackups: () -> Unit = {},
    onSyncBackupToCloud: ((BackupEntry, (Result<String>) -> Unit) -> Unit)? = null,
    cloudAccountState: CloudAccountState = CloudAccountState(),
    cloudBackups: List<CloudBackupItem> = emptyList(),
    isCloudBackupsLoading: Boolean = false,
    onRefreshCloudBackups: () -> Unit = {},
    onRestoreCloudBackup: (CloudBackupItem, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onDeleteCloudBackup: (CloudBackupItem, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onOpenCloudPortal: () -> Unit = {},
    onRefreshCloudAccount: () -> Unit = {},
    onTogglePlugin: (PluginModEntry) -> Unit,
    onDeletePlugin: (PluginModEntry) -> Unit,
    onInstallPluginOrMod: (fileName: String, downloadUrl: String, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onImportJar: (Uri, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onImportModpack: ((Uri, (com.devwithzachary.mineserve.modpack.ModpackInstallProgress) -> Unit, (Result<com.devwithzachary.mineserve.modpack.ModpackIndex>) -> Unit) -> Unit)? = null,
    isStandbyActive: Boolean = false,
    onSaveAutomationConfig: (com.devwithzachary.mineserve.model.ServerAutomationConfig) -> Unit = {},
    onEnterStandby: () -> Unit = {},
    onExitStandby: () -> Unit = {},
    onCheckForUpdate: suspend (MinecraftServer) -> com.devwithzachary.mineserve.model.ServerBuildInfo? = { null },
    onFetchVersions: suspend (ServerType) -> List<String> = { emptyList() },
    onUpdateBuild: (createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onUpgradeVersion: (newVersion: String, createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onGetWorldSummary: suspend () -> com.devwithzachary.mineserve.model.WorldSummary = { com.devwithzachary.mineserve.model.WorldSummary("world", 0L, emptyList()) },
    onImportWorld: suspend (Uri, Boolean, (String, Int) -> Unit) -> Result<String> = { _, _, _ -> Result.success("") },
    onExportWorld: suspend (java.io.OutputStream, (String, Int) -> Unit) -> Boolean = { _, _ -> true },
    onResetDimension: suspend (com.devwithzachary.mineserve.model.DimensionType, Boolean) -> Boolean = { _, _ -> true },
    onPruneChunks: suspend (com.devwithzachary.mineserve.model.ChunkPruneOptions, (String, Int) -> Unit) -> com.devwithzachary.mineserve.model.ChunkPruneResult = { _, _ -> com.devwithzachary.mineserve.model.ChunkPruneResult(0, 0, 0, 0, 0L, 0L) },
    onGetWebMapState: () -> com.devwithzachary.mineserve.model.WebMapState = { com.devwithzachary.mineserve.model.WebMapState() },
    onSetWebMapPort: (Int) -> Unit = {},
    onInstallWebMapPlugin: (com.devwithzachary.mineserve.model.WebMapPluginType, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onUninstallWebMapPlugin: () -> Boolean = { false },
    onLoadPlayerLists: suspend () -> com.devwithzachary.mineserve.model.ServerPlayerLists = { com.devwithzachary.mineserve.model.ServerPlayerLists() },
    onAddWhitelistPlayer: suspend (String) -> Boolean = { false },
    onRemoveWhitelistPlayer: suspend (String) -> Boolean = { false },
    onAddOp: suspend (String, Int) -> Boolean = { _, _ -> false },
    onRemoveOp: suspend (String) -> Boolean = { false },
    onAddBannedPlayer: suspend (String, String) -> Boolean = { _, _ -> false },
    onRemoveBannedPlayer: suspend (String) -> Boolean = { false },
    onAddBannedIp: suspend (String, String) -> Boolean = { _, _ -> false },
    onRemoveBannedIp: suspend (String) -> Boolean = { false },
    onDeleteServer: () -> Unit,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var selectedSettingsSection by remember { mutableStateOf(SettingsSubSection.GENERAL) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val topTabs = listOf(
        DetailTab.CONSOLE to stringResource(R.string.tab_console),
        DetailTab.PERFORMANCE to stringResource(R.string.tab_performance),
        DetailTab.PLAYERS to stringResource(R.string.tab_players),
        DetailTab.SETTINGS to stringResource(R.string.tab_settings)
    )

    val showPluginsOrMods = server.type.supportsPlugins || server.type.supportsMods
    val pluginModLabel = when {
        server.type.supportsMods && !server.type.supportsPlugins -> stringResource(R.string.tab_mods)
        server.type == ServerType.CUSTOM -> stringResource(R.string.tab_plugins_mods)
        else -> stringResource(R.string.tab_plugins)
    }
    val showWorld = !server.type.isProxy && !server.type.isBedrockNative
    val showLiveMap = !server.type.isProxy && !server.type.isBedrockNative &&
        server.type != ServerType.VANILLA && (server.type.supportsPlugins || server.type.supportsMods)

    val settingsSubSections = remember(server.type, pluginModLabel, showPluginsOrMods, showWorld, showLiveMap) {
        buildList {
            add(SettingsSubSection.GENERAL to "General")
            if (showPluginsOrMods) add(SettingsSubSection.PLUGINS_MODS to pluginModLabel)
            add(SettingsSubSection.FILES to context.getString(R.string.tab_files))
            if (showWorld) add(SettingsSubSection.WORLD to context.getString(R.string.tab_world))
            if (showLiveMap) add(SettingsSubSection.LIVE_MAP to context.getString(R.string.tab_live_map))
            add(SettingsSubSection.BACKUPS to context.getString(R.string.tab_backups))
            add(SettingsSubSection.AUTOMATION to context.getString(R.string.tab_automation))
        }
    }

    if (settingsSubSections.none { it.first == selectedSettingsSection }) {
        selectedSettingsSection = SettingsSubSection.GENERAL
    }

    val isRunning = status == ServerStatus.RUNNING
    val isStarting = status == ServerStatus.STARTING
    val isStopping = status == ServerStatus.STOPPING
    val isActive = isRunning || isStarting || isStopping

    val statusLabel = when (status) {
        ServerStatus.RUNNING -> stringResource(R.string.status_online)
        ServerStatus.STARTING -> stringResource(R.string.status_starting)
        ServerStatus.STOPPING -> stringResource(R.string.status_stopping)
        ServerStatus.ERROR -> stringResource(R.string.status_error)
        ServerStatus.STOPPED -> stringResource(R.string.status_offline)
        ServerStatus.STANDBY -> "Standby"
    }

    val isInspection = androidx.compose.ui.platform.LocalInspectionMode.current
    val localIp = remember(isInspection) {
        if (isInspection) "192.168.1.42" else getLocalIpAddress() ?: "127.0.0.1"
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    tint = colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.detail_delete_confirm_title),
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.detail_delete_confirm_msg, server.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteServer()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    ),
                    shape = ShapeDefaults.Medium
                ) {
                    Text(text = stringResource(R.string.detail_delete_confirm_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = colorScheme.surfaceContainerHighest,
            shape = ShapeDefaults.ExtraLarge
        )
    }

    if (showShareDialog) {
        ServerShareDialog(
            server = server,
            tunnelState = tunnelState,
            localIp = localIp,
            onDismiss = { showShareDialog = false },
            onEnableTunnel = onToggleTunnel
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                title = {
                    val statusDotColor = when (status) {
                        ServerStatus.RUNNING -> extended.onlineEmerald
                        ServerStatus.STARTING, ServerStatus.STOPPING, ServerStatus.STANDBY -> extended.warning
                        ServerStatus.ERROR -> colorScheme.error
                        ServerStatus.STOPPED -> colorScheme.onSurfaceVariant
                    }
                    val statusTextColor = when (status) {
                        ServerStatus.RUNNING -> extended.onlineEmerald
                        ServerStatus.STARTING, ServerStatus.STOPPING, ServerStatus.STANDBY -> extended.warning
                        ServerStatus.ERROR -> colorScheme.error
                        ServerStatus.STOPPED -> colorScheme.onSurfaceVariant
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusTextColor,
                                maxLines = 1
                            )
                            Text(
                                text = "• ${server.type.displayName} ${server.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (isActive) {
                        FilledTonalButton(
                            onClick = onStopServer,
                            enabled = !isStopping,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer,
                                disabledContainerColor = colorScheme.surfaceContainerHighest,
                                disabledContentColor = colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            shape = ShapeDefaults.Medium,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.btn_stop_server))
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onStartServer,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.primaryContainer,
                                contentColor = colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            shape = ShapeDefaults.Medium,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.btn_start_server))
                        }
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "Server options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            containerColor = colorScheme.surfaceContainerHighest
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share & QR Code", color = colorScheme.onSurface) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCode2,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showShareDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    val tunnelMenuLabel = if (tunnelState is TunnelState.Connected || tunnelState is TunnelState.Connecting) {
                                        "Disable Public Tunnel"
                                    } else {
                                        "Enable Public Tunnel"
                                    }
                                    Text(tunnelMenuLabel, color = colorScheme.onSurface)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = if (tunnelState is TunnelState.Connected) colorScheme.tertiary else colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onToggleTunnel()
                                }
                            )
                            HorizontalDivider(color = colorScheme.outlineVariant)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.detail_delete_server),
                                        color = colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteForever,
                                        contentDescription = null,
                                        tint = colorScheme.error
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface,
                    actionIconContentColor = colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Connection Card: Stacked LAN IP and Remote Tunnel rows
            val lanAddress = "$localIp:${server.port}"
            MineServeCard(
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboard.setText(AnnotatedString(lanAddress))
                                scope.launch { snackbarHostState.showSnackbar("Copied $lanAddress") }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lan,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "LAN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(52.dp)
                        )
                        Text(
                            text = lanAddress,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = monoFont,
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = "tnum"
                            ),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy LAN address",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.35f))

                    when (tunnelState) {
                        is TunnelState.Connected -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        clipboard.setText(AnnotatedString(tunnelState.fullAddress))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied ${tunnelState.fullAddress}")
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Remote",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.tertiary,
                                    modifier = Modifier.width(52.dp)
                                )
                                Text(
                                    text = tunnelState.fullAddress,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy remote address",
                                    tint = colorScheme.tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        is TunnelState.Connecting -> {
                            val claimUrl = tunnelState.claimUrl
                            val connectingTint = if (claimUrl != null) colorScheme.tertiary else extended.warning
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = claimUrl != null) {
                                        if (claimUrl != null) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(claimUrl)))
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = connectingTint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Remote",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = connectingTint,
                                    modifier = Modifier.width(52.dp)
                                )
                                Text(
                                    text = if (claimUrl != null) "Tap to claim public tunnel" else "Connecting public tunnel...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onToggleTunnel)
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Remote",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(52.dp)
                                )
                                Text(
                                    text = "Tunnel off",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Enable",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (tunnelState is TunnelState.Connected || tunnelState is TunnelState.Connecting) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    TunnelSecurityWarningCard()
                }
            }

            if (server.type.isBedrockNative) {
                Surface(
                    shape = ShapeDefaults.Medium,
                    color = extended.warningContainer,
                    contentColor = extended.onWarningContainer,
                    border = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = extended.warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Bedrock Dedicated Server (Beta)",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Native Bedrock hosting is currently experimental. On ARM64 devices, it runs via user-mode qemu-x86_64 emulation.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // M3 PrimaryScrollableTabRow: Console, Performance, Players, Settings
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = colorScheme.surfaceContainerLow,
                contentColor = colorScheme.primary,
                edgePadding = 16.dp,
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab, matchContentSize = true),
                        width = Dp.Unspecified,
                        color = colorScheme.primary
                    )
                },
                divider = {
                    HorizontalDivider(color = colorScheme.outlineVariant)
                }
            ) {
                topTabs.forEachIndexed { index, pair ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = colorScheme.primary,
                        unselectedContentColor = colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = pair.second,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (topTabs[selectedTab].first) {
                    DetailTab.CONSOLE -> ConsoleTab(
                        emulator = emulator,
                        refreshTrigger = refreshTrigger,
                        onSendCommand = onSendCommand,
                        onResizeTerminal = onResizeTerminal
                    )
                    DetailTab.PERFORMANCE -> PerformanceTab(
                        server = server,
                        status = status,
                        metrics = metrics,
                        storageBytes = storageBytes,
                        onUpdateServer = onSaveServer
                    )
                    DetailTab.PLAYERS -> PlayersTab(
                        server = server,
                        status = status,
                        metrics = metrics,
                        onSendCommand = onSendCommand,
                        onLoadPlayerLists = onLoadPlayerLists,
                        onAddWhitelistPlayer = onAddWhitelistPlayer,
                        onRemoveWhitelistPlayer = onRemoveWhitelistPlayer,
                        onAddOp = onAddOp,
                        onRemoveOp = onRemoveOp,
                        onAddBannedPlayer = onAddBannedPlayer,
                        onRemoveBannedPlayer = onRemoveBannedPlayer,
                        onAddBannedIp = onAddBannedIp,
                        onRemoveBannedIp = onRemoveBannedIp
                    )
                    DetailTab.SETTINGS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(settingsSubSections) { (section, label) ->
                                    val selected = selectedSettingsSection == section
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedSettingsSection = section },
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        shape = ShapeDefaults.Small,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = colorScheme.surfaceContainer,
                                            labelColor = colorScheme.onSurfaceVariant,
                                            selectedContainerColor = colorScheme.secondaryContainer,
                                            selectedLabelColor = colorScheme.onSecondaryContainer
                                        ),
                                        border = null
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when (selectedSettingsSection) {
                                    SettingsSubSection.GENERAL -> SettingsTab(
                                        server = server,
                                        allServers = allServers,
                                        status = status,
                                        initialProperties = properties,
                                        onSaveProperties = onSaveProperties,
                                        onSaveServer = onSaveServer,
                                        onCheckForUpdate = onCheckForUpdate,
                                        onFetchVersions = onFetchVersions,
                                        onUpdateBuild = onUpdateBuild,
                                        onUpgradeVersion = onUpgradeVersion,
                                        cloudAccountState = cloudAccountState,
                                        onOpenCloudPortal = onOpenCloudPortal,
                                        onRefreshCloudAccount = onRefreshCloudAccount
                                    )
                                    SettingsSubSection.PLUGINS_MODS -> PluginsTab(
                                        server = server,
                                        plugins = plugins,
                                        onTogglePlugin = onTogglePlugin,
                                        onDeletePlugin = onDeletePlugin,
                                        onInstallPluginOrMod = onInstallPluginOrMod,
                                        onImportJar = onImportJar,
                                        onImportModpack = onImportModpack
                                    )
                                    SettingsSubSection.FILES -> FilesTab(
                                        server = server,
                                        onListDirectory = onListDirectory,
                                        onCreateFile = onCreateFile,
                                        onCreateDirectory = onCreateDirectory,
                                        onDeleteFile = onDeleteFile,
                                        onRenameFile = onRenameFile,
                                        onDuplicateFile = onDuplicateFile,
                                        onReadFile = onReadFile,
                                        onWriteFile = onWriteFile,
                                        onImportFile = onImportFile,
                                        onExportFile = onExportFile,
                                        onUnzipFile = onUnzipFile,
                                        onSearchFiles = onSearchFiles,
                                        onAnalyzeCrash = onAnalyzeCrash,
                                        onApplyQuickFix = onApplyQuickFix
                                    )
                                    SettingsSubSection.WORLD -> WorldTab(
                                        server = server,
                                        status = status,
                                        onGetWorldSummary = onGetWorldSummary,
                                        onImportWorld = onImportWorld,
                                        onExportWorld = onExportWorld,
                                        onResetDimension = onResetDimension,
                                        onPruneChunks = onPruneChunks,
                                        onSendCommand = onSendCommand,
                                        onNavigateToLiveMap = {
                                            if (showLiveMap) {
                                                selectedSettingsSection = SettingsSubSection.LIVE_MAP
                                            }
                                        }
                                    )
                                    SettingsSubSection.LIVE_MAP -> LiveMapTab(
                                        server = server,
                                        status = status,
                                        levelName = properties.levelName,
                                        onSendCommand = onSendCommand,
                                        onGetWebMapState = onGetWebMapState,
                                        onSetWebMapPort = onSetWebMapPort,
                                        onInstallWebMapPlugin = onInstallWebMapPlugin,
                                        onUninstallWebMapPlugin = onUninstallWebMapPlugin,
                                        onStartServer = onStartServer
                                    )
                                    SettingsSubSection.BACKUPS -> BackupsTab(
                                        server = server,
                                        backups = backups,
                                        onCreateBackup = onCreateBackup,
                                        onRestoreBackup = onRestoreBackup,
                                        onExportBackup = onExportBackup,
                                        onDeleteBackup = onDeleteBackup,
                                        onGetShareIntent = onGetShareIntent,
                                        onRefreshBackups = onRefreshBackups,
                                        onSaveServer = onSaveServer,
                                        onSyncBackupToCloud = onSyncBackupToCloud,
                                        cloudAccountState = cloudAccountState,
                                        cloudBackups = cloudBackups,
                                        isCloudBackupsLoading = isCloudBackupsLoading,
                                        onRefreshCloudBackups = onRefreshCloudBackups,
                                        onRestoreCloudBackup = onRestoreCloudBackup,
                                        onDeleteCloudBackup = onDeleteCloudBackup,
                                        onOpenCloudPortal = onOpenCloudPortal
                                    )
                                    SettingsSubSection.AUTOMATION -> AutomationTab(
                                        server = server,
                                        status = status,
                                        isStandbyActive = isStandbyActive,
                                        onSaveAutomationConfig = onSaveAutomationConfig,
                                        onEnterStandby = onEnterStandby,
                                        onExitStandby = onExitStandby
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getLocalIpAddress(): String? {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
    } catch (_: Exception) {}
    return null
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun ServerDetailScreenConsolePreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        val emulator = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.rememberSampleTerminalEmulator()
        ServerDetailScreen(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            allServers = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleServers,
            status = com.devwithzachary.mineserve.model.ServerStatus.RUNNING,
            metrics = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleRunningMetrics,
            emulator = emulator,
            refreshTrigger = 1L,
            properties = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleProperties,
            backups = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleBackups,
            plugins = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.samplePlugins,
            storageBytes = 482_000_000L,
            tunnelState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleTunnelState,
            onBack = {},
            onStartServer = {},
            onStopServer = {},
            onSendCommand = {},
            onResizeTerminal = { _, _ -> },
            onSaveProperties = {},
            onTogglePlugin = {},
            onDeletePlugin = {},
            onDeleteServer = {},
            initialTab = 0
        )
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun ServerDetailScreenPerformancePreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        val emulator = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.rememberSampleTerminalEmulator()
        ServerDetailScreen(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            allServers = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleServers,
            status = com.devwithzachary.mineserve.model.ServerStatus.RUNNING,
            metrics = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleRunningMetrics,
            emulator = emulator,
            refreshTrigger = 1L,
            properties = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleProperties,
            backups = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleBackups,
            plugins = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.samplePlugins,
            storageBytes = 482_000_000L,
            tunnelState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleTunnelState,
            onBack = {},
            onStartServer = {},
            onStopServer = {},
            onSendCommand = {},
            onResizeTerminal = { _, _ -> },
            onSaveProperties = {},
            onTogglePlugin = {},
            onDeletePlugin = {},
            onDeleteServer = {},
            initialTab = 1
        )
    }
}
