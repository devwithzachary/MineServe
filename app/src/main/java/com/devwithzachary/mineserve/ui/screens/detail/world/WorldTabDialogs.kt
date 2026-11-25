package com.devwithzachary.mineserve.ui.screens.detail.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.ChunkPruneResult
import com.devwithzachary.mineserve.model.DimensionType

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
    MineServeDialog(
        onDismissRequest = { if (!isResetting) onDismiss() },
        title = if (targetDim == DimensionType.NETHER) {
            stringResource(R.string.world_reset_nether_title)
        } else {
            stringResource(R.string.world_reset_end_title)
        },
        confirmLabel = if (isResetting) "Resetting..." else stringResource(R.string.world_reset_confirm_btn),
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.cancel),
        icon = Icons.Rounded.Warning,
        isDestructive = true,
        confirmEnabled = !isResetting
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (targetDim == DimensionType.NETHER) {
                    stringResource(R.string.world_reset_nether_desc)
                } else {
                    stringResource(R.string.world_reset_end_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isRunning) {
                Surface(
                    shape = ShapeDefaults.Small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = stringResource(R.string.world_reset_running_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = resetBackupChecked,
                    onCheckedChange = onResetBackupCheckedChange
                )
                Text(
                    text = stringResource(R.string.world_reset_backup_checkbox),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
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
    val monoFont = LocalMineServeMonoFontFamily.current

    MineServeDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = stringResource(R.string.world_import_title),
        confirmLabel = if (isImporting) "Importing..." else "Import World",
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.cancel),
        icon = Icons.Rounded.CloudDownload,
        confirmEnabled = !isImporting
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.world_import_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isRunning) {
                Surface(
                    shape = ShapeDefaults.Small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = stringResource(R.string.world_import_running_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = importBackupChecked,
                    onCheckedChange = onImportBackupCheckedChange
                )
                Text(
                    text = stringResource(R.string.world_import_backup_checkbox),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isImporting) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { importProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Text(
                        text = importStatusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = monoFont,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (importErrorMessage != null) {
                Text(
                    text = importErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
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
    val monoFont = LocalMineServeMonoFontFamily.current

    MineServeDialog(
        onDismissRequest = { if (!isPruning) onDismiss() },
        title = stringResource(R.string.world_pruner_confirm_title),
        confirmLabel = if (isPruning) "Optimizing..." else "Start Optimization",
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.cancel),
        icon = Icons.Rounded.CleaningServices,
        confirmEnabled = !isPruning
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.world_pruner_confirm_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isRunning) {
                Surface(
                    shape = ShapeDefaults.Small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = stringResource(R.string.world_pruner_running_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (isPruning) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { pruneProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Text(
                        text = pruneStatusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = monoFont,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun ChunkPruneResultDialog(
    result: ChunkPruneResult,
    onDismiss: () -> Unit
) {
    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.world_pruner_success_title),
        confirmLabel = "Done",
        onConfirm = onDismiss,
        dismissLabel = "",
        icon = Icons.Rounded.CheckCircle
    ) {
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
