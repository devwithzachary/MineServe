package com.devwithzachary.mineserve.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.PlayerActionTarget
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900

private enum class ActionTab(val labelRes: Int) {
    ACTIONS(R.string.player_action_tab_actions),
    GAMEMODE(R.string.player_action_tab_gamemode),
    TELEPORT(R.string.player_action_tab_teleport),
    ITEMS(R.string.player_action_tab_items)
}

@Composable
fun PlayerActionDialog(
    target: PlayerActionTarget,
    isServerRunning: Boolean,
    allOnlinePlayers: List<String>,
    onDismiss: () -> Unit,
    onSendCommand: (String) -> Unit,
    onToggleWhitelist: (String, Boolean) -> Unit,
    onToggleOp: (String, Boolean) -> Unit,
    onBanPlayer: (String, String) -> Unit,
    onPardonPlayer: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    // Kick / Ban state
    var kickReason by remember { mutableStateOf("") }
    var banReason by remember { mutableStateOf("") }

    // Teleport state
    var coordX by remember { mutableStateOf("0") }
    var coordY by remember { mutableStateOf("100") }
    var coordZ by remember { mutableStateOf("0") }
    var targetTpPlayer by remember {
        mutableStateOf(allOnlinePlayers.firstOrNull { it != target.username } ?: "")
    }

    // Give items state
    var itemId by remember { mutableStateOf("minecraft:diamond") }
    var itemAmount by remember { mutableIntStateOf(1) }

    val popularItems = remember {
        listOf(
            "minecraft:diamond" to "Diamond",
            "minecraft:iron_ingot" to "Iron",
            "minecraft:golden_apple" to "G-Apple",
            "minecraft:cooked_beef" to "Steak",
            "minecraft:bread" to "Bread",
            "minecraft:torch" to "Torch",
            "minecraft:elytra" to "Elytra",
            "minecraft:oak_log" to "Wood"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerAvatar(
                        username = target.username,
                        uuid = target.uuid,
                        size = 52.dp,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = target.username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (target.isOnline) EmeraldPrimary.copy(alpha = 0.2f) else Slate800,
                                border = BorderStroke(1.dp, if (target.isOnline) EmeraldPrimary else Slate400.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = stringResource(if (target.isOnline) R.string.player_action_status_online else R.string.player_action_status_offline),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (target.isOnline) EmeraldPrimary else Slate400,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            if (target.isOp) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldYellow.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, GoldYellow)
                                ) {
                                    Text(
                                        text = stringResource(R.string.player_action_badge_op),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldYellow,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (target.isWhitelisted) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldPrimary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.player_action_badge_whitelisted),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (target.isBanned) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RedstoneRed.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, RedstoneRed)
                                ) {
                                    Text(
                                        text = stringResource(R.string.player_action_badge_banned),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedstoneRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Slate400)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tab Selection Row
                androidx.compose.material3.SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = ObsidianCard,
                    contentColor = EmeraldPrimary
                ) {
                    ActionTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) EmeraldPrimary else Slate400
                                )
                            }
                        )
                    }
                }

                // Feedback confirmation banner
                if (actionFeedback != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = actionFeedback ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                when (ActionTab.entries[selectedTabIndex]) {
                    ActionTab.ACTIONS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Kick Section (if server is running)
                            if (isServerRunning) {
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Slate900),
                                    border = BorderStroke(1.dp, ObsidianCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.player_action_kick),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        OutlinedTextField(
                                            value = kickReason,
                                            onValueChange = { kickReason = it },
                                            placeholder = { Text(stringResource(R.string.player_action_kick_default_reason)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = {
                                                val reason = kickReason.trim().ifBlank { "Kicked by an operator" }
                                                onSendCommand("kick ${target.username} $reason")
                                                actionFeedback = "Kicked ${target.username}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = RedstoneLight),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.player_action_kick), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Ban / Pardon Section
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate900),
                                border = BorderStroke(1.dp, ObsidianCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (target.isBanned) stringResource(R.string.player_action_pardon) else stringResource(R.string.player_action_ban),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (target.isBanned) EmeraldPrimary else RedstoneRed
                                    )

                                    if (!target.isBanned) {
                                        OutlinedTextField(
                                            value = banReason,
                                            onValueChange = { banReason = it },
                                            placeholder = { Text(stringResource(R.string.player_action_ban_default_reason)) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = {
                                                val reason = banReason.trim().ifBlank { "Banned by an operator" }
                                                onBanPlayer(target.username, reason)
                                                actionFeedback = "Banned ${target.username}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.player_action_ban), fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                onPardonPlayer(target.username)
                                                actionFeedback = "Pardoned ${target.username}"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.player_action_pardon), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Whitelist & OP Quick Toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val newState = !target.isWhitelisted
                                        onToggleWhitelist(target.username, newState)
                                        actionFeedback = if (newState) "Added ${target.username} to whitelist" else "Removed ${target.username} from whitelist"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(if (target.isWhitelisted) R.string.player_action_remove_whitelist else R.string.player_action_add_whitelist),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val newState = !target.isOp
                                        onToggleOp(target.username, newState)
                                        actionFeedback = if (newState) "Granted OP to ${target.username}" else "Removed OP from ${target.username}"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(if (target.isOp) R.string.player_action_deop else R.string.player_action_make_op),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    ActionTab.GAMEMODE -> {
                        if (!isServerRunning) {
                            ServerOfflineNotice()
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = stringResource(R.string.player_action_gamemode_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                val modes = listOf(
                                    "survival" to (R.string.player_action_gamemode_survival to "⚔️"),
                                    "creative" to (R.string.player_action_gamemode_creative to "🧱"),
                                    "adventure" to (R.string.player_action_gamemode_adventure to "🗺️"),
                                    "spectator" to (R.string.player_action_gamemode_spectator to "👁️")
                                )

                                for ((modeKey, info) in modes) {
                                    val (labelRes, iconEmoji) = info
                                    Button(
                                        onClick = {
                                            onSendCommand("gamemode $modeKey ${target.username}")
                                            actionFeedback = "Changed gamemode to $modeKey for ${target.username}"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                                        border = BorderStroke(1.dp, ObsidianCardBorder),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(iconEmoji, fontSize = 16.sp)
                                                Text(stringResource(labelRes), fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "/gamemode $modeKey",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate400
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ActionTab.TELEPORT -> {
                        if (!isServerRunning) {
                            ServerOfflineNotice()
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = stringResource(R.string.player_action_teleport_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                // Teleport to World Spawn
                                Button(
                                    onClick = {
                                        onSendCommand("tp ${target.username} 0 ~ 0")
                                        actionFeedback = "Teleported ${target.username} to spawn"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = EmeraldPrimary),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.player_action_tp_spawn), fontWeight = FontWeight.Bold)
                                }

                                // Teleport to Coordinates
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Slate900),
                                    border = BorderStroke(1.dp, ObsidianCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.player_action_tp_coords),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = coordX,
                                                onValueChange = { coordX = it },
                                                label = { Text(stringResource(R.string.player_action_coord_x)) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = coordY,
                                                onValueChange = { coordY = it },
                                                label = { Text(stringResource(R.string.player_action_coord_y)) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = coordZ,
                                                onValueChange = { coordZ = it },
                                                label = { Text(stringResource(R.string.player_action_coord_z)) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val x = coordX.trim().ifBlank { "0" }
                                                val y = coordY.trim().ifBlank { "100" }
                                                val z = coordZ.trim().ifBlank { "0" }
                                                onSendCommand("tp ${target.username} $x $y $z")
                                                actionFeedback = "Teleported ${target.username} to $x $y $z"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.player_action_tp_btn), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Teleport to another online player
                                val otherPlayers = allOnlinePlayers.filter { it != target.username }
                                if (otherPlayers.isNotEmpty()) {
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = Slate900),
                                        border = BorderStroke(1.dp, ObsidianCardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.player_action_tp_target),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(otherPlayers) { p ->
                                                    FilterChip(
                                                        selected = targetTpPlayer == p,
                                                        onClick = { targetTpPlayer = p },
                                                        label = { Text(p) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = EmeraldPrimary,
                                                            selectedLabelColor = Color.Black
                                                        )
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    if (targetTpPlayer.isNotBlank()) {
                                                        onSendCommand("tp ${target.username} $targetTpPlayer")
                                                        actionFeedback = "Teleported ${target.username} to $targetTpPlayer"
                                                    }
                                                },
                                                enabled = targetTpPlayer.isNotBlank(),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Teleport to $targetTpPlayer", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ActionTab.ITEMS -> {
                        if (!isServerRunning) {
                            ServerOfflineNotice()
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = stringResource(R.string.player_action_give_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                OutlinedTextField(
                                    value = itemId,
                                    onValueChange = { itemId = it },
                                    label = { Text(stringResource(R.string.player_action_give_item_id)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(popularItems) { (id, label) ->
                                        FilterChip(
                                            selected = itemId == id,
                                            onClick = { itemId = id },
                                            label = { Text(label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EmeraldPrimary,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.player_action_give_amount)}: $itemAmount",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )

                                    val amounts = listOf(1, 16, 32, 64)
                                    for (amt in amounts) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (itemAmount == amt) EmeraldPrimary else Slate900,
                                            border = BorderStroke(1.dp, if (itemAmount == amt) EmeraldPrimary else ObsidianCardBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { itemAmount = amt }
                                        ) {
                                            Text(
                                                text = amt.toString(),
                                                color = if (itemAmount == amt) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val cleanId = itemId.trim().ifBlank { "minecraft:diamond" }
                                        onSendCommand("give ${target.username} $cleanId $itemAmount")
                                        actionFeedback = "Gave $itemAmount x $cleanId to ${target.username}"
                                    },
                                    enabled = itemId.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.player_action_give_btn), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.players_dialog_close_btn), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ServerOfflineNotice() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate900,
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.player_action_requires_running),
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
        }
    }
}
