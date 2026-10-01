package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
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

    @Test
    fun testServerBuildAndUpgradeCompatibility() {
        val server = MinecraftServer(
            id = "test-upgrade",
            name = "Upgrade Test",
            type = ServerType.PAPER,
            version = "1.21.1",
            serverBuild = "115"
        )
        assertEquals("115", server.serverBuild)

        // Upgrade server to 1.21.4 and new build 232
        val upgraded = server.copy(
            version = "1.21.4",
            serverBuild = "232",
            javaVersion = com.devwithzachary.mineserve.model.determineJavaVersion("1.21.4", server.type)
        )
        assertEquals("1.21.4", upgraded.version)
        assertEquals("232", upgraded.serverBuild)
        assertEquals(21, upgraded.javaVersion)

        // Verify Java requirement for 26.x is 25
        val futureJava = com.devwithzachary.mineserve.model.determineJavaVersion("26.2", ServerType.PAPER)
        assertEquals(25, futureJava)

        // Verify Java requirement for 1.20.1 is 17
        val olderJava = com.devwithzachary.mineserve.model.determineJavaVersion("1.20.1", ServerType.PAPER)
        assertEquals(17, olderJava)
    }

    @Test
    fun testVersionSortingDescending() {
        val versions = listOf("1.20.1", "1.21.4", "1.21.1", "26.2")
        val sorted = versions.sortedMinecraftVersionsDescending()
        assertEquals(listOf("26.2", "1.21.4", "1.21.1", "1.20.1"), sorted)
    }

    @Test
    fun testLegacyPropertiesFormattingAndNumericParsing() {
        val modernProps = ServerProperties(
            gamemode = "survival",
            difficulty = "normal",
            levelType = "DEFAULT"
        )

        // Pre-1.13 (e.g. 1.0, 1.12.2) should format gamemode and difficulty as numbers
        val legacy10Content = modernProps.toPropertiesFileContent("1.0")
        assertTrue(legacy10Content.contains("gamemode=0"))
        assertTrue(legacy10Content.contains("difficulty=2"))
        assertTrue(legacy10Content.contains("level-type=DEFAULT"))
        assertFalse(legacy10Content.contains("level-type=minecraft:normal"))

        val legacy112Content = modernProps.toPropertiesFileContent("1.12.2")
        assertTrue(legacy112Content.contains("gamemode=0"))
        assertTrue(legacy112Content.contains("difficulty=2"))

        // Modern versions (>= 1.13) should retain named values
        val modernContent = modernProps.toPropertiesFileContent("1.20.1")
        assertTrue(modernContent.contains("gamemode=survival"))
        assertTrue(modernContent.contains("difficulty=normal"))

        // Parsing legacy numeric file content should convert back to canonical names
        val parsedFromNumeric = ServerProperties.parse(legacy10Content)
        assertEquals("survival", parsedFromNumeric.gamemode)
        assertEquals("normal", parsedFromNumeric.difficulty)
        assertEquals("DEFAULT", parsedFromNumeric.levelType)
    }

    @Test
    fun testBooleanPropertiesParsingResilience() {
        val rawConfig = """
            online-mode=TRUE
            pvp=False
            hardcore=1
            white-list=0
            allow-flight=True
            allow-nether=false
            spawn-monsters=1
            spawn-animals=0
        """.trimIndent()

        val parsed = ServerProperties.parse(rawConfig)
        assertTrue(parsed.onlineMode)
        assertFalse(parsed.pvp)
        assertTrue(parsed.hardcore)
        assertFalse(parsed.whiteList)
        assertTrue(parsed.allowFlight)
        assertFalse(parsed.allowNether)
        assertTrue(parsed.spawnMonsters)
        assertFalse(parsed.spawnAnimals)
    }
}
