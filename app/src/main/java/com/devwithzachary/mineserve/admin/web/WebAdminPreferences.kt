package com.devwithzachary.mineserve.admin.web

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

class WebAdminPreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "mineserve_web_admin_prefs"
        private const val KEY_ENABLED = "web_admin_enabled"
        private const val KEY_PORT = "web_admin_port"
        private const val KEY_USERNAME = "web_admin_username"
        private const val KEY_PASSWORD = "web_admin_password"
        private const val KEY_EXPOSURE = "web_admin_exposure"
        private const val KEY_BORE_HOST = "web_admin_bore_host"
        private const val KEY_BORE_PORT = "web_admin_bore_port"
        private const val KEY_PLAYIT_SECRET = "web_admin_playit_secret"

        fun generateRandomPassword(length: Int = 12): String {
            val chars = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val random = SecureRandom()
            return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConfig(): WebAdminConfig {
        var pwd = prefs.getString(KEY_PASSWORD, null)
        if (pwd.isNullOrBlank()) {
            pwd = generateRandomPassword()
            prefs.edit().putString(KEY_PASSWORD, pwd).apply()
        }

        val exposureName = prefs.getString(KEY_EXPOSURE, WebAdminExposure.LAN_ONLY.name) ?: WebAdminExposure.LAN_ONLY.name
        val exposure = try {
            WebAdminExposure.valueOf(exposureName)
        } catch (_: Exception) {
            WebAdminExposure.LAN_ONLY
        }

        return WebAdminConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            port = prefs.getInt(KEY_PORT, 8080),
            username = prefs.getString(KEY_USERNAME, "admin") ?: "admin",
            password = pwd,
            exposure = exposure,
            customBoreHost = prefs.getString(KEY_BORE_HOST, "bore.pub") ?: "bore.pub",
            customBorePort = prefs.getInt(KEY_BORE_PORT, 7835),
            playitSecret = prefs.getString(KEY_PLAYIT_SECRET, "") ?: ""
        )
    }

    fun saveConfig(config: WebAdminConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_PASSWORD, config.password)
            .putString(KEY_EXPOSURE, config.exposure.name)
            .putString(KEY_BORE_HOST, config.customBoreHost)
            .putInt(KEY_BORE_PORT, config.customBorePort)
            .putString(KEY_PLAYIT_SECRET, config.playitSecret)
            .apply()
    }
}

