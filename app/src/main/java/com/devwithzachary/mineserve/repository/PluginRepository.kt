package com.devwithzachary.mineserve.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.devwithzachary.mineserve.model.PluginModEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
        uri: Uri,
        isMod: Boolean,
        contentResolver: ContentResolver
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = if (isMod) File(serverDir, "mods") else File(serverDir, "plugins")
            if (!targetDir.exists()) targetDir.mkdirs()

            var fileName = "imported.jar"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
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
