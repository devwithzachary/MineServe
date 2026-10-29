package com.devwithzachary.mineserve.ui.screens.detail

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.util.Locale

const val AIKAR_FLAGS = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"

@Composable
fun PerformanceTab(
    server: MinecraftServer,
    status: ServerStatus,
    metrics: ServerMetrics?,
    storageBytes: Long,
    onUpdateServer: ((MinecraftServer) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isRunning = status == ServerStatus.RUNNING
    val isStarting = status == ServerStatus.STARTING
    val isOnline = isRunning || isStarting

    val cpuPct = if (isOnline) (metrics?.cpuPercentage ?: 0f) else 0f
    val ramUsedMb = if (isOnline) (metrics?.ramUsedMb ?: 0L) else 0L
    val ramMaxMb = server.allocatedRamMb.toLong()
    val ramRatio = if (ramMaxMb > 0) (ramUsedMb.toFloat() / ramMaxMb.toFloat()).coerceIn(0f, 1f) else 0f
    val ramPercentage = (ramRatio * 100f).toInt()

    val tps = if (isOnline) (metrics?.tps ?: 20.0) else 0.0
    val mspt = if (isOnline) (metrics?.mspt ?: 20.0) else 0.0
    val tpsHistory = metrics?.tpsHistory ?: emptyList()
    val msptHistory = metrics?.msptHistory ?: emptyList()

    val lagWarnings = if (isOnline) (metrics?.lagWarningsCount ?: 0) else 0
    val uptimeSeconds = if (isOnline) (metrics?.uptimeSeconds ?: 0L) else 0L
    val pid = if (isOnline) (metrics?.pid ?: -1) else -1

    val tpsRatio = (tps / 20.0).coerceIn(0.0, 1.0).toFloat()
    val formattedDisk = formatStorageSize(storageBytes)
    val formattedUptime = formatUptime(uptimeSeconds)

    val context = LocalContext.current
    val totalDiskBytes = remember { context.filesDir.totalSpace }
    val freeDiskBytes = remember { context.filesDir.usableSpace }
    val usedDiskBytes = (totalDiskBytes - freeDiskBytes).coerceAtLeast(0L)
    val isLowStorage = freeDiskBytes < 1024L * 1024L * 1024L // Less than 1 GB

    val diskRatio = if (totalDiskBytes > 0L) {
        (usedDiskBytes.toFloat() / totalDiskBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedDiskProgress by animateFloatAsState(targetValue = diskRatio, label = "diskProgress")
    val diskColor by animateColorAsState(
        targetValue = when {
            diskRatio >= 0.90f -> RedstoneRed
            diskRatio >= 0.75f -> GoldYellow
            else -> EmeraldLight
        },
        label = "diskColor"
    )

    val cpuColor by animateColorAsState(
        targetValue = when {
            cpuPct >= 80f -> RedstoneRed
            cpuPct >= 50f -> GoldYellow
            else -> EmeraldPrimary
        },
        label = "cpuColor"
    )

    val ramColor by animateColorAsState(
        targetValue = when {
            ramRatio >= 0.85f -> RedstoneRed
            ramRatio >= 0.65f -> GoldYellow
            else -> EmeraldPrimary
        },
        label = "ramColor"
    )

    val tpsColor by animateColorAsState(
        targetValue = when {
            !isOnline -> Slate400
            tps >= 19.0 -> EmeraldPrimary
            tps >= 15.0 -> GoldYellow
            else -> RedstoneRed
        },
        label = "tpsColor"
    )

    val animatedCpuProgress by animateFloatAsState(targetValue = (cpuPct / 100f).coerceIn(0f, 1f), label = "cpuProgress")
    val animatedRamProgress by animateFloatAsState(targetValue = ramRatio, label = "ramProgress")
    val animatedTpsProgress by animateFloatAsState(targetValue = tpsRatio, label = "tpsProgress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        // Status Bar Banner
        if (!isOnline) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate800.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, ObsidianCardBorder),
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
                            .background(Slate400)
                    )
                    Text(
                        text = "Server is currently offline. Start the server to view live CPU, RAM, and rolling TPS telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        }

        // Low Storage Warning Threshold Banner
        if (isLowStorage) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = RedstoneRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.5f)),
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
                        tint = RedstoneLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Low Device Storage Warning",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Only ${formatStorageSize(freeDiskBytes)} free space remaining. World saving or chunk generation may be interrupted if storage is exhausted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        }

        // Quick Telemetry Tiles (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
        ) {
            // CPU Tile
            MetricTile(
                icon = Icons.Default.Speed,
                iconColor = cpuColor,
                title = "CPU Usage",
                value = if (isOnline) String.format(Locale.US, "%.1f%%", cpuPct) else "0.0%",
                subtitle = if (isOnline) "Single-Core Load" else "Engine Idle",
                progress = animatedCpuProgress,
                progressColor = cpuColor,
                modifier = Modifier.weight(1f)
            )

            // RAM Tile
            MetricTile(
                icon = Icons.Default.Memory,
                iconColor = ramColor,
                title = "RAM Memory",
                value = if (isOnline) "$ramUsedMb MB" else "0 MB",
                subtitle = "$ramPercentage% of $ramMaxMb MB",
                progress = animatedRamProgress,
                progressColor = ramColor,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
        ) {
            // Disk Storage Tile
            MetricTile(
                icon = Icons.Default.Storage,
                iconColor = EmeraldLight,
                title = "Disk Storage",
                value = formattedDisk,
                subtitle = "${formatStorageSize(freeDiskBytes)} Available",
                progress = animatedDiskProgress,
                progressColor = diskColor,
                showProgress = true,
                modifier = Modifier.weight(1f)
            )

            // Health & TPS Tile
            MetricTile(
                icon = if (isOnline) (if (tps >= 19.0) Icons.Default.CheckCircle else Icons.Default.Warning) else Icons.Default.Timer,
                iconColor = tpsColor,
                title = "Health & TPS",
                value = if (isOnline) String.format(Locale.US, "%.1f TPS", tps) else "Offline",
                subtitle = if (isOnline) "${String.format(Locale.US, "%.1f", mspt)}ms / 50ms tick" else "Process Stopped",
                progress = if (isOnline) animatedTpsProgress else 0f,
                progressColor = tpsColor,
                showProgress = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Rolling Live Telemetry Graph (Canvas)
        RollingTelemetryChartCard(
            tpsHistory = tpsHistory,
            msptHistory = msptHistory,
            currentTps = tps,
            currentMspt = mspt,
            isOnline = isOnline
        )

        // Lag Diagnostics & Remediation Card (Triggered when running and MSPT > 50ms or TPS < 18.0)
        if (isRunning && (mspt > 50.0 || tps < 18.0 || lagWarnings > 0)) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, GoldYellow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GoldYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Server Lag Diagnostic Detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "The game loop is taking ${String.format(Locale.US, "%.1f", mspt)} ms per tick, exceeding the 50.0 ms tick budget. Players may notice delayed block breaks, mob freezing, or rubberbanding.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    HorizontalDivider(color = ObsidianCardBorder)

                    Text(
                        text = "Recommended Optimizations:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LagDiagnosticTipItem(tip = "Reduce view-distance to 6-8 and simulation-distance to 4 in Server Properties.")
                        LagDiagnosticTipItem(tip = "Check for runaway entity breeding or dense animal farms near world spawn.")
                        LagDiagnosticTipItem(tip = "Apply Aikar's optimized G1GC JVM flags to minimize mobile garbage collection pauses.")
                    }
                }
            }
        }

        // Engine Health & TPS Detailed Metrics Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            style = MaterialTheme.typography.titleMedium,
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
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tpsColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (isOnline) {
                    LinearProgressIndicator(
                        progress = { animatedTpsProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = tpsColor,
                        trackColor = Slate800
                    )
                }

                HorizontalDivider(color = ObsidianCardBorder)

                PerformanceInfoRow(
                    label = "Target Tick Rate",
                    value = "20.0 TPS (50.0 ms / tick)"
                )
                PerformanceInfoRow(
                    label = "Current Tick Rate",
                    value = if (isOnline) "${String.format(Locale.US, "%.1f", tps)} TPS (${(tps / 20.0 * 100).toInt()}%)" else "0.0 TPS"
                )
                PerformanceInfoRow(
                    label = "Tick Processing Time (MSPT)",
                    value = if (isOnline) "${String.format(Locale.US, "%.1f", mspt)} ms (Budget: 50.0 ms)" else "0.0 ms"
                )
                PerformanceInfoRow(
                    label = "Tick Budget Headroom",
                    value = if (isOnline) {
                        val headroom = 50.0 - mspt
                        if (headroom >= 0.0) {
                            "+${String.format(Locale.US, "%.1f", headroom)} ms headroom"
                        } else {
                            "-${String.format(Locale.US, "%.1f", -headroom)} ms behind"
                        }
                    } else {
                        "N/A (Stopped)"
                    }
                )
                PerformanceInfoRow(
                    label = "Game Loop State",
                    value = when {
                        !isOnline -> "Process Stopped"
                        tps >= 19.5 -> "Smooth (Normal game loop)"
                        tps >= 15.0 -> "Slight Delay (Catching up)"
                        else -> "Overloaded (Server lagging)"
                    }
                )
                PerformanceInfoRow(
                    label = "Overload Warnings Logged",
                    value = if (isOnline) {
                        if (lagWarnings == 0) "0 (Smooth game loop)" else "$lagWarnings detected (Can't keep up)"
                    } else {
                        "None (Stopped)"
                    }
                )
            }
        }

        // Memory Deep-Dive Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Memory (RAM)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (isOnline) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ramColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$ramPercentage% Allocated",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ramColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedRamProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ramColor,
                    trackColor = Slate800
                )

                HorizontalDivider(color = ObsidianCardBorder)

                PerformanceInfoRow(label = "Allocated Max Heap (-Xmx)", value = "$ramMaxMb MB")
                PerformanceInfoRow(label = "Initial Heap (-Xms)", value = "512 MB")
                PerformanceInfoRow(
                    label = "Active Resident Usage",
                    value = if (isOnline) "$ramUsedMb MB" else "0 MB"
                )
                PerformanceInfoRow(
                    label = "Available Headroom",
                    value = if (isOnline) "${(ramMaxMb - ramUsedMb).coerceAtLeast(0)} MB" else "$ramMaxMb MB"
                )
                PerformanceInfoRow(label = "Garbage Collector", value = "G1GC (ParallelRefProcEnabled)")
            }
        }

        // JVM Tuning & Aikar's GC Presets Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "JVM Tuning & Garbage Collection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = "Aikar's optimized G1GC flags tune Java memory allocation for minimal pause times and reduced CPU spikes on mobile Android processors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                val hasAikar = server.jvmArgs.contains("G1NewSizePercent=30")
                if (hasAikar) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldDark.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
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
                                tint = EmeraldLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Aikar's Optimized GC Flags are actively applied.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldLight
                            )
                        }
                    }
                } else if (onUpdateServer != null) {
                    Button(
                        onClick = {
                            val updated = server.copy(jvmArgs = AIKAR_FLAGS)
                            onUpdateServer(updated)
                            Toast.makeText(context, "Aikar's JVM flags applied! Restart server to take effect.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Aikar's Flags to Server", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CPU & Process Information Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeveloperBoard,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Process & CPU Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                HorizontalDivider(color = ObsidianCardBorder)

                PerformanceInfoRow(
                    label = "Process Status",
                    value = status.name
                )
                PerformanceInfoRow(
                    label = "Process ID (PID)",
                    value = if (pid > 0) pid.toString() else "None (Stopped)"
                )
                PerformanceInfoRow(
                    label = "Java Runtime",
                    value = "OpenJDK ${server.javaVersion} (Headless)"
                )
                PerformanceInfoRow(
                    label = "Isolation Engine",
                    value = "PRoot 5.3 (User-Space Sandbox)"
                )
                PerformanceInfoRow(
                    label = "Server Port",
                    value = "${server.port} (TCP/UDP)"
                )
            }
        }

        // Storage & Folder Details Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Disk & Storage Footprint",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                HorizontalDivider(color = ObsidianCardBorder)

                PerformanceInfoRow(
                    label = "Total Server Folder Size",
                    value = formattedDisk
                )
                PerformanceInfoRow(
                    label = "Container Path",
                    value = "/servers/${server.id}"
                )
                PerformanceInfoRow(
                    label = "Primary Jar File",
                    value = server.jarFileName
                )
                PerformanceInfoRow(
                    label = "Storage Contents",
                    value = "World chunks, logs, configs, plugins/mods"
                )
            }
        }
    }
}

