package com.devwithzachary.mineserve.ui.screens.detail.automation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.mineserve.engine.CronExpression
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddScheduledTaskDialog(
    serverId: String,
    onDismiss: () -> Unit,
    onAddTask: (ScheduledTask) -> Unit
) {
    var taskType by remember { mutableStateOf(ScheduledTaskType.WORLD_BACKUP) }
    var taskName by remember { mutableStateOf("Periodic World Backup") }
    var frequency by remember { mutableStateOf(ScheduleFrequency.INTERVAL) }
    
    // Interval state
    var intervalValueText by remember { mutableStateOf("6") }
    var intervalIsHours by remember { mutableStateOf(true) }

    // Time of day state
    var dailyHour by remember { mutableIntStateOf(4) }
    var dailyMinute by remember { mutableIntStateOf(0) }

    // Every X Days state
    var dayIntervalText by remember { mutableStateOf("1") }

    // Weekly on Days state
    var selectedDays by remember { mutableStateOf(setOf(Calendar.SUNDAY)) }
    var weekIntervalText by remember { mutableStateOf("1") }

    // Custom Cron state
    var cronExpressionText by remember { mutableStateOf("0 4 * * *") }

    var commandText by remember { mutableStateOf("/say Don't forget to join our Discord!") }
    var retentionCountText by remember { mutableStateOf("5") }

    val isCronValid = remember(cronExpressionText) {
        CronExpression.isValid(cronExpressionText)
    }

    val liveComputedDescription = remember(
        frequency,
        intervalValueText,
        intervalIsHours,
        dayIntervalText,
        selectedDays,
        weekIntervalText,
        dailyHour,
        dailyMinute,
        cronExpressionText
    ) {
        val computedIntervalMinutes = if (intervalIsHours) {
            (intervalValueText.toIntOrNull() ?: 1).coerceAtLeast(1) * 60
        } else {
            (intervalValueText.toIntOrNull() ?: 15).coerceAtLeast(1)
        }
        val computedDayInterval = (dayIntervalText.toIntOrNull() ?: 1).coerceAtLeast(1)
        val computedWeekInterval = (weekIntervalText.toIntOrNull() ?: 1).coerceAtLeast(1)
        val computedDaysOfWeek = selectedDays.toList().sorted()

        val tempTask = ScheduledTask(
            serverId = serverId,
            name = taskName,
            frequency = frequency,
            intervalMinutes = computedIntervalMinutes,
            dailyHour = dailyHour,
            dailyMinute = dailyMinute,
            dayInterval = computedDayInterval,
            daysOfWeek = computedDaysOfWeek,
            weekInterval = computedWeekInterval,
            cronExpression = cronExpressionText.trim()
        )
        tempTask.frequencyDescription
    }

    val canSave = taskName.isNotBlank() && (frequency != ScheduleFrequency.CUSTOM_CRON || isCronValid)

    val saveTaskAction = {
        if (canSave) {
            val computedIntervalMinutes = if (intervalIsHours) {
                (intervalValueText.toIntOrNull() ?: 1).coerceAtLeast(1) * 60
            } else {
                (intervalValueText.toIntOrNull() ?: 15).coerceAtLeast(1)
            }

            val computedDayInterval = (dayIntervalText.toIntOrNull() ?: 1).coerceAtLeast(1)
            val computedWeekInterval = (weekIntervalText.toIntOrNull() ?: 1).coerceAtLeast(1)
            val computedDaysOfWeek = selectedDays.toList().sorted()
            val computedRetentionCount = retentionCountText.toIntOrNull() ?: 5

            val created = ScheduledTask(
                id = UUID.randomUUID().toString().take(8),
                serverId = serverId,
                name = taskName.trim(),
                enabled = true,
                taskType = taskType,
                frequency = frequency,
                intervalMinutes = computedIntervalMinutes,
                dailyHour = dailyHour,
                dailyMinute = dailyMinute,
                dayInterval = computedDayInterval,
                daysOfWeek = computedDaysOfWeek,
                weekInterval = computedWeekInterval,
                cronExpression = cronExpressionText.trim(),
                command = commandText.trim(),
                backupRetentionCount = if (taskType == ScheduledTaskType.WORLD_BACKUP || taskType == ScheduledTaskType.FULL_BACKUP) computedRetentionCount else 0
            )
            onAddTask(created)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "New Scheduled Task",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Configure automated execution rules",
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = saveTaskAction,
                            enabled = canSave,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(text = "Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Slate950
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = Slate900,
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Slate400)
                        }
                        Button(
                            onClick = saveTaskAction,
                            enabled = canSave,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Task", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = Slate950
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
            ) {
                // Card 1: Task Details & Command
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Task Action",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Task Type Preset Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScheduledTaskType.values().forEach { type ->
                                FilterChip(
                                    selected = taskType == type,
                                    onClick = {
                                        taskType = type
                                        taskName = when (type) {
                                            ScheduledTaskType.WORLD_BACKUP -> "Periodic World Backup"
                                            ScheduledTaskType.FULL_BACKUP -> "Daily Full Backup"
                                            ScheduledTaskType.RESTART_SERVER -> "Nightly Server Restart"
                                            ScheduledTaskType.BROADCAST_MESSAGE -> "Discord Announcement"
                                            ScheduledTaskType.CUSTOM_COMMAND -> "Custom Console Command"
                                        }
                                        if (type == ScheduledTaskType.RESTART_SERVER) {
                                            frequency = ScheduleFrequency.EVERY_X_DAYS
                                            dayIntervalText = "1"
                                            dailyHour = 4
                                            dailyMinute = 0
                                        }
                                    },
                                    label = { Text(type.displayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Slate900,
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Task Name
                        OutlinedTextField(
                            value = taskName,
                            onValueChange = { taskName = it },
                            label = { Text("Task Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Command input (for broadcast & custom command)
                        if (taskType == ScheduledTaskType.BROADCAST_MESSAGE || taskType == ScheduledTaskType.CUSTOM_COMMAND) {
                            OutlinedTextField(
                                value = commandText,
                                onValueChange = { commandText = it },
                                label = { Text(if (taskType == ScheduledTaskType.BROADCAST_MESSAGE) "Message or /say ..." else "Console Command") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Backup Retention Setting (for World Backup and Full Backup)
                        if (taskType == ScheduledTaskType.WORLD_BACKUP || taskType == ScheduledTaskType.FULL_BACKUP) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Backup Retention Policy",
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if ((retentionCountText.toIntOrNull() ?: 0) > 0) {
                                                "Keep newest ${retentionCountText.toIntOrNull()} backups, automatically delete older ones"
                                            } else {
                                                "Keep all backups indefinitely without auto-deletion"
                                            },
                                            color = Slate400,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Quick preset chips
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val presets = listOf(3 to "3 Backups", 5 to "5 Backups", 10 to "10 Backups", 20 to "20 Backups", 0 to "Keep All")
                                    presets.forEach { (count, label) ->
                                        val isSelected = retentionCountText == count.toString()
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { retentionCountText = count.toString() },
                                            label = { Text(label, fontSize = 11.sp) },
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
                    }
                }

                // Card 2: Schedule Timing
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
                        Text(
                            text = "2. Schedule Timing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Live summary badge
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Resolved: $liveComputedDescription",
                                    color = EmeraldLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Schedule Timing Mode Chips
                        Text(text = "Select Timing Mode", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val frequencyModes = listOf(
                                ScheduleFrequency.INTERVAL to "Interval Timer",
                                ScheduleFrequency.EVERY_X_DAYS to "Every X Days",
                                ScheduleFrequency.WEEKLY_DAYS to "Weekly on Days",
                                ScheduleFrequency.CUSTOM_CRON to "Custom Cron"
                            )
                            frequencyModes.forEach { (freq, label) ->
                                FilterChip(
                                    selected = frequency == freq || (freq == ScheduleFrequency.EVERY_X_DAYS && frequency == ScheduleFrequency.DAILY_TIME),
                                    onClick = { frequency = freq },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Slate900,
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Mode 1: Interval Timer
                        if (frequency == ScheduleFrequency.INTERVAL) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "Repeat Every", color = Slate400, fontSize = 12.sp)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = intervalValueText,
                                        onValueChange = { intervalValueText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Value") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = ObsidianCardBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = !intervalIsHours,
                                        onClick = { intervalIsHours = false },
                                        label = { Text("Minutes") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )

                                    FilterChip(
                                        selected = intervalIsHours,
                                        onClick = { intervalIsHours = true },
                                        label = { Text("Hours") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )
                                }

                                // Quick interval preset chips
                                Text(text = "Quick Presets", color = Slate400, fontSize = 11.sp)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val intervalPresets = listOf(
                                        "15m" to Pair("15", false),
                                        "30m" to Pair("30", false),
                                        "1h" to Pair("1", true),
                                        "2h" to Pair("2", true),
                                        "4h" to Pair("4", true),
                                        "6h" to Pair("6", true),
                                        "12h" to Pair("12", true),
                                        "24h" to Pair("24", true)
                                    )
                                    intervalPresets.forEach { (label, config) ->
                                        val isCurrent = intervalValueText == config.first && intervalIsHours == config.second
                                        FilterChip(
                                            selected = isCurrent,
                                            onClick = {
                                                intervalValueText = config.first
                                                intervalIsHours = config.second
                                            },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DiamondCyan,
                                                selectedLabelColor = Color.Black,
                                                containerColor = Slate900,
                                                labelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Mode 2: Every X Days
                        if (frequency == ScheduleFrequency.EVERY_X_DAYS || frequency == ScheduleFrequency.DAILY_TIME) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "Day Interval", color = Slate400, fontSize = 12.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = dayIntervalText,
                                        onValueChange = { dayIntervalText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Every N Days") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = ObsidianCardBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = dayIntervalText == "1",
                                        onClick = { dayIntervalText = "1" },
                                        label = { Text("Daily (1)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )

                                    FilterChip(
                                        selected = dayIntervalText == "2",
                                        onClick = { dayIntervalText = "2" },
                                        label = { Text("Every 2d") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )
                                }

                                // Time of Day selector
                                TimeOfDaySelector(
                                    dailyHour = dailyHour,
                                    dailyMinute = dailyMinute,
                                    onTimeSelected = { h, m ->
                                        dailyHour = h
                                        dailyMinute = m
                                    }
                                )
                            }
                        }

                        // Mode 3: Weekly on Specific Days
                        if (frequency == ScheduleFrequency.WEEKLY_DAYS) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(text = "Days of Week", color = Slate400, fontSize = 12.sp)

                                // Day of week chips
                                val days = listOf(
                                    Calendar.SUNDAY to "Sun",
                                    Calendar.MONDAY to "Mon",
                                    Calendar.TUESDAY to "Tue",
                                    Calendar.WEDNESDAY to "Wed",
                                    Calendar.THURSDAY to "Thu",
                                    Calendar.FRIDAY to "Fri",
                                    Calendar.SATURDAY to "Sat"
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    days.forEach { (calDay, dayName) ->
                                        val isSelected = selectedDays.contains(calDay)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedDays = if (isSelected) {
                                                    if (selectedDays.size > 1) selectedDays - calDay else selectedDays
                                                } else {
                                                    selectedDays + calDay
                                                }
                                            },
                                            label = { Text(dayName, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EmeraldPrimary,
                                                selectedLabelColor = Color.Black,
                                                containerColor = Slate900,
                                                labelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                // Day quick presets
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = selectedDays == setOf(Calendar.SATURDAY, Calendar.SUNDAY),
                                        onClick = { selectedDays = setOf(Calendar.SATURDAY, Calendar.SUNDAY) },
                                        label = { Text("Weekends", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DiamondCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )
                                    FilterChip(
                                        selected = selectedDays == setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
                                        onClick = {
                                             selectedDays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                                        },
                                        label = { Text("Weekdays", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DiamondCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )
                                }

                                // Week interval
                                Text(text = "Week Cadence", color = Slate400, fontSize = 12.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = weekIntervalText,
                                        onValueChange = { weekIntervalText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Every N Weeks") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EmeraldPrimary,
                                            unfocusedBorderColor = ObsidianCardBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = weekIntervalText == "1",
                                        onClick = { weekIntervalText = "1" },
                                        label = { Text("Weekly (1)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )

                                    FilterChip(
                                        selected = weekIntervalText == "2",
                                        onClick = { weekIntervalText = "2" },
                                        label = { Text("Bi-Weekly (2)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Slate900,
                                            labelColor = Color.White
                                        )
                                    )
                                }

                                // Time of Day selector
                                TimeOfDaySelector(
                                    dailyHour = dailyHour,
                                    dailyMinute = dailyMinute,
                                    onTimeSelected = { h, m ->
                                        dailyHour = h
                                        dailyMinute = m
                                    }
                                )
                            }
                        }

                        // Mode 4: Custom Cron Expression
                        if (frequency == ScheduleFrequency.CUSTOM_CRON) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "Cron Expression", color = Slate400, fontSize = 12.sp)
                                OutlinedTextField(
                                    value = cronExpressionText,
                                    onValueChange = { cronExpressionText = it },
                                    label = { Text("5-Field Cron") },
                                    placeholder = { Text("0 4 * * *") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isCronValid) EmeraldPrimary else RedstoneRed,
                                        unfocusedBorderColor = if (isCronValid) ObsidianCardBorder else RedstoneRed.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Validation and format hint
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isCronValid) "✓ Valid cron expression" else "⚠️ Invalid syntax: minute hour day month weekday",
                                        color = if (isCronValid) EmeraldLight else RedstoneRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Cron presets
                                Text(text = "Common Cron Presets", color = Slate400, fontSize = 11.sp)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val cronPresets = listOf(
                                        "Every 30m" to "*/30 * * * *",
                                        "Nightly 4 AM" to "0 4 * * *",
                                        "Weekly Sun 4 AM" to "0 4 * * 0",
                                        "Twice Daily" to "0 4,16 * * *",
                                        "Weekdays 2 AM" to "0 2 * * 1-5"
                                    )
                                    cronPresets.forEach { (label, expr) ->
                                        FilterChip(
                                            selected = cronExpressionText == expr,
                                            onClick = { cronExpressionText = expr },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DiamondCyan,
                                                selectedLabelColor = Color.Black,
                                                containerColor = Slate900,
                                                labelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeOfDaySelector(
    dailyHour: Int,
    dailyMinute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "Execution Time", color = Slate400, fontSize = 12.sp)

        // Time quick preset chips
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val commonTimes = listOf(
                Pair(2, 0) to "02:00 AM",
                Pair(4, 0) to "04:00 AM",
                Pair(6, 0) to "06:00 AM",
                Pair(12, 0) to "12:00 PM",
                Pair(23, 0) to "11:00 PM"
            )
            commonTimes.forEach { (time, label) ->
                FilterChip(
                    selected = dailyHour == time.first && dailyMinute == time.second,
                    onClick = { onTimeSelected(time.first, time.second) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = Slate900,
                        labelColor = Color.White
                    )
                )
            }
        }

        // Stepper / manual adjustment row
        Surface(
            color = Slate900,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(Locale.US, "Time: %02d:%02d", dailyHour, dailyMinute),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val prevHour = if (dailyHour > 0) dailyHour - 1 else 23
                            onTimeSelected(prevHour, dailyMinute)
                        }
                    ) {
                        Text("-1h", color = Slate400, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = {
                            val nextHour = (dailyHour + 1) % 24
                            onTimeSelected(nextHour, dailyMinute)
                        }
                    ) {
                        Text("+1h", color = EmeraldLight, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = {
                            val nextMin = (dailyMinute + 15) % 60
                            onTimeSelected(dailyHour, nextMin)
                        }
                    ) {
                        Text("+15m", color = DiamondCyan, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
