package com.devwithzachary.mineserve.desktop

import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

data class HostMemoryInfo(
    val totalMb: Long,
    val availableMb: Long,
    val maxAllocatableMb: Int,
    val defaultRamMb: Int
)

object LinuxHostTelemetry {
    private val procCpuPrev = ConcurrentHashMap<Long, Pair<Long, Long>>()
    private val whitespaceRegex = Regex("\\s+")

    fun getMemoryInfo(): HostMemoryInfo {
        var totalKb = 0L
        var availKb = 0L
        try {
            val meminfo = File("/proc/meminfo")
            if (meminfo.exists()) {
                for (line in meminfo.readLines()) {
                    when {
                        line.startsWith("MemTotal:") -> {
                            totalKb = line.substringAfter(":").trim().substringBefore(" ").toLongOrNull() ?: 0L
                        }
                        line.startsWith("MemAvailable:") -> {
                            availKb = line.substringAfter(":").trim().substringBefore(" ").toLongOrNull() ?: 0L
                        }
                    }
                    if (totalKb > 0 && availKb > 0) break
                }
            }
        } catch (_: Exception) {}

        val totalMb = (totalKb / 1024L).coerceAtLeast(2048L)
        val availMb = (availKb / 1024L).coerceAtLeast(512L)
        val reservedMb = if (totalMb <= 4096L) 1024L else 2048L
        val rawMax = (totalMb - reservedMb).coerceIn(1024L, 65536L).toInt()
        val maxAlloc = ((rawMax / 256) * 256).coerceAtLeast(1024)
        val halfAligned = (((totalMb / 2) / 256) * 256).toInt()
        val defaultRam = halfAligned.coerceIn(1024, 4096).coerceAtMost(maxAlloc)
        return HostMemoryInfo(
            totalMb = totalMb,
            availableMb = availMb,
            maxAllocatableMb = maxAlloc,
            defaultRamMb = defaultRam
        )
    }

    fun getHostDisplayName(): String {
        val osName = System.getProperty("os.name") ?: "Linux"
        val arch = System.getProperty("os.arch") ?: "x86_64"
        val distro = try {
            val osRelease = File("/etc/os-release")
            if (osRelease.exists()) {
                osRelease.readLines()
                    .firstOrNull { it.startsWith("PRETTY_NAME=") }
                    ?.substringAfter("=")
                    ?.trim('"')
            } else null
        } catch (_: Exception) { null }
        return "${distro ?: osName} ($arch)"
    }

    fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            for (ni in interfaces) {
                if (!ni.isUp || ni.isLoopback || ni.isVirtual) continue
                for (addr in ni.inetAddresses.toList()) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getProcessRssMb(pid: Long): Long {
        if (pid <= 0L) return 0L
        return try {
            val statusFile = File("/proc/$pid/status")
            if (!statusFile.exists()) return 0L
            for (line in statusFile.readLines()) {
                if (line.startsWith("VmRSS:")) {
                    val kb = line.substringAfter(":").trim().substringBefore(" ").toLongOrNull() ?: 0L
                    return kb / 1024L
                }
            }
            0L
        } catch (_: Exception) {
            0L
        }
    }

    fun getDefaultGatewayIp(): String? {
        return try {
            val routeFile = File("/proc/net/route")
            if (!routeFile.exists()) return null
            for (line in routeFile.readLines().drop(1)) {
                val cols = line.trim().split(whitespaceRegex)
                if (cols.size >= 3 && cols[1] == "00000000") {
                    val hex = cols[2]
                    if (hex.length == 8) {
                        val b0 = hex.substring(6, 8).toInt(16)
                        val b1 = hex.substring(4, 6).toInt(16)
                        val b2 = hex.substring(2, 4).toInt(16)
                        val b3 = hex.substring(0, 2).toInt(16)
                        return "$b0.$b1.$b2.$b3"
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getProcessCpuPercent(pid: Long): Float {
        if (pid <= 0L) return 0f
        return try {
            val statFile = File("/proc/$pid/stat")
            if (!statFile.exists()) return 0f
            val content = statFile.readText()
            val rparen = content.lastIndexOf(')')
            if (rparen == -1 || rparen >= content.length - 2) return 0f
            val rest = content.substring(rparen + 2).trim().split(whitespaceRegex)
            if (rest.size < 15) return 0f
            val utime = rest[11].toLongOrNull() ?: 0L
            val stime = rest[12].toLongOrNull() ?: 0L
            val cutime = rest[13].toLongOrNull() ?: 0L
            val cstime = rest[14].toLongOrNull() ?: 0L
            val totalTicks = utime + stime + cutime + cstime
            val now = System.currentTimeMillis()
            val prev = procCpuPrev.put(pid, totalTicks to now) ?: return 0f
            val deltaTicks = totalTicks - prev.first
            val deltaMs = now - prev.second
            if (deltaMs <= 200L || deltaTicks < 0L) return 0f
            // Percentage of 1 full CPU core (Minecraft main tick loop is single-threaded; 100 Hz USER_HZ)
            val pct = (deltaTicks.toFloat() / (deltaMs / 1000f * 100f)) * 100f
            pct.coerceIn(0f, 100f)
        } catch (_: Exception) {
            0f
        }
    }
}
