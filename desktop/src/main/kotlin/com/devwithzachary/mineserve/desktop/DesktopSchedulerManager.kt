package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.engine.CronExpression
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.net.discord.DiscordWebhookClient
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DesktopSchedulerManager(
    private val serverRepository: DesktopServerRepository,
    private val processManager: DesktopProcessManager,
    private val cloudManager: DesktopCloudManager? = null
) {
    companion object {
        private const val TAG = "DesktopSchedulerManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var schedulerJob: Job? = null

    fun start() {
        if (schedulerJob?.isActive == true) return
        schedulerJob = scope.launch {
            CoreLog.i(TAG, "DesktopSchedulerManager started")
            while (isActive) {
                try {
                    evaluateTasksOnce()
                } catch (e: Exception) {
                    CoreLog.e(TAG, "Error evaluating scheduled tasks", e)
                }
                delay(30_000L)
            }
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    suspend fun evaluateTasksOnce(now: Long = System.currentTimeMillis()) {
        val servers = serverRepository.servers.value
        for (server in servers) {
            val tasks = server.automationConfig.scheduledTasks
            if (tasks.isEmpty()) continue

            var serverUpdated = false
            val updatedTasks = tasks.map { task ->
                if (!task.enabled) return@map task

                if (task.nextRunTimestamp == 0L) {
                    serverUpdated = true
                    return@map task.copy(nextRunTimestamp = calculateNextRun(task, now))
                }

                if (isTaskDue(task, now)) {
                    executeTask(server, task)
                    serverUpdated = true
                    task.copy(
                        lastRunTimestamp = now,
                        nextRunTimestamp = calculateNextRun(task, now)
                    )
                } else {
                    task
                }
            }

            if (serverUpdated) {
                val latest = serverRepository.getServer(server.id) ?: server
                val updatedConfig = latest.automationConfig.copy(scheduledTasks = updatedTasks)
                serverRepository.updateServer(latest.copy(automationConfig = updatedConfig))
            }
        }
    }

    private fun isTaskDue(task: ScheduledTask, now: Long): Boolean {
        if (task.lastRunTimestamp > 0L && now - task.lastRunTimestamp < 50_000L) {
            return false
        }
        if (task.nextRunTimestamp > 0L) {
            return now >= task.nextRunTimestamp
        }
        return when (task.frequency) {
            ScheduleFrequency.INTERVAL -> {
                if (task.lastRunTimestamp == 0L) {
                    false
                } else {
                    val intervalMs = task.intervalMinutes.coerceAtLeast(1) * 60_000L
                    now - task.lastRunTimestamp >= intervalMs
                }
            }
            ScheduleFrequency.DAILY_TIME, ScheduleFrequency.EVERY_X_DAYS -> {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                cal.get(Calendar.HOUR_OF_DAY) == task.dailyHour && cal.get(Calendar.MINUTE) == task.dailyMinute
            }
            ScheduleFrequency.WEEKLY_DAYS -> {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                val currentDow = cal.get(Calendar.DAY_OF_WEEK)
                val isDayMatch = task.daysOfWeek.isEmpty() || task.daysOfWeek.contains(currentDow)
                val isTimeMatch = cal.get(Calendar.HOUR_OF_DAY) == task.dailyHour && cal.get(Calendar.MINUTE) == task.dailyMinute
                isDayMatch && isTimeMatch
            }
            ScheduleFrequency.CUSTOM_CRON -> {
                val cal = Calendar.getInstance().apply { timeInMillis = now }
                CronExpression.parse(task.cronExpression)?.matches(cal) ?: false
            }
        }
    }

    fun calculateNextRun(task: ScheduledTask, referenceTime: Long = System.currentTimeMillis()): Long {
        return when (task.frequency) {
            ScheduleFrequency.INTERVAL -> {
                val mins = task.intervalMinutes.coerceAtLeast(1)
                referenceTime + (mins * 60_000L)
            }
            ScheduleFrequency.DAILY_TIME, ScheduleFrequency.EVERY_X_DAYS -> {
                val dayStep = task.dayInterval.coerceAtLeast(1)
                val cal = Calendar.getInstance().apply {
                    timeInMillis = referenceTime
                    set(Calendar.HOUR_OF_DAY, task.dailyHour)
                    set(Calendar.MINUTE, task.dailyMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= referenceTime) {
                    cal.add(Calendar.DAY_OF_YEAR, dayStep)
                }
                cal.timeInMillis
            }
            ScheduleFrequency.WEEKLY_DAYS -> {
                val selectedDays = if (task.daysOfWeek.isEmpty()) {
                    listOf(Calendar.SUNDAY)
                } else {
                    task.daysOfWeek
                }
                val weekInterval = task.weekInterval.coerceAtLeast(1)

                val cal = Calendar.getInstance().apply {
                    timeInMillis = referenceTime
                    set(Calendar.HOUR_OF_DAY, task.dailyHour)
                    set(Calendar.MINUTE, task.dailyMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (cal.timeInMillis <= referenceTime) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val maxDaysAhead = (7 * weekInterval + 7).coerceAtMost(90)
                for (d in 0 until maxDaysAhead) {
                    val dow = cal.get(Calendar.DAY_OF_WEEK)
                    if (selectedDays.contains(dow)) {
                        if (weekInterval <= 1 || task.lastRunTimestamp == 0L) {
                            return cal.timeInMillis
                        } else {
                            val elapsedDays = (cal.timeInMillis - task.lastRunTimestamp) / (24L * 60L * 60L * 1000L)
                            if (elapsedDays >= (weekInterval - 1) * 7L + 1L) {
                                return cal.timeInMillis
                            }
                        }
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            ScheduleFrequency.CUSTOM_CRON -> {
                val cron = CronExpression.parse(task.cronExpression)
                if (cron != null) {
                    val next = cron.next(referenceTime)
                    if (next > 0L) next else referenceTime + 3_600_000L
                } else {
                    referenceTime + 3_600_000L
                }
            }
        }
    }

    private suspend fun executeTask(server: MinecraftServer, task: ScheduledTask) {
        val isRunning = processManager.isServerRunning(server.id)
        CoreLog.i(TAG, "Executing scheduled task '${task.name}' (${task.taskType}) on server ${server.name}")

        when (task.taskType) {
            ScheduledTaskType.WORLD_BACKUP -> {
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-off")
                    processManager.sendCommand(server.id, "save-all flush")
                    delay(2000)
                }
                val res = serverRepository.createBackup(
                    serverId = server.id,
                    worldOnly = true,
                    retentionLimit = task.backupRetentionCount
                )
                val backupEntry = res.getOrNull()
                if (backupEntry != null) {
                    DiscordWebhookClient.notifyBackupCompleted(server, "World Backup", backupEntry.fileName)
                    val backupFile = java.io.File(serverRepository.getBackupsDirectory(server.id), backupEntry.fileName)
                    maybeAutoSyncBackup(server, backupFile)
                }
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-on")
                    processManager.appendSystemMessage(
                        server.id,
                        "[MineServe Scheduler] Automated world backup completed: ${backupEntry?.fileName ?: "done"}"
                    )
                }
            }
            ScheduledTaskType.FULL_BACKUP -> {
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-off")
                    processManager.sendCommand(server.id, "save-all flush")
                    delay(2000)
                }
                val res = serverRepository.createBackup(
                    serverId = server.id,
                    worldOnly = false,
                    retentionLimit = task.backupRetentionCount
                )
                val backupEntry = res.getOrNull()
                if (backupEntry != null) {
                    DiscordWebhookClient.notifyBackupCompleted(server, "Full Server Backup", backupEntry.fileName)
                    val backupFile = java.io.File(serverRepository.getBackupsDirectory(server.id), backupEntry.fileName)
                    maybeAutoSyncBackup(server, backupFile)
                }
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-on")
                    processManager.appendSystemMessage(
                        server.id,
                        "[MineServe Scheduler] Automated full backup completed: ${backupEntry?.fileName ?: "done"}"
                    )
                }
            }
            ScheduledTaskType.RESTART_SERVER -> {
                if (isRunning) {
                    processManager.sendCommand(server.id, "say [MineServe] Automated scheduled server restart in 10 seconds...")
                    delay(10_000)
                    processManager.stopServerAndWait(server.id)
                    val latest = serverRepository.getServer(server.id) ?: server
                    processManager.startServer(latest)
                }
            }
            ScheduledTaskType.BROADCAST_MESSAGE -> {
                if (isRunning) {
                    val formatted = if (task.command.startsWith("/")) {
                        task.command.removePrefix("/")
                    } else if (task.command.startsWith("say ", ignoreCase = true)) {
                        task.command
                    } else {
                        "say ${task.command}"
                    }
                    processManager.sendCommand(server.id, formatted)
                }
            }
            ScheduledTaskType.CUSTOM_COMMAND -> {
                if (isRunning && task.command.isNotBlank()) {
                    val formatted = if (task.command.startsWith("/")) task.command.removePrefix("/") else task.command
                    processManager.sendCommand(server.id, formatted)
                }
            }
        }
    }

    private suspend fun maybeAutoSyncBackup(server: MinecraftServer, backupFile: java.io.File) {
        val cfg = server.cloudBackupConfig
        if (!cfg.enabled || !backupFile.exists()) return
        when (cfg.provider) {
            com.devwithzachary.mineserve.backup.cloud.CloudProviderType.WEBDAV -> {
                if (cfg.webDavUrl.isNotBlank()) {
                    com.devwithzachary.mineserve.backup.cloud.WebDavClient().uploadBackup(backupFile, cfg)
                }
            }
            com.devwithzachary.mineserve.backup.cloud.CloudProviderType.MINESERVE_CLOUD -> {
                if (cloudManager?.isLinked == true) {
                    cloudManager.uploadBackupToCloud(server.id, backupFile)
                }
            }
            com.devwithzachary.mineserve.backup.cloud.CloudProviderType.SAF_FOLDER -> Unit
        }
    }
}
