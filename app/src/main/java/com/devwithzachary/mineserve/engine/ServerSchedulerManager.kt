package com.devwithzachary.mineserve.engine

import android.content.Context
import android.util.Log
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.repository.BackupRepository
import com.devwithzachary.mineserve.repository.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class ServerSchedulerManager private constructor(
    private val context: Context,
    private val serverRepository: ServerRepository,
    private val backupRepository: BackupRepository
) {
    companion object {
        private const val TAG = "ServerSchedulerManager"

        @Volatile
        private var INSTANCE: ServerSchedulerManager? = null

        fun getInstance(
            context: Context,
            serverRepository: ServerRepository,
            backupRepository: BackupRepository
        ): ServerSchedulerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServerSchedulerManager(
                    context.applicationContext,
                    serverRepository,
                    backupRepository
                ).also { INSTANCE = it }
            }
        }
    }

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var schedulerJob: Job? = null

    var processManagerProvider: (() -> ServerProcessManager)? = null
    var startServerCallback: ((MinecraftServer) -> Unit)? = null

    fun start() {
        if (schedulerJob?.isActive == true) return
        schedulerJob = scope.launch {
            Log.d(TAG, "ServerSchedulerManager started")
            while (isActive) {
                try {
                    evaluateTasks()
                } catch (e: Exception) {
                    Log.e(TAG, "Error evaluating scheduled tasks", e)
                }
                delay(30_000) // Check every 30 seconds
            }
        }
    }

    fun stop() {
        schedulerJob?.cancel()
        schedulerJob = null
    }

    private suspend fun evaluateTasks() {
        val servers = serverRepository.servers.value
        val now = System.currentTimeMillis()

        for (server in servers) {
            val tasks = server.automationConfig.scheduledTasks
            if (tasks.isEmpty()) continue

            var serverUpdated = false
            val updatedTasks = tasks.map { task ->
                if (!task.enabled) return@map task

                if (task.nextRunTimestamp == 0L) {
                    serverUpdated = true
                    val initialNextRun = calculateNextRun(task, now)
                    return@map task.copy(nextRunTimestamp = initialNextRun)
                }

                val isDue = checkIfTaskIsDue(task, now)
                if (isDue) {
                    executeTask(server, task)
                    serverUpdated = true
                    val nextRun = calculateNextRun(task, now)
                    task.copy(lastRunTimestamp = now, nextRunTimestamp = nextRun)
                } else {
                    task
                }
            }

            if (serverUpdated) {
                val updatedConfig = server.automationConfig.copy(scheduledTasks = updatedTasks)
                serverRepository.updateServer(server.copy(automationConfig = updatedConfig))
            }
        }
    }

    private fun checkIfTaskIsDue(task: ScheduledTask, now: Long): Boolean {
        // Prevent re-triggering if executed within the last 50 seconds
        if (task.lastRunTimestamp > 0L && now - task.lastRunTimestamp < 50_000L) {
            return false
        }

        if (task.nextRunTimestamp > 0L) {
            return now >= task.nextRunTimestamp
        }

        // If nextRunTimestamp is not yet set, evaluate by pattern
        return when (task.frequency) {
            ScheduleFrequency.INTERVAL -> {
                if (task.lastRunTimestamp == 0L) {
                    false
                } else {
                    val intervalMs = task.intervalMinutes.coerceAtLeast(1) * 60 * 1000L
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
                referenceTime + (mins * 60 * 1000L)
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
                            val elapsedDays = (cal.timeInMillis - task.lastRunTimestamp) / (24 * 60 * 60 * 1000L)
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
                    if (next > 0L) next else referenceTime + 3600_000L
                } else {
                    referenceTime + 3600_000L
                }
            }
        }
    }

    private suspend fun executeTask(server: MinecraftServer, task: ScheduledTask) {
        val processManager = processManagerProvider?.invoke() ?: return
        val isRunning = processManager.isServerRunning(server.id)
        val serverDir = serverRepository.getServerDirectory(server.id)

        Log.i(TAG, "Executing scheduled task '${task.name}' (${task.taskType}) on server ${server.name}")

        when (task.taskType) {
            ScheduledTaskType.WORLD_BACKUP -> {
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-off")
                    processManager.sendCommand(server.id, "save-all flush")
                    delay(2000)
                }
                val result = backupRepository.createBackup(
                    serverDir = serverDir,
                    isWorldOnly = true,
                    customName = "auto_world_${System.currentTimeMillis()}"
                )
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-on")
                    val notice = "\r\n\u001B[32m[MineServe Scheduler] Automated world backup completed: ${result?.fileName ?: "done"}\u001B[0m\r\n"
                    processManager.getEmulator(server.id).appendBytes(notice.toByteArray(Charsets.UTF_8), notice.length)
                }
            }
            ScheduledTaskType.FULL_BACKUP -> {
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-off")
                    processManager.sendCommand(server.id, "save-all flush")
                    delay(2000)
                }
                val result = backupRepository.createBackup(
                    serverDir = serverDir,
                    isWorldOnly = false,
                    customName = "auto_full_${System.currentTimeMillis()}"
                )
                if (isRunning) {
                    processManager.sendCommand(server.id, "save-on")
                    val notice = "\r\n\u001B[32m[MineServe Scheduler] Automated full backup completed: ${result?.fileName ?: "done"}\u001B[0m\r\n"
                    processManager.getEmulator(server.id).appendBytes(notice.toByteArray(Charsets.UTF_8), notice.length)
                }
            }
            ScheduledTaskType.RESTART_SERVER -> {
                if (isRunning) {
                    val warnCmd = "say [MineServe] Automated scheduled server restart in 10 seconds..."
                    processManager.sendCommand(server.id, warnCmd)
                    delay(10_000)
                    processManager.stopServer(server.id)
                    for (i in 0 until 30) {
                        if (!processManager.isServerRunning(server.id)) break
                        delay(1000)
                    }
                    startServerCallback?.invoke(server)
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
}
