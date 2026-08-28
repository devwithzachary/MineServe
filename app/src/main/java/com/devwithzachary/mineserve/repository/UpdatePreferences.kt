package com.devwithzachary.mineserve.repository

import android.content.Context
import android.content.SharedPreferences

class UpdatePreferences(context: Context) {
    companion object {
        private const val PREFS_NAME = "mineserve_update_prefs"
        private const val KEY_CHECK_GITHUB_UPDATES = "check_github_updates"
        private const val KEY_IGNORED_VERSION = "ignored_version"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isCheckGitHubUpdatesEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHECK_GITHUB_UPDATES, true)
        set(value) = prefs.edit().putBoolean(KEY_CHECK_GITHUB_UPDATES, value).apply()

    var ignoredVersion: String
        get() = prefs.getString(KEY_IGNORED_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IGNORED_VERSION, value).apply()

    var lastCheckTime: Long
        get() = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK_TIME, value).apply()

    fun ignoreVersion(versionTag: String) {
        ignoredVersion = versionTag
    }

    fun clearIgnoredVersion() {
        ignoredVersion = ""
    }
}
