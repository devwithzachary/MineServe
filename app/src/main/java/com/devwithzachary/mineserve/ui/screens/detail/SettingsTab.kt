package com.devwithzachary.mineserve.ui.screens.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.RedstoneLight
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SettingsEditorMode {
    VISUAL,
    RAW_FILE
}

@Composable
fun SettingsTab(
    initialProperties: ServerProperties,
    onSaveProperties: (ServerProperties) -> Unit,
    onReadRawConfigFile: suspend (String) -> String = { "" },
    onSaveRawConfigFile: suspend (String, String) -> Boolean = { _, _ -> true },
    onListConfigFiles: suspend () -> List<String> = { listOf("server.properties") },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var editorMode by remember { mutableStateOf(SettingsEditorMode.VISUAL) }

    // Visual editor state
    var props by remember(initialProperties) { mutableStateOf(initialProperties) }
    var motd by remember(props.motd) { mutableStateOf(props.motd) }
    var gamemode by remember(props.gamemode) { mutableStateOf(props.gamemode) }
    var difficulty by remember(props.difficulty) { mutableStateOf(props.difficulty) }
    var pvp by remember(props.pvp) { mutableStateOf(props.pvp) }
    var hardcore by remember(props.hardcore) { mutableStateOf(props.hardcore) }
    var onlineMode by remember(props.onlineMode) { mutableStateOf(props.onlineMode) }
    var whitelist by remember(props.whiteList) { mutableStateOf(props.whiteList) }
    var allowFlight by remember(props.allowFlight) { mutableStateOf(props.allowFlight) }
    var allowNether by remember(props.allowNether) { mutableStateOf(props.allowNether) }
    var maxPlayers by remember(props.maxPlayers) { mutableIntStateOf(props.maxPlayers) }
    var viewDistance by remember(props.viewDistance) { mutableIntStateOf(props.viewDistance) }
    var simulationDistance by remember(props.simulationDistance) { mutableIntStateOf(props.simulationDistance) }
    var levelSeed by remember(props.levelSeed) { mutableStateOf(props.levelSeed) }
    var visualSavedMessage by remember { mutableStateOf<String?>(null) }

    // Raw File editor state
    var configFiles by remember { mutableStateOf(listOf("server.properties")) }
    var selectedFile by remember { mutableStateOf("server.properties") }
    var rawFileContent by remember { mutableStateOf("") }
    var isFileLoading by remember { mutableStateOf(false) }
    var isFileSaving by remember { mutableStateOf(false) }
    var fileSaveStatus by remember { mutableStateOf<String?>(null) }

    // Load available config files and content when switching to Raw File mode
    LaunchedEffect(editorMode) {
        if (editorMode == SettingsEditorMode.RAW_FILE) {
            val list = onListConfigFiles()
            if (list.isNotEmpty()) {
                configFiles = list
                if (!list.contains(selectedFile)) {
                    selectedFile = list.first()
                }
            }
            isFileLoading = true
            rawFileContent = onReadRawConfigFile(selectedFile)
            isFileLoading = false
        }
    }

    // Load file content when switching selectedFile
    LaunchedEffect(selectedFile) {
        if (editorMode == SettingsEditorMode.RAW_FILE) {
            isFileLoading = true
            rawFileContent = onReadRawConfigFile(selectedFile)
            isFileLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Switcher Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = editorMode == SettingsEditorMode.VISUAL,
                    onClick = { editorMode = SettingsEditorMode.VISUAL },
                    label = { Text("Visual Settings") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black,
                        containerColor = Slate800,
                        labelColor = Color.White
                    )
                )

                FilterChip(
                    selected = editorMode == SettingsEditorMode.RAW_FILE,
                    onClick = { editorMode = SettingsEditorMode.RAW_FILE },
                    label = { Text("Raw Config File") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black,
                        containerColor = Slate800,
                        labelColor = Color.White
                    )
                )
            }
        }

        if (editorMode == SettingsEditorMode.VISUAL) {
            // Visual Config Editor
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_tab_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = {
                            val updated = props.copy(
                                motd = motd,
                                gamemode = gamemode,
                                difficulty = difficulty,
                                pvp = pvp,
                                hardcore = hardcore,
                                onlineMode = onlineMode,
                                whiteList = whitelist,
                                allowFlight = allowFlight,
                                allowNether = allowNether,
                                maxPlayers = maxPlayers,
                                viewDistance = viewDistance,
                                simulationDistance = simulationDistance,
                                levelSeed = levelSeed
                            )
                            onSaveProperties(updated)
                            visualSavedMessage = "Saved properties successfully!"
                            scope.launch {
                                delay(2500)
                                visualSavedMessage = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_tab_save),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (visualSavedMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldDark.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                            Text(visualSavedMessage ?: "", color = EmeraldLight, fontSize = 13.sp)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = motd,
                            onValueChange = { motd = it },
                            label = { Text(stringResource(R.string.wizard_step1_motd_label)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = levelSeed,
                            onValueChange = { levelSeed = it },
                            label = { Text(stringResource(R.string.settings_level_seed)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ObsidianCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = maxPlayers.toString(),
                                onValueChange = { maxPlayers = it.toIntOrNull() ?: 20 },
                                label = { Text(stringResource(R.string.settings_max_players)) },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = viewDistance.toString(),
                                onValueChange = { viewDistance = it.toIntOrNull() ?: 10 },
                                label = { Text(stringResource(R.string.settings_view_distance)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SettingSwitchRow(
                            title = stringResource(R.string.settings_pvp),
                            checked = pvp,
                            onCheckedChange = { pvp = it }
                        )
                        SettingSwitchRow(
                            title = "Hardcore Mode",
                            checked = hardcore,
                            onCheckedChange = { hardcore = it }
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_online_mode),
                            checked = onlineMode,
                            onCheckedChange = { onlineMode = it }
                        )
                        SettingSwitchRow(
                            title = "Enforce Whitelist",
                            checked = whitelist,
                            onCheckedChange = { whitelist = it }
                        )
                        SettingSwitchRow(
                            title = stringResource(R.string.settings_allow_flight),
                            checked = allowFlight,
                            onCheckedChange = { allowFlight = it }
                        )
                        SettingSwitchRow(
                            title = "Allow Nether Dimension",
                            checked = allowNether,
                            onCheckedChange = { allowNether = it }
                        )
                    }
                }
            }
        } else {
            // Raw Config File Document Editor
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // File Selector Row & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(configFiles) { fileName ->
                            FilterChip(
                                selected = selectedFile == fileName,
                                onClick = { selectedFile = fileName },
                                label = { Text(fileName, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.Black,
                                    selectedLeadingIconColor = Color.Black,
                                    containerColor = Slate800,
                                    labelColor = Slate400
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            scope.launch {
                                isFileLoading = true
                                rawFileContent = onReadRawConfigFile(selectedFile)
                                isFileLoading = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload File", tint = Slate400)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isFileSaving = true
                                val success = onSaveRawConfigFile(selectedFile, rawFileContent)
                                isFileSaving = false
                                fileSaveStatus = if (success) "Saved $selectedFile" else "Failed to save"
                                delay(2500)
                                fileSaveStatus = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isFileSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (fileSaveStatus != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (fileSaveStatus?.contains("Failed") == true) RedstoneLight.copy(alpha = 0.2f) else EmeraldDark.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (fileSaveStatus?.contains("Failed") == true) RedstoneLight else EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (fileSaveStatus?.contains("Failed") == true) Icons.Default.Description else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (fileSaveStatus?.contains("Failed") == true) RedstoneLight else EmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = fileSaveStatus ?: "",
                                color = if (fileSaveStatus?.contains("Failed") == true) RedstoneLight else EmeraldLight,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Monospace Code Document Editor
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate950),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isFileLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        }
                    } else {
                        OutlinedTextField(
                            value = rawFileContent,
                            onValueChange = { rawFileContent = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 18.sp
                            ),
                            placeholder = {
                                Text("Empty file content...", fontFamily = FontFamily.Monospace, color = Slate400)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate950,
                                unfocusedContainerColor = Slate950,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldPrimary,
                checkedTrackColor = EmeraldDark,
                uncheckedTrackColor = Slate800
            )
        )
    }
}
