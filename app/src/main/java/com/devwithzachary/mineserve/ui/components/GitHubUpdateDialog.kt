package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.api.GitHubRelease
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

@Composable
fun GitHubUpdateDialog(
    release: GitHubRelease,
    currentVersion: String,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    onDisableFuturePrompts: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extended = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        icon = {
            Surface(
                shape = ShapeDefaults.Medium,
                color = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
                border = null
            ) {
                Icon(
                    imageVector = Icons.Rounded.NewReleases,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(26.dp)
                )
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "New Update Available!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = ShapeDefaults.ExtraSmall,
                        color = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        border = null
                    ) {
                        Text(
                            text = release.tagName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = monoFont,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "Installed: v$currentVersion",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                        color = colorScheme.onSurfaceVariant
                    )
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
                if (release.body.isNotBlank()) {
                    MineServeCard(
                        shape = ShapeDefaults.Medium,
                        containerColor = colorScheme.surfaceContainer,
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 140.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Release Notes",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = release.body.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = ShapeDefaults.Medium,
                    color = extended.warningContainer,
                    contentColor = extended.onWarningContainer,
                    border = null,
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
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = extended.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Migrating from Google Play Store?",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Text(
                            text = "Google Play and direct GitHub releases use different Android signing keys. Android will prevent updating in-place over the Play Store build.\n\n" +
                                "1. Back up your servers in Server Details -> Backups.\n" +
                                "2. Uninstall the Play Store version.\n" +
                                "3. Install the GitHub APK (future GitHub updates will install seamlessly without uninstalling).",
                            style = MaterialTheme.typography.bodySmall
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    shape = ShapeDefaults.Medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (release.apkDownloadUrl != null) Icons.Rounded.Download else Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (release.apkDownloadUrl != null) "Download APK from GitHub" else "Open Release on GitHub"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDisableFuturePrompts) {
                        Text(
                            text = "Don't prompt again",
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Later",
                            color = colorScheme.onSurface
                        )
                    }
                }
            }
        },
        dismissButton = null
    )
}
