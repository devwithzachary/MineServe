package com.devwithzachary.mineserve.ui.components.player

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.PlayerActionTarget

@Composable
fun ServerOfflineNotice() {
    MineServeCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.player_action_requires_running),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    var showOpConfirm by remember { mutableStateOf(false) }
    var showWhitelistConfirm by remember { mutableStateOf(false) }
    var showKickConfirm by remember { mutableStateOf(false) }
    var showBanConfirm by remember { mutableStateOf(false) }
    val extColors = LocalMineServeColors.current

    val presetBanReasons = remember {
        listOf(
            R.string.player_action_reason_griefing,
            R.string.player_action_reason_cheating,
            R.string.player_action_reason_spam
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Whitelist & OP Quick Toggles Card
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.player_action_tab_actions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showWhitelistConfirm = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (target.isWhitelisted) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = if (target.isWhitelisted) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(
                                if (target.isWhitelisted) R.string.player_action_remove_whitelist
                                else R.string.player_action_add_whitelist
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = { showOpConfirm = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (target.isOp) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                extColors.warningContainer
                            },
                            contentColor = if (target.isOp) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                extColors.onWarningContainer
                            }
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(
                                if (target.isOp) R.string.player_action_deop
                                else R.string.player_action_make_op
                            ),
                            style = MaterialTheme.typography.labelMedium,
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
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.player_action_kick),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = kickReason,
                        onValueChange = { kickReason = it },
                        label = { Text(stringResource(R.string.player_action_kick_reason_label)) },
                        placeholder = { Text(stringResource(R.string.player_action_kick_default_reason)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(
                        onClick = { showKickConfirm = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.player_action_kick), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Ban / Pardon Section
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (target.isBanned) {
                        stringResource(R.string.player_action_pardon)
                    } else {
                        stringResource(R.string.player_action_ban)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (target.isBanned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                if (!target.isBanned) {
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { banReason = it },
                        label = { Text(stringResource(R.string.player_action_ban_reason_label)) },
                        placeholder = { Text(stringResource(R.string.player_action_ban_default_reason)) },
                        singleLine = true,
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
                                label = { Text(text, style = MaterialTheme.typography.labelMedium) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { showBanConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = ShapeDefaults.Small,
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.player_action_pardon), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showWhitelistConfirm) {
        val adding = !target.isWhitelisted
        MineServeDialog(
            onDismissRequest = { showWhitelistConfirm = false },
            title = stringResource(
                if (adding) R.string.player_action_add_whitelist else R.string.player_action_remove_whitelist
            ),
            confirmLabel = stringResource(
                if (adding) R.string.players_dialog_add_btn else R.string.players_remove
            ),
            onConfirm = {
                showWhitelistConfirm = false
                onToggleWhitelist(target.username, adding)
                onActionFeedback(
                    if (adding) "Added ${target.username} to whitelist"
                    else "Removed ${target.username} from whitelist"
                )
            },
            dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
            isDestructive = !adding
        ) {
            Text(
                text = target.username,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showOpConfirm) {
        val granting = !target.isOp
        MineServeDialog(
            onDismissRequest = { showOpConfirm = false },
            title = stringResource(
                if (granting) R.string.player_action_make_op else R.string.player_action_deop
            ),
            confirmLabel = stringResource(
                if (granting) R.string.player_action_make_op else R.string.player_action_deop
            ),
            onConfirm = {
                showOpConfirm = false
                onToggleOp(target.username, granting)
                onActionFeedback(
                    if (granting) "Granted OP to ${target.username}"
                    else "Removed OP from ${target.username}"
                )
            },
            dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
            isDestructive = !granting
        ) {
            Text(
                text = target.username,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showKickConfirm) {
        val reason = kickReason.trim().ifBlank { "Kicked by an operator" }
        MineServeDialog(
            onDismissRequest = { showKickConfirm = false },
            title = "${stringResource(R.string.player_action_kick)} ${target.username}?",
            confirmLabel = stringResource(R.string.player_action_kick),
            onConfirm = {
                showKickConfirm = false
                onSendCommand("kick ${target.username} $reason")
                onActionFeedback("Kicked ${target.username}")
            },
            dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
            isDestructive = true
        ) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showBanConfirm) {
        val reason = banReason.trim().ifBlank { "Banned by an operator" }
        MineServeDialog(
            onDismissRequest = { showBanConfirm = false },
            title = "${stringResource(R.string.player_action_ban)} ${target.username}?",
            confirmLabel = stringResource(R.string.player_action_ban),
            onConfirm = {
                showBanConfirm = false
                onBanPlayer(target.username, reason)
                onActionFeedback("Banned ${target.username}")
            },
            dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
            isDestructive = true
        ) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            color = MaterialTheme.colorScheme.onSurface
        )

        val modes = listOf(
            "survival" to (R.string.player_action_gamemode_survival to Icons.Rounded.Shield),
            "creative" to (R.string.player_action_gamemode_creative to Icons.Rounded.Brush),
            "adventure" to (R.string.player_action_gamemode_adventure to Icons.Rounded.Explore),
            "spectator" to (R.string.player_action_gamemode_spectator to Icons.Rounded.Visibility)
        )

        for ((modeKey, info) in modes) {
            val (labelRes, icon) = info
            MineServeCard(
                onClick = {
                    onSendCommand("gamemode $modeKey $username")
                    onActionFeedback("Changed gamemode to $modeKey for $username")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    MineServeTableMonoText(
                        text = "/gamemode $modeKey",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val monoFont = LocalMineServeMonoFontFamily.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.player_action_teleport_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Teleport to World Spawn
        FilledTonalButton(
            onClick = {
                onSendCommand("tp $username 0 ~ 0")
                onActionFeedback("Teleported $username to spawn")
            },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = ShapeDefaults.Medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.player_action_tp_spawn), fontWeight = FontWeight.Bold)
        }

        // Teleport to Coordinates
        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.player_action_tp_coords),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = coordX,
                        onValueChange = { coordX = it },
                        label = { Text(stringResource(R.string.player_action_coord_x)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = coordY,
                        onValueChange = { coordY = it },
                        label = { Text(stringResource(R.string.player_action_coord_y)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = coordZ,
                        onValueChange = { coordZ = it },
                        label = { Text(stringResource(R.string.player_action_coord_z)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
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
                        onSendCommand("tp $username $x $y $z")
                        onActionFeedback("Teleported $username to $x $y $z")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = ShapeDefaults.Small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.player_action_tp_btn), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Teleport to another online player
        val otherPlayers = allOnlinePlayers.filter { it != username }
        if (otherPlayers.isNotEmpty()) {
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.player_action_tp_target),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
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
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    labelColor = MaterialTheme.colorScheme.onSurface
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Small,
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
    val monoFont = LocalMineServeMonoFontFamily.current

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
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = itemId,
            onValueChange = { itemId = it },
            label = { Text(stringResource(R.string.player_action_give_item_id)) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
            singleLine = true,
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
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        MineServeCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(R.string.player_action_give_amount)}: $itemAmount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val amounts = listOf(1, 16, 32, 64)
                    for (amt in amounts) {
                        val selected = itemAmount == amt
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            modifier = Modifier
                                .clip(ShapeDefaults.Small)
                                .clickable { itemAmount = amt }
                        ) {
                            Text(
                                text = amt.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = monoFont,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
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
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = ShapeDefaults.Small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.player_action_give_btn), fontWeight = FontWeight.Bold)
        }
    }
}
