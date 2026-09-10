package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate900

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

        // Check for specific subcommands first
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

        // Check root command match
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

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate900.copy(alpha = 0.95f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in suggestions) {
                SuggestionChip(
                    onClick = {
                        val updated = MinecraftCommandRegistry.applySuggestion(commandText, item)
                        onSuggestionSelected(updated)
                    },
                    label = {
                        Text(
                            text = item,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = EmeraldPrimary
                        )
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF132030),
                        labelColor = EmeraldPrimary
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = ObsidianCardBorder
                    )
                )
            }
        }
    }
}
