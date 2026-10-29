package com.devwithzachary.mineserve.ui.components.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.PlayerActionTarget
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PlayerAvatar(
                                username = target.username,
                                uuid = target.uuid,
                                size = 36.dp,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Column {
                                Text(
                                    text = target.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.player_action_manage_title),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        OutlinedButton(
                            onClick = onDismiss,
                            border = BorderStroke(1.dp, Slate800),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(stringResource(R.string.players_dialog_close_btn), fontSize = 12.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Slate950
                    )
                )
            },
            containerColor = Slate950
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Player Profile Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PlayerAvatar(
                                username = target.username,
                                uuid = target.uuid,
                                size = 64.dp,
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
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!target.uuid.isNullOrBlank()) {
                                    Text(
                                        text = target.uuid,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Slate400,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Status badge
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (target.isOnline) EmeraldPrimary.copy(alpha = 0.2f) else Slate800,
                                        border = BorderStroke(1.dp, if (target.isOnline) EmeraldPrimary else Slate400.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = stringResource(if (target.isOnline) R.string.player_action_status_online else R.string.player_action_status_offline),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (target.isOnline) EmeraldPrimary else Slate400,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldYellow,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RedstoneRed,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Feedback confirmation banner
                AnimatedVisibility(visible = actionFeedback != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldDark.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                            Text(
                                text = actionFeedback ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Tab Selection Row
                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Slate900,
                    contentColor = EmeraldPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    ActionTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) EmeraldPrimary else Slate400
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
}
