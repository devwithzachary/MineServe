package com.devwithzachary.mineserve.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileStorageUtils {

    private const val TAG = "FileStorageUtils"

    fun formatBytes(bytes: Long, includeKb: Boolean = true, gbDecimals: Int = 2): String {
        if (bytes <= 0L) return "0 B"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.${gbDecimals}f GB", mb / 1024.0)
        } else if (mb >= 0.1 || !includeKb) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024.0
            String.format(Locale.US, "%.1f KB", kb)
        }
    }

    fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) calculateDirectorySize(f) else f.length()
        }
        return size
    }

    fun extractZipSafe(
        zipFile: File,
        targetDir: File,
        deleteZipAfter: Boolean = false
    ): Result<Int> {
        if (!zipFile.exists() || !zipFile.isFile) {
            return Result.failure(IllegalArgumentException("Archive file does not exist: ${zipFile.absolutePath}"))
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetCanonicalPath = targetDir.canonicalPath
        var extractedCount = 0

        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile), 65536)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                val buffer = ByteArray(65536)

                while (entry != null) {
                    val outputFile = File(targetDir, entry.name)
                    val outputCanonicalPath = outputFile.canonicalPath

                    // Zip-Slip protection
                    if (!outputCanonicalPath.startsWith(targetCanonicalPath)) {
                        throw SecurityException("Zip entry is outside target directory: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { outStream ->
                            var read: Int
                            while (zipIn.read(buffer).also { read = it } != -1) {
                                outStream.write(buffer, 0, read)
                            }
                        }
                        extractedCount++
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (deleteZipAfter) {
                zipFile.delete()
            }

            Result.success(extractedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed extracting zip: ${zipFile.name}", e)
            Result.failure(e)
        }
    }

    fun zipDirectoryToStream(
        sourceDir: File,
        zipOut: ZipOutputStream,
        basePrefix: String = "",
        excludeFilter: ((File) -> Boolean)? = null
    ) {
        val files = sourceDir.listFiles() ?: return
        val buffer = ByteArray(65536)

        for (file in files) {
            if (excludeFilter != null && excludeFilter(file)) {
                continue
            }

            val entryPath = if (basePrefix.isBlank()) file.name else "$basePrefix/${file.name}"
            if (file.isDirectory) {
                zipOut.putNextEntry(ZipEntry("$entryPath/"))
                zipOut.closeEntry()
                zipDirectoryToStream(file, zipOut, entryPath, excludeFilter)
            } else {
                zipOut.putNextEntry(ZipEntry(entryPath))
                FileInputStream(file).use { input ->
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        zipOut.write(buffer, 0, count)
                    }
                }
                zipOut.closeEntry()
            }
        }
    }

    fun exportFileToDownloads(
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
            Log.e(TAG, "Failed exporting $fileName to downloads", e)
            false
        }
    }
}

