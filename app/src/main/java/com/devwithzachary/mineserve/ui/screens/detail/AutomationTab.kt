package com.devwithzachary.mineserve.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerAutomationConfig
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.screens.detail.automation.AddScheduledTaskDialog
import com.devwithzachary.mineserve.ui.screens.detail.automation.ScheduledTaskCard
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationTab(
    server: MinecraftServer,
    status: ServerStatus,
    isStandbyActive: Boolean,
    onSaveAutomationConfig: (ServerAutomationConfig) -> Unit,
    onEnterStandby: () -> Unit,
    onExitStandby: () -> Unit,
    modifier: Modifier = Modifier
) {
    val automation = server.automationConfig
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Smart Idle Sleep & Auto-Wake on Ping Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = DiamondCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = DiamondCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Smart Idle Sleep & Auto-Wake",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Preserve battery and RAM with zero-overhead standby",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                // Auto-Sleep Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Shutdown when Idle",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Automatically stops the server after N minutes of 0 connected players to prevent battery drain and heat build-up.",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = automation.idleSleepEnabled,
                        onCheckedChange = { enabled ->
                            onSaveAutomationConfig(automation.copy(idleSleepEnabled = enabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = EmeraldPrimary.copy(alpha = 0.5f)
                        )
                    )
                }

                // Idle Timeout Selector
                AnimatedVisibility(visible = automation.idleSleepEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Shutdown Timeout",
                            color = Slate400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val timeouts = listOf(5, 10, 15, 30, 60)
                            timeouts.forEach { minutes ->
                                FilterChip(
                                    selected = automation.idleSleepTimeoutMinutes == minutes,
                                    onClick = {
                                        onSaveAutomationConfig(automation.copy(idleSleepTimeoutMinutes = minutes))
                                    },
                                    label = { Text("$minutes min") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Slate900,
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Auto-Wake on Ping Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Wake on Ping",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Keeps a lightweight standby listener on port ${server.port}. When a player queries or joins from their Minecraft client, the server automatically starts up!",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = automation.autoWakeOnPing,
                        onCheckedChange = { enabled ->
                            onSaveAutomationConfig(automation.copy(autoWakeOnPing = enabled))
                            if (!enabled && isStandbyActive) {
                                onExitStandby()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DiamondCyan,
                            checkedTrackColor = DiamondCyan.copy(alpha = 0.5f)
                        )
                    )
                }

                // Standby Action & Status Indicator
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Standby Status",
                                fontSize = 11.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = when {
                                isStandbyActive -> "💤 Standby Active: Listening on port ${server.port}"
                                status == ServerStatus.RUNNING && automation.idleSleepEnabled -> "Active • Sleep armed (${automation.idleSleepTimeoutMinutes}m timeout)"
                                status == ServerStatus.RUNNING -> "Active • Running Java process"
                                automation.autoWakeOnPing -> "Offline • Standby ready"
                                else -> "Offline • Auto-Wake disabled"
                            }
                            val statusColor = when {
                                isStandbyActive -> DiamondCyan
                                status == ServerStatus.RUNNING -> EmeraldLight
                                automation.autoWakeOnPing -> GoldYellow
                                else -> Slate400
                            }
                            Text(
                                text = statusText,
                                fontSize = 13.sp,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (status == ServerStatus.STOPPED || status == ServerStatus.STANDBY) {
                            if (isStandbyActive) {
                                OutlinedButton(
                                    onClick = onExitStandby,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "Exit Standby", color = Slate400, fontSize = 12.sp)
                                }
                            } else if (automation.autoWakeOnPing) {
                                Button(
                                    onClick = onEnterStandby,
                                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "Arm Standby", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Scheduled Tasks (Cron / Timer Engine) Card
        Card(
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Scheduled Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Automated backups, restarts, and timed announcements",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = EmeraldPrimary
                        )
                    }
                }

                if (automation.scheduledTasks.isEmpty()) {
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Slate400, modifier = Modifier.size(32.dp))
                            Text(
                                text = "No Scheduled Tasks Yet",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "Create scheduled world backups every 6 hours, automated nightly restarts at 4:00 AM, or repeating broadcast announcements.",
                                color = Slate400,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddTaskDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = "Add First Scheduled Task", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        automation.scheduledTasks.forEach { task ->
                            ScheduledTaskCard(
                                task = task,
                                onToggleEnabled = { enabled ->
                                    val updatedList = automation.scheduledTasks.map {
                                        if (it.id == task.id) it.copy(enabled = enabled) else it
                                    }
                                    onSaveAutomationConfig(automation.copy(scheduledTasks = updatedList))
                                },
                                onDelete = {
                                    val updatedList = automation.scheduledTasks.filter { it.id != task.id }
                                    onSaveAutomationConfig(automation.copy(scheduledTasks = updatedList))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddScheduledTaskDialog(
            serverId = server.id,
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { newTask ->
                val updatedList = automation.scheduledTasks + newTask
                onSaveAutomationConfig(automation.copy(scheduledTasks = updatedList))
                showAddTaskDialog = false
            }
        )
    }
}
