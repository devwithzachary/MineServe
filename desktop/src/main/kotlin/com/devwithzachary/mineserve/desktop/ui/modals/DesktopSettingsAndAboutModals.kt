package com.devwithzachary.mineserve.desktop.ui.modals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.api.GitHubUpdateChecker
import com.devwithzachary.mineserve.api.UpdateCheckResult
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServePreferenceRow
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.components.NetworkAddressChip
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.designsystem.theme.ThemeMode
import com.devwithzachary.mineserve.desktop.DesktopDiagnosticsManager
import com.devwithzachary.mineserve.desktop.DesktopHostTelemetry
import com.devwithzachary.mineserve.desktop.DesktopSelfTestItem
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.ChangelogPlatform
import com.devwithzachary.mineserve.model.MineServeAboutData
import com.devwithzachary.mineserve.model.MineServeWhatsNewCatalog
import com.devwithzachary.mineserve.model.SharedCreditCategory
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopAppSettingsModal(viewModel: DesktopViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var activeSection by remember { mutableIntStateOf(0) } // 0: Appearance & Web Admin, 1: Java Runtimes, 2: Diagnostics & Logs, 3: Updates
    val webRunning by viewModel.webAdminRunning.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val lanIp = remember { DesktopHostTelemetry.getLocalIpAddress() ?: "127.0.0.1" }
    val webPort = viewModel.webServer.actualPort

    var installingMajor by remember { mutableStateOf<Int?>(null) }
    var javaRefreshTick by remember { mutableIntStateOf(0) }

    var logFilterMode by remember { mutableStateOf("MINESERVE_TAGS") }
    var capturedLogs by remember {
        mutableStateOf(
            DesktopDiagnosticsManager.captureLogs("MINESERVE_TAGS", viewModel.serverRepository, viewModel.processManager)
        )
    }
    var selfTestResults by remember { mutableStateOf<List<DesktopSelfTestItem>>(emptyList()) }
    var selfTestBusy by remember { mutableStateOf(false) }

    var updateBusy by remember { mutableStateOf(false) }
    var updateHeadline by remember {
        mutableStateOf("Installed Version: v${MineServeAboutData.APP_VERSION} (Synced across Android & Desktop)")
    }
    var updateBody by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf(MineServeAboutData.GITHUB_RELEASES_URL) }

    var newAdminPass by remember { mutableStateOf("") }

    DesktopM3ModalSurface(width = 880.dp, height = 660.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Settings & Host Diagnostics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "MineServe v${MineServeAboutData.APP_VERSION} - ${DesktopHostTelemetry.getHostDisplayName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = { viewModel.openModal(DesktopModal.ABOUT_CREDITS) },
                    shape = ShapeDefaults.Medium
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("About & Credits")
                }
                IconButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val sectionTabs = listOf(
            0 to "Appearance & Web",
            1 to "Java Runtimes",
            2 to "Diagnostics",
            3 to "Updates"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            sectionTabs.forEachIndexed { index, (idx, title) ->
                SegmentedButton(
                    selected = activeSection == idx,
                    onClick = { activeSection = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = sectionTabs.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colorScheme.secondaryContainer,
                        activeContentColor = colorScheme.onSecondaryContainer,
                        inactiveContainerColor = colorScheme.surfaceContainer,
                        inactiveContentColor = colorScheme.onSurfaceVariant,
                        activeBorderColor = colorScheme.outlineVariant,
                        inactiveBorderColor = colorScheme.outlineVariant
                    )
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (activeSection) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Appearance & HiDPI Scaling
                    MineServeSettingsGroup(title = "Appearance & Display") {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Color Scheme", style = MaterialTheme.typography.bodyLarge)
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
                                        onClick = { viewModel.setThemeMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Desktop HiDPI UI Scale", style = MaterialTheme.typography.bodyLarge)
                                MineServeTableMonoText(text = "${(uiScale * 100).toInt()}%", color = colorScheme.primary)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1.0f, 1.15f, 1.25f, 1.35f, 1.50f, 1.75f).forEach { preset ->
                                    FilterChip(
                                        selected = kotlin.math.abs(uiScale - preset) < 0.03f,
                                        onClick = { viewModel.setUiScale(preset) },
                                        label = { Text("${(preset * 100).toInt()}%", style = MaterialTheme.typography.labelMedium.copy(fontFamily = monoFont)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colorScheme.secondaryContainer,
                                            selectedLabelColor = colorScheme.onSecondaryContainer,
                                            containerColor = colorScheme.surfaceContainerHigh,
                                            labelColor = colorScheme.onSurfaceVariant
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }
                    }

                    // Data Directories
                    val currentServersDir by viewModel.serversDirPath.collectAsState()
                    val currentRuntimesDir by viewModel.runtimesDirPath.collectAsState()
                    var serversDirInput by remember(currentServersDir) { mutableStateOf(currentServersDir) }
                    var runtimesDirInput by remember(currentRuntimesDir) { mutableStateOf(currentRuntimesDir) }

                    MineServeSettingsGroup(title = "Host Directories") {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = serversDirInput,
                                    onValueChange = { serversDirInput = it },
                                    label = { Text("Servers Directory") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.openInFileManager(viewModel.paths.serversDir) }
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Open Servers Directory")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = runtimesDirInput,
                                    onValueChange = { runtimesDirInput = it },
                                    label = { Text("Portable Java Runtimes Directory") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.openInFileManager(viewModel.paths.runtimesDir) }
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Open Java Runtimes Directory")
                                }
                            }
                            Text(
                                text = "Existing server files and Java runtimes are not moved automatically when changing paths.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.updateHostDirectories("", "")
                                    },
                                    shape = ShapeDefaults.Medium
                                ) {
                                    Text("Reset Defaults")
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateHostDirectories(serversDirInput, runtimesDirInput)
                                    },
                                    shape = ShapeDefaults.Medium
                                ) {
                                    Text("Save Directories")
                                }
                            }
                        }
                    }

                    // Web Remote Admin Daemon
                    MineServeSettingsGroup(title = "Headless WebUI & Remote Admin") {
                        MineServePreferenceRow(
                            title = "Web Remote Admin Panel",
                            subtitle = if (webRunning) "Running on port $webPort" else "Manage all servers remotely from a browser on your LAN",
                            checked = webRunning,
                            onCheckedChange = { enabled -> viewModel.setWebAdminEnabled(enabled) },
                            icon = Icons.Rounded.Dns
                        )
                        if (webRunning) {
                            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                            FlowRow(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                itemVerticalAlignment = Alignment.CenterVertically
                            ) {
                                NetworkAddressChip(
                                    address = "http://127.0.0.1:$webPort",
                                    isPublicTunnel = false,
                                    onCopied = { viewModel.copyToClipboard(it, "Copied local Web Admin URL") }
                                )
                                NetworkAddressChip(
                                    address = "http://$lanIp:$webPort",
                                    isPublicTunnel = true,
                                    onCopied = { viewModel.copyToClipboard(it, "Copied LAN Web Admin URL") }
                                )
                                Button(
                                    onClick = { viewModel.openUrlInBrowser("http://127.0.0.1:$webPort") },
                                    shape = ShapeDefaults.Medium
                                ) {
                                    Text("Open in Browser")
                                }
                            }
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newAdminPass,
                                onValueChange = { newAdminPass = it },
                                label = { Text("Reset Primary 'admin' Password (6+ chars)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val clean = newAdminPass.trim()
                                    if (clean.length < 6) {
                                        viewModel.showToast("Password must be at least 6 characters")
                                    } else {
                                        viewModel.userRepository.syncPrimaryOwnerCredentials("admin", clean)
                                        newAdminPass = ""
                                        viewModel.showToast("Updated 'admin' Owner password")
                                    }
                                },
                                shape = ShapeDefaults.Medium
                            ) {
                                Text("Update Password")
                            }
                        }
                    }
                }
            }

            1 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MineServeTableMonoText(
                            text = viewModel.paths.runtimesDir.absolutePath,
                            color = colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = { viewModel.openInFileManager(viewModel.paths.runtimesDir) },
                            shape = ShapeDefaults.Small
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Runtimes Folder")
                        }
                    }

                    MineServeTableContainer(modifier = Modifier.fillMaxWidth()) {
                        val majors = listOf(8, 17, 21, 25)
                        majors.forEachIndexed { idx, major ->
                            val resolved: File? = remember(major, javaRefreshTick) { viewModel.javaManager.getJavaBinary(major) }
                            val installed: Boolean = remember(major, javaRefreshTick) { viewModel.javaManager.isJavaInstalled(major) }
                            MineServeTableRow(
                                showDivider = idx < majors.lastIndex,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "OpenJDK $major LTS",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface
                                    )
                                    MineServeTableMonoText(
                                        text = if (installed && resolved != null) {
                                            resolved.absolutePath
                                        } else {
                                            "Not installed (auto-provisions on server start)"
                                        },
                                        color = if (installed) colorScheme.primary else colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = {
                                        installingMajor = major
                                        viewModel.scope.launch {
                                            viewModel.javaManager.installJava(major) { msg: String ->
                                                viewModel.showToast(msg)
                                            }
                                            installingMajor = null
                                            javaRefreshTick++
                                        }
                                    },
                                    enabled = installingMajor == null,
                                    shape = ShapeDefaults.Small,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (installed) colorScheme.primaryContainer else colorScheme.secondaryContainer,
                                        contentColor = if (installed) colorScheme.onPrimaryContainer else colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            installingMajor == major -> "Downloading..."
                                            installed -> "Installed"
                                            else -> "Install OpenJDK $major"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "MINESERVE_TAGS" to "All Engine & Console Logs",
                                "ERRORS_ONLY" to "Warnings & Errors Only"
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = logFilterMode == mode,
                                    onClick = {
                                        logFilterMode = mode
                                        capturedLogs = DesktopDiagnosticsManager.captureLogs(
                                            mode,
                                            viewModel.serverRepository,
                                            viewModel.processManager
                                        )
                                    },
                                    label = { Text(label) },
                                    border = null
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    capturedLogs = DesktopDiagnosticsManager.captureLogs(
                                        logFilterMode,
                                        viewModel.serverRepository,
                                        viewModel.processManager
                                    )
                                    viewModel.showToast("Refreshed diagnostic logs")
                                }
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh logs")
                            }
                            IconButton(
                                onClick = { viewModel.copyToClipboard(capturedLogs, "Copied redacted diagnostic logs") }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy logs")
                            }
                            FilledTonalButton(
                                onClick = {
                                    selfTestBusy = true
                                    viewModel.scope.launch {
                                        selfTestResults = DesktopDiagnosticsManager.runNetworkSelfTest()
                                        selfTestBusy = false
                                    }
                                },
                                enabled = !selfTestBusy,
                                shape = ShapeDefaults.Small
                            ) {
                                Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (selfTestBusy) "Testing..." else "Self-Test")
                            }
                            Button(
                                onClick = {
                                    viewModel.scope.launch {
                                        val zip = DesktopDiagnosticsManager.exportDiagnosticZip(
                                            paths = viewModel.paths,
                                            filterMode = logFilterMode,
                                            serverRepository = viewModel.serverRepository,
                                            processManager = viewModel.processManager
                                        )
                                        viewModel.showToast("Saved ${zip.name}")
                                        viewModel.openInFileManager(zip.parentFile ?: viewModel.paths.tmpDir)
                                    }
                                },
                                shape = ShapeDefaults.Small
                            ) {
                                Text("Export .zip")
                            }
                        }
                    }

                    if (selfTestResults.isNotEmpty()) {
                        MineServeTableContainer(modifier = Modifier.fillMaxWidth()) {
                            selfTestResults.forEachIndexed { idx, item ->
                                MineServeTableRow(
                                    showDivider = idx < selfTestResults.lastIndex,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${if (item.passed) "PASS" else "WARN"}  ${item.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.passed) colorScheme.primary else extended.warning,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MineServeTableMonoText(text = item.detail, color = colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Surface(
                        shape = ShapeDefaults.Medium,
                        color = extended.terminalSurface,
                        contentColor = extended.terminalOnSurface,
                        border = null,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = capturedLogs,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = monoFont,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = extended.terminalOnSurface
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                val showWhatsNewOnUpdate by viewModel.showWhatsNewOnUpdate.collectAsState()
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MineServeSettingsGroup(title = "Release Updates & Feature Guide") {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("GitHub Release Update Checker", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(updateHeadline, style = MaterialTheme.typography.bodySmall, color = colorScheme.primary)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { viewModel.openWhatsNewGuide() },
                                        shape = ShapeDefaults.Small
                                    ) {
                                        Text("What's New ${MineServeWhatsNewCatalog.CURRENT_DISPLAY_VERSION}")
                                    }
                                    Button(
                                        onClick = {
                                            updateBusy = true
                                            updateHeadline = "Checking GitHub Releases..."
                                            viewModel.scope.launch {
                                                val checker = GitHubUpdateChecker()
                                                when (val res = checker.checkLatestRelease(MineServeAboutData.APP_VERSION)) {
                                                    is UpdateCheckResult.UpToDate -> {
                                                        updateHeadline = "You are on the latest release (v${MineServeAboutData.APP_VERSION})."
                                                        updateBody = ""
                                                    }
                                                    is UpdateCheckResult.UpdateAvailable -> {
                                                        updateHeadline = "New update available: ${res.release.tagName} (${res.release.name})"
                                                        updateBody = res.release.body
                                                        updateUrl = res.release.htmlUrl
                                                    }
                                                    is UpdateCheckResult.Error -> {
                                                        updateHeadline = "Update check failed: ${res.message}"
                                                    }
                                                }
                                                updateBusy = false
                                            }
                                        },
                                        enabled = !updateBusy,
                                        shape = ShapeDefaults.Small
                                    ) {
                                        Text(if (updateBusy) "Checking..." else "Check Updates")
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.openUrlInBrowser(updateUrl) },
                                        shape = ShapeDefaults.Small
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                        MineServePreferenceRow(
                            title = "What's New Release Guide",
                            subtitle = "Automatically display the What's New feature guide after major updates",
                            checked = showWhatsNewOnUpdate,
                            onCheckedChange = { viewModel.setShowWhatsNewOnUpdate(it) }
                        )
                    }

                    if (updateBody.isNotBlank()) {
                        MineServeCard(
                            containerColor = colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Latest Release Notes", style = MaterialTheme.typography.titleSmall, color = colorScheme.primary)
                                Text(updateBody, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopAboutAndCreditsModal(viewModel: DesktopViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var activeTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf(SharedCreditCategory.ALL) }

    val platformChangelog = remember {
        MineServeAboutData.getChangelogForPlatform(com.devwithzachary.mineserve.desktop.DesktopPlatform.changelogPlatform)
    }
    val filteredCredits = remember(selectedCategory) {
        MineServeAboutData.getCreditsForPlatform(com.devwithzachary.mineserve.desktop.DesktopPlatform.changelogPlatform, selectedCategory)
    }

    DesktopM3ModalSurface(width = 880.dp, height = 660.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MineServe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        border = null
                    ) {
                        Text(
                            text = "v${MineServeAboutData.APP_VERSION}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = colorScheme.surfaceContainer,
                        contentColor = colorScheme.onSurfaceVariant,
                        border = null
                    ) {
                        Text(
                            text = "GPL-3.0 Open Source",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "Cross-Platform Minecraft Java, Modded, Proxy & Bedrock Server Manager",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        MineServeCard(
            containerColor = colorScheme.surfaceContainer,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.WEBSITE_URL) }, shape = ShapeDefaults.Small) {
                    Text("Website")
                }
                FilledTonalButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.GITHUB_REPO_URL) }, shape = ShapeDefaults.Small) {
                    Text("GitHub Repo")
                }
                FilledTonalButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.GITHUB_ISSUES_URL) }, shape = ShapeDefaults.Small) {
                    Text("Report Issue")
                }
                FilledTonalButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.DISCORD_URL) }, shape = ShapeDefaults.Small) {
                    Text("Discord")
                }
                FilledTonalButton(
                    onClick = { viewModel.openUrlInBrowser(MineServeAboutData.PATREON_URL) },
                    shape = ShapeDefaults.Small,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onErrorContainer
                    )
                ) {
                    Text("Patreon")
                }
                FilledTonalButton(
                    onClick = { viewModel.openUrlInBrowser(MineServeAboutData.BUYMEACOFFEE_URL) },
                    shape = ShapeDefaults.Small,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = extended.warningContainer,
                        contentColor = extended.onWarningContainer
                    )
                ) {
                    Text("Buy Me a Coffee")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingleChoiceSegmentedButtonRow {
                listOf(
                    0 to "Changelog",
                    1 to "Supporters (${MineServeAboutData.SUPPORTERS.size})",
                    2 to "Open-Source Credits (${filteredCredits.size})"
                ).forEachIndexed { index, (idx, label) ->
                    SegmentedButton(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
            FilledTonalButton(
                onClick = { viewModel.openWhatsNewGuide() },
                shape = ShapeDefaults.Small
            ) {
                Text("View What's New Guide")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (activeTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(platformChangelog, key = { it.version }) { release ->
                        MineServeCard(
                            containerColor = colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(release.version, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                                MineServeTableMonoText(text = release.date, color = colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(6.dp))
                            release.highlights.forEach { bullet ->
                                Text("- $bullet", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
            1 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MineServeCard(
                        containerColor = colorScheme.surfaceContainer,
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Project Supporters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Huge thank you to everyone supporting MineServe's free, open-source development on Patreon and Buy Me a Coffee.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    MineServeTableContainer(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(
                                MineServeAboutData.SUPPORTERS,
                                key = { index, supporter -> "${supporter.name}_$index" }
                            ) { index, supporter ->
                                MineServeTableRow(
                                    showDivider = index < MineServeAboutData.SUPPORTERS.lastIndex,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.errorContainer,
                                        border = null,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Favorite,
                                                contentDescription = null,
                                                tint = colorScheme.onErrorContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = supporter.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = supporter.tier,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.primary
                                        )
                                    }
                                    Surface(
                                        shape = ShapeDefaults.Small,
                                        color = colorScheme.errorContainer,
                                        contentColor = colorScheme.onErrorContainer,
                                        border = null
                                    ) {
                                        Text(
                                            text = "Supporter",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SharedCreditCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            border = null
                        )
                    }
                }

                MineServeTableContainer(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredCredits, key = { it.name }) { credit ->
                            MineServeTableRow(
                                showDivider = credit != filteredCredits.last(),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(credit.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Surface(
                                            shape = ShapeDefaults.ExtraSmall,
                                            color = colorScheme.primaryContainer,
                                            contentColor = colorScheme.onPrimaryContainer,
                                            border = null
                                        ) {
                                            Text(
                                                text = credit.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = ShapeDefaults.ExtraSmall,
                                            color = colorScheme.surfaceContainerHigh,
                                            contentColor = colorScheme.onSurfaceVariant,
                                            border = null
                                        ) {
                                            Text(
                                                text = credit.license,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(credit.description, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.openUrlInBrowser(credit.url) },
                                    shape = ShapeDefaults.Small
                                ) {
                                    Text("Open Project")
                                }
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
internal fun DesktopWhatsNewModal(viewModel: DesktopViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current

    val release = remember { MineServeWhatsNewCatalog.getForPlatform(com.devwithzachary.mineserve.desktop.DesktopPlatform.changelogPlatform) }
    val showOnUpdate by viewModel.showWhatsNewOnUpdate.collectAsState()

    DesktopM3ModalSurface(width = 820.dp, height = 640.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(release.headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        border = null
                    ) {
                        Text(
                            text = release.displayVersion,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(release.subtitle, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = {
                    DesktopDiagnosticsManager.markWhatsNewSeen(viewModel.paths)
                    viewModel.openModal(DesktopModal.NONE)
                }
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(release.features, key = { it.title }) { feature ->
                MineServeCard(
                    containerColor = colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer,
                            border = null
                        ) {
                            Text(
                                text = feature.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(feature.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(feature.summary, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = colorScheme.surfaceContainerLow,
                        contentColor = colorScheme.onSurface,
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("WHERE TO FIND IT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colorScheme.primary)
                            feature.stepsToFind.forEachIndexed { idx, step ->
                                Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.setShowWhatsNewOnUpdate(!showOnUpdate) }
            ) {
                Checkbox(
                    checked = !showOnUpdate,
                    onCheckedChange = { neverShow -> viewModel.setShowWhatsNewOnUpdate(!neverShow) }
                )
                Text(
                    text = "Don't show automatically on future updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    DesktopDiagnosticsManager.markWhatsNewSeen(viewModel.paths)
                    viewModel.openModal(DesktopModal.NONE)
                },
                shape = ShapeDefaults.Medium
            ) {
                Text("Got It, Let's Go!")
            }
        }
    }
}
