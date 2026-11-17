package com.devwithzachary.mineserve.api

import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String? = null,
    val isNewer: Boolean = false
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: GitHubRelease) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val latestTag: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
