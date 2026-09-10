package com.devwithzachary.mineserve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.devwithzachary.mineserve.model.ServerBuildInfo
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ServerSoftwareCard(
    server: MinecraftServer,
    status: ServerStatus,
    onCheckForUpdate: suspend (MinecraftServer) -> ServerBuildInfo?,
    onFetchVersions: suspend (ServerType) -> List<String>,
    onUpdateBuild: (createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit,
    onUpgradeVersion: (newVersion: String, createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showUpdateModal by remember { mutableStateOf(false) }
    var showUpgradeModal by remember { mutableStateOf(false) }

    // Update Modal State
    var isCheckingBuild by remember { mutableStateOf(false) }
    var buildInfo by remember { mutableStateOf<ServerBuildInfo?>(null) }
    var updateBackupChecked by remember { mutableStateOf(true) }
    var isUpdatingBuild by remember { mutableStateOf(false) }
    var updateProgressPercent by remember { mutableIntStateOf(0) }
    var updateStatusText by remember { mutableStateOf("") }
    var updateErrorText by remember { mutableStateOf<String?>(null) }

    // Upgrade Modal State
    var isLoadingVersions by remember { mutableStateOf(false) }
    var availableVersions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTargetVersion by remember { mutableStateOf(server.version) }
    var upgradeBackupChecked by remember { mutableStateOf(true) }
    var isUpgradingVersion by remember { mutableStateOf(false) }
    var upgradeProgressPercent by remember { mutableIntStateOf(0) }
    var upgradeStatusText by remember { mutableStateOf("") }
    var upgradeErrorText by remember { mutableStateOf<String?>(null) }
    var versionDropdownExpanded by remember { mutableStateOf(false) }

    val isRunning = status == ServerStatus.RUNNING || status == ServerStatus.STARTING

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.SystemUpdateAlt,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.software_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = stringResource(R.string.software_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                fontSize = 12.sp
            )

            // Metadata Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Platform chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldDark.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(12.dp))
                        Text(server.type.displayName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Version chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800,
                    border = BorderStroke(1.dp, ObsidianCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("MC ${server.version}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Build chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800,
                    border = BorderStroke(1.dp, ObsidianCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val buildLabel = server.serverBuild?.let { "Build #$it" } ?: stringResource(R.string.software_build_unknown)
                        Text(buildLabel, color = if (server.serverBuild != null) EmeraldLight else Slate400, fontSize = 11.sp)
                    }
                }

                // Java chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800,
                    border = BorderStroke(1.dp, ObsidianCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = DiamondLight, modifier = Modifier.size(12.dp))
                        Text("Java ${server.javaVersion}", color = DiamondLight, fontSize = 11.sp)
                    }
                }
            }

            // Action Buttons
            if (server.type != ServerType.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showUpdateModal = true
                            isCheckingBuild = true
                            buildInfo = null
                            updateErrorText = null
                            scope.launch {
                                buildInfo = onCheckForUpdate(server)
                                isCheckingBuild = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.software_btn_update_build),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            showUpgradeModal = true
                            isLoadingVersions = true
                            upgradeErrorText = null
                            scope.launch {
                                val list = onFetchVersions(server.type)
                                availableVersions = list
                                if (list.isNotEmpty()) {
                                    val firstDifferent = list.firstOrNull { it != server.version } ?: list.first()
                                    selectedTargetVersion = firstDifferent
                                }
                                isLoadingVersions = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upgrade,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.software_btn_upgrade_version),
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // Modal 1: Update Server Build Dialog
    if (showUpdateModal) {
        AlertDialog(
            onDismissRequest = {
                if (!isUpdatingBuild) showUpdateModal = false
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
                        val info = buildInfo!!
                        val isSame = info.currentBuild != null && info.currentBuild == info.latestBuild

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
                                            stringResource(R.string.software_up_to_date, info.latestBuild)
                                        } else {
                                            stringResource(R.string.software_update_available, info.latestBuild)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    if (info.currentBuild != null) {
                                        Text(
                                            text = "Installed build: #${info.currentBuild}",
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
                                onCheckedChange = { updateBackupChecked = it },
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
                            text = updateErrorText ?: "",
                            color = RedstoneRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdatingBuild = true
                        updateErrorText = null
                        onUpdateBuild(
                            updateBackupChecked,
                            { statusStr, percent ->
                                updateStatusText = statusStr
                                updateProgressPercent = percent
                            },
                            { success ->
                                isUpdatingBuild = false
                                if (success) {
                                    showUpdateModal = false
                                } else {
                                    updateErrorText = "Failed to update build. Please check your connection."
                                }
                            }
                        )
                    },
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
                        onClick = { showUpdateModal = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            },
            containerColor = Slate950
        )
    }

    // Modal 2: Upgrade Minecraft Version Dialog
    if (showUpgradeModal) {
        val targetJava = remember(selectedTargetVersion) {
            determineJavaVersion(selectedTargetVersion, server.type)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isUpgradingVersion) showUpgradeModal = false
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
                                modifier = Modifier.menuAnchor().fillMaxWidth()
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
                                            selectedTargetVersion = ver
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
                                onCheckedChange = { upgradeBackupChecked = it },
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
                            text = upgradeErrorText ?: "",
                            color = RedstoneRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpgradingVersion = true
                        upgradeErrorText = null
                        onUpgradeVersion(
                            selectedTargetVersion,
                            upgradeBackupChecked,
                            { statusStr, percent ->
                                upgradeStatusText = statusStr
                                upgradeProgressPercent = percent
                            },
                            { success ->
                                isUpgradingVersion = false
                                if (success) {
                                    showUpgradeModal = false
                                } else {
                                    upgradeErrorText = "Failed to upgrade server version. Please check connection."
                                }
                            }
                        )
                    },
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
                        onClick = { showUpgradeModal = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            },
            containerColor = Slate950
        )
    }
}
