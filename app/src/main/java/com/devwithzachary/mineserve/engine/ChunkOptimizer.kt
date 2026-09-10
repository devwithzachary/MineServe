package com.devwithzachary.mineserve.engine

import android.util.Log
import com.devwithzachary.mineserve.model.ChunkPruneOptions
import com.devwithzachary.mineserve.model.ChunkPruneResult
import com.devwithzachary.mineserve.model.DimensionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.zip.InflaterInputStream
import kotlin.math.ceil

/**
 * Pure Kotlin Minecraft Anvil (.mca) Region Parser and Chunk Optimizer.
 *
 * Inspects chunk headers and parses InhabitedTime NBT tags to identify
 * and prune uninhabited chunks, shrinking world storage footprint on mobile devices.
 */
object ChunkOptimizer {

    private const val TAG = "ChunkOptimizer"
    private const val SECTOR_SIZE = 4096

    /**
     * Optimizes region files in the given server directory according to options.
     */
    suspend fun optimizeWorld(
        serverDir: File,
        options: ChunkPruneOptions,
        levelName: String = "world",
        onProgress: (statusText: String, percent: Int) -> Unit = { _, _ -> }
    ): ChunkPruneResult = withContext(Dispatchers.IO) {
        val regionDirs = mutableListOf<File>()

        // Overworld region dir
        val overworldDir = DimensionType.OVERWORLD.getDimensionDir(serverDir, levelName)
        val overworldRegion = File(overworldDir, "region")
        if (overworldRegion.exists() && overworldRegion.isDirectory) {
            regionDirs.add(overworldRegion)
        }

        // Nether region dir
        if (options.pruneNether) {
            val netherDir = DimensionType.NETHER.getDimensionDir(serverDir, levelName)
            val netherRegion = when {
                File(netherDir, "DIM-1/region").exists() -> File(netherDir, "DIM-1/region")
                File(netherDir, "region").exists() -> File(netherDir, "region")
                else -> null
            }
            if (netherRegion != null && netherRegion.isDirectory) {
                regionDirs.add(netherRegion)
            }
        }

        // End region dir
        if (options.pruneEnd) {
            val endDir = DimensionType.THE_END.getDimensionDir(serverDir, levelName)
            val endRegion = when {
                File(endDir, "DIM1/region").exists() -> File(endDir, "DIM1/region")
                File(endDir, "region").exists() -> File(endDir, "region")
                else -> null
            }
            if (endRegion != null && endRegion.isDirectory) {
                regionDirs.add(endRegion)
            }
        }

        val allMcaFiles = regionDirs.flatMap { dir ->
            dir.listFiles { f -> f.isFile && f.name.endsWith(".mca") }?.toList() ?: emptyList()
        }

        if (allMcaFiles.isEmpty()) {
            return@withContext ChunkPruneResult(0, 0, 0, 0, 0L, 0L)
        }

        var totalBytesBefore = 0L
        allMcaFiles.forEach { totalBytesBefore += it.length() }

        var scannedRegions = 0
        var totalScannedChunks = 0
        var totalPrunedChunks = 0
        var deletedRegionsCount = 0

        val totalFiles = allMcaFiles.size

        for ((index, mcaFile) in allMcaFiles.withIndex()) {
            val percent = ((index.toDouble() / totalFiles) * 100).toInt()
            onProgress("Scanning and optimizing ${mcaFile.name} (${index + 1}/$totalFiles)...", percent)

            try {
                val stats = optimizeSingleRegion(mcaFile, options.threshold.ticks)
                totalScannedChunks += stats.scannedChunks
                totalPrunedChunks += stats.prunedChunks
                if (stats.wasDeleted) {
                    deletedRegionsCount++
                    // Also delete companion entities and poi files if empty
                    cleanupCompanionFiles(mcaFile)
                }
                scannedRegions++
            } catch (e: Exception) {
                Log.w(TAG, "Error optimizing region file: ${mcaFile.name}", e)
            }
        }

        var totalBytesAfter = 0L
        for (dir in regionDirs) {
            dir.listFiles { f -> f.isFile && f.name.endsWith(".mca") }?.forEach {
                totalBytesAfter += it.length()
            }
        }

        onProgress("Chunk optimization complete!", 100)

        ChunkPruneResult(
            scannedRegions = scannedRegions,
            scannedChunks = totalScannedChunks,
            prunedChunks = totalPrunedChunks,
            deletedRegions = deletedRegionsCount,
            bytesBefore = totalBytesBefore,
            bytesAfter = totalBytesAfter
        )
    }

    private data class RegionOptimizeResult(
        val scannedChunks: Int,
        val prunedChunks: Int,
        val wasDeleted: Boolean
    )

