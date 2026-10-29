package com.devwithzachary.mineserve.net.portforward

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.devwithzachary.mineserve.model.MinecraftServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap

class PortForwardManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PortForwardManager"

        @Volatile
        private var INSTANCE: PortForwardManager? = null

        fun getInstance(context: Context): PortForwardManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PortForwardManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val upnpClient = UpnpClient()
    private val natPmpClient = NatPmpClient()

    private val activeUpnpGateways = ConcurrentHashMap<String, UpnpGateway>()
    private val activeMappedServers = ConcurrentHashMap<String, MinecraftServer>()

    private val _states = MutableStateFlow<Map<String, PortForwardState>>(emptyMap())
    val states: StateFlow<Map<String, PortForwardState>> = _states.asStateFlow()

    fun getState(serverId: String): PortForwardState {
        return _states.value[serverId] ?: PortForwardState.Idle
    }

    fun forwardPortAsync(server: MinecraftServer, onComplete: ((PortForwardState) -> Unit)? = null) {
        val serverId = server.id
        updateState(serverId, PortForwardState.Discovering("Discovering router gateway..."))
        scope.launch {
            val state = forwardPortInternal(server)
            updateState(serverId, state)
            onComplete?.invoke(state)
        }
    }

    suspend fun forwardPort(server: MinecraftServer): PortForwardState {
        val serverId = server.id
        updateState(serverId, PortForwardState.Discovering("Discovering router gateway..."))
        val state = forwardPortInternal(server)
        updateState(serverId, state)
        return state
    }

    private suspend fun forwardPortInternal(server: MinecraftServer): PortForwardState {
        val localIp = getLocalWifiIpAddress()
        if (localIp == null) {
            val err = "Device is not connected to a local Wi-Fi network."
            Log.w(TAG, err)
            return PortForwardState.Failed(err)
        }

        val serverPort = server.port
        val protocolConfig = server.portForwardConfig.protocol
        val protocols = when (protocolConfig) {
            PortForwardProtocol.TCP -> listOf("TCP")
            PortForwardProtocol.UDP -> listOf("UDP")
            PortForwardProtocol.BOTH -> listOf("TCP", "UDP")
        }

        // 1. Try UPnP Discovery
        Log.i(TAG, "Attempting UPnP discovery for server ${server.name} ($localIp:$serverPort)...")
        val gateway = upnpClient.discoverGateway()
        if (gateway != null) {
            activeUpnpGateways[server.id] = gateway
            val externalIp = upnpClient.getExternalIp(gateway)
            if (externalIp != null) {
                var allSucceeded = true
                for (proto in protocols) {
                    val ok = upnpClient.addPortMapping(
                        gateway = gateway,
                        externalPort = serverPort,
                        internalPort = serverPort,
                        internalIp = localIp,
                        protocol = proto,
                        description = "MineServe-${server.name}",
                        leaseDurationSeconds = server.portForwardConfig.leaseDurationSeconds
                    )
                    if (!ok) allSucceeded = false
                }

                if (allSucceeded) {
                    activeMappedServers[server.id] = server
                    Log.i(TAG, "UPnP port forward success: $externalIp:$serverPort -> $localIp:$serverPort")
                    return PortForwardState.Mapped(
                        externalIp = externalIp,
                        externalPort = serverPort,
                        internalIp = localIp,
                        internalPort = serverPort,
                        protocol = protocolConfig,
                        gatewayName = gateway.friendlyName
                    )
                }
            }
        }

        // 2. Try NAT-PMP Fallback
        val gatewayIp = getDefaultGatewayIp()
        if (gatewayIp != null) {
            Log.i(TAG, "Attempting NAT-PMP discovery on gateway $gatewayIp...")
            val externalIp = natPmpClient.getExternalIp(gatewayIp)
            if (externalIp != null) {
                var mappedExternalPort: Int? = null
                for (proto in protocols) {
                    val isUdp = proto == "UDP"
                    val resPort = natPmpClient.mapPort(
                        gatewayIp = gatewayIp,
                        internalPort = serverPort,
                        externalPort = serverPort,
                        isUdp = isUdp,
                        lifetimeSeconds = 7200
                    )
                    if (resPort != null) mappedExternalPort = resPort
                }

                if (mappedExternalPort != null) {
                    activeMappedServers[server.id] = server
                    return PortForwardState.Mapped(
                        externalIp = externalIp,
                        externalPort = mappedExternalPort,
                        internalIp = localIp,
                        internalPort = serverPort,
                        protocol = protocolConfig,
                        gatewayName = "NAT-PMP Gateway"
                    )
                }
            }
        }

        return PortForwardState.Failed("No UPnP or NAT-PMP response from home router. Please verify UPnP is enabled in your router settings.")
    }

    fun releasePort(serverId: String) {
        val server = activeMappedServers.remove(serverId)
        val gateway = activeUpnpGateways.remove(serverId)

        if (server != null && gateway != null) {
            scope.launch {
                val protocols = when (server.portForwardConfig.protocol) {
                    PortForwardProtocol.TCP -> listOf("TCP")
                    PortForwardProtocol.UDP -> listOf("UDP")
                    PortForwardProtocol.BOTH -> listOf("TCP", "UDP")
                }
                for (proto in protocols) {
                    upnpClient.deletePortMapping(gateway, server.port, proto)
                }
            }
        }

        updateState(serverId, PortForwardState.Idle)
    }

    private fun updateState(serverId: String, state: PortForwardState) {
        val map = _states.value.toMutableMap()
        map[serverId] = state
        _states.value = map
    }

    fun getLocalWifiIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                // Prefer wlan0 or Wi-Fi interfaces
                val isWifi = intf.name.startsWith("wlan", ignoreCase = true) || intf.name.startsWith("eth", ignoreCase = true)
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        if (isWifi) return addr.hostAddress
                    }
                }
            }

            // Fallback: any non-loopback IPv4
            for (intf in NetworkInterface.getNetworkInterfaces()) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving local IP: ${e.message}")
        }
        return null
    }

    private fun getDefaultGatewayIp(): InetAddress? {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
            val activeNetwork = cm.activeNetwork ?: return null
            val linkProps = cm.getLinkProperties(activeNetwork) ?: return null

            for (route in linkProps.routes) {
                if (route.isDefaultRoute && route.gateway is Inet4Address) {
                    return route.gateway
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not determine default gateway: ${e.message}")
        }
        return null
    }
}

