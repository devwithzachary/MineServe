package com.devwithzachary.mineserve.repository

import android.util.Log
import com.devwithzachary.mineserve.model.BannedIpEntry
import com.devwithzachary.mineserve.model.BannedPlayerEntry
import com.devwithzachary.mineserve.model.OpEntry
import com.devwithzachary.mineserve.model.ServerPlayerLists
import com.devwithzachary.mineserve.model.WhitelistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PlayerListRepository(
    private val serversDir: File,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
) {
    companion object {
        private const val TAG = "PlayerListRepository"
    }

    suspend fun loadPlayerLists(serverId: String): ServerPlayerLists = withContext(Dispatchers.IO) {
        val serverDir = File(serversDir, serverId)
        if (!serverDir.exists()) return@withContext ServerPlayerLists()

        ServerPlayerLists(
            whitelist = loadWhitelist(serverDir),
            ops = loadOps(serverDir),
            bannedPlayers = loadBannedPlayers(serverDir),
            bannedIps = loadBannedIps(serverDir)
        )
    }

    private fun loadWhitelist(serverDir: File): List<WhitelistEntry> {
        val jsonFile = File(serverDir, "whitelist.json")
        if (jsonFile.exists() && jsonFile.length() > 0) {
            try {
                return json.decodeFromString<List<WhitelistEntry>>(jsonFile.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing whitelist.json, trying fallback", e)
            }
        }
        val txtFile = File(serverDir, "white-list.txt")
        if (txtFile.exists()) {
            return txtFile.readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { WhitelistEntry(name = it) }
        }
        return emptyList()
    }

    suspend fun saveWhitelist(serverId: String, list: List<WhitelistEntry>): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val jsonFile = File(serverDir, "whitelist.json")
            jsonFile.writeText(json.encodeToString(list))
            val txtFile = File(serverDir, "white-list.txt")
            if (txtFile.exists()) {
                txtFile.writeText(list.joinToString("\n") { it.name })
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving whitelist for $serverId", e)
            false
        }
    }

    private fun loadOps(serverDir: File): List<OpEntry> {
        val jsonFile = File(serverDir, "ops.json")
        if (jsonFile.exists() && jsonFile.length() > 0) {
            try {
                return json.decodeFromString<List<OpEntry>>(jsonFile.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing ops.json, trying fallback", e)
            }
        }
        val txtFile = File(serverDir, "ops.txt")
        if (txtFile.exists()) {
            return txtFile.readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { OpEntry(name = it, level = 4) }
        }
        return emptyList()
    }

    suspend fun saveOps(serverId: String, list: List<OpEntry>): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val jsonFile = File(serverDir, "ops.json")
            jsonFile.writeText(json.encodeToString(list))
            val txtFile = File(serverDir, "ops.txt")
            if (txtFile.exists()) {
                txtFile.writeText(list.joinToString("\n") { it.name })
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving ops for $serverId", e)
            false
        }
    }

    private fun loadBannedPlayers(serverDir: File): List<BannedPlayerEntry> {
        val jsonFile = File(serverDir, "banned-players.json")
        if (jsonFile.exists() && jsonFile.length() > 0) {
            try {
                return json.decodeFromString<List<BannedPlayerEntry>>(jsonFile.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing banned-players.json, trying fallback", e)
            }
        }
        val txtFile = File(serverDir, "banned-players.txt")
        if (txtFile.exists()) {
            return txtFile.readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { line ->
                    val parts = line.split("|")
                    if (parts.size >= 5) {
                        BannedPlayerEntry(name = parts[0], created = parts[1], source = parts[2], expires = parts[3], reason = parts[4])
                    } else {
                        BannedPlayerEntry(name = line)
                    }
                }
        }
        return emptyList()
    }

    suspend fun saveBannedPlayers(serverId: String, list: List<BannedPlayerEntry>): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val jsonFile = File(serverDir, "banned-players.json")
            jsonFile.writeText(json.encodeToString(list))
            val txtFile = File(serverDir, "banned-players.txt")
            if (txtFile.exists()) {
                txtFile.writeText(list.joinToString("\n") { it.name })
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving banned players for $serverId", e)
            false
        }
    }

    private fun loadBannedIps(serverDir: File): List<BannedIpEntry> {
        val jsonFile = File(serverDir, "banned-ips.json")
        if (jsonFile.exists() && jsonFile.length() > 0) {
            try {
                return json.decodeFromString<List<BannedIpEntry>>(jsonFile.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing banned-ips.json, trying fallback", e)
            }
        }
        val txtFile = File(serverDir, "banned-ips.txt")
        if (txtFile.exists()) {
            return txtFile.readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { line ->
                    val parts = line.split("|")
                    if (parts.size >= 5) {
                        BannedIpEntry(ip = parts[0], created = parts[1], source = parts[2], expires = parts[3], reason = parts[4])
                    } else {
                        BannedIpEntry(ip = line)
                    }
                }
        }
        return emptyList()
    }

    suspend fun saveBannedIps(serverId: String, list: List<BannedIpEntry>): Boolean = withContext(Dispatchers.IO) {
        try {
            val serverDir = File(serversDir, serverId)
            val jsonFile = File(serverDir, "banned-ips.json")
            jsonFile.writeText(json.encodeToString(list))
            val txtFile = File(serverDir, "banned-ips.txt")
            if (txtFile.exists()) {
                txtFile.writeText(list.joinToString("\n") { it.ip })
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving banned IPs for $serverId", e)
            false
        }
    }

    suspend fun addWhitelistPlayer(serverId: String, name: String, uuid: String = ""): Boolean {
        val current = loadPlayerLists(serverId).whitelist.toMutableList()
        if (current.none { it.name.equals(name, ignoreCase = true) }) {
            current.add(WhitelistEntry(uuid = uuid, name = name))
            return saveWhitelist(serverId, current)
        }
        return true
    }

    suspend fun removeWhitelistPlayer(serverId: String, name: String): Boolean {
        val current = loadPlayerLists(serverId).whitelist.filterNot { it.name.equals(name, ignoreCase = true) }
        return saveWhitelist(serverId, current)
    }

    suspend fun addOp(serverId: String, name: String, level: Int = 4, uuid: String = ""): Boolean {
        val current = loadPlayerLists(serverId).ops.toMutableList()
        current.removeAll { it.name.equals(name, ignoreCase = true) }
        current.add(OpEntry(uuid = uuid, name = name, level = level))
        return saveOps(serverId, current)
    }

    suspend fun removeOp(serverId: String, name: String): Boolean {
        val current = loadPlayerLists(serverId).ops.filterNot { it.name.equals(name, ignoreCase = true) }
        return saveOps(serverId, current)
    }

    suspend fun addBannedPlayer(serverId: String, name: String, reason: String = "Banned by an operator.", uuid: String = ""): Boolean {
        val current = loadPlayerLists(serverId).bannedPlayers.toMutableList()
        current.removeAll { it.name.equals(name, ignoreCase = true) }
        current.add(BannedPlayerEntry(uuid = uuid, name = name, reason = reason))
        return saveBannedPlayers(serverId, current)
    }

    suspend fun removeBannedPlayer(serverId: String, name: String): Boolean {
        val current = loadPlayerLists(serverId).bannedPlayers.filterNot { it.name.equals(name, ignoreCase = true) }
        return saveBannedPlayers(serverId, current)
    }

    suspend fun addBannedIp(serverId: String, ip: String, reason: String = "Banned by an operator."): Boolean {
        val current = loadPlayerLists(serverId).bannedIps.toMutableList()
        current.removeAll { it.ip.equals(ip, ignoreCase = true) }
        current.add(BannedIpEntry(ip = ip, reason = reason))
        return saveBannedIps(serverId, current)
    }

    suspend fun removeBannedIp(serverId: String, ip: String): Boolean {
        val current = loadPlayerLists(serverId).bannedIps.filterNot { it.ip.equals(ip, ignoreCase = true) }
        return saveBannedIps(serverId, current)
    }
}

