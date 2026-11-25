package com.devwithzachary.mineserve.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

@Composable
fun DashboardMetricsHeader(
    totalServersCount: Int,
    runningCount: Int,
    totalRamAllocatedMb: Int,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val formattedActiveRam = if (totalRamAllocatedMb > 1000) {
        val gb = totalRamAllocatedMb / 1024.0
        val formatted = String.format(java.util.Locale.US, "%.1f", gb)
        if (formatted.endsWith(".0")) "${formatted.substringBefore(".")} GB" else "$formatted GB"
    } else {
        "$totalRamAllocatedMb MB"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = ShapeDefaults.Small,
            color = colorScheme.surfaceContainer,
            contentColor = colorScheme.onSurface,
            border = null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (runningCount > 0) extended.onlineEmerald else colorScheme.outline)
                )
                Text(
                    text = "Active Servers",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$runningCount / $totalServersCount",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = monoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (runningCount > 0) colorScheme.primary else colorScheme.onSurface
                )
            }
        }

        Surface(
            shape = ShapeDefaults.Small,
            color = colorScheme.surfaceContainer,
            contentColor = colorScheme.onSurface,
            border = null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Allocated RAM",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedActiveRam,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = monoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = colorScheme.onSurface
                )
            }
        }
    }
}
