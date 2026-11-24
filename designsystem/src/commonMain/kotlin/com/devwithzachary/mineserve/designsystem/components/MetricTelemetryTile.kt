package com.devwithzachary.mineserve.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

internal enum class TelemetryLoadLevel {
    Normal,
    High,
    Critical
}

internal fun resolveTelemetryLoadLevel(progress: Float): TelemetryLoadLevel {
    return when {
        progress > 0.95f -> TelemetryLoadLevel.Critical
        progress > 0.80f -> TelemetryLoadLevel.High
        else -> TelemetryLoadLevel.Normal
    }
}

/**
 * Borderless surfaceContainer card (12dp radius) displaying standardized telemetry
 * (`CPU`, `Memory`, `TPS / MSPT`, `Players`) with a JetBrains Mono readout and a
 * 4dp LinearProgressIndicator shifting Emerald -> Gold (>80%) -> Redstone (>95%).
 */
@Composable
fun MetricTelemetryTile(
    title: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current
    val clampedProgress = progress.coerceIn(0f, 1f)

    val indicatorColor: Color = when (resolveTelemetryLoadLevel(clampedProgress)) {
        TelemetryLoadLevel.Critical -> colorScheme.error
        TelemetryLoadLevel.High -> extended.warning
        TelemetryLoadLevel.Normal -> colorScheme.primary
    }

    Surface(
        modifier = modifier,
        shape = ShapeDefaults.Medium,
        color = colorScheme.surfaceContainer,
        contentColor = colorScheme.onSurface,
        border = null
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = monoFont,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            LinearProgressIndicator(
                progress = { clampedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = indicatorColor,
                trackColor = colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
        }
    }
}
