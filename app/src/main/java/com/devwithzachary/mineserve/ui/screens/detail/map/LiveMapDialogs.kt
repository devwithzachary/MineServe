package com.devwithzachary.mineserve.ui.screens.detail.map

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.ui.components.AppAlertDialog
import com.devwithzachary.mineserve.ui.theme.DiamondCyan
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate700
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

@Composable
fun LiveMapPortDialog(
    initialPort: Int,
    onDismiss: () -> Unit,
    onSavePort: (Int) -> Unit
) {
    var portInputText by remember { mutableStateOf(initialPort.toString()) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = DiamondCyan,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.map_change_port_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.map_change_port_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                OutlinedTextField(
                    value = portInputText,
                    onValueChange = { portInputText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.map_port_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = DiamondCyan,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = portInputText.toIntOrNull() ?: 8080
                    val safePort = parsed.coerceIn(1024, 65535)
                    onSavePort(safePort)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DiamondCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.save), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        },
        containerColor = Slate950
    )
}

@Composable
fun LiveMapUninstallDialog(
    onDismiss: () -> Unit,
    onConfirmUninstall: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = RedstoneRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.map_uninstall_dialog_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = stringResource(R.string.map_uninstall_dialog_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Slate400
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmUninstall()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedstoneRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.players_remove), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        },
        containerColor = Slate950
    )
}

@Composable
fun LiveMapFullRenderDialog(
    onDismiss: () -> Unit,
    onSendCommand: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedDimension by remember { mutableStateOf(DimensionType.OVERWORLD) }
    var worldInputText by remember(selectedDimension) {
        mutableStateOf(getRenderWorldIdentifier(selectedDimension))
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.map_render_dialog_title),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Performance Warning Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldYellow.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, GoldYellow.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GoldYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.map_render_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Dimension Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.map_render_dimension_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate400
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
                            val isSelected = selectedDimension == dim
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else Slate900,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) EmeraldPrimary else Slate800
                                ),
                                onClick = {
                                    selectedDimension = dim
                                    worldInputText = getRenderWorldIdentifier(dim)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) EmeraldLight else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // World Name TextField
                OutlinedTextField(
                    value = worldInputText,
                    onValueChange = { worldInputText = it },
                    label = { Text(stringResource(R.string.map_render_world_name_label)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Cancel Active Render Option
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
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = RedstoneLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.map_render_cancel_btn),
                            color = RedstoneLight,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = normalizeWorldIdentifier(worldInputText, selectedDimension)
                    onSendCommand("squaremap fullrender $target")
                    Toast.makeText(context, R.string.map_render_dispatched_toast, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.map_render_start_btn),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.cancel), color = Color.White)
            }
        },
        containerColor = Slate950
    )
}
