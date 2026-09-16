package com.devwithzachary.mineserve.ui.screens.detail.world

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.WorldDimensionInfo
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
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
        DimensionType.OVERWORLD -> Icons.Default.Public to EmeraldLight
        DimensionType.NETHER -> Icons.Default.LocalFireDepartment to RedstoneRed
        DimensionType.THE_END -> Icons.Default.AutoAwesome to DiamondCyan
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate800,
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        text = dimInfo.dimension.displayName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (dimInfo.exists) {
                            "${formatWorldFileSize(dimInfo.sizeBytes)} • ${dimInfo.regionFilesCount} region files (${dimInfo.totalChunksCount} chunks)"
                        } else {
                            "Not yet generated"
                        },
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }

            if (dimInfo.dimension != DimensionType.OVERWORLD && dimInfo.exists) {
                OutlinedButton(
                    onClick = onResetRequested,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, RedstoneRed.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = if (dimInfo.dimension == DimensionType.NETHER) "Reset Nether" else "Reset End",
                        color = RedstoneLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (dimInfo.dimension == DimensionType.OVERWORLD) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EmeraldDark.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "Primary",
                        color = EmeraldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
