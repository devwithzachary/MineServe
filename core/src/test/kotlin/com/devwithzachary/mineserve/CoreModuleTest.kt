package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.auth.WebSessionManager
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.core.Logger
import com.devwithzachary.mineserve.engine.ChunkOptimizer
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelConfig
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.net.tunnel.BoreProtocolSupport
import com.devwithzachary.mineserve.util.FileStorageUtils
import com.devwithzachary.mineserve.util.ServerModFilter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

class CoreModuleTest {

    @Test
    fun coreLogRoutesToCustomSink() {
        val captured = mutableListOf<String>()
        val previous = CoreLog.sink
        try {
            CoreLog.sink = object : Logger {
                override fun d(tag: String, message: String) { captured += "D:$tag:$message" }
                override fun i(tag: String, message: String) { captured += "I:$tag:$message" }
                override fun w(tag: String, message: String, throwable: Throwable?) { captured += "W:$tag:$message" }
                override fun e(tag: String, message: String, throwable: Throwable?) { captured += "E:$tag:$message" }
            }
            CoreLog.i("CoreTest", "hello")
            assertEquals(listOf("I:CoreTest:hello"), captured)
        } finally {
            CoreLog.sink = previous
        }
    }

    @Test
    fun serverTypeCapabilitiesAreAccurate() {
        assertTrue(ServerType.PAPER.supportsPlugins)
        assertFalse(ServerType.PAPER.supportsMods)
        assertTrue(ServerType.FABRIC.supportsMods)
        assertTrue(ServerType.VELOCITY.isProxy)
        assertTrue(ServerType.BEDROCK_BDS.isBedrockNative)
    }

