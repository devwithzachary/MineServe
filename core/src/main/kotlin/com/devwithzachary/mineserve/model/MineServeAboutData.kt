package com.devwithzachary.mineserve.model

/**
 * Target platform filter for changelog highlights and software credits.
 */
enum class ChangelogPlatform {
    ALL,
    ANDROID,
    LINUX;

    fun matches(target: ChangelogPlatform): Boolean {
        return this == ALL || target == ALL || this == target
    }
}

data class PlatformChangelogHighlight(
    val text: String,
    val platform: ChangelogPlatform = ChangelogPlatform.ALL
)

data class PlatformReleaseChangelog(
    val version: String,
    val date: String,
    val initialExpanded: Boolean = false,
    val highlights: List<PlatformChangelogHighlight>
)

data class FilteredReleaseChangelog(
    val version: String,
    val date: String,
    val initialExpanded: Boolean = false,
    val highlights: List<String>
)

enum class SharedCreditCategory(val label: String) {
    ALL("All"),
    SERVERS("Server Software"),
    SYSTEM("Runtime & Linux"),
    TUNNELING("Tunneling"),
    LIBRARIES("APIs & Libraries")
}

data class SharedSoftwareCredit(
    val name: String,
    val category: SharedCreditCategory,
    val description: String,
    val url: String,
    val license: String,
    val tag: String,
    val platform: ChangelogPlatform = ChangelogPlatform.ALL
)

data class SharedSupporter(
    val name: String,
    val tier: String
)

/**
 * Single source of truth for MineServe version numbers, platform-filtered changelogs,
 * open-source software credits, and project supporters across Android, Linux Desktop, and Web UI.
 */
object MineServeAboutData {
    /**
     * Unified release version shared across Android, Linux Desktop, and Web Remote Admin.
     * Always kept in lockstep with `versionName` in `app/build.gradle.kts`.
     */
    const val APP_VERSION = "1.8.1"
    const val VERSION_CODE = 15

    const val WEBSITE_URL = "https://mineserve.app"
    const val GITHUB_REPO_URL = "https://github.com/devwithzachary/MineServe"
    const val GITHUB_ISSUES_URL = "https://github.com/devwithzachary/MineServe/issues"
    const val GITHUB_RELEASES_URL = "https://github.com/devwithzachary/MineServe/releases"
    const val DISCORD_URL = "https://discord.gg/QdT9yCrKmV"
    const val PATREON_URL = "https://patreon.com/devwithzachary"
    const val BUYMEACOFFEE_URL = "https://www.buymeacoffee.com/devwithzachary"
    const val PRIVACY_URL = "https://mineserve.app/privacy.html"
    const val LICENSE_NAME = "GNU General Public License v3.0 (GPL-3.0)"

    val SUPPORTERS: List<SharedSupporter> = listOf(
        SharedSupporter(name = "Alen", tier = "Tinkerer (BuyMeACoffee)"),
        SharedSupporter(name = "Many Fun Adventures", tier = "Supporter (BuyMeACoffee)"),
        SharedSupporter(name = "Old PC Gunk (and stuff)", tier = "Supporter (Patreon)"),
        SharedSupporter(name = "ShoaffSquatch", tier = "Supporter (YouTube)"),
        SharedSupporter(name = "Swamp Gaming Network", tier = "Supporter (Patreon)"),
        SharedSupporter(name = "насэр Хорр", tier = "Supporter (Patreon)"),
        SharedSupporter(name = "Alcove Electronics Workshop", tier = "Tipper (YouTube)"),
        SharedSupporter(name = "Ian Scales", tier = "Tipper (YouTube)")
    )

