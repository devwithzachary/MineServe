package com.devwithzachary.mineserve.ui.screens.diagnostics

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.diagnostics.DiagnosticCheckResult
import com.devwithzachary.mineserve.diagnostics.DiagnosticManager
import com.devwithzachary.mineserve.diagnostics.LogFilterMode
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var filterMode by remember { mutableStateOf(LogFilterMode.MINESERVE_TAGS) }
    var logText by remember { mutableStateOf("Tap 'Capture Logs' to read buffered app & engine logs.") }
    var isCapturingLogs by remember { mutableStateOf(false) }

    var networkResults by remember { mutableStateOf<List<DiagnosticCheckResult>>(emptyList()) }
    var isRunningNetworkTest by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    LaunchedEffect(filterMode) {
        isCapturingLogs = true
        logText = DiagnosticManager.captureLogcat(filterMode)
        isCapturingLogs = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics & Logcat", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Card 1: Networking & Tunnel Health Self-Test
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(22.dp))
                        Text("Tunnel & Network Self-Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Text(
                        text = "Tests DNS resolution, Bore/Playit reachability, MineServe Cloud API health, and local UPnP gateway discovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
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
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRunningNetworkTest) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Running Self-Test...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run Network Self-Test", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (networkResults.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            networkResults.forEach { res ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Slate900.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, ObsidianCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (res.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (res.passed) EmeraldLight else RedstoneLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(res.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text("${res.detail} (${res.latencyMs}ms)", style = MaterialTheme.typography.labelSmall, color = Slate400)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Redacted Logcat Capture & Export
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
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(22.dp))
                            Text("App Logcat Capture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
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
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Logs", tint = EmeraldLight)
                        }
                    }

                    Text(
                        text = "Tokens, passwords, webhook URLs, and public IPs are automatically redacted before display and export.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LogFilterMode.entries.forEach { mode ->
                            FilterChip(
                                selected = filterMode == mode,
                                onClick = { filterMode = mode },
                                label = { Text(mode.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldDark,
                                    selectedLabelColor = EmeraldLight,
                                    containerColor = Slate800,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, ObsidianCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 280.dp)
                    ) {
                        Text(
                            text = if (isCapturingLogs) "Capturing logcat..." else logText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            color = Slate400,
                            modifier = Modifier
                                .padding(10.dp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share .zip", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
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
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ObsidianCardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save .zip", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

