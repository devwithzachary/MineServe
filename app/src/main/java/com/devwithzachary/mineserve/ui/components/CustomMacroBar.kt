package com.devwithzachary.mineserve.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.ConsoleMacro
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneRed
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import java.util.UUID

@Composable
fun CustomMacroBar(
    macros: List<ConsoleMacro>,
    onMacroClicked: (ConsoleMacro) -> Unit,
    onSaveMacros: (List<ConsoleMacro>) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomizeDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Customize Button
        SuggestionChip(
            onClick = { showCustomizeDialog = true },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Customize Macros",
                        modifier = Modifier.size(14.dp),
                        tint = EmeraldPrimary
                    )
                    Text(
                        text = "Macros",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Slate900,
                labelColor = EmeraldPrimary
            ),
            border = SuggestionChipDefaults.suggestionChipBorder(
                enabled = true,
                borderColor = EmeraldPrimary.copy(alpha = 0.5f)
            )
        )

        for (macro in macros) {
            SuggestionChip(
                onClick = { onMacroClicked(macro) },
                label = {
                    Text(
                        text = macro.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Slate800,
                    labelColor = Color.White
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = ObsidianCardBorder
                )
            )
        }
    }

    if (showCustomizeDialog) {
        CustomizeMacrosDialog(
            currentMacros = macros,
            onDismiss = { showCustomizeDialog = false },
            onSave = { updated ->
                onSaveMacros(updated)
            },
            onResetDefaults = onResetDefaults
        )
    }
}

@Composable
fun CustomizeMacrosDialog(
    currentMacros: List<ConsoleMacro>,
    onDismiss: () -> Unit,
    onSave: (List<ConsoleMacro>) -> Unit,
    onResetDefaults: () -> Unit
) {
    var macros by remember { mutableStateOf(currentMacros) }
    var newLabel by remember { mutableStateOf("") }
    var newCommand by remember { mutableStateOf("") }
    var newAutoExecute by remember { mutableStateOf(true) }
    var showAddForm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Macro Bar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(
                    onClick = {
                        onResetDefaults()
                        onDismiss()
                    }
                ) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset Defaults",
                        modifier = Modifier.size(16.dp),
                        tint = Slate400
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Reset", color = Slate400, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Quick shortcuts above the console terminal for 1-tap commands:",
                    color = Slate400,
                    fontSize = 12.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(macros, key = { it.id }) { macro ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate900),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = macro.label,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = macro.command + if (macro.autoExecute) " (auto-run)" else "",
                                        color = Slate400,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val updated = macros.filter { it.id != macro.id }
                                        macros = updated
                                        onSave(updated)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Macro",
                                        tint = RedstoneRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!showAddForm) {
                    Button(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Custom Macro", color = Color.White)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate950),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newLabel,
                                onValueChange = { newLabel = it },
                                label = { Text("Label (e.g. Day)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newCommand,
                                onValueChange = { newCommand = it },
                                label = { Text("Command (e.g. /time set day)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = ObsidianCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Execute immediately",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Switch(
                                    checked = newAutoExecute,
                                    onCheckedChange = { newAutoExecute = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = EmeraldPrimary,
                                        checkedTrackColor = EmeraldPrimary.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddForm = false }) {
                                    Text(text = "Cancel", color = Slate400)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newLabel.isNotBlank() && newCommand.isNotBlank()) {
                                            val created = ConsoleMacro(
                                                id = UUID.randomUUID().toString().take(8),
                                                label = newLabel.trim(),
                                                command = newCommand.trim(),
                                                autoExecute = newAutoExecute
                                            )
                                            val updated = macros + created
                                            macros = updated
                                            onSave(updated)
                                            newLabel = ""
                                            newCommand = ""
                                            showAddForm = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "Add", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = ObsidianCard,
        shape = RoundedCornerShape(16.dp)
    )
}
