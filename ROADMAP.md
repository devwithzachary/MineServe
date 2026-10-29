# MineServe Project Roadmap

This document outlines the strategic product roadmap and feature milestones planned for upcoming releases of **MineServe**.

Our goal is to deliver the most reliable, capable, and accessible portable Minecraft server hosting platform on Android, completely rootless and privacy-respecting.

---

## 🗺️ Roadmap Status & Release Schedule

| Version | Focus / Theme | Target Milestone | Status |
| :--- | :--- | :--- | :--- |
| **v1.4.2** | Android x86_64 PRoot Reliability & Codebase Modularity | October 2026 | **Shipped** ✅ |
| **v1.5.0** | Connectivity, Cloud Backups, Web Remote Admin & Telemetry | October 2026 | **Shipped** ✅ |
| **v1.6.0** | Bedrock Dedicated Server, Proxy Networks & Discord Bridge | Q1 2027 | **Scheduled** 📋 |
| **v1.7.0** | World Mastery, NBT Editor & Android System Integration | Q2 2027 | **Scheduled** 📋 |
| **v1.8.0** | Alpine Lightweight Rootfs & Mesh VPN | Mid 2027 | **Scheduled** 📋 |
| **v1.9.0 / v2.0.0** | Autonomous Ops, Self-Healing & Multi-Device Clustering | Late 2027 | **Vision / Exploration** 💡 |
| **v2.1.0+** | Bare-Metal Bionic Runtimes & Multi-Game Hosting | Future Vision | **Exploration** 🔭 |

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

## 🎮 Version 1.6.0: Bedrock Native, Proxy Networks & Discord Bridge

The 1.6.0 milestone expands beyond Java-centric architectures by introducing native Bedrock server engines, multi-server network orchestration, and community Discord integration.

### 🧱 1. Native Bedrock Dedicated Server (BDS) & PocketMine
- **Official Mojang Bedrock Dedicated Server (BDS)**:
  - Run the native Ubuntu x86_64 / ARM64 `bedrock_server` binary inside the PRoot container.
  - Pure Bedrock hosting with zero Java protocol translation overhead, supporting official Add-Ons, Behavior Packs, and Resource Packs.
- **PocketMine-MP Engine Support**:
  - Ultra-lightweight PHP-based Minecraft: Bedrock Edition server software with plugin support.

### 🔀 2. Proxy & Multi-Server Networks (Velocity & BungeeCord)
- **Velocity & BungeeCord Proxy Orchestration**:
  - Support creating and launching Velocity or BungeeCord proxy servers alongside backend game servers on a single Android device.
  - Link survival, creative, and minigame servers together through a unified hub / lobby server.
- **Internal Loopback Binding**:
  - Secure internal binding on `127.0.0.1` so backend servers are only reachable through the proxy.

### 💬 3. Discord Webhook & Server Event Bridge
- **Native Discord Webhook Integration**:
  - Configure Discord channel webhooks directly in Server Settings (no server plugins required).
  - Live broadcast messages for:
    - Server startup, shutdown, and unexpected crash events.
    - Player join and leave notices with skins.
    - Completed automated world backups and storage health reports.

---

## 🌍 Version 1.7.0: World Mastery, NBT Editor & Android System Integration

The 1.7.0 milestone delivers advanced world editing tools, deep operating system integration, and mobile productivity enhancements.

### 🛠️ 1. In-App NBT Editor & Level.dat Modifier
- **Direct `level.dat` Inspector & Editor**:
  - View and modify world seed, game type, difficulty, hardcore mode, and world time directly within the UI without external desktop tools.
  - 1-tap toggles for common gamerules (`keepInventory`, `doDaylightCycle`, `mobGriefing`, `doWeatherCycle`, `spawnRadius`).
- **Player NBT & Inventory Inspector**:
  - View offline player inventories, ender chest contents, experience levels, and coordinates.

### 🗺️ 2. World Pre-Generation & Chunk Tools
- **Chunk Pre-Generator Tool**:
  - Integrated radius pre-generation tool to generate surrounding chunks while the server is empty, eliminating lag caused by player exploration.
- **Visual Region File Inspector**:
  - Interactive storage map showing individual `.mca` region files by dimension and size to identify bloated regions.

### 📱 3. Android System Widgets & Quick Settings Tile
- **Quick Settings Start/Stop Tile**:
  - Start, stop, or view status of your primary server directly from Android's pull-down Quick Settings panel.
