package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.ConsoleTerminalPane
import com.devwithzachary.mineserve.designsystem.components.MetricTelemetryTile
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.components.MineServeTelemetryChart
import com.devwithzachary.mineserve.designsystem.components.TelemetryChartSeries
import com.devwithzachary.mineserve.designsystem.components.TelemetrySeriesColor
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.desktop.DesktopHostTelemetry
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.QuickFixType
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.util.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

private const val DESKTOP_AIKAR_FLAGS =
    "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"

@Composable
fun DesktopConsoleTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    metrics: ServerMetrics?
) {
    val colorScheme = MaterialTheme.colorScheme
    val refreshTriggers by viewModel.processManager.refreshTriggers.collectAsState()
    val crashReports by viewModel.processManager.crashReports.collectAsState()
    val crashReport = crashReports[server.id]
    val trigger = refreshTriggers[server.id] ?: 0L
    val lines = remember(server.id, trigger) {
        viewModel.processManager.getRecentConsoleLines(server.id, 2000)
    }
    var commandText by remember(server.id) { mutableStateOf("") }

    fun submitCommand() {
        val cmd = commandText.trim()
        if (cmd.isNotEmpty()) {
            viewModel.processManager.sendCommand(server.id, cmd)
            commandText = ""
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val showSideInspector = maxWidth >= 1100.dp
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MineServeCard(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                if (crashReport != null) {
                    Surface(
                        color = colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = ShapeDefaults.Medium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Crash Diagnosis: ${crashReport.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onErrorContainer
                                )
                                TextButton(onClick = { viewModel.processManager.dismissCrashReport(server.id) }) {
                                    Text("Dismiss", color = colorScheme.onErrorContainer)
                                }
                            }
                            Text(
                                text = crashReport.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onErrorContainer
                            )
                            if (crashReport.suggestedFixes.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    crashReport.suggestedFixes.forEach { fix ->
                                        FilledTonalButton(
                                            onClick = {
                                                val dir = viewModel.serverRepository.getServerDirectory(server.id)
                                                when (fix.actionType) {
                                                    QuickFixType.ACCEPT_EULA -> {
                                                        java.io.File(dir, "eula.txt").writeText("eula=true\n")
                                                        viewModel.serverRepository.updateServer(server.copy(eulaAccepted = true))
                                                        viewModel.showToast("Accepted EULA in eula.txt")
                                                    }
                                                    QuickFixType.CHANGE_JAVA_VERSION -> {
                                                        val jVer = fix.payload.toIntOrNull() ?: 21
                                                        viewModel.serverRepository.updateServer(server.copy(javaVersion = jVer))
                                                        viewModel.showToast("Switched Java runtime to Java $jVer")
                                                    }
                                                    QuickFixType.INCREASE_RAM -> {
                                                        val nextRam = fix.payload.toIntOrNull() ?: (server.allocatedRamMb + 1024)
                                                        viewModel.serverRepository.updateServer(server.copy(allocatedRamMb = nextRam))
                                                        viewModel.showToast("Updated RAM allocation to ${nextRam} MB")
                                                    }
                                                    else -> viewModel.openServerSettings()
                                                }
                                                viewModel.processManager.dismissCrashReport(server.id)
                                            },
                                            shape = ShapeDefaults.Small
                                        ) {
                                            Text(fix.label)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Server Console (${lines.size} lines)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    FilledTonalButton(
                        onClick = {
                            if (lines.isEmpty()) {
                                viewModel.showToast("Console is empty")
                            } else {
                                viewModel.copyToClipboard(
                                    lines.joinToString("\n"),
                                    "Copied ${lines.size} console lines to clipboard"
                                )
                            }
                        },
                        shape = ShapeDefaults.Small,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.surfaceContainerHigh,
                            contentColor = colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Console Output",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ConsoleTerminalPane(
                    lines = lines,
                    commandInput = commandText,
                    onCommandInputChange = { commandText = it },
                    onSendCommand = { submitCommand() },
                    emptyPlaceholder = "[MineServe] Server console ready. Press Start to launch ${server.name}.",
                    commandPlaceholder = "Enter server command (e.g. say Hello, list, op Steve, save-all)...",
                    quickCommands = listOf("save-all", "list", "time set day", "weather clear"),
                    onQuickCommandClick = { cmd ->
                        viewModel.processManager.sendCommand(server.id, cmd)
                        viewModel.showToast("Sent: $cmd")
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            if (showSideInspector) {
                Column(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MineServeCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Quick Commands",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickCmdBtn("save-all", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "save-all") }
                            QuickCmdBtn("list", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "list") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickCmdBtn("time set day", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "time set day") }
                            QuickCmdBtn("weather clear", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "weather clear") }
                        }
                    }

                    MineServeCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        val players = metrics?.onlinePlayers ?: emptyList()
                        Text(
                            text = "Online Players (${players.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (players.isEmpty()) {
                            Text(
                                text = "No players currently online.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(players) { _, player ->
                                    Surface(
                                        shape = ShapeDefaults.Medium,
                                        color = colorScheme.surfaceContainerHigh,
                                        contentColor = colorScheme.onSurface,
                                        border = null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = player,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                QuickCmdBtn("OP", Modifier) {
                                                    viewModel.processManager.sendCommand(server.id, "op $player")
                                                    viewModel.showToast("Sent: op $player")
                                                }
                                                QuickCmdBtn("Kick", Modifier) {
                                                    viewModel.processManager.sendCommand(server.id, "kick $player")
                                                    viewModel.showToast("Sent: kick $player")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCmdBtn(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    FilledTonalButton(
        onClick = onClick,
        shape = ShapeDefaults.Small,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurface
        ),
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun DesktopPerformanceTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    metrics: ServerMetrics?
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val statuses by viewModel.processManager.serverStatuses.collectAsState()
    val status = statuses[server.id] ?: ServerStatus.STOPPED
    val isRunning = status == ServerStatus.RUNNING
    val isStarting = status == ServerStatus.STARTING
    val isOnline = isRunning || isStarting

    val cpuPct = if (isOnline) (metrics?.cpuPercentage ?: 0f) else 0f
    val ramUsedMb = if (isOnline) (metrics?.ramUsedMb ?: 0L) else 0L
    val ramMaxMb = server.allocatedRamMb.toLong().coerceAtLeast(1L)
    val ramRatio = (ramUsedMb.toFloat() / ramMaxMb.toFloat()).coerceIn(0f, 1f)
    val ramPercentage = (ramRatio * 100f).toInt()

    val tps = if (isOnline) (metrics?.tps ?: 20.0) else 0.0
    val mspt = if (isOnline) (metrics?.mspt ?: 15.0) else 0.0
    val tpsHistory = metrics?.tpsHistory ?: emptyList()
    val msptHistory = metrics?.msptHistory ?: emptyList()
    val lagWarnings = if (isOnline) (metrics?.lagWarningsCount ?: 0) else 0
    val uptimeSeconds = if (isOnline) (metrics?.uptimeSeconds ?: 0L) else 0L
    val pid = if (isOnline) (metrics?.pid ?: -1) else -1

    val cpuHistory = remember(server.id) { mutableStateListOf<Float>() }
    val ramHistory = remember(server.id) { mutableStateListOf<Float>() }
    LaunchedEffect(server.id, isOnline, cpuPct, ramRatio) {
        if (isOnline) {
            cpuHistory.add(cpuPct.coerceIn(0f, 100f))
            if (cpuHistory.size > 40) cpuHistory.removeAt(0)
            ramHistory.add((ramRatio * 100f).coerceIn(0f, 100f))
            if (ramHistory.size > 40) ramHistory.removeAt(0)
        }
    }

    val serverDir = remember(server.id) { viewModel.serverRepository.getServerDirectory(server.id) }
    var storageBytes by remember(server.id) { mutableStateOf(0L) }
    var totalDiskBytes by remember(server.id) { mutableStateOf(0L) }
    var freeDiskBytes by remember(server.id) { mutableStateOf(0L) }
    var hostMem by remember { mutableStateOf(DesktopHostTelemetry.getMemoryInfo()) }

    LaunchedEffect(server.id) {
        var tick = 0
        while (isActive) {
            if (tick % 5 == 0) {
                val (used, total, free) = withContext(Dispatchers.IO) {
                    Triple(
                        FileStorageUtils.calculateDirectorySize(serverDir),
                        serverDir.totalSpace,
                        serverDir.usableSpace
                    )
                }
                storageBytes = used
                totalDiskBytes = total
                freeDiskBytes = free
            }
            hostMem = withContext(Dispatchers.IO) { DesktopHostTelemetry.getMemoryInfo() }
            tick++
            delay(2000)
        }
    }

    val usedDiskBytes = (totalDiskBytes - freeDiskBytes).coerceAtLeast(0L)
    val diskRatio = if (totalDiskBytes > 0L) (usedDiskBytes.toFloat() / totalDiskBytes.toFloat()).coerceIn(0f, 1f) else 0f
    val isLowStorage = totalDiskBytes > 0L && freeDiskBytes < 1024L * 1024L * 1024L
    val hostUsedMb = (hostMem.totalMb - hostMem.availableMb).coerceAtLeast(0L)

    val formattedDisk = FileStorageUtils.formatBytes(storageBytes, includeKb = false, gbDecimals = 2)
    val formattedFreeDisk = FileStorageUtils.formatBytes(freeDiskBytes, includeKb = false, gbDecimals = 2)
    val formattedUptime = formatDesktopUptime(uptimeSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isOnline) {
            Surface(
                shape = ShapeDefaults.Large,
                color = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurfaceVariant,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colorScheme.outline)
                    )
                    Text(
                        text = "Server is currently offline. Start the server to view live CPU, RAM, and rolling TPS/MSPT telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLowStorage) {
            Surface(
                shape = ShapeDefaults.Large,
                color = colorScheme.errorContainer,
                contentColor = colorScheme.onErrorContainer,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Low Host Storage Warning",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Only $formattedFreeDisk free space remaining. World saving or chunk generation may be interrupted if storage is exhausted.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // 4 Quick Telemetry Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTelemetryTile(
                title = "CPU",
                value = "${String.format(Locale.US, "%.1f", cpuPct)}%",
                progress = cpuPct / 100f,
                supportingText = if (isOnline) "JVM host utilization" else "Offline",
                icon = Icons.Rounded.Speed,
                modifier = Modifier.weight(1f)
            )
            MetricTelemetryTile(
                title = "Memory",
                value = "$ramUsedMb / $ramMaxMb MB",
                progress = ramRatio,
                supportingText = "$ramPercentage% heap allocated",
                icon = Icons.Rounded.Memory,
                modifier = Modifier.weight(1f)
            )
            MetricTelemetryTile(
                title = "TPS / MSPT",
                value = if (isOnline) String.format(Locale.US, "%.1f TPS", tps) else "-- TPS",
                progress = if (isOnline) (tps / 20.0).toFloat() else 0f,
                supportingText = if (isOnline) String.format(Locale.US, "%.1f ms/tick", mspt) else "Offline",
                icon = Icons.Rounded.Timer,
                modifier = Modifier.weight(1f)
            )
            MetricTelemetryTile(
                title = "Storage",
                value = formattedDisk,
                progress = diskRatio,
                supportingText = if (totalDiskBytes > 0L) "$formattedFreeDisk free on host" else "Measuring...",
                icon = Icons.Rounded.Folder,
                modifier = Modifier.weight(1f)
            )
        }

        // 2-Column Grid of MineServeTelemetryChart Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MineServeTelemetryChart(
                title = "CPU Utilization",
                series = listOf(
                    TelemetryChartSeries(
                        label = "CPU %",
                        values = cpuHistory.toList(),
                        color = TelemetrySeriesColor.Emerald
                    )
                ),
                currentValueText = "${String.format(Locale.US, "%.1f", cpuPct)}%",
                minValue = 0f,
                maxValue = 100f,
                modifier = Modifier.weight(1f)
            )
            MineServeTelemetryChart(
                title = "Memory & Heap Usage",
                series = listOf(
                    TelemetryChartSeries(
                        label = "Heap %",
                        values = ramHistory.toList(),
                        color = TelemetrySeriesColor.Diamond
                    )
                ),
                currentValueText = "$ramPercentage% ($ramUsedMb MB)",
                minValue = 0f,
                maxValue = 100f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MineServeTelemetryChart(
                title = "TPS History (Target 20.0)",
                series = listOf(
                    TelemetryChartSeries(
                        label = "TPS",
                        values = tpsHistory,
                        color = TelemetrySeriesColor.Emerald
                    )
                ),
                currentValueText = if (isOnline) String.format(Locale.US, "%.1f TPS", tps) else "--",
                minValue = 0f,
                maxValue = 20f,
                modifier = Modifier.weight(1f)
            )
            MineServeTelemetryChart(
                title = "MSPT Tick Duration (Target <50ms)",
                series = listOf(
                    TelemetryChartSeries(
                        label = "MSPT",
                        values = msptHistory,
                        color = TelemetrySeriesColor.Diamond
                    )
                ),
                currentValueText = if (isOnline) String.format(Locale.US, "%.1f ms", mspt) else "--",
                minValue = 0f,
                maxValue = 60f,
                modifier = Modifier.weight(1f)
            )
        }

        // JVM & Host Runtime Metadata + Aikar's Flags Optimizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "JVM & Host Runtime",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                MineServeTableContainer(modifier = Modifier.fillMaxWidth()) {
                    RuntimeMetadataRow("Process PID", if (pid > 0) pid.toString() else "Stopped", monoFont)
                    RuntimeMetadataRow("Uptime", if (isOnline) formattedUptime else "00:00", monoFont)
                    RuntimeMetadataRow(
                        "Host Memory",
                        "${hostUsedMb} / ${hostMem.totalMb} MB",
                        monoFont
                    )
                    RuntimeMetadataRow("Lag Warnings", lagWarnings.toString(), monoFont)
                    RuntimeMetadataRow(
                        "Engine & Java",
                        "${server.type.displayName} ${server.version} (Java ${server.javaVersion})",
                        monoFont,
                        showDivider = false
                    )
                }
            }

            MineServeSettingsGroup(
                title = "JVM Flags & Garbage Collection",
                modifier = Modifier.weight(1f)
            ) {
                val isAikarActive = server.jvmArgs.contains("-XX:+UseG1GC") &&
                    server.jvmArgs.contains("-XX:G1NewSizePercent=30")
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Active JVM Arguments",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = extended.terminalSurface,
                        contentColor = extended.terminalOnSurface,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = server.jvmArgs.ifBlank { "(Default JVM flags)" },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            viewModel.serverRepository.updateServer(server.copy(jvmArgs = DESKTOP_AIKAR_FLAGS))
                            viewModel.showToast("Applied Aikar's G1GC Flags! Restart server to apply.")
                        },
                        enabled = !isAikarActive,
                        shape = ShapeDefaults.Medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isAikarActive) colorScheme.primaryContainer else colorScheme.secondaryContainer,
                            contentColor = if (isAikarActive) colorScheme.onPrimaryContainer else colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isAikarActive) Icons.Rounded.Check else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAikarActive) "Aikar's G1GC Flags Active" else "Apply Aikar's G1GC Preset",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuntimeMetadataRow(
    label: String,
    value: String,
    monoFont: androidx.compose.ui.text.font.FontFamily,
    showDivider: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    MineServeTableRow(
        showDivider = showDivider,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        MineServeTableMonoText(
            text = value,
            color = colorScheme.onSurface
        )
    }
}

private fun formatDesktopUptime(seconds: Long): String {
    if (seconds <= 0) return "00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}
