package com.devwithzachary.mineserve.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.devwithzachary.mineserve.designsystem.theme.MineServeTheme as SharedMineServeTheme
import com.devwithzachary.mineserve.designsystem.theme.ThemeMode

@Composable
fun MineServeTheme(
    darkTheme: Boolean = true, // Default to MineServe Obsidian dark theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeMode = if (darkTheme) ThemeMode.ObsidianDark else ThemeMode.SlateLight
    CompositionLocalProvider(LocalLayoutManager provides LayoutManager) {
        SharedMineServeTheme(
            themeMode = themeMode,
            useDynamicColor = dynamicColor,
            content = content
        )
    }
}

@Composable
fun MineServeTheme(
    themeMode: ThemeMode,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutManager provides LayoutManager) {
        SharedMineServeTheme(
            themeMode = themeMode,
            useDynamicColor = useDynamicColor,
            content = content
        )
    }
}
