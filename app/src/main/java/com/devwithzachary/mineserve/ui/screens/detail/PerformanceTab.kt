package com.devwithzachary.mineserve.ui.screens.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import java.util.Locale

@Composable
fun PerformanceTab(
    server: MinecraftServer,
    status: ServerStatus,
    metrics: ServerMetrics?,
    storageBytes: Long,
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
    val uptimeSeconds = if (isOnline) (metrics?.uptimeSeconds ?: 0L) else 0L
    val pid = if (isOnline) (metrics?.pid ?: -1) else -1

    val formattedDisk = formatStorageSize(storageBytes)
    val formattedUptime = formatUptime(uptimeSeconds)

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

    val animatedCpuProgress by animateFloatAsState(targetValue = (cpuPct / 100f).coerceIn(0f, 1f), label = "cpuProgress")
    val animatedRamProgress by animateFloatAsState(targetValue = ramRatio, label = "ramProgress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "Server is currently offline. Start the server to view live CPU, RAM, and TPS telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        }

        // Quick Telemetry Tiles (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // CPU Tile
            MetricTile(
                icon = Icons.Default.Speed,
                iconColor = cpuColor,
                title = "CPU Usage",
                value = if (isOnline) String.format(Locale.US, "%.1f%%", cpuPct) else "0.0%",
                subtitle = if (isOnline) "Load on Android Cores" else "Engine Idle",
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
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Disk Storage Tile
            MetricTile(
                icon = Icons.Default.Storage,
                iconColor = EmeraldLight,
                title = "Disk Storage",
                value = formattedDisk,
                subtitle = "World, Logs & Files",
                progress = 0f,
                progressColor = EmeraldLight,
                showProgress = false,
                modifier = Modifier.weight(1f)
            )

            // Health & TPS Tile
            MetricTile(
                icon = if (isOnline) Icons.Default.CheckCircle else Icons.Default.Timer,
                iconColor = if (isOnline) EmeraldPrimary else Slate400,
                title = "Health & TPS",
                value = if (isOnline) String.format(Locale.US, "%.1f TPS", tps) else "Offline",
                subtitle = if (isOnline) "Uptime: $formattedUptime" else "Process Stopped",
                progress = 0f,
                progressColor = EmeraldPrimary,
                showProgress = false,
                modifier = Modifier.weight(1f)
            )
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
                            text = "Memory (RAM) Diagnostics",
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
            color = Slate400
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.1f MB", mb)
    }
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
