package com.devwithzachary.mineserve.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.ui.components.NotificationPermissionCard
import com.devwithzachary.mineserve.ui.components.ServerCard
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    servers: List<MinecraftServer>,
    serverStatuses: Map<String, ServerStatus>,
    serverMetrics: Map<String, ServerMetrics>,
    serverStorageMap: Map<String, Long> = emptyMap(),
    onServerClick: (MinecraftServer) -> Unit,
    onStartServer: (MinecraftServer) -> Unit,
    onStopServer: (String) -> Unit,
    onConsoleClick: (MinecraftServer) -> Unit,
    onCreateServerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val runningCount = serverStatuses.count {
        it.value == ServerStatus.RUNNING || it.value == ServerStatus.STARTING
    }
    val totalRamUsedMb = serverMetrics.values.filter { it.isRunning }.sumOf { it.ramUsedMb }.toInt()
    val totalRamAllocatedMb = servers.filter {
        serverStatuses[it.id] == ServerStatus.RUNNING || serverStatuses[it.id] == ServerStatus.STARTING
    }.sumOf { it.allocatedRamMb }
    val displayActiveRamMb = if (totalRamUsedMb > 0) totalRamUsedMb else totalRamAllocatedMb

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = Slate400
                        )
                    }

                    IconButton(onClick = onCreditsClick) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = stringResource(R.string.credits_page_title),
                            tint = Slate400
                        )
                    }

                    IconButton(onClick = onAboutClick) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.about_page_title),
                            tint = EmeraldLight
                        )
                    }

                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_screen_title),
                            tint = Slate400
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateServerClick,
                containerColor = EmeraldPrimary,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dash_create_server))
            }
        },
        containerColor = Slate950,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notification permission banner with rationale (only shown when permission needed)
            item {
                NotificationPermissionCard(showOnlyWhenNeeded = true)
            }

            // Global Status Banner
            item {
                DashboardMetricsHeader(
                    totalServersCount = servers.size,
                    runningCount = runningCount,
                    totalRamAllocatedMb = displayActiveRamMb
                )
            }

            // Server Cards or Empty State
            if (servers.isEmpty()) {
                item {
                    DashboardEmptyState(onCreateServerClick = onCreateServerClick)
                }
            } else {
                items(servers, key = { it.id }) { server ->
                    val status = serverStatuses[server.id] ?: ServerStatus.STOPPED
                    val metrics = serverMetrics[server.id]
                    val storageBytes = serverStorageMap[server.id] ?: 0L

                    ServerCard(
                        server = server,
                        status = status,
                        metrics = metrics,
                        storageBytes = storageBytes,
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
