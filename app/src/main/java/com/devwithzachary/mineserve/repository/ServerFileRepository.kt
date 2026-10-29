package com.devwithzachary.mineserve.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.util.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ServerFileRepository(
    private val context: Context,
    private val serversDir: File
) {
    companion object {
        private const val TAG = "ServerFileRepository"
        private val EDITABLE_EXTS = setOf("properties", "txt", "json", "yml", "yaml", "toml", "cfg", "conf", "sh", "bat", "log", "mcmeta", "lang", "csv", "xml", "ini", "sk")
        private val LOG_EXTS = setOf("log", "txt")
        private val ARCHIVE_EXTS = setOf("zip", "tar", "gz", "tgz", "jar", "7z")
        private val WORLD_EXTS = setOf("mca", "mcr", "dat", "dat_old")
    }

    private fun toFileEntry(file: File, relativePath: String): FileEntry {
        val ext = file.extension.lowercase()
        val isDir = file.isDirectory
        return FileEntry(
            name = file.name,
            relativePath = relativePath,
            isDirectory = isDir,
            sizeBytes = if (isDir) 0L else file.length(),
            lastModified = file.lastModified(),
            extension = ext,
            isEditable = !isDir && EDITABLE_EXTS.contains(ext),
            isLog = !isDir && (LOG_EXTS.contains(ext) || file.name.contains("log")),
            isArchive = !isDir && ARCHIVE_EXTS.contains(ext),
            isWorldRegion = !isDir && WORLD_EXTS.contains(ext)
        )
    }

    suspend fun listDirectory(serverId: String, relativePath: String = ""): List<FileEntry> = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        val targetDir = if (relativePath.isBlank()) serverDir else File(serverDir, relativePath)
        if (!targetDir.exists() || !targetDir.isDirectory) return@withContext emptyList()

        val files = targetDir.listFiles() ?: return@withContext emptyList()
        val entries = files.map { file ->
            val relPath = if (relativePath.isBlank()) file.name else "$relativePath/${file.name}"
            toFileEntry(file, relPath)
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
        val serverDir = File(serversDir, serverId)
        val sourceFile = File(serverDir, relativePath)
        FileStorageUtils.exportFileToDownloads(context, sourceFile)
    }

    suspend fun unzipFile(
        serverId: String,
        relativePath: String,
        destinationRelativePath: String = "",
        deleteZipAfter: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        val zipFile = File(serverDir, relativePath)
        val targetDir = if (destinationRelativePath.isBlank()) {
            zipFile.parentFile ?: serverDir
        } else {
            File(serverDir, destinationRelativePath)
        }

        val serverCanonicalPath = serverDir.canonicalPath
        val targetCanonicalPath = targetDir.canonicalPath
        if (!targetCanonicalPath.startsWith(serverCanonicalPath)) {
            return@withContext Result.failure(SecurityException("Destination directory is outside server root"))
        }

        FileStorageUtils.extractZipSafe(zipFile, targetDir, deleteZipAfter)
    }

    suspend fun searchFiles(serverId: String, query: String): List<FileEntry> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val serverDir = File(serversDir, serverId)
        if (!serverDir.exists()) return@withContext emptyList()

        val q = query.lowercase()
        val results = mutableListOf<FileEntry>()

        fun searchDir(dir: File, currentRelPath: String) {
            val files = dir.listFiles() ?: return
            for (f in files) {
                val rel = if (currentRelPath.isBlank()) f.name else "$currentRelPath/${f.name}"
                if (f.name.lowercase().contains(q)) {
                    results.add(toFileEntry(f, rel))
                }
                if (f.isDirectory && results.size < 100) {
                    searchDir(f, rel)
                }
            }
        }

        searchDir(serverDir, "")
        results.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun getServerStorageBytes(serverId: String): Long = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        FileStorageUtils.calculateDirectorySize(serverDir)
    }
}

