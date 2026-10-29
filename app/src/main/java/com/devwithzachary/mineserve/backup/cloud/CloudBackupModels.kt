package com.devwithzachary.mineserve.backup.cloud

import kotlinx.serialization.Serializable

@Serializable
enum class CloudProviderType(val displayName: String) {
    WEBDAV("Nextcloud / WebDAV"),
    SAF_FOLDER("Android Cloud / Storage Folder")
}

@Serializable
data class CloudBackupConfig(
    val enabled: Boolean = false,
    val provider: CloudProviderType = CloudProviderType.WEBDAV,
    val webDavUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val webDavRemotePath: String = "/MineServe/Backups",
    val safFolderUri: String? = null,
    val autoSyncOnBackup: Boolean = false
)

sealed class CloudSyncState {
    data object Idle : CloudSyncState()

    data class Syncing(
        val backupName: String,
        val progressPercent: Int = 0
    ) : CloudSyncState()

    data class Success(
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CloudSyncState()

    data class Error(
        val errorMessage: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CloudSyncState()

    val isSyncing: Boolean get() = this is Syncing
}

