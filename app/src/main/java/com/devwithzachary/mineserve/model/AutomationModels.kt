package com.devwithzachary.mineserve.model

import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Serializable
enum class ScheduledTaskType(val displayName: String) {
    WORLD_BACKUP("World Backup"),
    FULL_BACKUP("Full Server Backup"),
    RESTART_SERVER("Restart Server"),
    BROADCAST_MESSAGE("In-Game Broadcast"),
    CUSTOM_COMMAND("Console Command")
}

@Serializable
enum class ScheduleFrequency(val displayName: String) {
    INTERVAL("Interval Timer"),
    DAILY_TIME("Daily at Specific Time"),
    EVERY_X_DAYS("Every X Days"),
    WEEKLY_DAYS("Weekly on Days"),
    CUSTOM_CRON("Custom Cron")
}

@Serializable
data class ScheduledTask(
    val id: String = UUID.randomUUID().toString().take(8),
    val serverId: String,
    val name: String,
    val enabled: Boolean = true,
    val taskType: ScheduledTaskType = ScheduledTaskType.WORLD_BACKUP,
    val frequency: ScheduleFrequency = ScheduleFrequency.INTERVAL,
    val intervalMinutes: Int = 360, // Default 6 hours
    val dailyHour: Int = 4,         // Default 04:00 AM
    val dailyMinute: Int = 0,
    val dayInterval: Int = 1,       // Repeat every N days (1 = daily, 2 = every 2 days...)
    val daysOfWeek: List<Int> = emptyList(), // Calendar.SUNDAY(1) to Calendar.SATURDAY(7)
    val weekInterval: Int = 1,      // Repeat every N weeks (1 = every week, 2 = every 2 weeks...)
    val cronExpression: String = "",
    val command: String = "",
    val lastRunTimestamp: Long = 0L,
    val nextRunTimestamp: Long = 0L
) {
    val frequencyDescription: String
        get() = when (frequency) {
            ScheduleFrequency.INTERVAL -> {
                if (intervalMinutes >= 60 && intervalMinutes % 60 == 0) {
                    val hours = intervalMinutes / 60
                    if (hours == 1) "Every 1 hour" else "Every $hours hours"
                } else {
                    if (intervalMinutes == 1) "Every 1 minute" else "Every $intervalMinutes minutes"
                }
            }
            ScheduleFrequency.DAILY_TIME, ScheduleFrequency.EVERY_X_DAYS -> {
                val timeStr = String.format(Locale.US, "%02d:%02d", dailyHour, dailyMinute)
                if (dayInterval <= 1) {
                    "Daily at $timeStr"
                } else {
                    "Every $dayInterval days at $timeStr"
                }
            }
            ScheduleFrequency.WEEKLY_DAYS -> {
                val timeStr = String.format(Locale.US, "%02d:%02d", dailyHour, dailyMinute)
                val dayNames = mapOf(
                    Calendar.SUNDAY to "Sun",
                    Calendar.MONDAY to "Mon",
                    Calendar.TUESDAY to "Tue",
                    Calendar.WEDNESDAY to "Wed",
                    Calendar.THURSDAY to "Thu",
                    Calendar.FRIDAY to "Fri",
                    Calendar.SATURDAY to "Sat"
                )
                val days = if (daysOfWeek.isEmpty()) "Sun" else daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
                if (weekInterval <= 1) {
                    "Every $days at $timeStr"
                } else {
                    "Every $weekInterval weeks on $days at $timeStr"
                }
            }
            ScheduleFrequency.CUSTOM_CRON -> {
                if (cronExpression.isNotBlank()) "Cron: $cronExpression" else "Custom Cron"
            }
        }
}

@Serializable
data class ServerAutomationConfig(
    val idleSleepEnabled: Boolean = false,
    val idleSleepTimeoutMinutes: Int = 10,
    val autoWakeOnPing: Boolean = false,
    val scheduledTasks: List<ScheduledTask> = emptyList()
)

@Serializable
data class ConsoleMacro(
    val id: String = UUID.randomUUID().toString().take(8),
    val label: String,
    val command: String,
    val autoExecute: Boolean = true
)
