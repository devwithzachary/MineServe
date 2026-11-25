package com.devwithzachary.mineserve.ui.screens.splash

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.assets.MineServeBrandAssets
import com.devwithzachary.mineserve.designsystem.components.MineServeCard
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.engine.RootfsSetupState
import com.devwithzachary.mineserve.ui.components.NotificationPermissionCard
import com.devwithzachary.mineserve.ui.theme.LayoutManager

@Composable
fun SplashScreen(
    isRootfsInstalled: Boolean,
    setupState: RootfsSetupState,
    onStartSetup: () -> Unit,
    onContinueToDashboard: () -> Unit,
    onImportLocalRootfs: (Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current

    val localRootfsPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportLocalRootfs(uri)
        }
    }

    LaunchedEffect(setupState) {
        if (setupState is RootfsSetupState.Success) {
            onContinueToDashboard()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Image(
                    painter = MineServeBrandAssets.brandIconPainter(),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(96.dp)
                        .clip(ShapeDefaults.ExtraLarge)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!isRootfsInstalled && setupState is RootfsSetupState.Idle) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NotificationPermissionCard()

                        MineServeCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = stringResource(R.string.splash_init_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.splash_init_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onStartSetup,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = ShapeDefaults.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.splash_init_btn),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = { localRootfsPicker.launch(arrayOf("application/gzip", "application/x-gzip", "*/*")) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = ShapeDefaults.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Import Local Rootfs (.tar.gz)",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else if (setupState is RootfsSetupState.Downloading || setupState is RootfsSetupState.Extracting) {
                    MineServeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (setupState is RootfsSetupState.Downloading) {
                                Text(
                                    text = stringResource(R.string.splash_setting_up_runtime, setupState.progressPercent),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { setupState.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Downloading Ubuntu base image: ${setupState.bytesDownloaded / (1024 * 1024)} MB / ${setupState.totalBytes / (1024 * 1024)} MB (${setupState.downloadPercent}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = monoFont,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            } else if (setupState is RootfsSetupState.Extracting) {
                                val titleText = setupState.stageTitle?.let { "$it (${setupState.progressPercent}%)" }
                                    ?: stringResource(R.string.splash_extracting_runtime, setupState.progressPercent)
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { setupState.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = setupState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                if (setupState.logs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .background(extColors.terminalSurface, ShapeDefaults.Small)
                                            .padding(10.dp)
                                    ) {
                                        items(setupState.logs) { log ->
                                            Text(
                                                text = log,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = monoFont,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    LaunchedEffect(setupState.logs.size) {
                                        listState.animateScrollToItem((setupState.logs.size - 1).coerceAtLeast(0))
                                    }
                                }
                            }
                        }
                    }
                } else if (setupState is RootfsSetupState.Error) {
                    MineServeCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.splash_setup_error),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = setupState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (setupState.logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(extColors.terminalSurface, ShapeDefaults.Small)
                                        .padding(10.dp)
                                ) {
                                    items(setupState.logs) { log ->
                                        Text(
                                            text = log,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = monoFont,
                                            color = if (log.startsWith("ERROR:") || log.startsWith("E:")) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                extColors.terminalOnSurface
                                            }
                                        )
                                    }
                                }
                                LaunchedEffect(setupState.logs.size) {
                                    listState.animateScrollToItem((setupState.logs.size - 1).coerceAtLeast(0))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onStartSetup,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = ShapeDefaults.Medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.splash_retry_setup),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = { localRootfsPicker.launch(arrayOf("application/gzip", "application/x-gzip", "*/*")) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = ShapeDefaults.Medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Import Local Rootfs (.tar.gz)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isRootfsInstalled || setupState.logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = onContinueToDashboard,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = ShapeDefaults.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.splash_continue_anyway),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.splash_loading_servers),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(600)
                        onContinueToDashboard()
                    }
                }
            }
        }
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun SplashScreenIdlePreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        SplashScreen(
            isRootfsInstalled = false,
            setupState = com.devwithzachary.mineserve.engine.RootfsSetupState.Idle,
            onStartSetup = {},
            onContinueToDashboard = {}
        )
    }
}

@com.devwithzachary.mineserve.ui.preview.MineServeScreenPreviews
@Composable
private fun SplashScreenExtractingPreview() {
    com.devwithzachary.mineserve.ui.preview.MineServePreviewContainer {
        SplashScreen(
            isRootfsInstalled = false,
            setupState = com.devwithzachary.mineserve.ui.preview.MineServePreviewFixtures.sampleRootfsExtractingState,
            onStartSetup = {},
            onContinueToDashboard = {}
        )
    }
}
