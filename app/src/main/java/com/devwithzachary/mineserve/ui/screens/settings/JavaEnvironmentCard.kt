package com.devwithzachary.mineserve.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeSettingsGroup
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

data class JavaRuntimeItem(
    val version: Int,
    val titleRes: Int,
    val subtitle: String
)

@Composable
fun JavaEnvironmentCard(
    isJavaVersionInstalled: (Int) -> Boolean,
    installingJavaVer: Int?,
    onInstallJava: (Int) -> Unit,
    javaInstallError: String? = null,
    modifier: Modifier = Modifier
) {
    val monoFont = LocalMineServeMonoFontFamily.current
    val runtimes = listOf(
        JavaRuntimeItem(25, R.string.settings_java_25_title, "Supports Minecraft 26.2+"),
        JavaRuntimeItem(21, R.string.settings_java_21_title, "Supports Minecraft 1.20.5 - 1.21.11"),
        JavaRuntimeItem(17, R.string.settings_java_17_title, "Supports Minecraft 1.18 - 1.20.4"),
        JavaRuntimeItem(8, R.string.settings_java_8_title, "Supports Minecraft 1.12.2 - 1.16.5")
    )

    MineServeSettingsGroup(
        title = stringResource(R.string.settings_java_section),
        modifier = modifier
    ) {
        runtimes.forEachIndexed { index, item ->
            val isInstalled = isJavaVersionInstalled(item.version)
            val isInstalling = installingJavaVer == item.version

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = monoFont,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = { onInstallJava(item.version) },
                        enabled = !isInstalling,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isInstalled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isInstalled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        ),
                        shape = ShapeDefaults.Small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isInstalling) {
                                "Installing..."
                            } else if (isInstalled) {
                                stringResource(R.string.settings_java_installed_badge)
                            } else {
                                stringResource(R.string.settings_java_install_btn)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (isInstalling) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }

            if (index < runtimes.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }

        if (!javaInstallError.isNullOrBlank()) {
            Text(
                text = javaInstallError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
