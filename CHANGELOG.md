# Changelog

All notable changes to the MineServe project will be documented in this file.

## [1.5.0] - 2026-10-29

### 🖥️ Web Remote Admin Panel (LAN & Web Management)
- **Built-in Local Web Admin**: Embedded zero-dependency lightweight local HTTP server allowing full management from any desktop browser or tablet on the same Wi-Fi network.
- **Full Server Console & Command Input**: Real-time terminal output with syntax highlighting, command execution bar, command history navigation, and quick action pills.
- **Remote File Manager & Editor**: In-browser directory browser with text file inspection and code editing for server configuration files (`server.properties`, configs) and file deletion.
- **Live Telemetry Dashboard**: Real-time TPS and MSPT gauges, server memory utilization progress bar, CPU usage, and online player lists.
- **Cryptographic Security & Brute-Force Rate Limiting**: Secure session tokens, constant-time credential verification, configurable admin username/password, and automatic 5-minute lockout after failed attempts.
- **Online Exposure Support**: Expose the web admin panel securely over the internet using UPnP router port forwarding, Bore public tunnels, or Playit.
- **In-App QR Code & One-Tap Sharing**: Display QR code in the app for instant scanning with camera devices plus one-tap copy and browser launch.

### 🌐 Universal Router Port Forwarding (UPnP & NAT-PMP)
- **Automatic Router Port Forwarding**: Added built-in zero-config UPnP (SSDP/SOAP) and RFC 6886 NAT-PMP client for automatic router port mapping. Friends on the Internet can now connect directly to the host's residential public WAN IP without requiring manual router admin configuration or third-party tunnel proxies.
- **Dynamic Connection Card & One-Tap Direct Address Copy**: Server Settings and status cards now display external mapped WAN IP and port with live mapping health, local IP mapping, and a 1-tap copy button for direct server invites.

### ☁️ Cloud Backup Sync (100% F-Droid Compliant)
- **Open-Source Cloud Sync Architecture**: Integrated automated off-device backup sync strictly adhering to F-Droid guidelines using 100% open-source protocols (RFC 4918 WebDAV and Android Storage Access Framework) without proprietary closed-source Google Play Services libraries.
- **Nextcloud & WebDAV Provider**: Full support for syncing world snapshots to self-hosted Nextcloud, ownCloud, or generic WebDAV endpoints with directory recursion, authentication testing, and streaming upload progress.
- **Android SAF Cloud / Storage Folder Integration**: Allows users to bind any cloud provider folder (Google Drive, Nextcloud, OneDrive, external SD card) via Android's native document tree picker with persistable permissions.
- **Automated Pre-Action & Instant Cloud Sync**: Added an auto-sync toggle that automatically pushes newly created world snapshots to the configured cloud destination upon backup creation, plus 1-tap manual cloud sync on individual backup entries.

### 🧩 Modrinth (.mrpack) Modpack Importer
- **One-Tap Modpack Installation**: Added native support for importing official Modrinth `.mrpack` modpack archives directly from device storage or downloads.
- **Dependency & Environment Filtering**: Automatically reads `modrinth.index.json`, filters out client-only mods (`env.server == "unsupported"`), checks version and modloader compatibility, and downloads all required server-side mods with SHA-1/SHA-512 checksum validation.
- **Configuration & Override Extraction**: Streams real-time progress while extracting `overrides/` and `server-overrides/` directly into the server directory.

### 📊 Live Performance Telemetry & Diagnostics
- **Rolling Canvas Telemetry Graphs**: Added smooth real-time Canvas charts in the Performance tab graphing rolling historical TPS (0 to 20 scale) and MSPT (0 to 100 ms scale) with dynamic threshold coloring and baseline target reference lines.
- **Proactive Server Lag Diagnostics**: Live diagnostics card detects sustained tick degradation beyond the 50.0 ms budget while gracefully ignoring normal startup spikes, providing actionable recommendations to optimize view distances and check runaway entity clusters.
- **Aikar's G1GC JVM Flags Preset**: Added a 1-tap action to apply Aikar's optimized G1GC flags tuned for minimal garbage collection pauses and reduced CPU contention on mobile processors.
- **Low Storage Threshold Safeguards**: Displays a proactive warning banner when available device storage drops below 1 GB to prevent world corruption during chunk generation.

### 🎨 UI & UX Polish
- **Server Creation Wizard EULA Layout**: Wrapped the EULA agreement link onto its own line with responsive padding on Step 4 of the server wizard, preventing text truncation on narrow screens.
- **Mod & Plugin Enable Switch Contrast**: Added distinct background track and border styling to the plugin enable toggle, along with increased margin from item descriptions, providing clear visual toggle status.

## [1.4.2] - 2026-10-29

