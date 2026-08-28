package com.devwithzachary.mineserve.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.tunnel.TunnelState
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.DiamondLight
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

@Composable
fun ServerShareDialog(
    server: MinecraftServer,
    tunnelState: TunnelState,
    localIp: String,
    onDismiss: () -> Unit,
    onEnableTunnel: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(if (tunnelState is TunnelState.Connected) 0 else 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldDark.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Share Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${server.name} • ${server.type.displayName} ${server.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate900,
                    contentColor = EmeraldPrimary,
                    indicator = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Public Online", fontWeight = FontWeight.Bold)
                            }
                        },
                        selectedContentColor = EmeraldLight,
                        unselectedContentColor = Slate400
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Local Wi-Fi", fontWeight = FontWeight.Bold)
                            }
                        },
                        selectedContentColor = EmeraldLight,
                        unselectedContentColor = Slate400
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Public Online Tunnel View
                        when (tunnelState) {
                            is TunnelState.Connected -> {
                                val host = tunnelState.publicHost
                                val port = tunnelState.publicPort
                                val fullAddress = tunnelState.fullAddress
                                val bedrockDeepLink = QrCodeHelper.buildBedrockDeepLink(server.name, host, port)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QrCodeView(
                                        content = bedrockDeepLink,
                                        sizeDp = 180.dp
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Slate900,
                                        border = BorderStroke(1.dp, Slate800),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "PUBLIC INTERNET ADDRESS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldLight,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = fullAddress,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Security Notice
                                    TunnelSecurityWarningCard()

                                    // Action Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bedrockDeepLink)).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Minecraft Bedrock is not installed", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Join Bedrock", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(fullAddress))
                                                Toast.makeText(context, "Copied $fullAddress", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy IP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val shareText = QrCodeHelper.buildInviteText(
                                                serverName = server.name,
                                                serverType = server.type.displayName,
                                                version = server.version,
                                                host = host,
                                                port = port,
                                                isTunnel = true
                                            )
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Minecraft Server"))
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Slate800),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share Full Invite", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            }
                            is TunnelState.Connecting -> {
                                val claimUrl = tunnelState.claimUrl
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (claimUrl != null) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = DiamondLight,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Playit.gg Account Setup Required",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Click the button below to link and claim your tunnel in your browser.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(claimUrl))
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Claim Tunnel on Playit.gg", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(36.dp))
                                        Text(
                                            text = tunnelState.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = GoldYellow,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            else -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Public Online Link is currently offline.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate400,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Enable the tunnel to allow remote friends on Cellular (4G/5G) or outside your home Wi-Fi to join without port forwarding.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400
                                    )
                                    Button(
                                        onClick = onEnableTunnel,
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enable Public Tunnel", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Local Wi-Fi LAN View
                        val host = localIp
                        val port = server.port
                        val fullAddress = "$host:$port"
                        val bedrockDeepLink = QrCodeHelper.buildBedrockDeepLink(server.name, host, port)

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QrCodeView(
                                content = bedrockDeepLink,
                                sizeDp = 180.dp
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate900,
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "LOCAL WI-FI LAN ADDRESS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = fullAddress,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bedrockDeepLink)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Minecraft Bedrock is not installed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Join Bedrock", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(fullAddress))
                                        Toast.makeText(context, "Copied $fullAddress", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy IP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val shareText = QrCodeHelper.buildInviteText(
                                        serverName = server.name,
                                        serverType = server.type.displayName,
                                        version = server.version,
                                        host = host,
                                        port = port,
                                        isTunnel = false
                                    )
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Minecraft Server"))
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Full Invite", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
