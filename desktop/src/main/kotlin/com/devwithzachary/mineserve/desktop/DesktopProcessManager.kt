package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.engine.CrashLogAnalyzer
import com.devwithzachary.mineserve.engine.StandbyPingManager
import com.devwithzachary.mineserve.model.CrashDiagnosticReport
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.net.discord.DiscordWebhookClient
import com.devwithzachary.mineserve.util.ServerModFilter
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DesktopProcessManager(
    private val serverRepository: DesktopServerRepository,
    private val javaManager: PortableJavaManager,
    private val networkManager: DesktopNetworkManager? = null,
    private val verbose: Boolean = false,
    private val hasActiveTunnel: (String) -> Boolean = { false }
) {
    companion object {
        private const val TAG = "DesktopProcessManager"
        private const val MAX_CONSOLE_LINES = 2000
        private const val MAX_CRASH_RESTARTS = 3
        private const val CRASH_WINDOW_MS = 5 * 60 * 1000L

        private val DONE_REGEX = Regex("""Done \([0-9.,]+s\)!|Listening on /|Server started\.""", RegexOption.IGNORE_CASE)
        private val JOIN_REGEX = Regex("""([a-zA-Z0-9_]{3,16}) joined the game""")
        private val LEAVE_REGEX = Regex("""([a-zA-Z0-9_]{3,16}) left the game""")
    }

    private data class RunningSession(
        val server: MinecraftServer,
        var process: Process? = null,
        var pid: Long = -1L,
        val startTimeMillis: Long = System.currentTimeMillis(),
        @Volatile var stopRequested: Boolean = false,
        @Volatile var isAutoSleeping: Boolean = false,
        @Volatile var idleStartTimeMillis: Long? = System.currentTimeMillis(),
        val onlinePlayers: MutableSet<String> = ConcurrentHashMap.newKeySet(),
        var currentTps: Double = 20.0,
        var currentMspt: Double = 15.0,
        var totalLagWarnings: Int = 0,
        var lastLagWarningTime: Long = 0L,
        val tpsHistory: ArrayDeque<Float> = ArrayDeque(30),
        val msptHistory: ArrayDeque<Float> = ArrayDeque(30)
    )

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val sessions = ConcurrentHashMap<String, RunningSession>()
    private val consoleBuffers = ConcurrentHashMap<String, ArrayDeque<String>>()
    private val crashTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private val restartingServers = ConcurrentHashMap.newKeySet<String>()

    private val _serverStatuses = MutableStateFlow<Map<String, ServerStatus>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = _serverStatuses.asStateFlow()

    private val _serverMetrics = MutableStateFlow<Map<String, ServerMetrics>>(emptyMap())
    val serverMetrics: StateFlow<Map<String, ServerMetrics>> = _serverMetrics.asStateFlow()

    private val _refreshTriggers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val refreshTriggers: StateFlow<Map<String, Long>> = _refreshTriggers.asStateFlow()

    private val _crashReports = MutableStateFlow<Map<String, CrashDiagnosticReport?>>(emptyMap())
    val crashReports: StateFlow<Map<String, CrashDiagnosticReport?>> = _crashReports.asStateFlow()

    fun dismissCrashReport(serverId: String) {
        _crashReports.update { it - serverId }
    }

    init {
        startMetricsLoop()
    }

    fun getServerStatus(serverId: String): ServerStatus = _serverStatuses.value[serverId] ?: ServerStatus.STOPPED

    fun isServerRunning(serverId: String): Boolean {
        val st = getServerStatus(serverId)
        return st == ServerStatus.RUNNING || st == ServerStatus.STARTING
    }

    fun isServerFullyStopped(serverId: String): Boolean {
        val st = getServerStatus(serverId)
        val session = sessions[serverId]
        val alive = session?.process?.isAlive == true
        return (st == ServerStatus.STOPPED || st == ServerStatus.STANDBY || st == ServerStatus.ERROR) &&
            !alive &&
            !sessions.containsKey(serverId)
    }

    fun isServerInStandby(serverId: String): Boolean {
        return getServerStatus(serverId) == ServerStatus.STANDBY || StandbyPingManager.instance.isStandbyActive(serverId)
    }

    fun enterStandby(server: MinecraftServer) {
        if (isServerRunning(server.id)) return
        updateStatus(server.id, ServerStatus.STANDBY)
        StandbyPingManager.instance.startStandby(
            server = server,
            onBindFailed = { failedServer, e ->
                CoreLog.e(TAG, "Standby listener failed to bind for ${failedServer.name}: ${e.message}")
                updateStatus(failedServer.id, ServerStatus.ERROR)
            },
            onWake = { wokeServer ->
                CoreLog.i(TAG, "Standby ping woke server ${wokeServer.name} (${wokeServer.id})")
                updateStatus(wokeServer.id, ServerStatus.STARTING)
                val latest = serverRepository.getServer(wokeServer.id) ?: wokeServer
                startServer(latest)
            }
        )
    }

    fun exitStandby(serverId: String) {
        StandbyPingManager.instance.stopStandby(serverId)
        if (getServerStatus(serverId) == ServerStatus.STANDBY) {
            updateStatus(serverId, ServerStatus.STOPPED)
        }
    }

    fun appendSystemMessage(serverId: String, line: String) {
        appendConsoleLine(serverId, line)
    }

    fun getRecentConsoleLines(serverId: String, limit: Int = 2000): List<String> {
        val buf = consoleBuffers[serverId] ?: return emptyList()
        synchronized(buf) {
            return if (buf.size > limit) buf.takeLast(limit) else buf.toList()
        }
    }

    private fun appendConsoleLine(serverId: String, line: String) {
        if (verbose) {
            println("[$serverId] $line")
        }
        val buf = consoleBuffers.computeIfAbsent(serverId) { ArrayDeque() }
        synchronized(buf) {
            if (buf.size >= MAX_CONSOLE_LINES) buf.removeFirst()
            buf.addLast(line)
        }
        _refreshTriggers.update { it + (serverId to System.currentTimeMillis()) }
    }

    private fun updateStatus(serverId: String, status: ServerStatus) {
        _serverStatuses.update { it + (serverId to status) }
    }

    fun startServer(server: MinecraftServer, autoRestartOnCrash: Boolean = true) {
        StandbyPingManager.instance.stopStandby(server.id)
        if (isServerRunning(server.id) || sessions.containsKey(server.id)) return
        _crashReports.update { it - server.id }
        val session = RunningSession(server = server)
        sessions[server.id] = session
        updateStatus(server.id, ServerStatus.STARTING)

        scope.launch {
            try {
                val serverDir = serverRepository.getServerDirectory(server.id)
                val eulaFile = File(serverDir, "eula.txt")
                if (!eulaFile.exists()) {
                    eulaFile.writeText("eula=${server.eulaAccepted}\n")
                }

                if (server.type.supportsMods) {
                    val disabledMods = ServerModFilter.disableClientOnlyMods(serverDir)
                    for ((modName, reason) in disabledMods) {
                        appendConsoleLine(server.id, "[MineServe ModFilter] Auto-disabled client-only mod: $modName ($reason)")
                    }
                }

                val cmd: List<String> = if (server.type == ServerType.BEDROCK_BDS) {
                    if (DesktopPlatform.isMacOs) {
                        throw IllegalStateException("Mojang does not publish a native macOS Bedrock Dedicated Server binary. Use Paper + GeyserMC (Bedrock + Java Crossplay) on macOS instead.")
                    }
                    val bdsBin = File(serverDir, DesktopPlatform.bedrockBinaryName)
                    if (!bdsBin.exists()) {
                        throw IllegalStateException("${DesktopPlatform.bedrockBinaryName} binary not found in ${serverDir.absolutePath}")
                    }
                    if (DesktopPlatform.isLinux) {
                        bdsBin.setExecutable(true, false)
                    }
                    listOf(bdsBin.absolutePath)
                } else {
                    appendConsoleLine(server.id, "[MineServe ${DesktopPlatform.shortName}] Checking OpenJDK ${server.javaVersion}...")
                    val javaBin = javaManager.installJava(server.javaVersion) { msg ->
                        appendConsoleLine(server.id, "[MineServe ${DesktopPlatform.shortName}] $msg")
                    }.getOrThrow()

                    buildJavaCommand(javaBin, server, serverDir)
                }

                appendConsoleLine(server.id, "[MineServe ${DesktopPlatform.shortName}] Launching: ${cmd.joinToString(" ")}")
                val pb = ProcessBuilder(cmd)
                    .directory(serverDir)
                    .redirectErrorStream(true)
                if (server.type == ServerType.BEDROCK_BDS && DesktopPlatform.isLinux) {
                    pb.environment()["LD_LIBRARY_PATH"] = serverDir.absolutePath
                }

                val proc = pb.start()
                session.process = proc
                session.pid = runCatching { proc.pid() }.getOrDefault(-1L)

                BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val current = line ?: break
                        appendConsoleLine(server.id, current)
                        if (getServerStatus(server.id) == ServerStatus.STARTING && DONE_REGEX.containsMatchIn(current)) {
                            updateStatus(server.id, ServerStatus.RUNNING)
                            val latest = serverRepository.getServer(server.id) ?: server
                            session.idleStartTimeMillis = System.currentTimeMillis()
                            DiscordWebhookClient.notifyServerStart(latest)
                            if (latest.tunnelConfig.autoStart &&
                                networkManager != null &&
                                networkManager.getTunnelState(server.id) is DesktopTunnelState.Disconnected
                            ) {
                                appendConsoleLine(server.id, "[MineServe Network] Auto-starting ${latest.tunnelConfig.provider.displayName} tunnel...")
                                networkManager.startTunnel(latest)
                            }
                            if (latest.portForwardConfig.autoForward &&
                                networkManager != null &&
                                networkManager.getPortForwardState(server.id) is DesktopPortForwardState.Idle
                            ) {
                                appendConsoleLine(server.id, "[MineServe Network] Auto-forwarding port ${latest.port} via UPnP/NAT-PMP...")
                                networkManager.startPortForward(latest)
                            }
                            if (!hasActiveTunnel(server.id)) {
                                val fw = DesktopFirewallChecker.checkInboundPort(server.port, isUdp = server.type == ServerType.BEDROCK_BDS)
                                if (fw.isActive && !fw.isPortConfirmedOpen && !fw.warningMessage.isNullOrBlank()) {
                                    appendConsoleLine(server.id, "[MineServe Firewall] WARNING: ${fw.warningMessage}")
                                    CoreLog.w(TAG, "[${server.name}] ${fw.warningMessage}")
                                }
                            }
                        }
                        JOIN_REGEX.find(current)?.groupValues?.getOrNull(1)?.let { player ->
                            session.onlinePlayers.add(player)
                            session.idleStartTimeMillis = null
                            val latest = serverRepository.getServer(server.id) ?: server
                            DiscordWebhookClient.notifyPlayerJoin(latest, player, session.onlinePlayers.size)
                        }
                        LEAVE_REGEX.find(current)?.groupValues?.getOrNull(1)?.let { player ->
                            session.onlinePlayers.remove(player)
                            if (session.onlinePlayers.isEmpty()) {
                                session.idleStartTimeMillis = System.currentTimeMillis()
                            }
                            val latest = serverRepository.getServer(server.id) ?: server
                            DiscordWebhookClient.notifyPlayerLeave(latest, player, session.onlinePlayers.size)
                        }
                        if (current.contains("Can't keep up!", ignoreCase = true)) {
                            session.totalLagWarnings++
                            session.lastLagWarningTime = System.currentTimeMillis()
                            session.currentTps = (session.currentTps - 2.5).coerceIn(5.0, 20.0)
                        }
                    }
                }

                val exitCode = proc.waitFor()
                appendConsoleLine(server.id, "[MineServe ${DesktopPlatform.shortName}] Server process exited with code $exitCode.")
                val wasRequested = session.stopRequested
                val wasAutoSleeping = session.isAutoSleeping
                val isRestarting = restartingServers.contains(server.id)
                val latest = serverRepository.getServer(server.id) ?: server
                sessions.remove(server.id, session)

                if (wasRequested || exitCode == 0) {
                    DiscordWebhookClient.notifyServerStop(latest)
                    if (!isRestarting && (wasAutoSleeping || latest.automationConfig.autoWakeOnPing)) {
                        enterStandby(latest)
                    } else {
                        updateStatus(server.id, ServerStatus.STOPPED)
                        if (!isRestarting) {
                            if (latest.tunnelConfig.autoStart) networkManager?.stopTunnel(server.id)
                            if (latest.portForwardConfig.autoForward) networkManager?.releasePortForward(server.id)
                        }
                    }
                } else {
                    DiscordWebhookClient.notifyServerCrash(latest, "Exit code $exitCode")
                    val diagReport = CrashLogAnalyzer.analyzeServer(serverDir)
                        ?: CrashLogAnalyzer.parseLogContent(getRecentConsoleLines(server.id, 200).joinToString("\n"))
                    if (diagReport != null) {
                        _crashReports.update { it + (server.id to diagReport) }
                        appendConsoleLine(server.id, "[MineServe Crash Analyzer] ${diagReport.title}: ${diagReport.summary}")
                    }
                    updateStatus(server.id, ServerStatus.ERROR)
                    if (autoRestartOnCrash && canAutoRestartAfterCrash(server.id)) {
                        appendConsoleLine(server.id, "[MineServe Watchdog] Unexpected exit ($exitCode). Auto-restarting in 5 seconds...")
                        delay(5000)
                        val latestForRestart = serverRepository.getServer(server.id) ?: server
                        startServer(latestForRestart, autoRestartOnCrash = true)
                    } else if (autoRestartOnCrash) {
                        appendConsoleLine(server.id, "[MineServe Watchdog] Crash loop detected ($MAX_CRASH_RESTARTS crashes in 5m). Auto-restart paused.")
                    }
                }
            } catch (e: Exception) {
                CoreLog.e(TAG, "Error running server ${server.id}", e)
                appendConsoleLine(server.id, "[MineServe ${DesktopPlatform.shortName}] ERROR: ${e.message}")
                sessions.remove(server.id, session)
                updateStatus(server.id, ServerStatus.ERROR)
            }
        }
    }

    private fun canAutoRestartAfterCrash(serverId: String): Boolean {
        val now = System.currentTimeMillis()
        val list = crashTimestamps.computeIfAbsent(serverId) { mutableListOf() }
        synchronized(list) {
            list.removeAll { now - it > CRASH_WINDOW_MS }
            if (list.size >= MAX_CRASH_RESTARTS) return false
            list.add(now)
            return true
        }
    }

    private fun buildJavaCommand(javaBin: File, server: MinecraftServer, serverDir: File): List<String> {
        val args = mutableListOf(
            javaBin.absolutePath,
            "-Xms${(server.allocatedRamMb / 2).coerceAtLeast(256)}M",
            "-Xmx${server.allocatedRamMb}M"
        )
        if (server.jvmArgs.isNotBlank()) {
            args.addAll(server.jvmArgs.trim().split(Regex("\\s+")))
        }
        val argsFileName = if (DesktopPlatform.isWindows) "win_args.txt" else "unix_args.txt"
        val unixArgs = File(serverDir, "libraries").takeIf {
            server.type == ServerType.FORGE || server.type == ServerType.NEOFORGE
        }?.walkTopDown()?.filter { it.name == argsFileName }?.toList()?.let { candidates ->
            candidates.firstOrNull { it.absolutePath.contains(server.version) }
                ?: candidates.maxByOrNull { it.lastModified() }
        }

        if (unixArgs != null) {
            args.add("@${unixArgs.relativeTo(serverDir).path}")
            args.add("nogui")
        } else {
            args.add("-jar")
            args.add(File(serverDir, server.jarFileName).absolutePath)
            if (!server.type.isProxy) {
                args.add("nogui")
            }
        }
        return args
    }

    fun sendCommand(serverId: String, command: String) {
        val proc = sessions[serverId]?.process ?: return
        scope.launch {
            runCatching {
                val formatted = if (command.endsWith("\n")) command else "$command\n"
                synchronized(proc.outputStream) {
                    proc.outputStream.write(formatted.toByteArray(Charsets.UTF_8))
                    proc.outputStream.flush()
                }
            }
        }
    }

    fun stopServer(serverId: String) {
        if (isServerInStandby(serverId)) {
            exitStandby(serverId)
            return
        }
        val session = sessions[serverId] ?: return
        session.stopRequested = true
        updateStatus(serverId, ServerStatus.STOPPING)
        sendCommand(serverId, if (session.server.type.isProxy) "end" else "stop")
        scope.launch {
            for (i in 0 until 30) {
                if (session.process?.isAlive != true) break
                delay(500)
            }
            if (session.process?.isAlive == true) {
                appendConsoleLine(serverId, "[MineServe ${DesktopPlatform.shortName}] Graceful stop timed out; terminating process...")
                session.process?.destroyForcibly()
            }
        }
    }

    suspend fun stopServerAndWait(serverId: String, timeoutMs: Long = 20_000L) {
        val session = sessions[serverId]
        stopServer(serverId)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!isServerFullyStopped(serverId) && System.currentTimeMillis() < deadline) {
            delay(250)
        }
        if (session?.process?.isAlive == true) {
            session.process?.destroyForcibly()
        }
    }

    fun restartServer(server: MinecraftServer) {
        if (!restartingServers.add(server.id)) return
        appendConsoleLine(server.id, "[MineServe ${DesktopPlatform.shortName}] Restarting server...")
        stopServer(server.id)
        scope.launch {
            try {
                var waited = 0L
                while (!isServerFullyStopped(server.id) && waited < 25_000L) {
                    delay(250)
                    waited += 250
                }
                delay(500)
                val latest = serverRepository.getServer(server.id) ?: server
                startServer(latest)
            } finally {
                restartingServers.remove(server.id)
            }
        }
    }

    fun stopAllServers() {
        StandbyPingManager.instance.stopAll()
        for (id in sessions.keys.toList()) {
            stopServer(id)
        }
    }

    fun stopAllServersBlocking(timeoutMs: Long = 25_000L) {
        StandbyPingManager.instance.stopAll()
        val liveEntries = sessions.entries.map { it.key to it.value }
        if (liveEntries.isEmpty()) return

        for ((id, session) in liveEntries) {
            session.stopRequested = true
            updateStatus(id, ServerStatus.STOPPING)
            val proc = session.process
            if (proc != null && proc.isAlive) {
                runCatching {
                    val cmd = if (session.server.type.isProxy) "end\n" else "stop\n"
                    synchronized(proc.outputStream) {
                        proc.outputStream.write(cmd.toByteArray(Charsets.UTF_8))
                        proc.outputStream.flush()
                    }
                }
            }
        }

        val deadline = System.currentTimeMillis() + timeoutMs
        for ((_, session) in liveEntries) {
            val proc = session.process ?: continue
            if (!proc.isAlive) continue
            val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
            val exited = runCatching { proc.waitFor(remaining, TimeUnit.MILLISECONDS) }.getOrDefault(false)
            if (!exited && proc.isAlive) {
                proc.destroyForcibly()
            }
        }
    }

    private fun startMetricsLoop() {
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val next = mutableMapOf<String, ServerMetrics>()
                for ((id, session) in sessions) {
                    if (session.process?.isAlive == true) {
                        val ramUsed = DesktopHostTelemetry.getProcessRssMb(session.pid)
                        val cpuPct = DesktopHostTelemetry.getProcessCpuPercent(session.pid)
                        val latestServer = serverRepository.getServer(id) ?: session.server

                        // Idle Auto-Sleep check
                        if (getServerStatus(id) == ServerStatus.RUNNING &&
                            latestServer.automationConfig.idleSleepEnabled &&
                            session.onlinePlayers.isEmpty()
                        ) {
                            val idleStart = session.idleStartTimeMillis ?: now.also { session.idleStartTimeMillis = it }
                            val timeoutMin = latestServer.automationConfig.idleSleepTimeoutMinutes.coerceAtLeast(1)
                            val timeoutMs = timeoutMin * 60_000L
                            if (now - idleStart >= timeoutMs && !session.isAutoSleeping) {
                                session.isAutoSleeping = true
                                appendConsoleLine(
                                    id,
                                    "[MineServe Auto-Sleep] 0 players connected for ${timeoutMin}m. Stopping server and entering standby..."
                                )
                                stopServer(id)
                            }
                        } else if (session.onlinePlayers.isNotEmpty()) {
                            session.idleStartTimeMillis = null
                        }
                        val isRecentLag = (now - session.lastLagWarningTime) < 30_000L
                        if (isRecentLag) {
                            val targetTps = if (cpuPct > 85f) (20.0 - (cpuPct - 85f) * 0.15).coerceIn(14.0, 20.0) else 20.0
                            session.currentTps = (session.currentTps + (targetTps - session.currentTps) * 0.15).coerceIn(1.0, 20.0)
                            session.currentMspt = if (session.currentTps >= 19.9) {
                                (12.0 + (cpuPct / 100f) * 35.0).coerceIn(8.0, 48.0)
                            } else {
                                (1000.0 / session.currentTps).coerceIn(50.0, 500.0)
                            }
                        } else if (cpuPct > 85f) {
                            val targetTps = (20.0 - (cpuPct - 85f) * 0.12).coerceIn(14.0, 20.0)
                            session.currentTps = (session.currentTps + (targetTps - session.currentTps) * 0.2).coerceIn(1.0, 20.0)
                            session.currentMspt = if (session.currentTps >= 19.9) {
                                (12.0 + (cpuPct / 100f) * 35.0).coerceIn(8.0, 48.0)
                            } else {
                                (1000.0 / session.currentTps).coerceIn(50.0, 500.0)
                            }
                        } else {
                            session.currentTps = (session.currentTps + (20.0 - session.currentTps) * 0.25).coerceIn(1.0, 20.0)
                            session.currentMspt = (10.0 + (cpuPct / 100f) * 32.0).coerceIn(8.0, 48.0)
                        }

                        synchronized(session.tpsHistory) {
                            session.tpsHistory.addLast(session.currentTps.toFloat())
                            if (session.tpsHistory.size > 30) session.tpsHistory.removeFirst()
                            session.msptHistory.addLast(session.currentMspt.toFloat())
                            if (session.msptHistory.size > 30) session.msptHistory.removeFirst()
                        }

                        val (tpsSnap, msptSnap) = synchronized(session.tpsHistory) {
                            session.tpsHistory.toList() to session.msptHistory.toList()
                        }

                        next[id] = ServerMetrics(
                            serverId = id,
                            isRunning = true,
                            cpuPercentage = cpuPct,
                            ramUsedMb = ramUsed,
                            ramMaxMb = session.server.allocatedRamMb.toLong(),
                            onlinePlayerCount = session.onlinePlayers.size,
                            onlinePlayers = session.onlinePlayers.toList(),
                            tps = session.currentTps,
                            mspt = session.currentMspt,
                            tpsHistory = tpsSnap,
                            msptHistory = msptSnap,
                            lagWarningsCount = session.totalLagWarnings,
                            uptimeSeconds = (now - session.startTimeMillis) / 1000L,
                            pid = session.pid.toInt()
                        )
                    }
                }
                _serverMetrics.value = next
                delay(2000)
            }
        }
    }
}
