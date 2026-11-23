package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.desktop.ui.DesktopColors
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopPanelCard
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun DesktopFilesTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val currentPath by viewModel.currentFilePath.collectAsState()
    val entries by viewModel.directoryEntries.collectAsState()
    val editingPath by viewModel.editingFilePath.collectAsState()
    val editingContent by viewModel.editingFileContent.collectAsState()

    var newItemName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left: File Browser
        DesktopPanelCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Server Files", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "/$currentPath",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = DesktopColors.EmeraldLight
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentPath.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val parent = currentPath.substringBeforeLast('/', "")
                                viewModel.loadDirectory(server.id, parent)
                            },
                            modifier = Modifier.height(30.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = DesktopColors.Slate300,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Up", fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val dir = File(viewModel.serverRepository.getServerDirectory(server.id), currentPath)
                            viewModel.openInFileManager(dir)
                        },
                        modifier = Modifier.height(30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = DesktopColors.Slate300,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Folder", fontSize = 11.sp, color = DesktopColors.Slate300)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Create File / Folder Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    placeholder = { Text("New file or folder name...", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            viewModel.createNewFile(newItemName.trim())
                            newItemName = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = null,
                        tint = DesktopColors.EmeraldLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("File", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            viewModel.createNewFolder(newItemName.trim())
                            newItemName = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = DesktopColors.EmeraldLight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Folder", fontSize = 11.sp)
                }
            }

            if (renameTarget != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Rename $renameTarget", fontSize = 11.sp) },
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
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { renameTarget = null }) {
                        Text("Cancel", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(entries, key = { it.relativePath }) { entry ->
                    val (entryIcon, entryTint) = resolveFileIconAndTint(entry)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (editingPath == entry.relativePath) DesktopColors.EmeraldPrimary.copy(alpha = 0.14f) else DesktopColors.Slate900,
                        border = BorderStroke(
                            1.dp,
                            if (editingPath == entry.relativePath) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (entry.isDirectory) {
                                    viewModel.loadDirectory(server.id, entry.relativePath)
                                } else if (entry.isEditable) {
                                    viewModel.openFileForEditing(server.id, entry.relativePath)
                                } else {
                                    viewModel.showToast("${entry.name} is binary (${FileStorageUtils.formatBytes(entry.sizeBytes)})")
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = entryIcon,
                                    contentDescription = null,
                                    tint = entryTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.name,
                                        fontWeight = if (entry.isDirectory) FontWeight.Bold else FontWeight.Medium,
                                        color = if (entry.isDirectory) DesktopColors.EmeraldLight else Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (entry.isDirectory) "Directory" else FileStorageUtils.formatBytes(entry.sizeBytes),
                                        fontSize = 10.sp,
                                        color = DesktopColors.Slate400
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (entry.name.endsWith(".zip", ignoreCase = true)) {
                                    OutlinedButton(
                                        onClick = { viewModel.unzipEntry(entry.relativePath) },
                                        modifier = Modifier.height(26.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text("Unzip", fontSize = 10.sp, color = DesktopColors.EmeraldLight)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        renameTarget = entry.relativePath
                                        renameText = entry.name
                                    },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Rename", fontSize = 10.sp, color = DesktopColors.Slate300)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.deleteEntry(entry.relativePath) },
                                    modifier = Modifier.height(26.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text("Del", fontSize = 10.sp, color = DesktopColors.RedstoneLight)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right: Built-in Text & Config Editor
        DesktopPanelCard(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
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
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = DesktopColors.EmeraldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = editingPath?.let { "Editing: $it" } ?: "Select a config or text file to edit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (editingPath != null) {
                    Button(
                        onClick = { viewModel.saveEditingFile() },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        Text("Save File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (editingPath == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Click any .properties, .yml, .json, .toml, or .txt file on the left to inspect and edit it.",
                        color = DesktopColors.Slate400,
                        fontSize = 12.sp
                    )
                }
            } else {
                OutlinedTextField(
                    value = editingContent,
                    onValueChange = { viewModel.updateEditingContent(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}

private fun resolveFileIconAndTint(entry: FileEntry): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    if (entry.isDirectory) {
        return Icons.Default.Folder to DesktopColors.EmeraldLight
    }
    val ext = entry.extension.lowercase()
    return when (ext) {
        "zip", "jar", "mrpack", "gz", "tar" -> Icons.Default.Archive to DesktopColors.AmberLight
        "yml", "yaml", "json", "toml", "properties", "conf", "cfg", "ini", "sh", "xml" ->
            Icons.Default.Code to DesktopColors.EmeraldPrimary
        "txt", "log", "md", "csv" -> Icons.Default.Description to DesktopColors.Slate300
        else -> Icons.Default.InsertDriveFile to DesktopColors.Slate400
    }
}

@Composable
fun DesktopBackupsTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val localBackups by viewModel.localBackups.collectAsState()
    val cloudBackups by viewModel.cloudBackups.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm") }
    val backupsDir = remember(server.id) { viewModel.serverRepository.getBackupsDirectory(server.id) }

    LaunchedEffect(server.id) {
        viewModel.refreshCloudBackups()
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left: Local Backups
        DesktopPanelCard(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Local Backups (${localBackups.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.createBackup(worldOnly = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        Text("+ World Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { viewModel.createBackup(worldOnly = false) }) {
                        Text("+ Full Backup", fontSize = 12.sp, color = DesktopColors.Slate300)
                    }
                    OutlinedButton(
                        onClick = { viewModel.openInFileManager(backupsDir) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = DesktopColors.Slate300,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Directory", fontSize = 12.sp, color = DesktopColors.Slate300)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (localBackups.isEmpty()) {
                Text("No local backups yet. Create a World or Full backup above.", color = DesktopColors.Slate400, fontSize = 12.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(localBackups, key = { it.fileName }) { b ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DesktopColors.Slate900,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = DesktopColors.EmeraldLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = b.fileName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${if (b.isWorldOnly) "World Only" else "Full Server"} - ${FileStorageUtils.formatBytes(b.sizeBytes)} - ${dateFormat.format(Date(b.timestamp))}",
                                            fontSize = 11.sp,
                                            color = DesktopColors.Slate400
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.openInFileManager(backupsDir) },
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = DesktopColors.Slate300,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Directory", fontSize = 11.sp, color = DesktopColors.Slate300)
                                    }
                                    if (viewModel.cloudManager.isLinked) {
                                        OutlinedButton(
                                            onClick = { viewModel.uploadBackupToCloud(b.fileName) },
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Cloud Upload", fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.restoreBackup(b.fileName) },
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Restore", fontSize = 11.sp, color = DesktopColors.Slate300)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.deleteBackup(b.fileName) },
                                        modifier = Modifier.height(30.dp),
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

        // Right: MineServe Cloud Off-Device Backups
        DesktopPanelCard(modifier = Modifier.weight(0.85f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MineServe Cloud Snapshots", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DesktopColors.EmeraldLight)
                OutlinedButton(
                    onClick = {
                        if (viewModel.cloudManager.isLinked) viewModel.refreshCloudBackups()
                        else viewModel.openModal(DesktopModal.CLOUD)
                    },
                    modifier = Modifier.height(30.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text(if (viewModel.cloudManager.isLinked) "Refresh" else "Sign In / Plans", fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (!viewModel.cloudManager.isLinked) {
                Text(
                    text = "Store encrypted off-device backups in Cloudflare R2 (10 GB on Plus, 50 GB on Pro). Link your MineServe Cloud account to upload and restore snapshots.",
                    fontSize = 12.sp,
                    color = DesktopColors.Slate400
                )
            } else if (cloudBackups.isEmpty()) {
                Text("No cloud snapshots uploaded for this server yet.", fontSize = 12.sp, color = DesktopColors.Slate400)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cloudBackups.size) { idx ->
                        val item = cloudBackups[idx]
                        val id = item.optString("backupId", item.optString("id", ""))
                        val name = item.optString("fileName", id)
                        val size = item.optLong("sizeBytes", 0L)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DesktopColors.Slate900,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    Text(FileStorageUtils.formatBytes(size), fontSize = 10.sp, color = DesktopColors.Slate400)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
