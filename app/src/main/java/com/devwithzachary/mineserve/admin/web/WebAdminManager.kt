package com.devwithzachary.mineserve.admin.web

import android.content.Context
import android.util.Log
import com.devwithzachary.mineserve.engine.ServerProcessManager
import com.devwithzachary.mineserve.model.TunnelConfig
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.net.portforward.PortForwardManager
import com.devwithzachary.mineserve.net.portforward.PortForwardProtocol
import com.devwithzachary.mineserve.net.portforward.PortForwardState
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.service.MineServeForegroundService
import com.devwithzachary.mineserve.tunnel.TunnelManager
import com.devwithzachary.mineserve.tunnel.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebAdminManager private constructor(
    private val context: Context,
    private val serverRepository: ServerRepository,
    private val processManager: ServerProcessManager,
    private val tunnelManager: TunnelManager,
    private val portForwardManager: PortForwardManager
) {
    companion object {
        private const val TAG = "WebAdminManager"
        private const val TUNNEL_ID = "web_admin"

        @Volatile
        private var INSTANCE: WebAdminManager? = null

        fun getInstance(
            context: Context,
            serverRepository: ServerRepository,
            processManager: ServerProcessManager,
            tunnelManager: TunnelManager,
            portForwardManager: PortForwardManager
        ): WebAdminManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebAdminManager(
                    context.applicationContext,
                    serverRepository,
                    processManager,
                    tunnelManager,
                    portForwardManager
                ).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val prefs = WebAdminPreferences(context)
    val security = WebAdminSecurity()

    private val _config = MutableStateFlow(prefs.loadConfig())
    val config: StateFlow<WebAdminConfig> = _config.asStateFlow()

    private val _state = MutableStateFlow(WebAdminState(port = _config.value.port))
    val state: StateFlow<WebAdminState> = _state.asStateFlow()

    private var server: WebAdminServer? = null
    private var exposureMonitorJob: Job? = null

    init {
        val initialConfig = _config.value
        if (initialConfig.enabled) {
            start()
        }
    }

    fun updateConfig(newConfig: WebAdminConfig) {
        val old = _config.value
        _config.value = newConfig
        prefs.saveConfig(newConfig)

        if (old.enabled != newConfig.enabled) {
            if (newConfig.enabled) start() else stop()
        } else if (newConfig.enabled && (old.port != newConfig.port || old.exposure != newConfig.exposure)) {
            // Restart if port or exposure changed while enabled
            stop()
            start()
        }
    }

    fun generateNewPassword(): String {
        val newPass = WebAdminPreferences.generateRandomPassword()
        val updated = _config.value.copy(password = newPass)
        updateConfig(updated)
        return newPass
    }

    fun start() {
        val cfg = _config.value
        if (server != null && server?.isRunning == true) return

        Log.i(TAG, "Starting Web Remote Admin on port ${cfg.port}...")
        val s = WebAdminServer(
            port = cfg.port,
            security = security,
            configProvider = { _config.value },
            serverRepository = serverRepository,
            processManager = processManager
        )
        s.start()
        server = s

        val localIp = portForwardManager.getLocalWifiIpAddress()
        val localUrl = if (localIp != null) "http://$localIp:${cfg.port}" else "http://localhost:${cfg.port}"

        _state.value = WebAdminState(
            isRunning = s.isRunning,
            port = cfg.port,
            localIp = localIp,
            localUrl = localUrl,
            exposureState = WebAdminExposureState.Idle,
            activeSessionsCount = security.getActiveSessionsCount()
        )

        MineServeForegroundService.start(context)
        setupExposure(cfg)
    }

    fun stop() {
        Log.i(TAG, "Stopping Web Remote Admin...")
        server?.stop()
        server = null

        exposureMonitorJob?.cancel()
        exposureMonitorJob = null

        tunnelManager.stopTunnel(TUNNEL_ID)
        portForwardManager.releasePort(TUNNEL_ID)

        _state.value = _state.value.copy(
            isRunning = false,
            exposureState = WebAdminExposureState.Idle
        )
    }

    private fun setupExposure(cfg: WebAdminConfig) {
        exposureMonitorJob?.cancel()
        tunnelManager.stopTunnel(TUNNEL_ID)
        portForwardManager.releasePort(TUNNEL_ID)

        when (cfg.exposure) {
            WebAdminExposure.LAN_ONLY -> {
                _state.value = _state.value.copy(exposureState = WebAdminExposureState.Idle)
            }
            WebAdminExposure.UPNP_PORT_FORWARD -> {
                _state.value = _state.value.copy(
                    exposureState = WebAdminExposureState.Connecting("Discovering router gateway for port ${cfg.port}...")
                )
                exposureMonitorJob = scope.launch {
                    val pfState = portForwardManager.forwardCustomPort(
                        mappingId = TUNNEL_ID,
                        port = cfg.port,
                        description = "MineServe-WebAdmin",
                        protocolConfig = PortForwardProtocol.TCP
                    )
                    when (pfState) {
                        is PortForwardState.Mapped -> {
                            val publicUrl = "http://${pfState.externalIp}:${pfState.externalPort}"
                            _state.value = _state.value.copy(
                                exposureState = WebAdminExposureState.Connected(
                                    publicUrl = publicUrl,
                                    details = "Mapped via ${pfState.gatewayName}"
                                )
                            )
                        }
                        is PortForwardState.Failed -> {
                            _state.value = _state.value.copy(
                                exposureState = WebAdminExposureState.Failed(pfState.reason)
                            )
                        }
                        else -> {}
                    }
                }
            }
            WebAdminExposure.BORE_TUNNEL -> {
                _state.value = _state.value.copy(
                    exposureState = WebAdminExposureState.Connecting("Connecting to Bore relay (${cfg.customBoreHost})...")
                )
                val tConfig = TunnelConfig(
                    provider = TunnelProvider.BORE,
                    customRelayHost = cfg.customBoreHost,
                    customRelayPort = cfg.customBorePort
                )
                tunnelManager.startCustomTunnel(
                    tunnelId = TUNNEL_ID,
                    localPort = cfg.port,
                    config = tConfig,
                    label = "Web Remote Admin"
                )

                exposureMonitorJob = scope.launch {
                    tunnelManager.tunnelStates.collect { states ->
                        val tState = states[TUNNEL_ID] ?: TunnelState.Disconnected
                        when (tState) {
                            is TunnelState.Connected -> {
                                val publicUrl = "http://${tState.fullAddress}"
                                _state.value = _state.value.copy(
                                    exposureState = WebAdminExposureState.Connected(
                                        publicUrl = publicUrl,
                                        details = "Public Bore Relay"
                                    )
                                )
                            }
                            is TunnelState.Connecting -> {
                                _state.value = _state.value.copy(
                                    exposureState = WebAdminExposureState.Connecting("Establishing Bore tunnel...")
                                )
                            }
                            is TunnelState.Error -> {
                                _state.value = _state.value.copy(
                                    exposureState = WebAdminExposureState.Failed(tState.errorMessage)
                                )
                            }
                            is TunnelState.Disconnected -> {}
                        }
                    }
                }
            }
            WebAdminExposure.PLAYIT_TUNNEL -> {
                _state.value = _state.value.copy(
                    exposureState = WebAdminExposureState.Connecting("Connecting Playit agent...")
                )
                val tConfig = TunnelConfig(
                    provider = TunnelProvider.PLAYIT,
                    playitSecret = cfg.playitSecret
                )
                tunnelManager.startCustomTunnel(
                    tunnelId = TUNNEL_ID,
                    localPort = cfg.port,
                    config = tConfig,
                    label = "Web Remote Admin"
                )

                exposureMonitorJob = scope.launch {
                    tunnelManager.tunnelStates.collect { states ->
                        val tState = states[TUNNEL_ID] ?: TunnelState.Disconnected
                        when (tState) {
                            is TunnelState.Connected -> {
                                val publicUrl = "http://${tState.fullAddress}"
                                _state.value = _state.value.copy(
                                    exposureState = WebAdminExposureState.Connected(
                                        publicUrl = publicUrl,
                                        details = "Playit Tunnel"
                                    )
                                )
                            }
                            is TunnelState.Connecting -> {
                                _state.value = _state.value.copy(
                                    exposureState = WebAdminExposureState.Connecting(
                                        message = if (tState.claimUrl != null) "Playit claim required to activate tunnel" else tState.message,
                                        claimUrl = tState.claimUrl
                                    )
                                )
                            }
                            is TunnelState.Error -> {
                                _state.value = _state.value.copy(
                                    exposureState = WebAdminExposureState.Failed(tState.errorMessage)
                                )
                            }
                            is TunnelState.Disconnected -> {}
                        }
                    }
                }
            }
        }
    }
}
