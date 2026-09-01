package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
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
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    server: MinecraftServer,
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
    onReadRawConfigFile: suspend (String) -> String = { "" },
    onSaveRawConfigFile: suspend (String, String) -> Boolean = { _, _ -> true },
    onListConfigFiles: suspend () -> List<String> = { listOf("server.properties") },
    onListDirectory: suspend (String) -> List<FileEntry> = { emptyList() },
    onCreateFile: suspend (String, String, String) -> Boolean = { _, _, _ -> false },
    onCreateDirectory: suspend (String, String) -> Boolean = { _, _ -> false },
    onDeleteFile: suspend (String) -> Boolean = { false },
    onRenameFile: suspend (String, String) -> Boolean = { _, _ -> false },
    onDuplicateFile: suspend (String) -> Boolean = { false },
    onReadFile: suspend (String) -> String = { "" },
    onWriteFile: suspend (String, String) -> Boolean = { _, _ -> false },
    onImportFile: suspend (String, android.net.Uri) -> Boolean = { _, _ -> false },
    onExportFile: suspend (String) -> Boolean = { false },
    onSearchFiles: suspend (String) -> List<FileEntry> = { emptyList() },
    onAnalyzeCrash: suspend () -> CrashDiagnosticReport? = { null },
    onApplyQuickFix: suspend (QuickFixAction) -> Boolean = { false },
    onCreateBackup: ((Boolean) -> Unit) -> Unit = {},
    onRestoreBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onExportBackup: (BackupEntry, (String?) -> Unit) -> Unit = { _, _ -> },
    onGetShareIntent: (BackupEntry) -> Intent? = { null },
    onTogglePlugin: (PluginModEntry) -> Unit,
    onDeletePlugin: (PluginModEntry) -> Unit,
    onInstallPluginOrMod: (fileName: String, downloadUrl: String, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onImportJar: (android.net.Uri, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onDeleteServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val showPluginsOrModsTab = server.type.supportsPlugins || server.type.supportsMods
    val pluginModTabName = when {
        server.type.supportsMods && !server.type.supportsPlugins -> stringResource(R.string.tab_mods)
        server.type == ServerType.CUSTOM -> stringResource(R.string.tab_plugins_mods)
        else -> stringResource(R.string.tab_plugins)
    }

    val consoleStr = stringResource(R.string.tab_console)
    val perfStr = stringResource(R.string.tab_performance)
    val filesStr = stringResource(R.string.tab_files)
    val settingsStr = stringResource(R.string.tab_settings)
    val playersStr = stringResource(R.string.tab_players)
    val backupsStr = stringResource(R.string.tab_backups)

    val tabs = remember(server.type, pluginModTabName) {
        buildList {
            add(consoleStr)
            add(perfStr)
            add(filesStr)
            add(settingsStr)
            add(playersStr)
            add(backupsStr)
            if (showPluginsOrModsTab) {
                add(pluginModTabName)
            }
        }
    }

    // Safety: ensure selectedTab is within range if tabs list shrinks
    if (selectedTab >= tabs.size) {
        selectedTab = 0
    }

    val isRunning = status == ServerStatus.RUNNING
    val isStarting = status == ServerStatus.STARTING
    val isStopping = status == ServerStatus.STOPPING

    val statusColor = when (status) {
        ServerStatus.RUNNING -> EmeraldPrimary
        ServerStatus.STARTING -> GoldYellow
        ServerStatus.STOPPING -> RedstoneLight
        ServerStatus.ERROR -> RedstoneRed
        ServerStatus.STOPPED -> Slate400
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = RedstoneRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.detail_delete_confirm_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.detail_delete_confirm_msg, server.name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteServer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.detail_delete_confirm_btn),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = Color.White
                    )
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showShareDialog) {
        val localIp = remember { getLocalIpAddress() ?: "127.0.0.1" }
        ServerShareDialog(
            server = server,
            tunnelState = tunnelState,
            localIp = localIp,
            onDismiss = { showShareDialog = false },
            onEnableTunnel = onToggleTunnel
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${server.type.displayName} ${server.version} • Port ${server.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Power Button
                    if (isRunning || isStarting || isStopping) {
                        IconButton(onClick = onStopServer, enabled = !isStopping) {
                            Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.btn_stop_server), tint = RedstoneRed)
                        }
                    } else {
                        IconButton(onClick = onStartServer) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.btn_start_server), tint = EmeraldPrimary)
                        }
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.detail_delete_server), tint = Slate400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Always-Visible Network Address & Public Tunnel Card
            NetworkAddressCard(
                server = server,
                tunnelState = tunnelState,
                onToggleTunnel = onToggleTunnel,
                onOpenShareDialog = { showShareDialog = true }
            )

            // Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate900,
                contentColor = EmeraldPrimary,
                edgePadding = 16.dp,
                indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) EmeraldLight else Slate400
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> ConsoleTab(
                        emulator = emulator,
                        refreshTrigger = refreshTrigger,
                        onSendCommand = onSendCommand,
                        onResizeTerminal = onResizeTerminal
                    )
                    1 -> PerformanceTab(
                        server = server,
                        status = status,
                        metrics = metrics,
                        storageBytes = storageBytes
                    )
                    2 -> FilesTab(
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
                        onSearchFiles = onSearchFiles,
                        onAnalyzeCrash = onAnalyzeCrash,
                        onApplyQuickFix = onApplyQuickFix
                    )
                    3 -> SettingsTab(
                        server = server,
                        initialProperties = properties,
                        onSaveProperties = onSaveProperties,
                        onSaveServer = onSaveServer,
                        onReadRawConfigFile = onReadRawConfigFile,
                        onSaveRawConfigFile = onSaveRawConfigFile,
                        onListConfigFiles = onListConfigFiles
                    )
                    4 -> PlayersTab(
                        metrics = metrics,
                        onSendCommand = onSendCommand
                    )
                    5 -> BackupsTab(
                        backups = backups,
                        onCreateBackup = onCreateBackup,
                        onRestoreBackup = onRestoreBackup,
                        onExportBackup = onExportBackup,
                        onGetShareIntent = onGetShareIntent
                    )
                    6 -> {
                        if (showPluginsOrModsTab) {
                            PluginsTab(
                                server = server,
                                plugins = plugins,
                                onTogglePlugin = onTogglePlugin,
                                onDeletePlugin = onDeletePlugin,
                                onInstallPluginOrMod = onInstallPluginOrMod,
                                onImportJar = onImportJar
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkAddressCard(
    server: MinecraftServer,
    tunnelState: TunnelState,
    onToggleTunnel: () -> Unit,
    onOpenShareDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val localIp = remember { getLocalIpAddress() ?: "127.0.0.1" }
    val localAddress = "$localIp:${server.port}"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Local LAN Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldDark.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "LOCAL WI-FI (LAN)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            fontSize = 10.sp
                        )
                        Text(
                            text = localAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(localAddress))
                            Toast.makeText(context, "Copied LAN: $localAddress", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy LAN Address",
                            tint = EmeraldLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenShareDialog,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Share & QR Code",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate800, thickness = 0.5.dp)

            // Public Online Link (Tunnel) Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (tunnelState) {
                            is TunnelState.Connected -> EmeraldDark.copy(alpha = 0.4f)
                            is TunnelState.Connecting -> GoldYellow.copy(alpha = 0.2f)
                            else -> Slate800
                        },
                        border = BorderStroke(
                            1.dp,
                            when (tunnelState) {
                                is TunnelState.Connected -> EmeraldPrimary
                                is TunnelState.Connecting -> GoldYellow
                                else -> ObsidianCardBorder
                            }
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = when (tunnelState) {
                                    is TunnelState.Connected -> EmeraldLight
                                    is TunnelState.Connecting -> GoldYellow
                                    else -> Slate400
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PUBLIC ONLINE LINK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                fontSize = 10.sp
                            )
                            when (tunnelState) {
                                is TunnelState.Connected -> {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmeraldDark
                                    ) {
                                        Text(
                                            text = "ONLINE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldLight,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                is TunnelState.Connecting -> {
                                    val isSetup = tunnelState.claimUrl != null
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSetup) DiamondCyan.copy(alpha = 0.2f) else GoldYellow.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isSetup) "CLAIM NEEDED" else "CONNECTING",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSetup) DiamondLight else GoldYellow,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }

                        Text(
                            text = when (tunnelState) {
                                is TunnelState.Connected -> tunnelState.fullAddress
                                is TunnelState.Connecting -> tunnelState.message.ifBlank { "Allocating public address..." }
                                is TunnelState.Error -> "Error: ${tunnelState.errorMessage}"
                                is TunnelState.Disconnected -> "Offline (Zero-Port-Forwarding)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (tunnelState is TunnelState.Connected) FontWeight.Bold else FontWeight.Normal,
                            color = when (tunnelState) {
                                is TunnelState.Connected -> EmeraldLight
                                is TunnelState.Connecting -> if (tunnelState.claimUrl != null) DiamondLight else GoldYellow
                                is TunnelState.Error -> RedstoneLight
                                is TunnelState.Disconnected -> Slate400
                            },
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (tunnelState is TunnelState.Connected) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(tunnelState.fullAddress))
                                Toast.makeText(context, "Copied: ${tunnelState.fullAddress}", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Public Address",
                                tint = EmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Switch(
                        checked = tunnelState is TunnelState.Connected || tunnelState is TunnelState.Connecting,
                        onCheckedChange = { onToggleTunnel() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = EmeraldDark
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            // Playit.gg 1-Tap Claim Banner
            if (tunnelState is TunnelState.Connecting && tunnelState.claimUrl != null) {
                val claimUrl = tunnelState.claimUrl
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DiamondCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, DiamondCyan.copy(alpha = 0.40f)),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(claimUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = DiamondCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Link your Playit.gg Account",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DiamondLight
                            )
                            Text(
                                text = "Tap here to claim this tunnel in your browser",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = DiamondCyan
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = DiamondCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Security Notice Banner when Public Tunneling is enabled/active
            if (tunnelState is TunnelState.Connected || tunnelState is TunnelState.Connecting) {
                TunnelSecurityWarningCard()
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
