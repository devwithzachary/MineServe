package com.devwithzachary.mineserve.ui.screens.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.ui.components.RamSlider
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.LayoutManager
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder

@Composable
fun WizardStep3Configuration(
    allocatedRamMb: Int,
    onAllocatedRamMbChange: (Int) -> Unit,
    port: Int,
    onPortChange: (Int) -> Unit,
    conflictingServerName: String? = null,
    modifier: Modifier = Modifier
) {
    var portText by remember { mutableStateOf(port.toString()) }

    LaunchedEffect(port) {
        if (portText.toIntOrNull() != port) {
            portText = port.toString()
        }
    }

    val isPortValid = portText.toIntOrNull()?.let { it in 1..65535 } == true

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LayoutManager.cardSpacing)
    ) {
        Text(
            text = stringResource(R.string.wizard_step3_ram_label),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            border = BorderStroke(1.dp, ObsidianCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                RamSlider(
                    allocatedMb = allocatedRamMb,
                    onValueChange = onAllocatedRamMbChange
                )
            }
        }

        OutlinedTextField(
            value = portText,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.take(5)
                portText = digitsOnly
                digitsOnly.toIntOrNull()?.let { parsed ->
                    if (parsed in 1..65535) {
                        onPortChange(parsed)
                    }
                }
            },
            label = { Text(stringResource(R.string.wizard_step3_port_label)) },
            singleLine = true,
            isError = portText.isNotEmpty() && !isPortValid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (conflictingServerName != null) GoldYellow else EmeraldPrimary,
                unfocusedBorderColor = if (conflictingServerName != null) GoldYellow else ObsidianCardBorder,
                focusedLabelColor = if (conflictingServerName != null) GoldYellow else EmeraldPrimary,
                unfocusedContainerColor = ObsidianCard,
                focusedContainerColor = ObsidianCard
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Port Conflict Warning
        AnimatedVisibility(visible = conflictingServerName != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldYellow.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, GoldYellow.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = GoldYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Port in use by '$conflictingServerName'",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldYellow
                        )
                        Text(
                            text = "Port $port is currently configured on '$conflictingServerName'. Only one server can be live and running on this port at one time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