    val SOFTWARE_CREDITS: List<SharedSoftwareCredit> = listOf(
        SharedSoftwareCredit(
            name = "PaperMC",
            category = SharedCreditCategory.SERVERS,
            description = "High-performance, secure Minecraft server engine designed for speed and extensive plugin API support.",
            url = "https://papermc.io",
            license = "GPL-3.0",
            tag = "Core Engine"
        ),
        SharedSoftwareCredit(
            name = "PurpurMC",
            category = SharedCreditCategory.SERVERS,
            description = "Drop-in replacement for Paper with extensive gameplay customization and performance optimizations.",
            url = "https://purpurmc.org",
            license = "MIT",
            tag = "Configurable"
        ),
        SharedSoftwareCredit(
            name = "Folia",
            category = SharedCreditCategory.SERVERS,
            description = "Regionized multithreading Minecraft server software by the PaperMC team for high player counts.",
            url = "https://papermc.io/software/folia",
            license = "GPL-3.0",
            tag = "Multithreaded"
        ),
        SharedSoftwareCredit(
            name = "Mojang Vanilla Server",
            category = SharedCreditCategory.SERVERS,
            description = "Official standalone Minecraft server software developed by Mojang Studios / Microsoft.",
            url = "https://www.minecraft.net",
            license = "Proprietary",
            tag = "Official"
        ),
        SharedSoftwareCredit(
            name = "FabricMC",
            category = SharedCreditCategory.SERVERS,
            description = "Lightweight, modular modding toolchain and server environment for fast snapshots and releases.",
            url = "https://fabricmc.net",
            license = "Apache-2.0",
            tag = "Modular Modding"
        ),
        SharedSoftwareCredit(
            name = "NeoForged",
            category = SharedCreditCategory.SERVERS,
            description = "Community-driven, modern modding API and server platform for modern Minecraft editions.",
            url = "https://neoforged.net",
            license = "LGPL-2.1",
            tag = "Modding Platform"
        ),
        SharedSoftwareCredit(
            name = "Minecraft Forge",
            category = SharedCreditCategory.SERVERS,
            description = "Classic modding API and server loader supporting historical and modern Minecraft mod ecosystems.",
            url = "https://files.minecraftforge.net",
            license = "LGPL-2.1",
            tag = "Legacy & Modern Mods"
        ),
        SharedSoftwareCredit(
            name = "Velocity & BungeeCord",
            category = SharedCreditCategory.SERVERS,
            description = "High-performance Minecraft proxy servers enabling multi-server networks and seamless server switching.",
            url = "https://papermc.io/software/velocity",
            license = "GPL-3.0 / BSD-3-Clause",
            tag = "Proxy Network"
        ),
        SharedSoftwareCredit(
            name = "GeyserMC & Floodgate",
            category = SharedCreditCategory.SERVERS,
            description = "Protocol translation proxy enabling Bedrock edition players to connect seamlessly to Java servers.",
            url = "https://geysermc.org",
            license = "MIT",
            tag = "Cross-Play"
        ),
        SharedSoftwareCredit(
            name = "Squaremap",
            category = SharedCreditCategory.SERVERS,
            description = "Lightweight, ultra-fast 2D live web map for Minecraft servers with real-time player tracking and minimal resource overhead.",
            url = "https://github.com/jpenilla/squaremap",
            license = "MIT",
            tag = "Live Web Map"
        ),
        SharedSoftwareCredit(
            name = "Adoptium Temurin OpenJDK",
            category = SharedCreditCategory.SYSTEM,
            description = "Open-source, TCK-certified OpenJDK 8, 17, 21, and 25 runtime binaries built by the Eclipse Adoptium Working Group.",
            url = "https://adoptium.net",
            license = "GPL-2.0-with-classpath-exception",
            tag = "Java Runtime"
        ),
        SharedSoftwareCredit(
            name = "Ubuntu Rootfs",
            category = SharedCreditCategory.SYSTEM,
            description = "Official Ubuntu base root filesystem providing the isolated Linux user-space container and APT package ecosystem.",
            url = "https://ubuntu.com",
            license = "Canonical",
            tag = "Linux Environment",
            platform = ChangelogPlatform.ANDROID
        ),
        SharedSoftwareCredit(
            name = "LinuxOnAndroid Project",
            category = SharedCreditCategory.SYSTEM,
            description = "PRoot virtualization engine, terminal emulator architecture, and Android compatibility foundation.",
            url = "https://github.com/devwithzachary/LinuxOnAndroid",
            license = "GPL-3.0",
            tag = "Virtualization Runtime",
            platform = ChangelogPlatform.ANDROID
        ),
        SharedSoftwareCredit(
            name = "PRoot",
            category = SharedCreditCategory.SYSTEM,
            description = "User-space implementation of chroot, mount --bind, and binfmt_misc without requiring root privileges.",
            url = "https://proot-me.github.io",
            license = "GPL-2.0",
            tag = "Rootless Sandbox",
            platform = ChangelogPlatform.ANDROID
        ),
        SharedSoftwareCredit(
            name = "bore (bore.pub)",
            category = SharedCreditCategory.TUNNELING,
            description = "Modern, simple TCP tunnel that exposes local Minecraft server ports to the Internet with zero configuration.",
            url = "https://github.com/ekzhang/bore",
            license = "MIT",
            tag = "TCP Tunnel"
        ),
        SharedSoftwareCredit(
            name = "Playit.gg",
            category = SharedCreditCategory.TUNNELING,
            description = "Global tunneling proxy network enabling persistent subdomains and zero-port-forwarding multiplayer.",
            url = "https://playit.gg",
            license = "MIT / AGPL-3.0",
            tag = "Global Proxy"
        ),
        SharedSoftwareCredit(
            name = "Modrinth",
            category = SharedCreditCategory.LIBRARIES,
            description = "Open-source community platform and public REST API for discovering and downloading Minecraft plugins and mods.",
            url = "https://modrinth.com",
            license = "AGPL-3.0",
            tag = "Plugin Directory"
        ),
        SharedSoftwareCredit(
            name = "Jetpack Compose & Compose Multiplatform",
            category = SharedCreditCategory.LIBRARIES,
            description = "Declarative UI framework by Google and JetBrains powering MineServe on Android and Linux Desktop.",
            url = "https://www.jetbrains.com/compose-multiplatform/",
            license = "Apache-2.0",
            tag = "UI Framework"
        ),
        SharedSoftwareCredit(
            name = "OkHttp",
            category = SharedCreditCategory.LIBRARIES,
            description = "HTTP and HTTP/2 client for Android and Java applications by Square.",
            url = "https://square.github.io/okhttp",
            license = "Apache-2.0",
            tag = "Networking"
        ),
        SharedSoftwareCredit(
            name = "Coil Compose",
            category = SharedCreditCategory.LIBRARIES,
            description = "Fast, lightweight image loading library for Android and Jetpack Compose.",
            url = "https://coil-kt.github.io/coil",
            license = "Apache-2.0",
            tag = "Image Loading",
            platform = ChangelogPlatform.ANDROID
        ),
        SharedSoftwareCredit(
            name = "KotlinX Coroutines & Serialization",
            category = SharedCreditCategory.LIBRARIES,
            description = "Asynchronous concurrency libraries and multiplatform JSON serialization for Kotlin by JetBrains.",
            url = "https://github.com/Kotlin",
            license = "Apache-2.0",
            tag = "Kotlin Ecosystem"
        )
    )

