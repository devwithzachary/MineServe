package com.devwithzachary.mineserve.ui.screens.detail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.cloud.CloudAccountState
import com.devwithzachary.mineserve.cloud.CloudPricing
import com.devwithzachary.mineserve.cloud.MineServeCloudManager
import com.devwithzachary.mineserve.designsystem.components.MineServePreferenceRow
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.engine.ProxyNetworkManager
import com.devwithzachary.mineserve.model.DiscordWebhookConfig
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ProxyRoute
import com.devwithzachary.mineserve.model.ServerBuildInfo
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.net.discord.DiscordWebhookClient
import com.devwithzachary.mineserve.net.portforward.PortForwardManager
import com.devwithzachary.mineserve.net.portforward.PortForwardState
import com.devwithzachary.mineserve.repository.ServerRepository
import com.devwithzachary.mineserve.ui.components.RamSlider
import com.devwithzachary.mineserve.ui.components.ServerSoftwareCard
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(
    server: MinecraftServer,
    allServers: List<MinecraftServer> = emptyList(),
    status: ServerStatus = ServerStatus.STOPPED,
    initialProperties: ServerProperties,
    onSaveProperties: (ServerProperties) -> Unit,
    onSaveServer: (MinecraftServer) -> Unit = {},
    onCheckForUpdate: suspend (MinecraftServer) -> ServerBuildInfo? = { null },
    onFetchVersions: suspend (ServerType) -> List<String> = { emptyList() },
    onUpdateBuild: (createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onUpgradeVersion: (newVersion: String, createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    cloudAccountState: CloudAccountState = CloudAccountState(),
    onOpenCloudPortal: () -> Unit = {},
    onRefreshCloudAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val scope = rememberCoroutineScope()
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    // Visual editor state
    var props by remember(initialProperties) { mutableStateOf(initialProperties) }
    var motd by remember(props.motd) { mutableStateOf(props.motd) }
    var gamemode by remember(props.gamemode) { mutableStateOf(props.gamemode) }
    var difficulty by remember(props.difficulty) { mutableStateOf(props.difficulty) }
    var pvp by remember(props.pvp) { mutableStateOf(props.pvp) }
    var hardcore by remember(props.hardcore) { mutableStateOf(props.hardcore) }
    var onlineMode by remember(props.onlineMode) { mutableStateOf(props.onlineMode) }
    var whitelist by remember(props.whiteList) { mutableStateOf(props.whiteList) }
    var allowFlight by remember(props.allowFlight) { mutableStateOf(props.allowFlight) }
    var allowNether by remember(props.allowNether) { mutableStateOf(props.allowNether) }
    var maxPlayers by remember(props.maxPlayers) { mutableIntStateOf(props.maxPlayers) }
    var viewDistance by remember(props.viewDistance) { mutableIntStateOf(props.viewDistance) }
    var simulationDistance by remember(props.simulationDistance) { mutableIntStateOf(props.simulationDistance) }
    var levelSeed by remember(props.levelSeed) { mutableStateOf(props.levelSeed) }
    var visualSavedMessage by remember { mutableStateOf<String?>(null) }

    // Tunnel config state
    var tunnelAutoStart by remember(server.tunnelConfig.autoStart) { mutableStateOf(server.tunnelConfig.autoStart) }
    var tunnelProvider by remember(server.tunnelConfig.provider) { mutableStateOf(server.tunnelConfig.provider) }
    var customRelayHost by remember(server.tunnelConfig.customRelayHost) { mutableStateOf(server.tunnelConfig.customRelayHost) }
    var customRelayPort by remember(server.tunnelConfig.customRelayPort) { mutableIntStateOf(server.tunnelConfig.customRelayPort) }
    var playitSecret by remember(server.tunnelConfig.playitSecret) { mutableStateOf(server.tunnelConfig.playitSecret) }
    var cloudSubdomain by remember(server.tunnelConfig.cloudSubdomain) { mutableStateOf(server.tunnelConfig.cloudSubdomain) }
    var cloudRegion by remember(server.tunnelConfig.cloudRegion) { mutableStateOf(server.tunnelConfig.cloudRegion.ifBlank { "us-east" }) }
    var cloudDomain by remember(server.tunnelConfig.cloudDomain) { mutableStateOf(server.tunnelConfig.cloudDomain.ifBlank { "play.mineserve.app" }) }
    var isReservingSubdomain by remember { mutableStateOf(false) }
    var cloudTunnelFeedback by remember { mutableStateOf<String?>(null) }

    // Port forwarding config state
    var portForwardAutoForward by remember(server.portForwardConfig.autoForward) { mutableStateOf(server.portForwardConfig.autoForward) }
    val portForwardManager = remember(isInspection) {
        if (isInspection) null else PortForwardManager.getInstance(context)
    }
    val portForwardStates by (portForwardManager?.states?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(emptyMap()) })
    val currentForwardState = portForwardStates[server.id]
        ?: portForwardManager?.getState(server.id)
        ?: PortForwardState.Idle
    val clipboardManager = LocalClipboardManager.current

    // RAM allocation state
    var currentRamMb by remember(server.allocatedRamMb) { mutableIntStateOf(server.allocatedRamMb) }
    var ramSavedMessage by remember { mutableStateOf<String?>(null) }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        labelColor = MaterialTheme.colorScheme.onSurface
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Server Software & Version Management
        ServerSoftwareCard(
            server = server,
            status = status,
            onCheckForUpdate = onCheckForUpdate,
            onFetchVersions = onFetchVersions,
            onUpdateBuild = onUpdateBuild,
            onUpgradeVersion = onUpgradeVersion,
            onUpdateJavaVersion = { newJava ->
                onSaveServer(server.copy(javaVersion = newJava))
            }
        )

        // Section: Server Memory & RAM Allocation (JVM heap only)
        if (!server.type.isBedrockNative) {
            MineServeSettingsGroup(title = stringResource(R.string.settings_ram_section_title)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${currentRamMb} MB",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = monoFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                val updated = server.copy(allocatedRamMb = currentRamMb)
                                onSaveServer(updated)
                                ramSavedMessage = context.getString(R.string.settings_ram_saved)
                                scope.launch {
                                    delay(2000)
                                    ramSavedMessage = null
                                }
                            },
                            enabled = currentRamMb != server.allocatedRamMb || ramSavedMessage != null,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = ShapeDefaults.Small
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_ram_save_button), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    AnimatedVisibility(visible = ramSavedMessage != null) {
                        SavedBanner(message = ramSavedMessage ?: "")
                    }

                    if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING) {
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = extColors.warningContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = extColors.onWarningContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.settings_ram_running_restart_notice),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extColors.onWarningContainer
                                )
                            }
                        }
                    }

                    RamSlider(
                        allocatedMb = currentRamMb,
                        onValueChange = { currentRamMb = it },
                        showTitle = false
                    )
                }
            }
        }

        // Section: Server Properties (Visual Editor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.settings_tab_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Button(
                onClick = {
                    val updated = props.copy(
                        motd = motd,
                        gamemode = gamemode,
                        difficulty = difficulty,
                        pvp = pvp,
                        hardcore = hardcore,
                        onlineMode = onlineMode,
                        whiteList = whitelist,
                        allowFlight = allowFlight,
                        allowNether = allowNether,
                        maxPlayers = maxPlayers,
                        viewDistance = viewDistance,
                        simulationDistance = simulationDistance,
                        levelSeed = levelSeed
                    )
                    props = updated
                    onSaveProperties(updated)
                    visualSavedMessage = context.getString(R.string.settings_saved_visual)
                    scope.launch {
                        delay(2000)
                        visualSavedMessage = null
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = ShapeDefaults.Small
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.settings_tab_save), fontWeight = FontWeight.SemiBold)
            }
        }

        AnimatedVisibility(visible = visualSavedMessage != null) {
            SavedBanner(message = visualSavedMessage ?: "")
        }

        // MOTD Card
        MineServeSettingsGroup(title = stringResource(R.string.settings_motd_label)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = motd,
                    onValueChange = { motd = it },
                    placeholder = { Text(stringResource(R.string.settings_motd_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Gameplay Settings Group
        MineServeSettingsGroup(title = stringResource(R.string.settings_section_gameplay)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_gamemode),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("survival", "creative", "adventure", "spectator").forEach { mode ->
                            FilterChip(
                                selected = gamemode == mode,
                                onClick = { gamemode = mode },
                                label = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                border = null,
                                colors = chipColors
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.settings_difficulty),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("peaceful", "easy", "normal", "hard").forEach { diff ->
                            FilterChip(
                                selected = difficulty == diff,
                                onClick = { difficulty = diff },
                                label = { Text(diff.replaceFirstChar { it.uppercase() }) },
                                border = null,
                                colors = chipColors
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            MineServePreferenceRow(
                title = stringResource(R.string.settings_pvp),
                checked = pvp,
                onCheckedChange = { pvp = it },
                icon = Icons.Rounded.SportsEsports
            )
            if (!server.type.isBedrockNative) {
                MineServePreferenceRow(
                    title = stringResource(R.string.settings_hardcore),
                    checked = hardcore,
                    onCheckedChange = { hardcore = it }
                )
                MineServePreferenceRow(
                    title = stringResource(R.string.settings_allow_flight),
                    checked = allowFlight,
                    onCheckedChange = { allowFlight = it }
                )
                MineServePreferenceRow(
                    title = stringResource(R.string.settings_allow_nether),
                    checked = allowNether,
                    onCheckedChange = { allowNether = it }
                )
            }
        }

        // Security & Networking Group
        MineServeSettingsGroup(title = stringResource(R.string.settings_section_security)) {
            MineServePreferenceRow(
                title = stringResource(R.string.settings_online_mode),
                checked = onlineMode,
                onCheckedChange = { onlineMode = it },
                icon = Icons.Rounded.Security
            )
            MineServePreferenceRow(
                title = stringResource(R.string.settings_whitelist_only),
                checked = whitelist,
                onCheckedChange = { whitelist = it }
            )
        }

        // World & Sizing Group
        MineServeSettingsGroup(title = stringResource(R.string.settings_section_world)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = maxPlayers.toString(),
                        onValueChange = { maxPlayers = it.toIntOrNull() ?: maxPlayers },
                        label = { Text(stringResource(R.string.settings_max_players)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewDistance.toString(),
                        onValueChange = { viewDistance = it.toIntOrNull() ?: viewDistance },
                        label = { Text(stringResource(R.string.settings_view_distance)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = simulationDistance.toString(),
                        onValueChange = { simulationDistance = it.toIntOrNull() ?: simulationDistance },
                        label = { Text(stringResource(R.string.settings_sim_distance)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = levelSeed,
                        onValueChange = { levelSeed = it },
                        label = { Text(stringResource(R.string.settings_level_seed_label)) },
                        placeholder = { Text(stringResource(R.string.settings_level_seed_placeholder)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Router Port Forwarding (UPnP & NAT-PMP)
        MineServeSettingsGroup(title = "Router Port Forwarding (UPnP / NAT-PMP)") {
            MineServePreferenceRow(
                title = "Auto-Forward Port on Server Boot",
                subtitle = "Automatically configure UPnP or NAT-PMP on your local Wi-Fi router for direct WAN play.",
                checked = portForwardAutoForward,
                onCheckedChange = {
                    portForwardAutoForward = it
                    val updated = server.portForwardConfig.copy(autoForward = it, enabled = it)
                    onSaveServer(server.copy(portForwardConfig = updated))
                },
                icon = Icons.Rounded.Router
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (val fState = currentForwardState) {
                    is PortForwardState.Mapped -> {
                        Surface(
                            shape = ShapeDefaults.Medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Public Direct Address",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = ShapeDefaults.Small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "MAPPED",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                MineServeTableMonoText(
                                    text = fState.directAddress,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                MineServeTableMonoText(
                                    text = "${fState.internalIp}:${fState.internalPort} (${fState.protocol}) - ${fState.gatewayName}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(fState.directAddress))
                                            Toast.makeText(context, "Copied ${fState.directAddress} to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        shape = ShapeDefaults.Small,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy IP", fontWeight = FontWeight.SemiBold)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            portForwardManager?.releasePort(server.id)
                                            Toast.makeText(context, "Released port mapping.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = ShapeDefaults.Small,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Release")
                                    }
                                }
                            }
                        }
                    }

                    is PortForwardState.Discovering -> {
                        Surface(
                            shape = ShapeDefaults.Medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = fState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is PortForwardState.Failed -> {
                        Surface(
                            shape = ShapeDefaults.Medium,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Port Forwarding Failed",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = fState.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                FilledTonalButton(
                                    onClick = { portForwardManager?.forwardPortAsync(server) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = ShapeDefaults.Small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Router Mapping")
                                }
                            }
                        }
                    }

                    is PortForwardState.Idle -> {
                        FilledTonalButton(
                            onClick = { portForwardManager?.forwardPortAsync(server) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Router, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Forward Port ${server.port} Now")
                        }
                    }
                }
            }
        }

        // Public Multiplayer Tunneling
        MineServeSettingsGroup(title = stringResource(R.string.settings_tunnel_section_title)) {
            MineServePreferenceRow(
                title = stringResource(R.string.settings_tunnel_autostart),
                subtitle = stringResource(R.string.settings_tunnel_section_desc),
                checked = tunnelAutoStart,
                onCheckedChange = {
                    tunnelAutoStart = it
                    val updated = server.tunnelConfig.copy(autoStart = it)
                    onSaveServer(server.copy(tunnelConfig = updated))
                },
                icon = Icons.Rounded.Language
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_tunnel_provider_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        TunnelProvider.BORE to stringResource(R.string.settings_tunnel_provider_bore),
                        TunnelProvider.PLAYIT to stringResource(R.string.settings_tunnel_provider_playit),
                        TunnelProvider.CUSTOM_BORE to stringResource(R.string.settings_tunnel_provider_custom),
                        TunnelProvider.MINESERVE_CLOUD to "MineServe Cloud"
                    ).forEach { (provider, label) ->
                        FilterChip(
                            selected = tunnelProvider == provider,
                            onClick = {
                                tunnelProvider = provider
                                val updated = server.tunnelConfig.copy(provider = provider)
                                onSaveServer(server.copy(tunnelConfig = updated))
                            },
                            label = { Text(label) },
                            border = null,
                            colors = chipColors
                        )
                    }
                }

                if (tunnelProvider == TunnelProvider.MINESERVE_CLOUD) {
                    val profile = cloudAccountState.profile
                    val tier = profile?.tier?.uppercase() ?: "FREE"
                    Surface(
                        shape = ShapeDefaults.Medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!cloudAccountState.isLinked) {
                                Text(
                                    text = "Link your MineServe Cloud account in Settings to use custom *.play.mineserve.app addresses and remote panel access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilledTonalButton(
                                    onClick = onOpenCloudPortal,
                                    shape = ShapeDefaults.Small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (BuildConfig.IS_PLAY_STORE) "Sign In to MineServe Cloud" else "Open MineServe Cloud Portal"
                                    )
                                }
                            } else if (tier == "FREE") {
                                if (BuildConfig.IS_PLAY_STORE) {
                                    Text(
                                        text = "Custom *.play.mineserve.app tunnels require an active MineServe Plus or Pro membership. Plan changes and purchases are not available in this app.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extColors.warning
                                    )
                                } else {
                                    val pricing = remember { CloudPricing.current() }
                                    Text(
                                        text = "Custom *.play.mineserve.app tunnels require MineServe Plus (7-day free trial, then ${pricing.plusMonthly}/mo, 1 address) or Pro (${pricing.proMonthly}/mo, 3 addresses).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extColors.warning
                                    )
                                    FilledTonalButton(
                                        onClick = onOpenCloudPortal,
                                        shape = ShapeDefaults.Small,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Start 7-Day Free Trial in Cloud Portal")
                                    }
                                }
                            } else {
                                val existingSubs = profile?.activeSubdomains.orEmpty()
                                val subDetails = profile?.activeSubdomainDetails.orEmpty()
                                val matchedCloudRegion = subDetails.firstOrNull {
                                    it.subdomain.equals(cloudSubdomain, ignoreCase = true)
                                }?.region
                                LaunchedEffect(matchedCloudRegion) {
                                    if (!matchedCloudRegion.isNullOrBlank() && matchedCloudRegion != cloudRegion) {
                                        cloudRegion = matchedCloudRegion
                                        onSaveServer(
                                            server.copy(
                                                tunnelConfig = server.tunnelConfig.copy(
                                                    provider = TunnelProvider.MINESERVE_CLOUD,
                                                    cloudSubdomain = cloudSubdomain,
                                                    cloudRegion = matchedCloudRegion
                                                )
                                            )
                                        )
                                    }
                                }
                                if (existingSubs.isNotEmpty()) {
                                    Text(
                                        text = "Your Claimed Addresses",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        existingSubs.forEach { sub ->
                                            val subInfo = subDetails.firstOrNull { it.subdomain.equals(sub, ignoreCase = true) }
                                            val subRegion = subInfo?.region
                                            val subDomain = subInfo?.domain?.ifBlank { "play.mineserve.app" } ?: "play.mineserve.app"
                                            val regionTag = when (subRegion) {
                                                "us-east" -> "US East"
                                                "eu-west" -> "EU West"
                                                "eu-central" -> "EU Central"
                                                "ap-southeast" -> "Asia Pacific"
                                                else -> subRegion
                                            }
                                            FilterChip(
                                                selected = cloudSubdomain.equals(sub, ignoreCase = true),
                                                onClick = {
                                                    val nextRegion = subRegion ?: cloudRegion
                                                    cloudSubdomain = sub
                                                    cloudRegion = nextRegion
                                                    cloudDomain = subDomain
                                                    onSaveServer(
                                                        server.copy(
                                                            tunnelConfig = server.tunnelConfig.copy(
                                                                provider = TunnelProvider.MINESERVE_CLOUD,
                                                                cloudSubdomain = sub,
                                                                cloudRegion = nextRegion,
                                                                cloudDomain = subDomain
                                                            )
                                                        )
                                                    )
                                                },
                                                label = {
                                                    val text = if (!regionTag.isNullOrBlank()) {
                                                        "$sub.$subDomain ($regionTag)"
                                                    } else {
                                                        "$sub.$subDomain"
                                                    }
                                                    Text(text, style = MaterialTheme.typography.labelSmall)
                                                },
                                                border = null,
                                                colors = chipColors
                                            )
                                        }
                                    }
                                }

                                val effectiveDomain = if (tier == "PRO") cloudDomain else "play.mineserve.app"
                                LaunchedEffect(tier, cloudDomain) {
                                    if (tier != "PRO" && cloudDomain != "play.mineserve.app") {
                                        cloudDomain = "play.mineserve.app"
                                        onSaveServer(
                                            server.copy(
                                                tunnelConfig = server.tunnelConfig.copy(
                                                    provider = TunnelProvider.MINESERVE_CLOUD,
                                                    cloudDomain = "play.mineserve.app"
                                                )
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = cloudSubdomain,
                                    onValueChange = { raw ->
                                        val clean = raw.lowercase().filter { it.isLetterOrDigit() || it == '-' }.take(32)
                                        cloudSubdomain = clean
                                        onSaveServer(
                                            server.copy(
                                                tunnelConfig = server.tunnelConfig.copy(
                                                    provider = TunnelProvider.MINESERVE_CLOUD,
                                                    cloudSubdomain = clean,
                                                    cloudRegion = cloudRegion,
                                                    cloudDomain = effectiveDomain
                                                )
                                            )
                                        )
                                    },
                                    label = { Text("Subdomain") },
                                    placeholder = { Text("my-smp", fontFamily = monoFont) },
                                    suffix = {
                                        Text(
                                            text = ".$effectiveDomain",
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontFamily = monoFont
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (tier == "PRO") {
                                    Text(
                                        text = "Domain Suffix (Pro)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("play.mineserve.app" to ".play.mineserve.app", "playms.uk" to ".playms.uk (Short)").forEach { (dom, label) ->
                                            FilterChip(
                                                selected = cloudDomain == dom,
                                                onClick = {
                                                    cloudDomain = dom
                                                    onSaveServer(
                                                        server.copy(
                                                            tunnelConfig = server.tunnelConfig.copy(
                                                                provider = TunnelProvider.MINESERVE_CLOUD,
                                                                cloudSubdomain = cloudSubdomain,
                                                                cloudRegion = cloudRegion,
                                                                cloudDomain = dom
                                                            )
                                                        )
                                                    )
                                                },
                                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                                border = null,
                                                colors = chipColors
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Relay Region",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "us-east" to "US East",
                                        "eu-west" to "EU West (London)",
                                        "eu-central" to "EU Central",
                                        "ap-southeast" to "Asia Pacific"
                                    ).forEach { (id, label) ->
                                        FilterChip(
                                            selected = cloudRegion == id,
                                            onClick = {
                                                cloudRegion = id
                                                onSaveServer(
                                                    server.copy(
                                                        tunnelConfig = server.tunnelConfig.copy(
                                                            provider = TunnelProvider.MINESERVE_CLOUD,
                                                            cloudSubdomain = cloudSubdomain,
                                                            cloudRegion = id
                                                        )
                                                    )
                                                )
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            border = null,
                                            colors = chipColors
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        isReservingSubdomain = true
                                        cloudTunnelFeedback = null
                                        scope.launch {
                                            val res = MineServeCloudManager.getInstance(context).allocateCloudTunnel(
                                                serverId = server.id,
                                                subdomain = cloudSubdomain,
                                                region = cloudRegion,
                                                domain = if (tier == "PRO") cloudDomain else "play.mineserve.app"
                                            )
                                            isReservingSubdomain = false
                                            res.fold(
                                                onSuccess = { alloc ->
                                                    cloudTunnelFeedback = "Reserved ${alloc.hostname} (Panel: ${alloc.panelHostname})"
                                                    onRefreshCloudAccount()
                                                },
                                                onFailure = { err ->
                                                    cloudTunnelFeedback = err.message ?: "Failed to reserve address"
                                                }
                                            )
                                        }
                                    },
                                    enabled = !isReservingSubdomain && cloudSubdomain.length >= 3,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = ShapeDefaults.Small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isReservingSubdomain) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reserving Address...", fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Text("Reserve Cloud Address", fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                cloudTunnelFeedback?.let { msg ->
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (msg.startsWith("Reserved")) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (tunnelProvider == TunnelProvider.PLAYIT) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = playitSecret,
                            onValueChange = {
                                playitSecret = it
                                val updated = server.tunnelConfig.copy(playitSecret = it.trim())
                                onSaveServer(server.copy(tunnelConfig = updated))
                            },
                            label = { Text(stringResource(R.string.settings_tunnel_playit_key_label)) },
                            placeholder = { Text(stringResource(R.string.settings_tunnel_playit_key_placeholder)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.settings_tunnel_playit_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (tunnelProvider == TunnelProvider.CUSTOM_BORE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customRelayHost,
                            onValueChange = {
                                customRelayHost = it
                                val updated = server.tunnelConfig.copy(
                                    provider = TunnelProvider.CUSTOM_BORE,
                                    customRelayHost = it.trim(),
                                    customRelayPort = customRelayPort,
                                    playitSecret = playitSecret
                                )
                                onSaveServer(server.copy(tunnelConfig = updated))
                            },
                            label = { Text(stringResource(R.string.settings_tunnel_relay_host)) },
                            placeholder = { Text(stringResource(R.string.settings_tunnel_relay_host_placeholder)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )

                        OutlinedTextField(
                            value = customRelayPort.toString(),
                            onValueChange = {
                                val port = it.toIntOrNull() ?: customRelayPort
                                customRelayPort = port
                                val updated = server.tunnelConfig.copy(
                                    provider = TunnelProvider.CUSTOM_BORE,
                                    customRelayHost = customRelayHost.trim(),
                                    customRelayPort = port,
                                    playitSecret = playitSecret
                                )
                                onSaveServer(server.copy(tunnelConfig = updated))
                            },
                            label = { Text(stringResource(R.string.settings_tunnel_port)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Proxy Network Orchestration / Internal Loopback Binding
        if (server.type.isProxy) {
            val serverRepo = remember(context, isInspection) {
                if (isInspection) null else ServerRepository(context, PRootEngine(context))
            }
            val candidateBackends = remember(allServers, server.id) {
                allServers.filter { it.id != server.id && !it.type.isProxy }
            }
            var routes by remember(server.id, server.proxyConfig) {
                mutableStateOf(server.proxyConfig.routes)
            }
            var proxyOnlineMode by remember(server.id, server.proxyConfig) {
                mutableStateOf(server.proxyConfig.onlineMode)
            }

            MineServeSettingsGroup(title = "Proxy Network & Linked Servers") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Link backend servers on this device to your ${server.type.displayName}. Applying writes ${if (server.type == ServerType.VELOCITY) "velocity.toml" else "config.yml"}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MineServePreferenceRow(
                    title = "Proxy Online Mode (Mojang Auth)",
                    checked = proxyOnlineMode,
                    onCheckedChange = { proxyOnlineMode = it },
                    icon = Icons.Rounded.Public
                )

                if (candidateBackends.isEmpty()) {
                    Text(
                        text = "No backend servers found. Create a Paper, Purpur, Fabric, or Vanilla server to link it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    candidateBackends.forEachIndexed { index, backend ->
                        val isLinked = routes.any { it.targetServerId == backend.id }
                        val alias = routes.firstOrNull { it.targetServerId == backend.id }?.alias
                            ?: ProxyNetworkManager.sanitizeAlias(backend.name)
                        MineServePreferenceRow(
                            title = backend.name,
                            subtitle = "${backend.type.displayName} | 127.0.0.1:${backend.port} | alias: $alias",
                            checked = isLinked,
                            onCheckedChange = { checked ->
                                routes = if (checked) {
                                    routes + ProxyRoute(
                                        alias = ProxyNetworkManager.sanitizeAlias(backend.name),
                                        targetServerId = backend.id,
                                        host = "127.0.0.1",
                                        port = backend.port
                                    )
                                } else {
                                    routes.filterNot { it.targetServerId == backend.id }
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val draft = server.copy(
                                    proxyConfig = server.proxyConfig.copy(
                                        routes = routes,
                                        tryOrder = routes.map { it.alias },
                                        onlineMode = proxyOnlineMode
                                    )
                                )
                                val appliedCfg = ProxyNetworkManager.applyProxyConfig(
                                    proxyServer = draft,
                                    proxyDir = serverRepo?.getServerDirectory(server.id) ?: java.io.File("/preview"),
                                    allServers = allServers
                                )
                                onSaveServer(draft.copy(proxyConfig = appliedCfg))
                                Toast.makeText(context, "Proxy config applied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Config", fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        enabled = routes.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                routes.forEach { r ->
                                    if (serverRepo != null) {
                                        ProxyNetworkManager.setBackendLoopbackMode(serverRepo, r.targetServerId, true)
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    "Bound ${routes.size} backend(s) to 127.0.0.1 (online-mode=false)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bind 127.0.0.1")
                    }
                }
            }
        } else if (!server.type.isBedrockNative) {
            val isLoopbackBound = props.serverIp.trim() == "127.0.0.1"
            MineServeSettingsGroup(title = "Proxy Backend") {
                MineServePreferenceRow(
                    title = "Internal Loopback Binding",
                    subtitle = "Bind server-ip to 127.0.0.1 and set online-mode=false so this server is only reachable through Velocity or BungeeCord.",
                    checked = isLoopbackBound,
                    onCheckedChange = { enabled ->
                        val updated = props.copy(
                            serverIp = if (enabled) "127.0.0.1" else "",
                            onlineMode = !enabled
                        )
                        props = updated
                        onSaveProperties(updated)
                        Toast.makeText(
                            context,
                            if (enabled) "Bound to 127.0.0.1 (proxy backend mode)" else "Restored public binding (0.0.0.0)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    icon = Icons.Rounded.Public
                )
            }
        }

        // Discord Webhook & Event Bridge
        var webhookEnabled by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.enabled)
        }
        var webhookUrl by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.webhookUrl)
        }
        var notifyStart by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.notifyServerStart)
        }
        var notifyStop by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.notifyServerStop)
        }
        var notifyCrash by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.notifyServerCrash)
        }
        var notifyJoin by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.notifyPlayerJoin)
        }
        var notifyLeave by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.notifyPlayerLeave)
        }
        var notifyBackup by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.notifyBackupComplete)
        }
        var includeSkins by remember(server.id, server.discordWebhookConfig) {
            mutableStateOf(server.discordWebhookConfig.includePlayerSkins)
        }
        var isTestingWebhook by remember { mutableStateOf(false) }

        fun buildDiscordConfig() = DiscordWebhookConfig(
            enabled = webhookEnabled,
            webhookUrl = webhookUrl.trim(),
            notifyServerStart = notifyStart,
            notifyServerStop = notifyStop,
            notifyServerCrash = notifyCrash,
            notifyPlayerJoin = notifyJoin,
            notifyPlayerLeave = notifyLeave,
            notifyBackupComplete = notifyBackup,
            includePlayerSkins = includeSkins
        )

        MineServeSettingsGroup(title = "Discord Webhook Bridge") {
            MineServePreferenceRow(
                title = "Enable Discord Bridge",
                subtitle = "Broadcast server lifecycle, player join/leave, and backup events to a Discord channel.",
                checked = webhookEnabled,
                onCheckedChange = {
                    webhookEnabled = it
                    onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                },
                icon = Icons.Rounded.NotificationsActive
            )

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Discord Webhook URL") },
                    placeholder = { Text("https://discord.com/api/webhooks/...", fontFamily = monoFont) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(visible = webhookEnabled) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    MineServePreferenceRow("Server Online", checked = notifyStart, onCheckedChange = {
                        notifyStart = it
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                    })
                    MineServePreferenceRow("Server Offline", checked = notifyStop, onCheckedChange = {
                        notifyStop = it
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                    })
                    MineServePreferenceRow("Crash / Unexpected Exit", checked = notifyCrash, onCheckedChange = {
                        notifyCrash = it
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                    })
                    MineServePreferenceRow("Player Join", checked = notifyJoin, onCheckedChange = {
                        notifyJoin = it
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                    })
                    MineServePreferenceRow("Player Leave", checked = notifyLeave, onCheckedChange = {
                        notifyLeave = it
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                    })
                    MineServePreferenceRow("Backup Completed", checked = notifyBackup, onCheckedChange = {
                        notifyBackup = it
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                    })
                    MineServePreferenceRow("Include Player Avatar Skins", checked = includeSkins, onCheckedChange = {
                        includeSkins = it
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                    })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onSaveServer(server.copy(discordWebhookConfig = buildDiscordConfig()))
                        Toast.makeText(context, "Discord webhook settings saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = ShapeDefaults.Small,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    enabled = !isTestingWebhook && webhookUrl.isNotBlank(),
                    onClick = {
                        isTestingWebhook = true
                        scope.launch {
                            val res = DiscordWebhookClient.sendTestMessage(webhookUrl, server.name)
                            isTestingWebhook = false
                            val msg = res.fold(
                                onSuccess = { "Test message sent to Discord!" },
                                onFailure = { err -> err.message ?: "Failed to send test message" }
                            )
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = ShapeDefaults.Small,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTestingWebhook) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test Webhook")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SavedBanner(message: String) {
    Surface(
        shape = ShapeDefaults.Small,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeTabPreviews
@Composable
private fun SettingsTabPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        SettingsTab(
            server = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.runningPaperServer,
            allServers = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleServers,
            status = ServerStatus.RUNNING,
            initialProperties = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleProperties,
            onSaveProperties = {},
            cloudAccountState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleCloudLinkedState
        )
    }
}
