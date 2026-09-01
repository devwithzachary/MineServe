# Changelog

All notable changes to the MineServe project will be documented in this file.

## [1.2.0] - 2026-09-01

### ⚡ Real-Time TPS, Engine Health & Local Timezone
- **Local Timezone Console Synchronization**: Synced the container environment (`/etc/timezone`, `/etc/localtime`, `TZ` environment variable) and JVM system properties (`-Duser.timezone`) with the host device's local timezone so Minecraft server logs and console timestamps match local time instead of UTC.
- **Dynamic TPS & MSPT Calculation**: Replaced static TPS metrics with live game engine telemetry combining real-time CPU core utilization, tick duration (MSPT), tick budget headroom, and passive console overload detection (`Can't keep up!`, `/tps`, `/tick query`).
- **Engine Health Diagnostics Card**: Added dedicated diagnostics card on the Server Performance tab with live TPS gauge, MSPT processing time, tick budget headroom, and logged overload warnings.

### 🧭 Navigation & Gesture Usability
- **System Back Gesture Navigation**: Integrated Compose `BackHandler` and backstack tracking across all app views (About, Credits, Settings, Server Details, and Create Server Wizard). Performing the edge-swipe back gesture or tapping hardware back now smoothly navigates back to previous screens instead of exiting the app.

### 🌐 Open Source Tunneling Credits & Recognition
- **In-App Tunneling Credits Directory**: Added dedicated **Tunneling** category to the Credits screen recognizing **bore (bore.pub)** and **Playit.gg** for powering zero-port-forwarding public multiplayer.
- **Open Source Attribution**: Updated README documentation and in-app software directory with license metadata, official links, and architectural descriptions for all integrated tunneling proxies.

## [1.1.0] - 2026-08-28

### 🌐 Public Tunneling & Zero-Port-Forwarding (CGNAT & Cellular Multiplayer)
- **Dual Provider Tunnel Architecture**: Full support for both **Instant Free Tunnel (bore.pub)** and **Playit.gg** persistent tunnels.
- **Playit.gg Native Tunnel Integration**: Embedded Playit agent management supporting instant setup via claim URLs or custom Playit Secret keys with persistent subdomains (e.g. `*.ply.gg`, `*.joinmc.link`).
- **1-Tap Playit.gg Claiming**: Interactive claim banner and status badge when starting Playit.gg tunnels for the first time, opening the setup link directly in the browser to link and activate custom subdomains.
- **Dismissable Security Notice**: Embedded 1-tap dismissable security warning alerts across the network card, share modal, and server settings explaining that public tunnels expose the port over the Internet and recommending server whitelisting to protect against griefers.
- **Graceful Disconnection Handling**: Turning off public tunneling disconnects sockets cleanly and transitions state to Offline without throwing false "Socket closed" error toasts.
- **Tunnel Customization in Server Settings**: Instant provider switching (`bore.pub`, `Playit.gg`, custom bore relays) with auto-saving, secret key management, dashboard linking, and auto-start configuration.

### 📱 Bedrock & Java Dynamic QR Code & Deep-Link Sharing
- **Interactive Server Share Modal**: Dedicated share sheet featuring dynamic QR codes and connection details for both **Public Online Link (Tunnel)** and **Local Wi-Fi (LAN)** networks.
- **Bedrock 1-Tap Import**: Automatically builds `minecraft://?addExternalServer=` deep links. Mobile players can tap "Join Bedrock" or scan the QR code to immediately launch Minecraft Bedrock and import the server.
- **Java Edition Quick Connection**: 1-tap copy of the public or LAN address for direct connection in Minecraft Java Edition.
- **System Share Sheet Integration**: Formatted multi-platform invite text exportable to Discord, WhatsApp, Telegram, SMS, and other messaging apps.

### ⚡ Interactive Console & Terminal Scrollback Enhancements
- **Smooth Terminal Scrollback Navigation**: Drag up and down on the live terminal canvas to review historical Minecraft server startup logs and execution output with smooth scrolling.
- **Scroll to Bottom Indicator**: Floating jump-to-bottom badge displaying the current scroll depth offset with 1-tap return to real-time logs.
- **Native Touch Word Detection & Selection**: Long-press on any log output or command argument to automatically select word boundaries with tactile haptic feedback.
- **Draggable Teardrop Selection Handles**: Fine-tune multi-line text selection ranges using interactive teardrop touch handles with live haptic tick feedback.
- **Visual Character Highlighting**: High-contrast theme-aware text highlight overlays across single-line and multi-line selection bounding boxes.
- **Floating Action Toolbar**: 1-tap floating toolbar supporting:
  - **Copy**: Instantly copy selected log text to the Android system clipboard.
  - **Select All**: Select the entire visible viewport buffer.
  - **Share**: Export and share log selections directly to external apps via Android's share sheet.
  - **Clear**: Dismiss the active selection overlay.

