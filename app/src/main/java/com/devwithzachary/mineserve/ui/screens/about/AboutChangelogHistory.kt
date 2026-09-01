package com.devwithzachary.mineserve.ui.screens.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.R
import com.devwithzachary.mineserve.ui.components.ChangelogItem
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder

data class ReleaseChangelog(
    val version: String,
    val date: String,
    val initialExpanded: Boolean = false,
    val highlights: List<String>
)

val APP_CHANGELOG_HISTORY: List<ReleaseChangelog> = listOf(
    ReleaseChangelog(
        version = "v1.2.0",
        date = "September 1, 2026",
        initialExpanded = true,
        highlights = listOf(
            "Real-Time TPS & Engine Health Diagnostics: Replaced static TPS with live engine telemetry combining real-time CPU core utilization, tick duration (MSPT), tick budget headroom, and passive overload detection.",
            "Performance Tab TPS Diagnostics Card: Added dedicated diagnostics card with live TPS gauge, MSPT processing time, tick budget headroom, and overload warning tracking.",
            "Tunneling Services Recognition: Added dedicated Tunneling category to the in-app Credits screen recognizing bore (bore.pub) and Playit.gg for zero-port-forwarding multiplayer.",
            "Open Source Documentation: Updated README and in-app credit directory with official links, licenses, and architecture descriptions for all integrated tunneling proxies."
        )
    ),
    ReleaseChangelog(
        version = "v1.1.0",
        date = "August 28, 2026",
        initialExpanded = false,
        highlights = listOf(
            "Public Tunneling & Zero-Port-Forwarding: Support for both instant free tunnels (bore.pub) and persistent Playit.gg tunnels. Host multiplayer servers over 4G/5G mobile data or remote Wi-Fi bypassing Carrier-Grade NAT (CGNAT) without port forwarding.",
            "Playit.gg Integration: Automatic agent execution supporting 1-tap browser claim setup and custom secret keys with persistent subdomains.",
            "Graceful Disconnection: Seamless 1-tap tunnel shutdown transitioning to Offline without throwing false socket closure error messages.",
            "Dismissable Security Notice: 1-tap dismissable security warning banners reminding server hosts to enable whitelisting in Server Properties to safeguard worlds against griefers.",
            "Dynamic Bedrock & Java QR Code Sharing: Dynamic QR code and share sheet generation with Bedrock 1-tap import links (minecraft://) and Java quick connection addresses.",
            "Smooth Terminal Scrollback Navigation: Drag up and down on the live terminal canvas to review historical Minecraft server startup logs and execution output with smooth scrolling.",
            "Scroll to Bottom Indicator: Floating jump-to-bottom badge displaying the current scroll depth offset with 1-tap return to real-time logs.",
            "Native Touch Word Detection & Selection: Long-press on any log output or command argument to automatically select word boundaries with tactile haptic feedback.",
            "Draggable Teardrop Selection Handles: Fine-tune multi-line text selection ranges using interactive teardrop touch handles with live haptic tick feedback.",
            "Visual Character Highlighting: High-contrast theme-aware text highlight overlays across single-line and multi-line selection bounding boxes.",
            "Floating Action Toolbar: 1-tap floating toolbar supporting Copy, Select All, Share, and Clear selection.",
            "GitHub Release Update Checker: Real-time update checks against GitHub Releases with Play Store certificate transition guidance and in-app settings controls.",
            "Patreon Supporters Recognition: Added dedicated supporters recognition on the Credits and About screens honoring Patreon supporters (Old PC Gunk (and stuff), насэр Хорр)."
        )
    ),
    ReleaseChangelog(
        version = "v1.0.0",
        date = "August 26, 2026",
        initialExpanded = false,
        highlights = listOf(
            "Multi-Engine Minecraft Server Creation & Management: Full support for PaperMC, PurpurMC, Folia, FabricMC, NeoForged, Mojang Vanilla, and Bedrock Geyser / Floodgate.",
            "Smart 4-Step Server Wizard: Server type, dynamic version querying, hardware & RAM allocation, and automated initialization.",
            "Rootless PRoot Virtualization & Container Architecture: 100% rootless operation on unrooted Android devices using minimal Ubuntu 24.04 LTS rootfs container.",
            "Multi-Version OpenJDK Isolation: Automatic 1-tap installation and binding of Java 25, 21, 17, and 8.",
            "Context-Aware Plugins & Mods Management: Modrinth API integration with search, filters, metadata details, and direct .jar installation/importing.",
            "Interactive Live Terminal & Command Console: Live colored output stream with standard input command prompt and terminal controls.",
            "Performance Monitoring & Live Telemetry: Real-time CPU %, Resident RAM (RSS in MB via /proc), active player counts, and disk footprints.",
            "Persistent Foreground Execution & WakeLock: 24/7 background uptime holding CPU WakeLock with interactive notification shade controls."
        )
    )
)

@Composable
fun AboutChangelogSection(modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        border = BorderStroke(1.dp, ObsidianCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.about_changelog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            for (entry in APP_CHANGELOG_HISTORY) {
                ChangelogItem(
                    version = entry.version,
                    date = entry.date,
                    initialExpanded = entry.initialExpanded,
                    highlights = entry.highlights
                )
            }
        }
    }
}
