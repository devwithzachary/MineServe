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
            title = "macOS and Windows Desktop Betas",
            summary = "MineServe now runs natively on macOS (Apple Silicon and Intel) and Windows x64 in addition to Linux and Android, with the desktop app, menu bar or system tray controls, automatic Java setup, and headless web admin mode.",
            stepsToFind = listOf(
                "macOS: Open MineServe.app from the .dmg installer, or run mineserve --headless from Terminal.",
                "Windows: Install via the .msi or .exe installer, or run mineserve.bat --headless from the portable .zip."
            ),
            iconKey = "desktop",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Desktop Parity",
            title = "Desktop Automation, Crash Quick-Fixes and Full Parity",
            summary = "Desktop hosts (Linux, macOS, and Windows) now support scheduled backups and restarts, idle sleep with auto-wake on ping, automatic tunnels and port forwarding, Discord event alerts, WebDAV backup sync, chunk pre-generation, automatic GeyserMC setup, crash log quick-fixes, and Velocity or BungeeCord proxy routing.",
            stepsToFind = listOf(
                "Open a server's Management tab on Desktop to configure Automation schedules, Discord webhooks, or Proxy Routing.",
                "Visit World & Map to pre-generate chunks, Backups to test or sync WebDAV, or Console after a crash for one-click fixes."
            ),
            iconKey = "desktop",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Material Design 3",
            title = "Material Design 3 Redesign",
            summary = "MineServe now has a unified Material Design 3 look across Android, desktop, and the web admin panel, with dark and light themes plus an optional Android 12+ Material You dynamic color toggle.",
            stepsToFind = listOf(
                "Open App Settings -> Appearance to switch between Dark, Light, or System themes.",
                "On Android 12+, enable Use Wallpaper Dynamic Colors in App Settings to match your device wallpaper."
            ),
            iconKey = "palette",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Desktop & WebUI",
            title = "Unified 6-Tab Workspace and Offline-Ready Web Admin",
            summary = "Desktop and the headless web panel now share a consistent navigation sidebar, six server tabs (Console, Performance, Players, Content, World & Map, and Management), locally bundled fonts for offline LAN access, and built-in dialogs.",
            stepsToFind = listOf(
                "Select any server in the Desktop app or Web Remote Admin to explore the unified 6-tab workspace.",
                "Open App Settings in the WebUI to switch themes or manage Java Runtimes and Host Diagnostics."
            ),
            iconKey = "web",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Server Creation",
            title = "Simpler 2-Step Server Creation",
            summary = "Creating a new server now follows the same two-step flow across every platform: pick your Minecraft version and server software in Step 1, then configure your server name, port, MOTD, RAM, and EULA in Step 2.",
            stepsToFind = listOf(
                "Click + Create Server on Android, Desktop, or the Web Remote Admin sidebar.",
                "Pick your Minecraft version and software type in Step 1, then click Next to configure your server."
            ),
            iconKey = "mods",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Team & Host Control",
            title = "Audit Log CSV Export, Server-Scoped Users and Custom Paths",
            summary = "Export your panel audit log to CSV on Android, desktop, and the web panel, restrict desktop user accounts to specific servers, and choose custom folders for server data and Java runtimes from App Settings.",
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
            subtitle = "Here are the main additions in this update and where to find them.",
            features = ALL_FEATURES_1_9.filter { it.platform.matches(platform) }
        )
    }
}
