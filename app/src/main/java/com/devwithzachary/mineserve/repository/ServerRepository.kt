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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class ServerRepository(
    private val context: Context,
    private val pRootEngine: PRootEngine,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
) {
    companion object {
        private const val TAG = "ServerRepository"
    }

    val serversDir: File get() = pRootEngine.serversDir

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
        serverBuild: String? = null
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
            serverBuild = serverBuild
        )

        // Save server_config.json
        val configFile = File(serverDir, "server_config.json")
        configFile.writeText(json.encodeToString(server))

        // Save initial server.properties
        val properties = ServerProperties(motd = motd, serverPort = port)
        val propFile = File(serverDir, "server.properties")
        propFile.writeText(properties.toPropertiesFileContent())

        // Save eula.txt
        val eulaFile = File(serverDir, "eula.txt")
        eulaFile.writeText("eula=true\n")

        // Create folders
        File(serverDir, "plugins").mkdirs()
        File(serverDir, "mods").mkdirs()
        File(serverDir, "backups").mkdirs()

        loadServers()
        server
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
        val propFile = File(serverDir, "server.properties")
        propFile.writeText(properties.toPropertiesFileContent())
    }

    suspend fun readRawConfigFile(serverId: String, fileName: String): String = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        val file = File(serverDir, fileName)
        if (file.exists()) {
            try { file.readText() } catch (e: Exception) { "" }
        } else {
            ""
        }
    }

    suspend fun saveRawConfigFile(serverId: String, fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val file = File(serverDir, fileName)
            file.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving raw config file $fileName in $serverId", e)
            false
        }
    }

    suspend fun listEditableConfigFiles(serverId: String): List<String> = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        if (!serverDir.exists()) return@withContext listOf("server.properties")
        val validExtensions = setOf("properties", "txt", "json", "yml", "yaml", "toml", "cfg", "conf")
        val files = serverDir.listFiles()?.filter { file ->
            file.isFile && validExtensions.contains(file.extension.lowercase()) && !file.name.endsWith(".jar") && !file.name.endsWith(".log") && !file.name.endsWith(".gz")
        }?.map { it.name }?.sorted() ?: emptyList()

        if (!files.contains("server.properties")) {
            listOf("server.properties") + files
        } else {
            listOf("server.properties") + files.filter { it != "server.properties" }
        }
    }

    suspend fun listDirectory(serverId: String, relativePath: String = ""): List<FileEntry> = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        val targetDir = if (relativePath.isBlank()) serverDir else File(serverDir, relativePath)
        if (!targetDir.exists() || !targetDir.isDirectory) return@withContext emptyList()

        val editableExts = setOf("properties", "txt", "json", "yml", "yaml", "toml", "cfg", "conf", "sh", "bat", "log", "mcmeta", "lang", "csv", "xml", "ini", "sk")
        val logExts = setOf("log", "txt")
        val archiveExts = setOf("zip", "tar", "gz", "tgz", "jar", "7z")
        val worldExts = setOf("mca", "mcr", "dat", "dat_old")

        val files = targetDir.listFiles() ?: return@withContext emptyList()
        val entries = files.map { file ->
            val relPath = if (relativePath.isBlank()) file.name else "$relativePath/${file.name}"
            val ext = file.extension.lowercase()
            FileEntry(
                name = file.name,
                relativePath = relPath,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isDirectory) 0L else file.length(),
                lastModified = file.lastModified(),
                extension = ext,
                isEditable = !file.isDirectory && editableExts.contains(ext),
                isLog = !file.isDirectory && (logExts.contains(ext) || file.name.contains("log")),
                isArchive = !file.isDirectory && archiveExts.contains(ext),
                isWorldRegion = !file.isDirectory && worldExts.contains(ext)
            )
        }

        // Sort: Directories first, then files alphabetically
        entries.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun createFile(serverId: String, relativePath: String, fileName: String, content: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val parentDir = if (relativePath.isBlank()) serverDir else File(serverDir, relativePath)
            if (!parentDir.exists()) parentDir.mkdirs()
            val newFile = File(parentDir, fileName)
            if (newFile.exists()) return@withContext false
            newFile.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating file $fileName in $serverId/$relativePath", e)
            false
        }
    }

    suspend fun createDirectory(serverId: String, relativePath: String, dirName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val parentDir = if (relativePath.isBlank()) serverDir else File(serverDir, relativePath)
            val newDir = File(parentDir, dirName)
            newDir.mkdirs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating directory $dirName in $serverId/$relativePath", e)
            false
        }
    }

    suspend fun deleteFileOrDirectory(serverId: String, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val target = File(serverDir, relativePath)
            if (!target.exists()) return@withContext false
            if (target.isDirectory) {
                target.deleteRecursively()
            } else {
                target.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting $relativePath in $serverId", e)
            false
        }
    }

    suspend fun renameFileOrDirectory(serverId: String, relativePath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val target = File(serverDir, relativePath)
            if (!target.exists()) return@withContext false
            val destination = File(target.parentFile, newName)
            target.renameTo(destination)
        } catch (e: Exception) {
            Log.e(TAG, "Failed renaming $relativePath to $newName in $serverId", e)
            false
        }
    }

    suspend fun duplicateFile(serverId: String, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val source = File(serverDir, relativePath)
            if (!source.exists() || source.isDirectory) return@withContext false
            val parent = source.parentFile ?: serverDir
            val nameWithoutExt = source.nameWithoutExtension
            val ext = if (source.extension.isNotBlank()) ".${source.extension}" else ""
            var copyIndex = 1
            var destFile = File(parent, "${nameWithoutExt}_copy$ext")
            while (destFile.exists()) {
                copyIndex++
                destFile = File(parent, "${nameWithoutExt}_copy$copyIndex$ext")
            }
            source.copyTo(destFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed duplicating $relativePath in $serverId", e)
            false
        }
    }

    suspend fun readFile(serverId: String, relativePath: String): String = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val target = File(serverDir, relativePath)
            if (target.exists() && target.isFile) target.readText() else ""
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading $relativePath in $serverId", e)
            ""
        }
    }

    suspend fun writeFile(serverId: String, relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val target = File(serverDir, relativePath)
            target.parentFile?.mkdirs()
            target.writeText(content)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing $relativePath in $serverId", e)
            false
        }
    }

    suspend fun importFile(serverId: String, relativePath: String, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val targetDir = if (relativePath.isBlank()) serverDir else File(serverDir, relativePath)
            if (!targetDir.exists()) targetDir.mkdirs()

            var fileName = "imported_file"
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val rawName = cursor.getString(nameIndex)
                        fileName = File(rawName).name.ifBlank { "imported_file" }
                    }
                }
            }

            val destFile = File(targetDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.exists() && destFile.length() > 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed importing file from URI to $serverId/$relativePath", e)
            false
        }
    }

    suspend fun exportFileToDownloads(serverId: String, relativePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val sourceFile = File(serverDir, relativePath)
            if (!sourceFile.exists() || sourceFile.isDirectory) return@withContext false

            val fileName = sourceFile.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MineServe")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                true
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFolder = File(downloadsDir, "MineServe").apply { if (!exists()) mkdirs() }
                val destFile = File(targetFolder, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed exporting $relativePath to downloads", e)
            false
        }
    }

    suspend fun searchFiles(serverId: String, query: String): List<FileEntry> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val serverDir = File(serversDir, serverId)
        if (!serverDir.exists()) return@withContext emptyList()

        val editableExts = setOf("properties", "txt", "json", "yml", "yaml", "toml", "cfg", "conf", "sh", "bat", "log", "mcmeta", "lang", "csv", "xml", "ini", "sk")
        val logExts = setOf("log", "txt")
        val archiveExts = setOf("zip", "tar", "gz", "tgz", "jar", "7z")
        val worldExts = setOf("mca", "mcr", "dat", "dat_old")
        val q = query.lowercase()

        val results = mutableListOf<FileEntry>()
        fun searchDir(dir: File, currentRelPath: String) {
            val files = dir.listFiles() ?: return
            for (f in files) {
                val rel = if (currentRelPath.isBlank()) f.name else "$currentRelPath/${f.name}"
                if (f.name.lowercase().contains(q)) {
                    val ext = f.extension.lowercase()
                    results.add(
                        FileEntry(
                            name = f.name,
                            relativePath = rel,
                            isDirectory = f.isDirectory,
                            sizeBytes = if (f.isDirectory) 0L else f.length(),
                            lastModified = f.lastModified(),
                            extension = ext,
                            isEditable = !f.isDirectory && editableExts.contains(ext),
                            isLog = !f.isDirectory && (logExts.contains(ext) || f.name.contains("log")),
                            isArchive = !f.isDirectory && archiveExts.contains(ext),
                            isWorldRegion = !f.isDirectory && worldExts.contains(ext)
                        )
                    )
                }
                if (f.isDirectory && results.size < 100) {
                    searchDir(f, rel)
                }
            }
        }
        searchDir(serverDir, "")
        results.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun getServerDirectory(serverId: String): File {
        return File(serversDir, serverId).apply { if (!exists()) mkdirs() }
    }

    suspend fun getServerStorageBytes(serverId: String): Long = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        if (!serverDir.exists()) return@withContext 0L
        calculateFolderSize(serverDir)
    }

    private fun calculateFolderSize(file: File): Long {
        var size = 0L
        val files = file.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) calculateFolderSize(f) else f.length()
        }
        return size
    }
}
