package com.devwithzachary.mineserve.admin.web

import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.engine.ServerSoftwareManager
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.repository.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class WebApiResult(
    val statusCode: Int,
    val statusText: String,
    val body: JSONObject
)

private data class CreationJob(
    val status: String,
    val progress: Int,
    val message: String,
    val serverId: String? = null
)

/**
 * Handles extended Web Admin API endpoints for server creation, software versions,
 * server settings/properties, and player list management.
 */
internal class WebAdminApiHandlers(
    private val scope: CoroutineScope,
    private val serverRepository: ServerRepository,
    private val processManager: ServerProcessManager,
    private val serverSoftwareManager: ServerSoftwareManager
) {
    private val creationJobs = ConcurrentHashMap<String, CreationJob>()

    fun handleSoftwareTypes(): WebApiResult {
        val arr = JSONArray()
        for (type in ServerType.entries) {
            if (type == ServerType.CUSTOM) continue
            arr.put(
                JSONObject()
                    .put("name", type.name)
                    .put("displayName", type.displayName)
                    .put("description", type.description)
                    .put("supportsPlugins", type.supportsPlugins)
                    .put("supportsMods", type.supportsMods)
                    .put("defaultJavaVersion", type.defaultJavaVersion)
            )
        }
        return ok(JSONObject().put("types", arr))
    }

    suspend fun handleSoftwareVersions(typeParam: String?): WebApiResult {
        val type = runCatching { ServerType.valueOf((typeParam ?: "PAPER").uppercase()) }
            .getOrElse { return badRequest("Invalid server type") }
        val versions = serverSoftwareManager.fetchAvailableVersionsForServer(type)
        return ok(JSONObject().put("type", type.name).put("versions", JSONArray(versions)))
    }

    fun handleCreateServer(body: String): WebApiResult {
        val json = parseJsonObject(body)
        val name = json.optString("name", "").trim()
        val typeName = json.optString("type", "PAPER").uppercase()
        val version = json.optString("version", "").trim()
        val port = json.optInt("port", 25565)
        val ramMb = json.optInt("ramMb", 2048)
        val motd = json.optString("motd", "A MineServe Minecraft Server").ifBlank { "A MineServe Minecraft Server" }
        val eulaAccepted = json.optBoolean("eulaAccepted", false)

        if (name.isBlank()) return badRequest("Server name is required.")
        if (version.isBlank()) return badRequest("Minecraft version is required.")
        if (port !in 1..65535) return badRequest("Port must be between 1 and 65535.")
        if (ramMb < 256) return badRequest("RAM must be at least 256 MB.")
        if (!eulaAccepted) return badRequest("You must accept the Minecraft EULA.")

        val type = runCatching { ServerType.valueOf(typeName) }
            .getOrElse { return badRequest("Unsupported server type: $typeName") }

        val jobId = UUID.randomUUID().toString().take(8)
        creationJobs[jobId] = CreationJob("running", 5, "Resolving ${type.displayName} $version...")

        scope.launch {
            try {
                val buildInfo = serverSoftwareManager.resolveLatestBuild(type, version)
                creationJobs[jobId] = CreationJob("running", 20, "Creating server directory...")

                val isInstaller = type == ServerType.NEOFORGE || type == ServerType.FORGE
                val jarFileName = when {
                    isInstaller -> "installer.jar"
                    type == ServerType.BEDROCK_BDS -> "bedrock_server"
                    else -> "server.jar"
                }
                val server = serverRepository.createServer(
                    name = name,
                    type = type,
                    version = version,
                    port = port,
                    ramMb = ramMb,
                    motd = motd,
                    jarFileName = jarFileName,
                    serverBuild = buildInfo?.first,
                    eulaAccepted = true
                )

                val serverDir = serverRepository.getServerDirectory(server.id)
                serverSoftwareManager.provisionServerSoftware(server, serverDir, buildInfo?.second) { msg, pct ->
                    creationJobs[jobId] = CreationJob("running", pct.coerceIn(20, 99), msg, server.id)
                }

                creationJobs[jobId] = CreationJob("done", 100, "Server created successfully!", server.id)
            } catch (e: Exception) {
                creationJobs[jobId] = CreationJob("error", 0, e.message ?: "Server creation failed")
            }
        }

        return WebApiResult(202, "Accepted", JSONObject().put("jobId", jobId))
    }

    fun handleGetCreationJob(jobId: String): WebApiResult {
        val job = creationJobs[jobId] ?: return notFound("Creation job not found")
        val obj = JSONObject()
            .put("status", job.status)
            .put("progress", job.progress)
            .put("message", job.message)
        if (job.serverId != null) obj.put("serverId", job.serverId)
        return ok(obj)
    }

    suspend fun handleDeleteServer(serverId: String): WebApiResult {
        processManager.forceStopAndCleanup(serverId)
        val deleted = serverRepository.deleteServer(serverId)
        return if (deleted) {
            ok(JSONObject().put("success", true))
        } else {
            WebApiResult(500, "Internal Server Error", JSONObject().put("error", "Failed to delete server"))
        }
    }

    suspend fun handleGetSettings(serverId: String): WebApiResult {
        val server = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val props = serverRepository.loadServerProperties(serverId)

        val srvObj = JSONObject()
            .put("id", server.id)
            .put("name", server.name)
            .put("type", server.type.name)
            .put("version", server.version)
            .put("port", server.port)
            .put("allocatedRamMb", server.allocatedRamMb)
            .put("javaVersion", server.javaVersion)
            .put("jvmArgs", server.jvmArgs)
            .put("autoStart", server.autoStart)
            .put("eulaAccepted", server.eulaAccepted)
            .put("serverBuild", server.serverBuild ?: "")

        val propsObj = JSONObject()
            .put("motd", props.motd)
            .put("serverPort", props.serverPort)
            .put("maxPlayers", props.maxPlayers)
            .put("gamemode", props.gamemode)
            .put("difficulty", props.difficulty)
            .put("pvp", props.pvp)
            .put("hardcore", props.hardcore)
            .put("onlineMode", props.onlineMode)
            .put("whiteList", props.whiteList)
            .put("enforceWhitelist", props.enforceWhitelist)
            .put("enableCommandBlock", props.enableCommandBlock)
            .put("spawnMonsters", props.spawnMonsters)
            .put("spawnAnimals", props.spawnAnimals)
            .put("spawnNpcs", props.spawnNpcs)
            .put("allowFlight", props.allowFlight)
            .put("allowNether", props.allowNether)
            .put("viewDistance", props.viewDistance)
            .put("simulationDistance", props.simulationDistance)
            .put("levelName", props.levelName)
            .put("levelSeed", props.levelSeed)
            .put("forceGamemode", props.forceGamemode)

        return ok(JSONObject().put("server", srvObj).put("properties", propsObj))
    }

    suspend fun handleSaveSettings(serverId: String, body: String): WebApiResult {
        val existingServer = serverRepository.getServer(serverId) ?: return notFound("Server not found")
        val existingProps = serverRepository.loadServerProperties(serverId)
        val json = parseJsonObject(body)

        val srvJson = json.optJSONObject("server") ?: JSONObject()
        val propJson = json.optJSONObject("properties") ?: JSONObject()

        val newPort = propJson.optInt("serverPort", srvJson.optInt("port", existingProps.serverPort))
            .coerceIn(1, 65535)
        val newMotd = propJson.optString("motd", existingProps.motd)
        val eulaAccepted = srvJson.optBoolean("eulaAccepted", existingServer.eulaAccepted)

        val updatedServer = existingServer.copy(
            name = srvJson.optString("name", existingServer.name).trim().ifBlank { existingServer.name },
            allocatedRamMb = srvJson.optInt("allocatedRamMb", existingServer.allocatedRamMb).coerceAtLeast(256),
            javaVersion = srvJson.optInt("javaVersion", existingServer.javaVersion),
            port = newPort,
            motd = newMotd,
            jvmArgs = srvJson.optString("jvmArgs", existingServer.jvmArgs),
            autoStart = srvJson.optBoolean("autoStart", existingServer.autoStart),
            eulaAccepted = eulaAccepted
        )
        serverRepository.updateServer(updatedServer)

        val serverDir = serverRepository.getServerDirectory(serverId)
        File(serverDir, "eula.txt").writeText("eula=${if (eulaAccepted) "true" else "false"}\n")

        val updatedProps = existingProps.copy(
            motd = newMotd,
            serverPort = newPort,
            maxPlayers = propJson.optInt("maxPlayers", existingProps.maxPlayers).coerceIn(1, 1000),
            gamemode = propJson.optString("gamemode", existingProps.gamemode),
            difficulty = propJson.optString("difficulty", existingProps.difficulty),
            pvp = propJson.optBoolean("pvp", existingProps.pvp),
            hardcore = propJson.optBoolean("hardcore", existingProps.hardcore),
            onlineMode = propJson.optBoolean("onlineMode", existingProps.onlineMode),
            whiteList = propJson.optBoolean("whiteList", existingProps.whiteList),
            enforceWhitelist = propJson.optBoolean("enforceWhitelist", existingProps.enforceWhitelist),
            enableCommandBlock = propJson.optBoolean("enableCommandBlock", existingProps.enableCommandBlock),
            spawnMonsters = propJson.optBoolean("spawnMonsters", existingProps.spawnMonsters),
            spawnAnimals = propJson.optBoolean("spawnAnimals", existingProps.spawnAnimals),
            spawnNpcs = propJson.optBoolean("spawnNpcs", existingProps.spawnNpcs),
            allowFlight = propJson.optBoolean("allowFlight", existingProps.allowFlight),
            allowNether = propJson.optBoolean("allowNether", existingProps.allowNether),
            viewDistance = propJson.optInt("viewDistance", existingProps.viewDistance).coerceIn(2, 32),
            simulationDistance = propJson.optInt("simulationDistance", existingProps.simulationDistance).coerceIn(2, 32),
            levelName = propJson.optString("levelName", existingProps.levelName).trim().ifBlank { existingProps.levelName },
            levelSeed = propJson.optString("levelSeed", existingProps.levelSeed),
            forceGamemode = propJson.optBoolean("forceGamemode", existingProps.forceGamemode)
        )
        serverRepository.saveServerProperties(serverId, updatedProps)

        return ok(JSONObject().put("success", true))
    }

    suspend fun handleGetPlayers(serverId: String): WebApiResult {
        val lists = serverRepository.loadPlayerLists(serverId)
        val online = processManager.serverMetrics.value[serverId]?.onlinePlayers ?: emptyList()

        val whitelistArr = JSONArray().apply {
            lists.whitelist.forEach { put(JSONObject().put("name", it.name).put("uuid", it.uuid)) }
        }
        val opsArr = JSONArray().apply {
            lists.ops.forEach { put(JSONObject().put("name", it.name).put("level", it.level).put("uuid", it.uuid)) }
        }
        val bannedArr = JSONArray().apply {
            lists.bannedPlayers.forEach {
                put(JSONObject().put("name", it.name).put("reason", it.reason).put("created", it.created))
            }
        }
        val bannedIpArr = JSONArray().apply {
            lists.bannedIps.forEach {
                put(JSONObject().put("ip", it.ip).put("reason", it.reason).put("created", it.created))
            }
        }

        return ok(
            JSONObject()
                .put("onlinePlayers", JSONArray(online))
                .put("whitelist", whitelistArr)
                .put("ops", opsArr)
                .put("bannedPlayers", bannedArr)
                .put("bannedIps", bannedIpArr)
        )
    }

    suspend fun handlePlayerAction(serverId: String, endpoint: String, body: String): WebApiResult {
        val json = parseJsonObject(body)
        val action = json.optString("action", "add").lowercase()
        val running = processManager.isServerRunning(serverId)

        val success = when (endpoint) {
            "whitelist" -> {
                val name = json.optString("name", "").trim()
                if (name.isBlank()) return badRequest("Player name is required")
                if (action == "remove") {
                    if (running) {
                        processManager.sendCommand(serverId, "whitelist remove $name")
                        processManager.sendCommand(serverId, "whitelist reload")
                    }
                    serverRepository.removeWhitelistPlayer(serverId, name)
                } else {
                    if (running) {
                        processManager.sendCommand(serverId, "whitelist add $name")
                        processManager.sendCommand(serverId, "whitelist reload")
                    }
                    serverRepository.addWhitelistPlayer(serverId, name)
                }
            }
            "ops" -> {
                val name = json.optString("name", "").trim()
                if (name.isBlank()) return badRequest("Player name is required")
                val level = json.optInt("level", 4).coerceIn(1, 4)
                if (action == "remove") {
                    if (running) processManager.sendCommand(serverId, "deop $name")
                    serverRepository.removeOp(serverId, name)
                } else {
                    if (running) processManager.sendCommand(serverId, "op $name")
                    serverRepository.addOp(serverId, name, level)
                }
            }
            "ban" -> {
                val name = json.optString("name", "").trim()
                if (name.isBlank()) return badRequest("Player name is required")
                val reason = json.optString("reason", "").ifBlank { "Banned by an operator." }
                if (action == "remove") {
                    if (running) processManager.sendCommand(serverId, "pardon $name")
                    serverRepository.removeBannedPlayer(serverId, name)
                } else {
                    if (running) processManager.sendCommand(serverId, "ban $name $reason")
                    serverRepository.addBannedPlayer(serverId, name, reason)
                }
            }
            "ban-ip" -> {
                val ip = json.optString("ip", "").trim()
                if (ip.isBlank()) return badRequest("IP address is required")
                val reason = json.optString("reason", "").ifBlank { "Banned by an operator." }
                if (action == "remove") {
                    if (running) processManager.sendCommand(serverId, "pardon-ip $ip")
                    serverRepository.removeBannedIp(serverId, ip)
                } else {
                    if (running) processManager.sendCommand(serverId, "ban-ip $ip $reason")
                    serverRepository.addBannedIp(serverId, ip, reason)
                }
            }
            "kick" -> {
                val name = json.optString("name", "").trim()
                if (name.isBlank()) return badRequest("Player name is required")
                val reason = json.optString("reason", "Kicked via Web Admin").ifBlank { "Kicked via Web Admin" }
                if (running) {
                    processManager.sendCommand(serverId, "kick $name $reason")
                    true
                } else {
                    return badRequest("Server is not running")
                }
            }
            else -> return notFound("Unknown player action: $endpoint")
        }

        return ok(JSONObject().put("success", success))
    }

    private fun parseJsonObject(body: String): JSONObject =
        runCatching { JSONObject(if (body.isBlank()) "{}" else body) }.getOrElse { JSONObject() }

    private fun ok(body: JSONObject) = WebApiResult(200, "OK", body)
    private fun badRequest(msg: String) = WebApiResult(400, "Bad Request", JSONObject().put("error", msg))
    private fun notFound(msg: String) = WebApiResult(404, "Not Found", JSONObject().put("error", msg))
}
