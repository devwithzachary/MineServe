package com.devwithzachary.mineserve.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.designsystem.theme.MineServePalette

/**
 * Pure Kotlin formatter converting Minecraft section codes (§0-§f, §l/§o/§n/§m/§r,
 * §x§R§R§G§G§B§B) and ANSI SGR escape sequences into Compose AnnotatedString.
 */
object MinecraftConsoleFormatter {
    private val ANSI_SGR_REGEX = Regex("\u001B\\[([0-9;]*)m")
    private val ANSI_OTHER_REGEX = Regex("\u001B\\[[;?\\d]*[ -/]*[@-~]")

    fun format(
        rawLine: String,
        defaultColor: Color = MineServePalette.McWhite
    ): AnnotatedString {
        if (rawLine.isEmpty()) return AnnotatedString("")
        val normalized = normalizeAnsiToSectionCodes(rawLine)
        return buildAnnotatedString {
            var currentColor = defaultColor
            var bold = false
            var italic = false
            var underline = false
            var strikethrough = false

            var i = 0
            val len = normalized.length
            val buffer = StringBuilder()

            fun flush() {
                if (buffer.isEmpty()) return
                val decoration = when {
                    underline && strikethrough -> TextDecoration.combine(
                        listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                    )
                    underline -> TextDecoration.Underline
                    strikethrough -> TextDecoration.LineThrough
                    else -> null
                }
                pushStyle(
                    SpanStyle(
                        color = currentColor,
                        fontWeight = if (bold) FontWeight.Bold else null,
                        fontStyle = if (italic) FontStyle.Italic else null,
                        textDecoration = decoration
                    )
                )
                append(buffer.toString())
                pop()
                buffer.clear()
            }

            while (i < len) {
                val ch = normalized[i]
                if (ch == '\u00A7' && i + 1 < len) {
                    val code = normalized[i + 1].lowercaseChar()
                    if (code == 'x' && i + 13 < len && isLegacyHexSequence(normalized, i)) {
                        flush()
                        currentColor = parseLegacyHexColor(normalized, i) ?: currentColor
                        bold = false
                        italic = false
                        underline = false
                        strikethrough = false
                        i += 14
                        continue
                    }
                    val mappedColor = mapSectionColor(code)
                    if (mappedColor != null) {
                        flush()
                        currentColor = mappedColor
                        bold = false
                        italic = false
                        underline = false
                        strikethrough = false
                        i += 2
                        continue
                    }
                    when (code) {
                        'l' -> {
                            flush()
                            bold = true
                            i += 2
                            continue
                        }
                        'o' -> {
                            flush()
                            italic = true
                            i += 2
                            continue
                        }
                        'n' -> {
                            flush()
                            underline = true
                            i += 2
                            continue
                        }
                        'm' -> {
                            flush()
                            strikethrough = true
                            i += 2
                            continue
                        }
                        'r' -> {
                            flush()
                            currentColor = defaultColor
                            bold = false
                            italic = false
                            underline = false
                            strikethrough = false
                            i += 2
                            continue
                        }
                        'k' -> {
                            i += 2
                            continue
                        }
                    }
                }
                buffer.append(ch)
                i++
            }
            flush()
        }
    }

    private fun normalizeAnsiToSectionCodes(input: String): String {
        val withCodes = ANSI_SGR_REGEX.replace(input) { match ->
            val params = match.groupValues[1]
                .split(';')
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
            if (params.isEmpty()) return@replace "\u00A7r"
            val sb = StringBuilder()
            for (code in params) {
                when (code) {
                    0 -> sb.append("\u00A7r")
                    1 -> sb.append("\u00A7l")
                    3 -> sb.append("\u00A7o")
                    4 -> sb.append("\u00A7n")
                    9 -> sb.append("\u00A7m")
                    30 -> sb.append("\u00A70")
                    31 -> sb.append("\u00A74")
                    32 -> sb.append("\u00A72")
                    33 -> sb.append("\u00A76")
                    34 -> sb.append("\u00A71")
                    35 -> sb.append("\u00A75")
                    36 -> sb.append("\u00A73")
                    37 -> sb.append("\u00A77")
                    90 -> sb.append("\u00A78")
                    91 -> sb.append("\u00A7c")
                    92 -> sb.append("\u00A7a")
                    93 -> sb.append("\u00A7e")
                    94 -> sb.append("\u00A79")
                    95 -> sb.append("\u00A7d")
                    96 -> sb.append("\u00A7b")
                    97 -> sb.append("\u00A7f")
                }
            }
            sb.toString()
        }
        return ANSI_OTHER_REGEX.replace(withCodes, "")
    }

