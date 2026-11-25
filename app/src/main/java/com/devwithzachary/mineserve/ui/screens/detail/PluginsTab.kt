package com.devwithzachary.mineserve.ui.screens.detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.api.ModrinthApiClient
import com.devwithzachary.mineserve.api.ModrinthProjectDetails
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.modpack.ModpackIndex
import com.devwithzachary.mineserve.modpack.ModpackInstallProgress
import com.devwithzachary.mineserve.modpack.ModpackInstaller
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginsTab(
    server: MinecraftServer,
    plugins: List<PluginModEntry>,
    onTogglePlugin: (PluginModEntry) -> Unit,
    onDeletePlugin: (PluginModEntry) -> Unit,
    onInstallPluginOrMod: (fileName: String, downloadUrl: String, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit,
    onImportJar: (Uri, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit,
    onImportModpack: ((Uri, (ModpackInstallProgress) -> Unit, (Result<ModpackIndex>) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val isModServer = server.type.supportsMods && !server.type.supportsPlugins
    val isHybrid = server.type == ServerType.CUSTOM

    val sectionTitle = when {
        isModServer -> stringResource(R.string.mods_title)
        isHybrid -> stringResource(R.string.plugins_mods_title)
        else -> stringResource(R.string.plugins_title)
    }

    val searchPlaceholder = if (isModServer) {
        stringResource(R.string.mods_search_placeholder)
    } else {
        stringResource(R.string.plugins_search_placeholder)
    }

    val installedSectionTitle = if (isModServer) {
        stringResource(R.string.mods_installed_section)
    } else {
        stringResource(R.string.plugins_installed_section)
    }

    val emptyMessage = if (isModServer) {
        stringResource(R.string.mods_installed_empty)
    } else {
        stringResource(R.string.plugins_installed_empty)
    }

    val loaderFilter = when (server.type) {
        ServerType.FABRIC -> "fabric"
        ServerType.FORGE -> "forge"
        ServerType.NEOFORGE -> "neoforge"
        ServerType.VELOCITY -> "velocity"
        ServerType.BUNGEECORD -> "bungeecord"
        else -> null
    }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PluginModEntry>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val installingMap = remember { mutableStateMapOf<String, Boolean>() }

    var selectedProjectForDetails by remember { mutableStateOf<PluginModEntry?>(null) }
    var projectDetails by remember { mutableStateOf<ModrinthProjectDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var pendingDeletePlugin by remember { mutableStateOf<PluginModEntry?>(null) }

    val jarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                onImportJar(uri, isModServer) { success ->
                    if (success) {
                        Toast.makeText(context, "Imported JAR successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to import JAR", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var pendingModpackUri by remember { mutableStateOf<Uri?>(null) }
    var pendingModpackIndex by remember { mutableStateOf<ModpackIndex?>(null) }
    var isInstallingModpack by remember { mutableStateOf(false) }
    var modpackProgress by remember { mutableStateOf<ModpackInstallProgress?>(null) }

    val modpackPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingModpackUri = uri
            scope.launch {
                val res = ModpackInstaller().readMetadata(context, uri)
                res.fold(
                    onSuccess = { meta -> pendingModpackIndex = meta },
                    onFailure = { err ->
                        pendingModpackUri = null
                        Toast.makeText(context, "Failed reading .mrpack: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    if (pendingModpackIndex != null && pendingModpackUri != null && !isInstallingModpack) {
        val meta = pendingModpackIndex!!
        val uri = pendingModpackUri!!
        val serverModsCount = meta.files.count { it.isServerCompatible }

        MineServeDialog(
            onDismissRequest = {
                pendingModpackIndex = null
                pendingModpackUri = null
            },
            title = "Install Modpack?",
            confirmLabel = "Install Modpack",
            onConfirm = {
                if (onImportModpack != null) {
                    isInstallingModpack = true
                    onImportModpack(uri, { prog ->
                        modpackProgress = prog
                    }) { result ->
                        isInstallingModpack = false
                        pendingModpackIndex = null
                        pendingModpackUri = null
                        modpackProgress = null
                        result.fold(
                            onSuccess = { installed ->
                                Toast.makeText(context, "Modpack '${installed.name}' installed successfully!", Toast.LENGTH_LONG).show()
                            },
                            onFailure = { err ->
                                Toast.makeText(context, "Modpack install failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            },
            icon = Icons.Rounded.Widgets
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = meta.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!meta.summary.isNullOrBlank()) {
                    Text(
                        text = meta.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Text(
                    text = "- $serverModsCount server mods will be downloaded into mods/",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "- Configuration overrides will be applied to the server root",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val targetMc = meta.dependencies["minecraft"]
                if (targetMc != null) {
                    Text(
                        text = "- Target Minecraft: $targetMc (Current: ${server.version})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (targetMc == server.version) MaterialTheme.colorScheme.primary else extColors.warning
                    )
                }
            }
        }
    }

    if (isInstallingModpack && modpackProgress != null) {
        val prog = modpackProgress!!
        MineServeDialog(
            onDismissRequest = {},
            title = "Installing Modpack...",
            confirmLabel = "Installing",
            onConfirm = {},
            confirmEnabled = false
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = prog.stage,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = prog.currentItem,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = monoFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { prog.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(ShapeDefaults.ExtraSmall),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Text(
                    text = "${prog.currentCount} / ${prog.totalCount} files (${(prog.progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = monoFont,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (selectedProjectForDetails != null) {
        val proj = selectedProjectForDetails!!
        val isInstalling = installingMap[proj.id] == true
        MineServeDialog(
            onDismissRequest = {
                selectedProjectForDetails = null
                projectDetails = null
            },
            title = proj.name,
            confirmLabel = if (isInstalling) "Installing..." else stringResource(R.string.plugins_install_btn),
            onConfirm = {
                installingMap[proj.id] = true
                scope.launch {
                    val resolved = ModrinthApiClient().resolveDownloadUrl(
                        projectIdOrSlug = proj.id,
                        isMod = isModServer,
                        loaderFilter = loaderFilter,
                        gameVersion = server.version
                    )
                    if (resolved != null) {
                        onInstallPluginOrMod(resolved.first, resolved.second, isModServer) { success ->
                            installingMap[proj.id] = false
                            if (success) {
                                Toast.makeText(context, "Installed ${resolved.first}!", Toast.LENGTH_SHORT).show()
                                selectedProjectForDetails = null
                            } else {
                                Toast.makeText(context, "Failed to install ${resolved.first}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        installingMap[proj.id] = false
                        Toast.makeText(context, "No compatible release found for this server version", Toast.LENGTH_LONG).show()
                    }
                }
            },
            dismissLabel = "Close",
            confirmEnabled = !isInstalling
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!proj.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(proj.iconUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = proj.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(ShapeDefaults.Small)
                        )
                    } else {
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isModServer) Icons.Rounded.Widgets else Icons.Rounded.Extension,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Column {
                        if (proj.author.isNotBlank()) {
                            Text(
                                text = "By ${proj.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (proj.downloads > 0) {
                            Text(
                                text = "Downloads: ${String.format(Locale.US, "%,d", proj.downloads)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = monoFont,
                                fontWeight = FontWeight.SemiBold,
                                color = extColors.warning
                            )
                        }
                    }
                }

                if (isLoadingDetails) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    if (proj.categories.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            proj.categories.take(6).forEach { cat ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                    border = null,
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    val desc = projectDetails?.description?.ifBlank { proj.description } ?: proj.description
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!projectDetails?.body.isNullOrBlank()) {
                        Text(
                            text = projectDetails!!.body.take(600) + if (projectDetails!!.body.length > 600) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    pendingDeletePlugin?.let { plugin ->
        MineServeDialog(
            onDismissRequest = { pendingDeletePlugin = null },
            title = "Delete ${plugin.name}?",
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                pendingDeletePlugin = null
                onDeletePlugin(plugin)
            },
            icon = Icons.Rounded.Delete,
            isDestructive = true
        ) {
            MineServeTableMonoText(
                text = plugin.fileName,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        // Top Title & Upload Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                MineServeTableMonoText(
                    text = "${server.type.displayName} - ${if (isModServer) "mods/" else "plugins/"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isModServer || isHybrid) {
                    FilledTonalButton(
                        onClick = { modpackPickerLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = ShapeDefaults.Small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Widgets, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(".mrpack", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                FilledTonalButton(
                    onClick = {
                        jarPickerLauncher.launch(arrayOf("application/java-archive", "application/zip", "application/octet-stream", "*/*"))
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = ShapeDefaults.Small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload .JAR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Modrinth Search Card
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search Modrinth",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = if (isModServer) "Mods ($loaderFilter)" else "Plugins (Paper/Spigot)",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = monoFont,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = searchPlaceholder,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                                scope.launch {
                                    searchResults = ModrinthApiClient().search(
                                        query = searchQuery,
                                        isMod = isModServer,
                                        loaderFilter = loaderFilter,
                                        gameVersion = server.version
                                    )
                                    isSearching = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Small,
                        enabled = !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                    }
                }

                // Search Results List
                if (searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (result in searchResults) {
                            val isInstalling = installingMap[result.id] == true
                            val alreadyInstalled = plugins.any {
                                it.fileName.contains(result.slug, ignoreCase = true) ||
                                    it.name.contains(result.name, ignoreCase = true)
                            }

                            MineServeCard(
                                onClick = {
                                    selectedProjectForDetails = result
                                    isLoadingDetails = true
                                    scope.launch {
                                        projectDetails = ModrinthApiClient().getProjectDetails(result.id)
                                        isLoadingDetails = false
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentPadding = PaddingValues(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!result.iconUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(result.iconUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = result.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(ShapeDefaults.Small)
                                            )
                                        } else {
                                            Surface(
                                                shape = ShapeDefaults.Small,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isModServer) Icons.Rounded.Widgets else Icons.Rounded.Extension,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = result.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (result.description.isNotBlank()) {
                                                Text(
                                                    text = result.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalButton(
                                        onClick = {
                                            installingMap[result.id] = true
                                            scope.launch {
                                                val resolved = ModrinthApiClient().resolveDownloadUrl(
                                                    projectIdOrSlug = result.id,
                                                    isMod = isModServer,
                                                    loaderFilter = loaderFilter,
                                                    gameVersion = server.version
                                                )
                                                if (resolved != null) {
                                                    onInstallPluginOrMod(resolved.first, resolved.second, isModServer) { success ->
                                                        installingMap[result.id] = false
                                                        if (success) {
                                                            Toast.makeText(context, "Installed ${resolved.first}!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to install ${resolved.first}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    installingMap[result.id] = false
                                                    Toast.makeText(context, "No download found for ${result.name}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (alreadyInstalled) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            },
                                            contentColor = if (alreadyInstalled) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            }
                                        ),
                                        shape = ShapeDefaults.Small,
                                        enabled = !isInstalling,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isInstalling) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else if (alreadyInstalled) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Installed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Text(
                                                text = stringResource(R.string.plugins_install_btn),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Installed List Section
        Text(
            text = "$installedSectionTitle (${plugins.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (plugins.isEmpty()) {
            MineServeEmptyState(
                icon = if (isModServer) Icons.Rounded.Widgets else Icons.Rounded.Extension,
                title = emptyMessage,
                supportingText = "Search Modrinth above or upload a .jar file."
            )
        } else {
            MineServeTableContainer {
                plugins.forEachIndexed { index, p ->
                    MineServeTableRow(showDivider = index < plugins.lastIndex) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = ShapeDefaults.Small,
                                color = if (p.enabled) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (p.isMod) Icons.Rounded.Widgets else Icons.Rounded.Extension,
                                        contentDescription = null,
                                        tint = if (p.enabled) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (p.enabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                MineServeTableMonoText(
                                    text = "${p.fileName} - ${p.formattedSize}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = p.enabled,
                                onCheckedChange = { onTogglePlugin(p) }
                            )
                            IconButton(onClick = { pendingDeletePlugin = p }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeTabPreviews
@Composable
private fun PluginsTabPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        PluginsTab(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            plugins = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.samplePlugins,
            onTogglePlugin = {},
            onDeletePlugin = {},
            onInstallPluginOrMod = { _, _, _, _ -> },
            onImportJar = { _, _, _ -> }
        )
    }
}
