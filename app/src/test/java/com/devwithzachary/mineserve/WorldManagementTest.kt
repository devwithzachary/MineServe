package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.engine.ChunkOptimizer
import com.devwithzachary.mineserve.engine.WorldManager
import com.devwithzachary.mineserve.model.ChunkPruneOptions
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.InhabitedTimeThreshold
import com.devwithzachary.mineserve.model.WebMapPluginType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.DeflaterOutputStream

class WorldManagementTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDimensionDirectoryResolution() {
        val serverDir = tempFolder.newFolder("server")

        // Paper layout test
        val paperNether = File(serverDir, "world_nether").apply { mkdirs() }
        val paperEnd = File(serverDir, "world_the_end").apply { mkdirs() }
        val overworld = File(serverDir, "world").apply { mkdirs() }

        assertEquals(overworld, DimensionType.OVERWORLD.getDimensionDir(serverDir))
        assertEquals(paperNether, DimensionType.NETHER.getDimensionDir(serverDir))
        assertEquals(paperEnd, DimensionType.THE_END.getDimensionDir(serverDir))

        // Vanilla layout test (clean server without separate paper dimension dirs)
        val vanillaServerDir = tempFolder.newFolder("vanilla_server")
        val vanillaOverworld = File(vanillaServerDir, "world").apply { mkdirs() }
        val vanillaNether = File(vanillaOverworld, "DIM-1").apply { mkdirs() }
        val vanillaEnd = File(vanillaOverworld, "DIM1").apply { mkdirs() }