    private fun isLegacyHexSequence(text: String, startIndex: Int): Boolean {
        for (offset in 2..12 step 2) {
            if (text[startIndex + offset] != '\u00A7') return false
            val hexChar = text[startIndex + offset + 1].lowercaseChar()
            if (hexChar !in '0'..'9' && hexChar !in 'a'..'f') return false
        }
        return true
    }

    private fun parseLegacyHexColor(text: String, startIndex: Int): Color? {
        val hexDigits = buildString(6) {
            for (offset in 3..13 step 2) {
                append(text[startIndex + offset])
            }
        }
        val rgb = hexDigits.toIntOrNull(16) ?: return null
        return Color(0xFF000000.toInt() or rgb)
    }

    private fun mapSectionColor(code: Char): Color? = when (code) {
        '0' -> MineServePalette.McBlack
        '1' -> MineServePalette.McDarkBlue
        '2' -> MineServePalette.McDarkGreen
        '3' -> MineServePalette.McDarkAqua
        '4' -> MineServePalette.McDarkRed
        '5' -> MineServePalette.McDarkPurple
        '6' -> MineServePalette.McGold
        '7' -> MineServePalette.McGray
        '8' -> MineServePalette.McDarkGray
        '9' -> MineServePalette.McBlue
        'a' -> MineServePalette.McGreen
        'b' -> MineServePalette.McAqua
        'c' -> MineServePalette.McRed
        'd' -> MineServePalette.McLightPurple
        'e' -> MineServePalette.McYellow
        'f' -> MineServePalette.McWhite
        else -> null
    }
}

/**
 * Console terminal viewport (`surfaceContainerLowest`, #070A12) with JetBrains Mono,
 * Minecraft color-code rendering, optional command suggestion chips, and an integrated
 * filled M3 command input bar with a trailing send IconButton.
 */
@Composable
fun ConsoleTerminalPane(
    lines: List<String>,
    commandInput: String,
    onCommandInputChange: (String) -> Unit,
    onSendCommand: () -> Unit,
    modifier: Modifier = Modifier,
    inputEnabled: Boolean = true,
    autoScroll: Boolean = true,
    emptyPlaceholder: String = "Server output will appear here...",
    commandPlaceholder: String = "Enter server command (e.g. say hello)...",
    quickCommands: List<String> = emptyList(),
    onQuickCommandClick: (String) -> Unit = { cmd -> onCommandInputChange(cmd) }
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size, autoScroll) {
        if (autoScroll && lines.isNotEmpty()) {
            listState.scrollToItem(lines.lastIndex)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = ShapeDefaults.Medium,
            color = extended.terminalSurface,
            contentColor = extended.terminalOnSurface,
            border = null
        ) {
            if (lines.isEmpty()) {
                Text(
                    text = emptyPlaceholder,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = monoFont,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(lines) { rawLine ->
                            val formatted = remember(rawLine, extended.terminalOnSurface) {
                                MinecraftConsoleFormatter.format(rawLine, extended.terminalOnSurface)
                            }
                            Text(
                                text = formatted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = monoFont,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        if (quickCommands.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { command ->
                    SuggestionChip(
                        onClick = { onQuickCommandClick(command) },
                        label = {
                            Text(
                                text = command,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = ShapeDefaults.Small,
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = colorScheme.surfaceContainerHigh,
                            labelColor = colorScheme.onSurface
                        ),
                        border = null
                    )
                }
            }
        }

        TextField(
            value = commandInput,
            onValueChange = onCommandInputChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = inputEnabled,
            singleLine = true,
            shape = ShapeDefaults.Medium,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = monoFont
            ),
            placeholder = {
                Text(
                    text = commandPlaceholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = monoFont
                    ),
                    color = colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = onSendCommand,
                    enabled = inputEnabled && commandInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send command",
                        tint = if (inputEnabled && commandInput.isNotBlank()) {
                            colorScheme.primary
                        } else {
                            colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (inputEnabled && commandInput.isNotBlank()) {
                        onSendCommand()
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = colorScheme.surfaceContainerHighest,
                disabledContainerColor = colorScheme.surfaceContainerHigh,
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}
