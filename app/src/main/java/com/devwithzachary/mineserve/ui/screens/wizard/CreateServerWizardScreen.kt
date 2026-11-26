package com.devwithzachary.mineserve.ui.screens.wizard

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.api.FabricApiClient
import com.devwithzachary.mineserve.api.MojangApiClient
import com.devwithzachary.mineserve.api.PaperApiClient
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.ServerBadgeState
import com.devwithzachary.mineserve.designsystem.components.ServerStatusBadge
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.determineJavaVersion
import com.devwithzachary.mineserve.model.findNextAvailablePort
import com.devwithzachary.mineserve.model.sortedMinecraftVersionsDescending
import com.devwithzachary.mineserve.ui.components.RamSlider
import com.devwithzachary.mineserve.ui.components.getDefaultAllocatedRamMb
import com.devwithzachary.mineserve.ui.theme.LayoutManager
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
        eulaAccepted: Boolean,
        localJarUri: Uri?,
        onProgress: (String, Int) -> Unit
    ) -> MinecraftServer?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) }

    BackHandler {
        if (step > 1 && !/* guard handled below */false) {
            step--
        } else {
            onCancel()
        }
    }

    val defaultUnusedPort = remember(existingServers) {
        findNextAvailablePort(existingServers)
    }

    var serverName by remember { mutableStateOf("My Minecraft Server") }
    var motd by remember { mutableStateOf("Welcome to my MineServe Server!") }
    var selectedType by remember { mutableStateOf(ServerType.PAPER) }
    var selectedVersion by remember { mutableStateOf("26.2") }
    var allocatedRamMb by remember { mutableIntStateOf(getDefaultAllocatedRamMb(context)) }
    var userModifiedPort by remember { mutableStateOf(false) }
    var port by remember { mutableIntStateOf(defaultUnusedPort) }
    var eulaAccepted by remember { mutableStateOf(false) }
    var localJarUri by remember { mutableStateOf<Uri?>(null) }

    val localJarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            localJarUri = uri
        }
    }

    LaunchedEffect(defaultUnusedPort) {
        if (!userModifiedPort) {
            port = defaultUnusedPort
        }
    }

    val conflictingServerName = remember(port, existingServers) {
        if (port in 1..65535) {
            existingServers.firstOrNull { it.port == port }?.name
        } else {
            null
        }
    }

    var availableVersions by remember {
        mutableStateOf(listOf("26.2", "26.1.2", "1.21.11", "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"))
    }
    var isLoadingVersions by remember { mutableStateOf(false) }

    var isCreating by remember { mutableStateOf(false) }
    var creationStatus by remember { mutableStateOf("") }
    @Suppress("UNUSED_VARIABLE")
    var creationProgress by remember { mutableIntStateOf(0) }
    var creationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedType) {
        if (!userModifiedPort) {
            port = if (selectedType.isBedrockNative) {
                findNextAvailablePort(existingServers, 19132)
            } else {
                defaultUnusedPort
            }
        }
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
                ServerType.VELOCITY -> {
                    PaperApiClient().getProjectVersions("velocity")
                }
                ServerType.BUNGEECORD -> {
                    com.devwithzachary.mineserve.api.BungeeCordApiClient().getVersions()
                }
                ServerType.BEDROCK_BDS -> {
                    com.devwithzachary.mineserve.api.BedrockApiClient().getBdsVersions()
                }
                ServerType.VANILLA -> {
                    MojangApiClient().getReleaseVersions()
                }
                ServerType.FABRIC -> {
                    FabricApiClient().getGameVersions()
                }
                ServerType.FORGE -> {
                    com.devwithzachary.mineserve.api.ForgeApiClient().getVersions()
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

    val stepSubtitle = if (step == 1) {
        "Step 1 of 2 - Software & Version"
    } else {
        "Step 2 of 2 - Configuration & EULA"
    }

    Scaffold(
        modifier = modifier,
        containerColor = colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.wizard_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stepSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step > 1 && !isCreating) step-- else if (!isCreating) onCancel()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = colorScheme.surfaceContainerLow,
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
                        FilledTonalButton(
                            onClick = { step-- },
                            shape = ShapeDefaults.Medium,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.wizard_btn_back))
                        }
                    }

                    Button(
                        onClick = {
                            if (step < 2) {
                                step = 2
                            } else {
                                isCreating = true
                                creationError = null
                                creationStatus = "Downloading ${selectedType.displayName} $selectedVersion..."
                                scope.launch {
                                    val created = onDownloadAndCreateServer(
                                        serverName.trim().ifEmpty { "Minecraft Server" },
                                        selectedType,
                                        selectedVersion,
                                        port,
                                        allocatedRamMb,
                                        motd,
                                        eulaAccepted,
                                        localJarUri
                                    ) { status, progress ->
                                        creationStatus = status
                                        creationProgress = progress
                                    }
                                    isCreating = false
                                    if (created != null) {
                                        onServerCreated(created)
                                    } else {
                                        creationError = "Failed to download or create ${selectedType.displayName}. Please check your internet connection and try again."
                                    }
                                }
                            }
                        },
                        enabled = !isCreating && if (step == 1) selectedVersion.isNotBlank() else (serverName.isNotBlank() && port in 1..65535 && eulaAccepted),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        shape = ShapeDefaults.Medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (step < 2) {
                            Text(text = stringResource(R.string.wizard_btn_next))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Rounded.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Create & Launch")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LayoutManager.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 2-Step M3 Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (i <= step) colorScheme.primary else colorScheme.surfaceContainerHighest)
                    )
                }
            }

            AnimatedContent(
                targetState = step,
                label = "wizardTwoStep",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { currentStep ->
                if (currentStep == 1) {
                    WizardStepSoftwareAndVersion(
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it },
                        selectedVersion = selectedVersion,
                        onVersionSelected = { selectedVersion = it },
                        availableVersions = availableVersions,
                        isLoadingVersions = isLoadingVersions
                    )
                } else {
                    WizardStepConfigurationAndEula(
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        motd = motd,
                        onMotdChange = { motd = it },
                        selectedType = selectedType,
                        selectedVersion = selectedVersion,
                        allocatedRamMb = allocatedRamMb,
                        onAllocatedRamMbChange = { allocatedRamMb = it },
                        port = port,
                        onPortChange = { newPort ->
                            userModifiedPort = true
                            port = newPort
                        },
                        conflictingServerName = conflictingServerName,
                        localJarUri = localJarUri,
                        onPickLocalJar = {
                            localJarPicker.launch(arrayOf("application/java-archive", "application/zip", "*/*"))
                        },
                        onClearLocalJar = { localJarUri = null },
                        eulaAccepted = eulaAccepted,
                        onEulaAcceptedChange = { eulaAccepted = it },
                        isCreating = isCreating,
                        creationStatus = creationStatus,
                        creationError = creationError
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WizardStepSoftwareAndVersion(
    selectedType: ServerType,
    onTypeSelected: (ServerType) -> Unit,
    selectedVersion: String,
    onVersionSelected: (String) -> Unit,
    availableVersions: List<String>,
    isLoadingVersions: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    var isVersionDropdownExpanded by remember { mutableStateOf(false) }

    val serverTypes = remember {
        listOf(
            ServerType.PAPER,
            ServerType.PURPUR,
            ServerType.BEDROCK_GEYSER,
            ServerType.VANILLA,
            ServerType.FABRIC,
            ServerType.FORGE,
            ServerType.NEOFORGE,
            ServerType.FOLIA,
            ServerType.VELOCITY,
            ServerType.BUNGEECORD,
            ServerType.BEDROCK_BDS
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.wizard_step2_version_label),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.primary
        )

        if (availableVersions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableVersions.take(5).forEach { v ->
                    FilterChip(
                        selected = selectedVersion == v,
                        onClick = { onVersionSelected(v) },
                        label = {
                            Text(
                                text = v,
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = monoFont)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colorScheme.secondaryContainer,
                            selectedLabelColor = colorScheme.onSecondaryContainer,
                            containerColor = colorScheme.surfaceContainer,
                            labelColor = colorScheme.onSurfaceVariant
                        ),
                        border = null
                    )
                }
            }
        }

        // Version Selector Dropdown
        ExposedDropdownMenuBox(
            expanded = isVersionDropdownExpanded,
            onExpandedChange = { isVersionDropdownExpanded = !isVersionDropdownExpanded }
        ) {
            OutlinedTextField(
                value = if (isLoadingVersions) stringResource(R.string.wizard_step2_loading_versions) else selectedVersion,
                onValueChange = {},
                readOnly = true,
                label = { Text("All Versions") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isVersionDropdownExpanded) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = monoFont),
                shape = ShapeDefaults.Medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                    focusedLabelColor = colorScheme.primary,
                    unfocusedLabelColor = colorScheme.onSurfaceVariant,
                    focusedContainerColor = colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = colorScheme.surfaceContainerLow,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface
                ),
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = isVersionDropdownExpanded,
                onDismissRequest = { isVersionDropdownExpanded = false },
                containerColor = colorScheme.surfaceContainerHighest
            ) {
                for (v in availableVersions) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = v,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                color = colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onVersionSelected(v)
                            isVersionDropdownExpanded = false
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.4f))

        Text(
            text = "Server Software",
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.primary
        )

        // Software Platform Cards (responsive 1-column Compact / 2-column Medium, independently scrollable)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            val isMedium = maxWidth >= 600.dp
            if (isMedium) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (pair in serverTypes.chunked(2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (st in pair) {
                                SoftwareChoiceCard(
                                    serverType = st,
                                    isSelected = selectedType == st,
                                    onClick = { onTypeSelected(st) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (st in serverTypes) {
                        SoftwareChoiceCard(
                            serverType = st,
                            isSelected = selectedType == st,
                            onClick = { onTypeSelected(st) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftwareChoiceCard(
    serverType: ServerType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current

    MineServeCard(
        onClick = onClick,
        modifier = modifier,
        shape = ShapeDefaults.Medium,
        containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
        contentColor = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = serverType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                    )
                    if (serverType.isBedrockNative) {
                        Surface(
                            shape = ShapeDefaults.ExtraSmall,
                            color = extended.warningContainer,
                            contentColor = extended.onWarningContainer,
                            border = null
                        ) {
                            Text(
                                text = "BETA",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = serverType.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WizardStepConfigurationAndEula(
    serverName: String,
    onServerNameChange: (String) -> Unit,
    motd: String,
    onMotdChange: (String) -> Unit,
    selectedType: ServerType,
    selectedVersion: String,
    allocatedRamMb: Int,
    onAllocatedRamMbChange: (Int) -> Unit,
    port: Int,
    onPortChange: (Int) -> Unit,
    conflictingServerName: String?,
    localJarUri: Uri?,
    onPickLocalJar: () -> Unit,
    onClearLocalJar: () -> Unit,
    eulaAccepted: Boolean,
    onEulaAcceptedChange: (Boolean) -> Unit,
    isCreating: Boolean,
    creationStatus: String,
    creationError: String?
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current
    val uriHandler = LocalUriHandler.current
    val eulaUrl = stringResource(R.string.minecraft_eula_url)

    var portText by remember { mutableStateOf(if (port in 1..65535) port.toString() else "") }
    LaunchedEffect(port) {
        if (port in 1..65535 && portText.toIntOrNull() != port) {
            portText = port.toString()
        }
    }
    val isPortValid = portText.isNotEmpty() && portText.toIntOrNull()?.let { it in 1..65535 } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live-updating MineServeCard preview at the top
        MineServeCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeDefaults.Large,
            containerColor = colorScheme.surfaceContainer,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = serverName.trim().ifEmpty { "My Minecraft Server" },
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${selectedType.displayName} $selectedVersion - Port $port",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ServerStatusBadge(
                    state = ServerBadgeState.Offline,
                    label = "Preview"
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val requiredJava = determineJavaVersion(selectedVersion, selectedType)
                PreviewMonoChip(text = "$allocatedRamMb MB")
                PreviewMonoChip(text = if (selectedType.isBedrockNative) "Bedrock" else "Java $requiredJava")
            }
        }

        OutlinedTextField(
            value = serverName,
            onValueChange = onServerNameChange,
            label = { Text(stringResource(R.string.wizard_step1_name_label)) },
            singleLine = true,
            shape = ShapeDefaults.Medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                focusedLabelColor = colorScheme.primary,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                focusedContainerColor = colorScheme.surfaceContainerLow,
                unfocusedContainerColor = colorScheme.surfaceContainerLow,
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = portText,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.take(5)
                portText = digitsOnly
                val parsed = digitsOnly.toIntOrNull()
                if (parsed != null && parsed in 1..65535) {
                    onPortChange(parsed)
                } else {
                    onPortChange(0)
                }
            },
            label = { Text(stringResource(R.string.wizard_step3_port_label)) },
            singleLine = true,
            isError = !isPortValid,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = monoFont),
            supportingText = {
                if (!isPortValid) {
                    Text(
                        text = if (portText.isEmpty()) {
                            stringResource(R.string.wizard_step3_port_empty_error)
                        } else {
                            stringResource(R.string.wizard_step3_port_range_error)
                        },
                        color = colorScheme.error
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            shape = ShapeDefaults.Medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (conflictingServerName != null) extended.warning else colorScheme.primary,
                unfocusedBorderColor = if (conflictingServerName != null) extended.warning else colorScheme.outlineVariant,
                focusedLabelColor = if (conflictingServerName != null) extended.warning else colorScheme.primary,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                focusedContainerColor = colorScheme.surfaceContainerLow,
                unfocusedContainerColor = colorScheme.surfaceContainerLow,
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = conflictingServerName != null) {
            Surface(
                shape = ShapeDefaults.Medium,
                color = extended.warningContainer,
                contentColor = extended.onWarningContainer,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = extended.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.wizard_port_conflict_title, conflictingServerName ?: ""),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = stringResource(R.string.wizard_port_conflict_desc, port, conflictingServerName ?: ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = motd,
            onValueChange = onMotdChange,
            label = { Text(stringResource(R.string.wizard_step1_motd_label)) },
            singleLine = true,
            shape = ShapeDefaults.Medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.primary,
                unfocusedBorderColor = colorScheme.outlineVariant,
                focusedLabelColor = colorScheme.primary,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                focusedContainerColor = colorScheme.surfaceContainerLow,
                unfocusedContainerColor = colorScheme.surfaceContainerLow,
                focusedTextColor = colorScheme.onSurface,
                unfocusedTextColor = colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (!selectedType.isBedrockNative) {
            MineServeCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeDefaults.Large,
                containerColor = colorScheme.surfaceContainer,
                contentPadding = PaddingValues(16.dp)
            ) {
                RamSlider(
                    allocatedMb = allocatedRamMb,
                    onValueChange = onAllocatedRamMbChange
                )
            }
        } else {
            MineServeCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeDefaults.Large,
                containerColor = colorScheme.surfaceContainer,
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = "Bedrock Dedicated Server is a native C++ binary and manages memory dynamically without JVM heap (-Xmx) limits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        // Optional Local Server JAR / Zip Import
        MineServeCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeDefaults.Large,
            containerColor = colorScheme.surfaceContainer,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Server Binary Source",
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface
                )
                if (localJarUri == null) {
                    Text(
                        text = "Downloads automatically in the background, or import a local .jar / .zip file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onPickLocalJar,
                        shape = ShapeDefaults.Medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Local Server File (.jar / .zip)")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Using local file: ${localJarUri.lastPathSegment ?: "selected"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearLocalJar, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear local file",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = selectedType.isBedrockNative) {
            Surface(
                shape = ShapeDefaults.Medium,
                color = extended.warningContainer,
                contentColor = extended.onWarningContainer,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = extended.warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Experimental Bedrock Support (Beta)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Mojang Bedrock Dedicated Server is in early beta and uses qemu-x86_64 emulation on ARM64 devices.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Top-aligned Checkbox wrapping cleanly on narrow screens
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = eulaAccepted,
                onCheckedChange = onEulaAcceptedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    checkmarkColor = colorScheme.onPrimary,
                    uncheckedColor = colorScheme.outline
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            val prefixText = stringResource(R.string.wizard_step4_eula_prefix)
            val linkText = stringResource(R.string.wizard_step4_eula_link)
            val annotatedString = buildAnnotatedString {
                append(prefixText.trim())
                append("\n")
                pushStringAnnotation(tag = "URL", annotation = eulaUrl)
                withStyle(
                    style = SpanStyle(
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(linkText)
                }
                pop()
            }

            @Suppress("DEPRECATION")
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                softWrap = true,
                onClick = { offset ->
                    val annotation = annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()
                    if (annotation != null) {
                        uriHandler.openUri(annotation.item)
                    } else {
                        onEulaAcceptedChange(!eulaAccepted)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (isCreating) {
            WizardProgressCard(creationStatus = creationStatus)
        }

        creationError?.let { err ->
            Surface(
                shape = ShapeDefaults.Medium,
                color = colorScheme.errorContainer,
                contentColor = colorScheme.onErrorContainer,
                border = null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewMonoChip(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    Surface(
        shape = ShapeDefaults.Small,
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurfaceVariant,
        border = null
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = monoFont,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun CreateServerWizardScreenPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        CreateServerWizardScreen(
            onServerCreated = {},
            onCancel = {},
            existingServers = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleServers,
            onDownloadAndCreateServer = { _, _, _, _, _, _, _, _, _ -> null }
        )
    }
}
