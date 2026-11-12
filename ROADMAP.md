# MineServe Project Roadmap

This document outlines the strategic product roadmap and feature milestones planned for upcoming releases of **MineServe**.

Our goal is to deliver the most reliable, capable, and accessible dedicated Minecraft server hosting platform - starting as a 100% rootless, privacy-respecting Android app and evolving by **v2.0.0** into a full **Kotlin Multiplatform (KMP)** ecosystem targeting **Android, Linux, macOS, and Windows** with both native UIs, a full-featured Web UI, and headless desktop server mode.

---

## 🗺️ Roadmap Status & Release Schedule

| Version | Focus / Theme | Target Milestone | Status |
| :--- | :--- | :--- | :--- |
| **v1.4.2** | Android x86_64 PRoot Reliability & Codebase Modularity | October 2026 | **Shipped** ✅ |
| **v1.5.0 / v1.5.1** | Connectivity, Cloud Backups, Web Remote Admin, Telemetry & Community | October / November 2026 | **Shipped** ✅ |
| **v1.6.0** | Full Feature-Parity Web UI, Bedrock Dedicated Server, Proxies & Discord Bridge | November 2026 | **Shipped** ✅ |
| **v1.7.0** | `:core` KMP Module Extraction, World Mastery, NBT Editor & Android Integration | Q2 2027 | **Scheduled** 📋 |
| **v1.8.0** | Linux Beta (Native UI & Headless Mode), Distro Packaging & Mesh VPN | Mid 2027 | **Scheduled** 📋 |
| **v1.9.0** | Stable Linux, macOS & Windows Betas & Brand New Multi-Platform Website | Late 2027 | **Scheduled** 📋 |
| **v2.0.0** | Stable Kotlin Multiplatform Release (Android, Linux, macOS, Windows) | Late 2027 / Early 2028 | **Target Milestone** 🎯 |
| **v2.1.0+** | Autonomous Ops, Clustering, Bare-Metal Bionic & Multi-Game Hosting | Future Vision | **Exploration** 🔭 |

---

## 🚀 Version 1.5.0: Connectivity, Cloud Backups & Telemetry

The 1.5.0 milestone concentrates on expanding connection options beyond third-party tunnels, safeguarding world saves with off-device cloud backups, streamlining modding workflows, and delivering deeper real-time performance insights.

### 🌐 1. Universal Port Forwarding & Networking
- **UPnP & NAT-PMP Automatic Router Port Forwarding**:
  - Integrated zero-config UPnP/NAT-PMP port mapper to automatically map external router ports (e.g. 25565) to the Android device.
  - Allows friends on the internet to connect directly to your home server without requiring manual router admin access or third-party tunnel services.
- **Cloudflare Tunnel (`cloudflared`) Integration**:
  - Support running native `cloudflared` for users with custom domains.
  - Zero-port-forwarding TCP proxying through Cloudflare's edge network with custom subdomains.
- **Dynamic DNS (DDNS) Client Integration**:
  - Built-in lightweight DDNS updater supporting DuckDNS, No-IP, and custom webhook endpoints for dynamic residential home IP addresses.

### ☁️ 2. Cloud Backup Sync & Off-Device Redundancy (F-Droid Compliant)
- **F-Droid Compliant Storage Architecture**:
  - Implemented strictly with 100% open-source protocols (OkHttp REST, WebDAV, Android Storage Access Framework) without proprietary closed-source Google Play Services client libraries.
- **WebDAV & Nextcloud Storage Provider**:
  - Direct world export and scheduled backup synchronization to self-hosted Nextcloud, ownCloud, or generic WebDAV endpoints.
- **Google Drive via Open REST & Android SAF**:
  - Seamless automated upload of `.zip` world backups and full server archives to a user-selected Google Drive folder using standard open OAuth2 REST endpoints and Android Storage Access Framework.
- **Pre-Upgrade Auto-Cloud Backup**:
  - Automatically dispatch an off-device backup prior to server software version migrations or dimension resets.

### 🧩 3. Modpack (.mrpack) & Batch Plugin Installer
- **Modrinth `.mrpack` 1-Tap Modpack Importer**:
  - Direct import and extraction of official Modrinth `.mrpack` modpacks for Fabric, NeoForge, and Forge.
  - Automatically downloads required server-side mods, configuration overrides, and modloader versions.
- **Batch Mod & Plugin Dependency Resolver**:
  - Automatic detection and prompt to install required dependencies (e.g. Fabric API for Fabric mods, Floodgate for Geyser).

