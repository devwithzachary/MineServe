package com.devwithzachary.mineserve.engine

import android.content.Context
import android.net.Uri
import android.util.Log
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.WorldDimensionInfo
import com.devwithzachary.mineserve.model.WorldSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * High-level World Manager for Minecraft Servers.
 * Handles world summaries, .zip/.mcworld imports, world exports, and dimension resets.
 */
object WorldManager {

    private const val TAG = "WorldManager"

    /**
     * Calculates storage footprint and dimension details for a server.
     */
    suspend fun getWorldSummary(serverDir: File, levelName: String = "world"): WorldSummary = withContext(Dispatchers.IO) {
        val dimensions = mutableListOf<WorldDimensionInfo>()
        var totalBytes = 0L

        for (dim in DimensionType.values()) {
            val dir = dim.getDimensionDir(serverDir, levelName)
            val exists = dir.exists() && dir.isDirectory
            var sizeBytes = 0L
            var regionCount = 0
            var chunkCount = 0

            if (exists) {
                sizeBytes = calculateDirSize(dir)
                totalBytes += sizeBytes

                val regionDir = when {
                    File(dir, "region").exists() -> File(dir, "region")
                    File(dir, "DIM-1/region").exists() -> File(dir, "DIM-1/region")
                    File(dir, "DIM1/region").exists() -> File(dir, "DIM1/region")
                    else -> null
                }

                if (regionDir != null && regionDir.exists()) {
                    val mcaFiles = regionDir.listFiles { f -> f.isFile && f.name.endsWith(".mca") } ?: emptyArray()
                    regionCount = mcaFiles.size
                    // Estimate chunks from location tables
                    chunkCount = countChunksInRegions(mcaFiles)
                }
            }

            dimensions.add(
                WorldDimensionInfo(
                    dimension = dim,
                    directory = dir,
                    exists = exists,
                    sizeBytes = sizeBytes,
                    regionFilesCount = regionCount,
                    totalChunksCount = chunkCount
                )
            )
        }

        val overworldDir = DimensionType.OVERWORLD.getDimensionDir(serverDir, levelName)
        val lastModified = overworldDir.lastModified()

        WorldSummary(
            levelName = levelName,
            totalSizeBytes = totalBytes,
            dimensions = dimensions,
            lastModifiedTimestamp = lastModified
        )
    }

