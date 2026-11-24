package com.devwithzachary.mineserve.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import kotlinx.coroutines.launch

/**
 * Compact M3 tonal chip for server addresses. Uses Diamond tertiaryContainer (#0E4F5C)
 * for active public tunnels and surfaceContainerHigh for local LAN IPs, rendered in
 * JetBrains Mono with one-tap copy and optional snackbar confirmation.
 */
@Composable
fun NetworkAddressChip(
    address: String,
    isPublicTunnel: Boolean,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    onCopied: (String) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val monoFont = LocalMineServeMonoFontFamily.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val containerColor = if (isPublicTunnel) {
        colorScheme.tertiaryContainer
    } else {
        colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isPublicTunnel) {
        colorScheme.onTertiaryContainer
    } else {
        colorScheme.onSurface
    }
    val leadingIcon = if (isPublicTunnel) Icons.Rounded.Public else Icons.Rounded.Lan

    Surface(
        onClick = {
            clipboard.setText(AnnotatedString(address))
            onCopied(address)
            if (snackbarHostState != null) {
                scope.launch {
                    snackbarHostState.showSnackbar("Copied $address")
                }
            }
        },
        modifier = modifier,
        shape = ShapeDefaults.Small,
        color = containerColor,
        contentColor = contentColor,
        border = null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = if (isPublicTunnel) "Public tunnel address" else "Local LAN address",
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = monoFont,
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy address",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
