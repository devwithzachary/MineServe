package com.devwithzachary.mineserve.net.portforward

import android.util.Log
import com.devwithzachary.mineserve.api.MineServeHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI

data class UpnpGateway(
    val location: String,
    val controlUrl: String,
    val serviceType: String,
    val friendlyName: String = "UPnP Gateway"
)

class UpnpClient {
    companion object {
        private const val TAG = "UpnpClient"
        private const val SSDP_IP = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val DISCOVERY_TIMEOUT_MS = 3500

        private val SEARCH_TARGETS = listOf(
            "urn:schemas-upnp-org:device:InternetGatewayDevice:1",
            "urn:schemas-upnp-org:service:WANIPConnection:1",
            "urn:schemas-upnp-org:service:WANPPPConnection:1"
        )
    }

    suspend fun discoverGateway(): UpnpGateway? = withContext(Dispatchers.IO) {
        val locations = mutableSetOf<String>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 1200
                socket.broadcast = true

                val group = InetAddress.getByName(SSDP_IP)
                for (target in SEARCH_TARGETS) {
                    val message = "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: $SSDP_IP:$SSDP_PORT\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 2\r\n" +
                            "ST: $target\r\n\r\n"
                    val bytes = message.toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(bytes, bytes.size, group, SSDP_PORT)
                    socket.send(packet)
                }

                val startTime = System.currentTimeMillis()
                val buffer = ByteArray(2048)
                while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT_MS) {
                    try {
                        val responsePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(responsePacket)
                        val response = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                        val location = extractHeader(response, "LOCATION")
                        if (!location.isNullOrBlank()) {
                            locations.add(location.trim())
                            break // Found a candidate gateway
                        }
                    } catch (_: Exception) {
                        // Socket read timeout or no further packet
                        if (locations.isNotEmpty()) break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "UPnP SSDP broadcast error: ${e.message}")
        }

