package com.devwithzachary.mineserve.backup.cloud

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class SafBackupStorage(private val context: Context) {
    companion object {
        private const val TAG = "SafBackupStorage"
    }

    suspend fun saveBackupToSaf(
        backupFile: File,
        treeUriString: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val treeUri = Uri.parse(treeUriString)
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                ?: return@withContext Result.failure(IOException("Invalid storage folder URI."))

            if (!rootDoc.canWrite()) {
                return@withContext Result.failure(IOException("No write permission for the selected storage folder."))
            }

            // Check if existing file exists and delete it to replace
            rootDoc.findFile(backupFile.name)?.delete()

            val newFile = rootDoc.createFile("application/zip", backupFile.name)
                ?: return@withContext Result.failure(IOException("Failed creating backup file in destination folder."))

            val totalBytes = backupFile.length()
            val resolver = context.contentResolver

            resolver.openOutputStream(newFile.uri)?.use { output ->
                FileInputStream(backupFile).use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var copiedBytes = 0L
                    var lastReportedPct = -1

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        copiedBytes += read
                        if (totalBytes > 0) {
                            val pct = ((copiedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            if (pct != lastReportedPct) {
                                lastReportedPct = pct
                                onProgress?.invoke(pct)
                            }
                        }
                    }
                }
            } ?: return@withContext Result.failure(IOException("Could not open destination output stream."))

            Log.i(TAG, "Successfully synced ${backupFile.name} to SAF folder ${rootDoc.name}")
            Result.success("Backup successfully saved to ${rootDoc.name ?: "cloud folder"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing backup to SAF folder: ${e.message}")
            Result.failure(e)
        }
    }
}

