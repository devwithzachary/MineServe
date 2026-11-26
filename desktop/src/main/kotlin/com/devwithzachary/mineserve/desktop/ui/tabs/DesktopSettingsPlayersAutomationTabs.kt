package com.devwithzachary.mineserve.desktop.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.designsystem.components.MineServePreferenceRow
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.LinuxPortForwardState
import com.devwithzachary.mineserve.desktop.LinuxTunnelState
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerPlayerLists
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.determineJavaVersion
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopSettingsTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

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

    val tunnelState by viewModel.tunnelState.collectAsState()
    val pfState by viewModel.portForwardState.collectAsState()
    var provider by remember(server.id) { mutableStateOf(server.tunnelConfig.provider) }
    var tunnelAutoStart by remember(server.id) { mutableStateOf(server.tunnelConfig.autoStart) }
    var customRelayHost by remember(server.id) { mutableStateOf(server.tunnelConfig.customRelayHost) }
    var customRelayPort by remember(server.id) { mutableStateOf(server.tunnelConfig.customRelayPort.toString()) }
    var cloudSubdomain by remember(server.id) { mutableStateOf(server.tunnelConfig.cloudSubdomain) }
    var pfAutoForward by remember(server.id) { mutableStateOf(server.portForwardConfig.autoForward) }

    var availableVersions by remember(server.type) { mutableStateOf<List<String>>(emptyList()) }
    var targetVersion by remember(server.id) { mutableStateOf(server.version) }
    var upgrading by remember { mutableStateOf(false) }

    var showDeleteDialog by remember(server.id) { mutableStateOf(false) }

    LaunchedEffect(server.type) {
        availableVersions = viewModel.softwareManager.fetchAvailableVersions(server.type)
    }
    LaunchedEffect(server.javaVersion) {
        javaVer = server.javaVersion
    }
    LaunchedEffect(server.version) {
        targetVersion = server.version
    }

    if (showDeleteDialog) {
        MineServeDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "Delete ${server.name}?",
            confirmLabel = "Delete Server",
            isDestructive = true,
            icon = Icons.Rounded.DeleteForever,
            onConfirm = {
                showDeleteDialog = false
                viewModel.scope.launch {
                    viewModel.networkManager.stopTunnel(server.id)
                    viewModel.networkManager.releasePortForward(server.id)
                    if (viewModel.processManager.isServerRunning(server.id)) {
                        viewModel.processManager.stopServerAndWait(server.id)
                    }
                    viewModel.serverRepository.deleteServer(server.id)
                    viewModel.showToast("Deleted server ${server.name}")
                }
            }
        ) {
            Text(
                text = "This permanently deletes ${server.name} and all world files on disk.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colorScheme.secondaryContainer,
        selectedLabelColor = colorScheme.onSecondaryContainer,
        containerColor = colorScheme.surfaceContainerHigh,
        labelColor = colorScheme.onSurfaceVariant
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. JVM & Runtime Allocation
        MineServeSettingsGroup(title = "JVM & Runtime Allocation") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Allocated Memory", style = MaterialTheme.typography.bodyLarge, color = colorScheme.onSurface)
                            MineServeTableMonoText(
                                text = "$ramMb MB / $maxRamMb MB max",
                                color = colorScheme.primary
                            )
                        }
                        Slider(
                            value = ramMb.toFloat(),
                            onValueChange = { ramMb = ((it.toInt() / 256) * 256).coerceIn(512, maxRamMb) },
                            valueRange = 512f..maxRamMb.toFloat()
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Java Runtime", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(8, 17, 21, 25).forEach { v ->
                                FilterChip(
                                    selected = javaVer == v,
                                    onClick = { javaVer = v },
                                    label = { Text("Java $v") },
                                    colors = chipColors,
                                    border = null
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = jvmArgs,
                    onValueChange = { jvmArgs = it },
                    label = { Text("Custom JVM Flags") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(
                title = "Auto-Start on Host Boot",
                subtitle = "Automatically launch this server when MineServe starts",
                checked = autoStart,
                onCheckedChange = { autoStart = it },
                icon = Icons.Rounded.PowerSettingsNew
            )
        }

        // 2. Public Tunneling & UPnP Port Forwarding (Diamond tertiary accents)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            MineServeSettingsGroup(
                title = "Public Multiplayer Tunneling",
                modifier = Modifier.weight(1.25f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TunnelProvider.entries.forEach { p ->
                            FilterChip(
                                selected = provider == p,
                                onClick = { provider = p },
                                label = { Text(p.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.tertiaryContainer,
                                    selectedLabelColor = colorScheme.onTertiaryContainer,
                                    containerColor = colorScheme.surfaceContainerHigh,
                                    labelColor = colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }

                    if (provider == TunnelProvider.CUSTOM_BORE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = customRelayHost,
                                onValueChange = { customRelayHost = it },
                                label = { Text("Custom Bore Host") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customRelayPort,
                                onValueChange = { customRelayPort = it },
                                label = { Text("Control Port") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                                modifier = Modifier.width(130.dp)
                            )
                        }
                    } else if (provider == TunnelProvider.MINESERVE_CLOUD) {
                        if (!viewModel.cloudManager.isLinked) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Sign in to MineServe Cloud to reserve custom .play.mineserve.app subdomains.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.warning,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.openModal(DesktopModal.CLOUD) },
                                    shape = ShapeDefaults.Small,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colorScheme.tertiaryContainer,
                                        contentColor = colorScheme.onTertiaryContainer
                                    )
                                ) {
                                    Text("Sign In / Plans")
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = cloudSubdomain,
                                onValueChange = { cloudSubdomain = it },
                                label = { Text("Preferred Subdomain (.play.mineserve.app)") },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                MineServePreferenceRow(
                    title = "Auto-Start Tunnel with Server",
                    checked = tunnelAutoStart,
                    onCheckedChange = { tunnelAutoStart = it },
                    icon = Icons.Rounded.Public
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (tunnelState is LinuxTunnelState.Connected || tunnelState is LinuxTunnelState.Connecting) {
                        FilledTonalButton(
                            onClick = { viewModel.networkManager.stopTunnel(server.id) },
                            shape = ShapeDefaults.Medium,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Stop Tunnel")
                        }
                    }
                    Button(
                        onClick = {
                            val updatedCfg = server.tunnelConfig.copy(
                                provider = provider,
                                autoStart = tunnelAutoStart,
                                customRelayHost = customRelayHost.trim().ifBlank { "bore.pub" },
                                customRelayPort = customRelayPort.toIntOrNull() ?: 7835,
                                cloudSubdomain = cloudSubdomain.trim().lowercase(Locale.US)
                            )
                            val updated = server.copy(tunnelConfig = updatedCfg)
                            viewModel.serverRepository.updateServer(updated)
                            viewModel.networkManager.startTunnel(updated)
                            viewModel.showToast("Starting ${provider.displayName} tunnel...")
                        },
                        shape = ShapeDefaults.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.tertiary,
                            contentColor = colorScheme.onTertiary
                        )
                    ) {
                        Text("Save & Start Tunnel")
                    }
                }
            }

            MineServeSettingsGroup(
                title = "Router Port Forwarding (UPnP)",
                modifier = Modifier.weight(0.85f)
            ) {
                val pfStatusText = when (val s = pfState) {
                    is LinuxPortForwardState.Mapped -> "Mapped: ${s.directAddress} (${s.gatewayName})"
                    is LinuxPortForwardState.Discovering -> s.message
                    is LinuxPortForwardState.Failed -> "Failed: ${s.reason}"
                    is LinuxPortForwardState.Idle -> "Idle (not mapped)"
                }
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Router,
                            contentDescription = null,
                            tint = colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = pfStatusText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                            color = colorScheme.tertiary
                        )
                    }
                }
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                MineServePreferenceRow(
                    title = "Auto-Forward Port on Start",
                    checked = pfAutoForward,
                    onCheckedChange = { pfAutoForward = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    FilledTonalButton(
                        onClick = { viewModel.networkManager.releasePortForward(server.id) },
                        shape = ShapeDefaults.Medium
                    ) {
                        Text("Release")
                    }
                    Button(
                        onClick = {
                            val updated = server.copy(portForwardConfig = server.portForwardConfig.copy(autoForward = pfAutoForward))
                            viewModel.serverRepository.updateServer(updated)
                            viewModel.networkManager.startPortForward(updated)
                        },
                        shape = ShapeDefaults.Medium
                    ) {
                        Text("Map Port")
                    }
                }
            }
        }

        // 3. Visual server.properties
        MineServeSettingsGroup(title = "server.properties & Gameplay") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        label = { Text("Port") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.width(120.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = maxPlayersText,
                        onValueChange = { maxPlayersText = it },
                        label = { Text("Max Players") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewDistText,
                        onValueChange = { viewDistText = it },
                        label = { Text("View Distance") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = simDistText,
                        onValueChange = { simDistText = it },
                        label = { Text("Sim Distance") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = gamemode,
                        onValueChange = { gamemode = it },
                        label = { Text("Gamemode") },
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
                    )
                    OutlinedTextField(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        label = { Text("Difficulty") },
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
                    )
                }

                OutlinedTextField(
                    value = seed,
                    onValueChange = { seed = it },
                    label = { Text("Level Seed (optional)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(title = "Online Mode (Mojang Auth)", checked = onlineMode, onCheckedChange = { onlineMode = it })
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(title = "Enforce Whitelist", checked = whitelist, onCheckedChange = { whitelist = it })
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(title = "PvP Enabled", checked = pvp, onCheckedChange = { pvp = it })
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(title = "Allow Flight", checked = allowFlight, onCheckedChange = { allowFlight = it })
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(title = "Enable Command Blocks", checked = commandBlocks, onCheckedChange = { commandBlocks = it })

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        val freshProps = viewModel.serverRepository.loadServerProperties(server.id)
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
                        val updatedProps = freshProps.copy(
                            motd = motd,
                            serverPort = newPort,
                            maxPlayers = maxPlayersText.toIntOrNull() ?: freshProps.maxPlayers,
                            viewDistance = viewDistText.toIntOrNull() ?: freshProps.viewDistance,
                            simulationDistance = simDistText.toIntOrNull() ?: freshProps.simulationDistance,
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
                    shape = ShapeDefaults.Medium
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Configuration")
                }
            }
        }

        // 4. Version Upgrader & Proxy Loopback Binding
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            MineServeSettingsGroup(
                title = "In-Place Version & Build Upgrader",
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Current: ${server.type.displayName} ${server.version} (Build: ${server.serverBuild ?: "standard"}). Automatically creates a backup before upgrading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = targetVersion,
                            onValueChange = { targetVersion = it },
                            label = { Text("Target Version (e.g. ${availableVersions.firstOrNull() ?: server.version})") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
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
                            shape = ShapeDefaults.Medium
                        ) {
                            Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (upgrading) "Upgrading..." else "Backup & Upgrade")
                        }
                    }
                }
            }

            MineServeSettingsGroup(
                title = if (server.type.isProxy) "Proxy Routing" else "Proxy Backend Loopback",
                modifier = Modifier.weight(1f)
            ) {
                var loopbackOnly by remember(server.id) {
                    mutableStateOf(viewModel.serverRepository.loadServerProperties(server.id).serverIp.trim() == "127.0.0.1")
                }
                if (server.type.isProxy) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Configure backend server routes in velocity.toml / config.yml via the Files tab or Web Admin UI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    MineServePreferenceRow(
                        title = "Bind strictly to 127.0.0.1",
                        subtitle = "Allows only local Velocity/BungeeCord proxies on this host to connect (sets online-mode=false)",
                        checked = loopbackOnly,
                        onCheckedChange = { checked ->
                            loopbackOnly = checked
                            if (checked) onlineMode = false
                            val currentProps = viewModel.serverRepository.loadServerProperties(server.id)
                            viewModel.serverRepository.saveServerProperties(
                                server.id,
                                currentProps.copy(
                                    serverIp = if (checked) "127.0.0.1" else "",
                                    onlineMode = if (checked) false else currentProps.onlineMode
                                )
                            )
                            viewModel.showToast(
                                if (checked) "Bound backend to 127.0.0.1 (online-mode=false)" else "Cleared 127.0.0.1 loopback binding"
                            )
                        },
                        icon = Icons.Rounded.Lan
                    )
                }
            }
        }

        // 5. Danger Zone
        MineServeCard(
            containerColor = colorScheme.errorContainer.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Danger Zone: Delete Server",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.error
                        )
                        Text(
                            text = "Permanently deletes ${server.name} and all world files on disk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    shape = ShapeDefaults.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error,
                        contentColor = colorScheme.onError
                    )
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Server")
                }
            }
        }
    }
}

