package com.devwithzachary.mineserve.modpack

import kotlinx.serialization.Serializable

@Serializable
data class ModpackIndex(
    val formatVersion: Int = 1,
    val game: String = "minecraft",
    val versionId: String = "",
    val name: String = "",
    val summary: String? = null,
    val files: List<ModpackFileEntry> = emptyList(),
    val dependencies: Map<String, String> = emptyMap()
)

@Serializable
data class ModpackFileEntry(
    val path: String,
    val hashes: Map<String, String> = emptyMap(),
    val env: ModpackEnv? = null,
    val downloads: List<String> = emptyList(),
    val fileSize: Long = 0L
) {
    val isServerCompatible: Boolean
        get() = env == null || env.server != "unsupported"
}

@Serializable
data class ModpackEnv(
    val client: String? = null,
    val server: String? = null
)

data class ModpackInstallProgress(
    val stage: String,
    val currentItem: String,
    val currentCount: Int,
    val totalCount: Int,
    val progress: Float
)

