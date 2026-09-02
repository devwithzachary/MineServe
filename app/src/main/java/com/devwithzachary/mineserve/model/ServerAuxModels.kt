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

@Serializable
data class FileEntry(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val extension: String = "",
    val isEditable: Boolean = false,
    val isLog: Boolean = false,
    val isArchive: Boolean = false,
    val isWorldRegion: Boolean = false
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            val mb = sizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
            } else if (mb >= 0.1) {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            } else {
                val kb = sizeBytes / 1024.0
                String.format(java.util.Locale.US, "%.1f KB", kb)
            }
        }
}

enum class CrashSeverity {
    CRITICAL,
    WARNING,
    INFO
}

enum class CrashIssueType {
    INCOMPATIBLE_JAVA_VERSION,
    OUT_OF_MEMORY,
    MOD_CONFLICT_OR_MISSING_DEP,
    PORT_ALREADY_IN_USE,
    CORRUPTED_WORLD_CHUNK,
    EULA_NOT_ACCEPTED,
    SYNTAX_ERROR_CONFIG,
    UNKNOWN_CRASH
}

enum class QuickFixType {
    ACCEPT_EULA,
    CHANGE_JAVA_VERSION,
    INCREASE_RAM,
    CHANGE_PORT,
    DELETE_FILE,
    OPEN_FILE_EDITOR
}

@Serializable
data class QuickFixAction(
    val label: String,
    val description: String,
    val actionType: QuickFixType,
    val payload: String = ""
)

@Serializable
data class CrashDiagnosticReport(
    val title: String,
    val severity: CrashSeverity,
    val issueType: CrashIssueType,
    val summary: String,
    val explanation: String,
    val suggestedFixes: List<QuickFixAction>,
    val logSnippet: String,
    val sourceFile: String,
    val timestamp: Long = System.currentTimeMillis()
)