### 🖥️ x86_64 Emulator & PRoot Runtime Fixes
- **x86_64 Android Zygote Seccomp `renameat2` & Legacy Syscall Translation**: Fixed `dpkg` and `apt-get` failing with `Function not implemented (ENOSYS)` under Android's `x86_64` Zygote seccomp policy in `external/proot/tracee/seccomp.c`, `external/proot/syscall/enter.c`, `external/proot/syscall/exit.c`, and `external/proot/tracee/event.c`. Rewrote legacy `PR_rename` and `PR_renameat` syscalls to `PR_renameat2` (and `PR_unlink`/`PR_rmdir` to `PR_unlinkat`) after `SYSCALL_ENTER_END` and in `SIGSYS` recovery so `link2symlink` link counts are never decremented twice, and added `SIGSYS` translation for `readlink`, `creat`, `mknod`, `epoll_create`, `inotify_init`, `eventfd`, `signalfd`, `fork`, `vfork`, `alarm`, and self-`dup2`.
- **Eliminated Android SELinux `setattr` Denial Flood in PRoot**: Patched `override_permissions` in `external/proot/extension/fake_id0/fake_id0.c` to skip host pseudo-filesystems (`/proc`, `/sys`, `/dev`) and system-owned paths not owned by the app UID, preventing thousands of `avc: denied { setattr }` audit logs per second during `apt-get` and `dpkg` execution on Android 14-16.
- **Removed Restricted `/proc/1` & Unreadable Storage Bindings**: Dropped the `/proc/self:/proc/1` host bind mount in `PRootEngine` (which triggered SELinux `getattr` denials against Android `init` while PRoot already virtualizes `/proc/1` internally) and guarded external storage mounts with `canRead()` checks.
- **x86_64 Native Loader Address & Calling Convention Alignment**: Fixed `scripts/build_native_libs.sh` to link the 64-bit `x86_64` `libproot_loader.so` at `0x600000000000` and the 32-bit `x86` `libproot_loader32.so` at `0xa0000000` with `-mregparm=3` matching `arch.h`, and exported `PROOT_LOADER_32` alongside `PROOT_LOADER32`.
- **Sanitized Guest Environment & Emulator DNS Resolution**: Explicitly cleared host `LD_LIBRARY_PATH` in the traced child process before `execvp` (`external/proot/tracee/event.c`), enforced `TMPDIR=/tmp` inside the OpenJDK installation script so `mktemp` in `ca-certificates` post-install scripts never targets inaccessible host `/data/local/tmp`, added Android Emulator SLIRP DNS (`10.0.2.3`) to `/etc/resolv.conf`, and enabled `Dpkg::Use-Pty "0"` and `Acquire::Retries "3"` in APT configuration.

### 🛠️ Setup Error Visibility & Console Diagnostics Persistence
- **Synchronous OpenJDK 21 Setup & Error Reporting**: Initial setup now remains on the initialization screen while OpenJDK 21 installs, streaming live progress logs directly to `SplashScreen`. If package installation fails, `SplashScreen` displays detailed error output with retry controls instead of dropping to the dashboard without a working runtime.
- **Persistent Server Console Diagnostics**: `ServerProcessManager` now maintains a persistent terminal buffer per server across session creation, stop, and error states, ensuring pre-launch auto-installation logs and startup errors remain visible in the Console tab.
- **Graceful Java Installation Failure Handling**: Failed runtime installations during server startup now halt launch immediately, preserving console error diagnostics and server error status instead of failing silently to an offline state.

### 🎛️ Device-Aware Default RAM Allocation
- **Adaptive Default RAM Preselection**: Updated Step 3 of the server creation wizard (`CreateServerWizardScreen`) and `RamSlider` (`ResourceAndRamComponents`) to calculate a device-aware default RAM allocation (`getDefaultAllocatedRamMb`) and maximum slider cap that leaves headroom for Android on lower-memory devices (2 GB to 3 GB total RAM) while preserving the 2048 MB default on devices with 4 GB+ RAM.
- **Fractional Total RAM Formatting**: Updated the "Device Total" memory readout in `RamSlider` to display fractional gigabytes (such as `2.5 GB`) instead of truncating via integer division.

### 🏗️ Codebase Architecture, Modularity & Dead Code Cleanup
- **Decomposed Monolithic Composables and Repositories**: Refactored oversized files into focused, single-responsibility components:
  - Extracted `PlayerListRepository` and `ServerFileRepository` from `ServerRepository` (reducing it from 806 to 242 lines).
  - Decomposed `LiveMapTab` into `map/LiveMapDialogs.kt` and `map/LiveMapStatusViews.kt` (reducing it from 985 to 478 lines).
  - Decomposed `PlayerActionDialog` into `player/PlayerActionTabs.kt` (reducing it from 949 to 297 lines).
  - Decomposed `PlayersTab` into `players/PlayerListSections.kt` (reducing it from 929 to 505 lines).
  - Decomposed `AddScheduledTaskDialog` into `automation/ScheduleTimingSelectors.kt` (reducing it from 891 to 523 lines).
  - Extracted standalone `StandbyPingManager` from `StandbyPingListener.kt`.
