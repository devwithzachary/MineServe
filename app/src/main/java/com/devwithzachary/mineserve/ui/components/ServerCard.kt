package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
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
import com.devwithzachary.mineserve.ui.theme.Slate700
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Language
import com.devwithzachary.mineserve.tunnel.TunnelState

@Composable
fun ServerCard(
    server: MinecraftServer,
    status: ServerStatus,
    metrics: ServerMetrics?,
    storageBytes: Long,
    tunnelState: TunnelState = TunnelState.Disconnected,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onConsoleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val statusText = when (status) {
        ServerStatus.RUNNING -> stringResource(R.string.status_online)
        ServerStatus.STARTING -> stringResource(R.string.status_starting)
        ServerStatus.STOPPING -> stringResource(R.string.status_stopping)
        ServerStatus.ERROR -> stringResource(R.string.status_error)
        ServerStatus.STOPPED -> stringResource(R.string.status_offline)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, if (isRunning) EmeraldPrimary.copy(alpha = 0.5f) else ObsidianCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, Software Badge, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${server.type.displayName} • ${server.version} • Port ${server.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Public Online Tunnel Line (own line before RAM/CPU)
            if (tunnelState is TunnelState.Connected || (tunnelState is TunnelState.Connecting && tunnelState.claimUrl != null)) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (tunnelState is TunnelState.Connected) EmeraldDark.copy(alpha = 0.25f) else DiamondCyan.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, if (tunnelState is TunnelState.Connected) EmeraldPrimary.copy(alpha = 0.4f) else DiamondCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = if (tunnelState is TunnelState.Connected) EmeraldLight else DiamondLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Public Link:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (tunnelState is TunnelState.Connected) tunnelState.fullAddress else "Setup needed (tap server to view)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (tunnelState is TunnelState.Connected) EmeraldLight else DiamondLight,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Live metrics or Specs with Per-Server Storage
            if (isRunning && metrics != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val ramPct = if (metrics.ramMaxMb > 0) metrics.ramUsedMb.toFloat() / metrics.ramMaxMb else 0f
                        ResourceBar(
                            label = stringResource(R.string.card_ram_label),
                            currentValue = "${metrics.ramUsedMb} MB / ${metrics.ramMaxMb} MB",
                            percentage = ramPct
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ResourceBar(
                            label = stringResource(R.string.card_cpu_label),
                            currentValue = "${metrics.cpuPercentage.toInt()}%",
                            percentage = metrics.cpuPercentage / 100f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = EmeraldLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (metrics.onlinePlayerCount == 0) {
                                stringResource(R.string.card_no_players)
                            } else {
                                stringResource(R.string.card_players_count_format, metrics.onlinePlayerCount)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = stringResource(R.string.card_storage_format, formatStorage(storageBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.card_ram_allocated_format, server.allocatedRamMb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.card_storage_format, formatStorage(storageBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.card_java_format, server.javaVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Actions: Start/Stop, Console, Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRunning || isStarting || isStopping) {
                    Button(
                        onClick = onStopClick,
                        enabled = !isStopping,
                        colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isStopping) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_stop_server), fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = onStartClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_start_server),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                OutlinedButton(
                    onClick = onConsoleClick,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate700),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_console))
                }
            }
        }
    }
}

private fun formatStorage(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}
