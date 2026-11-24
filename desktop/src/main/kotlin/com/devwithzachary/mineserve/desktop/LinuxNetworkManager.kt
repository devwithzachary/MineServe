package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.net.portforward.NatPmpClient
import com.devwithzachary.mineserve.net.portforward.PortForwardProtocol
import com.devwithzachary.mineserve.net.portforward.UpnpClient
import com.devwithzachary.mineserve.net.portforward.UpnpGateway
import com.devwithzachary.mineserve.net.tunnel.BoreProtocolSupport
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

sealed class LinuxTunnelState {
    object Disconnected : LinuxTunnelState()
    data class Connecting(val message: String, val claimUrl: String? = null) : LinuxTunnelState()
    data class Connected(val publicHost: String, val publicPort: Int, val fullAddress: String) : LinuxTunnelState()
    data class Error(val errorMessage: String) : LinuxTunnelState()
}

sealed class LinuxPortForwardState {
    object Idle : LinuxPortForwardState()
    data class Discovering(val message: String) : LinuxPortForwardState()
    data class Mapped(val externalIp: String, val externalPort: Int, val directAddress: String, val gatewayName: String) : LinuxPortForwardState()
    data class Failed(val reason: String) : LinuxPortForwardState()
}

class LinuxNetworkManager(
    private val paths: LinuxDataPaths,
    private val cloudManager: LinuxCloudManager? = null,
    private val verbose: Boolean = false
) {
    companion object {
        private const val TAG = "LinuxNetworkManager"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val PLAYIT_VERSION = "v0.15.26"
        private val ANSI_REGEX = Regex("\u001B\\[[0-9;]*[a-zA-Z]")
        private val CLAIM_URL_REGEX = Regex("""https://playit\.gg/claim/[a-zA-Z0-9_-]+""")
        private val PLAYIT_ADDR_REGEX = Regex("""\b([a-zA-Z0-9_.-]+\.(?:ply\.gg|joinmc\.link|playit\.gg)(?::\d+)?)\b""", RegexOption.IGNORE_CASE)
    }

    private data class ActiveTunnel(
        @Volatile var job: Job? = null,
        val stopped: AtomicBoolean = AtomicBoolean(false),
        @Volatile var controlSocket: Socket? = null,
        @Volatile var process: Process? = null
    )

    private data class ActivePortMapping(
        val gateway: UpnpGateway?,
        val externalPort: Int,
        val protocols: List<String>,
        val natPmpGateway: InetAddress? = null
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val upnpClient = UpnpClient()
    private val natPmpClient = NatPmpClient()
    private val playitDownloadMutex = Mutex()

    private val tunnelStates = ConcurrentHashMap<String, LinuxTunnelState>()
    private val activeTunnels = ConcurrentHashMap<String, ActiveTunnel>()

    private val pfStates = ConcurrentHashMap<String, LinuxPortForwardState>()
    private val activeMappings = ConcurrentHashMap<String, ActivePortMapping>()

    fun getTunnelState(serverId: String): LinuxTunnelState = tunnelStates[serverId] ?: LinuxTunnelState.Disconnected
    fun getPortForwardState(serverId: String): LinuxPortForwardState = pfStates[serverId] ?: LinuxPortForwardState.Idle

    fun startTunnel(server: MinecraftServer) {
        stopTunnel(server.id)
        val cfg = server.tunnelConfig
        CoreLog.i(TAG, "Starting ${cfg.provider.name} tunnel for '${server.name}' (local port ${server.port})")
        val stopped = AtomicBoolean(false)
        val handle = ActiveTunnel(stopped = stopped)
        activeTunnels[server.id] = handle
        val job = scope.launch {
            when (cfg.provider) {
                TunnelProvider.BORE, TunnelProvider.CUSTOM_BORE -> {
                    val host = if (cfg.provider == TunnelProvider.CUSTOM_BORE) {
                        cfg.customRelayHost.ifBlank { "bore.pub" }
                    } else {
                        "bore.pub"
                    }
                    val relayPort = if (cfg.provider == TunnelProvider.CUSTOM_BORE) cfg.customRelayPort else 7835
                    runBoreLoop(server.id, host, relayPort, server.port, 0, "", null, stopped) { sock -> handle.controlSocket = sock }
                }
                TunnelProvider.PLAYIT -> {
                    runPlayitLoop(server.id, cfg.playitSecret, stopped) { proc -> handle.process = proc }
                }
                TunnelProvider.MINESERVE_CLOUD -> {
                    val cm = cloudManager
                    if (cm == null || !cm.isLinked) {
                        tunnelStates[server.id] = LinuxTunnelState.Error("Sign in to MineServe Cloud in Host Tools first.")
                        return@launch
                    }
                    tunnelStates[server.id] = LinuxTunnelState.Connecting("Reserving ${cfg.cloudSubdomain}.${cfg.cloudDomain}...")
                    val allocRes = cm.allocateCloudTunnel(
                        serverId = server.id,
                        subdomain = cfg.cloudSubdomain,
                        region = cfg.cloudRegion,
                        protocol = "tcp",
                        domain = cfg.cloudDomain
                    )
                    allocRes.fold(
                        onSuccess = { alloc ->
                            if (alloc.relayHost.isBlank() || alloc.relayHost == "198.51.100.10") {
                                tunnelStates[server.id] = LinuxTunnelState.Error("Reserved ${alloc.hostname}, but no relay is online in ${alloc.region} yet.")
                            } else {
                                runBoreLoop(
                                    serverId = server.id,
                                    relayHost = alloc.relayHost,
                                    relayPort = 7835,
                                    localPort = server.port,
                                    requestedRemotePort = alloc.relayPort,
                                    secret = alloc.clientToken,
                                    displayHostname = alloc.hostname,
                                    stopped = stopped
                                ) { sock -> handle.controlSocket = sock }
                            }
                        },
                        onFailure = { err ->
                            tunnelStates[server.id] = LinuxTunnelState.Error(err.message ?: "Cloud tunnel allocation failed")
                        }
                    )
                }
            }
        }
        handle.job = job
    }

    fun stopTunnel(serverId: String) {
        val active = activeTunnels.remove(serverId)
        if (active != null) {
            active.stopped.set(true)
            runCatching { active.controlSocket?.setSoLinger(true, 0) }
            runCatching { active.controlSocket?.close() }
            runCatching { active.process?.destroyForcibly() }
            active.job?.cancel()
            CoreLog.i(TAG, "Stopped tunnel for server $serverId")
        }
        tunnelStates[serverId] = LinuxTunnelState.Disconnected
    }

    private suspend fun runBoreLoop(
        serverId: String,
        relayHost: String,
        relayPort: Int,
        localPort: Int,
        requestedRemotePort: Int,
        secret: String,
        displayHostname: String?,
        stopped: AtomicBoolean,
        onSocket: (Socket?) -> Unit
    ) {
        var retryMs = 2000L
        while (scope.isActive && !stopped.get()) {
            var socket: Socket? = null
            try {
                tunnelStates[serverId] = LinuxTunnelState.Connecting("Connecting to ${displayHostname ?: "$relayHost:$relayPort"}...")
                CoreLog.d(TAG, "[Bore] Connecting to $relayHost:$relayPort (remotePort=$requestedRemotePort) for 127.0.0.1:$localPort")
                socket = Socket().apply {
                    connect(InetSocketAddress(relayHost, relayPort), CONNECT_TIMEOUT_MS)
                    soTimeout = 0
                    tcpNoDelay = true
                    keepAlive = true
                }
                onSocket(socket)
                val input = socket.getInputStream()
                val output = socket.getOutputStream()

                if (secret.isNotBlank()) {
                    authenticateBore(input, output, secret)
                }

                BoreProtocolSupport.writeNullTerminatedMessage(output, JSONObject().put("Hello", requestedRemotePort).toString())
                val respStr = BoreProtocolSupport.readNullTerminatedMessage(input) ?: throw IllegalStateException("Relay closed connection during handshake")
                CoreLog.d(TAG, "[Bore] Handshake response: $respStr")
                val respJson = JSONObject(respStr)
                if (respJson.has("Error")) throw IllegalStateException("Relay error: ${respJson.getString("Error")}")
                val assignedPort = respJson.getInt("Hello")
                val fullAddress = displayHostname?.takeIf { it.isNotBlank() } ?: "$relayHost:$assignedPort"
                retryMs = 2000L

                CoreLog.i(TAG, "[Bore] Tunnel active at $fullAddress -> 127.0.0.1:$localPort")
                tunnelStates[serverId] = LinuxTunnelState.Connected(displayHostname ?: relayHost, assignedPort, fullAddress)

                while (scope.isActive && !stopped.get()) {
                    val msg = BoreProtocolSupport.readNullTerminatedMessage(input) ?: break
                    if (msg.startsWith("{")) {
                        val j = JSONObject(msg)
                        if (j.has("Connection")) {
                            val connId = j.getString("Connection")
                            CoreLog.d(TAG, "[Bore] Proxying incoming player connection $connId")
                            launchBoreProxy(relayHost, relayPort, connId, localPort, secret)
                        } else if (j.has("Error")) {
                            throw IllegalStateException(j.getString("Error"))
                        }
                    }
                }
                if (stopped.get()) break
                throw IllegalStateException("Bore control connection closed")
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                if (stopped.get()) break
                CoreLog.w(TAG, "[Bore] Tunnel error: ${e.message}")
                tunnelStates[serverId] = LinuxTunnelState.Error(e.message ?: "Bore connection failed")
            } finally {
                runCatching { socket?.close() }
                onSocket(null)
            }
            if (stopped.get()) break
            delay(retryMs)
            retryMs = (retryMs * 2).coerceAtMost(30_000L)
        }
        if (stopped.get()) tunnelStates[serverId] = LinuxTunnelState.Disconnected
    }

    private fun authenticateBore(input: InputStream, output: OutputStream, secret: String) {
        val challengeLine = BoreProtocolSupport.readNullTerminatedMessage(input) ?: throw IllegalStateException("Relay closed connection during auth")
        val challengeJson = JSONObject(challengeLine)
        if (challengeJson.has("Error")) throw IllegalStateException(challengeJson.getString("Error"))
        val challengeStr = challengeJson.optString("Challenge", "")
        if (challengeStr.isBlank()) throw IllegalStateException("Expected Bore auth challenge, got: $challengeLine")
        val tag = BoreProtocolSupport.computeChallengeHmacTag(challengeStr, secret)
        BoreProtocolSupport.writeNullTerminatedMessage(output, JSONObject().put("Authenticate", tag).toString())
    }

    private fun launchBoreProxy(relayHost: String, relayPort: Int, connId: String, localPort: Int, secret: String) {
        scope.launch {
            var relaySock: Socket? = null
            var localSock: Socket? = null
            try {
                relaySock = Socket().apply {
                    connect(InetSocketAddress(relayHost, relayPort), CONNECT_TIMEOUT_MS)
                    tcpNoDelay = true
                }
                if (secret.isNotBlank()) {
                    authenticateBore(relaySock.getInputStream(), relaySock.getOutputStream(), secret)
                }
                BoreProtocolSupport.writeNullTerminatedMessage(relaySock.getOutputStream(), JSONObject().put("Accept", connId).toString())

                localSock = Socket().apply {
                    connect(InetSocketAddress("127.0.0.1", localPort), CONNECT_TIMEOUT_MS)
                    tcpNoDelay = true
                }

                val rIn = relaySock.getInputStream()
                val rOut = relaySock.getOutputStream()
                val lIn = localSock.getInputStream()
                val lOut = localSock.getOutputStream()

                val j1 = launch { BoreProtocolSupport.pipeStreams(rIn, lOut) }
                val j2 = launch { BoreProtocolSupport.pipeStreams(lIn, rOut) }
                j1.join()
                j2.join()
            } catch (e: Exception) {
                CoreLog.d(TAG, "[Bore] Proxy stream $connId ended: ${e.message}")
            } finally {
                runCatching { relaySock?.close() }
                runCatching { localSock?.close() }
            }
        }
    }

    private suspend fun ensurePlayitBinary(): File = playitDownloadMutex.withLock {
        val bin = File(paths.binDir, "playit-$PLAYIT_VERSION")
        if (bin.exists() && bin.canExecute() && bin.length() > 100_000L) return@withLock bin
        val tmp = File(paths.binDir, "playit-$PLAYIT_VERSION.tmp")
        val arch = (System.getProperty("os.arch") ?: "amd64").lowercase()
        val asset = if (arch.contains("aarch64") || arch.contains("arm64")) "playit-linux-aarch64" else "playit-linux-amd64"
        val url = "https://github.com/playit-cloud/playit-agent/releases/download/$PLAYIT_VERSION/$asset"
        CoreLog.i(TAG, "[Playit] Downloading standalone Linux agent ($PLAYIT_VERSION) from $url...")
        val client = MineServeHttpClient.client.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.MINUTES)
            .followRedirects(true)
            .build()
        try {
            client.newCall(MineServeHttpClient.newRequestBuilder(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("Failed downloading playit agent (HTTP ${resp.code})")
                val body = resp.body ?: throw IllegalStateException("Empty playit download body")
                FileOutputStream(tmp).use { out -> body.byteStream().copyTo(out) }
            }
            tmp.setExecutable(true, false)
            runCatching {
                Files.move(tmp.toPath(), bin.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }.onFailure {
                if (!tmp.renameTo(bin)) {
                    tmp.copyTo(bin, overwrite = true)
                    tmp.delete()
                }
            }
            bin.setExecutable(true, false)
            return@withLock bin
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }

    private suspend fun runPlayitLoop(
        serverId: String,
        secret: String,
        stopped: AtomicBoolean,
        onProc: (Process?) -> Unit
    ) {
        try {
            tunnelStates[serverId] = LinuxTunnelState.Connecting("Preparing Playit.gg Linux agent...")
            val playitBin = ensurePlayitBinary()
            val secretFile = File(paths.rootDir, "playit.toml")
            val cmd = mutableListOf(playitBin.absolutePath, "--stdout")
            if (secret.isBlank()) {
                cmd.add("--secret_path")
                cmd.add(secretFile.absolutePath)
            }
            cmd.add("start")
            CoreLog.i(TAG, if (secret.isNotBlank()) "[Playit] Launching ${playitBin.name} (configured secret via env)" else "[Playit] Launching ${cmd.joinToString(" ")}")
            tunnelStates[serverId] = LinuxTunnelState.Connecting("Starting Playit.gg agent...")
            val pb = ProcessBuilder(cmd)
                .directory(paths.binDir)
                .redirectErrorStream(true)
            if (secret.isNotBlank()) {
                pb.environment()["PLAYIT_SECRET"] = secret.trim()
            }
            val proc = pb.start()
            onProc(proc)

            BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null && !stopped.get()) {
                    val clean = ANSI_REGEX.replace(line.orEmpty(), "").trim()
                    if (clean.isBlank()) continue
                    if (verbose) CoreLog.d(TAG, "[Playit] $clean")

                    CLAIM_URL_REGEX.find(clean)?.value?.let { claimUrl ->
                        CoreLog.i(TAG, "[Playit] Claim URL: $claimUrl")
                        tunnelStates[serverId] = LinuxTunnelState.Connecting("Open claim URL to link Playit.gg tunnel", claimUrl)
                    }
                    PLAYIT_ADDR_REGEX.find(clean)?.groupValues?.getOrNull(1)?.let { addr ->
                        val parts = addr.split(":")
                        val host = parts[0]
                        val port = parts.getOrNull(1)?.toIntOrNull() ?: 25565
                        CoreLog.i(TAG, "[Playit] Tunnel connected: $addr")
                        tunnelStates[serverId] = LinuxTunnelState.Connected(host, port, addr)
                    }
                }
            }
            val code = proc.waitFor()
            if (!stopped.get()) {
                tunnelStates[serverId] = LinuxTunnelState.Error("Playit.gg agent exited (code $code)")
            }
        } catch (e: CancellationException) {
            // Cancelled
        } catch (e: Exception) {
            if (!stopped.get()) {
                CoreLog.e(TAG, "[Playit] Error: ${e.message}", e)
                tunnelStates[serverId] = LinuxTunnelState.Error(e.message ?: "Playit.gg failed")
            }
        } finally {
            onProc(null)
            if (stopped.get()) tunnelStates[serverId] = LinuxTunnelState.Disconnected
        }
    }

    // --- UPnP / NAT-PMP Port Forwarding ---

    fun startPortForward(server: MinecraftServer) {
        scope.launch {
            val localIp = LinuxHostTelemetry.getLocalIpAddress()
            if (localIp.isNullOrBlank()) {
                pfStates[server.id] = LinuxPortForwardState.Failed("Could not detect LAN IP address")
                return@launch
            }
            pfStates[server.id] = LinuxPortForwardState.Discovering("Discovering UPnP / NAT-PMP router...")
            CoreLog.i(TAG, "[UPnP] Discovering gateway for ${server.name} ($localIp:${server.port})")

            val protocols = when (server.portForwardConfig.protocol) {
                PortForwardProtocol.TCP -> listOf("TCP")
                PortForwardProtocol.UDP -> listOf("UDP")
                PortForwardProtocol.BOTH -> listOf("TCP", "UDP")
            }
            val lease = server.portForwardConfig.leaseDurationSeconds

            val gw = upnpClient.discoverGateway()
            if (gw != null) {
                var allOk = true
                for (proto in protocols) {
                    val ok = upnpClient.addPortMapping(gw, server.port, server.port, localIp, proto, "MineServe (${server.name})", lease)
                    if (!ok) allOk = false
                }
                if (allOk) {
                    val extIp = upnpClient.getExternalIp(gw) ?: "Public-IP"
                    val direct = "$extIp:${server.port}"
                    activeMappings[server.id] = ActivePortMapping(gw, server.port, protocols)
                    pfStates[server.id] = LinuxPortForwardState.Mapped(extIp, server.port, direct, gw.friendlyName)
                    CoreLog.i(TAG, "[UPnP] Port mapped: $direct via ${gw.friendlyName}")
                    return@launch
                }
            }

            // NAT-PMP fallback
            val gwIp = LinuxHostTelemetry.getDefaultGatewayIp()
            if (gwIp != null) {
                val gwAddr = InetAddress.getByName(gwIp)
                val extIp = natPmpClient.getExternalIp(gwAddr)
                if (extIp != null) {
                    var mappedPort: Int? = null
                    for (proto in protocols) {
                        mappedPort = natPmpClient.mapPort(gwAddr, server.port, server.port, proto == "UDP", if (lease > 0) lease else 7200)
                    }
                    if (mappedPort != null) {
                        val direct = "$extIp:$mappedPort"
                        activeMappings[server.id] = ActivePortMapping(null, mappedPort, protocols, natPmpGateway = gwAddr)
                        pfStates[server.id] = LinuxPortForwardState.Mapped(extIp, mappedPort, direct, "NAT-PMP ($gwIp)")
                        CoreLog.i(TAG, "[NAT-PMP] Port mapped: $direct")
                        return@launch
                    }
                }
            }

            pfStates[server.id] = LinuxPortForwardState.Failed("No UPnP or NAT-PMP router responded. Ensure UPnP is enabled on your router, or use Bore / Playit.gg.")
        }
    }

    fun releasePortForward(serverId: String) {
        val active = activeMappings.remove(serverId)
        if (active != null) {
            scope.launch {
                if (active.gateway != null) {
                    for (proto in active.protocols) {
                        upnpClient.deletePortMapping(active.gateway, active.externalPort, proto)
                    }
                }
                if (active.natPmpGateway != null) {
                    for (proto in active.protocols) {
                        natPmpClient.mapPort(active.natPmpGateway, active.externalPort, active.externalPort, proto == "UDP", 0)
                    }
                }
            }
        }
        pfStates[serverId] = LinuxPortForwardState.Idle
    }

    fun stopAll() {
        for (id in activeTunnels.keys.toList()) stopTunnel(id)
        for (id in activeMappings.keys.toList()) releasePortForward(id)
    }
}
