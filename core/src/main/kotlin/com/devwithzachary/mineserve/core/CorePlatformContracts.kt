package com.devwithzachary.mineserve.core

import java.io.File

/**
 * Cross-platform storage abstraction for `:core` components.
 */
interface PlatformStorage {
    fun getServersRootDir(): File
    fun getCacheRootDir(): File
    fun getFreeSpaceBytes(): Long
}

/**
 * Cross-platform key-value settings store abstraction.
 */
interface SettingsStore {
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String?)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
}

/**
 * Cross-platform command/process runtime executor abstraction (PRoot on Android, native ProcessBuilder on desktop).
 */
interface RuntimeExecutor {
    suspend fun execute(
        command: List<String>,
        workingDir: File,
        environment: Map<String, String> = emptyMap(),
        onOutputLine: (String) -> Unit = {}
    ): Int
}

/**
 * Cross-platform Java runtime provisioning abstraction.
 */
interface JavaProvisioner {
    fun isJavaInstalled(majorVersion: Int): Boolean
    fun getJavaBinaryPath(majorVersion: Int): String?
    suspend fun ensureJavaInstalled(majorVersion: Int, onProgress: (String) -> Unit = {}): Boolean
}

/**
 * Cross-platform network interface discovery abstraction.
 */
interface NetworkInterfaceProvider {
    fun getLocalIpv4Address(): String?
    fun getDefaultGatewayAddress(): String?
}

