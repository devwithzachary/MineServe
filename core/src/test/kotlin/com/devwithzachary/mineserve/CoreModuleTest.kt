package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.core.Logger
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelConfig
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.WebMapPluginType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
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
}

