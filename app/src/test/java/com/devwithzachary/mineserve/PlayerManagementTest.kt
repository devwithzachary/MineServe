package com.devwithzachary.mineserve

import com.devwithzachary.mineserve.model.BannedIpEntry
import com.devwithzachary.mineserve.model.BannedPlayerEntry
import com.devwithzachary.mineserve.model.OpEntry
import com.devwithzachary.mineserve.model.WhitelistEntry
import com.devwithzachary.mineserve.model.getPlayerAvatarUrl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerManagementTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true; isLenient = true }

    @Test
    fun testPlayerAvatarUrlResolution() {
        val urlWithNameOnly = getPlayerAvatarUrl("Steve")
        assertEquals("https://minotar.net/helm/Steve/100.png", urlWithNameOnly)

        val trimmedUrl = getPlayerAvatarUrl("  Alex  ")
        assertEquals("https://minotar.net/helm/Alex/100.png", trimmedUrl)

        val uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5"
        val urlWithNameAndUuid = getPlayerAvatarUrl("Notch", uuid)
        assertEquals("https://minotar.net/helm/Notch/100.png", urlWithNameAndUuid)

        val urlWithUuidOnly = getPlayerAvatarUrl("", uuid)
        assertEquals("https://minotar.net/helm/$uuid/100.png", urlWithUuidOnly)

        val emptyUrl = getPlayerAvatarUrl("", null)
        assertEquals("", emptyUrl)
    }

    @Test
    fun testWhitelistJsonSerializationAndParsing() {
        val original = listOf(
            WhitelistEntry(uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5", name = "Notch"),
            WhitelistEntry(uuid = "616ab54c-9f4e-4b83-881b-9076427d18b3", name = "Jeb_")
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<List<WhitelistEntry>>(serialized)

        assertEquals(2, deserialized.size)
        assertEquals("Notch", deserialized[0].name)
        assertEquals("069a79f4-44e9-4726-a5be-fca90e38aaf5", deserialized[0].uuid)
        assertEquals("Jeb_", deserialized[1].name)
    }

    @Test
    fun testOpsJsonSerializationAndParsing() {
        val original = listOf(
            OpEntry(uuid = "12345", name = "AdminUser", level = 4, bypassesPlayerLimit = true)
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<List<OpEntry>>(serialized)

        assertEquals(1, deserialized.size)
        assertEquals("AdminUser", deserialized[0].name)
        assertEquals(4, deserialized[0].level)
        assertTrue(deserialized[0].bypassesPlayerLimit)
    }

    @Test
    fun testBannedPlayersJsonSerializationAndParsing() {
        val original = listOf(
            BannedPlayerEntry(
                uuid = "99999",
                name = "BadActor",
                created = "2026-10-01 12:00:00 +0000",
                source = "Server",
                expires = "forever",
                reason = "Griefing world spawn."
            )
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<List<BannedPlayerEntry>>(serialized)

        assertEquals(1, deserialized.size)
        assertEquals("BadActor", deserialized[0].name)
        assertEquals("Griefing world spawn.", deserialized[0].reason)
    }

    @Test
    fun testBannedIpsJsonSerializationAndParsing() {
        val original = listOf(
            BannedIpEntry(
                ip = "192.168.1.100",
                created = "2026-10-01 12:00:00 +0000",
                source = "Server",
                expires = "forever",
                reason = "Bot flood."
            )
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<List<BannedIpEntry>>(serialized)

        assertEquals(1, deserialized.size)
        assertEquals("192.168.1.100", deserialized[0].ip)
        assertEquals("Bot flood.", deserialized[0].reason)
    }

    @Test
    fun testLenientParsingWithExtraUnknownMinecraftFields() {
        val rawMinecraftJson = """
            [
              {
                "uuid": "abc-123",
                "name": "Steve",
                "extraServerMetadata": "customValue",
                "someInt": 42
              }
            ]
        """.trimIndent()

        val parsed = json.decodeFromString<List<WhitelistEntry>>(rawMinecraftJson)
        assertEquals(1, parsed.size)
        assertEquals("Steve", parsed[0].name)
        assertEquals("abc-123", parsed[0].uuid)
    }
}
