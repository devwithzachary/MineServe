package com.devwithzachary.mineserve.ui.components.software

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.model.determineJavaVersion
import java.io.File

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
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var versionDropdownExpanded by remember { mutableStateOf(false) }
    val hasLiveMap = remember(server.id) {
        val serverDir = File(context.filesDir, "servers/${server.id}")
        WebMapPluginType.detectInstalled(serverDir) != null
    }
    val targetJava = remember(selectedTargetVersion) {
        determineJavaVersion(selectedTargetVersion, server.type)
    }

    MineServeDialog(
        onDismissRequest = { if (!isUpgradingVersion) onDismiss() },
        title = stringResource(R.string.software_upgrade_dialog_title),
        confirmLabel = stringResource(R.string.software_btn_confirm_upgrade),
        onConfirm = onConfirmUpgrade,
        dismissLabel = if (!isUpgradingVersion) stringResource(R.string.cancel) else null,
        onDismiss = onDismiss,
        icon = Icons.Rounded.Upgrade,
        confirmEnabled = !isLoadingVersions && !isUpgradingVersion && selectedTargetVersion.isNotBlank()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.software_current_version, server.type.displayName, server.version),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )

            if (isLoadingVersions) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colorScheme.primary)
                    Text(
                        text = stringResource(R.string.software_loading_versions),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            } else if (availableVersions.isNotEmpty()) {
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
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        shape = ShapeDefaults.Medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant,
                            focusedContainerColor = colorScheme.surfaceContainer,
                            unfocusedContainerColor = colorScheme.surfaceContainer,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = versionDropdownExpanded,
                        onDismissRequest = { versionDropdownExpanded = false },
                        containerColor = colorScheme.surfaceContainerHighest
                    ) {
                        availableVersions.forEach { ver ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = ver,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                            color = if (ver == selectedTargetVersion) colorScheme.primary else colorScheme.onSurface
                                        )
                                        if (ver == server.version) {
                                            Text(
                                                text = stringResource(R.string.software_version_current_tag),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
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

                Surface(
                    shape = ShapeDefaults.Small,
                    color = colorScheme.tertiaryContainer,
                    contentColor = colorScheme.onTertiaryContainer,
                    border = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.software_target_java_notice, targetJava),
                            style = MaterialTheme.typography.bodySmall
                        )
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

            Surface(
                shape = ShapeDefaults.Small,
                color = extended.warningContainer,
                contentColor = extended.onWarningContainer,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = extended.warning, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (hasLiveMap) {
                            stringResource(R.string.software_upgrade_plugins_mods_live_map_notice, selectedTargetVersion)
                        } else {
                            stringResource(R.string.software_upgrade_plugins_mods_notice, selectedTargetVersion)
                        },
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

            if (!isUpgradingVersion) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = upgradeBackupChecked,
                        onCheckedChange = onUpgradeBackupCheckedChange,
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

            if (isUpgradingVersion) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { upgradeProgressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = upgradeStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary
                    )
                }
            }

            if (upgradeErrorText != null) {
                Text(
                    text = upgradeErrorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.error
                )
            }
        }
    }
}
