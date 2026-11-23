package com.devwithzachary.mineserve.auth

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole(val displayName: String, val level: Int) {
    OWNER("Owner", 100),
    ADMIN("Admin", 75),
    MODERATOR("Moderator", 50),
    VIEWER("Viewer", 10);

    companion object {
        fun fromString(raw: String?): UserRole {
            return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: VIEWER
        }
    }
}

enum class Permission {
    MANAGE_USERS,
    MANAGE_HOST,
    VIEW_AUDIT_LOG,
    SERVER_VIEW,
    SERVER_CONTROL,
    SERVER_CONSOLE_WRITE,
    SERVER_PLAYERS,
    SERVER_BACKUPS_CREATE,
    SERVER_BACKUPS_MANAGE,
    SERVER_FILES_WRITE,
    SERVER_SETTINGS_WRITE
}

@Serializable
data class PanelUser(
    val id: String,
    val username: String,
    val role: UserRole,
    val passwordHash: String,
    val restrictServers: Boolean = false,
    val allowedServerIds: List<String> = emptyList(),
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null
)

@Serializable
data class AuditLogEntry(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val username: String,
    val role: String = "",
    val action: String,
    val target: String = "",
    val details: String = "",
    val clientIp: String = ""
)
