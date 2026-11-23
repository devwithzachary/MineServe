package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.util.ServerModFilter
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LinuxProcessManager(
    private val serverRepository: LinuxServerRepository,
    private val javaManager: PortableJavaManager,
    private val verbose: Boolean = false,
    private val hasActiveTunnel: (String) -> Boolean = { false }
) {
    companion object {
        private const val TAG = "LinuxProcessManager"
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
        val onlinePlayers: MutableSet<String> = ConcurrentHashMap.newKeySet()
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
        return (st == ServerStatus.STOPPED || st == ServerStatus.ERROR) && !alive && !sessions.containsKey(serverId)
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
        val m = _refreshTriggers.value.toMutableMap()
        m[serverId] = System.currentTimeMillis()
        _refreshTriggers.value = m
    }

    private fun updateStatus(serverId: String, status: ServerStatus) {
        val m = _serverStatuses.value.toMutableMap()
        m[serverId] = status
        _serverStatuses.value = m
    }

    fun startServer(server: MinecraftServer, autoRestartOnCrash: Boolean = true) {
        if (isServerRunning(server.id) || sessions.containsKey(server.id)) return
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
                    val bdsBin = File(serverDir, "bedrock_server")
                    if (!bdsBin.exists()) {
                        throw IllegalStateException("bedrock_server binary not found in ${serverDir.absolutePath}")
                    }
                    bdsBin.setExecutable(true, false)
                    listOf(bdsBin.absolutePath)
                } else {
                    appendConsoleLine(server.id, "[MineServe Linux] Checking OpenJDK ${server.javaVersion}...")
                    val javaBin = javaManager.installJava(server.javaVersion) { msg ->
                        appendConsoleLine(server.id, "[MineServe Linux] $msg")
                    }.getOrThrow()

                    buildJavaCommand(javaBin, server, serverDir)
                }

                appendConsoleLine(server.id, "[MineServe Linux] Launching: ${cmd.joinToString(" ")}")
                val pb = ProcessBuilder(cmd)
                    .directory(serverDir)
                    .redirectErrorStream(true)
                if (server.type == ServerType.BEDROCK_BDS) {
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
                            if (!hasActiveTunnel(server.id)) {
                                val fw = LinuxFirewallChecker.checkInboundPort(server.port, isUdp = server.type == ServerType.BEDROCK_BDS)
                                if (fw.isActive && !fw.isPortConfirmedOpen && !fw.warningMessage.isNullOrBlank()) {
                                    appendConsoleLine(server.id, "[MineServe Firewall] WARNING: ${fw.warningMessage}")
                                    CoreLog.w(TAG, "[${server.name}] ${fw.warningMessage}")
                                }
                            }
                        }
                        JOIN_REGEX.find(current)?.groupValues?.getOrNull(1)?.let { session.onlinePlayers.add(it) }
                        LEAVE_REGEX.find(current)?.groupValues?.getOrNull(1)?.let { session.onlinePlayers.remove(it) }
                    }
                }

                val exitCode = proc.waitFor()
                appendConsoleLine(server.id, "[MineServe Linux] Server process exited with code $exitCode.")
                val wasRequested = session.stopRequested
                sessions.remove(server.id)

                if (wasRequested || exitCode == 0) {
                    updateStatus(server.id, ServerStatus.STOPPED)
                } else {
                    updateStatus(server.id, ServerStatus.ERROR)
                    if (autoRestartOnCrash && canAutoRestartAfterCrash(server.id)) {
                        appendConsoleLine(server.id, "[MineServe Watchdog] Unexpected exit ($exitCode). Auto-restarting in 5 seconds...")
                        delay(5000)
                        val latest = serverRepository.getServer(server.id) ?: server
                        startServer(latest, autoRestartOnCrash = true)
                    } else if (autoRestartOnCrash) {
                        appendConsoleLine(server.id, "[MineServe Watchdog] Crash loop detected ($MAX_CRASH_RESTARTS crashes in 5m). Auto-restart paused.")
                    }
                }
            } catch (e: Exception) {
                CoreLog.e(TAG, "Error running server ${server.id}", e)
                appendConsoleLine(server.id, "[MineServe Linux] ERROR: ${e.message}")
                sessions.remove(server.id)
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
        val unixArgs = File(serverDir, "libraries").takeIf {
            server.type == ServerType.FORGE || server.type == ServerType.NEOFORGE
        }?.walkTopDown()?.firstOrNull { it.name == "unix_args.txt" }

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
                proc.outputStream.write(formatted.toByteArray(Charsets.UTF_8))
                proc.outputStream.flush()
            }
        }
    }

    fun stopServer(serverId: String) {
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
                appendConsoleLine(serverId, "[MineServe Linux] Graceful stop timed out; terminating process...")
                session.process?.destroyForcibly()
            }
        }
    }

    fun restartServer(server: MinecraftServer) {
        if (!restartingServers.add(server.id)) return
        appendConsoleLine(server.id, "[MineServe Linux] Restarting server...")
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
        for (id in sessions.keys.toList()) {
            stopServer(id)
        }
    }

    private fun startMetricsLoop() {
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val next = mutableMapOf<String, ServerMetrics>()
                for ((id, session) in sessions) {
                    if (session.process?.isAlive == true) {
                        val ramUsed = LinuxHostTelemetry.getProcessRssMb(session.pid)
                        val cpuPct = LinuxHostTelemetry.getProcessCpuPercent(session.pid)
                        next[id] = ServerMetrics(
                            serverId = id,
                            isRunning = true,
                            cpuPercentage = cpuPct,
                            ramUsedMb = ramUsed,
                            ramMaxMb = session.server.allocatedRamMb.toLong(),
                            onlinePlayerCount = session.onlinePlayers.size,
                            onlinePlayers = session.onlinePlayers.toList(),
                            tps = 20.0,
                            mspt = 15.0,
                            uptimeSeconds = (now - session.startTimeMillis) / 1000L
                        )
                    }
                }
                _serverMetrics.value = next
                delay(2000)
            }
        }
    }
}
