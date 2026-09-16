package com.devwithzachary.mineserve.ui.screens.detail.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileListItem(
    entry: FileEntry,
    onOpenFolder: () -> Unit,
    onOpenFile: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val icon: ImageVector = when {
        entry.isDirectory -> Icons.Default.Folder
        entry.extension in setOf("yml", "yaml", "json", "toml", "properties", "cfg", "conf") -> Icons.Default.Code
        entry.isLog -> Icons.Default.Description
        entry.isArchive -> Icons.Default.FolderZip
        entry.isWorldRegion -> Icons.Default.Public
        entry.extension == "jar" -> Icons.Default.Extension
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    val iconColor = when {
        entry.isDirectory -> GoldYellow
        entry.extension in setOf("yml", "yaml", "json", "toml", "properties") -> EmeraldPrimary
        entry.isLog -> Slate400
        entry.isArchive -> DiamondCyan
        entry.extension == "jar" -> Color(0xFFC084FC)
        else -> Slate400
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    val formattedDate = remember(entry.lastModified) {
        if (entry.lastModified > 0) dateFormat.format(Date(entry.lastModified)) else ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (entry.isDirectory) onOpenFolder() else onOpenFile()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry.isDirectory) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontSize = 11.sp
                    )
                    if (formattedDate.isNotBlank()) {
                        Text("•", color = Slate700, fontSize = 10.sp)
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Slate400
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!entry.isDirectory && (entry.isEditable || entry.isLog)) {
                    DropdownMenuItem(
                        text = { Text("Edit in Code Editor") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary) },
                        onClick = {
                            showMenu = false
                            onOpenFile()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Slate400) },
                    onClick = {
                        showMenu = false
                        onRename()
                    }
                )
                if (!entry.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Slate400) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export to Downloads") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Slate400) },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = RedstoneRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RedstoneRed) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
