package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.ServerStatusBadge
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.tunnel.TunnelState
import com.devwithzachary.mineserve.util.FileStorageUtils

@OptIn(ExperimentalLayoutApi::class)
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
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val isRunning = status == ServerStatus.RUNNING
    val isStarting = status == ServerStatus.STARTING
    val isStopping = status == ServerStatus.STOPPING

    val statusLabel = when (status) {
        ServerStatus.RUNNING -> stringResource(R.string.status_online)
        ServerStatus.STARTING -> stringResource(R.string.status_starting)
        ServerStatus.STOPPING -> stringResource(R.string.status_stopping)
        ServerStatus.ERROR -> stringResource(R.string.status_error)
        ServerStatus.STOPPED -> stringResource(R.string.status_offline)
        ServerStatus.STANDBY -> "Standby"
    }

    MineServeCard(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        shape = ShapeDefaults.Large,
        containerColor = colorScheme.surfaceContainer,
        contentPadding = PaddingValues(16.dp)
    ) {
        // Header: Title, Software + Version + Port, and ServerStatusBadge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${server.type.displayName} ${server.version} - Port ${server.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ServerStatusBadge(
                state = status.toBadgeState(),
                label = statusLabel
            )
        }

        // Optional Public Tunnel Pill
        if (tunnelState is TunnelState.Connected || (tunnelState is TunnelState.Connecting && tunnelState.claimUrl != null)) {
            Spacer(modifier = Modifier.height(10.dp))
            val isConnected = tunnelState is TunnelState.Connected
            Surface(
                shape = ShapeDefaults.Small,
                color = if (isConnected) colorScheme.tertiaryContainer else extended.warningContainer,
                contentColor = if (isConnected) colorScheme.onTertiaryContainer else extended.onWarningContainer,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isConnected) tunnelState.fullAddress else "Tunnel claim needed",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = monoFont,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metadata Row: Compact surfaceContainerHigh chips in JetBrains Mono
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val ramText = if (isRunning && metrics != null) {
                "${formatRam(metrics.ramUsedMb)} / ${formatRam(metrics.ramMaxMb)}"
            } else {
                "${server.allocatedRamMb} MB"
            }
            MetadataMonoChip(text = ramText)

            val runtimeText = if (server.type.isBedrockNative) "Bedrock" else "Java ${server.javaVersion}"
            MetadataMonoChip(text = runtimeText)

            MetadataMonoChip(text = formatStorage(storageBytes))

            if (isRunning && metrics != null) {
                MetadataMonoChip(text = "CPU ${metrics.cpuPercentage.toInt()}%")
                MetadataMonoChip(
                    text = if (metrics.onlinePlayerCount == 1) "1 Player" else "${metrics.onlinePlayerCount} Players"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Row: Primary Start/Stop Button + FilledTonalButton Console
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRunning || isStarting || isStopping) {
                Button(
                    onClick = onStopClick,
                    enabled = !isStopping,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onErrorContainer,
                        disabledContainerColor = colorScheme.surfaceContainerHighest,
                        disabledContentColor = colorScheme.onSurfaceVariant
                    ),
                    shape = ShapeDefaults.Medium,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isStopping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_stop_server))
                    }
                }
            } else {
                Button(
                    onClick = onStartClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    shape = ShapeDefaults.Medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_start_server))
                }
            }

            FilledTonalButton(
                onClick = onConsoleClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colorScheme.secondaryContainer,
                    contentColor = colorScheme.onSecondaryContainer
                ),
                shape = ShapeDefaults.Medium,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_console))
            }
        }
    }
}

@Composable
private fun MetadataMonoChip(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    Surface(
        shape = ShapeDefaults.Small,
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurfaceVariant,
        border = null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = monoFont,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatStorage(bytes: Long): String {
    return FileStorageUtils.formatBytes(bytes, includeKb = false, gbDecimals = 1)
}

private fun formatRam(mb: Long): String {
    return if (mb > 1000) {
        val gb = mb / 1024.0
        val formatted = String.format(java.util.Locale.US, "%.1f", gb)
        if (formatted.endsWith(".0")) {
            "${formatted.substringBefore(".")} GB"
        } else {
            "$formatted GB"
        }
    } else {
        "$mb MB"
    }
}
