package com.devwithzachary.mineserve.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.model.PlayerActionTarget

private enum class ActionTab(val labelRes: Int) {
    ACTIONS(R.string.player_action_tab_actions),
    GAMEMODE(R.string.player_action_tab_gamemode),
    TELEPORT(R.string.player_action_tab_teleport),
    ITEMS(R.string.player_action_tab_items)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extColors = LocalMineServeColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.player_action_manage_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.players_dialog_close_btn),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Player Profile Card
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PlayerAvatar(
                        username = target.username,
                        uuid = target.uuid,
                        size = 56.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = target.username,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        target.uuid?.takeIf { it.isNotBlank() }?.let { uuid ->
                            MineServeTableMonoText(
                                text = uuid,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StatusPill(
                                text = stringResource(
                                    if (target.isOnline) R.string.player_action_status_online
                                    else R.string.player_action_status_offline
                                ),
                                containerColor = if (target.isOnline) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                contentColor = if (target.isOnline) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            if (target.isOp) {
                                StatusPill(
                                    text = stringResource(R.string.player_action_badge_op),
                                    containerColor = extColors.warningContainer,
                                    contentColor = extColors.onWarningContainer
                                )
                            }

                            if (target.isWhitelisted) {
                                StatusPill(
                                    text = stringResource(R.string.player_action_badge_whitelisted),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (target.isBanned) {
                                StatusPill(
                                    text = stringResource(R.string.player_action_badge_banned),
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Feedback confirmation banner
            AnimatedVisibility(visible = actionFeedback != null) {
                Surface(
                    shape = ShapeDefaults.Medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = actionFeedback ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Tab Selection Row
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(ShapeDefaults.Medium)
            ) {
                ActionTab.entries.forEachIndexed { index, tab ->
                    val selected = selectedTabIndex == index
                    Tab(
                        selected = selected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    )
                }
            }

            when (ActionTab.entries[selectedTabIndex]) {
                ActionTab.ACTIONS -> {
                    PlayerQuickActionsSection(
                        target = target,
                        isServerRunning = isServerRunning,
                        onSendCommand = onSendCommand,
                        onToggleWhitelist = onToggleWhitelist,
                        onToggleOp = onToggleOp,
                        onBanPlayer = onBanPlayer,
                        onPardonPlayer = onPardonPlayer,
                        onActionFeedback = { actionFeedback = it }
                    )
                }

                ActionTab.GAMEMODE -> {
                    PlayerGamemodeSection(
                        username = target.username,
                        isServerRunning = isServerRunning,
                        onSendCommand = onSendCommand,
                        onActionFeedback = { actionFeedback = it }
                    )
                }

                ActionTab.TELEPORT -> {
                    PlayerTeleportSection(
                        username = target.username,
                        isServerRunning = isServerRunning,
                        allOnlinePlayers = allOnlinePlayers,
                        onSendCommand = onSendCommand,
                        onActionFeedback = { actionFeedback = it }
                    )
                }

                ActionTab.ITEMS -> {
                    PlayerGiveItemsSection(
                        username = target.username,
                        isServerRunning = isServerRunning,
                        onSendCommand = onSendCommand,
                        onActionFeedback = { actionFeedback = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color
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
