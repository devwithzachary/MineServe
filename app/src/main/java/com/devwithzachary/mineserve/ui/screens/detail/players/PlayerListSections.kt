package com.devwithzachary.mineserve.ui.screens.detail.players

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.BannedIpEntry
import com.devwithzachary.mineserve.model.BannedPlayerEntry
import com.devwithzachary.mineserve.model.OpEntry
import com.devwithzachary.mineserve.model.ServerPlayerLists
import com.devwithzachary.mineserve.model.WhitelistEntry
import com.devwithzachary.mineserve.ui.components.player.PlayerAvatar
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800

@Composable
fun EmptyListCard(message: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate400,
            modifier = Modifier.padding(16.dp)
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
    if (onlinePlayers.isEmpty()) {
        EmptyListCard(stringResource(R.string.players_online_empty))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (player in onlinePlayers) {
                val isOp = playerLists.ops.any { it.name.equals(player, ignoreCase = true) }
                val isWhitelisted = playerLists.whitelist.any { it.name.equals(player, ignoreCase = true) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(player) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(username = player, size = 42.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(player, fontWeight = FontWeight.Bold, color = Color.White)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (isOp) {
                                        Text("⭐ OP", fontSize = 11.sp, color = GoldYellow, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (isWhitelisted) {
                                        Text("✓ Whitelisted", fontSize = 11.sp, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onQuickKick(player) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.players_kick),
                                    fontSize = 12.sp,
                                    color = RedstoneLight,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_whitelist_title)} (${whitelist.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.players_add_to_whitelist), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (whitelist.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_whitelist_empty))
        } else {
            for (entry in whitelist) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(entry) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(username = entry.name, uuid = entry.uuid, size = 40.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(entry.name, fontWeight = FontWeight.Bold, color = Color.White)
                                if (entry.uuid.isNotBlank()) {
                                    Text(entry.uuid.take(16) + "…", fontSize = 10.sp, color = Slate400)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onRemoveClick(entry) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_remove),
                                fontSize = 12.sp,
                                color = RedstoneLight,
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
fun OpsSection(
    ops: List<OpEntry>,
    onAddClick: () -> Unit,
    onPlayerClick: (OpEntry) -> Unit,
    onRemoveClick: (OpEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_ops_title)} (${ops.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.players_add_operator), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (ops.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_ops_empty))
        } else {
            for (entry in ops) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(entry) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(username = entry.name, uuid = entry.uuid, size = 40.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(entry.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Level ${entry.level}", fontSize = 11.sp, color = GoldYellow, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = { onRemoveClick(entry) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_deop),
                                fontSize = 12.sp,
                                color = RedstoneLight,
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
fun BannedPlayersSection(
    bannedPlayers: List<BannedPlayerEntry>,
    onAddClick: () -> Unit,
    onPlayerClick: (BannedPlayerEntry) -> Unit,
    onPardonClick: (BannedPlayerEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_banned_players_title)} (${bannedPlayers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.players_ban_player), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (bannedPlayers.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_banned_players_empty))
        } else {
            for (entry in bannedPlayers) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerClick(entry) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(username = entry.name, uuid = entry.uuid, size = 40.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(entry.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(entry.reason, fontSize = 11.sp, color = Slate400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Button(
                            onClick = { onPardonClick(entry) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_pardon),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.players_banned_ips_title)} (${bannedIps.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.players_ban_ip), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (bannedIps.isEmpty()) {
            EmptyListCard(stringResource(R.string.players_banned_ips_empty))
        } else {
            for (entry in bannedIps) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate800,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = RedstoneRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(entry.ip, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(entry.reason, fontSize = 11.sp, color = Slate400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Button(
                            onClick = { onPardonClick(entry) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.players_pardon),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

