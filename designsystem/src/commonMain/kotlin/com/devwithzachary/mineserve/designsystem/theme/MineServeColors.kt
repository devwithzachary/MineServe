package com.devwithzachary.mineserve.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for all MineServe "Obsidian & Ore" Material Design 3 color tokens.
 * Raw hex literals are isolated to this file per DESIGN_SYSTEM.md.
 */
object MineServePalette {
    // Dark Surface Ladder (zero 1px card borders; separated via tonal stepping)
    val DarkSurfaceContainerLowest = Color(0xFF070A12) // Console terminal viewport only
    val DarkSurface = Color(0xFF0B0F19)                // Deep Obsidian main canvas
    val DarkSurfaceContainerLow = Color(0xFF0F172A)    // Drawers, rails, sidebars, scrolled top bars
    val DarkSurfaceContainer = Color(0xFF1B2333)       // Primary borderless server cards, metric tiles
    val DarkSurfaceContainerHigh = Color(0xFF242F44)   // Card hover states, elevated toolbars
    val DarkSurfaceContainerHighest = Color(0xFF2E3B54) // Dialogs, menus, filled inputs, offline badges
    val DarkOnSurface = Color(0xFFE2E8F0)
    val DarkOnSurfaceVariant = Color(0xFF94A3B8)
    val DarkOutline = Color(0xFF64748B)
    val DarkOutlineVariant = Color(0xFF334155)         // Outlined inputs, segmented groups, table dividers only

    // Emerald Primary (Dark)
    val EmeraldSeed = Color(0xFF10B981)
    val DarkPrimary = Color(0xFF34D399)
    val DarkOnPrimary = Color(0xFF003824)
    val DarkPrimaryContainer = Color(0xFF064E3B)
    val DarkOnPrimaryContainer = Color(0xFFA7F3D0)

    // Slate Secondary (Dark)
    val DarkSecondary = Color(0xFF94A3B8)
    val DarkOnSecondary = Color(0xFF0F172A)
    val DarkSecondaryContainer = Color(0xFF1E293B)
    val DarkOnSecondaryContainer = Color(0xFFE2E8F0)

    // Diamond Tertiary (Dark)
    val DarkTertiary = Color(0xFF22D3EE)
    val DarkOnTertiary = Color(0xFF003640)
    val DarkTertiaryContainer = Color(0xFF0E4F5C)
    val DarkOnTertiaryContainer = Color(0xFFCFFAFE)

    // Gold Warning Extended (Dark)
    val GoldSeed = Color(0xFFF59E0B)
    val DarkWarning = Color(0xFFFBBF24)
    val DarkOnWarning = Color(0xFF3D2600)
    val DarkWarningContainer = Color(0xFF452706)
    val DarkOnWarningContainer = Color(0xFFFDE68A)

    // Redstone Error (Dark)
    val DarkError = Color(0xFFF87171)
    val DarkOnError = Color(0xFF450A0A)
    val DarkErrorContainer = Color(0xFF5C1111)
    val DarkOnErrorContainer = Color(0xFFFECACA)

