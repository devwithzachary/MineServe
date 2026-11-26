package com.devwithzachary.mineserve.admin.web

import android.content.Context
import android.os.Build
import android.util.Log
import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.Permission
import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.engine.ServerSoftwareManager
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.net.http.WebHttpUtils
import com.devwithzachary.mineserve.net.portforward.PortForwardManager
import com.devwithzachary.mineserve.net.portforward.PortForwardState
import com.devwithzachary.mineserve.repository.BackupRepository
import com.devwithzachary.mineserve.repository.PluginRepository
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.tunnel.TunnelManager
import com.devwithzachary.mineserve.tunnel.TunnelState
import com.devwithzachary.mineserve.ui.components.getDefaultAllocatedRamMb
import com.devwithzachary.mineserve.ui.components.getDeviceTotalMemoryMb
import com.devwithzachary.mineserve.ui.components.getMaxAllocatableRamMb
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class WebAdminServer(
    private val context: Context,
    val port: Int,
    private val security: WebAdminSecurity,
    private val userRepository: UserRepository,
    private val configProvider: () -> WebAdminConfig,
    private val serverRepository: ServerRepository,
    private val processManager: ServerProcessManager,
    private val serverSoftwareManager: ServerSoftwareManager,
    private val backupRepository: BackupRepository,
    private val pluginRepository: PluginRepository,
    private val tunnelManager: TunnelManager,
    private val portForwardManager: PortForwardManager,
    private val assetProvider: (String) -> ByteArray?
) {
    companion object {
        private const val TAG = "WebAdminServer"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val apiHandlers = WebAdminApiHandlers(context, scope, serverRepository, processManager, serverSoftwareManager)
    private val extHandlers = WebAdminExtendedHandlers(
        context = context,
        scope = scope,
        webAdminPort = port,
        serverRepository = serverRepository,
        processManager = processManager,
        serverSoftwareManager = serverSoftwareManager,
        backupRepository = backupRepository,
        pluginRepository = pluginRepository,
        tunnelManager = tunnelManager,
        portForwardManager = portForwardManager
    )
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
            socket.soTimeout = 30000 // 30s socket timeout
            val rawIn = BufferedInputStream(socket.getInputStream())
            val out = socket.getOutputStream()

            val requestLine = readHeaderLine(rawIn) ?: run {
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
                val headerLine = readHeaderLine(rawIn) ?: break
                if (headerLine.isEmpty()) break
                val colonIdx = headerLine.indexOf(':')
                if (colonIdx > 0) {
                    val key = headerLine.substring(0, colonIdx).trim().lowercase()
                    val value = headerLine.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            // Handle CORS preflight
            if (method == "OPTIONS") {
                sendResponse(out, 204, "No Content", "text/plain", ByteArray(0))
                return
            }

            // Static Web Admin Assets
            if (method == "GET" && serveStaticAsset(out, path)) {
                return
            }

            val contentLength = headers["content-length"]?.toLongOrNull() ?: 0L
            val isWorldImportUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/world/import")
            val isPluginJarUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/plugins/upload")
            val isModpackUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/modpack/import")
            val isFileUpload = method == "POST" && path.startsWith("/api/servers/") && path.endsWith("/files/upload")
            val isServerImportUpload = method == "POST" && path == "/api/servers/import"
            val isBinaryUpload = isWorldImportUpload || isPluginJarUpload || isModpackUpload || isFileUpload || isServerImportUpload

            // Extract Auth Token before reading potentially large bodies
            val authHeader = headers["authorization"]
            val cookieHeader = headers["cookie"]
            val token = extractToken(authHeader, cookieHeader)
            val effectiveIp = if (clientIp == "127.0.0.1" || clientIp == "::1") {
                headers["x-forwarded-for"]?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() } ?: clientIp
            } else {
                clientIp
            }

            if (isBinaryUpload) {
                val uploadSession = security.validateSession(token)
                val uploadUser = uploadSession?.let { userRepository.getUser(it.userId) }?.takeIf { it.enabled }
                if (uploadSession == null || uploadUser == null) {
                    val errObj = JSONObject().put("error", "Unauthorized. Please log in.")
                    sendResponse(out, 401, "Unauthorized", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
                    return
                }
                val segments = path.removePrefix("/api/servers/").split("/").filter { it.isNotBlank() }
                val serverId = if (isServerImportUpload) null else (segments.getOrNull(0) ?: "")
                if (!isServerImportUpload) {
                    val targetServer = serverId?.let { serverRepository.getServer(it) }
                    if (targetServer == null) {
                        sendResponse(
                            out,
                            404,
                            "Not Found",
                            "application/json",
                            JSONObject().put("error", "Server not found").toString().toByteArray(Charsets.UTF_8)
                        )
                        return
                    }
                }
                val uploadPerm = when {
                    isServerImportUpload -> Permission.MANAGE_HOST
                    isFileUpload -> Permission.SERVER_FILES_WRITE
                    else -> Permission.SERVER_SETTINGS_WRITE
                }
                if (!requirePerm(out, uploadUser, uploadPerm, serverId)) {
                    return
                }
                if (contentLength <= 0L || contentLength > 512L * 1024L * 1024L) {
                    val errObj = JSONObject().put("error", "Upload must be between 1 byte and 512 MB.")
                    sendResponse(out, 400, "Bad Request", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
                    return
                }
                val tempFile = File.createTempFile("web_upload_", ".bin")
                var keepTempForAsync = false
                try {
                    streamFixedBytesToFile(rawIn, contentLength, tempFile)
                    val result = when {
                        isServerImportUpload -> {
                            userRepository.recordAudit(uploadUser, "SERVER_IMPORT", queryParams["name"] ?: "imported", "Imported server archive (.zip)", effectiveIp)
                            apiHandlers.handleImportServerZip(queryParams, tempFile)
                        }
                        isWorldImportUpload -> {
                            val createBackup = queryParams["createBackup"]?.toBooleanStrictOrNull() ?: true
                            userRepository.recordAudit(uploadUser, "WORLD_IMPORT", serverId.orEmpty(), "Uploaded world archive", effectiveIp)
                            extHandlers.handleImportWorld(serverId.orEmpty(), tempFile, createBackup)
                        }
                        isPluginJarUpload -> {
                            val fname = queryParams["name"] ?: ""
                            userRepository.recordAudit(uploadUser, "PLUGIN_UPLOAD", serverId.orEmpty(), fname, effectiveIp)
                            extHandlers.handleUploadPluginJar(serverId.orEmpty(), fname, tempFile)
                        }
                        isFileUpload -> {
                            val dirPath = queryParams["path"] ?: ""
                            val rawName = queryParams["name"] ?: ""
                            val safeName = File(rawName).name.trim()
                            if (safeName.isBlank() || safeName == "." || safeName == "..") {
                                WebApiResult(400, "Bad Request", JSONObject().put("error", "Invalid file name."))
                            } else {
                                val serverDir = serverRepository.getServerDirectory(serverId.orEmpty())
                                val targetDir = if (dirPath.isBlank()) serverDir else File(serverDir, dirPath)
                                val destFile = File(targetDir, safeName)
                                val serverCanon = serverDir.canonicalPath
                                val destCanon = destFile.canonicalPath
                                if (!destCanon.startsWith(serverCanon + File.separator)) {
                                    WebApiResult(403, "Forbidden", JSONObject().put("error", "Target path is outside server directory."))
                                } else {
                                    destFile.parentFile?.mkdirs()
                                    tempFile.copyTo(destFile, overwrite = true)
                                    val relPath = if (dirPath.isBlank()) safeName else "$dirPath/$safeName"
                                    userRepository.recordAudit(uploadUser, "FILE_UPLOAD", serverId.orEmpty(), relPath, effectiveIp)
                                    WebApiResult(200, "OK", JSONObject().put("success", true).put("path", relPath))
                                }
                            }
                        }
                        else -> {
                            keepTempForAsync = true
                            userRepository.recordAudit(uploadUser, "MODPACK_IMPORT", serverId.orEmpty(), "Uploaded .mrpack modpack", effectiveIp)
                            extHandlers.handleStartModpackImport(serverId.orEmpty(), tempFile)
                        }
                    }
                    sendApiResult(out, result)
                } finally {
                    if (!keepTempForAsync) {
                        tempFile.delete()
                    }
                }
                return
            }

            // Read JSON/Text Body if present (Max 5MB)
            val body = if (contentLength > 0 && contentLength < 5L * 1024 * 1024) {
                readFixedBytes(rawIn, contentLength.toInt()).toString(Charsets.UTF_8)
            } else {
                ""
            }

            // Public Endpoint: Login
            if (path == "/api/login" && method == "POST") {
                handleLogin(out, effectiveIp, body)
                return
            }

            // Protected API Endpoints: Require valid session and active user
            val session = security.validateSession(token)
            val currentUser = session?.let { userRepository.getUser(it.userId) }?.takeIf { it.enabled }
            if (session == null || currentUser == null) {
                if (session != null) security.invalidateSession(token)
                val errObj = JSONObject().put("error", "Unauthorized. Please log in.")
                sendResponse(out, 401, "Unauthorized", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Logout
            if (path == "/api/logout" && method == "POST") {
                security.invalidateSession(token)
                userRepository.recordAudit(currentUser, "LOGOUT", currentUser.username, "Signed out", effectiveIp)
                val resp = JSONObject().put("success", true)
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Session Verification & Profile
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
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Change Own Password
            if (path == "/api/me/password" && method == "POST") {
                val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())
                val currentPass = jsonBody.optString("currentPassword", "")
                val newPass = jsonBody.optString("newPassword", "")
                val res = userRepository.changeOwnPassword(currentUser.id, currentPass, newPass)
                res.fold(
                    onSuccess = {
                        userRepository.recordAudit(currentUser, "PASSWORD_CHANGE", currentUser.username, "Changed own password", effectiveIp)
                        sendResponse(out, 200, "OK", "application/json", JSONObject().put("success", true).toString().toByteArray(Charsets.UTF_8))
                    },
                    onFailure = { err ->
                        sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", err.message ?: "Failed to change password").toString().toByteArray(Charsets.UTF_8))
                    }
                )
                return
            }

            // Multi-User Management (Owner only)
            if (path == "/api/users" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_USERS)) return
                val arr = JSONArray()
                userRepository.listUsers().forEach { arr.put(userToJson(it)) }
                sendResponse(out, 200, "OK", "application/json", JSONObject().put("users", arr).toString().toByteArray(Charsets.UTF_8))
                return
            }
            if (path == "/api/users" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_USERS)) return
                val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())
                val username = jsonBody.optString("username", "")
                val password = jsonBody.optString("password", "")
                val role = UserRole.fromString(jsonBody.optString("role", "VIEWER"))
                val restrictServers = jsonBody.optBoolean("restrictServers", false)
                val allowedArr = jsonBody.optJSONArray("allowedServerIds")
                val allowedIds = mutableListOf<String>()
                if (allowedArr != null) {
                    for (i in 0 until allowedArr.length()) {
                        val sid = allowedArr.optString(i, "").trim()
                        if (sid.isNotBlank()) allowedIds.add(sid)
                    }
                }
                userRepository.createUser(username, password, role, restrictServers, allowedIds).fold(
                    onSuccess = { created ->
                        userRepository.recordAudit(currentUser, "USER_CREATE", created.username, "Role: ${created.role.name}", effectiveIp)
                        sendResponse(out, 200, "OK", "application/json", JSONObject().put("success", true).put("user", userToJson(created)).toString().toByteArray(Charsets.UTF_8))
                    },
                    onFailure = { err ->
                        sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", err.message ?: "Failed to create user").toString().toByteArray(Charsets.UTF_8))
                    }
                )
                return
            }
            if (path.startsWith("/api/users/")) {
                if (!requirePerm(out, currentUser, Permission.MANAGE_USERS)) return
                val targetUserId = path.removePrefix("/api/users/").trim('/')
                if (method == "PUT" || method == "POST") {
                    val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())
                    val role = if (jsonBody.has("role")) UserRole.fromString(jsonBody.optString("role")) else null
                    val restrictServers = if (jsonBody.has("restrictServers")) jsonBody.optBoolean("restrictServers") else null
                    val enabled = if (jsonBody.has("enabled")) jsonBody.optBoolean("enabled") else null
                    val newPassword = jsonBody.optString("password", "").takeIf { it.isNotEmpty() }
                    val allowedIds = if (jsonBody.has("allowedServerIds")) {
                        val arr = jsonBody.optJSONArray("allowedServerIds")
                        buildList<String> {
                            if (arr != null) {
                                for (i in 0 until arr.length()) {
                                    val sid = arr.optString(i, "").trim()
                                    if (sid.isNotBlank()) add(sid)
                                }
                            }
                        }
                    } else null
                    userRepository.updateUser(targetUserId, role, restrictServers, allowedIds, enabled, newPassword).fold(
                        onSuccess = { updated ->
                            if (!updated.enabled || role != null || restrictServers != null || allowedIds != null || newPassword != null) {
                                security.revokeUserSessions(updated.id)
                            }
                            userRepository.recordAudit(
                                currentUser,
                                "USER_UPDATE",
                                updated.username,
                                "Role: ${updated.role.name}, Enabled: ${updated.enabled}",
                                effectiveIp
                            )
                            sendResponse(out, 200, "OK", "application/json", JSONObject().put("success", true).put("user", userToJson(updated)).toString().toByteArray(Charsets.UTF_8))
                        },
                        onFailure = { err ->
                            sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", err.message ?: "Failed to update user").toString().toByteArray(Charsets.UTF_8))
                        }
                    )
                    return
                } else if (method == "DELETE") {
                    val targetName = userRepository.getUser(targetUserId)?.username ?: targetUserId
                    userRepository.deleteUser(targetUserId).fold(
                        onSuccess = {
                            security.revokeUserSessions(targetUserId)
                            userRepository.recordAudit(currentUser, "USER_DELETE", targetName, "Deleted user account", effectiveIp)
                            sendResponse(out, 200, "OK", "application/json", JSONObject().put("success", true).toString().toByteArray(Charsets.UTF_8))
                        },
                        onFailure = { err ->
                            sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", err.message ?: "Failed to delete user").toString().toByteArray(Charsets.UTF_8))
                        }
                    )
                    return
                }
            }

            // Active Sessions Management (Owner only)
            if (path == "/api/sessions" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_USERS)) return
                val arr = JSONArray()
                for (s in security.listSessions()) {
                    arr.put(
                        JSONObject()
                            .put("id", s.id)
                            .put("userId", s.userId)
                            .put("username", s.username)
                            .put("role", s.role.name)
                            .put("clientIp", s.clientIp)
                            .put("createdAt", s.createdAt)
                            .put("expiresAt", s.expiresAt)
                            .put("isCurrent", s.token == session.token)
                    )
                }
                sendResponse(out, 200, "OK", "application/json", JSONObject().put("sessions", arr).toString().toByteArray(Charsets.UTF_8))
                return
            }
            if (path.startsWith("/api/sessions/") && method == "DELETE") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_USERS)) return
                val sid = path.removePrefix("/api/sessions/").trim('/')
                val revoked = security.revokeSessionById(sid)
                if (revoked) {
                    userRepository.recordAudit(currentUser, "SESSION_REVOKE", sid, "Revoked active web session", effectiveIp)
                }
                sendResponse(out, 200, "OK", "application/json", JSONObject().put("success", revoked).toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Audit Log (Owner & Admin)
            if (path == "/api/audit" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.VIEW_AUDIT_LOG)) return
                val limit = queryParams["limit"]?.toIntOrNull() ?: 150
                val arr = JSONArray()
                for (entry in userRepository.getAuditLog(limit)) {
                    arr.put(
                        JSONObject()
                            .put("id", entry.id)
                            .put("timestamp", entry.timestamp)
                            .put("username", entry.username)
                            .put("role", entry.role)
                            .put("action", entry.action)
                            .put("target", entry.target)
                            .put("details", entry.details)
                            .put("clientIp", entry.clientIp)
                    )
                }
                sendResponse(out, 200, "OK", "application/json", JSONObject().put("entries", arr).toString().toByteArray(Charsets.UTF_8))
                return
            }

            // System Overview
            if (path == "/api/system" && method == "GET") {
                val rt = Runtime.getRuntime()
                val totalRam = rt.totalMemory() / (1024 * 1024)
                val freeRam = rt.freeMemory() / (1024 * 1024)
                val deviceTotalRamMb = getDeviceTotalMemoryMb(context)
                val maxAllocatableRamMb = getMaxAllocatableRamMb(deviceTotalRamMb)
                val defaultRamMb = getDefaultAllocatedRamMb(context)
                val resp = JSONObject()
                    .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                    .put("android", Build.VERSION.RELEASE)
                    .put("platform", "android")
                    .put("version", com.devwithzachary.mineserve.BuildConfig.VERSION_NAME)
                    .put("totalRamMb", totalRam)
                    .put("freeRamMb", freeRam)
                    .put("deviceTotalRamMb", deviceTotalRamMb)
                    .put("maxAllocatableRamMb", maxAllocatableRamMb)
                    .put("defaultRamMb", defaultRamMb)
                    .put("minRamMb", 512)
                    .put("ramStepMb", 256)
                    .put("isPlayStore", com.devwithzachary.mineserve.BuildConfig.IS_PLAY_STORE)
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // About, Platform-Filtered Changelog & Open-Source Credits
            if (path == "/api/about" && method == "GET") {
                val isPlayStore = com.devwithzachary.mineserve.BuildConfig.IS_PLAY_STORE
                val platform = com.devwithzachary.mineserve.model.ChangelogPlatform.ANDROID
                val aboutData = com.devwithzachary.mineserve.model.MineServeAboutData
                val links = JSONObject()
                    .put("website", aboutData.WEBSITE_URL)
                    .put("githubRepo", aboutData.GITHUB_REPO_URL)
                    .put("githubIssues", aboutData.GITHUB_ISSUES_URL)
                    .put("discord", aboutData.DISCORD_URL)
                    .put("privacy", aboutData.PRIVACY_URL)
                    .put("license", aboutData.LICENSE_NAME)
                if (!isPlayStore) {
                    links.put("githubReleases", aboutData.GITHUB_RELEASES_URL)
                    links.put("patreon", aboutData.PATREON_URL)
                    links.put("buyMeACoffee", aboutData.BUYMEACOFFEE_URL)
                }
                val supportersArr = JSONArray()
                if (!isPlayStore) {
                    aboutData.SUPPORTERS.forEach { s ->
                        supportersArr.put(JSONObject().put("name", s.name).put("tier", s.tier))
                    }
                }
                val creditsArr = JSONArray()
                aboutData.getCreditsForPlatform(platform).forEach { c ->
                    creditsArr.put(
                        JSONObject()
                            .put("name", c.name)
                            .put("category", c.category.name)
                            .put("categoryLabel", c.category.label)
                            .put("description", c.description)
                            .put("url", c.url)
                            .put("license", c.license)
                            .put("tag", c.tag)
                    )
                }
                val changelogArr = JSONArray()
                aboutData.getChangelogForPlatform(platform).forEach { entry ->
                    changelogArr.put(
                        JSONObject()
                            .put("version", entry.version)
                            .put("date", entry.date)
                            .put("initialExpanded", entry.initialExpanded)
                            .put("highlights", JSONArray(entry.highlights))
                    )
                }
                val resp = JSONObject()
                    .put("version", com.devwithzachary.mineserve.BuildConfig.VERSION_NAME)
                    .put("versionCode", com.devwithzachary.mineserve.BuildConfig.VERSION_CODE)
                    .put("platform", "android")
                    .put("isPlayStore", isPlayStore)
                    .put("links", links)
                    .put("supporters", supportersArr)
                    .put("credits", creditsArr)
                    .put("changelog", changelogArr)
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // GitHub Release Update Checker
            if (path == "/api/updates/check" && method == "GET") {
                val currentVer = com.devwithzachary.mineserve.BuildConfig.VERSION_NAME
                if (com.devwithzachary.mineserve.BuildConfig.IS_PLAY_STORE) {
                    val resp = JSONObject()
                        .put("status", "MANAGED_BY_PLAY_STORE")
                        .put("currentVersion", currentVer)
                        .put("message", "App updates are managed via Google Play Store.")
                    sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                    return
                }
                val checker = com.devwithzachary.mineserve.api.GitHubUpdateChecker()
                val resp = when (val res = checker.checkLatestRelease(currentVer)) {
                    is com.devwithzachary.mineserve.api.UpdateCheckResult.UpToDate -> JSONObject()
                        .put("status", "UP_TO_DATE")
                        .put("currentVersion", currentVer)
                        .put("latestTag", res.latestTag.ifBlank { "v$currentVer" })
                        .put("htmlUrl", com.devwithzachary.mineserve.model.MineServeAboutData.GITHUB_RELEASES_URL)
                    is com.devwithzachary.mineserve.api.UpdateCheckResult.UpdateAvailable -> JSONObject()
                        .put("status", "UPDATE_AVAILABLE")
                        .put("currentVersion", currentVer)
                        .put("latestTag", res.release.tagName)
                        .put("releaseName", res.release.name)
                        .put("releaseNotes", res.release.body)
                        .put("htmlUrl", res.release.htmlUrl)
                        .put("publishedAt", res.release.publishedAt)
                    is com.devwithzachary.mineserve.api.UpdateCheckResult.Error -> JSONObject()
                        .put("status", "ERROR")
                        .put("currentVersion", currentVer)
                        .put("error", res.message)
                        .put("htmlUrl", com.devwithzachary.mineserve.model.MineServeAboutData.GITHUB_RELEASES_URL)
                }
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // What's New Feature Guide (Synced with Android UpdatePreferences)
            if (path == "/api/whats-new" && (method == "GET" || method == "POST")) {
                val updatePrefs = com.devwithzachary.mineserve.repository.UpdatePreferences(context)
                val release = com.devwithzachary.mineserve.model.MineServeWhatsNewCatalog.getForPlatform(
                    com.devwithzachary.mineserve.model.ChangelogPlatform.ANDROID
                )
                if (method == "POST") {
                    val j = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
                    if (j.optBoolean("markSeen", false)) {
                        updatePrefs.lastSeenWhatsNewVersion = release.majorVersion
                    }
                    if (j.has("showOnUpdateEnabled")) {
                        updatePrefs.isShowWhatsNewOnUpdateEnabled = j.optBoolean("showOnUpdateEnabled", true)
                    }
                }
                val featuresArr = JSONArray()
                release.features.forEach { f ->
                    featuresArr.put(
                        JSONObject()
                            .put("badge", f.badge)
                            .put("title", f.title)
                            .put("summary", f.summary)
                            .put("stepsToFind", JSONArray(f.stepsToFind))
                            .put("iconKey", f.iconKey)
                    )
                }
                val shouldAuto = if (method == "POST") {
                    false
                } else {
                    serverRepository.servers.value.isNotEmpty() && updatePrefs.shouldAutoShowWhatsNew(release.majorVersion)
                }
                val resp = JSONObject()
                    .put("majorVersion", release.majorVersion)
                    .put("displayVersion", release.displayVersion)
                    .put("headline", release.headline)
                    .put("subtitle", release.subtitle)
                    .put("features", featuresArr)
                    .put("showOnUpdateEnabled", updatePrefs.isShowWhatsNewOnUpdateEnabled)
                    .put("lastSeenVersion", updatePrefs.lastSeenWhatsNewVersion)
                    .put("shouldAutoShow", shouldAuto)
                sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                return
            }

            // Java Runtime Manager (Owner only)
            if (path == "/api/java" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                sendApiResult(out, apiHandlers.handleGetJavaStatus())
                return
            }
            if (path == "/api/java/install" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                userRepository.recordAudit(currentUser, "JAVA_INSTALL", "host", body.take(80), effectiveIp)
                sendApiResult(out, apiHandlers.handleInstallJava(body))
                return
            }

            // MineServe Cloud Status & OTP Sign-In (Owner only)
            if (path == "/api/cloud/status" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                sendApiResult(out, extHandlers.handleGetCloudStatus())
                return
            }
            if (path == "/api/cloud/refresh" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                sendApiResult(out, extHandlers.handleRefreshCloudStatus())
                return
            }
            if (path == "/api/cloud/otp/request" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                userRepository.recordAudit(currentUser, "CLOUD_OTP_REQUEST", "host", "Requested Cloud sign-in code", effectiveIp)
                sendApiResult(out, extHandlers.handleRequestCloudOtp(body))
                return
            }
            if (path == "/api/cloud/otp/verify" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                userRepository.recordAudit(currentUser, "CLOUD_SIGN_IN", "host", "Verified Cloud sign-in code", effectiveIp)
                sendApiResult(out, extHandlers.handleVerifyCloudOtp(body))
                return
            }
            if (path == "/api/cloud/logout" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                userRepository.recordAudit(currentUser, "CLOUD_SIGN_OUT", "host", "Signed out of MineServe Cloud", effectiveIp)
                sendApiResult(out, extHandlers.handleLogoutCloud())
                return
            }

            // Battery & Thermal Safeguards (Owner only)
            if (path == "/api/safeguards") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                if (method == "GET") {
                    sendApiResult(out, extHandlers.handleGetSafeguards())
                    return
                } else if (method == "POST") {
                    userRepository.recordAudit(currentUser, "SAFEGUARDS_SAVE", "host", "Updated battery/thermal safeguards", effectiveIp)
                    sendApiResult(out, extHandlers.handleSaveSafeguards(body))
                    return
                }
            }

            // Diagnostics & Logcat (Owner only)
            if (path == "/api/diagnostics/logcat" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                sendApiResult(out, extHandlers.handleCaptureLogcat(queryParams["filter"]))
                return
            }
            if (path == "/api/diagnostics/selftest" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                sendApiResult(out, extHandlers.handleRunSelfTest())
                return
            }
            if (path == "/api/diagnostics/bundle" && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                val (zipFile, errResult) = extHandlers.exportDiagnosticZip(queryParams["filter"])
                if (errResult != null) {
                    sendApiResult(out, errResult)
                } else if (zipFile != null) {
                    try {
                        sendFileStreamResponse(
                            out = out,
                            contentType = "application/zip",
                            file = zipFile,
                            extraHeaders = mapOf("Content-Disposition" to "attachment; filename=\"${zipFile.name}\"")
                        )
                    } finally {
                        zipFile.delete()
                    }
                }
                return
            }

            // Software Types & Versions
            if (path == "/api/software/types" && method == "GET") {
                sendApiResult(out, apiHandlers.handleSoftwareTypes())
                return
            }
            if (path == "/api/software/versions" && method == "GET") {
                sendApiResult(out, apiHandlers.handleSoftwareVersions(queryParams["type"]))
                return
            }

            // List or Create Servers
            if (path == "/api/servers" && method == "GET") {
                val servers = serverRepository.servers.value.filter { userRepository.canAccessServer(currentUser, it.id) }
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
                        .put("network", buildNetworkSummary(s))
                    arr.put(obj)
                }
                sendResponse(out, 200, "OK", "application/json", arr.toString().toByteArray(Charsets.UTF_8))
                return
            }
            if (path == "/api/servers" && method == "POST") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                userRepository.recordAudit(currentUser, "SERVER_CREATE", "servers", "Requested new server creation", effectiveIp)
                sendApiResult(out, apiHandlers.handleCreateServer(body))
                return
            }
            if (path.startsWith("/api/servers/jobs/") && method == "GET") {
                if (!requirePerm(out, currentUser, Permission.MANAGE_HOST)) return
                val jobId = path.removePrefix("/api/servers/jobs/").trim('/')
                sendApiResult(out, apiHandlers.handleGetCreationJob(jobId))
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
                    if (!requirePerm(out, currentUser, Permission.SERVER_VIEW, serverId)) {
                        return
                    }

                    // 1. GET or DELETE /api/servers/{id}
                    if (segments.size == 1 && method == "DELETE") {
                        if (!requirePerm(out, currentUser, Permission.MANAGE_HOST, serverId)) return
                        userRepository.recordAudit(currentUser, "SERVER_DELETE", server.name, "Deleted server ${server.id}", effectiveIp)
                        sendApiResult(out, apiHandlers.handleDeleteServer(serverId))
                        return
                    }
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
                        resp.put("network", buildNetworkSummary(server))
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    val action = segments.getOrNull(1)

                    // 2. POST /api/servers/{id}/start
                    if (action == "start" && method == "POST") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_CONTROL, serverId)) return
                        val serverDir = serverRepository.getServerDirectory(server.id)
                        processManager.startServer(server, serverDir)
                        userRepository.recordAudit(currentUser, "SERVER_START", server.name, "Started server", effectiveIp)
                        val resp = JSONObject().put("success", true).put("message", "Server start requested")
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    // 3. POST /api/servers/{id}/stop
                    if (action == "stop" && method == "POST") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_CONTROL, serverId)) return
                        processManager.stopServer(server.id)
                        userRepository.recordAudit(currentUser, "SERVER_STOP", server.name, "Stopped server", effectiveIp)
                        val resp = JSONObject().put("success", true).put("message", "Server stop requested")
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    // 4. POST /api/servers/{id}/restart
                    if (action == "restart" && method == "POST") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_CONTROL, serverId)) return
                        val serverDir = serverRepository.getServerDirectory(server.id)
                        processManager.restartServer(server, serverDir)
                        userRepository.recordAudit(currentUser, "SERVER_RESTART", server.name, "Restarted server", effectiveIp)
                        val resp = JSONObject().put("success", true).put("message", "Server restart requested")
                        sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                        return
                    }

                    // 4b. POST /api/servers/{id}/delete
                    if (action == "delete" && method == "POST") {
                        if (!requirePerm(out, currentUser, Permission.MANAGE_HOST, serverId)) return
                        userRepository.recordAudit(currentUser, "SERVER_DELETE", server.name, "Deleted server ${server.id}", effectiveIp)
                        sendApiResult(out, apiHandlers.handleDeleteServer(serverId))
                        return
                    }

                    // 4b2. GET /api/servers/{id}/build-status & POST /api/servers/{id}/upgrade
                    if (action == "build-status" && method == "GET") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                        sendApiResult(out, apiHandlers.handleCheckBuildStatus(serverId))
                        return
                    }
                    if (action == "upgrade" && method == "POST") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                        userRepository.recordAudit(currentUser, "SERVER_UPGRADE", server.name, body.take(120), effectiveIp)
                        sendApiResult(out, apiHandlers.handleUpgradeServer(serverId, body))
                        return
                    }

                    // 4c. GET & POST /api/servers/{id}/settings
                    if (action == "settings") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_SETTINGS_WRITE, serverId)) return
                        if (method == "GET") {
                            sendApiResult(out, apiHandlers.handleGetSettings(serverId))
                            return
                        } else if (method == "POST") {
                            userRepository.recordAudit(currentUser, "SETTINGS_SAVE", server.name, "Updated server settings", effectiveIp)
                            sendApiResult(out, apiHandlers.handleSaveSettings(serverId, body))
                            return
                        }
                    }

                    // 4d. Players Endpoints: /api/servers/{id}/players[/whitelist|ops|ban|ban-ip|kick]
                    if (action == "players") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_PLAYERS, serverId)) return
                        val playerAction = segments.getOrNull(2)
                        if (playerAction == null && method == "GET") {
                            sendApiResult(out, apiHandlers.handleGetPlayers(serverId))
                            return
                        } else if (playerAction != null && method == "POST") {
                            userRepository.recordAudit(currentUser, "PLAYER_ACTION", server.name, "Action: $playerAction", effectiveIp)
                            sendApiResult(out, apiHandlers.handlePlayerAction(serverId, playerAction, body))
                            return
                        }
                    }

                    // 4e. Performance, Automation, World, Live Map, Network, Backups, Plugins, Modpack
                    if (action in setOf("performance", "automation", "world", "livemap", "network", "backups", "plugins", "modpack", "discord", "proxy")) {
                        val sub = segments.getOrNull(2)
                        val requiredPerm = when (action) {
                            "performance", "livemap" -> {
                                if (method == "GET") Permission.SERVER_VIEW else Permission.SERVER_SETTINGS_WRITE
                            }
                            "backups" -> {
                                when {
                                    sub == "cloud" -> Permission.SERVER_BACKUPS_MANAGE
                                    method == "GET" -> Permission.SERVER_BACKUPS_CREATE
                                    sub == "create" -> Permission.SERVER_BACKUPS_CREATE
                                    else -> Permission.SERVER_BACKUPS_MANAGE
                                }
                            }
                            else -> Permission.SERVER_SETTINGS_WRITE
                        }
                        if (!requirePerm(out, currentUser, requiredPerm, serverId)) return
                    }
                    if (dispatchExtendedRoutes(out, currentUser, serverId, action, segments, method, queryParams, body)) {
                        return
                    }

                    // 5. GET /api/servers/{id}/stream (Real-Time SSE Console & Telemetry)
                    if (action == "stream" && method == "GET") {
                        handleServerSseStream(socket, out, server, token)
                        return
                    }

                    // 5b. GET & POST /api/servers/{id}/console
                    if (action == "console") {
                        if (method == "GET") {
                            val limit = queryParams["limit"]?.toIntOrNull()?.coerceIn(100, 2000) ?: 2000
                            val lines = processManager.getRecentConsoleLines(serverId, limit)
                            val resp = JSONObject().put("lines", JSONArray(lines))
                            sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        } else if (method == "POST") {
                            if (!requirePerm(out, currentUser, Permission.SERVER_CONSOLE_WRITE, serverId)) return
                            val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrElse {
                                sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", "Invalid JSON body.").toString().toByteArray(Charsets.UTF_8))
                                return
                            }
                            val cmd = jsonBody.optString("command", "").trim()
                            if (cmd.isNotBlank()) {
                                processManager.sendCommand(serverId, cmd)
                                userRepository.recordAudit(currentUser, "CONSOLE_CMD", server.name, cmd, effectiveIp)
                            }
                            val resp = JSONObject().put("success", true)
                            sendResponse(out, 200, "OK", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }
                    }

                    // 6. Files Endpoints (Admin+ only)
                    if (action == "files") {
                        if (!requirePerm(out, currentUser, Permission.SERVER_FILES_WRITE, serverId)) return
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
                            if (!requirePerm(out, currentUser, Permission.SERVER_FILES_WRITE, serverId)) return
                            val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrElse {
                                sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", "Invalid JSON body.").toString().toByteArray(Charsets.UTF_8))
                                return
                            }
                            val targetPath = jsonBody.optString("path", "")
                            val content = jsonBody.optString("content", "")
                            val ok = serverRepository.writeFile(serverId, targetPath, content)
                            if (ok) userRepository.recordAudit(currentUser, "FILE_EDIT", server.name, targetPath, effectiveIp)
                            val resp = JSONObject().put("success", ok)
                            sendResponse(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // POST /api/servers/{id}/files/delete
                        if (fileAction == "delete" && method == "POST") {
                            if (!requirePerm(out, currentUser, Permission.SERVER_FILES_WRITE, serverId)) return
                            val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrElse {
                                sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", "Invalid JSON body.").toString().toByteArray(Charsets.UTF_8))
                                return
                            }
                            val targetPath = jsonBody.optString("path", "")
                            val ok = serverRepository.deleteFileOrDirectory(serverId, targetPath)
                            if (ok) userRepository.recordAudit(currentUser, "FILE_DELETE", server.name, targetPath, effectiveIp)
                            val resp = JSONObject().put("success", ok)
                            sendResponse(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", "application/json", resp.toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // POST /api/servers/{id}/files/create
                        if (fileAction == "create" && method == "POST") {
                            val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())
                            val dirPath = jsonBody.optString("path", "")
                            val fileName = File(jsonBody.optString("name", "")).name.trim()
                            val content = jsonBody.optString("content", "")
                            if (fileName.isBlank() || fileName == "." || fileName == "..") {
                                sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", "Invalid file name.").toString().toByteArray(Charsets.UTF_8))
                                return
                            }
                            val ok = serverRepository.createFile(serverId, dirPath, fileName, content)
                            if (ok) userRepository.recordAudit(currentUser, "FILE_CREATE", server.name, if (dirPath.isBlank()) fileName else "$dirPath/$fileName", effectiveIp)
                            sendResponse(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", "application/json", JSONObject().put("success", ok).put("error", if (ok) "" else "File already exists or could not be created.").toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // POST /api/servers/{id}/files/mkdir
                        if (fileAction == "mkdir" && method == "POST") {
                            val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())
                            val dirPath = jsonBody.optString("path", "")
                            val dirName = File(jsonBody.optString("name", "")).name.trim()
                            if (dirName.isBlank() || dirName == "." || dirName == "..") {
                                sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", "Invalid folder name.").toString().toByteArray(Charsets.UTF_8))
                                return
                            }
                            val ok = serverRepository.createDirectory(serverId, dirPath, dirName)
                            if (ok) userRepository.recordAudit(currentUser, "DIR_CREATE", server.name, if (dirPath.isBlank()) dirName else "$dirPath/$dirName", effectiveIp)
                            sendResponse(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", "application/json", JSONObject().put("success", ok).toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // POST /api/servers/{id}/files/rename
                        if (fileAction == "rename" && method == "POST") {
                            val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())
                            val targetPath = jsonBody.optString("path", "")
                            val newName = File(jsonBody.optString("newName", "")).name.trim()
                            if (targetPath.isBlank() || newName.isBlank() || newName == "." || newName == "..") {
                                sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", "Invalid rename parameters.").toString().toByteArray(Charsets.UTF_8))
                                return
                            }
                            val ok = serverRepository.renameFileOrDirectory(serverId, targetPath, newName)
                            if (ok) userRepository.recordAudit(currentUser, "FILE_RENAME", server.name, "$targetPath -> $newName", effectiveIp)
                            sendResponse(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", "application/json", JSONObject().put("success", ok).toString().toByteArray(Charsets.UTF_8))
                            return
                        }

                        // POST /api/servers/{id}/files/unzip
                        if (fileAction == "unzip" && method == "POST") {
                            val jsonBody = runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrDefault(JSONObject())
                            val targetPath = jsonBody.optString("path", "")
                            val deleteAfter = jsonBody.optBoolean("deleteZipAfter", false)
                            val res = serverRepository.unzipFile(serverId, targetPath, "", deleteAfter)
                            res.fold(
                                onSuccess = { count ->
                                    userRepository.recordAudit(currentUser, "FILE_UNZIP", server.name, "$targetPath ($count files)", effectiveIp)
                                    sendResponse(out, 200, "OK", "application/json", JSONObject().put("success", true).put("extractedCount", count).toString().toByteArray(Charsets.UTF_8))
                                },
                                onFailure = { err ->
                                    sendResponse(out, 400, "Bad Request", "application/json", JSONObject().put("error", err.message ?: "Failed to unzip archive.").toString().toByteArray(Charsets.UTF_8))
                                }
                            )
                            return
                        }

                        // GET /api/servers/{id}/files/download?path=...
                        if (fileAction == "download" && method == "GET") {
                            val targetPath = queryParams["path"] ?: ""
                            val serverDir = serverRepository.getServerDirectory(serverId)
                            val file = File(serverDir, targetPath)
                            if (!file.exists() || !file.isFile || !file.canonicalPath.startsWith(serverDir.canonicalPath + File.separator)) {
                                sendResponse(out, 404, "Not Found", "application/json", JSONObject().put("error", "File not found.").toString().toByteArray(Charsets.UTF_8))
                                return
                            }
                            val safeHeaderName = file.name.replace("\"", "_")
                            sendFileStreamResponse(
                                out = out,
                                contentType = "application/octet-stream",
                                file = file,
                                extraHeaders = mapOf("Content-Disposition" to "attachment; filename=\"$safeHeaderName\"")
                            )
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

    private suspend fun handleServerSseStream(
        socket: Socket,
        out: OutputStream,
        server: MinecraftServer,
        token: String?
    ) {
        try {
            socket.soTimeout = 0
            val header = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/event-stream; charset=utf-8\r\n")
                append("Cache-Control: no-cache\r\n")
                append("Connection: keep-alive\r\n")
                append("X-Accel-Buffering: no\r\n")
                append("\r\n")
            }
            out.write(header.toByteArray(Charsets.UTF_8))
            out.flush()

            var lastRefreshTs = -1L
            var lastStatusSentAt = 0L
            var lastStatusStr = ""
            var lastKeepaliveAt = System.currentTimeMillis()

            while (isRunning && !socket.isClosed) {
                val activeSession = security.validateSession(token) ?: break
                val sseUser = userRepository.getUser(activeSession.userId)?.takeIf { it.enabled } ?: break
                if (!userRepository.canAccessServer(sseUser, server.id) ||
                    !userRepository.permissionsFor(sseUser.role).contains(Permission.SERVER_VIEW)
                ) {
                    break
                }
                if (serverRepository.getServer(server.id) == null) break

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
                    val latestServer = serverRepository.getServer(server.id) ?: server
                    val metrics = processManager.serverMetrics.value[server.id]
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
                    val statusPayload = JSONObject()
                        .put("id", latestServer.id)
                        .put("status", currentStatus)
                        .put("allocatedRamMb", latestServer.allocatedRamMb)
                        .put("metrics", metricsObj)
                        .put("network", buildNetworkSummary(latestServer))
                        .toString()
                    out.write("event: status\ndata: $statusPayload\n\n".toByteArray(Charsets.UTF_8))
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

    private fun buildNetworkSummary(server: MinecraftServer): JSONObject {
        val tState = tunnelManager.getTunnelState(server.id)
        val pfState = portForwardManager.getState(server.id)
        val localIp = portForwardManager.getLocalWifiIpAddress().orEmpty()
        val providerLabel = when (server.tunnelConfig.provider) {
            TunnelProvider.MINESERVE_CLOUD -> "MineServe Cloud"
            TunnelProvider.BORE -> "Bore"
            TunnelProvider.CUSTOM_BORE -> "Custom Bore"
            TunnelProvider.PLAYIT -> "Playit.gg"
        }

        val tunnelStatus = when (tState) {
            is TunnelState.Connected -> "connected"
            is TunnelState.Connecting -> "connecting"
            is TunnelState.Error -> "error"
            is TunnelState.Disconnected -> "disconnected"
        }
        val tunnelAddress = (tState as? TunnelState.Connected)?.fullAddress.orEmpty()
        val tunnelMessage = when (tState) {
            is TunnelState.Connecting -> tState.message
            is TunnelState.Error -> tState.errorMessage
            else -> ""
        }

        val portForwardStatus = when (pfState) {
            is PortForwardState.Mapped -> "mapped"
            is PortForwardState.Discovering -> "discovering"
            is PortForwardState.Failed -> "failed"
            is PortForwardState.Idle -> "idle"
        }
        val directAddress = (pfState as? PortForwardState.Mapped)?.directAddress.orEmpty()
        val portForwardMessage = when (pfState) {
            is PortForwardState.Discovering -> pfState.message
            is PortForwardState.Failed -> pfState.reason
            else -> ""
        }

        val publicAddress = when {
            tunnelAddress.isNotBlank() -> tunnelAddress
            directAddress.isNotBlank() -> directAddress
            else -> ""
        }
        val activeMethod = when {
            tunnelAddress.isNotBlank() -> providerLabel
            directAddress.isNotBlank() -> "UPnP / NAT-PMP"
            tunnelStatus == "connecting" -> providerLabel
            portForwardStatus == "discovering" -> "UPnP / NAT-PMP"
            else -> ""
        }

        return JSONObject()
            .put("localAddress", if (localIp.isNotBlank()) "$localIp:${server.port}" else "")
            .put("publicAddress", publicAddress)
            .put("activeMethod", activeMethod)
            .put("tunnelProvider", server.tunnelConfig.provider.name)
            .put("tunnelProviderLabel", providerLabel)
            .put("tunnelStatus", tunnelStatus)
            .put("tunnelAddress", tunnelAddress)
            .put("tunnelMessage", tunnelMessage)
            .put("portForwardStatus", portForwardStatus)
            .put("directAddress", directAddress)
            .put("portForwardMessage", portForwardMessage)
    }

    private fun requirePerm(
        out: OutputStream,
        user: PanelUser,
        permission: Permission,
        serverId: String? = null
    ): Boolean {
        if (userRepository.hasPermission(user, permission, serverId)) return true
        val err = JSONObject().put("error", "Insufficient permissions (${user.role.displayName} cannot perform this action).")
        sendResponse(out, 403, "Forbidden", "application/json", err.toString().toByteArray(Charsets.UTF_8))
        return false
    }

    private fun userToJson(user: PanelUser): JSONObject {
        return JSONObject()
            .put("id", user.id)
            .put("username", user.username)
            .put("role", user.role.name)
            .put("roleLabel", user.role.displayName)
            .put("restrictServers", user.restrictServers)
            .put("allowedServerIds", JSONArray(user.allowedServerIds))
            .put("enabled", user.enabled)
            .put("createdAt", user.createdAt)
            .put("lastLoginAt", user.lastLoginAt ?: 0L)
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
        val userLockKey = inputUser.lowercase().takeIf { it.isNotBlank() }?.let { "user:$it" }

        if (userLockKey != null && security.isIpLockedOut(userLockKey)) {
            val errObj = JSONObject().put("error", "Too many failed attempts for this account. Temporary lockout for 5 minutes.")
            sendResponse(out, 429, "Too Many Requests", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
            return
        }

        val cfg = configProvider()
        userRepository.ensureDefaultOwner(cfg.username, cfg.password)
        val authedUser = userRepository.authenticate(inputUser, inputPass)

        if (authedUser != null) {
            security.resetFailedAttempts(clientIp)
            if (userLockKey != null) security.resetFailedAttempts(userLockKey)
            val session = security.createSession(authedUser.id, authedUser.username, authedUser.role, clientIp)
            userRepository.recordAudit(authedUser, "LOGIN", authedUser.username, "Signed in (${authedUser.role.displayName})", clientIp)
            val cookie = "session=${session.token}; Path=/; HttpOnly; SameSite=Lax"
            val resp = JSONObject()
                .put("success", true)
                .put("token", session.token)
                .put("user", userToJson(authedUser))
                .put("permissions", JSONArray(userRepository.permissionsFor(authedUser.role).map { it.name }))
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
            if (userLockKey != null) security.recordFailedAttempt(userLockKey)
            val errObj = JSONObject().put("error", "Invalid username or password.")
            sendResponse(out, 401, "Unauthorized", "application/json", errObj.toString().toByteArray(Charsets.UTF_8))
        }
    }

    private fun extractToken(authHeader: String?, cookieHeader: String?): String? =
        WebHttpUtils.extractToken(authHeader, cookieHeader)

    private fun parseQueryParams(query: String): Map<String, String> =
        WebHttpUtils.parseQueryParams(query)

    private fun sendResponse(
        out: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) = WebHttpUtils.sendResponse(out, statusCode, statusText, contentType, body, extraHeaders)

    private fun serveStaticAsset(out: OutputStream, path: String): Boolean {
        val assetName = when (path) {
            "/", "/index.html" -> "index.html"
            "/mineserve-m3-tokens.css" -> "mineserve-m3-tokens.css"
            "/styles.css" -> "styles.css"
            "/app.js" -> "app.js"
            "/logo.png" -> "logo.png"
            "/fonts/plus-jakarta-sans-400.woff2" -> "fonts/plus-jakarta-sans-400.woff2"
            "/fonts/plus-jakarta-sans-500.woff2" -> "fonts/plus-jakarta-sans-500.woff2"
            "/fonts/plus-jakarta-sans-600.woff2" -> "fonts/plus-jakarta-sans-600.woff2"
            "/fonts/plus-jakarta-sans-700.woff2" -> "fonts/plus-jakarta-sans-700.woff2"
            "/fonts/jetbrains-mono-400.woff2" -> "fonts/jetbrains-mono-400.woff2"
            "/fonts/jetbrains-mono-700.woff2" -> "fonts/jetbrains-mono-700.woff2"
            else -> return false
        }
        val bytes = assetProvider(assetName) ?: return false
        val contentType = when {
            assetName.endsWith(".html") -> "text/html; charset=utf-8"
            assetName.endsWith(".css") -> "text/css; charset=utf-8"
            assetName.endsWith(".js") -> "application/javascript; charset=utf-8"
            assetName.endsWith(".woff2") -> "font/woff2"
            assetName.endsWith(".svg") -> "image/svg+xml"
            assetName.endsWith(".png") -> "image/png"
            else -> "application/octet-stream"
        }
        val extraHeaders = if (assetName.endsWith(".woff2")) {
            mapOf("Cache-Control" to "public, max-age=31536000, immutable")
        } else {
            emptyMap()
        }
        sendResponse(out, 200, "OK", contentType, bytes, extraHeaders)
        return true
    }

    private suspend fun dispatchExtendedRoutes(
        out: OutputStream,
        currentUser: PanelUser,
        serverId: String,
        action: String?,
        segments: List<String>,
        method: String,
        queryParams: Map<String, String>,
        body: String
    ): Boolean {
        val sub = segments.getOrNull(2)
        val result: WebApiResult? = when (action) {
            "performance" -> when {
                sub == null && method == "GET" -> extHandlers.handleGetPerformance(serverId)
                sub == "aikar" && method == "POST" -> extHandlers.handleApplyAikarFlags(serverId)
                else -> null
            }
            "automation" -> when {
                sub == null && method == "GET" -> extHandlers.handleGetAutomation(serverId)
                sub == null && method == "POST" -> extHandlers.handleSaveAutomation(serverId, body)
                sub == "tasks" && method == "POST" -> {
                    val taskAction = segments.getOrNull(3) ?: "add"
                    extHandlers.handleAutomationTask(serverId, taskAction, body)
                }
                else -> null
            }
            "world" -> when {
                sub == null && method == "GET" -> extHandlers.handleGetWorld(serverId)
                sub == "reset" && method == "POST" -> extHandlers.handleResetDimension(serverId, body)
                sub == "optimize" && method == "POST" -> extHandlers.handleOptimizeWorld(serverId, body)
                sub == "regions" -> {
                    val regAction = segments.getOrNull(3)
                    when {
                        regAction == null && method == "GET" -> extHandlers.handleListRegions(serverId)
                        regAction == "delete" && method == "POST" -> extHandlers.handleDeleteRegion(serverId, body)
                        else -> null
                    }
                }
                sub == "pregen" -> when {
                    method == "GET" -> extHandlers.handleGetPregenStatus(serverId)
                    method == "POST" -> extHandlers.handlePregenAction(serverId, body)
                    else -> null
                }
                sub == "export" && method == "GET" -> {
                    val (tempFile, errResult) = extHandlers.exportWorldToTempZip(serverId)
                    if (errResult != null) {
                        errResult
                    } else if (tempFile != null) {
                        try {
                            sendFileStreamResponse(
                                out = out,
                                contentType = "application/zip",
                                file = tempFile,
                                extraHeaders = mapOf("Content-Disposition" to "attachment; filename=\"${serverId}-world.zip\"")
                            )
                        } finally {
                            tempFile.delete()
                        }
                        return true
                    } else {
                        WebApiResult(500, "Internal Server Error", JSONObject().put("error", "World export failed"))
                    }
                }
                else -> null
            }
            "livemap" -> when {
                sub == null && method == "GET" -> extHandlers.handleGetLiveMap(serverId)
                sub == "install" && method == "POST" -> extHandlers.handleInstallLiveMap(serverId)
                sub == "uninstall" && method == "POST" -> extHandlers.handleUninstallLiveMap(serverId)
                sub == "port" && method == "POST" -> extHandlers.handleSetLiveMapPort(serverId, body)
                else -> null
            }
            "network" -> when {
                sub == null && method == "GET" -> extHandlers.handleGetNetwork(serverId)
                sub == "tunnel" && method == "POST" -> extHandlers.handleTunnelAction(serverId, body)
                sub == "portforward" && method == "POST" -> extHandlers.handlePortForwardAction(serverId, body)
                else -> null
            }
            "discord" -> when {
                sub == null && method == "GET" -> extHandlers.handleGetDiscord(serverId)
                sub == null && method == "POST" -> extHandlers.handleSaveDiscord(serverId, body)
                sub == "test" && method == "POST" -> extHandlers.handleTestDiscord(serverId, body)
                else -> null
            }
            "proxy" -> when {
                sub == null && method == "GET" -> extHandlers.handleGetProxy(
                    serverId = serverId,
                    accessibleServerIds = if (currentUser.restrictServers) currentUser.allowedServerIds.toSet() else null
                )
                sub == null && method == "POST" -> extHandlers.handleSaveProxy(
                    serverId = serverId,
                    body = body,
                    accessibleServerIds = if (currentUser.restrictServers) currentUser.allowedServerIds.toSet() else null
                )
                else -> null
            }
            "backups" -> when {
                sub == null && method == "GET" -> extHandlers.handleListBackups(serverId)
                sub == "create" && method == "POST" -> extHandlers.handleCreateBackup(serverId, body)
                sub == "restore" && method == "POST" -> extHandlers.handleRestoreBackup(serverId, body)
                sub == "delete" && method == "POST" -> extHandlers.handleDeleteBackup(serverId, body)
                sub == "cloud" -> {
                    val cloudAction = segments.getOrNull(3)
                    when {
                        cloudAction == null && method == "GET" -> extHandlers.handleListCloudBackups(serverId)
                        cloudAction == "upload" && method == "POST" -> extHandlers.handleUploadCloudBackup(serverId, body)
                        cloudAction == "restore" && method == "POST" -> extHandlers.handleRestoreCloudBackup(serverId, body)
                        cloudAction == "delete" && method == "POST" -> extHandlers.handleDeleteCloudBackup(serverId, body)
                        else -> null
                    }
                }
                sub == "download" && method == "GET" -> {
                    val file = extHandlers.getBackupFile(serverId, queryParams["file"] ?: "")
                    if (file != null) {
                        val safeName = file.name.replace("\"", "_")
                        sendFileStreamResponse(
                            out, "application/zip", file,
                            extraHeaders = mapOf("Content-Disposition" to "attachment; filename=\"$safeName\"")
                        )
                        return true
                    }
                    WebApiResult(404, "Not Found", JSONObject().put("error", "Backup not found"))
                }
                else -> null
            }
            "plugins" -> when {
                sub == null && method == "GET" -> extHandlers.handleListPlugins(serverId)
                sub == "search" && method == "GET" -> extHandlers.handleSearchModrinth(serverId, queryParams["q"] ?: "")
                sub == "install" && method == "POST" -> extHandlers.handleInstallModrinth(serverId, body)
                (sub == "toggle" || sub == "delete") && method == "POST" -> extHandlers.handlePluginAction(serverId, sub, body)
                else -> null
            }
            "modpack" -> when {
                sub == "jobs" && method == "GET" -> {
                    val jobId = segments.getOrNull(3) ?: ""
                    extHandlers.handleGetModpackJobStatus(serverId, jobId)
                }
                else -> null
            }
            else -> null
        }
        if (result != null) {
            sendApiResult(out, result)
            return true
        }
        return false
    }

    private fun sendApiResult(out: OutputStream, result: WebApiResult) {
        sendResponse(
            out = out,
            statusCode = result.statusCode,
            statusText = result.statusText,
            contentType = "application/json",
            body = result.body.toString().toByteArray(Charsets.UTF_8)
        )
    }

    private fun readHeaderLine(input: InputStream): String? =
        WebHttpUtils.readAsciiLine(input)

    private fun readFixedBytes(input: InputStream, length: Int): ByteArray =
        WebHttpUtils.readFixedBytes(input, length)

    private fun streamFixedBytesToFile(input: InputStream, length: Long, dest: File) =
        WebHttpUtils.streamFixedBytesToFile(input, length, dest)

    private fun sendFileStreamResponse(
        out: OutputStream,
        contentType: String,
        file: File,
        extraHeaders: Map<String, String> = emptyMap()
    ) = WebHttpUtils.sendFileStreamResponse(out, contentType, file, extraHeaders)
}
