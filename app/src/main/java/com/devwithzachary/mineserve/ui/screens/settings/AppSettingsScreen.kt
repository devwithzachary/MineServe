package com.devwithzachary.mineserve.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.ui.components.NotificationPermissionCard
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    storageUsedMb: Long,
    isJavaVersionInstalled: (Int) -> Boolean,
    onInstallJava: (Int) -> Unit,
    onReinstallRuntime: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onAboutClick: (() -> Unit)? = null
) {
    var keepScreenAwake by remember { mutableStateOf(true) }
    var installingJavaVer by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notification Permission Card
            NotificationPermissionCard()

            // Container Runtime Card
            ContainerRuntimeCard(
                storageUsedMb = storageUsedMb,
                onReinstallRuntime = onReinstallRuntime
            )

            // Java Environment Card
            JavaEnvironmentCard(
                isJavaVersionInstalled = isJavaVersionInstalled,
                installingJavaVer = installingJavaVer,
                onInstallJava = { ver ->
                    installingJavaVer = ver
                    onInstallJava(ver)
                }
            )

            // Behavior Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_behavior_section),
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_keep_awake_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.settings_keep_awake_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            Switch(
                                checked = keepScreenAwake,
                                onCheckedChange = { keepScreenAwake = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = EmeraldDark
                                )
                            )
                        }
                    }
                }
            }

            // About MineServe Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_about_section),
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Version ${com.devwithzachary.mineserve.BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            if (onAboutClick != null) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = onAboutClick,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        contentColor = EmeraldLight
                                    ),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                                ) {
                                    Text("About & Changelog", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
