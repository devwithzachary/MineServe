package com.devwithzachary.mineserve.ui.screens.detail.world

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Warning
import com.devwithzachary.mineserve.ui.components.AppAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.ChunkPruneResult
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate950

@Composable
fun DimensionResetDialog(
    targetDim: DimensionType,
    isRunning: Boolean,
    isResetting: Boolean,
    resetBackupChecked: Boolean,
    onResetBackupCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = { if (!isResetting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = RedstoneRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (targetDim == DimensionType.NETHER) {
                    stringResource(R.string.world_reset_nether_title)
                } else {
                    stringResource(R.string.world_reset_end_title)
                },
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (targetDim == DimensionType.NETHER) {
                        stringResource(R.string.world_reset_nether_desc)
                    } else {
                        stringResource(R.string.world_reset_end_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                if (isRunning) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RedstoneRed.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.world_reset_running_warning),
                            color = RedstoneLight,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = resetBackupChecked,
                        onCheckedChange = onResetBackupCheckedChange,
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Text(
                        text = stringResource(R.string.world_reset_backup_checkbox),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isResetting,
                colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isResetting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(stringResource(R.string.world_reset_confirm_btn), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isResetting) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        },
        containerColor = Slate950
    )
}

@Composable
fun WorldImportDialog(
    isRunning: Boolean,
    isImporting: Boolean,
    importBackupChecked: Boolean,
    onImportBackupCheckedChange: (Boolean) -> Unit,
    importProgressPercent: Int,
    importStatusText: String,
    importErrorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.world_import_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.world_import_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                if (isRunning) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RedstoneRed.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.world_import_running_warning),
                            color = RedstoneLight,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = importBackupChecked,
                        onCheckedChange = onImportBackupCheckedChange,
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Text(
                        text = stringResource(R.string.world_import_backup_checkbox),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                if (isImporting) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { importProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = EmeraldPrimary,
                            trackColor = Slate800
                        )
                        Text(text = importStatusText, color = EmeraldLight, fontSize = 11.sp)
                    }
                }

                if (importErrorMessage != null) {
                    Text(
                        text = importErrorMessage,
                        color = RedstoneRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isImporting,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                } else {
                    Text("Import World", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isImporting) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        },
        containerColor = Slate950
    )
}

@Composable
fun ChunkPruneDialog(
    isRunning: Boolean,
    isPruning: Boolean,
    pruneProgressPercent: Int,
    pruneStatusText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = { if (!isPruning) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                tint = DiamondCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.world_pruner_confirm_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.world_pruner_confirm_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                if (isRunning) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RedstoneRed.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(R.string.world_pruner_running_warning),
                            color = RedstoneLight,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (isPruning) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { pruneProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = DiamondCyan,
                            trackColor = Slate800
                        )
                        Text(text = pruneStatusText, color = DiamondLight, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isPruning,
                colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isPruning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                } else {
                    Text("Start Optimization", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isPruning) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            }
        },
        containerColor = Slate950
    )
}

@Composable
fun ChunkPruneResultDialog(
    result: ChunkPruneResult,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.world_pruner_success_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.world_pruner_success_desc,
                        result.scannedRegions,
                        result.scannedChunks,
                        result.prunedChunks,
                        result.deletedRegions,
                        formatWorldFileSize(result.bytesFreed),
                        result.percentFreed
                    ),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Slate950
    )
}