### 🔄 GitHub Release Update Checker & Play Store Migration Guidance
- **Automated GitHub Update Checks**: Real-time checking against the GitHub Releases API to notify users immediately when new releases are published without waiting for store approval delays.
- **Google Play vs GitHub Certificate Advisory**: In-app migration guidance clarifying Android package signing certificate differences between Google Play builds and direct GitHub APKs, providing clear steps for 1-tap backup creation and transition.
- **App Updates Configuration**: Dedicated settings section in App Settings to toggle automatic GitHub update notifications and manually trigger update checks anytime with live status reporting.

### ❤️ Community & Supporters
- **Patreon Supporters Recognition**: Dedicated supporters section on the **Credits** and **About** screens celebrating Patreon supporters (**Old PC Gunk (and stuff)**, **насэр Хорр**) who fuel ongoing MineServe development.

## [1.0.0] - 2026-08-26

### 🎮 Multi-Engine Minecraft Server Creation & Management
- **Universal Engine Support**: Full support for downloading, configuring, and executing 7 distinct server types:
  - **PaperMC**: High-performance, low-latency, and stable server engine with rich plugin compatibility.
  - **PurpurMC**: Drop-in replacement for Paper with extensive gameplay configurability and performance patches.
  - **Folia**: Cutting-edge regionized multithreading Minecraft server software by PaperMC.
  - **FabricMC**: Modular, lightweight modding toolchain and server environment.
  - **NeoForged**: Modern community-driven modding API and server platform.
  - **Mojang Vanilla**: Official standalone Minecraft server software from Mojang Studios.
  - **Bedrock Geyser / Floodgate**: Cross-play protocol translation proxy enabling Bedrock players to connect directly to Java servers.
- **Smart 4-Step Server Wizard**:
  - Step 1: Server Type & Engine selection.
  - Step 2: Minecraft Version selection (queried dynamically from official Mojang, Paper, Purpur, Fabric, and NeoForge APIs).
  - Step 3: Hardware & Configuration (Name, Port, RAM allocation, MOTD).
  - Step 4: Review, automated download, and instance initialization.
- **Smart Port Allocation & Collision Protection**: Automatically detects existing server ports and selects the next available port (starting at `25565`). Displays interactive warning banners when a chosen port conflicts with another server.

### 🔒 Rootless PRoot Virtualization & Container Architecture
- **100% Rootless Operation**: Intercepts and rewrites system calls using native PRoot `ptrace` system call interception. Runs full Linux user-space binaries on unrooted Android devices.
- **Ubuntu 24.04 LTS (Noble) Base Container**: Minimal rootfs container providing standard GNU/Linux filesystem hierarchy, APT package manager, and native OpenJDK environments.
- **JNI POSIX Pseudo-Terminal (PTY)**: Native C++ pseudo-terminal bridge (`pty.cpp`) allocating POSIX PTYs via `posix_openpt()`, managing window dimensions, ANSI escape code sequencing, and standard I/O streaming.

### ☕ Multi-Version OpenJDK Isolation & Runtime Manager
- **Dynamic Java Version Selector**: Automatic detection and 1-tap installation of OpenJDK packages inside the container:
  - **Java 25**: Early-access OpenJDK 25 runtime environment.
  - **Java 21**: Standard LTS runtime for Minecraft 1.20.5+ and modern snapshots.
  - **Java 17**: Standard LTS runtime for Minecraft 1.17 through 1.20.4.
  - **Java 8**: Legacy runtime for Minecraft 1.12.2 and older server versions.
- **Per-Server Java Version Binding**: Executes each Minecraft server with the exact OpenJDK binary required by its Minecraft version.

### 🧩 Context-Aware Plugins & Mods Management
- **Engine-Aware Separation**:
  - **Vanilla Servers**: Plugins and Mods tabs are completely hidden.
  - **Paper / Purpur / Folia / Bedrock**: Dedicated **"Plugins"** tab managing `.jar` files in `plugins/`.
  - **Fabric / NeoForge**: Dedicated **"Mods"** tab managing `.jar` files in `mods/`.
