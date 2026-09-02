package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupsTab(
    backups: List<BackupEntry>,
    onCreateBackup: ((Boolean) -> Unit) -> Unit = {},
    onRestoreBackup: (BackupEntry, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onExportBackup: (BackupEntry, (String?) -> Unit) -> Unit = { _, _ -> },
    onGetShareIntent: (BackupEntry) -> Intent? = { null },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isCreatingBackup by remember { mutableStateOf(false) }
    var restoringBackupId by remember { mutableStateOf<String?>(null) }
    var exportingBackupId by remember { mutableStateOf<String?>(null) }
    var backupToRestore by remember { mutableStateOf<BackupEntry?>(null) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

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
        AlertDialog(
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.backups_title),
                style = MaterialTheme.typography.titleLarge,
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
                                enabled = !isRestoringThis && !isExportingThis,
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
                                enabled = !isRestoringThis && !isExportingThis,
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
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(15.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
