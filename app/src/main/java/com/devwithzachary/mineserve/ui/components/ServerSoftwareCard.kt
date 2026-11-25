package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerBuildInfo
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.ui.components.software.UpdateBuildModal
import com.devwithzachary.mineserve.ui.components.software.UpgradeVersionModal
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerSoftwareCard(
    server: MinecraftServer,
    status: ServerStatus,
    onCheckForUpdate: suspend (MinecraftServer) -> ServerBuildInfo?,
    onFetchVersions: suspend (ServerType) -> List<String>,
    onUpdateBuild: (createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit,
    onUpgradeVersion: (newVersion: String, createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit,
    onUpdateJavaVersion: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    val scope = rememberCoroutineScope()

    var showUpdateModal by remember { mutableStateOf(false) }
    var showUpgradeModal by remember { mutableStateOf(false) }

    var isCheckingBuild by remember { mutableStateOf(false) }
    var buildInfo by remember { mutableStateOf<ServerBuildInfo?>(null) }
    var updateBackupChecked by remember { mutableStateOf(true) }
    var isUpdatingBuild by remember { mutableStateOf(false) }
    var updateProgressPercent by remember { mutableIntStateOf(0) }
    var updateStatusText by remember { mutableStateOf("") }
    var updateErrorText by remember { mutableStateOf<String?>(null) }

    var isLoadingVersions by remember { mutableStateOf(false) }
    var availableVersions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTargetVersion by remember { mutableStateOf(server.version) }
    var upgradeBackupChecked by remember { mutableStateOf(true) }
    var isUpgradingVersion by remember { mutableStateOf(false) }
    var upgradeProgressPercent by remember { mutableIntStateOf(0) }
    var upgradeStatusText by remember { mutableStateOf("") }
    var upgradeErrorText by remember { mutableStateOf<String?>(null) }

    var showJavaVersionDialog by remember { mutableStateOf(false) }
    val isRunning = status == ServerStatus.RUNNING || status == ServerStatus.STARTING

    MineServeCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeDefaults.Large,
        containerColor = colorScheme.surfaceContainer,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdateAlt,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.software_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(R.string.software_card_desc),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = ShapeDefaults.Small,
                    color = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    border = null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(13.dp))
                        Text(
                            text = server.type.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Surface(
                    shape = ShapeDefaults.Small,
                    color = colorScheme.surfaceContainerHigh,
                    contentColor = colorScheme.onSurface,
                    border = null
                ) {
                    Text(
                        text = stringResource(R.string.software_mc_version, server.version),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = ShapeDefaults.Small,
                    color = colorScheme.surfaceContainerHigh,
                    contentColor = if (server.serverBuild != null) colorScheme.primary else colorScheme.onSurfaceVariant,
                    border = null
                ) {
                    val buildLabel = server.serverBuild?.let { stringResource(R.string.software_build_chip, it) }
                        ?: stringResource(R.string.software_build_unknown)
                    Text(
                        text = buildLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (!server.type.isBedrockNative) {
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = colorScheme.tertiaryContainer,
                        contentColor = colorScheme.onTertiaryContainer,
                        border = null,
                        modifier = Modifier.clickable(enabled = onUpdateJavaVersion != null && !isRunning) {
                            showJavaVersionDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Memory, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text(
                                text = stringResource(R.string.software_java_chip, server.javaVersion),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont)
                            )
                            if (onUpdateJavaVersion != null && !isRunning) {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            if (server.type != ServerType.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
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
                        shape = ShapeDefaults.Medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.software_btn_update_build),
                            maxLines = 1
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
                                    selectedTargetVersion = list.firstOrNull { it != server.version } ?: list.first()
                                }
                                isLoadingVersions = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Upgrade,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.software_btn_upgrade_version),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showUpdateModal) {
        UpdateBuildModal(
            server = server,
            isRunning = isRunning,
            isCheckingBuild = isCheckingBuild,
            buildInfo = buildInfo,
            isUpdatingBuild = isUpdatingBuild,
            updateProgressPercent = updateProgressPercent,
            updateStatusText = updateStatusText,
            updateErrorText = updateErrorText,
            updateBackupChecked = updateBackupChecked,
            onUpdateBackupCheckedChange = { updateBackupChecked = it },
            onDismiss = { showUpdateModal = false },
            onConfirmUpdate = {
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
                            updateErrorText = context.getString(R.string.software_update_error)
                        }
                    }
                )
            }
        )
    }

    if (showUpgradeModal) {
        UpgradeVersionModal(
            server = server,
            isRunning = isRunning,
            isLoadingVersions = isLoadingVersions,
            availableVersions = availableVersions,
            selectedTargetVersion = selectedTargetVersion,
            onSelectTargetVersion = { selectedTargetVersion = it },
            isUpgradingVersion = isUpgradingVersion,
            upgradeProgressPercent = upgradeProgressPercent,
            upgradeStatusText = upgradeStatusText,
            upgradeErrorText = upgradeErrorText,
            upgradeBackupChecked = upgradeBackupChecked,
            onUpgradeBackupCheckedChange = { upgradeBackupChecked = it },
            onDismiss = { showUpgradeModal = false },
            onConfirmUpgrade = {
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
                            upgradeErrorText = context.getString(R.string.software_upgrade_error)
                        }
                    }
                )
            }
        )
    }

    if (showJavaVersionDialog && onUpdateJavaVersion != null) {
        val supportedJavaVersions = listOf(8, 17, 21, 25)
        MineServeDialog(
            onDismissRequest = { showJavaVersionDialog = false },
            title = "Select Java Runtime",
            confirmLabel = "Done",
            onConfirm = { showJavaVersionDialog = false },
            dismissLabel = null,
            icon = Icons.Rounded.Memory
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Choose the OpenJDK version used to launch ${server.name}. The selected runtime will be auto-installed if not already present.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                supportedJavaVersions.forEach { jVer ->
                    val isCurrent = server.javaVersion == jVer
                    Surface(
                        onClick = {
                            onUpdateJavaVersion(jVer)
                            showJavaVersionDialog = false
                        },
                        shape = ShapeDefaults.Small,
                        color = if (isCurrent) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                        contentColor = if (isCurrent) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Java $jVer (OpenJDK $jVer)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (isCurrent) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
