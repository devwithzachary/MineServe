package com.devwithzachary.mineserve.tunnel

import android.content.Context
import android.os.Build
import android.util.Log
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.net.tunnel.BoreProtocolSupport
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class BoreTunnelClient(
    private val context: Context? = null,
    private val relayHost: String = "bore.pub",
    private val relayPort: Int = 7835,
    private val localPort: Int = 25565,
    private val provider: TunnelProvider = TunnelProvider.BORE,
    private val requestedRemotePort: Int = 0,
    private val secret: String = "",
    private val onStateChanged: (TunnelState) -> Unit
) {
    companion object {
        private const val TAG = "BoreTunnelClient"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val BUFFER_SIZE = 8192
        private const val MAX_PORT_RECLAIM_ATTEMPTS = 10
        private val LISTENING_REGEX = Regex("""listening at\s+([a-zA-Z0-9_.-]+):([0-9]+)""")
        private val ANSI_REGEX = Regex("\u001B\\[[0-9;]*[a-zA-Z]")
    }

    private var tunnelJob: Job? = null
    private var clientScope: CoroutineScope? = null
    private val activeConnectionsCount = AtomicInteger(0)
    private val isExplicitlyStopped = AtomicBoolean(false)
    private var controlSocket: Socket? = null
    private var nativeProcess: Process? = null
    private var assignedPort: Int = 0

    fun start(scope: CoroutineScope) {
        stop()
        isExplicitlyStopped.set(false)
        clientScope = scope
        tunnelJob = scope.launch(Dispatchers.IO) {
            val nativeLibDir = context?.applicationInfo?.nativeLibraryDir?.let { File(it) }
            val bundledBore = nativeLibDir?.let { File(it, "libbore.so") }
            if (bundledBore != null && bundledBore.exists() && bundledBore.canExecute() && relayPort == 7835 && requestedRemotePort == 0) {
                runNativeBoreLoop(scope, bundledBore)
            } else {
                runTunnelLoop(scope)
            }
        }
    }

    fun stop() {
        isExplicitlyStopped.set(true)
        tunnelJob?.cancel()
        tunnelJob = null
        clientScope = null
        try {
            controlSocket?.setSoLinger(true, 0)
        } catch (_: Exception) {}
        try {
            controlSocket?.close()
        } catch (_: Exception) {}
        controlSocket = null
        destroyProcessSafely(nativeProcess)
        nativeProcess = null
        onStateChanged(TunnelState.Disconnected)
    }

    private fun destroyProcessSafely(proc: Process?) {
        if (proc == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                proc.destroyForcibly()
            } else {
                proc.destroy()
            }
        } catch (_: Exception) {}
    }

    private suspend fun runNativeBoreLoop(parentScope: CoroutineScope, boreBin: File) {
        var retryDelayMs = 2000L
        val effectiveLocalPort = if (localPort in 1..65535) localPort else 25565

        while (parentScope.isActive && !isExplicitlyStopped.get()) {
            try {
                if (isExplicitlyStopped.get()) break
                onStateChanged(TunnelState.Connecting("Connecting to $relayHost via native bore..."))
                Log.i(TAG, "Starting native Bore tunnel to $relayHost for local port $effectiveLocalPort")

                val cmd = mutableListOf(
                    boreBin.absolutePath,
                    "local",
                    effectiveLocalPort.toString(),
                    "--to",
                    relayHost
                )

                val pb = ProcessBuilder(cmd)
                context?.filesDir?.let { pb.directory(it) }
                context?.filesDir?.let { pb.environment()["HOME"] = it.absolutePath }
                context?.cacheDir?.let { pb.environment()["TMPDIR"] = it.absolutePath }
                pb.redirectErrorStream(true)

                val proc = pb.start()
                nativeProcess = proc

                val reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
                while (parentScope.isActive && !isExplicitlyStopped.get()) {
                    val rawLine = reader.readLine() ?: break
                    val line = ANSI_REGEX.replace(rawLine, "").trim()
                    Log.d(TAG, "[bore] $line")

                    val match = LISTENING_REGEX.find(line)
                    if (match != null) {
                        val host = match.groupValues[1]
                        val port = match.groupValues[2].toIntOrNull() ?: 0
                        assignedPort = port
                        val fullAddress = "$host:$port"
                        retryDelayMs = 2000L
                        Log.i(TAG, "Native Bore tunnel connected: $fullAddress")
                        onStateChanged(
                            TunnelState.Connected(
                                publicHost = host,
                                publicPort = port,
                                fullAddress = fullAddress,
                                provider = provider,
                                assignedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                val exitCode = proc.waitFor()
                Log.i(TAG, "Native Bore process exited with code $exitCode")

            } catch (e: CancellationException) {
                Log.d(TAG, "Native Bore tunnel cancelled gracefully")
                break
            } catch (e: Exception) {
                if (isExplicitlyStopped.get() || !parentScope.isActive) {
                    Log.d(TAG, "Native Bore tunnel stopped intentionally")
                    break
                }
                Log.e(TAG, "Native Bore tunnel error: ${e.message}", e)
                onStateChanged(TunnelState.Error(e.message ?: "Bore tunnel connection failed"))
            } finally {
                destroyProcessSafely(nativeProcess)
                nativeProcess = null
            }

            if (!parentScope.isActive || isExplicitlyStopped.get()) break
            Log.d(TAG, "Reconnecting native Bore in ${retryDelayMs}ms...")
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 1.5).toLong().coerceAtMost(30000L)
        }

        if (isExplicitlyStopped.get()) {
            onStateChanged(TunnelState.Disconnected)
        }
    }

    private suspend fun runTunnelLoop(parentScope: CoroutineScope) {
        var retryDelayMs = 2000L
        var portReclaimAttempts = 0
        val effectiveLocalPort = if (localPort in 1..65535) localPort else 25565

        while (parentScope.isActive && !isExplicitlyStopped.get()) {
            var socket: Socket? = null
            try {
                if (isExplicitlyStopped.get()) break
                if (portReclaimAttempts == 0) {
                    onStateChanged(TunnelState.Connecting("Connecting to $relayHost:$relayPort..."))
                }
                Log.d(TAG, "Connecting to bore relay at $relayHost:$relayPort for local port $effectiveLocalPort")

                socket = Socket()
                socket.connect(InetSocketAddress(relayHost, relayPort), CONNECT_TIMEOUT_MS)
                socket.soTimeout = 0
                socket.tcpNoDelay = true
                socket.keepAlive = true
                controlSocket = socket

                val inputStream = socket.getInputStream()
                val outputStream = socket.getOutputStream()

                if (secret.isNotBlank()) {
                    authenticateIfRequired(inputStream, outputStream)
                }

                // Send Hello Handshake with requested port (0 for random public port)
                val helloJson = JSONObject().apply {
                    put("Hello", requestedRemotePort)
                }
                writeNullTerminatedMessage(outputStream, helloJson.toString())
                Log.d(TAG, "Sent Hello to bore relay: $helloJson")

                // Read Hello Response (null-terminated JSON)
                val responseLine = readNullTerminatedMessage(inputStream)
                if (responseLine == null) {
                    if (isExplicitlyStopped.get()) break
                    throw Exception("Relay closed connection during handshake")
                }
                Log.d(TAG, "Relay handshake response: $responseLine")

                val responseJson = JSONObject(responseLine)
                if (responseJson.has("Error")) {
                    val errorMsg = responseJson.getString("Error")
                    throw Exception("Relay error: $errorMsg")
                }

                if (!responseJson.has("Hello")) {
                    throw Exception("Unexpected greeting response: $responseLine")
                }

                assignedPort = responseJson.getInt("Hello")
                val fullAddress = "$relayHost:$assignedPort"
                Log.i(TAG, "Public tunnel active! Allocated address: $fullAddress (forwarding to 127.0.0.1:$effectiveLocalPort)")
                retryDelayMs = 2000L
                portReclaimAttempts = 0

                val connectedState = TunnelState.Connected(
                    publicHost = relayHost,
                    publicPort = assignedPort,
                    fullAddress = fullAddress,
                    provider = provider,
                    assignedAt = System.currentTimeMillis(),
                    activeConnections = activeConnectionsCount.get()
                )
                if (!isExplicitlyStopped.get()) {
                    onStateChanged(connectedState)
                }

                // Listen for incoming proxy connections from relay (Delimited by null byte \0)
                while (parentScope.isActive && !isExplicitlyStopped.get()) {
                    val messageStr = readNullTerminatedMessage(inputStream) ?: break
                    if (messageStr.isBlank()) continue

                    try {
                        if (messageStr.startsWith("{")) {
                            val messageJson = JSONObject(messageStr)
                            if (messageJson.has("Connection")) {
                                val connId = messageJson.getString("Connection")
                                Log.d(TAG, "Incoming client proxy request: $connId")
                                launchProxyConnection(parentScope, connId, effectiveLocalPort)
                            } else if (messageJson.has("Error")) {
                                val errorMsg = messageJson.getString("Error")
                                Log.e(TAG, "Server error message: $errorMsg")
                                throw Exception("Server error: $errorMsg")
                            }
                        } else if (messageStr == "\"Heartbeat\"" || messageStr == "Heartbeat") {
                            Log.v(TAG, "Received heartbeat ping from relay")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error handling message: $messageStr", e)
                    }
                }

                if (isExplicitlyStopped.get()) break
                throw Exception("Control connection closed by relay")

            } catch (e: CancellationException) {
                Log.d(TAG, "Tunnel cancelled gracefully")
                break
            } catch (e: Exception) {
                if (isExplicitlyStopped.get() || !parentScope.isActive) {
                    Log.d(TAG, "Tunnel stopped intentionally")
                    break
                }
                if (e is java.net.SocketException && (e.message?.contains("closed", ignoreCase = true) == true || e.message?.contains("EBADF", ignoreCase = true) == true)) {
                    Log.d(TAG, "Tunnel socket closed intentionally")
                    break
                }
                Log.e(TAG, "Tunnel connection failed: ${e.message}", e)
                val isPortReclaim = e.message?.contains("port already in use", ignoreCase = true) == true
                if (isPortReclaim) {
                    portReclaimAttempts++
                    if (portReclaimAttempts > MAX_PORT_RECLAIM_ATTEMPTS) {
                        onStateChanged(
                            TunnelState.Error(
                                "Relay port is still in use (another device may be using this subdomain, or the previous session has not expired yet). Try reconnecting in a moment."
                            )
                        )
                        break
                    }
                    retryDelayMs = when {
                        portReclaimAttempts <= 3 -> 2000L
                        portReclaimAttempts <= 6 -> 3500L
                        else -> 5000L
                    }
                    onStateChanged(
                        TunnelState.Connecting("Reclaiming relay port (attempt $portReclaimAttempts/$MAX_PORT_RECLAIM_ATTEMPTS)...")
                    )
                } else {
                    portReclaimAttempts = 0
                    onStateChanged(TunnelState.Error(e.message ?: "Tunnel connection lost"))
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {}
                controlSocket = null
            }

            if (!parentScope.isActive || isExplicitlyStopped.get()) break
            Log.d(TAG, "Reconnecting in ${retryDelayMs}ms...")
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 1.5).toLong().coerceAtMost(30000L)
        }

        if (isExplicitlyStopped.get()) {
            onStateChanged(TunnelState.Disconnected)
        }
    }

    private fun launchProxyConnection(scope: CoroutineScope, connId: String, targetLocalPort: Int) {
        scope.launch(Dispatchers.IO) {
            var relayProxySocket: Socket? = null
            var localServerSocket: Socket? = null
            activeConnectionsCount.incrementAndGet()
            notifyConnectionCount()

            try {
                Log.i(TAG, "Connecting proxy to bore relay $relayHost:$relayPort for connId $connId")
                // 1. Connect to relay control host
                relayProxySocket = Socket().apply {
                    connect(InetSocketAddress(relayHost, relayPort), CONNECT_TIMEOUT_MS)
                    tcpNoDelay = true
                    soTimeout = 0
                }

                // 2. Authenticate if relay requires secret, then send Accept Handshake
                if (secret.isNotBlank()) {
                    authenticateIfRequired(relayProxySocket.getInputStream(), relayProxySocket.getOutputStream())
                }
                val acceptJson = JSONObject().apply {
                    put("Accept", connId)
                }
                writeNullTerminatedMessage(relayProxySocket.getOutputStream(), acceptJson.toString())
                Log.i(TAG, "Sent Accept for $connId, connecting to local Minecraft server 127.0.0.1:$targetLocalPort")

                // 3. Connect to local Minecraft server
                localServerSocket = Socket().apply {
                    connect(InetSocketAddress("127.0.0.1", targetLocalPort), CONNECT_TIMEOUT_MS)
                    tcpNoDelay = true
                    soTimeout = 0
                }
                Log.i(TAG, "Successfully linked proxy connection $connId to Minecraft server on port $targetLocalPort")

                // 4. Pipe bidirectionally raw bytes
                val relayIn = relayProxySocket.getInputStream()
                val relayOut = relayProxySocket.getOutputStream()
                val localIn = localServerSocket.getInputStream()
                val localOut = localServerSocket.getOutputStream()

                val relayToLocal = launch {
                    BoreProtocolSupport.pipeStreams(relayIn, localOut, BUFFER_SIZE)
                    try { localServerSocket.shutdownOutput() } catch (_: Exception) {}
                }

                val localToRelay = launch {
                    BoreProtocolSupport.pipeStreams(localIn, relayOut, BUFFER_SIZE)
                    try { relayProxySocket.shutdownOutput() } catch (_: Exception) {}
                }

                relayToLocal.join()
                localToRelay.join()

            } catch (e: Exception) {
                if (!isExplicitlyStopped.get()) {
                    Log.w(TAG, "Proxy connection $connId ended: ${e.message}", e)
                }
            } finally {
                try { relayProxySocket?.close() } catch (_: Exception) {}
                try { localServerSocket?.close() } catch (_: Exception) {}
                activeConnectionsCount.decrementAndGet().coerceAtLeast(0)
                notifyConnectionCount()
            }
        }
    }

    private fun authenticateIfRequired(inputStream: InputStream, outputStream: OutputStream) {
        val challengeLine = readNullTerminatedMessage(inputStream)
            ?: throw Exception("Relay closed connection before auth challenge")
        val challengeJson = JSONObject(challengeLine)
        if (challengeJson.has("Error")) {
            throw Exception("Relay error: ${challengeJson.getString("Error")}")
        }
        val challengeStr = challengeJson.optString("Challenge", "")
        if (challengeStr.isBlank()) {
            throw Exception("Expected bore auth challenge, got: $challengeLine")
        }
        val tag = BoreProtocolSupport.computeChallengeHmacTag(challengeStr, secret)
        val authJson = JSONObject().put("Authenticate", tag)
        writeNullTerminatedMessage(outputStream, authJson.toString())
    }

    private fun readNullTerminatedMessage(inputStream: InputStream): String? =
        try {
            BoreProtocolSupport.readNullTerminatedMessage(inputStream)
        } catch (e: Exception) {
            if (isExplicitlyStopped.get()) null else throw e
        }

    private fun writeNullTerminatedMessage(outputStream: OutputStream, msg: String) =
        BoreProtocolSupport.writeNullTerminatedMessage(outputStream, msg)

    private fun notifyConnectionCount() {
        if (assignedPort > 0 && !isExplicitlyStopped.get()) {
            val count = activeConnectionsCount.get()
            onStateChanged(
                TunnelState.Connected(
                    publicHost = relayHost,
                    publicPort = assignedPort,
                    fullAddress = "$relayHost:$assignedPort",
                    provider = provider,
                    activeConnections = count
                )
            )
        }
    }
}