### 📊 4. Live Performance Telemetry & Diagnostics
- **Live TPS & MSPT History Graphs**:
  - Real-time rolling graphs in the Performance tab tracking Ticks Per Second (TPS) and Milliseconds Per Tick (MSPT).
  - Immediate visual alerts when tick duration exceeds 50 ms (indicating server lag).
- **Aikar's JVM Garbage Collection Presets**:
  - 1-tap toggle in Server Settings to apply optimized G1GC Aikar's flags tuned specifically for mobile RAM constraints and minimal GC pause times.
- **Storage Warning Thresholds**:
  - Proactive warning banner when available device storage drops below 1 GB during active server runtime or world generation.

### 🌐 5. Web Remote Admin Panel (LAN & Web Management)
- **Built-in Local Web Admin**:
  - Optional lightweight local HTTP server enabling full server management from a desktop browser or tablet on the same Wi-Fi network.
  - Access terminal console with live command execution, file manager with in-browser text editing, and real-time performance telemetry without touching the Android phone.
- **Security & Strong Authentication**:
  - Secure credential validation with cryptographically random session tokens, constant-time verification, and brute-force rate limiting.
- **Online Exposure Support**:
  - Expose the web admin panel securely over the internet via UPnP router port forwarding, Bore tunnels, or Playit.

---

## 🎮 Version 1.6.0: Full Web UI, Bedrock Native, Proxy Networks & Discord Bridge

The 1.6.0 milestone upgrades the Web Remote Admin Panel into a complete, standalone web interface with full feature parity to the mobile app, while introducing native Bedrock server engines, multi-server network orchestration, and community Discord integration.

### 🌐 1. Full Feature-Parity Web UI
- **Complete App Parity in the Browser**:
  - Expand `WebAdminServer` from a remote monitor/console into a full control panel where every action available in the Android app can be performed from a browser.
  - **Full Server Lifecycle**: Create new servers via a web-based setup wizard, switch server software/versions, and manage isolated Java runtimes.
  - **Plugins, Mods & Modpacks**: Search and install Modrinth plugins/mods, import `.mrpack` modpacks, and upload custom `.jar` files from the browser.
  - **Worlds, Backups & Automation**: Manage world imports/exports, dimension resets, local & cloud backup sync (Nextcloud/WebDAV), scheduled cron tasks, and UPnP/Bore/Playit networking.
  - **Modern Bundled Web Assets**: Refactor the embedded single-string HTML into clean, bundled static web assets served by the embedded HTTP server.

### 🧱 2. Native Bedrock Dedicated Server (BDS - Beta)
- **Official Mojang Bedrock Dedicated Server (BDS)**:
  - Run the native Ubuntu `x86_64` `bedrock_server` binary inside the PRoot container (native on `x86_64`, `qemu-x86_64` user-mode emulation on `ARM64`).
  - Pure Bedrock hosting with zero Java protocol translation overhead, supporting official Add-Ons, Behavior Packs, and Resource Packs.

### 🔀 3. Proxy & Multi-Server Networks (Velocity & BungeeCord)
- **Velocity & BungeeCord Proxy Orchestration**:
  - Support creating and launching Velocity or BungeeCord proxy servers alongside backend game servers on a single Android device.
  - Link survival, creative, and minigame servers together through a unified hub / lobby server.
- **Internal Loopback Binding**:
  - Secure internal binding on `127.0.0.1` so backend servers are only reachable through the proxy.

### 💬 4. Discord Webhook & Server Event Bridge
- **Native Discord Webhook Integration**:
  - Configure Discord channel webhooks directly in Server Settings (no server plugins required).
  - Live broadcast messages for:
    - Server startup, shutdown, and unexpected crash events.
    - Player join and leave notices with skins.
    - Completed automated world backups and storage health reports.

---

## 🌍 Version 1.7.0: Core KMP Architecture Prep, World Mastery & Android Integration

The 1.7.0 milestone lays the architectural foundation for Kotlin Multiplatform by extracting a shared platform-agnostic `:core` module (while still building and releasing exclusively for Android), alongside advanced world editing tools and Android system integration.

### 🧱 1. Core Module Extraction & Multiplatform Preparation
- **Shared `:core` Module Split**:
  - Extract models, API clients (Mojang, Paper, Purpur, Fabric, Forge, NeoForge, Modrinth), modpack installer, UPnP/NAT-PMP port forwarders, WebDAV backup sync, cron scheduler, log/crash analyzers, and `WebAdminServer` out of `:app` into a pure-Kotlin `:core` module.
  - Replace direct `android.content.Context`, `SharedPreferences`, `Uri`, `MediaStore`, `org.json`, and `android.util.Log` usages in core logic with clean cross-platform interfaces (`Logger`, `PlatformStorage`, `SettingsStore`, `RuntimeExecutor`, `JavaProvisioner`, `NetworkInterfaceProvider`).
