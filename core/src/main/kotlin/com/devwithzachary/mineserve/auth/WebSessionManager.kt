package com.devwithzachary.mineserve.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

data class WebAuthSession(
    val id: String,
    val token: String,
    val userId: String,
    val username: String,
    val role: UserRole = UserRole.OWNER,
    val clientIp: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
)

/**
 * Shared Web Admin session & IP brute-force lockout manager used by both Android and Linux servers.
 */
open class WebSessionManager {
    private val activeSessions = ConcurrentHashMap<String, WebAuthSession>()
    private val failedAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val secureRandom = SecureRandom()

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L
        private const val ATTEMPT_WINDOW_MS = 5 * 60 * 1000L
    }

    fun isIpLockedOut(clientIp: String): Boolean {
        cleanOldAttempts(clientIp)
        val attempts = failedAttempts[clientIp] ?: return false
        synchronized(attempts) {
            if (attempts.size >= MAX_FAILED_ATTEMPTS) {
                val lastAttempt = attempts.lastOrNull() ?: 0L
                return System.currentTimeMillis() - lastAttempt < LOCKOUT_DURATION_MS
            }
        }
        return false
    }

    fun recordFailedAttempt(clientIp: String) {
        cleanOldAttempts(clientIp)
        val list = failedAttempts.getOrPut(clientIp) { mutableListOf() }
        synchronized(list) {
            list.add(System.currentTimeMillis())
        }
    }

    fun resetFailedAttempts(clientIp: String) {
        failedAttempts.remove(clientIp)
    }

    private fun cleanOldAttempts(clientIp: String) {
        val list = failedAttempts[clientIp] ?: return
        val now = System.currentTimeMillis()
        synchronized(list) {
            list.removeAll { now - it > ATTEMPT_WINDOW_MS + LOCKOUT_DURATION_MS }
        }
    }

    fun verifyCredentials(inputUser: String, inputPass: String, expectedUser: String, expectedPass: String): Boolean {
        val userMatch = constantTimeEquals(inputUser, expectedUser)
        val passMatch = constantTimeEquals(inputPass, expectedPass)
        return userMatch && passMatch
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }

    fun createSession(
        userId: String,
        username: String,
        role: UserRole,
        clientIp: String
    ): WebAuthSession {
        cleanExpiredSessions()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        val session = WebAuthSession(
            id = token.take(12),
            token = token,
            userId = userId,
            username = username,
            role = role,
            clientIp = clientIp
        )
        activeSessions[token] = session
        return session
    }

    fun validateSession(token: String?): WebAuthSession? {
        if (token.isNullOrBlank()) return null
        val session = activeSessions[token] ?: return null
        if (System.currentTimeMillis() > session.expiresAt) {
            activeSessions.remove(token)
            return null
        }
        return session
    }

    fun invalidateSession(token: String?) {
        if (!token.isNullOrBlank()) {
            activeSessions.remove(token)
        }
    }

    fun revokeSessionById(sessionId: String): Boolean {
        val match = activeSessions.values.firstOrNull { it.id == sessionId } ?: return false
        return activeSessions.remove(match.token) != null
    }

    fun revokeUserSessions(userId: String) {
        val tokens = activeSessions.values.filter { it.userId == userId }.map { it.token }
        for (t in tokens) {
            activeSessions.remove(t)
        }
    }

    fun listSessions(): List<WebAuthSession> {
        cleanExpiredSessions()
        return activeSessions.values.sortedByDescending { it.createdAt }
    }

    fun getActiveSessionsCount(): Int {
        cleanExpiredSessions()
        return activeSessions.size
    }

    private fun cleanExpiredSessions() {
        val now = System.currentTimeMillis()
        val expired = activeSessions.filter { it.value.expiresAt < now }.keys
        for (k in expired) {
            activeSessions.remove(k)
        }
    }
}
