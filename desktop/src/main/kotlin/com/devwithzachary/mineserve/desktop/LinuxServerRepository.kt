package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LinuxServerRepository(
    private val paths: LinuxDataPaths
) {
    companion object {
        private const val TAG = "LinuxServerRepository"
        private val EDITABLE_EXTS = setOf(
            "properties", "txt", "json", "yml", "yaml", "toml", "cfg",
            "conf", "sh", "bat", "log", "mcmeta", "lang", "csv", "xml", "ini", "sk"
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val lock = Any()

    private val _servers = MutableStateFlow<List<MinecraftServer>>(emptyList())
    val servers: StateFlow<List<MinecraftServer>> = _servers.asStateFlow()

    init {
        loadServers()
    }

    fun loadServers(): List<MinecraftServer> = synchronized(lock) {
        val loaded = try {
            if (paths.serversJson.exists()) {
                json.decodeFromString<List<MinecraftServer>>(paths.serversJson.readText())
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed to load servers.json", e)
            emptyList()
        }
        _servers.value = loaded
        loaded
    }

    private fun saveServersLocked(list: List<MinecraftServer>) {
        val tmp = File(paths.rootDir, "servers.json.tmp")
        tmp.writeText(json.encodeToString(list))
        if (!tmp.renameTo(paths.serversJson)) {
            tmp.copyTo(paths.serversJson, overwrite = true)
            tmp.delete()
        }
        _servers.value = list
    }

    fun getServer(serverId: String): MinecraftServer? = _servers.value.firstOrNull { it.id == serverId }

    fun getServerDirectory(serverId: String): File = File(paths.serversDir, serverId).apply { mkdirs() }

    fun getBackupsDirectory(serverId: String): File = File(getServerDirectory(serverId), "backups").apply { mkdirs() }

    fun createServer(
        name: String,
        type: ServerType,
        version: String,
        port: Int,
        ramMb: Int,
        motd: String,
        jarFileName: String = "server.jar",
        serverBuild: String? = null,
        eulaAccepted: Boolean = true
    ): MinecraftServer = synchronized(lock) {
        val id = UUID.randomUUID().toString().take(8)
        val javaVer = determineJavaVersion(version, type)
        val server = MinecraftServer(
            id = id,
            name = name,
            type = type,
            version = version,
            port = port,
            allocatedRamMb = ramMb,
            javaVersion = javaVer,
            jarFileName = jarFileName,
            serverBuild = serverBuild,
            eulaAccepted = eulaAccepted
        )
        val dir = getServerDirectory(id)
        File(dir, "eula.txt").writeText("eula=$eulaAccepted\n")
        saveServerProperties(id, ServerProperties(motd = motd, serverPort = port))

        val next = _servers.value + server
        saveServersLocked(next)
        server
    }

    fun updateServer(updated: MinecraftServer) = synchronized(lock) {
        val next = _servers.value.map { if (it.id == updated.id) updated else it }
        saveServersLocked(next)
    }

    fun deleteServer(serverId: String): Boolean = synchronized(lock) {
        val next = _servers.value.filterNot { it.id == serverId }
        saveServersLocked(next)
        val dir = File(paths.serversDir, serverId)
        if (dir.exists()) dir.deleteRecursively() else true
    }

    fun loadServerProperties(serverId: String): ServerProperties {
        val propFile = File(getServerDirectory(serverId), "server.properties")
        if (!propFile.exists()) return ServerProperties()
        return try {
            ServerProperties.parse(propFile.readText())
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed loading server.properties for $serverId", e)
            ServerProperties()
        }
    }

    fun saveServerProperties(serverId: String, p: ServerProperties) {
        val propFile = File(getServerDirectory(serverId), "server.properties")
        val version = getServer(serverId)?.version
        propFile.writeText(p.toPropertiesFileContent(version))
    }

    // --- File Manager ---

    private fun resolveSafeFile(serverId: String, relativePath: String): File? {
        val root = getServerDirectory(serverId)
        val target = if (relativePath.isBlank()) root else File(root, relativePath)
        return if (target.canonicalPath.startsWith(root.canonicalPath)) target else null
    }

    fun listDirectory(serverId: String, relativePath: String = ""): List<FileEntry> {
        val dir = resolveSafeFile(serverId, relativePath) ?: return emptyList()
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return (dir.listFiles() ?: emptyArray())
            .map { f ->
                val rel = if (relativePath.isBlank()) f.name else "$relativePath/${f.name}"
                val ext = f.extension.lowercase()
                FileEntry(
                    name = f.name,
                    relativePath = rel,
                    isDirectory = f.isDirectory,
                    sizeBytes = if (f.isDirectory) 0L else f.length(),
                    lastModified = f.lastModified(),
                    extension = ext,
                    isEditable = !f.isDirectory && EDITABLE_EXTS.contains(ext)
                )
            }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun readFile(serverId: String, relativePath: String): String {
        val f = resolveSafeFile(serverId, relativePath) ?: return ""
        return if (f.exists() && f.isFile) runCatching { f.readText() }.getOrDefault("") else ""
    }

    fun writeFile(serverId: String, relativePath: String, content: String): Boolean {
        val f = resolveSafeFile(serverId, relativePath) ?: return false
        return runCatching {
            f.parentFile?.mkdirs()
            f.writeText(content)
            true
        }.getOrDefault(false)
    }

    fun createFile(serverId: String, dirPath: String, name: String, content: String = ""): Boolean {
        val safeName = File(name).name.trim()
        if (safeName.isBlank() || safeName == "." || safeName == "..") return false
        val rel = if (dirPath.isBlank()) safeName else "$dirPath/$safeName"
        val f = resolveSafeFile(serverId, rel) ?: return false
        if (f.exists()) return false
        return runCatching {
            f.parentFile?.mkdirs()
            f.writeText(content)
            true
        }.getOrDefault(false)
    }

    fun createDirectory(serverId: String, dirPath: String, name: String): Boolean {
        val safeName = File(name).name.trim()
        if (safeName.isBlank() || safeName == "." || safeName == "..") return false
        val rel = if (dirPath.isBlank()) safeName else "$dirPath/$safeName"
        val f = resolveSafeFile(serverId, rel) ?: return false
        return f.mkdirs()
    }

    fun renameFileOrDirectory(serverId: String, relativePath: String, newName: String): Boolean {
        val safeName = File(newName).name.trim()
        if (safeName.isBlank() || safeName == "." || safeName == "..") return false
        val src = resolveSafeFile(serverId, relativePath) ?: return false
        if (!src.exists()) return false
        val dst = File(src.parentFile, safeName)
        val root = getServerDirectory(serverId)
        if (!dst.canonicalPath.startsWith(root.canonicalPath)) return false
        return src.renameTo(dst)
    }

    fun deleteFileOrDirectory(serverId: String, relativePath: String): Boolean {
        val target = resolveSafeFile(serverId, relativePath) ?: return false
        if (target.canonicalPath == getServerDirectory(serverId).canonicalPath) return false
        if (!target.exists()) return false
        return if (target.isDirectory) target.deleteRecursively() else target.delete()
    }

    fun unzipFile(serverId: String, relativePath: String, deleteZipAfter: Boolean = false): Result<Int> {
        val zip = resolveSafeFile(serverId, relativePath)
            ?: return Result.failure(SecurityException("Path outside server directory"))
        val destDir = zip.parentFile ?: getServerDirectory(serverId)
        return FileStorageUtils.extractZipSafe(zip, destDir, deleteZipAfter)
    }

    // --- Backups & Retention ---

    fun listBackups(serverId: String): List<BackupEntry> {
        val dir = getBackupsDirectory(serverId)
        return (dir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".zip", ignoreCase = true) }
            .sortedByDescending { it.lastModified() }
            .map { f ->
                BackupEntry(
                    id = f.name,
                    serverId = serverId,
                    name = f.nameWithoutExtension,
                    fileName = f.name,
                    sizeBytes = f.length(),
                    timestamp = f.lastModified(),
                    isWorldOnly = f.name.contains("world", ignoreCase = true)
                )
            }
    }

    suspend fun createBackup(
        serverId: String,
        worldOnly: Boolean = true,
        retentionLimit: Int = 10
    ): Result<BackupEntry> = withContext(Dispatchers.IO) {
        try {
            val serverDir = getServerDirectory(serverId)
            val backupsDir = getBackupsDirectory(serverId)
            val prefix = if (worldOnly) "world_backup" else "full_backup"
            val zipFile = File(backupsDir, "${prefix}_${System.currentTimeMillis()}.zip")

            val sourceDir = if (worldOnly) {
                val levelName = loadServerProperties(serverId).levelName.ifBlank { "world" }
                File(serverDir, levelName).takeIf { it.exists() } ?: serverDir
            } else {
                serverDir
            }

            java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(zipFile))).use { zos ->
                FileStorageUtils.zipDirectoryToStream(
                    sourceDir = sourceDir,
                    zipOut = zos,
                    excludeFilter = { f -> f.isDirectory && f.name == "backups" }
                )
            }

            // Apply backup retention policy
            if (retentionLimit > 0) {
                val all = listBackups(serverId)
                if (all.size > retentionLimit) {
                    all.drop(retentionLimit).forEach { old ->
                        runCatching { File(backupsDir, old.fileName).delete() }
                    }
                }
            }

            Result.success(
                BackupEntry(
                    id = zipFile.name,
                    serverId = serverId,
                    name = zipFile.nameWithoutExtension,
                    fileName = zipFile.name,
                    sizeBytes = zipFile.length(),
                    timestamp = zipFile.lastModified(),
                    isWorldOnly = worldOnly
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(serverId: String, fileName: String): Result<Int> = withContext(Dispatchers.IO) {
        val zipFile = File(getBackupsDirectory(serverId), File(fileName).name)
        if (!zipFile.exists()) return@withContext Result.failure(IllegalArgumentException("Backup not found"))
        val serverDir = getServerDirectory(serverId)
        val targetDir = if (fileName.contains("world", ignoreCase = true)) {
            val levelName = loadServerProperties(serverId).levelName.ifBlank { "world" }
            File(serverDir, levelName).apply { mkdirs() }
        } else {
            serverDir
        }
        FileStorageUtils.extractZipSafe(zipFile, targetDir, deleteZipAfter = false)
    }

    fun deleteBackup(serverId: String, fileName: String): Boolean {
        val zipFile = File(getBackupsDirectory(serverId), File(fileName).name)
        return zipFile.exists() && zipFile.delete()
    }
}
