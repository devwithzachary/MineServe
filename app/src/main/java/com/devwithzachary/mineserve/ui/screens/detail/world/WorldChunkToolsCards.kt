package com.devwithzachary.mineserve.ui.screens.detail.world

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.engine.PregenState
import com.devwithzachary.mineserve.engine.RegionFileInfo
import com.devwithzachary.mineserve.engine.WorldManager
import com.devwithzachary.mineserve.engine.WorldPregenManager
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate900
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
    val serverDir = remember(server.id) {
        val pRoot = PRootEngine(context)
        ServerRepository(context, pRoot).getServerDirectory(server.id)
    }
    val allStates by WorldPregenManager.states.collectAsState()
    val pregenState = allStates[server.id] ?: PregenState()

    var selectedDim by remember { mutableStateOf(DimensionType.OVERWORLD) }
    var selectedRadius by remember { mutableIntStateOf(500) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Chunk Pre-Generator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "Generate chunks around spawn while the server is empty to eliminate player exploration lag. Uses Chunky if installed or native /forceload stepping.",
                color = Slate400,
                fontSize = 12.sp
            )

            Text("Dimension", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DimensionType.values().forEach { dim ->
                    val selected = selectedDim == dim
                    Surface(
                        onClick = { if (!pregenState.isRunning) selectedDim = dim },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) EmeraldDark.copy(alpha = 0.35f) else Slate900,
                        border = BorderStroke(1.dp, if (selected) EmeraldPrimary else Slate700)
                    ) {
                        Text(
                            text = dim.displayName,
                            color = if (selected) EmeraldLight else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Text("Radius (blocks around 0, 0)", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(250, 500, 1000, 2000).forEach { r ->
                    val selected = selectedRadius == r
                    Surface(
                        onClick = { if (!pregenState.isRunning) selectedRadius = r },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) EmeraldDark.copy(alpha = 0.35f) else Slate900,
                        border = BorderStroke(1.dp, if (selected) EmeraldPrimary else Slate700)
                    ) {
                        Text(
                            text = "${r}m",
                            color = if (selected) EmeraldLight else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (pregenState.isRunning || pregenState.statusMessage != "Idle") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = pregenState.statusMessage,
                        color = if (pregenState.isRunning) EmeraldLight else Slate400,
                        fontSize = 12.sp
                    )
                    if (pregenState.isRunning) {
                        LinearProgressIndicator(
                            progress = { (pregenState.percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = EmeraldPrimary,
                            trackColor = Slate900
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "Start Pre-Generation" else "Start Server to Pre-Generate",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Button(
                        onClick = { WorldPregenManager.stopPregen(server.id, onSendCommand) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Stop Pre-Generation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
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
    val scope = rememberCoroutineScope()
    val serverDir = remember(server.id) {
        val pRoot = PRootEngine(context)
        ServerRepository(context, pRoot).getServerDirectory(server.id)
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

    Card(
        shape = RoundedCornerShape(14.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = DiamondCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Region File Inspector (.mca)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = { loadRegions() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh regions", tint = Slate400, modifier = Modifier.size(18.dp))
                }
            }

            Text(
                text = "Inspect individual Anvil (.mca) region files sorted by size to spot bloated or corrupted regions.",
                color = Slate400,
                fontSize = 12.sp
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options: List<Pair<String, DimensionType?>> = listOf(
                    "All (${regions.size})" to null
                ) + DimensionType.values().map { dim ->
                    val count = regions.count { it.dimension == dim }
                    "${dim.displayName} ($count)" to dim
                }
                options.forEach { (label, dimVal) ->
                    val selected = filterDim == dimVal
                    Surface(
                        onClick = { filterDim = dimVal },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) DiamondCyan.copy(alpha = 0.22f) else Slate900,
                        border = BorderStroke(1.dp, if (selected) DiamondCyan else Slate700)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) DiamondCyan else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DiamondCyan)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning .mca region headers...", color = Slate400, fontSize = 12.sp)
                }
            } else if (filtered.isEmpty()) {
                Text("No .mca region files found.", color = Slate400, fontSize = 12.sp)
            } else {
                val topRegions = filtered.take(20)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topRegions.forEach { r ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate900,
                            border = BorderStroke(1.dp, ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = r.fileName,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${r.dimension.displayName} · ${r.chunkCount}/1024 chunks · ${formatWorldFileSize(r.sizeBytes)}",
                                            color = Slate400,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (!isRunning) {
                                        IconButton(
                                            onClick = { regionToDelete = r },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete region",
                                                tint = RedstoneLight,
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
                                    color = DiamondCyan,
                                    trackColor = ObsidianCard
                                )
                            }
                        }
                    }
                    if (filtered.size > 20) {
                        Text(
                            text = "Showing top 20 largest regions of ${filtered.size}.",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    if (regionToDelete != null) {
        val target = regionToDelete!!
        AlertDialog(
            onDismissRequest = { regionToDelete = null },
            containerColor = ObsidianCard,
            title = { Text("Delete Region ${target.fileName}?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes ${target.fileName} (${target.chunkCount} chunks, ${formatWorldFileSize(target.sizeBytes)}) from ${target.dimension.displayName}. The server will regenerate this region when visited.",
                    color = Slate400,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { regionToDelete = null }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

