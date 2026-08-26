package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate800

@Composable
fun QuickCommandBar(
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickCommands = listOf(
        "/op " to "OP Player",
        "/deop " to "De-OP",
        "/whitelist on" to "Whitelist ON",
        "/whitelist add " to "Whitelist Player",
        "/save-all" to "Save World",
        "/say Hello World!" to "Broadcast Message",
        "/time set day" to "Day Time",
        "/time set night" to "Night Time",
        "/weather clear" to "Clear Weather",
        "/gamemode creative " to "Creative Mode",
        "/gamemode survival " to "Survival Mode",
        "/kick " to "Kick Player",
        "/ban " to "Ban Player",
        "/stop" to "Stop Server"
    )

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((cmd, label) in quickCommands) {
            SuggestionChip(
                onClick = { onCommandSelected(cmd) },
                label = { Text(text = label, style = MaterialTheme.typography.labelSmall) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Slate800,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = ObsidianCardBorder
                )
            )
        }
    }
}
