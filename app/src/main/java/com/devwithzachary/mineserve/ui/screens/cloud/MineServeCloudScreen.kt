package com.devwithzachary.mineserve.ui.screens.cloud

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.cloud.CloudAccountProfile
import com.devwithzachary.mineserve.cloud.CloudAccountState
import com.devwithzachary.mineserve.cloud.CloudPricing
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.components.MineServeTableContainer
import com.devwithzachary.mineserve.designsystem.components.MineServeTableMonoText
import com.devwithzachary.mineserve.designsystem.components.MineServeTableRow
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineServeCloudScreen(
    cloudState: CloudAccountState,
    onBack: () -> Unit,
    onOpenCloudPortal: () -> Unit,
    onRefreshCloud: () -> Unit,
    onSignOutCloud: () -> Unit,
    onRequestCloudOtp: (email: String, onResult: (Result<Unit>) -> Unit) -> Unit = { _, _ -> },
    onVerifyCloudOtp: (email: String, code: String, onResult: (Result<CloudAccountProfile>) -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    var otpEmail by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpBusy by remember { mutableStateOf(false) }
    var otpFeedback by remember { mutableStateOf<String?>(null) }
    var otpIsError by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(cloudState.isLinked) {
        if (cloudState.isLinked) {
            otpSent = false
            otpCode = ""
            otpFeedback = null
            otpIsError = false
        }
        while (cloudState.isLinked) {
            onRefreshCloud()
            delay(15_000L)
        }
    }

    val profile = cloudState.profile

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("MineServe Cloud", fontWeight = FontWeight.Bold)
                        Surface(
                            shape = ShapeDefaults.Small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "CLOUD",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = LayoutManager.screenHorizontalPadding, vertical = LayoutManager.screenVerticalPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
        ) {
            // Account Status Card
            MineServeCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = when {
                                    cloudState.isLinked -> "Linked Account"
                                    BuildConfig.IS_PLAY_STORE -> "Sign In to MineServe Cloud"
                                    else -> "Optional Cloud Add-On"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (cloudState.isLinked && profile != null) {
                            Surface(
                                shape = ShapeDefaults.Small,
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = profile.tier,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (!cloudState.isLinked) {
                        if (BuildConfig.IS_PLAY_STORE) {
                            Text(
                                text = "Sign in with an existing MineServe Cloud account to sync custom *.play.mineserve.app addresses, remote Web Admin access, and encrypted off-device backups. Local hosting is 100% free. Purchases and plan changes are not available in this app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = otpEmail,
                                onValueChange = {
                                    otpEmail = it
                                    if (otpIsError) otpFeedback = null
                                },
                                label = { Text("Email address") },
                                singleLine = true,
                                enabled = !otpBusy && !otpSent,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!otpSent) {
                                Button(
                                    onClick = {
                                        val trimmed = otpEmail.trim()
                                        if (trimmed.isBlank() || !trimmed.contains("@")) {
                                            otpIsError = true
                                            otpFeedback = "Please enter a valid email address."
                                            return@Button
                                        }
                                        otpBusy = true
                                        otpFeedback = null
                                        otpIsError = false
                                        onRequestCloudOtp(trimmed) { result ->
                                            otpBusy = false
                                            result.fold(
                                                onSuccess = {
                                                    otpSent = true
                                                    otpCode = ""
                                                    otpIsError = false
                                                    otpFeedback = "A 6-digit verification code has been sent to $trimmed."
                                                },
                                                onFailure = { err ->
                                                    otpIsError = true
                                                    otpFeedback = err.message ?: "Failed to send verification code."
                                                }
                                            )
                                        }
                                    },
                                    enabled = !otpBusy && otpEmail.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = ShapeDefaults.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (otpBusy) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Send Sign-In Code", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { input ->
                                        otpCode = input.filter { it.isDigit() }.take(6)
                                        if (otpIsError) otpFeedback = null
                                    },
                                    label = { Text("6-digit verification code") },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                                    singleLine = true,
                                    enabled = !otpBusy,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        otpBusy = true
                                        otpFeedback = null
                                        otpIsError = false
                                        onVerifyCloudOtp(otpEmail.trim(), otpCode) { result ->
                                            otpBusy = false
                                            result.fold(
                                                onSuccess = {
                                                    otpSent = false
                                                    otpCode = ""
                                                    otpFeedback = null
                                                    otpIsError = false
                                                },
                                                onFailure = { err ->
                                                    otpIsError = true
                                                    otpFeedback = err.message ?: "Invalid verification code."
                                                }
                                            )
                                        }
                                    },
                                    enabled = !otpBusy && otpCode.length == 6,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = ShapeDefaults.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (otpBusy) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Verify & Sign In", fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = {
                                            otpSent = false
                                            otpCode = ""
                                            otpFeedback = null
                                            otpIsError = false
                                        },
                                        enabled = !otpBusy
                                    ) {
                                        Text("Change Email")
                                    }
                                    TextButton(
                                        onClick = {
                                            otpBusy = true
                                            otpFeedback = null
                                            otpIsError = false
                                            onRequestCloudOtp(otpEmail.trim()) { result ->
                                                otpBusy = false
                                                result.fold(
                                                    onSuccess = {
                                                        otpIsError = false
                                                        otpFeedback = "New 6-digit code sent to ${otpEmail.trim()}."
                                                    },
                                                    onFailure = { err ->
                                                        otpIsError = true
                                                        otpFeedback = err.message ?: "Failed to resend code."
                                                    }
                                                )
                                            }
                                        },
                                        enabled = !otpBusy
                                    ) {
                                        Text("Resend Code")
                                    }
                                }
                            }

                            otpFeedback?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (otpIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Text(
                                text = "Your Android phone hosts your Minecraft servers 100% free forever (with Bore, Playit.gg, UPnP, and local/WebDAV backups). MineServe Cloud is an optional add-on (not 24/7 cloud hosting) that adds a custom *.play.mineserve.app address, remote browser Web Admin from anywhere, and encrypted R2 backups. Try Plus free for 7 days.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "How linking works:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "1. Sign in or pick a plan in the Cloud Portal.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "2. Tap 'Open MineServe App' in your browser to link this phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onOpenCloudPortal,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = ShapeDefaults.Medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In / Link Account", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = profile?.email ?: "Account linked",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (profile != null && profile.storageQuotaBytes > 0L) {
                            val usedMb = profile.storageUsedBytes / (1024.0 * 1024.0)
                            val quotaGb = profile.storageQuotaBytes / (1024L * 1024L * 1024L)
                            val progress = (profile.storageUsedBytes.toFloat() / profile.storageQuotaBytes.toFloat()).coerceIn(0f, 1f)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MineServeTableMonoText(
                                    text = String.format(Locale.US, "Cloud Backups: %.1f MB / %d GB", usedMb, quotaGb),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                            }
                        }

                        if (profile != null && profile.activeSubdomains.isNotEmpty()) {
                            Text(
                                text = "Active Addresses",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MineServeTableContainer {
                                profile.activeSubdomains.forEachIndexed { index, sub ->
                                    val info = profile.activeSubdomainDetails.firstOrNull { it.subdomain.equals(sub, ignoreCase = true) }
                                    val dom = info?.domain?.ifBlank { "play.mineserve.app" } ?: "play.mineserve.app"
                                    val reg = info?.region?.let { " ($it)" }.orEmpty()
                                    MineServeTableRow(showDivider = index < profile.activeSubdomains.lastIndex) {
                                        MineServeTableMonoText(
                                            text = "$sub.$dom$reg",
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (!BuildConfig.IS_PLAY_STORE) {
                                Button(
                                    onClick = onOpenCloudPortal,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = ShapeDefaults.Small,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Portal", fontWeight = FontWeight.Bold)
                                }
                            }
                            FilledTonalButton(
                                onClick = { showSignOutConfirm = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = ShapeDefaults.Small,
                                modifier = if (BuildConfig.IS_PLAY_STORE) Modifier.fillMaxWidth() else Modifier
                            ) {
                                Text("Sign Out", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            val isPaidSubscriber = cloudState.isLinked && (profile?.tier == "PLUS" || profile?.tier == "PRO")

            if (isPaidSubscriber) {
                Text(
                    text = "How to Use Cloud Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                CloudSetupGuideCard(
                    title = "Custom Server Domain & Remote Panel",
                    steps = listOf(
                        "1. Open any server and go to the Settings tab -> Public Multiplayer Tunneling.",
                        "2. Tap the MineServe Cloud provider chip.",
                        "3. Pick one of your claimed addresses (or enter a subdomain, choose your closest relay region, and tap Reserve Cloud Address).",
                        "4. Toggle the Public Tunnel switch ON in the server header to go live. If Web Remote Admin is enabled in App Settings, your panel is also live at https://<subdomain>.panel.mineserve.app."
                    )
                )

                CloudSetupGuideCard(
                    title = "Encrypted Cloud Backups",
                    steps = listOf(
                        "1. Open any server and go to the Backups tab -> Cloud Backup Sync.",
                        "2. Turn on Enable Cloud Backup Sync (and optionally Auto-Sync on Backup Creation).",
                        "3. Tap the MineServe Cloud storage provider chip.",
                        "4. Tap Cloud on any local backup to upload, or manage and restore snapshots under MineServe Cloud Snapshots."
                    )
                )
            } else if (BuildConfig.IS_PLAY_STORE) {
                if (cloudState.isLinked) {
                    MineServeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Free Plan Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your account is currently on the Free plan. Custom cloud addresses, remote Web Admin relays, and encrypted cloud backups require an active MineServe Plus or Pro membership. Account upgrades and purchases are not available in this app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val pricing = remember { CloudPricing.current() }
                Text(
                    text = "Plans & Pricing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (pricing.isEstimatedLocal) {
                    Text(
                        text = "Estimated local pricing (${pricing.currencyCode}) - Stripe Checkout confirms your exact local currency.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CloudPlanCard(
                    title = "Free Forever",
                    price = "${pricing.free} / month",
                    subtitle = "Everything local in MineServe",
                    isCurrent = profile?.tier == "FREE" || !cloudState.isLinked,
                    features = listOf(
                        "Unlimited Java & Bedrock servers",
                        "Built-in Bore, Playit.gg & router UPnP",
                        "Local Wi-Fi Web Admin & Live Map",
                        "Local backups & self-hosted WebDAV"
                    )
                )

                CloudPlanCard(
                    title = "MineServe Plus (7-Day Free Trial)",
                    price = "${pricing.plusMonthly} / mo  (or ${pricing.plusYearly} / yr)",
                    subtitle = "7 days free for new subscribers - cancel anytime",
                    highlighted = true,
                    isCurrent = false,
                    features = listOf(
                        "1 custom *.play.mineserve.app address",
                        "Remote Web Admin & Live Map access",
                        "10 GB encrypted Cloudflare R2 backups",
                        "Multi-region low-latency relays (US, UK, EU, AP)"
                    )
                )

                CloudPlanCard(
                    title = "MineServe Pro",
                    price = "${pricing.proMonthly} / mo  (or ${pricing.proYearly} / yr)",
                    subtitle = "For communities & creators",
                    isCurrent = false,
                    features = listOf(
                        "Up to 3 custom addresses",
                        "Short *.playms.uk addresses (e.g. smp.playms.uk)",
                        "50 GB encrypted Cloudflare R2 backups",
                        "Custom domain CNAME / SRV support"
                    )
                )

                FilledTonalButton(
                    onClick = onOpenCloudPortal,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = ShapeDefaults.Medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start 7-Day Free Trial in Browser Portal", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showSignOutConfirm) {
        MineServeDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = "Sign Out of MineServe Cloud?",
            confirmLabel = "Sign Out",
            onConfirm = {
                showSignOutConfirm = false
                onSignOutCloud()
            },
            icon = Icons.Rounded.Logout,
            isDestructive = true
        ) {
            Text(
                text = "This unlinks your device from MineServe Cloud. Local servers and backups are not affected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CloudSetupGuideCard(
    title: String,
    steps: List<String>
) {
    MineServeCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            steps.forEach { step ->
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CloudPlanCard(
    title: String,
    price: String,
    subtitle: String,
    features: List<String>,
    highlighted: Boolean = false,
    isCurrent: Boolean = false
) {
    MineServeCard(
        containerColor = if (highlighted) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCurrent) {
                    Surface(
                        shape = ShapeDefaults.Small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "CURRENT",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                features.forEach { feat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = feat,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun MineServeCloudLinkedPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        MineServeCloudScreen(
            cloudState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleCloudLinkedState,
            onBack = {},
            onOpenCloudPortal = {},
            onRefreshCloud = {},
            onSignOutCloud = {}
        )
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun MineServeCloudUnlinkedPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        MineServeCloudScreen(
            cloudState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleCloudUnlinkedState,
            onBack = {},
            onOpenCloudPortal = {},
            onRefreshCloud = {},
            onSignOutCloud = {}
        )
    }
}
