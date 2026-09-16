package com.devwithzachary.mineserve.ui.screens.detail.automation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduledTaskCard(
    task: ScheduledTask,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = when (task.taskType) {
                    ScheduledTaskType.WORLD_BACKUP, ScheduledTaskType.FULL_BACKUP -> EmeraldPrimary.copy(alpha = 0.15f)
                    ScheduledTaskType.RESTART_SERVER -> RedstoneRed.copy(alpha = 0.15f)
                    ScheduledTaskType.BROADCAST_MESSAGE -> GoldYellow.copy(alpha = 0.15f)
                    ScheduledTaskType.CUSTOM_COMMAND -> DiamondCyan.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (task.taskType) {
                        ScheduledTaskType.WORLD_BACKUP, ScheduledTaskType.FULL_BACKUP -> Icons.Default.Archive
                        ScheduledTaskType.RESTART_SERVER -> Icons.Default.RestartAlt
                        ScheduledTaskType.BROADCAST_MESSAGE -> Icons.Default.Campaign
                        ScheduledTaskType.CUSTOM_COMMAND -> Icons.Default.Terminal
                    }
                    val iconTint = when (task.taskType) {
                        ScheduledTaskType.WORLD_BACKUP, ScheduledTaskType.FULL_BACKUP -> EmeraldPrimary
                        ScheduledTaskType.RESTART_SERVER -> RedstoneRed
                        ScheduledTaskType.BROADCAST_MESSAGE -> GoldYellow
                        ScheduledTaskType.CUSTOM_COMMAND -> DiamondCyan
                    }
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "${task.taskType.displayName} • ${task.frequencyDescription}",
                    color = Slate400,
                    fontSize = 12.sp
                )
                if (task.lastRunTimestamp > 0L) {
                    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(task.lastRunTimestamp))
                    Text(
                        text = "Last executed: $dateStr",
                        color = Slate400.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = task.enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EmeraldPrimary,
                    checkedTrackColor = EmeraldPrimary.copy(alpha = 0.5f)
                )
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = Slate400, modifier = Modifier.size(18.dp))
            }
        }
    }
}
