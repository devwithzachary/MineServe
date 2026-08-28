package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.api.GitHubRelease
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900

@Composable
fun GitHubUpdateDialog(
    release: GitHubRelease,
    currentVersion: String,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    onDisableFuturePrompts: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = ObsidianCard,
        tonalElevation = 8.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldDark.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "New Update Available!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = release.tagName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Installed: v$currentVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Release Highlights Summary (if available)
                if (release.body.isNotBlank()) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900),
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "RELEASE NOTES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = release.body.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Important Play Store Migration Advisory Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldYellow.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, GoldYellow.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = GoldYellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Migrating from Google Play Store?",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldYellow
                            )
                        }

                        Text(
                            text = "Google Play and direct GitHub releases use different Android signing keys. Android will prevent updating in-place over the Play Store build.\n\n" +
                                    "1. Back up your servers in Server Details ➔ Backups.\n" +
                                    "2. Uninstall the Play Store version.\n" +
                                    "3. Install the GitHub APK (future GitHub updates will install seamlessly without uninstalling).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val targetUrl = release.apkDownloadUrl ?: release.htmlUrl
                        onDownload(targetUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (release.apkDownloadUrl != null) Icons.Default.Download else Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (release.apkDownloadUrl != null) "Download APK from GitHub" else "Open Release on GitHub",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDisableFuturePrompts
                    ) {
                        Text(
                            text = "Don't prompt again",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Text(
                            text = "Later",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        dismissButton = null
    )
}
