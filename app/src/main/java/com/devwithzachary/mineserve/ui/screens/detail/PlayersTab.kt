package com.devwithzachary.mineserve.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.PlayerActionTarget
import com.devwithzachary.mineserve.model.PlayerManagementTab
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerPlayerLists
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.components.player.AddOpDialog
import com.devwithzachary.mineserve.ui.components.player.AddWhitelistDialog
import com.devwithzachary.mineserve.ui.components.player.BanIpDialog
import com.devwithzachary.mineserve.ui.components.player.BanPlayerDialog
import com.devwithzachary.mineserve.ui.components.player.PlayerActionDialog
import com.devwithzachary.mineserve.ui.screens.detail.players.BannedIpsSection
import com.devwithzachary.mineserve.ui.screens.detail.players.BannedPlayersSection
import com.devwithzachary.mineserve.ui.screens.detail.players.OnlinePlayersSection
import com.devwithzachary.mineserve.ui.screens.detail.players.OpsSection
import com.devwithzachary.mineserve.ui.screens.detail.players.WhitelistSection
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import kotlinx.coroutines.launch

@Composable
fun PlayersTab(
    server: MinecraftServer? = null,
    status: ServerStatus = ServerStatus.STOPPED,
    metrics: ServerMetrics?,
    onSendCommand: (String) -> Unit,
    onLoadPlayerLists: (suspend () -> ServerPlayerLists)? = null,
    onAddWhitelistPlayer: (suspend (String) -> Boolean)? = null,
    onRemoveWhitelistPlayer: (suspend (String) -> Boolean)? = null,
    onAddOp: (suspend (String, Int) -> Boolean)? = null,
    onRemoveOp: (suspend (String) -> Boolean)? = null,
    onAddBannedPlayer: (suspend (String, String) -> Boolean)? = null,
    onRemoveBannedPlayer: (suspend (String) -> Boolean)? = null,
    onAddBannedIp: (suspend (String, String) -> Boolean)? = null,
    onRemoveBannedIp: (suspend (String) -> Boolean)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedSection by remember { mutableStateOf(PlayerManagementTab.ONLINE) }
    var playerLists by remember { mutableStateOf(ServerPlayerLists()) }
    var isLoadingLists by remember { mutableStateOf(true) }

    // Dialog states
    var selectedPlayerForAction by remember { mutableStateOf<PlayerActionTarget?>(null) }
    var showAddWhitelistDialog by remember { mutableStateOf(false) }
    var showAddOpDialog by remember { mutableStateOf(false) }
    var showBanPlayerDialog by remember { mutableStateOf(false) }
    var showBanIpDialog by remember { mutableStateOf(false) }

    // Quick Command text input
    var newPlayerName by remember { mutableStateOf("") }
    val onlineList = metrics?.onlinePlayers ?: emptyList()
    val isRunning = status == ServerStatus.RUNNING || status == ServerStatus.STARTING

    fun refreshLists() {
        if (onLoadPlayerLists != null) {
            scope.launch {
                isLoadingLists = true
                playerLists = onLoadPlayerLists()
                isLoadingLists = false
            }
        }
    }

    LaunchedEffect(server?.id) {
        refreshLists()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        // Section Selector Bar with Badge Counts
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(PlayerManagementTab.entries) { tab ->
                    val count = when (tab) {
                        PlayerManagementTab.ONLINE -> onlineList.size
                        PlayerManagementTab.WHITELIST -> playerLists.whitelist.size
                        PlayerManagementTab.OPS -> playerLists.ops.size
                        PlayerManagementTab.BANNED_PLAYERS -> playerLists.bannedPlayers.size
                        PlayerManagementTab.BANNED_IPS -> playerLists.bannedIps.size
                    }
                    val isSelected = selectedSection == tab

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSection = tab },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(tab.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.25f) else Slate800
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Slate400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            IconButton(onClick = { refreshLists() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.players_refresh),
                    tint = Slate400
                )
            }
        }

        // Active Section Content
        when (selectedSection) {
            PlayerManagementTab.ONLINE -> {
                OnlinePlayersSection(
                    onlinePlayers = onlineList,
                    playerLists = playerLists,
                    onPlayerClick = { player ->
                        selectedPlayerForAction = PlayerActionTarget(
                            username = player,
                            isOnline = true,
                            isOp = playerLists.ops.any { it.name.equals(player, ignoreCase = true) },
                            isWhitelisted = playerLists.whitelist.any { it.name.equals(player, ignoreCase = true) },
                            isBanned = playerLists.bannedPlayers.any { it.name.equals(player, ignoreCase = true) }
                        )
                    },
                    onQuickKick = { p -> onSendCommand("kick $p") }
                )
            }

            PlayerManagementTab.WHITELIST -> {
                WhitelistSection(
                    whitelist = playerLists.whitelist,
                    onAddClick = { showAddWhitelistDialog = true },
                    onPlayerClick = { entry ->
                        selectedPlayerForAction = PlayerActionTarget(
                            username = entry.name,
                            uuid = entry.uuid,
                            isOnline = onlineList.any { it.equals(entry.name, ignoreCase = true) },
                            isWhitelisted = true,
                            isOp = playerLists.ops.any { it.name.equals(entry.name, ignoreCase = true) },
                            isBanned = playerLists.bannedPlayers.any { it.name.equals(entry.name, ignoreCase = true) }
                        )
                    },
                    onRemoveClick = { entry ->
                        scope.launch {
                            onRemoveWhitelistPlayer?.invoke(entry.name)
                            refreshLists()
                        }
                    }
                )
            }

            PlayerManagementTab.OPS -> {
                OpsSection(
                    ops = playerLists.ops,
                    onAddClick = { showAddOpDialog = true },
                    onPlayerClick = { entry ->
                        selectedPlayerForAction = PlayerActionTarget(
                            username = entry.name,
                            uuid = entry.uuid,
                            isOnline = onlineList.any { it.equals(entry.name, ignoreCase = true) },
                            isOp = true,
                            isWhitelisted = playerLists.whitelist.any { it.name.equals(entry.name, ignoreCase = true) },
                            isBanned = playerLists.bannedPlayers.any { it.name.equals(entry.name, ignoreCase = true) }
                        )
                    },
                    onRemoveClick = { entry ->
                        scope.launch {
                            onRemoveOp?.invoke(entry.name)
                            refreshLists()
                        }
                    }
                )
            }

            PlayerManagementTab.BANNED_PLAYERS -> {
                BannedPlayersSection(
                    bannedPlayers = playerLists.bannedPlayers,
                    onAddClick = { showBanPlayerDialog = true },
                    onPlayerClick = { entry ->
                        selectedPlayerForAction = PlayerActionTarget(
                            username = entry.name,
                            uuid = entry.uuid,
                            isOnline = false,
                            isBanned = true
                        )
                    },
                    onPardonClick = { entry ->
                        scope.launch {
                            onRemoveBannedPlayer?.invoke(entry.name)
                            refreshLists()
                        }
                    }
                )
            }

            PlayerManagementTab.BANNED_IPS -> {
                BannedIpsSection(
                    bannedIps = playerLists.bannedIps,
                    onAddClick = { showBanIpDialog = true },
                    onPardonClick = { entry ->
                        scope.launch {
                            onRemoveBannedIp?.invoke(entry.ip)
                            refreshLists()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Command Bar (Preserved & Enhanced)
        Text(
            text = stringResource(R.string.players_manage_commands),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it.filterNot { c -> c.isWhitespace() } },
                    label = { Text(stringResource(R.string.players_username_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            if (newPlayerName.isNotBlank()) {
                                scope.launch {
                                    if (onAddOp != null) {
                                        onAddOp.invoke(newPlayerName, 4)
                                    } else {
                                        onSendCommand("op $newPlayerName")
                                    }
                                    newPlayerName = ""
                                    refreshLists()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.players_op),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            if (newPlayerName.isNotBlank()) {
                                scope.launch {
                                    if (onAddWhitelistPlayer != null) {
                                        onAddWhitelistPlayer.invoke(newPlayerName)
                                    } else {
                                        onSendCommand("whitelist add $newPlayerName")
                                    }
                                    newPlayerName = ""
                                    refreshLists()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.players_whitelist),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            if (newPlayerName.isNotBlank()) {
                                scope.launch {
                                    if (onAddBannedPlayer != null) {
                                        onAddBannedPlayer.invoke(newPlayerName, "Banned by an operator.")
                                    } else {
                                        onSendCommand("ban $newPlayerName")
                                    }
                                    newPlayerName = ""
                                    refreshLists()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.players_ban),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Player Action Dialog
    selectedPlayerForAction?.let { target ->
        PlayerActionDialog(
            target = target,
            isServerRunning = isRunning,
            allOnlinePlayers = onlineList,
            onDismiss = {
                selectedPlayerForAction = null
                refreshLists()
            },
            onSendCommand = onSendCommand,
            onToggleWhitelist = { username, add ->
                scope.launch {
                    if (add) onAddWhitelistPlayer?.invoke(username) else onRemoveWhitelistPlayer?.invoke(username)
                    refreshLists()
                }
            },
            onToggleOp = { username, makeOp ->
                scope.launch {
                    if (makeOp) onAddOp?.invoke(username, 4) else onRemoveOp?.invoke(username)
                    refreshLists()
                }
            },
            onBanPlayer = { username, reason ->
                scope.launch {
                    onAddBannedPlayer?.invoke(username, reason)
                    refreshLists()
                }
            },
            onPardonPlayer = { username ->
                scope.launch {
                    onRemoveBannedPlayer?.invoke(username)
                    refreshLists()
                }
            }
        )
    }

    // Add Modals
    if (showAddWhitelistDialog) {
        AddWhitelistDialog(
            onDismiss = { showAddWhitelistDialog = false },
            onConfirm = { username ->
                showAddWhitelistDialog = false
                scope.launch {
                    onAddWhitelistPlayer?.invoke(username)
                    refreshLists()
                }
            }
        )
    }

    if (showAddOpDialog) {
        AddOpDialog(
            onDismiss = { showAddOpDialog = false },
            onConfirm = { username, level ->
                showAddOpDialog = false
                scope.launch {
                    onAddOp?.invoke(username, level)
                    refreshLists()
                }
            }
        )
    }

    if (showBanPlayerDialog) {
        BanPlayerDialog(
            onDismiss = { showBanPlayerDialog = false },
            onConfirm = { username, reason ->
                showBanPlayerDialog = false
                scope.launch {
                    onAddBannedPlayer?.invoke(username, reason)
                    refreshLists()
                }
            }
        )
    }

    if (showBanIpDialog) {
        BanIpDialog(
            onDismiss = { showBanIpDialog = false },
            onConfirm = { ip, reason ->
                showBanIpDialog = false
                scope.launch {
                    onAddBannedIp?.invoke(ip, reason)
                    refreshLists()
                }
            }
        )
    }
}
