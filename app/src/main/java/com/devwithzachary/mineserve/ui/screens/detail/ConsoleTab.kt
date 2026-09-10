package com.devwithzachary.mineserve.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.engine.TerminalEmulator
import com.devwithzachary.mineserve.repository.ConsolePreferences
import com.devwithzachary.mineserve.ui.components.CommandAutoCompleteRibbon
import com.devwithzachary.mineserve.ui.components.CustomMacroBar
import com.devwithzachary.mineserve.ui.components.TerminalCanvasView
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

@Composable
fun ConsoleTab(
    emulator: TerminalEmulator,
    refreshTrigger: Long,
    onSendCommand: (String) -> Unit,
    onResizeTerminal: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val consolePrefs = remember { ConsolePreferences(context) }
    var macros by remember { mutableStateOf(consolePrefs.getMacros()) }
    var commandHistory by remember { mutableStateOf(consolePrefs.getCommandHistory()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var commandText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .imePadding()
    ) {
        // Customizable Macro bar above live terminal
        CustomMacroBar(
            macros = macros,
            onMacroClicked = { macro ->
                if (macro.autoExecute) {
                    onSendCommand(macro.command)
                    commandHistory = consolePrefs.addCommandToHistory(macro.command)
                    historyIndex = -1
                } else {
                    commandText = macro.command
                }
            },
            onSaveMacros = { updated ->
                macros = updated
                consolePrefs.saveMacros(updated)
            },
            onResetDefaults = {
                macros = consolePrefs.resetMacrosToDefaults()
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        // Terminal Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            TerminalCanvasView(
                emulator = emulator,
                refreshTrigger = refreshTrigger,
                onResizeTerminal = onResizeTerminal
            )
        }

        // Auto-completion ribbon when typing /
        CommandAutoCompleteRibbon(
            commandText = commandText,
            onSuggestionSelected = { completedText ->
                commandText = completedText
            }
        )

        // Command Input Bar & History Recall
        Surface(
            color = Slate900,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Command History Navigation Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (commandHistory.isNotEmpty()) {
                                val nextIndex = (historyIndex + 1).coerceAtMost(commandHistory.size - 1)
                                historyIndex = nextIndex
                                commandText = commandHistory[nextIndex]
                            }
                        },
                        enabled = commandHistory.isNotEmpty() && historyIndex < commandHistory.size - 1,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Previous Command",
                            tint = if (commandHistory.isNotEmpty() && historyIndex < commandHistory.size - 1) Color.White else Slate800
                        )
                    }

                    IconButton(
                        onClick = {
                            if (historyIndex > 0) {
                                historyIndex--
                                commandText = commandHistory[historyIndex]
                            } else if (historyIndex == 0) {
                                historyIndex = -1
                                commandText = ""
                            }
                        },
                        enabled = historyIndex >= 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Next Command",
                            tint = if (historyIndex >= 0) Color.White else Slate800
                        )
                    }
                }

                OutlinedTextField(
                    value = commandText,
                    onValueChange = {
                        commandText = it
                        historyIndex = -1
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.console_input_placeholder),
                            color = Slate400,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianCardBorder,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldPrimary,
                    modifier = Modifier.size(46.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (commandText.isNotBlank()) {
                                val cmd = commandText.trim()
                                onSendCommand(cmd)
                                commandHistory = consolePrefs.addCommandToHistory(cmd)
                                historyIndex = -1
                                commandText = ""
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.console_send),
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
