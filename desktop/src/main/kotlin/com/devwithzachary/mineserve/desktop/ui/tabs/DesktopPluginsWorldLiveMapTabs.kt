package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.designsystem.components.MineServePreferenceRow
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.ui.DesktopRegionFileEntry
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.util.FileStorageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.roundToInt

private fun pickDesktopFile(title: String, extensionSuffix: String): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    val picked = File(dir, file)
    return if (picked.exists() && picked.name.endsWith(extensionSuffix, ignoreCase = true)) picked else null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopPluginsTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current

    val installed by viewModel.installedPlugins.collectAsState()
    val results by viewModel.modrinthResults.collectAsState()
    val modrinthBusy by viewModel.modrinthBusy.collectAsState()
    val opBusy by viewModel.operationBusy.collectAsState()
    val opProgress by viewModel.operationProgress.collectAsState()
    val opStatus by viewModel.operationStatus.collectAsState()

    var installedFilter by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    val isModServer = server.type.supportsMods && !server.type.supportsPlugins
    val label = if (isModServer) "Mods" else "Plugins"

    if (!server.type.supportsPlugins && !server.type.supportsMods) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            MineServeEmptyState(
                icon = Icons.Rounded.Extension,
                title = "$label Not Supported",
                supportingText = "${server.type.displayName} does not load external plugins or mods."
            )
        }
        return
    }

    pendingDelete?.let { file ->
        MineServeDialog(
            onDismissRequest = { pendingDelete = null },
            title = "Remove ${file.name.removeSuffix(".disabled")}?",
            confirmLabel = "Delete",
            isDestructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = {
                viewModel.deletePlugin(file)
                pendingDelete = null
            }
        ) {
            Text(
                text = "This permanently deletes ${file.name} from the server.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    val filteredInstalled = remember(installed, installedFilter) {
        val q = installedFilter.trim()
        if (q.isEmpty()) installed else installed.filter { it.name.contains(q, ignoreCase = true) }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left: Installed Plugins / Mods
        MineServeCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Installed $label (${installed.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
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
                        shape = ShapeDefaults.Small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import .jar", style = MaterialTheme.typography.labelMedium)
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
                            shape = ShapeDefaults.Small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import .mrpack", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = installedFilter,
                onValueChange = { installedFilter = it },
                placeholder = { Text("Filter installed $label...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (opBusy) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(opStatus, style = MaterialTheme.typography.labelMedium, color = colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (opProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredInstalled.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.Extension,
                        title = if (installed.isEmpty()) "No $label Installed" else "No Matches",
                        supportingText = if (installed.isEmpty()) {
                            "Import a local .jar or install from Modrinth on the right."
                        } else {
                            "Try clearing the filter text."
                        }
                    )
                }
            } else {
                MineServeTableContainer(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredInstalled, key = { it.name }) { file ->
                            val enabled = !file.name.endsWith(".disabled")
                            MineServeTableRow(
                                showDivider = file != filteredInstalled.last(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name.removeSuffix(".disabled"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (enabled) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    MineServeTableMonoText(
                                        text = "${FileStorageUtils.formatBytes(file.length())} - ${if (enabled) "Enabled" else "Disabled"}",
                                        color = colorScheme.onSurfaceVariant
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
                                    IconButton(
                                        onClick = { pendingDelete = file },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete $label",
                                            tint = colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right: Modrinth Marketplace Browser
        MineServeCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Text(
                text = "Modrinth Marketplace (${server.type.displayName} ${server.version})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search Modrinth plugins & mods...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            if ((event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                                event.type == KeyEventType.KeyDown &&
                                !modrinthBusy
                            ) {
                                viewModel.searchModrinth(query)
                                true
                            } else {
                                false
                            }
                        }
                )
                Button(
                    onClick = { viewModel.searchModrinth(query) },
                    enabled = !modrinthBusy,
                    shape = ShapeDefaults.Medium,
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(if (modrinthBusy) "Searching..." else "Search")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.Search,
                        title = "Search Modrinth",
                        supportingText = "Enter a plugin or mod name above to browse compatible versions for ${server.type.displayName} ${server.version}."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results, key = { it.id }) { item ->
                        MineServeCard(
                            containerColor = colorScheme.surfaceContainerHigh,
                            shape = ShapeDefaults.Medium,
                            contentPadding = PaddingValues(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AssistChip(
                                            onClick = {},
                                            label = {
                                                Text(
                                                    text = server.type.displayName,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = colorScheme.surfaceContainer,
                                                labelColor = colorScheme.onSurfaceVariant
                                            ),
                                            border = null,
                                            modifier = Modifier.height(26.dp)
                                        )
                                        item.categories.take(2).forEach { cat ->
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        text = cat,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = colorScheme.surfaceContainer,
                                                    labelColor = colorScheme.onSurfaceVariant
                                                ),
                                                border = null,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                        if (item.downloads > 0) {
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        text = "${item.downloads} dl",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont)
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = colorScheme.surfaceContainer,
                                                    labelColor = colorScheme.tertiary
                                                ),
                                                border = null,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.installModrinthProject(item.id) },
                                    enabled = !modrinthBusy,
                                    shape = ShapeDefaults.Small,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colorScheme.primaryContainer,
                                        contentColor = colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Install", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopWorldTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current

    val dims by viewModel.worldDimensions.collectAsState()
    val regions by viewModel.regionFiles.collectAsState()
    val initialProps = remember(server.id) { viewModel.serverRepository.loadServerProperties(server.id) }
    var difficulty by remember(server.id) { mutableStateOf(initialProps.difficulty) }
    var gamemode by remember(server.id) { mutableStateOf(initialProps.gamemode) }
    var allowNether by remember(server.id) { mutableStateOf(initialProps.allowNether) }
    var spawnMonsters by remember(server.id) { mutableStateOf(initialProps.spawnMonsters) }

    var pendingResetDim by remember { mutableStateOf<DimensionType?>(null) }
    var pendingPruneThreshold by remember { mutableStateOf<InhabitedTimeThreshold?>(null) }
    var pendingDeleteRegion by remember { mutableStateOf<DesktopRegionFileEntry?>(null) }

    pendingResetDim?.let { dim ->
        MineServeDialog(
            onDismissRequest = { pendingResetDim = null },
            title = "Reset ${dim.displayName}?",
            confirmLabel = "Reset Dimension",
            isDestructive = true,
            icon = Icons.Rounded.RestartAlt,
            onConfirm = {
                viewModel.resetDimension(dim)
                pendingResetDim = null
            }
        ) {
            Text(
                text = "This permanently deletes all ${dim.displayName} region data so it regenerates fresh on next start. Stop the server first.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    pendingPruneThreshold?.let { threshold ->
        MineServeDialog(
            onDismissRequest = { pendingPruneThreshold = null },
            title = "Prune Chunks (${threshold.label})?",
            confirmLabel = "Prune Chunks",
            isDestructive = true,
            icon = Icons.Rounded.CleaningServices,
            onConfirm = {
                viewModel.pruneChunks(threshold)
                pendingPruneThreshold = null
            }
        ) {
            Text(
                text = "Creates an automatic world backup, then removes Anvil chunks matching '${threshold.label}'. Stop the server before pruning.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    pendingDeleteRegion?.let { reg ->
        MineServeDialog(
            onDismissRequest = { pendingDeleteRegion = null },
            title = "Delete ${reg.fileName}?",
            confirmLabel = "Delete Region",
            isDestructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = {
                viewModel.deleteRegionFile(reg)
                pendingDeleteRegion = null
            }
        ) {
            Text(
                text = "Deletes ${reg.fileName} (${reg.dimension.displayName}, ~${reg.estimatedChunks} chunks). Those chunks will regenerate from the world seed.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row 1: Dimension Footprint Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            dims.forEach { d ->
                MineServeCard(modifier = Modifier.weight(1f)) {
                    Text(
                        text = d.dimension.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FileStorageUtils.formatBytes(d.sizeBytes),
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = monoFont),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                    MineServeTableMonoText(
                        text = "${d.regionCount} region files - ~${d.estimatedChunks} chunks",
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { pendingResetDim = d.dimension },
                        enabled = d.exists,
                        shape = ShapeDefaults.Medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.errorContainer,
                            contentColor = colorScheme.onErrorContainer,
                            disabledContainerColor = colorScheme.surfaceContainerHighest,
                            disabledContentColor = colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset ${d.dimension.displayName}")
                    }
                }
            }
        }

        // Row 2: Seed & Gamerules + Import/Export + Chunk Pruner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MineServeCard(
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "World Seed & Gamerules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Level Seed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = initialProps.levelSeed.ifBlank { "(Random seed on first generation)" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            color = colorScheme.onSurface
                        )
                    }
                    if (initialProps.levelSeed.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.copyToClipboard(initialProps.levelSeed, "Copied seed: ${initialProps.levelSeed}")
                            }
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Seed", tint = colorScheme.primary)
                        }
                    }
                }
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Difficulty", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("peaceful", "easy", "normal", "hard").forEach { d ->
                            FilterChip(
                                selected = difficulty == d,
                                onClick = { difficulty = d },
                                label = { Text(d.replaceFirstChar { it.uppercase(Locale.US) }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.secondaryContainer,
                                    selectedLabelColor = colorScheme.onSecondaryContainer,
                                    containerColor = colorScheme.surfaceContainerHigh,
                                    labelColor = colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }
                    Text("Default Gamemode", style = MaterialTheme.typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("survival", "creative", "adventure").forEach { gm ->
                            FilterChip(
                                selected = gamemode == gm,
                                onClick = { gamemode = gm },
                                label = { Text(gm.replaceFirstChar { it.uppercase(Locale.US) }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.secondaryContainer,
                                    selectedLabelColor = colorScheme.onSecondaryContainer,
                                    containerColor = colorScheme.surfaceContainerHigh,
                                    labelColor = colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }
                }
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                MineServePreferenceRow(
                    title = "Allow Nether",
                    checked = allowNether,
                    onCheckedChange = { allowNether = it }
                )
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                MineServePreferenceRow(
                    title = "Spawn Monsters",
                    checked = spawnMonsters,
                    onCheckedChange = { spawnMonsters = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val fresh = viewModel.serverRepository.loadServerProperties(server.id)
                            viewModel.serverRepository.saveServerProperties(
                                server.id,
                                fresh.copy(
                                    difficulty = difficulty,
                                    gamemode = gamemode,
                                    allowNether = allowNether,
                                    spawnMonsters = spawnMonsters
                                )
                            )
                            viewModel.showToast("Saved world gamerules to server.properties")
                        },
                        shape = ShapeDefaults.Medium
                    ) {
                        Text("Save World Rules")
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MineServeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "World Import & Export (.zip / .mcworld)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import an existing Minecraft world archive (automatically backs up current world first) or export your world to a .zip file. Stop the server first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val dialog = FileDialog(null as Frame?, "Select World .zip or .mcworld Archive", FileDialog.LOAD)
                                dialog.isVisible = true
                                if (dialog.directory != null && dialog.file != null) {
                                    val f = File(dialog.directory, dialog.file)
                                    if (f.exists()) viewModel.importWorldArchive(f)
                                }
                            },
                            shape = ShapeDefaults.Medium
                        ) {
                            Text("Import World (.zip / .mcworld)")
                        }
                        FilledTonalButton(
                            onClick = {
                                val dialog = FileDialog(null as Frame?, "Save World Export .zip", FileDialog.SAVE)
                                dialog.file = "${server.name.replace(' ', '_')}_world.zip"
                                dialog.isVisible = true
                                if (dialog.directory != null && dialog.file != null) {
                                    viewModel.exportWorldZip(File(dialog.directory, dialog.file))
                                }
                            },
                            shape = ShapeDefaults.Medium
                        ) {
                            Text("Export World (.zip)")
                        }
                    }
                }

                MineServeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "World Chunk Pruner & Optimizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Creates a world backup and removes Anvil chunks players only passed through briefly (based on InhabitedTime). Stop the server before pruning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InhabitedTimeThreshold.entries.forEach { threshold ->
                            FilledTonalButton(
                                onClick = { pendingPruneThreshold = threshold },
                                shape = ShapeDefaults.Small,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colorScheme.secondaryContainer,
                                    contentColor = colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("Prune: ${threshold.label}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Row 3: Anvil Region File Inspector (.mca)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anvil Region File Inspector (${regions.size} .mca files)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                FilledTonalButton(
                    onClick = { viewModel.refreshWorldSummary(server) },
                    shape = ShapeDefaults.Small,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh Regions", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (regions.isEmpty()) {
                MineServeEmptyState(
                    icon = Icons.Rounded.Public,
                    title = "No Region Files Yet",
                    supportingText = "Start the server to generate Anvil .mca world regions."
                )
            } else {
                MineServeTableContainer(modifier = Modifier.fillMaxWidth()) {
                    regions.take(100).forEachIndexed { idx, reg ->
                        MineServeTableRow(
                            showDivider = idx < regions.take(100).lastIndex,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reg.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                MineServeTableMonoText(
                                    text = "${reg.dimension.displayName} - ~${reg.estimatedChunks}/1024 chunks - ${FileStorageUtils.formatBytes(reg.sizeBytes)}",
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { pendingDeleteRegion = reg },
                                shape = ShapeDefaults.Small,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colorScheme.errorContainer,
                                    contentColor = colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Delete Region", style = MaterialTheme.typography.labelSmall)
                            }
                        }
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
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val installed by viewModel.squaremapInstalled.collectAsState()
    val port by viewModel.squaremapPort.collectAsState()
    var portInput by remember(port) { mutableStateOf(port.toString()) }
    val localIp = LinuxHostTelemetry.getLocalIpAddress() ?: "127.0.0.1"
    val mapUrl = "http://$localIp:$port"
    val localLoopbackUrl = "http://127.0.0.1:$port"

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

    LaunchedEffect(server.id, installed, port, selectedWorld, reloadGen) {
        if (!installed) return@LaunchedEffect
        while (isActive) {
            val currentWorld = selectedWorld
            val pollResult = withContext(Dispatchers.IO) {
                var parsedWorlds: List<String>? = null
                var reachable = false
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
                                if (parsed.isNotEmpty()) parsedWorlds = parsed
                            }
                            reachable = true
                        }
                    }
                }

                val targetWorld = parsedWorlds?.let { if (currentWorld in it) currentWorld else it.first() } ?: currentWorld
                var parsedPlayers: List<SquaremapPlayerMarker>? = null
                if (reachable) {
                    runCatching {
                        val pReq = MineServeHttpClient.newRequestBuilder("$localLoopbackUrl/tiles/$targetWorld/players.json").get().build()
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
                                                world = p.optString("world", targetWorld),
                                                x = p.optInt("x", 0),
                                                z = p.optInt("z", 0)
                                            )
                                        )
                                    }
                                }
                                parsedPlayers = markers
                            }
                        }
                    }
                }
                Triple(parsedWorlds, reachable, parsedPlayers)
            }

            pollResult.first?.let { parsed ->
                availableWorlds = parsed
                if (selectedWorld !in parsed) selectedWorld = parsed.first()
            }
            squaremapReachable = pollResult.second
            pollResult.third?.let { livePlayers = it }

            delay(4000)
        }
    }

    val centerTileX = (-panOffset.x / 256f).roundToInt()
    val centerTileZ = (-panOffset.y / 256f).roundToInt()

    LaunchedEffect(server.id, installed, port, squaremapReachable, selectedWorld, squaremapZoom, centerTileX, centerTileZ, reloadGen) {
        if (!installed || !squaremapReachable) return@LaunchedEffect
        for (dz in -2..2) {
            for (dx in -3..3) {
                val tx = centerTileX + dx
                val tz = centerTileZ + dz
                val key = tx to tz
                if (attemptedTiles.containsKey(key)) continue
                attemptedTiles[key] = true
                launch {
                    var shouldRetry = false
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            val url = "$localLoopbackUrl/tiles/$selectedWorld/$squaremapZoom/${tx}_${tz}.png"
                            val req = MineServeHttpClient.newRequestBuilder(url).get().build()
                            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val bytes = resp.body?.bytes()
                                    if (bytes != null && bytes.isNotEmpty()) {
                                        ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
                                    } else {
                                        null
                                    }
                                } else {
                                    if (resp.code != 404) shouldRetry = true
                                    null
                                }
                            }
                        }
                        if (bitmap != null) {
                            tileBitmaps[key] = bitmap
                        }
                    } catch (ce: CancellationException) {
                        shouldRetry = true
                        throw ce
                    } catch (_: Exception) {
                        shouldRetry = true
                    } finally {
                        if (shouldRetry) {
                            withContext(NonCancellable) {
                                attemptedTiles.remove(key)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Control Bar Card
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Squaremap Live Web Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            installed && squaremapReachable -> "Connected to Squaremap on port $port ($mapUrl) - Drag canvas below to pan"
                            installed -> "Squaremap installed on port $port (start server or run '/squaremap fullrender' to generate live tiles)"
                            else -> "Install Squaremap for a live 2D world map viewable right here or in your browser."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (installed) {
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = colorScheme.surfaceContainerHighest,
                            contentColor = colorScheme.onSurface,
                            border = null,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "Port",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                BasicTextField(
                                    value = portInput,
                                    onValueChange = { portInput = it.filter { ch -> ch.isDigit() }.take(5) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = monoFont,
                                        color = colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(colorScheme.primary),
                                    modifier = Modifier.width(64.dp)
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                val p = portInput.toIntOrNull()
                                if (p != null) viewModel.updateSquaremapPort(p)
                            },
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Save Port")
                        }
                        Button(
                            onClick = { viewModel.openUrlInBrowser(mapUrl) },
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Browser")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.uninstallSquaremap() },
                            shape = ShapeDefaults.Small,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Uninstall")
                        }
                    } else if (server.type.supportsPlugins || server.type.supportsMods) {
                        Button(
                            onClick = { viewModel.installSquaremap() },
                            shape = ShapeDefaults.Medium
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install Squaremap")
                        }
                    } else {
                        Text(
                            text = "Requires Paper, Purpur, Spigot, Fabric, or NeoForge",
                            style = MaterialTheme.typography.labelMedium,
                            color = extended.warning
                        )
                    }
                }
            }
        }

        if (!installed && regionCells.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                MineServeEmptyState(
                    icon = Icons.Rounded.Map,
                    title = "Live Map Not Configured",
                    supportingText = if (server.type.supportsPlugins || server.type.supportsMods) {
                        "Install Squaremap above or start the server to generate world regions."
                    } else {
                        "Live map plugins are not supported for ${server.type.displayName}."
                    },
                    actionLabel = if (server.type.supportsPlugins || server.type.supportsMods) "Install Squaremap" else null,
                    onActionClick = if (server.type.supportsPlugins || server.type.supportsMods) {
                        { viewModel.installSquaremap() }
                    } else {
                        null
                    }
                )
            }
        } else {
            MineServeCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        availableWorlds.forEach { w ->
                            FilterChip(
                                selected = w == selectedWorld,
                                onClick = {
                                    selectedWorld = w
                                    panOffset = Offset.Zero
                                },
                                label = {
                                    Text(
                                        text = w.removePrefix("minecraft_").replace('_', ' ')
                                            .replaceFirstChar { it.uppercase(Locale.US) },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.secondaryContainer,
                                    selectedLabelColor = colorScheme.onSecondaryContainer,
                                    containerColor = colorScheme.surfaceContainerHigh,
                                    labelColor = colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (installed && viewModel.processManager.isServerRunning(server.id)) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.processManager.sendCommand(server.id, "squaremap fullrender")
                                    viewModel.showToast("Triggered Squaremap fullrender")
                                },
                                shape = ShapeDefaults.Small,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colorScheme.primaryContainer,
                                    contentColor = colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Render Tiles", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        FilledTonalButton(
                            onClick = { squaremapZoom = (squaremapZoom - 1).coerceIn(0, 3) },
                            shape = ShapeDefaults.Small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Zoom -", style = MaterialTheme.typography.labelSmall)
                        }
                        FilledTonalButton(
                            onClick = { squaremapZoom = (squaremapZoom + 1).coerceIn(0, 3) },
                            shape = ShapeDefaults.Small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Zoom +", style = MaterialTheme.typography.labelSmall)
                        }
                        FilledTonalButton(
                            onClick = { panOffset = Offset.Zero },
                            shape = ShapeDefaults.Small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Center (0, 0)", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(
                            onClick = { reloadGen++ },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh map",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val gridLineColor = colorScheme.outlineVariant.copy(alpha = 0.35f)
                val regionFillBase = colorScheme.primary
                val playerDotOuter = extended.warning
                val playerDotInner = colorScheme.onPrimary

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(ShapeDefaults.Medium)
                        .background(extended.terminalSurface)
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

                        val gridStep = 128f
                        var gx = cx % gridStep
                        while (gx < size.width) {
                            drawLine(
                                color = gridLineColor,
                                start = Offset(gx, 0f),
                                end = Offset(gx, size.height),
                                strokeWidth = 1f
                            )
                            gx += gridStep
                        }
                        var gy = cy % gridStep
                        while (gy < size.height) {
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, gy),
                                end = Offset(size.width, gy),
                                strokeWidth = 1f
                            )
                            gy += gridStep
                        }

                        if (tileBitmaps.isNotEmpty()) {
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
                                    color = regionFillBase.copy(alpha = fillAlpha),
                                    topLeft = Offset(rxPx, rzPx),
                                    size = Size(cellPx - 2f, cellPx - 2f)
                                )
                            }
                        }

                        drawLine(
                            color = regionFillBase.copy(alpha = 0.7f),
                            start = Offset(cx - 10f, cy),
                            end = Offset(cx + 10f, cy),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = regionFillBase.copy(alpha = 0.7f),
                            start = Offset(cx, cy - 10f),
                            end = Offset(cx, cy + 10f),
                            strokeWidth = 2f
                        )

                        val blocksPerPixel = when (squaremapZoom) {
                            3 -> 1f
                            2 -> 2f
                            1 -> 4f
                            else -> 8f
                        }
                        for (p in livePlayers) {
                            val px = cx + (p.x / blocksPerPixel)
                            val pz = cy + (p.z / blocksPerPixel)
                            drawCircle(color = playerDotOuter, radius = 7f, center = Offset(px, pz))
                            drawCircle(color = playerDotInner, radius = 3.5f, center = Offset(px, pz))
                        }
                    }

                    Surface(
                        shape = ShapeDefaults.Small,
                        color = colorScheme.surfaceContainer.copy(alpha = 0.9f),
                        contentColor = colorScheme.onSurface,
                        border = null,
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = when {
                                    tileBitmaps.isNotEmpty() -> "Squaremap Live Tiles (${tileBitmaps.size} tiles loaded)"
                                    regionCells.isNotEmpty() -> "Anvil Region Map (${regionCells.size} .mca regions on disk)"
                                    else -> "No world regions generated yet"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.primary
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
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