    val FULL_CHANGELOG_HISTORY: List<PlatformReleaseChangelog> = listOf(
        PlatformReleaseChangelog(
            version = "v1.9.0",
            date = "In Progress",
            initialExpanded = true,
            highlights = listOf(
                PlatformChangelogHighlight(
                    text = "Material Design 3 \"Obsidian & Ore\" Redesign: Refreshed Android, Linux Desktop, and Web Remote Admin with Light and Dark themes, cleaner borderless tonal surfaces, and updated typography (Plus Jakarta Sans + JetBrains Mono).",
                    platform = ChangelogPlatform.ALL
                ),
                PlatformChangelogHighlight(
                    text = "Android Material You & Workspace Parity: Added an optional Android 12+ Material You wallpaper dynamic color toggle in App Settings, unified the 6-tab server workspace, and added Audit Log CSV export.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight(
                    text = "Unified 2-Step Create Server & WebUI Parity: Standardized server creation (software descriptions + Minecraft version in Step 1, configuration & EULA in Step 2) and brought 1:1 Headless WebUI parity with offline self-hosted fonts, native dialogs, and Desktop-matching App Settings tabs.",
                    platform = ChangelogPlatform.ALL
                ),
                PlatformChangelogHighlight(
                    text = "Desktop Team & Host Controls: Added per-server access restrictions in Desktop Create User, Audit Log CSV export on Desktop, and configurable Server Data and Java Runtimes directories in Desktop and Linux WebUI App Settings.",
                    platform = ChangelogPlatform.LINUX
                )
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.8.1",
            date = "November 24, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight(
                    text = "Play Store Compliance & Android Reliability: Hid external donation links and GitHub APK self-update prompts on Google Play Store builds (retained on direct APK and F-Droid builds), fixed Bore and MineServe Cloud tunnels on Android 6 to 12, wired Keep Screen Awake on the live console, kept the foreground service active while Web Remote Admin is running, and honored custom JVM flags.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight(
                    text = "Linux Desktop, Headless & Packaging Fixes: Fixed 0755 executable permissions and 512x512/pixmap desktop icons on Arch/CachyOS and Debian/Ubuntu, added graceful blocking server shutdown on Quit/SIGTERM, fixed Playit.gg port detection, added live --reset-admin reload on running daemons, added systemd sysusers/StateDirectory provisioning, fixed Docker builds (-PexcludeAndroid=true), and improved Desktop UI responsiveness, system tray safety, and Web Admin parity.",
                    platform = ChangelogPlatform.LINUX
                ),
                PlatformChangelogHighlight(
                    text = "Security, World & Web Admin Hardening: Made the Chunk Pruner fail-safe on Minecraft 1.20.5+ LZ4 and external .mcc chunks, fixed Forge vs NeoForge and proxy Modrinth searches, hardened file/upload/modpack path containment and RBAC session revocation, and fixed Web UI telemetry chart fills, cloud backup loading, and error toasts.",
                    platform = ChangelogPlatform.ALL
                )
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.8.0",
            date = "November 23, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight(
                    text = "MineServe for Linux (Desktop GUI & Headless Beta): Added a native Linux server engine and adaptive multi-pane Compose Desktop GUI (:desktop) with Linux System Tray integration, HiDPI UI scaling, portable Adoptium OpenJDK 8/17/21/25 provisioning, /proc telemetry, active firewall detection, client-only modpack filtering, Crash Watchdog auto-restart, systemd unit generation, and the embedded Web Admin panel.",
                    platform = ChangelogPlatform.LINUX
                ),
                PlatformChangelogHighlight(
                    text = "Multi-User Web Admin, RBAC & Audit Log: Create and manage multiple panel user accounts with Owner, Admin, Moderator, or Viewer roles, restrict users to specific servers, view and revoke active sessions, change passwords, and inspect the live audit log from both the app and the Web UI.",
                    platform = ChangelogPlatform.ALL
                ),
                PlatformChangelogHighlight(
                    text = "In-App Cloud Sign-In & Localized Pricing: Sign in to MineServe Cloud directly inside the app with a 6-digit email code, with regional currency pricing estimates on direct builds.",
                    platform = ChangelogPlatform.ALL
                ),
                PlatformChangelogHighlight(
                    text = "Smooth First-Run Setup Progress: Re-weighted initial runtime setup across download, extraction, and live OpenJDK package installation steps so the progress bar moves steadily instead of stalling at 98%.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight(
                    text = "End-to-End Web Remote Admin Upgrades: Added real-time SSE live console streaming (2,000-line scrollback), full-screen searchable Audit Log with CSV export, drag-and-drop File Manager (upload, download, create, rename, unzip), Java Runtime Manager (8/17/21/25), in-place Server Version Upgrader, Import Existing Server (.zip), live public join address bar, device-aware RAM sliders, and fixed Restart.",
                    platform = ChangelogPlatform.ALL
                ),
                PlatformChangelogHighlight(
                    text = "Automatic Client-Only Mod Filtering (.mrpack): Automatically detects and disables client-only rendering/UI mods (such as Sodium, Iris, ImmediatelyFast, EntityCulling) and duplicate JARs when importing Modrinth .mrpack archives or booting modded servers.",
                    platform = ChangelogPlatform.ALL
                ),
                PlatformChangelogHighlight(
                    text = "Networking & Reliability Fixes: Changed Squaremap Live Map's default port to 8081 so it never collides with Web Remote Admin on port 8080, fixed Squaremap uninstall state refresh, prevented already-connected MineServe Cloud tunnels from reconnecting on server boot with clean port-reclaim retry limits, improved Web Admin login rate limiting behind reverse proxies, and ensured panel tunnels shut down cleanly.",
                    platform = ChangelogPlatform.ALL
                )
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.7.0",
            date = "November 19, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("MineServe Cloud Account Linking: Link your optional MineServe Cloud account in App Settings to view your plan, backup quota, and custom server addresses."),
                PlatformChangelogHighlight("MineServe Cloud Backups: Select MineServe Cloud in a server's Backups tab to upload world backups to encrypted cloud storage, auto-sync new backups, and restore or delete cloud snapshots."),
                PlatformChangelogHighlight("MineServe Cloud Tunnels: Reserve and connect custom *.play.mineserve.app (and Pro *.playms.uk) server addresses and regional relays directly from Server Settings."),
                PlatformChangelogHighlight("Dedicated MineServe Cloud Page: Access your MineServe Cloud account, storage usage, and transparent plan pricing directly from the app."),
                PlatformChangelogHighlight("In-App Diagnostics & Log Export: Capture redacted app/engine logs, run a one-tap tunnel & DNS network self-test, and export diagnostic .zip bundles from App Settings."),
                PlatformChangelogHighlight(
                    text = "Resumable Background Downloads & Local Import: Initial Ubuntu rootfs and server JAR downloads now continue safely in the background via foreground service, resume from partial downloads, and support importing local .tar.gz / .jar / .zip files.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight(
                    text = "Android Home Screen Widget & Quick Settings Tile: Monitor your primary server's status and start or stop it with one tap from your home screen or Quick Settings panel.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight(
                    text = "Battery & Thermal Safeguards: Configure automatic server shutdown in App Settings when battery drops below a chosen percentage (while unplugged) or device temperature exceeds a safe threshold.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight(
                    text = "What's New Feature Guide: Added a friendly What's New screen on first launch of major (1.x) updates with step-by-step guides on where to find new features, plus controls in App Settings to review it anytime or disable future update prompts.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight("Web Remote Admin v1.7.0 Parity: Manage MineServe Cloud tunnels and R2 snapshots, capture redacted logs, and run network self-tests directly from the Web UI."),
                PlatformChangelogHighlight("World Pre-Generation & Region Inspector: Pre-generate chunks around spawn via Chunky or native /forceload stepping and inspect or delete bloated .mca region files in both the app and Web UI."),
                PlatformChangelogHighlight("Performance & Architecture: Added full-width responsive terminal line reflow and extracted shared core networking and engine models into a dedicated :core module.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.6.1",
            date = "November 16, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight(
                    text = "F-Droid Packaging Compliance: Enabled R8 minification and optimizations with comprehensive ProGuard rules for JNI, Kotlinx serialization, and Compose.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight(
                    text = "Binary Optimization: Reduced release APK footprint while maintaining complete runtime stability and native execution.",
                    platform = ChangelogPlatform.ANDROID
                )
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.6.0",
            date = "November 12, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Full Feature-Parity Web UI: Create and delete servers, manage players (Whitelist, Ops, Bans), configure automation and scheduled tasks, and edit all server settings directly from your browser."),
                PlatformChangelogHighlight("Web Performance, World & Live Map: Added live CPU/RAM/TPS telemetry charts over time, 1-click Aikar's flags, world import/export, configurable chunk pruning, and Squaremap live map integration with automatic port conflict protection."),
                PlatformChangelogHighlight("Web Backups, Plugins & Networking: Added browser-based backup creation/restore/download, Modrinth plugin/mod search and install, custom .jar upload, .mrpack modpack importer, and Bore/Playit.gg tunnel and UPnP/NAT-PMP port forwarding controls."),
                PlatformChangelogHighlight("Discord Webhook & Event Bridge: Configure Discord webhooks per server in Settings or the Web UI to broadcast server online/offline/crash events, player join/leave notices with skins, and completed backups with zero plugins."),
                PlatformChangelogHighlight("Proxy & Multi-Server Networks (Velocity & BungeeCord): Create Velocity or BungeeCord proxies, link multiple backend servers into a unified network, and bind backends strictly to 127.0.0.1."),
                PlatformChangelogHighlight("Native Bedrock Dedicated Server (BDS - Beta): Experimental support for Mojang's official Bedrock Dedicated Server with Bedrock-tailored diagnostics."),
                PlatformChangelogHighlight("NeoForge Java Runtime Fix: Correctly maps NeoForge 21.x to Java 21 and added a manual Java version selector (8, 17, 21, 25) in Server Settings.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.5.1",
            date = "November 11, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Project Supporters & Community Links: Updated Project Supporters recognition across Credits and About for Patreon, YouTube, and Buy Me a Coffee members, added a direct Buy Me a Coffee button on Credits, and unified Discord community invite links."),
                PlatformChangelogHighlight("UI & Layout Polish: Updated the About screen supporters section to wrap chips cleanly across rows and refined support button spacing.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.5.0",
            date = "October 29, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Universal Port Forwarding: Zero-config UPnP and NAT-PMP router port mapping so friends can connect directly via WAN IP without third-party tunnel proxies."),
                PlatformChangelogHighlight("Web Remote Admin Panel: Manage your servers, terminal console, and files from a desktop browser or tablet over Wi-Fi or public tunnels with secure authentication."),
                PlatformChangelogHighlight("Cloud Backup Sync (F-Droid Compliant): Automated off-device world backup sync to Nextcloud, ownCloud, WebDAV, or cloud folders with zero proprietary SDKs."),
                PlatformChangelogHighlight("Modrinth Modpack Importer: 1-tap download and installation of official Modrinth .mrpack modpacks with automatic dependency resolution and config extraction."),
                PlatformChangelogHighlight("Live Telemetry & Diagnostics: Real-time rolling Canvas TPS and MSPT performance charts, proactive lag diagnostics, and 1-tap Aikar's optimized G1GC flags."),
                PlatformChangelogHighlight("UI & Layout Polish: Enhanced wizard EULA responsiveness, high-contrast mod toggles, and dynamic telemetry chart colors.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.4.2",
            date = "October 29, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight(
                    text = "x86_64 Emulator & PRoot Reliability: Fixed OpenJDK installation failures and eliminated SELinux permission denial floods under PRoot on Android x86_64 emulators and devices.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight("Runtime Setup & Console Diagnostics: First-time setup now streams live OpenJDK installation logs and surfaces any installation errors with retry controls, while server console buffers persist across startups and errors."),
                PlatformChangelogHighlight("Device-Aware RAM Allocation: Server creation wizard now automatically scales default and maximum RAM preselection to leave memory headroom for the host OS."),
                PlatformChangelogHighlight("Architecture & Performance: Streamlined codebase modularity across large composables and repositories, eliminated dead code paths, and optimized live console parsing.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.4.1",
            date = "October 20, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Minecraft EULA Opt-In: Server creation wizard now requires explicit opt-in to the Minecraft End User License Agreement before creating a server."),
                PlatformChangelogHighlight(
                    text = "Security & Backup Protection: Android automatic cloud backup is explicitly disabled and extraction rules configured to prevent server world data and sensitive credentials from being backed up.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight("Network & Startup Privacy: Automatic GitHub release update check at startup is now disabled by default with on-demand user toggle in App Settings."),
                PlatformChangelogHighlight("Native Tunnel Submodules: Integrated Playit and Bore native Rust source repositories as pinned git submodules for enhanced build reproducibility."),
                PlatformChangelogHighlight("F-Droid Review Compliance: Dropped unofficial archive mirror fallbacks in Mojang version resolution and standardized repository licensing under GPL-3.0.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.4.0",
            date = "October 2, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("High-Performance Native Tunneling: Playit.gg and Bore tunnel agents run directly as native host processes, reducing public tunnel latency."),
                PlatformChangelogHighlight("Minecraft Forge & Legacy Support: Run Forge modded servers with automatic installer execution, alongside full support for historical Minecraft releases down to version 1.0 with automated Java 8 runtime mapping."),
                PlatformChangelogHighlight("In-App Zip Extraction & Batch Import: Unpack zip archives directly in the file manager with subfolder routing and optional archive cleanup, plus multi-file batch imports for plugins and mods."),
                PlatformChangelogHighlight("Visual Player & Whitelist Management: Full player management for Whitelists, Operators, Banned Players, and Banned IPs."),
                PlatformChangelogHighlight("Server Settings & RAM Reallocation: Adjust allocated RAM memory on existing servers directly within Settings with live memory guidance, alongside automatic port collision detection."),
                PlatformChangelogHighlight("Engine Reliability & Architecture: Modular ServerSoftwareManager lifecycle, automated live map upgrades, and unified OkHttp networking.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.3.1",
            date = "September 17, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Standby Auto-Wake on Ping: Servers automatically start up on player ping across both Java Edition (TCP) and Bedrock Edition (RakNet) with dynamic port binding."),
                PlatformChangelogHighlight("World Backup Deletion & Retention: Delete individual backups directly in the app and set automatic retention limits on scheduled backup tasks to automatically prune older backups."),
                PlatformChangelogHighlight("Live Web Map Full World Render: Render complete world map tiles across all dimensions (Overworld, Nether, and The End)."),
                PlatformChangelogHighlight("Performance & Reliability: Centralized network clients, O(1) terminal scrollback memory buffers, and scoped file descriptors.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.3.0",
            date = "September 10, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Smart Idle Auto-Shutdown: Automatically stops running Minecraft servers after N minutes of 0 connected players."),
                PlatformChangelogHighlight("Auto-Wake on Ping: Standby socket listener on the server port (TCP for Java, UDP RakNet for Bedrock). When a player queries or joins from their Minecraft server list, the server spins up automatically."),
                PlatformChangelogHighlight("Dedicated Automation Tab: New tab in Server Details providing configuration for Smart Sleep, Auto-Wake on Ping, and active scheduler management."),
                PlatformChangelogHighlight("Flexible Scheduled Tasks & Cron Engine: Automated background timer engine supporting intervals, daily schedules, weekly schedules, and 5-field cron syntax."),
                PlatformChangelogHighlight("Upstream Server Build Updates & In-Place Version Upgrades: Upgrade existing servers to newer Minecraft versions or builds directly from Settings with automated safety backups."),
                PlatformChangelogHighlight("World Import/Export, Dimension Reset & Chunk Pruning: Pure Kotlin Anvil (.mca) optimizer that deletes uninhabited chunks to shrink world storage footprint."),
                PlatformChangelogHighlight("Embedded Live Web Map (Squaremap): Dedicated Live Map tab with 1-click Squaremap installation and uninstall support.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.2.0",
            date = "September 2, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Full In-App File Explorer & Code Editor: Directory navigation and built-in config editor for YAML, JSON, TOML, and properties files."),
                PlatformChangelogHighlight("Real-Time TPS & Engine Health Diagnostics: Live engine telemetry combining real-time CPU core utilization, tick duration (MSPT), tick budget headroom, and passive overload detection."),
                PlatformChangelogHighlight("Playit.gg Claim & Connection Reliability: Automatic claim URL detection, ANSI escape code filtering, and reliable public address resolution.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.1.0",
            date = "August 28, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Public Tunneling & Zero-Port-Forwarding: Support for both instant free tunnels (bore.pub) and persistent Playit.gg tunnels."),
                PlatformChangelogHighlight("GitHub Release Update Checker: Real-time update checks against GitHub Releases."),
                PlatformChangelogHighlight("Patreon Supporters Recognition: Added dedicated supporters recognition on the Credits and About screens.")
            )
        ),
        PlatformReleaseChangelog(
            version = "v1.0.0",
            date = "August 26, 2026",
            initialExpanded = false,
            highlights = listOf(
                PlatformChangelogHighlight("Multi-Engine Minecraft Server Creation & Management: Full support for PaperMC, PurpurMC, Folia, FabricMC, NeoForged, Mojang Vanilla, and Bedrock Geyser / Floodgate."),
                PlatformChangelogHighlight(
                    text = "Rootless PRoot Virtualization & Container Architecture: 100% rootless operation on unrooted Android devices using minimal Ubuntu 24.04 LTS rootfs container.",
                    platform = ChangelogPlatform.ANDROID
                ),
                PlatformChangelogHighlight("Multi-Version OpenJDK Isolation: Automatic installation and binding of Java 25, 21, 17, and 8."),
                PlatformChangelogHighlight("Context-Aware Plugins & Mods Management: Modrinth API integration with search, filters, metadata details, and direct .jar installation/importing."),
                PlatformChangelogHighlight("Interactive Live Terminal & Performance Telemetry: Live console output stream with command prompt and real-time CPU %, Resident RAM, active player counts, and disk footprints.")
            )
        )
    )

    fun getChangelogForPlatform(platform: ChangelogPlatform): List<FilteredReleaseChangelog> {
        val entries = if (platform == ChangelogPlatform.LINUX) {
            // Linux Desktop & Headless launched in v1.8.0; omit pre-1.8.0 Android-only releases
            FULL_CHANGELOG_HISTORY.takeWhile { it.version != "v1.7.0" }
        } else {
            FULL_CHANGELOG_HISTORY
        }
        return entries.mapNotNull { release ->
            val matching = release.highlights
                .filter { it.platform.matches(platform) }
                .map { it.text }
            if (matching.isEmpty()) {
                null
            } else {
                FilteredReleaseChangelog(
                    version = release.version,
                    date = release.date,
                    initialExpanded = release.initialExpanded,
                    highlights = matching
                )
            }
        }
    }

    fun getCreditsForPlatform(
        platform: ChangelogPlatform,
        category: SharedCreditCategory = SharedCreditCategory.ALL
    ): List<SharedSoftwareCredit> {
        return SOFTWARE_CREDITS.filter { credit ->
            credit.platform.matches(platform) &&
                (category == SharedCreditCategory.ALL || credit.category == category)
        }
    }
}
