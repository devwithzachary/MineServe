package com.devwithzachary.mineserve.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.designsystem.components.MineServePreferenceRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerAutomationConfig
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.screens.detail.automation.AddScheduledTaskDialog
import com.devwithzachary.mineserve.ui.screens.detail.automation.ScheduledTaskCard
import com.devwithzachary.mineserve.ui.theme.LayoutManager

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
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        // 1. Smart Idle Sleep & Auto-Wake on Ping Card
        MineServeCard(
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Smart Idle Sleep & Auto-Wake",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preserve battery and RAM with zero-overhead standby",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                MineServePreferenceRow(
                    title = "Auto-Shutdown when Idle",
                    subtitle = "Stops the server after N minutes of 0 connected players to save battery and prevent heat.",
                    checked = automation.idleSleepEnabled,
                    onCheckedChange = { enabled ->
                        onSaveAutomationConfig(automation.copy(idleSleepEnabled = enabled))
                    }
                )

                AnimatedVisibility(visible = automation.idleSleepEnabled) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Shutdown Timeout",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(5, 10, 15, 30, 60).forEach { minutes ->
                                FilterChip(
                                    selected = automation.idleSleepTimeoutMinutes == minutes,
                                    onClick = {
                                        onSaveAutomationConfig(automation.copy(idleSleepTimeoutMinutes = minutes))
                                    },
                                    label = { Text("$minutes min", fontFamily = monoFont) },
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                MineServePreferenceRow(
                    title = "Auto-Wake on Ping",
                    subtitle = "Keeps a lightweight standby listener on port ${server.port} and starts the server when a player joins.",
                    checked = automation.autoWakeOnPing,
                    onCheckedChange = { enabled ->
                        onSaveAutomationConfig(automation.copy(autoWakeOnPing = enabled))
                        if (!enabled && isStandbyActive) {
                            onExitStandby()
                        }
                    }
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = ShapeDefaults.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Standby Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = when {
                                isStandbyActive -> "Standby Active - Listening on port ${server.port}"
                                status == ServerStatus.RUNNING && automation.idleSleepEnabled -> "Active - Sleep armed (${automation.idleSleepTimeoutMinutes}m)"
                                status == ServerStatus.RUNNING -> "Active - Running server process"
                                automation.autoWakeOnPing -> "Offline - Standby ready"
                                else -> "Offline - Auto-Wake disabled"
                            }
                            val statusColor = when {
                                isStandbyActive -> MaterialTheme.colorScheme.tertiary
                                status == ServerStatus.RUNNING -> MaterialTheme.colorScheme.primary
                                automation.autoWakeOnPing -> extColors.warning
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = monoFont,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (status == ServerStatus.STOPPED || status == ServerStatus.STANDBY) {
                            if (isStandbyActive) {
                                FilledTonalButton(
                                    onClick = onExitStandby,
                                    shape = ShapeDefaults.Small
                                ) {
                                    Text("Exit Standby", style = MaterialTheme.typography.labelMedium)
                                }
                            } else if (automation.autoWakeOnPing) {
                                FilledTonalButton(
                                    onClick = onEnterStandby,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    shape = ShapeDefaults.Small
                                ) {
                                    Text("Arm Standby", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Scheduled Tasks (Cron / Timer Engine) Card
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Scheduled Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automated backups, restarts, and timed announcements",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (automation.scheduledTasks.isEmpty()) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.Timer,
                        title = "No Scheduled Tasks Yet",
                        supportingText = "Create scheduled world backups every 6 hours, automated nightly restarts at 4:00 AM, or repeating announcements.",
                        actionLabel = "Add First Scheduled Task",
                        onActionClick = { showAddTaskDialog = true }
                    )
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

                        FilledTonalButton(
                            onClick = { showAddTaskDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Another Scheduled Task", fontWeight = FontWeight.SemiBold)
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

@com.devwithzachary.mineserve.ui.preview.MineServeTabPreviews
@Composable
private fun AutomationTabPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        AutomationTab(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            status = ServerStatus.RUNNING,
            isStandbyActive = false,
            onSaveAutomationConfig = {},
            onEnterStandby = {},
            onExitStandby = {}
        )
    }
}
