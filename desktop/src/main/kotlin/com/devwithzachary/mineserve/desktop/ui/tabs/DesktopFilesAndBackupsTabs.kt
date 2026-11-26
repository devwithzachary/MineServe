package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.backup.cloud.CloudProviderType
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
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopFilesTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val currentPath by viewModel.currentFilePath.collectAsState()
    val entries by viewModel.directoryEntries.collectAsState()
    val editingPath by viewModel.editingFilePath.collectAsState()
    val editingContent by viewModel.editingFileContent.collectAsState()

    var newItemName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<FileEntry?>(null) }

    pendingDelete?.let { target ->
        MineServeDialog(
            onDismissRequest = { pendingDelete = null },
            title = "Delete ${target.name}?",
            confirmLabel = "Delete",
            isDestructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = {
                viewModel.deleteEntry(target.relativePath)
                pendingDelete = null
            }
        ) {
            Text(
                text = "This permanently removes ${target.relativePath} from disk.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left: File Browser
        MineServeCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Breadcrumbs + Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val segments = remember(currentPath) {
                    currentPath.split('/').filter { it.isNotBlank() }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { viewModel.loadDirectory(server.id, "") },
                        shape = ShapeDefaults.Small,
                        color = if (currentPath.isEmpty()) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                        contentColor = if (currentPath.isEmpty()) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                        border = null
                    ) {
                        Text(
                            text = "/",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = monoFont),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    segments.forEachIndexed { idx, segment ->
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        val segmentPath = segments.take(idx + 1).joinToString("/")
                        val isLast = idx == segments.lastIndex
                        Surface(
                            onClick = { viewModel.loadDirectory(server.id, segmentPath) },
                            shape = ShapeDefaults.Small,
                            color = if (isLast) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                            contentColor = if (isLast) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                            border = null
                        ) {
                            Text(
                                text = segment,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = monoFont),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (currentPath.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = {
                                val parent = currentPath.substringBeforeLast('/', "")
                                viewModel.loadDirectory(server.id, parent)
                            },
                            shape = ShapeDefaults.Small,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Up", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            val dialog = FileDialog(null as Frame?, "Upload File to /$currentPath", FileDialog.LOAD)
                            dialog.isVisible = true
                            val dir = dialog.directory
                            val file = dialog.file
                            if (dir != null && file != null) {
                                viewModel.importServerFile(server.id, currentPath, File(dir, file))
                            }
                        },
                        shape = ShapeDefaults.Small,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(
                        onClick = {
                            val dir = File(viewModel.serverRepository.getServerDirectory(server.id), currentPath)
                            viewModel.openInFileManager(dir)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "Open Folder",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Create File / Folder Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    placeholder = { Text("New file or folder name...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            viewModel.createNewFile(newItemName.trim())
                            newItemName = ""
                        }
                    },
                    shape = ShapeDefaults.Small
                ) {
                    Icon(Icons.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("File", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            viewModel.createNewFolder(newItemName.trim())
                            newItemName = ""
                        }
                    },
                    shape = ShapeDefaults.Small
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Folder", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (renameTarget != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Rename $renameTarget") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val target = renameTarget
                            if (target != null && renameText.isNotBlank()) {
                                viewModel.renameEntry(target, renameText.trim())
                                renameTarget = null
                            }
                        },
                        shape = ShapeDefaults.Small
                    ) {
                        Text("Apply")
                    }
                    TextButton(onClick = { renameTarget = null }) {
                        Text("Cancel")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (entries.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.FolderOpen,
                        title = "Empty Directory",
                        supportingText = "Create a file or folder above, or upload an existing configuration file."
                    )
                }
            } else {
                MineServeTableContainer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(entries, key = { it.relativePath }) { entry ->
                            val (entryIcon, entryTint) = resolveFileIconAndTint(entry)
                            val isSelected = editingPath == entry.relativePath
                            MineServeTableRow(
                                showDivider = entry != entries.last(),
                                onClick = {
                                    if (entry.isDirectory) {
                                        viewModel.loadDirectory(server.id, entry.relativePath)
                                    } else if (entry.isEditable) {
                                        viewModel.openFileForEditing(server.id, entry.relativePath)
                                    } else {
                                        viewModel.showToast("${entry.name} is binary (${FileStorageUtils.formatBytes(entry.sizeBytes)})")
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = entryIcon,
                                    contentDescription = null,
                                    tint = entryTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                        fontWeight = if (entry.isDirectory || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> colorScheme.primary
                                            entry.isDirectory -> colorScheme.primary
                                            else -> colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    MineServeTableMonoText(
                                        text = if (entry.isDirectory) "Directory" else FileStorageUtils.formatBytes(entry.sizeBytes),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (entry.name.endsWith(".zip", ignoreCase = true)) {
                                        FilledTonalButton(
                                            onClick = { viewModel.unzipEntry(entry.relativePath) },
                                            shape = ShapeDefaults.Small,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Unzip", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            renameTarget = entry.relativePath
                                            renameText = entry.name
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DriveFileRenameOutline,
                                            contentDescription = "Rename",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { pendingDelete = entry },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete",
                                            tint = colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right: Integrated #070A12 JetBrains Mono Config / Code Editor
        MineServeCard(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = editingPath ?: "Config & Code Editor",
                        style = if (editingPath != null) {
                            MaterialTheme.typography.titleSmall.copy(fontFamily = monoFont)
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (editingPath != null) {
                    Button(
                        onClick = { viewModel.saveEditingFile() },
                        shape = ShapeDefaults.Medium
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save File")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (editingPath == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.EditNote,
                        title = "No File Selected",
                        supportingText = "Select any .properties, .yml, .json, .toml, or .txt file on the left to inspect and edit it."
                    )
                }
            } else {
                Surface(
                    shape = ShapeDefaults.Medium,
                    color = extended.terminalSurface,
                    contentColor = extended.terminalOnSurface,
                    border = null,
                    modifier = Modifier.fillMaxSize()
                ) {
                    TextField(
                        value = editingContent,
                        onValueChange = { viewModel.updateEditingContent(it) },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = monoFont,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = extended.terminalSurface,
                            unfocusedContainerColor = extended.terminalSurface,
                            focusedTextColor = extended.terminalOnSurface,
                            unfocusedTextColor = extended.terminalOnSurface,
                            cursorColor = colorScheme.primary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun resolveFileIconAndTint(entry: FileEntry): Pair<ImageVector, Color> {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    if (entry.isDirectory) {
        return Icons.Rounded.Folder to colorScheme.primary
    }
    val ext = entry.extension.lowercase(Locale.US)
    return when (ext) {
        "zip", "jar", "mrpack", "gz", "tar" -> Icons.Rounded.Archive to extended.warning
        "yml", "yaml", "json", "toml", "properties", "conf", "cfg", "ini", "sh", "xml" ->
            Icons.Rounded.Code to colorScheme.tertiary
        "txt", "log", "md", "csv" -> Icons.Rounded.Description to colorScheme.onSurface
        else -> Icons.Rounded.InsertDriveFile to colorScheme.onSurfaceVariant
    }
}

@Composable
fun DesktopBackupsTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    val localBackups by viewModel.localBackups.collectAsState()
    val cloudBackups by viewModel.cloudBackups.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val backupsDir = remember(server.id) { viewModel.serverRepository.getBackupsDirectory(server.id) }

    var pendingRestore by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    var cloudSyncEnabled by remember(server.id) { mutableStateOf(server.cloudBackupConfig.enabled) }
    var cloudProvider by remember(server.id) { mutableStateOf(server.cloudBackupConfig.provider) }
    var webDavUrl by remember(server.id) { mutableStateOf(server.cloudBackupConfig.webDavUrl) }
    var webDavUser by remember(server.id) { mutableStateOf(server.cloudBackupConfig.webDavUsername) }
    var webDavPass by remember(server.id) { mutableStateOf(server.cloudBackupConfig.webDavPassword) }
    var webDavRemotePath by remember(server.id) { mutableStateOf(server.cloudBackupConfig.webDavRemotePath) }

    LaunchedEffect(server.id) {
        viewModel.refreshCloudBackups()
    }

    pendingRestore?.let { fileName ->
        MineServeDialog(
            onDismissRequest = { pendingRestore = null },
            title = "Restore Backup?",
            confirmLabel = "Restore",
            icon = Icons.Rounded.Restore,
            onConfirm = {
                viewModel.restoreBackup(fileName)
                pendingRestore = null
            }
        ) {
            Text(
                text = "Restoring $fileName overwrites matching world and server files. Make sure the server is stopped first.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    pendingDelete?.let { fileName ->
        MineServeDialog(
            onDismissRequest = { pendingDelete = null },
            title = "Delete Backup?",
            confirmLabel = "Delete",
            isDestructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = {
                viewModel.deleteBackup(fileName)
                pendingDelete = null
            }
        ) {
            Text(
                text = "Permanently delete local snapshot $fileName?",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left: Local Snapshots
        MineServeCard(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Local Snapshots (${localBackups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.createBackup(worldOnly = true) },
                        shape = ShapeDefaults.Medium
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("World Backup")
                    }
                    FilledTonalButton(
                        onClick = { viewModel.createBackup(worldOnly = false) },
                        shape = ShapeDefaults.Medium
                    ) {
                        Text("Full Backup")
                    }
                    IconButton(onClick = { viewModel.openInFileManager(backupsDir) }) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "Open Backups Folder",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (localBackups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.Archive,
                        title = "No Local Backups",
                        supportingText = "Create a World Backup or Full Server snapshot above."
                    )
                }
            } else {
                MineServeTableContainer(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(localBackups, key = { it.fileName }) { b ->
                            MineServeTableRow(
                                showDivider = b != localBackups.last(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Archive,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = b.fileName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    MineServeTableMonoText(
                                        text = "${if (b.isWorldOnly) "World" else "Full"} - ${FileStorageUtils.formatBytes(b.sizeBytes)} - ${dateFormat.format(Date(b.timestamp))}",
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (viewModel.cloudManager.isLinked) {
                                        FilledTonalButton(
                                            onClick = { viewModel.uploadBackupToCloud(b.fileName) },
                                            shape = ShapeDefaults.Small,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = colorScheme.tertiaryContainer,
                                                contentColor = colorScheme.onTertiaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cloud", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { pendingRestore = b.fileName },
                                        shape = ShapeDefaults.Small,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Restore", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(
                                        onClick = { pendingDelete = b.fileName },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete backup",
                                            tint = colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right: Cloud Quota, Off-Device Snapshots & WebDAV Sync Config
        Column(
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                val cloudJson = remember(viewModel.cloudManager.isLinked, cloudBackups) {
                    viewModel.cloudManager.toStatusJson()
                }
                val tier = cloudJson.optString("tier", "FREE").uppercase(Locale.US)
                val usedBytes = cloudJson.optLong("storageUsedBytes", 0L)
                val quotaBytes = when (tier) {
                    "PRO" -> 50L * 1024L * 1024L * 1024L
                    "PLUS" -> 10L * 1024L * 1024L * 1024L
                    else -> 0L
                }
                val quotaProgress = if (quotaBytes > 0L) (usedBytes.toFloat() / quotaBytes.toFloat()).coerceIn(0f, 1f) else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "MineServe Cloud Snapshots",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (viewModel.cloudManager.isLinked) viewModel.refreshCloudBackups()
                            else viewModel.openModal(DesktopModal.CLOUD)
                        },
                        shape = ShapeDefaults.Small,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.tertiaryContainer,
                            contentColor = colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(if (viewModel.cloudManager.isLinked) "Refresh" else "Sign In / Plans")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!viewModel.cloudManager.isLinked) {
                    Text(
                        text = "Store encrypted off-device backups in Cloudflare R2 (10 GB on Plus, 50 GB on Pro). Link your MineServe Cloud account to upload and restore snapshots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    if (quotaBytes > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cloud Storage ($tier)",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                            MineServeTableMonoText(
                                text = "${FileStorageUtils.formatBytes(usedBytes)} / ${FileStorageUtils.formatBytes(quotaBytes)}",
                                color = colorScheme.tertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { quotaProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = colorScheme.tertiary,
                            trackColor = colorScheme.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (cloudBackups.isEmpty()) {
                        Text(
                            text = "No cloud snapshots uploaded for this server yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        MineServeTableContainer(modifier = Modifier.fillMaxWidth()) {
                            cloudBackups.forEachIndexed { idx, item ->
                                val id = item.optString("backupId", item.optString("id", ""))
                                val name = item.optString("fileName", id)
                                val size = item.optLong("sizeBytes", 0L)
                                MineServeTableRow(
                                    showDivider = idx < cloudBackups.lastIndex,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        MineServeTableMonoText(
                                            text = FileStorageUtils.formatBytes(size),
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MineServeSettingsGroup(title = "Encrypted Off-Device Sync") {
                MineServePreferenceRow(
                    title = "Auto-Sync Backups",
                    subtitle = "Upload new local backups automatically using ${cloudProvider.displayName}",
                    checked = cloudSyncEnabled,
                    onCheckedChange = { cloudSyncEnabled = it },
                    icon = Icons.Rounded.CloudUpload
                )
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CloudProviderType.entries.filter { it != CloudProviderType.SAF_FOLDER }.forEach { provider ->
                            val selected = cloudProvider == provider
                            FilledTonalButton(
                                onClick = { cloudProvider = provider },
                                shape = ShapeDefaults.Small,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (selected) colorScheme.secondaryContainer else colorScheme.surfaceContainerHigh,
                                    contentColor = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(provider.displayName, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    if (cloudProvider == CloudProviderType.WEBDAV) {
                        OutlinedTextField(
                            value = webDavUrl,
                            onValueChange = { webDavUrl = it },
                            label = { Text("WebDAV Endpoint URL") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = webDavUser,
                                onValueChange = { webDavUser = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = webDavPass,
                                onValueChange = { webDavPass = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = webDavRemotePath,
                            onValueChange = { webDavRemotePath = it },
                            label = { Text("Remote Folder Path") },
                            placeholder = { Text("/MineServe/Backups") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        if (cloudProvider == CloudProviderType.WEBDAV) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.testWebDavConnection(
                                        server.cloudBackupConfig.copy(
                                            enabled = cloudSyncEnabled,
                                            provider = cloudProvider,
                                            webDavUrl = webDavUrl.trim(),
                                            webDavUsername = webDavUser.trim(),
                                            webDavPassword = webDavPass,
                                            webDavRemotePath = webDavRemotePath.trim().ifBlank { "/MineServe/Backups" }
                                        )
                                    )
                                },
                                shape = ShapeDefaults.Medium
                            ) {
                                Text("Test Connection")
                            }
                            FilledTonalButton(
                                onClick = {
                                    val updated = server.cloudBackupConfig.copy(
                                        enabled = cloudSyncEnabled,
                                        provider = cloudProvider,
                                        webDavUrl = webDavUrl.trim(),
                                        webDavUsername = webDavUser.trim(),
                                        webDavPassword = webDavPass,
                                        webDavRemotePath = webDavRemotePath.trim().ifBlank { "/MineServe/Backups" }
                                    )
                                    val nextSrv = server.copy(cloudBackupConfig = updated)
                                    viewModel.serverRepository.updateServer(nextSrv)
                                    viewModel.uploadLatestBackupToWebDav(nextSrv)
                                },
                                shape = ShapeDefaults.Medium
                            ) {
                                Text("Sync Latest")
                            }
                        }
                        Button(
                            onClick = {
                                val next = server.cloudBackupConfig.copy(
                                    enabled = cloudSyncEnabled,
                                    provider = cloudProvider,
                                    webDavUrl = webDavUrl.trim(),
                                    webDavUsername = webDavUser.trim(),
                                    webDavPassword = webDavPass,
                                    webDavRemotePath = webDavRemotePath.trim().ifBlank { "/MineServe/Backups" }
                                )
                                viewModel.serverRepository.updateServer(server.copy(cloudBackupConfig = next))
                                viewModel.showToast("Saved cloud backup sync settings")
                            },
                            shape = ShapeDefaults.Medium
                        ) {
                            Text("Save Sync Settings")
                        }
                    }
                }
            }
        }
    }
}
