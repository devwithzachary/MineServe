package com.devwithzachary.mineserve.ui.screens.settings

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.cloud.CloudAccountState
import com.devwithzachary.mineserve.designsystem.components.MineServePreferenceRow
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.designsystem.theme.ThemeMode
import com.devwithzachary.mineserve.system.SafeguardPreferences
import com.devwithzachary.mineserve.ui.components.NotificationPermissionCard
import com.devwithzachary.mineserve.ui.screens.whatsnew.CURRENT_WHATS_NEW_RELEASE
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    cloudState: CloudAccountState = CloudAccountState(),
    onOpenCloudPortal: () -> Unit = {},
    onRefreshCloud: () -> Unit = {},
    onSignOutCloud: () -> Unit = {},
    onOpenCloudPage: (() -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
    isShowWhatsNewOnUpdateEnabled: Boolean = true,
    onToggleShowWhatsNewOnUpdate: (Boolean) -> Unit = {},
    onOpenWhatsNew: (() -> Unit)? = null,
    themeMode: ThemeMode = ThemeMode.ObsidianDark,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    useDynamicColor: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    onAboutClick: (() -> Unit)? = null,
    onCreditsClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    val settingsContext = LocalContext.current
    val uiPrefs = remember(settingsContext) {
        settingsContext.getSharedPreferences("mineserve_ui_prefs", Context.MODE_PRIVATE)
    }
    var keepScreenAwake by remember { mutableStateOf(uiPrefs.getBoolean("keep_screen_awake", true)) }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface
                )
            )
        },
        containerColor = colorScheme.surface,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Appearance & Theme
            MineServeSettingsGroup(title = "Appearance & Theme") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Color Scheme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface
                        )
                    }

                    val themeOptions = listOf(
                        ThemeMode.ObsidianDark to "Dark",
                        ThemeMode.SlateLight to "Light",
                        ThemeMode.System to "System"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = themeOptions.size
                                )
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    MineServePreferenceRow(
                        title = "Use Wallpaper Dynamic Colors",
                        subtitle = "Adapt theme accents to your wallpaper on Android 12+ (MineServe Obsidian is default).",
                        checked = useDynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                }
            }

            // 2. MineServe Cloud Account
            MineServeCloudCard(
                cloudState = cloudState,
                onOpenCloudPortal = onOpenCloudPortal,
                onRefreshCloud = onRefreshCloud,
                onSignOutCloud = onSignOutCloud,
                onOpenCloudPage = onOpenCloudPage
            )

            // 3. Notifications
            NotificationPermissionCard()

            // 4. Container Runtime
            ContainerRuntimeCard(
                storageUsedMb = storageUsedMb,
                onReinstallRuntime = onReinstallRuntime
            )

            // 5. Java Environments
            JavaEnvironmentCard(
                isJavaVersionInstalled = isJavaVersionInstalled,
                installingJavaVer = installingJavaVer,
                javaInstallError = javaInstallError,
                onInstallJava = onInstallJava
            )

            // 6. Remote Admin & Diagnostics
            MineServeSettingsGroup(title = "Remote Access & Diagnostics") {
                MineServePreferenceRow(
                    title = "Web Remote Admin",
                    subtitle = if (webAdminEnabled && webAdminLocalUrl != null) {
                        "Active at $webAdminLocalUrl"
                    } else {
                        "Access console terminals, files, and telemetry from a browser"
                    },
                    icon = Icons.Rounded.Dns,
                    onClick = { onRemoteAdminClick?.invoke() },
                    trailingContent = {
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = if (webAdminEnabled) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = if (webAdminEnabled) "Running" else "Stopped",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (webAdminEnabled) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )

                if (onOpenDiagnostics != null) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    MineServePreferenceRow(
                        title = "Diagnostics & Logcat",
                        subtitle = "Capture redacted logs, run tunnel & DNS tests, and export a bundle",
                        icon = Icons.Rounded.BugReport,
                        onClick = onOpenDiagnostics
                    )
                }
            }

            // 7. Behavior & Safeguards
            val safeguardPrefs = remember { SafeguardPreferences(settingsContext) }
            var safeguardConfig by remember { mutableStateOf(safeguardPrefs.load()) }
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colorScheme.secondaryContainer,
                selectedLabelColor = colorScheme.onSecondaryContainer,
                containerColor = colorScheme.surfaceContainerHighest,
                labelColor = colorScheme.onSurface
            )

            MineServeSettingsGroup(title = stringResource(R.string.settings_behavior_section)) {
                MineServePreferenceRow(
                    title = stringResource(R.string.settings_keep_awake_title),
                    subtitle = stringResource(R.string.settings_keep_awake_desc),
                    checked = keepScreenAwake,
                    onCheckedChange = { enabled ->
                        keepScreenAwake = enabled
                        uiPrefs.edit().putBoolean("keep_screen_awake", enabled).apply()
                    },
                    icon = Icons.Rounded.ScreenLockPortrait
                )

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                MineServePreferenceRow(
                    title = "Low Battery Auto-Stop",
                    subtitle = "Stop servers when unplugged at or below ${safeguardConfig.minBatteryPercent}%",
                    checked = safeguardConfig.batteryAutoStopEnabled,
                    onCheckedChange = { enabled ->
                        safeguardConfig = safeguardConfig.copy(batteryAutoStopEnabled = enabled)
                        safeguardPrefs.save(safeguardConfig)
                    },
                    icon = Icons.Rounded.BatteryAlert
                )

                if (safeguardConfig.batteryAutoStopEnabled) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 20).forEach { pct ->
                            FilterChip(
                                selected = safeguardConfig.minBatteryPercent == pct,
                                onClick = {
                                    safeguardConfig = safeguardConfig.copy(minBatteryPercent = pct)
                                    safeguardPrefs.save(safeguardConfig)
                                },
                                label = { Text("$pct%", fontFamily = monoFont) },
                                border = null,
                                colors = chipColors
                            )
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                MineServePreferenceRow(
                    title = "Thermal Auto-Stop",
                    subtitle = "Stop servers when battery reaches ${safeguardConfig.maxBatteryTempC}°C",
                    checked = safeguardConfig.thermalAutoStopEnabled,
                    onCheckedChange = { enabled ->
                        safeguardConfig = safeguardConfig.copy(thermalAutoStopEnabled = enabled)
                        safeguardPrefs.save(safeguardConfig)
                    },
                    icon = Icons.Rounded.Thermostat
                )

                if (safeguardConfig.thermalAutoStopEnabled) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(42, 45, 48).forEach { tempC ->
                            FilterChip(
                                selected = safeguardConfig.maxBatteryTempC == tempC,
                                onClick = {
                                    safeguardConfig = safeguardConfig.copy(maxBatteryTempC = tempC)
                                    safeguardPrefs.save(safeguardConfig)
                                },
                                label = { Text("${tempC}°C", fontFamily = monoFont) },
                                border = null,
                                colors = chipColors
                            )
                        }
                    }
                }
            }

            // 8. App Updates & Releases
            MineServeSettingsGroup(title = "App Updates & Releases") {
                MineServePreferenceRow(
                    title = "Show What's New After Updates",
                    subtitle = "Open the What's New feature guide when launching a new major (1.x) update.",
                    checked = isShowWhatsNewOnUpdateEnabled,
                    onCheckedChange = onToggleShowWhatsNewOnUpdate,
                    icon = Icons.Rounded.AutoAwesome
                )

                if (onOpenWhatsNew != null) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    MineServePreferenceRow(
                        title = "Review What's New in ${CURRENT_WHATS_NEW_RELEASE.displayVersion}",
                        icon = Icons.Rounded.AutoAwesome,
                        onClick = onOpenWhatsNew
                    )
                }

                if (!BuildConfig.IS_PLAY_STORE) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    MineServePreferenceRow(
                        title = "Check for GitHub Releases",
                        subtitle = "Notify when new updates are published to GitHub.",
                        checked = isCheckGitHubUpdatesEnabled,
                        onCheckedChange = onToggleCheckGitHubUpdates,
                        icon = Icons.Rounded.Update
                    )

                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Release Channel",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = updateCheckStatusMessage ?: "Installed: v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = monoFont,
                                fontWeight = FontWeight.SemiBold,
                                color = if (updateCheckStatusMessage != null) colorScheme.primary else colorScheme.onSurface
                            )
                        }

                        FilledTonalButton(
                            onClick = onCheckForUpdatesNow,
                            enabled = !isCheckingUpdate,
                            shape = ShapeDefaults.Small,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.primaryContainer,
                                contentColor = colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checking...", style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Now", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 9. About MineServe
            MineServeSettingsGroup(title = stringResource(R.string.settings_about_section)) {
                if (onAboutClick != null) {
                    MineServePreferenceRow(
                        title = stringResource(R.string.app_name),
                        subtitle = "Version ${BuildConfig.VERSION_NAME} - About & Changelog",
                        icon = Icons.Rounded.Info,
                        onClick = onAboutClick
                    )
                }
                if (onCreditsClick != null) {
                    if (onAboutClick != null) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                    MineServePreferenceRow(
                        title = "Credits & Open-Source Licenses",
                        subtitle = "Third-party libraries, contributors, and GPL-3.0",
                        icon = Icons.Rounded.Favorite,
                        onClick = onCreditsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MineServeCloudCard(
    cloudState: CloudAccountState,
    onOpenCloudPortal: () -> Unit,
    onRefreshCloud: () -> Unit,
    onSignOutCloud: () -> Unit,
    onOpenCloudPage: (() -> Unit)? = null
) {
    LaunchedEffect(cloudState.isLinked) {
        while (cloudState.isLinked) {
            onRefreshCloud()
            delay(15_000L)
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val profile = cloudState.profile

    MineServeSettingsGroup(title = "MineServe Cloud") {
        Column(
            modifier = Modifier.padding(16.dp),
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
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (cloudState.isLinked && profile != null) profile.email else "Cloud Sync & Tunnels",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                }

                if (cloudState.isLinked && profile != null) {
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = profile.tier,
                            color = colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!cloudState.isLinked) {
                Text(
                    text = if (BuildConfig.IS_PLAY_STORE) {
                        "Sign in with an existing MineServe Cloud account to sync custom *.play.mineserve.app addresses, remote Web Admin access, and encrypted off-device backups."
                    } else {
                        "Optional cloud account for custom *.play.mineserve.app addresses, remote Web Admin access, and encrypted off-device backups."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                if (BuildConfig.IS_PLAY_STORE) {
                    FilledTonalButton(
                        onClick = { onOpenCloudPage?.invoke() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign In", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (onOpenCloudPage != null) {
                            FilledTonalButton(
                                onClick = onOpenCloudPage,
                                shape = ShapeDefaults.Small,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Plans & Details")
                            }
                        }
                        FilledTonalButton(
                            onClick = onOpenCloudPortal,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.primaryContainer,
                                contentColor = colorScheme.onPrimaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Link Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (profile != null) {
                    val usedMb = profile.storageUsedBytes / (1024.0 * 1024.0)
                    val quotaGb = profile.storageQuotaBytes / (1024L * 1024L * 1024L)
                    MineServeTableMonoText(
                        text = "Storage: ${String.format(Locale.US, "%.1f", usedMb)} MB / $quotaGb GB | Addresses: ${profile.activeSubdomains.size}/${profile.maxSubdomains}",
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Account linked. Tap Refresh to sync profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                cloudState.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (BuildConfig.IS_PLAY_STORE) {
                        if (onOpenCloudPage != null) {
                            FilledTonalButton(
                                onClick = onOpenCloudPage,
                                shape = ShapeDefaults.Small,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Account Details", fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onOpenCloudPortal,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.primaryContainer,
                                contentColor = colorScheme.onPrimaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Portal", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    FilledTonalButton(
                        onClick = onSignOutCloud,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.surfaceContainerHighest,
                            contentColor = colorScheme.onSurfaceVariant
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sign Out", maxLines = 1)
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun AppSettingsScreenPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        AppSettingsScreen(
            storageUsedMb = 2450L,
            isJavaVersionInstalled = { ver -> ver == 17 || ver == 21 },
            onInstallJava = {},
            onReinstallRuntime = {},
            onBack = {},
            webAdminEnabled = true,
            webAdminLocalUrl = "http://192.168.1.42:8080",
            onRemoteAdminClick = {},
            cloudState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleCloudLinkedState,
            onOpenCloudPage = {},
            onOpenDiagnostics = {},
            onOpenWhatsNew = {},
            onAboutClick = {},
            onCreditsClick = {}
        )
    }
}