- **Precompiled Regex Performance in Console Streaming**: Extracted `ConsoleOutputParser` with precompiled static regexes for done indicators, player joins/leaves, and server ports, eliminating repetitive dynamic regex allocations on every received console log buffer.
- **Optimized Per-Server Metric Sampling**: Discovery of PID trees in `ServerProcessManager` is now performed once per tick and shared between memory and CPU calculations, halving the `/proc` directory traversal overhead during metric polling.
- **Consolidated Shared Storage & Zip Operations**: Centralized file formatting, directory sizing, recursive stream zipping, and safe zip extraction with Zip-Slip traversal protection into `FileStorageUtils`.
- **Eliminated Dead Code & Redundant Commands**: Removed unused raw config reading/saving routes, legacy unused utility helpers (`QuickCommandBar`, `ensurePRootExecutable`, `applyTheme`, etc.), and prevented duplicate console command dispatches on player management actions.

## [1.4.1] - 2026-10-20

### 📜 Minecraft EULA & User Opt-In
- **Explicit EULA Acceptance in Wizard**: The Minecraft End User License Agreement checkbox in Step 4 (Review) of the server setup wizard now defaults to unticked (`false`). The "Create Server" button remains disabled until the user explicitly reviews and agrees to the agreement.
- **Process Startup EULA Non-Override**: Removed the unconditional automatic overwrite of `eula.txt` on server startup in `ServerProcessManager`. Existing `eula.txt` files are strictly preserved, honoring user opt-out or custom configuration, and missing `eula.txt` files are generated respecting the server's recorded opt-in state.

### 🛡️ Security, Privacy & Android Backup Protection
- **Disabled Android Automatic Backup**: Set `android:allowBackup="false"` in `AndroidManifest.xml` to prevent server configuration files, world saves, PRoot rootfs directories, and sensitive credentials (such as Playit secrets and RCON passwords) from being backed up to unencrypted cloud backups or device transfers.
- **Explicit Backup and Extraction Rules**: Configured comprehensive XML backup exclusion rules (`data_extraction_rules.xml` and `backup_rules.xml`) across all storage domains (`root`, `file`, `database`, `sharedpref`) as an additional safeguard.

### 🌐 Network & Startup Privacy
- **Default-Disabled Automatic Update Checks**: Startup release checks via the GitHub API now default to disabled. Users retain full control to enable automatic checks via App Settings or manually check for new releases on demand.
- **Comprehensive Network Disclosures**: Updated store metadata (`full_description.txt`) with transparent disclosures detailing all external network interactions, including Mojang server manifests, Ubuntu root filesystem and OpenJDK downloads, tunnel fallback binaries, Modrinth REST APIs, and GitHub release queries.

### 🛠️ Native Tooling & Reproducible Submodules
- **Native Rust Submodules**: Pinned upstream `playit-agent` (v0.15.26) and `bore` (v0.6.0) source repositories as git submodules under `external/playit-agent` and `external/bore`, ensuring the F-Droid source scanner has full visibility without cloning during build time.
- **Native Build Script Optimization**: Updated `scripts/build_native_libs.sh` to prefer local submodule source trees when present with seamless fallback to online shallow clones for local standalone builds.

### ⚖️ F-Droid Compliance & Licensing Alignment
- **Official License Harmonization**: Aligned the repository `LICENSE` file with the GNU General Public License v3.0 (GPL-3.0) text, harmonizing with `README.md` and setting `License: GPL-3.0-only` in the F-Droid recipe metadata.
- **Removal of Unofficial Archive Fallbacks**: Dropped third-party archive mirror fallbacks (`omniarchive.uk`) from `MojangApiClient`, ensuring only official Mojang manifest endpoints are utilized for server JAR resolution.
- **Declared F-Droid Anti-Features**: Added `NonFreeDep` and `NonFreeNet` anti-feature declarations to `com.devwithzachary.mineserve.yml` to reflect dependencies on Mojang's official proprietary server binaries and network downloads.

## [1.4.0] - 2026-10-02