- **Android-Only Shipping**:
  - Keep release builds focused strictly on Android while validating the new `:core` boundaries with platform-independent unit tests.

### 🛠️ 2. In-App NBT Editor & Level.dat Modifier
- **Direct `level.dat` Inspector & Editor**:
  - View and modify world seed, game type, difficulty, hardcore mode, and world time directly within the UI without external desktop tools.
  - 1-tap toggles for common gamerules (`keepInventory`, `doDaylightCycle`, `mobGriefing`, `doWeatherCycle`, `spawnRadius`).
- **Player NBT & Inventory Inspector**:
  - View offline player inventories, ender chest contents, experience levels, and coordinates.

### 🗺️ 3. World Pre-Generation & Chunk Tools
- **Chunk Pre-Generator Tool**:
  - Integrated radius pre-generation tool to generate surrounding chunks while the server is empty, eliminating lag caused by player exploration.
- **Visual Region File Inspector**:
  - Interactive storage map showing individual `.mca` region files by dimension and size to identify bloated regions.

### 📱 4. Android System Widgets & Quick Settings Tile
- **Quick Settings Start/Stop Tile**:
  - Start, stop, or view status of your primary server directly from Android's pull-down Quick Settings panel.
- **Home Screen Widget**:
  - Glanceable home screen widget showing active server status, online player count, memory utilization, and 1-tap start/stop controls.
- **Battery & Thermal Safeguards**:
  - Configurable auto-shutdown when device battery falls below a defined percentage (e.g. 15%) or when battery temperature exceeds safety thresholds.

---

## 🐧 Version 1.8.0: Linux Beta, Headless Mode, Distro Packaging & Mesh VPN

The 1.8.0 milestone marks MineServe's first expansion beyond Android with **Beta** builds for Linux desktops and headless servers, plus container footprint minimization and private mesh networking.

### 🖥️ 1. Linux Desktop & Headless Server (Beta)
- **Native Linux Runtime (No PRoot Required)**:
  - Execute Minecraft servers natively via `ProcessBuilder` with automatic portable OpenJDK provisioning (Java 8, 17, 21, 25) and native Linux `bore` / `playit` binaries.
- **Native UI + Headless Web UI Mode**:
  - Compose Multiplatform desktop UI for Linux workstations alongside a `--headless` / daemon CLI mode that runs purely in the background and exposes the full Web UI (ideal for home servers, VPS hosts, and Raspberry Pi).
- **Linux Distro Build & Release Pipeline**:
  - Automated CI/CD packaging and release distribution across popular Linux formats: `.deb` (Debian/Ubuntu), `.rpm` (Fedora/RHEL), `AppImage` / `Flatpak`, standalone `.tar.gz`, and official Docker images.

### 📦 2. Lightweight Android Container Profile (Alpine Linux)
- **Ultra-Minimal Alpine Linux Rootfs**:
  - Optional musl/Alpine rootfs profile reducing Android container base disk consumption by over 60% compared to standard Ubuntu.
  - Faster cold-boot times and minimal footprint for budget Android hardware.

### 🔒 3. Private Mesh VPN Networking (Tailscale & WireGuard)
- **Tailscale Mesh Integration**:
  - Connect your server to a private Tailscale tailnet directly within the app, allowing friends with Tailscale to join privately without public exposure.

---

## 🌐 Version 1.9.0: Stable Linux, macOS & Windows Betas & New Website

The 1.9.0 milestone graduates Linux to **Stable**, introduces **Beta** builds for **macOS** and **Windows**, and launches a redesigned website for the multi-platform MineServe ecosystem.

### 🐧 1. Stable Linux Release
- **Production-Ready Linux Builds**:
  - Harden systemd service integration, auto-update flows, headless CLI tooling, and distro packages across Debian/Ubuntu, Fedora, Arch, and Docker.

### 🍎 2. macOS & Windows Desktop Betas (Native UI & Headless Mode)
- **Windows Beta (`.exe` / `.msi` / Portable)**:
  - Native Windows process execution, portable JRE management, system tray integration, and `--headless` Web UI daemon mode.
