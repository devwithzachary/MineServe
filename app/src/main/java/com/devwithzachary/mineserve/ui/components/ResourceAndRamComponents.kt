package com.devwithzachary.mineserve.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900

@Composable
fun ResourceBar(
    label: String,
    currentValue: String,
    percentage: Float,
    modifier: Modifier = Modifier,
    activeColor: Color? = null
) {
    val animatedPercent by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 1f),
        label = "resourceBarPercent"
    )

    val barColor = activeColor ?: when {
        percentage > 0.85f -> RedstoneRed
        percentage > 0.70f -> GoldYellow
        else -> EmeraldPrimary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Slate800)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPercent)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
fun RamSlider(
    allocatedMb: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalDeviceRamMb = getDeviceTotalMemoryMb(context)
    val maxAllocatableMb = (totalDeviceRamMb - 1024).coerceAtLeast(2048).coerceAtMost(32768).toInt()

    var textValue by remember(allocatedMb) { mutableStateOf(allocatedMb.toString()) }

    val steps = ((maxAllocatableMb - 512) / 256).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RAM Allocation",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = if (allocatedMb >= 1024) String.format(java.util.Locale.US, "%.1f GB", allocatedMb / 1024.0) else "$allocatedMb MB",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Device Total: ${totalDeviceRamMb / 1024} GB • Recommended: 2 GB to 4 GB for standard play",
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = allocatedMb.coerceIn(512, maxAllocatableMb).toFloat(),
            onValueChange = { 
                val rounded = (it.toInt() / 256) * 256
                onValueChange(rounded) 
            },
            valueRange = 512f..maxAllocatableMb.toFloat(),
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = EmeraldPrimary,
                activeTrackColor = EmeraldPrimary,
                inactiveTrackColor = Slate700
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = textValue,
            onValueChange = { newText ->
                textValue = newText
                val parsed = newText.toIntOrNull()
                if (parsed != null && parsed >= 256) {
                    onValueChange(parsed)
                }
            },
            label = { Text("Manual RAM Allocation (MB)") },
            placeholder = { Text("e.g. 2048") },
            trailingIcon = {
                Text(
                    text = "MB",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedLabelColor = EmeraldPrimary,
                unfocusedLabelColor = Slate400,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                unfocusedContainerColor = Slate900,
                focusedContainerColor = Slate900
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun getDeviceTotalMemoryMb(context: Context): Long {
    val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    actManager?.getMemoryInfo(memInfo)
    return memInfo.totalMem / (1024 * 1024)
}
