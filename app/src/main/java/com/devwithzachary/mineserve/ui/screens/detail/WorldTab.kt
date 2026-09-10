package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.ChunkPruneOptions
import com.devwithzachary.mineserve.model.ChunkPruneResult
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.WorldDimensionInfo
import com.devwithzachary.mineserve.model.WorldSummary
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldTab(
    server: MinecraftServer,
    status: ServerStatus,
    onGetWorldSummary: suspend () -> WorldSummary,
    onImportWorld: suspend (Uri, Boolean, (String, Int) -> Unit) -> Result<String>,
    onExportWorld: suspend (OutputStream, (String, Int) -> Unit) -> Boolean,
    onResetDimension: suspend (DimensionType, Boolean) -> Boolean,
    onPruneChunks: suspend (ChunkPruneOptions, (String, Int) -> Unit) -> ChunkPruneResult,
    onNavigateToLiveMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var worldSummary by remember { mutableStateOf<WorldSummary?>(null) }
    var isLoadingSummary by remember { mutableStateOf(true) }

    fun refreshSummary() {
        scope.launch {
            isLoadingSummary = true
            worldSummary = onGetWorldSummary()
            isLoadingSummary = false
        }
    }

    LaunchedEffect(server.id) {
        refreshSummary()
    }

    // Reset Modal State
    var dimensionToReset by remember { mutableStateOf<DimensionType?>(null) }
    var resetBackupChecked by remember { mutableStateOf(true) }
    var isResettingDimension by remember { mutableStateOf(false) }

    // Import State
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var importBackupChecked by remember { mutableStateOf(true) }
    var isImportingWorld by remember { mutableStateOf(false) }
    var importProgressPercent by remember { mutableIntStateOf(0) }
    var importStatusText by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    // Export State
    var isExportingWorld by remember { mutableStateOf(false) }
    var exportProgressPercent by remember { mutableIntStateOf(0) }
    var exportStatusText by remember { mutableStateOf("") }

    // Chunk Prune State
    var selectedThreshold by remember { mutableStateOf(InhabitedTimeThreshold.UNTOUCHED) }
    var pruneNetherChecked by remember { mutableStateOf(true) }
    var pruneEndChecked by remember { mutableStateOf(true) }
    var pruneBackupChecked by remember { mutableStateOf(true) }
    var showPruneConfirmDialog by remember { mutableStateOf(false) }
    var isPruningChunks by remember { mutableStateOf(false) }
    var pruneProgressPercent by remember { mutableIntStateOf(0) }
    var pruneStatusText by remember { mutableStateOf("") }
    var pruneResultDialogData by remember { mutableStateOf<ChunkPruneResult?>(null) }

    // SAF Launchers
    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImportUri = uri
            showImportConfirmDialog = true
        }
    }

    val exportFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isExportingWorld = true
                exportStatusText = "Exporting world..."
                exportProgressPercent = 10
                val ok = try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        onExportWorld(stream) { statusStr, percent ->
                            exportStatusText = statusStr
                            exportProgressPercent = percent
                        }
                    } ?: false
                } catch (e: Exception) {
                    false
                }
                isExportingWorld = false
                if (ok) {
                    Toast.makeText(context, "World exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to export world.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isRunning = status == ServerStatus.RUNNING || status == ServerStatus.STARTING

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Shortcut Banner to Live Map (Only for plugin/mod capable servers, not Vanilla)
        val supportsLiveMap = server.type != ServerType.VANILLA && (server.type.supportsPlugins || server.type.supportsMods)
        if (supportsLiveMap) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, DiamondCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = DiamondCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.map_shortcut_banner_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.map_shortcut_banner_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Button(
                        onClick = onNavigateToLiveMap,
                        colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.map_shortcut_banner_btn),
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 1: World Footprint & Overview
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(R.string.world_card_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { refreshSummary() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Slate700)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.refresh), color = Slate400, fontSize = 11.sp)
                    }
                }

                Text(
                    text = stringResource(R.string.world_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 12.sp
                )

                // Total Footprint Highlight
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.world_total_size),
                                color = Slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val totalSize = worldSummary?.totalSizeBytes ?: 0L
                            Text(
                                text = formatFileSize(totalSize),
                                color = EmeraldLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isLoadingSummary) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = EmeraldLight)
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldDark.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Active Level: ${worldSummary?.levelName ?: "world"}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Dimension breakdown cards
                worldSummary?.dimensions?.forEach { dimInfo ->
                    DimensionRowCard(
                        dimInfo = dimInfo,
                        isRunning = isRunning,
                        onResetRequested = { dimensionToReset = dimInfo.dimension }
                    )
                }
            }
        }

        // Section 2: World Import & Export Card
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.world_io_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.world_io_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            importFilePickerLauncher.launch(
                                arrayOf("application/zip", "application/octet-stream", "*/*")
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.world_btn_import),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                            val suggestedName = "world_${server.name.replace(" ", "_")}_$timeStamp.zip"
                            exportFilePickerLauncher.launch(suggestedName)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.world_btn_export),
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Section 3: Chunk Pruner & Storage Optimizer
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = DiamondLight, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(R.string.world_pruner_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.world_pruner_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 12.sp
                )

                // Threshold Selection
                Text(
                    text = stringResource(R.string.world_pruner_threshold_label),
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InhabitedTimeThreshold.values().forEach { thresh ->
                        val isSelected = selectedThreshold == thresh
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) DiamondCyan.copy(alpha = 0.2f) else Slate800,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) DiamondCyan else ObsidianCardBorder
                            ),
                            onClick = { selectedThreshold = thresh }
                        ) {
                            Text(
                                text = thresh.label,
                                color = if (isSelected) DiamondCyan else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = selectedThreshold.description,
                    color = Slate400,
                    fontSize = 11.sp
                )

                // Checkboxes
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = pruneNetherChecked,
                            onCheckedChange = { pruneNetherChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = DiamondCyan)
                        )
                        Text("Include Nether dimension", color = Color.White, fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = pruneEndChecked,
                            onCheckedChange = { pruneEndChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = DiamondCyan)
                        )
                        Text("Include The End dimension", color = Color.White, fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = pruneBackupChecked,
                            onCheckedChange = { pruneBackupChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = DiamondCyan)
                        )
                        Text(
                            stringResource(R.string.world_pruner_backup_checkbox),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = { showPruneConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.world_pruner_btn),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Modal: Dimension Reset Confirmation
    if (dimensionToReset != null) {
        val targetDim = dimensionToReset!!
        AlertDialog(
            onDismissRequest = { if (!isResettingDimension) dimensionToReset = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedstoneRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (targetDim == DimensionType.NETHER) {
                        stringResource(R.string.world_reset_nether_title)
                    } else {
                        stringResource(R.string.world_reset_end_title)
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (targetDim == DimensionType.NETHER) {
                            stringResource(R.string.world_reset_nether_desc)
                        } else {
                            stringResource(R.string.world_reset_end_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    if (isRunning) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RedstoneRed.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.world_reset_running_warning),
                                color = RedstoneLight,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = resetBackupChecked,
                            onCheckedChange = { resetBackupChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Text(
                            text = stringResource(R.string.world_reset_backup_checkbox),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isResettingDimension = true
                            val ok = onResetDimension(targetDim, resetBackupChecked)
                            isResettingDimension = false
                            dimensionToReset = null
                            if (ok) {
                                refreshSummary()
                                Toast.makeText(context, context.getString(R.string.world_reset_success), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to reset dimension.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isResettingDimension,
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isResettingDimension) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(stringResource(R.string.world_reset_confirm_btn), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isResettingDimension) {
                    OutlinedButton(
                        onClick = { dimensionToReset = null },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            },
            containerColor = Slate950
        )
    }

    // Modal: World Import Confirmation
    if (showImportConfirmDialog && selectedImportUri != null) {
        AlertDialog(
            onDismissRequest = { if (!isImportingWorld) showImportConfirmDialog = false },
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
                    text = stringResource(R.string.world_import_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.world_import_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    if (isRunning) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RedstoneRed.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.world_import_running_warning),
                                color = RedstoneLight,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = importBackupChecked,
                            onCheckedChange = { importBackupChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Text(
                            text = stringResource(R.string.world_import_backup_checkbox),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    if (isImportingWorld) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { importProgressPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = EmeraldPrimary,
                                trackColor = Slate800
                            )
                            Text(text = importStatusText, color = EmeraldLight, fontSize = 11.sp)
                        }
                    }

                    if (importErrorMessage != null) {
                        Text(
                            text = importErrorMessage ?: "",
                            color = RedstoneRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedImportUri ?: return@Button
                        scope.launch {
                            isImportingWorld = true
                            importErrorMessage = null
                            val res = onImportWorld(uri, importBackupChecked) { statusStr, pct ->
                                importStatusText = statusStr
                                importProgressPercent = pct
                            }
                            isImportingWorld = false
                            if (res.isSuccess) {
                                showImportConfirmDialog = false
                                refreshSummary()
                                Toast.makeText(context, context.getString(R.string.world_import_success), Toast.LENGTH_LONG).show()
                            } else {
                                importErrorMessage = res.exceptionOrNull()?.message ?: "Failed importing world"
                            }
                        }
                    },
                    enabled = !isImportingWorld,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isImportingWorld) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Text("Import World", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isImportingWorld) {
                    OutlinedButton(
                        onClick = { showImportConfirmDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            },
            containerColor = Slate950
        )
    }

    // Modal: Chunk Pruning Confirmation & Progress
    if (showPruneConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPruningChunks) showPruneConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = DiamondCyan,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.world_pruner_confirm_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.world_pruner_confirm_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    if (isRunning) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = RedstoneRed.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = stringResource(R.string.world_pruner_running_warning),
                                color = RedstoneLight,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (isPruningChunks) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { pruneProgressPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = DiamondCyan,
                                trackColor = Slate800
                            )
                            Text(text = pruneStatusText, color = DiamondLight, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isPruningChunks = true
                            val options = ChunkPruneOptions(
                                threshold = selectedThreshold,
                                pruneNether = pruneNetherChecked,
                                pruneEnd = pruneEndChecked,
                                createBackup = pruneBackupChecked
                            )
                            val result = onPruneChunks(options) { statusStr, pct ->
                                pruneStatusText = statusStr
                                pruneProgressPercent = pct
                            }
                            isPruningChunks = false
                            showPruneConfirmDialog = false
                            refreshSummary()
                            pruneResultDialogData = result
                        }
                    },
                    enabled = !isPruningChunks,
                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isPruningChunks) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Text("Start Optimization", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isPruningChunks) {
                    OutlinedButton(
                        onClick = { showPruneConfirmDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.cancel), color = Color.White)
                    }
                }
            },
            containerColor = Slate950
        )
    }

    // Modal: Chunk Pruning Results Dialog
    if (pruneResultDialogData != null) {
        val res = pruneResultDialogData!!
        AlertDialog(
            onDismissRequest = { pruneResultDialogData = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.world_pruner_success_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            R.string.world_pruner_success_desc,
                            res.scannedRegions,
                            res.scannedChunks,
                            res.prunedChunks,
                            res.deletedRegions,
                            formatFileSize(res.bytesFreed),
                            res.percentFreed
                        ),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { pruneResultDialogData = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Slate950
        )
    }
}

@Composable
private fun DimensionRowCard(
    dimInfo: WorldDimensionInfo,
    isRunning: Boolean,
    onResetRequested: () -> Unit
) {
    val (icon, tintColor) = when (dimInfo.dimension) {
        DimensionType.OVERWORLD -> Icons.Default.Public to EmeraldLight
        DimensionType.NETHER -> Icons.Default.LocalFireDepartment to RedstoneRed
        DimensionType.THE_END -> Icons.Default.AutoAwesome to DiamondCyan
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate800,
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        text = dimInfo.dimension.displayName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (dimInfo.exists) {
                            "${formatFileSize(dimInfo.sizeBytes)} • ${dimInfo.regionFilesCount} region files (${dimInfo.totalChunksCount} chunks)"
                        } else {
                            "Not yet generated"
                        },
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }

            if (dimInfo.dimension != DimensionType.OVERWORLD && dimInfo.exists) {
                OutlinedButton(
                    onClick = onResetRequested,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (dimInfo.dimension == DimensionType.NETHER) "Reset Nether" else "Reset End",
                        color = RedstoneLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (dimInfo.dimension == DimensionType.OVERWORLD) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EmeraldDark.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "Primary",
                        color = EmeraldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
