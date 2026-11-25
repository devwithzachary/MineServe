package com.devwithzachary.mineserve.ui.screens.diagnostics

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.diagnostics.DiagnosticCheckResult
import com.devwithzachary.mineserve.diagnostics.DiagnosticManager
import com.devwithzachary.mineserve.diagnostics.LogFilterMode
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    initialLogText: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = androidx.compose.ui.platform.LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var filterMode by remember { mutableStateOf(LogFilterMode.MINESERVE_TAGS) }
    var logText by remember { mutableStateOf(initialLogText ?: "Tap 'Refresh' to read buffered app & engine logs.") }
    var isCapturingLogs by remember { mutableStateOf(false) }

    var networkResults by remember { mutableStateOf<List<DiagnosticCheckResult>>(emptyList()) }
    var isRunningNetworkTest by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(filterMode) {
        if (!isInspection) {
            isCapturingLogs = true
            logText = DiagnosticManager.captureLogcat(filterMode)
            isCapturingLogs = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Diagnostics & Logcat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
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
            // Card 1: Networking & Tunnel Health Self-Test
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Tunnel & Network Self-Test",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Tests DNS resolution, Bore/Playit reachability, MineServe Cloud API health, and local UPnP gateway discovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            isRunningNetworkTest = true
                            scope.launch {
                                networkResults = DiagnosticManager.runNetworkSelfTest(context)
                                isRunningNetworkTest = false
                            }
                        },
                        enabled = !isRunningNetworkTest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRunningNetworkTest) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running Self-Test...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Network Self-Test", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (networkResults.isNotEmpty()) {
                        MineServeTableContainer {
                            networkResults.forEachIndexed { index, res ->
                                MineServeTableRow(showDivider = index < networkResults.lastIndex) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (res.passed) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                            contentDescription = null,
                                            tint = if (res.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = res.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            MineServeTableMonoText(
                                                text = "${res.detail} (${res.latencyMs}ms)",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Redacted Logcat Capture & Export
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                imageVector = Icons.Rounded.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "App Logcat Capture",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                isCapturingLogs = true
                                scope.launch {
                                    logText = DiagnosticManager.captureLogcat(filterMode)
                                    isCapturingLogs = false
                                }
                            },
                            enabled = !isCapturingLogs
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh Logs",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Tokens, passwords, webhook URLs, and public IPs are automatically redacted before display and export.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LogFilterMode.entries.forEach { mode ->
                            FilterChip(
                                selected = filterMode == mode,
                                onClick = { filterMode = mode },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Surface(
                        shape = ShapeDefaults.Medium,
                        color = extColors.terminalSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 280.dp)
                    ) {
                        Text(
                            text = if (isCapturingLogs) "Capturing logcat..." else logText,
                            fontFamily = monoFont,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = extColors.terminalOnSurface,
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                isExporting = true
                                scope.launch {
                                    val zipFile = DiagnosticManager.exportDiagnosticBundle(context, logText, networkResults)
                                    isExporting = false
                                    if (zipFile != null) {
                                        val intent = DiagnosticManager.getShareIntent(context, zipFile)
                                        if (intent != null) {
                                            context.startActivity(Intent.createChooser(intent, "Share Diagnostics Bundle"))
                                        } else {
                                            Toast.makeText(context, "Unable to open share sheet", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to create diagnostic bundle", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isExporting && !isCapturingLogs,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share .zip", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                isExporting = true
                                scope.launch {
                                    val zipFile = DiagnosticManager.exportDiagnosticBundle(context, logText, networkResults)
                                    val savedPath = if (zipFile != null) DiagnosticManager.saveBundleToDownloads(context, zipFile) else null
                                    isExporting = false
                                    Toast.makeText(
                                        context,
                                        if (savedPath != null) "Saved to $savedPath" else "Failed to save bundle",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = !isExporting && !isCapturingLogs,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save .zip")
                        }
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun DiagnosticsScreenPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        DiagnosticsScreen(
            onBack = {},
            initialLogText = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.SAMPLE_DIAGNOSTICS_LOG
        )
    }
}
