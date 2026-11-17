package com.devwithzachary.mineserve.net.portforward

import kotlinx.serialization.Serializable

@Serializable
enum class PortForwardProtocol {
    TCP,
    UDP,
    BOTH
}

@Serializable
data class PortForwardConfig(
    val enabled: Boolean = false,
    val autoForward: Boolean = false,
    val protocol: PortForwardProtocol = PortForwardProtocol.BOTH,
    val leaseDurationSeconds: Int = 0 // 0 = permanent / indefinite
)

sealed class PortForwardState {
    data object Idle : PortForwardState()

    data class Discovering(
        val message: String = "Discovering router gateway..."
    ) : PortForwardState()

    data class Mapped(
        val externalIp: String,
        val externalPort: Int,
        val internalIp: String,
        val internalPort: Int,
        val protocol: PortForwardProtocol,
        val gatewayName: String = "UPnP Gateway",
        val mappedAt: Long = System.currentTimeMillis()
    ) : PortForwardState() {
        val directAddress: String get() = "$externalIp:$externalPort"
    }

    data class Failed(
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : PortForwardState()

    val isMapped: Boolean get() = this is Mapped
    val isDiscovering: Boolean get() = this is Discovering
}

