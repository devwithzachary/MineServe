package com.devwithzachary.mineserve.admin.web

import kotlinx.serialization.Serializable

enum class WebAdminExposure {
    LAN_ONLY,
    UPNP_PORT_FORWARD,
    BORE_TUNNEL,
    PLAYIT_TUNNEL
}

@Serializable
data class WebAdminConfig(
    val enabled: Boolean = false,
    val port: Int = 8080,
    val username: String = "admin",
    val password: String = "",
    val exposure: WebAdminExposure = WebAdminExposure.LAN_ONLY,
    val customBoreHost: String = "bore.pub",
    val customBorePort: Int = 7835,
    val playitSecret: String = ""
)

sealed class WebAdminExposureState {
    data object Idle : WebAdminExposureState()
    data class Connecting(
        val message: String,
        val claimUrl: String? = null
    ) : WebAdminExposureState()
    data class Connected(
        val publicUrl: String,
        val details: String
    ) : WebAdminExposureState()
    data class Failed(val error: String) : WebAdminExposureState()
}

data class WebAdminState(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val localIp: String? = null,
    val localUrl: String? = null,
    val exposureState: WebAdminExposureState = WebAdminExposureState.Idle,
    val activeSessionsCount: Int = 0
)

