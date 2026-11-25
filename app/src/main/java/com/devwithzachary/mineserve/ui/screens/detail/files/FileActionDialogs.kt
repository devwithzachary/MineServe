package com.devwithzachary.mineserve.ui.screens.detail.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.FileEntry

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newFileName by remember { mutableStateOf("") }
    val monoFont = LocalMineServeMonoFontFamily.current

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = "Create New File",
        confirmLabel = "Create",
        onConfirm = {
            val name = newFileName.trim()
            if (name.isNotBlank()) {
                onConfirm(name)
            }
        },
        confirmEnabled = newFileName.trim().isNotBlank()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Enter file name with extension (e.g. config.yml):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = newFileName,
                onValueChange = { newFileName = it },
                placeholder = { Text("filename.yml", fontFamily = monoFont) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    val monoFont = LocalMineServeMonoFontFamily.current

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = "Create New Folder",
        confirmLabel = "Create",
        onConfirm = {
            val name = newFolderName.trim()
            if (name.isNotBlank()) {
                onConfirm(name)
            }
        },
        confirmEnabled = newFolderName.trim().isNotBlank()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Enter folder name:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = newFolderName,
                onValueChange = { newFolderName = it },
                placeholder = { Text("folder_name", fontFamily = monoFont) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RenameFileDialog(
    target: FileEntry,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var renameTargetName by remember { mutableStateOf(target.name) }
    val monoFont = LocalMineServeMonoFontFamily.current

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = "Rename ${if (target.isDirectory) "Folder" else "File"}",
        confirmLabel = "Rename",
        onConfirm = {
            val newName = renameTargetName.trim()
            if (newName.isNotBlank() && newName != target.name) {
                onConfirm(newName)
            } else {
                onDismiss()
            }
        },
        confirmEnabled = renameTargetName.trim().isNotBlank()
    ) {
        OutlinedTextField(
            value = renameTargetName,
            onValueChange = { renameTargetName = it },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DeleteFileDialog(
    target: FileEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    MineServeDialog(
        onDismissRequest = onDismiss,
        title = "Delete ${target.name}?",
        confirmLabel = "Delete",
        onConfirm = onConfirm,
        icon = Icons.Rounded.Delete,
        isDestructive = true
    ) {
        Text(
            text = if (target.isDirectory) {
                "This will permanently delete this folder and all its contents."
            } else {
                "This will permanently delete this file from the server."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UnzipArchiveDialog(
    target: FileEntry,
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (destinationPath: String, deleteZipAfter: Boolean) -> Unit
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    val parentDir = remember(target.relativePath) {
        if (target.relativePath.contains('/')) {
            target.relativePath.substringBeforeLast('/')
        } else {
            ""
        }
    }
    val defaultSubfolderName = remember(target.name) {
        target.name.substringBeforeLast('.')
    }
    var extractToSubfolder by remember { mutableStateOf(false) }
    var subfolderName by remember { mutableStateOf(defaultSubfolderName) }
    var deleteZipAfter by remember { mutableStateOf(false) }

    val resolvedDestination = remember(parentDir, extractToSubfolder, subfolderName) {
        if (extractToSubfolder && subfolderName.isNotBlank()) {
            if (parentDir.isBlank()) subfolderName.trim() else "$parentDir/${subfolderName.trim()}"
        } else {
            parentDir
        }
    }

    val displayDestination = remember(resolvedDestination) {
        if (resolvedDestination.isBlank()) "/" else "/$resolvedDestination/"
    }

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = "Extract Archive",
        confirmLabel = "Extract",
        onConfirm = { onConfirm(resolvedDestination, deleteZipAfter) },
        icon = Icons.Rounded.FolderZip
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = ShapeDefaults.Small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = monoFont,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        MineServeTableMonoText(
                            text = target.formattedSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Destination",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = ShapeDefaults.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayDestination,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = monoFont,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { extractToSubfolder = !extractToSubfolder }
            ) {
                Checkbox(
                    checked = extractToSubfolder,
                    onCheckedChange = { extractToSubfolder = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Extract into subfolder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (extractToSubfolder) {
                OutlinedTextField(
                    value = subfolderName,
                    onValueChange = { subfolderName = it },
                    label = { Text("Subfolder name") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { deleteZipAfter = !deleteZipAfter }
            ) {
                Checkbox(
                    checked = deleteZipAfter,
                    onCheckedChange = { deleteZipAfter = it }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Delete archive after unzip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saves storage once extracted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
