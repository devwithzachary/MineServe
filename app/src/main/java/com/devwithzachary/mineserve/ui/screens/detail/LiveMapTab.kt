package com.devwithzachary.mineserve.ui.screens.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.model.WebMapState
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveMapTab(
    server: MinecraftServer,
    status: ServerStatus,
    onGetWebMapState: () -> WebMapState,
    onSetWebMapPort: (Int) -> Unit,
    onInstallWebMapPlugin: (WebMapPluginType, (Boolean) -> Unit) -> Unit,
    onUninstallWebMapPlugin: () -> Boolean,
    onStartServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mapState by remember { mutableStateOf(onGetWebMapState()) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewLoading by remember { mutableStateOf(true) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var isInstallingSquaremap by remember { mutableStateOf(false) }
    var showPortDialog by remember { mutableStateOf(false) }
    var showUninstallDialog by remember { mutableStateOf(false) }
    var portInputText by remember { mutableStateOf(mapState.port.toString()) }

    fun refreshState() {
        mapState = onGetWebMapState()
        portInputText = mapState.port.toString()
    }

    LaunchedEffect(status, server.id) {
        refreshState()
    }

    val isRunning = status == ServerStatus.RUNNING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Top Toolbar
        Surface(
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plugin & Port Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mapState.installedPlugin != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldDark.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Squaremap (2D)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Port clicker
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate800,
                            border = BorderStroke(1.dp, Slate700),
                            onClick = { showPortDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ":${mapState.port}",
                                    color = DiamondLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Edit Port",
                                    tint = Slate400,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Live World Map",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (mapState.installedPlugin != null) {
                        if (isRunning) {
                            IconButton(
                                onClick = {
                                    webViewError = null
                                    isWebViewLoading = true
                                    webViewRef?.reload()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapState.webMapUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Open in Browser",
                                    tint = Slate400,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Uninstall button
                        IconButton(
                            onClick = { showUninstallDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Uninstall Squaremap",
                                tint = RedstoneLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                // Scenario 1: No map plugin installed
                mapState.installedPlugin == null -> {
                    SquaremapInstallView(
                        isInstalling = isInstallingSquaremap,
                        onInstall = {
                            isInstallingSquaremap = true
                            onInstallWebMapPlugin(WebMapPluginType.SQUAREMAP) { ok ->
                                isInstallingSquaremap = false
                                refreshState()
                                if (ok) {
                                    Toast.makeText(
                                        context,
                                        "Squaremap installed! Start your server to launch the live map.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(context, "Failed to download Squaremap.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                // Scenario 2: Server is stopped
                !isRunning -> {
                    ServerStoppedMapView(
                        port = mapState.port,
                        pluginName = "Squaremap",
                        onStartServer = onStartServer
                    )
                }

                // Scenario 3: Server running and plugin installed -> Embedded WebView
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false

                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isWebViewLoading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            if (request?.isForMainFrame == true) {
                                                isWebViewLoading = false
                                                webViewError = error?.description?.toString() ?: "Cannot connect to web map"
                                            }
                                        }
                                    }

                                    loadUrl(mapState.webMapUrl)
                                    webViewRef = this
                                }
                            },
                            update = { webView ->
                                if (webView.url != mapState.webMapUrl) {
                                    isWebViewLoading = true
                                    webViewError = null
                                    webView.loadUrl(mapState.webMapUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Loading progress indicator
                        if (isWebViewLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter),
                                color = EmeraldPrimary,
                                trackColor = Color.Transparent
                            )
                        }

                        // Error / Initializing notice
                        if (webViewError != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate900.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, Slate700),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = GoldYellow,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Connecting to Squaremap...",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "The Minecraft web map server may still be initializing tiles on port ${mapState.port}. Please wait a moment and tap Retry.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400,
                                        fontSize = 12.sp
                                    )
                                    Button(
                                        onClick = {
                                            webViewError = null
                                            isWebViewLoading = true
                                            webViewRef?.loadUrl(mapState.webMapUrl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Connection", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Port Configuration Dialog
    if (showPortDialog) {
        AlertDialog(
            onDismissRequest = { showPortDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = DiamondCyan,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.map_change_port_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.map_change_port_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                    OutlinedTextField(
                        value = portInputText,
                        onValueChange = { portInputText = it.filter { char -> char.isDigit() } },
                        label = { Text("Web Map Port") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = DiamondCyan,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = portInputText.toIntOrNull() ?: mapState.port
                        if (parsed in 1..65535) {
                            onSetWebMapPort(parsed)
                            refreshState()
                            showPortDialog = false
                            webViewRef?.loadUrl("http://127.0.0.1:$parsed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Port", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPortDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            },
            containerColor = Slate950
        )
    }

    // Modal: Uninstall Confirmation Dialog
    if (showUninstallDialog) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = RedstoneRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.map_uninstall_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.map_uninstall_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = onUninstallWebMapPlugin()
                        showUninstallDialog = false
                        refreshState()
                        if (ok) {
                            Toast.makeText(context, "Squaremap uninstalled.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to uninstall Squaremap.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Uninstall", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showUninstallDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.cancel), color = Color.White)
                }
            },
            containerColor = Slate950
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
        }
    }
}

@Composable
private fun SquaremapInstallView(
    isInstalling: Boolean,
    onInstall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            tint = DiamondCyan,
            modifier = Modifier.size(48.dp)
        )

        Text(
            text = stringResource(R.string.map_no_plugin_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = stringResource(R.string.map_no_plugin_desc),
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
            fontSize = 12.sp
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Squaremap",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = EmeraldDark.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Recommended",
                            color = EmeraldLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Lightweight, ultra-fast 2D web map optimized for mobile devices with minimal RAM and CPU overhead. Features real-time player tracking, multi-world support, and Leaflet web rendering.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 12.sp
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Default HTTP Port: 8080 • Form factor: 2D Interactive Leaflet",
                        color = DiamondLight,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Button(
                    onClick = onInstall,
                    enabled = !isInstalling,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.map_btn_install_squaremap),
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerStoppedMapView(
    port: Int,
    pluginName: String,
    onStartServer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianCard,
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = DiamondCyan,
                    modifier = Modifier.size(40.dp)
                )

                Text(
                    text = "$pluginName is Installed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = stringResource(R.string.map_offline_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 12.sp
                )

                Button(
                    onClick = onStartServer,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.map_btn_start_server),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