        for (loc in locations) {
            val gateway = parseDeviceDescription(loc)
            if (gateway != null) {
                return@withContext gateway
            }
        }
        null
    }

    private suspend fun parseDeviceDescription(locationUrl: String): UpnpGateway? = withContext(Dispatchers.IO) {
        try {
            val req = MineServeHttpClient.newGetRequest(locationUrl)
            val xml = MineServeHttpClient.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.string()
            } ?: return@withContext null

            val friendlyName = extractTag(xml, "friendlyName") ?: "UPnP Router"

            // Look for WANIPConnection or WANPPPConnection
            val serviceTypes = listOf(
                "urn:schemas-upnp-org:service:WANIPConnection:1",
                "urn:schemas-upnp-org:service:WANIPConnection:2",
                "urn:schemas-upnp-org:service:WANPPPConnection:1"
            )

            for (st in serviceTypes) {
                val serviceBlock = findServiceBlock(xml, st)
                if (serviceBlock != null) {
                    val rawControlUrl = extractTag(serviceBlock, "controlURL") ?: continue
                    val resolvedControlUrl = resolveUrl(locationUrl, rawControlUrl)
                    Log.i(TAG, "Discovered UPnP Gateway '$friendlyName' with service $st at $resolvedControlUrl")
                    return@withContext UpnpGateway(
                        location = locationUrl,
                        controlUrl = resolvedControlUrl,
                        serviceType = st,
                        friendlyName = friendlyName
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing UPnP device description from $locationUrl: ${e.message}")
            null
        }
    }

    suspend fun getExternalIp(gateway: UpnpGateway): String? = withContext(Dispatchers.IO) {
        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:GetExternalIPAddress xmlns:u="${gateway.serviceType}"/>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val response = executeSoap(gateway.controlUrl, gateway.serviceType, "GetExternalIPAddress", soapBody)
        if (response != null) {
            extractTag(response, "NewExternalIPAddress")?.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    suspend fun addPortMapping(
        gateway: UpnpGateway,
        externalPort: Int,
        internalPort: Int,
        internalIp: String,
        protocol: String,
        description: String = "MineServe Server",
        leaseDurationSeconds: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:AddPortMapping xmlns:u="${gateway.serviceType}">
                        <NewRemoteHost></NewRemoteHost>
                        <NewExternalPort>$externalPort</NewExternalPort>
                        <NewProtocol>$protocol</NewProtocol>
                        <NewInternalPort>$internalPort</NewInternalPort>
                        <NewInternalClient>$internalIp</NewInternalClient>
                        <NewEnabled>1</NewEnabled>
                        <NewPortMappingDescription>$description</NewPortMappingDescription>
                        <NewLeaseDuration>$leaseDurationSeconds</NewLeaseDuration>
                    </u:AddPortMapping>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val response = executeSoap(gateway.controlUrl, gateway.serviceType, "AddPortMapping", soapBody)
        if (response != null && !response.contains("<s:Fault", ignoreCase = true)) {
            Log.i(TAG, "Successfully added UPnP port mapping $externalPort -> $internalIp:$internalPort ($protocol)")
            true
        } else {
            Log.w(TAG, "Failed to add UPnP port mapping: $response")
            false
        }
    }

    suspend fun deletePortMapping(
        gateway: UpnpGateway,
        externalPort: Int,
        protocol: String
    ): Boolean = withContext(Dispatchers.IO) {
        val soapBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:DeletePortMapping xmlns:u="${gateway.serviceType}">
                        <NewRemoteHost></NewRemoteHost>
                        <NewExternalPort>$externalPort</NewExternalPort>
                        <NewProtocol>$protocol</NewProtocol>
                    </u:DeletePortMapping>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val response = executeSoap(gateway.controlUrl, gateway.serviceType, "DeletePortMapping", soapBody)
        if (response != null && !response.contains("<s:Fault", ignoreCase = true)) {
            Log.i(TAG, "Successfully deleted UPnP port mapping $externalPort ($protocol)")
            true
        } else {
            Log.w(TAG, "Failed deleting UPnP port mapping: $response")
            false
        }
    }

    private suspend fun executeSoap(
        controlUrl: String,
        serviceType: String,
        action: String,
        soapBody: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val mediaType = "text/xml; charset=utf-8".toMediaType()
            val req = Request.Builder()
                .url(controlUrl)
                .addHeader("SOAPAction", "\"$serviceType#$action\"")
                .post(soapBody.toRequestBody(mediaType))
                .build()

            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                resp.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SOAP execution error for $action: ${e.message}")
            null
        }
    }

    private fun extractHeader(response: String, headerName: String): String? {
        val prefix = "$headerName:"
        for (line in response.lines()) {
            if (line.startsWith(prefix, ignoreCase = true)) {
                return line.substring(prefix.length).trim()
            }
        }
        return null
    }

    private fun extractTag(xml: String, tagName: String): String? {
        val startTag = "<$tagName>"
        val endTag = "</$tagName>"
        val startIndex = xml.indexOf(startTag, ignoreCase = true)
        if (startIndex == -1) return null
        val contentStart = startIndex + startTag.length
        val endIndex = xml.indexOf(endTag, contentStart, ignoreCase = true)
        if (endIndex == -1) return null
        return xml.substring(contentStart, endIndex).trim()
    }

    private fun findServiceBlock(xml: String, serviceType: String): String? {
        val serviceStartTag = "<service>"
        val serviceEndTag = "</service>"
        var pos = 0
        while (pos < xml.length) {
            val start = xml.indexOf(serviceStartTag, pos, ignoreCase = true)
            if (start == -1) break
            val end = xml.indexOf(serviceEndTag, start, ignoreCase = true)
            if (end == -1) break
            val block = xml.substring(start, end + serviceEndTag.length)
            if (block.contains(serviceType, ignoreCase = true)) {
                return block
            }
            pos = end + serviceEndTag.length
        }
        return null
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val base = URI(baseUrl)
            base.resolve(relativeUrl).toString()
        } catch (_: Exception) {
            relativeUrl
        }
    }
}

