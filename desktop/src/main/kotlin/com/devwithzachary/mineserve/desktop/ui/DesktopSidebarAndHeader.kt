package com.devwithzachary.mineserve.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.desktop.LinuxFirewallChecker
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.LinuxPortForwardState
import com.devwithzachary.mineserve.desktop.LinuxTunnelState
import com.devwithzachary.mineserve.model.MineServeAboutData
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType

@Composable
fun DesktopSidebar(
    viewModel: DesktopViewModel,
    servers: List<MinecraftServer>,
    selectedServerId: String?,
    statuses: Map<String, ServerStatus>,
    collapsed: Boolean
) {
    val width = if (collapsed) 72.dp else 280.dp
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(DesktopColors.ObsidianCard)
            .border(BorderStroke(1.dp, DesktopColors.ObsidianCardBorder))
    ) {
        // Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DesktopColors.EmeraldPrimary.copy(alpha = 0.2f))
                        .border(1.dp, DesktopColors.EmeraldPrimary, RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleSidebar() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (collapsed) ">" else "M",
                        color = DesktopColors.EmeraldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                if (!collapsed) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("MineServe", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("v${MineServeAboutData.APP_VERSION}", fontSize = 11.sp, color = DesktopColors.EmeraldLight, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = LinuxHostTelemetry.getHostDisplayName(),
                            fontSize = 11.sp,
                            color = DesktopColors.Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (!collapsed) {
                OutlinedButton(
                    onClick = { viewModel.toggleSidebar() },
                    modifier = Modifier.height(28.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("<", fontSize = 11.sp, color = DesktopColors.Slate300)
                }
            }
        }

        HorizontalDivider(color = DesktopColors.ObsidianCardBorder)

        // Action Buttons (+ New Server / Import .zip)
        if (!collapsed) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.openModal(DesktopModal.CREATE_SERVER) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                ) {
                    Text("+ New Server", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.openModal(DesktopModal.IMPORT_SERVER) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Server (.zip)", fontSize = 12.sp, color = DesktopColors.Slate300)
                }
            }
            HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DesktopColors.EmeraldPrimary)
                    .clickable { viewModel.openModal(DesktopModal.CREATE_SERVER) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Server List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(servers, key = { it.id }) { srv ->
                val isSelected = srv.id == selectedServerId
                val st = statuses[srv.id] ?: ServerStatus.STOPPED
                val dotColor = when (st) {
                    ServerStatus.RUNNING -> DesktopColors.EmeraldPrimary
                    ServerStatus.STARTING, ServerStatus.STOPPING -> DesktopColors.AmberWarning
                    ServerStatus.ERROR -> DesktopColors.RedstonePrimary
                    ServerStatus.STOPPED, ServerStatus.STANDBY -> DesktopColors.Slate400
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) DesktopColors.EmeraldPrimary.copy(alpha = 0.14f) else DesktopColors.Slate900,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectServer(srv.id) }
                ) {
                    if (collapsed) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = srv.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${srv.type.displayName} ${srv.version} - :${srv.port}",
                                    fontSize = 11.sp,
                                    color = DesktopColors.Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
                        }
                    }
                }
            }
        }

        // Sidebar Footer
        if (!collapsed) {
            HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.openModal(DesktopModal.CLOUD) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesktopColors.EmeraldPrimary.copy(alpha = 0.18f),
                        contentColor = DesktopColors.EmeraldLight
                    )
                ) {
                    val cloudTier = viewModel.cloudManager.toStatusJson().optString("tier", "FREE")
                    Text(
                        text = if (viewModel.cloudManager.isLinked) "MineServe Cloud (${cloudTier.uppercase()})" else "MineServe Cloud",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.openModal(DesktopModal.HOST_TOOLS) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        Text("App Settings", fontSize = 11.sp, color = DesktopColors.Slate300)
                    }
                    OutlinedButton(
                        onClick = { viewModel.openModal(DesktopModal.USERS_AUDIT) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        Text("Users & Audit", fontSize = 11.sp, color = DesktopColors.Slate300)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.openModal(DesktopModal.ABOUT_CREDITS) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        Text("About & Credits", fontSize = 11.sp, color = DesktopColors.Slate300)
                    }
                    OutlinedButton(
                        onClick = {
                            if (viewModel.webServer.isRunning) {
                                viewModel.openUrlInBrowser("http://127.0.0.1:${viewModel.webServer.actualPort}")
                            } else {
                                viewModel.openModal(DesktopModal.HOST_TOOLS)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        Text("Web Admin", fontSize = 11.sp, color = DesktopColors.Slate400)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopServerHeader(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    status: ServerStatus,
    metrics: ServerMetrics?,
    activeTab: DesktopTab
) {
    val tunnelState by viewModel.tunnelState.collectAsState()
    val portForwardState by viewModel.portForwardState.collectAsState()
    val isRunning = status == ServerStatus.RUNNING || status == ServerStatus.STARTING
    val localIp = LinuxHostTelemetry.getLocalIpAddress() ?: "127.0.0.1"
    val lanAddress = "$localIp:${server.port}"
    val fwStatus = LinuxFirewallChecker.checkInboundPort(
        server.port,
        isUdp = server.type == ServerType.BEDROCK_BDS || server.type == ServerType.BEDROCK_GEYSER
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DesktopColors.ObsidianCard)
            .border(BorderStroke(1.dp, DesktopColors.ObsidianCardBorder))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Server Title + Power Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(server.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        DesktopStatusBadge(status)
                    }
                    Text(
                        text = "${server.type.displayName} ${server.version} - Port ${server.port} - Java ${server.javaVersion} - ${server.allocatedRamMb} MB RAM",
                        fontSize = 12.sp,
                        color = DesktopColors.Slate400
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { viewModel.openInFileManager(viewModel.serverRepository.getServerDirectory(server.id)) }
                ) {
                    Text("Open Folder", fontSize = 12.sp, color = DesktopColors.Slate300)
                }
                Button(
                    onClick = { viewModel.processManager.startServer(server) },
                    enabled = !isRunning && status != ServerStatus.STOPPING,
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                ) {
                    Text("Start", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.processManager.restartServer(server) },
                    enabled = isRunning
                ) {
                    Text("Restart", fontSize = 12.sp, color = DesktopColors.Slate300)
                }
                Button(
                    onClick = { viewModel.processManager.stopServer(server.id) },
                    enabled = isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.RedstonePrimary, contentColor = Color.White)
                ) {
                    Text("Stop", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Row 2: Connection & Public Access Bar
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DesktopColors.Slate950,
            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "CONNECTION & PUBLIC ACCESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesktopColors.Slate400
                    )
                    when (val ts = tunnelState) {
                        is LinuxTunnelState.Connected -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DesktopColors.EmeraldPrimary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, DesktopColors.EmeraldPrimary),
                                modifier = Modifier.clickable {
                                    viewModel.copyToClipboard(ts.fullAddress, "Copied tunnel address: ${ts.fullAddress}")
                                }
                            ) {
                                Text(
                                    text = "Tunnel (${server.tunnelConfig.provider.displayName}): ${ts.fullAddress} (Click to copy)",
                                    color = DesktopColors.EmeraldLight,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        is LinuxTunnelState.Connecting -> {
                            Text(
                                text = "Tunnel connecting: ${ts.claimUrl ?: ts.message}",
                                color = DesktopColors.AmberLight,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {
                                    ts.claimUrl?.let { viewModel.openUrlInBrowser(it) }
                                }
                            )
                        }
                        is LinuxTunnelState.Error -> {
                            Text("Tunnel Error: ${ts.errorMessage}", color = DesktopColors.RedstoneLight, fontSize = 11.sp)
                        }
                        is LinuxTunnelState.Disconnected -> {
                            Text("Tunnel: Off", color = DesktopColors.Slate400, fontSize = 11.sp)
                        }
                    }

                    if (portForwardState is LinuxPortForwardState.Mapped) {
                        val mapped = portForwardState as LinuxPortForwardState.Mapped
                        Text(
                            text = "UPnP: ${mapped.directAddress}",
                            color = DesktopColors.EmeraldLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable {
                                viewModel.copyToClipboard(mapped.directAddress, "Copied UPnP address")
                            }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DesktopColors.Slate900,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.clickable {
                            viewModel.copyToClipboard(lanAddress, "Copied LAN address: $lanAddress")
                        }
                    ) {
                        Text(
                            text = "LAN: $lanAddress",
                            color = DesktopColors.Slate300,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tunnelState is LinuxTunnelState.Connected || tunnelState is LinuxTunnelState.Connecting) {
                        OutlinedButton(
                            onClick = { viewModel.networkManager.stopTunnel(server.id) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Stop Tunnel", fontSize = 11.sp, color = DesktopColors.RedstoneLight)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.networkManager.startTunnel(server) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("Start ${server.tunnelConfig.provider.name} Tunnel", fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.selectTab(DesktopTab.SETTINGS) },
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Configure", fontSize = 11.sp, color = DesktopColors.Slate300)
                    }
                }
            }
        }

        // Firewall Warning Banner if active and no tunnel is connected
        if (fwStatus.isActive && !fwStatus.isPortConfirmedOpen && tunnelState !is LinuxTunnelState.Connected) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DesktopColors.AmberWarning.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, DesktopColors.AmberWarning.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${fwStatus.firewallName} firewall detected: LAN players may be blocked on port ${server.port} unless opened (${fwStatus.fixCommand ?: ""}) or using a tunnel.",
                        color = DesktopColors.AmberLight,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    fwStatus.fixCommand?.let { cmd ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Command",
                            color = DesktopColors.EmeraldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                viewModel.copyToClipboard(cmd, "Copied firewall command: $cmd")
                            }
                        )
                    }
                }
            }
        }

        // Row 3: 4-Metric Quick Statistics Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val cpuText = if (metrics != null) "${"%.1f".format(metrics.cpuPercentage)}%" else "0.0%"
            val ramText = if (metrics != null) "${metrics.ramUsedMb} / ${server.allocatedRamMb} MB" else "0 / ${server.allocatedRamMb} MB"
            val tpsText = if (metrics != null) "${"%.1f".format(metrics.tps)} TPS (${"%.1f".format(metrics.mspt)} ms)" else "-- TPS"
            val playersText = "${metrics?.onlinePlayerCount ?: 0} Online"

            HeaderMetricPill("CPU USAGE", cpuText, Modifier.weight(1f))
            HeaderMetricPill("MEMORY", ramText, Modifier.weight(1f))
            HeaderMetricPill("TICK RATE", tpsText, Modifier.weight(1f))
            HeaderMetricPill("PLAYERS", playersText, Modifier.weight(1f))
        }

        // Row 4: Horizontally Scrollable Navigation Tabs Bar (Below CPU/RAM Statistics)
        val tabScrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tabScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DesktopTab.entries.forEach { tab ->
                    val active = tab == activeTab
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                        border = BorderStroke(
                            1.dp,
                            if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder
                        ),
                        modifier = Modifier.clickable { viewModel.selectTab(tab) }
                    ) {
                        Text(
                            text = tab.title,
                            color = if (active) Color.Black else DesktopColors.Slate300,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            if (tabScrollState.maxValue > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(tabScrollState),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeaderMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DesktopColors.Slate950,
        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 10.sp, color = DesktopColors.Slate400, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(value, fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        }
    }
}
