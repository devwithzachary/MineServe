package com.devwithzachary.mineserve.ui.screens.detail.map

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.designsystem.components.MineServeDialog
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeColors
import com.devwithzachary.mineserve.designsystem.theme.LocalMineServeMonoFontFamily
import com.devwithzachary.mineserve.model.DimensionType

@Composable
fun LiveMapPortDialog(
    initialPort: Int,
    onDismiss: () -> Unit,
    onSavePort: (Int) -> Unit
) {
    var portInputText by remember { mutableStateOf(initialPort.toString()) }
    val monoFont = LocalMineServeMonoFontFamily.current

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.map_change_port_title),
        confirmLabel = stringResource(R.string.save),
        onConfirm = {
            val parsed = portInputText.toIntOrNull() ?: 8081
            val safePort = parsed.coerceIn(1024, 65535)
            onSavePort(safePort)
            onDismiss()
        },
        dismissLabel = stringResource(R.string.cancel),
        icon = Icons.Rounded.Settings
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.map_change_port_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = portInputText,
                onValueChange = { portInputText = it.filter { ch -> ch.isDigit() } },
                label = { Text(stringResource(R.string.map_port_label)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LiveMapUninstallDialog(
    onDismiss: () -> Unit,
    onConfirmUninstall: () -> Unit
) {
    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.map_uninstall_dialog_title),
        confirmLabel = stringResource(R.string.players_remove),
        onConfirm = {
            onConfirmUninstall()
            onDismiss()
        },
        dismissLabel = stringResource(R.string.cancel),
        icon = Icons.Rounded.Delete,
        isDestructive = true
    ) {
        Text(
            text = stringResource(R.string.map_uninstall_dialog_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LiveMapFullRenderDialog(
    onDismiss: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    val context = LocalContext.current
    val extColors = LocalMineServeColors.current
    val monoFont = LocalMineServeMonoFontFamily.current
    var selectedDimension by remember { mutableStateOf(DimensionType.OVERWORLD) }
    var worldInputText by remember(selectedDimension) {
        mutableStateOf(getRenderWorldIdentifier(selectedDimension))
    }

    MineServeDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.map_render_dialog_title),
        confirmLabel = stringResource(R.string.map_render_start_btn),
        onConfirm = {
            val target = normalizeWorldIdentifier(worldInputText, selectedDimension)
            onSendCommand("squaremap fullrender $target")
            Toast.makeText(context, R.string.map_render_dispatched_toast, Toast.LENGTH_SHORT).show()
            onDismiss()
        },
        dismissLabel = stringResource(R.string.cancel),
        icon = Icons.Rounded.Layers
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = ShapeDefaults.Small,
                color = extColors.warningContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = extColors.onWarningContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.map_render_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = extColors.onWarningContainer
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.map_render_dimension_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        DimensionType.OVERWORLD to "Overworld",
                        DimensionType.NETHER to "Nether",
                        DimensionType.THE_END to "The End"
                    ).forEach { (dim, label) ->
                        FilterChip(
                            selected = selectedDimension == dim,
                            onClick = {
                                selectedDimension = dim
                                worldInputText = getRenderWorldIdentifier(dim)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = worldInputText,
                onValueChange = { worldInputText = it },
                label = { Text(stringResource(R.string.map_render_world_name_label)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = monoFont),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val target = normalizeWorldIdentifier(worldInputText, selectedDimension)
                        onSendCommand("squaremap cancelrender $target")
                        Toast.makeText(context, R.string.map_render_cancelled_toast, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.map_render_cancel_btn),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
