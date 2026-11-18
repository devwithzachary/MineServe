package com.devwithzachary.mineserve.repository

import android.content.Context
import android.content.SharedPreferences

class UpdatePreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "mineserve_update_prefs"
        private const val KEY_CHECK_GITHUB_UPDATES = "check_github_updates"
        private const val KEY_IGNORED_VERSION = "ignored_version"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_SHOW_WHATS_NEW_ON_UPDATE = "show_whats_new_on_update"
        private const val KEY_LAST_SEEN_WHATS_NEW_VERSION = "last_seen_whats_new_version"

        /**
         * Extracts the major (1.x) release key from a full version string (e.g. "1.7.0" -> "1.7").
         * Minor/patch bug-fix updates (e.g. "1.7.1") share the same "1.7" key so the What's New
         * screen only triggers once per major (1.x) feature release.
         */
        fun extractMajorReleaseKey(versionName: String): String {
            val clean = versionName.trim().removePrefix("v")
            val parts = clean.split(".")
            return if (parts.size >= 2) "${parts[0]}.${parts[1]}" else clean
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isCheckGitHubUpdatesEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHECK_GITHUB_UPDATES, false)
        set(value) = prefs.edit().putBoolean(KEY_CHECK_GITHUB_UPDATES, value).apply()

    var ignoredVersion: String
        get() = prefs.getString(KEY_IGNORED_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IGNORED_VERSION, value).apply()

    var lastCheckTime: Long
        get() = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK_TIME, value).apply()

    var isShowWhatsNewOnUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WHATS_NEW_ON_UPDATE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WHATS_NEW_ON_UPDATE, value).apply()

    var lastSeenWhatsNewVersion: String
        get() = prefs.getString(KEY_LAST_SEEN_WHATS_NEW_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SEEN_WHATS_NEW_VERSION, value).apply()

    fun shouldAutoShowWhatsNew(majorVersionKey: String): Boolean {
        return isShowWhatsNewOnUpdateEnabled && lastSeenWhatsNewVersion != majorVersionKey
    }
}

