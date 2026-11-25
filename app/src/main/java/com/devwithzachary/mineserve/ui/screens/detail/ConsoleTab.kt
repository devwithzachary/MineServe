package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.ConsoleTerminalPane
import com.devwithzachary.mineserve.engine.TerminalEmulator
import com.devwithzachary.mineserve.repository.ConsolePreferences
import com.devwithzachary.mineserve.ui.components.CommandAutoCompleteRibbon
import com.devwithzachary.mineserve.ui.components.CustomMacroBar

@Composable
fun ConsoleTab(
    emulator: TerminalEmulator,
    refreshTrigger: Long,
    onSendCommand: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onResizeTerminal: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val uiPrefs = remember(context) {
        context.getSharedPreferences("mineserve_ui_prefs", Context.MODE_PRIVATE)
    }
    val keepScreenAwake = remember { uiPrefs.getBoolean("keep_screen_awake", true) }
    val view = LocalView.current
    DisposableEffect(view, keepScreenAwake) {
        val previousKeepScreenOn = view.keepScreenOn
        if (keepScreenAwake) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    val consolePrefs = remember { ConsolePreferences(context) }
    var macros by remember { mutableStateOf(consolePrefs.getMacros()) }
    var commandText by remember { mutableStateOf("") }

    val logLines = remember(emulator, refreshTrigger) {
        emulator.getRecentLines(maxLines = 500)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .imePadding()
    ) {
        // Horizontally scrollable M3 SuggestionChip quick-action bar
        CustomMacroBar(
            macros = macros,
            onMacroClicked = { macro ->
                if (macro.autoExecute) {
                    onSendCommand(macro.command)
                    consolePrefs.addCommandToHistory(macro.command)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 6.dp)
        )

        CommandAutoCompleteRibbon(
            commandText = commandText,
            onSuggestionSelected = { completedText ->
                commandText = completedText
            }
        )

        ConsoleTerminalPane(
            lines = logLines,
            commandInput = commandText,
            onCommandInputChange = { commandText = it },
            onSendCommand = {
                val cmd = commandText.trim()
                if (cmd.isNotEmpty()) {
                    onSendCommand(cmd)
                    consolePrefs.addCommandToHistory(cmd)
                    commandText = ""
                }
            },
            commandPlaceholder = stringResource(R.string.console_input_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .padding(bottom = 8.dp)
        )
    }
}
