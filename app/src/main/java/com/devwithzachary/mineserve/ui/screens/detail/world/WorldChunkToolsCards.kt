package com.devwithzachary.mineserve.ui.screens.detail.world

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.engine.PregenState
import com.devwithzachary.mineserve.engine.RegionFileInfo
import com.devwithzachary.mineserve.engine.WorldManager
import com.devwithzachary.mineserve.engine.WorldPregenManager
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.repository.ServerRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChunkPreGenCard(
    server: MinecraftServer,
    isRunning: Boolean,
    onSendCommand: (String) -> Unit
) {
    if (server.type.isBedrockNative || server.type.isProxy) return
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val monoFont = LocalMineServeMonoFontFamily.current
    val serverDir = remember(server.id, isInspection) {
        if (isInspection) {
            java.io.File("/preview/servers/${server.id}")
        } else {
            val pRoot = PRootEngine(context)
            ServerRepository(context, pRoot).getServerDirectory(server.id)
        }
    }
    val allStates by WorldPregenManager.states.collectAsState()
    val pregenState = allStates[server.id] ?: PregenState()

    var selectedDim by remember { mutableStateOf(DimensionType.OVERWORLD) }
    var selectedRadius by remember { mutableIntStateOf(500) }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    MineServeCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Chunk Pre-Generator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Generate chunks around spawn while the server is empty to eliminate player exploration lag. Uses Chunky if installed or native /forceload stepping.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Dimension",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DimensionType.values().forEach { dim ->
                    FilterChip(
                        selected = selectedDim == dim,
                        onClick = { if (!pregenState.isRunning) selectedDim = dim },
                        label = { Text(dim.displayName) },
                        border = null,
                        colors = chipColors
                    )
                }
            }

            Text(
                text = "Radius (blocks around 0, 0)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(250, 500, 1000, 2000).forEach { r ->
                    FilterChip(
                        selected = selectedRadius == r,
                        onClick = { if (!pregenState.isRunning) selectedRadius = r },
                        label = { Text("${r}m", fontFamily = monoFont) },
                        border = null,
                        colors = chipColors
                    )
                }
            }

            if (pregenState.isRunning || pregenState.statusMessage != "Idle") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = pregenState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = monoFont,
                        color = if (pregenState.isRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (pregenState.isRunning) {
                        LinearProgressIndicator(
                            progress = { (pregenState.percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }

            if (!pregenState.isRunning) {
                Button(
                    onClick = {
                        if (!isRunning) {
                            Toast.makeText(context, "Start the server first to pre-generate chunks.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        WorldPregenManager.startPregen(
                            serverId = server.id,
                            serverDir = serverDir,
                            dimension = selectedDim,
                            centerX = 0,
                            centerZ = 0,
                            radiusBlocks = selectedRadius,
                            sendCommand = onSendCommand
                        )
                    },
                    enabled = isRunning,
                    shape = ShapeDefaults.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "Start Pre-Generation" else "Start Server to Pre-Generate",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = { WorldPregenManager.stopPregen(server.id, onSendCommand) },
                    shape = ShapeDefaults.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop Pre-Generation", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegionInspectorCard(
    server: MinecraftServer,
    isRunning: Boolean,
    levelName: String,
    refreshKey: Any,
    onWorldMutated: () -> Unit
) {
    if (server.type.isBedrockNative || server.type.isProxy) return
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val monoFont = LocalMineServeMonoFontFamily.current
    val serverDir = remember(server.id, isInspection) {
        if (isInspection) {
            java.io.File("/preview/servers/${server.id}")
        } else {
            val pRoot = PRootEngine(context)
            ServerRepository(context, pRoot).getServerDirectory(server.id)
        }
    }

    var regions by remember { mutableStateOf<List<RegionFileInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filterDim by remember { mutableStateOf<DimensionType?>(null) }
    var regionToDelete by remember { mutableStateOf<RegionFileInfo?>(null) }

    fun loadRegions() {
        scope.launch {
            isLoading = true
            regions = WorldManager.inspectRegions(serverDir, levelName)
            isLoading = false
        }
    }

    LaunchedEffect(server.id, refreshKey) {
        loadRegions()
    }

    val filtered = remember(regions, filterDim) {
        if (filterDim == null) regions else regions.filter { it.dimension == filterDim }
    }
    val maxBytes = remember(filtered) {
        (filtered.maxOfOrNull { it.sizeBytes } ?: 1L).coerceAtLeast(1L)
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    MineServeCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Region File Inspector (.mca)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { loadRegions() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh regions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Inspect individual Anvil (.mca) region files sorted by size to spot bloated or corrupted regions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val options: List<Pair<String, DimensionType?>> = listOf(
                    "All (${regions.size})" to null
                ) + DimensionType.values().map { dim ->
                    val count = regions.count { it.dimension == dim }
                    "${dim.displayName} ($count)" to dim
                }
                options.forEach { (label, dimVal) ->
                    FilterChip(
                        selected = filterDim == dimVal,
                        onClick = { filterDim = dimVal },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        border = null,
                        colors = chipColors
                    )
                }
            }

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Scanning .mca region headers...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filtered.isEmpty()) {
                Text(
                    text = "No .mca region files found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val topRegions = filtered.take(20)
                MineServeTableContainer {
                    topRegions.forEachIndexed { index, r ->
                        MineServeTableRow(showDivider = index < topRegions.lastIndex) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = r.fileName,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontFamily = monoFont,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        MineServeTableMonoText(
                                            text = "${r.dimension.displayName} - ${r.chunkCount}/1024 chunks - ${formatWorldFileSize(r.sizeBytes)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!isRunning) {
                                        IconButton(
                                            onClick = { regionToDelete = r },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete region",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { (r.sizeBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.02f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }
                }
                if (filtered.size > 20) {
                    Text(
                        text = "Showing top 20 largest regions of ${filtered.size}.",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = monoFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (regionToDelete != null) {
        val target = regionToDelete!!
        MineServeDialog(
            onDismissRequest = { regionToDelete = null },
            title = "Delete Region ${target.fileName}?",
            confirmLabel = "Delete",
            onConfirm = {
                scope.launch {
                    val ok = WorldManager.deleteRegionFile(serverDir, target.dimension, target.fileName, levelName)
                    regionToDelete = null
                    if (ok) {
                        Toast.makeText(context, "Deleted ${target.fileName}", Toast.LENGTH_SHORT).show()
                        loadRegions()
                        onWorldMutated()
                    } else {
                        Toast.makeText(context, "Failed to delete region file.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            icon = Icons.Rounded.Delete,
            isDestructive = true
        ) {
            Text(
                text = "This permanently deletes ${target.fileName} (${target.chunkCount} chunks, ${formatWorldFileSize(target.sizeBytes)}) from ${target.dimension.displayName}. The server will regenerate this region when visited.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
