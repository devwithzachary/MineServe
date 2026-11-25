package com.devwithzachary.mineserve.ui.preview

import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityOptionsCompat
import com.devwithzachary.mineserve.admin.web.WebAdminConfig
import com.devwithzachary.mineserve.admin.web.WebAdminExposure
import com.devwithzachary.mineserve.admin.web.WebAdminExposureState
import com.devwithzachary.mineserve.admin.web.WebAdminState
import com.devwithzachary.mineserve.auth.AuditLogEntry
import com.devwithzachary.mineserve.auth.PanelUser
import com.devwithzachary.mineserve.auth.UserRole
import com.devwithzachary.mineserve.cloud.CloudAccountProfile
import com.devwithzachary.mineserve.cloud.CloudAccountState
import com.devwithzachary.mineserve.cloud.CloudBackupItem
import com.devwithzachary.mineserve.cloud.CloudSubdomainInfo
import com.devwithzachary.mineserve.designsystem.theme.ThemeMode
import com.devwithzachary.mineserve.engine.RootfsSetupState
import com.devwithzachary.mineserve.engine.TerminalEmulator
import com.devwithzachary.mineserve.model.BackupEntry
import com.devwithzachary.mineserve.model.BannedIpEntry
import com.devwithzachary.mineserve.model.BannedPlayerEntry
import com.devwithzachary.mineserve.model.DimensionType
import com.devwithzachary.mineserve.model.FileEntry
import com.devwithzachary.mineserve.model.MinecraftServer
import com.devwithzachary.mineserve.model.OpEntry
import com.devwithzachary.mineserve.model.PluginModEntry
import com.devwithzachary.mineserve.model.ScheduleFrequency
import com.devwithzachary.mineserve.model.ScheduledTask
import com.devwithzachary.mineserve.model.ScheduledTaskType
import com.devwithzachary.mineserve.model.ServerAutomationConfig
import com.devwithzachary.mineserve.model.ServerMetrics
import com.devwithzachary.mineserve.model.ServerPlayerLists
import com.devwithzachary.mineserve.model.ServerProperties
import com.devwithzachary.mineserve.model.ServerStatus
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.model.TunnelConfig
import com.devwithzachary.mineserve.model.TunnelProvider
import com.devwithzachary.mineserve.model.WebMapPluginType
import com.devwithzachary.mineserve.model.WebMapState
import com.devwithzachary.mineserve.model.WhitelistEntry
import com.devwithzachary.mineserve.model.WorldDimensionInfo
import com.devwithzachary.mineserve.model.WorldSummary
import com.devwithzachary.mineserve.tunnel.TunnelState
import com.devwithzachary.mineserve.ui.theme.MineServeTheme
import java.io.File

/**
 * Multi-preview annotation for top-level MineServe Android screens across
 * compact phones, Pixel 7 Dark/Light, foldable, tablet landscape, and large font scale.
 */
