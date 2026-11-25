package com.devwithzachary.mineserve.ui.screens.detail

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MetricTelemetryTile
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeTelemetryChart
import com.devwithzachary.mineserve.designsystem.components.TelemetryChartSeries
import com.devwithzachary.mineserve.designsystem.components.TelemetrySeriesColor
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.util.Locale

const val AIKAR_FLAGS =
    "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"

@Composable
fun PerformanceTab(
    server: MinecraftServer,
    status: ServerStatus,
    metrics: ServerMetrics?,
    storageBytes: Long,
    onUpdateServer: ((MinecraftServer) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

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
    val playerCount = if (isOnline) (metrics?.onlinePlayerCount ?: 0) else 0

    val lagWarnings = if (isOnline) (metrics?.lagWarningsCount ?: 0) else 0
    val pid = if (isOnline) (metrics?.pid ?: -1) else -1
    val formattedDisk = formatStorageSize(storageBytes)

    val totalDiskBytes = remember(isInspection) {
        if (isInspection) 64L * 1024L * 1024L * 1024L else context.filesDir?.totalSpace ?: 0L
    }
    val freeDiskBytes = remember(isInspection) {
        if (isInspection) 38L * 1024L * 1024L * 1024L else context.filesDir?.usableSpace ?: 0L
    }
    val usedDiskBytes = (totalDiskBytes - freeDiskBytes).coerceAtLeast(0L)
    val isLowStorage = freeDiskBytes < 1024L * 1024L * 1024L

    val diskRatio = if (totalDiskBytes > 0L) {
        (usedDiskBytes.toFloat() / totalDiskBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedDiskProgress by animateFloatAsState(targetValue = diskRatio, label = "diskProgress")
    val animatedCpuProgress by animateFloatAsState(targetValue = (cpuPct / 100f).coerceIn(0f, 1f), label = "cpuProgress")
    val animatedRamProgress by animateFloatAsState(targetValue = ramRatio, label = "ramProgress")
    val tpsLoadProgress = if (isOnline) ((20.0 - tps).coerceIn(0.0, 20.0) / 20.0).toFloat() else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        if (!isOnline) {
            Surface(
                shape = ShapeDefaults.Medium,
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .let { mod -> mod }
                    ) {
                        Surface(color = colorScheme.outline, shape = CircleShape, modifier = Modifier.fillMaxSize()) {}
                    }
                    Text(
                        text = "Server is currently offline. Start the server to view live CPU, Memory, and TPS / MSPT telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isLowStorage) {
            Surface(
                shape = ShapeDefaults.Medium,
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
                            text = "Low Device Storage Warning",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Only ${formatStorageSize(freeDiskBytes)} free space remaining. World saving may be interrupted if storage is exhausted.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Standardized 2x2 MetricTelemetryTile Grid: CPU, Memory, TPS / MSPT, Players
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
        ) {
            MetricTelemetryTile(
                title = "CPU",
                value = if (isOnline) String.format(Locale.US, "%.1f%%", cpuPct) else "0.0%",
                progress = animatedCpuProgress,
                supportingText = if (isOnline) "Active Load" else "Engine Idle",
                icon = Icons.Rounded.Speed,
                modifier = Modifier.weight(1f)
            )

            MetricTelemetryTile(
                title = "Memory",
                value = if (isOnline) "$ramUsedMb MB" else "0 MB",
                progress = if (server.type.isBedrockNative) 0f else animatedRamProgress,
                supportingText = if (server.type.isBedrockNative) "Native C++ RSS" else "$ramPercentage% of $ramMaxMb MB",
                icon = Icons.Rounded.Memory,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
        ) {
            MetricTelemetryTile(
                title = "TPS / MSPT",
                value = if (isOnline) String.format(Locale.US, "%.1f TPS", tps) else "Offline",
                progress = tpsLoadProgress,
                supportingText = if (isOnline) "${String.format(Locale.US, "%.1f", mspt)} ms / 50 ms" else "Process Stopped",
                icon = Icons.Rounded.Timer,
                modifier = Modifier.weight(1f)
            )

            MetricTelemetryTile(
                title = "Players",
                value = playerCount.toString(),
                progress = (playerCount / 20f).coerceIn(0f, 1f),
                supportingText = if (isOnline) "Online now" else "Server stopped",
                icon = Icons.Rounded.People,
                modifier = Modifier.weight(1f)
            )
        }

        // Rolling Live Telemetry Chart using shared MineServeTelemetryChart
        RollingTelemetryChartCard(
            tpsHistory = tpsHistory,
            msptHistory = msptHistory,
            currentTps = tps,
            currentMspt = mspt,
            isOnline = isOnline
        )

        // Memory Usage Chart (Diamond series)
        val memorySampleSeries = remember(ramUsedMb, ramMaxMb, isOnline) {
            val pct = if (isOnline && ramMaxMb > 0) (ramUsedMb.toFloat() / ramMaxMb.toFloat()) * 100f else 0f
            listOf(
                TelemetryChartSeries(
                    label = "Memory (%)",
                    values = if (isOnline) listOf(pct * 0.85f, pct * 0.92f, pct) else listOf(0f, 0f),
                    color = TelemetrySeriesColor.Diamond
                )
            )
        }
        MineServeTelemetryChart(
            title = "Memory",
            series = memorySampleSeries,
            currentValueText = if (isOnline) "$ramUsedMb / $ramMaxMb MB" else "0 MB",
            minValue = 0f,
            maxValue = 100f
        )

        if (isRunning && (mspt > 50.0 || tps < 18.0 || lagWarnings > 0)) {
            MineServeCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeDefaults.Large,
                containerColor = extended.warningContainer,
                contentColor = extended.onWarningContainer,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = extended.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Server Lag Diagnostic Detected",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "The game loop is taking ${String.format(Locale.US, "%.1f", mspt)} ms per tick, exceeding the 50.0 ms tick budget.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "- Reduce view-distance to 6-8 and simulation-distance to 4.\n- Check for dense entity farms near spawn." +
                        if (!server.type.isBedrockNative) "\n- Apply Aikar's G1GC JVM flags below." else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // JVM Tuning & Aikar's GC Presets Card (Java only)
        if (!server.type.isBedrockNative) {
            MineServeCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeDefaults.Large,
                containerColor = colorScheme.surfaceContainer,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "JVM Tuning & Garbage Collection",
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Aikar's optimized G1GC flags tune Java memory allocation for minimal pause times and reduced CPU spikes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    val hasAikar = server.jvmArgs.contains("G1NewSizePercent=30")
                    if (hasAikar) {
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer,
                            border = null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Aikar's Optimized GC Flags are actively applied.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else if (onUpdateServer != null) {
                        FilledTonalButton(
                            onClick = {
                                onUpdateServer(server.copy(jvmArgs = AIKAR_FLAGS))
                                Toast.makeText(context, "Aikar's JVM flags applied! Restart server to take effect.", Toast.LENGTH_SHORT).show()
                            },
                            shape = ShapeDefaults.Medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Aikar's Flags to Server")
                        }
                    }
                }
            }
        }

        // Process & CPU Diagnostics
        MineServeCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeDefaults.Large,
            containerColor = colorScheme.surfaceContainer,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeveloperBoard,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Process & CPU Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                PerformanceInfoRow(label = "Process Status", value = status.name, monoValue = true)
                PerformanceInfoRow(label = "Process ID (PID)", value = if (pid > 0) pid.toString() else "None", monoValue = true)
                PerformanceInfoRow(
                    label = if (server.type.isBedrockNative) "Runtime Engine" else "Java Runtime",
                    value = if (server.type.isBedrockNative) {
                        val isX86_64 = Build.SUPPORTED_ABIS.firstOrNull()?.contains("x86_64") == true
                        if (isX86_64) "Native Linux x86_64" else "Native x86_64 (qemu-x86_64)"
                    } else {
                        "OpenJDK ${server.javaVersion} (Headless)"
                    }
                )
                PerformanceInfoRow(
                    label = "Server Port",
                    value = if (server.type.isBedrockNative) "${server.port} (UDP)" else "${server.port} (TCP/UDP)",
                    monoValue = true
                )
            }
        }

        // Disk & Storage Footprint
        MineServeCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeDefaults.Large,
            containerColor = colorScheme.surfaceContainer,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Disk & Storage Footprint",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedDiskProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (diskRatio >= 0.90f) colorScheme.error else if (diskRatio >= 0.75f) extended.warning else colorScheme.primary,
                    trackColor = colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                PerformanceInfoRow(label = "Server Folder Size", value = formattedDisk, monoValue = true)
                PerformanceInfoRow(label = "Container Path", value = "/servers/${server.id}", monoValue = true)
                PerformanceInfoRow(label = "Primary Jar File", value = server.jarFileName, monoValue = true)
                PerformanceInfoRow(label = "Device Free Space", value = formatStorageSize(freeDiskBytes), monoValue = true)
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
    val options = listOf("TPS", "MSPT")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedMetric == index,
                    onClick = { selectedMetric = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        val dataPoints = if (selectedMetric == 0) tpsHistory else msptHistory
        val series = listOf(
            TelemetryChartSeries(
                label = if (selectedMetric == 0) "TPS (Target 20.0)" else "MSPT (Budget 50ms)",
                values = if (isOnline && dataPoints.size >= 2) dataPoints else listOf(0f, 0f),
                color = TelemetrySeriesColor.Emerald
            )
        )

        MineServeTelemetryChart(
            title = "TPS / MSPT",
            series = series,
            currentValueText = if (!isOnline) {
                "Offline"
            } else if (selectedMetric == 0) {
                String.format(Locale.US, "%.1f TPS", currentTps)
            } else {
                String.format(Locale.US, "%.1f ms", currentMspt)
            },
            minValue = 0f,
            maxValue = if (selectedMetric == 0) 20f else 100f
        )
    }
}

@Composable
private fun PerformanceInfoRow(
    label: String,
    value: String,
    monoValue: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            style = if (monoValue) {
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = monoFont,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                )
            } else {
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
            },
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatStorageSize(bytes: Long): String {
    return FileStorageUtils.formatBytes(bytes, includeKb = true, gbDecimals = 2)
}

@com.devwithzachary.mineserve.ui.preview.MineServeTabPreviews
@Composable
private fun PerformanceTabPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        PerformanceTab(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            status = ServerStatus.RUNNING,
            metrics = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleRunningMetrics,
            storageBytes = 482_000_000L
        )
    }
}
