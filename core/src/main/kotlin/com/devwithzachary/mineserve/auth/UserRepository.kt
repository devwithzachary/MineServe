package com.devwithzachary.mineserve.auth

import com.devwithzachary.mineserve.core.CoreLog
import java.io.File
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserRepository(
    private val dataDir: File
) {
    companion object {
        private const val TAG = "UserRepository"
        private const val USERS_FILE = "panel_users.json"
        private const val AUDIT_FILE = "panel_audit_log.json"
        private const val MAX_AUDIT_ENTRIES = 500
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_.-]{3,32}$")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val usersFile: File get() = File(dataDir, USERS_FILE)
    private val auditFile: File get() = File(dataDir, AUDIT_FILE)

    private val lock = Any()
    private var usersCache: MutableList<PanelUser> = mutableListOf()
    private var auditCache: MutableList<AuditLogEntry> = mutableListOf()

    init {
        synchronized(lock) {
            if (!dataDir.exists()) dataDir.mkdirs()
            loadUsersLocked()
            loadAuditLocked()
        }
    }

    /**
     * Ensures at least one Owner account exists, seamlessly migrating legacy single-admin credentials.
     */
    fun ensureDefaultOwner(username: String, password: String): PanelUser = synchronized(lock) {
        val cleanUser = username.trim().ifBlank { "admin" }
        val existingOwner = usersCache.firstOrNull { it.role == UserRole.OWNER }
        if (existingOwner != null) {
            return existingOwner
        }
        val owner = PanelUser(
            id = "owner-default",
            username = cleanUser,
            role = UserRole.OWNER,
            passwordHash = PasswordHasher.hash(password),
            restrictServers = false,
            allowedServerIds = emptyList(),
            enabled = true
        )
        usersCache.add(owner)
        saveUsersLocked()
        owner
    }

    /**
     * Syncs the default owner account when the user edits primary admin credentials in local app settings.
     */
    fun syncPrimaryOwnerCredentials(username: String, password: String) {
        synchronized(lock) {
            val cleanUser = username.trim().ifBlank { "admin" }
            if (password.isBlank()) return
            val idx = usersCache.indexOfFirst { it.id == "owner-default" }
                .takeIf { it >= 0 }
                ?: usersCache.indexOfFirst { it.role == UserRole.OWNER }
            if (idx >= 0) {
                val current = usersCache[idx]
                val userChanged = current.username != cleanUser
                val passChanged = !PasswordHasher.verify(password, current.passwordHash)
                if (userChanged || passChanged) {
                    usersCache[idx] = current.copy(
                        username = cleanUser,
                        passwordHash = if (passChanged) PasswordHasher.hash(password) else current.passwordHash,
                        enabled = true
                    )
                    saveUsersLocked()
                }
            } else {
                ensureDefaultOwner(cleanUser, password)
            }
        }
    }

    fun authenticate(username: String, password: String): PanelUser? = synchronized(lock) {
        val user = usersCache.firstOrNull { it.username.equals(username.trim(), ignoreCase = true) }
            ?: return null
        if (!user.enabled) return null
        if (!PasswordHasher.verify(password, user.passwordHash)) return null
        val updated = user.copy(lastLoginAt = System.currentTimeMillis())
        val idx = usersCache.indexOfFirst { it.id == user.id }
        if (idx >= 0) {
            usersCache[idx] = updated
            saveUsersLocked()
        }
        updated
    }

    fun listUsers(): List<PanelUser> = synchronized(lock) {
        usersCache.toList()
    }

    fun getUser(userId: String): PanelUser? = synchronized(lock) {
        usersCache.firstOrNull { it.id == userId }
    }

    fun findByUsername(username: String): PanelUser? = synchronized(lock) {
        usersCache.firstOrNull { it.username.equals(username.trim(), ignoreCase = true) }
    }

    fun createUser(
        username: String,
        password: String,
        role: UserRole,
        restrictServers: Boolean = false,
        allowedServerIds: List<String> = emptyList()
    ): Result<PanelUser> = synchronized(lock) {
        val cleanUser = username.trim()
        if (!USERNAME_REGEX.matches(cleanUser)) {
            return Result.failure(IllegalArgumentException("Username must be 3-32 characters (letters, numbers, _, ., -)."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (usersCache.any { it.username.equals(cleanUser, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("A user named '$cleanUser' already exists."))
        }
        val effectiveRestrict = if (role == UserRole.OWNER) false else restrictServers
        val user = PanelUser(
            id = UUID.randomUUID().toString(),
            username = cleanUser,
            role = role,
            passwordHash = PasswordHasher.hash(password),
            restrictServers = effectiveRestrict,
            allowedServerIds = if (effectiveRestrict) allowedServerIds.distinct() else emptyList(),
            enabled = true
        )
        usersCache.add(user)
        saveUsersLocked()
        Result.success(user)
    }

    fun updateUser(
        userId: String,
        role: UserRole? = null,
        restrictServers: Boolean? = null,
        allowedServerIds: List<String>? = null,
        enabled: Boolean? = null,
        newPassword: String? = null
    ): Result<PanelUser> = synchronized(lock) {
        val idx = usersCache.indexOfFirst { it.id == userId }
        if (idx < 0) return Result.failure(IllegalArgumentException("User not found."))
        val existing = usersCache[idx]

        val nextRole = role ?: existing.role
        val nextEnabled = enabled ?: existing.enabled

        // Protect against removing or disabling the last enabled Owner
        if (existing.role == UserRole.OWNER && (nextRole != UserRole.OWNER || !nextEnabled)) {
            val otherEnabledOwners = usersCache.count { it.id != userId && it.role == UserRole.OWNER && it.enabled }
            if (otherEnabledOwners == 0) {
                return Result.failure(IllegalStateException("Cannot demote or disable the last active Owner account."))
            }
        }

        if (!newPassword.isNullOrEmpty() && newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }

        val nextRestrict = if (nextRole == UserRole.OWNER) false else (restrictServers ?: existing.restrictServers)
        val nextAllowed = if (nextRestrict) {
            (allowedServerIds ?: existing.allowedServerIds).distinct()
        } else {
            emptyList()
        }

        val updated = existing.copy(
            role = nextRole,
            restrictServers = nextRestrict,
            allowedServerIds = nextAllowed,
            enabled = nextEnabled,
            passwordHash = if (!newPassword.isNullOrEmpty()) PasswordHasher.hash(newPassword) else existing.passwordHash
        )
        usersCache[idx] = updated
        saveUsersLocked()
        Result.success(updated)
    }

    fun changeOwnPassword(userId: String, currentPassword: String, newPassword: String): Result<Unit> = synchronized(lock) {
        val idx = usersCache.indexOfFirst { it.id == userId }
        if (idx < 0) return Result.failure(IllegalArgumentException("User not found."))
        val existing = usersCache[idx]
        if (!PasswordHasher.verify(currentPassword, existing.passwordHash)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        usersCache[idx] = existing.copy(passwordHash = PasswordHasher.hash(newPassword))
        saveUsersLocked()
        Result.success(Unit)
    }

    fun deleteUser(userId: String): Result<Unit> = synchronized(lock) {
        val existing = usersCache.firstOrNull { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))
        if (existing.role == UserRole.OWNER) {
            val otherEnabledOwners = usersCache.count { it.id != userId && it.role == UserRole.OWNER && it.enabled }
            if (otherEnabledOwners == 0) {
                return Result.failure(IllegalStateException("Cannot delete the last active Owner account."))
            }
        }
        usersCache.removeAll { it.id == userId }
        saveUsersLocked()
        Result.success(Unit)
    }

    fun canAccessServer(user: PanelUser, serverId: String): Boolean {
        if (!user.enabled) return false
        if (user.role == UserRole.OWNER || !user.restrictServers) return true
        return user.allowedServerIds.contains(serverId)
    }

    fun permissionsFor(role: UserRole): Set<Permission> {
        return when (role) {
            UserRole.OWNER -> Permission.entries.toSet()
            UserRole.ADMIN -> setOf(
                Permission.VIEW_AUDIT_LOG,
                Permission.SERVER_VIEW,
                Permission.SERVER_CONTROL,
                Permission.SERVER_CONSOLE_WRITE,
                Permission.SERVER_PLAYERS,
                Permission.SERVER_BACKUPS_CREATE,
                Permission.SERVER_BACKUPS_MANAGE,
                Permission.SERVER_FILES_WRITE,
                Permission.SERVER_SETTINGS_WRITE
            )
            UserRole.MODERATOR -> setOf(
                Permission.SERVER_VIEW,
                Permission.SERVER_CONTROL,
                Permission.SERVER_CONSOLE_WRITE,
                Permission.SERVER_PLAYERS,
                Permission.SERVER_BACKUPS_CREATE
            )
            UserRole.VIEWER -> setOf(
                Permission.SERVER_VIEW
            )
        }
    }

    fun hasPermission(user: PanelUser, permission: Permission, serverId: String? = null): Boolean {
        if (!user.enabled) return false
        if (!permissionsFor(user.role).contains(permission)) return false
        if (serverId != null && !canAccessServer(user, serverId)) return false
        return true
    }

    fun recordAudit(
        user: PanelUser?,
        action: String,
        target: String = "",
        details: String = "",
        clientIp: String = ""
    ) = synchronized(lock) {
        val entry = AuditLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            userId = user?.id.orEmpty(),
            username = user?.username ?: "system",
            role = user?.role?.name ?: "SYSTEM",
            action = action,
            target = target.take(120),
            details = details.take(300),
            clientIp = clientIp.take(64)
        )
        auditCache.add(0, entry)
        if (auditCache.size > MAX_AUDIT_ENTRIES) {
            auditCache = auditCache.take(MAX_AUDIT_ENTRIES).toMutableList()
        }
        saveAuditLocked()
    }

    fun getAuditLog(limit: Int = 100): List<AuditLogEntry> = synchronized(lock) {
        auditCache.take(limit.coerceIn(1, MAX_AUDIT_ENTRIES))
    }

    private fun loadUsersLocked() {
        try {
            if (usersFile.exists()) {
                val parsed = json.decodeFromString<List<PanelUser>>(usersFile.readText())
                usersCache = parsed.toMutableList()
            }
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed to load panel_users.json", e)
            usersCache = mutableListOf()
        }
    }

    private fun saveUsersLocked() {
        try {
            if (!dataDir.exists()) dataDir.mkdirs()
            val tmp = File(dataDir, "$USERS_FILE.tmp")
            tmp.writeText(json.encodeToString(usersCache))
            if (!tmp.renameTo(usersFile)) {
                tmp.copyTo(usersFile, overwrite = true)
                tmp.delete()
            }
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed to save panel_users.json", e)
        }
    }

    private fun loadAuditLocked() {
        try {
            if (auditFile.exists()) {
                val parsed = json.decodeFromString<List<AuditLogEntry>>(auditFile.readText())
                auditCache = parsed.toMutableList()
            }
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed to load panel_audit_log.json", e)
            auditCache = mutableListOf()
        }
    }

    private fun saveAuditLocked() {
        try {
            if (!dataDir.exists()) dataDir.mkdirs()
            val tmp = File(dataDir, "$AUDIT_FILE.tmp")
            tmp.writeText(json.encodeToString(auditCache))
            if (!tmp.renameTo(auditFile)) {
                tmp.copyTo(auditFile, overwrite = true)
                tmp.delete()
            }
        } catch (e: Exception) {
            CoreLog.e(TAG, "Failed to save panel_audit_log.json", e)
        }
    }
}
