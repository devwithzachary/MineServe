package com.devwithzachary.mineserve.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400

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
    modifier: Modifier = Modifier
) {
    val runtimes = listOf(
        JavaRuntimeItem(25, R.string.settings_java_25_title, "Supports Minecraft 26.2+"),
        JavaRuntimeItem(21, R.string.settings_java_21_title, "Supports Minecraft 1.20.5 - 1.21.11"),
        JavaRuntimeItem(17, R.string.settings_java_17_title, "Supports Minecraft 1.18 - 1.20.4"),
        JavaRuntimeItem(8, R.string.settings_java_8_title, "Supports Minecraft 1.12.2 - 1.16.5")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_java_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                runtimes.forEach { item ->
                    val isInstalled = isJavaVersionInstalled(item.version)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(item.titleRes),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (isInstalled) {
                            Button(
                                onClick = { onInstallJava(item.version) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (installingJavaVer == item.version) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black)
                                } else {
                                    Text(
                                        text = stringResource(R.string.settings_java_installed_badge),
                                        color = Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onInstallJava(item.version) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (installingJavaVer == item.version) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp))
                                } else {
                                    Text(
                                        text = stringResource(R.string.settings_java_install_btn),
                                        fontSize = 13.sp
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