- **Modrinth API Integration**:
  - Loader-scoped searches (`project_type=plugin` for Paper/Purpur, `categories=fabric` for Fabric, `categories=neoforge,forge` for NeoForge).
  - Project icon and thumbnail rendering via Coil image loading.
  - Rich Project Details modal displaying authors, download counts, category tags, summary, and full markdown body descriptions.
- **1-Tap Installation Flow**: Automatically matches version files with the target server's loader and Minecraft version, downloading `.jar` files with progress tracking.
- **Custom `.JAR` Upload / Import**: Launch Android's system Document Picker (SAF) to import `.jar` files directly from internal device storage or Downloads into the server's `plugins/` or `mods/` directory.

### ⚡ Interactive Live Terminal & Command Console
- **Live Colored Output Stream**: High-throughput terminal view rendering colorized Minecraft server logs, ANSI escape codes, and timestamps.
- **Interactive Stdin Delivery**: Direct standard input command prompt supporting all Minecraft server commands (`op <player>`, `deop <player>`, `gamemode creative <player>`, `whitelist add <player>`, `say <message>`, `stop`).
- **Terminal Controls**: 1-tap Clear Buffer action, auto-scroll to latest log output, and instant Server Stop / Restart buttons.

### 📊 Performance Monitoring & Live Telemetry
- **Dashboard Telemetry Overview**: Live metrics tracking Total Servers, Active Online Servers, and Combined RAM consumption.
- **Per-Server Performance Tab**: Real-time monitoring of CPU utilization (%), Resident RAM (RSS in MB via `/proc`), allocated memory, and active player counts.
- **Disk Footprint Analytics**: Per-server disk storage calculation and global rootfs container footprint tracking in Settings.

### 🛡️ Persistent Foreground Execution & WakeLock
- **Android Foreground Service**: Runs the server process inside a persistent Foreground Service holding a partial CPU `WakeLock`, ensuring 24/7 server uptime without interruption from Android Doze, task switching, or screen lock.
- **Interactive Notification**: Real-time notification shade card showing running server count, active RAM utilization, and quick 1-tap **Stop All Servers** action.
- **Screen Keep-Awake Setting**: Optional setting to prevent display sleep when actively watching the live terminal console.

### ⚙️ Server Configuration & Raw Text File Editor
- **Visual Property Management**: Toggle switches and sliders for Server Port, MOTD, Max Players, Game Mode (Survival/Creative/Adventure/Spectator), Difficulty (Peaceful/Easy/Normal/Hard), PVP, Whitelist, View Distance, Animal/Monster Spawning, and Simulation Distance.
- **Raw Configuration Text Editor**: Built-in in-app text document editor for advanced configuration files (`server.properties`, `paper-global.yml`, `purpur.yml`, `bukkit.yml`, `spigot.yml`, `eula.txt`).

### 💾 World & Server Snapshot Backup System
- **Two Backup Modes**:
  - **World Save Backup**: Rapid archive of only the `world`, `world_nether`, and `world_the_end` folders.
  - **Full Server Backup**: Complete snapshot of the entire server directory (config, plugins, mods, logs, worlds).
- **1-Tap Rollback Restore**: Immediate extraction and rollback restoration of previous backups.
- **Export to Storage & Sharing**: 1-tap export to the public Android `Downloads/` directory and Android system Share Sheet.

### 🌐 Persistent LAN Address & Multiplayer Connection
- **Persistent Network Address Card**: Embedded LAN address card (`<ip>:<port>`) placed persistently above all server detail tabs with a 1-tap clipboard copy button.
- **How to Connect Guide**: Integrated connection guide for Wi-Fi local multiplayer, Phone Hotspot on-the-go play, and Public Internet tunneling (Playit.gg / ngrok).

### 🎨 Modular UI, About & Credits Pages
- **Reorganized Navigation**: Top bar quick-access buttons for Refresh, Credits, About, and Settings.
- **Dedicated About Page**: Support cards (Patreon, Buy Me a Coffee), Discord community invite, GitHub Issues / Feedback link, and System & Build Metadata diagnostics (Android OS, API level, ABI, Device Model, PRoot version, Ubuntu rootfs).
- **Dedicated Credits Page**: Categorized open-source software and server engine directory with interactive filter chips, license badges, external project links, and Community Contributors & Pull Requests recognition.
