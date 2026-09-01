# MineServe

[![Google Play](https://img.shields.io/badge/Google_Play-Get%20it%20on%20Google%20Play-414141?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.devwithzachary.mineserve)
[![Discord Community](https://img.shields.io/badge/Discord-Join%20Community-5865F2.svg?logo=discord&logoColor=white)](https://discord.gg/csGrrg5MGF)
[![Android MinSDK](https://img.shields.io/badge/Min%20SDK-26%20%28Android%208.0%2B%29-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-purple.svg)](https://developer.android.com/jetpack/compose)
[![Java Environments](https://img.shields.io/badge/Java%20Runtimes-Java%208%20%7C%2017%20%7C%2021%20%7C%2025-orange.svg)](#isolated-java-runtime-environments)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64%20%7C%20x86__64%20%7C%20ARMv7-orange.svg)](#multi-architecture-support)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

**MineServe** is an open-source Android application designed to download, configure, run, and manage full-featured dedicated Minecraft servers natively on Android devices **without requiring root permissions**.

Powered by a native **PRoot** virtualization engine, a JNI-backed **PTY pseudo-terminal**, multi-version **OpenJDK runtime isolation (Java 8, 17, 21, 25)**, live telemetry monitoring, and a modern **Jetpack Compose** interface, MineServe turns your smartphone or tablet into a portable, high-performance Minecraft dedicated server host.

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
* **⚡ Interactive Live Terminal & Console**: Built-in VT100/ANSI terminal emulator with direct standard input command delivery (`op`, `gamemode`, `whitelist`, `stop`), colored log streaming, instant buffer clearing, and automatic scroll-to-bottom.
* **📊 Live Telemetry & Resource Monitoring**: Real-time tracking of active container CPU utilization, resident RAM consumption (RSS via `/proc`), allocated memory, live player counts, and per-server storage footprints.
* **🧩 Context-Aware Plugins & Mods Management**:
  * Engine-aware UI: Displays **"Plugins"** for Paper/Purpur, **"Mods"** for Fabric/NeoForge, and automatically hides the tab for Vanilla servers.
  * **Modrinth API Integration**: Search and browse plugins and mods with project thumbnails, categories, author credits, and full descriptions.
  * **1-Tap Installation**: Automatically resolves version download URLs matching the target server's loader and Minecraft release.
  * **Custom `.JAR` Import**: Import plugins and mods directly from your Android device storage using the system Document Picker.
* **⚙️ Server Properties & Raw File Editor**:
  * **Visual Configuration**: Intuitive switches and sliders for Server Port, MOTD, Max Players, Game Mode, Difficulty, PVP, Whitelist, View Distance, Animal/Monster Spawning, and Simulation Distance.
  * **Raw Text Document Editor**: Direct in-app text editor for `server.properties`, `paper-global.yml`, `purpur.yml`, `bukkit.yml`, `spigot.yml`, and `eula.txt`.
* **💾 World & Server Snapshot Backups**:
  * **World Save Backup**: Rapid snapshot archiving of only the world save directory.
  * **Full Server Backup**: Complete backup of server configuration, plugins/mods, logs, and worlds.
  * **1-Tap Restore & Export**: Instant rollback restoration and export to Android public Downloads or system Share sheet.
* **🌐 Smart Port Allocation & Persistent LAN Card**:
  * Automatically suggests the first available port (starting at `25565`) when creating new servers and displays warnings for port conflicts.
  * Persistent LAN address card above server tabs (`<ip>:<port>`) with a 1-tap copy button for fast multiplayer connection sharing.
* **🛡️ Persistent Foreground Execution**: Runs inside an Android Foreground Service with CPU `WakeLock` protection, preventing Android Doze or battery optimizers from terminating your server when switching apps or locking your screen.

---

## 🛠️ How It Works (Technical Architecture)

```
+-----------------------------------------------------------------------+
|                    Android UI Layer (Jetpack Compose)                 |
|   DashboardScreen | ServerDetailScreen | CreateWizard | AppSettings   |
|   (ConsoleTab | PlayersTab | BackupsTab | PluginsTab | PropertiesTab) |
+-----------------------------------------------------------------------+
                                   |
                                   v
+-----------------------------------------------------------------------+
|                         Kotlin Engine Core                            |
|    MainViewModel | ServerProcessManager | JavaRuntimeManager          |
|    ServerRepository | RootfsManager | PRootEngine | ModrinthApiClient |
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

MineServe provisions isolated OpenJDK runtimes inside the container filesystem and dynamically passes the correct binary path (`/usr/lib/jvm/java-<version>-openjdk-arm64/bin/java`) when executing each server.

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
| **Ubuntu Base** | Official root filesystem tarball providing the Linux container environment. | [Canonical Ltd.](https://cdimage.ubuntu.com/ubuntu-base/) / Canonical |
| **Modrinth API** | Public REST API for discovering and downloading Minecraft plugins and mods. | [Modrinth](https://modrinth.com/) / AGPL-3.0 |
| **OkHttp & Coil** | High-performance HTTP client and image loading engine for Compose. | [Square](https://square.github.io/okhttp) & [Coil](https://coil-kt.github.io/coil) / Apache-2.0 |

---

## 📥 Download & Installation

* **Google Play Store**: Install directly with automatic updates from [Google Play](https://play.google.com/store/apps/details?id=com.devwithzachary.mineserve).
* **GitHub Releases**: Download standalone signed APK packages directly from [GitHub Releases](https://github.com/devwithzachary/MineServe/releases).

---

## 🛠️ Building from Source

### Prerequisites
* **Android Studio**: Ladybug (2024.2.1) or newer recommended.
* **JDK**: Java 17.
* **Android NDK**: Version 25 or higher (configured for C++ CMake compilation of `pty.cpp`).

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
2. Select your desired server engine (Paper, Purpur, Fabric, NeoForge, Vanilla, Bedrock).
3. Select the Minecraft version and assign RAM (e.g. 2048 MB).
4. MineServe automatically assigns an unused port (e.g. `25565`) and sets up `server.properties` and `eula.txt`.
5. Tap **Download & Build Server**.

### 3. Managing the Server
* **Console Tab**: View live colored logs and send commands (e.g., `op username` or `whitelist add friend`).
* **Plugins / Mods Tab**: Search Modrinth for plugins (Paper/Purpur) or mods (Fabric/NeoForge) and install them with 1 tap, or upload custom `.jar` files.
* **Backups Tab**: Create full server or world-only snapshot zip archives and export them to your Downloads folder.
* **Properties Tab**: Toggle server rules or switch to the raw configuration file editor.

---

## 🤝 Contributing & AI Policy

Contributions, bug reports, and feature requests are warmly welcomed! Feel free to open an issue or submit a pull request.

Please review our **[AI Usage Policy](AI.md)** for guidelines regarding the use of AI coding assistants when contributing to this project. All submitted code must be thoroughly tested, verified, and personally owned by the human author.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** - see the [LICENSE](LICENSE) file for details. Included binaries (PRoot, talloc, libandroid-shmem) and server software remain under their respective open-source licenses.
