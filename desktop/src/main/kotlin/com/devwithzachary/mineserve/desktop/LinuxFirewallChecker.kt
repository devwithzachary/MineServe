package com.devwithzachary.mineserve.desktop

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class FirewallCheckResult(
    val firewallName: String,
    val isActive: Boolean,
    val isPortConfirmedOpen: Boolean,
    val warningMessage: String?,
    val fixCommand: String?
)

/**
 * Checks common Linux host firewalls (ufw on CachyOS/Ubuntu/Arch, firewalld on Fedora/RHEL,
 * nftables) without requiring root privileges and produces actionable inbound port warnings.
 */
object LinuxFirewallChecker {
    private const val CACHE_TTL_MS = 30_000L
    private val cache = ConcurrentHashMap<String, Pair<Long, FirewallCheckResult>>()

    fun checkInboundPort(port: Int, isUdp: Boolean = false): FirewallCheckResult {
        val proto = if (isUdp) "udp" else "tcp"
        val key = "$port/$proto"
        val now = System.currentTimeMillis()
        cache[key]?.let { (ts, res) ->
            if (now - ts < CACHE_TTL_MS) return res
        }

        val result = detectFirewallState(port, proto)
        cache[key] = now to result
        return result
    }

    private fun detectFirewallState(port: Int, proto: String): FirewallCheckResult {
        // 1. UFW (CachyOS, Ubuntu, Debian, Arch, Mint)
        if (isUfwActive()) {
            val open = isPortOpenInUfw(port, proto)
            return if (open) {
                FirewallCheckResult("ufw", isActive = true, isPortConfirmedOpen = true, warningMessage = null, fixCommand = null)
            } else {
                val cmd = "sudo ufw allow $port/$proto"
                FirewallCheckResult(
                    firewallName = "ufw",
                    isActive = true,
                    isPortConfirmedOpen = false,
                    warningMessage = "UFW firewall is active; inbound $port/$proto may be blocked for LAN/direct play. Run '$cmd' or enable a Bore/Playit tunnel.",
                    fixCommand = cmd
                )
            }
        }

        // 2. firewalld (Fedora, RHEL, openSUSE)
        if (isFirewalldActive()) {
            val open = isPortOpenInFirewalld(port, proto)
            return if (open) {
                FirewallCheckResult("firewalld", isActive = true, isPortConfirmedOpen = true, warningMessage = null, fixCommand = null)
            } else {
                val cmd = "sudo firewall-cmd --add-port=$port/$proto --permanent && sudo firewall-cmd --reload"
                FirewallCheckResult(
                    firewallName = "firewalld",
                    isActive = true,
                    isPortConfirmedOpen = false,
                    warningMessage = "firewalld is active; inbound $port/$proto may be blocked for LAN/direct play. Run '$cmd' or enable a Bore/Playit tunnel.",
                    fixCommand = cmd
                )
            }
        }

        // 3. nftables standalone
        if (isServiceActive("nftables")) {
            val cmd = "sudo nft add rule inet filter input $proto dport $port accept"
            return FirewallCheckResult(
                firewallName = "nftables",
                isActive = true,
                isPortConfirmedOpen = false,
                warningMessage = "nftables is active; ensure inbound $port/$proto is allowed or enable a Bore/Playit tunnel.",
                fixCommand = cmd
            )
        }

        return FirewallCheckResult("none", isActive = false, isPortConfirmedOpen = true, warningMessage = null, fixCommand = null)
    }

    private fun isUfwActive(): Boolean {
        if (isServiceActive("ufw")) return true
        val ufwConf = File("/etc/ufw/ufw.conf")
        if (ufwConf.exists() && ufwConf.canRead()) {
            val enabled = runCatching {
                ufwConf.readLines().any { line ->
                    val trimmed = line.trim()
                    !trimmed.startsWith("#") && trimmed.equals("ENABLED=yes", ignoreCase = true)
                }
            }.getOrDefault(false)
            if (enabled) return true
        }
        return false
    }

    private fun isPortOpenInUfw(port: Int, proto: String): Boolean {
        // If running with sufficient permissions, check `ufw status`
        val statusOut = runQuickCommand("ufw", "status")
        if (!statusOut.isNullOrBlank()) {
            val pattern = Regex("""\b$port(?:/$proto)?\b.*\bALLOW\b""", RegexOption.IGNORE_CASE)
            if (statusOut.lines().any { pattern.containsMatchIn(it) }) return true
        }
        // Check /etc/ufw/user.rules if readable (match -p, --dport, and -j ACCEPT on the same rule line)
        val rulesFile = File("/etc/ufw/user.rules")
        if (rulesFile.exists() && rulesFile.canRead()) {
            val protoPattern = Regex("""(?:^|\s)-p\s+${Regex.escape(proto)}(?:\s|$)""", RegexOption.IGNORE_CASE)
            val dportPattern = Regex("""(?:^|\s)--dport\s+$port(?:\s|$)""")
            val acceptPattern = Regex("""(?:^|\s)-j\s+ACCEPT(?:\s|$)""", RegexOption.IGNORE_CASE)
            val matched = runCatching {
                rulesFile.readLines().any { rawLine ->
                    val line = rawLine.trim()
                    line.isNotEmpty() &&
                        !line.startsWith("#") &&
                        protoPattern.containsMatchIn(line) &&
                        dportPattern.containsMatchIn(line) &&
                        acceptPattern.containsMatchIn(line)
                }
            }.getOrDefault(false)
            if (matched) return true
        }
        return false
    }

    private fun isFirewalldActive(): Boolean {
        if (isServiceActive("firewalld")) return true
        val state = runQuickCommand("firewall-cmd", "--state")?.trim()
        return state.equals("running", ignoreCase = true)
    }

    private fun isPortOpenInFirewalld(port: Int, proto: String): Boolean {
        val ports = runQuickCommand("firewall-cmd", "--list-ports") ?: return false
        return ports.split(Regex("\\s+")).any { it.equals("$port/$proto", ignoreCase = true) }
    }

    private fun isServiceActive(serviceName: String): Boolean {
        val out = runQuickCommand("systemctl", "is-active", serviceName)?.trim()
        return out == "active"
    }

    private fun runQuickCommand(vararg cmd: String): String? {
        return try {
            val proc = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            val finished = proc.waitFor(1500, TimeUnit.MILLISECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return null
            }
            if (proc.exitValue() != 0) return null
            proc.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            null
        }
    }
}