- **macOS Beta (`.dmg` Universal Apple Silicon & Intel)**:
  - Native macOS app bundle with Compose Multiplatform UI, background menu bar / tray support, and `--headless` Web UI mode.

### ✨ 3. Brand New Multi-Platform Website
- **Complete Website Redesign**:
  - Launch a brand new website showcasing the entire MineServe project across **Android, Linux, macOS, Windows, and Docker/Headless** hosting, with platform-specific download selectors, documentation, and live Web UI previews.

---

## 🚀 Version 2.0.0: Stable Kotlin Multiplatform Across All Platforms

The 2.0.0 milestone completes MineServe's transformation into a unified **Kotlin Multiplatform (KMP)** application with **Stable** releases across **Android, Linux, macOS, and Windows**.

### 🧩 1. Unified Kotlin Multiplatform Architecture
- **Single Shared Core & UI**:
  - Unified KMP codebase powering native UIs (Jetpack Compose on Android, Compose Multiplatform on Linux, macOS, and Windows) alongside the full-featured Web UI.
- **Full Feature Parity Everywhere**:
  - Every platform supports all server engines, isolated Java management, Modrinth plugins/mods/`.mrpack`, UPnP/NAT-PMP, Bore & Playit tunnels, Cloud Backups, Automation, and the Web UI.
- **First-Class Headless Desktop Mode**:
  - Seamless toggle or CLI flag across Linux, macOS, and Windows to run MineServe headless with only the Web UI active for lightweight, always-on server hosting.

---

## 🔮 Version 2.1.0+ Beyond: Autonomous Ops, Clustering, Bare-Metal & Multi-Game

Long-term research initiatives exploring autonomous operations, clustering, bare-metal Android runtimes, and expanding beyond Minecraft.

### 🤖 1. Autonomous Server Watchdog & Self-Healing Operations (MineServe Sentinel)
- **Automated Lag Machine & Runaway Entity Quarantine**:
  - Background telemetry monitor that identifies server stalls caused by runaway mob farms (e.g. 500 chickens in a single chunk) or rapid redstone clocks.
  - Automatically isolates problematic chunks, safely caps entity limits, or triggers targeted cleanup commands without crashing the server.
- **Automated Crash Self-Repair**:
  - Automated detection of corrupted region headers (`.mca`), removing bad entity NBT tags identified in crash dumps and rebooting cleanly with user notifications.

### 📱 2. Multi-Device Hardware Clustering (MineServe Swarm)
- **Multi-Node Cluster Orchestration**:
  - Connect two or more MineServe nodes (Android or Desktop) on the same local network into a collaborative hosting cluster.
  - Example: A secondary device hosts the Nether and End dimensions or runs a Velocity proxy, while your primary machine handles Overworld player activity.
- **P2P Player Inventory & State Sync**:
  - Peer-to-peer synchronization across clustered nodes without requiring external cloud servers or paid hosting.

### 🛰️ 3. WebRTC P2P Direct Hole-Punching
- **Serverless Direct Multiplayer**:
  - Direct peer-to-peer WebRTC hole-punching allowing friends to connect from anywhere in the world across cellular data and CGNAT without relying on centralized relay services.

### ⚡ 4. Containerless Bare-Metal Android Bionic Runtimes
- **Direct Android Bionic Java Execution**:
  - Running OpenJDK compiled directly for Android Bionic libc, bypassing PRoot system call emulation entirely.
  - Eliminates ptrace overhead, increasing tick efficiency by 25% to 35% and dropping memory overhead to bare-metal speeds.
- **GraalVM Native Image Exploration**:
  - Ahead-of-time (AOT) compiled native server binaries with sub-second startup times and minimal memory footprint.

### 🎮 5. Multi-Game Dedicated Server Platform (GameServe Ecosystem)
- **Modular Game Server Packs**:
  - Expanding MineServe's process management, terminal emulation, automated backups, and tunneling architecture to host other popular dedicated servers:
    - Terraria Dedicated Server (vanilla and TShock / tModLoader)
    - Mindustry Dedicated Server
    - Additional lightweight multiplayer game servers

---

## 🤝 Community Feedback & Feature Requests

Have a feature request or suggestion that should be on our roadmap?
- **GitHub Discussions**: Share ideas and workflows in [GitHub Discussions](https://github.com/devwithzachary/MineServe/discussions).
- **Issue Tracker**: Submit detailed bug reports or enhancement proposals on [GitHub Issues](https://github.com/devwithzachary/MineServe/issues).
- **Discord Community**: Join our community of mobile server hosts on [Discord](https://discord.gg/QdT9yCrKmV).

