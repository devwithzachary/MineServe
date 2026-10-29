package com.devwithzachary.mineserve.engine

import com.devwithzachary.mineserve.model.ServerStatus

object ConsoleOutputParser {

    private val ANSI_COLOR_REGEX = Regex("\u001B\\[[;\\d]*[ -/]*[@-~]")
    private val PLAYER_JOIN_REGEX_1 = Regex("""(?:\[Server thread/INFO\]:?\s+|INFO\]:\s+|:\s+)?([a-zA-Z0-9_.*+]{1,32})\s+joined the game""", RegexOption.IGNORE_CASE)
    private val PLAYER_JOIN_REGEX_2 = Regex("""(?:\[Server thread/INFO\]:?\s+|INFO\]:\s+|:\s+)?([a-zA-Z0-9_.*+]{1,32})\[/.*?\]\s+logged in""", RegexOption.IGNORE_CASE)
    private val PLAYER_LEAVE_REGEX = Regex("""(?:\[Server thread/INFO\]:?\s+|INFO\]:\s+|:\s+)?([a-zA-Z0-9_.*+]{1,32})\s+(?:left the game|lost connection)""", RegexOption.IGNORE_CASE)
    private val LAG_WARNING_REGEX = Regex("""Can't keep up!.*?Running\s+(\d+)ms\s+or\s+(\d+)\s+ticks behind""", RegexOption.IGNORE_CASE)
    private val PAPER_TPS_REGEX = Regex("""TPS from last [^:]+:\s*[*§a-f0-9]*([0-9.]+)(?:,\s*[*§a-f0-9]*([0-9.]+))?""", RegexOption.IGNORE_CASE)
    private val TICK_MSPT_REGEX = Regex("""(?:Average tick time|The average tick time is):\s*([0-9.]+)ms""", RegexOption.IGNORE_CASE)

    fun parseBuffer(
        serverId: String,
        buffer: ByteArray,
        length: Int,
        session: ActiveServerSession,
        onUpdateStatus: (String, ServerStatus) -> Unit
    ) {
        val text = String(buffer, 0, length, Charsets.UTF_8)
        val lines = text.split("\n", "\r")

        for (rawLine in lines) {
            val line = rawLine.replace(ANSI_COLOR_REGEX, "").trim()
            if (line.isBlank()) continue

            // 1. Done / Running trigger
            if (line.contains("Done (") || line.contains("For help, type \"help\"") || line.contains("Time elapsed:")) {
                onUpdateStatus(serverId, ServerStatus.RUNNING)
            }

            // 2. Player joins
            if (line.contains("joined the game", ignoreCase = true) || line.contains("logged in with entity id", ignoreCase = true)) {
                val match = PLAYER_JOIN_REGEX_1.find(line) ?: PLAYER_JOIN_REGEX_2.find(line)
                match?.groupValues?.get(1)?.let { player ->
                    session.onlinePlayers.add(player)
                    session.idleStartTimeMillis = null
                    session.isAutoSleeping = false
                }
            }

            // 3. Player leaves
            if (line.contains("left the game", ignoreCase = true) || line.contains("lost connection", ignoreCase = true)) {
                val match = PLAYER_LEAVE_REGEX.find(line)
                match?.groupValues?.get(1)?.let { player ->
                    session.onlinePlayers.remove(player)
                    if (session.onlinePlayers.isEmpty()) {
                        session.idleStartTimeMillis = System.currentTimeMillis()
                    }
                }
            }

            // 4. Server overload / tick lag detection ("Can't keep up!")
            if (line.contains("Can't keep up!", ignoreCase = true)) {
                val match = LAG_WARNING_REGEX.find(line)
                val msBehind = match?.groupValues?.get(1)?.toDoubleOrNull() ?: 1000.0
                val now = System.currentTimeMillis()
                session.totalLagWarnings++
                session.lastLagWarningTime = now
                session.lastTpsLogTime = now
                val dropTps = (20.0 * (1.0 - (msBehind / (msBehind + 1000.0)).coerceIn(0.0, 0.95))).coerceIn(1.0, 19.5)
                session.currentTps = kotlin.math.min(session.currentTps, dropTps)
                session.currentMspt = (1000.0 / session.currentTps).coerceIn(50.0, 500.0)
            }

            // 5. Paper / Purpur / Spigot /tps output
            if (line.contains("TPS from last", ignoreCase = true)) {
                val match = PAPER_TPS_REGEX.find(line)
                match?.groupValues?.get(1)?.toDoubleOrNull()?.let { parsedTps ->
                    session.currentTps = parsedTps.coerceIn(0.0, 20.0)
                    session.currentMspt = if (session.currentTps >= 19.95) 20.0 else (1000.0 / session.currentTps)
                    session.lastTpsLogTime = System.currentTimeMillis()
                }
            }

            // 6. Minecraft 1.20+ /tick query or MSPT logs
            if (line.contains("average tick time", ignoreCase = true)) {
                val match = TICK_MSPT_REGEX.find(line)
                match?.groupValues?.get(1)?.toDoubleOrNull()?.let { parsedMspt ->
                    session.currentMspt = parsedMspt
                    session.currentTps = if (parsedMspt <= 50.0) 20.0 else (1000.0 / parsedMspt).coerceIn(0.0, 20.0)
                    session.lastTpsLogTime = System.currentTimeMillis()
                }
            }

            // 7. Stopping trigger
            if (line.contains("Stopping the server") || line.contains("Stopping server")) {
                onUpdateStatus(serverId, ServerStatus.STOPPING)
            }
        }
    }
}

