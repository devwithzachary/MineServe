package com.devwithzachary.mineserve.ui.screens.dashboard

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.assets.MineServeBrandAssets
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.tunnel.TunnelState
import com.devwithzachary.mineserve.ui.components.NotificationPermissionCard
import com.devwithzachary.mineserve.ui.components.ServerCard
import com.devwithzachary.mineserve.ui.theme.LayoutManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    servers: List<MinecraftServer>,
    serverStatuses: Map<String, ServerStatus>,
    serverMetrics: Map<String, ServerMetrics>,
    serverStorageMap: Map<String, Long> = emptyMap(),
    tunnelStates: Map<String, TunnelState> = emptyMap(),
    onServerClick: (MinecraftServer) -> Unit,
    onStartServer: (MinecraftServer) -> Unit,
    onStopServer: (String) -> Unit,
    onConsoleClick: (MinecraftServer) -> Unit,
    onCreateServerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onCreditsClick: () -> Unit,
    webAdminEnabled: Boolean = false,
    onRemoteAdminClick: (() -> Unit)? = null,
    isCloudLinked: Boolean = false,
    onCloudClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val uiPrefs = remember { context.getSharedPreferences("mineserve_ui_prefs", Context.MODE_PRIVATE) }
    var cloudIntroDismissed by remember {
        mutableStateOf(uiPrefs.getBoolean("dismissed_cloud_intro_1_7_0", false))
    }
    var overflowExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 40
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val runningCount = serverStatuses.count {
        it.value == ServerStatus.RUNNING || it.value == ServerStatus.STARTING
    }
    val totalRamUsedMb = serverMetrics.values.filter { it.isRunning }.sumOf { it.ramUsedMb }.toInt()
    val totalRamAllocatedMb = servers.filter {
        serverStatuses[it.id] == ServerStatus.RUNNING || serverStatuses[it.id] == ServerStatus.STARTING
    }.sumOf { it.allocatedRamMb }
    val displayActiveRamMb = if (totalRamUsedMb > 0) totalRamUsedMb else totalRamAllocatedMb

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = MineServeBrandAssets.brandIconPainter(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(ShapeDefaults.Small)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings_screen_title)
                        )
                    }

                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                            containerColor = colorScheme.surfaceContainerHighest
                        ) {
                            if (onCloudClick != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isCloudLinked) "MineServe Cloud (Linked)" else "MineServe Cloud",
                                            color = colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Cloud,
                                            contentDescription = null,
                                            tint = if (isCloudLinked) colorScheme.tertiary else colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        onCloudClick()
                                    }
                                )
                            }
                            if (onRemoteAdminClick != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (webAdminEnabled) "Web Remote Admin (On)" else "Web Remote Admin",
                                            color = colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Dns,
                                            contentDescription = null,
                                            tint = if (webAdminEnabled) colorScheme.primary else colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        overflowExpanded = false
                                        onRemoteAdminClick()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.credits_page_title),
                                        color = colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Code,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onCreditsClick()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "About",
                                        color = colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onAboutClick()
                                }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = colorScheme.surface,
                    scrolledContainerColor = colorScheme.surfaceContainerLow,
                    titleContentColor = colorScheme.onSurface,
                    actionIconContentColor = colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateServerClick,
                expanded = fabExpanded,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(text = stringResource(R.string.dash_create_server))
                },
                containerColor = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
                shape = ShapeDefaults.Large
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isMediumWidth = maxWidth >= 600.dp

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LayoutManager.screenHorizontalPadding),
                contentPadding = PaddingValues(top = 4.dp, bottom = LayoutManager.fabBottomClearance),
                verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
            ) {
                item {
                    NotificationPermissionCard(showOnlyWhenNeeded = true)
                }

                if (servers.isNotEmpty()) {
                    item {
                        DashboardMetricsHeader(
                            totalServersCount = servers.size,
                            runningCount = runningCount,
                            totalRamAllocatedMb = displayActiveRamMb
                        )
                    }
                }

                if (!BuildConfig.IS_PLAY_STORE && onCloudClick != null && !isCloudLinked && !cloudIntroDismissed) {
                    item {
                        MineServeCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeDefaults.Large,
                            containerColor = colorScheme.surfaceContainerHigh,
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = ShapeDefaults.Small,
                                        color = colorScheme.tertiaryContainer,
                                        contentColor = colorScheme.onTertiaryContainer,
                                        border = null
                                    ) {
                                        Text(
                                            text = "NEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Optional MineServe Cloud",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        cloudIntroDismissed = true
                                        uiPrefs.edit().putBoolean("dismissed_cloud_intro_1_7_0", true).apply()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val cloudPricing = remember { com.devwithzachary.mineserve.cloud.CloudPricing.current() }
                            Text(
                                text = "Keep hosting free on your phone - add a custom *.play.mineserve.app address, remote browser Web Admin, and encrypted cloud backups. Try Plus free for 7 days (then ${cloudPricing.plusMonthly}/mo; Pro ${cloudPricing.proMonthly}/mo).",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onCloudClick,
                                shape = ShapeDefaults.Medium
                            ) {
                                Text("Try Plus Free (7 Days)")
                            }
                        }
                    }
                }

                if (servers.isEmpty()) {
                    item {
                        DashboardEmptyState(onCreateServerClick = onCreateServerClick)
                    }
                } else if (isMediumWidth) {
                    val rows = servers.chunked(2)
                    items(rows) { rowServers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
                        ) {
                            for (server in rowServers) {
                                val status = serverStatuses[server.id] ?: ServerStatus.STOPPED
                                val metrics = serverMetrics[server.id]
                                val storageBytes = serverStorageMap[server.id] ?: 0L
                                val tunnelState = tunnelStates[server.id] ?: TunnelState.Disconnected

                                ServerCard(
                                    server = server,
                                    status = status,
                                    metrics = metrics,
                                    storageBytes = storageBytes,
                                    tunnelState = tunnelState,
                                    onCardClick = { onServerClick(server) },
                                    onStartClick = { onStartServer(server) },
                                    onStopClick = { onStopServer(server.id) },
                                    onConsoleClick = { onConsoleClick(server) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowServers.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(servers, key = { it.id }) { server ->
                        val status = serverStatuses[server.id] ?: ServerStatus.STOPPED
                        val metrics = serverMetrics[server.id]
                        val storageBytes = serverStorageMap[server.id] ?: 0L
                        val tunnelState = tunnelStates[server.id] ?: TunnelState.Disconnected

                        ServerCard(
                            server = server,
                            status = status,
                            metrics = metrics,
                            storageBytes = storageBytes,
                            tunnelState = tunnelState,
                            onCardClick = { onServerClick(server) },
                            onStartClick = { onStartServer(server) },
                            onStopClick = { onStopServer(server.id) },
                            onConsoleClick = { onConsoleClick(server) }
                        )
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun DashboardScreenPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        DashboardScreen(
            servers = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleServers,
            serverStatuses = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleStatuses,
            serverMetrics = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleMetricsMap,
            serverStorageMap = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleStorageBytesMap,
            tunnelStates = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleTunnelStates,
            onServerClick = {},
            onStartServer = {},
            onStopServer = {},
            onConsoleClick = {},
            onCreateServerClick = {},
            onSettingsClick = {},
            onAboutClick = {},
            onCreditsClick = {},
            webAdminEnabled = true,
            onRemoteAdminClick = {},
            isCloudLinked = true,
            onCloudClick = {}
        )
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun DashboardScreenEmptyPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        DashboardScreen(
            servers = emptyList(),
            serverStatuses = emptyMap(),
            serverMetrics = emptyMap(),
            onServerClick = {},
            onStartServer = {},
            onStopServer = {},
            onConsoleClick = {},
            onCreateServerClick = {},
            onSettingsClick = {},
            onAboutClick = {},
            onCreditsClick = {}
        )
    }
}
