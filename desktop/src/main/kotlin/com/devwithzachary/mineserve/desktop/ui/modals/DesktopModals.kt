package com.devwithzachary.mineserve.desktop.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeEmptyState
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.util.FileStorageUtils
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopModalOverlay(
    viewModel: DesktopViewModel,
    modal: DesktopModal
) {
    if (modal == DesktopModal.NONE) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        when (modal) {
            DesktopModal.CREATE_SERVER -> CreateServerModal(viewModel)
            DesktopModal.IMPORT_SERVER -> ImportServerZipModal(viewModel)
            DesktopModal.CLOUD -> MineServeCloudModal(viewModel)
            DesktopModal.HOST_TOOLS -> DesktopAppSettingsModal(viewModel)
            DesktopModal.USERS_AUDIT -> UsersAndAuditModal(viewModel)
            DesktopModal.ABOUT_CREDITS -> DesktopAboutAndCreditsModal(viewModel)
            DesktopModal.WHATS_NEW -> DesktopWhatsNewModal(viewModel)
            DesktopModal.NONE -> {}
        }
    }
}

@Composable
internal fun DesktopM3ModalSurface(
    width: Dp,
    height: Dp? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sizeMod = if (height != null) {
        Modifier.padding(24.dp).widthIn(max = width).fillMaxWidth().heightIn(max = height)
    } else {
        Modifier.padding(24.dp).widthIn(max = width).fillMaxWidth()
    }
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurface,
        border = null,
        modifier = sizeMod
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateServerModal(viewModel: DesktopViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current

    val hostMem = remember { LinuxHostTelemetry.getMemoryInfo() }
    val maxRam = (hostMem.totalMb.toInt() - 1024).coerceAtLeast(2048)
    val supportedTypes = remember { ServerType.entries.filter { it != ServerType.CUSTOM } }

    var step by remember { mutableIntStateOf(1) }
    var name by remember { mutableStateOf("My Minecraft Server") }
    var type by remember { mutableStateOf(ServerType.PAPER) }
    var availableVersions by remember { mutableStateOf(listOf("1.21.4", "1.21.1", "1.20.4")) }
    var version by remember { mutableStateOf("1.21.4") }
    var versionDropdownExpanded by remember { mutableStateOf(false) }
    var portText by remember { mutableStateOf("25565") }
    var ramMb by remember { mutableIntStateOf(2048.coerceAtMost(maxRam)) }
    var motd by remember { mutableStateOf("A MineServe Linux Server") }
    var eula by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    LaunchedEffect(type) {
        if (type == ServerType.BEDROCK_BDS && portText == "25565") {
            portText = "19132"
        } else if (type != ServerType.BEDROCK_BDS && portText == "19132") {
            portText = "25565"
        }
        val fetched = viewModel.softwareManager.fetchAvailableVersions(type)
        if (fetched.isNotEmpty()) {
            availableVersions = fetched
            version = fetched.first()
        }
    }

    DesktopM3ModalSurface(width = 700.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Create New Server",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = if (step == 1) "Step 1 of 2 - Engine, Name & Version" else "Step 2 of 2 - Resources, Port & EULA",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { if (!busy) viewModel.openModal(DesktopModal.NONE) },
                enabled = !busy
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { if (step == 1) 0.5f else 1.0f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = colorScheme.primary,
            trackColor = colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (step == 1) {
            Text(
                text = "Select Server Engine",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                supportedTypes.chunked(3).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowTypes.forEach { t ->
                            val active = type == t
                            Surface(
                                onClick = { type = t },
                                shape = ShapeDefaults.Medium,
                                color = if (active) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                                contentColor = if (active) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                border = null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = t.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                        repeat(3 - rowTypes.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Server Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                ExposedDropdownMenuBox(
                    expanded = versionDropdownExpanded,
                    onExpandedChange = { versionDropdownExpanded = !versionDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = version,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("All Versions") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = versionDropdownExpanded) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = versionDropdownExpanded,
                        onDismissRequest = { versionDropdownExpanded = false },
                        containerColor = colorScheme.surfaceContainerHighest,
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        availableVersions.forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = v,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                        color = if (v == version) colorScheme.primary else colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    version = v
                                    versionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                availableVersions.take(5).forEach { v ->
                    FilterChip(
                        selected = version == v,
                        onClick = { version = v },
                        label = { Text(v, style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont)) },
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

            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { step = 2 },
                    shape = ShapeDefaults.Medium
                ) {
                    Text("Next: Resources")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = motd,
                    onValueChange = { motd = it },
                    label = { Text("Server MOTD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                    modifier = Modifier.width(130.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Allocated Memory", style = MaterialTheme.typography.bodyLarge)
                MineServeTableMonoText(text = "$ramMb MB / $maxRam MB max", color = colorScheme.primary)
            }
            Slider(
                value = ramMb.toFloat(),
                onValueChange = { ramMb = ((it.toInt() / 256) * 256).coerceIn(512, maxRam) },
                valueRange = 512f..maxRam.toFloat()
            )

            Spacer(modifier = Modifier.height(8.dp))

            MineServeCard(
                containerColor = colorScheme.surfaceContainer,
                shape = ShapeDefaults.Medium,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name.trim().ifBlank { "Minecraft Server" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    MineServeTableMonoText(
                        text = "${type.displayName} $version - :$portText - $ramMb MB",
                        color = colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = eula, onCheckedChange = { eula = it })
                Text(
                    text = "I agree to the ",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { eula = !eula }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        viewModel.openUrlInBrowser("https://www.minecraft.net/en-us/eula")
                    }
                ) {
                    Text(
                        text = "Minecraft End User License Agreement (EULA)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open Minecraft EULA",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            if (statusMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusMsg, style = MaterialTheme.typography.bodySmall, color = colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = { if (!busy) step = 1 },
                    enabled = !busy,
                    shape = ShapeDefaults.Medium
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
                Button(
                    onClick = {
                        if (!eula) {
                            viewModel.showToast("Please accept the Minecraft EULA first.")
                            return@Button
                        }
                        busy = true
                        statusMsg = "Creating server and downloading ${type.displayName} $version..."
                        val resolvedPort = (portText.toIntOrNull() ?: if (type == ServerType.BEDROCK_BDS) 19132 else 25565).coerceIn(1, 65535)
                        val jarFileName = when (type) {
                            ServerType.FORGE, ServerType.NEOFORGE -> "installer.jar"
                            ServerType.BEDROCK_BDS -> "bedrock_server"
                            else -> "server.jar"
                        }
                        viewModel.scope.launch {
                            try {
                                val srv = viewModel.serverRepository.createServer(
                                    name = name.trim().ifBlank { "Minecraft Server" },
                                    type = type,
                                    version = version.trim(),
                                    port = resolvedPort,
                                    ramMb = ramMb,
                                    motd = motd.trim(),
                                    jarFileName = jarFileName,
                                    eulaAccepted = true
                                )
                                runCatching {
                                    viewModel.softwareManager.provisionServer(srv, null) { msg, _ ->
                                        statusMsg = msg
                                    }
                                }.fold(
                                    onSuccess = {
                                        viewModel.selectServer(srv.id)
                                        viewModel.openModal(DesktopModal.NONE)
                                        viewModel.showToast("Created ${srv.name}!")
                                    },
                                    onFailure = { err ->
                                        viewModel.serverRepository.deleteServer(srv.id)
                                        statusMsg = "Download failed: ${err.message}"
                                    }
                                )
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && eula,
                    shape = ShapeDefaults.Medium
                ) {
                    Text(if (busy) "Installing..." else "Create & Download Server")
                }
            }
        }
    }
}

@Composable
private fun ImportServerZipModal(viewModel: DesktopViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current

    val hostMem = remember { LinuxHostTelemetry.getMemoryInfo() }
    val maxRam = (hostMem.totalMb.toInt() - 1024).coerceAtLeast(2048)
    val supportedTypes = remember { ServerType.entries.filter { it != ServerType.CUSTOM } }

    var pickedZip by remember { mutableStateOf<File?>(null) }
    var name by remember { mutableStateOf("Imported Server") }
    var type by remember { mutableStateOf(ServerType.PAPER) }
    var version by remember { mutableStateOf("1.21.4") }
    var portText by remember { mutableStateOf("25565") }
    var ramMb by remember { mutableIntStateOf(2048.coerceAtMost(maxRam)) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(type) {
        if (type == ServerType.BEDROCK_BDS && portText == "25565") {
            portText = "19132"
        } else if (type != ServerType.BEDROCK_BDS && portText == "19132") {
            portText = "25565"
        }
    }

    DesktopM3ModalSurface(width = 660.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Import Existing Server (.zip)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { if (!busy) viewModel.openModal(DesktopModal.NONE) }, enabled = !busy) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        MineServeCard(
            containerColor = colorScheme.surfaceContainer,
            shape = ShapeDefaults.Medium,
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (pickedZip != null) Icons.Rounded.CheckCircle else Icons.Rounded.FolderZip,
                        contentDescription = null,
                        tint = if (pickedZip != null) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = pickedZip?.name ?: "No .zip archive selected",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pickedZip != null) {
                            MineServeTableMonoText(
                                text = "Validated archive - ${FileStorageUtils.formatBytes(pickedZip!!.length())}",
                                color = colorScheme.primary
                            )
                        }
                    }
                }
                FilledTonalButton(
                    onClick = {
                        val dialog = FileDialog(null as Frame?, "Select Server .zip Archive", FileDialog.LOAD)
                        dialog.isVisible = true
                        if (dialog.directory != null && dialog.file != null) {
                            val f = File(dialog.directory, dialog.file)
                            if (f.exists() && f.name.endsWith(".zip", ignoreCase = true)) {
                                pickedZip = f
                                name = f.nameWithoutExtension
                            } else {
                                viewModel.showToast("Please select a valid .zip archive")
                            }
                        }
                    },
                    shape = ShapeDefaults.Medium
                ) {
                    Text("Choose .zip")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Server Engine", style = MaterialTheme.typography.labelLarge, color = colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            supportedTypes.chunked(3).forEach { rowTypes ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    rowTypes.forEach { t ->
                        val active = type == t
                        Surface(
                            onClick = { type = t },
                            shape = ShapeDefaults.Small,
                            color = if (active) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                            contentColor = if (active) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                            border = null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = t.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                    repeat(3 - rowTypes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Server Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = version,
                onValueChange = { version = it },
                label = { Text("Minecraft Version") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                modifier = Modifier.width(150.dp)
            )
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it },
                label = { Text("Port") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                modifier = Modifier.width(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Allocated Memory", style = MaterialTheme.typography.bodyMedium)
            MineServeTableMonoText(text = "$ramMb MB", color = colorScheme.primary)
        }
        Slider(
            value = ramMb.toFloat(),
            onValueChange = { ramMb = ((it.toInt() / 256) * 256).coerceIn(512, maxRam) },
            valueRange = 512f..maxRam.toFloat()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = {
                    val zip = pickedZip
                    if (zip == null) {
                        viewModel.showToast("Pick a .zip archive first")
                        return@Button
                    }
                    busy = true
                    val resolvedPort = (portText.toIntOrNull() ?: if (type == ServerType.BEDROCK_BDS) 19132 else 25565).coerceIn(1, 65535)
                    val initialJar = if (type == ServerType.BEDROCK_BDS) "bedrock_server" else "server.jar"
                    viewModel.scope.launch {
                        try {
                            val srv = viewModel.serverRepository.createServer(
                                name = name.trim().ifBlank { "Imported Server" },
                                type = type,
                                version = version.trim(),
                                port = resolvedPort,
                                ramMb = ramMb,
                                motd = "Imported MineServe Server",
                                jarFileName = initialJar,
                                eulaAccepted = true
                            )
                            val dir = viewModel.serverRepository.getServerDirectory(srv.id)
                            val res = FileStorageUtils.extractZipSafe(zip, dir, deleteZipAfter = false)
                            if (res.isFailure) {
                                viewModel.serverRepository.deleteServer(srv.id)
                                viewModel.showToast("Import failed: ${res.exceptionOrNull()?.message ?: "Unable to extract zip"}")
                                return@launch
                            }
                            val topEntries = dir.listFiles()?.filter { it.name != "__MACOSX" } ?: emptyList()
                            if (topEntries.size == 1 && topEntries.first().isDirectory) {
                                val inner = topEntries.first()
                                inner.listFiles()?.forEach { child ->
                                    val target = File(dir, child.name)
                                    if (!child.renameTo(target)) {
                                        if (child.isDirectory) child.copyRecursively(target, overwrite = true) else child.copyTo(target, overwrite = true)
                                        child.deleteRecursively()
                                    }
                                }
                                inner.deleteRecursively()
                            }
                            val detectedJar = if (type == ServerType.BEDROCK_BDS) {
                                "bedrock_server"
                            } else {
                                val jars = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".jar", ignoreCase = true) } ?: emptyList()
                                (jars.firstOrNull { it.name.equals("server.jar", ignoreCase = true) } ?: jars.firstOrNull())?.name ?: "server.jar"
                            }
                            if (detectedJar != srv.jarFileName) {
                                viewModel.serverRepository.updateServer(srv.copy(jarFileName = detectedJar))
                            }
                            File(dir, "eula.txt").writeText("eula=true\n")
                            viewModel.selectServer(srv.id)
                            viewModel.openModal(DesktopModal.NONE)
                            viewModel.showToast("Imported ${srv.name} ($detectedJar)!")
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                shape = ShapeDefaults.Medium
            ) {
                Text(if (busy) "Extracting..." else "Import Server")
            }
        }
    }
}

@Composable
private fun MineServeCloudModal(viewModel: DesktopViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var linked by remember { mutableStateOf(viewModel.cloudManager.isLinked) }
    var emailInput by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    DesktopM3ModalSurface(width = 700.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = colorScheme.tertiary, modifier = Modifier.size(26.dp))
                Column {
                    Text(
                        text = "MineServe Cloud",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Custom .play.mineserve.app / .playms.uk Domains, Relays & Encrypted R2 Backups",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MineServeCard(
            containerColor = colorScheme.surfaceContainer,
            shape = ShapeDefaults.Large,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (linked) {
                val cloudStatus = viewModel.cloudManager.toStatusJson()
                val cloudEmail = cloudStatus.optString("email", "")
                val cloudTier = cloudStatus.optString("tier", "FREE").uppercase(Locale.US)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = cloudEmail,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = colorScheme.tertiaryContainer,
                            contentColor = colorScheme.onTertiaryContainer,
                            border = null
                        ) {
                            Text(
                                text = "$cloudTier TIER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.scope.launch {
                                    viewModel.cloudManager.refreshProfile()
                                    viewModel.showToast("Refreshed cloud account status")
                                }
                            },
                            shape = ShapeDefaults.Small
                        ) {
                            Text("Refresh Plan")
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.cloudManager.signOut()
                                linked = false
                                viewModel.showToast("Signed out of MineServe Cloud")
                            },
                            shape = ShapeDefaults.Small,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            } else {
                Text(
                    text = "Sign In or Link Account (6-Digit Email Code)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            viewModel.scope.launch {
                                viewModel.cloudManager.requestEmailOtp(emailInput).fold(
                                    onSuccess = {
                                        otpSent = true
                                        statusMsg = "Sent 6-digit verification code to $emailInput"
                                    },
                                    onFailure = { err -> statusMsg = err.message ?: "Failed to send code" }
                                )
                            }
                        },
                        shape = ShapeDefaults.Medium
                    ) {
                        Text("Send Code")
                    }
                }
                if (otpSent) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("6-Digit Verification Code") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                viewModel.scope.launch {
                                    viewModel.cloudManager.verifyEmailOtp(emailInput, otpCode).fold(
                                        onSuccess = {
                                            linked = true
                                            statusMsg = "Signed in!"
                                        },
                                        onFailure = { err -> statusMsg = err.message ?: "Verification failed" }
                                    )
                                }
                            },
                            shape = ShapeDefaults.Medium
                        ) {
                            Text("Verify & Link")
                        }
                    }
                }
                if (statusMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(statusMsg, style = MaterialTheme.typography.bodySmall, color = colorScheme.tertiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MineServeCard(
                containerColor = colorScheme.surfaceContainer,
                shape = ShapeDefaults.Large,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("MineServe Plus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "\$2.99 / month",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = monoFont),
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("- 1 Custom .play.mineserve.app / .playms.uk Subdomain", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                Text("- 10 GB Encrypted R2 Cloud Backups", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                Text("- Priority US & EU Relay Bandwidth", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
            }
            MineServeCard(
                containerColor = colorScheme.surfaceContainer,
                shape = ShapeDefaults.Large,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("MineServe Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "\$6.99 / month",
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = monoFont),
                    fontWeight = FontWeight.Bold,
                    color = extended.warning
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("- Up to 3 Custom Subdomains + Admin Relay", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                Text("- 50 GB Encrypted R2 Cloud Backups", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                Text("- Unmetered Multi-Region Relay Traffic", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = { viewModel.openUrlInBrowser("https://cloud.mineserve.app") },
                shape = ShapeDefaults.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.tertiary,
                    contentColor = colorScheme.onTertiary
                )
            ) {
                Text("Open Cloud Portal (cloud.mineserve.app)")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UsersAndAuditModal(viewModel: DesktopViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Users & Roles, 1: Audit Log
    var users by remember { mutableStateOf(viewModel.userRepository.listUsers()) }
    val auditEntries = remember(users) { viewModel.userRepository.getAuditLog(150) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(UserRole.ADMIN) }
    var auditQuery by remember { mutableStateOf("") }
    var pendingDeleteUserId by remember { mutableStateOf<Pair<String, String>?>(null) }

    pendingDeleteUserId?.let { (id, uname) ->
        MineServeDialog(
            onDismissRequest = { pendingDeleteUserId = null },
            title = "Remove User $uname?",
            confirmLabel = "Remove",
            isDestructive = true,
            icon = Icons.Rounded.Delete,
            onConfirm = {
                viewModel.userRepository.deleteUser(id)
                users = viewModel.userRepository.listUsers()
                pendingDeleteUserId = null
                viewModel.showToast("Removed user $uname")
            }
        ) {
            Text(
                text = "Revokes Web Remote Admin access and active sessions for $uname.",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }

    DesktopM3ModalSurface(width = 840.dp, height = 620.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Users & Audit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(
                0 to "Users & Roles (${users.size})",
                1 to "Audit Log (${auditEntries.size})"
            ).forEachIndexed { index, (idx, label) ->
                SegmentedButton(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colorScheme.secondaryContainer,
                        activeContentColor = colorScheme.onSecondaryContainer,
                        inactiveContainerColor = colorScheme.surfaceContainer,
                        inactiveContentColor = colorScheme.onSurfaceVariant,
                        activeBorderColor = colorScheme.outlineVariant,
                        inactiveBorderColor = colorScheme.outlineVariant
                    )
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeTab == 0) {
            MineServeCard(
                containerColor = colorScheme.surfaceContainer,
                shape = ShapeDefaults.Large,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.width(170.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Min 6 chars", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.width(200.dp)
                    )
                    listOf(UserRole.ADMIN, UserRole.MODERATOR, UserRole.VIEWER).forEach { r ->
                        FilterChip(
                            selected = newRole == r,
                            onClick = { newRole = r },
                            label = { Text(r.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.secondaryContainer,
                                selectedLabelColor = colorScheme.onSecondaryContainer,
                                containerColor = colorScheme.surfaceContainerHigh,
                                labelColor = colorScheme.onSurfaceVariant
                            ),
                            border = null
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.userRepository.createUser(newUsername.trim(), newPassword, newRole).fold(
                                onSuccess = {
                                    users = viewModel.userRepository.listUsers()
                                    newUsername = ""
                                    newPassword = ""
                                    viewModel.showToast("Created user")
                                },
                                onFailure = { err -> viewModel.showToast(err.message ?: "Failed to create user") }
                            )
                        },
                        shape = ShapeDefaults.Medium
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add User")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MineServeTableContainer(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(users, key = { it.id }) { u ->
                        MineServeTableRow(
                            showDivider = u != users.last(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = u.username,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                MineServeTableMonoText(
                                    text = "ID: ${u.id}",
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = ShapeDefaults.Small,
                                color = if (u.role == UserRole.OWNER) colorScheme.primaryContainer else colorScheme.secondaryContainer,
                                contentColor = if (u.role == UserRole.OWNER) colorScheme.onPrimaryContainer else colorScheme.onSecondaryContainer,
                                border = null
                            ) {
                                Text(
                                    text = u.role.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (u.role != UserRole.OWNER) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { pendingDeleteUserId = u.id to u.username },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Remove user",
                                        tint = colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val filteredAudit = remember(auditEntries, auditQuery) {
                val q = auditQuery.trim()
                if (q.isEmpty()) auditEntries else auditEntries.filter {
                    it.username.contains(q, ignoreCase = true) ||
                        it.action.contains(q, ignoreCase = true) ||
                        it.target.contains(q, ignoreCase = true) ||
                        it.clientIp.contains(q, ignoreCase = true)
                }
            }

            OutlinedTextField(
                value = auditQuery,
                onValueChange = { auditQuery = it },
                placeholder = { Text("Filter audit events by user, action, target, or IP...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (filteredAudit.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MineServeEmptyState(
                        icon = Icons.Rounded.LinearScale,
                        title = "No Audit Entries",
                        supportingText = "Panel login and action events will appear here."
                    )
                }
            } else {
                MineServeTableContainer(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(filteredAudit) { idx, entry ->
                            MineServeTableRow(
                                showDivider = idx < filteredAudit.lastIndex,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${entry.username} (${entry.role})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = entry.action,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                                            color = colorScheme.primary
                                        )
                                        if (entry.target.isNotBlank()) {
                                            MineServeTableMonoText(
                                                text = entry.target,
                                                color = colorScheme.tertiary
                                            )
                                        }
                                    }
                                    if (entry.details.isNotBlank()) {
                                        Text(
                                            text = entry.details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    MineServeTableMonoText(
                                        text = dateFormat.format(Date(entry.timestamp)),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    if (entry.clientIp.isNotBlank()) {
                                        MineServeTableMonoText(
                                            text = entry.clientIp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
