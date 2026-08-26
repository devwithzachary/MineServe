package com.devwithzachary.mineserve.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
                        list.add(server.copy(status = ServerStatus.STOPPED))
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
        jarFileName: String = "server.jar"
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
            javaVersion = com.devwithzachary.mineserve.model.determineJavaVersion(version, type),
            status = ServerStatus.STOPPED,
            motd = motd,
            jarFileName = jarFileName
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

class BackupRepository(private val context: Context) {
    companion object {
        private const val TAG = "BackupRepository"
    }

    suspend fun listBackups(serverDir: File): List<BackupEntry> = withContext(Dispatchers.IO) {
        val backupsDir = File(serverDir, "backups").apply { if (!exists()) mkdirs() }
        val list = mutableListOf<BackupEntry>()

        backupsDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".zip") || file.name.endsWith(".tar.gz"))) {
                val isWorld = file.name.contains("world_backup")
                list.add(
                    BackupEntry(
                        id = file.nameWithoutExtension,
                        serverId = serverDir.name,
                        name = file.name,
                        fileName = file.name,
                        sizeBytes = file.length(),
                        timestamp = file.lastModified(),
                        isWorldOnly = isWorld
                    )
                )
            }
        }
        list.sortedByDescending { it.timestamp }
    }

    suspend fun createBackup(
        serverDir: File,
        isWorldOnly: Boolean = true,
        customName: String? = null
    ): BackupEntry? = withContext(Dispatchers.IO) {
        try {
            val backupsDir = File(serverDir, "backups").apply { if (!exists()) mkdirs() }
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
            val prefix = if (isWorldOnly) "world_backup" else "full_server_backup"
            val fileName = customName?.let { "$it.zip" } ?: "${prefix}_$timeStamp.zip"
            val destZip = File(backupsDir, fileName)

            val sourceDir = if (isWorldOnly) File(serverDir, "world") else serverDir
            if (!sourceDir.exists()) return@withContext null

            ZipOutputStream(FileOutputStream(destZip)).use { zipOut ->
                zipFileOrDirectory(sourceDir, sourceDir.name, zipOut, excludeBackups = !isWorldOnly)
            }

            BackupEntry(
                id = destZip.nameWithoutExtension,
                serverId = serverDir.name,
                name = destZip.name,
                fileName = destZip.name,
                sizeBytes = destZip.length(),
                timestamp = destZip.lastModified(),
                isWorldOnly = isWorldOnly
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating backup", e)
            null
        }
    }

    suspend fun restoreBackup(serverDir: File, backupFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) return@withContext false
            val isWorld = backupFile.name.contains("world_backup")
            val targetDir = if (isWorld) File(serverDir, "world") else serverDir
            if (isWorld && targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            targetDir.mkdirs()

            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val rawName = entry.name.replace('\\', '/')
                    val entryName = if (isWorld) {
                        rawName.removePrefix("world/").removePrefix("/")
                    } else {
                        rawName.removePrefix("/")
                    }
                    if (entryName.isNotEmpty()) {
                        val newFile = File(targetDir, entryName)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { out ->
                                zipIn.copyTo(out)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed restoring backup", e)
            false
        }
    }

    suspend fun exportBackupToDownloads(backupFile: File): String? = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) return@withContext null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, backupFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MineServe")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext null

                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(backupFile).use { input ->
                        input.copyTo(out)
                    }
                }
                "Downloads/MineServe/${backupFile.name}"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val mineServeDir = File(downloadsDir, "MineServe").apply { if (!exists()) mkdirs() }
                val dest = File(mineServeDir, backupFile.name)
                backupFile.copyTo(dest, overwrite = true)
                dest.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed exporting backup", e)
            null
        }
    }

    fun getShareIntent(backupFile: File): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, backupFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating share intent for backup", e)
            null
        }
    }

    private fun zipFileOrDirectory(
        fileToZip: File,
        fileName: String,
        zipOut: ZipOutputStream,
        excludeBackups: Boolean = false
    ) {
        if (excludeBackups && fileToZip.name == "backups") return
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles() ?: return
            for (child in children) {
                zipFileOrDirectory(child, "$fileName/${child.name}", zipOut, excludeBackups)
            }
            return
        }
        FileInputStream(fileToZip).use { fis ->
            val zipEntry = ZipEntry(fileName)
            zipOut.putNextEntry(zipEntry)
            fis.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
}

class PluginRepository {
    companion object {
        private const val TAG = "PluginRepository"
    }

    suspend fun listPluginsAndMods(serverDir: File): List<PluginModEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<PluginModEntry>()
        val pluginsDir = File(serverDir, "plugins").apply { if (!exists()) mkdirs() }
        val modsDir = File(serverDir, "mods").apply { if (!exists()) mkdirs() }

        pluginsDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".jar") || file.name.endsWith(".jar.disabled")) {
                val isEnabled = file.name.endsWith(".jar")
                val cleanName = file.name.removeSuffix(".disabled").removeSuffix(".jar")
                list.add(
                    PluginModEntry(
                        id = file.name,
                        fileName = file.name,
                        name = cleanName.replace("-", " ").replaceFirstChar { it.uppercase() },
                        enabled = isEnabled,
                        fileSizeBytes = file.length(),
                        isMod = false
                    )
                )
            }
        }

        modsDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".jar") || file.name.endsWith(".jar.disabled")) {
                val isEnabled = file.name.endsWith(".jar")
                val cleanName = file.name.removeSuffix(".disabled").removeSuffix(".jar")
                list.add(
                    PluginModEntry(
                        id = file.name,
                        fileName = file.name,
                        name = cleanName.replace("-", " ").replaceFirstChar { it.uppercase() },
                        enabled = isEnabled,
                        fileSizeBytes = file.length(),
                        isMod = true
                    )
                )
            }
        }

        list.sortedBy { it.name }
    }

    suspend fun importJarFromUri(
        serverDir: File,
        uri: android.net.Uri,
        isMod: Boolean,
        contentResolver: android.content.ContentResolver
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (isMod) File(serverDir, "mods") else File(serverDir, "plugins")
            if (!targetDir.exists()) targetDir.mkdirs()

            var fileName = "imported.jar"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = it.getString(nameIndex) ?: "imported.jar"
                    }
                }
            }

            if (!fileName.endsWith(".jar")) {
                fileName = "$fileName.jar"
            }

            val destFile = File(targetDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Imported JAR successfully to ${destFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import JAR from URI", e)
            false
        }
    }

    suspend fun togglePlugin(serverDir: File, entry: PluginModEntry): Boolean = withContext(Dispatchers.IO) {
        val folder = if (entry.isMod) File(serverDir, "mods") else File(serverDir, "plugins")
        val currentFile = File(folder, entry.fileName)
        if (!currentFile.exists()) return@withContext false

        val newFile = if (entry.enabled) {
            File(folder, entry.fileName + ".disabled")
        } else {
            File(folder, entry.fileName.removeSuffix(".disabled"))
        }

        currentFile.renameTo(newFile)
    }

    suspend fun deletePlugin(serverDir: File, entry: PluginModEntry): Boolean = withContext(Dispatchers.IO) {
        val folder = if (entry.isMod) File(serverDir, "mods") else File(serverDir, "plugins")
        val file = File(folder, entry.fileName)
        if (file.exists()) file.delete() else false
    }
}
