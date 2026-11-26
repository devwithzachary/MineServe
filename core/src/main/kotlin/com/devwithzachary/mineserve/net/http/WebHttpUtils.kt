package com.devwithzachary.mineserve.net.http

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder

/**
 * Shared low-level HTTP request parsing and response streaming utilities used by both
 * Android's `WebAdminServer` and Desktop's `DesktopWebServer`.
 */
object WebHttpUtils {

    fun extractToken(authHeader: String?, cookieHeader: String?): String? {
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ", ignoreCase = true)) {
            return authHeader.substring(7).trim()
        }
        if (!cookieHeader.isNullOrBlank()) {
            for (part in cookieHeader.split(";")) {
                val kv = part.trim().split("=", limit = 2)
                if (kv.size == 2 && kv[0].trim() == "session") {
                    return kv[1].trim()
                }
            }
        }
        return null
    }

    fun resolveEffectiveIp(socketIp: String, xForwardedFor: String?): String {
        return if (socketIp == "127.0.0.1" || socketIp == "::1") {
            xForwardedFor?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() } ?: socketIp
        } else {
            socketIp
        }
    }

    fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val k = runCatching { URLDecoder.decode(pair.substring(0, idx), "UTF-8") }.getOrDefault(pair.substring(0, idx))
                val v = runCatching { URLDecoder.decode(pair.substring(idx + 1), "UTF-8") }.getOrDefault(pair.substring(idx + 1))
                map[k] = v
            }
        }
        return map
    }

    fun readAsciiLine(input: InputStream): String? {
        val baos = ByteArrayOutputStream()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) return if (baos.size() == 0) null else baos.toString("ISO-8859-1")
            if (b == '\n'.code) {
                val bytes = baos.toByteArray()
                val len = if (prev == '\r'.code && bytes.isNotEmpty()) bytes.size - 1 else bytes.size
                return String(bytes, 0, len, Charsets.ISO_8859_1)
            }
            baos.write(b)
            prev = b
        }
    }

    fun readFixedBytes(input: InputStream, length: Int): ByteArray {
        val buf = ByteArray(length)
        var read = 0
        while (read < length) {
            val r = input.read(buf, read, length - read)
            if (r == -1) break
            read += r
        }
        return if (read == length) buf else buf.copyOf(read)
    }

    fun streamFixedBytesToFile(input: InputStream, length: Long, destFile: File) {
        FileOutputStream(destFile).use { out ->
            val buf = ByteArray(65536)
            var remaining = length
            while (remaining > 0L) {
                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                val r = input.read(buf, 0, toRead)
                if (r == -1) break
                out.write(buf, 0, r)
                remaining -= r
            }
            out.flush()
        }
    }

    fun sendResponse(
        out: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val sb = StringBuilder()
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n")
        sb.append("Content-Type: ").append(contentType).append("\r\n")
        sb.append("Content-Length: ").append(body.size).append("\r\n")
        sb.append("Connection: close\r\n")
        extraHeaders.forEach { (k, v) -> sb.append(k).append(": ").append(v).append("\r\n") }
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
    }

    fun sendFileStreamResponse(
        out: OutputStream,
        contentType: String,
        file: File,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val sb = StringBuilder()
        sb.append("HTTP/1.1 200 OK\r\n")
        sb.append("Content-Type: ").append(contentType).append("\r\n")
        sb.append("Content-Length: ").append(file.length()).append("\r\n")
        sb.append("Connection: close\r\n")
        extraHeaders.forEach { (k, v) -> sb.append(k).append(": ").append(v).append("\r\n") }
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
        FileInputStream(file).use { fis ->
            val buf = ByteArray(65536)
            while (true) {
                val r = fis.read(buf)
                if (r == -1) break
                out.write(buf, 0, r)
            }
        }
        out.flush()
    }
}
