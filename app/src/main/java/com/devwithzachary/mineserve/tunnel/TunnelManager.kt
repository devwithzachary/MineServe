package com.devwithzachary.mineserve.tunnel

import android.content.Context
import android.util.Log
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.TunnelProvider
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TunnelManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "TunnelManager"

        @Volatile
        private var INSTANCE: TunnelManager? = null

        fun getInstance(context: Context): TunnelManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TunnelManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeBoreClients = ConcurrentHashMap<String, BoreTunnelClient>()
    private val activePlayitClients = ConcurrentHashMap<String, PlayitTunnelClient>()

    private val _tunnelStates = MutableStateFlow<Map<String, TunnelState>>(emptyMap())
    val tunnelStates: StateFlow<Map<String, TunnelState>> = _tunnelStates.asStateFlow()

    fun getTunnelState(serverId: String): TunnelState {
        return _tunnelStates.value[serverId] ?: TunnelState.Disconnected
    }

    fun isTunnelActive(serverId: String): Boolean {
        return getTunnelState(serverId) is TunnelState.Connected
    }

    fun startTunnel(server: MinecraftServer) {
        startCustomTunnel(server.id, server.port, server.tunnelConfig, server.name)
    }

    fun startCustomTunnel(
        tunnelId: String,
        localPort: Int,
        config: com.devwithzachary.mineserve.model.TunnelConfig,
        label: String = tunnelId
    ) {
        stopTunnel(tunnelId)
        Log.i(TAG, "Starting public tunnel for $label ($tunnelId) on port $localPort using ${config.provider}")

        when (config.provider) {
            TunnelProvider.PLAYIT -> {
                lateinit var client: PlayitTunnelClient
                client = PlayitTunnelClient(
                    context = context,
                    localPort = localPort,
                    secret = config.playitSecret,
                    onStateChanged = { newState ->
                        if (activePlayitClients[tunnelId] === client || newState is TunnelState.Disconnected) {
                            updateTunnelState(tunnelId, newState)
                        }
                    }
                )
                activePlayitClients[tunnelId] = client
                client.start(scope)
            }
            TunnelProvider.BORE, TunnelProvider.CUSTOM_BORE -> {
                val relayHost = if (config.provider == TunnelProvider.CUSTOM_BORE && config.customRelayHost.isNotBlank()) {
                    config.customRelayHost.trim()
                } else {
                    "bore.pub"
                }
                val relayPort = if (config.provider == TunnelProvider.CUSTOM_BORE && config.customRelayPort > 0) {
                    config.customRelayPort
                } else {
                    7835
                }

                lateinit var client: BoreTunnelClient
                client = BoreTunnelClient(
                    context = context,
                    relayHost = relayHost,
                    relayPort = relayPort,
                    localPort = localPort,
                    provider = config.provider,
                    onStateChanged = { newState ->
                        if (activeBoreClients[tunnelId] === client || newState is TunnelState.Disconnected) {
                            updateTunnelState(tunnelId, newState)
                        }
                    }
                )
                activeBoreClients[tunnelId] = client
                client.start(scope)
            }
        }
    }

    fun stopTunnel(serverId: String) {
        val boreClient = activeBoreClients.remove(serverId)
        boreClient?.stop()

        val playitClient = activePlayitClients.remove(serverId)
        playitClient?.stop()

        updateTunnelState(serverId, TunnelState.Disconnected)
    }

    fun toggleTunnel(server: MinecraftServer) {
        val currentState = getTunnelState(server.id)
        if (currentState is TunnelState.Connected || currentState is TunnelState.Connecting) {
            stopTunnel(server.id)
        } else {
            startTunnel(server)
        }
    }

    fun stopAllTunnels() {
        for ((_, client) in activeBoreClients) {
            client.stop()
        }
        activeBoreClients.clear()

        for ((_, client) in activePlayitClients) {
            client.stop()
        }
        activePlayitClients.clear()

        _tunnelStates.value = emptyMap()
    }

    private fun updateTunnelState(serverId: String, state: TunnelState) {
        val map = _tunnelStates.value.toMutableMap()
        if (state is TunnelState.Disconnected) {
            map.remove(serverId)
        } else {
            map[serverId] = state
        }
        _tunnelStates.value = map
    }
}
