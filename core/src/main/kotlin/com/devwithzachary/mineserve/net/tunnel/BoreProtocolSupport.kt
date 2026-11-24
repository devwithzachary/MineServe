package com.devwithzachary.mineserve.net.tunnel

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Shared pure-Kotlin Bore protocol framing, HMAC-SHA256 challenge/response authentication,
 * and bidirectional socket stream piping used by both Android and Linux tunnel clients.
 */
object BoreProtocolSupport {

    fun writeNullTerminatedMessage(output: OutputStream, message: String) {
        output.write(message.toByteArray(Charsets.UTF_8))
        output.write(0)
        output.flush()
    }

    fun readNullTerminatedMessage(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) return if (baos.size() > 0) String(baos.toByteArray(), Charsets.UTF_8) else null
            if (b == 0) return String(baos.toByteArray(), Charsets.UTF_8)
            baos.write(b)
        }
    }

    fun computeChallengeHmacTag(challengeUuidStr: String, secret: String): String {
        val uuid = UUID.fromString(challengeUuidStr)
        val challengeBytes = ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()
        val key = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(key, "HmacSHA256"))
        }
        return mac.doFinal(challengeBytes).joinToString("") { "%02x".format(it) }
    }

    fun pipeStreams(input: InputStream, output: OutputStream, bufferSize: Int = 16384) {
        val buf = ByteArray(bufferSize)
        try {
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                output.write(buf, 0, n)
                output.flush()
            }
        } catch (_: Exception) {
            // Stream closed
        }
    }
}
