package com.devwithzachary.mineserve.model

import kotlinx.serialization.Serializable

@Serializable
data class MinecraftServer(
    val id: String,
    val name: String,
    val type: ServerType,
    val version: String,
    val port: Int = 25565,
    val allocatedRamMb: Int = 2048,
    val javaVersion: Int = 21,
    val status: ServerStatus = ServerStatus.STOPPED,
    val motd: String = "A MineServe Minecraft Server",
    val autoStart: Boolean = false,
    val eulaAccepted: Boolean = true,
    val jvmArgs: String = "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200",
    val createdAt: Long = System.currentTimeMillis(),
    val lastStartedAt: Long? = null,
    val jarFileName: String = "server.jar"
) {
    val isRunning: Boolean get() = status == ServerStatus.RUNNING || status == ServerStatus.STARTING
}

fun determineJavaVersion(version: String, type: ServerType = ServerType.PAPER): Int {
    val clean = version.trim().lowercase()
    if (clean == "26.2" || clean.startsWith("26.") || clean.startsWith("25.") || clean.startsWith("1.22")) {
        return 25
    }
    if (clean.startsWith("1.21") || clean.startsWith("1.20.5") || clean.startsWith("1.20.6")) {
        return 21
    }
    if (clean.startsWith("1.18") || clean.startsWith("1.19") || clean.startsWith("1.20") || clean.startsWith("1.17")) {
        return 17
    }
    return 8
}

fun compareMinecraftVersions(v1: String, v2: String): Int {
    if (v1 == v2) return 0
    val p1 = v1.split('-', '_', '+')[0].split('.').mapNotNull { it.toIntOrNull() }
    val p2 = v2.split('-', '_', '+')[0].split('.').mapNotNull { it.toIntOrNull() }

    val maxLen = maxOf(p1.size, p2.size)
    for (i in 0 until maxLen) {
        val num1 = p1.getOrElse(i) { 0 }
        val num2 = p2.getOrElse(i) { 0 }
        if (num1 != num2) {
            return num1.compareTo(num2)
        }
    }
    return v1.compareTo(v2)
}

fun List<String>.sortedMinecraftVersionsDescending(): List<String> {
    return this.distinct().sortedWith { a, b -> compareMinecraftVersions(b, a) }
}

