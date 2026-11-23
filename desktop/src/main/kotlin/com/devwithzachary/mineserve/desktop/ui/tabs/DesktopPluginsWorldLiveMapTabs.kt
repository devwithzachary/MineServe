package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.ui.DesktopColors
import com.devwithzachary.mineserve.desktop.ui.DesktopPanelCard
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private fun pickDesktopFile(title: String, extensionSuffix: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    val picked = File(dir, file)
    return if (picked.exists() && picked.name.endsWith(extensionSuffix, ignoreCase = true)) picked else null
}

@Composable
fun DesktopPluginsTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val installed by viewModel.installedPlugins.collectAsState()
    val results by viewModel.modrinthResults.collectAsState()
    val modrinthBusy by viewModel.modrinthBusy.collectAsState()
    val opBusy by viewModel.operationBusy.collectAsState()
    val opProgress by viewModel.operationProgress.collectAsState()
    val opStatus by viewModel.operationStatus.collectAsState()

    var query by remember { mutableStateOf("") }
    val isModServer = server.type.supportsMods && !server.type.supportsPlugins
    val label = if (isModServer) "Mods" else "Plugins"

    if (!server.type.supportsPlugins && !server.type.supportsMods) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text("${server.type.displayName} does not use external plugins or mods.", color = DesktopColors.Slate400)
            }
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left: Installed Plugins / Mods + .jar & .mrpack Import
        DesktopPanelCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Installed $label (${installed.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val jar = pickDesktopFile("Select $label .jar", ".jar")
                            if (jar != null) {
                                val folder = if (isModServer) "mods" else "plugins"
                                val destDir = File(viewModel.serverRepository.getServerDirectory(server.id), folder).apply { mkdirs() }
                                jar.copyTo(File(destDir, jar.name), overwrite = true)
                                viewModel.refreshInstalledPlugins(server)
                                viewModel.showToast("Imported ${jar.name}")
                            }
                        },
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("+ Import .jar", fontSize = 11.sp, color = DesktopColors.Slate300)
                    }
                    if (isModServer) {
                        Button(
                            onClick = {
                                val mrpack = pickDesktopFile("Select Modrinth .mrpack Modpack", ".mrpack")
                                if (mrpack != null) {
                                    viewModel.importMrpackFile(mrpack)
                                }
                            },
                            enabled = !opBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black),
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("+ Import .mrpack", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (opBusy) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(opStatus, fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (opProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = DesktopColors.EmeraldPrimary,
                    trackColor = DesktopColors.Slate800
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (installed.isEmpty()) {
                Text("No $label installed yet.", color = DesktopColors.Slate400, fontSize = 12.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(installed, key = { it.name }) { file ->
                        val enabled = !file.name.endsWith(".disabled")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DesktopColors.Slate900,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name.removeSuffix(".disabled"),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (enabled) Color.White else DesktopColors.Slate400,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${FileStorageUtils.formatBytes(file.length())} - ${if (enabled) "Enabled" else "Disabled"}",
                                        fontSize = 10.sp,
                                        color = DesktopColors.Slate400
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { viewModel.togglePlugin(file) }
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.deletePlugin(file) },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Delete", fontSize = 11.sp, color = DesktopColors.RedstoneLight)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right: Modrinth Marketplace Browser
        DesktopPanelCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text("Search Modrinth Marketplace (${server.type.displayName} ${server.version})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search Modrinth (e.g. Geyser, LuckPerms, Lithium, WorldEdit)...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.searchModrinth(query) },
                    enabled = !modrinthBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                ) {
                    Text(if (modrinthBusy) "..." else "Search", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DesktopColors.Slate900,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text(
                                    text = item.description,
                                    fontSize = 11.sp,
                                    color = DesktopColors.Slate400,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { viewModel.installModrinthProject(item.id) },
                                enabled = !modrinthBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                            ) {
                                Text("Install", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopWorldTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val dims by viewModel.worldDimensions.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            dims.forEach { d ->
                DesktopPanelCard(modifier = Modifier.weight(1f)) {
                    Text(d.dimension.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FileStorageUtils.formatBytes(d.sizeBytes),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesktopColors.EmeraldLight
                    )
                    Text(
                        text = "${d.regionCount} region files - ~${d.estimatedChunks} chunks",
                        fontSize = 11.sp,
                        color = DesktopColors.Slate400
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetDimension(d.dimension) },
                        enabled = d.exists,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset ${d.dimension.displayName}", fontSize = 12.sp, color = DesktopColors.RedstoneLight)
                    }
                }
            }
        }

        DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
            Text("World Chunk Pruner & Optimizer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Automatically creates a world backup and removes Anvil chunks that players only passed through briefly (based on InhabitedTime). Stop the server before pruning.",
                fontSize = 12.sp,
                color = DesktopColors.Slate400
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InhabitedTimeThreshold.entries.forEach { threshold ->
                    Button(
                        onClick = { viewModel.pruneChunks(threshold) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DesktopColors.Slate800,
                            contentColor = DesktopColors.EmeraldLight
                        )
                    ) {
                        Text("Prune: ${threshold.label}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private data class SquaremapPlayerMarker(
    val name: String,
    val world: String,
    val x: Int,
    val z: Int
)

private data class RegionCellInfo(
    val rx: Int,
    val rz: Int,
    val sizeBytes: Long,
    val estimatedChunks: Int
)

@Composable
fun DesktopLiveMapTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val installed by viewModel.squaremapInstalled.collectAsState()
    val port by viewModel.squaremapPort.collectAsState()
    var portInput by remember(port) { mutableStateOf(port.toString()) }
    val localIp = LinuxHostTelemetry.getLocalIpAddress() ?: "127.0.0.1"
    val mapUrl = "http://$localIp:$port"
    val localLoopbackUrl = "http://127.0.0.1:$port"

    // In-App Live Map State
    var availableWorlds by remember(server.id, port) {
        mutableStateOf(listOf("minecraft_overworld", "minecraft_the_nether", "minecraft_the_end"))
    }
    var selectedWorld by remember(server.id, port) { mutableStateOf("minecraft_overworld") }
    var squaremapZoom by remember(server.id) { mutableIntStateOf(3) }
    var panOffset by remember(server.id) { mutableStateOf(Offset.Zero) }
    var reloadGen by remember { mutableIntStateOf(0) }
    var squaremapReachable by remember(server.id, port) { mutableStateOf(false) }
    var livePlayers by remember(server.id, port) { mutableStateOf<List<SquaremapPlayerMarker>>(emptyList()) }
    val tileBitmaps = remember(server.id, port, selectedWorld, squaremapZoom, reloadGen) {
        mutableStateMapOf<Pair<Int, Int>, ImageBitmap>()
    }
    val attemptedTiles = remember(server.id, port, selectedWorld, squaremapZoom, reloadGen) {
        mutableStateMapOf<Pair<Int, Int>, Boolean>()
    }

    // Fallback Anvil Region (.mca) Grid Map data from disk
    val regionCells = remember(server.id, selectedWorld, reloadGen) {
        val serverDir = viewModel.serverRepository.getServerDirectory(server.id)
        val levelName = viewModel.serverRepository.loadServerProperties(server.id).levelName.ifBlank { "world" }
        val dim = when {
            selectedWorld.contains("nether", ignoreCase = true) -> DimensionType.NETHER
            selectedWorld.contains("end", ignoreCase = true) -> DimensionType.THE_END
            else -> DimensionType.OVERWORLD
        }
        val dDir = dim.getDimensionDir(serverDir, levelName)
        val rDir = when (dim) {
            DimensionType.OVERWORLD -> File(dDir, "region")
            DimensionType.NETHER -> File(dDir, "DIM-1/region").takeIf { it.exists() } ?: File(dDir, "region")
            DimensionType.THE_END -> File(dDir, "DIM1/region").takeIf { it.exists() } ?: File(dDir, "region")
        }
        if (!rDir.exists()) emptyList()
        else {
            (rDir.listFiles() ?: emptyArray())
                .filter { it.isFile && it.name.endsWith(".mca") }
                .mapNotNull { f ->
                    val parts = f.name.split(".")
                    val rx = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                    val rz = parts.getOrNull(2)?.toIntOrNull() ?: return@mapNotNull null
                    RegionCellInfo(
                        rx = rx,
                        rz = rz,
                        sizeBytes = f.length(),
                        estimatedChunks = ((f.length() / 4096L).toInt()).coerceIn(1, 1024)
                    )
                }
        }
    }

    // Poll Squaremap settings.json & players.json when installed
    LaunchedEffect(server.id, installed, port, selectedWorld, reloadGen) {
        if (!installed) return@LaunchedEffect
        while (isActive) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val req = MineServeHttpClient.newRequestBuilder("$localLoopbackUrl/tiles/settings.json").get().build()
                    MineServeHttpClient.client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val json = JSONObject(resp.body?.string().orEmpty())
                            val worldsArr = json.optJSONArray("worlds")
                            if (worldsArr != null && worldsArr.length() > 0) {
                                val parsed = mutableListOf<String>()
                                for (i in 0 until worldsArr.length()) {
                                    val wName = worldsArr.optJSONObject(i)?.optString("name", "").orEmpty()
                                    if (wName.isNotBlank()) parsed.add(wName)
                                }
                                if (parsed.isNotEmpty()) {
                                    availableWorlds = parsed
                                    if (selectedWorld !in parsed) selectedWorld = parsed.first()
                                }
                            }
                            squaremapReachable = true
                        } else {
                            squaremapReachable = false
                        }
                    }
                }.onFailure {
                    squaremapReachable = false
                }

                if (squaremapReachable) {
                    runCatching {
                        val pReq = MineServeHttpClient.newRequestBuilder("$localLoopbackUrl/tiles/$selectedWorld/players.json").get().build()
                        MineServeHttpClient.client.newCall(pReq).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val json = JSONObject(resp.body?.string().orEmpty())
                                val pArr = json.optJSONArray("players")
                                val markers = mutableListOf<SquaremapPlayerMarker>()
                                if (pArr != null) {
                                    for (i in 0 until pArr.length()) {
                                        val p = pArr.optJSONObject(i) ?: continue
                                        markers.add(
                                            SquaremapPlayerMarker(
                                                name = p.optString("name", "Player"),
                                                world = p.optString("world", selectedWorld),
                                                x = p.optInt("x", 0),
                                                z = p.optInt("z", 0)
                                            )
                                        )
                                    }
                                }
                                livePlayers = markers
                            }
                        }
                    }
                }
            }
            delay(4000)
        }
    }

    // Fetch visible Squaremap PNG tiles around current pan offset
    LaunchedEffect(server.id, installed, port, squaremapReachable, selectedWorld, squaremapZoom, panOffset, reloadGen) {
        if (!installed || !squaremapReachable) return@LaunchedEffect
        val centerTileX = (-panOffset.x / 256f).roundToInt()
        val centerTileZ = (-panOffset.y / 256f).roundToInt()
        for (dz in -2..2) {
            for (dx in -3..3) {
                val tx = centerTileX + dx
                val tz = centerTileZ + dz
                val key = tx to tz
                if (attemptedTiles.containsKey(key)) continue
                attemptedTiles[key] = true
                launch(Dispatchers.IO) {
                    runCatching {
                        val url = "$localLoopbackUrl/tiles/$selectedWorld/$squaremapZoom/${tx}_${tz}.png"
                        val req = MineServeHttpClient.newRequestBuilder(url).get().build()
                        MineServeHttpClient.client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val bytes = resp.body?.bytes()
                                if (bytes != null && bytes.isNotEmpty()) {
                                    val img = ImageIO.read(ByteArrayInputStream(bytes))
                                    if (img != null) {
                                        tileBitmaps[key] = img.toComposeImageBitmap()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Control Bar Card
        DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Squaremap Live Web Map", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            installed && squaremapReachable -> "Connected to Squaremap on port $port ($mapUrl) - Drag canvas below to pan"
                            installed -> "Squaremap installed on port $port (start server or run '/squaremap fullrender' to generate live tiles; showing Anvil region overview)"
                            else -> "Install Squaremap for a live 2D world map viewable right here in the app or in your browser."
                        },
                        fontSize = 12.sp,
                        color = DesktopColors.Slate400
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (installed) {
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                val p = portInput.toIntOrNull()
                                if (p != null) viewModel.updateSquaremapPort(p)
                            }
                        ) {
                            Text("Save Port", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { viewModel.openUrlInBrowser(mapUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                        ) {
                            Text("Open in Browser", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { viewModel.uninstallSquaremap() }) {
                            Text("Uninstall", fontSize = 12.sp, color = DesktopColors.RedstoneLight)
                        }
                    } else if (server.type.supportsPlugins || server.type.supportsMods) {
                        Button(
                            onClick = { viewModel.installSquaremap() },
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                        ) {
                            Text("1-Click Install Squaremap", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text("Requires Paper, Purpur, Spigot, Fabric, or NeoForge", color = DesktopColors.AmberLight, fontSize = 12.sp)
                    }
                }
            }
        }

        // Interactive In-App Live Map Canvas Viewer
        DesktopPanelCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Dimension:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DesktopColors.Slate300)
                    availableWorlds.forEach { w ->
                        val active = w == selectedWorld
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                            border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.clickable {
                                selectedWorld = w
                                panOffset = Offset.Zero
                            }
                        ) {
                            Text(
                                text = w.removePrefix("minecraft_").replace('_', ' ').uppercase(),
                                color = if (active) Color.Black else DesktopColors.Slate300,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (installed && viewModel.processManager.isServerRunning(server.id)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.processManager.sendCommand(server.id, "squaremap fullrender")
                                viewModel.showToast("Triggered Squaremap fullrender")
                            },
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Render Tiles", fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                        }
                    }
                    OutlinedButton(
                        onClick = { squaremapZoom = (squaremapZoom - 1).coerceIn(0, 3) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Zoom -", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { squaremapZoom = (squaremapZoom + 1).coerceIn(0, 3) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Zoom +", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { panOffset = Offset.Zero },
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Center (0, 0)", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { reloadGen++ },
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Refresh", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070B12))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panOffset += dragAmount
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f + panOffset.x
                    val cy = size.height / 2f + panOffset.y

                    // Subtle coordinate grid lines
                    val gridStep = 128f
                    val startX = cx % gridStep
                    var gx = startX
                    while (gx < size.width) {
                        drawLine(
                            color = DesktopColors.Slate800.copy(alpha = 0.45f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, size.height),
                            strokeWidth = 1f
                        )
                        gx += gridStep
                    }
                    val startY = cy % gridStep
                    var gy = startY
                    while (gy < size.height) {
                        drawLine(
                            color = DesktopColors.Slate800.copy(alpha = 0.45f),
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1f
                        )
                        gy += gridStep
                    }

                    if (tileBitmaps.isNotEmpty()) {
                        // Draw rendered Squaremap 2D PNG tiles
                        for ((coords, bitmap) in tileBitmaps) {
                            val drawX = (cx + coords.first * 256f).roundToInt()
                            val drawY = (cy + coords.second * 256f).roundToInt()
                            drawImage(
                                image = bitmap,
                                dstOffset = IntOffset(drawX, drawY),
                                dstSize = IntSize(256, 256)
                            )
                        }
                    } else if (regionCells.isNotEmpty()) {
                        // Draw Anvil Region (.mca) overview map when Squaremap PNG tiles aren't rendered yet
                        val cellPx = when (squaremapZoom) {
                            0 -> 28f
                            1 -> 44f
                            2 -> 64f
                            else -> 88f
                        }
                        for (cell in regionCells) {
                            val rxPx = cx + cell.rx * cellPx
                            val rzPx = cy + cell.rz * cellPx
                            val fillAlpha = (0.20f + (cell.estimatedChunks / 1024f) * 0.65f).coerceIn(0.20f, 0.85f)
                            drawRect(
                                color = DesktopColors.EmeraldPrimary.copy(alpha = fillAlpha),
                                topLeft = Offset(rxPx, rzPx),
                                size = Size(cellPx - 2f, cellPx - 2f)
                            )
                        }
                    }

                    // Origin (0, 0) Spawn Crosshair
                    drawLine(
                        color = DesktopColors.EmeraldLight.copy(alpha = 0.7f),
                        start = Offset(cx - 10f, cy),
                        end = Offset(cx + 10f, cy),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = DesktopColors.EmeraldLight.copy(alpha = 0.7f),
                        start = Offset(cx, cy - 10f),
                        end = Offset(cx, cy + 10f),
                        strokeWidth = 2f
                    )

                    // Live Player Dots
                    val blocksPerPixel = when (squaremapZoom) {
                        3 -> 1f
                        2 -> 2f
                        1 -> 4f
                        else -> 8f
                    }
                    for (p in livePlayers) {
                        val px = cx + (p.x / blocksPerPixel)
                        val pz = cy + (p.z / blocksPerPixel)
                        drawCircle(color = DesktopColors.AmberWarning, radius = 7f, center = Offset(px, pz))
                        drawCircle(color = Color.White, radius = 3.5f, center = Offset(px, pz))
                    }
                }

                // Bottom-Left Legend Overlay
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DesktopColors.ObsidianCard.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = when {
                                tileBitmaps.isNotEmpty() -> "Squaremap Live Tiles (${tileBitmaps.size} tiles loaded)"
                                regionCells.isNotEmpty() -> "Anvil Region Map (${regionCells.size} .mca regions on disk)"
                                else -> "No world regions generated yet"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DesktopColors.EmeraldLight
                        )
                        val blocksPerPixel = when (squaremapZoom) {
                            3 -> 1f
                            2 -> 2f
                            1 -> 4f
                            else -> 8f
                        }
                        val centerBlockX = (-panOffset.x * blocksPerPixel).roundToInt()
                        val centerBlockZ = (-panOffset.y * blocksPerPixel).roundToInt()
                        Text(
                            text = "Center: X=$centerBlockX, Z=$centerBlockZ | Players: ${livePlayers.size}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = DesktopColors.Slate300
                        )
                    }
                }
            }
        }
    }
}