    // Slate-Tinted Light Mode Surface Ladder
    val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFF8FAFC)
    val LightSurfaceContainerLow = Color(0xFFF1F5F9)
    val LightSurfaceContainer = Color(0xFFE2E8F0)
    val LightSurfaceContainerHigh = Color(0xFFDAE3EE)
    val LightSurfaceContainerHighest = Color(0xFFCBD5E1)
    val LightOnSurface = Color(0xFF0F172A)
    val LightOnSurfaceVariant = Color(0xFF475569)
    val LightOutline = Color(0xFF64748B)
    val LightOutlineVariant = Color(0xFFCBD5E1)

    // Emerald Primary (Light)
    val LightPrimary = Color(0xFF059669)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFD1FAE5)
    val LightOnPrimaryContainer = Color(0xFF064E3B)

    // Slate Secondary (Light)
    val LightSecondary = Color(0xFF475569)
    val LightOnSecondary = Color(0xFFFFFFFF)
    val LightSecondaryContainer = Color(0xFFE2E8F0)
    val LightOnSecondaryContainer = Color(0xFF0F172A)

    // Diamond Tertiary (Light)
    val LightTertiary = Color(0xFF0891B2)
    val LightOnTertiary = Color(0xFFFFFFFF)
    val LightTertiaryContainer = Color(0xFFCFFAFE)
    val LightOnTertiaryContainer = Color(0xFF0E4F5C)

    // Gold Warning Extended (Light)
    val LightWarning = Color(0xFFD97706)
    val LightOnWarning = Color(0xFFFFFFFF)
    val LightWarningContainer = Color(0xFFFEF3C7)
    val LightOnWarningContainer = Color(0xFF452706)

    // Redstone Error (Light)
    val LightError = Color(0xFFDC2626)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFFEE2E2)
    val LightOnErrorContainer = Color(0xFF5C1111)

    // Minecraft Console 16-Color ANSI / Section Code Palette (rendered on #070A12 terminalSurface)
    val McBlack = Color(0xFF000000)
    val McDarkBlue = Color(0xFF0000AA)
    val McDarkGreen = Color(0xFF00AA00)
    val McDarkAqua = Color(0xFF00AAAA)
    val McDarkRed = Color(0xFFAA0000)
    val McDarkPurple = Color(0xFFAA00AA)
    val McGold = Color(0xFFFFAA00)
    val McGray = Color(0xFFAAAAAA)
    val McDarkGray = Color(0xFF555555)
    val McBlue = Color(0xFF5555FF)
    val McGreen = Color(0xFF55FF55)
    val McAqua = Color(0xFF55FFFF)
    val McRed = Color(0xFFFF5555)
    val McLightPurple = Color(0xFFFF55FF)
    val McYellow = Color(0xFFFFFF55)
    val McWhite = Color(0xFFE2E8F0)
}

val ObsidianDarkColorScheme: ColorScheme = darkColorScheme(
    primary = MineServePalette.DarkPrimary,
    onPrimary = MineServePalette.DarkOnPrimary,
    primaryContainer = MineServePalette.DarkPrimaryContainer,
    onPrimaryContainer = MineServePalette.DarkOnPrimaryContainer,
    secondary = MineServePalette.DarkSecondary,
    onSecondary = MineServePalette.DarkOnSecondary,
    secondaryContainer = MineServePalette.DarkSecondaryContainer,
    onSecondaryContainer = MineServePalette.DarkOnSecondaryContainer,
    tertiary = MineServePalette.DarkTertiary,
    onTertiary = MineServePalette.DarkOnTertiary,
    tertiaryContainer = MineServePalette.DarkTertiaryContainer,
    onTertiaryContainer = MineServePalette.DarkOnTertiaryContainer,
    background = MineServePalette.DarkSurface,
    onBackground = MineServePalette.DarkOnSurface,
    surface = MineServePalette.DarkSurface,
    onSurface = MineServePalette.DarkOnSurface,
    surfaceVariant = MineServePalette.DarkSurfaceContainer,
    onSurfaceVariant = MineServePalette.DarkOnSurfaceVariant,
    surfaceContainerLowest = MineServePalette.DarkSurfaceContainerLowest,
    surfaceContainerLow = MineServePalette.DarkSurfaceContainerLow,
    surfaceContainer = MineServePalette.DarkSurfaceContainer,
    surfaceContainerHigh = MineServePalette.DarkSurfaceContainerHigh,
    surfaceContainerHighest = MineServePalette.DarkSurfaceContainerHighest,
    outline = MineServePalette.DarkOutline,
    outlineVariant = MineServePalette.DarkOutlineVariant,
    error = MineServePalette.DarkError,
    onError = MineServePalette.DarkOnError,
    errorContainer = MineServePalette.DarkErrorContainer,
    onErrorContainer = MineServePalette.DarkOnErrorContainer
)