- **Home Screen Widget**:
  - Glanceable home screen widget showing active server status, online player count, memory utilization, and 1-tap start/stop controls.
- **Battery & Thermal Safeguards**:
  - Configurable auto-shutdown when device battery falls below a defined percentage (e.g. 15%) or when battery temperature exceeds safety thresholds.

---

## 💡 Version 1.8.0: Alpine Lightweight Rootfs & Mesh VPN

The 1.8.0 milestone focuses on container footprint minimization and private mesh networking.

### 🖥️ 1. Lightweight Container Profile (Alpine Linux)
- **Ultra-Minimal Alpine Linux Rootfs**:
  - Optional musl/Alpine rootfs profile reducing container base disk consumption by over 60% compared to standard Ubuntu.
  - Faster cold-boot times and minimal footprint for budget Android hardware.

### 🔒 2. Private Mesh VPN Networking (Tailscale & WireGuard)
- **Tailscale Mesh Integration**:
  - Connect your server to a private Tailscale tailnet directly within the app, allowing friends with Tailscale to join privately without public exposure.

---

## 🔮 Version 1.9.0 / 2.0.0: Autonomous Operations, Self-Healing & Multi-Device Clustering

The 2.0 milestone represents a generational leap: transforming MineServe from an interactive host into an autonomous, self-healing server platform capable of multi-device clustering.

### 🤖 1. Autonomous Server Watchdog & Self-Healing Operations (MineServe Sentinel)
- **Automated Lag Machine & Runaway Entity Quarantine**:
  - Background telemetry monitor that identifies server stalls caused by runaway mob farms (e.g. 500 chickens in a single chunk) or rapid redstone clocks.
  - Automatically isolates problematic chunks, safely caps entity limits, or triggers targeted cleanup commands without crashing the server.
- **Automated Crash Self-Repair**:
  - Automated detection of corrupted region headers (`.mca`), removing bad entity NBT tags identified in crash dumps and rebooting cleanly with user notifications.

### 📱 2. Multi-Device Hardware Clustering (MineServe Swarm)
- **Android Phone Cluster Orchestration**:
  - Connect two or more Android devices on the same local network into a collaborative hosting cluster.
  - Example: An older secondary phone hosts the Nether and End dimensions or running a Velocity proxy, while your primary phone handles Overworld player activity.
- **P2P Player Inventory & State Sync**:
  - Peer-to-peer synchronization across clustered nodes without requiring external cloud servers or paid hosting.

### 🛰️ 3. WebRTC P2P Direct Hole-Punching
- **Serverless Direct Multiplayer**:
  - Direct peer-to-peer WebRTC hole-punching allowing friends to connect from anywhere in the world across cellular data and CGNAT without relying on centralized relay services.

---

## 🔭 Version 2.1.0+ Beyond: Bare-Metal Android Runtimes & Multi-Game Hosting

Long-term research initiatives exploring post-PRoot architectures and expanding beyond Minecraft.

### ⚡ 1. Containerless Bare-Metal Bionic Runtimes
- **Direct Android Bionic Java Execution**:
  - Running OpenJDK compiled directly for Android Bionic libc, bypassing PRoot system call emulation entirely.
  - Eliminates ptrace overhead, increasing tick efficiency by 25% to 35% and dropping memory overhead to bare-metal speeds.
- **GraalVM Native Image Exploration**:
  - Ahead-of-time (AOT) compiled native server binaries with sub-second startup times and minimal memory footprint.

### 🎮 2. Multi-Game Dedicated Server Platform (GameServe Ecosystem)
- **Modular Game Server Packs**:
  - Expanding MineServe's process management, terminal emulation, automated backups, and tunneling architecture to host other popular dedicated servers on mobile:
    - Terraria Dedicated Server (vanilla and TShock / tModLoader)
    - Mindustry Dedicated Server
    - Additional lightweight Linux-compatible multiplayer game servers

---

## 🤝 Community Feedback & Feature Requests

Have a feature request or suggestion that should be on our roadmap?
- **GitHub Discussions**: Share ideas and workflows in [GitHub Discussions](https://github.com/devwithzachary/MineServe/discussions).
- **Issue Tracker**: Submit detailed bug reports or enhancement proposals on [GitHub Issues](https://github.com/devwithzachary/MineServe/issues).
- **Discord Community**: Join our community of mobile server hosts on [Discord](https://discord.gg/csGrrg5MGF).

