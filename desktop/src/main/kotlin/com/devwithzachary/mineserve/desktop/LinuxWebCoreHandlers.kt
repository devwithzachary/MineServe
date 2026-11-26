package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.Permission
import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.MineServeAboutData
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.net.http.WebHttpUtils
import com.devwithzachary.mineserve.util.FileStorageUtils
import com.devwithzachary.mineserve.util.ServerModFilter
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class AsyncJobState(
    val status: String,
    val progress: Int,
    val message: String,
    val serverId: String? = null
)

private data class LinuxModpackJob(
    val id: String,
    val serverId: String,
    @Volatile var status: String = "RUNNING",
    @Volatile var message: String = "Reading Modpack Metadata...",
    @Volatile var currentFile: String = "",
    @Volatile var progressPercent: Int = 0,
    @Volatile var error: String? = null
)

internal class LinuxWebCoreHandlers(
    private val paths: LinuxDataPaths,
    private val serverRepository: LinuxServerRepository,
    private val processManager: LinuxProcessManager,
    private val softwareManager: LinuxServerSoftwareManager,
    private val javaManager: PortableJavaManager,
    private val cloudManager: LinuxCloudManager,
    private val userRepository: UserRepository,
    private val scopeProvider: () -> CoroutineScope?
) {
    companion object {
        private const val TAG = "LinuxWebCoreHandlers"
    }

    private val jobs = ConcurrentHashMap<String, AsyncJobState>()
    private val modpackJobs = ConcurrentHashMap<String, LinuxModpackJob>()

    @Volatile private var installingJavaVersion: Int? = null
    @Volatile private var javaInstallStatus: String = ""
    @Volatile private var javaInstallError: String? = null

    fun handleGetSystem(out: OutputStream) {
        val mem = LinuxHostTelemetry.getMemoryInfo()
        val rt = Runtime.getRuntime()
        val resp = JSONObject()
            .put("device", LinuxHostTelemetry.getHostDisplayName())
            .put("android", "Linux Native")
            .put("platform", "linux")
            .put("totalRamMb", rt.totalMemory() / (1024 * 1024))
            .put("freeRamMb", rt.freeMemory() / (1024 * 1024))
            .put("deviceTotalRamMb", mem.totalMb)
            .put("maxAllocatableRamMb", mem.maxAllocatableMb)
            .put("defaultRamMb", mem.defaultRamMb)
            .put("minRamMb", 512)
            .put("ramStepMb", 256)
            .put("version", MineServeAboutData.APP_VERSION)
            .put("isPlayStore", false)
            .put("serversDir", paths.serversDir.absolutePath)
            .put("runtimesDir", paths.runtimesDir.absolutePath)
            .put("defaultServersDir", paths.defaultServersDir.absolutePath)
            .put("defaultRuntimesDir", paths.defaultRuntimesDir.absolutePath)
        sendJson(out, 200, "OK", resp)
    }

    fun handleGetHostDirectories(out: OutputStream) {
        sendJson(
            out,
            200,
            "OK",
            JSONObject()
                .put("serversDir", paths.serversDir.absolutePath)
                .put("runtimesDir", paths.runtimesDir.absolutePath)
                .put("defaultServersDir", paths.defaultServersDir.absolutePath)
                .put("defaultRuntimesDir", paths.defaultRuntimesDir.absolutePath)
        )
    }

    fun handleUpdateHostDirectories(out: OutputStream, body: String, currentUser: PanelUser, effectiveIp: String) {
        val anyActive = processManager.serverStatuses.value.values.any {
            it == com.devwithzachary.mineserve.model.ServerStatus.RUNNING ||
                it == com.devwithzachary.mineserve.model.ServerStatus.STARTING
        }
        if (anyActive) {
            sendJson(out, 409, "Conflict", JSONObject().put("error", "Stop all running servers before changing host directories."))
            return
        }
        val j = parseJson(body)
        val serversDirPath = if (j.has("serversDir")) j.optString("serversDir", "") else null
        val runtimesDirPath = if (j.has("runtimesDir")) j.optString("runtimesDir", "") else null
        paths.updateDirectories(serversDirPath, runtimesDirPath).fold(
            onSuccess = { (srvDir, rtDir) ->
                userRepository.recordAudit(
                    currentUser,
                    "HOST_DIRECTORIES_UPDATE",
                    "host",
                    "servers=${srvDir.absolutePath}, runtimes=${rtDir.absolutePath}",
                    effectiveIp
                )
                sendJson(
                    out,
                    200,
                    "OK",
                    JSONObject()
                        .put("success", true)
                        .put("serversDir", srvDir.absolutePath)
                        .put("runtimesDir", rtDir.absolutePath)
                        .put("defaultServersDir", paths.defaultServersDir.absolutePath)
                        .put("defaultRuntimesDir", paths.defaultRuntimesDir.absolutePath)
                )
            },
            onFailure = { err ->
                sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Invalid directory path"))
            }
        )
    }

    fun handleGetJavaStatus(out: OutputStream) {
        val arr = JSONArray()
        PortableJavaManager.SUPPORTED_VERSIONS.forEach { v ->
            arr.put(JSONObject().put("version", v).put("installed", javaManager.isJavaInstalled(v)))
        }
        sendJson(
            out, 200, "OK",
            JSONObject()
                .put("runtimes", arr)
                .put("installingVersion", installingJavaVersion ?: JSONObject.NULL)
                .put("statusMessage", javaInstallStatus)
                .put("error", javaInstallError ?: "")
        )
    }

    fun handleInstallJava(out: OutputStream, body: String, currentUser: PanelUser, effectiveIp: String) {
        val version = parseJson(body).optInt("version", 21)
        if (version !in PortableJavaManager.SUPPORTED_VERSIONS) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Unsupported Java version: $version"))
            return
        }
        if (installingJavaVersion != null) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Java $installingJavaVersion is already installing."))
            return
        }
        if (javaManager.isJavaInstalled(version)) {
            sendJson(out, 200, "OK", JSONObject().put("success", true).put("message", "Java $version is already installed."))
            return
        }
        installingJavaVersion = version
        javaInstallStatus = "Downloading Adoptium Temurin OpenJDK $version..."
        javaInstallError = null
        userRepository.recordAudit(currentUser, "JAVA_INSTALL", "host", "OpenJDK $version", effectiveIp)
        scopeProvider()?.launch {
            try {
                val res = javaManager.installJava(version) { msg -> javaInstallStatus = msg.take(120) }
                if (res.isSuccess) {
                    javaInstallStatus = "OpenJDK $version installed successfully."
                    javaInstallError = null
                } else {
                    javaInstallError = res.exceptionOrNull()?.message ?: "Java installation failed"
                    javaInstallStatus = ""
                }
            } finally {
                installingJavaVersion = null
            }
        }
        sendJson(out, 202, "Accepted", JSONObject().put("success", true).put("installingVersion", version))
    }

    fun handleSoftwareTypes(out: OutputStream) {
        val arr = JSONArray()
        ServerType.entries.filter { it != ServerType.CUSTOM }.forEach { t ->
            arr.put(
                JSONObject()
                    .put("name", t.name)
                    .put("displayName", t.displayName)
                    .put("description", t.description)
                    .put("supportsPlugins", t.supportsPlugins)
                    .put("supportsMods", t.supportsMods)
                    .put("defaultJavaVersion", t.defaultJavaVersion)
            )
        }
        sendJson(out, 200, "OK", JSONObject().put("types", arr))
    }

    suspend fun handleSoftwareVersions(out: OutputStream, typeParam: String?) {
        val type = runCatching { ServerType.valueOf((typeParam ?: "PAPER").uppercase()) }
            .getOrElse {
                sendJson(out, 400, "Bad Request", JSONObject().put("error", "Invalid server type"))
                return
            }
        val versions = softwareManager.fetchAvailableVersions(type)
        sendJson(out, 200, "OK", JSONObject().put("type", type.name).put("versions", JSONArray(versions)))
    }

    fun handleCreateServer(out: OutputStream, body: String, currentUser: PanelUser, effectiveIp: String) {
        val j = parseJson(body)
        val name = j.optString("name", "").trim()
        val type = runCatching { ServerType.valueOf(j.optString("type", "PAPER").uppercase()) }
            .getOrElse {
                sendJson(out, 400, "Bad Request", JSONObject().put("error", "Invalid server type"))
                return
            }
        val version = j.optString("version", "").trim()
        val port = j.optInt("port", 25565)
        val ramMb = j.optInt("ramMb", LinuxHostTelemetry.getMemoryInfo().defaultRamMb)
        val motd = j.optString("motd", "A MineServe Linux Server").ifBlank { "A MineServe Linux Server" }
        val eula = j.optBoolean("eulaAccepted", false)
        if (name.isBlank() || version.isBlank() || !eula) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Name, version, and EULA acceptance are required."))
            return
        }

        val jobId = UUID.randomUUID().toString().take(8)
        jobs[jobId] = AsyncJobState("running", 5, "Resolving ${type.displayName} $version...")
        userRepository.recordAudit(currentUser, "SERVER_CREATE", name, "${type.displayName} $version", effectiveIp)

        scopeProvider()?.launch {
            try {
                val buildInfo = softwareManager.resolveLatestBuild(type, version)
                val jarName = when {
                    type == ServerType.FORGE || type == ServerType.NEOFORGE -> "installer.jar"
                    type == ServerType.BEDROCK_BDS -> "bedrock_server"
                    else -> "server.jar"
                }
                val server = serverRepository.createServer(
                    name = name,
                    type = type,
                    version = version,
                    port = port.coerceIn(1, 65535),
                    ramMb = ramMb.coerceAtLeast(256),
                    motd = motd,
                    jarFileName = jarName,
                    serverBuild = buildInfo?.first,
                    eulaAccepted = true
                )
                softwareManager.provisionServer(server, buildInfo?.second) { msg, pct ->
                    jobs[jobId] = AsyncJobState("running", pct.coerceIn(15, 99), msg, server.id)
                }
                jobs[jobId] = AsyncJobState("done", 100, "Server created successfully!", server.id)
            } catch (e: Exception) {
                jobs[jobId] = AsyncJobState("error", 0, e.message ?: "Server creation failed")
            }
        }
        sendJson(out, 202, "Accepted", JSONObject().put("jobId", jobId))
    }

    fun handleGetJob(out: OutputStream, jobId: String) {
        val job = jobs[jobId]
        if (job == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Job not found"))
        } else {
            val obj = JSONObject()
                .put("status", job.status)
                .put("progress", job.progress)
                .put("message", job.message)
            if (job.serverId != null) obj.put("serverId", job.serverId)
            sendJson(out, 200, "OK", obj)
        }
    }

    suspend fun handleBuildStatus(out: OutputStream, server: MinecraftServer) {
        val resolved = softwareManager.resolveLatestBuild(server.type, server.version)
        val latestBuild = resolved?.first.orEmpty()
        val currentBuild = server.serverBuild.orEmpty()
        sendJson(
            out, 200, "OK",
            JSONObject()
                .put("type", server.type.name)
                .put("currentVersion", server.version)
                .put("currentBuild", currentBuild)
                .put("latestBuild", latestBuild)
                .put("updateAvailable", latestBuild.isNotBlank() && latestBuild != currentBuild)
        )
    }

    fun handleUpgradeServer(out: OutputStream, server: MinecraftServer, body: String, currentUser: PanelUser, effectiveIp: String) {
        val j = parseJson(body)
        val newVer = j.optString("newVersion", "").trim().ifBlank { server.version }
        val createBackup = j.optBoolean("createBackup", true)
        val jobId = UUID.randomUUID().toString().take(8)
        jobs[jobId] = AsyncJobState("running", 2, "Preparing upgrade to $newVer...", server.id)
        userRepository.recordAudit(currentUser, "SERVER_UPGRADE", server.name, "Target: $newVer", effectiveIp)
        scopeProvider()?.launch {
            val ok = softwareManager.upgradeServerVersion(server, newVer, createBackup, processManager) { msg, pct ->
                jobs[jobId] = AsyncJobState("running", pct.coerceIn(1, 99), msg, server.id)
            }
            jobs[jobId] = if (ok) {
                AsyncJobState("done", 100, "Upgraded ${server.name} to $newVer!", server.id)
            } else {
                AsyncJobState("error", 0, "Failed to upgrade to $newVer.", server.id)
            }
        }
        sendJson(out, 202, "Accepted", JSONObject().put("jobId", jobId))
    }

    fun handleImportServerZip(
        out: OutputStream,
        queryParams: Map<String, String>,
        zipFile: File,
        user: PanelUser,
        effectiveIp: String
    ) {
        val name = (queryParams["name"] ?: "").trim()
        val type = runCatching { ServerType.valueOf((queryParams["type"] ?: "PAPER").uppercase()) }.getOrDefault(ServerType.PAPER)
        val version = (queryParams["version"] ?: "Custom").trim().ifBlank { "Custom" }
        val port = queryParams["port"]?.toIntOrNull() ?: 25565
        val ramMb = queryParams["ramMb"]?.toIntOrNull() ?: 2048
        val eula = queryParams["eulaAccepted"]?.toBooleanStrictOrNull() ?: false
        if (name.isBlank() || !eula) {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Server name and EULA acceptance are required."))
            return
        }
        val srv = serverRepository.createServer(
            name = name,
            type = type,
            version = version,
            port = port.coerceIn(1, 65535),
            ramMb = ramMb.coerceAtLeast(256),
            motd = "Imported MineServe Server",
            jarFileName = if (type == ServerType.BEDROCK_BDS) "bedrock_server" else "server.jar",
            eulaAccepted = true
        )
        val dir = serverRepository.getServerDirectory(srv.id)
        val res = FileStorageUtils.extractZipSafe(zipFile, dir, deleteZipAfter = false)
        if (res.isFailure) {
            serverRepository.deleteServer(srv.id)
            sendJson(out, 400, "Bad Request", JSONObject().put("error", res.exceptionOrNull()?.message ?: "Failed to extract zip"))
            return
        }
        val topEntries = dir.listFiles()?.filter { it.name != "__MACOSX" } ?: emptyList()
        if (topEntries.size == 1 && topEntries.first().isDirectory) {
            val inner = topEntries.first()
            inner.listFiles()?.forEach { child ->
                val target = File(dir, child.name)
                if (!child.renameTo(target)) {
                    if (child.isDirectory) child.copyRecursively(target, overwrite = true) else child.copyTo(target, overwrite = true)
                    child.deleteRecursively()
                }
            }
            inner.deleteRecursively()
        }
        val detectedJar = if (type == ServerType.BEDROCK_BDS) {
            "bedrock_server"
        } else {
            val jars = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".jar", ignoreCase = true) } ?: emptyList()
            (jars.firstOrNull { it.name.equals("server.jar", ignoreCase = true) } ?: jars.firstOrNull())?.name ?: "server.jar"
        }
        if (detectedJar != srv.jarFileName) {
            serverRepository.updateServer(srv.copy(jarFileName = detectedJar))
        }
        userRepository.recordAudit(user, "SERVER_IMPORT", srv.name, "Imported zip ($detectedJar)", effectiveIp)
        sendJson(
            out, 200, "OK",
            JSONObject()
                .put("success", true)
                .put("serverId", srv.id)
                .put("name", srv.name)
                .put("jarFileName", detectedJar)
                .put("extractedCount", res.getOrDefault(0))
        )
    }

    fun handleStartModpackImport(
        out: OutputStream,
        serverId: String,
        mrpackFile: File,
        user: PanelUser,
        effectiveIp: String
    ) {
        val srv = serverRepository.getServer(serverId)
        if (srv == null) {
            mrpackFile.delete()
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Server not found"))
            return
        }
        if (processManager.isServerRunning(serverId)) {
            mrpackFile.delete()
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before importing a modpack."))
            return
        }
        if (!srv.type.supportsMods && !srv.type.supportsPlugins) {
            mrpackFile.delete()
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "This server software does not support modpacks."))
            return
        }

        val jobId = UUID.randomUUID().toString()
        val job = LinuxModpackJob(id = jobId, serverId = serverId)
        modpackJobs[jobId] = job
        val serverDir = serverRepository.getServerDirectory(serverId)
        userRepository.recordAudit(user, "MODPACK_IMPORT", srv.name, "Started .mrpack import", effectiveIp)

        scopeProvider()?.launch {
            try {
                var indexJsonStr: String? = null
                ZipInputStream(BufferedInputStream(FileInputStream(mrpackFile))).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/')
                        if (name == "modrinth.index.json") {
                            indexJsonStr = zis.bufferedReader(Charsets.UTF_8).readText()
                        } else if ((name.startsWith("overrides/") || name.startsWith("server-overrides/")) && !entry.isDirectory) {
                            val rel = if (name.startsWith("server-overrides/")) {
                                name.removePrefix("server-overrides/")
                            } else {
                                name.removePrefix("overrides/")
                            }
                            val target = File(serverDir, rel)
                            if (isPathInside(target, serverDir)) {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { zis.copyTo(it) }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val rawIndex = indexJsonStr ?: throw IllegalArgumentException("Invalid .mrpack archive: modrinth.index.json is missing.")
                val indexObj = JSONObject(rawIndex)
                val packName = indexObj.optString("name", "Modpack")
                val versionId = indexObj.optString("versionId", "")
                val filesArr = indexObj.optJSONArray("files") ?: JSONArray()

                data class ModFileSpec(val path: String, val downloads: List<String>)
                val toDownload = mutableListOf<ModFileSpec>()
                for (i in 0 until filesArr.length()) {
                    val fObj = filesArr.optJSONObject(i) ?: continue
                    val env = fObj.optJSONObject("env")
                    val serverEnv = env?.optString("server", "required") ?: "required"
                    if (serverEnv.equals("unsupported", ignoreCase = true)) continue
                    val fPath = fObj.optString("path", "").trim()
                    val dls = parseStringArray(fObj.optJSONArray("downloads"))
                    if (fPath.isNotBlank() && dls.isNotEmpty()) {
                        toDownload.add(ModFileSpec(fPath, dls))
                    }
                }

                val total = toDownload.size.coerceAtLeast(1)
                for ((idx, spec) in toDownload.withIndex()) {
                    val dest = File(serverDir, spec.path)
                    if (!isPathInside(dest, serverDir)) continue
                    dest.parentFile?.mkdirs()
                    val fname = dest.name
                    job.message = "Downloading Mods (${idx + 1}/${toDownload.size})"
                    job.currentFile = fname
                    job.progressPercent = (((idx + 1) * 100) / total).coerceIn(1, 99)

                    var ok = false
                    for (url in spec.downloads) {
                        if (runCatching { softwareManager.downloadFile(url, dest) }.getOrDefault(false)) {
                            ok = true
                            break
                        }
                    }
                    if (!ok) CoreLog.w(TAG, "[Modpack] Failed downloading $fname from all mirrors")
                }

                val disabledClientMods = ServerModFilter.disableClientOnlyMods(serverDir)
                val disabledSuffix = if (disabledClientMods.isNotEmpty()) {
                    ", auto-disabled ${disabledClientMods.size} client-only mod(s)"
                } else ""

                job.status = "DONE"
                job.progressPercent = 100
                job.currentFile = ""
                job.message = "Imported $packName${if (versionId.isNotBlank()) " ($versionId)" else ""} (${toDownload.size} files$disabledSuffix)"
            } catch (e: Exception) {
                CoreLog.e(TAG, "Modpack import failed", e)
                job.status = "ERROR"
                job.error = e.message ?: "Modpack import failed."
            } finally {
                mrpackFile.delete()
            }
        }

        sendJson(out, 200, "OK", JSONObject().put("success", true).put("jobId", jobId))
    }

    fun handleGetModpackJob(out: OutputStream, serverId: String, jobId: String) {
        val job = modpackJobs[jobId]?.takeIf { it.serverId == serverId }
        if (job == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Modpack import job not found"))
        } else {
            val obj = JSONObject()
                .put("jobId", job.id)
                .put("status", job.status)
                .put("message", job.message)
                .put("currentFile", job.currentFile)
                .put("progressPercent", job.progressPercent)
            if (job.error != null) obj.put("error", job.error)
            sendJson(out, 200, "OK", obj)
        }
    }

    fun handleSettings(out: OutputStream, method: String, server: MinecraftServer, body: String, currentUser: PanelUser, effectiveIp: String) {
        if (method == "GET") {
            val props = serverRepository.loadServerProperties(server.id)
            val srvObj = JSONObject()
                .put("id", server.id).put("name", server.name).put("type", server.type.name)
                .put("version", server.version).put("port", server.port)
                .put("allocatedRamMb", server.allocatedRamMb).put("javaVersion", server.javaVersion)
                .put("jvmArgs", server.jvmArgs).put("autoStart", server.autoStart)
                .put("eulaAccepted", server.eulaAccepted).put("serverBuild", server.serverBuild ?: "")
            val propsObj = JSONObject()
                .put("motd", props.motd).put("serverPort", props.serverPort).put("maxPlayers", props.maxPlayers)
                .put("gamemode", props.gamemode).put("difficulty", props.difficulty).put("pvp", props.pvp)
                .put("hardcore", props.hardcore).put("onlineMode", props.onlineMode).put("whiteList", props.whiteList)
                .put("enforceWhitelist", props.enforceWhitelist).put("enableCommandBlock", props.enableCommandBlock)
                .put("spawnMonsters", props.spawnMonsters).put("spawnAnimals", props.spawnAnimals)
                .put("spawnNpcs", props.spawnNpcs).put("allowFlight", props.allowFlight)
                .put("allowNether", props.allowNether).put("viewDistance", props.viewDistance)
                .put("simulationDistance", props.simulationDistance).put("levelName", props.levelName)
                .put("levelSeed", props.levelSeed).put("forceGamemode", props.forceGamemode)
            sendJson(out, 200, "OK", JSONObject().put("server", srvObj).put("properties", propsObj))
        } else if (method == "POST") {
            val j = parseJson(body)
            val sObj = j.optJSONObject("server") ?: JSONObject()
            val pObj = j.optJSONObject("properties") ?: JSONObject()
            val existingProps = serverRepository.loadServerProperties(server.id)
            val newPort = pObj.optInt("serverPort", sObj.optInt("port", existingProps.serverPort)).coerceIn(1, 65535)
            val newMotd = pObj.optString("motd", existingProps.motd)
            val eula = sObj.optBoolean("eulaAccepted", server.eulaAccepted)
            serverRepository.updateServer(
                server.copy(
                    name = sObj.optString("name", server.name).trim().ifBlank { server.name },
                    allocatedRamMb = sObj.optInt("allocatedRamMb", server.allocatedRamMb).coerceAtLeast(256),
                    javaVersion = sObj.optInt("javaVersion", server.javaVersion),
                    port = newPort,
                    motd = newMotd,
                    jvmArgs = sObj.optString("jvmArgs", server.jvmArgs),
                    autoStart = sObj.optBoolean("autoStart", server.autoStart),
                    eulaAccepted = eula
                )
            )
            File(serverRepository.getServerDirectory(server.id), "eula.txt").writeText("eula=$eula\n")
            serverRepository.saveServerProperties(
                server.id,
                existingProps.copy(
                    motd = newMotd,
                    serverPort = newPort,
                    maxPlayers = pObj.optInt("maxPlayers", existingProps.maxPlayers).coerceIn(1, 1000),
                    gamemode = pObj.optString("gamemode", existingProps.gamemode),
                    difficulty = pObj.optString("difficulty", existingProps.difficulty),
                    pvp = pObj.optBoolean("pvp", existingProps.pvp),
                    hardcore = pObj.optBoolean("hardcore", existingProps.hardcore),
                    onlineMode = pObj.optBoolean("onlineMode", existingProps.onlineMode),
                    whiteList = pObj.optBoolean("whiteList", existingProps.whiteList),
                    enforceWhitelist = pObj.optBoolean("enforceWhitelist", existingProps.enforceWhitelist),
                    enableCommandBlock = pObj.optBoolean("enableCommandBlock", existingProps.enableCommandBlock),
                    spawnMonsters = pObj.optBoolean("spawnMonsters", existingProps.spawnMonsters),
                    spawnAnimals = pObj.optBoolean("spawnAnimals", existingProps.spawnAnimals),
                    spawnNpcs = pObj.optBoolean("spawnNpcs", existingProps.spawnNpcs),
                    allowFlight = pObj.optBoolean("allowFlight", existingProps.allowFlight),
                    allowNether = pObj.optBoolean("allowNether", existingProps.allowNether),
                    viewDistance = pObj.optInt("viewDistance", existingProps.viewDistance).coerceIn(2, 32),
                    simulationDistance = pObj.optInt("simulationDistance", existingProps.simulationDistance).coerceIn(2, 32),
                    levelName = pObj.optString("levelName", existingProps.levelName).trim().ifBlank { existingProps.levelName },
                    levelSeed = pObj.optString("levelSeed", existingProps.levelSeed),
                    forceGamemode = pObj.optBoolean("forceGamemode", existingProps.forceGamemode)
                )
            )
            userRepository.recordAudit(currentUser, "SETTINGS_SAVE", server.name, "Updated server settings", effectiveIp)
            sendJson(out, 200, "OK", JSONObject().put("success", true))
        } else {
            sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
        }
    }

    private fun isPathInside(target: File, root: File): Boolean {
        val rootPath = root.canonicalPath
        val targetPath = target.canonicalPath
        return targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
    }

    private fun readJsonArrayFile(dir: File, fileName: String): JSONArray {
        val f = File(dir, fileName)
        if (!f.exists() || !f.isFile) return JSONArray()
        return runCatching {
            val raw = f.readText().trim()
            if (raw.isBlank()) JSONArray() else JSONArray(raw)
        }.getOrDefault(JSONArray())
    }

    fun handlePlayers(out: OutputStream, method: String, server: MinecraftServer, sub: String?, body: String, currentUser: PanelUser, effectiveIp: String) {
        if (sub == null && method == "GET") {
            val online = processManager.serverMetrics.value[server.id]?.onlinePlayers ?: emptyList()
            val dir = serverRepository.getServerDirectory(server.id)
            sendJson(
                out, 200, "OK",
                JSONObject()
                    .put("onlinePlayers", JSONArray(online))
                    .put("whitelist", readJsonArrayFile(dir, "whitelist.json"))
                    .put("ops", readJsonArrayFile(dir, "ops.json"))
                    .put("bannedPlayers", readJsonArrayFile(dir, "banned-players.json"))
                    .put("bannedIps", readJsonArrayFile(dir, "banned-ips.json"))
            )
        } else if (sub != null && method == "POST") {
            val j = parseJson(body)
            val act = j.optString("action", "add").lowercase()
            val name = j.optString("name", "").trim()
            val ip = j.optString("ip", "").trim()
            val reason = j.optString("reason", "Action via Web Admin")
            val cmd = when (sub) {
                "whitelist" -> if (act == "remove") "whitelist remove $name" else "whitelist add $name"
                "ops" -> if (act == "remove") "deop $name" else "op $name"
                "ban" -> if (act == "remove") "pardon $name" else "ban $name $reason"
                "ban-ip" -> if (act == "remove") "pardon-ip $ip" else "ban-ip $ip $reason"
                "kick" -> "kick $name $reason"
                else -> ""
            }
            if (cmd.isNotBlank() && processManager.isServerRunning(server.id)) {
                processManager.sendCommand(server.id, cmd)
            }
            userRepository.recordAudit(currentUser, "PLAYER_ACTION", server.name, cmd.ifBlank { sub }, effectiveIp)
            sendJson(out, 200, "OK", JSONObject().put("success", true))
        } else {
            sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
        }
    }

    fun handleFilesRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        fileAction: String?,
        queryParams: Map<String, String>,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String
    ) {
        when {
            fileAction == null && method == "GET" -> {
                val targetPath = queryParams["path"] ?: ""
                val arr = JSONArray()
                serverRepository.listDirectory(server.id, targetPath).forEach { f ->
                    arr.put(
                        JSONObject()
                            .put("name", f.name)
                            .put("relativePath", f.relativePath)
                            .put("isDirectory", f.isDirectory)
                            .put("sizeBytes", f.sizeBytes)
                            .put("isEditable", f.isEditable)
                            .put("lastModified", f.lastModified)
                    )
                }
                sendJson(out, 200, "OK", JSONObject().put("path", targetPath).put("files", arr))
            }
            fileAction == "read" && method == "GET" -> {
                val targetPath = queryParams["path"] ?: ""
                sendJson(out, 200, "OK", JSONObject().put("path", targetPath).put("content", serverRepository.readFile(server.id, targetPath)))
            }
            fileAction == "write" && method == "POST" -> {
                val j = parseJson(body)
                val target = j.optString("path", "")
                val ok = serverRepository.writeFile(server.id, target, j.optString("content", ""))
                if (ok) userRepository.recordAudit(currentUser, "FILE_EDIT", server.name, target, effectiveIp)
                sendJson(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", JSONObject().put("success", ok))
            }
            fileAction == "create" && method == "POST" -> {
                val j = parseJson(body)
                val ok = serverRepository.createFile(server.id, j.optString("path", ""), j.optString("name", ""), j.optString("content", ""))
                if (ok) userRepository.recordAudit(currentUser, "FILE_CREATE", server.name, j.optString("name", ""), effectiveIp)
                sendJson(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", JSONObject().put("success", ok))
            }
            fileAction == "mkdir" && method == "POST" -> {
                val j = parseJson(body)
                val ok = serverRepository.createDirectory(server.id, j.optString("path", ""), j.optString("name", ""))
                if (ok) userRepository.recordAudit(currentUser, "DIR_CREATE", server.name, j.optString("name", ""), effectiveIp)
                sendJson(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", JSONObject().put("success", ok))
            }
            fileAction == "rename" && method == "POST" -> {
                val j = parseJson(body)
                val ok = serverRepository.renameFileOrDirectory(server.id, j.optString("path", ""), j.optString("newName", ""))
                if (ok) userRepository.recordAudit(currentUser, "FILE_RENAME", server.name, j.optString("path", ""), effectiveIp)
                sendJson(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", JSONObject().put("success", ok))
            }
            fileAction == "delete" && method == "POST" -> {
                val j = parseJson(body)
                val ok = serverRepository.deleteFileOrDirectory(server.id, j.optString("path", ""))
                if (ok) userRepository.recordAudit(currentUser, "FILE_DELETE", server.name, j.optString("path", ""), effectiveIp)
                sendJson(out, if (ok) 200 else 400, if (ok) "OK" else "Bad Request", JSONObject().put("success", ok))
            }
            fileAction == "unzip" && method == "POST" -> {
                val j = parseJson(body)
                val res = serverRepository.unzipFile(server.id, j.optString("path", ""), j.optBoolean("deleteZipAfter", false))
                res.fold(
                    onSuccess = { count ->
                        userRepository.recordAudit(currentUser, "FILE_UNZIP", server.name, "${j.optString("path", "")} ($count files)", effectiveIp)
                        sendJson(out, 200, "OK", JSONObject().put("success", true).put("extractedCount", count))
                    },
                    onFailure = { err ->
                        sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Unzip failed"))
                    }
                )
            }
            fileAction == "download" && method == "GET" -> {
                val targetPath = queryParams["path"] ?: ""
                val root = serverRepository.getServerDirectory(server.id)
                val file = File(root, targetPath)
                if (!file.exists() || !file.isFile || !isPathInside(file, root)) {
                    sendJson(out, 404, "Not Found", JSONObject().put("error", "File not found"))
                } else {
                    WebHttpUtils.sendFileStreamResponse(out, "application/octet-stream", file, mapOf("Content-Disposition" to "attachment; filename=\"${file.name.replace("\"", "_")}\""))
                }
            }
            else -> sendJson(out, 405, "Method Not Allowed", JSONObject().put("error", "Method not allowed"))
        }
    }

    fun handleFileUpload(
        out: OutputStream,
        serverId: String,
        queryParams: Map<String, String>,
        tmpFile: File,
        user: PanelUser,
        effectiveIp: String
    ) {
        if (serverRepository.getServer(serverId) == null) {
            sendJson(out, 404, "Not Found", JSONObject().put("error", "Server not found"))
            return
        }
        val dirPath = queryParams["path"] ?: ""
        val safeName = File(queryParams["name"] ?: "").name.trim()
        if (safeName.isBlank() || safeName == "." || safeName == "..") {
            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Invalid file name."))
            return
        }
        val root = serverRepository.getServerDirectory(serverId)
        val destDir = if (dirPath.isBlank()) root else File(root, dirPath)
        val destFile = File(destDir, safeName)
        if (!isPathInside(destFile, root)) {
            sendJson(out, 403, "Forbidden", JSONObject().put("error", "Path outside server directory."))
            return
        }
        destFile.parentFile?.mkdirs()
        tmpFile.copyTo(destFile, overwrite = true)
        val rel = if (dirPath.isBlank()) safeName else "$dirPath/$safeName"
        userRepository.recordAudit(user, "FILE_UPLOAD", serverId, rel, effectiveIp)
        sendJson(out, 200, "OK", JSONObject().put("success", true).put("path", rel))
    }

    suspend fun handleBackupsRoutes(
        out: OutputStream,
        method: String,
        server: MinecraftServer,
        sub: String?,
        subAction: String?,
        queryParams: Map<String, String>,
        body: String,
        currentUser: PanelUser,
        effectiveIp: String,
        requirePerm: (Permission) -> Boolean
    ) {
        val perm = when {
            sub == "cloud" -> Permission.SERVER_BACKUPS_MANAGE
            method == "GET" || sub == "create" -> Permission.SERVER_BACKUPS_CREATE
            else -> Permission.SERVER_BACKUPS_MANAGE
        }
        if (!requirePerm(perm)) return

        when {
            sub == null && method == "GET" -> {
                val arr = JSONArray()
                serverRepository.listBackups(server.id).forEach { b ->
                    arr.put(
                        JSONObject()
                            .put("id", b.id)
                            .put("name", b.name)
                            .put("fileName", b.fileName)
                            .put("sizeBytes", b.sizeBytes)
                            .put("timestamp", b.timestamp)
                            .put("isWorldOnly", b.isWorldOnly)
                    )
                }
                sendJson(out, 200, "OK", JSONObject().put("backups", arr))
            }
            sub == "create" && method == "POST" -> {
                val j = parseJson(body)
                val worldOnly = j.optBoolean("isWorldOnly", j.optBoolean("worldOnly", true))
                serverRepository.createBackup(server.id, worldOnly).fold(
                    onSuccess = { b ->
                        userRepository.recordAudit(currentUser, "BACKUP_CREATE", server.name, b.fileName, effectiveIp)
                        sendJson(out, 200, "OK", JSONObject().put("success", true).put("fileName", b.fileName))
                    },
                    onFailure = { err ->
                        sendJson(out, 500, "Internal Server Error", JSONObject().put("error", err.message ?: "Backup failed"))
                    }
                )
            }
            sub == "download" && method == "GET" -> {
                val rawName = queryParams["file"] ?: queryParams["fileName"] ?: ""
                val name = File(rawName).name
                val f = File(serverRepository.getBackupsDirectory(server.id), name)
                if (name.isBlank() || !f.exists() || !f.isFile) {
                    sendJson(out, 404, "Not Found", JSONObject().put("error", "Backup not found"))
                } else {
                    WebHttpUtils.sendFileStreamResponse(out, "application/zip", f, mapOf("Content-Disposition" to "attachment; filename=\"$name\""))
                }
            }
            sub == "restore" && method == "POST" -> {
                if (processManager.isServerRunning(server.id)) {
                    sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before restoring a backup."))
                    return
                }
                val name = parseJson(body).optString("fileName", "")
                serverRepository.restoreBackup(server.id, name).fold(
                    onSuccess = {
                        userRepository.recordAudit(currentUser, "BACKUP_RESTORE", server.name, name, effectiveIp)
                        sendJson(out, 200, "OK", JSONObject().put("success", true))
                    },
                    onFailure = { err ->
                        sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Restore failed"))
                    }
                )
            }
            sub == "delete" && method == "POST" -> {
                val name = parseJson(body).optString("fileName", "")
                val ok = serverRepository.deleteBackup(server.id, name)
                if (ok) userRepository.recordAudit(currentUser, "BACKUP_DELETE", server.name, name, effectiveIp)
                sendJson(out, if (ok) 200 else 404, if (ok) "OK" else "Not Found", JSONObject().put("success", ok))
            }
            sub == "cloud" -> {
                when {
                    subAction == null && method == "GET" -> {
                        cloudManager.listCloudBackups(server.id).fold(
                            onSuccess = { arr -> sendJson(out, 200, "OK", JSONObject().put("backups", arr)) },
                            onFailure = { err -> sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed listing cloud backups")) }
                        )
                    }
                    subAction == "upload" && method == "POST" -> {
                        val fileName = File(parseJson(body).optString("fileName", "")).name
                        val f = File(serverRepository.getBackupsDirectory(server.id), fileName)
                        if (fileName.isBlank() || !f.exists()) {
                            sendJson(out, 404, "Not Found", JSONObject().put("error", "Local backup file not found."))
                            return
                        }
                        cloudManager.uploadBackupToCloud(server.id, f).fold(
                            onSuccess = { msg ->
                                userRepository.recordAudit(currentUser, "CLOUD_BACKUP_UPLOAD", server.name, fileName, effectiveIp)
                                sendJson(out, 200, "OK", JSONObject().put("success", true).put("message", msg))
                            },
                            onFailure = { err -> sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Cloud upload failed")) }
                        )
                    }
                    subAction == "restore" && method == "POST" -> {
                        if (processManager.isServerRunning(server.id)) {
                            sendJson(out, 400, "Bad Request", JSONObject().put("error", "Stop the server before restoring a cloud snapshot."))
                            return
                        }
                        val j = parseJson(body)
                        val backupId = j.optString("backupId", "").trim()
                        val safeName = File(j.optString("fileName", "cloud_backup.zip")).name.ifBlank { "cloud_backup.zip" }
                        val tmp = File(paths.tmpDir, "cloud_restore_${UUID.randomUUID()}_$safeName")
                        try {
                            cloudManager.downloadCloudBackup(backupId, tmp).fold(
                                onSuccess = { downloaded ->
                                    val destBackup = File(serverRepository.getBackupsDirectory(server.id), safeName)
                                    downloaded.copyTo(destBackup, overwrite = true)
                                    val res = serverRepository.restoreBackup(server.id, safeName)
                                    if (res.isSuccess) {
                                        userRepository.recordAudit(currentUser, "CLOUD_BACKUP_RESTORE", server.name, safeName, effectiveIp)
                                        sendJson(out, 200, "OK", JSONObject().put("success", true).put("message", "Cloud snapshot restored."))
                                    } else {
                                        sendJson(out, 400, "Bad Request", JSONObject().put("error", "Failed extracting cloud snapshot."))
                                    }
                                },
                                onFailure = { err -> sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed downloading cloud snapshot.")) }
                            )
                        } finally {
                            tmp.delete()
                        }
                    }
                    subAction == "delete" && method == "POST" -> {
                        val backupId = parseJson(body).optString("backupId", "").trim()
                        cloudManager.deleteCloudBackup(backupId).fold(
                            onSuccess = {
                                userRepository.recordAudit(currentUser, "CLOUD_BACKUP_DELETE", server.name, backupId, effectiveIp)
                                sendJson(out, 200, "OK", JSONObject().put("success", true))
                            },
                            onFailure = { err -> sendJson(out, 400, "Bad Request", JSONObject().put("error", err.message ?: "Failed deleting cloud snapshot.")) }
                        )
                    }
                }
            }
            else -> sendJson(out, 200, "OK", JSONObject().put("backups", JSONArray()))
        }
    }

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
