package com.devwithzachary.mineserve.backup.cloud

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class CloudBackupManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "CloudBackupManager"

        @Volatile
        private var INSTANCE: CloudBackupManager? = null

        fun getInstance(context: Context): CloudBackupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudBackupManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val webDavClient = WebDavClient()
    private val safStorage = SafBackupStorage(context)

    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    suspend fun testWebDavConnection(config: CloudBackupConfig): Result<String> {
        return webDavClient.testConnection(config)
    }

    fun syncBackupAsync(
        backupFile: File,
        config: CloudBackupConfig,
        onComplete: ((Result<String>) -> Unit)? = null
    ) {
        if (!config.enabled) {
            val err = "Cloud backup sync is not enabled for this server."
            _syncState.value = CloudSyncState.Error(err)
            onComplete?.invoke(Result.failure(IllegalStateException(err)))
            return
        }

        _syncState.value = CloudSyncState.Syncing(backupFile.name, 0)
        scope.launch {
            val result = when (config.provider) {
                CloudProviderType.WEBDAV -> {
                    webDavClient.uploadBackup(backupFile, config) { pct ->
                        _syncState.value = CloudSyncState.Syncing(backupFile.name, pct)
                    }
                }
                CloudProviderType.SAF_FOLDER -> {
                    val uri = config.safFolderUri
                    if (uri.isNullOrBlank()) {
                        Result.failure(IllegalArgumentException("No destination storage folder selected."))
                    } else {
                        safStorage.saveBackupToSaf(backupFile, uri) { pct ->
                            _syncState.value = CloudSyncState.Syncing(backupFile.name, pct)
                        }
                    }
                }
            }

            result.fold(
                onSuccess = { msg ->
                    _syncState.value = CloudSyncState.Success(msg)
                    onComplete?.invoke(Result.success(msg))
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Unknown cloud sync error"
                    _syncState.value = CloudSyncState.Error(errMsg)
                    onComplete?.invoke(Result.failure(err))
                }
            )
        }
    }

    fun resetState() {
        _syncState.value = CloudSyncState.Idle
    }
}

