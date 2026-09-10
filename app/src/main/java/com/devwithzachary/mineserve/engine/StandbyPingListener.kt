package com.devwithzachary.mineserve.engine

import android.util.Log
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class StandbyPingListener(
    val server: MinecraftServer,
    private val onWakeRequested: (MinecraftServer) -> Unit
) {
    companion object {
        private const val TAG = "StandbyPingListener"
    }

    private val isRunning = AtomicBoolean(false)
    private var tcpServerSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

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

    private fun startTcpListener() {
        try {
            val ss = ServerSocket()
            ss.reuseAddress = true
            ss.bind(InetSocketAddress(server.port))
            tcpServerSocket = ss
            Log.i(TAG, "Standby TCP listener active on port ${server.port} for server ${server.name} (${server.id})")

            while (isRunning.get() && !ss.isClosed) {
                val clientSocket = try {
                    ss.accept()
                } catch (e: Exception) {
                    break
                }
                handleTcpClient(clientSocket)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Standby TCP listener on port ${server.port}", e)
        } finally {
            closeSockets()
        }
    }

    private fun handleTcpClient(socket: Socket) {
        scope.launch {
            try {
                socket.soTimeout = 3000
                val input = socket.getInputStream()
                val output = socket.getOutputStream()

                // Read Handshake
                val packetLen = readVarInt(input)
                if (packetLen > 0) {
                    val packetId = readVarInt(input)
                    if (packetId == 0x00) {
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
                triggerWake()
            }
        }
    }

    private fun startUdpListener() {
        val bedrockPort = 19132
        try {
            val ds = DatagramSocket(null)
            ds.reuseAddress = true
            ds.bind(InetSocketAddress(bedrockPort))
            udpSocket = ds
            Log.i(TAG, "Standby UDP listener active on port $bedrockPort for server ${server.name}")

            val buf = ByteArray(1500)
            while (isRunning.get() && !ds.isClosed) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    ds.receive(packet)
                    triggerWake()
                    break
                } catch (e: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Standby UDP socket on port $bedrockPort: ${e.message}")
        } finally {
            closeSockets()
        }
    }

    private fun triggerWake() {
        if (isRunning.compareAndSet(true, false)) {
            Log.i(TAG, "Standby ping detected for server ${server.name}! Triggering auto-wake...")
            closeSockets()
            onWakeRequested(server)
        }
    }

    fun stop() {
        isRunning.set(false)
        closeSockets()
        job.cancel()
    }

    private fun closeSockets() {
        try {
            tcpServerSocket?.close()
        } catch (_: Exception) {}
        tcpServerSocket = null

        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null
    }

    private fun buildStatusJson(serverName: String): String {
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
            "text": "§e⚡ MineServe Auto-Wake Standby\n§aPinging wakes the server! Booting up now..."
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

    fun startStandby(server: MinecraftServer, onWake: (MinecraftServer) -> Unit) {
        stopStandby(server.id)
        val listener = StandbyPingListener(server) { wokeServer ->
            listeners.remove(wokeServer.id)
            onWake(wokeServer)
        }
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
        return listeners.containsKey(serverId)
    }

    fun getStandbyCount(): Int = listeners.size

    fun getStandbyPorts(): List<Int> = listeners.values.map { it.server.port }
}
