package com.devwithzachary.mineserve.ui.components.software

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Shield
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
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerBuildInfo
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

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
    AppAlertDialog(
        onDismissRequest = {
            if (!isUpdatingBuild) onDismiss()
        },
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
                text = stringResource(R.string.software_update_dialog_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${server.type.displayName} ${server.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                if (isCheckingBuild) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        Text(stringResource(R.string.software_checking_updates), color = Slate400, fontSize = 13.sp)
                    }
                } else if (buildInfo != null) {
                    val isSame = buildInfo.currentBuild != null && buildInfo.currentBuild == buildInfo.latestBuild

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSame) Slate800 else EmeraldDark.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (isSame) Slate700 else EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSame) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (isSame) Slate400 else EmeraldLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (isSame) {
                                        stringResource(R.string.software_up_to_date, buildInfo.latestBuild)
                                    } else {
                                        stringResource(R.string.software_update_available, buildInfo.latestBuild)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                if (buildInfo.currentBuild != null) {
                                    Text(
                                        text = "Installed build: #${buildInfo.currentBuild}",
                                        color = Slate400,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // World preservation guarantee
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = DiamondCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.software_safe_notice),
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                // Running warning
                if (isRunning) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RedstoneRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RedstoneRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.software_running_warning),
                                color = RedstoneRed,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Backup checkbox
                if (!isUpdatingBuild) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = updateBackupChecked,
                            onCheckedChange = onUpdateBackupCheckedChange,
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Text(
                            text = stringResource(R.string.software_backup_checkbox),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                // Progress bar while updating
                if (isUpdatingBuild) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { updateProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = EmeraldPrimary,
                            trackColor = Slate800
                        )
                        Text(
                            text = updateStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldLight,
                            fontSize = 12.sp
                        )
                    }
                }

                if (updateErrorText != null) {
                    Text(
                        text = updateErrorText,
                        color = RedstoneRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmUpdate,
                enabled = !isCheckingBuild && !isUpdatingBuild,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.software_btn_confirm_update),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        dismissButton = {
            if (!isUpdatingBuild) {
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
