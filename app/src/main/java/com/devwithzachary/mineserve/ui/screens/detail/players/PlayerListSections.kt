package com.devwithzachary.mineserve.ui.screens.detail.players

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GroupOff
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.model.BannedIpEntry
import com.devwithzachary.mineserve.model.BannedPlayerEntry
import com.devwithzachary.mineserve.model.OpEntry
import com.devwithzachary.mineserve.model.ServerPlayerLists
import com.devwithzachary.mineserve.model.WhitelistEntry
import com.devwithzachary.mineserve.ui.components.player.PlayerAvatar

@Composable
fun EmptyListCard(message: String) {
    MineServeEmptyState(
        icon = Icons.Rounded.GroupOff,
        title = message,
        supportingText = ""
    )
}

@Composable
private fun PlayerRoleBadge(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = ShapeDefaults.Small,
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun OnlinePlayersSection(
    onlinePlayers: List<String>,
    playerLists: ServerPlayerLists,
    onPlayerClick: (String) -> Unit,
    onQuickKick: (String) -> Unit
) {
    var pendingKickPlayer by remember { mutableStateOf<String?>(null) }

    if (onlinePlayers.isEmpty()) {
        EmptyListCard(stringResource(R.string.players_online_empty))
    } else {
        val extColors = LocalMineServeColors.current
        MineServeTableContainer {
            onlinePlayers.forEachIndexed { index, player ->
                val isOp = playerLists.ops.any { it.name.equals(player, ignoreCase = true) }
                val isWhitelisted = playerLists.whitelist.any { it.name.equals(player, ignoreCase = true) }

                MineServeTableRow(
                    showDivider = index < onlinePlayers.lastIndex,
                    onClick = { onPlayerClick(player) }
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerAvatar(username = player, size = 40.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = player,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isOp || isWhitelisted) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (isOp) {
                                        PlayerRoleBadge(
                                            text = stringResource(R.string.player_action_badge_op),
                                            containerColor = extColors.warningContainer,
                                            contentColor = extColors.onWarningContainer
                                        )
                                    }
                                    if (isWhitelisted) {
                                        PlayerRoleBadge(
                                            text = stringResource(R.string.player_action_badge_whitelisted),
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = { pendingKickPlayer = player },
                        shape = ShapeDefaults.Small,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.players_kick),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    pendingKickPlayer?.let { player ->
        MineServeDialog(
            onDismissRequest = { pendingKickPlayer = null },
            title = "${stringResource(R.string.players_kick)} $player?",
            confirmLabel = stringResource(R.string.players_kick),
            onConfirm = {
                pendingKickPlayer = null
                onQuickKick(player)
            },
            dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
            isDestructive = true
        ) {
            Text(
                text = stringResource(R.string.player_action_kick_default_reason),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WhitelistSection(
    whitelist: List<WhitelistEntry>,
    onAddClick: () -> Unit,
    onPlayerClick: (WhitelistEntry) -> Unit,
    onRemoveClick: (WhitelistEntry) -> Unit
) {
    var pendingRemove by remember { mutableStateOf<WhitelistEntry?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_whitelist_title)} (${whitelist.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(
                onClick = onAddClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = ShapeDefaults.Small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.players_add_to_whitelist),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (whitelist.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_whitelist_empty))
        } else {
            MineServeTableContainer {
                whitelist.forEachIndexed { index, entry ->
                    MineServeTableRow(
                        showDivider = index < whitelist.lastIndex,
                        onClick = { onPlayerClick(entry) }
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(username = entry.name, uuid = entry.uuid, size = 38.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (entry.uuid.isNotBlank()) {
                                    MineServeTableMonoText(
                                        text = entry.uuid.take(18) + "...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = { pendingRemove = entry },
                            shape = ShapeDefaults.Small,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_remove),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    pendingRemove?.let { entry ->
        MineServeDialog(
            onDismissRequest = { pendingRemove = null },
            title = "${stringResource(R.string.players_remove)} ${entry.name}?",
            confirmLabel = stringResource(R.string.players_remove),
            onConfirm = {
                pendingRemove = null
                onRemoveClick(entry)
            },
            dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
            isDestructive = true
        ) {
            Text(
                text = stringResource(R.string.player_action_remove_whitelist),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OpsSection(
    ops: List<OpEntry>,
    onAddClick: () -> Unit,
    onPlayerClick: (OpEntry) -> Unit,
    onRemoveClick: (OpEntry) -> Unit
) {
    var pendingDeop by remember { mutableStateOf<OpEntry?>(null) }
    val extColors = LocalMineServeColors.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_ops_title)} (${ops.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(
                onClick = onAddClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = ShapeDefaults.Small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.players_add_operator),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (ops.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_ops_empty))
        } else {
            MineServeTableContainer {
                ops.forEachIndexed { index, entry ->
                    MineServeTableRow(
                        showDivider = index < ops.lastIndex,
                        onClick = { onPlayerClick(entry) }
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(username = entry.name, uuid = entry.uuid, size = 38.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                PlayerRoleBadge(
                                    text = "OP Level ${entry.level}",
                                    containerColor = extColors.warningContainer,
                                    contentColor = extColors.onWarningContainer
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { pendingDeop = entry },
                            shape = ShapeDefaults.Small,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_deop),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeop?.let { entry ->
        MineServeDialog(
            onDismissRequest = { pendingDeop = null },
            title = "${stringResource(R.string.players_deop)} ${entry.name}?",
            confirmLabel = stringResource(R.string.players_deop),
            onConfirm = {
                pendingDeop = null
                onRemoveClick(entry)
            },
            dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
            isDestructive = true
        ) {
            Text(
                text = stringResource(R.string.player_action_deop),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BannedPlayersSection(
    bannedPlayers: List<BannedPlayerEntry>,
    onAddClick: () -> Unit,
    onPlayerClick: (BannedPlayerEntry) -> Unit,
    onPardonClick: (BannedPlayerEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_banned_players_title)} (${bannedPlayers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(
                onClick = onAddClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = ShapeDefaults.Small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.players_ban_player),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (bannedPlayers.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_banned_players_empty))
        } else {
            MineServeTableContainer {
                bannedPlayers.forEachIndexed { index, entry ->
                    MineServeTableRow(
                        showDivider = index < bannedPlayers.lastIndex,
                        onClick = { onPlayerClick(entry) }
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(username = entry.name, uuid = entry.uuid, size = 38.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = entry.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { onPardonClick(entry) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_pardon),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BannedIpsSection(
    bannedIps: List<BannedIpEntry>,
    onAddClick: () -> Unit,
    onPardonClick: (BannedIpEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_banned_ips_title)} (${bannedIps.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(
                onClick = onAddClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = ShapeDefaults.Small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.players_ban_ip),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (bannedIps.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_banned_ips_empty))
        } else {
            MineServeTableContainer {
                bannedIps.forEachIndexed { index, entry ->
                    MineServeTableRow(
                        showDivider = index < bannedIps.lastIndex
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = ShapeDefaults.Small,
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                MineServeTableMonoText(
                                    text = entry.ip,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = entry.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { onPardonClick(entry) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_pardon),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