    /**
     * Imports a Minecraft world from a .zip or .mcworld URI into the server.
     */
    suspend fun importWorld(
        serverDir: File,
        uri: Uri,
        context: Context,
        levelName: String = "world",
        onProgress: (status: String, percent: Int) -> Unit = { _, _ -> }
    ): Result<String> = withContext(Dispatchers.IO) {
        var tempZipFile: File? = null
        try {
            onProgress("Reading world archive...", 10)
            val tempDir = File(serverDir, "temp_import").apply { mkdirs() }
            tempZipFile = File(tempDir, "uploaded_world.zip")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Could not open world file stream"))

            onProgress("Analyzing world archive contents...", 25)

            // Inspect archive entries
            var hasLevelDat = false
            var levelDatPath: String? = null
            var hasBedrockDb = false
            var hasJavaRegion = false

            ZipInputStream(BufferedInputStream(FileInputStream(tempZipFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name.replace('\\', '/')
                    if (name.endsWith("level.dat")) {
                        hasLevelDat = true
                        if (levelDatPath == null) {
                            levelDatPath = name
                        }
                    }
                    if (name.contains("/db/") || name.startsWith("db/")) {
                        hasBedrockDb = true
                    }
                    if (name.contains("/region/") || name.startsWith("region/")) {
                        hasJavaRegion = true
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // Bedrock detection: if has db/ but no region/, it's Bedrock
            if (hasBedrockDb && !hasJavaRegion) {
                return@withContext Result.failure(
                    Exception(
                        "This archive is in Minecraft Bedrock (LevelDB) format. MineServe runs Java Edition servers which require Anvil (.mca) region files. You can convert it using Chunker (chunker.app) or similar tools before importing."
                    )
                )
            }

            if (!hasLevelDat && !hasJavaRegion) {
                return@withContext Result.failure(
                    Exception("The selected archive does not appear to be a valid Minecraft world (missing level.dat or region directory).")
                )
            }

            // Determine root prefix inside zip
            val rootPrefix = if (levelDatPath != null && levelDatPath.contains("/")) {
                levelDatPath.substringBeforeLast("/") + "/"
            } else {
                ""
            }

            onProgress("Clearing existing world data...", 40)
            val targetWorldDir = DimensionType.OVERWORLD.getDimensionDir(serverDir, levelName)
            if (targetWorldDir.exists()) {
                targetWorldDir.deleteRecursively()
            }
            targetWorldDir.mkdirs()

            // Also clean Nether and End if replacing world
            val netherDir = DimensionType.NETHER.getDimensionDir(serverDir, levelName)
            if (netherDir.exists() && netherDir != targetWorldDir) {
                netherDir.deleteRecursively()
            }
            val endDir = DimensionType.THE_END.getDimensionDir(serverDir, levelName)
            if (endDir.exists() && endDir != targetWorldDir) {
                endDir.deleteRecursively()
            }

            onProgress("Extracting world files...", 60)

            var extractedCount = 0
            ZipInputStream(BufferedInputStream(FileInputStream(tempZipFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val rawName = entry.name.replace('\\', '/')

                    // Check if entry belongs to the world
                    if (rootPrefix.isEmpty() || rawName.startsWith(rootPrefix)) {
                        val relPath = if (rootPrefix.isNotEmpty()) rawName.removePrefix(rootPrefix) else rawName
                        if (relPath.isNotEmpty()) {
                            val destFile = File(targetWorldDir, relPath)
                            if (entry.isDirectory) {
                                destFile.mkdirs()
                            } else {
                                destFile.parentFile?.mkdirs()
                                FileOutputStream(destFile).use { out ->
                                    zipIn.copyTo(out)
                                }
                                extractedCount++
                            }
                        }
                    } else if (rawName.startsWith("world_nether/") || rawName.contains("_nether/")) {
                        val netherDest = File(serverDir, "world_nether")
                        val cleanRel = rawName.substringAfter("/")
                        if (cleanRel.isNotEmpty()) {
                            val dest = File(netherDest, cleanRel)
                            if (entry.isDirectory) dest.mkdirs() else {
                                dest.parentFile?.mkdirs()
                                FileOutputStream(dest).use { zipIn.copyTo(it) }
                            }
                        }
                    } else if (rawName.startsWith("world_the_end/") || rawName.contains("_the_end/")) {
                        val endDest = File(serverDir, "world_the_end")
                        val cleanRel = rawName.substringAfter("/")
                        if (cleanRel.isNotEmpty()) {
                            val dest = File(endDest, cleanRel)
                            if (entry.isDirectory) dest.mkdirs() else {
                                dest.parentFile?.mkdirs()
                                FileOutputStream(dest).use { zipIn.copyTo(it) }
                            }
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            onProgress("World imported successfully! ($extractedCount files)", 100)
            Result.success("World imported successfully ($extractedCount files extracted).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed importing world", e)
            Result.failure(e)
        } finally {
            tempZipFile?.delete()
            File(serverDir, "temp_import").deleteRecursively()
        }
    }

    /**
     * Exports the server's world (Overworld, Nether, End) into an output stream.
     */
    suspend fun exportWorld(
        serverDir: File,
        outputStream: OutputStream,
        levelName: String = "world",
        onProgress: (status: String, percent: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress("Collecting world files...", 10)
            val overworldDir = DimensionType.OVERWORLD.getDimensionDir(serverDir, levelName)
            val netherDir = DimensionType.NETHER.getDimensionDir(serverDir, levelName)
            val endDir = DimensionType.THE_END.getDimensionDir(serverDir, levelName)

            val dirsToExport = mutableListOf<Pair<File, String>>()
            if (overworldDir.exists()) dirsToExport.add(overworldDir to "world")
            if (netherDir.exists() && netherDir != overworldDir && !netherDir.startsWith(overworldDir)) {
                dirsToExport.add(netherDir to "world_nether")
            }
            if (endDir.exists() && endDir != overworldDir && !endDir.startsWith(overworldDir)) {
                dirsToExport.add(endDir to "world_the_end")
            }

            onProgress("Compressing world archive...", 30)
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                for ((dir, entryName) in dirsToExport) {
                    zipDirectory(dir, entryName, zipOut)
                }
            }

            onProgress("World export complete!", 100)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export world", e)
            false
        }
    }

    /**
     * 1-tap reset of Nether or The End dimension.
     * Wipes region and entity files so Minecraft regenerates fresh terrain.
     */
    suspend fun resetDimension(
        serverDir: File,
        dimension: DimensionType,
        levelName: String = "world"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (dimension == DimensionType.OVERWORLD) {
                Log.w(TAG, "Overworld reset not allowed via dimension reset.")
                return@withContext false
            }

            var deletedAny = false

            when (dimension) {
                DimensionType.NETHER -> {
                    // Paper/Purpur layout
                    val paperNether = File(serverDir, "${levelName}_nether")
                    if (paperNether.exists()) {
                        paperNether.deleteRecursively()
                        deletedAny = true
                    }
                    val defaultNether = File(serverDir, "world_nether")
                    if (defaultNether.exists()) {
                        defaultNether.deleteRecursively()
                        deletedAny = true
                    }

                    // Vanilla/Fabric layout
                    val overworldDir = DimensionType.OVERWORLD.getDimensionDir(serverDir, levelName)
                    val vanillaNether = File(overworldDir, "DIM-1")
                    if (vanillaNether.exists()) {
                        vanillaNether.deleteRecursively()
                        deletedAny = true
                    }
                }
                DimensionType.THE_END -> {
                    // Paper/Purpur layout
                    val paperEnd = File(serverDir, "${levelName}_the_end")
                    if (paperEnd.exists()) {
                        paperEnd.deleteRecursively()
                        deletedAny = true
                    }
                    val defaultEnd = File(serverDir, "world_the_end")
                    if (defaultEnd.exists()) {
                        defaultEnd.deleteRecursively()
                        deletedAny = true
                    }

                    // Vanilla/Fabric layout
                    val overworldDir = DimensionType.OVERWORLD.getDimensionDir(serverDir, levelName)
                    val vanillaEnd = File(overworldDir, "DIM1")
                    if (vanillaEnd.exists()) {
                        vanillaEnd.deleteRecursively()
                        deletedAny = true
                    }
                }
                else -> {}
            }

            deletedAny
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting dimension ${dimension.name}", e)
            false
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    private fun countChunksInRegions(mcaFiles: Array<File>): Int {
        var count = 0
        val buffer = ByteArray(4096)
        for (file in mcaFiles) {
            try {
                if (file.length() >= 4096) {
                    FileInputStream(file).use { fis ->
                        val read = fis.read(buffer)
                        if (read == 4096) {
                            for (i in 0 until 1024) {
                                val off = ((buffer[i * 4].toInt() and 0xFF) shl 16) or
                                        ((buffer[i * 4 + 1].toInt() and 0xFF) shl 8) or
                                        (buffer[i * 4 + 2].toInt() and 0xFF)
                                if (off != 0) {
                                    count++
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore corrupt individual region header
            }
        }
        return count
    }

    private fun zipDirectory(dir: File, baseName: String, zipOut: ZipOutputStream) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            val entryPath = "$baseName/${file.name}"
            if (file.isDirectory) {
                zipOut.putNextEntry(ZipEntry("$entryPath/"))
                zipOut.closeEntry()
                zipDirectory(file, entryPath, zipOut)
            } else {
                zipOut.putNextEntry(ZipEntry(entryPath))
                FileInputStream(file).use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }
}
