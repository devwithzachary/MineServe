package com.devwithzachary.mineserve.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.assets.MineServeBrandAssets
import com.devwithzachary.mineserve.designsystem.components.MetricTelemetryTile
import com.devwithzachary.mineserve.designsystem.components.NetworkAddressChip
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.desktop.FirewallCheckResult
import com.devwithzachary.mineserve.desktop.DesktopFirewallChecker
import com.devwithzachary.mineserve.desktop.DesktopHostTelemetry
import com.devwithzachary.mineserve.desktop.DesktopPortForwardState
import com.devwithzachary.mineserve.desktop.DesktopTunnelState
import com.devwithzachary.mineserve.model.MineServeAboutData
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSidebar(
    viewModel: DesktopViewModel,
    servers: List<MinecraftServer>,
    selectedServerId: String?,
    statuses: Map<String, ServerStatus>,
    collapsed: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current
    val drawerWidth = if (collapsed) 72.dp else 280.dp

    Surface(
        color = colorScheme.surfaceContainerLow,
        contentColor = colorScheme.onSurface,
        border = null,
        modifier = Modifier
            .width(drawerWidth)
            .fillMaxHeight()
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // Brand Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = !collapsed)
                ) {
                    Image(
                        painter = MineServeBrandAssets.brandIconPainter(),
                        contentDescription = "MineServe",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(ShapeDefaults.Small)
                            .clickable { if (collapsed) viewModel.toggleSidebar() }
                    )
                    if (!collapsed) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "MineServe",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Surface(
                                    shape = ShapeDefaults.Small,
                                    color = colorScheme.surfaceContainerHighest,
                                    contentColor = colorScheme.onSurfaceVariant,
                                    border = null
                                ) {
                                    Text(
                                        text = "v${MineServeAboutData.APP_VERSION}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = DesktopHostTelemetry.getHostDisplayName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (!collapsed) {
                    IconButton(
                        onClick = { viewModel.toggleSidebar() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Collapse navigation drawer",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Compact Creation Row
            if (!collapsed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.openModal(DesktopModal.CREATE_SERVER) },
                        shape = ShapeDefaults.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "New Server",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Import Server (.zip)") } },
                        state = rememberTooltipState()
                    ) {
                        OutlinedIconButton(
                            onClick = { viewModel.openModal(DesktopModal.IMPORT_SERVER) },
                            shape = ShapeDefaults.Medium,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant),
                            colors = IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderZip,
                                contentDescription = "Import Server (.zip)",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("New Server") } },
                        state = rememberTooltipState()
                    ) {
                        FilledIconButton(
                            onClick = { viewModel.openModal(DesktopModal.CREATE_SERVER) },
                            shape = ShapeDefaults.Medium,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "New Server",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Server Switcher
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(servers, key = { it.id }) { srv ->
                    val isSelected = srv.id == selectedServerId
                    val st = statuses[srv.id] ?: ServerStatus.STOPPED
                    val dotColor = when (st) {
                        ServerStatus.RUNNING -> extended.onlineEmerald
                        ServerStatus.STARTING, ServerStatus.STOPPING, ServerStatus.STANDBY -> extended.warning
                        ServerStatus.ERROR -> colorScheme.error
                        ServerStatus.STOPPED -> colorScheme.outline
                    }

                    if (collapsed) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text(srv.name) } },
                            state = rememberTooltipState()
                        ) {
                            Surface(
                                onClick = { viewModel.selectServer(srv.id) },
                                shape = ShapeDefaults.Medium,
                                color = if (isSelected) colorScheme.secondaryContainer else Color.Transparent,
                                contentColor = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                                border = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = srv.name.take(1).uppercase(Locale.US),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                    } else {
                        NavigationDrawerItem(
                            selected = isSelected,
                            onClick = { viewModel.selectServer(srv.id) },
                            shape = ShapeDefaults.Large,
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = colorScheme.secondaryContainer,
                                unselectedContainerColor = Color.Transparent,
                                selectedTextColor = colorScheme.onSecondaryContainer,
                                unselectedTextColor = colorScheme.onSurface,
                                selectedIconColor = colorScheme.onSecondaryContainer,
                                unselectedIconColor = colorScheme.onSurfaceVariant
                            ),
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            },
                            label = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = srv.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${srv.type.displayName} ${srv.version} :${srv.port}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = monoFont,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = if (isSelected) {
                                            colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Drawer Footer
            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

            val cloudStatus = remember(viewModel.cloudManager.isLinked) {
                viewModel.cloudManager.toStatusJson()
            }
            val cloudTier = cloudStatus.optString("tier", "FREE")
            val cloudBadgeText = if (viewModel.cloudManager.isLinked && cloudTier.isNotBlank()) {
                cloudTier.uppercase(Locale.US)
            } else {
                null
            }

            if (!collapsed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DrawerFooterRow(
                        label = "MineServe Cloud",
                        icon = Icons.Rounded.Cloud,
                        iconTint = colorScheme.tertiary,
                        badgeText = cloudBadgeText,
                        onClick = { viewModel.openModal(DesktopModal.CLOUD) }
                    )
                    DrawerFooterRow(
                        label = "Web Admin",
                        icon = Icons.Rounded.Dns,
                        iconTint = colorScheme.onSurfaceVariant,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            if (viewModel.webServer.isRunning) {
                                viewModel.openUrlInBrowser("http://127.0.0.1:${viewModel.webServer.actualPort}")
                            } else {
                                viewModel.openModal(DesktopModal.HOST_TOOLS)
                            }
                        }
                    )
                    DrawerFooterRow(
                        label = "Users & Audit",
                        icon = Icons.Rounded.ManageAccounts,
                        iconTint = colorScheme.onSurfaceVariant,
                        onClick = { viewModel.openModal(DesktopModal.USERS_AUDIT) }
                    )
                    DrawerFooterRow(
                        label = "App Settings",
                        icon = Icons.Rounded.Settings,
                        iconTint = colorScheme.onSurfaceVariant,
                        onClick = { viewModel.openModal(DesktopModal.HOST_TOOLS) }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CollapsedFooterIcon(
                        tooltip = "MineServe Cloud",
                        icon = Icons.Rounded.Cloud,
                        tint = colorScheme.tertiary,
                        onClick = { viewModel.openModal(DesktopModal.CLOUD) }
                    )
                    CollapsedFooterIcon(
                        tooltip = "Web Admin",
                        icon = Icons.Rounded.Dns,
                        tint = colorScheme.onSurfaceVariant,
                        onClick = {
                            if (viewModel.webServer.isRunning) {
                                viewModel.openUrlInBrowser("http://127.0.0.1:${viewModel.webServer.actualPort}")
                            } else {
                                viewModel.openModal(DesktopModal.HOST_TOOLS)
                            }
                        }
                    )
                    CollapsedFooterIcon(
                        tooltip = "Users & Audit",
                        icon = Icons.Rounded.ManageAccounts,
                        tint = colorScheme.onSurfaceVariant,
                        onClick = { viewModel.openModal(DesktopModal.USERS_AUDIT) }
                    )
                    CollapsedFooterIcon(
                        tooltip = "App Settings",
                        icon = Icons.Rounded.Settings,
                        tint = colorScheme.onSurfaceVariant,
                        onClick = { viewModel.openModal(DesktopModal.HOST_TOOLS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerFooterRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    NavigationDrawerItem(
        selected = false,
        onClick = onClick,
        shape = ShapeDefaults.Medium,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            unselectedTextColor = colorScheme.onSurface,
            unselectedIconColor = iconTint
        ),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        badge = if (badgeText != null) {
            {
                Surface(
                    shape = ShapeDefaults.Small,
                    color = colorScheme.tertiaryContainer,
                    contentColor = colorScheme.onTertiaryContainer,
                    border = null
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        } else if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            null
        },
        modifier = Modifier.height(44.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsedFooterIcon(
    tooltip: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DesktopServerHeader(
    viewModel: DesktopViewModel,
    server: MinecraftServer,
    status: ServerStatus,
    metrics: ServerMetrics?,
    activeTab: DesktopTab
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val tunnelState by viewModel.tunnelState.collectAsState()
    val portForwardState by viewModel.portForwardState.collectAsState()
    val isRunning = status == ServerStatus.RUNNING || status == ServerStatus.STARTING
    var localIp by remember { mutableStateOf("127.0.0.1") }
    var fwStatus by remember(server.id, server.port, server.type) {
        mutableStateOf(
            FirewallCheckResult(
                firewallName = "none",
                isActive = false,
                isPortConfirmedOpen = true,
                warningMessage = null,
                fixCommand = null
            )
        )
    }

    LaunchedEffect(server.id, server.port, server.type) {
        while (isActive) {
            val (ip, fw) = withContext(Dispatchers.IO) {
                val detectedIp = DesktopHostTelemetry.getLocalIpAddress() ?: "127.0.0.1"
                val check = DesktopFirewallChecker.checkInboundPort(
                    server.port,
                    isUdp = server.type == ServerType.BEDROCK_BDS || server.type == ServerType.BEDROCK_GEYSER
                )
                detectedIp to check
            }
            localIp = ip
            fwStatus = fw
            delay(30_000)
        }
    }

    val lanAddress = "$localIp:${server.port}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Unified Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Server identity, runtime metadata, and connection chips
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        DesktopStatusBadge(status)
                    }

                    Text(
                        text = "${server.type.displayName} ${server.version} / Java ${server.javaVersion} / ${server.allocatedRamMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right: Open Folder + Start / Restart / Stop actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text("Open Folder") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.openInFileManager(viewModel.serverRepository.getServerDirectory(server.id))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = "Open Folder",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.processManager.startServer(server) },
                        enabled = !isRunning && status != ServerStatus.STOPPING,
                        shape = ShapeDefaults.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary,
                            disabledContainerColor = colorScheme.surfaceContainerHighest,
                            disabledContentColor = colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start", fontWeight = FontWeight.SemiBold)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.processManager.restartServer(server) },
                        enabled = isRunning,
                        shape = ShapeDefaults.Medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer,
                            disabledContainerColor = colorScheme.surfaceContainerHighest,
                            disabledContentColor = colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restart")
                    }

                    FilledTonalButton(
                        onClick = { viewModel.processManager.stopServer(server.id) },
                        enabled = isRunning,
                        shape = ShapeDefaults.Medium,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colorScheme.errorContainer,
                            contentColor = colorScheme.onErrorContainer,
                            disabledContainerColor = colorScheme.surfaceContainerHighest,
                            disabledContentColor = colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Full-width Connection Strip (LAN IP, Tunnel, UPnP, Configure)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                NetworkAddressChip(
                    address = lanAddress,
                    isPublicTunnel = false,
                    onCopied = { addr ->
                        viewModel.copyToClipboard(addr, "Copied LAN address: $addr")
                    }
                )

                when (val ts = tunnelState) {
                    is DesktopTunnelState.Connected -> {
                        NetworkAddressChip(
                            address = ts.fullAddress,
                            isPublicTunnel = true,
                            onCopied = { addr ->
                                viewModel.copyToClipboard(addr, "Copied tunnel address: $addr")
                            }
                        )
                        val pUrl = ts.panelUrl
                        if (!pUrl.isNullOrBlank()) {
                            Surface(
                                onClick = { viewModel.copyToClipboard(pUrl, "Copied Cloud Panel URL: $pUrl") },
                                shape = ShapeDefaults.Small,
                                color = colorScheme.secondaryContainer,
                                contentColor = colorScheme.onSecondaryContainer,
                                border = null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Panel: ${pUrl.removePrefix("https://")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = LocalMineServeMonoFontFamily.current
                                    )
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = { viewModel.networkManager.stopTunnel(server.id) },
                            shape = ShapeDefaults.Small,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "Stop Tunnel",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    is DesktopTunnelState.Connecting -> {
                        Surface(
                            onClick = { ts.claimUrl?.let { viewModel.openUrlInBrowser(it) } },
                            shape = ShapeDefaults.Small,
                            color = if (ts.claimUrl != null) colorScheme.tertiaryContainer else extended.warningContainer,
                            contentColor = if (ts.claimUrl != null) colorScheme.onTertiaryContainer else extended.onWarningContainer,
                            border = null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (ts.claimUrl != null) "Claim Tunnel" else "Connecting Tunnel...",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    is DesktopTunnelState.Error -> {
                        Surface(
                            onClick = { viewModel.networkManager.startTunnel(server) },
                            shape = ShapeDefaults.Small,
                            color = colorScheme.errorContainer,
                            contentColor = colorScheme.onErrorContainer,
                            border = null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Tunnel Error - Retry",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    is DesktopTunnelState.Disconnected -> {
                        Surface(
                            onClick = { viewModel.networkManager.startTunnel(server) },
                            shape = ShapeDefaults.Small,
                            color = colorScheme.surfaceContainerHigh,
                            contentColor = colorScheme.onSurfaceVariant,
                            border = null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Start Tunnel",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                if (portForwardState is DesktopPortForwardState.Mapped) {
                    val mapped = portForwardState as DesktopPortForwardState.Mapped
                    NetworkAddressChip(
                        address = mapped.directAddress,
                        isPublicTunnel = true,
                        onCopied = { addr ->
                            viewModel.copyToClipboard(addr, "Copied UPnP address: $addr")
                        }
                    )
                }

                FilledTonalButton(
                    onClick = { viewModel.openServerSettings() },
                    shape = ShapeDefaults.Small,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colorScheme.surfaceContainerHigh,
                        contentColor = colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Configure",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Firewall Warning Banner if active and no tunnel is connected
            if (fwStatus.isActive && !fwStatus.isPortConfirmedOpen && tunnelState !is DesktopTunnelState.Connected) {
                Surface(
                    shape = ShapeDefaults.Medium,
                    color = extended.warningContainer,
                    contentColor = extended.onWarningContainer,
                    border = null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = extended.warning,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${fwStatus.firewallName} firewall detected: LAN players may be blocked on port ${server.port} unless opened (${fwStatus.fixCommand ?: ""}) or using a tunnel.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        fwStatus.fixCommand?.let { cmd ->
                            TextButton(
                                onClick = { viewModel.copyToClipboard(cmd, "Copied firewall command: $cmd") }
                            ) {
                                Text(
                                    text = "Copy Command",
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Telemetry Strip: 4 equal-width borderless MetricTelemetryTile cards
            val cpuPct = if (isRunning) (metrics?.cpuPercentage ?: 0f) else 0f
            val ramUsedMb = if (isRunning) (metrics?.ramUsedMb ?: 0L) else 0L
            val ramProgress = if (server.allocatedRamMb > 0) {
                ramUsedMb.toFloat() / server.allocatedRamMb.toFloat()
            } else {
                0f
            }
            val tpsValue = if (isRunning && metrics != null) {
                "${String.format(Locale.US, "%.1f", metrics.tps)} / ${String.format(Locale.US, "%.1f", metrics.mspt)}ms"
            } else {
                "-- / --"
            }
            val msptProgress = if (isRunning && metrics != null) {
                (metrics.mspt / 50.0).toFloat()
            } else {
                0f
            }
            val onlinePlayers = metrics?.onlinePlayerCount ?: 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTelemetryTile(
                    title = "CPU",
                    value = "${String.format(Locale.US, "%.1f", cpuPct)}%",
                    progress = cpuPct / 100f,
                    icon = Icons.Rounded.Speed,
                    modifier = Modifier.weight(1f)
                )
                MetricTelemetryTile(
                    title = "Memory",
                    value = "$ramUsedMb / ${server.allocatedRamMb} MB",
                    progress = ramProgress,
                    icon = Icons.Rounded.Memory,
                    modifier = Modifier.weight(1f)
                )
                MetricTelemetryTile(
                    title = "TPS / MSPT",
                    value = tpsValue,
                    progress = msptProgress,
                    icon = Icons.Rounded.Timer,
                    modifier = Modifier.weight(1f)
                )
                MetricTelemetryTile(
                    title = "Players",
                    value = "$onlinePlayers Online",
                    progress = (onlinePlayers / 20f).coerceIn(0f, 1f),
                    icon = Icons.Rounded.People,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6-Tab Workspace Bar
        val tabs = DesktopTab.entries
        val selectedIndex = tabs.indexOf(activeTab).coerceAtLeast(0)
        SecondaryTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = colorScheme.surface,
            contentColor = colorScheme.primary,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedIndex),
                    color = colorScheme.primary
                )
            },
            divider = {
                HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        ) {
            tabs.forEach { tab ->
                val selected = tab == activeTab
                Tab(
                    selected = selected,
                    onClick = { viewModel.selectTab(tab) },
                    selectedContentColor = colorScheme.primary,
                    unselectedContentColor = colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }
    }
}