        assertEquals(vanillaOverworld, DimensionType.OVERWORLD.getDimensionDir(vanillaServerDir))
        assertEquals(vanillaNether, DimensionType.NETHER.getDimensionDir(vanillaServerDir))
        assertEquals(vanillaEnd, DimensionType.THE_END.getDimensionDir(vanillaServerDir))
    }

    @Test
    fun testInhabitedTimeThresholds() {
        assertEquals(0L, InhabitedTimeThreshold.UNTOUCHED.ticks)
        assertEquals(600L, InhabitedTimeThreshold.THIRTY_SECONDS.ticks)
        assertEquals(2400L, InhabitedTimeThreshold.TWO_MINUTES.ticks)
        assertEquals(6000L, InhabitedTimeThreshold.FIVE_MINUTES.ticks)
    }

    @Test
    fun testWebMapPluginDetection() {
        val serverDir = tempFolder.newFolder("map_server")
        val pluginsDir = File(serverDir, "plugins").apply { mkdirs() }

        // Nothing installed
        assertEquals(null, WebMapPluginType.detectInstalled(serverDir))
        assertEquals(null, WebMapPluginType.findInstalledFile(serverDir))

        // Install squaremap in plugins
        val squaremapJar = File(pluginsDir, "squaremap-paper-1.21.4.jar").apply { createNewFile() }
        assertEquals(WebMapPluginType.SQUAREMAP, WebMapPluginType.detectInstalled(serverDir))
        assertEquals(8080, WebMapPluginType.SQUAREMAP.defaultPort)
        assertEquals(squaremapJar.absolutePath, WebMapPluginType.findInstalledFile(serverDir)?.absolutePath)

        // Uninstall squaremap
        val deleted = WebMapPluginType.findInstalledFile(serverDir)?.delete() ?: false
        assertTrue(deleted)
        assertEquals(null, WebMapPluginType.detectInstalled(serverDir))
        assertEquals(null, WebMapPluginType.findInstalledFile(serverDir))

        // Install squaremap in mods (e.g. Fabric server)
        val modsDir = File(serverDir, "mods").apply { mkdirs() }
        val modJar = File(modsDir, "squaremap-fabric-1.21.jar").apply { createNewFile() }
        assertEquals(WebMapPluginType.SQUAREMAP, WebMapPluginType.detectInstalled(serverDir))
        assertEquals(modJar.absolutePath, WebMapPluginType.findInstalledFile(serverDir)?.absolutePath)
    }

    @Test
    fun testExtractInhabitedTimeFromNbt() {
        // Build synthetic NBT compound: TAG_Compound ("") -> TAG_Long ("InhabitedTime", 12500L) -> TAG_End
        val nbtBytes = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                dos.writeByte(10) // TAG_Compound
                dos.writeShort(0) // Root name length 0

                dos.writeByte(4) // TAG_Long
                val name = "InhabitedTime".toByteArray(Charsets.UTF_8)
                dos.writeShort(name.size)
                dos.write(name)
                dos.writeLong(12500L) // Inhabited time ticks

                dos.writeByte(0) // TAG_End
            }
            baos.toByteArray()
        }

        // Compress with Deflater (Zlib compression type 2)
        val compressedBytes = ByteArrayOutputStream().use { baos ->
            DeflaterOutputStream(baos).use { def ->
                def.write(nbtBytes)
            }
            baos.toByteArray()
        }

        val extracted = ChunkOptimizer.extractInhabitedTime(compressedBytes, 2)
        assertEquals(12500L, extracted)

        // Verify empty/corrupt returns 0L safely
        val emptyExtracted = ChunkOptimizer.extractInhabitedTime(ByteArray(0), 2)
        assertEquals(0L, emptyExtracted)
    }

    @Test
    fun testDimensionResetWipesOnlyTargetDimension() = runBlocking {
        val serverDir = tempFolder.newFolder("reset_test_server")
        val overworld = File(serverDir, "world").apply { mkdirs() }
        File(overworld, "level.dat").writeText("dummy level.dat")

        val nether = File(serverDir, "world_nether").apply { mkdirs() }
        File(nether, "DIM-1/region").mkdirs()
        val netherMca = File(nether, "DIM-1/region/r.0.0.mca").apply { writeText("nether data") }

        val end = File(serverDir, "world_the_end").apply { mkdirs() }
        File(end, "DIM1/region").mkdirs()
        val endMca = File(end, "DIM1/region/r.0.0.mca").apply { writeText("end data") }

        // Reset Nether
        val netherResetResult = WorldManager.resetDimension(serverDir, DimensionType.NETHER)
        assertTrue(netherResetResult)
        assertFalse(nether.exists())
        assertTrue(overworld.exists())
        assertTrue(end.exists())

        // Reset End
        val endResetResult = WorldManager.resetDimension(serverDir, DimensionType.THE_END)
        assertTrue(endResetResult)
        assertFalse(end.exists())
        assertTrue(overworld.exists())
    }

    @Test
    fun testChunkOptimizerPruning() = runBlocking {
        val serverDir = tempFolder.newFolder("prune_test_server")
        val regionDir = File(serverDir, "world/region").apply { mkdirs() }
        val mcaFile = File(regionDir, "r.0.0.mca")

        // Construct synthetic MCA file with 2 chunks:
        // Chunk 0 at sector 2: InhabitedTime = 0L (should be pruned)
        // Chunk 1 at sector 3: InhabitedTime = 8000L (should be kept)

        val chunk0Payload = createChunkPayload(0L)
        val chunk1Payload = createChunkPayload(8000L)

        RandomAccessFile(mcaFile, "rw").use { raf ->
            val header = ByteArray(8192)

            // Chunk 0: sector 2, count 1
            header[0] = 0
            header[1] = 0
            header[2] = 2
            header[3] = 1

            // Chunk 1: sector 3, count 1
            header[4] = 0
            header[5] = 0
            header[6] = 3
            header[7] = 1

            raf.write(header)

            // Write chunk 0 at sector 2 (offset 8192)
            raf.seek(8192)
            raf.writeInt(chunk0Payload.size + 1)
            raf.writeByte(2) // Zlib
            raf.write(chunk0Payload)
            val pad0 = 4096 - (chunk0Payload.size + 5)
            if (pad0 > 0) raf.write(ByteArray(pad0))

            // Write chunk 1 at sector 3 (offset 12288)
            raf.seek(12288)
            raf.writeInt(chunk1Payload.size + 1)
            raf.writeByte(2) // Zlib
            raf.write(chunk1Payload)
            val pad1 = 4096 - (chunk1Payload.size + 5)
            if (pad1 > 0) raf.write(ByteArray(pad1))
        }

        // Run ChunkOptimizer with UNTOUCHED threshold (0L)
        val result = ChunkOptimizer.optimizeWorld(
            serverDir = serverDir,
            options = ChunkPruneOptions(threshold = InhabitedTimeThreshold.UNTOUCHED, pruneNether = false, pruneEnd = false)
        )

        assertEquals(1, result.scannedRegions)
        assertEquals(2, result.scannedChunks)
        assertEquals(1, result.prunedChunks) // Chunk 0 pruned
        assertEquals(0, result.deletedRegions) // Region preserved because chunk 1 remains

        // Verify surviving file
        assertTrue(mcaFile.exists())
        RandomAccessFile(mcaFile, "r").use { raf ->
            val header = ByteArray(8192)
            raf.readFully(header)

            // Chunk 0 location should now be 0 (pruned)
            assertEquals(0, header[0].toInt())
            assertEquals(0, header[1].toInt())
            assertEquals(0, header[2].toInt())
            assertEquals(0, header[3].toInt())

            // Chunk 1 location should still exist at sector 2
            val sec1 = ((header[4].toInt() and 0xFF) shl 16) or
                    ((header[5].toInt() and 0xFF) shl 8) or
                    (header[6].toInt() and 0xFF)
            assertEquals(2, sec1)
        }
    }

    private fun createChunkPayload(inhabitedTicks: Long): ByteArray {
        val nbtBytes = ByteArrayOutputStream().use { baos ->
            DataOutputStream(baos).use { dos ->
                dos.writeByte(10) // TAG_Compound
                dos.writeShort(0) // Name ""

                dos.writeByte(4) // TAG_Long
                val name = "InhabitedTime".toByteArray(Charsets.UTF_8)
                dos.writeShort(name.size)
                dos.write(name)
                dos.writeLong(inhabitedTicks)

                dos.writeByte(0) // TAG_End
            }
            baos.toByteArray()
        }

        return ByteArrayOutputStream().use { baos ->
            DeflaterOutputStream(baos).use { def ->
                def.write(nbtBytes)
            }
            baos.toByteArray()
        }
    }
}
