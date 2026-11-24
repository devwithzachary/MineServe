package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.Permission
import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.auth.WebAuthSession
import com.devwithzachary.mineserve.auth.WebSessionManager
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.net.http.WebHttpUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class LinuxWebServer(
    private val bindHost: String,
    private val port: Int,
    private val paths: LinuxDataPaths,
    private val serverRepository: LinuxServerRepository,
    private val processManager: LinuxProcessManager,
    private val softwareManager: LinuxServerSoftwareManager,
    private val javaManager: PortableJavaManager,
    private val networkManager: LinuxNetworkManager,
    private val cloudManager: LinuxCloudManager,
    private val userRepository: UserRepository,
    private val verbose: Boolean = false
) {
    companion object {
        private const val TAG = "LinuxWebServer"
        private const val MAX_UPLOAD_BYTES = 512L * 1024L * 1024L
    }

    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null
    @Volatile var isRunning: Boolean = false
        private set

    val actualPort: Int
        get() = serverSocket?.localPort ?: port

    private val sessionManager = WebSessionManager()

    private val coreHandlers = LinuxWebCoreHandlers(
        paths = paths,
        serverRepository = serverRepository,
        processManager = processManager,
        softwareManager = softwareManager,
        javaManager = javaManager,
        cloudManager = cloudManager,
        userRepository = userRepository,
        scopeProvider = { scope }
    )

    private val extHandlers = LinuxWebExtendedHandlers(
        paths = paths,
        serverRepository = serverRepository,
        processManager = processManager,
        softwareManager = softwareManager,
        networkManager = networkManager,
        userRepository = userRepository,
        actualWebPortProvider = { actualPort }
    )

    fun start(): Boolean {
        if (isRunning) return true
        return try {
            val addr = InetAddress.getByName(bindHost)
            val ss = ServerSocket(port, 64, addr)
            serverSocket = ss
            isRunning = true
            val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope = serverScope
            serverScope.launch {
                while (isRunning && !ss.isClosed) {
                    try {
                        val client = ss.accept()
                        serverScope.launch { handleClient(client) }
                    } catch (_: Exception) {
                        if (!isRunning) break
                    }
                }
            }
            true
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed to bind LinuxWebServer on $bindHost:$port", e)
            false
        }
    }

    fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        scope?.cancel()
        scope = null
    }

    private suspend fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        try {
            socket.soTimeout = 15_000
            val rawIn = BufferedInputStream(socket.getInputStream())
            val out = socket.getOutputStream()

            val requestLine = WebHttpUtils.readAsciiLine(rawIn) ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val fullTarget = parts[1]
            val qIdx = fullTarget.indexOf('?')
            val path = if (qIdx >= 0) fullTarget.substring(0, qIdx) else fullTarget
            val queryParams = if (qIdx >= 0) WebHttpUtils.parseQueryParams(fullTarget.substring(qIdx + 1)) else emptyMap()
            if (verbose && path.startsWith("/api/")) {
                CoreLog.d(TAG, "[HTTP] $clientIp -> $method $path")
            }

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = WebHttpUtils.readAsciiLine(rawIn) ?: break
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
                }
            }
            val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L

            if (method == "GET" && !path.startsWith("/api/")) {
                serveStaticAsset(out, path)
                return
            }

            val token = WebHttpUtils.extractToken(headers["authorization"], headers["cookie"])
            val effectiveIp = WebHttpUtils.resolveEffectiveIp(clientIp, headers["x-forwarded-for"])

            val isFileUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/files/upload")
            val isPluginUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/plugins/upload")
            val isModpackUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/modpack/import")
            val isWorldImportUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/world/import")
            val isServerImport = method == "POST" && path == "/api/servers/import"
            if (isFileUpload || isPluginUpload || isModpackUpload || isWorldImportUpload || isServerImport) {
                handleBinaryUpload(
                    out, rawIn, contentLength, path, queryParams, token, effectiveIp,
                    isFileUpload, isPluginUpload, isModpackUpload, isWorldImportUpload, isServerImport
                )
                return
            }

            val body = if (contentLength in 1..(5L * 1024L * 1024L)) {
                WebHttpUtils.readFixedBytes(rawIn, contentLength.toInt()).toString(Charsets.UTF_8)
            } else ""

            if (path == "/api/login" && method == "POST") {
                handleLogin(out, effectiveIp, body)
                return
            }

            val session = sessionManager.validateSession(token)
            val currentUser = session?.let { userRepository.getUser(it.userId) }?.takeIf { it.enabled }
            if (session == null || currentUser == null) {
                if (session != null) sessionManager.invalidateSession(token)
                sendJson(out, 401, "Unauthorized", JSONObject().put("error", "Unauthorized. Please log in."))
                return
            }

            if (path == "/api/logout" && method == "POST") {
                sessionManager.invalidateSession(token)
                userRepository.recordAudit(currentUser, "LOGOUT", currentUser.username, "Signed out", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
                return
            }

            if (path == "/api/me" && method == "GET") {
                val resp = JSONObject()
                    .put("authenticated", true)
                    .put("id", currentUser.id)
                    .put("username", currentUser.username)
                    .put("role", currentUser.role.name)
                    .put("roleLabel", currentUser.role.displayName)
                    .put("restrictServers", currentUser.restrictServers)
                    .put("allowedServerIds", JSONArray(currentUser.allowedServerIds))
                    .put("permissions", JSONArray(userRepository.permissionsFor(currentUser.role).map { it.name }))
                sendJson(out, 200, "OK", resp)
                return
            }

            if (path == "/api/me/password" && method == "POST") {
                val j = parseJson(body)
                userRepository.changeOwnPassword(
                    currentUser.id,
                    j.optString("currentPassword", ""),
                    j.optString("newPassword", "")
                ).fold(
                    onSuccess = {
                        userRepository.recordAudit(currentUser, "PASSWORD_CHANGE", currentUser.username, "Changed own password", effectiveIp)
                        sendJson(out, 200, "OK", JSONObject().put("success", true))
                    },
                    onFailure = { err ->
                        sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed to change password"))
                    }
                )
                return
            }

            if (path == "/api/users" || path.startsWith("/api/users/")) {
                handleUsersRoutes(out, method, path, body, currentUser, effectiveIp)
                return
            }
            if (path == "/api/sessions" || path.startsWith("/api/sessions/")) {
                handleSessionsRoutes(out, method, path, session, currentUser, effectiveIp)
                return
            }
            if (path == "/api/audit" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.VIEW_AUDIT_LOG)) return
                val limit = queryParams["limit"]?.toIntOrNull() ?: 150
                val arr = JSONArray()
                userRepository.getAuditLog(limit).forEach { e ->
                    arr.put(
                        JSONObject()
                            .put("id", e.id).put("timestamp", e.timestamp)
                            .put("username", e.username).put("role", e.role)
                            .put("action", e.action).put("target", e.target)
                            .put("details", e.details).put("clientIp", e.clientIp)
                    )
                }
                sendJson(out, 200, "OK", JSONObject().put("entries", arr))
                return
            }

            if (path == "/api/system" && method == "GET") {
                coreHandlers.handleGetSystem(out)
                return
            }
            if (path == "/api/about" && method == "GET") {
                sendJson(
                    out,
                    200,
                    "OK",
                    LinuxDiagnosticsManager.buildAboutJson(
                        platform = com.devwithzachary.mineserve.model.ChangelogPlatform.LINUX,
                        isPlayStore = false
                    )
                )
                return
            }
            if (path == "/api/updates/check" && method == "GET") {
                sendJson(out, 200, "OK", LinuxDiagnosticsManager.checkGitHubUpdatesJson())
                return
            }
            if (path == "/api/whats-new" && method == "GET") {
                val hasServers = serverRepository.servers.value.isNotEmpty()
                val shouldAuto = LinuxDiagnosticsManager.shouldAutoShowWhatsNew(paths, hasServers)
                val resp = LinuxDiagnosticsManager.buildWhatsNewJson(
                    platform = com.devwithzachary.mineserve.model.ChangelogPlatform.LINUX,
                    showOnUpdateEnabled = LinuxDiagnosticsManager.isShowWhatsNewOnUpdateEnabled(paths),
                    lastSeenVersion = LinuxDiagnosticsManager.getLastSeenWhatsNewVersion(paths),
                    shouldAutoShow = shouldAuto
                )
                sendJson(out, 200, "OK", resp)
                return
            }
            if (path == "/api/whats-new" && method == "POST") {
                val j = parseJson(body)
                if (j.optBoolean("markSeen", false)) {
                    LinuxDiagnosticsManager.markWhatsNewSeen(paths)
                }
                if (j.has("showOnUpdateEnabled")) {
                    LinuxDiagnosticsManager.setShowWhatsNewOnUpdateEnabled(paths, j.optBoolean("showOnUpdateEnabled", true))
                }
                val resp = LinuxDiagnosticsManager.buildWhatsNewJson(
                    platform = com.devwithzachary.mineserve.model.ChangelogPlatform.LINUX,
                    showOnUpdateEnabled = LinuxDiagnosticsManager.isShowWhatsNewOnUpdateEnabled(paths),
                    lastSeenVersion = LinuxDiagnosticsManager.getLastSeenWhatsNewVersion(paths),
                    shouldAutoShow = false
                )
                sendJson(out, 200, "OK", resp)
                return
            }
            if (path == "/api/diagnostics/logcat" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                val filter = queryParams["filter"]
                val logs = LinuxDiagnosticsManager.captureLogs(filter, serverRepository, processManager)
                sendJson(out, 200, "OK", JSONObject().put("filter", filter ?: "MINESERVE_TAGS").put("logs", logs))
                return
            }
            if (path == "/api/diagnostics/selftest" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                val checks = LinuxDiagnosticsManager.runNetworkSelfTest()
                val arr = JSONArray()
                checks.forEach { c ->
                    arr.put(JSONObject().put("name", c.name).put("passed", c.passed).put("detail", c.detail))
                }
                sendJson(out, 200, "OK", JSONObject().put("checks", arr))
                return
            }
            if (path == "/api/diagnostics/bundle" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                val zipFile = LinuxDiagnosticsManager.exportDiagnosticZip(
                    paths = paths,
                    filterMode = queryParams["filter"],
                    serverRepository = serverRepository,
                    processManager = processManager
                )
                try {
                    WebHttpUtils.sendFileStreamResponse(
                        out = out,
                        contentType = "application/zip",
                        file = zipFile,
                        extraHeaders = mapOf("Content-Disposition" to "attachment; filename=\"${zipFile.name}\"")
                    )
                } finally {
                    zipFile.delete()
                }
                return
            }
            if (path == "/api/java" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                coreHandlers.handleGetJavaStatus(out)
                return
            }
            if (path == "/api/java/install" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                coreHandlers.handleInstallJava(out, body, currentUser, effectiveIp)
                return
            }
            if (path == "/api/safeguards") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                sendJson(
                    out, 200, "OK",
                    JSONObject()
                        .put("batteryAutoStopEnabled", false)
                        .put("minBatteryPercent", 15)
                        .put("thermalAutoStopEnabled", false)
                        .put("maxBatteryTempC", 45.0)
                )
                return
            }
            if (path.startsWith("/api/cloud/")) {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                handleCloudRoutes(out, method, path, body, currentUser, effectiveIp)
                return
            }
            if (path == "/api/software/types" && method == "GET") {
                coreHandlers.handleSoftwareTypes(out)
                return
            }
            if (path == "/api/software/versions" && method == "GET") {
                coreHandlers.handleSoftwareVersions(out, queryParams["type"])
                return
            }

            if (path == "/api/servers" && method == "GET") {
                val list = serverRepository.servers.value.filter { userRepository.canAccessServer(currentUser, it.id) }
                val statuses = processManager.serverStatuses.value
                val arr = JSONArray()
                list.forEach { s ->
                    arr.put(
                        JSONObject()
                            .put("id", s.id).put("name", s.name)
                            .put("type", s.type.name).put("version", s.version)
                            .put("port", s.port).put("allocatedRamMb", s.allocatedRamMb)
                            .put("status", statuses[s.id]?.name ?: "STOPPED")
                            .put("network", extHandlers.buildNetworkSummary(s))
                    )
                }
                WebHttpUtils.sendResponse(out, 200, "OK", "application/json; charset=utf-8", arr.toString().toByteArray(Charsets.UTF_8))
                return
            }
            if (path == "/api/servers" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                coreHandlers.handleCreateServer(out, body, currentUser, effectiveIp)
                return
            }
            if (path.startsWith("/api/servers/jobs/") && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                coreHandlers.handleGetJob(out, path.removePrefix("/api/servers/jobs/").trim('/'))
                return
            }

            if (path.startsWith("/api/servers/")) {
                handleServerRoutes(socket, out, method, path, queryParams, body, currentUser, token, effectiveIp)
                return
            }

            sendJson(out, 404, "Not Found", JSONObject().put("error", "Endpoint not found: $path"))
        } catch (e: Exception) {
            CoreLog.e(TAG, "Client error from $clientIp", e)
        } finally {
            runCatching { socket.close() }
        }
    }

    private suspend fun handleBinaryUpload(
        out: OutputStream,
        rawIn: InputStream,
        contentLength: Long,
        path: String,
        queryParams: Map<String, String>,
        token: String?,
        effectiveIp: String,
        isFileUpload: Boolean,
        isPluginUpload: Boolean,
        isModpackUpload: Boolean,
        isWorldImportUpload: Boolean,
        isServerImport: Boolean
    ) {
        val session = sessionManager.validateSession(token)
        val user = session?.let { userRepository.getUser(it.userId) }?.takeIf { it.enabled }
        if (session == null || user == null) {
            sendJson(out, 401, "Unauthorized", JSONObject().put("error", "Unauthorized. Please log in."))
            return
        }
        val segments = path.removePrefix("/api/servers/").split("/").filter { it.isNotBlank() }
        val serverId = if (isServerImport) null else segments.getOrNull(0).orEmpty()
        if (!isServerImport) {
            val sid = serverId.orEmpty()
            if (sid.isBlank() || sid.contains('/') || sid.contains('\\') || sid.contains("..") || sid == ".") {
                sendJson(out, 400, "Bad Request", JSONObject().put("error", "Invalid server ID"))
                return
            }
            if (serverRepository.getServer(sid) == null) {
                sendJson(out, 404, "Not Found", JSONObject().put("error", "Server not found"))
                return
            }
        }
        val perm = when {
            isServerImport -> Permission.MANAGE_HOST
            isFileUpload -> Permission.SERVER_FILES_WRITE
            else -> Permission.SERVER_SETTINGS_WRITE
        }
        if (!requirePerm(out, user, perm, serverId)) return
        if (contentLength <= 0L || contentLength > MAX_UPLOAD_BYTES) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Upload must be between 1 byte and 512 MB."))
            return
        }

        val tmp = File(paths.tmpDir, "upload_${UUID.randomUUID()}.bin")
        var keepTempForAsync = false
        try {
            WebHttpUtils.streamFixedBytesToFile(rawIn, contentLength, tmp)
            when {
                isServerImport -> coreHandlers.handleImportServerZip(out, queryParams, tmp, user, effectiveIp)
                isFileUpload -> coreHandlers.handleFileUpload(out, serverId.orEmpty(), queryParams, tmp, user, effectiveIp)
                isPluginUpload -> extHandlers.handlePluginUpload(out, serverId.orEmpty(), queryParams, tmp, user, effectiveIp)
                isModpackUpload -> {
                    keepTempForAsync = true
                    coreHandlers.handleStartModpackImport(out, serverId.orEmpty(), tmp, user, effectiveIp)
                }
                isWorldImportUpload -> extHandlers.handleWorldImport(out, serverId.orEmpty(), queryParams, tmp, user, effectiveIp)
            }
        } finally {
            if (!keepTempForAsync) tmp.delete()
        }
    }

    private fun handleLogin(out: OutputStream, clientIp: String, body: String) {
        val j = parseJson(body)
        val inputUser = j.optString("username", "").trim()
        val userLockKey = if (inputUser.isNotEmpty()) "user:${inputUser.lowercase()}" else null
        if (sessionManager.isIpLockedOut(clientIp) || (userLockKey != null && sessionManager.isIpLockedOut(userLockKey))) {
            sendJson(out, 429, "Too Many Requests", JSONObject().put("error", "Too many failed attempts. Try again in 5 minutes."))
            return
        }
        val user = userRepository.authenticate(inputUser, j.optString("password", ""))
        if (user != null) {
            sessionManager.resetFailedAttempts(clientIp)
            if (userLockKey != null) sessionManager.resetFailedAttempts(userLockKey)
            val session = sessionManager.createSession(user.id, user.username, user.role, clientIp)
            userRepository.recordAudit(user, "LOGIN", user.username, "Signed in (${user.role.displayName})", clientIp)
            val resp = JSONObject()
                .put("success", true)
                .put("token", session.token)
                .put("user", userToJson(user))
                .put("permissions", JSONArray(userRepository.permissionsFor(user.role).map { it.name }))
            WebHttpUtils.sendResponse(
                out, 200, "OK", "application/json; charset=utf-8", resp.toString().toByteArray(Charsets.UTF_8),
                mapOf("Set-Cookie" to "session=${session.token}; Path=/; HttpOnly; SameSite=Lax")
            )
        } else {
            sessionManager.recordFailedAttempt(clientIp)
            if (userLockKey != null) sessionManager.recordFailedAttempt(userLockKey)
            sendJson(out, 401, "Unauthorized", JSONObject().put("error", "Invalid username or password."))
        }
    }

    private fun handleUsersRoutes(
        out: OutputStream,
        method: String,
        path: String,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        if (!requirePerm(out, currentUser, Permission.MANAGE_USERS)) return
        if (path == "/api/users" && method == "GET") {
            val arr = JSONArray()
            userRepository.listUsers().forEach { arr.put(userToJson(it)) }
            sendJson(out, 200, "OK", JSONObject().put("users", arr))
            return
        }
        if (path == "/api/users" && method == "POST") {
            val j = parseJson(body)
            userRepository.createUser(
                username = j.optString("username", ""),
                password = j.optString("password", ""),
                role = UserRole.fromString(j.optString("role", "VIEWER")),
                restrictServers = j.optBoolean("restrictServers", false),
                allowedServerIds = parseStringArray(j.optJSONArray("allowedServerIds"))
            ).fold(
                onSuccess = { created ->
                    userRepository.recordAudit(currentUser, "USER_CREATE", created.username, "Role: ${created.role.name}", effectiveIp)
                    sendJson(out, 200, "OK", JSONObject().put("success", true).put("user", userToJson(created)))
                },
                onFailure = { err ->
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed to create user"))
                }
            )
            return
        }
        val targetId = path.removePrefix("/api/users/").trim('/')
        if (method == "PUT" || method == "POST") {
            val j = parseJson(body)
            val newPw = j.optString("password", "").takeIf { it.isNotEmpty() }
            val roleChanged = j.has("role")
            val restrictChanged = j.has("restrictServers") || j.has("allowedServerIds")
            userRepository.updateUser(
                userId = targetId,
                role = if (roleChanged) UserRole.fromString(j.optString("role")) else null,
                restrictServers = if (j.has("restrictServers")) j.optBoolean("restrictServers") else null,
                allowedServerIds = if (j.has("allowedServerIds")) parseStringArray(j.optJSONArray("allowedServerIds")) else null,
                enabled = if (j.has("enabled")) j.optBoolean("enabled") else null,
                newPassword = newPw
            ).fold(
                onSuccess = { updated ->
                    if (!updated.enabled || roleChanged || restrictChanged || newPw != null) {
                        sessionManager.revokeUserSessions(updated.id)
                    }
                    userRepository.recordAudit(currentUser, "USER_UPDATE", updated.username, "Role: ${updated.role.name}", effectiveIp)
                    sendJson(out, 200, "OK", JSONObject().put("success", true).put("user", userToJson(updated)))
                },
                onFailure = { err ->
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed to update user"))
                }
            )
            return
        }
        if (method == "DELETE") {
            val name = userRepository.getUser(targetId)?.username ?: targetId
            userRepository.deleteUser(targetId).fold(
                onSuccess = {
                    sessionManager.revokeUserSessions(targetId)
                    userRepository.recordAudit(currentUser, "USER_DELETE", name, "Deleted user", effectiveIp)
                    sendJson(out, 200, "OK", JSONObject().put("success", true))
                },
                onFailure = { err ->
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed to delete user"))
                }
            )
            return
        }
        sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
    }

    private fun handleSessionsRoutes(
        out: OutputStream,
        method: String,
        path: String,
        currentSession: WebAuthSession,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        if (!requirePerm(out, currentUser, Permission.MANAGE_USERS)) return
        if (path == "/api/sessions" && method == "GET") {
            val arr = JSONArray()
            sessionManager.listSessions().forEach { s ->
                arr.put(
                    JSONObject()
                        .put("id", s.id).put("userId", s.userId)
                        .put("username", s.username).put("role", s.role.name)
                        .put("clientIp", s.clientIp).put("createdAt", s.createdAt)
                        .put("expiresAt", s.expiresAt).put("isCurrent", s.token == currentSession.token)
                )
            }
            sendJson(out, 200, "OK", JSONObject().put("sessions", arr))
            return
        }
        if (method == "DELETE") {
            val sid = path.removePrefix("/api/sessions/").trim('/')
            val removed = sessionManager.revokeSessionById(sid)
            if (removed) userRepository.recordAudit(currentUser, "SESSION_REVOKE", sid, "Revoked session", effectiveIp)
            sendJson(out, 200, "OK", JSONObject().put("success", removed))
            return
        }
        sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
    }

    private suspend fun handleCloudRoutes(
        out: OutputStream,
        method: String,
        path: String,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        when {
            path == "/api/cloud/status" && method == "GET" -> sendJson(out, 200, "OK", cloudManager.toStatusJson())
            path == "/api/cloud/refresh" && method == "POST" -> {
                cloudManager.refreshProfile()
                sendJson(out, 200, "OK", cloudManager.toStatusJson())
            }
            path == "/api/cloud/otp/request" && method == "POST" -> {
                val email = parseJson(body).optString("email", "")
                cloudManager.requestEmailOtp(email).fold(
                    onSuccess = {
                        userRepository.recordAudit(currentUser, "CLOUD_OTP_REQUEST", email, "Requested Cloud sign-in code", effectiveIp)
                        sendJson(out, 200, "OK", JSONObject().put("success", true))
                    },
                    onFailure = { err -> sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed to send code")) }
                )
            }
            path == "/api/cloud/otp/verify" && method == "POST" -> {
                val j = parseJson(body)
                val email = j.optString("email", "")
                val code = j.optString("code", "")
                cloudManager.verifyEmailOtp(email, code).fold(
                    onSuccess = { statusJson ->
                        userRepository.recordAudit(currentUser, "CLOUD_SIGN_IN", email, "Signed in to MineServe Cloud", effectiveIp)
                        sendJson(out, 200, "OK", JSONObject().put("success", true).put("account", statusJson))
                    },
                    onFailure = { err -> sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Invalid verification code")) }
                )
            }
            path == "/api/cloud/logout" && method == "POST" -> {
                cloudManager.signOut()
                userRepository.recordAudit(currentUser, "CLOUD_SIGN_OUT", "host", "Signed out of MineServe Cloud", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            else -> sendJson(out, 404, "Not Found", JSONObject().put("error", "Unknown cloud route"))
        }
    }

    private suspend fun handleServerRoutes(
        socket: Socket,
        out: OutputStream,
        method: String,
        path: String,
        queryParams: Map<String, String>,
        body: String,
        currentUser: PanelUser,
        token: String?,
        effectiveIp: String
    ) {
        val segments = path.removePrefix("/api/servers/").split("/").filter { it.isNotBlank() }
        val serverId = segments.getOrNull(0) ?: return
        val server = serverRepository.getServer(serverId)
        if (server == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Server not found"))
            return
        }
        if (!requirePerm(out, currentUser, Permission.SERVER_VIEW, serverId)) return

        if (segments.size == 1) {
            if (method == "DELETE") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST, serverId)) return
                networkManager.stopTunnel(serverId)
                networkManager.releasePortForward(serverId)
                processManager.stopServerAndWait(serverId)
                serverRepository.deleteServer(serverId)
                userRepository.recordAudit(currentUser, "SERVER_DELETE", server.name, "Deleted server", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
                return
            }
            if (method == "GET") {
                sendJson(out, 200, "OK", buildServerDetailJson(server))
                return
            }
            sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
            return
        }

        val action = segments.getOrNull(1) ?: return
        when (action) {
            "start" -> {
                if (method != "POST") {
                    sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
                    return
                }
                if (!requirePerm(out, currentUser, Permission.SERVER_CONTROL, serverId)) return
                processManager.startServer(server)
                userRepository.recordAudit(currentUser, "SERVER_START", server.name, "Started server", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            "stop" -> {
                if (method != "POST") {
                    sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
                    return
                }
                if (!requirePerm(out, currentUser, Permission.SERVER_CONTROL, serverId)) return
                processManager.stopServer(server.id)
                userRepository.recordAudit(currentUser, "SERVER_STOP", server.name, "Stopped server", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            "restart" -> {
                if (method != "POST") {
                    sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
                    return
                }
                if (!requirePerm(out, currentUser, Permission.SERVER_CONTROL, serverId)) return
                processManager.restartServer(server)
                userRepository.recordAudit(currentUser, "SERVER_RESTART", server.name, "Restarted server", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            "delete" -> {
                if (method != "POST") {
                    sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
                    return
                }
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST, serverId)) return
                networkManager.stopTunnel(server.id)
                networkManager.releasePortForward(server.id)
                processManager.stopServerAndWait(server.id)
                serverRepository.deleteServer(server.id)
                userRepository.recordAudit(currentUser, "SERVER_DELETE", server.name, "Deleted server", effectiveIp)
                sendJson(out, 200, "OK", JSONObject().put("success", true))
            }
            "stream" -> handleServerSseStream(socket, out, server, token)
            "console" -> {
                if (method == "GET") {
                    val limit = queryParams["limit"]?.toIntOrNull()?.coerceIn(100, 2000) ?: 2000
                    sendJson(out, 200, "OK", JSONObject().put("lines", JSONArray(processManager.getRecentConsoleLines(serverId, limit))))
                } else if (method == "POST") {
                    if (!requirePerm(out, currentUser, Permission.SERVER_CONSOLE_WRITE, serverId)) return
                    val cmd = parseJson(body).optString("command", "").trim()
                    if (cmd.isNotBlank()) {
                        processManager.sendCommand(serverId, cmd)
                        userRepository.recordAudit(currentUser, "CONSOLE_CMD", server.name, cmd, effectiveIp)
                    }
                    sendJson(out, 200, "OK", JSONObject().put("success", true))
                } else {
                    sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
                }
            }
            "build-status" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                coreHandlers.handleBuildStatus(out, server)
            }
            "upgrade" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                coreHandlers.handleUpgradeServer(out, server, body, currentUser, effectiveIp)
            }
            "settings" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                coreHandlers.handleSettings(out, method, server, body, currentUser, effectiveIp)
            }
            "players" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_PLAYERS, serverId)) return
                coreHandlers.handlePlayers(out, method, server, segments.getOrNull(2), body, currentUser, effectiveIp)
            }
            "files" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_FILES_WRITE, serverId)) return
                coreHandlers.handleFilesRoutes(out, method, server, segments.getOrNull(2), queryParams, body, currentUser, effectiveIp)
            }
            "backups" -> coreHandlers.handleBackupsRoutes(out, method, server, segments.getOrNull(2), segments.getOrNull(3), queryParams, body, currentUser, effectiveIp) { perm ->
                requirePerm(out, currentUser, perm, serverId)
            }
            "plugins" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                extHandlers.handlePluginsRoutes(out, method, server, segments.getOrNull(2), queryParams, body, currentUser, effectiveIp)
            }
            "world" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                extHandlers.handleWorldRoutes(out, method, server, segments.getOrNull(2), segments.getOrNull(3), body, currentUser, effectiveIp)
            }
            "automation" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                extHandlers.handleAutomationRoutes(out, method, server, segments.getOrNull(2), segments.getOrNull(3), body, currentUser, effectiveIp)
            }
            "discord" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                extHandlers.handleDiscordRoutes(out, method, server, segments.getOrNull(2), body, currentUser, effectiveIp)
            }
            "proxy" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                val accessibleIds = if (currentUser.restrictServers) currentUser.allowedServerIds.toSet() else null
                extHandlers.handleProxyRoutes(out, method, server, body, currentUser, effectiveIp, accessibleIds)
            }
            "performance" -> extHandlers.handlePerformanceRoutes(out, method, server, segments.getOrNull(2), currentUser, effectiveIp) { perm ->
                requirePerm(out, currentUser, perm, serverId)
            }
            "network" -> {
                if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                extHandlers.handleNetworkRoutes(out, method, server, segments.getOrNull(2), body, currentUser, effectiveIp)
            }
            "livemap" -> extHandlers.handleLiveMapRoutes(out, method, server, segments.getOrNull(2), body, currentUser, effectiveIp) { perm ->
                requirePerm(out, currentUser, perm, serverId)
            }
            "modpack" -> {
                if (segments.getOrNull(2) == "jobs" && method == "GET") {
                    coreHandlers.handleGetModpackJob(out, serverId, segments.getOrNull(3).orEmpty())
                } else {
                    sendJson(out, 404, "Not Found", JSONObject().put("error", "Unknown modpack route"))
                }
            }
            else -> sendJson(out, 404, "Not Found", JSONObject().put("error", "Unknown server action: $action"))
        }
    }

    private suspend fun handleServerSseStream(
        socket: Socket,
        out: OutputStream,
        server: MinecraftServer,
        token: String?
    ) {
        try {
            socket.soTimeout = 0
            val header = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream; charset=utf-8\r\nCache-Control: no-cache\r\nConnection: keep-alive\r\nX-Accel-Buffering: no\r\n\r\n"
            out.write(header.toByteArray(Charsets.UTF_8))
            out.flush()

            var lastRefreshTs = -1L
            var lastStatusSentAt = 0L
            var lastStatusStr = ""
            var lastKeepaliveAt = System.currentTimeMillis()

            while (isRunning && !socket.isClosed) {
                val activeSession = sessionManager.validateSession(token) ?: break
                val activeUser = userRepository.getUser(activeSession.userId) ?: break
                if (!activeUser.enabled || !userRepository.hasPermission(activeUser, Permission.SERVER_VIEW, server.id)) {
                    break
                }
                val latestServer = serverRepository.getServer(server.id) ?: break

                val now = System.currentTimeMillis()
                val refreshTs = processManager.refreshTriggers.value[server.id] ?: 0L
                if (refreshTs != lastRefreshTs) {
                    lastRefreshTs = refreshTs
                    val lines = processManager.getRecentConsoleLines(server.id, 2000)
                    val payload = JSONObject().put("lines", JSONArray(lines)).toString()
                    out.write("event: console\ndata: $payload\n\n".toByteArray(Charsets.UTF_8))
                    out.flush()
                    lastKeepaliveAt = now
                }
                val currentStatus = processManager.getServerStatus(server.id).name
                if (currentStatus != lastStatusStr || now - lastStatusSentAt >= 2000L) {
                    lastStatusStr = currentStatus
                    lastStatusSentAt = now
                    val payload = buildServerDetailJson(latestServer).toString()
                    out.write("event: status\ndata: $payload\n\n".toByteArray(Charsets.UTF_8))
                    out.flush()
                    lastKeepaliveAt = now
                } else if (now - lastKeepaliveAt >= 10_000L) {
                    out.write(": keepalive\n\n".toByteArray(Charsets.UTF_8))
                    out.flush()
                    lastKeepaliveAt = now
                }
                delay(250)
            }
        } catch (_: Exception) {
            // Client disconnected
        }
    }

    private fun buildServerDetailJson(server: MinecraftServer): JSONObject {
        val status = processManager.getServerStatus(server.id).name
        val m = processManager.serverMetrics.value[server.id]
        val metricsObj = JSONObject()
        if (m != null) {
            metricsObj.put("isRunning", m.isRunning)
            metricsObj.put("cpuPercentage", m.cpuPercentage.toDouble())
            metricsObj.put("ramUsedMb", m.ramUsedMb)
            metricsObj.put("ramMaxMb", m.ramMaxMb)
            metricsObj.put("onlinePlayerCount", m.onlinePlayerCount)
            metricsObj.put("onlinePlayers", JSONArray(m.onlinePlayers))
            metricsObj.put("tps", m.tps)
            metricsObj.put("mspt", m.mspt)
            metricsObj.put("uptimeSeconds", m.uptimeSeconds)
        }
        return JSONObject()
            .put("id", server.id).put("name", server.name)
            .put("type", server.type.name).put("version", server.version)
            .put("port", server.port).put("allocatedRamMb", server.allocatedRamMb)
            .put("status", status).put("metrics", metricsObj)
            .put("network", extHandlers.buildNetworkSummary(server))
    }

    private fun serveStaticAsset(out: OutputStream, path: String) {
        val clean = when (path) {
            "/", "/index.html" -> "webadmin/index.html"
            "/app.js" -> "webadmin/app.js"
            "/styles.css" -> "webadmin/styles.css"
            "/logo.png" -> "webadmin/logo.png"
            else -> null
        }
        if (clean == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Asset not found"))
            return
        }
        val stream = javaClass.classLoader.getResourceAsStream(clean)
        if (stream == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Asset missing from classpath: $clean"))
            return
        }
        val bytes = stream.use { it.readBytes() }
        val contentType = when {
            clean.endsWith(".html") -> "text/html; charset=utf-8"
            clean.endsWith(".js") -> "application/javascript; charset=utf-8"
            clean.endsWith(".css") -> "text/css; charset=utf-8"
            clean.endsWith(".png") -> "image/png"
            else -> "application/octet-stream"
        }
        WebHttpUtils.sendResponse(out, 200, "OK", contentType, bytes)
    }

    private fun requirePerm(out: OutputStream, user: PanelUser, permission: Permission, serverId: String? = null): Boolean {
        if (userRepository.hasPermission(user, permission, serverId)) return true
        sendJson(out, 403, "Forbidden", JSONObject().put("error", "Insufficient permissions (${user.role.displayName})."))
        return false
    }

    private fun userToJson(user: PanelUser): JSONObject = JSONObject()
        .put("id", user.id).put("username", user.username)
        .put("role", user.role.name).put("roleLabel", user.role.displayName)
        .put("restrictServers", user.restrictServers)
        .put("allowedServerIds", JSONArray(user.allowedServerIds))
        .put("enabled", user.enabled).put("createdAt", user.createdAt)
        .put("lastLoginAt", user.lastLoginAt ?: 0L)

    private fun parseJson(body: String): JSONObject =
        runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "").trim()
                if (s.isNotBlank()) add(s)
            }
        }
    }

    private fun sendJson(out: OutputStream, code: Int, text: String, obj: JSONObject) {
        WebHttpUtils.sendResponse(out, code, text, "application/json; charset=utf-8", obj.toString().toByteArray(Charsets.UTF_8))
    }
}
