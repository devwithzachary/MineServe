package com.devwithzachary.mineserve.ui.screens.detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.api.ModrinthApiClient
import com.devwithzachary.mineserve.api.ModrinthProjectDetails
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ServerType
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
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginsTab(
    server: MinecraftServer,
    plugins: List<PluginModEntry>,
    onTogglePlugin: (PluginModEntry) -> Unit,
    onDeletePlugin: (PluginModEntry) -> Unit,
    onInstallPluginOrMod: (fileName: String, downloadUrl: String, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit,
    onImportJar: (Uri, isMod: Boolean, onResult: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        ServerType.NEOFORGE -> "neoforge"
        else -> null
    }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<PluginModEntry>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Installing states per project id
    val installingMap = remember { mutableStateMapOf<String, Boolean>() }

    // Selected project detail dialog
    var selectedProjectForDetails by remember { mutableStateOf<PluginModEntry?>(null) }
    var projectDetails by remember { mutableStateOf<ModrinthProjectDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    // File picker launcher for uploading custom .jar files
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

    // Detail Dialog
    if (selectedProjectForDetails != null) {
        val proj = selectedProjectForDetails!!
        AlertDialog(
            onDismissRequest = {
                selectedProjectForDetails = null
                projectDetails = null
            },
            title = {
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
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate800,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isModServer) Icons.Default.Widgets else Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = proj.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (proj.author.isNotBlank()) {
                            Text(
                                text = "By ${proj.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isLoadingDetails) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        // Categories
                        if (proj.categories.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                proj.categories.take(6).forEach { cat ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Slate800
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldLight,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Downloads / Stats
                        if (proj.downloads > 0) {
                            Text(
                                text = "Downloads: ${String.format(java.util.Locale.US, "%,d", proj.downloads)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldYellow
                            )
                        }

                        // Description
                        val desc = projectDetails?.description?.ifBlank { proj.description } ?: proj.description
                        if (desc.isNotBlank()) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        if (!projectDetails?.body.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = projectDetails!!.body.take(600) + if (projectDetails!!.body.length > 600) "…" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val isInstalling = installingMap[proj.id] == true
                Button(
                    onClick = {
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isInstalling
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Installing…", color = Color.Black)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.plugins_install_btn), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        selectedProjectForDetails = null
                        projectDetails = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title & Upload Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${server.type.displayName} • ${if (isModServer) "mods/" else "plugins/"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }

            Button(
                onClick = {
                    jarPickerLauncher.launch(arrayOf("application/java-archive", "application/zip", "application/octet-stream", "*/*"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Upload .JAR",
                    color = EmeraldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Modrinth Search Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search Modrinth",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = if (isModServer) "Mods ($loaderFilter)" else "Plugins (Paper/Spigot)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
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
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black)
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

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate900,
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProjectForDetails = result
                                        isLoadingDetails = true
                                        scope.launch {
                                            projectDetails = ModrinthApiClient().getProjectDetails(result.id)
                                            isLoadingDetails = false
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Mod / Plugin Icon
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
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Slate800,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isModServer) Icons.Default.Widgets else Icons.Default.Extension,
                                                        contentDescription = null,
                                                        tint = EmeraldPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = result.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            if (result.description.isNotBlank()) {
                                                Text(
                                                    text = result.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Slate400,
                                                    maxLines = 1,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
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
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (alreadyInstalled) EmeraldDark else EmeraldPrimary
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        enabled = !isInstalling
                                    ) {
                                        if (isInstalling) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black)
                                        } else if (alreadyInstalled) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Installed", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text(stringResource(R.string.plugins_install_btn), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            color = Color.White
        )

        if (plugins.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            for (p in plugins) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (p.enabled) EmeraldDark.copy(alpha = 0.4f) else Slate800,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (p.isMod) Icons.Default.Widgets else Icons.Default.Extension,
                                        contentDescription = null,
                                        tint = if (p.enabled) EmeraldPrimary else Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = p.name,
                                    fontWeight = FontWeight.Bold,
                                    color = if (p.enabled) Color.White else Slate400
                                )
                                Text(
                                    text = "${p.fileName} • ${p.formattedSize}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = p.enabled,
                                onCheckedChange = { onTogglePlugin(p) },
                                colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                            )
                            IconButton(onClick = { onDeletePlugin(p) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = RedstoneLight)
                            }
                        }
                    }
                }
            }
        }
    }
}
