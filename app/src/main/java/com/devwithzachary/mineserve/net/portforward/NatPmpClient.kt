package com.devwithzachary.mineserve.net.portforward

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NatPmpClient {
    companion object {
        private const val TAG = "NatPmpClient"
        private const val NAT_PMP_PORT = 5351
        private const val TIMEOUT_MS = 2500
    }

    suspend fun getExternalIp(gatewayIp: InetAddress): String? = withContext(Dispatchers.IO) {
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                val requestData = byteArrayOf(0x00, 0x00) // Version 0, Opcode 0 (External IP)
                val sendPacket = DatagramPacket(requestData, requestData.size, gatewayIp, NAT_PMP_PORT)
                socket.send(sendPacket)

                val buffer = ByteArray(12)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)

                if (receivePacket.length >= 12 && buffer[0].toInt() == 0 && (buffer[1].toInt() and 0xFF) == 128) {
                    val resultCode = ByteBuffer.wrap(buffer, 2, 2).order(ByteOrder.BIG_ENDIAN).short.toInt()
                    if (resultCode == 0) {
                        val ip = InetAddress.getByAddress(buffer.copyOfRange(8, 12)).hostAddress
                        Log.i(TAG, "NAT-PMP discovered external IP: $ip")
                        return@withContext ip
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "NAT-PMP getExternalIp skipped/failed: ${e.message}")
            null
        }
    }

    suspend fun mapPort(
        gatewayIp: InetAddress,
        internalPort: Int,
        externalPort: Int,
        isUdp: Boolean,
        lifetimeSeconds: Int = 7200
    ): Int? = withContext(Dispatchers.IO) {
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                val opcode = if (isUdp) 1 else 2 // 1: UDP, 2: TCP
                val buffer = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
                buffer.put(0.toByte()) // version
                buffer.put(opcode.toByte()) // opcode
                buffer.putShort(0.toShort()) // reserved
                buffer.putShort(internalPort.toShort())
                buffer.putShort(externalPort.toShort())
                buffer.putInt(lifetimeSeconds)

                val reqBytes = buffer.array()
                val sendPacket = DatagramPacket(reqBytes, reqBytes.size, gatewayIp, NAT_PMP_PORT)
                socket.send(sendPacket)

                val respBuffer = ByteArray(16)
                val receivePacket = DatagramPacket(respBuffer, respBuffer.size)
                socket.receive(receivePacket)

                if (receivePacket.length >= 16 && respBuffer[0].toInt() == 0 && (respBuffer[1].toInt() and 0xFF) == (128 + opcode)) {
                    val bb = ByteBuffer.wrap(respBuffer).order(ByteOrder.BIG_ENDIAN)
                    bb.position(2)
                    val resultCode = bb.short.toInt()
                    if (resultCode == 0) {
                        bb.position(10)
                        val mappedExternalPort = bb.short.toInt() and 0xFFFF
                        Log.i(TAG, "NAT-PMP successfully mapped port $internalPort -> $mappedExternalPort (${if (isUdp) "UDP" else "TCP"})")
                        return@withContext mappedExternalPort
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "NAT-PMP mapPort skipped/failed: ${e.message}")
            null
        }
    }
}

