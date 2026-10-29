package com.devwithzachary.mineserve.admin.web

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

data class AdminSession(
    val token: String,
    val username: String,
    val clientIp: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
)

class WebAdminSecurity {
    private val activeSessions = ConcurrentHashMap<String, AdminSession>()
    private val failedAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val secureRandom = SecureRandom()

    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val ATTEMPT_WINDOW_MS = 5 * 60 * 1000L // 5 minutes
    }

    fun isIpLockedOut(clientIp: String): Boolean {
        cleanOldAttempts(clientIp)
        val attempts = failedAttempts[clientIp] ?: return false
        synchronized(attempts) {
            if (attempts.size >= MAX_FAILED_ATTEMPTS) {
                val lastAttempt = attempts.lastOrNull() ?: 0L
                val timePassed = System.currentTimeMillis() - lastAttempt
                return timePassed < LOCKOUT_DURATION_MS
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
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    fun createSession(username: String, clientIp: String): AdminSession {
        cleanExpiredSessions()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        val session = AdminSession(token = token, username = username, clientIp = clientIp)
        activeSessions[token] = session
        return session
    }

    fun validateSession(token: String?): AdminSession? {
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