    private fun optimizeSingleRegion(mcaFile: File, thresholdTicks: Long): RegionOptimizeResult {
        if (!mcaFile.exists() || mcaFile.length() < 8192) {
            return RegionOptimizeResult(0, 0, false)
        }

        val headerBuffer = ByteArray(8192)
        RandomAccessFile(mcaFile, "r").use { raf ->
            raf.readFully(headerBuffer)

            // Parse 1024 location table entries
            val chunksToKeep = mutableListOf<ChunkData>()
            var scannedCount = 0
            var prunedCount = 0

            for (i in 0 until 1024) {
                val locOffset = i * 4
                val sectorOffset = ((headerBuffer[locOffset].toInt() and 0xFF) shl 16) or
                        ((headerBuffer[locOffset + 1].toInt() and 0xFF) shl 8) or
                        (headerBuffer[locOffset + 2].toInt() and 0xFF)
                val sectorCount = headerBuffer[locOffset + 3].toInt() and 0xFF

                if (sectorOffset == 0 || sectorCount == 0) {
                    continue // Chunk ungenerated
                }

                scannedCount++
                val chunkBytePos = sectorOffset.toLong() * SECTOR_SIZE
                if (chunkBytePos + 5 > raf.length()) {
                    continue // Corrupt or truncated offset
                }

                raf.seek(chunkBytePos)
                val length = raf.readInt()
                val compressionType = raf.readByte()

                if (length <= 1 || chunkBytePos + 4 + length > raf.length()) {
                    continue
                }

                val payload = ByteArray(length - 1)
                raf.readFully(payload)

                val inhabitedTime = extractInhabitedTime(payload, compressionType.toInt())

                if (inhabitedTime <= thresholdTicks) {
                    prunedCount++
                } else {
                    val tsOffset = 4096 + (i * 4)
                    val timestamp = ((headerBuffer[tsOffset].toInt() and 0xFF) shl 24) or
                            ((headerBuffer[tsOffset + 1].toInt() and 0xFF) shl 16) or
                            ((headerBuffer[tsOffset + 2].toInt() and 0xFF) shl 8) or
                            (headerBuffer[tsOffset + 3].toInt() and 0xFF)

                    chunksToKeep.add(
                        ChunkData(
                            index = i,
                            timestamp = timestamp,
                            compressionType = compressionType,
                            payload = payload
                        )
                    )
                }
            }

            // Decision:
            if (chunksToKeep.isEmpty()) {
                // Delete the entire file
                raf.close()
                mcaFile.delete()
                return RegionOptimizeResult(scannedCount, prunedCount, true)
            }

            if (chunksToKeep.size == scannedCount) {
                // No chunks were pruned
                return RegionOptimizeResult(scannedCount, 0, false)
            }

            // Rewrite compacted MCA file
            val tempFile = File(mcaFile.parentFile, "${mcaFile.name}.opt")
            RandomAccessFile(tempFile, "rw").use { outRaf ->
                val newHeader = ByteArray(8192)
                var currentSector = 2

                outRaf.seek(8192)

                for (chunk in chunksToKeep) {
                    val rawLength = chunk.payload.size + 1
                    val neededBytes = rawLength + 4
                    val neededSectors = ceil(neededBytes.toDouble() / SECTOR_SIZE).toInt()

                    // Update location table
                    val locOffset = chunk.index * 4
                    newHeader[locOffset] = ((currentSector shr 16) and 0xFF).toByte()
                    newHeader[locOffset + 1] = ((currentSector shr 8) and 0xFF).toByte()
                    newHeader[locOffset + 2] = (currentSector and 0xFF).toByte()
                    newHeader[locOffset + 3] = (neededSectors and 0xFF).toByte()

                    // Update timestamp table
                    val tsOffset = 4096 + (chunk.index * 4)
                    newHeader[tsOffset] = ((chunk.timestamp shr 24) and 0xFF).toByte()
                    newHeader[tsOffset + 1] = ((chunk.timestamp shr 16) and 0xFF).toByte()
                    newHeader[tsOffset + 2] = ((chunk.timestamp shr 8) and 0xFF).toByte()
                    newHeader[tsOffset + 3] = (chunk.timestamp and 0xFF).toByte()

                    // Write chunk data
                    outRaf.writeInt(rawLength)
                    outRaf.writeByte(chunk.compressionType.toInt())
                    outRaf.write(chunk.payload)

                    // Write padding
                    val writtenSoFar = neededBytes
                    val padding = (neededSectors * SECTOR_SIZE) - writtenSoFar
                    if (padding > 0) {
                        outRaf.write(ByteArray(padding))
                    }

                    currentSector += neededSectors
                }

                // Write final header
                outRaf.seek(0)
                outRaf.write(newHeader)
            }

            // Replace original file atomically
            raf.close()
            if (tempFile.exists()) {
                mcaFile.delete()
                tempFile.renameTo(mcaFile)
            }

            return RegionOptimizeResult(scannedCount, prunedCount, false)
        }
    }

