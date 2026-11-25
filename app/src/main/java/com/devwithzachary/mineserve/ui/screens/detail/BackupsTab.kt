package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.backup.cloud.CloudBackupConfig
import com.devwithzachary.mineserve.backup.cloud.CloudBackupManager
import com.devwithzachary.mineserve.backup.cloud.CloudProviderType
import com.devwithzachary.mineserve.cloud.CloudAccountState
import com.devwithzachary.mineserve.cloud.CloudBackupItem
import com.devwithzachary.mineserve.cloud.CloudPricing
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
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackupsTab(
    server: MinecraftServer? = null,
    backups: List<BackupEntry>,
    onCreateBackup: ((Boolean) -> Unit) -> Unit = {},
    onRestoreBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onExportBackup: (BackupEntry, (String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onGetShareIntent: (BackupEntry) -> Intent? = { null },
    onRefreshBackups: () -> Unit = {},
    onSaveServer: ((MinecraftServer) -> Unit)? = null,
    onSyncBackupToCloud: ((BackupEntry, (Result<String>) -> Unit) -> Unit)? = null,
    cloudAccountState: CloudAccountState = CloudAccountState(),
    cloudBackups: List<CloudBackupItem> = emptyList(),
    isCloudBackupsLoading: Boolean = false,
    onRefreshCloudBackups: () -> Unit = {},
    onRestoreCloudBackup: (CloudBackupItem, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onDeleteCloudBackup: (CloudBackupItem, (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onOpenCloudPortal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    LaunchedEffect(server?.id) {
        onRefreshBackups()
    }

    var isCreatingBackup by remember { mutableStateOf(false) }
    var restoringBackupId by remember { mutableStateOf<String?>(null) }
    var exportingBackupId by remember { mutableStateOf<String?>(null) }
    var deletingBackupId by remember { mutableStateOf<String?>(null) }
    var syncingBackupId by remember { mutableStateOf<String?>(null) }

    var backupToRestore by remember { mutableStateOf<BackupEntry?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupEntry?>(null) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    val cloudManager = remember(isInspection) {
        if (isInspection) null else CloudBackupManager.getInstance(context)
    }
    val cloudSyncState by (cloudManager?.syncState?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(com.devwithzachary.mineserve.backup.cloud.CloudSyncState.Idle) })

    var cloudConfig by remember(server?.cloudBackupConfig) {
        mutableStateOf(server?.cloudBackupConfig ?: CloudBackupConfig())
    }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var restoringCloudBackupId by remember { mutableStateOf<String?>(null) }
    var deletingCloudBackupId by remember { mutableStateOf<String?>(null) }
    var cloudBackupToRestore by remember { mutableStateOf<CloudBackupItem?>(null) }
    var cloudBackupToDelete by remember { mutableStateOf<CloudBackupItem?>(null) }

    LaunchedEffect(server?.id, cloudAccountState.isLinked, cloudConfig.provider) {
        if (cloudAccountState.isLinked && cloudConfig.provider == CloudProviderType.MINESERVE_CLOUD) {
            onRefreshCloudBackups()
        }
    }

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

    if (backupToRestore != null) {
        val target = backupToRestore!!
        MineServeDialog(
            onDismissRequest = { backupToRestore = null },
            title = "Restore World Backup?",
            confirmLabel = "Restore World",
            onConfirm = {
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
            dismissLabel = stringResource(R.string.cancel),
            icon = Icons.Rounded.Warning,
            isDestructive = true
        ) {
            Text(
                text = "Restoring '${target.name}' will overwrite your current world with this snapshot. Any unsaved changes or blocks placed since this backup was created will be replaced.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (backupToDelete != null) {
        val target = backupToDelete!!
        MineServeDialog(
            onDismissRequest = { backupToDelete = null },
            title = stringResource(R.string.backups_delete_dialog_title),
            confirmLabel = stringResource(R.string.backups_delete),
            onConfirm = {
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
            dismissLabel = stringResource(R.string.cancel),
            icon = Icons.Rounded.Delete,
            isDestructive = true
        ) {
            Text(
                text = stringResource(R.string.backups_delete_dialog_desc, target.name, target.formattedSize),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                color = MaterialTheme.colorScheme.onSurface
            )
            MineServeTableMonoText(
                text = "${backups.size} snapshot(s)",
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = ShapeDefaults.Medium,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCreatingBackup) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zipping World...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.backups_create_world_btn), fontWeight = FontWeight.Bold)
            }
        }

        // Status / Feedback Banner
        AnimatedVisibility(
            visible = statusMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = ShapeDefaults.Small,
                color = if (isStatusError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isStatusError) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isStatusError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isStatusError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        }

        // Section: Cloud Backup Sync
        MineServeSettingsGroup(title = "Cloud Backup Sync") {
            MineServePreferenceRow(
                title = "Enable Cloud Backup Sync",
                subtitle = "Sync world backups off-device using Nextcloud, ownCloud, WebDAV, or an Android storage folder.",
                checked = cloudConfig.enabled,
                onCheckedChange = {
                    val updated = cloudConfig.copy(enabled = it)
                    cloudConfig = updated
                    if (server != null && onSaveServer != null) {
                        onSaveServer(server.copy(cloudBackupConfig = updated))
                    }
                },
                icon = Icons.Rounded.CloudSync
            )

            if (cloudConfig.enabled) {
                MineServePreferenceRow(
                    title = "Auto-Sync on Backup Creation",
                    subtitle = "Upload automatically when a new world snapshot is zipped",
                    checked = cloudConfig.autoSyncOnBackup,
                    onCheckedChange = {
                        val updated = cloudConfig.copy(autoSyncOnBackup = it)
                        cloudConfig = updated
                        if (server != null && onSaveServer != null) {
                            onSaveServer(server.copy(cloudBackupConfig = updated))
                        }
                    }
                )

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Storage Provider",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            CloudProviderType.MINESERVE_CLOUD to "MineServe Cloud",
                            CloudProviderType.WEBDAV to "Nextcloud / WebDAV",
                            CloudProviderType.SAF_FOLDER to "Android Cloud / SAF Folder"
                        ).forEach { (provider, label) ->
                            FilterChip(
                                selected = cloudConfig.provider == provider,
                                onClick = {
                                    val updated = cloudConfig.copy(provider = provider)
                                    cloudConfig = updated
                                    if (server != null && onSaveServer != null) {
                                        onSaveServer(server.copy(cloudBackupConfig = updated))
                                    }
                                },
                                label = { Text(label) },
                                border = null,
                                colors = chipColors
                            )
                        }
                    }

                    if (cloudConfig.provider == CloudProviderType.MINESERVE_CLOUD) {
                        val profile = cloudAccountState.profile
                        Surface(
                            shape = ShapeDefaults.Medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!cloudAccountState.isLinked) {
                                    Text(
                                        text = "MineServe Cloud Not Linked",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = extColors.warning
                                    )
                                    Text(
                                        text = "Sign in to MineServe Cloud in Settings to sync world backups to encrypted Cloudflare R2 storage (10 GB on Plus, 50 GB on Pro).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FilledTonalButton(
                                        onClick = onOpenCloudPortal,
                                        shape = ShapeDefaults.Small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (BuildConfig.IS_PLAY_STORE) "Sign In to MineServe Cloud" else "Open MineServe Cloud Portal"
                                        )
                                    }
                                } else {
                                    val tier = profile?.tier?.uppercase() ?: "FREE"
                                    val usedMb = (profile?.storageUsedBytes ?: 0L) / (1024.0 * 1024.0)
                                    val quotaGb = (profile?.storageQuotaBytes ?: 0L) / (1024L * 1024L * 1024L)
                                    Text(
                                        text = "${profile?.email ?: "Linked Account"} ($tier)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (tier == "FREE") {
                                        if (BuildConfig.IS_PLAY_STORE) {
                                            Text(
                                                text = "Encrypted Cloud Backups require an active MineServe Plus or Pro membership. Plan changes and purchases are not available in this app.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extColors.warning
                                            )
                                        } else {
                                            val pricing = remember { CloudPricing.current() }
                                            Text(
                                                text = "Encrypted Cloud Backups require MineServe Plus (7-day free trial, then ${pricing.plusMonthly}/mo, 10 GB) or Pro (${pricing.proMonthly}/mo, 50 GB).",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = extColors.warning
                                            )
                                            FilledTonalButton(
                                                onClick = onOpenCloudPortal,
                                                shape = ShapeDefaults.Small,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Start 7-Day Free Trial in Cloud Portal")
                                            }
                                        }
                                    } else {
                                        MineServeTableMonoText(
                                            text = String.format(
                                                Locale.US,
                                                "Cloud Storage: %.1f MB / %d GB used",
                                                usedMb,
                                                quotaGb
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else if (cloudConfig.provider == CloudProviderType.WEBDAV) {
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
                            placeholder = { Text("https://cloud.example.com/remote.php/dav/files/user/", fontFamily = monoFont) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            singleLine = true,
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
                                singleLine = true,
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
                                label = { Text("Password / Token") },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
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
                            placeholder = { Text("/MineServe/Backups", fontFamily = monoFont) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        FilledTonalButton(
                            onClick = {
                                isTestingConnection = true
                                scope.launch {
                                    val result = cloudManager?.testWebDavConnection(cloudConfig)
                                        ?: Result.failure(IllegalStateException("Unavailable in preview"))
                                    isTestingConnection = false
                                    result.fold(
                                        onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                                        onFailure = { err -> Toast.makeText(context, "Connection failed: ${err.message}", Toast.LENGTH_LONG).show() }
                                    )
                                }
                            },
                            enabled = !isTestingConnection && cloudConfig.webDavUrl.isNotBlank(),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing WebDAV Connection...")
                            } else {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test WebDAV Connection")
                            }
                        }
                    } else {
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
                            shape = ShapeDefaults.Medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                    color = if (selectedFolderName != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                FilledTonalButton(
                                    onClick = { safFolderPickerLauncher.launch(null) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = ShapeDefaults.Small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedFolderName != null) "Change Storage Folder" else "Pick Cloud / SD Folder",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Backups List
        if (backups.isEmpty()) {
            MineServeEmptyState(
                icon = Icons.Rounded.Backup,
                title = "No Backups Yet",
                supportingText = "Create a world backup snapshot above before updating plugins, modifying world configs, or making major changes."
            )
        } else {
            val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault()) }
            MineServeTableContainer {
                backups.forEachIndexed { index, b ->
                    val isRestoringThis = restoringBackupId == b.id
                    val isExportingThis = exportingBackupId == b.id
                    val isDeletingThis = deletingBackupId == b.id
                    val isSyncingThis = syncingBackupId == b.id
                    val formattedDate = dateFormat.format(Date(b.timestamp))

                    MineServeTableRow(showDivider = index < backups.lastIndex) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = b.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    MineServeTableMonoText(
                                        text = formattedDate,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = ShapeDefaults.Small,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = b.formattedSize,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { backupToRestore = b },
                                    enabled = !isRestoringThis && !isExportingThis && !isSyncingThis,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = extColors.warningContainer,
                                        contentColor = extColors.onWarningContainer
                                    ),
                                    shape = ShapeDefaults.Small,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isRestoringThis) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.backups_restore),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (cloudConfig.enabled && onSyncBackupToCloud != null) {
                                    FilledTonalButton(
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
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        shape = ShapeDefaults.Small,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isSyncingThis) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cloud", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                        }
                                    }
                                }

                                FilledTonalButton(
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
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = ShapeDefaults.Small,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isExportingThis) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.backups_export),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val shareIntent = onGetShareIntent(b)
                                        if (shareIntent != null) {
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                                        } else {
                                            showFeedback("Unable to open share sheet", isError = true)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { backupToDelete = b },
                                    enabled = !isRestoringThis && !isExportingThis && !isDeletingThis && !isSyncingThis
                                ) {
                                    if (isDeletingThis) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.error,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = stringResource(R.string.backups_delete),
                                            tint = MaterialTheme.colorScheme.error,
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

        if (cloudAccountState.isLinked && cloudConfig.provider == CloudProviderType.MINESERVE_CLOUD) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MineServe Cloud Snapshots",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCloudBackupsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }

            if (cloudBackups.isEmpty() && !isCloudBackupsLoading) {
                MineServeCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No cloud snapshots stored for this server yet. Tap Cloud on any local backup above to upload it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (cloudBackups.isNotEmpty()) {
                val cloudDateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
                MineServeTableContainer {
                    cloudBackups.forEachIndexed { index, item ->
                        val isRestoringItem = restoringCloudBackupId == item.backupId
                        val isDeletingItem = deletingCloudBackupId == item.backupId
                        val sizeMb = item.sizeBytes / (1024.0 * 1024.0)

                        MineServeTableRow(showDivider = index < cloudBackups.lastIndex) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = monoFont,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                MineServeTableMonoText(
                                    text = String.format(
                                        Locale.US,
                                        "%.1f MB - %s",
                                        sizeMb,
                                        cloudDateFormat.format(Date(item.createdAtEpochMs))
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = { cloudBackupToRestore = item },
                                    enabled = !isRestoringItem && !isDeletingItem,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = ShapeDefaults.Small,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (isRestoringItem) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Restore", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                IconButton(
                                    onClick = { cloudBackupToDelete = item },
                                    enabled = !isRestoringItem && !isDeletingItem
                                ) {
                                    if (isDeletingItem) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = MaterialTheme.colorScheme.error,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete Cloud Snapshot",
                                            tint = MaterialTheme.colorScheme.error,
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
    }

    cloudBackupToRestore?.let { item ->
        MineServeDialog(
            onDismissRequest = { cloudBackupToRestore = null },
            title = "Restore Cloud Snapshot?",
            confirmLabel = "Restore",
            onConfirm = {
                val target = item
                cloudBackupToRestore = null
                restoringCloudBackupId = target.backupId
                onRestoreCloudBackup(target) { res ->
                    restoringCloudBackupId = null
                    res.fold(
                        onSuccess = { showFeedback("Restored ${target.fileName} from MineServe Cloud") },
                        onFailure = { err -> showFeedback(err.message ?: "Cloud restore failed", isError = true) }
                    )
                }
            },
            isDestructive = true
        ) {
            Text(
                text = "Download ${item.fileName} from MineServe Cloud and restore it over your current server files?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    cloudBackupToDelete?.let { item ->
        MineServeDialog(
            onDismissRequest = { cloudBackupToDelete = null },
            title = "Delete Cloud Snapshot?",
            confirmLabel = "Delete",
            onConfirm = {
                val target = item
                cloudBackupToDelete = null
                deletingCloudBackupId = target.backupId
                onDeleteCloudBackup(target) { res ->
                    deletingCloudBackupId = null
                    res.fold(
                        onSuccess = { showFeedback("Deleted ${target.fileName} from MineServe Cloud") },
                        onFailure = { err -> showFeedback(err.message ?: "Failed to delete cloud snapshot", isError = true) }
                    )
                }
            },
            isDestructive = true
        ) {
            Text(
                text = "Permanently delete ${item.fileName} from MineServe Cloud storage? Local backups will remain untouched.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeTabPreviews
@Composable
private fun BackupsTabPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        BackupsTab(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            backups = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleBackups,
            cloudAccountState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleCloudLinkedState,
            cloudBackups = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleCloudBackups
        )
    }
}
