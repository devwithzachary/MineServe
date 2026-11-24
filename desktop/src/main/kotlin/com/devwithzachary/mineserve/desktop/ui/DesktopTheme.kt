package com.devwithzachary.mineserve.desktop.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.ServerBadgeState
import com.devwithzachary.mineserve.designsystem.components.ServerStatusBadge
import com.devwithzachary.mineserve.designsystem.theme.MineServePalette
import com.devwithzachary.mineserve.designsystem.theme.MineServeTheme
import com.devwithzachary.mineserve.designsystem.theme.ThemeMode
import com.devwithzachary.mineserve.model.ServerStatus

/**
 * Compatibility facade aliasing the centralized MineServePalette tokens
 * so existing desktop screens transition cleanly without raw hex literals.
 */
object DesktopColors {
    val Slate950 = MineServePalette.DarkSurface
    val Slate900 = MineServePalette.DarkSurfaceContainerLow
    val Slate800 = MineServePalette.DarkSecondaryContainer
    val Slate700 = MineServePalette.DarkOutlineVariant
    val Slate400 = MineServePalette.DarkOnSurfaceVariant
    val Slate300 = MineServePalette.DarkOnSurface
    val ObsidianCard = MineServePalette.DarkSurfaceContainer
    val ObsidianCardBorder = MineServePalette.DarkOutlineVariant
    val EmeraldPrimary = MineServePalette.EmeraldSeed
    val EmeraldDark = MineServePalette.DarkPrimaryContainer
    val EmeraldLight = MineServePalette.DarkPrimary
    val RedstonePrimary = MineServePalette.DarkError
    val RedstoneLight = MineServePalette.DarkOnErrorContainer
    val AmberWarning = MineServePalette.DarkWarning
    val AmberLight = MineServePalette.DarkOnWarningContainer
}

@Composable
fun MineServeDesktopTheme(
    uiScale: Float = 1.0f,
    themeMode: ThemeMode = ThemeMode.ObsidianDark,
    content: @Composable () -> Unit
) {
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = (baseDensity.density * uiScale).coerceIn(0.8f, 3.0f),
        fontScale = baseDensity.fontScale
    )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MineServeTheme(
            themeMode = themeMode,
            useDynamicColor = false,
            content = content
        )
    }
}

@Composable
fun DesktopPanelCard(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") borderColor: Color = DesktopColors.ObsidianCardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    MineServeCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        content = content
    )
}

@Composable
fun DesktopStatusBadge(status: ServerStatus) {
    val badgeState = when (status) {
        ServerStatus.RUNNING -> ServerBadgeState.Online
        ServerStatus.STARTING -> ServerBadgeState.Starting
        ServerStatus.STOPPING -> ServerBadgeState.Stopping
        ServerStatus.ERROR -> ServerBadgeState.Error
        ServerStatus.STOPPED,
        ServerStatus.STANDBY -> ServerBadgeState.Offline
    }
    ServerStatusBadge(state = badgeState)
}
