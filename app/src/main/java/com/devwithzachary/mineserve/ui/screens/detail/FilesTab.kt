package com.devwithzachary.mineserve.ui.screens.detail

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.CrashDiagnosticReport
import com.devwithzachary.mineserve.model.CrashSeverity
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.QuickFixAction
import com.devwithzachary.mineserve.model.QuickFixType
import com.devwithzachary.mineserve.ui.components.editor.AdvancedCodeEditor
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    onSearchFiles: suspend (String) -> List<FileEntry>,
    onAnalyzeCrash: suspend () -> CrashDiagnosticReport?,
    onApplyQuickFix: suspend (QuickFixAction) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Navigation & Path state
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FileEntry>>(emptyList()) }

    // Dialog states
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var fileToRename by remember { mutableStateOf<FileEntry?>(null) }
    var renameTargetName by remember { mutableStateOf("") }

    var fileToDelete by remember { mutableStateOf<FileEntry?>(null) }

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
        refreshFiles()
    }

    // Import file launcher
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val success = onImportFile(currentPath, uri)
                isLoading = false
                if (success) {
                    Toast.makeText(context, "File imported successfully", Toast.LENGTH_SHORT).show()
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

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New File", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter file name with extension (e.g. config.yml):", color = Slate400, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = { Text("filename.yml", color = Slate400) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newFileName.trim()
                        if (name.isNotBlank()) {
                            showNewFileDialog = false
                            newFileName = ""
                            scope.launch {
                                val ok = onCreateFile(currentPath, name, "")
                                if (ok) {
                                    refreshFiles()
                                } else {
                                    Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter folder name:", color = Slate400, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("folder_name", color = Slate400) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isNotBlank()) {
                            showNewFolderDialog = false
                            newFolderName = ""
                            scope.launch {
                                val ok = onCreateDirectory(currentPath, name)
                                if (ok) {
                                    refreshFiles()
                                } else {
                                    Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Rename Dialog
    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename ${if (fileToRename!!.isDirectory) "Folder" else "File"}", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameTargetName,
                    onValueChange = { renameTargetName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = fileToRename!!
                        val newName = renameTargetName.trim()
                        if (newName.isNotBlank() && newName != target.name) {
                            fileToRename = null
                            scope.launch {
                                val ok = onRenameFile(target.relativePath, newName)
                                if (ok) {
                                    refreshFiles()
                                } else {
                                    Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            fileToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = RedstoneRed, modifier = Modifier.size(32.dp))
            },
            title = { Text("Delete ${target.name}?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    if (target.isDirectory) "This will permanently delete this folder and all its contents."
                    else "This will permanently delete this file from the server.",
                    color = Slate400,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        fileToDelete = null
                        scope.launch {
                            val ok = onDeleteFile(target.relativePath)
                            if (ok) {
                                refreshFiles()
                            } else {
                                Toast.makeText(context, "Failed to delete ${target.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Crash Diagnostics Modal BottomSheet
    if (showCrashModal && crashReport != null) {
        val report = crashReport!!
        ModalBottomSheet(
            onDismissRequest = { showCrashModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ObsidianCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    when (report.severity) {
                                        CrashSeverity.CRITICAL -> RedstoneLight.copy(alpha = 0.2f)
                                        CrashSeverity.WARNING -> GoldYellow.copy(alpha = 0.2f)
                                        CrashSeverity.INFO -> DiamondLight.copy(alpha = 0.2f)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (report.severity == CrashSeverity.CRITICAL) Icons.Default.Warning else Icons.Default.BugReport,
                                contentDescription = null,
                                tint = when (report.severity) {
                                    CrashSeverity.CRITICAL -> RedstoneRed
                                    CrashSeverity.WARNING -> GoldYellow
                                    CrashSeverity.INFO -> DiamondCyan
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = report.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Source: ${report.sourceFile}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Summary & Explanation
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = report.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = report.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Log Snippet
                if (report.logSnippet.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate950,
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Relevant Log Snippet:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = report.logSnippet,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFFF1F5F9),
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Suggested Fixes
                if (report.suggestedFixes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Suggested 1-Tap Fixes:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        report.suggestedFixes.forEach { fix ->
                            Button(
                                onClick = {
                                    scope.launch {
                                        val ok = onApplyQuickFix(fix)
                                        showCrashModal = false
                                        if (ok) {
                                            Toast.makeText(context, "Fix applied successfully", Toast.LENGTH_SHORT).show()
                                            refreshFiles()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = fix.label,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Action Bar & Diagnostic Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Breadcrumbs Bar
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
                        imageVector = Icons.Default.Home,
                        contentDescription = "Root",
                        tint = if (currentPath.isEmpty()) EmeraldPrimary else Slate400,
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
                            Text("/", color = Slate700, fontSize = 12.sp)
                            TextButton(
                                onClick = {
                                    val index = segments.indexOf(segment)
                                    currentPath = segments.take(index + 1).joinToString("/")
                                }
                            ) {
                                Text(
                                    text = segment,
                                    color = if (segment == segments.last()) EmeraldLight else Slate400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = if (segment == segments.last()) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "root",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }
            }

            // Right: Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crash Analyzer Button
                Button(
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
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isAnalyzingCrash) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GoldYellow)
                    } else {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Diagnostics", color = GoldYellow, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchActive) EmeraldPrimary else Slate400
                    )
                }

                IconButton(onClick = { refreshFiles() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Slate400)
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
                placeholder = { Text("Search files & folders...", fontSize = 13.sp, color = Slate400) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = Slate800
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action Buttons Row: New File, New Folder, Import
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showNewFileDialog = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New File", color = Color.White, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { showNewFolderDialog = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Folder", color = Color.White, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { importFileLauncher.launch("*/*") },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = DiamondCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import", color = Color.White, fontSize = 12.sp)
            }
        }

        // File List Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmeraldPrimary)
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isSearchActive) "No matching files found" else "This directory is empty",
                                color = Slate400,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        // Parent directory jump item if inside subdirectory
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
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Up",
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "..",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }
                                HorizontalDivider(color = Slate900)
                            }
                        }

                        items(displayList) { entry ->
                            FileListItem(
                                entry = entry,
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
                                    } else {
                                        Toast.makeText(context, "Binary file: Use Export or Duplicate to manage", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRename = {
                                    fileToRename = entry
                                    renameTargetName = entry.name
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
                                }
                            )
                            HorizontalDivider(color = Slate900.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    entry: FileEntry,
    onOpenFolder: () -> Unit,
    onOpenFile: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val icon: ImageVector = when {
        entry.isDirectory -> Icons.Default.Folder
        entry.extension in setOf("yml", "yaml", "json", "toml", "properties", "cfg", "conf") -> Icons.Default.Code
        entry.isLog -> Icons.Default.Description
        entry.isArchive -> Icons.Default.FolderZip
        entry.isWorldRegion -> Icons.Default.Public
        entry.extension == "jar" -> Icons.Default.Extension
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    val iconColor = when {
        entry.isDirectory -> GoldYellow
        entry.extension in setOf("yml", "yaml", "json", "toml", "properties") -> EmeraldPrimary
        entry.isLog -> Slate400
        entry.isArchive -> DiamondCyan
        entry.extension == "jar" -> Color(0xFFC084FC)
        else -> Slate400
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    val formattedDate = remember(entry.lastModified) {
        if (entry.lastModified > 0) dateFormat.format(Date(entry.lastModified)) else ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (entry.isDirectory) onOpenFolder() else onOpenFile()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry.isDirectory) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontSize = 11.sp
                    )
                    if (formattedDate.isNotBlank()) {
                        Text("•", color = Slate700, fontSize = 10.sp)
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Slate400
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (!entry.isDirectory && (entry.isEditable || entry.isLog)) {
                    DropdownMenuItem(
                        text = { Text("Edit in Code Editor") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary) },
                        onClick = {
                            showMenu = false
                            onOpenFile()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = Slate400) },
                    onClick = {
                        showMenu = false
                        onRename()
                    }
                )
                if (!entry.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Slate400) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export to Downloads") },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Slate400) },
                        onClick = {
                            showMenu = false
                            onExport()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = RedstoneRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RedstoneRed) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
