package com.devwithzachary.mineserve.designsystem.components

import kotlin.test.Test
import kotlin.test.assertEquals

class MineServeTelemetryChartTest {

    @Test
    fun normalizesAndClampsTelemetryValues() {
        assertEquals(0f, normalizeTelemetryValue(0f, 0f, 100f))
        assertEquals(0.5f, normalizeTelemetryValue(50f, 0f, 100f))
        assertEquals(1f, normalizeTelemetryValue(120f, 0f, 100f))
        assertEquals(0f, normalizeTelemetryValue(-10f, 0f, 100f))
    }

    @Test
    fun handlesDegenerateRangeSafely() {
        assertEquals(0f, normalizeTelemetryValue(50f, 100f, 100f))
        assertEquals(0f, normalizeTelemetryValue(50f, 100f, 50f))
    }
}
