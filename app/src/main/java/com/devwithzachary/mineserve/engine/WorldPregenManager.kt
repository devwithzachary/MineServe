package com.devwithzachary.mineserve.engine

import com.devwithzachary.mineserve.model.DimensionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class PregenState(
    val isRunning: Boolean = false,
    val dimension: DimensionType = DimensionType.OVERWORLD,
    val radiusBlocks: Int = 500,
    val completedBatches: Int = 0,
    val totalBatches: Int = 0,
    val percent: Int = 0,
    val usingChunky: Boolean = false,
    val statusMessage: String = "Idle"
)

/**
 * Coordinates chunk pre-generation on a running Minecraft server via Chunky (if installed)
 * or native `/forceload` batch stepping so exploration lag is eliminated.
 */
object WorldPregenManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val _states = MutableStateFlow<Map<String, PregenState>>(emptyMap())
    val states: StateFlow<Map<String, PregenState>> = _states.asStateFlow()

    fun getState(serverId: String): PregenState = _states.value[serverId] ?: PregenState()

    fun hasChunkyInstalled(serverDir: File): Boolean {
        val dirs = listOf(File(serverDir, "plugins"), File(serverDir, "mods"))
        return dirs.any { dir ->
            dir.isDirectory && (dir.listFiles()?.any {
                it.isFile && it.name.contains("chunky", ignoreCase = true) && it.name.endsWith(".jar", ignoreCase = true)
            } == true)
        }
    }

    fun startPregen(
        serverId: String,
        serverDir: File,
        dimension: DimensionType,
        centerX: Int,
        centerZ: Int,
        radiusBlocks: Int,
        sendCommand: (String) -> Unit
    ) {
        jobs[serverId]?.cancel()
        val clampedRadius = radiusBlocks.coerceIn(64, 5000)
        val dimKey = when (dimension) {
            DimensionType.OVERWORLD -> "minecraft:overworld"
            DimensionType.NETHER -> "minecraft:the_nether"
            DimensionType.THE_END -> "minecraft:the_end"
        }

        if (hasChunkyInstalled(serverDir)) {
            sendCommand("chunky world $dimKey")
            sendCommand("chunky center $centerX $centerZ")
            sendCommand("chunky radius $clampedRadius")
            sendCommand("chunky start")
            updateState(
                serverId,
                PregenState(
                    isRunning = true,
                    dimension = dimension,
                    radiusBlocks = clampedRadius,
                    completedBatches = 0,
                    totalBatches = 1,
                    percent = 10,
                    usingChunky = true,
                    statusMessage = "Chunky pre-generation started (${clampedRadius}m radius). Monitor progress in Console."
                )
            )
            return
        }

        val step = 64
        val tiles = mutableListOf<Pair<Int, Int>>()
        var x = centerX - clampedRadius
        while (x < centerX + clampedRadius) {
            var z = centerZ - clampedRadius
            while (z < centerZ + clampedRadius) {
                tiles.add(x to z)
                z += step
            }
            x += step
        }
        val total = tiles.size.coerceAtLeast(1)

        updateState(
            serverId,
            PregenState(
                isRunning = true,
                dimension = dimension,
                radiusBlocks = clampedRadius,
                completedBatches = 0,
                totalBatches = total,
                percent = 0,
                usingChunky = false,
                statusMessage = "Starting native chunk pre-generation ($total batches)..."
            )
        )

        jobs[serverId] = scope.launch {
            try {
                for ((idx, tile) in tiles.withIndex()) {
                    if (!isActive) break
                    val (bx, bz) = tile
                    val bx2 = bx + step - 1
                    val bz2 = bz + step - 1
                    sendCommand("execute in $dimKey run forceload add $bx $bz $bx2 $bz2")
                    delay(1800)
                    sendCommand("execute in $dimKey run forceload remove all")

                    val done = idx + 1
                    val pct = ((done * 100) / total).coerceIn(1, 100)
                    updateState(
                        serverId,
                        PregenState(
                            isRunning = done < total,
                            dimension = dimension,
                            radiusBlocks = clampedRadius,
                            completedBatches = done,
                            totalBatches = total,
                            percent = pct,
                            usingChunky = false,
                            statusMessage = if (done < total) {
                                "Generated batch $done/$total at ($bx, $bz)..."
                            } else {
                                "Pre-generation complete ($total batches)."
                            }
                        )
                    )
                }
                sendCommand("save-all")
            } finally {
                sendCommand("execute in $dimKey run forceload remove all")
            }
        }
    }

    fun stopPregen(serverId: String, sendCommand: (String) -> Unit) {
        val current = getState(serverId)
        jobs.remove(serverId)?.cancel()
        if (current.usingChunky) {
            sendCommand("chunky cancel")
        } else {
            val dimKey = when (current.dimension) {
                DimensionType.OVERWORLD -> "minecraft:overworld"
                DimensionType.NETHER -> "minecraft:the_nether"
                DimensionType.THE_END -> "minecraft:the_end"
            }
            sendCommand("execute in $dimKey run forceload remove all")
        }
        updateState(
            serverId,
            current.copy(
                isRunning = false,
                statusMessage = "Pre-generation stopped."
            )
        )
    }

    private fun updateState(serverId: String, state: PregenState) {
        val copy = _states.value.toMutableMap()
        copy[serverId] = state
        _states.value = copy
    }
}
