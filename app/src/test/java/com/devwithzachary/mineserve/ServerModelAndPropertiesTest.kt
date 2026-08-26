package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerModelAndPropertiesTest {

    @Test
    fun testServerPropertiesSerializationAndParsing() {
        val original = ServerProperties(
            motd = "Zachary's Custom Server",
            serverPort = 25566,
            maxPlayers = 50,
            gamemode = "survival",
            difficulty = "hard",
            pvp = true,
            hardcore = true,
            onlineMode = false,
            whiteList = true,
            viewDistance = 12,
            simulationDistance = 10,
            levelSeed = "1234567890"
        )

        val propertiesFileContent = original.toPropertiesFileContent()
        val parsed = ServerProperties.parse(propertiesFileContent)

        assertEquals("Zachary's Custom Server", parsed.motd)
        assertEquals(25566, parsed.serverPort)
        assertEquals(50, parsed.maxPlayers)
        assertEquals("survival", parsed.gamemode)
        assertEquals("hard", parsed.difficulty)
        assertTrue(parsed.pvp)
        assertTrue(parsed.hardcore)
        assertFalse(parsed.onlineMode)
        assertTrue(parsed.whiteList)
        assertEquals(12, parsed.viewDistance)
        assertEquals(10, parsed.simulationDistance)
        assertEquals("1234567890", parsed.levelSeed)
    }

    @Test
    fun testServerTypeDefaults() {
        assertEquals("PaperMC", ServerType.PAPER.displayName)
        assertTrue(ServerType.PAPER.supportsPlugins)
        assertFalse(ServerType.PAPER.supportsMods)
        assertEquals(21, ServerType.PAPER.defaultJavaVersion)

        assertEquals("Fabric", ServerType.FABRIC.displayName)
        assertTrue(ServerType.FABRIC.supportsMods)
        assertFalse(ServerType.FABRIC.supportsPlugins)

        assertEquals("Vanilla", ServerType.VANILLA.displayName)
        assertFalse(ServerType.VANILLA.supportsPlugins)
        assertFalse(ServerType.VANILLA.supportsMods)
    }

    @Test
    fun testMinecraftServerRunningState() {
        val stoppedServer = MinecraftServer(
            id = "test-1",
            name = "Test Server",
            type = ServerType.PAPER,
            version = "1.21.4",
            status = ServerStatus.STOPPED
        )
        assertFalse(stoppedServer.isRunning)

        val runningServer = stoppedServer.copy(status = ServerStatus.RUNNING)
        assertTrue(runningServer.isRunning)

        val startingServer = stoppedServer.copy(status = ServerStatus.STARTING)
        assertTrue(startingServer.isRunning)
    }
}
