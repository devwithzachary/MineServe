package com.devwithzachary.mineserve.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

enum class TelemetrySeriesColor {
    Emerald,
    Diamond
}

data class TelemetryChartSeries(
    val label: String,
    val values: List<Float>,
    val color: TelemetrySeriesColor
)

internal fun normalizeTelemetryValue(
    value: Float,
    minValue: Float,
    maxValue: Float
): Float {
    if (maxValue <= minValue) return 0f
    return ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
}

/**
 * Borderless surfaceContainer telemetry chart using Compose Canvas line and area fills
 * in Emerald (CPU/TPS) and Diamond (Memory) with subtle outlineVariant gridlines and
 * zero neon glow filters.
 */
@Composable
fun MineServeTelemetryChart(
    title: String,
    series: List<TelemetryChartSeries>,
    modifier: Modifier = Modifier,
    currentValueText: String? = null,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    chartHeight: Dp = 140.dp,
    gridLineCount: Int = 4
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    val gridColor = colorScheme.outlineVariant.copy(alpha = 0.35f)
    val emeraldColor = colorScheme.primary
    val diamondColor = colorScheme.tertiary

    MineServeCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeDefaults.Large,
        containerColor = colorScheme.surfaceContainer,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface
                )
                if (!currentValueText.isNullOrBlank()) {
                    Text(
                        text = currentValueText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = monoFont,
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = colorScheme.onSurface
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                val width = size.width
                val height = size.height
                val strokeWidth = 2.dp.toPx()

                val safeGridCount = gridLineCount.coerceAtLeast(2)
                for (i in 0 until safeGridCount) {
                    val y = height * (i.toFloat() / (safeGridCount - 1).toFloat())
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                series.forEach { item ->
                    val values = item.values
                    if (values.size >= 2) {
                        val lineColor = when (item.color) {
                            TelemetrySeriesColor.Emerald -> emeraldColor
                            TelemetrySeriesColor.Diamond -> diamondColor
                        }

                        val linePath = Path()
                        val fillPath = Path()
                        val stepX = width / (values.size - 1).toFloat()

                        values.forEachIndexed { index, raw ->
                            val x = index * stepX
                            val normalized = normalizeTelemetryValue(raw, minValue, maxValue)
                            val y = height - (normalized * (height - strokeWidth)) - (strokeWidth / 2f)
                            if (index == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (index == values.lastIndex) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.24f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            if (series.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    series.forEach { item ->
                        val dotColor = when (item.color) {
                            TelemetrySeriesColor.Emerald -> emeraldColor
                            TelemetrySeriesColor.Diamond -> diamondColor
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
