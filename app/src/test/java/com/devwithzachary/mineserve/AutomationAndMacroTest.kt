package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.model.ServerAutomationConfig
import com.devwithzachary.mineserve.ui.components.MinecraftCommandRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationAndMacroTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testServerAutomationConfigSerialization() {
        val original = ServerAutomationConfig(
            idleSleepEnabled = true,
            idleSleepTimeoutMinutes = 15,
            autoWakeOnPing = true,
            scheduledTasks = listOf(
                ScheduledTask(
                    id = "task-1",
                    serverId = "server-1",
                    name = "Periodic Backup",
                    enabled = true,
                    taskType = ScheduledTaskType.WORLD_BACKUP,
                    frequency = ScheduleFrequency.INTERVAL,
                    intervalMinutes = 360
                ),
                ScheduledTask(
                    id = "task-2",
                    serverId = "server-1",
                    name = "Nightly Restart",
                    enabled = true,
                    taskType = ScheduledTaskType.RESTART_SERVER,
                    frequency = ScheduleFrequency.DAILY_TIME,
                    dailyHour = 4,
                    dailyMinute = 30
                )
            )
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<ServerAutomationConfig>(serialized)

        assertTrue(deserialized.idleSleepEnabled)
        assertEquals(15, deserialized.idleSleepTimeoutMinutes)
        assertTrue(deserialized.autoWakeOnPing)
        assertEquals(2, deserialized.scheduledTasks.size)

        val task1 = deserialized.scheduledTasks[0]
        assertEquals("Periodic Backup", task1.name)
        assertEquals(ScheduledTaskType.WORLD_BACKUP, task1.taskType)
        assertEquals("Every 6 hours", task1.frequencyDescription)

        val task2 = deserialized.scheduledTasks[1]
        assertEquals("Nightly Restart", task2.name)
        assertEquals(ScheduledTaskType.RESTART_SERVER, task2.taskType)
        assertEquals("Daily at 04:30", task2.frequencyDescription)
    }

    @Test
    fun testCommandAutoCompleteSuggestions() {
        // Root command suggestions
        val timeSuggestions = MinecraftCommandRegistry.getSuggestions("/ti")
        assertTrue(timeSuggestions.contains("/time"))

        val weatherSuggestions = MinecraftCommandRegistry.getSuggestions("/wea")
        assertTrue(weatherSuggestions.contains("/weather"))

        // Subcommand suggestions for /time set
        val timeSetSuggestions = MinecraftCommandRegistry.getSuggestions("/time set ")
        assertTrue(timeSetSuggestions.contains("day"))
        assertTrue(timeSetSuggestions.contains("night"))
        assertTrue(timeSetSuggestions.contains("midnight"))

        // Subcommand suggestions for /weather
        val weatherSubSuggestions = MinecraftCommandRegistry.getSuggestions("/weather ")
        assertTrue(weatherSubSuggestions.contains("clear"))
        assertTrue(weatherSubSuggestions.contains("rain"))

        // Non-slash inputs return empty
        val emptySuggestions = MinecraftCommandRegistry.getSuggestions("hello")
        assertTrue(emptySuggestions.isEmpty())
    }

    @Test
    fun testApplySuggestion() {
        val completed1 = MinecraftCommandRegistry.applySuggestion("/ti", "/time")
        assertEquals("/time ", completed1)

        val completed2 = MinecraftCommandRegistry.applySuggestion("/time set ", "day")
        assertEquals("/time set day", completed2)
    }

    @Test
    fun testCronExpressionParsingAndValidation() {
        assertTrue(com.devwithzachary.mineserve.engine.CronExpression.isValid("*/30 * * * *"))
        assertTrue(com.devwithzachary.mineserve.engine.CronExpression.isValid("0 4 * * *"))
        assertTrue(com.devwithzachary.mineserve.engine.CronExpression.isValid("0 4 * * 0"))
        assertTrue(com.devwithzachary.mineserve.engine.CronExpression.isValid("0 2 * * 1-5"))
        assertTrue(com.devwithzachary.mineserve.engine.CronExpression.isValid("0,30 4 * * *"))

        assertFalse(com.devwithzachary.mineserve.engine.CronExpression.isValid(""))
        assertFalse(com.devwithzachary.mineserve.engine.CronExpression.isValid("invalid"))
        assertFalse(com.devwithzachary.mineserve.engine.CronExpression.isValid("* * * *")) // 4 parts
        assertFalse(com.devwithzachary.mineserve.engine.CronExpression.isValid("* * * * * *")) // 6 parts
        assertFalse(com.devwithzachary.mineserve.engine.CronExpression.isValid("60 * * * *")) // minute 60 out of range
        assertFalse(com.devwithzachary.mineserve.engine.CronExpression.isValid("* 25 * * *")) // hour 25 out of range
    }

    @Test
    fun testCronExpressionMatchesAndNext() {
        val cron = com.devwithzachary.mineserve.engine.CronExpression.parse("0 4 * * *")!!
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 4)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        assertTrue(cron.matches(cal))

        cal.set(java.util.Calendar.MINUTE, 1)
        assertFalse(cron.matches(cal))

        // Next execution from 03:00 should be 04:00 today
        val refCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 3)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nextTime = cron.next(refCal.timeInMillis)
        val nextCal = java.util.Calendar.getInstance().apply { timeInMillis = nextTime }
        assertEquals(4, nextCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(java.util.Calendar.MINUTE))
    }

    @Test
    fun testFlexibleFrequencyDescriptions() {
        val intervalTask = ScheduledTask(
            serverId = "s1",
            name = "Interval",
            frequency = ScheduleFrequency.INTERVAL,
            intervalMinutes = 45
        )
        assertEquals("Every 45 minutes", intervalTask.frequencyDescription)

        val intervalHoursTask = ScheduledTask(
            serverId = "s1",
            name = "Interval Hours",
            frequency = ScheduleFrequency.INTERVAL,
            intervalMinutes = 180
        )
        assertEquals("Every 3 hours", intervalHoursTask.frequencyDescription)

        val dailyTask = ScheduledTask(
            serverId = "s1",
            name = "Daily",
            frequency = ScheduleFrequency.EVERY_X_DAYS,
            dayInterval = 1,
            dailyHour = 4,
            dailyMinute = 0
        )
        assertEquals("Daily at 04:00", dailyTask.frequencyDescription)

        val multiDayTask = ScheduledTask(
            serverId = "s1",
            name = "Every 2 Days",
            frequency = ScheduleFrequency.EVERY_X_DAYS,
            dayInterval = 2,
            dailyHour = 4,
            dailyMinute = 30
        )
        assertEquals("Every 2 days at 04:30", multiDayTask.frequencyDescription)

        val weeklyTask = ScheduledTask(
            serverId = "s1",
            name = "Weekly",
            frequency = ScheduleFrequency.WEEKLY_DAYS,
            daysOfWeek = listOf(java.util.Calendar.SUNDAY, java.util.Calendar.WEDNESDAY),
            weekInterval = 1,
            dailyHour = 4,
            dailyMinute = 0
        )
        assertEquals("Every Sun, Wed at 04:00", weeklyTask.frequencyDescription)

        val biWeeklyTask = ScheduledTask(
            serverId = "s1",
            name = "Bi-Weekly",
            frequency = ScheduleFrequency.WEEKLY_DAYS,
            daysOfWeek = listOf(java.util.Calendar.SUNDAY),
            weekInterval = 2,
            dailyHour = 4,
            dailyMinute = 0
        )
        assertEquals("Every 2 weeks on Sun at 04:00", biWeeklyTask.frequencyDescription)

        val cronTask = ScheduledTask(
            serverId = "s1",
            name = "Cron",
            frequency = ScheduleFrequency.CUSTOM_CRON,
            cronExpression = "0 4 * * *"
        )
        assertEquals("Cron: 0 4 * * *", cronTask.frequencyDescription)
    }

    @Test
    fun testFullScheduledTaskSerialization() {
        val task = ScheduledTask(
            id = "task-full",
            serverId = "server-1",
            name = "Weekly Backup",
            enabled = true,
            taskType = ScheduledTaskType.WORLD_BACKUP,
            frequency = ScheduleFrequency.WEEKLY_DAYS,
            intervalMinutes = 120,
            dailyHour = 3,
            dailyMinute = 15,
            dayInterval = 2,
            daysOfWeek = listOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY),
            weekInterval = 2,
            cronExpression = "0 3 * * 0,6",
            command = "/say backup",
            lastRunTimestamp = 1000L,
            nextRunTimestamp = 2000L
        )

        val jsonStr = json.encodeToString(task)
        val decoded = json.decodeFromString<ScheduledTask>(jsonStr)

        assertEquals(task.id, decoded.id)
        assertEquals(task.frequency, decoded.frequency)
        assertEquals(2, decoded.dayInterval)
        assertEquals(listOf(java.util.Calendar.SATURDAY, java.util.Calendar.SUNDAY), decoded.daysOfWeek)
        assertEquals(2, decoded.weekInterval)
        assertEquals("0 3 * * 0,6", decoded.cronExpression)
        assertEquals("Every 2 weeks on Sun, Sat at 03:15", decoded.frequencyDescription)
    }
}