@Composable
fun RollingTelemetryChartCard(
    tpsHistory: List<Float>,
    msptHistory: List<Float>,
    currentTps: Double,
    currentMspt: Double,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableIntStateOf(0) } // 0: TPS, 1: MSPT

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        tint = EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Rolling Live Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Switcher Pills: TPS / MSPT
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedMetric == 0) EmeraldDark else Color.Transparent)
                            .clickable { selectedMetric = 0 }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TPS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMetric == 0) EmeraldLight else Slate400
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedMetric == 1) EmeraldDark else Color.Transparent)
                            .clickable { selectedMetric = 1 }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MSPT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMetric == 1) EmeraldLight else Slate400
                        )
                    }
                }
            }

            val isMsptUnderTarget = currentMspt <= 50f
            val dataPoints = if (selectedMetric == 0) tpsHistory else msptHistory
            val maxValue = if (selectedMetric == 0) 20f else 100f
            val targetValue = if (selectedMetric == 0) 20f else 50f
            val primaryColor = if (selectedMetric == 0) {
                EmeraldPrimary
            } else if (isMsptUnderTarget) {
                EmeraldPrimary
            } else {
                GoldYellow
            }

            // Legend / Current Value display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMetric == 0) {
                        "Target: 20.0 TPS (Dashed Line)"
                    } else {
                        "Target: < 50.0 ms (Tick Budget)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                Text(
                    text = if (!isOnline) {
                        "Offline"
                    } else if (selectedMetric == 0) {
                        String.format(Locale.US, "Latest: %.1f TPS", currentTps)
                    } else {
                        String.format(Locale.US, "Latest: %.1f ms", currentMspt)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (!isOnline) Slate400 else if (selectedMetric == 0) EmeraldLight else if (isMsptUnderTarget) EmeraldLight else GoldYellow
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900.copy(alpha = 0.5f))
            ) {
                if (!isOnline || dataPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!isOnline) "Server is stopped" else "Collecting telemetry...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                        val w = size.width
                        val h = size.height

                        // Draw target threshold line (dashed)
                        val targetY = h - (targetValue / maxValue) * h
                        drawLine(
                            color = primaryColor.copy(alpha = 0.4f),
                            start = Offset(0f, targetY),
                            end = Offset(w, targetY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Calculate points
                        val count = dataPoints.size
                        val stepX = if (count > 1) w / (count - 1) else w
                        val points = dataPoints.mapIndexed { i, value ->
                            val x = i * stepX
                            val clamped = value.coerceIn(0f, maxValue)
                            val y = h - (clamped / maxValue) * h
                            Offset(x, y)
                        }

                        if (points.isNotEmpty()) {
                            // Path for line
                            val linePath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (p in points) {
                                    lineTo(p.x, p.y)
                                }
                            }

                            // Path for gradient fill
                            val fillPath = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (p in points) {
                                    lineTo(p.x, p.y)
                                }
                                lineTo(points.last().x, h)
                                lineTo(points.first().x, h)
                                close()
                            }

                            // Draw gradient fill under curve
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.30f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                            )

                            // Draw curve stroke
                            drawPath(
                                path = linePath,
                                color = primaryColor,
                                style = Stroke(width = 3.5f)
                            )

                            // Draw dot on latest point
                            val lastPoint = points.last()
                            drawCircle(
                                color = primaryColor,
                                radius = 5f,
                                center = lastPoint
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f,
                                center = lastPoint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LagDiagnosticTipItem(tip: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", color = EmeraldLight, fontWeight = FontWeight.Bold)
        Text(
            text = tip,
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )
    }
}

@Composable
fun MetricTile(
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
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (showProgress) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = progressColor,
                    trackColor = Slate800
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
    }
}

@Composable
fun PerformanceInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun formatStorageSize(bytes: Long): String {
    return FileStorageUtils.formatBytes(bytes, includeKb = false, gbDecimals = 2)
}

private fun formatUptime(seconds: Long): String {
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
