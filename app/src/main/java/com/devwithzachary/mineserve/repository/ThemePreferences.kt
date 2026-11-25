package com.devwithzachary.mineserve.repository

import android.content.Context
import com.devwithzachary.mineserve.designsystem.theme.ThemeMode

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("mineserve_ui_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"
    }

    var themeMode: ThemeMode
        get() = when (prefs.getString(KEY_THEME_MODE, "OBSIDIAN_DARK")) {
            "SLATE_LIGHT" -> ThemeMode.SlateLight
            "SYSTEM" -> ThemeMode.System
            else -> ThemeMode.ObsidianDark
        }
        set(value) {
            val raw = when (value) {
                ThemeMode.ObsidianDark -> "OBSIDIAN_DARK"
                ThemeMode.SlateLight -> "SLATE_LIGHT"
                ThemeMode.System -> "SYSTEM"
            }
            prefs.edit().putString(KEY_THEME_MODE, raw).apply()
        }

    var useDynamicColor: Boolean
        get() = prefs.getBoolean(KEY_USE_DYNAMIC_COLOR, false)
        set(value) {
            prefs.edit().putBoolean(KEY_USE_DYNAMIC_COLOR, value).apply()
        }
}