@Preview(
    name = "Compact Dark",
    group = "Phone",
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Preview(
    name = "Phone Dark",
    group = "Phone",
    device = Devices.PIXEL_7,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Preview(
    name = "Phone Light",
    group = "Phone",
    device = Devices.PIXEL_7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true
)
@Preview(
    name = "Foldable Dark",
    group = "Large",
    device = Devices.FOLDABLE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Preview(
    name = "Tablet Dark",
    group = "Large",
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Preview(
    name = "Large Font",
    group = "Accessibility",
    widthDp = 360,
    heightDp = 800,
    fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
annotation class MineServeScreenPreviews

/**
 * Multi-preview annotation for Server Detail workspace sub-tabs across
 * Phone Dark, Phone Light, and Tablet Dark layouts.
 */
@Preview(
    name = "Phone Dark",
    group = "Phone",
    device = Devices.PIXEL_7,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Preview(
    name = "Phone Light",
    group = "Phone",
    device = Devices.PIXEL_7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
@Preview(
    name = "Tablet Dark",
    group = "Large",
    device = Devices.TABLET,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
annotation class MineServeTabPreviews

private object PreviewActivityResultRegistryOwner : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            // No-op in Android Studio Preview
        }
    }
}

/**
 * Wraps Compose previews in MineServeTheme (mapping system UI night mode to
 * ObsidianDark / SlateLight) and provides a no-op ActivityResultRegistryOwner
 * so file/permission launchers never crash in Layoutlib.
 */
@Composable
fun MineServePreviewContainer(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalActivityResultRegistryOwner provides PreviewActivityResultRegistryOwner
    ) {
        MineServeTheme(
            themeMode = ThemeMode.System,
            useDynamicColor = false,
            content = content
        )
    }
}

object MineServePreviewFixtures {
    private const val SAMPLE_EPOCH_MS = 1_740_000_000_000L

    val sampleAutomationConfig = ServerAutomationConfig(
        idleSleepEnabled = true,
        idleSleepTimeoutMinutes = 15,
        autoWakeOnPing = true,
        scheduledTasks = listOf(
            ScheduledTask(
                id = "task-backup-1",
                serverId = "srv-paper-1",
                name = "Nightly World Backup",
                enabled = true,
                taskType = ScheduledTaskType.WORLD_BACKUP,
                frequency = ScheduleFrequency.DAILY_TIME,
                dailyHour = 4,
                dailyMinute = 0,
                backupRetentionCount = 5,
                lastRunTimestamp = SAMPLE_EPOCH_MS - 3_600_000L,
                nextRunTimestamp = SAMPLE_EPOCH_MS + 43_200_000L
            ),
            ScheduledTask(
                id = "task-msg-2",
                serverId = "srv-paper-1",
                name = "Auto-Save Notice",
                enabled = true,
                taskType = ScheduledTaskType.BROADCAST_MESSAGE,
                frequency = ScheduleFrequency.INTERVAL,
                intervalMinutes = 360,
                command = "World save complete. Happy crafting!",
                lastRunTimestamp = SAMPLE_EPOCH_MS - 1_800_000L,
                nextRunTimestamp = SAMPLE_EPOCH_MS + 19_800_000L
            )
        )
    )

    val runningPaperServer = MinecraftServer(
        id = "srv-paper-1",
        name = "Obsidian SMP",
        type = ServerType.PAPER,
        version = "1.21.4",
        port = 25565,
        allocatedRamMb = 2048,
        javaVersion = 21,
        status = ServerStatus.RUNNING,
        motd = "MineServe Survival Network",
        autoStart = true,
        eulaAccepted = true,
        createdAt = SAMPLE_EPOCH_MS - 86_400_000L,
        lastStartedAt = SAMPLE_EPOCH_MS - 5_420_000L,
        serverBuild = "185",
        tunnelConfig = TunnelConfig(
            enabled = true,
            provider = TunnelProvider.MINESERVE_CLOUD,
            autoStart = true,
            cloudSubdomain = "obsidian",
            cloudRegion = "us-east",
            cloudDomain = "play.mineserve.app"
        ),
        automationConfig = sampleAutomationConfig
    )

    val stoppedFabricServer = MinecraftServer(
        id = "srv-fabric-2",
        name = "Create Fabric",
        type = ServerType.FABRIC,
        version = "1.21.4",
        port = 25566,
        allocatedRamMb = 4096,
        javaVersion = 21,
        status = ServerStatus.STOPPED,
        motd = "Modded Survival Realm",
        eulaAccepted = true,
        createdAt = SAMPLE_EPOCH_MS - 172_800_000L
    )

    val standbyBedrockServer = MinecraftServer(
        id = "srv-bedrock-3",
        name = "Pocket Creative",
        type = ServerType.BEDROCK_BDS,
        version = "1.21.60",
        port = 19132,
        allocatedRamMb = 1024,
        javaVersion = 21,
        status = ServerStatus.STANDBY,
        motd = "Crossplay Creative Build",
        eulaAccepted = true,
        createdAt = SAMPLE_EPOCH_MS - 259_200_000L
    )

    val sampleServers = listOf(runningPaperServer, stoppedFabricServer, standbyBedrockServer)

    val sampleStatuses = mapOf(
        runningPaperServer.id to ServerStatus.RUNNING,
        stoppedFabricServer.id to ServerStatus.STOPPED,
        standbyBedrockServer.id to ServerStatus.STANDBY
    )

    val sampleRunningMetrics = ServerMetrics(
        serverId = runningPaperServer.id,
        isRunning = true,
        cpuPercentage = 34.6f,
        ramUsedMb = 1380L,
        ramMaxMb = 2048L,
        onlinePlayerCount = 4,
        onlinePlayers = listOf("Steve", "Alex", "Notch", "Zachary"),
        tps = 19.92,
        mspt = 14.3,
        tpsHistory = listOf(19.8f, 19.9f, 20.0f, 19.7f, 19.9f, 20.0f, 19.9f, 19.8f, 20.0f, 19.9f, 19.9f, 19.92f),
        msptHistory = listOf(16.1f, 14.8f, 13.9f, 18.4f, 15.2f, 14.0f, 13.5f, 15.6f, 14.2f, 13.8f, 14.5f, 14.3f),
        lagWarningsCount = 0,
        uptimeSeconds = 5420L,
        pid = 14820
    )

    val sampleMetricsMap = mapOf(runningPaperServer.id to sampleRunningMetrics)

    val sampleStorageBytesMap = mapOf(
        runningPaperServer.id to 482_000_000L,
        stoppedFabricServer.id to 1_240_000_000L,
        standbyBedrockServer.id to 195_000_000L
    )

    val sampleTunnelState = TunnelState.Connected(
        publicHost = "obsidian.play.mineserve.app",
        publicPort = 25565,
        fullAddress = "obsidian.play.mineserve.app",
        provider = TunnelProvider.MINESERVE_CLOUD,
        assignedAt = SAMPLE_EPOCH_MS,
        activeConnections = 4
    )

    val sampleTunnelStates = mapOf(runningPaperServer.id to sampleTunnelState)

    val sampleProperties = ServerProperties(
        motd = "MineServe Survival Network",
        maxPlayers = 20,
        gamemode = "survival",
        difficulty = "normal",
        pvp = true,
        onlineMode = true,
        whiteList = true,
        viewDistance = 10,
        simulationDistance = 8,
        levelSeed = "849201948271"
    )

    val samplePlayerLists = ServerPlayerLists(
        whitelist = listOf(
            WhitelistEntry(uuid = "8667ba71-b85a-4004-af54-457a9734eed7", name = "Steve"),
            WhitelistEntry(uuid = "ec561538-f3fd-461d-aff5-086b22154bce", name = "Alex"),
            WhitelistEntry(uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5", name = "Notch")
        ),
        ops = listOf(
            OpEntry(uuid = "11111111-2222-3333-4444-555555555555", name = "Zachary", level = 4),
            OpEntry(uuid = "ec561538-f3fd-461d-aff5-086b22154bce", name = "Alex", level = 3)
        ),
        bannedPlayers = listOf(
            BannedPlayerEntry(
                uuid = "99999999-8888-7777-6666-555555555555",
                name = "Griefer99",
                created = "2026-11-20 14:22:00 +0000",
                source = "Zachary",
                reason = "Spawn griefing"
            )
        ),
        bannedIps = listOf(
            BannedIpEntry(
                ip = "203.0.113.42",
                created = "2026-11-20 14:25:00 +0000",
                source = "Zachary",
                reason = "Repeated ban evasion"
            )
        )
    )

    val samplePlugins = listOf(
        PluginModEntry(
            id = "essentialsx",
            fileName = "EssentialsX-2.21.0.jar",
            name = "EssentialsX",
            version = "2.21.0",
            description = "Core commands, warps, homes, and economy utilities.",
            author = "EssentialsX Team",
            enabled = true,
            fileSizeBytes = 2_450_000L,
            downloads = 12_400_000,
            categories = listOf("Utility", "Admin")
        ),
        PluginModEntry(
            id = "spark",
            fileName = "spark-1.10.119-bukkit.jar",
            name = "spark",
            version = "1.10.119",
            description = "Performance profiler for CPU, memory, and TPS diagnostics.",
            author = "lucko",
            enabled = true,
            fileSizeBytes = 1_180_000L,
            downloads = 5_800_000,
            categories = listOf("Performance")
        ),
        PluginModEntry(
            id = "worldedit",
            fileName = "worldedit-bukkit-7.3.10.jar",
            name = "WorldEdit",
            version = "7.3.10",
            description = "In-game voxel map editor and schematic builder.",
            author = "EngineHub",
            enabled = true,
            fileSizeBytes = 5_920_000L,
            downloads = 24_000_000,
            categories = listOf("World Management")
        ),
        PluginModEntry(
            id = "squaremap",
            fileName = "squaremap-paper-1.3.4.jar.disabled",
            name = "squaremap",
            version = "1.3.4",
            description = "Lightweight 2D world map viewer.",
            author = "jpenilla",
            enabled = false,
            fileSizeBytes = 3_410_000L,
            downloads = 1_900_000,
            categories = listOf("Map")
        )
    )

    val sampleFiles = listOf(
        FileEntry(name = "world", relativePath = "world", isDirectory = true, lastModified = SAMPLE_EPOCH_MS),
        FileEntry(name = "plugins", relativePath = "plugins", isDirectory = true, lastModified = SAMPLE_EPOCH_MS),
        FileEntry(
            name = "server.properties",
            relativePath = "server.properties",
            isDirectory = false,
            sizeBytes = 1_420L,
            lastModified = SAMPLE_EPOCH_MS - 300_000L,
            extension = "properties",
            isEditable = true
        ),
        FileEntry(
            name = "paper-global.yml",
            relativePath = "config/paper-global.yml",
            isDirectory = false,
            sizeBytes = 4_860L,
            lastModified = SAMPLE_EPOCH_MS - 600_000L,
            extension = "yml",
            isEditable = true
        ),
        FileEntry(
            name = "latest.log",
            relativePath = "logs/latest.log",
            isDirectory = false,
            sizeBytes = 64_200L,
            lastModified = SAMPLE_EPOCH_MS,
            extension = "log",
            isEditable = true,
            isLog = true
        ),
        FileEntry(
            name = "world-snapshot.zip",
            relativePath = "world-snapshot.zip",
            isDirectory = false,
            sizeBytes = 148_500_000L,
            lastModified = SAMPLE_EPOCH_MS - 86_400_000L,
            extension = "zip",
            isArchive = true
        )
    )

    val sampleBackups = listOf(
        BackupEntry(
            id = "bkp-1",
            serverId = runningPaperServer.id,
            name = "Pre-Update Snapshot",
            fileName = "obsidian-smp-full-20261124.zip",
            sizeBytes = 284_000_000L,
            timestamp = SAMPLE_EPOCH_MS - 7_200_000L,
            isWorldOnly = false
        ),
        BackupEntry(
            id = "bkp-2",
            serverId = runningPaperServer.id,
            name = "Nightly World Save",
            fileName = "obsidian-smp-world-20261123.zip",
            sizeBytes = 162_400_000L,
            timestamp = SAMPLE_EPOCH_MS - 86_400_000L,
            isWorldOnly = true
        )
    )

    val sampleCloudBackups = listOf(
        CloudBackupItem(
            backupId = "cloud-bkp-1",
            userId = "usr-123",
            serverId = runningPaperServer.id,
            fileName = "obsidian-smp-full-20261124.zip",
            sizeBytes = 284_000_000L,
            r2Key = "backups/usr-123/obsidian-smp-full-20261124.zip",
            status = "READY",
            createdAtEpochMs = SAMPLE_EPOCH_MS - 7_000_000L
        )
    )

    val sampleWorldSummary = WorldSummary(
        levelName = "world",
        totalSizeBytes = 386_500_000L,
        dimensions = listOf(
            WorldDimensionInfo(
                dimension = DimensionType.OVERWORLD,
                directory = File("/preview/world"),
                exists = true,
                sizeBytes = 248_000_000L,
                regionFilesCount = 18,
                totalChunksCount = 4_210
            ),
            WorldDimensionInfo(
                dimension = DimensionType.NETHER,
                directory = File("/preview/world_nether"),
                exists = true,
                sizeBytes = 94_200_000L,
                regionFilesCount = 8,
                totalChunksCount = 1_540
            ),
            WorldDimensionInfo(
                dimension = DimensionType.THE_END,
                directory = File("/preview/world_the_end"),
                exists = true,
                sizeBytes = 44_300_000L,
                regionFilesCount = 4,
                totalChunksCount = 620
            )
        ),
        seed = "849201948271",
        lastModifiedTimestamp = SAMPLE_EPOCH_MS
    )

    val sampleWebMapStateRunning = WebMapState(
        installedPlugin = WebMapPluginType.SQUAREMAP,
        port = 8081,
        isServerRunning = true
    )

    val sampleCloudLinkedState = CloudAccountState(
        isLinked = true,
        isLoading = false,
        profile = CloudAccountProfile(
            userId = "usr-preview-01",
            email = "player@mineserve.app",
            tier = "PLUS",
            storageUsedBytes = 3_400_000_000L,
            activeSubdomains = listOf("obsidian"),
            activeSubdomainDetails = listOf(
                CloudSubdomainInfo(
                    subdomain = "obsidian",
                    region = "us-east",
                    domain = "play.mineserve.app"
                )
            )
        )
    )

    val sampleCloudUnlinkedState = CloudAccountState(isLinked = false)

    val sampleWebAdminConfig = WebAdminConfig(
        enabled = true,
        port = 8080,
        username = "admin",
        password = "preview-password-123",
        exposure = WebAdminExposure.BORE_TUNNEL
    )

    val sampleWebAdminState = WebAdminState(
        isRunning = true,
        port = 8080,
        localIp = "192.168.1.42",
        localUrl = "http://192.168.1.42:8080",
        exposureState = WebAdminExposureState.Connected(
            publicUrl = "https://obsidian.panel.mineserve.app",
            details = "MineServe Cloud Panel"
        ),
        activeSessionsCount = 2
    )

    val samplePanelUsers = listOf(
        PanelUser(
            id = "u-owner",
            username = "zachary",
            role = UserRole.OWNER,
            passwordHash = "hash",
            createdAt = SAMPLE_EPOCH_MS - 604_800_000L
        ),
        PanelUser(
            id = "u-admin",
            username = "alex_mod",
            role = UserRole.ADMIN,
            passwordHash = "hash",
            restrictServers = true,
            allowedServerIds = listOf(runningPaperServer.id),
            createdAt = SAMPLE_EPOCH_MS - 259_200_000L
        ),
        PanelUser(
            id = "u-mod",
            username = "steve_helper",
            role = UserRole.MODERATOR,
            passwordHash = "hash",
            createdAt = SAMPLE_EPOCH_MS - 86_400_000L
        )
    )

    val sampleAuditEntries = listOf(
        AuditLogEntry(
            id = "aud-1",
            timestamp = SAMPLE_EPOCH_MS - 1_200_000L,
            userId = "u-owner",
            username = "zachary",
            role = "OWNER",
            action = "SERVER_START",
            target = "Obsidian SMP",
            details = "Started Paper 1.21.4",
            clientIp = "192.168.1.42"
        ),
        AuditLogEntry(
            id = "aud-2",
            timestamp = SAMPLE_EPOCH_MS - 600_000L,
            userId = "u-admin",
            username = "alex_mod",
            role = "ADMIN",
            action = "PLAYER_WHITELIST_ADD",
            target = "Steve",
            details = "Added player to whitelist",
            clientIp = "192.168.1.88"
        )
    )

    val sampleRootfsExtractingState = RootfsSetupState.Extracting(
        message = "Installing OpenJDK 21 runtime...",
        progressPercent = 68,
        logs = listOf(
            "Extracting Ubuntu 24.04 ARM64 rootfs...",
            "Configuring APT package repositories...",
            "Unpacking openjdk-21-jre-headless (21.0.5)..."
        ),
        stageTitle = "Configuring Java Runtime"
    )

    const val SAMPLE_DIAGNOSTICS_LOG =
        "11-24 20:14:02.110 I/MineServe: Booting PRoot engine (arm64-v8a)\n" +
            "11-24 20:14:03.421 I/ServerProcess: Starting Obsidian SMP (Paper 1.21.4, 2048 MB)\n" +
            "11-24 20:14:08.904 I/TunnelManager: Connected to us-east relay -> obsidian.play.mineserve.app\n" +
            "11-24 20:14:12.310 I/WebAdmin: Panel listening on 0.0.0.0:8080\n" +
            "11-24 20:15:01.005 I/Paper: Done (4.218s)! For help, type \"help\""

    @Composable
    fun rememberSampleTerminalEmulator(): TerminalEmulator {
        return remember {
            TerminalEmulator(cols = 80, rows = 24).apply {
                val lines = listOf(
                    "\u001B[36m[20:14:02 INFO]\u001B[0m Starting minecraft server version 1.21.4",
                    "\u001B[36m[20:14:03 INFO]\u001B[0m Loading properties",
                    "\u001B[36m[20:14:04 INFO]\u001B[0m Default game type: SURVIVAL",
                    "\u001B[36m[20:14:05 INFO]\u001B[0m Preparing level \"world\"",
                    "\u001B[32m[20:14:08 INFO]\u001B[0m Done (3.412s)! For help, type \"help\"",
                    "\u001B[33m[20:14:14 INFO]\u001B[0m Steve[/192.168.1.50:54312] logged in with entity id 142",
                    "\u001B[33m[20:14:19 INFO]\u001B[0m Alex joined the game"
                ).joinToString("\r\n") + "\r\n"
                val bytes = lines.toByteArray()
                appendBytes(bytes, bytes.size)
            }
        }
    }
}
