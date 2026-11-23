package com.devwithzachary.mineserve.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val activeModal by viewModel.activeModal.collectAsState()
    val sidebarCollapsed by viewModel.sidebarCollapsed.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val toast by viewModel.toastMessage.collectAsState()

    val selectedServer = servers.firstOrNull { it.id == selectedId } ?: servers.firstOrNull()

    MineServeDesktopTheme(uiScale = uiScale) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesktopColors.Slate950)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                DesktopSidebar(
                    viewModel = viewModel,
                    servers = servers,
                    selectedServerId = selectedServer?.id,
                    statuses = statuses,
                    collapsed = sidebarCollapsed
                )

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (selectedServer == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            DesktopPanelCard {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "Welcome to MineServe for Linux",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Create a new Paper, Fabric, NeoForge, Forge, Purpur, Vanilla, Bedrock, or Proxy server, or import an existing server .zip archive.",
                                        fontSize = 13.sp,
                                        color = DesktopColors.Slate400
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = { viewModel.openModal(DesktopModal.CREATE_SERVER) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = DesktopColors.EmeraldPrimary,
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Text("+ Create First Server", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.openModal(DesktopModal.IMPORT_SERVER) }
                                        ) {
                                            Text("Import Server (.zip)", color = DesktopColors.Slate300)
                                        }
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

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when (activeTab) {
                                DesktopTab.CONSOLE -> DesktopConsoleTab(viewModel, selectedServer, metrics)
                                DesktopTab.PERFORMANCE -> DesktopPerformanceTab(viewModel, selectedServer, metrics)
                                DesktopTab.FILES -> DesktopFilesTab(viewModel, selectedServer)
                                DesktopTab.BACKUPS -> DesktopBackupsTab(viewModel, selectedServer)
                                DesktopTab.PLUGINS -> DesktopPluginsTab(viewModel, selectedServer)
                                DesktopTab.WORLD -> DesktopWorldTab(viewModel, selectedServer)
                                DesktopTab.LIVEMAP -> DesktopLiveMapTab(viewModel, selectedServer)
                                DesktopTab.SETTINGS -> DesktopSettingsTab(viewModel, selectedServer)
                                DesktopTab.PLAYERS -> DesktopPlayersTab(viewModel, selectedServer, metrics)
                                DesktopTab.AUTOMATION -> DesktopAutomationTab(viewModel, selectedServer)
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
                    shape = RoundedCornerShape(999.dp),
                    color = DesktopColors.ObsidianCard,
                    border = BorderStroke(1.dp, DesktopColors.EmeraldPrimary),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = toast.orEmpty(),
                        color = DesktopColors.EmeraldLight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
