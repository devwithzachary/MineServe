package com.devwithzachary.mineserve.ui.screens.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder

@Composable
fun WizardStep1Identity(
    serverName: String,
    onServerNameChange: (String) -> Unit,
    motd: String,
    onMotdChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.wizard_step1_name_label),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        OutlinedTextField(
            value = serverName,
            onValueChange = onServerNameChange,
            label = { Text(stringResource(R.string.wizard_step1_name_label)) },
            placeholder = { Text(stringResource(R.string.wizard_step1_name_placeholder)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedLabelColor = EmeraldPrimary,
                unfocusedContainerColor = ObsidianCard,
                focusedContainerColor = ObsidianCard
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = motd,
            onValueChange = onMotdChange,
            label = { Text(stringResource(R.string.wizard_step1_motd_label)) },
            placeholder = { Text(stringResource(R.string.wizard_step1_motd_placeholder)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = ObsidianCardBorder,
                focusedLabelColor = EmeraldPrimary,
                unfocusedContainerColor = ObsidianCard,
                focusedContainerColor = ObsidianCard
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
