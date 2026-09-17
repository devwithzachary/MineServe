package com.devwithzachary.mineserve.engine

import android.util.Log
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class StandbyPingListener(
    val server: MinecraftServer,
    private val onWakeRequested: (MinecraftServer) -> Unit,
    private val onBindFailed: ((MinecraftServer, Exception) -> Unit)? = null
) {
    companion object {
        private const val TAG = "StandbyPingListener"
        private const val MAX_BIND_ATTEMPTS = 30
        private const val BIND_RETRY_DELAY_MS = 500L

        // RakNet Unconnected Ping/Pong Magic (16 bytes)
        private val RAKNET_MAGIC = byteArrayOf(
            0x00.toByte(), 0xff.toByte(), 0xff.toByte(), 0x00.toByte(),
            0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(),
            0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(),
            0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte()
        )
    }

    private val isRunning = AtomicBoolean(false)
    private val isTcpBound = AtomicBoolean(false)
    private val isUdpBound = AtomicBoolean(false)
    private var tcpServerSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun isRunning(): Boolean = isRunning.get()

    fun isListening(): Boolean {
        if (!isRunning.get()) return false
        val tcpOk = tcpServerSocket?.isBound == true && tcpServerSocket?.isClosed == false
        val udpOk = udpSocket?.isBound == true && udpSocket?.isClosed == false
        return if (server.type == ServerType.BEDROCK_GEYSER) {
            tcpOk || udpOk
        } else {
            tcpOk
        }
    }

    fun start() {
        if (!isRunning.compareAndSet(false, true)) return

        scope.launch {
            startTcpListener()
        }

        if (server.type == ServerType.BEDROCK_GEYSER) {
            scope.launch {
                startUdpListener()
            }
        }
    }

    private suspend fun startTcpListener() {
        var attempts = 0
        var ss: ServerSocket? = null

        while (isRunning.get() && attempts < MAX_BIND_ATTEMPTS) {
            var candidate: ServerSocket? = null
            try {
                candidate = ServerSocket()
                candidate.reuseAddress = true
                candidate.bind(InetSocketAddress(server.port))
                ss = candidate
                tcpServerSocket = candidate
                isTcpBound.set(true)
                Log.i(TAG, "Standby TCP listener active on port ${server.port} for server ${server.name} (${server.id})")
                break
            } catch (e: java.net.SocketException) {
                try { candidate?.close() } catch (_: Exception) {}
                attempts++
                Log.d(TAG, "Standby TCP port ${server.port} currently in use, retrying in ${BIND_RETRY_DELAY_MS}ms... (attempt $attempts/$MAX_BIND_ATTEMPTS)")
                delay(BIND_RETRY_DELAY_MS)
            } catch (e: Exception) {
                try { candidate?.close() } catch (_: Exception) {}
                Log.e(TAG, "Unexpected error binding Standby TCP listener on port ${server.port}", e)
                onBindFailed?.invoke(server, e)
                return
            }
        }

        if (ss == null || ss.isClosed) {
            Log.e(TAG, "Failed to bind Standby TCP port ${server.port} after $MAX_BIND_ATTEMPTS attempts")
            onBindFailed?.invoke(server, java.net.BindException("Port ${server.port} in use after $MAX_BIND_ATTEMPTS retries"))
            return
        }

        try {
            while (isRunning.get() && !ss.isClosed) {
                val clientSocket = try {
                    ss.accept()
                } catch (e: Exception) {
                    break
                }
                handleTcpClient(clientSocket)
            }
        } finally {
            try { ss.close() } catch (_: Exception) {}
            if (tcpServerSocket === ss) {
                tcpServerSocket = null
                isTcpBound.set(false)
            }
        }
    }

    private fun handleTcpClient(socket: Socket) {
        scope.launch {
            var isMinecraftHandshake = false
            try {
                socket.soTimeout = 3000
                val input = socket.getInputStream()
                val output = socket.getOutputStream()

                // Read Handshake
                val packetLen = readVarInt(input)
                if (packetLen > 0) {
                    val packetId = readVarInt(input)
                    if (packetId == 0x00) {
                        isMinecraftHandshake = true
                        val protocolVersion = readVarInt(input)
                        val addressLen = readVarInt(input)
                        val addressBytes = ByteArray(addressLen.coerceIn(0, 255))
                        if (addressLen > 0) input.read(addressBytes)
                        val portHigh = input.read()
                        val portLow = input.read()
                        val nextState = readVarInt(input) // 1 = Status, 2 = Login

                        if (nextState == 1) {
                            // Status Request
                            val reqLen = readVarInt(input)
                            if (reqLen > 0) {
                                val reqId = readVarInt(input)
                                if (reqId == 0x00) {
                                    // Send Status Response
                                    val motdJson = buildStatusJson(server.name)
                                    sendPacket(output, 0x00) { packetOut ->
                                        writeString(packetOut, motdJson)
                                    }

                                    // Handle optional Ping
                                    try {
                                        val pingLen = readVarInt(input)
                                        if (pingLen == 9) {
                                            val pingId = readVarInt(input)
                                            if (pingId == 0x01) {
                                                val payload = ByteArray(8)
                                                input.read(payload)
                                                sendPacket(output, 0x01) { packetOut ->
                                                    packetOut.write(payload)
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Standby client exchange note: ${e.message}")
            } finally {
                try { socket.close() } catch (_: Exception) {}
                if (isMinecraftHandshake) {
                    triggerWake()
                }
            }
        }
    }

    private suspend fun startUdpListener() {
        val bedrockPort = if (server.port in 1..65535) server.port else 19132
        var attempts = 0
        var ds: DatagramSocket? = null

        while (isRunning.get() && attempts < MAX_BIND_ATTEMPTS) {
            var candidate: DatagramSocket? = null
            try {
                candidate = DatagramSocket(null)
                candidate.reuseAddress = true
                candidate.bind(InetSocketAddress(bedrockPort))
                ds = candidate
                udpSocket = candidate
                isUdpBound.set(true)
                Log.i(TAG, "Standby UDP listener active on port $bedrockPort for server ${server.name}")
                break
            } catch (e: java.net.SocketException) {
                try { candidate?.close() } catch (_: Exception) {}
                attempts++
                Log.d(TAG, "Standby UDP port $bedrockPort currently in use, retrying in ${BIND_RETRY_DELAY_MS}ms... (attempt $attempts/$MAX_BIND_ATTEMPTS)")
                delay(BIND_RETRY_DELAY_MS)
            } catch (e: Exception) {
                try { candidate?.close() } catch (_: Exception) {}
                Log.d(TAG, "Standby UDP socket on port $bedrockPort: ${e.message}")
                return
            }
        }

        if (ds == null || ds.isClosed) {
            Log.d(TAG, "Could not bind Standby UDP port $bedrockPort after $MAX_BIND_ATTEMPTS attempts")
            return
        }

        try {
            val buf = ByteArray(1500)
            while (isRunning.get() && !ds.isClosed) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    ds.receive(packet)
                    handleUdpPacket(ds, packet, bedrockPort)
                } catch (e: Exception) {
                    break
                }
            }
        } finally {
            try { ds.close() } catch (_: Exception) {}
            if (udpSocket === ds) {
                udpSocket = null
                isUdpBound.set(false)
            }
        }
    }

    private fun handleUdpPacket(ds: DatagramSocket, packet: DatagramPacket, bedrockPort: Int) {
        val data = packet.data
        val len = packet.length
        if (len < 1) return

        val packetId = data[0].toInt() and 0xFF
        // RakNet Unconnected Ping (0x01) or Open Connection Request (0x05)
        if (packetId == 0x01 || packetId == 0x02) {
            try {
                // Send RakNet Unconnected Pong (0x1c)
                val clientTime = if (len >= 9) {
                    ByteBuffer.wrap(data, 1, 8).long
                } else {
                    System.currentTimeMillis()
                }

                val cleanName = server.name.replace(";", "")
                val motd = "MCPE;§e⚡ $cleanName (Standby);589;1.20.0;0;20;${server.id};MineServe;Survival;1;$bedrockPort;$bedrockPort;"
                val motdBytes = motd.toByteArray(Charsets.UTF_8)

                val pongBuffer = ByteBuffer.allocate(1 + 8 + 8 + 16 + 2 + motdBytes.size)
                pongBuffer.put(0x1c.toByte()) // ID_UNCONNECTED_PONG
                pongBuffer.putLong(clientTime)
                pongBuffer.putLong(0x0000000012345678L) // Server GUID
                pongBuffer.put(RAKNET_MAGIC)
                pongBuffer.putShort(motdBytes.size.toShort())
                pongBuffer.put(motdBytes)

                val pongBytes = pongBuffer.array()
                val responsePacket = DatagramPacket(pongBytes, pongBytes.size, packet.socketAddress)
                ds.send(responsePacket)
            } catch (e: Exception) {
                Log.d(TAG, "Error sending Bedrock standby pong: ${e.message}")
            }
            triggerWake()
        } else if (packetId == 0x05) {
            // Open connection request
            triggerWake()
        }
    }

    private fun triggerWake() {
        if (isRunning.compareAndSet(true, false)) {
            Log.i(TAG, "Standby ping detected for server ${server.name}! Triggering auto-wake...")
            closeSockets()
            scope.cancel()
            onWakeRequested(server)
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            closeSockets()
            scope.cancel()
        }
    }

    private fun closeSockets() {
        try {
            tcpServerSocket?.close()
        } catch (_: Exception) {}
        tcpServerSocket = null
        isTcpBound.set(false)

        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null
        isUdpBound.set(false)
    }

    private fun buildStatusJson(serverName: String): String {
        val cleanName = serverName.replace("\"", "\\\"")
        return """
        {
          "version": {
            "name": "MineServe Standby",
            "protocol": -1
          },
          "players": {
            "max": 20,
            "online": 0,
            "sample": []
          },
          "description": {
            "text": "§e⚡ $cleanName (Standby)\n§aPinging wakes the server! Booting up now..."
          }
        }
        """.trimIndent().replace("\n", " ")
    }

    private fun readVarInt(input: InputStream): Int {
        var value = 0
        var length = 0
        while (true) {
            val currentByte = input.read()
            if (currentByte == -1) throw EOFException("VarInt stream ended prematurely")
            value = value or ((currentByte and 0x7F) shl (length * 7))
            length++
            if (length > 5) throw RuntimeException("VarInt is too big")
            if ((currentByte and 0x80) != 0x80) break
        }
        return value
    }

    private fun writeVarInt(output: OutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                output.write(v)
                return
            } else {
                output.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
    }

    private fun writeString(output: OutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        writeVarInt(output, bytes.size)
        output.write(bytes)
    }

    private fun sendPacket(output: OutputStream, packetId: Int, block: (OutputStream) -> Unit) {
        val payloadStream = ByteArrayOutputStream()
        writeVarInt(payloadStream, packetId)
        block(payloadStream)
        val payload = payloadStream.toByteArray()

        writeVarInt(output, payload.size)
        output.write(payload)
        output.flush()
    }
}

class StandbyPingManager private constructor() {
    companion object {
        val instance by lazy { StandbyPingManager() }
    }

    private val listeners = ConcurrentHashMap<String, StandbyPingListener>()

    fun startStandby(
        server: MinecraftServer,
        onBindFailed: ((MinecraftServer, Exception) -> Unit)? = null,
        onWake: (MinecraftServer) -> Unit
    ) {
        stopStandby(server.id)
        val listener = StandbyPingListener(
            server = server,
            onWakeRequested = { wokeServer ->
                listeners.remove(wokeServer.id)
                onWake(wokeServer)
            },
            onBindFailed = { failedServer, e ->
                listeners.remove(failedServer.id)
                onBindFailed?.invoke(failedServer, e)
            }
        )
        listeners[server.id] = listener
        listener.start()
    }

    fun stopStandby(serverId: String) {
        listeners.remove(serverId)?.stop()
    }

    fun stopAll() {
        for ((_, listener) in listeners) {
            listener.stop()
        }
        listeners.clear()
    }

    fun isStandbyActive(serverId: String): Boolean {
        val listener = listeners[serverId] ?: return false
        return listener.isRunning()
    }

    fun isStandbyListening(serverId: String): Boolean {
        val listener = listeners[serverId] ?: return false
        return listener.isListening()
    }

    fun getStandbyCount(): Int = listeners.size

    fun getStandbyPorts(): List<Int> = listeners.values.map { it.server.port }
}

