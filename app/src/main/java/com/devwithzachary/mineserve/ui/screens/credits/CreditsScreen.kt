package com.devwithzachary.mineserve.ui.screens.credits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.GoldYellow
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400
import com.devwithzachary.mineserve.ui.theme.Slate800
import com.devwithzachary.mineserve.ui.theme.Slate900
import com.devwithzachary.mineserve.ui.theme.Slate950

enum class CreditCategory(val label: String) {
    ALL("All"),
    SERVERS("Server Software"),
    SYSTEM("Runtime & Linux"),
    LIBRARIES("APIs & Libraries")
}

data class SoftwareCredit(
    val name: String,
    val category: CreditCategory,
    val description: String,
    val url: String,
    val license: String,
    val tag: String
)

val SOFTWARE_CREDITS: List<SoftwareCredit> = listOf(
    SoftwareCredit(
        name = "PaperMC",
        category = CreditCategory.SERVERS,
        description = "High-performance, secure Minecraft server engine designed for speed and extensive plugin API support.",
        url = "https://papermc.io",
        license = "GPL-3.0",
        tag = "Core Engine"
    ),
    SoftwareCredit(
        name = "PurpurMC",
        category = CreditCategory.SERVERS,
        description = "Drop-in replacement for Paper with extensive gameplay customization and performance optimizations.",
        url = "https://purpurmc.org",
        license = "MIT",
        tag = "Configurable"
    ),
    SoftwareCredit(
        name = "Folia",
        category = CreditCategory.SERVERS,
        description = "Regionized multithreading Minecraft server software by the PaperMC team for high player counts.",
        url = "https://papermc.io/software/folia",
        license = "GPL-3.0",
        tag = "Multithreaded"
    ),
    SoftwareCredit(
        name = "Mojang Vanilla Server",
        category = CreditCategory.SERVERS,
        description = "Official standalone Minecraft server software developed by Mojang Studios / Microsoft.",
        url = "https://www.minecraft.net",
        license = "Proprietary",
        tag = "Official"
    ),
    SoftwareCredit(
        name = "FabricMC",
        category = CreditCategory.SERVERS,
        description = "Lightweight, modular modding toolchain and server environment for fast snapshots and releases.",
        url = "https://fabricmc.net",
        license = "Apache-2.0",
        tag = "Modular Modding"
    ),
    SoftwareCredit(
        name = "NeoForged",
        category = CreditCategory.SERVERS,
        description = "Community-driven, modern modding API and server platform for modern Minecraft editions.",
        url = "https://neoforged.net",
        license = "LGPL-2.1",
        tag = "Modding Platform"
    ),
    SoftwareCredit(
        name = "GeyserMC & Floodgate",
        category = CreditCategory.SERVERS,
        description = "Protocol translation proxy enabling Bedrock edition players to connect seamlessly to Java servers.",
        url = "https://geysermc.org",
        license = "MIT",
        tag = "Cross-Play"
    ),
    SoftwareCredit(
        name = "Ubuntu Rootfs",
        category = CreditCategory.SYSTEM,
        description = "Official Ubuntu base root filesystem providing the isolated Linux user-space container and APT package ecosystem.",
        url = "https://ubuntu.com",
        license = "Canonical",
        tag = "Linux Environment"
    ),
    SoftwareCredit(
        name = "LinuxOnAndroid Project",
        category = CreditCategory.SYSTEM,
        description = "PRoot virtualization engine, terminal emulator architecture, and Android compatibility foundation.",
        url = "https://github.com/devwithzachary/LinuxOnAndroid",
        license = "GPL-3.0",
        tag = "Virtualization Runtime"
    ),
    SoftwareCredit(
        name = "PRoot",
        category = CreditCategory.SYSTEM,
        description = "User-space implementation of chroot, mount --bind, and binfmt_misc without requiring root privileges.",
        url = "https://proot-me.github.io",
        license = "GPL-2.0",
        tag = "Rootless Sandbox"
    ),
    SoftwareCredit(
        name = "Modrinth",
        category = CreditCategory.LIBRARIES,
        description = "Open-source community platform and public REST API for discovering and downloading Minecraft plugins and mods.",
        url = "https://modrinth.com",
        license = "AGPL-3.0",
        tag = "Plugin Directory"
    ),
    SoftwareCredit(
        name = "OkHttp",
        category = CreditCategory.LIBRARIES,
        description = "HTTP and HTTP/2 client for Android and Java applications by Square.",
        url = "https://square.github.io/okhttp",
        license = "Apache-2.0",
        tag = "Networking"
    ),
    SoftwareCredit(
        name = "Coil Compose",
        category = CreditCategory.LIBRARIES,
        description = "Fast, lightweight image loading library for Android and Jetpack Compose.",
        url = "https://coil-kt.github.io/coil",
        license = "Apache-2.0",
        tag = "Image Loading"
    ),
    SoftwareCredit(
        name = "KotlinX Coroutines & Serialization",
        category = CreditCategory.LIBRARIES,
        description = "Asynchronous concurrency libraries and multiplatform JSON serialization for Kotlin by JetBrains.",
        url = "https://github.com/Kotlin",
        license = "Apache-2.0",
        tag = "Kotlin Ecosystem"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var selectedCategory by remember { mutableStateOf(CreditCategory.ALL) }

    val filteredCredits = remember(selectedCategory) {
        if (selectedCategory == CreditCategory.ALL) {
            SOFTWARE_CREDITS
        } else {
            SOFTWARE_CREDITS.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.credits_page_title),
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
            // Contributors & Community Section (At the Top)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                border = BorderStroke(1.dp, ObsidianCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        Text(
                            text = stringResource(R.string.credits_contributors_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = stringResource(R.string.credits_contributors_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate900,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Handshake, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Submit a PR or report an issue on GitHub to join the credits list!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Button(
                        onClick = { uriHandler.openUri("https://github.com/devwithzachary/MineServe") },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contribute on GitHub", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CreditCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat.label,
                                fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = Slate900,
                            labelColor = Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = ObsidianCardBorder,
                            selectedBorderColor = EmeraldPrimary,
                            enabled = true,
                            selected = selectedCategory == cat
                        )
                    )
                }
            }

            // Software Credits Cards
            filteredCredits.forEach { credit ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.dp, ObsidianCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(credit.url) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = credit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = credit.tag,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldLight
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate800
                            ) {
                                Text(
                                    text = credit.license,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = credit.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate400
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Visit Project",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
