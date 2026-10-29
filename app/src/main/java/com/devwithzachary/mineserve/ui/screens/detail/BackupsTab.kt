package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.backup.cloud.CloudBackupConfig
import com.devwithzachary.mineserve.backup.cloud.CloudBackupManager
import com.devwithzachary.mineserve.backup.cloud.CloudProviderType
import com.devwithzachary.mineserve.backup.cloud.CloudSyncState
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.ui.components.AppAlertDialog
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupsTab(
    server: MinecraftServer? = null,
    backups: List<BackupEntry>,
    onCreateBackup: ((Boolean) -> Unit) -> Unit = {},
    onRestoreBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onExportBackup: (BackupEntry, (String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onGetShareIntent: (BackupEntry) -> Intent? = { null },
    onSaveServer: ((MinecraftServer) -> Unit)? = null,
    onSyncBackupToCloud: ((BackupEntry, (Result<String>) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCreatingBackup by remember { mutableStateOf(false) }
    var restoringBackupId by remember { mutableStateOf<String?>(null) }
    var exportingBackupId by remember { mutableStateOf<String?>(null) }
    var deletingBackupId by remember { mutableStateOf<String?>(null) }
    var syncingBackupId by remember { mutableStateOf<String?>(null) }

    var backupToRestore by remember { mutableStateOf<BackupEntry?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupEntry?>(null) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    // Cloud Backup Sync States
    val cloudManager = remember { CloudBackupManager.getInstance(context) }
    val cloudSyncState by cloudManager.syncState.collectAsStateWithLifecycle()

    var cloudConfig by remember(server?.cloudBackupConfig) {
        mutableStateOf(server?.cloudBackupConfig ?: CloudBackupConfig())
    }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    // SAF folder picker launcher
    val safFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                val updated = cloudConfig.copy(safFolderUri = uri.toString())
                cloudConfig = updated
                if (server != null && onSaveServer != null) {
                    onSaveServer(server.copy(cloudBackupConfig = updated))
                }
                Toast.makeText(context, "Storage folder connected!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not persist folder permission: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showFeedback(message: String, isError: Boolean = false) {
        statusMessage = message
        isStatusError = isError
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        scope.launch {
            delay(4000)
            if (statusMessage == message) {
                statusMessage = null
            }
        }
    }

    // Restore Confirmation Dialog
    if (backupToRestore != null) {
        val target = backupToRestore!!
        AppAlertDialog(
            onDismissRequest = { backupToRestore = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = GoldYellow,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Restore World Backup?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Restoring '${target.name}' will overwrite your current world with this snapshot.\n\n⚠️ Any unsaved changes or new blocks placed since this backup was created will be replaced.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = target
                        backupToRestore = null
                        restoringBackupId = backup.id
                        onRestoreBackup(backup) { success ->
                            restoringBackupId = null
                            if (success) {
                                showFeedback("World successfully restored from ${backup.name}")
                            } else {
                                showFeedback("Failed to restore backup", isError = true)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYellow),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Restore World", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { backupToRestore = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Confirmation Dialog
    if (backupToDelete != null) {
        val target = backupToDelete!!
        AppAlertDialog(
            onDismissRequest = { backupToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RedstoneRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.backups_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.backups_delete_dialog_desc, target.name, target.formattedSize),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = target
                        backupToDelete = null
                        deletingBackupId = backup.id
                        onDeleteBackup(backup) { success ->
                            deletingBackupId = null
                            if (success) {
                                showFeedback("Deleted backup: ${backup.name}")
                            } else {
                                showFeedback("Failed to delete backup", isError = true)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.backups_delete), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { backupToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.backups_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${backups.size} Snapshot(s) Available",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
        }

        // Full-Width Create Backup Button
        Button(
            onClick = {
                if (!isCreatingBackup) {
                    isCreatingBackup = true
                    onCreateBackup { success ->
                        isCreatingBackup = false
                        if (success) {
                            showFeedback("World backup created successfully!")
                        } else {
                            showFeedback("Failed to create world backup", isError = true)
                        }
                    }
                }
            },
            enabled = !isCreatingBackup,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCreatingBackup) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zipping World...", color = Color.Black, fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    Icons.Default.Backup,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.backups_create_world_btn),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Status / Feedback Banner
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isStatusError) RedstoneRed.copy(alpha = 0.2f) else EmeraldDark.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, if (isStatusError) RedstoneLight else EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isStatusError) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isStatusError) RedstoneLight else EmeraldLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isStatusError) RedstoneLight else EmeraldLight
                    )
                }
            }
        }

        // Section: Cloud Backup Sync (F-Droid Compliant)
        Card(
            shape = RoundedCornerShape(14.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Cloud Backup Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldDark.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "F-DROID COMPLIANT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Sync world backups off-device using 100% open-source protocols: WebDAV (Nextcloud / ownCloud) or Android Storage Access Framework (SAF) without proprietary Google Play Services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 12.sp
                )

                // Enable Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Cloud Backup Sync", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Switch(
                        checked = cloudConfig.enabled,
                        onCheckedChange = {
                            val updated = cloudConfig.copy(enabled = it)
                            cloudConfig = updated
                            if (server != null && onSaveServer != null) {
                                onSaveServer(server.copy(cloudBackupConfig = updated))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = EmeraldDark,
                            uncheckedTrackColor = Slate800
                        )
                    )
                }

                if (cloudConfig.enabled) {
                    HorizontalDivider(color = ObsidianCardBorder)

                    // Auto Sync Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Sync on Backup Creation", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text("Upload automatically when a new world snapshot is zipped", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                        Switch(
                            checked = cloudConfig.autoSyncOnBackup,
                            onCheckedChange = {
                                val updated = cloudConfig.copy(autoSyncOnBackup = it)
                                cloudConfig = updated
                                if (server != null && onSaveServer != null) {
                                    onSaveServer(server.copy(cloudBackupConfig = updated))
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = EmeraldDark,
                                uncheckedTrackColor = Slate800
                            )
                        )
                    }

                    // Provider Selection Chips
                    Text("Storage Provider:", style = MaterialTheme.typography.labelSmall, color = Slate400)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = cloudConfig.provider == CloudProviderType.WEBDAV,
                            onClick = {
                                val updated = cloudConfig.copy(provider = CloudProviderType.WEBDAV)
                                cloudConfig = updated
                                if (server != null && onSaveServer != null) {
                                    onSaveServer(server.copy(cloudBackupConfig = updated))
                                }
                            },
                            label = { Text("Nextcloud / WebDAV") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDark,
                                selectedLabelColor = EmeraldLight
                            )
                        )

                        FilterChip(
                            selected = cloudConfig.provider == CloudProviderType.SAF_FOLDER,
                            onClick = {
                                val updated = cloudConfig.copy(provider = CloudProviderType.SAF_FOLDER)
                                cloudConfig = updated
                                if (server != null && onSaveServer != null) {
                                    onSaveServer(server.copy(cloudBackupConfig = updated))
                                }
                            },
                            label = { Text("Android Cloud / SAF Folder") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDark,
                                selectedLabelColor = EmeraldLight
                            )
                        )
                    }

                    if (cloudConfig.provider == CloudProviderType.WEBDAV) {
                        // WebDAV Inputs
                        OutlinedTextField(
                            value = cloudConfig.webDavUrl,
                            onValueChange = {
                                val updated = cloudConfig.copy(webDavUrl = it)
                                cloudConfig = updated
                                if (server != null && onSaveServer != null) {
                                    onSaveServer(server.copy(cloudBackupConfig = updated))
                                }
                            },
                            label = { Text("WebDAV Server URL") },
                            placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/", color = Slate400) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = cloudConfig.webDavUsername,
                                onValueChange = {
                                    val updated = cloudConfig.copy(webDavUsername = it)
                                    cloudConfig = updated
                                    if (server != null && onSaveServer != null) {
                                        onSaveServer(server.copy(cloudBackupConfig = updated))
                                    }
                                },
                                label = { Text("Username") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = cloudConfig.webDavPassword,
                                onValueChange = {
                                    val updated = cloudConfig.copy(webDavPassword = it)
                                    cloudConfig = updated
                                    if (server != null && onSaveServer != null) {
                                        onSaveServer(server.copy(cloudBackupConfig = updated))
                                    }
                                },
                                label = { Text("Password / App Token") },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Slate400
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = cloudConfig.webDavRemotePath,
                            onValueChange = {
                                val updated = cloudConfig.copy(webDavRemotePath = it)
                                cloudConfig = updated
                                if (server != null && onSaveServer != null) {
                                    onSaveServer(server.copy(cloudBackupConfig = updated))
                                }
                            },
                            label = { Text("Remote Folder Path") },
                            placeholder = { Text("/MineServe/Backups", color = Slate400) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Test Connection Button
                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                scope.launch {
                                    val result = cloudManager.testWebDavConnection(cloudConfig)
                                    isTestingConnection = false
                                    result.fold(
                                        onSuccess = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, "Connection failed: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            enabled = !isTestingConnection && cloudConfig.webDavUrl.isNotBlank(),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing WebDAV Connection...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test WebDAV Connection", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // SAF Folder Picker
                        val safUri = cloudConfig.safFolderUri
                        val selectedFolderName = if (!safUri.isNullOrBlank()) {
                            try {
                                val doc = DocumentFile.fromTreeUri(context, Uri.parse(safUri))
                                doc?.name ?: "Connected Folder"
                            } catch (_: Exception) {
                                "Connected Storage Folder"
                            }
                        } else null

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedFolderName != null) "Folder: $selectedFolderName" else "No cloud/storage folder linked yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedFolderName != null) EmeraldLight else Slate400
                                )

                                Button(
                                    onClick = { safFolderPickerLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedFolderName != null) "Change Storage Folder" else "Pick Cloud / SD Folder",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Backups List
        if (backups.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No Backups Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Create a world backup snapshot above before updating plugins, modifying world configs, or making major changes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                }
            }
        } else {
            for (b in backups) {
                val isRestoringThis = restoringBackupId == b.id
                val isExportingThis = exportingBackupId == b.id
                val isDeletingThis = deletingBackupId == b.id
                val isSyncingThis = syncingBackupId == b.id
                val formattedDate = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(b.timestamp))

                Card(
                    shape = RoundedCornerShape(14.dp),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = b.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate800
                            ) {
                                Text(
                                    text = b.formattedSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Restore Button
                            OutlinedButton(
                                onClick = { backupToRestore = b },
                                enabled = !isRestoringThis && !isExportingThis && !isSyncingThis,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isRestoringThis) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restoring...", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                } else {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp), tint = GoldYellow)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.backups_restore),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Sync to Cloud Button
                            if (cloudConfig.enabled && onSyncBackupToCloud != null) {
                                OutlinedButton(
                                    onClick = {
                                        syncingBackupId = b.id
                                        onSyncBackupToCloud(b) { res ->
                                            syncingBackupId = null
                                            res.fold(
                                                onSuccess = { msg -> showFeedback(msg) },
                                                onFailure = { err -> showFeedback("Sync failed: ${err.message}", isError = true) }
                                            )
                                        }
                                    },
                                    enabled = !isRestoringThis && !isExportingThis && !isSyncingThis,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSyncingThis) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = EmeraldLight)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Syncing...", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmeraldLight)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cloud", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                    }
                                }
                            }

                            // Export Button
                            OutlinedButton(
                                onClick = {
                                    exportingBackupId = b.id
                                    onExportBackup(b) { path ->
                                        exportingBackupId = null
                                        if (path != null) {
                                            showFeedback("Saved to $path")
                                        } else {
                                            showFeedback("Export failed", isError = true)
                                        }
                                    }
                                },
                                enabled = !isRestoringThis && !isExportingThis && !isSyncingThis,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isExportingThis) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Exporting...", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmeraldLight)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.backups_export),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Share Button
                            OutlinedButton(
                                onClick = {
                                    val shareIntent = onGetShareIntent(b)
                                    if (shareIntent != null) {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                                    } else {
                                        showFeedback("Unable to open share sheet", isError = true)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(15.dp), tint = Color.White)
                            }

                            // Delete Button
                            OutlinedButton(
                                onClick = { backupToDelete = b },
                                enabled = !isRestoringThis && !isExportingThis && !isDeletingThis && !isSyncingThis,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                if (isDeletingThis) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        color = RedstoneRed,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.backups_delete),
                                        modifier = Modifier.size(15.dp),
                                        tint = RedstoneRed
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
