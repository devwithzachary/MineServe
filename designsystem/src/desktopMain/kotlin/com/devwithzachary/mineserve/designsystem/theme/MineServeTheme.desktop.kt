package com.devwithzachary.mineserve.designsystem.theme

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

@Composable
actual fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    useDynamicColor: Boolean
): ColorScheme {
    return if (darkTheme) ObsidianDarkColorScheme else SlateLightColorScheme
}

@Composable
actual fun PlatformThemeDecorator(
    darkTheme: Boolean,
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    val scrollbarStyle = defaultScrollbarStyle().copy(
        unhoverColor = colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        hoverColor = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        thickness = 8.dp,
        shape = RoundedCornerShape(4.dp)
    )
    CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle) {
        content()
    }
}
