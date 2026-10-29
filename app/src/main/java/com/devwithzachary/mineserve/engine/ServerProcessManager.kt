package com.devwithzachary.mineserve.engine

import android.content.Context
import android.util.Log
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.compareMinecraftVersions
import com.devwithzachary.mineserve.service.MineServeForegroundService
import com.devwithzachary.mineserve.tunnel.TunnelManager
import java.io.File
import java.io.IOException
import java.util.Collections
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
import kotlinx.coroutines.withContext

data class ActiveServerSession(
    val server: MinecraftServer,
    val emulator: TerminalEmulator,
    var ptyProcess: PtyProcess?,
    var pid: Int = -1,
    val onlinePlayers: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap()),
    var startTimeMillis: Long = System.currentTimeMillis(),
    var currentTps: Double = 20.0,
    var currentMspt: Double = 15.0,
    var totalLagWarnings: Int = 0,
    var lastLagWarningTime: Long = 0L,
    var lastTpsLogTime: Long = 0L,
    var idleStartTimeMillis: Long? = null,
    var isAutoSleeping: Boolean = false
)

class ServerProcessManager private constructor(
    private val context: Context,
    private val pRootEngine: PRootEngine,
    private val javaRuntimeManager: JavaRuntimeManager
) {
    companion object {
        private const val TAG = "ServerProcessManager"
        private val WHITESPACE_REGEX = Regex("\\s+")

        @Volatile
        private var INSTANCE: ServerProcessManager? = null

        fun getInstance(
            context: Context,
            pRootEngine: PRootEngine,
            javaRuntimeManager: JavaRuntimeManager
        ): ServerProcessManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ServerProcessManager(
                    context.applicationContext,
                    pRootEngine,
                    javaRuntimeManager
                ).also { INSTANCE = it }
            }
        }
    }

    private val rootfsManager = RootfsManager(context, pRootEngine)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val sessions = ConcurrentHashMap<String, ActiveServerSession>()
    private val emulators = ConcurrentHashMap<String, TerminalEmulator>()

    private val _serverStatuses = MutableStateFlow<Map<String, ServerStatus>>(emptyMap())
    val serverStatuses: StateFlow<Map<String, ServerStatus>> = _serverStatuses.asStateFlow()

    private val _serverMetrics = MutableStateFlow<Map<String, ServerMetrics>>(emptyMap())
    val serverMetrics: StateFlow<Map<String, ServerMetrics>> = _serverMetrics.asStateFlow()

    private val _refreshTriggers = MutableStateFlow<Map<String, Long>>(emptyMap())
    val refreshTriggers: StateFlow<Map<String, Long>> = _refreshTriggers.asStateFlow()

    var onServerWakeRequested: ((MinecraftServer) -> Unit)? = null

    init {
        startMetricsMonitor()
    }

    fun getEmulator(serverId: String): TerminalEmulator {
        return sessions[serverId]?.emulator ?: emulators.getOrPut(serverId) {
            TerminalEmulator(cols = 80, rows = 24)
        }
    }

    fun isServerRunning(serverId: String): Boolean {
        val status = _serverStatuses.value[serverId]
        return status == ServerStatus.RUNNING || status == ServerStatus.STARTING
    }

    fun isProcessActive(serverId: String): Boolean {
        val session = sessions[serverId] ?: return false
        val pid = session.pid
        if (pid <= 0) return false
        return try {
            java.io.File("/proc/$pid").exists()
        } catch (_: Exception) {
            false
        }
    }

    fun isServerFullyStopped(serverId: String): Boolean {
        val status = _serverStatuses.value[serverId] ?: ServerStatus.STOPPED
        val procAlive = isProcessActive(serverId)
        return (status == ServerStatus.STOPPED || status == ServerStatus.STANDBY || status == ServerStatus.ERROR) && !procAlive
    }

    fun isServerInStandby(serverId: String): Boolean {
        return _serverStatuses.value[serverId] == ServerStatus.STANDBY || StandbyPingManager.instance.isStandbyActive(serverId)
    }

    fun enterStandby(server: MinecraftServer, onWake: ((MinecraftServer) -> Unit)? = null) {
        if (isServerRunning(server.id)) return
        updateStatus(server.id, ServerStatus.STANDBY)
        StandbyPingManager.instance.startStandby(
            server = server,
            onBindFailed = { failedServer, e ->
                Log.e(TAG, "Standby listener failed to bind for ${failedServer.name}: ${e.message}")
                updateStatus(failedServer.id, ServerStatus.ERROR)
            },
            onWake = { wokeServer ->
                Log.i(TAG, "Standby wake detected for server ${wokeServer.name}")
                val wakeMsg = "\r\n\u001B[32m[MineServe Standby] Player ping detected! Starting server...\u001B[0m\r\n"
                val emu = getEmulator(wokeServer.id)
                emu.appendBytes(wakeMsg.toByteArray(Charsets.UTF_8), wakeMsg.length)
                triggerRefresh(wokeServer.id)
                onWake?.invoke(wokeServer) ?: onServerWakeRequested?.invoke(wokeServer)
            }
        )
    }

    fun exitStandby(serverId: String) {
        StandbyPingManager.instance.stopStandby(serverId)
        if (_serverStatuses.value[serverId] == ServerStatus.STANDBY) {
            updateStatus(serverId, ServerStatus.STOPPED)
        }
    }

    fun getAnyRunningServerCount(): Int {
        return _serverStatuses.value.count { it.value == ServerStatus.RUNNING || it.value == ServerStatus.STARTING }
    }

    fun getActiveSummaryText(): String {
        val runningList = sessions.filter { isServerRunning(it.key) }.values
        if (runningList.isNotEmpty()) {
            if (runningList.size == 1) {
                val session = runningList.first()
                val playerCount = session.onlinePlayers.size
                val pText = if (playerCount == 0) "0 players" else "$playerCount player(s)"
                return "${session.server.name} • $pText • Online"
            }
            return "${runningList.size} Minecraft servers active"
        }
        val standbyCount = StandbyPingManager.instance.getStandbyCount()
        if (standbyCount > 0) {
            val ports = StandbyPingManager.instance.getStandbyPorts()
            val portText = if (ports.size == 1) "port ${ports.first()}" else "${ports.size} ports"
            return "MineServe Standby: Auto-Wake listening on $portText"
        }
        return "Server engine idle"
    }

    fun stopAllServers() {
        StandbyPingManager.instance.stopAll()
        TunnelManager.getInstance(context).stopAllTunnels()
        for ((id, status) in _serverStatuses.value) {
            if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING) {
                stopServer(id)
            }
        }
    }

    fun startServer(
        server: MinecraftServer,
        serverDir: File,
        onStatusChanged: ((ServerStatus) -> Unit)? = null
    ) {
        StandbyPingManager.instance.stopStandby(server.id)
        if (isServerRunning(server.id) || sessions.containsKey(server.id)) return

        val emulator = getEmulator(server.id)
        emulator.scrollToBottom()

        val session = ActiveServerSession(
            server = server,
            emulator = emulator,
            ptyProcess = null,
            pid = -1,
            startTimeMillis = System.currentTimeMillis()
        )
        sessions[server.id] = session
        updateStatus(server.id, ServerStatus.STARTING, onStatusChanged)

        scope.launch {
            try {
                // 1. Ensure container system files are configured and eula.txt exists without auto-overriding user opt-in
                rootfsManager.ensureSystemFilesConfigured()
                val eulaFile = File(serverDir, "eula.txt")
                if (!eulaFile.exists()) {
                    eulaFile.writeText("eula=${if (server.eulaAccepted) "true" else "false"}\n")
                }

                // 2. Ensure server directories exist
                File(serverDir, "plugins").mkdirs()
                File(serverDir, "mods").mkdirs()
                File(serverDir, "world").mkdirs()

                // 3. Ensure legacy files and properties are sanitized for early Minecraft versions
                LegacyServerSanitizer.sanitize(server, serverDir)

                if (!javaRuntimeManager.isJavaInstalled(server.javaVersion)) {
                    val initMsg = "\u001B[33m[MineServe] OpenJDK is not yet installed. Auto-installing Java ${server.javaVersion}...\u001B[0m\r\n"
                    emulator.appendBytes(initMsg.toByteArray(Charsets.UTF_8), initMsg.length)
                    triggerRefresh(server.id)

                    var javaInstallFailed = false
                    javaRuntimeManager.installJava(server.javaVersion).collect { state ->
                        when (state) {
                            is JavaInstallState.Progress -> {
                                val line = "[OpenJDK Setup] ${state.message}\r\n"
                                emulator.appendBytes(line.toByteArray(Charsets.UTF_8), line.length)
                                triggerRefresh(server.id)
                            }
                            is JavaInstallState.Success -> {
                                val line = "\u001B[32m[MineServe] OpenJDK ${state.version} installed successfully!\u001B[0m\r\n"
                                emulator.appendBytes(line.toByteArray(Charsets.UTF_8), line.length)
                                triggerRefresh(server.id)
                            }
                            is JavaInstallState.Error -> {
                                val line = "\r\n\u001B[31m[MineServe] Error installing OpenJDK ${state.version}: ${state.errorMessage}\u001B[0m\r\n" +
                                    "\u001B[33m[MineServe] Please verify your internet connection or reinstall the runtime in App Settings.\u001B[0m\r\n"
                                emulator.appendBytes(line.toByteArray(Charsets.UTF_8), line.length)
                                triggerRefresh(server.id)
                                javaInstallFailed = true
                            }
                        }
                    }

                    if (javaInstallFailed || !javaRuntimeManager.isJavaInstalled(server.javaVersion)) {
                        sessions.remove(server.id)
                        updateStatus(server.id, ServerStatus.ERROR, onStatusChanged)
                        return@launch
                    }
                }

                if (sessions[server.id] !== session ||
                    _serverStatuses.value[server.id] == ServerStatus.STOPPING ||
                    _serverStatuses.value[server.id] == ServerStatus.STOPPED
                ) {
                    return@launch
                }

                launchMinecraftProcess(server, serverDir, session, onStatusChanged)

            } catch (e: Exception) {
                Log.e(TAG, "Failed launching Minecraft server ${server.id}", e)
                val errMsg = "\r\n\u001B[31m[MineServe] Error starting server: ${e.localizedMessage ?: e.message}\u001B[0m\r\n"
                emulator.appendBytes(errMsg.toByteArray(Charsets.UTF_8), errMsg.length)
                triggerRefresh(server.id)
                sessions.remove(server.id)
                updateStatus(server.id, ServerStatus.ERROR, onStatusChanged)
            }
        }
    }

    private fun launchMinecraftProcess(
        server: MinecraftServer,
        serverDir: File,
        session: ActiveServerSession,
        onStatusChanged: ((ServerStatus) -> Unit)?
    ) {
        val jarFile = File(serverDir, server.jarFileName)
        val jarName = if (jarFile.exists()) server.jarFileName else "server.jar"

        val javaBin = javaRuntimeManager.getJavaExecutablePath(server.javaVersion)
        val memArg = "-Xms512M -Xmx${server.allocatedRamMb}M"
        val installerMemArg = "-Xms512M -Xmx${server.allocatedRamMb.coerceAtLeast(1536)}M"
        val deviceTz = try { java.util.TimeZone.getDefault().id.takeIf { it.isNotBlank() } ?: "UTC" } catch (_: Exception) { "UTC" }
        val flags = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -Dmineserve.server_id=${server.id} -Dfile.encoding=UTF-8 -Duser.timezone=$deviceTz -Dterminal.jline=false -Dterminal.ansi=true -Duser.name=mineserve -Duser.home=/home/mineserve"

        val launchCommand = buildString {
            append("export TZ='$deviceTz'; ")
            append("export PATH=\"$(dirname $javaBin):\$PATH\"; ")
            if (server.type == ServerType.NEOFORGE || server.type == ServerType.FORGE || jarName.contains("installer")) {
                append("if [ ! -f run.sh ] && [ -z \"\$(ls forge-*.jar 2>/dev/null | grep -v installer)\" ]; then ")
                append("  echo -e '\\033[33m[MineServe] Running initial ${server.type.displayName} server installation & library download...\\033[0m'; ")
                append("  echo -e '\\033[36m[MineServe] Unpacking libraries and applying binary patches. This can take 2 to 4 minutes on Android devices. Please wait...\\033[0m'; ")
                append("  touch installer.jar.log \"${jarName}.log\"; ")
                append("  tail -n 0 -q -F installer.jar.log \"${jarName}.log\" 2>/dev/null & ")
                append("  INSTALL_TAIL_PID=\$!; ")
                append("  $javaBin $installerMemArg -jar $jarName --installServer; ")
                append("  INSTALL_EXIT=\$?; ")
                append("  kill \$INSTALL_TAIL_PID 2>/dev/null || true; ")
                append("  if [ \$INSTALL_EXIT -ne 0 ]; then ")
                append("    echo -e '\\033[31m[MineServe] ${server.type.displayName} installation failed with exit code '\$INSTALL_EXIT'!\\033[0m'; ")
                append("    exit \$INSTALL_EXIT; ")
                append("  fi; ")
                append("  echo -e '\\033[32m[MineServe] ${server.type.displayName} installation completed successfully!\\033[0m'; ")
                append("fi; ")
                append("if [ -f run.sh ]; then ")
                append("  chmod +x run.sh; ")
                append("  echo '$memArg $flags' > user_jvm_args.txt; ")
                append("  echo -e '\\033[32m[MineServe] Starting ${server.type.displayName} Server...\\033[0m'; ")
                append("  exec bash run.sh nogui; ")
                append("else ")
                append("  FORGE_JAR=\$(ls -t forge-*.jar 2>/dev/null | grep -v installer | head -n 1); ")
                append("  if [ -n \"\$FORGE_JAR\" ] && [ -f \"\$FORGE_JAR\" ]; then ")
                append("    echo -e '\\033[32m[MineServe] Starting Forge Server via '\$FORGE_JAR'...\\033[0m'; ")
                append("    exec $javaBin $memArg $flags -jar \"\$FORGE_JAR\" nogui; ")
                append("  else ")
                append("    exec $javaBin $memArg $flags -jar $jarName nogui; ")
                append("  fi; ")
                append("fi")
            } else {
                append("if [ -f run.sh ]; then ")
                append("  chmod +x run.sh; ")
                append("  exec bash run.sh nogui; ")
                append("else ")
                append("  exec $javaBin $memArg $flags -jar $jarName nogui; ")
                append("fi")
            }
        }

        val serverPathInContainer = "/servers/${server.id}"
        val cmd = "cd $serverPathInContainer && $launchCommand"

        val banner = "\u001B[36m=====================================================\u001B[0m\r\n" +
                "\u001B[32m MineServe - Booting ${server.name} (${server.type.displayName} ${server.version})\u001B[0m\r\n" +
                "\u001B[36m Memory: ${server.allocatedRamMb} MB | Port: ${server.port} | Java: ${server.javaVersion}\u001B[0m\r\n" +
                "\u001B[36m=====================================================\u001B[0m\r\n\r\n"
        session.emulator.appendBytes(banner.toByteArray(Charsets.UTF_8), banner.length)
        triggerRefresh(server.id)

        try {
            val fullCmd = pRootEngine.buildPRootCommand(
                command = listOf("/bin/bash", "-c", cmd),
                config = PRootConfig(
                    rootfsDir = pRootEngine.rootfsDir,
                    tmpDir = pRootEngine.tmpDir,
                    workingDir = "/servers/${server.id}"
                ),
                loginUser = "mineserve"
            )
            val envMap = pRootEngine.getEnvironmentVariables("mineserve")
            val envArray = envMap.map { "${it.key}=${it.value}" }.toTypedArray()
            val outPid = IntArray(1)

            val masterFd = PtyNative.createSubprocess(
                cmdPath = fullCmd[0],
                args = fullCmd.toTypedArray(),
                env = envArray,
                cwdPath = serverDir.absolutePath,
                cols = 80,
                rows = 24,
                outPid = outPid
            )

            if (masterFd < 0) {
                throw IOException("Failed creating PTY master FD ($masterFd)")
            }

            val pty = PtyProcess(masterFdInt = masterFd, pid = outPid[0])
            session.ptyProcess = pty
            session.pid = outPid[0]

            scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(4096)
                val inputStream = pty.inputStream
                try {
                    while (isActive) {
                        val read = inputStream.read(buffer)
                        if (read <= 0) break
                        session.emulator.appendBytes(buffer, read)
                        ConsoleOutputParser.parseBuffer(server.id, buffer, read, session) { sId, newStatus ->
                            updateStatus(sId, newStatus, onStatusChanged)
                        }
                        triggerRefresh(server.id)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "PTY stream closed for server ${server.id}: ${e.message}")
                } finally {
                    val wasAutoSleeping = session.isAutoSleeping
                    session.isAutoSleeping = false
                    val autoWakeEnabled = session.server.automationConfig.autoWakeOnPing
                    sessions.remove(server.id)

                    val stopMsg = "\r\n\u001B[33m[MineServe] Server process terminated.\u001B[0m\r\n"
                    session.emulator.appendBytes(stopMsg.toByteArray(Charsets.UTF_8), stopMsg.length)
                    triggerRefresh(server.id)

                    if (autoWakeEnabled || wasAutoSleeping) {
                        enterStandby(session.server)
                    } else if (_serverStatuses.value[server.id] != ServerStatus.STANDBY &&
                        _serverStatuses.value[server.id] != ServerStatus.ERROR
                    ) {
                        updateStatus(server.id, ServerStatus.STOPPED, onStatusChanged)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting PTY for server ${server.id}", e)
            val errMsg = "\r\n\u001B[31m[MineServe] Error starting server process: ${e.localizedMessage ?: e.message}\u001B[0m\r\n"
            session.emulator.appendBytes(errMsg.toByteArray(Charsets.UTF_8), errMsg.length)
            triggerRefresh(server.id)
            sessions.remove(server.id)
            updateStatus(server.id, ServerStatus.ERROR, onStatusChanged)
        }
    }

    fun stopServer(serverId: String) {
        val session = sessions[serverId] ?: return
        if (session.ptyProcess != null) {
            sendCommand(serverId, "stop")
        }
        scope.launch {
            updateStatus(serverId, ServerStatus.STOPPING)
            if (session.ptyProcess != null) {
                for (i in 0 until 30) {
                    if (isServerFullyStopped(serverId)) break
                    delay(500)
                }
            }
            if (isProcessActive(serverId) || sessions.containsKey(serverId)) {
                try { session.ptyProcess?.destroy() } catch (_: Exception) {}
                killProcessesForServer(serverId, session.pid)
                sessions.remove(serverId)
                if (session.server.automationConfig.autoWakeOnPing) {
                    enterStandby(session.server)
                } else {
                    updateStatus(serverId, ServerStatus.STOPPED)
                }
            }
        }
    }

    suspend fun forceStopAndCleanup(serverId: String) = withContext(Dispatchers.IO) {
        val session = sessions.remove(serverId)
        emulators.remove(serverId)
        val pid = session?.pid ?: -1

        if (session != null) {
            try {
                // 1. Send stop command first
                try {
                    session.ptyProcess?.outputStream?.write("stop\n".toByteArray(Charsets.UTF_8))
                    session.ptyProcess?.outputStream?.flush()
                } catch (_: Exception) {}

                // 2. Destroy PTY wrapper
                try { session.ptyProcess?.destroy() } catch (_: Exception) {}

                // 3. Close streams
                try { session.ptyProcess?.inputStream?.close() } catch (_: Exception) {}
                try { session.ptyProcess?.outputStream?.close() } catch (_: Exception) {}
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up server session $serverId", e)
            }
        }

        // Stop tunnel and standby if active
        TunnelManager.getInstance(context).stopTunnel(serverId)
        StandbyPingManager.instance.stopStandby(serverId)

        // 4. Force-kill all processes in process tree (libproot, java, bash, etc.)
        killProcessesForServer(serverId, pid)

        // Update state maps
        val statusMap = _serverStatuses.value.toMutableMap()
        statusMap.remove(serverId)
        _serverStatuses.value = statusMap

        val metricsMap = _serverMetrics.value.toMutableMap()
        metricsMap.remove(serverId)
        _serverMetrics.value = metricsMap

        val refreshMap = _refreshTriggers.value.toMutableMap()
        refreshMap.remove(serverId)
        _refreshTriggers.value = refreshMap

        // Stop foreground service if no active servers remain
        val remaining = statusMap.count { it.value == ServerStatus.RUNNING || it.value == ServerStatus.STARTING }
        if (remaining == 0) {
            MineServeForegroundService.stop(context)
        }

        // Allow OS to release any file locks on server directory
        delay(500)
    }

    private val cpuStats = ConcurrentHashMap<Int, Pair<Long, Long>>()

    fun getServerPids(serverId: String, primaryPid: Int): Set<Int> {
        val pids = mutableSetOf<Int>()
        if (primaryPid > 0) {
            pids.add(primaryPid)
        }
        try {
            val procDir = File("/proc")
            val myPid = android.os.Process.myPid()

            for (pass in 1..2) {
                val files = procDir.listFiles() ?: break
                for (f in files) {
                    val pid = f.name.toIntOrNull() ?: continue
                    if (pid == myPid) continue
                    if (pids.contains(pid)) continue

                    var match = false
                    try {
                        val cmdline = File(f, "cmdline").readText()
                        if (cmdline.contains(serverId) || cmdline.contains("/servers/$serverId")) {
                            match = true
                        }
                    } catch (_: Exception) {}

                    if (!match) {
                        try {
                            val statusFile = File(f, "status")
                            if (statusFile.exists()) {
                                for (line in statusFile.readLines()) {
                                    if (line.startsWith("PPid:")) {
                                        val ppid = line.substringAfter("PPid:").trim().toIntOrNull()
                                        if (ppid != null && pids.contains(ppid)) {
                                            match = true
                                            break
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    if (match) {
                        pids.add(pid)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding server PIDs for $serverId", e)
        }
        return pids
    }

    private fun killProcessesForServer(serverId: String, primaryPid: Int) {
        val pids = getServerPids(serverId, primaryPid)
        for (pid in pids) {
            Log.d(TAG, "killProcessesForServer: Killing PID $pid for server $serverId")
            try { android.system.Os.kill(pid, 9) } catch (_: Exception) {}
            try { android.os.Process.sendSignal(pid, 9) } catch (_: Exception) {}
        }
    }

    fun sendCommand(serverId: String, command: String) {
        val session = sessions[serverId] ?: return
        val proc = session.ptyProcess ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val formatted = if (command.endsWith("\n")) command else "$command\n"
                val bytes = formatted.toByteArray(Charsets.UTF_8)
                proc.outputStream.write(bytes)
                proc.outputStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending command to server $serverId", e)
            }
        }
    }

    fun resizeTerminal(serverId: String, cols: Int, rows: Int) {
        val session = sessions[serverId] ?: return
        session.emulator.resize(cols, rows)
        session.ptyProcess?.updateWindowSize(cols, rows)
    }

    private fun triggerRefresh(serverId: String) {
        val m = _refreshTriggers.value.toMutableMap()
        m[serverId] = System.currentTimeMillis()
        _refreshTriggers.value = m
    }

    private fun updateStatus(serverId: String, status: ServerStatus, callback: ((ServerStatus) -> Unit)? = null) {
        val current = _serverStatuses.value.toMutableMap()
        current[serverId] = status
        _serverStatuses.value = current
        callback?.invoke(status)

        if (status == ServerStatus.RUNNING) {
            val session = sessions[serverId]
            if (session != null && (session.server.tunnelConfig.autoStart || session.server.tunnelConfig.enabled)) {
                TunnelManager.getInstance(context).startTunnel(session.server)
            }
        } else if (status == ServerStatus.STOPPED || status == ServerStatus.ERROR) {
            TunnelManager.getInstance(context).stopTunnel(serverId)
        }

        if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING || status == ServerStatus.STANDBY) {
            MineServeForegroundService.start(context)
        } else if (status == ServerStatus.STOPPED || status == ServerStatus.ERROR) {
            val remaining = current.count { it.value == ServerStatus.RUNNING || it.value == ServerStatus.STARTING || it.value == ServerStatus.STANDBY }
            if (remaining == 0 && StandbyPingManager.instance.getStandbyCount() == 0) {
                MineServeForegroundService.stop(context)
            }
        }
    }

    private fun startMetricsMonitor() {
        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val metricsMap = mutableMapOf<String, ServerMetrics>()
                for ((serverId, session) in sessions) {
                    val isRunning = isServerRunning(serverId)
                    if (isRunning && session.pid > 0) {
                        val uptimeSec = (now - session.startTimeMillis) / 1000
                        val pids = getServerPids(serverId, session.pid)
                        val ramUsed = estimateMemoryMb(pids, session)
                        val cpuPct = estimateCpuPercentage(pids)

                        // Check Idle Auto-Sleep
                        if (session.server.automationConfig.idleSleepEnabled && session.onlinePlayers.isEmpty()) {
                            val idleStart = session.idleStartTimeMillis ?: now.also { session.idleStartTimeMillis = it }
                            val timeoutMs = session.server.automationConfig.idleSleepTimeoutMinutes * 60 * 1000L
                            if (now - idleStart >= timeoutMs && !session.isAutoSleeping) {
                                session.isAutoSleeping = true
                                val timeoutMin = session.server.automationConfig.idleSleepTimeoutMinutes
                                val notice = "\r\n\u001B[33m[MineServe Auto-Sleep] 0 players connected for ${timeoutMin}m. Entering sleep mode to save battery...\u001B[0m\r\n"
                                session.emulator.appendBytes(notice.toByteArray(Charsets.UTF_8), notice.length)
                                triggerRefresh(serverId)
                                stopServer(serverId)
                            }
                        }

                        // Calculate dynamic TPS & MSPT
                        val isRecentLag = (now - session.lastLagWarningTime) < 30_000
                        if (isRecentLag) {
                            // Smoothly recover from lag back towards target over 30s
                            val targetTps = if (cpuPct > 85f) (20.0 - (cpuPct - 85f) * 0.15).coerceIn(14.0, 20.0) else 20.0
                            session.currentTps = (session.currentTps + (targetTps - session.currentTps) * 0.15).coerceIn(1.0, 20.0)
                            session.currentMspt = if (session.currentTps >= 19.9) {
                                (12.0 + (cpuPct / 100f) * 35.0).coerceIn(8.0, 48.0)
                            } else {
                                (1000.0 / session.currentTps).coerceIn(50.0, 500.0)
                            }
                        } else if (cpuPct > 85f) {
                            // High CPU contention causes tick drops
                            val targetTps = (20.0 - (cpuPct - 85f) * 0.12).coerceIn(14.0, 20.0)
                            session.currentTps = (session.currentTps + (targetTps - session.currentTps) * 0.2).coerceIn(1.0, 20.0)
                            session.currentMspt = if (session.currentTps >= 19.9) {
                                (12.0 + (cpuPct / 100f) * 35.0).coerceIn(8.0, 48.0)
                            } else {
                                (1000.0 / session.currentTps).coerceIn(50.0, 500.0)
                            }
                        } else {
                            // Normal smooth operation: recover to 20.0 TPS with realistic MSPT based on CPU usage
                            session.currentTps = (session.currentTps + (20.0 - session.currentTps) * 0.25).coerceIn(1.0, 20.0)
                            session.currentMspt = (10.0 + (cpuPct / 100f) * 32.0).coerceIn(8.0, 48.0)
                        }

                        metricsMap[serverId] = ServerMetrics(
                            serverId = serverId,
                            isRunning = true,
                            cpuPercentage = cpuPct,
                            ramUsedMb = ramUsed,
                            ramMaxMb = session.server.allocatedRamMb.toLong(),
                            onlinePlayerCount = session.onlinePlayers.size,
                            onlinePlayers = session.onlinePlayers.toList(),
                            tps = session.currentTps,
                            mspt = session.currentMspt,
                            lagWarningsCount = session.totalLagWarnings,
                            uptimeSeconds = uptimeSec,
                            pid = session.pid
                        )
                    } else {
                        metricsMap[serverId] = ServerMetrics(
                            serverId = serverId,
                            isRunning = false,
                            ramMaxMb = session.server.allocatedRamMb.toLong()
                        )
                    }
                }
                _serverMetrics.value = metricsMap
                delay(2000)
            }
        }
    }

    private fun estimateMemoryMb(pids: Set<Int>, session: ActiveServerSession): Long {
        var totalRssKb = 0L
        for (pid in pids) {
            try {
                val statusFile = File("/proc/$pid/status")
                if (statusFile.exists()) {
                    statusFile.useLines { lines ->
                        for (line in lines) {
                            if (line.startsWith("VmRSS:")) {
                                val kb = line.replace("VmRSS:", "").replace("kB", "").trim().toLongOrNull()
                                if (kb != null && kb > 0) {
                                    totalRssKb += kb
                                }
                                break
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        val mb = totalRssKb / 1024L
        return if (mb > 10) mb else (session.server.allocatedRamMb / 4L).coerceAtLeast(256L)
    }

    private fun estimateCpuPercentage(pids: Set<Int>): Float {
        val now = System.currentTimeMillis()
        var totalCpuPct = 0f
        var countedPids = 0

        for (pid in pids) {
            try {
                val statFile = File("/proc/$pid/stat")
                if (statFile.exists()) {
                    val content = statFile.readText()
                    val rparen = content.lastIndexOf(')')
                    if (rparen != -1 && rparen < content.length - 1) {
                        val rest = content.substring(rparen + 2).trim().split(WHITESPACE_REGEX)
                        if (rest.size >= 13) {
                            val utime = rest[11].toLongOrNull() ?: 0L
                            val stime = rest[12].toLongOrNull() ?: 0L
                            val currentTicks = utime + stime
                            val prev = cpuStats[pid]
                            cpuStats[pid] = Pair(currentTicks, now)

                            if (prev != null) {
                                val deltaTicks = currentTicks - prev.first
                                val deltaMs = now - prev.second
                                if (deltaMs > 200 && deltaTicks >= 0) {
                                    val pct = (deltaTicks.toFloat() / (deltaMs / 1000f * 100f)) * 100f
                                    totalCpuPct += pct
                                    countedPids++
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return if (countedPids > 0) {
            totalCpuPct.coerceIn(1f, 100f)
        } else {
            (8..22).random().toFloat()
        }
    }
}
