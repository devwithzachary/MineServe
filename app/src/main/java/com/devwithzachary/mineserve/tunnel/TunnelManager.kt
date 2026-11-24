package com.devwithzachary.mineserve.tunnel

import android.content.Context
import android.util.Log
import com.devwithzachary.mineserve.admin.web.WebAdminPreferences
import com.devwithzachary.mineserve.cloud.MineServeCloudManager
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.TunnelProvider
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private data class TunnelSpec(
        val localPort: Int,
        val provider: TunnelProvider,
        val customRelayHost: String,
        val customRelayPort: Int,
        val cloudSubdomain: String,
        val cloudRegion: String,
        val cloudDomain: String,
        val playitSecret: String
    )

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeBoreClients = ConcurrentHashMap<String, BoreTunnelClient>()
    private val activePanelBoreClients = ConcurrentHashMap<String, BoreTunnelClient>()
    private val activePlayitClients = ConcurrentHashMap<String, PlayitTunnelClient>()
    private val activeCloudJobs = ConcurrentHashMap<String, Job>()
    private val activeTunnelSpecs = ConcurrentHashMap<String, TunnelSpec>()
    private val mineServeCloudManager = MineServeCloudManager.getInstance(context)

    private val _tunnelStates = MutableStateFlow<Map<String, TunnelState>>(emptyMap())
    val tunnelStates: StateFlow<Map<String, TunnelState>> = _tunnelStates.asStateFlow()

    fun getTunnelState(serverId: String): TunnelState {
        return _tunnelStates.value[serverId] ?: TunnelState.Disconnected
    }

    fun isTunnelActive(serverId: String): Boolean {
        return getTunnelState(serverId) is TunnelState.Connected
    }

    fun isTunnelActiveOrConnecting(serverId: String): Boolean {
        val state = getTunnelState(serverId)
        return state is TunnelState.Connected || state is TunnelState.Connecting
    }

    fun startTunnel(server: MinecraftServer, forceRestart: Boolean = false) {
        startCustomTunnel(server.id, server.port, server.tunnelConfig, server.name, forceRestart)
    }

    fun startCustomTunnel(
        tunnelId: String,
        localPort: Int,
        config: com.devwithzachary.mineserve.model.TunnelConfig,
        label: String = tunnelId,
        forceRestart: Boolean = false
    ) {
        val spec = TunnelSpec(
            localPort = localPort,
            provider = config.provider,
            customRelayHost = config.customRelayHost.trim(),
            customRelayPort = config.customRelayPort,
            cloudSubdomain = config.cloudSubdomain.trim().lowercase(),
            cloudRegion = config.cloudRegion.trim().lowercase(),
            cloudDomain = config.cloudDomain.trim().lowercase(),
            playitSecret = config.playitSecret
        )
        val currentState = getTunnelState(tunnelId)
        if (!forceRestart &&
            (currentState is TunnelState.Connected || currentState is TunnelState.Connecting) &&
            activeTunnelSpecs[tunnelId] == spec
        ) {
            Log.i(TAG, "Tunnel for $label ($tunnelId) is already ${currentState::class.simpleName} with identical config; skipping reconnect.")
            return
        }

        stopTunnel(tunnelId)
        activeTunnelSpecs[tunnelId] = spec
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
            TunnelProvider.MINESERVE_CLOUD -> {
                val subdomain = config.cloudSubdomain.trim().lowercase()
                if (subdomain.isBlank()) {
                    updateTunnelState(
                        tunnelId,
                        TunnelState.Error("Enter a MineServe Cloud subdomain in Server Settings first.")
                    )
                    return
                }
                updateTunnelState(
                    tunnelId,
                    TunnelState.Connecting("Provisioning $subdomain.play.mineserve.app...")
                )
                val job = scope.launch {
                    mineServeCloudManager.refreshProfile()
                    val matchedInfo = mineServeCloudManager.state.value.profile
                        ?.activeSubdomainDetails
                        ?.firstOrNull { it.subdomain.equals(subdomain, ignoreCase = true) }
                    val effectiveRegion = matchedInfo?.region?.takeIf { it.isNotBlank() }
                        ?: config.cloudRegion.ifBlank { "us-east" }
                    val effectiveDomain = matchedInfo?.domain?.takeIf { it.isNotBlank() }
                        ?: config.cloudDomain.ifBlank { "play.mineserve.app" }

                    val allocResult = mineServeCloudManager.allocateCloudTunnel(
                        serverId = tunnelId,
                        subdomain = subdomain,
                        region = effectiveRegion,
                        protocol = "tcp",
                        domain = effectiveDomain
                    )
                    allocResult.fold(
                        onSuccess = { allocation ->
                            if (allocation.relayHost.isBlank() || allocation.relayHost == "198.51.100.10") {
                                updateTunnelState(
                                    tunnelId,
                                    TunnelState.Error(
                                        "Reserved ${allocation.hostname}, but no MineServe Cloud relay node is online in ${allocation.region} yet."
                                    )
                                )
                                return@fold
                            }
                            lateinit var client: BoreTunnelClient
                            client = BoreTunnelClient(
                                context = context,
                                relayHost = allocation.relayHost,
                                relayPort = 7835,
                                localPort = localPort,
                                provider = TunnelProvider.MINESERVE_CLOUD,
                                requestedRemotePort = allocation.relayPort,
                                secret = allocation.clientToken,
                                onStateChanged = { newState ->
                                    if (activeBoreClients[tunnelId] === client || newState is TunnelState.Disconnected) {
                                        val mapped = when (newState) {
                                            is TunnelState.Connected -> newState.copy(
                                                publicHost = allocation.hostname,
                                                fullAddress = allocation.hostname,
                                                provider = TunnelProvider.MINESERVE_CLOUD
                                            )
                                            else -> newState
                                        }
                                        updateTunnelState(tunnelId, mapped)
                                    }
                                }
                            )
                            activeBoreClients[tunnelId] = client
                            client.start(scope)

                            if (tunnelId != "web_admin" && allocation.relayPort in 20000..39999) {
                                val webAdminPort = WebAdminPreferences(context).loadConfig().port
                                val panelClient = BoreTunnelClient(
                                    context = context,
                                    relayHost = allocation.relayHost,
                                    relayPort = 7835,
                                    localPort = webAdminPort,
                                    provider = TunnelProvider.MINESERVE_CLOUD,
                                    requestedRemotePort = allocation.relayPort + 20000,
                                    secret = allocation.clientToken,
                                    onStateChanged = {}
                                )
                                activePanelBoreClients[tunnelId] = panelClient
                                panelClient.start(scope)
                            }
                        },
                        onFailure = { err ->
                            updateTunnelState(
                                tunnelId,
                                TunnelState.Error(err.message ?: "Failed to allocate MineServe Cloud tunnel")
                            )
                        }
                    )
                }
                activeCloudJobs[tunnelId] = job
            }
        }
    }

    fun stopTunnel(serverId: String) {
        activeCloudJobs.remove(serverId)?.cancel()
        activeTunnelSpecs.remove(serverId)

        val boreClient = activeBoreClients.remove(serverId)
        boreClient?.stop()

        val panelBoreClient = activePanelBoreClients.remove(serverId)
        panelBoreClient?.stop()

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
        for ((_, job) in activeCloudJobs) {
            job.cancel()
        }
        activeCloudJobs.clear()
        activeTunnelSpecs.clear()

        for ((_, client) in activeBoreClients) {
            client.stop()
        }
        activeBoreClients.clear()

        for ((_, client) in activePanelBoreClients) {
            client.stop()
        }
        activePanelBoreClients.clear()

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
