package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

object MinecraftCommandRegistry {
    val ROOT_COMMANDS = listOf(
        "/ban", "/ban-ip", "/clear", "/clone", "/deop", "/difficulty", "/effect",
        "/enchant", "/gamemode", "/give", "/help", "/kick", "/kill", "/list",
        "/op", "/pardon", "/reload", "/save-all", "/save-off", "/save-on", "/say",
        "/seed", "/setblock", "/stop", "/teleport", "/tell", "/tellraw", "/time",
        "/tp", "/tps", "/weather", "/whitelist", "/worldborder"
    )

    val SUBCOMMANDS = mapOf(
        "/time set " to listOf("day", "night", "noon", "midnight", "0", "6000", "12000", "18000"),
        "/time " to listOf("set ", "add ", "query "),
        "/weather " to listOf("clear", "rain", "thunder"),
        "/gamemode " to listOf("survival", "creative", "adventure", "spectator"),
        "/difficulty " to listOf("peaceful", "easy", "normal", "hard"),
        "/whitelist " to listOf("on", "off", "list", "add ", "remove ", "reload"),
        "/effect give " to listOf("speed", "slowness", "haste", "strength", "instant_health", "jump_boost", "regeneration", "night_vision"),
        "/effect " to listOf("give ", "clear "),
        "/save-all " to listOf("flush")
    )

    fun getSuggestions(input: String): List<String> {
        val trimmed = input.trimStart()
        if (!trimmed.startsWith("/")) return emptyList()

        for ((prefix, subList) in SUBCOMMANDS) {
            if (trimmed.startsWith(prefix, ignoreCase = true)) {
                val arg = trimmed.removePrefix(prefix)
                return if (arg.isBlank()) {
                    subList
                } else {
                    subList.filter { it.startsWith(arg, ignoreCase = true) }
                }
            }
        }

        val cleanInput = trimmed.lowercase()
        return ROOT_COMMANDS.filter { it.startsWith(cleanInput) }
    }

    fun applySuggestion(currentInput: String, suggestion: String): String {
        for ((prefix, _) in SUBCOMMANDS) {
            if (currentInput.startsWith(prefix, ignoreCase = true)) {
                return "$prefix$suggestion"
            }
        }
        return if (suggestion.endsWith(" ")) suggestion else "$suggestion "
    }
}

@Composable
fun CommandAutoCompleteRibbon(
    commandText: String,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = MinecraftCommandRegistry.getSuggestions(commandText)
    if (suggestions.isEmpty()) return

    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surfaceContainerLow)
            .padding(vertical = 4.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(suggestions) { item ->
                SuggestionChip(
                    onClick = {
                        val updated = MinecraftCommandRegistry.applySuggestion(commandText, item)
                        onSuggestionSelected(updated)
                    },
                    label = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                            color = colorScheme.primary
                        )
                    },
                    shape = ShapeDefaults.Small,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = colorScheme.surfaceContainerHigh,
                        labelColor = colorScheme.primary
                    ),
                    border = null
                )
            }
        }
    }
}
