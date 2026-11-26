package com.devwithzachary.mineserve.engine

import com.devwithzachary.mineserve.model.MinecraftServer
import java.util.concurrent.ConcurrentHashMap

class StandbyPingManager private constructor() {
    companion object {
        val instance by lazy { StandbyPingManager() }
    }

    private val listeners = ConcurrentHashMap<String, StandbyPingListener>()

    fun startStandby(
        server: MinecraftServer,
        onBindFailed: ((MinecraftServer, Exception) -> Unit)? = null,
        onWake: (MinecraftServer) -> Unit
    ) {
        stopStandby(server.id)
        val listener = StandbyPingListener(
            server = server,
            onWakeRequested = { wokeServer ->
                listeners.remove(wokeServer.id)
                onWake(wokeServer)
            },
            onBindFailed = { failedServer, e ->
                listeners.remove(failedServer.id)
                onBindFailed?.invoke(failedServer, e)
            }
        )
        listeners[server.id] = listener
        listener.start()
    }

    fun stopStandby(serverId: String) {
        listeners.remove(serverId)?.stop()
    }

    fun stopAll() {
        for ((_, listener) in listeners) {
            listener.stop()
        }
        listeners.clear()
    }

    fun isStandbyActive(serverId: String): Boolean {
        val listener = listeners[serverId] ?: return false
        return listener.isRunning()
    }

    fun getStandbyCount(): Int = listeners.size

    fun getStandbyPorts(): List<Int> = listeners.values.map { it.server.port }
}

