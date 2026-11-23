package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.LinuxPortForwardState
import com.devwithzachary.mineserve.desktop.LinuxTunnelState
import com.devwithzachary.mineserve.desktop.ui.DesktopColors
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopPanelCard
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.DiscordWebhookConfig
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.determineJavaVersion
import java.util.UUID
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Composable
fun DesktopSettingsTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val props = remember(server.id) { viewModel.serverRepository.loadServerProperties(server.id) }
    val hostMem = remember { LinuxHostTelemetry.getMemoryInfo() }
    val maxRamMb = (hostMem.totalMb.toInt() - 1024).coerceAtLeast(2048)

    var name by remember(server.id) { mutableStateOf(server.name) }
    var ramMb by remember(server.id) { mutableStateOf(server.allocatedRamMb) }
    var javaVer by remember(server.id) { mutableStateOf(server.javaVersion) }
    var autoStart by remember(server.id) { mutableStateOf(server.autoStart) }
    var jvmArgs by remember(server.id) { mutableStateOf(server.jvmArgs) }

    var motd by remember(server.id) { mutableStateOf(props.motd) }
    var portText by remember(server.id) { mutableStateOf(props.serverPort.toString()) }
    var maxPlayersText by remember(server.id) { mutableStateOf(props.maxPlayers.toString()) }
    var viewDistText by remember(server.id) { mutableStateOf(props.viewDistance.toString()) }
    var simDistText by remember(server.id) { mutableStateOf(props.simulationDistance.toString()) }
    var gamemode by remember(server.id) { mutableStateOf(props.gamemode) }
    var difficulty by remember(server.id) { mutableStateOf(props.difficulty) }
    var seed by remember(server.id) { mutableStateOf(props.levelSeed) }
    var onlineMode by remember(server.id) { mutableStateOf(props.onlineMode) }
    var whitelist by remember(server.id) { mutableStateOf(props.whiteList) }
    var pvp by remember(server.id) { mutableStateOf(props.pvp) }
    var allowFlight by remember(server.id) { mutableStateOf(props.allowFlight) }
    var commandBlocks by remember(server.id) { mutableStateOf(props.enableCommandBlock) }

    // Tunnel & Port Forward
    val tunnelState by viewModel.tunnelState.collectAsState()
    val pfState by viewModel.portForwardState.collectAsState()
    var provider by remember(server.id) { mutableStateOf(server.tunnelConfig.provider) }
    var tunnelAutoStart by remember(server.id) { mutableStateOf(server.tunnelConfig.autoStart) }
    var customRelayHost by remember(server.id) { mutableStateOf(server.tunnelConfig.customRelayHost) }
    var customRelayPort by remember(server.id) { mutableStateOf(server.tunnelConfig.customRelayPort.toString()) }
    var cloudSubdomain by remember(server.id) { mutableStateOf(server.tunnelConfig.cloudSubdomain) }
    var pfAutoForward by remember(server.id) { mutableStateOf(server.portForwardConfig.autoForward) }

    // Version Upgrade
    var availableVersions by remember(server.type) { mutableStateOf<List<String>>(emptyList()) }
    var targetVersion by remember(server.id) { mutableStateOf(server.version) }
    var upgrading by remember { mutableStateOf(false) }

    // Discord Webhook
    var discordEnabled by remember(server.id) { mutableStateOf(server.discordWebhookConfig.enabled) }
    var discordUrl by remember(server.id) { mutableStateOf(server.discordWebhookConfig.webhookUrl) }

    var confirmDelete by remember(server.id) { mutableStateOf(false) }

    LaunchedEffect(server.type) {
        availableVersions = viewModel.softwareManager.fetchAvailableVersions(server.type)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Core Server & server.properties Configuration
        DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Server Configuration & server.properties", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Button(
                    onClick = {
                        val newPort = portText.toIntOrNull()?.coerceIn(1024, 65535) ?: server.port
                        val updatedSrv = server.copy(
                            name = name.trim().ifBlank { server.name },
                            port = newPort,
                            allocatedRamMb = ramMb,
                            javaVersion = javaVer,
                            autoStart = autoStart,
                            jvmArgs = jvmArgs.trim()
                        )
                        viewModel.serverRepository.updateServer(updatedSrv)
                        val updatedProps = props.copy(
                            motd = motd,
                            serverPort = newPort,
                            maxPlayers = maxPlayersText.toIntOrNull() ?: props.maxPlayers,
                            viewDistance = viewDistText.toIntOrNull() ?: props.viewDistance,
                            simulationDistance = simDistText.toIntOrNull() ?: props.simulationDistance,
                            gamemode = gamemode,
                            difficulty = difficulty,
                            levelSeed = seed,
                            onlineMode = onlineMode,
                            whiteList = whitelist,
                            pvp = pvp,
                            allowFlight = allowFlight,
                            enableCommandBlock = commandBlocks
                        )
                        viewModel.serverRepository.saveServerProperties(server.id, updatedProps)
                        viewModel.showToast("Saved settings and server.properties")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                ) {
                    Text("Save Configuration", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Display Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = motd,
                    onValueChange = { motd = it },
                    label = { Text("MOTD") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Server Port") },
                    singleLine = true,
                    modifier = Modifier.width(130.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Allocated Memory: $ramMb MB (Host max allocatable: $maxRamMb MB)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = ramMb.toFloat(),
                        onValueChange = { ramMb = ((it.toInt() / 256) * 256).coerceIn(512, maxRamMb) },
                        valueRange = 512f..maxRamMb.toFloat()
                    )
                }
                Column {
                    Text("Java Runtime", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(8, 17, 21, 25).forEach { v ->
                            val selected = javaVer == v
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                                border = BorderStroke(1.dp, if (selected) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                                modifier = Modifier.clickable { javaVer = v }
                            ) {
                                Text(
                                    text = "Java $v",
                                    color = if (selected) Color.Black else DesktopColors.Slate300,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = maxPlayersText,
                    onValueChange = { maxPlayersText = it },
                    label = { Text("Max Players") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = viewDistText,
                    onValueChange = { viewDistText = it },
                    label = { Text("View Distance") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = simDistText,
                    onValueChange = { simDistText = it },
                    label = { Text("Sim Distance") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = gamemode,
                    onValueChange = { gamemode = it },
                    label = { Text("Gamemode (survival/creative)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    label = { Text("Difficulty (easy/normal/hard)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = seed,
                    onValueChange = { seed = it },
                    label = { Text("Level Seed (optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = jvmArgs,
                    onValueChange = { jvmArgs = it },
                    label = { Text("Custom JVM Flags") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabeledCheckbox("Online Mode", onlineMode) { onlineMode = it }
                LabeledCheckbox("Whitelist", whitelist) { whitelist = it }
                LabeledCheckbox("PvP Enabled", pvp) { pvp = it }
                LabeledCheckbox("Allow Flight", allowFlight) { allowFlight = it }
                LabeledCheckbox("Command Blocks", commandBlocks) { commandBlocks = it }
                LabeledCheckbox("Auto-Start on Boot", autoStart) { autoStart = it }
            }
        }

        // Card 2: Public Multiplayer Tunneling & UPnP Router Port Forwarding
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            DesktopPanelCard(modifier = Modifier.weight(1.2f)) {
                Text("Public Multiplayer Tunneling", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TunnelProvider.entries.forEach { p ->
                        val active = provider == p
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                            border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.clickable { provider = p }
                        ) {
                            Text(
                                text = p.name,
                                color = if (active) Color.Black else DesktopColors.Slate300,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (provider == TunnelProvider.CUSTOM_BORE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customRelayHost,
                            onValueChange = { customRelayHost = it },
                            label = { Text("Custom Bore Host") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customRelayPort,
                            onValueChange = { customRelayPort = it },
                            label = { Text("Control Port") },
                            singleLine = true,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (provider == TunnelProvider.MINESERVE_CLOUD) {
                    if (!viewModel.cloudManager.isLinked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign in to MineServe Cloud to reserve custom .play.mineserve.app subdomains.", fontSize = 12.sp, color = DesktopColors.AmberLight)
                            OutlinedButton(onClick = { viewModel.openModal(DesktopModal.CLOUD) }) {
                                Text("Sign In / Plans", fontSize = 11.sp)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = cloudSubdomain,
                            onValueChange = { cloudSubdomain = it },
                            label = { Text("Preferred Subdomain (.play.mineserve.app)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LabeledCheckbox("Auto-start tunnel with server", tunnelAutoStart) { tunnelAutoStart = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val updatedCfg = server.tunnelConfig.copy(
                                    provider = provider,
                                    autoStart = tunnelAutoStart,
                                    customRelayHost = customRelayHost.trim().ifBlank { "bore.pub" },
                                    customRelayPort = customRelayPort.toIntOrNull() ?: 7835,
                                    cloudSubdomain = cloudSubdomain.trim().lowercase()
                                )
                                val updated = server.copy(tunnelConfig = updatedCfg)
                                viewModel.serverRepository.updateServer(updated)
                                viewModel.networkManager.startTunnel(updated)
                                viewModel.showToast("Starting ${provider.displayName} tunnel...")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                        ) {
                            Text("Save & Start Tunnel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        if (tunnelState is LinuxTunnelState.Connected || tunnelState is LinuxTunnelState.Connecting) {
                            OutlinedButton(onClick = { viewModel.networkManager.stopTunnel(server.id) }) {
                                Text("Stop Tunnel", fontSize = 12.sp, color = DesktopColors.RedstoneLight)
                            }
                        }
                    }
                }
            }

            DesktopPanelCard(modifier = Modifier.weight(0.8f)) {
                Text("Router Port Forwarding (UPnP / NAT-PMP)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val pfStatusText = when (val s = pfState) {
                    is LinuxPortForwardState.Mapped -> "Mapped: ${s.directAddress} (${s.gatewayName})"
                    is LinuxPortForwardState.Discovering -> s.message
                    is LinuxPortForwardState.Failed -> "Failed: ${s.reason}"
                    is LinuxPortForwardState.Idle -> "Idle (not mapped)"
                }
                Text(pfStatusText, fontSize = 12.sp, color = DesktopColors.EmeraldLight, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(10.dp))
                LabeledCheckbox("Auto-forward port on start", pfAutoForward) { pfAutoForward = it }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val updated = server.copy(portForwardConfig = server.portForwardConfig.copy(autoForward = pfAutoForward))
                            viewModel.serverRepository.updateServer(updated)
                            viewModel.networkManager.startPortForward(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        Text("Map Router Port", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { viewModel.networkManager.releasePortForward(server.id) }) {
                        Text("Release", fontSize = 12.sp)
                    }
                }
            }
        }

        // Card 3: In-Place Server Version & Build Upgrader + Discord Webhook
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            DesktopPanelCard(modifier = Modifier.weight(1f)) {
                Text("In-Place Server Version & Build Upgrader", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current: ${server.type.displayName} ${server.version} (Build: ${server.serverBuild ?: "standard"}). Automatically creates a backup before upgrading.",
                    fontSize = 12.sp,
                    color = DesktopColors.Slate400
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = targetVersion,
                        onValueChange = { targetVersion = it },
                        label = { Text("Target Version (e.g. ${availableVersions.firstOrNull() ?: server.version})") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (viewModel.processManager.isServerRunning(server.id)) {
                                viewModel.showToast("Stop the server before upgrading.")
                                return@Button
                            }
                            upgrading = true
                            viewModel.scope.launch {
                                try {
                                    val ok = viewModel.softwareManager.upgradeServerVersion(
                                        server = server,
                                        newVersion = targetVersion.trim(),
                                        createBackup = true,
                                        processManager = viewModel.processManager
                                    ) { msg, _ ->
                                        viewModel.showToast(msg)
                                    }
                                    if (ok) {
                                        val newJava = determineJavaVersion(targetVersion.trim(), server.type)
                                        viewModel.showToast("Upgraded to ${server.type.displayName} $targetVersion (Java $newJava)")
                                    } else {
                                        viewModel.showToast("Upgrade failed for ${server.type.displayName} $targetVersion")
                                    }
                                } finally {
                                    upgrading = false
                                }
                            }
                        },
                        enabled = !upgrading,
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                    ) {
                        Text(if (upgrading) "Upgrading..." else "Backup & Upgrade", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            DesktopPanelCard(modifier = Modifier.weight(1f)) {
                Text("Discord Webhook Notifications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = discordUrl,
                    onValueChange = { discordUrl = it },
                    label = { Text("Discord Webhook URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LabeledCheckbox("Enable Webhook Alerts", discordEnabled) { discordEnabled = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.scope.launch {
                                    val payload = JSONObject()
                                        .put("username", "MineServe")
                                        .put("content", "MineServe Linux test notification for **${server.name}**.")
                                        .toString()
                                    val ok = runCatching {
                                        val req = MineServeHttpClient.newRequestBuilder(discordUrl.trim())
                                            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                                            .build()
                                        MineServeHttpClient.client.newCall(req).execute().use { it.isSuccessful }
                                    }.getOrDefault(false)
                                    viewModel.showToast(if (ok) "Sent test message to Discord!" else "Discord webhook test failed")
                                }
                            }
                        ) {
                            Text("Test", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val cfg = DiscordWebhookConfig(enabled = discordEnabled, webhookUrl = discordUrl.trim())
                                viewModel.serverRepository.updateServer(server.copy(discordWebhookConfig = cfg))
                                viewModel.showToast("Saved Discord webhook settings")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Card 4: Proxy Network / Internal Loopback Binding (127.0.0.1)
        DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
            var loopbackOnly by remember(server.id) {
                mutableStateOf(viewModel.serverRepository.loadServerProperties(server.id).serverIp.trim() == "127.0.0.1")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (server.type.isProxy) "Velocity / BungeeCord Proxy Routing" else "Internal Loopback Binding (Proxy Backend Mode)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (server.type.isProxy) {
                            "Configure backend server routes in velocity.toml / config.yml via the Files tab or Web Admin UI."
                        } else {
                            "Bind server-ip strictly to 127.0.0.1 so only a local Velocity or BungeeCord proxy on this host can connect to this backend server."
                        },
                        fontSize = 12.sp,
                        color = DesktopColors.Slate400
                    )
                }
                if (!server.type.isProxy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledCheckbox("Bind strictly to 127.0.0.1", loopbackOnly) { checked ->
                            loopbackOnly = checked
                            val currentProps = viewModel.serverRepository.loadServerProperties(server.id)
                            viewModel.serverRepository.saveServerProperties(
                                server.id,
                                currentProps.copy(
                                    serverIp = if (checked) "127.0.0.1" else "",
                                    onlineMode = if (checked) false else currentProps.onlineMode
                                )
                            )
                            viewModel.showToast(if (checked) "Bound backend to 127.0.0.1 (online-mode=false)" else "Cleared 127.0.0.1 loopback binding")
                        }
                    }
                }
            }
        }

        // Card 5: Danger Zone
        DesktopPanelCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = DesktopColors.RedstonePrimary.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Danger Zone: Delete Server", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DesktopColors.RedstoneLight)
                    Text("Permanently deletes ${server.name} and all world files on disk.", fontSize = 12.sp, color = DesktopColors.Slate400)
                }
                if (!confirmDelete) {
                    Button(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.RedstonePrimary, contentColor = Color.White)
                    ) {
                        Text("Delete Server", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (viewModel.processManager.isServerRunning(server.id)) {
                                    viewModel.processManager.stopServer(server.id)
                                }
                                viewModel.networkManager.stopTunnel(server.id)
                                viewModel.serverRepository.deleteServer(server.id)
                                val next = viewModel.serverRepository.servers.value.firstOrNull()?.id
                                if (next != null) viewModel.selectServer(next)
                                viewModel.showToast("Deleted server ${server.name}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.RedstonePrimary, contentColor = Color.White)
                        ) {
                            Text("Confirm Permanent Delete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { confirmDelete = false }) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, fontSize = 12.sp, color = DesktopColors.Slate300)
    }
}

@Composable
fun DesktopPlayersTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    metrics: ServerMetrics?
) {
    var targetPlayer by remember { mutableStateOf("") }
    val onlinePlayers = metrics?.onlinePlayers ?: emptyList()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
            Text("Player Administration & Access Control", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = targetPlayer,
                    onValueChange = { targetPlayer = it },
                    placeholder = { Text("Minecraft Username...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                listOf(
                    "OP" to "op",
                    "De-OP" to "deop",
                    "Whitelist Add" to "whitelist add",
                    "Whitelist Remove" to "whitelist remove",
                    "Kick" to "kick",
                    "Ban" to "ban",
                    "Pardon" to "pardon"
                ).forEach { (label, cmdPrefix) ->
                    OutlinedButton(
                        onClick = {
                            val p = targetPlayer.trim()
                            if (p.isNotEmpty()) {
                                viewModel.processManager.sendCommand(server.id, "$cmdPrefix $p")
                                viewModel.showToast("Sent: $cmdPrefix $p")
                            }
                        }
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
        }

        DesktopPanelCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text("Currently Online (${onlinePlayers.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            if (onlinePlayers.isEmpty()) {
                Text("No players are currently connected.", color = DesktopColors.Slate400, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    onlinePlayers.forEach { player ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DesktopColors.Slate900,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(player, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { viewModel.processManager.sendCommand(server.id, "op $player") }) {
                                        Text("OP", fontSize = 11.sp)
                                    }
                                    OutlinedButton(onClick = { viewModel.processManager.sendCommand(server.id, "kick $player") }) {
                                        Text("Kick", fontSize = 11.sp, color = DesktopColors.AmberLight)
                                    }
                                    OutlinedButton(onClick = { viewModel.processManager.sendCommand(server.id, "ban $player") }) {
                                        Text("Ban", fontSize = 11.sp, color = DesktopColors.RedstoneLight)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopAutomationTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val cfg = server.automationConfig
    var idleSleep by remember(server.id) { mutableStateOf(cfg.idleSleepEnabled) }
    var idleMinutes by remember(server.id) { mutableStateOf(cfg.idleSleepTimeoutMinutes.toString()) }
    var autoWake by remember(server.id) { mutableStateOf(cfg.autoWakeOnPing) }

    var newTaskName by remember { mutableStateOf("") }
    var newTaskType by remember { mutableStateOf(ScheduledTaskType.WORLD_BACKUP) }
    var newIntervalMin by remember { mutableStateOf("360") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DesktopPanelCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Idle Auto-Sleep & Auto-Wake on Ping", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Button(
                    onClick = {
                        val updated = cfg.copy(
                            idleSleepEnabled = idleSleep,
                            idleSleepTimeoutMinutes = idleMinutes.toIntOrNull()?.coerceIn(1, 240) ?: 30,
                            autoWakeOnPing = autoWake
                        )
                        viewModel.serverRepository.updateServer(server.copy(automationConfig = updated))
                        viewModel.showToast("Saved automation settings")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                ) {
                    Text("Save Automation", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LabeledCheckbox("Enable Idle Auto-Sleep", idleSleep) { idleSleep = it }
                OutlinedTextField(
                    value = idleMinutes,
                    onValueChange = { idleMinutes = it },
                    label = { Text("Idle Timeout (minutes)") },
                    singleLine = true,
                    modifier = Modifier.width(180.dp)
                )
                LabeledCheckbox("Auto-Wake when player pings server", autoWake) { autoWake = it }
            }
        }

        DesktopPanelCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text("Scheduled Tasks (${cfg.scheduledTasks.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskName,
                    onValueChange = { newTaskName = it },
                    placeholder = { Text("Task Name (optional)...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                ScheduledTaskType.entries.forEach { t ->
                    val active = newTaskType == t
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                        border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.clickable { newTaskType = t }
                    ) {
                        Text(
                            text = t.displayName,
                            color = if (active) Color.Black else DesktopColors.Slate300,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = newIntervalMin,
                    onValueChange = { newIntervalMin = it },
                    label = { Text("Every (min)") },
                    singleLine = true,
                    modifier = Modifier.width(110.dp)
                )
                Button(
                    onClick = {
                        val task = ScheduledTask(
                            id = UUID.randomUUID().toString().take(8),
                            serverId = server.id,
                            name = newTaskName.trim().ifBlank { newTaskType.displayName },
                            enabled = true,
                            taskType = newTaskType,
                            frequency = ScheduleFrequency.INTERVAL,
                            intervalMinutes = newIntervalMin.toIntOrNull()?.coerceAtLeast(5) ?: 360
                        )
                        viewModel.serverRepository.updateServer(
                            server.copy(automationConfig = cfg.copy(scheduledTasks = cfg.scheduledTasks + task))
                        )
                        newTaskName = ""
                        viewModel.showToast("Added scheduled task ${task.name}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                ) {
                    Text("+ Add Task", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cfg.scheduledTasks.forEach { task ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DesktopColors.Slate900,
                        border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(task.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("${task.taskType.displayName} - ${task.frequencyDescription}", fontSize = 11.sp, color = DesktopColors.Slate400)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Switch(
                                    checked = task.enabled,
                                    onCheckedChange = { enabled ->
                                        val next = cfg.scheduledTasks.map { if (it.id == task.id) it.copy(enabled = enabled) else it }
                                        viewModel.serverRepository.updateServer(server.copy(automationConfig = cfg.copy(scheduledTasks = next)))
                                    }
                                )
                                OutlinedButton(
                                    onClick = {
                                        val next = cfg.scheduledTasks.filterNot { it.id == task.id }
                                        viewModel.serverRepository.updateServer(server.copy(automationConfig = cfg.copy(scheduledTasks = next)))
                                    }
                                ) {
                                    Text("Delete", fontSize = 11.sp, color = DesktopColors.RedstoneLight)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
