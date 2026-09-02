package com.devwithzachary.mineserve.ui.components.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.BackHandler
import com.devwithzachary.mineserve.model.FileEntry
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedCodeEditor(
    file: FileEntry,
    initialContent: String,
    onSave: suspend (String) -> Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(initialContent)) }
    var initialLoadedContent by remember { mutableStateOf(initialContent) }

    val hasUnsavedChanges by remember {
        derivedStateOf { textFieldValue.text != initialLoadedContent }
    }

    // Undo / Redo stacks
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    var lastRecordedText by remember { mutableStateOf(initialContent) }

    // Search and replace states
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    // Saving states
    var isSaving by remember { mutableStateOf(false) }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Synchronized scroll states
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // IME / Keyboard visibility
    val isImeVisible = WindowInsets.isImeVisible

    fun getLineAndColumn(text: String, charOffset: Int): Pair<Int, Int> {
        if (charOffset <= 0 || text.isEmpty()) return Pair(0, 0)
        val safeOffset = charOffset.coerceIn(0, text.length)
        var line = 0
        var lastNewline = -1
        for (i in 0 until safeOffset) {
            if (text[i] == '\n') {
                line++
                lastNewline = i
            }
        }
        val col = safeOffset - (lastNewline + 1)
        return Pair(line, col)
    }

    val (cursorLine, cursorCol) = remember(textFieldValue.text, textFieldValue.selection) {
        getLineAndColumn(textFieldValue.text, textFieldValue.selection.min)
    }

    // Auto-scroll when keyboard opens to ensure tapped text is visible in upper portion of viewport
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            delay(100) // Allow IME resize animation to settle
            val lineHeightPx = with(density) { 20.sp.toPx() }
            val topPaddingPx = with(density) { 8.dp.toPx() }
            val lineTopY = topPaddingPx + cursorLine * lineHeightPx
            val viewportH = verticalScrollState.viewportSize
            if (viewportH > 0) {
                val targetScroll = (lineTopY - (viewportH / 3f))
                    .coerceIn(0f, verticalScrollState.maxValue.toFloat())
                    .toInt()
                verticalScrollState.animateScrollTo(targetScroll)
            }
        }
    }

    // Auto-scroll when cursor line changes (user clicks text or types) outside current viewport
    LaunchedEffect(cursorLine) {
        val lineHeightPx = with(density) { 20.sp.toPx() }
        val topPaddingPx = with(density) { 8.dp.toPx() }
        val lineTopY = topPaddingPx + cursorLine * lineHeightPx
        val lineBottomY = lineTopY + lineHeightPx
        val currentScroll = verticalScrollState.value
        val viewportH = verticalScrollState.viewportSize

        if (viewportH > 0) {
            val bufferPx = with(density) { 32.dp.toPx() }
            if (lineTopY < currentScroll + bufferPx || lineBottomY > currentScroll + viewportH - bufferPx) {
                val targetScroll = (lineTopY - (viewportH / 3f))
                    .coerceIn(0f, verticalScrollState.maxValue.toFloat())
                    .toInt()
                verticalScrollState.animateScrollTo(targetScroll)
            }
        }
    }

    fun pushHistory(newText: String) {
        if (newText != lastRecordedText) {
            undoStack.add(lastRecordedText)
            redoStack.clear()
            lastRecordedText = newText
            if (undoStack.size > 50) {
                undoStack.removeAt(0)
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(textFieldValue.text)
            lastRecordedText = prev
            textFieldValue = TextFieldValue(prev)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(textFieldValue.text)
            lastRecordedText = next
            textFieldValue = TextFieldValue(next)
        }
    }

    fun handleBack() {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onClose()
        }
    }

    BackHandler(onBack = { handleBack() })

    // Find all matches
    val searchMatches = remember(searchQuery, textFieldValue.text) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val matches = mutableListOf<IntRange>()
            var index = textFieldValue.text.indexOf(searchQuery, ignoreCase = true)
            while (index >= 0) {
                matches.add(index until index + searchQuery.length)
                index = textFieldValue.text.indexOf(searchQuery, index + 1, ignoreCase = true)
            }
            matches
        }
    }

    if (searchMatches.isNotEmpty() && currentMatchIndex >= searchMatches.size) {
        currentMatchIndex = 0
    }

    // Auto-scroll to search match when currentMatchIndex changes
    LaunchedEffect(currentMatchIndex, searchMatches) {
        if (searchMatches.isNotEmpty() && currentMatchIndex in searchMatches.indices) {
            val matchOffset = searchMatches[currentMatchIndex].first
            val (matchLine, _) = getLineAndColumn(textFieldValue.text, matchOffset)
            val lineHeightPx = with(density) { 20.sp.toPx() }
            val topPaddingPx = with(density) { 8.dp.toPx() }
            val lineTopY = topPaddingPx + matchLine * lineHeightPx
            val viewportH = verticalScrollState.viewportSize
            if (viewportH > 0) {
                val targetScroll = (lineTopY - (viewportH / 3f))
                    .coerceIn(0f, verticalScrollState.maxValue.toFloat())
                    .toInt()
                verticalScrollState.animateScrollTo(targetScroll)
            }
        }
    }

    // Custom Visual Transformation for Syntax Highlighting & Search Highlighting
    val syntaxTransformation = remember(file.extension, searchQuery, searchMatches) {
        VisualTransformation { original ->
            val highlighted = SyntaxHighlighter.highlight(original.text, file.extension)
            val finalAnnotated = if (searchQuery.isNotBlank() && searchMatches.isNotEmpty()) {
                buildAnnotatedString {
                    append(highlighted)
                    searchMatches.forEachIndexed { i, range ->
                        val bg = if (i == currentMatchIndex) Color(0xFFF59E0B) else Color(0x66F59E0B)
                        addStyle(SpanStyle(background = bg, color = Color.Black), range.first, range.last + 1)
                    }
                }
            } else {
                highlighted
            }
            TransformedText(finalAnnotated, OffsetMapping.Identity)
        }
    }

    // Line Count Calculation
    val lineCount by remember(textFieldValue.text) {
        derivedStateOf {
            textFieldValue.text.count { it == '\n' } + 1
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "You have unsaved changes in ${file.name}. Do you want to discard them?",
                    color = Slate400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Discard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = ObsidianCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (hasUnsavedChanges) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(GoldYellow, CircleShape)
                                )
                            }
                        }
                        Text(
                            text = file.relativePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Find & Replace Toggle
                    IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                        Icon(
                            if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchOpen) EmeraldPrimary else Color.White
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val success = onSave(textFieldValue.text)
                                isSaving = false
                                if (success) {
                                    initialLoadedContent = textFieldValue.text
                                    saveStatusMessage = "Saved successfully"
                                } else {
                                    saveStatusMessage = "Failed to save file"
                                }
                                delay(2500)
                                saveStatusMessage = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        bottomBar = {
            // Status bar footer
            Surface(
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ln ${cursorLine + 1}, Col ${cursorCol + 1}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldLight
                        )
                        Text(
                            text = "Lines: $lineCount",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate400
                        )
                        Text(
                            text = "Chars: ${textFieldValue.text.length}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (saveStatusMessage != null) {
                            Text(
                                text = saveStatusMessage ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (saveStatusMessage?.contains("Failed") == true) RedstoneLight else EmeraldLight
                            )
                        } else {
                            Text(
                                text = file.extension.uppercase().ifBlank { "TEXT" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                            Text(
                                text = "UTF-8",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        },
        containerColor = Slate950,
        modifier = modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search & Replace Bar
            AnimatedVisibility(
                visible = isSearchOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = ObsidianCard,
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Find Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    currentMatchIndex = 0
                                },
                                placeholder = { Text("Find in file...", fontSize = 13.sp, color = Slate400) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        Text(
                                            text = if (searchMatches.isEmpty()) "0 of 0" else "${currentMatchIndex + 1} of ${searchMatches.size}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (searchMatches.isEmpty()) RedstoneLight else GoldYellow,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                },
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate700
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    if (searchMatches.isNotEmpty()) {
                                        currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else searchMatches.size - 1
                                    }
                                },
                                enabled = searchMatches.isNotEmpty()
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match", tint = Color.White)
                            }

                            IconButton(
                                onClick = {
                                    if (searchMatches.isNotEmpty()) {
                                        currentMatchIndex = if (currentMatchIndex < searchMatches.size - 1) currentMatchIndex + 1 else 0
                                    }
                                },
                                enabled = searchMatches.isNotEmpty()
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match", tint = Color.White)
                            }
                        }

                        // Replace Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = replaceQuery,
                                onValueChange = { replaceQuery = it },
                                placeholder = { Text("Replace with...", fontSize = 13.sp, color = Slate400) },
                                leadingIcon = {
                                    Icon(Icons.Default.FindReplace, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                                },
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate700
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    if (searchMatches.isNotEmpty() && currentMatchIndex in searchMatches.indices) {
                                        val match = searchMatches[currentMatchIndex]
                                        val newText = textFieldValue.text.replaceRange(match, replaceQuery)
                                        pushHistory(newText)
                                        textFieldValue = TextFieldValue(newText)
                                    }
                                },
                                enabled = searchMatches.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Replace", fontSize = 12.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    if (searchQuery.isNotEmpty()) {
                                        val newText = textFieldValue.text.replace(searchQuery, replaceQuery, ignoreCase = true)
                                        pushHistory(newText)
                                        textFieldValue = TextFieldValue(newText)
                                    }
                                },
                                enabled = searchMatches.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("All", fontSize = 12.sp, color = EmeraldLight)
                            }
                        }
                    }
                }
            }

            // Editor Canvas with Line Numbers Gutter
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Slate950),
                border = BorderStroke(1.dp, Slate900),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                ) {
                    // Line numbers gutter
                    Column(
                        modifier = Modifier
                            .background(Slate900.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..lineCount) {
                            Text(
                                text = "$i",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Slate700
                            )
                        }
                    }

                    // Vertical divider line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Slate800)
                    )

                    // Code text editing area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                pushHistory(it.text)
                                textFieldValue = it
                            },
                            visualTransformation = syntaxTransformation,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFFE2E8F0)
                            ),
                            cursorBrush = SolidColor(EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
