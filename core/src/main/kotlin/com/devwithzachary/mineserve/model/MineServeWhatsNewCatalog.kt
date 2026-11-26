package com.devwithzachary.mineserve.model

data class SharedWhatsNewFeature(
    val badge: String,
    val title: String,
    val summary: String,
    val stepsToFind: List<String>,
    val iconKey: String,
    val platform: ChangelogPlatform = ChangelogPlatform.ALL
)

data class SharedWhatsNewRelease(
    val majorVersion: String,
    val displayVersion: String,
    val headline: String,
    val subtitle: String,
    val features: List<SharedWhatsNewFeature>
)

/**
 * Single source of truth for the "What's New" major release feature guide across
 * Android, Linux Desktop, and the Web Remote Admin UI.
 */
object MineServeWhatsNewCatalog {
    const val CURRENT_MAJOR_VERSION = "1.9"
    const val CURRENT_DISPLAY_VERSION = "v1.9"

    private val ALL_FEATURES_1_9 = listOf(
        SharedWhatsNewFeature(
            badge = "New Platforms",
            title = "macOS & Windows Desktop Betas (Native GUI & Headless)",
            summary = "MineServe now runs natively on macOS (Apple Silicon & Intel) and Windows x64 in addition to Linux and Android, with native Compose Desktop UIs, menu bar / system tray controls, automatic portable OpenJDK setup, and --headless Web Admin mode.",
            stepsToFind = listOf(
                "macOS: Open MineServe.app from the .dmg installer (or run mineserve --headless from Terminal).",
                "Windows: Install via the .msi or .exe installer (or run mineserve.bat --headless from the portable .zip)."
            ),
            iconKey = "desktop",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Desktop Parity",
            title = "Desktop Automation, Crash Quick-Fixes & Full Feature Parity",
            summary = "Desktop hosts (Linux, macOS, and Windows) now run scheduled backups and restarts, Idle Sleep with Auto-Wake on Ping, auto-start tunnels and UPnP, live Discord event alerts, WebDAV backup sync, Chunk Pre-Generation, automatic GeyserMC setup, Crash Log quick-fixes, and native Velocity/BungeeCord proxy routing.",
            stepsToFind = listOf(
                "Open a server's Management tab on Desktop to configure Automation schedules, Discord webhooks, or Proxy Routing.",
                "Visit World & Map to pre-generate chunks, Backups to test/sync WebDAV, or Console after a crash for 1-click fixes."
            ),
            iconKey = "desktop",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Material Design 3",
            title = "All-New Obsidian & Ore Material 3 Redesign",
            summary = "MineServe now features a unified Material Design 3 look across Android, Linux Desktop, and Web Remote Admin with Light & Dark themes, cleaner borderless tonal cards, and an optional Android 12+ Material You wallpaper color mode.",
            stepsToFind = listOf(
                "Open App Settings -> Appearance to switch between Dark, Light, or System themes.",
                "On Android 12+, enable Use Wallpaper Dynamic Colors in App Settings to match your device wallpaper."
            ),
            iconKey = "palette",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Desktop & WebUI",
            title = "Unified 6-Tab Workspace & Offline-Ready Web Admin",
            summary = "Desktop and Headless WebUI now share a clean 280dp navigation drawer, standardized Console, Performance, Players, Content, World & Map, and Management tabs, self-hosted fonts for offline LAN access, and native modals.",
            stepsToFind = listOf(
                "Select any server in the Desktop app or Web Remote Admin to explore the unified 6-tab workspace.",
                "Open App Settings in the WebUI to switch themes or manage Java Runtimes and Host Diagnostics."
            ),
            iconKey = "web",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Server Creation",
            title = "Streamlined 2-Step Create Server Flow",
            summary = "Creating a new server is now consistent across every platform: pick your Minecraft version and server software with clear descriptions in Step 1, then configure your server name, port, MOTD, RAM, and EULA in Step 2.",
            stepsToFind = listOf(
                "Click + Create Server on Android, Desktop, or the Web Remote Admin sidebar.",
                "Pick your Minecraft version and software type in Step 1, then click Next to configure your server."
            ),
            iconKey = "mods",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Team & Host Control",
            title = "Audit Log CSV Export, Server-Scoped Users & Custom Host Paths",
            summary = "Export your panel Audit Log to CSV on Android, Desktop, and WebUI, restrict Desktop user accounts to specific servers, and customize your Server Data and Java Runtimes directories right from App Settings.",
            stepsToFind = listOf(
                "Open Users & Audit (or Panel Users & Roles on Android) and click Export CSV.",
                "On Desktop or Desktop WebUI, open App Settings -> Appearance & Host to change Server Data or Java Runtimes directories."
            ),
            iconKey = "security",
            platform = ChangelogPlatform.ALL
        )
    )

    fun getForPlatform(platform: ChangelogPlatform): SharedWhatsNewRelease {
        return SharedWhatsNewRelease(
            majorVersion = CURRENT_MAJOR_VERSION,
            displayVersion = CURRENT_DISPLAY_VERSION,
            headline = "What's New in MineServe 1.9",
            subtitle = "Explore the biggest new features added in this update, ordered by impact, with quick steps on where to find them.",
            features = ALL_FEATURES_1_9.filter { it.platform.matches(platform) }
        )
    }
}
