package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.ui.DesktopColors
import com.devwithzachary.mineserve.desktop.ui.DesktopPanelCard
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.util.Locale

private const val DESKTOP_AIKAR_FLAGS =
    "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"

@Composable
fun DesktopConsoleTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    metrics: ServerMetrics?
) {
    val refreshTriggers by viewModel.processManager.refreshTriggers.collectAsState()
    val trigger = refreshTriggers[server.id] ?: 0L
    val lines = remember(server.id, trigger) {
        viewModel.processManager.getRecentConsoleLines(server.id, 2000)
    }
    var commandText by remember(server.id) { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.scrollToItem(lines.size - 1)
        }
    }

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
            // Left: Live Monospace Terminal + Command Input
            DesktopPanelCard(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Server Console (${lines.size} lines)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    OutlinedButton(
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
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Copy Console Output", fontSize = 11.sp, color = DesktopColors.Slate300)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF06090F),
                    border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    SelectionContainer {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (lines.isEmpty()) {
                                item {
                                    Text(
                                        text = "[MineServe] Server console ready. Press Start to launch ${server.name}.",
                                        color = DesktopColors.Slate400,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                itemsIndexed(lines) { _, line ->
                                    val lineColor = when {
                                        line.contains("ERROR", ignoreCase = true) || line.contains("Exception", ignoreCase = true) -> DesktopColors.RedstoneLight
                                        line.contains("WARN", ignoreCase = true) -> DesktopColors.AmberLight
                                        line.contains("Done (", ignoreCase = true) -> DesktopColors.EmeraldLight
                                        else -> Color(0xFFD1D5DB)
                                    }
                                    Text(
                                        text = line,
                                        color = lineColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commandText,
                        onValueChange = { commandText = it },
                        placeholder = { Text("Enter server command (e.g. say Hello, list, op Steve, save-all)...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                    submitCommand()
                                    true
                                } else {
                                    false
                                }
                            }
                    )
                    Button(
                        onClick = { submitCommand() },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Right Side-by-Side Inspector on Wide Desktop Windows
            if (showSideInspector) {
                Column(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Quick Commands", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickCmdBtn("save-all", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "save-all") }
                            QuickCmdBtn("list", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "list") }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickCmdBtn("time set day", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "time set day") }
                            QuickCmdBtn("weather clear", Modifier.weight(1f)) { viewModel.processManager.sendCommand(server.id, "weather clear") }
                        }
                    }

                    DesktopPanelCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        val players = metrics?.onlinePlayers ?: emptyList()
                        Text(
                            text = "Online Players (${players.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (players.isEmpty()) {
                            Text("No players currently online.", color = DesktopColors.Slate400, fontSize = 12.sp)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsIndexed(players) { _, player ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DesktopColors.Slate900,
                                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(player, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(30.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 11.sp, color = DesktopColors.Slate300)
    }
}

@Composable
fun DesktopPerformanceTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    metrics: ServerMetrics?
) {
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
    val tpsRatio = (tps / 20.0).coerceIn(0.0, 1.0).toFloat()

    val serverDir = remember(server.id) { viewModel.serverRepository.getServerDirectory(server.id) }
    val storageBytes = remember(server.id, uptimeSeconds / 10) {
        FileStorageUtils.calculateDirectorySize(serverDir)
    }
    val totalDiskBytes = remember(server.id) { serverDir.totalSpace }
    val freeDiskBytes = remember(server.id, uptimeSeconds / 10) { serverDir.usableSpace }
    val usedDiskBytes = (totalDiskBytes - freeDiskBytes).coerceAtLeast(0L)
    val diskRatio = if (totalDiskBytes > 0L) (usedDiskBytes.toFloat() / totalDiskBytes.toFloat()).coerceIn(0f, 1f) else 0f
    val isLowStorage = freeDiskBytes < 1024L * 1024L * 1024L
    val hostMem = remember(uptimeSeconds) { LinuxHostTelemetry.getMemoryInfo() }
    val hostUsedMb = (hostMem.totalMb - hostMem.availableMb).coerceAtLeast(0L)

    val cpuColor = when {
        cpuPct >= 80f -> DesktopColors.RedstonePrimary
        cpuPct >= 50f -> DesktopColors.AmberWarning
        else -> DesktopColors.EmeraldPrimary
    }
    val ramColor = when {
        ramRatio >= 0.85f -> DesktopColors.RedstonePrimary
        ramRatio >= 0.65f -> DesktopColors.AmberWarning
        else -> DesktopColors.EmeraldPrimary
    }
    val tpsColor = when {
        !isOnline -> DesktopColors.Slate400
        tps >= 19.0 -> DesktopColors.EmeraldPrimary
        tps >= 15.0 -> DesktopColors.AmberWarning
        else -> DesktopColors.RedstonePrimary
    }
    val diskColor = when {
        diskRatio >= 0.90f -> DesktopColors.RedstonePrimary
        diskRatio >= 0.75f -> DesktopColors.AmberWarning
        else -> DesktopColors.EmeraldLight
    }

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
        // Offline Status Banner
        if (!isOnline) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DesktopColors.Slate800.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
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
                            .background(DesktopColors.Slate400)
                    )
                    Text(
                        text = "Server is currently offline. Start the server to view live CPU, RAM, and rolling TPS/MSPT telemetry.",
                        fontSize = 12.sp,
                        color = DesktopColors.Slate400
                    )
                }
            }
        }

        // Low Storage Warning Banner
        if (isLowStorage) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DesktopColors.RedstonePrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, DesktopColors.RedstonePrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DesktopColors.RedstoneLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Low Host Storage Warning",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Only $formattedFreeDisk free space remaining. World saving or chunk generation may be interrupted if storage is exhausted.",
                            fontSize = 12.sp,
                            color = DesktopColors.Slate400
                        )
                    }
                }
            }
        }

        // 4 Quick Telemetry Tiles (Matching Android MetricTile layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DesktopMetricTile(
                icon = Icons.Default.Speed,
                iconColor = cpuColor,
                title = "CPU Usage",
                value = if (isOnline) String.format(Locale.US, "%.1f%%", cpuPct) else "0.0%",
                subtitle = if (isOnline) "Single-Core Load" else "Engine Idle",
                progress = (cpuPct / 100f).coerceIn(0f, 1f),
                progressColor = cpuColor,
                modifier = Modifier.weight(1f)
            )
            DesktopMetricTile(
                icon = Icons.Default.Memory,
                iconColor = if (server.type.isBedrockNative) DesktopColors.EmeraldPrimary else ramColor,
                title = "RAM Memory",
                value = if (isOnline) "$ramUsedMb MB" else "0 MB",
                subtitle = if (server.type.isBedrockNative) "Native C++ RSS" else "$ramPercentage% of $ramMaxMb MB",
                progress = if (server.type.isBedrockNative) 0f else ramRatio,
                progressColor = ramColor,
                showProgress = !server.type.isBedrockNative,
                modifier = Modifier.weight(1f)
            )
            DesktopMetricTile(
                icon = Icons.Default.Storage,
                iconColor = DesktopColors.EmeraldLight,
                title = "Disk Storage",
                value = formattedDisk,
                subtitle = "$formattedFreeDisk Available",
                progress = diskRatio,
                progressColor = diskColor,
                modifier = Modifier.weight(1f)
            )
            DesktopMetricTile(
                icon = if (isOnline) (if (tps >= 19.0) Icons.Default.CheckCircle else Icons.Default.Warning) else Icons.Default.Timer,
                iconColor = tpsColor,
                title = "Health & TPS",
                value = if (isOnline) String.format(Locale.US, "%.1f TPS", tps) else "Offline",
                subtitle = if (isOnline) "${String.format(Locale.US, "%.1f", mspt)}ms / 50ms tick (Up $formattedUptime)" else "Process Stopped",
                progress = if (isOnline) tpsRatio else 0f,
                progressColor = tpsColor,
                modifier = Modifier.weight(1f)
            )
        }

        // Rolling Live Telemetry Graph Card (Matching Android RollingTelemetryChartCard)
        DesktopRollingTelemetryChartCard(
            tpsHistory = tpsHistory,
            msptHistory = msptHistory,
            currentTps = tps,
            currentMspt = mspt,
            isOnline = isOnline
        )

        // Lag Diagnostics Card (if lagging)
        if (isRunning && (mspt > 50.0 || tps < 18.0 || lagWarnings > 0)) {
            DesktopPanelCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = DesktopColors.AmberWarning.copy(alpha = 0.6f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DesktopColors.AmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Server Lag Diagnostic Detected",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "The game loop is taking ${String.format(Locale.US, "%.1f", mspt)} ms per tick, exceeding the 50.0 ms tick budget. Players may notice delayed block breaks, mob freezing, or rubberbanding.",
                    fontSize = 12.sp,
                    color = DesktopColors.Slate400
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Recommended Optimizations:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DesktopColors.EmeraldLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("- Reduce view-distance to 6-8 and simulation-distance to 4 in Server Properties.", fontSize = 12.sp, color = DesktopColors.Slate300)
                Text("- Check for runaway entity breeding or dense animal farms near world spawn.", fontSize = 12.sp, color = DesktopColors.Slate300)
                if (!server.type.isBedrockNative) {
                    Text("- Apply Aikar's optimized G1GC JVM flags below to minimize garbage collection pauses.", fontSize = 12.sp, color = DesktopColors.Slate300)
                }
            }
        }

        // Side-by-Side Engine Health & Game Loop + Memory (RAM) Deep-Dive Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Health & Game Loop Card
            DesktopPanelCard(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = tpsColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Engine Health & Game Loop",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (isOnline) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tpsColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = when {
                                    tps >= 19.5 -> "Optimal"
                                    tps >= 15.0 -> "Moderate"
                                    else -> "Lag"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = tpsColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (isOnline) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { tpsRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = tpsColor,
                        trackColor = DesktopColors.Slate800
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopPerfInfoRow("Target Tick Rate", "20.0 TPS (50.0 ms / tick)")
                    DesktopPerfInfoRow(
                        "Current Tick Rate",
                        if (isOnline) "${String.format(Locale.US, "%.1f", tps)} TPS (${(tps / 20.0 * 100).toInt()}%)" else "0.0 TPS"
                    )
                    DesktopPerfInfoRow(
                        "Tick Processing Time (MSPT)",
                        if (isOnline) "${String.format(Locale.US, "%.1f", mspt)} ms (Budget: 50.0 ms)" else "0.0 ms"
                    )
                    DesktopPerfInfoRow(
                        "Tick Budget Headroom",
                        if (isOnline) {
                            val headroom = 50.0 - mspt
                            if (headroom >= 0.0) "+${String.format(Locale.US, "%.1f", headroom)} ms headroom"
                            else "-${String.format(Locale.US, "%.1f", -headroom)} ms behind"
                        } else "N/A (Stopped)"
                    )
                    DesktopPerfInfoRow(
                        "Game Loop State",
                        when {
                            !isOnline -> "Process Stopped"
                            tps >= 19.5 -> "Smooth (Normal game loop)"
                            tps >= 15.0 -> "Slight Delay (Catching up)"
                            else -> "Overloaded (Server lagging)"
                        }
                    )
                    DesktopPerfInfoRow(
                        "Overload Warnings Logged",
                        if (isOnline) {
                            if (lagWarnings == 0) "0 (Smooth game loop)" else "$lagWarnings detected (Can't keep up)"
                        } else "None (Stopped)"
                    )
                }
            }

            // Memory (RAM) Deep-Dive Card
            DesktopPanelCard(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = DesktopColors.EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Memory (RAM)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (isOnline && !server.type.isBedrockNative) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ramColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$ramPercentage% Allocated",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ramColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (!server.type.isBedrockNative) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { ramRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ramColor,
                        trackColor = DesktopColors.Slate800
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (server.type.isBedrockNative) {
                        DesktopPerfInfoRow("Memory Management", "Native C++ (OS-managed)")
                        DesktopPerfInfoRow("Active Resident Usage (RSS)", if (isOnline) "$ramUsedMb MB" else "0 MB")
                    } else {
                        DesktopPerfInfoRow("Allocated Max Heap (-Xmx)", "$ramMaxMb MB")
                        DesktopPerfInfoRow("Initial Heap (-Xms)", "${(ramMaxMb / 2).coerceAtLeast(256)} MB")
                        DesktopPerfInfoRow("Active Resident Usage (RSS)", if (isOnline) "$ramUsedMb MB" else "0 MB")
                        DesktopPerfInfoRow(
                            "Available Heap Headroom",
                            if (isOnline) "${(ramMaxMb - ramUsedMb).coerceAtLeast(0)} MB" else "$ramMaxMb MB"
                        )
                        DesktopPerfInfoRow("Garbage Collector", "G1GC (ParallelRefProcEnabled)")
                    }
                    DesktopPerfInfoRow(
                        "Host System Memory",
                        "$hostUsedMb / ${hostMem.totalMb} MB (${hostMem.availableMb} MB free)"
                    )
                }
            }
        }

        // JVM Tuning & Garbage Collection (Aikar's G1GC Flags) Card
        if (!server.type.isBedrockNative) {
            val hasAikar = server.jvmArgs.contains("G1NewSizePercent=30")
            DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = DesktopColors.EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "JVM Tuning & Garbage Collection",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aikar's optimized G1GC flags tune Java memory allocation for minimal pause times and reduced CPU spikes on dedicated Minecraft servers.",
                    fontSize = 12.sp,
                    color = DesktopColors.Slate400
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = server.jvmArgs,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DesktopColors.Slate300,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DesktopColors.Slate950, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (hasAikar) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DesktopColors.EmeraldDark.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, DesktopColors.EmeraldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DesktopColors.EmeraldLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Aikar's Optimized GC Flags are actively applied.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DesktopColors.EmeraldLight
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.serverRepository.updateServer(server.copy(jvmArgs = DESKTOP_AIKAR_FLAGS))
                            viewModel.showToast("Aikar's JVM flags applied! Restart server to take effect.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Aikar's Flags to Server", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Side-by-Side Process & CPU Diagnostics + Disk & Storage Footprint Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DesktopPanelCard(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = null,
                        tint = DesktopColors.EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Process & CPU Diagnostics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopPerfInfoRow("Process Status", status.name)
                    DesktopPerfInfoRow("Process ID (PID)", if (pid > 0) pid.toString() else "None (Stopped)")
                    DesktopPerfInfoRow("Uptime", if (isOnline) formattedUptime else "00:00")
                    DesktopPerfInfoRow(
                        if (server.type.isBedrockNative) "Runtime Engine" else "Java Runtime",
                        if (server.type.isBedrockNative) "Native Linux x86_64" else "OpenJDK ${server.javaVersion} (Adoptium Temurin)"
                    )
                    DesktopPerfInfoRow("Execution Engine", "Native Linux Process (Zero Overhead)")
                    DesktopPerfInfoRow(
                        "Server Port",
                        if (server.type.isBedrockNative) "${server.port} (UDP / RakNet)" else "${server.port} (TCP/UDP)"
                    )
                }
            }

            DesktopPanelCard(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = DesktopColors.EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Disk & Storage Footprint",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopPerfInfoRow("Total Server Folder Size", formattedDisk)
                    DesktopPerfInfoRow("Available Host Disk Space", formattedFreeDisk)
                    DesktopPerfInfoRow("Host Directory", serverDir.absolutePath)
                    DesktopPerfInfoRow("Primary Jar File", server.jarFileName)
                    DesktopPerfInfoRow("Storage Contents", "World chunks, logs, configs, plugins/mods")
                }
            }
        }
    }
}

