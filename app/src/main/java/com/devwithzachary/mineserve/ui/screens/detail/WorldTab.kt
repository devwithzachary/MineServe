package com.devwithzachary.mineserve.ui.screens.detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.ChunkPruneOptions
import com.devwithzachary.mineserve.model.ChunkPruneResult
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.WorldSummary
import com.devwithzachary.mineserve.ui.screens.detail.world.ChunkPreGenCard
import com.devwithzachary.mineserve.ui.screens.detail.world.ChunkPruneDialog
import com.devwithzachary.mineserve.ui.screens.detail.world.ChunkPruneResultDialog
import com.devwithzachary.mineserve.ui.screens.detail.world.DimensionResetDialog
import com.devwithzachary.mineserve.ui.screens.detail.world.DimensionRowCard
import com.devwithzachary.mineserve.ui.screens.detail.world.RegionInspectorCard
import com.devwithzachary.mineserve.ui.screens.detail.world.WorldImportDialog
import com.devwithzachary.mineserve.ui.screens.detail.world.formatWorldFileSize
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onSendCommand: (String) -> Unit = {},
    onNavigateToLiveMap: () -> Unit,
    initialWorldSummary: WorldSummary? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = androidx.compose.ui.platform.LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val monoFont = LocalMineServeMonoFontFamily.current

    var worldSummary by remember { mutableStateOf(initialWorldSummary) }
    var isLoadingSummary by remember { mutableStateOf(initialWorldSummary == null) }

    fun refreshSummary() {
        scope.launch {
            isLoadingSummary = true
            worldSummary = onGetWorldSummary()
            isLoadingSummary = false
        }
    }

    LaunchedEffect(server.id) {
        if (!(isInspection && initialWorldSummary != null)) {
            refreshSummary()
        }
    }

    var dimensionToReset by remember { mutableStateOf<DimensionType?>(null) }
    var resetBackupChecked by remember { mutableStateOf(true) }
    var isResettingDimension by remember { mutableStateOf(false) }

    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var importBackupChecked by remember { mutableStateOf(true) }
    var isImportingWorld by remember { mutableStateOf(false) }
    var importProgressPercent by remember { mutableIntStateOf(0) }
    var importStatusText by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    var isExportingWorld by remember { mutableStateOf(false) }
    var exportProgressPercent by remember { mutableIntStateOf(0) }
    var exportStatusText by remember { mutableStateOf("") }

    var selectedThreshold by remember { mutableStateOf(InhabitedTimeThreshold.UNTOUCHED) }
    var pruneNetherChecked by remember { mutableStateOf(true) }
    var pruneEndChecked by remember { mutableStateOf(true) }
    var pruneBackupChecked by remember { mutableStateOf(true) }
    var showPruneConfirmDialog by remember { mutableStateOf(false) }
    var isPruningChunks by remember { mutableStateOf(false) }
    var pruneProgressPercent by remember { mutableIntStateOf(0) }
    var pruneStatusText by remember { mutableStateOf("") }
    var pruneResultDialogData by remember { mutableStateOf<ChunkPruneResult?>(null) }

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
                } catch (_: Exception) {
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
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        val supportsLiveMap = server.type != ServerType.VANILLA && (server.type.supportsPlugins || server.type.supportsMods)
        if (supportsLiveMap) {
            MineServeCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Map,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.map_shortcut_banner_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.map_shortcut_banner_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = onNavigateToLiveMap,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = ShapeDefaults.Small
                    ) {
                        Text(
                            text = stringResource(R.string.map_shortcut_banner_btn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 1: World Footprint & Overview
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            imageVector = Icons.Rounded.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.world_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = { refreshSummary() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = ShapeDefaults.Small
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.refresh), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Text(
                    text = stringResource(R.string.world_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = ShapeDefaults.Medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.world_total_size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val totalSize = worldSummary?.totalSizeBytes ?: 0L
                            Text(
                                text = formatWorldFileSize(totalSize),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = monoFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isLoadingSummary) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Surface(
                                shape = ShapeDefaults.Small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Level: ${worldSummary?.levelName ?: "world"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = monoFont,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

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
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.world_io_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.world_io_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            importFilePickerLauncher.launch(
                                arrayOf("application/zip", "application/octet-stream", "*/*")
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = ShapeDefaults.Small
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.world_btn_import),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val suggestedName = "world_${server.name.replace(" ", "_")}_$timeStamp.zip"
                            exportFilePickerLauncher.launch(suggestedName)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Small
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.world_btn_export),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Section 3: Chunk Pruner & Storage Optimizer
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.world_pruner_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.world_pruner_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.world_pruner_threshold_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InhabitedTimeThreshold.values().forEach { thresh ->
                        FilterChip(
                            selected = selectedThreshold == thresh,
                            onClick = { selectedThreshold = thresh },
                            label = { Text(thresh.label) },
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Text(
                    text = selectedThreshold.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = pruneNetherChecked,
                            onCheckedChange = { pruneNetherChecked = it }
                        )
                        Text(
                            text = "Include Nether dimension",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = pruneEndChecked,
                            onCheckedChange = { pruneEndChecked = it }
                        )
                        Text(
                            text = "Include The End dimension",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = pruneBackupChecked,
                            onCheckedChange = { pruneBackupChecked = it }
                        )
                        Text(
                            text = stringResource(R.string.world_pruner_backup_checkbox),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { showPruneConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = ShapeDefaults.Small,
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.world_pruner_btn),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        ChunkPreGenCard(
            server = server,
            isRunning = isRunning,
            onSendCommand = onSendCommand
        )

        RegionInspectorCard(
            server = server,
            isRunning = isRunning,
            levelName = worldSummary?.levelName ?: "world",
            refreshKey = worldSummary?.totalSizeBytes ?: 0L,
            onWorldMutated = { refreshSummary() }
        )
    }

    if (dimensionToReset != null) {
        val targetDim = dimensionToReset!!
        DimensionResetDialog(
            targetDim = targetDim,
            isRunning = isRunning,
            isResetting = isResettingDimension,
            resetBackupChecked = resetBackupChecked,
            onResetBackupCheckedChange = { resetBackupChecked = it },
            onDismiss = { dimensionToReset = null },
            onConfirm = {
                scope.launch {
                    isResettingDimension = true
                    val ok = onResetDimension(targetDim, resetBackupChecked)
                    isResettingDimension = false
                    dimensionToReset = null
                    if (ok) {
                        refreshSummary()
                        Toast.makeText(context, R.string.world_reset_success, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to reset dimension.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showImportConfirmDialog && selectedImportUri != null) {
        WorldImportDialog(
            isRunning = isRunning,
            isImporting = isImportingWorld,
            importBackupChecked = importBackupChecked,
            onImportBackupCheckedChange = { importBackupChecked = it },
            importProgressPercent = importProgressPercent,
            importStatusText = importStatusText,
            importErrorMessage = importErrorMessage,
            onDismiss = { showImportConfirmDialog = false },
            onConfirm = {
                val uri = selectedImportUri ?: return@WorldImportDialog
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
                        Toast.makeText(context, R.string.world_import_success, Toast.LENGTH_LONG).show()
                    } else {
                        importErrorMessage = res.exceptionOrNull()?.message ?: "Failed importing world"
                    }
                }
            }
        )
    }

    if (showPruneConfirmDialog) {
        ChunkPruneDialog(
            isRunning = isRunning,
            isPruning = isPruningChunks,
            pruneProgressPercent = pruneProgressPercent,
            pruneStatusText = pruneStatusText,
            onDismiss = { showPruneConfirmDialog = false },
            onConfirm = {
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
            }
        )
    }

    if (pruneResultDialogData != null) {
        ChunkPruneResultDialog(
            result = pruneResultDialogData!!,
            onDismiss = { pruneResultDialogData = null }
        )
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeTabPreviews
@Composable
private fun WorldTabPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        WorldTab(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            status = ServerStatus.RUNNING,
            onGetWorldSummary = { com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleWorldSummary },
            onImportWorld = { _, _, _ -> Result.success("") },
            onExportWorld = { _, _ -> true },
            onResetDimension = { _, _ -> true },
            onPruneChunks = { _, _ -> com.devwithzachary.mineserve.model.ChunkPruneResult(0, 0, 0, 0, 0L, 0L) },
            onNavigateToLiveMap = {},
            initialWorldSummary = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleWorldSummary
        )
    }
}
