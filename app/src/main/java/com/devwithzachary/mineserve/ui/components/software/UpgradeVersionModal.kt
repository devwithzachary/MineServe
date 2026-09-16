package com.devwithzachary.mineserve.ui.components.software

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeVersionModal(
    server: MinecraftServer,
    isRunning: Boolean,
    isLoadingVersions: Boolean,
    availableVersions: List<String>,
    selectedTargetVersion: String,
    onSelectTargetVersion: (String) -> Unit,
    isUpgradingVersion: Boolean,
    upgradeProgressPercent: Int,
    upgradeStatusText: String,
    upgradeErrorText: String?,
    upgradeBackupChecked: Boolean,
    onUpgradeBackupCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirmUpgrade: () -> Unit
) {
    var versionDropdownExpanded by remember { mutableStateOf(false) }
    val targetJava = remember(selectedTargetVersion) {
        determineJavaVersion(selectedTargetVersion, server.type)
    }

    AlertDialog(
        onDismissRequest = {
            if (!isUpgradingVersion) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Upgrade,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.software_upgrade_dialog_title),
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
                    text = "Current: ${server.type.displayName} ${server.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )

                if (isLoadingVersions) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        Text(stringResource(R.string.software_loading_versions), color = Slate400, fontSize = 13.sp)
                    }
                } else if (availableVersions.isNotEmpty()) {
                    // Dropdown selection for Minecraft Version
                    ExposedDropdownMenuBox(
                        expanded = versionDropdownExpanded,
                        onExpandedChange = { if (!isUpgradingVersion) versionDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTargetVersion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.software_select_version_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = versionDropdownExpanded,
                            onDismissRequest = { versionDropdownExpanded = false },
                            modifier = Modifier.background(Slate900)
                        ) {
                            availableVersions.forEach { ver ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(ver, color = if (ver == selectedTargetVersion) EmeraldPrimary else Color.White)
                                            if (ver == server.version) {
                                                Text("(Current)", color = Slate400, fontSize = 11.sp)
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectTargetVersion(ver)
                                        versionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Java version requirement chip
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate800,
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = DiamondLight, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Target requires Java $targetJava (Configured automatically)",
                                color = DiamondLight,
                                fontSize = 11.sp
                            )
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
                if (!isUpgradingVersion) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = upgradeBackupChecked,
                            onCheckedChange = onUpgradeBackupCheckedChange,
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

                // Progress bar while upgrading
                if (isUpgradingVersion) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { upgradeProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = EmeraldPrimary,
                            trackColor = Slate800
                        )
                        Text(
                            text = upgradeStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldLight,
                            fontSize = 12.sp
                        )
                    }
                }

                if (upgradeErrorText != null) {
                    Text(
                        text = upgradeErrorText,
                        color = RedstoneRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmUpgrade,
                enabled = !isLoadingVersions && !isUpgradingVersion && selectedTargetVersion.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.software_btn_confirm_upgrade),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        dismissButton = {
            if (!isUpgradingVersion) {
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