    @Test
    fun minecraftServerSerializationRoundTrip() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val server = MinecraftServer(
            id = "srv-1",
            name = "Test World",
            type = ServerType.PAPER,
            version = "1.21.4",
            port = 25565,
            tunnelConfig = TunnelConfig(
                enabled = true,
                provider = TunnelProvider.MINESERVE_CLOUD,
                cloudSubdomain = "survival",
                cloudRegion = "eu-west",
                cloudDomain = "play.mineserve.app"
            )
        )
        val encoded = json.encodeToString(server)
        val decoded = json.decodeFromString<MinecraftServer>(encoded)
        assertEquals(server, decoded)
    }

    @Test
    fun dimensionTypeResolvesModernAndLegacyLayouts() {
        val root = createTempDirectory("mineserve-dim-test").toFile()
        try {
            val modernNether = File(root, "world/dimensions/minecraft/the_nether/region").apply { mkdirs() }
            File(root, "world/dimensions/minecraft/the_end/region").mkdirs()

            val resolvedNether = DimensionType.NETHER.getDimensionDir(root, "world")
            assertEquals(modernNether.parentFile.absolutePath, resolvedNether.absolutePath)

            val props = ServerProperties(motd = "Core Test", maxPlayers = 25)
            assertEquals("Core Test", props.motd)
            assertEquals(25, props.maxPlayers)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun squaremapDetectionIgnoresConfigDirectoryAfterUninstall() {
        val root = createTempDirectory("mineserve-squaremap-test").toFile()
        try {
            WebMapPluginType.setSquaremapPort(root, isModServer = false, port = 8081)
            assertEquals(8081, WebMapPluginType.detectSquaremapPort(root))
            // Config directory alone must not report Squaremap as installed
            assertNull(WebMapPluginType.detectInstalled(root))
            assertNull(WebMapPluginType.findInstalledFile(root))

            val jarFile = File(root, "plugins/squaremap-paper-1.2.5.jar").apply {
                parentFile?.mkdirs()
                writeText("jar")
            }
            assertEquals(WebMapPluginType.SQUAREMAP, WebMapPluginType.detectInstalled(root))
            assertEquals(jarFile.absolutePath, WebMapPluginType.findInstalledFile(root)?.absolutePath)

            assertTrue(jarFile.delete())
            // After deleting jar, plugins/squaremap/ still exists, but detection must immediately return null
            assertNull(WebMapPluginType.detectInstalled(root))
            assertNull(WebMapPluginType.findInstalledFile(root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun boreProtocolReadsUtf8NullTerminatedFrames() {
        val baos = ByteArrayOutputStream()
        BoreProtocolSupport.writeNullTerminatedMessage(baos, "Hello-UTF8-Test")
        BoreProtocolSupport.writeNullTerminatedMessage(baos, "Second")
        val input = ByteArrayInputStream(baos.toByteArray())
        assertEquals("Hello-UTF8-Test", BoreProtocolSupport.readNullTerminatedMessage(input))
        assertEquals("Second", BoreProtocolSupport.readNullTerminatedMessage(input))
        assertNull(BoreProtocolSupport.readNullTerminatedMessage(input))
    }

    @Test
    fun chunkOptimizerFailsSafeOnLz4CorruptAndUnknownTags() {
        // Unsupported compression type (4 = LZ4 in MC 1.20.5+) -> null (keep chunk)
        assertNull(ChunkOptimizer.extractInhabitedTimeOrNull(byteArrayOf(1, 2, 3), 4))
        // Corrupt deflate payload -> null (keep chunk)
        assertNull(ChunkOptimizer.extractInhabitedTimeOrNull(ByteArray(0), 2))

        // Unknown NBT tag type (99) inside TAG_Compound -> null (fail-safe)
        val unknownTagBytes = ByteArrayOutputStream().apply {
            DataOutputStream(this).use { dos ->
                dos.writeByte(10)
                dos.writeShort(0)
                dos.writeByte(99)
                dos.writeShort(3)
                dos.writeBytes("bad")
                dos.writeByte(0)
            }
        }.toByteArray()
        assertNull(ChunkOptimizer.extractInhabitedTimeOrNull(unknownTagBytes, 3))

        // Valid compressed NBT with InhabitedTime = 4200L -> 4200L
        val validNbt = ByteArrayOutputStream().apply {
            DataOutputStream(this).use { dos ->
                dos.writeByte(10)
                dos.writeShort(0)
                dos.writeByte(4)
                dos.writeShort("InhabitedTime".length)
                dos.writeBytes("InhabitedTime")
                dos.writeLong(4200L)
                dos.writeByte(0)
            }
        }.toByteArray()
        val deflated = ByteArrayOutputStream().apply {
            DeflaterOutputStream(this).use { it.write(validNbt) }
        }.toByteArray()
        assertEquals(4200L, ChunkOptimizer.extractInhabitedTimeOrNull(deflated, 2))
    }

    @Test
    fun serverPropertiesClearsServerIpWithoutDuplicating() {
        val raw = """
            motd=Proxy Backend
            server-ip=127.0.0.1
            server-port=25566
            online-mode=false
        """.trimIndent()
        val parsed = ServerProperties.parse(raw)
        assertEquals("127.0.0.1", parsed.serverIp)
        assertFalse(parsed.customProperties.containsKey("server-ip"))

        val updated = parsed.copy(serverIp = "", onlineMode = true).toPropertiesFileContent()
        val serverIpLines = updated.lines().filter { it.startsWith("server-ip=") }
        assertEquals(listOf("server-ip="), serverIpLines)
    }

    @Test
    fun serverModFilterIgnoresCommentedClientSideOnlyToml() {
        val root = createTempDirectory("mineserve-modfilter-test").toFile()
        try {
            val commentOnlyJar = File(root, "server-helper-1.0.jar")
            ZipOutputStream(commentOnlyJar.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("META-INF/mods.toml"))
                zos.write("# Example: clientSideOnly = true\nmodId=\"helper\"\n".toByteArray())
                zos.closeEntry()
            }
            assertNull(ServerModFilter.detectClientOnlyReason(commentOnlyJar))

            val activeClientJar = File(root, "client-helper-1.0.jar")
            ZipOutputStream(activeClientJar.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("META-INF/neoforge.mods.toml"))
                zos.write("modId=\"hud\"\n  clientSideOnly = true\n".toByteArray())
                zos.closeEntry()
            }
            assertEquals("mods.toml declares clientSideOnly=true", ServerModFilter.detectClientOnlyReason(activeClientJar))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun extractZipSafeBlocksSiblingDirectoryPrefixTraversal() {
        val root = createTempDirectory("mineserve-zip-test").toFile()
        try {
            val targetDir = File(root, "world")
            val evilZip = File(root, "evil.zip")
            ZipOutputStream(evilZip.outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry("../world_evil/pwn.txt"))
                zos.write("owned".toByteArray())
                zos.closeEntry()
            }
            val result = FileStorageUtils.extractZipSafe(evilZip, targetDir)
            assertTrue(result.isFailure)
            assertFalse(File(root, "world_evil/pwn.txt").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun webSessionManagerLockoutAndResetBehaveCleanly() {
        val mgr = WebSessionManager()
        val ip = "192.168.1.50"
        assertFalse(mgr.isIpLockedOut(ip))
        repeat(4) { mgr.recordFailedAttempt(ip) }
        assertFalse(mgr.isIpLockedOut(ip))
        mgr.recordFailedAttempt(ip)
        assertTrue(mgr.isIpLockedOut(ip))
        mgr.resetFailedAttempts(ip)
        assertFalse(mgr.isIpLockedOut(ip))
    }
}

