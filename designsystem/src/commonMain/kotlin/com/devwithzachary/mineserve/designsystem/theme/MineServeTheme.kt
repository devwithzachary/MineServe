package com.devwithzachary.mineserve.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class ThemeMode {
    ObsidianDark,
    SlateLight,
    System
}

@Composable
expect fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    useDynamicColor: Boolean
): ColorScheme

@Composable
expect fun PlatformThemeDecorator(
    darkTheme: Boolean,
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
)

/**
 * Unified Material Design 3 theme for MineServe across Android and Desktop.
 * Defaults to ObsidianDark with dynamic colors disabled so the Obsidian & Ore
 * brand palette is preserved out of the box.
 */
@Composable
fun MineServeTheme(
    themeMode: ThemeMode = ThemeMode.ObsidianDark,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.ObsidianDark -> true
        ThemeMode.SlateLight -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val colorScheme = resolvePlatformColorScheme(
        darkTheme = darkTheme,
        useDynamicColor = useDynamicColor
    )
    val extendedColors = if (darkTheme) DarkMineServeExtendedColors else LightMineServeExtendedColors
    val typography = rememberMineServeTypography()
    val monoFontFamily = rememberJetBrainsMonoFontFamily()

    CompositionLocalProvider(
        LocalMineServeColors provides extendedColors,
        LocalMineServeMonoFontFamily provides monoFontFamily
    ) {
        PlatformThemeDecorator(
            darkTheme = darkTheme,
            colorScheme = colorScheme
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = typography,
                shapes = MineServeShapes,
                content = content
            )
        }
    }
}
