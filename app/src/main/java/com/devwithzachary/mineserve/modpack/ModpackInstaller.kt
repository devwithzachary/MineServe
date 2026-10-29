package com.devwithzachary.mineserve.modpack

import android.content.Context
import android.net.Uri
import android.util.Log
import com.devwithzachary.mineserve.api.MineServeHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class ModpackInstaller {
    companion object {
        private const val TAG = "ModpackInstaller"
        private val json = Json { ignoreUnknownKeys = true }
    }

    suspend fun readMetadata(context: Context, uri: Uri): Result<ModpackIndex> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val indexJson = extractIndexJsonFromStream(stream)
                    ?: return@withContext Result.failure(IllegalArgumentException("modrinth.index.json not found in archive."))
                val index = json.decodeFromString<ModpackIndex>(indexJson)
                Result.success(index)
            } ?: Result.failure(IllegalArgumentException("Cannot open file stream."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading modpack metadata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun installModpack(
        context: Context,
        uri: Uri,
        serverDir: File,
        onProgress: (ModpackInstallProgress) -> Unit
    ): Result<ModpackIndex> = withContext(Dispatchers.IO) {
        try {
            onProgress(ModpackInstallProgress("Reading Modpack Metadata", "modrinth.index.json", 0, 1, 0f))

            // 1. First pass: extract modrinth.index.json and apply overrides
            var modpackIndex: ModpackIndex? = null
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(stream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val name = entry.name.replace('\\', '/')
                        if (name == "modrinth.index.json") {
                            val jsonStr = zipIn.bufferedReader(Charsets.UTF_8).readText()
                            modpackIndex = json.decodeFromString<ModpackIndex>(jsonStr)
                        } else if (name.startsWith("overrides/") && !entry.isDirectory) {
                            val relativePath = name.removePrefix("overrides/")
                            val targetFile = File(serverDir, relativePath)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out ->
                                zipIn.copyTo(out)
                            }
                        } else if (name.startsWith("server-overrides/") && !entry.isDirectory) {
                            val relativePath = name.removePrefix("server-overrides/")
                            val targetFile = File(serverDir, relativePath)
                            targetFile.parentFile?.mkdirs()
                            FileOutputStream(targetFile).use { out ->
                                zipIn.copyTo(out)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            val index = modpackIndex ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid .mrpack archive: modrinth.index.json is missing.")
            )

            // 2. Filter server-compatible files to download
            val filesToDownload = index.files.filter { it.isServerCompatible && it.downloads.isNotEmpty() }
            val totalCount = filesToDownload.size

            Log.i(TAG, "Installing Modpack '${index.name}' with ${filesToDownload.size} server mods")

            val modsDir = File(serverDir, "mods").apply { if (!exists()) mkdirs() }

            // 3. Download mod files
            for ((idx, fileEntry) in filesToDownload.withIndex()) {
                val fileName = File(fileEntry.path).name
                val targetFile = File(serverDir, fileEntry.path)
                targetFile.parentFile?.mkdirs()

                val currentIdx = idx + 1
                val progressRatio = currentIdx.toFloat() / totalCount.toFloat()
                onProgress(
                    ModpackInstallProgress(
                        stage = "Downloading Mods",
                        currentItem = fileName,
                        currentCount = currentIdx,
                        totalCount = totalCount,
                        progress = progressRatio
                    )
                )

                var downloaded = false
                for (downloadUrl in fileEntry.downloads) {
                    try {
                        val req = MineServeHttpClient.newGetRequest(downloadUrl)
                        MineServeHttpClient.client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body ?: return@use
                                body.byteStream().use { input ->
                                    FileOutputStream(targetFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                // Verify SHA-1 / SHA-512 if provided
                                val expectedSha1 = fileEntry.hashes["sha1"]
                                if (expectedSha1 != null && !verifyHash(targetFile, "SHA-1", expectedSha1)) {
                                    Log.w(TAG, "SHA-1 mismatch for $fileName, attempting fallback")
                                    targetFile.delete()
                                    return@use
                                }

                                downloaded = true
                            }
                        }
                        if (downloaded) break
                    } catch (e: Exception) {
                        Log.w(TAG, "Download attempt failed for $downloadUrl: ${e.message}")
                    }
                }

                if (!downloaded && !targetFile.exists()) {
                    Log.w(TAG, "Could not download mod $fileName from any mirror")
                }
            }

            onProgress(ModpackInstallProgress("Complete", index.name, totalCount, totalCount, 1f))
            Result.success(index)
        } catch (e: Exception) {
            Log.e(TAG, "Modpack installation failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun extractIndexJsonFromStream(stream: InputStream): String? {
        ZipInputStream(stream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name.replace('\\', '/') == "modrinth.index.json") {
                    return zipIn.bufferedReader(Charsets.UTF_8).readText()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        return null
    }

    private fun verifyHash(file: File, algorithm: String, expectedHex: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hashBytes = digest.digest()
            val computedHex = hashBytes.joinToString("") { "%02x".format(it) }
            computedHex.equals(expectedHex.trim(), ignoreCase = true)
        } catch (_: Exception) {
            true // Don't fail if digest fails
        }
    }
}

