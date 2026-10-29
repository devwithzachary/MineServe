package com.devwithzachary.mineserve.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.admin.web.WebAdminConfig
import com.devwithzachary.mineserve.admin.web.WebAdminExposure
import com.devwithzachary.mineserve.admin.web.WebAdminExposureState
import com.devwithzachary.mineserve.admin.web.WebAdminState
import com.devwithzachary.mineserve.ui.components.QrCodeHelper
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.Slate200
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteAdminScreen(
    adminState: WebAdminState,
    adminConfig: WebAdminConfig,
    onUpdateConfig: (WebAdminConfig) -> Unit,
    onGeneratePassword: () -> String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (adminConfig.enabled) EmeraldDark.copy(alpha = 0.4f) else Slate800,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = if (adminConfig.enabled) EmeraldLight else Slate400,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Web Remote Admin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (adminConfig.enabled) "Server running on port ${adminConfig.port}" else "Service stopped",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (adminConfig.enabled) EmeraldLight else Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = adminConfig.enabled,
                        onCheckedChange = { isChecked ->
                            val updated = adminConfig.copy(enabled = isChecked)
                            onUpdateConfig(updated)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldPrimary,
                            checkedTrackColor = Slate800,
                            checkedBorderColor = EmeraldPrimary.copy(alpha = 0.6f),
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate900,
                            uncheckedBorderColor = Slate700
                        )
                    )
                }
            }

            // Connection & Access Card
            if (adminConfig.enabled && adminState.localUrl != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Local Network Access",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Open this address in any browser on your home Wi-Fi network to control your Minecraft servers from a PC or tablet:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )

                        // Address Row with Actions
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate900,
                            border = BorderStroke(1.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = adminState.localUrl,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("MineServe Web Admin URL", adminState.localUrl))
                                            Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy URL",
                                            tint = Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adminState.localUrl))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Open in Browser",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // QR Code Preview
                        if (qrBitmap != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Slate900)
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
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }

            // Security & Authentication Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Security & Admin Credentials",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Require strong credentials to access server consoles, file management, and configuration via the web panel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    // Username Input
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = {
                            usernameInput = it
                            onUpdateConfig(adminConfig.copy(username = it.trim()))
                        },
                        label = { Text("Admin Username") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Slate400)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            onUpdateConfig(adminConfig.copy(password = it))
                        },
                        label = { Text("Admin Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Slate400)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = Slate400
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action buttons: Copy & Generate Password
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MineServe Admin Password", passwordInput))
                                Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Slate700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Password", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val newPass = onGeneratePassword()
                                passwordInput = newPass
                                Toast.makeText(context, "Generated new strong password", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate", color = EmeraldLight, fontSize = 12.sp)
                        }
                    }

                    // Port Setting
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Slate700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Public Internet Exposure Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Public Internet Exposure",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Optionally make the remote web panel accessible outside your local Wi-Fi network using UPnP router port forwarding or public tunnels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    // Vertical List of Exposure Options
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
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) EmeraldDark.copy(alpha = 0.25f) else Slate900,
                                border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary.copy(alpha = 0.6f) else Slate800),
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
                                        onClick = { onUpdateConfig(adminConfig.copy(exposure = exposureType)) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = EmeraldPrimary,
                                            unselectedColor = Slate700
                                        )
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Slate200
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Playit Secret Configuration
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
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Leave empty to automatically generate a claim link on playit.gg.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Bore Custom Relay Configuration
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
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
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
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Default public relay is bore.pub on port 7835.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Exposure Status Display
                    if (adminConfig.exposure != WebAdminExposure.LAN_ONLY && adminConfig.enabled) {
                        HorizontalDivider(color = ObsidianCardBorder)

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
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = exp.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400
                                        )
                                    }

                                    if (exp.claimUrl != null) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(exp.claimUrl))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                tint = EmeraldLight,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open Playit Claim Link", color = EmeraldLight, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            is WebAdminExposureState.Connected -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldDark.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Public Web Admin URL",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldLight
                                            )
                                            Text(
                                                text = exp.publicUrl,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Public Web Admin URL", exp.publicUrl))
                                                Toast.makeText(context, "Public URL copied", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Public URL",
                                                tint = EmeraldLight
                                            )
                                        }
                                    }
                                }
                            }
                            is WebAdminExposureState.Failed -> {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = RedstoneLight.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, RedstoneLight.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Exposure error: ${exp.error}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RedstoneLight,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is WebAdminExposureState.Idle -> {}
                        }

                        // Security Callout
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate900,
                            border = BorderStroke(1.dp, Slate800),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Internet exposure allows remote management outside your Wi-Fi network. Ensure you have set a strong password and never share it publicly.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

