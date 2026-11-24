package com.devwithzachary.mineserve.designsystem.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.devwithzachary.mineserve.designsystem.theme.MineServePalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinecraftConsoleFormatterTest {

    @Test
    fun parsesMinecraftSectionColorsAndReset() {
        val formatted = MinecraftConsoleFormatter.format("\u00A7a[INFO] \u00A7lReady\u00A7r normal")
        assertEquals("[INFO] Ready normal", formatted.text)
        assertTrue(formatted.spanStyles.any { it.item.color == MineServePalette.McGreen })
        assertTrue(formatted.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun parsesLegacyHexColorSequence() {
        val formatted = MinecraftConsoleFormatter.format("\u00A7x\u00A71\u00A70\u00A7B\u00A79\u00A78\u00A71Emerald")
        assertEquals("Emerald", formatted.text)
        assertTrue(formatted.spanStyles.any { it.item.color == Color(0xFF10B981) })
    }

    @Test
    fun parsesAnsiSgrCodesAndItalic() {
        val formatted = MinecraftConsoleFormatter.format("\u001B[91mERROR\u001B[0m \u001B[3mwarn\u001B[0m")
        assertEquals("ERROR warn", formatted.text)
        assertTrue(formatted.spanStyles.any { it.item.color == MineServePalette.McRed })
        assertTrue(formatted.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun metricLoadThresholdsMatchDesignSystem() {
        assertEquals(TelemetryLoadLevel.Normal, resolveTelemetryLoadLevel(0.80f))
        assertEquals(TelemetryLoadLevel.High, resolveTelemetryLoadLevel(0.81f))
        assertEquals(TelemetryLoadLevel.High, resolveTelemetryLoadLevel(0.95f))
        assertEquals(TelemetryLoadLevel.Critical, resolveTelemetryLoadLevel(0.96f))
    }
}
