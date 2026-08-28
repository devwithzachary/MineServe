package com.devwithzachary.mineserve.tunnel

import com.devwithzachary.mineserve.model.TunnelProvider

sealed class TunnelState {
    data object Disconnected : TunnelState()

    data class Connecting(
        val message: String = "Connecting to tunnel network...",
        val claimUrl: String? = null
    ) : TunnelState()

    data class Connected(
        val publicHost: String,
        val publicPort: Int,
        val fullAddress: String,
        val provider: TunnelProvider,
        val assignedAt: Long = System.currentTimeMillis(),
        val activeConnections: Int = 0
    ) : TunnelState()

    data class Error(
        val errorMessage: String
    ) : TunnelState()

    val isConnected: Boolean get() = this is Connected
    val isConnecting: Boolean get() = this is Connecting
    val displayAddress: String? get() = (this as? Connected)?.fullAddress
}