@Composable
private fun DesktopRollingTelemetryChartCard(
    tpsHistory: List<Float>,
    msptHistory: List<Float>,
    currentTps: Double,
    currentMspt: Double,
    isOnline: Boolean
) {
    var selectedMetric by remember { mutableIntStateOf(0) } // 0: TPS, 1: MSPT

    DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = DesktopColors.EmeraldLight,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Rolling Live Telemetry",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Switcher Pills: TPS / MSPT
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DesktopColors.Slate800)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedMetric == 0) DesktopColors.EmeraldDark else Color.Transparent)
                        .clickable { selectedMetric = 0 }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "TPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMetric == 0) DesktopColors.EmeraldLight else DesktopColors.Slate400
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedMetric == 1) DesktopColors.EmeraldDark else Color.Transparent)
                        .clickable { selectedMetric = 1 }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "MSPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMetric == 1) DesktopColors.EmeraldLight else DesktopColors.Slate400
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val isMsptUnderTarget = currentMspt <= 50.0
        val dataPoints = if (selectedMetric == 0) tpsHistory else msptHistory
        val maxValue = if (selectedMetric == 0) 20f else 100f
        val targetValue = if (selectedMetric == 0) 20f else 50f
        val primaryColor = if (selectedMetric == 0) {
            DesktopColors.EmeraldPrimary
        } else if (isMsptUnderTarget) {
            DesktopColors.EmeraldPrimary
        } else {
            DesktopColors.AmberWarning
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedMetric == 0) "Target: 20.0 TPS (Dashed Line)" else "Target: < 50.0 ms (Tick Budget)",
                fontSize = 12.sp,
                color = DesktopColors.Slate400
            )
            Text(
                text = when {
                    !isOnline -> "Offline"
                    selectedMetric == 0 -> String.format(Locale.US, "Latest: %.1f TPS", currentTps)
                    else -> String.format(Locale.US, "Latest: %.1f ms", currentMspt)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isOnline) DesktopColors.Slate400 else if (selectedMetric == 0 || isMsptUnderTarget) DesktopColors.EmeraldLight else DesktopColors.AmberLight
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DesktopColors.Slate900.copy(alpha = 0.6f))
        ) {
            if (!isOnline || dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (!isOnline) "Server is stopped" else "Collecting telemetry...",
                        fontSize = 12.sp,
                        color = DesktopColors.Slate400
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    val w = size.width
                    val h = size.height

                    val targetY = h - (targetValue / maxValue) * h
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(0f, targetY),
                        end = Offset(w, targetY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    val count = dataPoints.size
                    val stepX = if (count > 1) w / (count - 1) else w
                    val points = dataPoints.mapIndexed { i, value ->
                        val x = i * stepX
                        val clamped = value.coerceIn(0f, maxValue)
                        val y = h - (clamped / maxValue) * h
                        Offset(x, y)
                    }

                    if (points.isNotEmpty()) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (p in points) lineTo(p.x, p.y)
                        }
                        val fillPath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (p in points) lineTo(p.x, p.y)
                            lineTo(points.last().x, h)
                            lineTo(points.first().x, h)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.30f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.5f)
                        )

                        val lastPoint = points.last()
                        drawCircle(color = primaryColor, radius = 5.5f, center = lastPoint)
                        drawCircle(color = Color.White, radius = 2.5f, center = lastPoint)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopMetricTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier,
    showProgress: Boolean = true
) {
    DesktopPanelCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = DesktopColors.Slate400
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (showProgress) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = DesktopColors.Slate800
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = DesktopColors.Slate400
        )
    }
}

@Composable
private fun DesktopPerfInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = DesktopColors.Slate400,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
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
