package com.devwithzachary.mineserve.model

import kotlinx.serialization.Serializable
import java.security.SecureRandom

/**
 * A backend server route registered on a Velocity or BungeeCord proxy.
 */
@Serializable
data class ProxyRoute(
    val alias: String,
    val targetServerId: String,
    val host: String = "127.0.0.1",
    val port: Int = 25566
)

/**
 * Proxy network configuration for Velocity and BungeeCord servers.
 */
@Serializable
data class ProxyServerConfig(
    val routes: List<ProxyRoute> = emptyList(),
    val tryOrder: List<String> = emptyList(),
    val forwardingSecret: String = "",
    val onlineMode: Boolean = true
) {
    companion object {
        private val CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private val RANDOM = SecureRandom()

        fun generateSecret(length: Int = 12): String =
            (1..length).map { CHARS[RANDOM.nextInt(CHARS.length)] }.joinToString("")
    }
}