    private data class ChunkData(
        val index: Int,
        val timestamp: Int,
        val compressionType: Byte,
        val payload: ByteArray
    )

    /**
     * Extracts the InhabitedTime (ticks) from a compressed chunk payload.
     * Returns 0L if not found or on decompression error.
     */
    fun extractInhabitedTime(payload: ByteArray, compressionType: Int): Long {
        return try {
            val inStream: InputStream = when (compressionType) {
                1 -> java.util.zip.GZIPInputStream(ByteArrayInputStream(payload))
                2 -> InflaterInputStream(ByteArrayInputStream(payload))
                3 -> ByteArrayInputStream(payload)
                else -> return 0L
            }

            DataInputStream(BufferedInputStream(inStream)).use { dataIn ->
                parseNbtForInhabitedTime(dataIn)
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Lightweight streaming NBT reader that searches specifically for InhabitedTime.
     */
    private fun parseNbtForInhabitedTime(dis: DataInputStream): Long {
        try {
            val rootTag = dis.readByte().toInt()
            if (rootTag != 10) return 0L // Must be TAG_Compound

            // Read root name
            val rootNameLen = dis.readShort().toInt() and 0xFFFF
            if (rootNameLen > 0) {
                dis.skipBytes(rootNameLen)
            }

            return scanCompound(dis, depth = 0)
        } catch (e: Exception) {
            return 0L
        }
    }

    private fun scanCompound(dis: DataInputStream, depth: Int): Long {
        if (depth > 20) return 0L // Recursion safety limit

        while (true) {
            val tagType = dis.readByte().toInt()
            if (tagType == 0) {
                // TAG_End
                return 0L
            }

            val nameLen = dis.readShort().toInt() and 0xFFFF
            val nameBytes = ByteArray(nameLen)
            dis.readFully(nameBytes)
            val name = String(nameBytes, Charsets.UTF_8)

            if (tagType == 4 && name == "InhabitedTime") {
                return dis.readLong()
            }

            // If it's a nested compound (e.g. "Level"), scan inside
            if (tagType == 10) {
                val found = scanCompound(dis, depth + 1)
                if (found > 0L) return found
            } else {
                skipTagPayload(dis, tagType)
            }
        }
    }

    private fun skipTagPayload(dis: DataInputStream, tagType: Int) {
        when (tagType) {
            1 -> dis.skipBytes(1) // TAG_Byte
            2 -> dis.skipBytes(2) // TAG_Short
            3 -> dis.skipBytes(4) // TAG_Int
            4 -> dis.skipBytes(8) // TAG_Long
            5 -> dis.skipBytes(4) // TAG_Float
            6 -> dis.skipBytes(8) // TAG_Double
            7 -> {
                // TAG_Byte_Array
                val len = dis.readInt()
                if (len > 0) dis.skipBytes(len)
            }
            8 -> {
                // TAG_String
                val len = dis.readShort().toInt() and 0xFFFF
                if (len > 0) dis.skipBytes(len)
            }
            9 -> {
                // TAG_List
                val elemType = dis.readByte().toInt()
                val count = dis.readInt()
                if (count > 0) {
                    for (i in 0 until count) {
                        if (elemType == 10) {
                            skipCompoundPayload(dis)
                        } else {
                            skipTagPayload(dis, elemType)
                        }
                    }
                }
            }
            11 -> {
                // TAG_Int_Array
                val len = dis.readInt()
                if (len > 0) dis.skipBytes(len * 4)
            }
            12 -> {
                // TAG_Long_Array
                val len = dis.readInt()
                if (len > 0) dis.skipBytes(len * 8)
            }
        }
    }

    private fun skipCompoundPayload(dis: DataInputStream) {
        while (true) {
            val type = dis.readByte().toInt()
            if (type == 0) break
            val nameLen = dis.readShort().toInt() and 0xFFFF
            dis.skipBytes(nameLen)
            if (type == 10) {
                skipCompoundPayload(dis)
            } else {
                skipTagPayload(dis, type)
            }
        }
    }

    private fun cleanupCompanionFiles(regionFile: File) {
        try {
            val parent = regionFile.parentFile ?: return
            val dimDir = parent.parentFile ?: return
            val fileName = regionFile.name

            val entitiesFile = File(dimDir, "entities/$fileName")
            if (entitiesFile.exists()) entitiesFile.delete()

            val poiFile = File(dimDir, "poi/$fileName")
            if (poiFile.exists()) poiFile.delete()
        } catch (e: Exception) {
            Log.d(TAG, "Companion cleanup non-fatal error: ${e.message}")
        }
    }
}
