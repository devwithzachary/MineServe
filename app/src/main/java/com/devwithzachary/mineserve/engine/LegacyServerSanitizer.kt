package com.devwithzachary.mineserve.engine

import android.util.Log
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.compareMinecraftVersions
import java.io.File

object LegacyServerSanitizer {

    private const val TAG = "LegacyServerSanitizer"
    private val EMPTY_LEVEL_TYPE_REGEX = Regex("(?m)^level-type=\\s*$")
    private val LEVEL_TYPE_LINE_REGEX = Regex("(?im)^level-type=.*$")

    private val GAMEMODE_REGEXES = mapOf(
        "survival" to (Regex("(?im)^gamemode\\s*=\\s*survival\\s*$") to "0"),
        "creative" to (Regex("(?im)^gamemode\\s*=\\s*creative\\s*$") to "1"),
        "adventure" to (Regex("(?im)^gamemode\\s*=\\s*adventure\\s*$") to "2"),
        "spectator" to (Regex("(?im)^gamemode\\s*=\\s*spectator\\s*$") to "3")
    )

    private val DIFFICULTY_REGEXES = mapOf(
        "peaceful" to (Regex("(?im)^difficulty\\s*=\\s*peaceful\\s*$") to "0"),
        "easy" to (Regex("(?im)^difficulty\\s*=\\s*easy\\s*$") to "1"),
        "normal" to (Regex("(?im)^difficulty\\s*=\\s*normal\\s*$") to "2"),
        "hard" to (Regex("(?im)^difficulty\\s*=\\s*hard\\s*$") to "3")
    )

    fun sanitize(server: MinecraftServer, serverDir: File) {
        val isLegacy = compareMinecraftVersions(server.version, "1.13") < 0
        if (isLegacy) {
            val legacyFiles = listOf("banned-players.txt", "banned-ips.txt", "ops.txt", "white-list.txt")
            for (name in legacyFiles) {
                val f = File(serverDir, name)
                if (!f.exists()) {
                    try {
                        f.createNewFile()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed creating legacy file $name for server ${server.id}", e)
                    }
                }
            }
        }

        val propFile = File(serverDir, "server.properties")
        if (propFile.exists()) {
            try {
                var content = propFile.readText()
                var modified = false

                if (isLegacy) {
                    if (content.contains("level-type=minecraft:normal", ignoreCase = true) ||
                        content.contains(EMPTY_LEVEL_TYPE_REGEX)
                    ) {
                        content = content.replace(LEVEL_TYPE_LINE_REGEX, "level-type=DEFAULT")
                        modified = true
                    }

                    for ((_, pair) in GAMEMODE_REGEXES) {
                        val (regex, code) = pair
                        if (regex.containsMatchIn(content)) {
                            content = content.replace(regex, "gamemode=$code")
                            modified = true
                        }
                    }

                    for ((_, pair) in DIFFICULTY_REGEXES) {
                        val (regex, code) = pair
                        if (regex.containsMatchIn(content)) {
                            content = content.replace(regex, "difficulty=$code")
                            modified = true
                        }
                    }
                }

                if (modified) {
                    propFile.writeText(content)
                    Log.i(TAG, "Sanitized server.properties for server ${server.id} (${server.version})")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sanitize server.properties for server ${server.id}", e)
            }
        } else {
            try {
                val defaultProps = ServerProperties(motd = server.name, serverPort = server.port)
                propFile.writeText(defaultProps.toPropertiesFileContent(server.version))
            } catch (e: Exception) {
                Log.w(TAG, "Failed creating default server.properties for server ${server.id}", e)
            }
        }
    }
}

