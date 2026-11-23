package com.devwithzachary.mineserve.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.ServerStatus

object DesktopColors {
    val Slate950 = Color(0xFF090D16)
    val Slate900 = Color(0xFF0F172A)
    val Slate800 = Color(0xFF1E293B)
    val Slate700 = Color(0xFF334155)
    val Slate400 = Color(0xFF94A3B8)
    val Slate300 = Color(0xFFCBD5E1)
    val ObsidianCard = Color(0xFF111827)
    val ObsidianCardBorder = Color(0xFF1F2937)
    val EmeraldPrimary = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF065F46)
    val EmeraldLight = Color(0xFF6EE7B7)
    val RedstonePrimary = Color(0xFFEF4444)
    val RedstoneLight = Color(0xFFFCA5A5)
    val AmberWarning = Color(0xFFF59E0B)
    val AmberLight = Color(0xFFFCD34D)
}

private val MineServeDarkScheme = darkColorScheme(
    primary = DesktopColors.EmeraldPrimary,
    onPrimary = Color.Black,
    secondary = DesktopColors.EmeraldLight,
    onSecondary = Color.Black,
    background = DesktopColors.Slate950,
    onBackground = Color.White,
    surface = DesktopColors.ObsidianCard,
    onSurface = Color.White,
    surfaceVariant = DesktopColors.Slate900,
    onSurfaceVariant = DesktopColors.Slate300,
    error = DesktopColors.RedstonePrimary,
    onError = Color.White
)

@Composable
fun MineServeDesktopTheme(
    uiScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = (baseDensity.density * uiScale).coerceIn(0.8f, 3.0f),
        fontScale = baseDensity.fontScale
    )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = MineServeDarkScheme,
            content = content
        )
    }
}

@Composable
fun DesktopPanelCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DesktopColors.ObsidianCardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DesktopColors.ObsidianCard),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun DesktopStatusBadge(status: ServerStatus) {
    val (bg, border, fg) = when (status) {
        ServerStatus.RUNNING -> Triple(
            DesktopColors.EmeraldPrimary.copy(alpha = 0.16f),
            DesktopColors.EmeraldPrimary,
            DesktopColors.EmeraldLight
        )
        ServerStatus.STARTING, ServerStatus.STOPPING -> Triple(
            DesktopColors.AmberWarning.copy(alpha = 0.16f),
            DesktopColors.AmberWarning,
            DesktopColors.AmberLight
        )
        ServerStatus.ERROR -> Triple(
            DesktopColors.RedstonePrimary.copy(alpha = 0.16f),
            DesktopColors.RedstonePrimary,
            DesktopColors.RedstoneLight
        )
        ServerStatus.STOPPED, ServerStatus.STANDBY -> Triple(
            DesktopColors.Slate800,
            DesktopColors.ObsidianCardBorder,
            DesktopColors.Slate400
        )
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = status.name,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
