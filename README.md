# MineServe

[![Google Play](https://img.shields.io/badge/Google_Play-Get%20it%20on%20Google%20Play-414141?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.devwithzachary.mineserve)
[![Discord Community](https://img.shields.io/badge/Discord-Join%20Community-5865F2.svg?logo=discord&logoColor=white)](https://discord.gg/csGrrg5MGF)
[![Android MinSDK](https://img.shields.io/badge/Min%20SDK-23%20%28Android%206.0%2B%29-brightgreen.svg)](https://developer.android.com/about/versions/marshmallow)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Java Environments](https://img.shields.io/badge/Java%20Runtimes-Java%208%20%7C%2017%20%7C%2021%20%7C%2025-orange.svg)](#3-isolated-java-runtime-architecture-javaruntimemanagerkt)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64%20%7C%20x86__64%20%7C%20ARMv7-orange.svg)](#4-multi-architecture-support)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

**MineServe** is an open-source Android application designed to download, configure, run, and manage full-featured dedicated Minecraft servers natively on Android devices **without requiring root permissions**.

Powered by a native **PRoot** virtualization engine, a JNI-backed **PTY pseudo-terminal**, multi-version **OpenJDK runtime isolation (Java 8, 17, 21, 25)**, live telemetry monitoring, public zero-port-forwarding tunneling, smart auto-wake automation, and a modern **Jetpack Compose** interface, MineServe turns your smartphone or tablet into a portable, high-performance Minecraft dedicated server host.

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.devwithzachary.mineserve">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">
  </a>
</p>

> [!NOTE]
> **Active Development & Bug Reporting**: MineServe is under active development. If you discover any bugs, compatibility issues, or have feature suggestions, please [submit an issue on GitHub](https://github.com/devwithzachary/MineServe/issues)!

---

## 🚀 Key Features

* **🔒 100% Rootless Operation**: Executes entirely in Android user-space using PRoot ptrace system call interception. No root access, Magisk, or bootloader unlocking required.
* **🎮 Comprehensive Server Engine Support**:
  * **PaperMC**: High-performance, stable Minecraft server software with comprehensive Bukkit/Spigot/Paper plugin support.
  * **PurpurMC**: Drop-in Paper replacement with extensive gameplay configurability and performance tweaks.
  * **Folia**: Cutting-edge regionized multithreading server software for high player concurrency.
  * **FabricMC**: Lightweight, modular modding framework and server platform for modern versions and snapshots.
  * **NeoForged**: Modern community-driven modding API and server platform.
  * **Mojang Vanilla**: Official standalone Minecraft server software from Mojang Studios.
  * **Bedrock Geyser / Floodgate**: Seamless protocol translation proxy enabling Bedrock edition players on iOS, Android, and consoles to connect to your Java server.
* **☕ Isolated Java Runtime Environments**: Automatic detection and 1-tap installation of OpenJDK versions inside the container:
  * **Java 25 / Java 21**: For modern Minecraft 1.20.5+ and snapshots.
  * **Java 17**: For Minecraft 1.17 - 1.20.4.
  * **Java 8**: For legacy Minecraft 1.12.2 and older server versions.
* **🌐 Zero-Port-Forwarding Public Tunneling**:
  * **Dual Tunnel Providers**: Instant free TCP tunneling with **bore (`bore.pub`)** or persistent vanity subdomains (`*.ply.gg`, `*.joinmc.link`) with **Playit.gg**.
  * **Play Anywhere**: Host servers over mobile cellular data (4G/5G) or restrictive home Wi-Fi behind CGNAT without configuring router port forwarding.
  * **1-Tap Browser Claiming**: Interactive claim banners and direct Playit Secret key support with automatic reconnection.
* **📱 Dynamic QR Code & Cross-Play Deep-Link Sharing**:
  * Dynamic QR codes for both Public Online Link (Tunnel) and Local Wi-Fi (LAN) connections.
  * 1-tap Bedrock `minecraft://?addExternalServer=` deep links so friends can tap or scan to open Bedrock and import the server instantly.
  * Native system share sheet export for Discord, WhatsApp, Telegram, and SMS invites.
* **💤 Smart Idle Sleep & Auto-Wake on Ping**:
  * **Idle Auto-Shutdown**: Automatically suspends servers after N minutes of 0 connected players (configurable to 5m, 10m, 15m, 30m, 60m) to preserve battery life and prevent device heating.
  * **Auto-Wake on Ping**: Lightweight background standby listener on the server port (TCP for Java, UDP RakNet Unconnected Pong for Bedrock). When a player queries or joins from their in-game server list, MineServe automatically wakes and boots the server.
* **⏱️ Flexible Scheduled Tasks & Cron Engine**:
  * Background task automation for periodic world backups, full server backups, nightly server restarts, and custom in-game announcements.
  * Supports interval quick chips (minutes/hours), daily schedules, weekly day-of-week selections with interval weeks, and advanced 5-field cron syntax with live validation.
* **🗺️ Embedded Live Web Map (Squaremap)**:
  * Built-in 2D live web map hosted on `http://127.0.0.1:8080` and rendered directly inside the app using Jetpack Compose `WebView`.
  * 1-tap installation and removal of Squaremap with minimal mobile RAM overhead.
  * Full world rendering tool with Overworld, Nether, and The End dimension selection, plus real-time player tracking.
* **🌍 Advanced World Management & Chunk Storage Optimizer**:
  * **World Importer**: Direct `.zip` and `.mcworld` import from device storage or Google Drive with automated format detection, Java Anvil verification, and pre-import safety backups.
  * **World Archive Exporter**: Export complete world saves (Overworld, Nether, End) with Save to Storage and Android Share Sheet support.
  * **1-Tap Nether & End Dimension Reset**: Wipe and regenerate `world_nether` or `world_the_end` (`DIM-1` / `DIM1`) without affecting Overworld builds.
  * **Chunk Pruning & Storage Optimizer**: Pure Kotlin Anvil (`.mca`) region analyzer that deletes uninhabited chunks (`InhabitedTime == 0`) and purges empty region files to reclaim storage space.
* **📁 In-App File Explorer & Monospace Code Editor**:
  * Full interactive directory navigation for `/servers/{serverId}/` with search, upload, create, delete, rename, and duplicate operations.
  * Syntax-highlighted code editor for `.yml`, `.json`, `.properties`, `.toml`, and `.txt` files with line numbering, search and replace with match counters, and cursor position tracking.
* **🩺 Automated Crash Log Analyzer & Quick Fix Diagnostics**:
  * Automatically parses `crash-reports/` and `logs/latest.log` upon failure to diagnose root causes (Java version mismatch, Out of Memory / OOM, mod ID conflicts, port conflicts, unaccepted EULA).
  * Interactive diagnostic sheet providing 1-tap quick fixes to accept the EULA, switch Java runtimes, allocate RAM, or assign open ports.
* **🔄 Upstream Build Updates & In-Place Minecraft Version Upgrades**:
  * 1-tap checking and updating for PaperMC and Purpur upstream server builds while preserving worlds and configs.
  * In-place Minecraft version upgrades from Server Settings with automated pre-upgrade safety backups and automatic Java runtime requirement alignment.
* **⚡ Interactive Live Terminal & Console Enhancements**:
  * VT100/ANSI terminal emulator with direct standard input command delivery, colored log streaming, and scroll-to-bottom.
  * Customizable quick-command macro hotbar chips above the console with built-in macro editor.
  * Context-aware command auto-completion ribbon for `/` commands and subcommands.
  * Command history memory recall (Up and Down buttons).
  * Smooth terminal canvas scrollback with touch word detection, draggable teardrop selection handles, and floating action toolbar (Copy, Select All, Share).
* **📊 Real-Time Game Engine Telemetry & Health**:
  * Live tracking of dynamic TPS gauge, MSPT processing time, tick budget headroom, and passive overload warnings (`Can't keep up!`).
  * Active container CPU utilization, RSS memory consumption via `/proc`, live player counts, and server storage footprints.
  * Local timezone synchronization for container and JVM logs so console timestamps match host device time rather than UTC.
* **🧩 Context-Aware Plugins & Mods Management**:
  * Engine-aware UI: Displays **"Plugins"** for Paper/Purpur, **"Mods"** for Fabric/NeoForge, and automatically hides the tab for Vanilla servers.
  * **Modrinth API Integration**: Search and browse plugins and mods with project thumbnails, categories, author credits, and full descriptions.
  * **1-Tap Installation**: Automatically resolves version download URLs matching the target server's loader and Minecraft release.
  * **Custom `.JAR` Import**: Import plugins and mods directly from Android device storage using the system Document Picker.
* **⚙️ Server Properties & Visual Configuration**:
  * Intuitive switches and sliders for Server Port, MOTD, Max Players, Game Mode, Difficulty, PVP, Whitelist, View Distance, Animal/Monster Spawning, and Simulation Distance.
* **💾 World & Server Snapshot Backups**:
  * **World Save Backup**: Rapid snapshot archiving of only the world save directory.
  * **Full Server Backup**: Complete backup of server configuration, plugins/mods, logs, and worlds.
  * **1-Tap Restore & Export**: Instant rollback restoration and export to Android public Downloads or system Share sheet.
* **🌐 Smart Port Allocation & Persistent LAN Card**:
  * Automatically suggests the first available port (starting at `25565`) when creating new servers and displays warnings for port conflicts.
  * Persistent LAN address card above server tabs (`<ip>:<port>`) with a 1-tap copy button for fast multiplayer connection sharing.
* **🛡️ Persistent Foreground Execution**:
  * Runs inside an Android Foreground Service with CPU `WakeLock` protection, preventing Android Doze or battery optimizers from terminating your server when switching apps or locking your screen.

---

## 🛠️ How It Works (Technical Architecture)

```
+-----------------------------------------------------------------------+
|                    Android UI Layer (Jetpack Compose)                 |
|   DashboardScreen | ServerDetailScreen | CreateWizard | AppSettings   |
|   (Console | Perf | Automation | Files | World | LiveMap | Settings)  |
|   (Players | Backups | Plugins/Mods)                                  |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                         Kotlin Engine Core                            |
|    MainViewModel | ServerProcessManager | JavaRuntimeManager          |
|    ServerRepository | BackupRepository | PluginRepository             |
|    AutomationScheduler | StandbyPingListener | TunnelManager          |
|    ChunkOptimizer | CrashDiagnosticEngine | PRootEngine               |
+-----------------------------------------------------------------------+
             |                             |
             v                             v
+-----------------------+     +-----------------------------------------+
|  Native JNI Layer     |     |   PRoot Virtualization Engine           |
|  pty.cpp (Posix PTY)  |     |   libproot.so                           |
|  - posix_openpt()     |     |   - ptrace syscall interception         |
|  - grantpt/unlockpt   |     |   - Rootfs path isolation (-r)          |
|  - fork() & execve()  |     |   - Fake root user mapping (-0)         |
|  - Window resize      |     |   - Bind mounts (/dev, /proc, /sdcard)  |
+-----------------------+     +-----------------------------------------+
             |                             |
             +--------------+--------------+
                            |
                            v
+-----------------------------------------------------------------------+
|                  Guest Linux Container (Ubuntu Base)                  |
|     /usr/lib/jvm/java-{8,17,21,25}-openjdk-arm64                     |
|     /servers/{serverId}/                                              |
|     ├── server.jar (Paper / Purpur / Fabric / NeoForge / Vanilla)     |
|     ├── server.properties, eula.txt                                  |
|     ├── world/, world_nether/, world_the_end/                         |
|     └── plugins/ or mods/                                             |
+-----------------------------------------------------------------------+
```

### 1. PRoot Virtualization Engine (`libproot.so`)
PRoot uses the `ptrace` system call mechanism to intercept and rewrite system calls from guest Linux binaries (such as OpenJDK). It translates file paths on-the-fly, allowing Minecraft server JARs and Java runtimes to operate within standard Linux filesystem hierarchies (`/usr`, `/etc`, `/tmp`) while physically residing in Android's app-private data directory (`context.filesDir`).

### 2. Native PTY Terminal Subsystem (`pty.cpp`)
Interactive server consoles require a Unix pseudo-terminal (PTY) to handle window dimensions (`TIOCSWINSZ`), process signals, ANSI escape color sequencing, and unbuffered standard I/O streaming. The native C++ layer allocates POSIX PTYs via `posix_openpt()` and launches OpenJDK child processes via `fork()` and `execve()`.

### 3. Isolated Java Runtime Architecture (`JavaRuntimeManager.kt`)
Different Minecraft versions require specific OpenJDK bytecode compatibility:
* Minecraft 1.20.5+ requires **Java 21** or **Java 25**.
* Minecraft 1.17 - 1.20.4 requires **Java 17**.
* Minecraft 1.12.2 and older requires **Java 8**.

MineServe provisions isolated OpenJDK runtimes inside the container filesystem and dynamically passes the correct binary path (`/usr/lib/jvm/java-<version>-openjdk-<arch>/bin/java`) when executing each server.

### 4. Multi-Architecture Support
MineServe compiles native virtualization libraries and bridges across multiple Android ABIs:
* **ARM64 (`arm64-v8a`)**: Primary architecture for modern 64-bit Android smartphones and tablets.
* **x86_64**: Optimized for 64-bit Android emulators, ChromeOS devices, and Intel/AMD hardware.
* **ARMv7 (`armeabi-v7a`)**: Legacy 32-bit architecture support for older Android devices.

---

## 📦 Open Source Credits & Components

MineServe is built on the shoulders of incredible open-source projects:

| Component / Project | Description & Purpose | License / Source |
| :--- | :--- | :--- |
| **PRoot (`libproot.so`)** | User-space `chroot`, `mount --bind`, and root emulation engine. | [PRoot Project](https://proot-me.github.io/) / GPL-2.0 |
| **LinuxOnAndroid Project** | PRoot virtualization runtime, terminal bridge, and Android system foundation. | [LinuxOnAndroid](https://github.com/devwithzachary/LinuxOnAndroid) / GPL-3.0 |
| **PaperMC & Folia** | High-performance and multithreaded Minecraft server engines. | [PaperMC](https://papermc.io/) / GPL-3.0 |
| **PurpurMC** | Highly configurable drop-in replacement for Paper. | [PurpurMC](https://purpurmc.org/) / MIT |
| **FabricMC** | Modular, lightweight modding toolchain and server environment. | [FabricMC](https://fabricmc.net/) / Apache-2.0 |
| **NeoForged** | Modern community-driven modding API and server platform. | [NeoForged](https://neoforged.net/) / LGPL-2.1 |
| **GeyserMC & Floodgate** | Protocol translation proxy enabling Bedrock players to join Java servers. | [GeyserMC](https://geysermc.org/) / MIT |
| **Squaremap** | Lightweight, ultra-fast 2D live web map with Leaflet player tracking. | [Squaremap](https://github.com/jpenilla/squaremap) / MIT |
| **Ubuntu Base** | Official root filesystem tarball providing the Linux container environment. | [Canonical Ltd.](https://cdimage.ubuntu.com/ubuntu-base/) / Canonical |
| **Modrinth API** | Public REST API for discovering and downloading Minecraft plugins and mods. | [Modrinth](https://modrinth.com/) / AGPL-3.0 |
| **bore (`bore.pub`)** | Modern, zero-config TCP tunneling tool enabling instant public port forwarding. | [bore](https://github.com/ekzhang/bore) / MIT |
| **Playit.gg** | Global game server tunneling proxy network providing persistent subdomains. | [Playit.gg](https://playit.gg) / MIT |
| **OkHttp & Coil** | High-performance HTTP client and image loading engine for Compose. | [Square](https://square.github.io/okhttp) & [Coil](https://coil-kt.github.io/coil) / Apache-2.0 |
| **KotlinX Coroutines & Serialization** | Asynchronous coroutines and multiplatform JSON serialization for Kotlin. | [JetBrains](https://github.com/Kotlin) / Apache-2.0 |

### 💖 Patreon Supporters
Special thanks to our generous Patreon supporters whose contributions help fuel ongoing MineServe development:
* **Old PC Gunk (and stuff)**
* **насэр Хорр**

---

## 📥 Download & Installation

* **Google Play Store**: Install directly with automatic updates from [Google Play](https://play.google.com/store/apps/details?id=com.devwithzachary.mineserve).
* **GitHub Releases**: Download standalone signed APK packages directly from [GitHub Releases](https://github.com/devwithzachary/MineServe/releases).
* **F-Droid**: Build recipe and metadata configured under `com.devwithzachary.mineserve.yml`.

---

## 🛠️ Building from Source

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer recommended.
* **JDK**: Java 21 (or Java 17+).
* **Android NDK**: Version 28 (specifically `28.2.13676358` configured in `app/build.gradle.kts` for C++ compilation of `pty.cpp`).

### Build Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/devwithzachary/MineServe.git
   cd MineServe
   ```

2. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected device via ADB**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📖 Quick Usage Guide

### 1. First-Time Setup
On initial launch, tap **Initialize Server Runtime**. MineServe will download and unpack the minimal Ubuntu base container and prepare the OpenJDK environment.

### 2. Creating a Server
1. Tap the **+** button on the Dashboard.
2. Select your desired server engine (Paper, Purpur, Folia, Fabric, NeoForge, Vanilla).
3. Select the Minecraft version and assign RAM (e.g. 2048 MB).
4. MineServe automatically assigns an unused port (e.g. `25565`) and sets up `server.properties` and `eula.txt`.
5. Tap **Download & Build Server**.

### 3. Managing the Server
The Server Details screen provides dedicated tabs to manage every aspect of your server:
* **Console Tab**: View live colored terminal logs, use customizable macro hotbar buttons, and execute Minecraft commands with auto-completion.
* **Performance Tab**: Monitor real-time TPS gauges, MSPT processing time, tick budget headroom, and live CPU/RAM utilization.
* **Automation Tab**: Configure idle auto-shutdown, arm auto-wake on ping standby listeners, and set up cron-scheduled backups and restarts.
* **Files Tab**: Browse server files, edit configs in the monospace code editor, or inspect crash reports with the 1-tap Diagnostic Sheet.
* **World Tab**: Import singleplayer `.zip` or `.mcworld` saves, export world archives, reset Nether or End dimensions, and optimize chunk storage.
* **Live Map Tab**: Explore your world with an embedded 2D Squaremap web view and trigger full world renders.
* **Settings Tab**: Adjust server rules (PVP, difficulty, max players), manage Java runtime versions, update builds, and configure public tunneling (bore.pub or Playit.gg).
* **Players Tab**: View connected players, manage operator permissions, and kick or ban players directly from the UI.
* **Backups Tab**: Create full server or world-only snapshot zip archives and export them to your Downloads folder or external apps.
* **Plugins / Mods Tab**: Search Modrinth for plugins (Paper/Purpur) or mods (Fabric/NeoForge) and install them with 1 tap, or upload custom `.jar` files.

---

## 🤝 Contributing & AI Policy

Contributions, bug reports, and feature requests are warmly welcomed! Feel free to open an issue or submit a pull request.

Please review our **[AI Usage Policy](AI.md)** for guidelines regarding the use of AI coding assistants when contributing to this project. All submitted code must be thoroughly tested, verified, and personally owned by the human author.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** - see the [LICENSE](LICENSE) file for details. Included binaries (PRoot, talloc, libandroid-shmem) and server software remain under their respective open-source licenses.
