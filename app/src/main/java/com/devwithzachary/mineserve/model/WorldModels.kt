package com.devwithzachary.mineserve.model

import java.io.File

/**
 * Minecraft dimension types supported across Paper, Purpur, Fabric, NeoForge, and Vanilla.
 */
enum class DimensionType(
    val displayName: String,
    val folderName: String,
    val vanillaSubFolder: String
) {
    OVERWORLD("Overworld", "world", "world"),
    NETHER("The Nether", "world_nether", "DIM-1"),
    THE_END("The End", "world_the_end", "DIM1");

    fun getDimensionDir(serverDir: File, levelName: String = "world"): File {
        return when (this) {
            OVERWORLD -> {
                val custom = File(serverDir, levelName)
                if (custom.exists()) custom else File(serverDir, "world")
            }
            NETHER -> {
                // Check Paper/Purpur layout first
                val paperNether = File(serverDir, "${levelName}_nether")
                if (paperNether.exists()) return paperNether
                val defaultPaperNether = File(serverDir, "world_nether")
                if (defaultPaperNether.exists()) return defaultPaperNether

                // Vanilla/Fabric layout inside Overworld/DIM-1
                val overworld = OVERWORLD.getDimensionDir(serverDir, levelName)
                File(overworld, "DIM-1")
            }
            THE_END -> {
                // Check Paper/Purpur layout first
                val paperEnd = File(serverDir, "${levelName}_the_end")
                if (paperEnd.exists()) return paperEnd
                val defaultPaperEnd = File(serverDir, "world_the_end")
                if (defaultPaperEnd.exists()) return defaultPaperEnd

                // Vanilla/Fabric layout inside Overworld/DIM1
                val overworld = OVERWORLD.getDimensionDir(serverDir, levelName)
                File(overworld, "DIM1")
            }
        }
    }
}

/**
 * Information regarding a specific Minecraft dimension.
 */
data class WorldDimensionInfo(
    val dimension: DimensionType,
    val directory: File,
    val exists: Boolean,
    val sizeBytes: Long,
    val regionFilesCount: Int,
    val totalChunksCount: Int
)

/**
 * Summary of a server's entire world footprint.
 */
data class WorldSummary(
    val levelName: String,
    val totalSizeBytes: Long,
    val dimensions: List<WorldDimensionInfo>,
    val seed: String? = null,
    val lastModifiedTimestamp: Long = 0L
)

/**
 * Inhabited time thresholds for chunk pruning.
 */
enum class InhabitedTimeThreshold(
    val ticks: Long,
    val label: String,
    val description: String
) {
    UNTOUCHED(0L, "0s (Untouched)", "Deletes chunks generated without any player habitation (pass-through only)"),
    THIRTY_SECONDS(600L, "30s (Quick Sprint)", "Deletes chunks where players spent less than 30 seconds"),
    TWO_MINUTES(2400L, "2m (Brief Visit)", "Deletes chunks where players spent less than 2 minutes"),
    FIVE_MINUTES(6000L, "5m (Light Exploration)", "Deletes chunks explored for less than 5 minutes");
}

/**
 * Options for running the Chunk Pruning tool.
 */
data class ChunkPruneOptions(
    val threshold: InhabitedTimeThreshold = InhabitedTimeThreshold.UNTOUCHED,
    val pruneNether: Boolean = true,
    val pruneEnd: Boolean = true,
    val createBackup: Boolean = true
)

/**
 * Results returned after chunk pruning optimization finishes.
 */
data class ChunkPruneResult(
    val scannedRegions: Int,
    val scannedChunks: Int,
    val prunedChunks: Int,
    val deletedRegions: Int,
    val bytesBefore: Long,
    val bytesAfter: Long
) {
    val bytesFreed: Long get() = (bytesBefore - bytesAfter).coerceAtLeast(0L)
    val percentFreed: Int get() = if (bytesBefore > 0) ((bytesFreed.toDouble() / bytesBefore) * 100).toInt() else 0
}

/**
 * Supported Web Map plugin (Squaremap: lightweight, ultra-fast 2D map optimized for mobile hosting).
 */
enum class WebMapPluginType(
    val displayName: String,
    val defaultPort: Int,
    val modrinthSlug: String,
    val description: String
) {
    SQUAREMAP(
        displayName = "Squaremap",
        defaultPort = 8080,
        modrinthSlug = "squaremap",
        description = "Lightweight, ultra-fast 2D web map with real-time player markers and minimal CPU/RAM overhead, specifically tailored for mobile servers."
    );

    companion object {
        fun detectInstalled(serverDir: File): WebMapPluginType? {
            val pluginsDir = File(serverDir, "plugins")
            val modsDir = File(serverDir, "mods")
            val files = (pluginsDir.listFiles() ?: emptyArray()) + (modsDir.listFiles() ?: emptyArray())

            for (file in files) {
                val lower = file.name.lowercase()
                if (lower.contains("squaremap")) return SQUAREMAP
            }
            return null
        }

        fun findInstalledFile(serverDir: File): File? {
            val pluginsDir = File(serverDir, "plugins")
            val modsDir = File(serverDir, "mods")
            val files = (pluginsDir.listFiles() ?: emptyArray()) + (modsDir.listFiles() ?: emptyArray())
            return files.firstOrNull { it.name.lowercase().contains("squaremap") }
        }
    }
}

/**
 * Web map state for a server.
 */
data class WebMapState(
    val installedPlugin: WebMapPluginType? = null,
    val port: Int = 8080,
    val isServerRunning: Boolean = false
) {
    val webMapUrl: String get() = "http://127.0.0.1:$port"
}
