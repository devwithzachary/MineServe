package com.devwithzachary.mineserve.repository

import android.content.Context
import android.content.SharedPreferences
import com.devwithzachary.mineserve.model.ConsoleMacro
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConsolePreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "mineserve_console_prefs"
        private const val KEY_MACROS_JSON = "custom_macros_json"
        private const val KEY_HISTORY_JSON = "command_history_json"
        private const val MAX_HISTORY = 60

        val DEFAULT_MACROS = listOf(
            ConsoleMacro(id = "1", label = "Day", command = "/time set day", autoExecute = true),
            ConsoleMacro(id = "2", label = "Clear Weather", command = "/weather clear", autoExecute = true),
            ConsoleMacro(id = "3", label = "Save World", command = "/save-all", autoExecute = true),
            ConsoleMacro(id = "4", label = "Check TPS", command = "/tps", autoExecute = true),
            ConsoleMacro(id = "5", label = "Whitelist ON", command = "/whitelist on", autoExecute = true),
            ConsoleMacro(id = "6", label = "OP Player", command = "/op ", autoExecute = false),
            ConsoleMacro(id = "7", label = "Creative", command = "/gamemode creative ", autoExecute = false),
            ConsoleMacro(id = "8", label = "Survival", command = "/gamemode survival ", autoExecute = false),
            ConsoleMacro(id = "9", label = "Broadcast", command = "/say Hello World!", autoExecute = false),
            ConsoleMacro(id = "10", label = "Stop Server", command = "/stop", autoExecute = true)
        )
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun getMacros(): List<ConsoleMacro> {
        val raw = prefs.getString(KEY_MACROS_JSON, null) ?: return DEFAULT_MACROS
        return try {
            val list = json.decodeFromString<List<ConsoleMacro>>(raw)
            if (list.isEmpty()) DEFAULT_MACROS else list
        } catch (_: Exception) {
            DEFAULT_MACROS
        }
    }

    fun saveMacros(macros: List<ConsoleMacro>) {
        try {
            val serialized = json.encodeToString(macros)
            prefs.edit().putString(KEY_MACROS_JSON, serialized).apply()
        } catch (_: Exception) {}
    }

    fun resetMacrosToDefaults(): List<ConsoleMacro> {
        saveMacros(DEFAULT_MACROS)
        return DEFAULT_MACROS
    }

    fun getCommandHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addCommandToHistory(command: String): List<String> {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return getCommandHistory()

        val history = getCommandHistory().toMutableList()
        // Avoid identical consecutive duplicate
        if (history.isNotEmpty() && history.first() == trimmed) {
            return history
        }
        history.remove(trimmed)
        history.add(0, trimmed)
        val trimmedHistory = history.take(MAX_HISTORY)

        try {
            val serialized = json.encodeToString(trimmedHistory)
            prefs.edit().putString(KEY_HISTORY_JSON, serialized).apply()
        } catch (_: Exception) {}

        return trimmedHistory
    }

    fun clearCommandHistory() {
        prefs.edit().remove(KEY_HISTORY_JSON).apply()
    }
}