val SlateLightColorScheme: ColorScheme = lightColorScheme(
    primary = MineServePalette.LightPrimary,
    onPrimary = MineServePalette.LightOnPrimary,
    primaryContainer = MineServePalette.LightPrimaryContainer,
    onPrimaryContainer = MineServePalette.LightOnPrimaryContainer,
    secondary = MineServePalette.LightSecondary,
    onSecondary = MineServePalette.LightOnSecondary,
    secondaryContainer = MineServePalette.LightSecondaryContainer,
    onSecondaryContainer = MineServePalette.LightOnSecondaryContainer,
    tertiary = MineServePalette.LightTertiary,
    onTertiary = MineServePalette.LightOnTertiary,
    tertiaryContainer = MineServePalette.LightTertiaryContainer,
    onTertiaryContainer = MineServePalette.LightOnTertiaryContainer,
    background = MineServePalette.LightSurface,
    onBackground = MineServePalette.LightOnSurface,
    surface = MineServePalette.LightSurface,
    onSurface = MineServePalette.LightOnSurface,
    surfaceVariant = MineServePalette.LightSurfaceContainer,
    onSurfaceVariant = MineServePalette.LightOnSurfaceVariant,
    surfaceContainerLowest = MineServePalette.LightSurfaceContainerLowest,
    surfaceContainerLow = MineServePalette.LightSurfaceContainerLow,
    surfaceContainer = MineServePalette.LightSurfaceContainer,
    surfaceContainerHigh = MineServePalette.LightSurfaceContainerHigh,
    surfaceContainerHighest = MineServePalette.LightSurfaceContainerHighest,
    outline = MineServePalette.LightOutline,
    outlineVariant = MineServePalette.LightOutlineVariant,
    error = MineServePalette.LightError,
    onError = MineServePalette.LightOnError,
    errorContainer = MineServePalette.LightErrorContainer,
    onErrorContainer = MineServePalette.LightOnErrorContainer
)

/**
 * Extended Material 3 semantic color tokens for MineServe's Gold warning states,
 * Online Emerald indicators, and Console terminal surface.
 */
@Immutable
data class MineServeExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val warningSeed: Color,
    val onlineEmerald: Color,
    val onlineEmeraldHighlight: Color,
    val onlineContainer: Color,
    val onOnlineContainer: Color,
    val terminalSurface: Color,
    val terminalOnSurface: Color
)

val DarkMineServeExtendedColors = MineServeExtendedColors(
    warning = MineServePalette.DarkWarning,
    onWarning = MineServePalette.DarkOnWarning,
    warningContainer = MineServePalette.DarkWarningContainer,
    onWarningContainer = MineServePalette.DarkOnWarningContainer,
    warningSeed = MineServePalette.GoldSeed,
    onlineEmerald = MineServePalette.EmeraldSeed,
    onlineEmeraldHighlight = MineServePalette.DarkPrimary,
    onlineContainer = MineServePalette.DarkPrimaryContainer,
    onOnlineContainer = MineServePalette.DarkOnPrimaryContainer,
    terminalSurface = MineServePalette.DarkSurfaceContainerLowest,
    terminalOnSurface = MineServePalette.DarkOnSurface
)

val LightMineServeExtendedColors = MineServeExtendedColors(
    warning = MineServePalette.LightWarning,
    onWarning = MineServePalette.LightOnWarning,
    warningContainer = MineServePalette.LightWarningContainer,
    onWarningContainer = MineServePalette.LightOnWarningContainer,
    warningSeed = MineServePalette.GoldSeed,
    onlineEmerald = MineServePalette.EmeraldSeed,
    onlineEmeraldHighlight = MineServePalette.DarkPrimary,
    onlineContainer = MineServePalette.LightPrimaryContainer,
    onOnlineContainer = MineServePalette.LightOnPrimaryContainer,
    terminalSurface = MineServePalette.DarkSurfaceContainerLowest,
    terminalOnSurface = MineServePalette.DarkOnSurface
)

val LocalMineServeColors = staticCompositionLocalOf { DarkMineServeExtendedColors }
