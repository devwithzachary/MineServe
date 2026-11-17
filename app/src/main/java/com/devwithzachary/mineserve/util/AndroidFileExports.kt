package com.devwithzachary.mineserve.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream

fun FileStorageUtils.exportFileToDownloads(
    context: Context,
    sourceFile: File,
    mimeType: String = "application/octet-stream",
    subFolder: String = "MineServe"
): Boolean {
    if (!sourceFile.exists() || sourceFile.isDirectory) return false
    val fileName = sourceFile.name

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$subFolder")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            true
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetFolder = File(downloadsDir, subFolder).apply { if (!exists()) mkdirs() }
            val destFile = File(targetFolder, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            true
        }
    } catch (e: Exception) {
        Log.e("FileStorageUtils", "Failed exporting $fileName to downloads", e)
        false
    }
}

