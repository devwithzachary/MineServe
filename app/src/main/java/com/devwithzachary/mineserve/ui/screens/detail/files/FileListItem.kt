package com.devwithzachary.mineserve.ui.screens.detail.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Public
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.FileEntry
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
    onUnzip: () -> Unit = {},
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val icon: ImageVector = when {
        entry.isDirectory -> Icons.Rounded.Folder
        entry.extension in setOf("yml", "yaml", "json", "toml", "properties", "cfg", "conf") -> Icons.Rounded.Code
        entry.isLog -> Icons.Rounded.Description
        entry.isArchive -> Icons.Rounded.FolderZip
        entry.isWorldRegion -> Icons.Rounded.Public
        entry.extension == "jar" -> Icons.Rounded.Extension
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }

    val iconColor = when {
        entry.isDirectory -> extColors.warning
        entry.extension in setOf("yml", "yaml", "json", "toml", "properties") -> MaterialTheme.colorScheme.primary
        entry.isLog -> MaterialTheme.colorScheme.onSurfaceVariant
        entry.isArchive -> MaterialTheme.colorScheme.tertiary
        entry.extension == "jar" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    val formattedDate = remember(entry.lastModified) {
        if (entry.lastModified > 0) dateFormat.format(Date(entry.lastModified)) else ""
    }

    MineServeTableRow(
        modifier = modifier,
        showDivider = showDivider,
        onClick = { if (entry.isDirectory) onOpenFolder() else onOpenFile() }
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
                modifier = Modifier.size(22.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = monoFont,
                    fontWeight = if (entry.isDirectory) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MineServeTableMonoText(
                        text = entry.formattedSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (formattedDate.isNotBlank()) {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        MineServeTableMonoText(
                            text = formattedDate,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!entry.isDirectory && (entry.isEditable || entry.isLog)) {
                    DropdownMenuItem(
                        text = { Text("Edit in Code Editor") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            showMenu = false
                            onOpenFile()
                        }
                    )
                }
                if (!entry.isDirectory && (entry.isArchive || entry.extension.equals("zip", ignoreCase = true))) {
                    DropdownMenuItem(
                        text = { Text("Unzip / Extract") },
                        leadingIcon = {
                            Icon(Icons.Rounded.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        },
                        onClick = {
                            showMenu = false
                            onUnzip()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = {
                        Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onRename()
                    }
                )
                if (!entry.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export to Downloads") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
