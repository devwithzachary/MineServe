package com.devwithzachary.mineserve.desktop.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.desktop.LinuxHostTelemetry
import com.devwithzachary.mineserve.desktop.ui.DesktopColors
import com.devwithzachary.mineserve.desktop.ui.DesktopModal
import com.devwithzachary.mineserve.desktop.ui.DesktopPanelCard
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun DesktopModalOverlay(
    viewModel: DesktopViewModel,
    modal: DesktopModal
) {
    if (modal == DesktopModal.NONE) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
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
            DesktopModal.NONE -> {}
        }
    }
}

@Composable
private fun CreateServerModal(viewModel: DesktopViewModel) {
    val hostMem = remember { LinuxHostTelemetry.getMemoryInfo() }
    val maxRam = (hostMem.totalMb.toInt() - 1024).coerceAtLeast(2048)

    var name by remember { mutableStateOf("My Minecraft Server") }
    var type by remember { mutableStateOf(ServerType.PAPER) }
    var availableVersions by remember { mutableStateOf(listOf("1.21.4", "1.21.1", "1.20.4")) }
    var version by remember { mutableStateOf("1.21.4") }
    var portText by remember { mutableStateOf("25565") }
    var ramMb by remember { mutableStateOf(2048.coerceAtMost(maxRam)) }
    var motd by remember { mutableStateOf("A MineServe Linux Server") }
    var eula by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }

    LaunchedEffect(type) {
        val fetched = viewModel.softwareManager.fetchAvailableVersions(type)
        if (fetched.isNotEmpty()) {
            availableVersions = fetched
            version = fetched.first()
        }
    }

    DesktopPanelCard(modifier = Modifier.width(680.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Create New Minecraft Server", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedButton(onClick = { if (!busy) viewModel.openModal(DesktopModal.NONE) }) {
                Text("Close", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Select Engine", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DesktopColors.Slate300)
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ServerType.entries.chunked(5).forEach { rowTypes ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    rowTypes.forEach { t ->
                        val active = type == t
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                            border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.weight(1f).clickable { type = t }
                        ) {
                            Text(
                                text = t.displayName,
                                color = if (active) Color.Black else DesktopColors.Slate300,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
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
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = version,
                onValueChange = { version = it },
                label = { Text("Version (${availableVersions.take(3).joinToString(", ")})") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it },
                label = { Text("Port") },
                singleLine = true,
                modifier = Modifier.width(110.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Allocated Memory: $ramMb MB", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Slider(
            value = ramMb.toFloat(),
            onValueChange = { ramMb = ((it.toInt() / 256) * 256).coerceIn(512, maxRam) },
            valueRange = 512f..maxRam.toFloat()
        )

        OutlinedTextField(
            value = motd,
            onValueChange = { motd = it },
            label = { Text("Server MOTD") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = eula, onCheckedChange = { eula = it })
            Text("I agree to the Minecraft End User License Agreement (EULA)", fontSize = 12.sp, color = DesktopColors.Slate300)
        }

        if (statusMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(statusMsg, fontSize = 12.sp, color = DesktopColors.EmeraldLight)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = {
                    if (!eula) {
                        viewModel.showToast("Please accept the Minecraft EULA first.")
                        return@Button
                    }
                    busy = true
                    statusMsg = "Creating server and downloading ${type.displayName} $version..."
                    viewModel.scope.launch {
                        try {
                            val srv = viewModel.serverRepository.createServer(
                                name = name.trim().ifBlank { "Minecraft Server" },
                                type = type,
                                version = version.trim(),
                                port = portText.toIntOrNull() ?: 25565,
                                ramMb = ramMb,
                                motd = motd.trim(),
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
                                    statusMsg = "Download failed: ${err.message}"
                                }
                            )
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text(if (busy) "Installing..." else "Create & Download Server", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ImportServerZipModal(viewModel: DesktopViewModel) {
    var pickedZip by remember { mutableStateOf<File?>(null) }
    var name by remember { mutableStateOf("Imported Server") }
    var type by remember { mutableStateOf(ServerType.PAPER) }
    var version by remember { mutableStateOf("1.21.4") }
    var portText by remember { mutableStateOf("25565") }
    var ramMb by remember { mutableStateOf(2048) }
    var busy by remember { mutableStateOf(false) }

    DesktopPanelCard(modifier = Modifier.width(560.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Import Existing Server (.zip)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedButton(onClick = { if (!busy) viewModel.openModal(DesktopModal.NONE) }) {
                Text("Close", fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val dialog = FileDialog(null as Frame?, "Select Server .zip Archive", FileDialog.LOAD)
                    dialog.isVisible = true
                    if (dialog.directory != null && dialog.file != null) {
                        val f = File(dialog.directory, dialog.file)
                        if (f.exists()) {
                            pickedZip = f
                            name = f.nameWithoutExtension
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.Slate800, contentColor = DesktopColors.EmeraldLight)
            ) {
                Text("Choose .zip Archive", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(
                text = pickedZip?.name ?: "No archive selected",
                fontSize = 12.sp,
                color = DesktopColors.Slate300
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
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
                modifier = Modifier.width(150.dp)
            )
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it },
                label = { Text("Port") },
                singleLine = true,
                modifier = Modifier.width(110.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = {
                    val zip = pickedZip
                    if (zip == null) {
                        viewModel.showToast("Pick a .zip archive first")
                        return@Button
                    }
                    busy = true
                    viewModel.scope.launch {
                        try {
                            val srv = viewModel.serverRepository.createServer(
                                name = name.trim().ifBlank { "Imported Server" },
                                type = type,
                                version = version.trim(),
                                port = portText.toIntOrNull() ?: 25565,
                                ramMb = ramMb,
                                motd = "Imported MineServe Server",
                                eulaAccepted = true
                            )
                            val dir = viewModel.serverRepository.getServerDirectory(srv.id)
                            FileStorageUtils.extractZipSafe(zip, dir, deleteZipAfter = false)
                            File(dir, "eula.txt").writeText("eula=true\n")
                            viewModel.selectServer(srv.id)
                            viewModel.openModal(DesktopModal.NONE)
                            viewModel.showToast("Imported ${srv.name}!")
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text(if (busy) "Extracting..." else "Import Server", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MineServeCloudModal(viewModel: DesktopViewModel) {
    var linked by remember { mutableStateOf(viewModel.cloudManager.isLinked) }
    var emailInput by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    DesktopPanelCard(modifier = Modifier.width(680.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MineServe Cloud", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DesktopColors.EmeraldLight)
                Text("Custom .play.mineserve.app Subdomains, High-Speed Relays & Encrypted R2 Cloud Backups", fontSize = 12.sp, color = DesktopColors.Slate400)
            }
            OutlinedButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Text("Close", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Account Sign-In / Status Card
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = DesktopColors.Slate900,
            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (linked) {
                    val cloudStatus = viewModel.cloudManager.toStatusJson()
                    val cloudEmail = cloudStatus.optString("email", "")
                    val cloudTier = cloudStatus.optString("tier", "FREE")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Signed in as $cloudEmail", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Active Plan: ${cloudTier.uppercase()}", fontSize = 12.sp, color = DesktopColors.EmeraldLight)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.scope.launch {
                                        viewModel.cloudManager.refreshProfile()
                                        viewModel.showToast("Refreshed cloud account status")
                                    }
                                }
                            ) {
                                Text("Refresh Plan", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.cloudManager.signOut()
                                    linked = false
                                    viewModel.showToast("Signed out of MineServe Cloud")
                                }
                            ) {
                                Text("Sign Out", fontSize = 11.sp, color = DesktopColors.RedstoneLight)
                            }
                        }
                    }
                } else {
                    Text("Sign In or Create Account (6-Digit Email Code)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
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
                            colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                        ) {
                            Text("Send Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    if (otpSent) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { otpCode = it },
                                label = { Text("6-Digit Verification Code") },
                                singleLine = true,
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
                                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
                            ) {
                                Text("Verify & Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    if (statusMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(statusMsg, fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Plans & Pricing Cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DesktopColors.Slate900,
                border = BorderStroke(1.dp, DesktopColors.EmeraldPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MineServe Plus", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("$2.99 / month", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DesktopColors.EmeraldLight)
                    Text("- 1 Custom .play.mineserve.app Subdomain", fontSize = 11.sp, color = DesktopColors.Slate300)
                    Text("- 10 GB Encrypted R2 Cloud Backups", fontSize = 11.sp, color = DesktopColors.Slate300)
                    Text("- Priority US & EU Relay Bandwidth", fontSize = 11.sp, color = DesktopColors.Slate300)
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DesktopColors.Slate900,
                border = BorderStroke(1.dp, DesktopColors.AmberWarning.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MineServe Pro", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("$6.99 / month", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DesktopColors.AmberLight)
                    Text("- Up to 5 Custom Subdomains + Admin Relay", fontSize = 11.sp, color = DesktopColors.Slate300)
                    Text("- 50 GB Encrypted R2 Cloud Backups", fontSize = 11.sp, color = DesktopColors.Slate300)
                    Text("- Unmetered Multi-Region Relay Traffic", fontSize = 11.sp, color = DesktopColors.Slate300)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = { viewModel.openUrlInBrowser("https://cloud.mineserve.app") },
                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text("Open MineServe Cloud Portal (cloud.mineserve.app)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HostToolsModal(viewModel: DesktopViewModel) {
    var installingMajor by remember { mutableStateOf<Int?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    DesktopPanelCard(modifier = Modifier.width(620.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Host Tools & Portable Java Runtime Manager", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Text("Close", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Manage isolated Adoptium Temurin OpenJDK runtimes in ${viewModel.paths.runtimesDir.absolutePath}",
            fontSize = 12.sp,
            color = DesktopColors.Slate400
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(8, 17, 21, 25).forEach { major ->
                val resolved: File? = remember(major, refreshTick) { viewModel.javaManager.getJavaBinary(major) }
                val portableInstalled: Boolean = remember(major, refreshTick) { viewModel.javaManager.isJavaInstalled(major) }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DesktopColors.Slate900,
                    border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("OpenJDK $major LTS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            Text(
                                text = when {
                                    portableInstalled -> "Installed (${resolved?.absolutePath})"
                                    resolved != null -> "System JDK detected (${resolved.absolutePath})"
                                    else -> "Not installed (will auto-download on server start)"
                                },
                                fontSize = 11.sp,
                                color = if (resolved != null) DesktopColors.EmeraldLight else DesktopColors.Slate400
                            )
                        }
                        Button(
                            onClick = {
                                installingMajor = major
                                viewModel.scope.launch {
                                    viewModel.javaManager.installJava(major) { msg: String ->
                                        viewModel.showToast(msg)
                                    }
                                    installingMajor = null
                                    refreshTick++
                                }
                            },
                            enabled = installingMajor == null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (portableInstalled) DesktopColors.Slate800 else DesktopColors.EmeraldPrimary,
                                contentColor = if (portableInstalled) DesktopColors.EmeraldLight else Color.Black
                            )
                        ) {
                            Text(
                                text = when {
                                    installingMajor == major -> "Downloading..."
                                    portableInstalled -> "Installed"
                                    else -> "Install Portable JDK $major"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersAndAuditModal(viewModel: DesktopViewModel) {
    var users by remember { mutableStateOf(viewModel.userRepository.listUsers()) }
    val auditEntries = remember(users) { viewModel.userRepository.getAuditLog(100) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(UserRole.ADMIN) }

    DesktopPanelCard(modifier = Modifier.width(760.dp).height(560.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Panel Users (RBAC) & Audit Log", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedButton(onClick = { viewModel.openModal(DesktopModal.NONE) }) {
                Text("Close", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newUsername,
                onValueChange = { newUsername = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("Password (min 6 chars)") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            listOf(UserRole.ADMIN, UserRole.MODERATOR, UserRole.VIEWER).forEach { r ->
                val active = newRole == r
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (active) DesktopColors.EmeraldPrimary else DesktopColors.Slate900,
                    border = BorderStroke(1.dp, if (active) DesktopColors.EmeraldPrimary else DesktopColors.ObsidianCardBorder),
                    modifier = Modifier.clickable { newRole = r }
                ) {
                    Text(
                        text = r.name,
                        color = if (active) Color.Black else DesktopColors.Slate300,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
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
                colors = ButtonDefaults.buttonColors(containerColor = DesktopColors.EmeraldPrimary, contentColor = Color.Black)
            ) {
                Text("+ Add User", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DesktopColors.ObsidianCardBorder)
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            // Left: Users
            Column(modifier = Modifier.weight(0.9f)) {
                Text("Accounts (${users.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(users, key = { it.id }) { u ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DesktopColors.Slate900,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(u.username, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(u.role.displayName, fontSize = 11.sp, color = DesktopColors.EmeraldLight)
                                }
                                if (u.role != UserRole.OWNER) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.userRepository.deleteUser(u.id)
                                            users = viewModel.userRepository.listUsers()
                                        },
                                        modifier = Modifier.height(26.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Remove", fontSize = 10.sp, color = DesktopColors.RedstoneLight)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right: Audit Log
            Column(modifier = Modifier.weight(1.3f)) {
                Text("Recent Audit Trail (${auditEntries.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(auditEntries) { entry ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DesktopColors.Slate950,
                            border = BorderStroke(1.dp, DesktopColors.ObsidianCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${entry.username} (${entry.role}) -> ${entry.action} [${entry.target}] ${entry.details}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DesktopColors.Slate300,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