### ⚙️ Minecraft Forge & Historical Version Support
- **Minecraft Forge Server Support**: Added full support for Minecraft Forge across modern and historical releases (from 1.1 up to 1.20+). Automatic installer execution (`--installServer`), dynamic `run.sh` script execution with configured memory arguments (`user_jvm_args.txt`), and classic `forge-*.jar` execution for legacy versions (1.5.2 to 1.16.5).
- **Forge Installer Heap Allocation & Live Log Streaming**: Allocated dedicated JVM heap memory for the Forge installer binary patcher to prevent garbage collector thrashing under PRoot, and added live log streaming in the console so users can observe real-time download and class patching progress without perceived freezing.
- **Legacy Minecraft Version Support Down to 1.0**: Added official support for very old Minecraft releases down to version 1.0 (including 1.0.0, 1.1, 1.2.1-1.2.4). Seamless fallback downloads from verified archive mirrors for early versions where Mojang's modern manifest omits server binaries.
- **Legacy File & Property Compatibility Sanitization**: Automatically creates required legacy authentication and operator files (`banned-players.txt`, `banned-ips.txt`, `ops.txt`, `white-list.txt`) to eliminate startup file-not-found warnings on Minecraft versions before 1.7.5. Automatically sanitizes `server.properties` for legacy servers (`< 1.13`) by converting numeric gamemode/difficulty and enforcing standard `DEFAULT` level types to prevent spawn biome crashes.
- **Modrinth Mod Search Integration**: Added Forge filter to the Plugins & Mods browser, allowing direct search and installation of Forge-compatible mods from Modrinth.
- **Legacy Java 8 Environment Mapping**: Configured automated Java 8 runtime selection for all legacy Minecraft versions (1.16.5 down to 1.0), ensuring smooth startup on Android via the built-in OpenJDK 8 environment.

### 📁 File Manager & Archive Extraction
- **In-App Zip Extraction**: Added ability to extract zip archives directly inside the file manager. Users can tap any `.zip` archive or select "Unzip / Extract" from the file options menu to unpack plugins, mods, configurations, or world files without needing external file managers.
- **Flexible Extraction Targets & Subfolder Routing**: Supports extracting archives directly into the active directory (ideal for quickly unpacking collections of plugin or mod jars) or extracting into a customizable dedicated subfolder.
- **Automated Archive Cleanup**: Added an optional "Delete archive after unzip" checkbox to automatically remove source zip archives upon successful extraction, saving phone storage.
- **Security & Zip-Slip Protection**: Implemented strict canonical path verification to prevent zip directory traversal vulnerabilities, alongside automatic filtering of macOS metadata artifacts (`__MACOSX` resource forks and `.DS_Store`).
- **Batch File Import**: Upgraded the file manager Import action to support multi-file selection, enabling users to import multiple plugins, mods, or configs in a single operation.

### ⚡ High-Performance Native Tunneling (Playit.gg & Bore)
- **Native Android Process Execution**: Migrated both Playit.gg and Bore tunneling agents to run directly as native Android processes (`ProcessBuilder`). Eliminates `ptrace(PTRACE_SYSCALL)` and user-space emulation overhead on every network socket call (`epoll_wait`, `recvmsg`, `sendto`), reducing public tunnel latency by 30+ ms and eliminating packet jitter for smoother multiplayer gameplay.
- **Native Bore Rust Engine**: Bundles and compiles `bore` natively for 64-bit Android (`aarch64-linux-android` and `x86_64-linux-android`) using Tokio async I/O. Automatically executes `libbore.so` from native library storage with automatic connection parsing, while retaining seamless fallback to pure Kotlin sockets if the native binary is unavailable or when custom control ports are configured.
- **Zero-Isolation Loopback Routing**: The native tunnel agents and PRoot Ubuntu container communicate directly over the Android Linux kernel loopback interface (`127.0.0.1:<port>`), achieving sub-millisecond local proxy transfer with zero network isolation overhead.
- **Seamless Tunnel & Secret Migration**: Automatically detects and migrates existing Playit binaries and secret configurations (`playit.toml`) from legacy PRoot rootfs directories to native app storage, preserving claimed public domains and credentials without requiring re-claim.

### 👥 Visual Player & Whitelist Management
- **Visual Management Tables**: Added dedicated, file-backed management tables for Whitelisted Players (`whitelist.json`), Server Operators (`ops.json`), Banned Players (`banned-players.json`), and Banned IP Addresses (`banned-ips.json`), with automatic backwards compatibility for legacy text files.
- **3D Player Avatars & Skins**: Integrated 3D player helm and skin rendering via Minotar for online, whitelisted, and operator player cards with automated fallback placeholders.
- **Full-Screen Player Management Page**: Transformed player administration into a dedicated full-screen page featuring a top app bar with back navigation, player avatar banner, live status badges, and spacious management tabs: kick, ban (with preset reasons), teleport (to spawn, coordinates, or other players), gamemode switching, and item granting.
- **Offline & Live Dual-Mode Synchronization**: File-backed mutations (Whitelist, OP, Ban, Pardon) operate seamlessly whether the server is running or offline, while live in-game actions automatically dispatch real-time console commands when online.

