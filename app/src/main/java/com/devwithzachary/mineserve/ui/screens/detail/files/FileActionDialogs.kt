package com.devwithzachary.mineserve.ui.screens.detail.files

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import com.devwithzachary.mineserve.ui.components.AppAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900

@Composable
fun CreateFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newFileName by remember { mutableStateOf("") }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter file name with extension (e.g. config.yml):", color = Slate400, fontSize = 13.sp)
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("filename.yml", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = newFileName.trim()
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Folder", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter folder name:", color = Slate400, fontSize = 13.sp)
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("folder_name", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = newFolderName.trim()
                    if (name.isNotBlank()) {
                        onConfirm(name)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun RenameFileDialog(
    target: FileEntry,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var renameTargetName by remember { mutableStateOf(target.name) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${if (target.isDirectory) "Folder" else "File"}", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            OutlinedTextField(
                value = renameTargetName,
                onValueChange = { renameTargetName = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = Slate700
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val newName = renameTargetName.trim()
                    if (newName.isNotBlank() && newName != target.name) {
                        onConfirm(newName)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DeleteFileDialog(
    target: FileEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = RedstoneRed, modifier = Modifier.size(32.dp))
        },
        title = { Text("Delete ${target.name}?", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Text(
                if (target.isDirectory) "This will permanently delete this folder and all its contents."
                else "This will permanently delete this file from the server.",
                color = Slate400,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun UnzipArchiveDialog(
    target: FileEntry,
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (destinationPath: String, deleteZipAfter: Boolean) -> Unit
) {
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

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FolderZip,
                contentDescription = null,
                tint = DiamondCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Extract Archive", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Archive Info Card
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Slate800),
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
                            Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = DiamondCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = target.formattedSize,
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Destination Directory info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Destination:",
                        color = Slate400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayDestination,
                            color = EmeraldLight,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Extract to subfolder checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { extractToSubfolder = !extractToSubfolder }
                ) {
                    Checkbox(
                        checked = extractToSubfolder,
                        onCheckedChange = { extractToSubfolder = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldPrimary,
                            uncheckedColor = Slate700,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Extract into subfolder",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                if (extractToSubfolder) {
                    OutlinedTextField(
                        value = subfolderName,
                        onValueChange = { subfolderName = it },
                        label = { Text("Subfolder name", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Delete archive checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { deleteZipAfter = !deleteZipAfter }
                ) {
                    Checkbox(
                        checked = deleteZipAfter,
                        onCheckedChange = { deleteZipAfter = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldPrimary,
                            uncheckedColor = Slate700,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "Delete archive after unzip",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Saves storage once extracted",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(resolvedDestination, deleteZipAfter)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Extract", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        },
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(16.dp)
    )
}

