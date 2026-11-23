package com.devwithzachary.mineserve.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2_sha256"
    private const val DEFAULT_ITERATIONS = 100_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private val secureRandom = SecureRandom()

    fun hash(password: String, iterations: Int = DEFAULT_ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = deriveKey(password, salt, iterations)
        return "$PREFIX$$iterations$${salt.toHex()}$${derived.toHex()}"
    }

    fun verify(password: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        val parts = storedHash.split("$")
        if (parts.size != 4 || parts[0] != PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations < 1_000 || iterations > 1_000_000) return false
        val salt = hexToBytes(parts[2]) ?: return false
        val expected = hexToBytes(parts[3]) ?: return false
        val actual = deriveKey(password, salt, iterations)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { idx ->
                hex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }
}
