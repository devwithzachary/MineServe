package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.engine.StandbyPingListener
import com.devwithzachary.mineserve.engine.StandbyPingManager
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerAutomationConfig
import com.devwithzachary.mineserve.model.ServerType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class StandbyPingListenerTest {

    private fun findFreePort(): Int {
        val ss = ServerSocket(0)
        val port = ss.localPort
        ss.close()
        return port
    }

    private fun createTestServer(port: Int, type: ServerType = ServerType.PAPER): MinecraftServer {
        return MinecraftServer(
            id = "test-standby-server",
            name = "Test Survival Server",
            type = type,
            version = "1.21.1",
            port = port,
            allocatedRamMb = 2048,
            javaVersion = 21,
            motd = "Test MOTD",
            automationConfig = ServerAutomationConfig(
                idleSleepEnabled = true,
                idleSleepTimeoutMinutes = 10,
                autoWakeOnPing = true
            )
        )
    }

    @Test
    fun testJavaEditionTcpPingAndWake() = runBlocking {
        val port = findFreePort()
        val server = createTestServer(port, ServerType.PAPER)
        val wakeLatch = CountDownLatch(1)
        val wokeServerRef = java.util.concurrent.atomic.AtomicReference<MinecraftServer?>(null)

        val listener = StandbyPingListener(
            server = server,
            onWakeRequested = { woke ->
                wokeServerRef.set(woke)
                wakeLatch.countDown()
            }
        )

        listener.start()

        // Wait for listener to bind
        var bound = false
        for (i in 0 until 20) {
            if (listener.isListening()) {
                bound = true
                break
            }
            delay(100)
        }
        assertTrue("Listener should successfully bind to port $port", bound)
        assertTrue(listener.isRunning())

        // Connect a mock Minecraft Java client
        val client = Socket("127.0.0.1", port)
        client.soTimeout = 5000
        val out = client.getOutputStream()
        val inp = client.getInputStream()

        // 1. Send Handshake packet (ID 0x00, protocolVersion 765, address "127.0.0.1", port, nextState 1 [status])
        val handshakePayload = ByteArrayOutputStream()
        writeVarInt(handshakePayload, 0x00) // Packet ID 0
        writeVarInt(handshakePayload, 765) // Protocol version
        writeString(handshakePayload, "127.0.0.1")
        handshakePayload.write((port shr 8) and 0xFF)
        handshakePayload.write(port and 0xFF)
        writeVarInt(handshakePayload, 1) // nextState = 1 (status)

        val handshakeBytes = handshakePayload.toByteArray()
        writeVarInt(out, handshakeBytes.size)
        out.write(handshakeBytes)
        out.flush()

        // 2. Send Status Request packet (ID 0x00, empty payload)
        val statusRequestPayload = ByteArrayOutputStream()
        writeVarInt(statusRequestPayload, 0x00)
        val statusReqBytes = statusRequestPayload.toByteArray()
        writeVarInt(out, statusReqBytes.size)
        out.write(statusReqBytes)
        out.flush()

        // 3. Read Status Response packet
        val respLen = readVarInt(inp)
        assertTrue("Response length should be > 0", respLen > 0)
        val packetId = readVarInt(inp)
        assertEquals(0x00, packetId)

        val jsonStr = readString(inp)
        assertTrue("MOTD should contain server name", jsonStr.contains("Test Survival Server"))
        assertTrue("MOTD should contain Standby indicator", jsonStr.contains("MineServe Standby"))

        client.close()

        // Verify that wake was triggered
        val woken = wakeLatch.await(5, TimeUnit.SECONDS)
        assertTrue("Wake should be requested when client ping completes", woken)
        assertEquals("test-standby-server", wokeServerRef.get()?.id)
        assertFalse("Listener should stop running after wake", listener.isRunning())
    }

    @Test
    fun testBedrockUdpPingAndWake() = runBlocking {
        val port = findFreePort()
        val server = createTestServer(port, ServerType.BEDROCK_GEYSER)
        val wakeLatch = CountDownLatch(1)
        val wokeServerRef = java.util.concurrent.atomic.AtomicReference<MinecraftServer?>(null)

        val listener = StandbyPingListener(
            server = server,
            onWakeRequested = { woke ->
                wokeServerRef.set(woke)
                wakeLatch.countDown()
            }
        )

        listener.start()

        // Wait for listener to bind
        var bound = false
        for (i in 0 until 20) {
            if (listener.isListening()) {
                bound = true
                break
            }
            delay(100)
        }
        assertTrue("Listener should successfully bind UDP to port $port", bound)

        // Send a RakNet Unconnected Ping packet (0x01)
        val raknetMagic = byteArrayOf(
            0x00.toByte(), 0xff.toByte(), 0xff.toByte(), 0x00.toByte(),
            0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(),
            0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(),
            0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte()
        )

        val pingBuffer = ByteBuffer.allocate(1 + 8 + 16 + 8)
        pingBuffer.put(0x01.toByte()) // ID_UNCONNECTED_PING
        val testTime = 1234567890L
        pingBuffer.putLong(testTime)
        pingBuffer.put(raknetMagic)
        pingBuffer.putLong(9876543210L) // Client GUID

        val pingBytes = pingBuffer.array()
        val clientSocket = DatagramSocket()
        clientSocket.soTimeout = 5000
        val targetAddr = InetAddress.getByName("127.0.0.1")
        val sendPacket = DatagramPacket(pingBytes, pingBytes.size, targetAddr, port)
        clientSocket.send(sendPacket)

        // Read RakNet Unconnected Pong packet
        val recvBuf = ByteArray(1500)
        val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
        clientSocket.receive(recvPacket)

        assertEquals(0x1c.toByte(), recvPacket.data[0]) // ID_UNCONNECTED_PONG
        val returnedTime = ByteBuffer.wrap(recvPacket.data, 1, 8).long
        assertEquals(testTime, returnedTime)

        clientSocket.close()

        val woken = wakeLatch.await(5, TimeUnit.SECONDS)
        assertTrue("Bedrock ping should trigger wake", woken)
        assertEquals("test-standby-server", wokeServerRef.get()?.id)
        assertFalse(listener.isRunning())
    }

    @Test
    fun testStandbyManagerLifecycle() = runBlocking {
        val port = findFreePort()
        val server = createTestServer(port)
        val manager = StandbyPingManager.instance

        val woke = AtomicBoolean(false)
        manager.startStandby(server) {
            woke.set(true)
        }

        assertTrue(manager.isStandbyActive(server.id))
        assertTrue(manager.getStandbyCount() >= 1)
        assertTrue(manager.getStandbyPorts().contains(port))

        manager.stopStandby(server.id)
        assertFalse(manager.isStandbyActive(server.id))
    }

    @Test
    fun testPortBindingRetryWhenPortInitiallyBusy() = runBlocking {
        val port = findFreePort()

        // Temporarily occupy the port
        val blockingSocket = ServerSocket(port)
        assertTrue(blockingSocket.isBound)

        val server = createTestServer(port)
        val wakeLatch = CountDownLatch(1)
        val listener = StandbyPingListener(
            server = server,
            onWakeRequested = { wakeLatch.countDown() }
        )

        // Start listener while port is busy
        listener.start()

        // Initially, the listener should not be listening yet because port is held
        delay(200)
        assertFalse("Listener should not be listening while port is blocked", listener.isListening())

        // Now free the port
        blockingSocket.close()

        // Within a couple retries (up to 2 seconds), listener should acquire the port
        var bound = false
        for (i in 0 until 30) {
            if (listener.isListening()) {
                bound = true
                break
            }
            delay(100)
        }

        assertTrue("Listener should have retried and bound to port $port once freed", bound)
        listener.stop()
    }

    // Helper VarInt and String methods for testing
    private fun writeVarInt(out: OutputStream, value: Int) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                out.write(v)
                return
            } else {
                out.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
        }
    }

    private fun readVarInt(inp: InputStream): Int {
        var value = 0
        var length = 0
        while (true) {
            val b = inp.read()
            if (b == -1) error("EOF")
            value = value or ((b and 0x7F) shl (length * 7))
            length++
            if (length > 5) error("VarInt too big")
            if ((b and 0x80) != 0x80) break
        }
        return value
    }

    private fun writeString(out: OutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        writeVarInt(out, bytes.size)
        out.write(bytes)
    }

    private fun readString(inp: InputStream): String {
        val len = readVarInt(inp)
        val bytes = ByteArray(len)
        var read = 0
        while (read < len) {
            val r = inp.read(bytes, read, len - read)
            if (r == -1) error("EOF while reading string")
            read += r
        }
        return String(bytes, Charsets.UTF_8)
    }
}
