package com.devwithzachary.mineserve.admin.web

import android.os.Build
import android.os.Debug
import android.util.Log
import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.repository.ServerRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class WebAdminServer(
    val port: Int,
    private val security: WebAdminSecurity,
    private val configProvider: () -> WebAdminConfig,
    private val serverRepository: ServerRepository,
    private val processManager: ServerProcessManager
) {
    companion object {
        private const val TAG = "WebAdminServer"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    fun start() {
        if (isRunning) return
        try {
            val ss = ServerSocket(port)
            serverSocket = ss
            isRunning = true
            Log.i(TAG, "Web Admin server listening on port $port")

            acceptJob = scope.launch {
                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!ss.isClosed) {
                            Log.e(TAG, "Accept error on port $port", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting Web Admin server on port $port", e)
            isRunning = false
        }
    }

    fun stop() {
        isRunning = false
        acceptJob?.cancel()
        acceptJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        Log.i(TAG, "Web Admin server stopped")
    }

    private suspend fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        try {
            socket.soTimeout = 10000 // 10s socket timeout
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: run {
                socket.close()
                return
            }

            val parts = requestLine.split(" ")
            if (parts.size < 2) {
                socket.close()
                return
            }
            val method = parts[0].uppercase()
            val rawUri = parts[1]
            val path = rawUri.substringBefore("?")
            val rawQuery = if (rawUri.contains("?")) rawUri.substringAfter("?") else ""
            val queryParams = parseQueryParams(rawQuery)

            // Read Headers
            val headers = mutableMapOf<String, String>()
            while (true) {
                val headerLine = reader.readLine() ?: break
                if (headerLine.isEmpty()) break
                val colonIdx = headerLine.indexOf(':')
                if (colonIdx > 0) {
                    val key = headerLine.substring(0, colonIdx).trim().lowercase()
                    val value = headerLine.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            // Read Body if present
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0 && contentLength < 5 * 1024 * 1024) { // Max 5MB
                val buf = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val r = reader.read(buf, readTotal, contentLength - readTotal)
                    if (r < 0) break
                    readTotal += r
                }
                String(buf, 0, readTotal)
            } else {
                ""
            }

            // Handle CORS preflight
            if (method == "OPTIONS") {
                sendResponse(out, 204, "No Content", "text/plain", ByteArray(0))
                return
            }

            // Static Root / SPA HTML
            if (path == "/" || path == "/index.html") {
                val htmlBytes = WebAdminHtml.HTML.toByteArray(Charsets.UTF_8)
                sendResponse(out, 200, "OK", "text/html; charset=utf-8", htmlBytes)
                return
            }

            // Extract Auth Token
            val authHeader = headers["authorization"]
            val cookieHeader = headers["cookie"]
            val token = extractToken(authHeader, cookieHeader)

            // Public Endpoint: Login
            if (path == "/api/login" && method == "POST") {
                handleLogin(out, clientIp, body)
                return
            }

            // Protected API Endpoints: Require valid session
            val session = security.validateSession(token)
            if (session == null) {
                val errObj = JSONObject().put("error", "Unauthorized. Please log in.")
                sendResponse(out, 401, "Unauthorized", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Logout
            if (path == "/api/logout" && method == "POST") {
                security.invalidateSession(token)
                val resp = JSONObject().put("success", true)
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Session Verification
            if (path == "/api/me" && method == "GET") {
                val resp = JSONObject().put("authenticated", true).put("username", session.username)
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // System Overview
            if (path == "/api/system" && method == "GET") {
                val rt = Runtime.getRuntime()
                val totalRam = rt.totalMemory() / (1024 * 1024)
                val freeRam = rt.freeMemory() / (1024 * 1024)
                val resp = JSONObject()
                    .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                    .put("android", Build.VERSION.RELEASE)
                    .put("totalRamMb", totalRam)
                    .put("freeRamMb", freeRam)
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // List Servers
            if (path == "/api/servers" && method == "GET") {
                val servers = serverRepository.servers.value
                val statuses = processManager.serverStatuses.value
                val arr = JSONArray()
                for (s in servers) {
                    val status = statuses[s.id]?.name ?: "STOPPED"
                    val obj = JSONObject()
                        .put("id", s.id)
                        .put("name", s.name)
                        .put("type", s.type.name)
                        .put("version", s.version)
                        .put("port", s.port)
                        .put("allocatedRamMb", s.allocatedRamMb)
                        .put("status", status)
                    arr.put(obj)
                }
                sendResponse(out, 200, "OK", "application/json", arr.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Server Specific Endpoints: /api/servers/{id}/...
            if (path.startsWith("/api/servers/")) {
                val subPath = path.removePrefix("/api/servers/")
                val segments = subPath.split("/").filter { it.isNotBlank() }
                if (segments.isNotEmpty()) {
                    val serverId = segments[0]
                    val server = serverRepository.getServer(serverId)
                    if (server == null) {
                        val err = JSONObject().put("error", "Server not found")
                        sendResponse(out, 404, "Not Found", "application/json", err.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    // 1. GET /api/servers/{id}
                    if (segments.size == 1 && method == "GET") {
                        val status = processManager.getServerStatus(serverId).name
                        val metrics = processManager.serverMetrics.value[serverId]
                        val metricsObj = JSONObject()
                        if (metrics != null) {
                            metricsObj.put("isRunning", metrics.isRunning)
                            metricsObj.put("cpuPercentage", metrics.cpuPercentage.toDouble())
                            metricsObj.put("ramUsedMb", metrics.ramUsedMb)
                            metricsObj.put("ramMaxMb", metrics.ramMaxMb)
                            metricsObj.put("onlinePlayerCount", metrics.onlinePlayerCount)
                            metricsObj.put("onlinePlayers", JSONArray(metrics.onlinePlayers))
                            metricsObj.put("tps", metrics.tps)
                            metricsObj.put("mspt", metrics.mspt)
                            metricsObj.put("uptimeSeconds", metrics.uptimeSeconds)
                        }
                        val resp = JSONObject()
                        resp.put("id", server.id)
                        resp.put("name", server.name)
                        resp.put("type", server.type.name)
                        resp.put("version", server.version)
                        resp.put("port", server.port)
                        resp.put("allocatedRamMb", server.allocatedRamMb)
                        resp.put("status", status)
                        resp.put("metrics", metricsObj)
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    val action = segments.getOrNull(1)

                    // 2. POST /api/servers/{id}/start
                    if (action == "start" && method == "POST") {
                        val serverDir = serverRepository.getServerDirectory(server.id)
                        processManager.startServer(server, serverDir)
                        val resp = JSONObject().put("success", true).put("message", "Server start requested")
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    // 3. POST /api/servers/{id}/stop
                    if (action == "stop" && method == "POST") {
                        processManager.stopServer(server.id)
                        val resp = JSONObject().put("success", true).put("message", "Server stop requested")
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    // 4. POST /api/servers/{id}/restart
                    if (action == "restart" && method == "POST") {
                        val serverDir = serverRepository.getServerDirectory(server.id)
                        processManager.restartServer(server, serverDir)
                        val resp = JSONObject().put("success", true).put("message", "Server restart requested")
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    // 5. GET & POST /api/servers/{id}/console
                    if (action == "console") {
                        if (method == "GET") {
                            val lines = processManager.getRecentConsoleLines(serverId, 200)
                            val resp = JSONObject().put("lines", JSONArray(lines))
                            sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        } else if (method == "POST") {
                            val jsonBody = JSONObject(if (body.isBlank()) "{}" else body)
                            val cmd = jsonBody.optString("command", "").trim()
                            if (cmd.isNotBlank()) {
                                processManager.sendCommand(serverId, cmd)
                            }
                            val resp = JSONObject().put("success", true)
                            sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }
                    }

                    // 6. Files Endpoints
                    if (action == "files") {
                        val fileAction = segments.getOrNull(2)

                        // GET /api/servers/{id}/files?path=...
                        if (fileAction == null && method == "GET") {
                            val targetPath = queryParams["path"] ?: ""
                            val files = serverRepository.listDirectory(serverId, targetPath)
                            val arr = JSONArray()
                            for (f in files) {
                                val obj = JSONObject()
                                    .put("name", f.name)
                                    .put("relativePath", f.relativePath)
                                    .put("isDirectory", f.isDirectory)
                                    .put("sizeBytes", f.sizeBytes)
                                    .put("isEditable", f.isEditable)
                                    .put("lastModified", f.lastModified)
                                arr.put(obj)
                            }
                            val resp = JSONObject().put("path", targetPath).put("files", arr)
                            sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // GET /api/servers/{id}/files/read?path=...
                        if (fileAction == "read" && method == "GET") {
                            val targetPath = queryParams["path"] ?: ""
                            val content = serverRepository.readFile(serverId, targetPath)
                            val resp = JSONObject().put("path", targetPath).put("content", content)
                            sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // POST /api/servers/{id}/files/write
                        if (fileAction == "write" && method == "POST") {
                            val jsonBody = JSONObject(if (body.isBlank()) "{}" else body)
                            val targetPath = jsonBody.optString("path", "")
                            val content = jsonBody.optString("content", "")
                            val ok = serverRepository.writeFile(serverId, targetPath, content)
                            val resp = JSONObject().put("success", ok)
                            sendResponse(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // POST /api/servers/{id}/files/delete
                        if (fileAction == "delete" && method == "POST") {
                            val jsonBody = JSONObject(if (body.isBlank()) "{}" else body)
                            val targetPath = jsonBody.optString("path", "")
                            val ok = serverRepository.deleteFileOrDirectory(serverId, targetPath)
                            val resp = JSONObject().put("success", ok)
                            sendResponse(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }
                    }
                }
            }

            // 404 Catch-All
            val notFoundObj = JSONObject().put("error", "Endpoint not found: $path")
            sendResponse(out, 404, "Not Found", "application/json", notFoundObj.toString().toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client request from $clientIp", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun handleLogin(out: OutputStream, clientIp: String, body: String) {
        if (security.isIpLockedOut(clientIp)) {
            val errObj = JSONObject().put("error", "Too many failed attempts. Temporary lockout for 5 minutes.")
            sendResponse(out, 429, "Too Many Requests", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
            return
        }

        val json = try { JSONObject(if (body.isBlank()) "{}" else body) } catch (_: Exception) { JSONObject() }
        val inputUser = json.optString("username", "").trim()
        val inputPass = json.optString("password", "")

        val cfg = configProvider()
        val isValid = security.verifyCredentials(inputUser, inputPass, cfg.username, cfg.password)

        if (isValid) {
            security.resetFailedAttempts(clientIp)
            val session = security.createSession(cfg.username, clientIp)
            val cookie = "session=${session.token}; Path=/; HttpOnly; SameSite=Lax"
            val resp = JSONObject().put("success", true).put("token", session.token)
            sendResponse(
                out = out,
                statusCode = 200,
                statusText = "OK",
                contentType = "application/json",
                body = resp.toString().toByteArray(Charsets.UTF_8),
                extraHeaders = mapOf("Set-Cookie" to cookie)
            )
        } else {
            security.recordFailedAttempt(clientIp)
            val errObj = JSONObject().put("error", "Invalid username or password.")
            sendResponse(out, 401, "Unauthorized", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
        }
    }

    private fun extractToken(authHeader: String?, cookieHeader: String?): String? {
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ", ignoreCase = true)) {
            return authHeader.substring(7).trim()
        }
        if (!cookieHeader.isNullOrBlank()) {
            val cookies = cookieHeader.split(";").map { it.trim() }
            for (c in cookies) {
                if (c.startsWith("session=")) {
                    return c.substring(8).trim()
                }
            }
        }
        return null
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val k = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[k] = v
            } else if (pair.isNotBlank()) {
                map[URLDecoder.decode(pair, "UTF-8")] = ""
            }
        }
        return map
    }

    private fun sendResponse(
        out: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val sb = java.lang.StringBuilder()
        sb.append("HTTP/1.1 $statusCode $statusText\r\n")
        sb.append("Content-Type: $contentType\r\n")
        sb.append("Content-Length: ${body.size}\r\n")
        sb.append("Connection: close\r\n")
        sb.append("Access-Control-Allow-Origin: *\r\n")
        sb.append("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
        sb.append("Access-Control-Allow-Headers: Content-Type, Authorization, Cookie\r\n")
        sb.append("X-Content-Type-Options: nosniff\r\n")
        sb.append("X-Frame-Options: SAMEORIGIN\r\n")
        for ((k, v) in extraHeaders) {
            sb.append("$k: $v\r\n")
        }
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
        if (body.isNotEmpty()) {
            out.write(body)
        }
        out.flush()
    }
}
