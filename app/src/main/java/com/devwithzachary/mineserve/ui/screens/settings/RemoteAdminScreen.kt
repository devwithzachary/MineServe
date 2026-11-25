package com.devwithzachary.mineserve.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.admin.web.WebAdminConfig
import com.devwithzachary.mineserve.admin.web.WebAdminExposure
import com.devwithzachary.mineserve.admin.web.WebAdminExposureState
import com.devwithzachary.mineserve.admin.web.WebAdminState
import com.devwithzachary.mineserve.auth.AuditLogEntry
import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.components.NetworkAddressChip
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.ui.components.QrCodeHelper
import com.devwithzachary.mineserve.ui.theme.LayoutManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteAdminScreen(
    adminState: WebAdminState,
    adminConfig: WebAdminConfig,
    onUpdateConfig: (WebAdminConfig) -> Unit,
    onGeneratePassword: () -> String,
    servers: List<MinecraftServer> = emptyList(),
    panelUsers: List<PanelUser> = emptyList(),
    auditEntries: List<AuditLogEntry> = emptyList(),
    onCreateUser: (String, String, UserRole, Boolean, List<String>) -> Result<PanelUser> = { _, _, _, _, _ -> Result.failure(IllegalStateException("Not configured")) },
    onUpdateUser: (String, UserRole, Boolean, List<String>, Boolean, String?) -> Result<PanelUser> = { _, _, _, _, _, _ -> Result.failure(IllegalStateException("Not configured")) },
    onDeleteUser: (String) -> Result<Unit> = { Result.failure(IllegalStateException("Not configured")) },
    onRefreshUsers: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    var usernameInput by remember(adminConfig.username) { mutableStateOf(adminConfig.username) }
    var passwordInput by remember(adminConfig.password) { mutableStateOf(adminConfig.password) }
    var portInput by remember(adminConfig.port) { mutableStateOf(adminConfig.port.toString()) }
    var passwordVisible by remember { mutableStateOf(false) }

    var playitSecretInput by remember(adminConfig.playitSecret) { mutableStateOf(adminConfig.playitSecret) }
    var customBoreHostInput by remember(adminConfig.customBoreHost) { mutableStateOf(adminConfig.customBoreHost) }
    var customBorePortInput by remember(adminConfig.customBorePort) { mutableStateOf(adminConfig.customBorePort.toString()) }

    val qrBitmap = remember(adminState.localUrl) {
        val targetUrl = adminState.localUrl
        if (!targetUrl.isNullOrBlank()) {
            QrCodeHelper.generateQrBitmap(
                content = targetUrl,
                sizePx = 360,
                darkColor = android.graphics.Color.WHITE,
                lightColor = android.graphics.Color.TRANSPARENT
            )
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Web Remote Admin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Master Toggle Card
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = if (adminConfig.enabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Dns,
                                    contentDescription = null,
                                    tint = if (adminConfig.enabled) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Web Remote Admin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (adminConfig.enabled) "Server running on port ${adminConfig.port}" else "Service stopped",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = monoFont,
                                color = if (adminConfig.enabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = adminConfig.enabled,
                        onCheckedChange = { isChecked ->
                            onUpdateConfig(adminConfig.copy(enabled = isChecked))
                        }
                    )
                }
            }

            // Connection & Access Group
            if (adminConfig.enabled && adminState.localUrl != null) {
                MineServeSettingsGroup(title = "Local Network Access") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Open this address in any browser on your home Wi-Fi network to control your Minecraft servers from a PC or tablet:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NetworkAddressChip(
                                address = adminState.localUrl,
                                isPublicTunnel = false,
                                onCopied = {
                                    Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adminState.localUrl))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Open in Browser",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (qrBitmap != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ShapeDefaults.Medium)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Web Admin QR Code",
                                    modifier = Modifier.size(160.dp)
                                )
                                Text(
                                    text = "Scan with your laptop or tablet camera to open instantly",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Security & Authentication Group
            MineServeSettingsGroup(title = "Security & Admin Credentials") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Require strong credentials to access server consoles, files, and configuration via the web panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = {
                            usernameInput = it
                            onUpdateConfig(adminConfig.copy(username = it.trim()))
                        },
                        label = { Text("Admin Username") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            onUpdateConfig(adminConfig.copy(password = it))
                        },
                        label = { Text("Admin Password") },
                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MineServe Admin Password", passwordInput))
                                Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Password", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = {
                                val newPass = onGeneratePassword()
                                passwordInput = newPass
                                Toast.makeText(context, "Generated new strong password", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = ShapeDefaults.Small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedTextField(
                        value = portInput,
                        onValueChange = {
                            portInput = it
                            val parsedPort = it.toIntOrNull()
                            if (parsedPort != null && parsedPort in 1024..65535) {
                                onUpdateConfig(adminConfig.copy(port = parsedPort))
                            }
                        },
                        label = { Text("Local HTTP Port") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Panel Users, Roles & Audit Log Card
            PanelUsersCard(
                users = panelUsers,
                auditEntries = auditEntries,
                servers = servers,
                onCreateUser = onCreateUser,
                onUpdateUser = onUpdateUser,
                onDeleteUser = onDeleteUser,
                onRefresh = onRefreshUsers
            )

            // Public Internet Exposure Group
            MineServeSettingsGroup(title = "Public Internet Exposure") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Optionally make the remote web panel accessible outside your local Wi-Fi network using UPnP router port forwarding or public tunnels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val exposureOptions = listOf(
                        Triple(
                            WebAdminExposure.LAN_ONLY,
                            "LAN Only (Local Wi-Fi)",
                            "Restricted strictly to devices on your local Wi-Fi network (most secure)."
                        ),
                        Triple(
                            WebAdminExposure.UPNP_PORT_FORWARD,
                            "Router Port Forwarding (UPnP)",
                            "Automatic router port mapping directly to your residential public WAN IP."
                        ),
                        Triple(
                            WebAdminExposure.BORE_TUNNEL,
                            "Bore Public Tunnel (bore.pub)",
                            "Instant zero-config public TCP tunnel via bore.pub relay."
                        ),
                        Triple(
                            WebAdminExposure.PLAYIT_TUNNEL,
                            "Playit Tunnel (playit.gg)",
                            "Global anycast tunnel network with custom domains or claim links."
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((exposureType, title, desc) in exposureOptions) {
                            val isSelected = adminConfig.exposure == exposureType
                            Surface(
                                shape = ShapeDefaults.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                onClick = { onUpdateConfig(adminConfig.copy(exposure = exposureType)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onUpdateConfig(adminConfig.copy(exposure = exposureType)) }
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (adminConfig.exposure == WebAdminExposure.PLAYIT_TUNNEL) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = playitSecretInput,
                                onValueChange = {
                                    playitSecretInput = it
                                    onUpdateConfig(adminConfig.copy(playitSecret = it.trim()))
                                },
                                label = { Text("Playit Secret Key (Optional)") },
                                placeholder = { Text("Paste playit.gg secret key or leave blank to claim") },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Leave empty to automatically generate a claim link on playit.gg.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (adminConfig.exposure == WebAdminExposure.BORE_TUNNEL) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customBoreHostInput,
                                    onValueChange = {
                                        customBoreHostInput = it
                                        onUpdateConfig(adminConfig.copy(customBoreHost = it.trim()))
                                    },
                                    label = { Text("Relay Host") },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                    singleLine = true,
                                    modifier = Modifier.weight(2f)
                                )

                                OutlinedTextField(
                                    value = customBorePortInput,
                                    onValueChange = {
                                        customBorePortInput = it
                                        val p = it.toIntOrNull()
                                        if (p != null && p in 1..65535) {
                                            onUpdateConfig(adminConfig.copy(customBorePort = p))
                                        }
                                    },
                                    label = { Text("Port") },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Default public relay is bore.pub on port 7835.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (adminConfig.exposure != WebAdminExposure.LAN_ONLY && adminConfig.enabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        when (val exp = adminState.exposureState) {
                            is WebAdminExposureState.Connecting -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = exp.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (exp.claimUrl != null) {
                                        FilledTonalButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exp.claimUrl))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            shape = ShapeDefaults.Small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open Playit Claim Link")
                                        }
                                    }
                                }
                            }
                            is WebAdminExposureState.Connected -> {
                                NetworkAddressChip(
                                    address = exp.publicUrl,
                                    isPublicTunnel = true,
                                    onCopied = {
                                        Toast.makeText(context, "Public URL copied", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            is WebAdminExposureState.Failed -> {
                                Surface(
                                    shape = ShapeDefaults.Small,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Exposure error: ${exp.error}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is WebAdminExposureState.Idle -> {}
                        }

                        Surface(
                            shape = ShapeDefaults.Small,
                            color = extColors.warningContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = extColors.onWarningContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Internet exposure allows remote management outside your Wi-Fi network. Ensure you have set a strong password and never share it publicly.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extColors.onWarningContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun RemoteAdminScreenPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        RemoteAdminScreen(
            adminState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleWebAdminState,
            adminConfig = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleWebAdminConfig,
            onUpdateConfig = {},
            onGeneratePassword = { "generated-pass-123" },
            servers = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleServers,
            panelUsers = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.samplePanelUsers,
            auditEntries = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleAuditEntries,
            onBack = {}
        )
    }
}
