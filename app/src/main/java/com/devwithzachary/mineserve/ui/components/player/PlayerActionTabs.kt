package com.devwithzachary.mineserve.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.PlayerActionTarget
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900

@Composable
fun ServerOfflineNotice() {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerQuickActionsSection(
    target: PlayerActionTarget,
    isServerRunning: Boolean,
    onSendCommand: (String) -> Unit,
    onToggleWhitelist: (String, Boolean) -> Unit,
    onToggleOp: (String, Boolean) -> Unit,
    onBanPlayer: (String, String) -> Unit,
    onPardonPlayer: (String) -> Unit,
    onActionFeedback: (String) -> Unit
) {
    var kickReason by remember { mutableStateOf("") }
    var banReason by remember { mutableStateOf("") }

    val presetBanReasons = remember {
        listOf(
            R.string.player_action_reason_griefing,
            R.string.player_action_reason_cheating,
            R.string.player_action_reason_spam
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Whitelist & OP Quick Toggles Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_action_tab_actions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val newState = !target.isWhitelisted
                            onToggleWhitelist(target.username, newState)
                            onActionFeedback(if (newState) "Added ${target.username} to whitelist" else "Removed ${target.username} from whitelist")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (target.isWhitelisted) RedstoneLight else EmeraldPrimary
                        ),
                        border = BorderStroke(1.dp, if (target.isWhitelisted) RedstoneLight.copy(alpha = 0.5f) else EmeraldPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(if (target.isWhitelisted) R.string.player_action_remove_whitelist else R.string.player_action_add_whitelist),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val newState = !target.isOp
                            onToggleOp(target.username, newState)
                            onActionFeedback(if (newState) "Granted OP to ${target.username}" else "Removed OP from ${target.username}")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (target.isOp) RedstoneLight else GoldYellow
                        ),
                        border = BorderStroke(1.dp, if (target.isOp) RedstoneLight.copy(alpha = 0.5f) else GoldYellow.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(if (target.isOp) R.string.player_action_deop else R.string.player_action_make_op),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Kick Section (if server is running)
        if (isServerRunning) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.player_action_kick),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = kickReason,
                        onValueChange = { kickReason = it },
                        label = { Text(stringResource(R.string.player_action_kick_reason_label)) },
                        placeholder = { Text(stringResource(R.string.player_action_kick_default_reason)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val reason = kickReason.trim().ifBlank { "Kicked by an operator" }
                            onSendCommand("kick ${target.username} $reason")
                            onActionFeedback("Kicked ${target.username}")
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
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (target.isBanned) stringResource(R.string.player_action_pardon) else stringResource(R.string.player_action_ban),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (target.isBanned) EmeraldPrimary else RedstoneRed
                )

                if (!target.isBanned) {
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { banReason = it },
                        label = { Text(stringResource(R.string.player_action_ban_reason_label)) },
                        placeholder = { Text(stringResource(R.string.player_action_ban_default_reason)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedstoneRed,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Preset reason chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetBanReasons.forEach { reasonRes ->
                            val text = stringResource(reasonRes)
                            FilterChip(
                                selected = banReason == text,
                                onClick = { banReason = text },
                                label = { Text(text, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedstoneRed.copy(alpha = 0.2f),
                                    selectedLabelColor = RedstoneLight,
                                    containerColor = Slate800,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val reason = banReason.trim().ifBlank { "Banned by an operator" }
                            onBanPlayer(target.username, reason)
                            onActionFeedback("Banned ${target.username}")
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
                            onActionFeedback("Pardoned ${target.username}")
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
    }
}

@Composable
fun PlayerGamemodeSection(
    username: String,
    isServerRunning: Boolean,
    onSendCommand: (String) -> Unit,
    onActionFeedback: (String) -> Unit
) {
    if (!isServerRunning) {
        ServerOfflineNotice()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.player_action_gamemode_title),
            style = MaterialTheme.typography.titleSmall,
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
                    onSendCommand("gamemode $modeKey $username")
                    onActionFeedback("Changed gamemode to $modeKey for $username")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(iconEmoji, fontSize = 20.sp)
                        Text(stringResource(labelRes), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        text = "/gamemode $modeKey",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Slate400
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerTeleportSection(
    username: String,
    isServerRunning: Boolean,
    allOnlinePlayers: List<String>,
    onSendCommand: (String) -> Unit,
    onActionFeedback: (String) -> Unit
) {
    if (!isServerRunning) {
        ServerOfflineNotice()
        return
    }

    var coordX by remember { mutableStateOf("0") }
    var coordY by remember { mutableStateOf("100") }
    var coordZ by remember { mutableStateOf("0") }
    var targetTpPlayer by remember {
        mutableStateOf(allOnlinePlayers.firstOrNull { it != username } ?: "")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.player_action_teleport_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Teleport to World Spawn
        Button(
            onClick = {
                onSendCommand("tp $username 0 ~ 0")
                onActionFeedback("Teleported $username to spawn")
            },
            colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = EmeraldPrimary),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.player_action_tp_spawn), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // Teleport to Coordinates
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.player_action_tp_coords),
                    style = MaterialTheme.typography.titleSmall,
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = coordY,
                        onValueChange = { coordY = it },
                        label = { Text(stringResource(R.string.player_action_coord_y)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = coordZ,
                        onValueChange = { coordZ = it },
                        label = { Text(stringResource(R.string.player_action_coord_z)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = {
                        val x = coordX.trim().ifBlank { "0" }
                        val y = coordY.trim().ifBlank { "100" }
                        val z = coordZ.trim().ifBlank { "0" }
                        onSendCommand("tp $username $x $y $z")
                        onActionFeedback("Teleported $username to $x $y $z")
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
        val otherPlayers = allOnlinePlayers.filter { it != username }
        if (otherPlayers.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.player_action_tp_target),
                        style = MaterialTheme.typography.titleSmall,
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
                                onSendCommand("tp $username $targetTpPlayer")
                                onActionFeedback("Teleported $username to $targetTpPlayer")
                            }
                        },
                        enabled = targetTpPlayer.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.player_action_tp_to_player_btn, targetTpPlayer),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerGiveItemsSection(
    username: String,
    isServerRunning: Boolean,
    onSendCommand: (String) -> Unit,
    onActionFeedback: (String) -> Unit
) {
    if (!isServerRunning) {
        ServerOfflineNotice()
        return
    }

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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.player_action_give_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        OutlinedTextField(
            value = itemId,
            onValueChange = { itemId = it },
            label = { Text(stringResource(R.string.player_action_give_item_id)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            popularItems.forEach { (id, label) ->
                FilterChip(
                    selected = itemId == id,
                    onClick = { itemId = id },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = Slate800,
                        labelColor = Color.White
                    )
                )
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.player_action_give_amount)}: $itemAmount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val amounts = listOf(1, 16, 32, 64)
                    for (amt in amounts) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (itemAmount == amt) EmeraldPrimary else Slate800,
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                val cleanId = itemId.trim().ifBlank { "minecraft:diamond" }
                onSendCommand("give $username $cleanId $itemAmount")
                onActionFeedback("Gave $itemAmount x $cleanId to $username")
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

