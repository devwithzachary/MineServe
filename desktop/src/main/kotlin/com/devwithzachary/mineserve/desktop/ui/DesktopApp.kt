package com.devwithzachary.mineserve.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.desktop.ui.modals.DesktopModalOverlay
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopAutomationTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopBackupsTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopConsoleTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopFilesTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopLiveMapTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopPerformanceTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopPlayersTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopPluginsTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopSettingsTab
import com.devwithzachary.mineserve.desktop.ui.tabs.DesktopWorldTab
import com.devwithzachary.mineserve.model.ServerStatus

@Composable
fun MineServeDesktopApp(viewModel: DesktopViewModel) {
    val servers by viewModel.serverRepository.servers.collectAsState()
    val selectedId by viewModel.selectedServerId.collectAsState()
    val statuses by viewModel.processManager.serverStatuses.collectAsState()
    val metricsMap by viewModel.processManager.serverMetrics.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val contentSection by viewModel.contentSection.collectAsState()
    val worldSection by viewModel.worldSection.collectAsState()
    val managementSection by viewModel.managementSection.collectAsState()
    val activeModal by viewModel.activeModal.collectAsState()
    val sidebarCollapsed by viewModel.sidebarCollapsed.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val toast by viewModel.toastMessage.collectAsState()

    val selectedServer = servers.firstOrNull { it.id == selectedId } ?: servers.firstOrNull()

    MineServeDesktopTheme(uiScale = uiScale, themeMode = themeMode) {
        val colorScheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                DesktopSidebar(
                    viewModel = viewModel,
                    servers = servers,
                    selectedServerId = selectedServer?.id,
                    statuses = statuses,
                    collapsed = sidebarCollapsed
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(colorScheme.surface)
                ) {
                    if (selectedServer == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.widthIn(max = 520.dp)
                            ) {
                                MineServeEmptyState(
                                    icon = Icons.Rounded.Dns,
                                    title = "Welcome to MineServe for ${com.devwithzachary.mineserve.desktop.DesktopPlatform.shortName}",
                                    supportingText = "Create a new Paper, Fabric, NeoForge, Forge, Purpur, Vanilla, Bedrock, or Proxy server, or import an existing server .zip archive."
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { viewModel.openModal(DesktopModal.CREATE_SERVER) },
                                        shape = ShapeDefaults.Medium,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.primary,
                                            contentColor = colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create First Server", fontWeight = FontWeight.SemiBold)
                                    }
                                    FilledTonalButton(
                                        onClick = { viewModel.openModal(DesktopModal.IMPORT_SERVER) },
                                        shape = ShapeDefaults.Medium,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = colorScheme.secondaryContainer,
                                            contentColor = colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FolderZip,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import Server (.zip)")
                                    }
                                }
                            }
                        }
                    } else {
                        val status = statuses[selectedServer.id] ?: ServerStatus.STOPPED
                        val metrics = metricsMap[selectedServer.id]

                        DesktopServerHeader(
                            viewModel = viewModel,
                            server = selectedServer,
                            status = status,
                            metrics = metrics,
                            activeTab = activeTab
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            when (activeTab) {
                                DesktopTab.CONSOLE -> DesktopConsoleTab(viewModel, selectedServer, metrics)
                                DesktopTab.PERFORMANCE -> DesktopPerformanceTab(viewModel, selectedServer, metrics)
                                DesktopTab.PLAYERS -> DesktopPlayersTab(viewModel, selectedServer, metrics)
                                DesktopTab.CONTENT -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        DesktopSubSectionBar(
                                            items = DesktopContentSection.entries.map { it to it.title },
                                            selected = contentSection,
                                            onSelect = { viewModel.selectContentSection(it) }
                                        )
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            when (contentSection) {
                                                DesktopContentSection.FILES -> DesktopFilesTab(viewModel, selectedServer)
                                                DesktopContentSection.PLUGINS -> DesktopPluginsTab(viewModel, selectedServer)
                                            }
                                        }
                                    }
                                }
                                DesktopTab.WORLD_MAP -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        DesktopSubSectionBar(
                                            items = DesktopWorldSection.entries.map { it to it.title },
                                            selected = worldSection,
                                            onSelect = { viewModel.selectWorldSection(it) }
                                        )
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            when (worldSection) {
                                                DesktopWorldSection.WORLD -> DesktopWorldTab(viewModel, selectedServer)
                                                DesktopWorldSection.LIVE_MAP -> DesktopLiveMapTab(viewModel, selectedServer)
                                            }
                                        }
                                    }
                                }
                                DesktopTab.MANAGEMENT -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        DesktopSubSectionBar(
                                            items = DesktopManagementSection.entries.map { it to it.title },
                                            selected = managementSection,
                                            onSelect = { viewModel.selectManagementSection(it) }
                                        )
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            when (managementSection) {
                                                DesktopManagementSection.SETTINGS -> DesktopSettingsTab(viewModel, selectedServer)
                                                DesktopManagementSection.BACKUPS -> DesktopBackupsTab(viewModel, selectedServer)
                                                DesktopManagementSection.AUTOMATION -> DesktopAutomationTab(viewModel, selectedServer)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Modal Dialog Overlay
            DesktopModalOverlay(viewModel = viewModel, modal = activeModal)

            // Bottom Toast Banner
            if (toast != null) {
                Surface(
                    shape = ShapeDefaults.Medium,
                    color = colorScheme.surfaceContainerHighest,
                    contentColor = colorScheme.onSurface,
                    border = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = toast.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> DesktopSubSectionBar(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SingleChoiceSegmentedButtonRow {
                items.forEachIndexed { index, (item, label) ->
                    SegmentedButton(
                        selected = item == selected,
                        onClick = { onSelect(item) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = items.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colorScheme.secondaryContainer,
                            activeContentColor = colorScheme.onSecondaryContainer,
                            inactiveContainerColor = colorScheme.surfaceContainer,
                            inactiveContentColor = colorScheme.onSurfaceVariant,
                            activeBorderColor = colorScheme.outlineVariant,
                            inactiveBorderColor = colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}
