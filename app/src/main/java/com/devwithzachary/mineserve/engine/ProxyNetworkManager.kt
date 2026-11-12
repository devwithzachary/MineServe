package com.devwithzachary.mineserve.engine

import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ProxyServerConfig
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.repository.ServerRepository
import java.io.File

/**
 * Generates Velocity (velocity.toml + forwarding.secret) and BungeeCord (config.yml)
 * proxy configurations and manages internal 127.0.0.1 loopback binding on backend servers.
 */
object ProxyNetworkManager {

    fun sanitizeAlias(raw: String): String {
        val cleaned = raw.lowercase().replace(Regex("[^a-z0-9_-]"), "-").trim('-')
        return cleaned.ifBlank { "server" }
    }

    fun applyProxyConfig(
        proxyServer: MinecraftServer,
        proxyDir: File,
        allServers: List<MinecraftServer>
    ): ProxyServerConfig {
        proxyDir.mkdirs()
        val cfg = proxyServer.proxyConfig
        val secret = cfg.forwardingSecret.ifBlank { ProxyServerConfig.generateSecret() }
        val resolvedRoutes = cfg.routes.map { r ->
            val target = allServers.firstOrNull { it.id == r.targetServerId }
            r.copy(
                alias = sanitizeAlias(r.alias),
                port = target?.port ?: r.port
            )
        }
        val tryList = if (cfg.tryOrder.isNotEmpty()) {
            cfg.tryOrder.filter { alias -> resolvedRoutes.any { it.alias == alias } }
        } else {
            resolvedRoutes.map { it.alias }
        }.ifEmpty { resolvedRoutes.map { it.alias } }

        when (proxyServer.type) {
            ServerType.VELOCITY -> {
                File(proxyDir, "forwarding.secret").writeText(secret)
                val serversBlock = if (resolvedRoutes.isEmpty()) {
                    "lobby = \"127.0.0.1:25566\""
                } else {
                    resolvedRoutes.joinToString("\n") { "${it.alias} = \"${it.host}:${it.port}\"" }
                }
                val tryArray = if (tryList.isEmpty()) {
                    "[\"lobby\"]"
                } else {
                    tryList.joinToString(", ", prefix = "[", postfix = "]") { "\"$it\"" }
                }
                val escapedMotd = proxyServer.motd.replace("\"", "\\\"")
                val toml = """
                    config-version = "2.7"
                    bind = "0.0.0.0:${proxyServer.port}"
                    motd = "<green>$escapedMotd</green>"
                    show-max-players = 100
                    online-mode = ${cfg.onlineMode}
                    force-key-authentication = true
                    prevent-client-proxy-connections = false
                    player-info-forwarding-mode = "modern"
                    forwarding-secret-file = "forwarding.secret"
                    announce-forge = false

                    [servers]
                    $serversBlock
                    try = $tryArray

                    [forced-hosts]

                    [advanced]
                    compression-threshold = 256
                    compression-level = -1
                    login-ratelimit = 3000
                    connection-timeout = 5000
                    read-timeout = 30000
                    haproxy-protocol = false
                    tcp-fast-open = false
                    bungee-plugin-message-channel = true
                    show-ping-requests = false
                    failover-on-unexpected-server-disconnect = true
                    announce-proxy-commands = true
                    log-command-executions = false
                    log-player-connections = true
                """.trimIndent() + "\n"
                File(proxyDir, "velocity.toml").writeText(toml)
            }
            ServerType.BUNGEECORD -> {
                val serversYaml = if (resolvedRoutes.isEmpty()) {
                    """
                    |  lobby:
                    |    motd: '${proxyServer.motd.replace("'", "''")}'
                    |    address: 127.0.0.1:25566
                    |    restricted: false
                    """.trimMargin()
                } else {
                    resolvedRoutes.joinToString("\n") { r ->
                        """
                        |  ${r.alias}:
                        |    motd: '${proxyServer.motd.replace("'", "''")}'
                        |    address: ${r.host}:${r.port}
                        |    restricted: false
                        """.trimMargin()
                    }
                }
                val prioritiesYaml = if (tryList.isEmpty()) {
                    "  - lobby"
                } else {
                    tryList.joinToString("\n") { "  - $it" }
                }
                val yml = """
                    forge_support: true
                    connection_throttle_limit: 3
                    timeout: 30000
                    online_mode: ${cfg.onlineMode}
                    log_commands: false
                    network_compression_threshold: 256
                    listeners:
                    - query_port: ${proxyServer.port}
                      motd: '${proxyServer.motd.replace("'", "''")}'
                      tab_list: GLOBAL_PING
                      query_enabled: false
                      proxy_protocol: false
                      forced_hosts: {}
                      ping_passthrough: false
                      priorities:
                    $prioritiesYaml
                      bind_local_address: true
                      host: 0.0.0.0:${proxyServer.port}
                      max_players: 100
                      tab_size: 60
                      force_default_server: true
                    connection_throttle: 4000
                    groups: {}
                    log_pings: true
                    ip_forward: true
                    prevent_proxy_connections: false
                    servers:
                    $serversYaml
                    stats: ${java.util.UUID.randomUUID()}
                """.trimIndent() + "\n"
                File(proxyDir, "config.yml").writeText(yml)
            }
            else -> Unit
        }

        return cfg.copy(
            routes = resolvedRoutes,
            tryOrder = tryList,
            forwardingSecret = secret
        )
    }

    suspend fun setBackendLoopbackMode(
        serverRepository: ServerRepository,
        serverId: String,
        loopbackOnly: Boolean
    ) {
        val props = serverRepository.loadServerProperties(serverId)
        val updated = props.copy(
            serverIp = if (loopbackOnly) "127.0.0.1" else "",
            onlineMode = !loopbackOnly
        )
        serverRepository.saveServerProperties(serverId, updated)
    }
}

