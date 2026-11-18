package com.devwithzachary.mineserve.ui.screens.whatsnew

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

data class WhatsNewFeature(
    val badge: String,
    val title: String,
    val summary: String,
    val stepsToFind: List<String>,
    val icon: ImageVector
)

data class WhatsNewRelease(
    val majorVersion: String,
    val displayVersion: String,
    val headline: String,
    val subtitle: String,
    val features: List<WhatsNewFeature>
)

val CURRENT_WHATS_NEW_RELEASE = WhatsNewRelease(
    majorVersion = "1.7",
    displayVersion = "v1.7",
    headline = "What's New in MineServe 1.7",
    subtitle = "Explore the biggest new features added in this update, ordered by impact, with quick steps on where to find them in the app.",
    features = listOf(
        WhatsNewFeature(
            badge = "Optional Cloud",
            title = "MineServe Cloud Addresses & Cloud Backups",
            summary = "Want a custom server address (like myworld.play.mineserve.app) or automatic off-device cloud backups? You can now optionally link a MineServe Cloud account while all local hosting features stay 100% free.",
            stepsToFind = listOf(
                "Tap the Cloud icon on the Dashboard top bar (or open App Settings and check the MineServe Cloud card at the top) to view plans or link your account.",
                "Custom Server Address: Open your server's Settings tab, scroll to Public Multiplayer Tunneling, and choose MineServe Cloud.",
                "Cloud Backups: Open your server's Backups tab, expand Cloud Backup Sync, and select MineServe Cloud."
            ),
            icon = Icons.Default.Cloud
        ),
        WhatsNewFeature(
            badge = "Android Shortcuts",
            title = "Home Screen Widget & Quick Settings Tile",
            summary = "Start, stop, and check on your main Minecraft server right from your phone's home screen or notification shade without opening the app.",
            stepsToFind = listOf(
                "Home Screen Widget: Long-press an empty spot on your Android home screen, tap Widgets, and drag the MineServe widget onto your screen.",
                "Quick Settings Tile: Swipe down twice from the top of your screen, tap the Edit (pencil) button, and drag the MineServe tile into your active shortcuts."
            ),
            icon = Icons.Default.Widgets
        ),
        WhatsNewFeature(
            badge = "World Tools",
            title = "World Pre-Generator & Region Storage Cleaner",
            summary = "Build terrain around spawn ahead of time so exploring with friends feels smooth and lag-free, and inspect individual world region files to clean up unused storage space.",
            stepsToFind = listOf(
                "Tap your server on the Dashboard and switch to the World tab.",
                "Scroll to Chunk Pre-Generator, pick a dimension and radius, and tap Start Pre-Generation while your server is running.",
                "Scroll down to Region File Inspector to view your largest world files or delete unwanted regions while your server is stopped."
            ),
            icon = Icons.Default.Public
        ),
        WhatsNewFeature(
            badge = "Device Safety",
            title = "Battery & Overheating Protection",
            summary = "Host for hours with peace of mind. MineServe can now automatically save and stop your running servers if your battery gets low while unplugged or if your phone gets too warm.",
            stepsToFind = listOf(
                "Tap the Settings (gear) icon on the top bar of the Dashboard.",
                "Scroll down to the Battery & Thermal Safeguards card.",
                "Turn on Low Battery Auto-Stop or Thermal Auto-Stop and pick your preferred safety limit."
            ),
            icon = Icons.Default.BatteryChargingFull
        ),
        WhatsNewFeature(
            badge = "Desktop Browser",
            title = "Upgraded Web Remote Admin Panel",
            summary = "All of the new 1.7 features are also ready to use from your computer or tablet browser, including MineServe Cloud controls, World Pre-Generation, Region Inspection, Battery & Thermal Safeguards, and Network Self-Tests.",
            stepsToFind = listOf(
                "Tap the Settings (gear) icon on the Dashboard and tap Configure / Manage Web Remote Admin (or tap the Web Admin icon on the Dashboard top bar when active).",
                "Open the displayed web address in any browser on your computer or tablet."
            ),
            icon = Icons.Default.Dns
        ),
        WhatsNewFeature(
            badge = "Smoother Setup",
            title = "Background Downloads & Local File Import",
            summary = "System and server downloads now keep running safely in the background if you switch apps, pick up right where they left off if your Wi-Fi drops, and let you import server files you already downloaded.",
            stepsToFind = listOf(
                "Tap the + (Create Server) button on the Dashboard and go to Step 2 (Version).",
                "Tap Import Local Server JAR / Archive to pick a downloaded server file from your phone.",
                "During initial system setup, you can also tap Import Local Rootfs (.tar.gz) on the setup screen."
            ),
            icon = Icons.Default.Download
        ),
        WhatsNewFeature(
            badge = "Easy Support",
            title = "One-Tap Network Check & Troubleshooting Logs",
            summary = "Having trouble getting friends connected? Run a quick connection check to test your Wi-Fi, internet, and multiplayer tunnels, or save a privacy-safe support log bundle with personal details automatically hidden.",
            stepsToFind = listOf(
                "Tap the Settings (gear) icon on the Dashboard.",
                "Scroll down to Diagnostics & Logcat and tap Open Diagnostics & Logcat.",
                "Tap Run Self-Test to check your connection, or Share / Export .zip to save a support report."
            ),
            icon = Icons.Default.HealthAndSafety
        )
    )
)
