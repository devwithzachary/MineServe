package com.devwithzachary.mineserve.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerMetrics(
    val serverId: String = "",
    val isRunning: Boolean = false,
    val cpuPercentage: Float = 0f,
    val ramUsedMb: Long = 0L,
    val ramMaxMb: Long = 2048L,
    val onlinePlayerCount: Int = 0,
    val onlinePlayers: List<String> = emptyList(),
    val tps: Double = 20.0,
    val mspt: Double = 20.0,
    val lagWarningsCount: Int = 0,
    val uptimeSeconds: Long = 0L,
    val pid: Int = -1
)

@Serializable
data class BackupEntry(
    val id: String,
    val serverId: String,
    val name: String,
    val fileName: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val isWorldOnly: Boolean
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            }
        }
}

@Serializable
data class PluginModEntry(
    val id: String,
    val fileName: String,
    val name: String,
    val version: String = "",
    val description: String = "",
    val author: String = "",
    val enabled: Boolean = true,
    val fileSizeBytes: Long = 0L,
    val isMod: Boolean = false,
    val iconUrl: String? = null,
    val downloads: Int = 0,
    val categories: List<String> = emptyList(),
    val downloadUrl: String? = null,
    val slug: String = ""
) {
    val formattedSize: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
            } else if (mb >= 0.1) {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            } else {
                val kb = fileSizeBytes / 1024.0
                String.format(java.util.Locale.US, "%.1f KB", kb)
            }
        }
}