### 🎛️ Server Settings & RAM Allocation
- **Dynamic Server RAM Allocation**: Reconfigure allocated RAM memory on existing servers directly within the Settings tab. Features interactive slider and manual input controls, device memory recommendations, and automatic server restart advisories when running.
- **Settings Port Synchronization**: Synchronizes server configuration files and in-memory server state whenever a server's port is modified via `server.properties` in Settings.

### 🧙 Server Setup Wizard & Port Conflict Detection
- **First Available Port Pre-population**: The creation wizard automatically scans existing configured servers and suggests the next available network port starting from 25565 (e.g. 25566, 25567), avoiding accidental port collisions out of the box.
- **Inline Port Conflict Warning**: Displays real-time inline warnings in both Step 3 (Configuration) and Step 4 (Review) when an entered port is already configured on another server, informing users which server shares the port.
- **Port Input Validation Safeguards**: Validates port bounds (1-65535) with inline error messages and disables the Next button when the port field is empty or out of range.

### 🛡️ Reliability & Engine Safeguards
- **Unified OkHttp Networking & Leaked Stream Elimination**: Standardized all HTTP requests throughout the entire app onto `MineServeHttpClient.client` and unified request construction helpers. Completely eliminated legacy `HttpURLConnection` and manual redirect loops across `MainViewModel`, `RootfsManager`, and `PlayitTunnelClient`. Centralized all external requests with HTTP/2 connection pooling, automatic redirect handling, and scoped `.use { ... }` blocks to prevent socket exhaustion and file descriptor leaks on interrupted transfers.
- **NeoForge Upgrade Jar Cleanup**: Fixed server version upgrade logic to delete previous `neoforge-*.jar` files alongside Forge jars, preventing obsolete binaries from executing when upgrading NeoForge servers.
- **Resilient Property Boolean Parsing**: Enhanced `ServerProperties` parsing to safely handle case variations (`True`, `TRUE`) and numeric flags (`1`/`0`) without reverting to defaults.
- **Native Subprocess Memory Safety**: Fixed native memory and JNI string reference cleanup in `pty.cpp` if `fork()` fails during process creation.
- **Live Map Auto-Upgrade on Server Upgrade**: Automatically detects and upgrades the installed web map plugin (e.g. Squaremap) when upgrading a server's Minecraft version, ensuring web map compatibility without manual reinstallation.
- **Plugin & Mod Manual Upgrade Advisory**: Added clear user notices in the server upgrade dialog advising that third-party plugins and mods must be manually upgraded to match the target Minecraft version to avoid launch failures.

### 🏗️ Architecture & ViewModel Modularization
- **MainViewModel Decomposition**: Decoupled monolithic server management by extracting version manifest resolution, build verification, download streaming, and server upgrade workflows into a dedicated `ServerSoftwareManager`.
- **Encapsulated Rootfs Bootstrap Lifecycle**: Encapsulated the complete PRoot Ubuntu bootstrap state, download pipeline, extraction, and Java installation sequence directly inside `RootfsManager`, simplifying `MainViewModel` into a clean UI state coordinator.

### 🛠️ Native Build Pipeline & F-Droid Reproducibility
- **Source Compilation for Native Tunnel Agents**: Integrated Cargo compilation of `playit-agent` and `bore` directly into `scripts/build_native_libs.sh` and the containerized Debian Trixie release environment (`scripts/build_release_linux.sh`). Compiles high-performance `libplayit.so` and `libbore.so` binaries directly from upstream source repositories to ensure 100% compliance with F-Droid source-only build rules.

### 🌐 Localization & String Extraction
- **Resource Extraction**: Extracted hardcoded user-visible text strings across `PlayersTab`, `SettingsTab`, `ServerSoftwareCard`, `UpdateBuildModal`, `UpgradeVersionModal`, player action dialogs, and wizard configuration steps into `res/values/strings.xml`, ensuring clean separation of presentation text and establishing full localization readiness for upcoming releases.

## [1.3.1] - 2026-09-17

