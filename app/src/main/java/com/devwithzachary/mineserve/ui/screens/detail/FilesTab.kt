package com.devwithzachary.mineserve.ui.screens.detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.CrashDiagnosticReport
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.QuickFixAction
import com.devwithzachary.mineserve.ui.components.editor.AdvancedCodeEditor
import com.devwithzachary.mineserve.ui.screens.detail.files.CrashDiagnosticSheet
import com.devwithzachary.mineserve.ui.screens.detail.files.CreateFileDialog
import com.devwithzachary.mineserve.ui.screens.detail.files.CreateFolderDialog
import com.devwithzachary.mineserve.ui.screens.detail.files.DeleteFileDialog
import com.devwithzachary.mineserve.ui.screens.detail.files.FileListItem
import com.devwithzachary.mineserve.ui.screens.detail.files.RenameFileDialog
import com.devwithzachary.mineserve.ui.screens.detail.files.UnzipArchiveDialog
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesTab(
    server: MinecraftServer,
    onListDirectory: suspend (String) -> List<FileEntry>,
    onCreateFile: suspend (String, String, String) -> Boolean,
    onCreateDirectory: suspend (String, String) -> Boolean,
    onDeleteFile: suspend (String) -> Boolean,
    onRenameFile: suspend (String, String) -> Boolean,
    onDuplicateFile: suspend (String) -> Boolean,
    onReadFile: suspend (String) -> String,
    onWriteFile: suspend (String, String) -> Boolean,
    onImportFile: suspend (String, Uri) -> Boolean,
    onExportFile: suspend (String) -> Boolean,
    onUnzipFile: suspend (String, String, Boolean) -> Result<Int> = { _, _, _ -> Result.success(0) },
    onSearchFiles: suspend (String) -> List<FileEntry>,
    onAnalyzeCrash: suspend () -> CrashDiagnosticReport?,
    onApplyQuickFix: suspend (QuickFixAction) -> Boolean,
    initialFiles: List<FileEntry>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = androidx.compose.ui.platform.LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    // Navigation & Path state
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf(initialFiles ?: emptyList()) }
    var isLoading by remember { mutableStateOf(initialFiles == null) }

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FileEntry>>(emptyList()) }

    // Dialog states
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<FileEntry?>(null) }
    var fileToDelete by remember { mutableStateOf<FileEntry?>(null) }
    var fileToUnzip by remember { mutableStateOf<FileEntry?>(null) }
    var isUnzipping by remember { mutableStateOf(false) }

    // Active Code Editor state
    var editingFile by remember { mutableStateOf<FileEntry?>(null) }
    var editingFileContent by remember { mutableStateOf("") }

    // Crash Diagnostics state
    var crashReport by remember { mutableStateOf<CrashDiagnosticReport?>(null) }
    var showCrashModal by remember { mutableStateOf(false) }
    var isAnalyzingCrash by remember { mutableStateOf(false) }

    fun refreshFiles() {
        scope.launch {
            isLoading = true
            files = onListDirectory(currentPath)
            isLoading = false
        }
    }

    LaunchedEffect(currentPath) {
        if (!(isInspection && initialFiles != null)) {
            refreshFiles()
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isLoading = true
                var successCount = 0
                for (uri in uris) {
                    val success = onImportFile(currentPath, uri)
                    if (success) successCount++
                }
                isLoading = false
                if (successCount > 0) {
                    val msg = if (uris.size == 1) "File imported successfully" else "Imported $successCount of ${uris.size} files"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    refreshFiles()
                } else {
                    Toast.makeText(context, "Failed to import file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Full screen Code Editor
    if (editingFile != null) {
        AdvancedCodeEditor(
            file = editingFile!!,
            initialContent = editingFileContent,
            onSave = { content ->
                onWriteFile(editingFile!!.relativePath, content)
            },
            onClose = {
                editingFile = null
                refreshFiles()
            }
        )
        return
    }

    if (showNewFileDialog) {
        CreateFileDialog(
            onDismiss = { showNewFileDialog = false },
            onConfirm = { name ->
                showNewFileDialog = false
                scope.launch {
                    val ok = onCreateFile(currentPath, name, "")
                    if (ok) {
                        refreshFiles()
                    } else {
                        Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showNewFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onConfirm = { name ->
                showNewFolderDialog = false
                scope.launch {
                    val ok = onCreateDirectory(currentPath, name)
                    if (ok) {
                        refreshFiles()
                    } else {
                        Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (fileToRename != null) {
        RenameFileDialog(
            target = fileToRename!!,
            onDismiss = { fileToRename = null },
            onConfirm = { newName ->
                val target = fileToRename!!
                fileToRename = null
                scope.launch {
                    val ok = onRenameFile(target.relativePath, newName)
                    if (ok) {
                        refreshFiles()
                    } else {
                        Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (fileToDelete != null) {
        val target = fileToDelete!!
        DeleteFileDialog(
            target = target,
            onDismiss = { fileToDelete = null },
            onConfirm = {
                fileToDelete = null
                scope.launch {
                    val ok = onDeleteFile(target.relativePath)
                    if (ok) {
                        refreshFiles()
                    } else {
                        Toast.makeText(context, "Failed to delete ${target.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (fileToUnzip != null) {
        val target = fileToUnzip!!
        UnzipArchiveDialog(
            target = target,
            currentPath = currentPath,
            onDismiss = { fileToUnzip = null },
            onConfirm = { destPath, deleteZipAfter ->
                fileToUnzip = null
                scope.launch {
                    isUnzipping = true
                    val result = onUnzipFile(target.relativePath, destPath, deleteZipAfter)
                    isUnzipping = false
                    result.fold(
                        onSuccess = { count ->
                            val msg = if (count == 1) "Extracted 1 file successfully" else "Extracted $count files successfully"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            refreshFiles()
                        },
                        onFailure = { error ->
                            Toast.makeText(context, "Unzip failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                            refreshFiles()
                        }
                    )
                }
            }
        )
    }

    if (isUnzipping) {
        MineServeDialog(
            onDismissRequest = {},
            title = "Extracting Archive",
            confirmLabel = "Please Wait",
            onConfirm = {},
            confirmEnabled = false
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Extracting files, please wait...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCrashModal && crashReport != null) {
        CrashDiagnosticSheet(
            report = crashReport!!,
            onDismiss = { showCrashModal = false },
            onApplyQuickFix = { fix ->
                scope.launch {
                    val ok = onApplyQuickFix(fix)
                    showCrashModal = false
                    if (ok) {
                        Toast.makeText(context, "Fix applied successfully", Toast.LENGTH_SHORT).show()
                        refreshFiles()
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Action Bar & Diagnostic Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { currentPath = "" },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "Root",
                        tint = if (currentPath.isEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (currentPath.isNotEmpty()) {
                    val segments = currentPath.split("/").filter { it.isNotBlank() }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(segments) { segment ->
                            Text(
                                text = "/",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = monoFont,
                                color = MaterialTheme.colorScheme.outline
                            )
                            TextButton(
                                onClick = {
                                    val index = segments.indexOf(segment)
                                    currentPath = segments.take(index + 1).joinToString("/")
                                }
                            ) {
                                Text(
                                    text = segment,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = monoFont,
                                    color = if (segment == segments.last()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (segment == segments.last()) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "root",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = monoFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            isAnalyzingCrash = true
                            val result = onAnalyzeCrash()
                            isAnalyzingCrash = false
                            if (result != null) {
                                crashReport = result
                                showCrashModal = true
                            } else {
                                Toast.makeText(context, "No fatal crash errors detected in server logs", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = extColors.warningContainer,
                        contentColor = extColors.onWarningContainer
                    ),
                    shape = ShapeDefaults.Small,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (isAnalyzingCrash) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = extColors.onWarningContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Diagnostics", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = if (isSearchActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(onClick = { refreshFiles() }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live Search Bar
        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.isNotBlank()) {
                        scope.launch {
                            searchResults = onSearchFiles(it)
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                placeholder = { Text("Search files & folders...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action Buttons Row: New File, New Folder, Import
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { showNewFileDialog = true },
                shape = ShapeDefaults.Small,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New File",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = { showNewFolderDialog = true },
                shape = ShapeDefaults.Small,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreateNewFolder,
                    contentDescription = null,
                    tint = extColors.warning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New Folder",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = { importFileLauncher.launch("*/*") },
                shape = ShapeDefaults.Small,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Import",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // File List Card
        MineServeCard(
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val displayList = if (isSearchActive && searchQuery.isNotBlank()) searchResults else files

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MineServeEmptyState(
                            icon = Icons.Rounded.FolderOff,
                            title = if (isSearchActive) "No matching files found" else "This directory is empty",
                            supportingText = if (isSearchActive) "Try a different filename or path." else "Create a file, folder, or import an archive."
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (currentPath.isNotEmpty() && !isSearchActive) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val parent = currentPath.substringBeforeLast('/', "")
                                            currentPath = parent
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Up",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "..",
                                        fontFamily = monoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            }
                        }

                        itemsIndexed(displayList) { index, entry ->
                            FileListItem(
                                entry = entry,
                                showDivider = index < displayList.lastIndex,
                                onOpenFolder = {
                                    currentPath = entry.relativePath
                                },
                                onOpenFile = {
                                    if (entry.isEditable || entry.isLog) {
                                        scope.launch {
                                            isLoading = true
                                            val content = onReadFile(entry.relativePath)
                                            isLoading = false
                                            editingFile = entry
                                            editingFileContent = content
                                        }
                                    } else if (entry.extension.equals("zip", ignoreCase = true) || entry.isArchive) {
                                        fileToUnzip = entry
                                    } else {
                                        Toast.makeText(context, "Binary file: Use Export or Duplicate to manage", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRename = {
                                    fileToRename = entry
                                },
                                onDuplicate = {
                                    scope.launch {
                                        val ok = onDuplicateFile(entry.relativePath)
                                        if (ok) refreshFiles()
                                    }
                                },
                                onExport = {
                                    scope.launch {
                                        val ok = onExportFile(entry.relativePath)
                                        if (ok) {
                                            Toast.makeText(context, "Exported to Downloads/MineServe", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDelete = {
                                    fileToDelete = entry
                                },
                                onUnzip = {
                                    fileToUnzip = entry
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeTabPreviews
@Composable
private fun FilesTabPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        FilesTab(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            onListDirectory = { emptyList() },
            onCreateFile = { _, _, _ -> true },
            onCreateDirectory = { _, _ -> true },
            onDeleteFile = { true },
            onRenameFile = { _, _ -> true },
            onDuplicateFile = { true },
            onReadFile = { "" },
            onWriteFile = { _, _ -> true },
            onImportFile = { _, _ -> true },
            onExportFile = { true },
            onSearchFiles = { emptyList() },
            onAnalyzeCrash = { null },
            onApplyQuickFix = { true },
            initialFiles = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleFiles
        )
    }
}
