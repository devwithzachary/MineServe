package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.api.ForgeApiClient
import com.devwithzachary.mineserve.api.MojangApiClient
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeAndLegacyMinecraftTest {

    @Test
    fun testServerTypeForgeProperties() {
        assertEquals("Forge", ServerType.FORGE.displayName)
        assertTrue(ServerType.FORGE.supportsMods)
        assertFalse(ServerType.FORGE.supportsPlugins)
        assertEquals(21, ServerType.FORGE.defaultJavaVersion)
    }

    @Test
    fun testDetermineJavaVersionForLegacyReleases() {
        // Historical Minecraft versions down to 1.0 must map to Java 8
        assertEquals(8, determineJavaVersion("1.0", ServerType.VANILLA))
        assertEquals(8, determineJavaVersion("1.0.0", ServerType.VANILLA))
        assertEquals(8, determineJavaVersion("1.1", ServerType.VANILLA))
        assertEquals(8, determineJavaVersion("1.2.5", ServerType.VANILLA))
        assertEquals(8, determineJavaVersion("1.5.2", ServerType.FORGE))
        assertEquals(8, determineJavaVersion("1.7.10", ServerType.FORGE))
        assertEquals(8, determineJavaVersion("1.8.9", ServerType.FORGE))
        assertEquals(8, determineJavaVersion("1.12.2", ServerType.FORGE))
        assertEquals(8, determineJavaVersion("1.16.5", ServerType.FORGE))

        // Modern versions
        assertEquals(17, determineJavaVersion("1.17.1", ServerType.FORGE))
        assertEquals(17, determineJavaVersion("1.18.2", ServerType.FORGE))
        assertEquals(17, determineJavaVersion("1.19.4", ServerType.FORGE))
        assertEquals(17, determineJavaVersion("1.20.1", ServerType.FORGE))
        assertEquals(21, determineJavaVersion("1.20.6", ServerType.FORGE))
        assertEquals(21, determineJavaVersion("1.21.1", ServerType.FORGE))
        assertEquals(25, determineJavaVersion("26.2", ServerType.FORGE))
    }

    @Test
    fun testLegacyVersionSortingDescending() {
        val versions = listOf("1.0", "1.12.2", "1.20.1", "1.7.10", "1.2.5", "1.1", "1.16.5")
        val sorted = versions.sortedMinecraftVersionsDescending()
        assertEquals(listOf("1.20.1", "1.16.5", "1.12.2", "1.7.10", "1.2.5", "1.1", "1.0"), sorted)
    }

    @Test
    fun testMojangLegacyServerJarFallback() = runBlocking {
        val client = MojangApiClient()

        // Test fallback URLs for versions 1.0 and 1.1 where Mojang's manifest lacks downloads.server
        val url10 = client.getServerJarDownloadUrl("1.0")
        assertNotNull(url10)
        assertTrue(url10!!.contains("omniarchive.uk") && url10.endsWith(".jar"))

        val url100 = client.getServerJarDownloadUrl("1.0.0")
        assertNotNull(url100)
        assertTrue(url100!!.contains("omniarchive.uk") && url100.endsWith(".jar"))

        val url11 = client.getServerJarDownloadUrl("1.1")
        assertNotNull(url11)
        assertTrue(url11!!.contains("omniarchive.uk") && url11.endsWith(".jar"))

        val url124 = client.getServerJarDownloadUrl("1.2.4")
        assertNotNull(url124)
        assertTrue(url124!!.contains("omniarchive.uk") && url124.endsWith(".jar"))
    }

    @Test
    fun testForgeApiClientVersionsAndUrls() = runBlocking {
        val client = ForgeApiClient()

        val versions = client.getVersions()
        assertTrue(versions.isNotEmpty())
        assertTrue(versions.contains("1.20.1"))
        assertTrue(versions.contains("1.16.5"))
        assertTrue(versions.contains("1.12.2"))
        assertTrue(versions.contains("1.7.10"))

        val promo1201 = client.getPromoVersion("1.20.1")
        assertNotNull(promo1201)

        val promo1122 = client.getPromoVersion("1.12.2")
        assertNotNull(promo1122)

        val url1201 = client.getDownloadUrl("1.20.1")
        assertNotNull(url1201)
        assertTrue(url1201!!.contains("maven.minecraftforge.net"))
        assertTrue(url1201.contains("1.20.1"))
        assertTrue(url1201.endsWith("-installer.jar"))

        val url1122 = client.getDownloadUrl("1.12.2")
        assertNotNull(url1122)
        assertTrue(url1122!!.contains("maven.minecraftforge.net"))
        assertTrue(url1122.contains("1.12.2"))
        assertTrue(url1122.endsWith("-installer.jar"))
    }
}