private enum class DesktopPlayerFilter(val label: String) {
    ONLINE("Online"),
    OPERATORS("Operators"),
    WHITELISTED("Whitelisted"),
    BANNED("Banned")
}

private data class DesktopPlayerRowItem(
    val title: String,
    val subtitle: String,
    val isOp: Boolean = false,
    val isOnline: Boolean = false,
    val isBanned: Boolean = false,
    val isIpBan: Boolean = false
)

private data class PendingPlayerAction(
    val username: String,
    val actionTitle: String,
    val commandPrefix: String,
    val message: String,
    val isDestructive: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopPlayersTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    metrics: ServerMetrics?
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var searchQuery by remember { mutableStateOf("") }
    var targetPlayer by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(DesktopPlayerFilter.ONLINE) }
    var playerLists by remember(server.id) { mutableStateOf(ServerPlayerLists()) }
    var pendingAction by remember { mutableStateOf<PendingPlayerAction?>(null) }

    val onlinePlayers = metrics?.onlinePlayers ?: emptyList()

    LaunchedEffect(server.id, onlinePlayers.size) {
        playerLists = viewModel.loadPlayerLists(server.id)
    }

    pendingAction?.let { action ->
        MineServeDialog(
            onDismissRequest = { pendingAction = null },
            title = action.actionTitle,
            confirmLabel = "Confirm",
            isDestructive = action.isDestructive,
            icon = if (action.isDestructive) Icons.Rounded.Block else Icons.Rounded.Shield,
            onConfirm = {
                viewModel.processManager.sendCommand(server.id, "${action.commandPrefix} ${action.username}")
                viewModel.showToast("Sent: ${action.commandPrefix} ${action.username}")
                playerLists = viewModel.loadPlayerLists(server.id)
                pendingAction = null
            }
        ) {
            Text(
                text = action.message,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    val opNames = remember(playerLists) { playerLists.ops.map { it.name.lowercase(Locale.US) }.toSet() }
    val rows: List<DesktopPlayerRowItem> = remember(activeFilter, onlinePlayers, playerLists, searchQuery) {
        val base = when (activeFilter) {
            DesktopPlayerFilter.ONLINE -> onlinePlayers.map { player ->
                DesktopPlayerRowItem(
                    title = player,
                    subtitle = "Connected now",
                    isOp = player.lowercase(Locale.US) in opNames,
                    isOnline = true
                )
            }
            DesktopPlayerFilter.OPERATORS -> playerLists.ops.map { op ->
                DesktopPlayerRowItem(
                    title = op.name,
                    subtitle = "Level ${op.level} - ${op.uuid.ifBlank { "Offline UUID" }}",
                    isOp = true
                )
            }
            DesktopPlayerFilter.WHITELISTED -> playerLists.whitelist.map { w ->
                DesktopPlayerRowItem(
                    title = w.name,
                    subtitle = w.uuid.ifBlank { "Whitelisted player" },
                    isOp = w.name.lowercase(Locale.US) in opNames
                )
            }
            DesktopPlayerFilter.BANNED -> playerLists.bannedPlayers.map { b ->
                DesktopPlayerRowItem(
                    title = b.name,
                    subtitle = "${b.uuid.ifBlank { "Banned" }} - ${b.reason}",
                    isBanned = true
                )
            } + playerLists.bannedIps.map { ip ->
                DesktopPlayerRowItem(
                    title = ip.ip,
                    subtitle = "IP Ban - ${ip.reason}",
                    isBanned = true,
                    isIpBan = true
                )
            }
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) base else base.filter {
            it.title.contains(q, ignoreCase = true) || it.subtitle.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Player Command Bar
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Player Administration & Access Control",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = targetPlayer,
                    onValueChange = { targetPlayer = it },
                    placeholder = { Text("Username or IP...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    modifier = Modifier.width(280.dp)
                )
                listOf(
                    Triple("OP", "op", false),
                    Triple("De-OP", "deop", false),
                    Triple("Whitelist", "whitelist add", false),
                    Triple("Kick", "kick", true),
                    Triple("Ban", "ban", true),
                    Triple("Pardon", "pardon", false)
                ).forEach { (label, cmdPrefix, destructive) ->
                    FilledTonalButton(
                        onClick = {
                            val p = targetPlayer.trim()
                            if (p.isNotEmpty()) {
                                if (destructive) {
                                    pendingAction = PendingPlayerAction(
                                        username = p,
                                        actionTitle = "$label $p?",
                                        commandPrefix = cmdPrefix,
                                        message = "Run '$cmdPrefix $p' on ${server.name}?",
                                        isDestructive = true
                                    )
                                } else {
                                    viewModel.processManager.sendCommand(server.id, "$cmdPrefix $p")
                                    viewModel.showToast("Sent: $cmdPrefix $p")
                                }
                            }
                        },
                        shape = ShapeDefaults.Small,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (destructive) colorScheme.errorContainer else colorScheme.surfaceContainerHigh,
                            contentColor = if (destructive) colorScheme.onErrorContainer else colorScheme.onSurface
                        )
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Search + Filter Chips + Roster Table
        MineServeCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopPlayerFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.secondaryContainer,
                                selectedLabelColor = colorScheme.onSecondaryContainer,
                                containerColor = colorScheme.surfaceContainerHigh,
                                labelColor = colorScheme.onSurfaceVariant
                            ),
                            border = null
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search roster...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.width(260.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.People,
                        title = "No ${activeFilter.label} Players",
                        supportingText = if (searchQuery.isNotBlank()) {
                            "No players match \"$searchQuery\"."
                        } else {
                            "Players in this category will appear here."
                        }
                    )
                }
            } else {
                MineServeTableContainer(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        rows.forEachIndexed { idx, row ->
                            MineServeTableRow(
                                showDivider = idx < rows.lastIndex,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                // Player Avatar Squircle
                                Surface(
                                    shape = ShapeDefaults.Small,
                                    color = if (row.isOp) extended.warningContainer else colorScheme.secondaryContainer,
                                    contentColor = if (row.isOp) extended.onWarningContainer else colorScheme.onSecondaryContainer,
                                    border = null,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = row.title.take(1).uppercase(Locale.US),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = row.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface
                                        )
                                        if (row.isOp) {
                                            Surface(
                                                shape = ShapeDefaults.Small,
                                                color = extended.warningContainer,
                                                contentColor = extended.onWarningContainer,
                                                border = null
                                            ) {
                                                Text(
                                                    text = "OP",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    MineServeTableMonoText(
                                        text = row.subtitle,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (row.isBanned) {
                                        FilledTonalButton(
                                            onClick = {
                                                val cmd = if (row.isIpBan) "pardon-ip" else "pardon"
                                                pendingAction = PendingPlayerAction(
                                                    username = row.title,
                                                    actionTitle = "Pardon ${row.title}?",
                                                    commandPrefix = cmd,
                                                    message = "Remove ban for ${row.title}?",
                                                    isDestructive = false
                                                )
                                            },
                                            shape = ShapeDefaults.Small
                                        ) {
                                            Text("Pardon")
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = {
                                                val cmd = if (row.isOp) "deop" else "op"
                                                viewModel.processManager.sendCommand(server.id, "$cmd ${row.title}")
                                                viewModel.showToast("Sent: $cmd ${row.title}")
                                            },
                                            shape = ShapeDefaults.Small
                                        ) {
                                            Text(if (row.isOp) "De-OP" else "OP")
                                        }
                                        if (activeFilter == DesktopPlayerFilter.WHITELISTED) {
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.processManager.sendCommand(server.id, "whitelist remove ${row.title}")
                                                    viewModel.showToast("Removed ${row.title} from whitelist")
                                                },
                                                shape = ShapeDefaults.Small
                                            ) {
                                                Text("Unwhitelist")
                                            }
                                        } else {
                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.processManager.sendCommand(server.id, "whitelist add ${row.title}")
                                                    viewModel.showToast("Whitelisted ${row.title}")
                                                },
                                                shape = ShapeDefaults.Small
                                            ) {
                                                Text("Whitelist")
                                            }
                                        }
                                        if (row.isOnline) {
                                            FilledTonalButton(
                                                onClick = {
                                                    pendingAction = PendingPlayerAction(
                                                        username = row.title,
                                                        actionTitle = "Kick ${row.title}?",
                                                        commandPrefix = "kick",
                                                        message = "Disconnect ${row.title} from ${server.name}?",
                                                        isDestructive = true
                                                    )
                                                },
                                                shape = ShapeDefaults.Small,
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = extended.warningContainer,
                                                    contentColor = extended.onWarningContainer
                                                )
                                            ) {
                                                Text("Kick")
                                            }
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                pendingAction = PendingPlayerAction(
                                                    username = row.title,
                                                    actionTitle = "Ban ${row.title}?",
                                                    commandPrefix = "ban",
                                                    message = "Permanently ban ${row.title} from ${server.name}?",
                                                    isDestructive = true
                                                )
                                            },
                                            shape = ShapeDefaults.Small,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = colorScheme.errorContainer,
                                                contentColor = colorScheme.onErrorContainer
                                            )
                                        ) {
                                            Text("Ban")
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopAutomationTab(
    viewModel: DesktopViewModel,
    server: MinecraftServer
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    val cfg = server.automationConfig

    var idleSleep by remember(server.id) { mutableStateOf(cfg.idleSleepEnabled) }
    var idleMinutes by remember(server.id) { mutableStateOf(cfg.idleSleepTimeoutMinutes.toString()) }
    var autoWake by remember(server.id) { mutableStateOf(cfg.autoWakeOnPing) }

    var newTaskName by remember { mutableStateOf("") }
    var newTaskType by remember { mutableStateOf(ScheduledTaskType.WORLD_BACKUP) }
    var newFrequency by remember { mutableStateOf(ScheduleFrequency.INTERVAL) }
    var newIntervalMin by remember { mutableStateOf("360") }
    var newCronExpr by remember { mutableStateOf("0 */6 * * *") }
    var newCommand by remember { mutableStateOf("") }

    var discordEnabled by remember(server.id) { mutableStateOf(server.discordWebhookConfig.enabled) }
    var discordUrl by remember(server.id) { mutableStateOf(server.discordWebhookConfig.webhookUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Smart Idle Sleep & Auto-Wake
        MineServeSettingsGroup(title = "Smart Idle Sleep & Auto-Wake") {
            MineServePreferenceRow(
                title = "Enable Idle Auto-Sleep",
                subtitle = "Automatically stop the server after inactivity to save CPU and RAM",
                checked = idleSleep,
                onCheckedChange = { idleSleep = it },
                icon = Icons.Rounded.PowerSettingsNew
            )
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(
                title = "Auto-Wake on Player Ping",
                subtitle = "Wake the server automatically when a player joins or pings the port",
                checked = autoWake,
                onCheckedChange = { autoWake = it }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = idleMinutes,
                    onValueChange = { idleMinutes = it },
                    label = { Text("Idle Timeout (minutes)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                    modifier = Modifier.width(200.dp)
                )
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
                    shape = ShapeDefaults.Medium
                ) {
                    Text("Save Idle Policy")
                }
            }
        }

        // 2. Scheduled Tasks (Interval & Cron)
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    text = "Scheduled Tasks (${cfg.scheduledTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskName,
                    onValueChange = { newTaskName = it },
                    label = { Text("Task Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.width(200.dp)
                )
                ScheduledTaskType.entries.forEach { t ->
                    FilterChip(
                        selected = newTaskType == t,
                        onClick = { newTaskType = t },
                        label = { Text(t.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.secondaryContainer,
                            selectedLabelColor = colorScheme.onSecondaryContainer,
                            containerColor = colorScheme.surfaceContainerHigh,
                            labelColor = colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = newFrequency == ScheduleFrequency.INTERVAL,
                    onClick = { newFrequency = ScheduleFrequency.INTERVAL },
                    label = { Text("Interval") },
                    border = null
                )
                FilterChip(
                    selected = newFrequency == ScheduleFrequency.CUSTOM_CRON,
                    onClick = { newFrequency = ScheduleFrequency.CUSTOM_CRON },
                    label = { Text("Cron") },
                    border = null
                )
                if (newFrequency == ScheduleFrequency.CUSTOM_CRON) {
                    OutlinedTextField(
                        value = newCronExpr,
                        onValueChange = { newCronExpr = it },
                        label = { Text("Cron Expression") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.width(180.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = newIntervalMin,
                        onValueChange = { newIntervalMin = it },
                        label = { Text("Every (min)") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.width(130.dp)
                    )
                }
                if (newTaskType == ScheduledTaskType.CUSTOM_COMMAND || newTaskType == ScheduledTaskType.BROADCAST_MESSAGE) {
                    OutlinedTextField(
                        value = newCommand,
                        onValueChange = { newCommand = it },
                        label = { Text(if (newTaskType == ScheduledTaskType.CUSTOM_COMMAND) "Console Command" else "Broadcast Text") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        modifier = Modifier.width(240.dp)
                    )
                }
                Button(
                    onClick = {
                        val task = ScheduledTask(
                            id = UUID.randomUUID().toString().take(8),
                            serverId = server.id,
                            name = newTaskName.trim().ifBlank { newTaskType.displayName },
                            enabled = true,
                            taskType = newTaskType,
                            frequency = newFrequency,
                            intervalMinutes = newIntervalMin.toIntOrNull()?.coerceAtLeast(5) ?: 360,
                            cronExpression = if (newFrequency == ScheduleFrequency.CUSTOM_CRON) newCronExpr.trim() else "",
                            command = newCommand.trim()
                        )
                        viewModel.serverRepository.updateServer(
                            server.copy(automationConfig = cfg.copy(scheduledTasks = cfg.scheduledTasks + task))
                        )
                        newTaskName = ""
                        newCommand = ""
                        viewModel.showToast("Added scheduled task ${task.name}")
                    },
                    shape = ShapeDefaults.Medium
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (cfg.scheduledTasks.isEmpty()) {
                MineServeEmptyState(
                    icon = Icons.Rounded.Schedule,
                    title = "No Scheduled Tasks",
                    supportingText = "Add an automated backup, restart, or command schedule above."
                )
            } else {
                MineServeTableContainer(modifier = Modifier.fillMaxWidth()) {
                    cfg.scheduledTasks.forEachIndexed { idx, task ->
                        MineServeTableRow(
                            showDivider = idx < cfg.scheduledTasks.lastIndex,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                MineServeTableMonoText(
                                    text = "${task.taskType.displayName} - ${task.frequencyDescription}",
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Switch(
                                    checked = task.enabled,
                                    onCheckedChange = { enabled ->
                                        val next = cfg.scheduledTasks.map { if (it.id == task.id) it.copy(enabled = enabled) else it }
                                        viewModel.serverRepository.updateServer(server.copy(automationConfig = cfg.copy(scheduledTasks = next)))
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        val next = cfg.scheduledTasks.filterNot { it.id == task.id }
                                        viewModel.serverRepository.updateServer(server.copy(automationConfig = cfg.copy(scheduledTasks = next)))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete task",
                                        tint = colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Discord Webhook Event Notifications
        MineServeSettingsGroup(title = "Discord Webhook Notifications") {
            MineServePreferenceRow(
                title = "Enable Webhook Alerts",
                subtitle = "Send server start, stop, crash, and player events to Discord",
                checked = discordEnabled,
                onCheckedChange = { discordEnabled = it },
                icon = Icons.Rounded.Notifications
            )
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = discordUrl,
                    onValueChange = { discordUrl = it },
                    label = { Text("Discord Webhook URL") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    FilledTonalButton(
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
                        },
                        shape = ShapeDefaults.Medium
                    ) {
                        Text("Test")
                    }
                    Button(
                        onClick = {
                            val updated = server.discordWebhookConfig.copy(
                                enabled = discordEnabled,
                                webhookUrl = discordUrl.trim()
                            )
                            viewModel.serverRepository.updateServer(server.copy(discordWebhookConfig = updated))
                            viewModel.showToast("Saved Discord webhook settings")
                        },
                        shape = ShapeDefaults.Medium
                    ) {
                        Text("Save Webhook")
                    }
                }
            }
        }
    }
}
