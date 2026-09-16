package com.devwithzachary.mineserve.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.devwithzachary.mineserve.model.BackupEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
