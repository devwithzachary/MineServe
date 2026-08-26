package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
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
    onBack: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onSendCommand: (String) -> Unit,
    onResizeTerminal: (Int, Int) -> Unit,
    onSaveProperties: (ServerProperties) -> Unit,
    onReadRawConfigFile: suspend (String) -> String = { "" },
    onSaveRawConfigFile: suspend (String, String) -> Boolean = { _, _ -> true },
    onListConfigFiles: suspend () -> List<String> = { listOf("server.properties") },
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

    val showPluginsOrModsTab = server.type.supportsPlugins || server.type.supportsMods
    val pluginModTabName = when {
        server.type.supportsMods && !server.type.supportsPlugins -> stringResource(R.string.tab_mods)
        server.type == ServerType.CUSTOM -> stringResource(R.string.tab_plugins_mods)
        else -> stringResource(R.string.tab_plugins)
    }

    val consoleStr = stringResource(R.string.tab_console)
    val perfStr = stringResource(R.string.tab_performance)
    val settingsStr = stringResource(R.string.tab_settings)
    val playersStr = stringResource(R.string.tab_players)
    val backupsStr = stringResource(R.string.tab_backups)

    val tabs = remember(server.type, pluginModTabName) {
        buildList {
            add(consoleStr)
            add(perfStr)
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
            // Always-Visible Network Address Card
            NetworkAddressCard(port = server.port)

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
                    2 -> SettingsTab(
                        initialProperties = properties,
                        onSaveProperties = onSaveProperties,
                        onReadRawConfigFile = onReadRawConfigFile,
                        onSaveRawConfigFile = onSaveRawConfigFile,
                        onListConfigFiles = onListConfigFiles
                    )
                    3 -> PlayersTab(
                        metrics = metrics,
                        onSendCommand = onSendCommand
                    )
                    4 -> BackupsTab(
                        backups = backups,
                        onCreateBackup = onCreateBackup,
                        onRestoreBackup = onRestoreBackup,
                        onExportBackup = onExportBackup,
                        onGetShareIntent = onGetShareIntent
                    )
                    5 -> {
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
    port: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val localIp = remember { getLocalIpAddress() ?: "127.0.0.1" }
    val fullAddress = "$localIp:$port"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldDark.copy(alpha = 0.3f),
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
                        text = "SERVER LAN ADDRESS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        fontSize = 10.sp
                    )
                    Text(
                        text = fullAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(fullAddress))
                    Toast.makeText(context, "Copied $fullAddress", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Server Address",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
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
