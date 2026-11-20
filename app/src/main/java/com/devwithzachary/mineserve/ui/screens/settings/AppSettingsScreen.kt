package com.devwithzachary.mineserve.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.system.SafeguardPreferences
import com.devwithzachary.mineserve.ui.components.NotificationPermissionCard
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    storageUsedMb: Long,
    isJavaVersionInstalled: (Int) -> Boolean,
    onInstallJava: (Int) -> Unit,
    onReinstallRuntime: () -> Unit,
    onBack: () -> Unit,
    installingJavaVer: Int? = null,
    javaInstallError: String? = null,
    isCheckGitHubUpdatesEnabled: Boolean = false,
    onToggleCheckGitHubUpdates: (Boolean) -> Unit = {},
    isCheckingUpdate: Boolean = false,
    updateCheckStatusMessage: String? = null,
    onCheckForUpdatesNow: () -> Unit = {},
    onOpenGitHubReleases: () -> Unit = {},
    webAdminEnabled: Boolean = false,
    webAdminLocalUrl: String? = null,
    onRemoteAdminClick: (() -> Unit)? = null,
    cloudState: com.devwithzachary.mineserve.cloud.CloudAccountState = com.devwithzachary.mineserve.cloud.CloudAccountState(),
    onOpenCloudPortal: () -> Unit = {},
    onRefreshCloud: () -> Unit = {},
    onSignOutCloud: () -> Unit = {},
    onOpenCloudPage: (() -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
    isShowWhatsNewOnUpdateEnabled: Boolean = true,
    onToggleShowWhatsNewOnUpdate: (Boolean) -> Unit = {},
    onOpenWhatsNew: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onAboutClick: (() -> Unit)? = null
) {
    var keepScreenAwake by remember { mutableStateOf(true) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
        ) {
            // MineServe Cloud Account Card (top of Settings)
            MineServeCloudCard(
                cloudState = cloudState,
                onOpenCloudPortal = onOpenCloudPortal,
                onRefreshCloud = onRefreshCloud,
                onSignOutCloud = onSignOutCloud,
                onOpenCloudPage = onOpenCloudPage
            )

            // Notification Permission Card
            NotificationPermissionCard()

            // Container Runtime Card
            ContainerRuntimeCard(
                storageUsedMb = storageUsedMb,
                onReinstallRuntime = onReinstallRuntime
            )

            // Java Environment Card
            JavaEnvironmentCard(
                isJavaVersionInstalled = isJavaVersionInstalled,
                installingJavaVer = installingJavaVer,
                javaInstallError = javaInstallError,
                onInstallJava = { ver ->
                    onInstallJava(ver)
                }
            )

            // Web Remote Admin Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Web Remote Admin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (webAdminEnabled) EmeraldDark.copy(alpha = 0.4f) else Slate800
                        ) {
                            Text(
                                text = if (webAdminEnabled) "Running" else "Stopped",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (webAdminEnabled) EmeraldLight else Slate400,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = if (webAdminEnabled && webAdminLocalUrl != null) {
                            "Web admin is active at $webAdminLocalUrl. Access console terminals, manage files, and view telemetry from your desktop browser."
                        } else {
                            "Access server console terminals, manage files, and view telemetry from a desktop browser or tablet on your network."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    Button(
                        onClick = { onRemoteAdminClick?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (webAdminEnabled) "Manage Remote Admin" else "Configure Web Remote Admin",
                            color = EmeraldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Behavior Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_behavior_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_keep_awake_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.settings_keep_awake_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Switch(
                                checked = keepScreenAwake,
                                onCheckedChange = { keepScreenAwake = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = EmeraldDark
                                )
                            )
                        }
                    }
                }
            }

            if (onOpenDiagnostics != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Diagnostics & Logcat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Capture redacted app/engine logs, run tunnel & DNS self-tests, and export a diagnostic bundle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                        OutlinedButton(
                            onClick = onOpenDiagnostics,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Diagnostics & Logcat", color = EmeraldLight, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Battery & Thermal Safeguards Card
            run {
                val context = LocalContext.current
                val safeguardPrefs = remember { SafeguardPreferences(context) }
                var safeguardConfig by remember { mutableStateOf(safeguardPrefs.load()) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Battery & Thermal Safeguards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Automatically stop running servers if battery runs low while unplugged or if device temperature gets too high.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Low Battery Auto-Stop",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Stop servers when unplugged at or below ${safeguardConfig.minBatteryPercent}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Switch(
                                checked = safeguardConfig.batteryAutoStopEnabled,
                                onCheckedChange = { enabled ->
                                    safeguardConfig = safeguardConfig.copy(batteryAutoStopEnabled = enabled)
                                    safeguardPrefs.save(safeguardConfig)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = Slate800,
                                    checkedBorderColor = EmeraldPrimary.copy(alpha = 0.6f)
                                )
                            )
                        }

                        if (safeguardConfig.batteryAutoStopEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(10, 15, 20).forEach { pct ->
                                    val selected = safeguardConfig.minBatteryPercent == pct
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            safeguardConfig = safeguardConfig.copy(minBatteryPercent = pct)
                                            safeguardPrefs.save(safeguardConfig)
                                        },
                                        label = { Text("$pct%") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldDark,
                                            selectedLabelColor = EmeraldLight
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = ObsidianCardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Thermal Auto-Stop",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Stop servers when battery reaches ${safeguardConfig.maxBatteryTempC}°C",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Switch(
                                checked = safeguardConfig.thermalAutoStopEnabled,
                                onCheckedChange = { enabled ->
                                    safeguardConfig = safeguardConfig.copy(thermalAutoStopEnabled = enabled)
                                    safeguardPrefs.save(safeguardConfig)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = Slate800,
                                    checkedBorderColor = EmeraldPrimary.copy(alpha = 0.6f)
                                )
                            )
                        }

                        if (safeguardConfig.thermalAutoStopEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(42, 45, 48).forEach { tempC ->
                                    val selected = safeguardConfig.maxBatteryTempC == tempC
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            safeguardConfig = safeguardConfig.copy(maxBatteryTempC = tempC)
                                            safeguardPrefs.save(safeguardConfig)
                                        },
                                        label = { Text("${tempC}°C") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldDark,
                                            selectedLabelColor = EmeraldLight
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // App Updates & Releases Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "App Updates & Releases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Show What's New After Updates",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Automatically open the What's New feature guide when launching a new major (1.x) update.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isShowWhatsNewOnUpdateEnabled,
                                onCheckedChange = onToggleShowWhatsNewOnUpdate,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = Slate800,
                                    checkedBorderColor = EmeraldPrimary.copy(alpha = 0.6f)
                                )
                            )
                        }

                        if (onOpenWhatsNew != null) {
                            OutlinedButton(
                                onClick = onOpenWhatsNew,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Review What's New in ${com.devwithzachary.mineserve.ui.screens.whatsnew.CURRENT_WHATS_NEW_RELEASE.displayVersion}",
                                    color = EmeraldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        HorizontalDivider(color = ObsidianCardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Check for GitHub Releases",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Receive notifications when new updates are published directly to GitHub.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isCheckGitHubUpdatesEnabled,
                                onCheckedChange = onToggleCheckGitHubUpdates,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = EmeraldDark
                                )
                            )
                        }

                        HorizontalDivider(color = ObsidianCardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Release Channel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                                Text(
                                    text = updateCheckStatusMessage ?: "Installed: v${com.devwithzachary.mineserve.BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (updateCheckStatusMessage != null) EmeraldLight else Color.White
                                )
                            }

                            Button(
                                onClick = onCheckForUpdatesNow,
                                enabled = !isCheckingUpdate,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isCheckingUpdate) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Checking...",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Check Now",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // About MineServe Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_about_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Version ${com.devwithzachary.mineserve.BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            if (onAboutClick != null) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = onAboutClick,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        contentColor = EmeraldLight
                                    ),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                                ) {
                                    Text("About & Changelog", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MineServeCloudCard(
    cloudState: com.devwithzachary.mineserve.cloud.CloudAccountState,
    onOpenCloudPortal: () -> Unit,
    onRefreshCloud: () -> Unit,
    onSignOutCloud: () -> Unit,
    onOpenCloudPage: (() -> Unit)? = null
) {
    androidx.compose.runtime.LaunchedEffect(cloudState.isLinked) {
        while (cloudState.isLinked) {
            onRefreshCloud()
            kotlinx.coroutines.delay(15_000L)
        }
    }

    val profile = cloudState.profile
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "MineServe Cloud",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (cloudState.isLinked && profile != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldDark.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = profile.tier,
                            color = EmeraldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (!cloudState.isLinked) {
                Text(
                    text = if (com.devwithzachary.mineserve.BuildConfig.IS_PLAY_STORE) {
                        "Sign in with an existing MineServe Cloud account to sync custom *.play.mineserve.app addresses, remote Web Admin access, and encrypted off-device backups."
                    } else {
                        "Optional cloud account for custom *.play.mineserve.app addresses, remote Web Admin access, and encrypted off-device backups."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                if (com.devwithzachary.mineserve.BuildConfig.IS_PLAY_STORE) {
                    Button(
                        onClick = { onOpenCloudPage?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (onOpenCloudPage != null) {
                            OutlinedButton(
                                onClick = onOpenCloudPage,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ObsidianCardBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Plans & Details", color = EmeraldLight)
                            }
                        }
                        Button(
                            onClick = onOpenCloudPortal,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Link Account", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (profile != null) {
                    Text(
                        text = profile.email,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    val usedMb = profile.storageUsedBytes / (1024.0 * 1024.0)
                    val quotaGb = profile.storageQuotaBytes / (1024L * 1024L * 1024L)
                    Text(
                        text = "Storage: ${String.format(java.util.Locale.US, "%.1f", usedMb)} MB / $quotaGb GB  |  Addresses: ${profile.activeSubdomains.size} / ${profile.maxSubdomains}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                } else {
                    Text(
                        text = "Account linked. Tap Refresh to sync profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }

                cloudState.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (com.devwithzachary.mineserve.BuildConfig.IS_PLAY_STORE) {
                        if (onOpenCloudPage != null) {
                            OutlinedButton(
                                onClick = onOpenCloudPage,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Account Details", color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenCloudPortal,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Portal", color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOutCloud,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sign Out", color = Slate400, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
