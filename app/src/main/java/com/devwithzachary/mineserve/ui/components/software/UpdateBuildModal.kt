package com.devwithzachary.mineserve.ui.components.software

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerBuildInfo

@Composable
fun UpdateBuildModal(
    server: MinecraftServer,
    isRunning: Boolean,
    isCheckingBuild: Boolean,
    buildInfo: ServerBuildInfo?,
    isUpdatingBuild: Boolean,
    updateProgressPercent: Int,
    updateStatusText: String,
    updateErrorText: String?,
    updateBackupChecked: Boolean,
    onUpdateBackupCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirmUpdate: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current

    MineServeDialog(
        onDismissRequest = { if (!isUpdatingBuild) onDismiss() },
        title = stringResource(R.string.software_update_dialog_title),
        confirmLabel = stringResource(R.string.software_btn_confirm_update),
        onConfirm = onConfirmUpdate,
        dismissLabel = if (!isUpdatingBuild) stringResource(R.string.cancel) else null,
        onDismiss = onDismiss,
        icon = Icons.Rounded.CloudDownload,
        confirmEnabled = !isCheckingBuild && !isUpdatingBuild
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${server.type.displayName} ${server.version}",
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface
            )

            if (isCheckingBuild) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colorScheme.primary)
                    Text(
                        text = stringResource(R.string.software_checking_updates),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            } else if (buildInfo != null) {
                val isSame = buildInfo.currentBuild != null && buildInfo.currentBuild == buildInfo.latestBuild
                Surface(
                    shape = ShapeDefaults.Small,
                    color = if (isSame) colorScheme.surfaceContainer else colorScheme.primaryContainer,
                    contentColor = if (isSame) colorScheme.onSurface else colorScheme.onPrimaryContainer,
                    border = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSame) Icons.Rounded.CheckCircle else Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (isSame) {
                                    stringResource(R.string.software_up_to_date, buildInfo.latestBuild)
                                } else {
                                    stringResource(R.string.software_update_available, buildInfo.latestBuild)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                            buildInfo.currentBuild?.let { currentBuild ->
                                Text(
                                    text = stringResource(R.string.software_installed_build, currentBuild),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = ShapeDefaults.Small,
                color = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurfaceVariant,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = colorScheme.tertiary, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.software_safe_notice),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (isRunning) {
                Surface(
                    shape = ShapeDefaults.Small,
                    color = colorScheme.errorContainer,
                    contentColor = colorScheme.onErrorContainer,
                    border = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.software_running_warning),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (!isUpdatingBuild) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = updateBackupChecked,
                        onCheckedChange = onUpdateBackupCheckedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colorScheme.primary,
                            checkmarkColor = colorScheme.onPrimary
                        )
                    )
                    Text(
                        text = stringResource(R.string.software_backup_checkbox),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface
                    )
                }
            }

            if (isUpdatingBuild) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { updateProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = updateStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary
                    )
                }
            }

            if (updateErrorText != null) {
                Text(
                    text = updateErrorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error
                )
            }
        }
    }
}