### ⚡ Performance & Core Engine Optimizations
- **Unified Network Client**: Centralized all remote API calls (PaperMC, Purpur, Fabric, NeoForge, Mojang, Modrinth, and GitHub update checker) through a single shared HTTP client with global connection pooling and resilient timeout management.
- **Socket Safety & Leaked Connection Prevention**: Wrapped all HTTP response bodies in scoped resource management blocks (`use { ... }`) to eliminate socket and connection pool exhaustion across all remote API operations.
- **Terminal Emulator O(1) Scrollback Buffer**: Refactored `TerminalEmulator` scrollback storage from `ArrayList` to `ArrayDeque`, replacing O(N) array-copy operations with O(1) line pops when the scrollback limit is exceeded.
- **Terminal Canvas Rendering Memory Optimization**: Added reusable character buffer in Compose `TerminalView` eliminating transient single-character String allocations during high-frequency console output rendering.
- **Chunk Optimizer File Descriptor Safeguard**: Scoped read and write file handles in `ChunkOptimizer` to guarantee descriptors are safely closed and temporary `.opt` files are deleted upon cancellation or failure.
- **Metrics Monitoring Loop Efficiency**: Precompiled static regex patterns and switched `/proc/[pid]/status` parsing to memory-efficient line sequence streaming (`useLines`).
- **Live Web Map Memory Leak Fix**: Explicitly stops loading and destroys the Compose WebView instance in `LiveMapTab` upon disposal via `AndroidView`'s `onRelease`.

### 💤 Standby & Auto-Wake Resiliency
- **Automatic Port Binding Retries**: Added a 30-attempt socket bind retry loop with backoff so the standby listener seamlessly binds once the Minecraft server process finishes releasing the port during shutdown.
- **Decoupled Socket Lifetimes**: Separated TCP and UDP listener teardown in `StandbyPingListener`, preventing UDP cleanup from prematurely closing active TCP sockets.
- **Deterministic Shutdown Transitions**: Standby mode is now triggered when the server process actually terminates in PTY cleanup, eliminating race conditions with premature status polling during auto-sleep or manual stops.
- **Bedrock RakNet Unconnected Pong**: Added RakNet Unconnected Pong response with server MOTD in the standby listener so Bedrock clients display standby status and trigger wake upon query.
- **UI Standby Synchronization**: Toggling Auto-Wake on Ping on an offline server now immediately arms the standby listener, and the Exit Standby action remains accessible and responsive while in Standby status.

### 🏗️ Architecture & Codebase Modularization
- **Repository Decomposition**: Decomposed the monolithic `ServerRepositories.kt` file into clean, single-responsibility repositories: `ServerRepository.kt`, `BackupRepository.kt`, and `PluginRepository.kt`.
- **UI Screen Decomposition**: Modularized multi-hundred-line composable screens into maintainable components:
  - Extracted `ScheduledTaskCard` and `AddScheduledTaskDialog` from `AutomationTab.kt`.
  - Extracted `FileListItem`, `CrashDiagnosticSheet`, and `FileActionDialogs` from `FilesTab.kt`.
  - Extracted `DimensionRowCard` and `WorldTabDialogs` from `WorldTab.kt`.
  - Extracted `UpdateBuildModal` and `UpgradeVersionModal` from `ServerSoftwareCard.kt`.
- **Centralized LayoutManager**: Introduced a central `LayoutManager` design tokens system and `MaterialTheme.layout` provider defining unified card spacing, screen horizontal padding, and layout dimensions across all screens.

### 💾 World & Backup Management
- **Manual Backup Deletion**: Added individual backup deletion with a confirmation dialog detailing file size and backup target in the Backups tab, allowing users to reclaim storage without external file managers.
- **Automated Backup Retention & Pruning**: Added retention limits to scheduled backup tasks with preset chips (Keep 3, 5, 10, 20, or All) and automatic pruning of older automated world or server backups upon new backup completion.

### 🎨 UI & UX Improvements
- **Server Creation Port Stability**: Fixed an issue where creating a new server on a non-default port showed the default port briefly on the review step before changing to the configured port. Decoupled port state from re-creation and added safe numeric validation in the configuration wizard.
- **Dashboard FAB Scroll Clearance**: Added bottom scroll clearance padding (`fabBottomClearance`) to the dashboard server list so the bottom card and its Console button are never obscured by the create server floating action button.
- **Performance Disk Storage Progress & Unified Tile Heights**: Added device storage progress tracking (showing used vs available space) to the Disk Storage tile and unified progress bar visibility across all four telemetry tiles in the Performance tab, ensuring consistent card heights.
- **Popup Dialog Layout & Margin Control**: Standardized all modal dialogs through `AppAlertDialog` and `LayoutManager`, eliminating wide platform default dialog margins with compact 8dp screen margins. Fixed the Share Server QR code dialog so tab titles ("Public Online" and "Local Wi-Fi") fit cleanly on a single line without wrapping.
- **Live Web Map Full World Render**: Added full world map rendering tool with hardware performance warning, dimension selection (Overworld, Nether, The End), and cancel active render controls to eliminate patchy world maps.
- **Compact Card Layout Spacing**: Halved horizontal side margins and vertical spacing around cards from 16dp to 8dp across the dashboard, server detail tabs, wizard, settings, and about screens to maximize screen real estate and card width.
- **Telemetry CPU Subtitle Clarity**: Clarified CPU telemetry tile subtitle to "Single-Core Load" in the Performance tab, eliminating ambiguity around multi-core capacity and single-threaded server tick load.
- **Console Full-Width Macro Bar**: Removed side margins from the macro bar above the live console, allowing quick-command chips to span the full width of the screen in alignment with the terminal output canvas.
- **Scheduled Task Creation Accessibility**: Added a persistent "Add Another Scheduled Task" button below active tasks and weighted the card header to prevent the add action from being pushed off-screen or hidden once tasks exist.
- **Console Input Field Layout**: Shortened the command input placeholder hint text and applied single-line ellipsis truncation so the console command text box stays cleanly on a single line on mobile screens.

