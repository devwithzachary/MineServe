package com.devwithzachary.mineserve.desktop.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.api.GitHubUpdateChecker
import com.devwithzachary.mineserve.api.UpdateCheckResult
import com.devwithzachary.mineserve.desktop.LinuxDiagnosticsManager
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.LinuxSelfTestItem
import com.devwithzachary.mineserve.desktop.ui.DesktopColors
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopPanelCard
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.ChangelogPlatform
import com.devwithzachary.mineserve.model.MineServeAboutData
import com.devwithzachary.mineserve.model.SharedCreditCategory
import java.io.File
import kotlinx.coroutines.launch

@Composable
internal fun DesktopAppSettingsModal(viewModel: DesktopViewModel) {
    var activeSection by remember { mutableIntStateOf(0) } // 0: General & Web Admin, 1: Java Runtimes, 2: Diagnostics & Logs, 3: Updates
    val webRunning by viewModel.webAdminRunning.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val lanIp = remember { LinuxHostTelemetry.getLocalIpAddress() ?: "127.0.0.1" }
    val webPort = viewModel.webServer.actualPort

    // Java Runtimes state
    var installingMajor by remember { mutableStateOf<Int?>(null) }
    var javaRefreshTick by remember { mutableIntStateOf(0) }

    // Diagnostics state
    var logFilterMode by remember { mutableStateOf("MINESERVE_TAGS") }
    var capturedLogs by remember {
        mutableStateOf(
            LinuxDiagnosticsManager.captureLogs("MINESERVE_TAGS", viewModel.serverRepository, viewModel.processManager)
        )
    }
    var selfTestResults by remember { mutableStateOf<List<LinuxSelfTestItem>>(emptyList()) }
    var selfTestBusy by remember { mutableStateOf(false) }

    // Updates state
    var updateBusy by remember { mutableStateOf(false) }
    var updateHeadline by remember {
        mutableStateOf("Installed Version: v${MineServeAboutData.APP_VERSION} (Synced across Android & Linux)")
    }
    var updateBody by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf(MineServeAboutData.GITHUB_RELEASES_URL) }

    // Admin password quick reset
    var newAdminPass by remember { mutableStateOf("") }

    DesktopPanelCard(modifier = Modifier.width(860.dp).height(640.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("App Settings & Host Diagnostics", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Text(
                    text = "MineServe v${MineServeAboutData.APP_VERSION} - ${LinuxHostTelemetry.getHostDisplayName()}",
                    fontSize = 12.sp,
                    color = DesktopColors.Slate400
                )
            }
            OutlinedButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Text("Close", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                0 to "Web Admin & Display",
                1 to "Java Runtimes (OpenJDK)",
                2 to "Diagnostics & Error Logs",
                3 to "Updates & Releases"
            ).forEach { (idx, title) ->
                val active = activeSection == idx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                    border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                    modifier = Modifier.clickable { activeSection = idx }
                ) {
                    Text(
                        text = title,
                        color = if (active) Color.Black else DesktopColors.Slate300,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
        Spacer(modifier = Modifier.height(12.dp))

        when (activeSection) {
            0 -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Web Remote Admin Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DesktopColors.Slate900,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Web Remote Admin Panel", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (webRunning) DesktopColors.EmeraldPrimary.copy(alpha = 0.2f) else DesktopColors.Slate800,
                                            border = BorderStroke(1.dp, if (webRunning) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder)
                                        ) {
                                            Text(
                                                text = if (webRunning) "ONLINE (:$webPort)" else "DISABLED",
                                                color = if (webRunning) DesktopColors.EmeraldLight else DesktopColors.Slate400,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Manage all servers remotely from any browser on your LAN or through a tunnel.",
                                        fontSize = 12.sp,
                                        color = DesktopColors.Slate400
                                    )
                                }
                                Switch(
                                    checked = webRunning,
                                    onCheckedChange = { enabled -> viewModel.setWebAdminEnabled(enabled) }
                                )
                            }

                            if (webRunning) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Local: http://127.0.0.1:$webPort  |  LAN: http://$lanIp:$webPort",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = DesktopColors.EmeraldLight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.copyToClipboard("http://$lanIp:$webPort", "Copied LAN Web Admin URL") }
                                    ) {
                                        Text("Copy LAN URL", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { viewModel.openUrlInBrowser("http://127.0.0.1:$webPort") },
                                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                                    ) {
                                        Text("Open in Browser", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            HorizontalDivider(color = DesktopColors.ObsidianCardBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newAdminPass,
                                    onValueChange = { newAdminPass = it },
                                    label = { Text("Reset Primary 'admin' Password (min 6 chars)") },
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
                                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                                ) {
                                    Text("Update Password", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Display & UI Scaling Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DesktopColors.Slate900,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Desktop Display & HiDPI UI Scaling", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Text(
                                text = "Current UI Scale: ${(uiScale * 100).toInt()}% (persisted in ${viewModel.paths.rootDir.absolutePath}/desktop_ui.json)",
                                fontSize = 12.sp,
                                color = DesktopColors.Slate400
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(1.0f, 1.15f, 1.25f, 1.35f, 1.50f, 1.75f).forEach { preset ->
                                    val selected = kotlin.math.abs(uiScale - preset) < 0.03f
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) DesktopColors.EmeraldPrimary else DesktopColors.Slate950,
                                        border = BorderStroke(1.dp, if (selected) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                                        modifier = Modifier.clickable { viewModel.setUiScale(preset) }
                                    ) {
                                        Text(
                                            text = "${(preset * 100).toInt()}%",
                                            color = if (selected) Color.Black else DesktopColors.Slate300,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Java Runtimes Section
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Portable Adoptium Temurin OpenJDK directory: ${viewModel.paths.runtimesDir.absolutePath}",
                            fontSize = 12.sp,
                            color = DesktopColors.Slate400
                        )
                        OutlinedButton(onClick = { viewModel.openInFileManager(viewModel.paths.runtimesDir) }) {
                            Text("Open Runtimes Folder", fontSize = 11.sp)
                        }
                    }

                    listOf(8, 17, 21, 25).forEach { major ->
                        val resolved: File? = remember(major, javaRefreshTick) { viewModel.javaManager.getJavaBinary(major) }
                        val installed: Boolean = remember(major, javaRefreshTick) { viewModel.javaManager.isJavaInstalled(major) }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DesktopColors.Slate900,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("OpenJDK $major LTS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text(
                                        text = if (installed && resolved != null) {
                                            "Installed (${resolved.absolutePath})"
                                        } else {
                                            "Not installed (can be installed now or auto-provisioned on server start)"
                                        },
                                        fontSize = 11.sp,
                                        color = if (installed) DesktopColors.EmeraldLight else DesktopColors.Slate400
                                    )
                                }
                                Button(
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (installed) DesktopColors.Slate800 else DesktopColors.EmeraldPrimary,
                                        contentColor = if (installed) DesktopColors.EmeraldLight else Color.Black
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            installingMajor == major -> "Downloading..."
                                            installed -> "Installed"
                                            else -> "Install OpenJDK $major"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Diagnostics & Error Logs Section
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "MINESERVE_TAGS" to "All Engine & Console Logs",
                                "ERRORS_ONLY" to "Warnings & Errors Only"
                            ).forEach { (mode, label) ->
                                val active = logFilterMode == mode
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                                    border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                                    modifier = Modifier.clickable {
                                        logFilterMode = mode
                                        capturedLogs = LinuxDiagnosticsManager.captureLogs(
                                            mode,
                                            viewModel.serverRepository,
                                            viewModel.processManager
                                        )
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) Color.Black else DesktopColors.Slate300,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    capturedLogs = LinuxDiagnosticsManager.captureLogs(
                                        logFilterMode,
                                        viewModel.serverRepository,
                                        viewModel.processManager
                                    )
                                    viewModel.showToast("Refreshed diagnostic logs")
                                }
                            ) {
                                Text("Refresh Logs", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.copyToClipboard(capturedLogs, "Copied redacted diagnostic logs")
                                }
                            ) {
                                Text("Copy Logs", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    selfTestBusy = true
                                    viewModel.scope.launch {
                                        selfTestResults = LinuxDiagnosticsManager.runNetworkSelfTest()
                                        selfTestBusy = false
                                    }
                                },
                                enabled = !selfTestBusy
                            ) {
                                Text(if (selfTestBusy) "Testing..." else "Run Self-Test", fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                            }
                            Button(
                                onClick = {
                                    viewModel.scope.launch {
                                        val zip = LinuxDiagnosticsManager.exportDiagnosticZip(
                                            paths = viewModel.paths,
                                            filterMode = logFilterMode,
                                            serverRepository = viewModel.serverRepository,
                                            processManager = viewModel.processManager
                                        )
                                        viewModel.showToast("Saved ${zip.name}")
                                        viewModel.openInFileManager(zip.parentFile ?: viewModel.paths.tmpDir)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                            ) {
                                Text("Export .zip Bundle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selfTestResults.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            selfTestResults.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DesktopColors.Slate900,
                                    border = BorderStroke(
                                        1.dp,
                                        if (item.passed) DesktopColors.EmeraldPrimary.copy(alpha = 0.5f) else DesktopColors.AmberWarning.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${if (item.passed) "[PASS]" else "[WARN]"} ${item.name}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.passed) DesktopColors.EmeraldLight else DesktopColors.AmberLight
                                        )
                                        Text(item.detail, fontSize = 11.sp, color = DesktopColors.Slate300)
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF06090F),
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
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
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = DesktopColors.Slate300,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                // App Updates & Releases Section
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DesktopColors.Slate900,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GitHub Release Update Checker", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(updateHeadline, fontSize = 12.sp, color = DesktopColors.EmeraldLight)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                                ) {
                                    Text(if (updateBusy) "Checking..." else "Check for Updates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(onClick = { viewModel.openUrlInBrowser(updateUrl) }) {
                                    Text("Open GitHub Releases", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (updateBody.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DesktopColors.Slate950,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text("Latest Release Notes:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DesktopColors.EmeraldLight)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(updateBody, fontSize = 12.sp, color = DesktopColors.Slate300)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopAboutAndCreditsModal(viewModel: DesktopViewModel) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Platform Changelog, 1: Open-Source Credits
    var selectedCategory by remember { mutableStateOf(SharedCreditCategory.ALL) }

    val platformChangelog = remember {
        MineServeAboutData.getChangelogForPlatform(ChangelogPlatform.LINUX)
    }
    val filteredCredits = remember(selectedCategory) {
        MineServeAboutData.getCreditsForPlatform(ChangelogPlatform.LINUX, selectedCategory)
    }

    DesktopPanelCard(modifier = Modifier.width(880.dp).height(660.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MineServe", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DesktopColors.EmeraldPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, DesktopColors.EmeraldPrimary)
                    ) {
                        Text(
                            text = "v${MineServeAboutData.APP_VERSION}",
                            color = DesktopColors.EmeraldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DesktopColors.Slate800,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder)
                    ) {
                        Text(
                            text = "GPL-3.0 Open Source",
                            color = DesktopColors.Slate300,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "Cross-Platform Minecraft Java, Modded, Proxy & Bedrock Server Manager",
                    fontSize = 12.sp,
                    color = DesktopColors.Slate400
                )
            }
            OutlinedButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Text("Close", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Community & Support Links + Supporters
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DesktopColors.Slate900,
            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.WEBSITE_URL) }) {
                        Text("Website", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.GITHUB_REPO_URL) }) {
                        Text("GitHub Repo", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.GITHUB_ISSUES_URL) }) {
                        Text("Report Issue", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { viewModel.openUrlInBrowser(MineServeAboutData.DISCORD_URL) }) {
                        Text("Discord Community", fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                    }
                    Button(
                        onClick = { viewModel.openUrlInBrowser(MineServeAboutData.PATREON_URL) },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.RedstonePrimary, contentColor = Color.White)
                    ) {
                        Text("Patreon", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.openUrlInBrowser(MineServeAboutData.BUYMEACOFFEE_URL) },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.AmberWarning, contentColor = Color.Black)
                    ) {
                        Text("Buy Me a Coffee", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Project Supporters: " + MineServeAboutData.SUPPORTERS.joinToString("  |  ") { "${it.name} (${it.tier})" },
                    fontSize = 11.sp,
                    color = DesktopColors.Slate300
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-Tab Switcher
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                0 to "Release Changelog (Linux & Cross-Platform)",
                1 to "Software & Open-Source Credits (${filteredCredits.size})"
            ).forEach { (idx, label) ->
                val active = activeTab == idx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                    border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                    modifier = Modifier.clickable { activeTab = idx }
                ) {
                    Text(
                        text = label,
                        color = if (active) Color.Black else DesktopColors.Slate300,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(platformChangelog, key = { it.version }) { release ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DesktopColors.Slate900,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(release.version, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DesktopColors.EmeraldLight)
                                Text(release.date, fontSize = 11.sp, color = DesktopColors.Slate400)
                            }
                            HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
                            release.highlights.forEach { bullet ->
                                Text("- $bullet", fontSize = 12.sp, color = DesktopColors.Slate300, lineHeight = 17.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SharedCreditCategory.entries.forEach { cat ->
                        val active = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                            border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat.label,
                                color = if (active) Color.Black else DesktopColors.Slate300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCredits, key = { it.name }) { credit ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DesktopColors.Slate900,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(credit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DesktopColors.EmeraldPrimary.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = credit.tag,
                                                color = DesktopColors.EmeraldLight,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DesktopColors.Slate800
                                        ) {
                                            Text(
                                                text = credit.license,
                                                color = DesktopColors.Slate300,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(credit.description, fontSize = 12.sp, color = DesktopColors.Slate400)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(onClick = { viewModel.openUrlInBrowser(credit.url) }) {
                                    Text("Open Project", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
