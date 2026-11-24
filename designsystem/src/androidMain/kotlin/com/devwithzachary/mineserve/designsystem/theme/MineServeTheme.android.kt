package com.devwithzachary.mineserve.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun resolvePlatformColorScheme(
    darkTheme: Boolean,
    useDynamicColor: Boolean
): ColorScheme {
    if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return if (darkTheme) ObsidianDarkColorScheme else SlateLightColorScheme
}

@Composable
actual fun PlatformThemeDecorator(
    darkTheme: Boolean,
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        if (activity != null) {
            SideEffect {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    content()
}
