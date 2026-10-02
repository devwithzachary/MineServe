package com.devwithzachary.mineserve.model

import kotlinx.serialization.Serializable

@Serializable
data class WhitelistEntry(
    val uuid: String = "",
    val name: String = ""
)

@Serializable
data class OpEntry(
    val uuid: String = "",
    val name: String = "",
    val level: Int = 4,
    val bypassesPlayerLimit: Boolean = false
)

@Serializable
data class BannedPlayerEntry(
    val uuid: String = "",
    val name: String = "",
    val created: String = "",
    val source: String = "Server",
    val expires: String = "forever",
    val reason: String = "Banned by an operator."
)

@Serializable
data class BannedIpEntry(
    val ip: String = "",
    val created: String = "",
    val source: String = "Server",
    val expires: String = "forever",
    val reason: String = "Banned by an operator."
)

data class ServerPlayerLists(
    val whitelist: List<WhitelistEntry> = emptyList(),
    val ops: List<OpEntry> = emptyList(),
    val bannedPlayers: List<BannedPlayerEntry> = emptyList(),
    val bannedIps: List<BannedIpEntry> = emptyList()
)

data class PlayerActionTarget(
    val username: String,
    val uuid: String? = null,
    val isOnline: Boolean = false,
    val isOp: Boolean = false,
    val isWhitelisted: Boolean = false,
    val isBanned: Boolean = false
)

enum class PlayerManagementTab(val title: String) {
    ONLINE("Online"),
    WHITELIST("Whitelist"),
    OPS("Operators"),
    BANNED_PLAYERS("Banned Players"),
    BANNED_IPS("Banned IPs")
}

/**
 * Returns a 3D player head/helm avatar URL using Minotar.
 */
fun getPlayerAvatarUrl(username: String, uuid: String? = null): String {
    val cleanName = username.trim()
    val cleanUuid = uuid?.trim().orEmpty()
    val identifier = cleanName.ifBlank { cleanUuid }
    return if (identifier.isNotBlank()) {
        "https://minotar.net/helm/$identifier/100.png"
    } else {
        ""
    }
}
