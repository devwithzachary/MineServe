package com.devwithzachary.mineserve.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.model.WhitelistEntry
import com.devwithzachary.mineserve.model.OpEntry
import com.devwithzachary.mineserve.model.BannedPlayerEntry
import com.devwithzachary.mineserve.model.BannedIpEntry
import com.devwithzachary.mineserve.model.ServerPlayerLists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

class ServerRepository(
    private val context: Context,
    private val pRootEngine: PRootEngine,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true; isLenient = true }
) {
    companion object {
        private const val TAG = "ServerRepository"

        fun extractZip(
            zipFile: File,
            targetDir: File,
            deleteZipAfter: Boolean = false
        ): Result<Int> = com.devwithzachary.mineserve.util.FileStorageUtils.extractZipSafe(zipFile, targetDir, deleteZipAfter)
    }

    val serversDir: File get() = pRootEngine.serversDir

    val playerListRepository by lazy { PlayerListRepository(serversDir, json) }
    val fileRepository by lazy { ServerFileRepository(context, serversDir) }

    private val _servers = MutableStateFlow<List<MinecraftServer>>(emptyList())
    val servers: StateFlow<List<MinecraftServer>> = _servers.asStateFlow()

    suspend fun loadServers(): List<MinecraftServer> = withContext(Dispatchers.IO) {
        if (!serversDir.exists()) serversDir.mkdirs()
        val list = mutableListOf<MinecraftServer>()

        serversDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val configFile = File(dir, "server_config.json")
                if (configFile.exists()) {
                    try {
                        val server = json.decodeFromString<MinecraftServer>(configFile.readText())
                        val propFile = File(dir, "server.properties")
                        val actualPort = if (propFile.exists()) {
                            try {
                                ServerProperties.parse(propFile.readText()).serverPort
                            } catch (_: Exception) {
                                if (server.port in 1..65535) server.port else 25565
                            }
                        } else {
                            if (server.port in 1..65535) server.port else 25565
                        }
                        list.add(server.copy(port = actualPort, status = ServerStatus.STOPPED))
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed reading server config in ${dir.name}", e)
                    }
                }
            }
        }

        val sorted = list.sortedByDescending { it.createdAt }
        _servers.value = sorted
        sorted
    }

    suspend fun createServer(
        name: String,
        type: ServerType,
        version: String,
        port: Int = 25565,
        ramMb: Int = 2048,
        motd: String = "A MineServe Minecraft Server",
        jarFileName: String = "server.jar",
        serverBuild: String? = null,
        eulaAccepted: Boolean = false
    ): MinecraftServer = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString().take(8)
        val serverDir = File(serversDir, id).apply { mkdirs() }

        val server = MinecraftServer(
            id = id,
            name = name,
            type = type,
            version = version,
            port = port,
            allocatedRamMb = ramMb,
            javaVersion = determineJavaVersion(version, type),
            status = ServerStatus.STOPPED,
            motd = motd,
            jarFileName = jarFileName,
            serverBuild = serverBuild,
            eulaAccepted = eulaAccepted
        )

        // Save server_config.json
        val configFile = File(serverDir, "server_config.json")
        configFile.writeText(json.encodeToString(server))

        // Save initial server.properties
        val properties = ServerProperties(motd = motd, serverPort = port)
        val propFile = File(serverDir, "server.properties")
        propFile.writeText(properties.toPropertiesFileContent(version))

        // Pre-create legacy auth and operator list files for Minecraft releases pre-1.7.5
        listOf("banned-players.txt", "banned-ips.txt", "ops.txt", "white-list.txt").forEach { name ->
            val f = File(serverDir, name)
            if (!f.exists()) {
                try { f.createNewFile() } catch (_: Exception) {}
            }
        }

        // Save eula.txt
        val eulaFile = File(serverDir, "eula.txt")
        eulaFile.writeText("eula=${if (eulaAccepted) "true" else "false"}\n")

        // Create folders
        File(serverDir, "plugins").mkdirs()
        File(serverDir, "mods").mkdirs()
        File(serverDir, "backups").mkdirs()

        loadServers()
        server
    }

    fun getServer(serverId: String): MinecraftServer? {
        _servers.value.find { it.id == serverId }?.let { return it }
        val serverDir = File(serversDir, serverId)
        val configFile = File(serverDir, "server_config.json")
        if (configFile.exists()) {
            return try {
                json.decodeFromString<MinecraftServer>(configFile.readText())
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    suspend fun updateServer(server: MinecraftServer) = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, server.id)
        if (serverDir.exists()) {
            val configFile = File(serverDir, "server_config.json")
            configFile.writeText(json.encodeToString(server))
            loadServers()
        }
    }

    suspend fun deleteServer(serverId: String): Boolean = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        var result = true
        if (serverDir.exists()) {
            result = serverDir.deleteRecursively()
            if (serverDir.exists()) {
                serverDir.listFiles()?.forEach { it.deleteRecursively() }
                serverDir.delete()
            }
        }
        loadServers()
        result
    }

    suspend fun loadServerProperties(serverId: String): ServerProperties = withContext(Dispatchers.IO) {
        val propFile = File(File(serversDir, serverId), "server.properties")
        if (propFile.exists()) {
            ServerProperties.parse(propFile.readText())
        } else {
            ServerProperties()
        }
    }

    suspend fun saveServerProperties(serverId: String, properties: ServerProperties) = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        val server = getServer(serverId)
        val propFile = File(serverDir, "server.properties")
        propFile.writeText(properties.toPropertiesFileContent(server?.version))
        if (server != null && properties.serverPort in 1..65535 && server.port != properties.serverPort) {
            val updated = server.copy(port = properties.serverPort)
            val configFile = File(serverDir, "server_config.json")
            configFile.writeText(json.encodeToString(updated))
        }
        loadServers()
    }

    // Player Lists Delegation
    suspend fun loadPlayerLists(serverId: String): ServerPlayerLists = playerListRepository.loadPlayerLists(serverId)
    suspend fun saveWhitelist(serverId: String, list: List<WhitelistEntry>): Boolean = playerListRepository.saveWhitelist(serverId, list)
    suspend fun saveOps(serverId: String, list: List<OpEntry>): Boolean = playerListRepository.saveOps(serverId, list)
    suspend fun saveBannedPlayers(serverId: String, list: List<BannedPlayerEntry>): Boolean = playerListRepository.saveBannedPlayers(serverId, list)
    suspend fun saveBannedIps(serverId: String, list: List<BannedIpEntry>): Boolean = playerListRepository.saveBannedIps(serverId, list)
    suspend fun addWhitelistPlayer(serverId: String, name: String, uuid: String = ""): Boolean = playerListRepository.addWhitelistPlayer(serverId, name, uuid)
    suspend fun removeWhitelistPlayer(serverId: String, name: String): Boolean = playerListRepository.removeWhitelistPlayer(serverId, name)
    suspend fun addOp(serverId: String, name: String, level: Int = 4, uuid: String = ""): Boolean = playerListRepository.addOp(serverId, name, level, uuid)
    suspend fun removeOp(serverId: String, name: String): Boolean = playerListRepository.removeOp(serverId, name)
    suspend fun addBannedPlayer(serverId: String, name: String, reason: String = "Banned by an operator.", uuid: String = ""): Boolean = playerListRepository.addBannedPlayer(serverId, name, reason, uuid)
    suspend fun removeBannedPlayer(serverId: String, name: String): Boolean = playerListRepository.removeBannedPlayer(serverId, name)
    suspend fun addBannedIp(serverId: String, ip: String, reason: String = "Banned by an operator."): Boolean = playerListRepository.addBannedIp(serverId, ip, reason)
    suspend fun removeBannedIp(serverId: String, ip: String): Boolean = playerListRepository.removeBannedIp(serverId, ip)

    // File Manager Operations Delegation
    suspend fun listDirectory(serverId: String, relativePath: String = ""): List<FileEntry> = fileRepository.listDirectory(serverId, relativePath)
    suspend fun createFile(serverId: String, relativePath: String, fileName: String, content: String = ""): Boolean = fileRepository.createFile(serverId, relativePath, fileName, content)
    suspend fun createDirectory(serverId: String, relativePath: String, dirName: String): Boolean = fileRepository.createDirectory(serverId, relativePath, dirName)
    suspend fun deleteFileOrDirectory(serverId: String, relativePath: String): Boolean = fileRepository.deleteFileOrDirectory(serverId, relativePath)
    suspend fun renameFileOrDirectory(serverId: String, relativePath: String, newName: String): Boolean = fileRepository.renameFileOrDirectory(serverId, relativePath, newName)
    suspend fun duplicateFile(serverId: String, relativePath: String): Boolean = fileRepository.duplicateFile(serverId, relativePath)
    suspend fun readFile(serverId: String, relativePath: String): String = fileRepository.readFile(serverId, relativePath)
    suspend fun writeFile(serverId: String, relativePath: String, content: String): Boolean = fileRepository.writeFile(serverId, relativePath, content)
    suspend fun importFile(serverId: String, relativePath: String, sourceUri: Uri): Boolean = fileRepository.importFile(serverId, relativePath, sourceUri)
    suspend fun exportFileToDownloads(serverId: String, relativePath: String): Boolean = fileRepository.exportFileToDownloads(serverId, relativePath)
    suspend fun unzipFile(serverId: String, relativePath: String, destinationRelativePath: String = "", deleteZipAfter: Boolean = false): Result<Int> = fileRepository.unzipFile(serverId, relativePath, destinationRelativePath, deleteZipAfter)
    suspend fun searchFiles(serverId: String, query: String): List<FileEntry> = fileRepository.searchFiles(serverId, query)
    fun getServerDirectory(serverId: String): File = File(serversDir, serverId).apply { if (!exists()) mkdirs() }
    suspend fun getServerStorageBytes(serverId: String): Long = fileRepository.getServerStorageBytes(serverId)
}
