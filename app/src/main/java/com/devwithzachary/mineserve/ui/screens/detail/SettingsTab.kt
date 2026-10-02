package com.devwithzachary.mineserve.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.TunnelConfig
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.ui.components.TunnelSecurityWarningCard
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.devwithzachary.mineserve.model.ServerBuildInfo
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.ui.components.RamSlider
import com.devwithzachary.mineserve.ui.components.ServerSoftwareCard
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(
    server: MinecraftServer,
    status: ServerStatus = ServerStatus.STOPPED,
    initialProperties: ServerProperties,
    onSaveProperties: (ServerProperties) -> Unit,
    onSaveServer: (MinecraftServer) -> Unit = {},
    onCheckForUpdate: suspend (MinecraftServer) -> ServerBuildInfo? = { null },
    onFetchVersions: suspend (ServerType) -> List<String> = { emptyList() },
    onUpdateBuild: (createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onUpgradeVersion: (newVersion: String, createBackup: Boolean, onProgress: (String, Int) -> Unit, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    // RAM allocation state
    var currentRamMb by remember(server.allocatedRamMb) { mutableIntStateOf(server.allocatedRamMb) }
    var ramSavedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        // Section: Server Software & Version Management
        ServerSoftwareCard(
            server = server,
            status = status,
            onCheckForUpdate = onCheckForUpdate,
            onFetchVersions = onFetchVersions,
            onUpdateBuild = onUpdateBuild,
            onUpgradeVersion = onUpgradeVersion
        )

        // Section: Server Memory & RAM Allocation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.settings_ram_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    disabledContainerColor = Slate800,
                    disabledContentColor = Slate400
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = if (currentRamMb != server.allocatedRamMb) Color.Black else Slate400,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_ram_save_button),
                    color = if (currentRamMb != server.allocatedRamMb) Color.Black else Slate400,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = ramSavedMessage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldDark.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                    Text(text = ramSavedMessage ?: "", color = EmeraldLight, fontSize = 13.sp)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (status == ServerStatus.RUNNING || status == ServerStatus.STARTING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldYellow.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GoldYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_ram_running_restart_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldYellow,
                            fontSize = 12.sp
                        )
                    }
                }

                RamSlider(
                    allocatedMb = currentRamMb,
                    onValueChange = { currentRamMb = it },
                    showTitle = false
                )
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
                Icon(Icons.Default.Tune, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.settings_tab_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_tab_save),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = visualSavedMessage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldDark.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                    Text(text = visualSavedMessage ?: "", color = EmeraldLight, fontSize = 13.sp)
                }
            }
        }

        // MOTD Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_motd_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400
                )
                OutlinedTextField(
                    value = motd,
                    onValueChange = { motd = it },
                    placeholder = { Text(stringResource(R.string.settings_motd_placeholder), color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Gameplay Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_section_gameplay), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)

                // Gamemode
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.settings_gamemode), style = MaterialTheme.typography.labelSmall, color = Slate400)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("survival", "creative", "adventure", "spectator").forEach { mode ->
                            FilterChip(
                                selected = gamemode == mode,
                                onClick = { gamemode = mode },
                                label = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Slate800,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Difficulty
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.settings_difficulty), style = MaterialTheme.typography.labelSmall, color = Slate400)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("peaceful", "easy", "normal", "hard").forEach { diff ->
                            FilterChip(
                                selected = difficulty == diff,
                                onClick = { difficulty = diff },
                                label = { Text(diff.replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Slate800,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Switches
                SettingSwitchRow(
                    title = stringResource(R.string.settings_pvp),
                    checked = pvp,
                    onCheckedChange = { pvp = it }
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_hardcore),
                    checked = hardcore,
                    onCheckedChange = { hardcore = it }
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_allow_flight),
                    checked = allowFlight,
                    onCheckedChange = { allowFlight = it }
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_allow_nether),
                    checked = allowNether,
                    onCheckedChange = { allowNether = it }
                )
            }
        }

        // Security & Networking Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.settings_section_security), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)

                SettingSwitchRow(
                    title = stringResource(R.string.settings_online_mode),
                    checked = onlineMode,
                    onCheckedChange = { onlineMode = it }
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_whitelist_only),
                    checked = whitelist,
                    onCheckedChange = { whitelist = it }
                )
            }
        }

        // World & Sizing Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.settings_section_world), style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = maxPlayers.toString(),
                        onValueChange = { maxPlayers = it.toIntOrNull() ?: maxPlayers },
                        label = { Text(stringResource(R.string.settings_max_players)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewDistance.toString(),
                        onValueChange = { viewDistance = it.toIntOrNull() ?: viewDistance },
                        label = { Text(stringResource(R.string.settings_view_distance)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = simulationDistance.toString(),
                        onValueChange = { simulationDistance = it.toIntOrNull() ?: simulationDistance },
                        label = { Text(stringResource(R.string.settings_sim_distance)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = levelSeed,
                        onValueChange = { levelSeed = it },
                        label = { Text(stringResource(R.string.settings_level_seed_label)) },
                        placeholder = { Text(stringResource(R.string.settings_level_seed_placeholder), color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section: Public Multiplayer Tunneling
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_tunnel_section_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.settings_tunnel_section_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 12.sp
                )

                SettingSwitchRow(
                    title = stringResource(R.string.settings_tunnel_autostart),
                    checked = tunnelAutoStart,
                    onCheckedChange = {
                        tunnelAutoStart = it
                        val updated = server.tunnelConfig.copy(autoStart = it)
                        onSaveServer(server.copy(tunnelConfig = updated))
                    }
                )

                // Provider Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.settings_tunnel_provider_label), style = MaterialTheme.typography.labelSmall, color = Slate400)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = tunnelProvider == TunnelProvider.BORE,
                            onClick = {
                                tunnelProvider = TunnelProvider.BORE
                                val updated = server.tunnelConfig.copy(provider = TunnelProvider.BORE)
                                onSaveServer(server.copy(tunnelConfig = updated))
                            },
                            label = { Text(stringResource(R.string.settings_tunnel_provider_bore)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = Slate800,
                                labelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = tunnelProvider == TunnelProvider.PLAYIT,
                            onClick = {
                                tunnelProvider = TunnelProvider.PLAYIT
                                val updated = server.tunnelConfig.copy(provider = TunnelProvider.PLAYIT)
                                onSaveServer(server.copy(tunnelConfig = updated))
                            },
                            label = { Text(stringResource(R.string.settings_tunnel_provider_playit)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = Slate800,
                                labelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = tunnelProvider == TunnelProvider.CUSTOM_BORE,
                            onClick = {
                                tunnelProvider = TunnelProvider.CUSTOM_BORE
                                val updated = server.tunnelConfig.copy(provider = TunnelProvider.CUSTOM_BORE)
                                onSaveServer(server.copy(tunnelConfig = updated))
                            },
                            label = { Text(stringResource(R.string.settings_tunnel_provider_custom)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = Slate800,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // Playit.gg configuration
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
                            placeholder = { Text(stringResource(R.string.settings_tunnel_playit_key_placeholder), color = Slate400) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.settings_tunnel_playit_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                // Custom Bore configuration
                if (tunnelProvider == TunnelProvider.CUSTOM_BORE) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                placeholder = { Text(stringResource(R.string.settings_tunnel_relay_host_placeholder), color = Slate400) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
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
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldPrimary,
                checkedTrackColor = EmeraldDark,
                uncheckedTrackColor = Slate800
            )
        )
    }
}
