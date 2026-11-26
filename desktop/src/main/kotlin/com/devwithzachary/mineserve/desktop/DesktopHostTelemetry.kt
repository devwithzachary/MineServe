package com.devwithzachary.mineserve.desktop

import java.io.File
import java.lang.management.ManagementFactory
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class HostMemoryInfo(
    val totalMb: Long,
    val availableMb: Long,
    val maxAllocatableMb: Int,
    val defaultRamMb: Int
)

object DesktopHostTelemetry {
    private val procCpuPrev = ConcurrentHashMap<Long, Pair<Long, Long>>()
    private val handleCpuPrev = ConcurrentHashMap<Long, Pair<Long, Long>>()
    private val whitespaceRegex = Regex("\\s+")
    private val cachedHostDisplayName: String by lazy { resolveHostDisplayName() }

    fun getMemoryInfo(): HostMemoryInfo {
        var totalKb = 0L
        var availKb = 0L
        if (DesktopPlatform.isLinux) {
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
        }

        var totalMb = totalKb / 1024L
        var availMb = availKb / 1024L

        if (totalMb <= 0L) {
            runCatching {
                val osBean = ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.OperatingSystemMXBean
                if (osBean != null) {
                    totalMb = osBean.totalMemorySize / (1024L * 1024L)
                    availMb = osBean.freeMemorySize / (1024L * 1024L)
                }
            }
        }

        totalMb = totalMb.coerceAtLeast(2048L)
        availMb = availMb.coerceAtLeast(512L)
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

    fun getHostDisplayName(): String = cachedHostDisplayName

    private fun resolveHostDisplayName(): String {
        val osName = System.getProperty("os.name") ?: DesktopPlatform.shortName
        val arch = System.getProperty("os.arch") ?: "x86_64"
        return when (DesktopPlatform.currentOs) {
            DesktopOs.LINUX -> {
                val distro = try {
                    val osRelease = File("/etc/os-release")
                    if (osRelease.exists()) {
                        osRelease.readLines()
                            .firstOrNull { it.startsWith("PRETTY_NAME=") }
                            ?.substringAfter("=")
                            ?.trim('"')
                    } else null
                } catch (_: Exception) { null }
                "${distro ?: osName} ($arch)"
            }
            DesktopOs.MACOS -> {
                val prod = runQuickCommand("sw_vers", "-productName")?.trim()?.takeIf { it.isNotBlank() } ?: "macOS"
                val ver = runQuickCommand("sw_vers", "-productVersion")?.trim()?.takeIf { it.isNotBlank() }
                if (ver != null) "$prod $ver ($arch)" else "$prod ($arch)"
            }
            DesktopOs.WINDOWS -> "$osName ($arch)"
        }
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
        return when (DesktopPlatform.currentOs) {
            DesktopOs.LINUX -> try {
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
            DesktopOs.MACOS -> {
                val out = runQuickCommand("ps", "-o", "rss=", "-p", pid.toString())?.trim() ?: return 0L
                val kb = out.toLongOrNull() ?: 0L
                kb / 1024L
            }
            DesktopOs.WINDOWS -> {
                val out = runQuickCommand("tasklist", "/FI", "PID eq $pid", "/FO", "CSV", "/NH") ?: return 0L
                val line = out.lines().firstOrNull { it.contains(",\"$pid\",") } ?: return 0L
                val cols = line.split("\",\"")
                val memCol = cols.lastOrNull() ?: return 0L
                val digits = memCol.filter { it.isDigit() }
                val kb = digits.toLongOrNull() ?: 0L
                kb / 1024L
            }
        }
    }

    fun getDefaultGatewayIp(): String? {
        return when (DesktopPlatform.currentOs) {
            DesktopOs.LINUX -> try {
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
            DesktopOs.MACOS -> {
                val out = runQuickCommand("route", "-n", "get", "default") ?: return null
                out.lines()
                    .firstOrNull { it.trim().startsWith("gateway:") }
                    ?.substringAfter("gateway:")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
            DesktopOs.WINDOWS -> {
                val out = runQuickCommand("route", "print", "0.0.0.0") ?: return null
                for (line in out.lines()) {
                    val cols = line.trim().split(whitespaceRegex)
                    if (cols.size >= 3 && cols[0] == "0.0.0.0" && cols[1] == "0.0.0.0" && cols[2].contains('.')) {
                        return cols[2]
                    }
                }
                null
            }
        }
    }

    fun getProcessCpuPercent(pid: Long): Float {
        if (pid <= 0L) return 0f
        if (DesktopPlatform.isLinux) {
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

        return try {
            val handle = ProcessHandle.of(pid).orElse(null) ?: return 0f
            val cpuDur = handle.info().totalCpuDuration().orElse(null) ?: return 0f
            val cpuNanos = cpuDur.toNanos()
            val now = System.currentTimeMillis()
            val prev = handleCpuPrev.put(pid, cpuNanos to now) ?: return 0f
            val deltaNanos = cpuNanos - prev.first
            val deltaMs = now - prev.second
            if (deltaMs <= 200L || deltaNanos < 0L) return 0f
            val pct = ((deltaNanos / 1_000_000f) / deltaMs.toFloat()) * 100f
            pct.coerceIn(0f, 100f)
        } catch (_: Exception) {
            0f
        }
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