### 🛠️ Compatibility, Lint & System Robustness
- **Android Compatibility & NewApi Compliance**: Updated foreground service termination to `ServiceCompat.stopForeground`, safeguarded process destruction in Playit tunnels, guarded NIO filesystem traversal behind Android Oreo, and transitioned concurrent player tracking to `Collections.newSetFromMap` for robust backwards compatibility down to Android 6.0 (API 23).
- **Security & Path Traversal Safeguard**: Sanitized ContentProvider display names during file imports in `ServerRepository` to prevent directory traversal.
- **Compiler & Deprecation Cleanup**: Migrated to Compose Material 3 `PrimaryTabRow` and modern `menuAnchor`, resolved exhaustive `when` warnings in dimension reset, removed deprecated WebSettings, and fixed code block formatting in GitHub update checking.


## [1.3.0] - 2026-09-10

### ⚡ Automation, Smart Schedules & Battery Optimization
- **Smart Idle Auto-Shutdown**: Automatically stops running Minecraft servers after N minutes of 0 connected players (configurable to 5m, 10m, 15m, 30m, 60m) to preserve phone battery and prevent thermal throttling.
- **Auto-Wake on Ping**: Standby socket listener running directly on the server port (TCP for Java Edition, UDP RakNet for Bedrock). When a player queries or joins from their Minecraft multiplayer server list, MineServe automatically spins up the Java process.
- **Dedicated Automation Tab**: New tab in Server Details providing 1-tap configuration for Smart Sleep, Auto-Wake on Ping, manual standby arming, and active scheduler management.
- **Flexible Scheduled Tasks & Cron Engine**: Automated background timer engine for periodic world backups, full server backups, nightly server restarts, and in-game announcements. Supports arbitrary intervals (minutes or hours with quick chips), daily execution at specific times, execution every N days, weekly execution on selected days of the week (Sun through Sat) with N-week intervals, and advanced 5-field custom cron expressions (minute hour day month weekday) with real-time validation.
- **Customizable Console Macro Bar**: Fully customizable quick-command hotbar chips above the live terminal with built-in macro editor to add, edit, delete, or reset 1-tap command shortcuts.
- **Interactive Command Auto-Completion**: Context-aware auto-completion ribbon displayed when typing /, suggesting root Minecraft commands and subcommands (/time set ..., /weather ..., /gamemode ...).
- **Command History Recall**: Memory history with Up and Down navigation buttons next to the console input bar to recall previous commands.

### 🔄 Server Build Updates & Minecraft Version Upgrades
- **Upstream Build Updates**: Added 1-tap upstream build checking and updating for PaperMC and Purpur servers. Easily re-download or update `server.jar` to the latest upstream build for bug fixes and patches while preserving all existing world files, player data, and configurations.
- **In-Place Minecraft Version Upgrades**: Directly upgrade servers to newer Minecraft versions from the Settings tab without losing your existing world, Nether/The End dimensions, player inventories, or configs.
- **Automated Safety Backups**: Both build updating and version upgrading feature an automatic pre-upgrade safety backup option (enabled by default) to protect your worlds before changes are applied.
- **Automatic Java Runtime Alignment**: Upgrading to newer Minecraft versions automatically checks and aligns the server Java runtime requirement (e.g. automatically upgrading from Java 17 to Java 21).
- **Server Software & Version Management Card**: Interactive management card in Server Settings displaying the current platform, version, build number, and Java runtime, complete with live download progress tracking and active server state protection.

