package com.devwithzachary.mineserve.ui.screens.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.api.FabricApiClient
import com.devwithzachary.mineserve.api.MojangApiClient
import com.devwithzachary.mineserve.api.PaperApiClient
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate950
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerWizardScreen(
    onServerCreated: (MinecraftServer) -> Unit,
    onCancel: () -> Unit,
    existingServers: List<MinecraftServer> = emptyList(),
    onDownloadAndCreateServer: suspend (
        name: String,
        type: ServerType,
        version: String,
        port: Int,
        ramMb: Int,
        motd: String,
        onProgress: (String, Int) -> Unit
    ) -> MinecraftServer?,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    // Calculate next available default port starting from 25565
    val defaultUnusedPort = remember(existingServers) {
        val usedPorts = existingServers.map { it.port }.toSet()
        var candidate = 25565
        while (usedPorts.contains(candidate)) {
            candidate++
        }
        candidate
    }

    // Form State
    var serverName by remember { mutableStateOf("My Minecraft Server") }
    var motd by remember { mutableStateOf("Welcome to my MineServe Server!") }
    var selectedType by remember { mutableStateOf(ServerType.PAPER) }
    var selectedVersion by remember { mutableStateOf("26.2") }
    var allocatedRamMb by remember { mutableIntStateOf(2048) }
    var port by remember(defaultUnusedPort) { mutableIntStateOf(defaultUnusedPort) }
    var eulaAccepted by remember { mutableStateOf(true) }

    // Check if the currently chosen port is in use by an existing server
    val conflictingServerName = remember(port, existingServers) {
        existingServers.firstOrNull { it.port == port }?.name
    }

    // Version lists (Newest first)
    var availableVersions by remember {
        mutableStateOf(listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"))
    }
    var isLoadingVersions by remember { mutableStateOf(false) }

    // Creation progress state
    var isCreating by remember { mutableStateOf(false) }
    var creationStatus by remember { mutableStateOf("") }
    var creationProgress by remember { mutableIntStateOf(0) }

    // Fetch versions when server type changes (Descending order with newest as default)
    LaunchedEffect(selectedType) {
        isLoadingVersions = true
        try {
            val vList = when (selectedType) {
                ServerType.PAPER, ServerType.BEDROCK_GEYSER -> {
                    PaperApiClient().getProjectVersions("paper")
                }
                ServerType.PURPUR -> {
                    com.devwithzachary.mineserve.api.PurpurApiClient().getVersions()
                }
                ServerType.FOLIA -> {
                    PaperApiClient().getProjectVersions("folia")
                }
                ServerType.VANILLA -> {
                    MojangApiClient().getReleaseVersions()
                }
                ServerType.FABRIC -> {
                    FabricApiClient().getGameVersions()
                }
                ServerType.NEOFORGE -> {
                    com.devwithzachary.mineserve.api.NeoForgeApiClient().getVersions()
                }
                else -> {
                    listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.1", "1.20.4", "1.20.1")
                }
            }
            val sorted = vList.sortedMinecraftVersionsDescending()
            if (sorted.isNotEmpty()) {
                availableVersions = sorted
                selectedVersion = sorted.first()
            }
        } catch (_: Exception) {}
        isLoadingVersions = false
    }

    val stepSubtitle = when (step) {
        1 -> stringResource(R.string.wizard_step_1_of_4)
        2 -> stringResource(R.string.wizard_step_2_of_4)
        3 -> stringResource(R.string.wizard_step_3_of_4)
        else -> stringResource(R.string.wizard_step_4_of_4)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.wizard_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stepSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else onCancel() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        bottomBar = {
            Surface(
                color = Slate950,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step > 1 && !isCreating) {
                        OutlinedButton(
                            onClick = { step-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.wizard_btn_back))
                        }
                    }

                    Button(
                        onClick = {
                            if (step < 4) {
                                step++
                            } else {
                                // Launch server creation
                                isCreating = true
                                creationStatus = "Downloading ${selectedType.displayName} $selectedVersion..."
                                scope.launch {
                                    val created = onDownloadAndCreateServer(
                                        serverName.trim().ifEmpty { "Minecraft Server" },
                                        selectedType,
                                        selectedVersion,
                                        port,
                                        allocatedRamMb,
                                        motd
                                    ) { status, progress ->
                                        creationStatus = status
                                        creationProgress = progress
                                    }
                                    isCreating = false
                                    if (created != null) {
                                        onServerCreated(created)
                                    }
                                }
                            }
                        },
                        enabled = !isCreating && (step != 4 || eulaAccepted),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (step < 4) {
                            Text(
                                text = stringResource(R.string.wizard_btn_next),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.wizard_btn_create),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        },
        containerColor = Slate950,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) EmeraldPrimary else Slate800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(targetState = step, label = "wizardStep") { currentStep ->
                when (currentStep) {
                    1 -> WizardStep1Identity(
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        motd = motd,
                        onMotdChange = { motd = it }
                    )
                    2 -> WizardStep2Software(
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it },
                        selectedVersion = selectedVersion,
                        onVersionSelected = { selectedVersion = it },
                        availableVersions = availableVersions,
                        isLoadingVersions = isLoadingVersions
                    )
                    3 -> WizardStep3Configuration(
                        allocatedRamMb = allocatedRamMb,
                        onAllocatedRamMbChange = { allocatedRamMb = it },
                        port = port,
                        onPortChange = { port = it },
                        conflictingServerName = conflictingServerName
                    )
                    4 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            WizardStep4Review(
                                serverName = serverName,
                                selectedType = selectedType,
                                selectedVersion = selectedVersion,
                                allocatedRamMb = allocatedRamMb,
                                port = port,
                                eulaAccepted = eulaAccepted,
                                onEulaAcceptedChange = { eulaAccepted = it },
                                conflictingServerName = conflictingServerName
                            )

                            if (isCreating) {
                                WizardProgressCard(creationStatus = creationStatus)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
