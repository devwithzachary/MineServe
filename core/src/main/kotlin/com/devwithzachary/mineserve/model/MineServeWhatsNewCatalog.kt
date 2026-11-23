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
    const val CURRENT_MAJOR_VERSION = "1.8"
    const val CURRENT_DISPLAY_VERSION = "v1.8"

    private val ALL_FEATURES_1_8 = listOf(
        SharedWhatsNewFeature(
            badge = "Linux Desktop & Headless",
            title = "Native Linux Desktop GUI, System Tray & Headless Daemon",
            summary = "MineServe now runs natively on Linux with zero virtualization overhead, automatic portable OpenJDK 8/17/21/25 downloads, an adaptive multi-pane Desktop UI, KDE/Wayland System Tray support, and a --headless CLI mode for home servers and VPS hosts.",
            stepsToFind = listOf(
                "Desktop GUI: Launch MineServe normally to use the side-by-side Console, Config Editor, Modrinth Browser, and In-App Live Map.",
                "System Tray: Close the window while a server is running to minimize to your Linux notification tray. Left-click to restore or right-click for quick actions.",
                "Headless Mode: Run mineserve --headless (or --print-service for a systemd unit) to host purely via the Web Remote Admin UI."
            ),
            iconKey = "desktop",
            platform = ChangelogPlatform.LINUX
        ),
        SharedWhatsNewFeature(
            badge = "Team Access & Security",
            title = "Multi-User Web Admin Accounts, Roles & Audit Log",
            summary = "Share your server panel safely with friends and moderators. Create individual accounts with Owner, Admin, Moderator, or Viewer permissions, limit users to specific servers, manage active login sessions, and review every action in the Audit Log.",
            stepsToFind = listOf(
                "In the Android app: Open App Settings -> Web Remote Admin, then scroll to Panel Users & Roles.",
                "In the Desktop UI or Web UI: Click Users & Sessions (or Users & Audit) in the left sidebar to add accounts, assign roles, or revoke active sessions.",
                "In the Web UI: Click Audit Log in the left sidebar to search, filter, and export a CSV history of all panel actions."
            ),
            iconKey = "security",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Web Admin Upgrade",
            title = "Live SSE Console, Full File Manager & In-Place Upgrader",
            summary = "The Web Remote Admin panel now streams console output in real time with 2,000-line scrollback, includes a drag-and-drop File Manager (create, rename, upload, download, unzip), and lets you manage OpenJDK runtimes, upgrade Minecraft versions in place, or import existing server .zip archives.",
            stepsToFind = listOf(
                "Live Console & Copy: Open the Console tab in the Web UI or Desktop UI and click Copy Console Output anytime.",
                "File Manager: Open the Files tab to create files/folders, rename items, extract .zip archives, or drag-and-drop uploads from your computer.",
                "Version Upgrader: Open Settings & Network and scroll to Server Software Version & Upgrade to update builds or Minecraft versions with an automatic safety backup."
            ),
            iconKey = "web",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Modded Servers",
            title = "Automatic Client-Only Mod Filtering for .mrpack Modpacks",
            summary = "Importing Modrinth .mrpack modpacks is now much more reliable. MineServe automatically detects and disables client-only rendering and UI mods (like Sodium, Iris, ImmediatelyFast, and EntityCulling) as well as duplicate JARs so dedicated servers don't crash on startup.",
            stepsToFind = listOf(
                "Create or select a Fabric, NeoForge, or Forge server and open the Plugins / Mods tab.",
                "Click Import .mrpack and pick any Modrinth modpack archive.",
                "MineServe will download all server mods, extract overrides, and automatically rename client-only mods to .jar.disabled."
            ),
            iconKey = "mods",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "MineServe Cloud",
            title = "Quick 6-Digit Email Code Cloud Sign-In",
            summary = "Link your optional MineServe Cloud account directly inside the app, Desktop UI, or Web UI using a 6-digit email verification code to manage custom *.play.mineserve.app addresses and encrypted R2 cloud backups.",
            stepsToFind = listOf(
                "Click MineServe Cloud in the sidebar (or top bar on Android).",
                "Enter your email address and click Send 6-Digit Code.",
                "Enter the code from your email to link your host immediately."
            ),
            iconKey = "cloud",
            platform = ChangelogPlatform.ALL
        ),
        SharedWhatsNewFeature(
            badge = "Setup & Live Map",
            title = "Smooth First-Run Setup Progress & Port 8081 Live Map",
            summary = "Initial runtime setup now tracks live package extraction and OpenJDK installation stages smoothly from start to finish, and Squaremap Live Map now defaults to port 8081 so it never conflicts with Web Remote Admin on port 8080.",
            stepsToFind = listOf(
                "Open any plugin or mod server and switch to the Live Map tab.",
                "Tap 1-Click Install Squaremap to pre-configure Squaremap on port 8081 and view your world map live."
            ),
            iconKey = "map",
            platform = ChangelogPlatform.ANDROID
        )
    )

    fun getForPlatform(platform: ChangelogPlatform): SharedWhatsNewRelease {
        return SharedWhatsNewRelease(
            majorVersion = CURRENT_MAJOR_VERSION,
            displayVersion = CURRENT_DISPLAY_VERSION,
            headline = "What's New in MineServe 1.8",
            subtitle = "Explore the biggest new features added in this update, ordered by impact, with quick steps on where to find them.",
            features = ALL_FEATURES_1_8.filter { it.platform.matches(platform) }
        )
    }
}