### 🌍 World & Map Management
- **Direct .zip & .mcworld Importer**: Import existing singleplayer worlds directly from device storage or Google Drive with automatic level detection, Java Anvil verification, Bedrock format detection, and automated safety backup protection.
- **World Archive Exporter**: Export complete server worlds (Overworld, Nether, and End dimensions) as portable `.zip` archives with integrated Save to Storage and Android Share Sheet support.
- **1-Tap Nether & End Dimension Reset**: Wipe and regenerate `world_nether` or `world_the_end` (and Vanilla `DIM-1` / `DIM1`) without affecting the Overworld, complete with confirmation safeguards and automated pre-reset backups.
- **Chunk Pruning & Storage Optimizer**: Pure Kotlin Anvil (`.mca`) region analyzer and optimizer that identifies and removes uninhabited chunks (`InhabitedTime == 0` or configurable thresholds) and purges empty region files to reclaim mobile storage space.
- **Embedded Live Web Map (Squaremap)**: Dedicated Live Map tab hosting an embedded Compose `WebView` to view real-time 2D world maps on `http://127.0.0.1:8080` with 1-tap installation and uninstallation of Squaremap (optimized for mobile RAM and CPU efficiency). Automatically gated to plugin and mod capable servers (hidden on Vanilla servers where plugins cannot run).

### 🎨 UI Refinements & Open Source Credits
- **Responsive Dashboard RAM Formatting**: Converted server RAM usage values over 1000MB to dynamic GB representations (e.g. 1327MB displays as 1.3GB) and added text overflow protection to prevent two-line wrapping on active server cards.
- **Container Runtime Card Layout**: Fixed layout constraints on the Settings Container Runtime card so the "Online" badge retains its single-line layout without wrapping.
- **Squaremap Open Source Credits**: Added Squaremap to the in-app Credits directory and repository documentation in recognition of its lightweight live web map engine.

## [1.2.0] - 2026-09-02

### 📁 Full In-App File Explorer & Advanced Config Editor
- **Interactive File Explorer**: Full directory navigation for `/servers/{id}/` with breadcrumb path bar, file search, and operations for creating, deleting, renaming, duplicating, and uploading/importing files and folders.
- **Syntax-Highlighted Code Editor**: Built-in monospace code editor with real-time syntax highlighting for `.yml`, `.json`, `.properties`, `.toml`, and config files.
- **Editor Productivity Tools**: Streamlined top bar layout, integrated line numbering gutter, search & replace bar with match counter, and live line/column position tracking.
- **Keyboard & Tap Auto-Scroll**: Automatically scrolls the editor viewport when the virtual keyboard opens or when tapping text so the active cursor line remains comfortably visible.
- **Streamlined Server Settings**: Consolidated raw config file editing into the full In-App File Explorer and dedicated Code Editor, keeping the Settings tab clean and focused on visual Server Properties and Public Tunneling configuration.

### 🩺 Crash Log Analyzer & Diagnostic Engine
- **Automated Root Cause Detection**: Automatically parses `crash-reports/crash-*.txt` and `logs/latest.log` to identify exact crash reasons (incompatible Java versions, out of memory / OOM, mod ID conflicts or missing dependencies, port already in use, corrupted chunks, unaccepted EULA).
- **1-Tap Quick Fix Actions**: Interactive diagnostic sheet providing one-tap solutions to accept the EULA, switch Java runtimes, allocate more RAM, or auto-assign open ports.

### ⚡ Real-Time TPS, Engine Health & Local Timezone
- **Local Timezone Console Synchronization**: Synced the container environment (`/etc/timezone`, `/etc/localtime`, `TZ` environment variable) and JVM system properties (`-Duser.timezone`) with the host device's local timezone so Minecraft server logs and console timestamps match local time instead of UTC.
- **Dynamic TPS & MSPT Calculation**: Replaced static TPS metrics with live game engine telemetry combining real-time CPU core utilization, tick duration (MSPT), tick budget headroom, and passive console overload detection (`Can't keep up!`, `/tps`, `/tick query`).
- **Engine Health Diagnostics Card**: Added dedicated diagnostics card on the Server Performance tab with live TPS gauge, MSPT processing time, tick budget headroom, and logged overload warnings.

### 🧭 Navigation & Gesture Usability
- **System Back Gesture Navigation**: Integrated Compose `BackHandler` and backstack tracking across all app views (About, Credits, Settings, Server Details, and Create Server Wizard). Performing the edge-swipe back gesture or tapping hardware back now smoothly navigates back to previous screens instead of exiting the app.

### 🌐 Playit.gg Tunnel Claiming & Connection Improvements
- **Claim Banner Auto-Dismissal**: Automatically clears the claim banner and prompt as soon as the Playit.gg agent is claimed in the browser or authenticated with credentials.
- **Robust Public Tunnel Address Detection**: Enhanced regex and ANSI escape sequence filtering to instantly detect all public tunnel domains, IP mappings, and custom domains without getting stuck on "Connecting to Playit.gg network".

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
