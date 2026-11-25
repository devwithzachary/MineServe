package com.devwithzachary.mineserve.ui.components.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily

@Composable
fun AddWhitelistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.players_add_to_whitelist),
        confirmLabel = stringResource(R.string.players_dialog_add_btn),
        onConfirm = {
            if (username.isNotBlank()) {
                onConfirm(username.trim())
            }
        },
        dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
        confirmEnabled = username.isNotBlank()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.filterNot { c -> c.isWhitespace() } },
                label = { Text(stringResource(R.string.players_dialog_username_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AddOpDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var levelSlider by remember { mutableFloatStateOf(4f) }

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.players_add_operator),
        confirmLabel = stringResource(R.string.players_dialog_add_btn),
        onConfirm = {
            if (username.isNotBlank()) {
                onConfirm(username.trim(), levelSlider.toInt())
            }
        },
        dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
        confirmEnabled = username.isNotBlank()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.filterNot { c -> c.isWhitespace() } },
                label = { Text(stringResource(R.string.players_dialog_username_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${stringResource(R.string.players_dialog_op_level_label)}: ${levelSlider.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Slider(
                value = levelSlider,
                onValueChange = { levelSlider = it },
                valueRange = 1f..4f,
                steps = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BanPlayerDialog(
    initialUsername: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var reason by remember { mutableStateOf("") }

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.players_ban_player),
        confirmLabel = stringResource(R.string.players_ban),
        onConfirm = {
            if (username.isNotBlank()) {
                onConfirm(username.trim(), reason.trim().ifBlank { "Banned by an operator." })
            }
        },
        dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
        isDestructive = true,
        confirmEnabled = username.isNotBlank()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.filterNot { c -> c.isWhitespace() } },
                label = { Text(stringResource(R.string.players_dialog_username_label)) },
                singleLine = true,
                enabled = initialUsername.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(R.string.players_dialog_reason_label)) },
                placeholder = { Text(stringResource(R.string.player_action_ban_default_reason)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BanIpDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val monoFont = LocalMineServeMonoFontFamily.current

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.players_ban_ip),
        confirmLabel = stringResource(R.string.players_ban),
        onConfirm = {
            if (ip.isNotBlank()) {
                onConfirm(ip.trim(), reason.trim().ifBlank { "Banned by an operator." })
            }
        },
        dismissLabel = stringResource(R.string.players_dialog_cancel_btn),
        isDestructive = true,
        confirmEnabled = ip.isNotBlank()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it.filterNot { c -> c.isWhitespace() } },
                label = { Text(stringResource(R.string.players_dialog_ip_label)) },
                placeholder = { Text("192.168.1.1", fontFamily = monoFont) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(stringResource(R.string.players_dialog_reason_label)) },
                placeholder = { Text(stringResource(R.string.player_action_ban_default_reason)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
