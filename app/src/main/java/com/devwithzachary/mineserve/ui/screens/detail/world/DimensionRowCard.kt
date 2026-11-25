package com.devwithzachary.mineserve.ui.screens.detail.world

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.WorldDimensionInfo
import java.text.DecimalFormat

fun formatWorldFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

@Composable
fun DimensionRowCard(
    dimInfo: WorldDimensionInfo,
    isRunning: Boolean,
    onResetRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tintColor) = when (dimInfo.dimension) {
        DimensionType.OVERWORLD -> Icons.Rounded.Public to MaterialTheme.colorScheme.primary
        DimensionType.NETHER -> Icons.Rounded.LocalFireDepartment to MaterialTheme.colorScheme.error
        DimensionType.THE_END -> Icons.Rounded.AutoAwesome to MaterialTheme.colorScheme.tertiary
    }

    MineServeCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = ShapeDefaults.Medium,
        contentPadding = PaddingValues(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(22.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = dimInfo.dimension.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    MineServeTableMonoText(
                        text = if (dimInfo.exists) {
                            "${formatWorldFileSize(dimInfo.sizeBytes)} - ${dimInfo.regionFilesCount} region files (${dimInfo.totalChunksCount} chunks)"
                        } else {
                            "Not yet generated"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (dimInfo.dimension != DimensionType.OVERWORLD && dimInfo.exists) {
                FilledTonalButton(
                    onClick = onResetRequested,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = ShapeDefaults.Small
                ) {
                    Text(
                        text = if (dimInfo.dimension == DimensionType.NETHER) "Reset Nether" else "Reset End",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (dimInfo.dimension == DimensionType.OVERWORLD) {
                Surface(
                    shape = ShapeDefaults.Small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Primary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
