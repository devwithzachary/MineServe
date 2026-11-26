package com.devwithzachary.mineserve.desktop

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.core.Logger
import com.devwithzachary.mineserve.designsystem.assets.DesktopBrandAssets
import com.devwithzachary.mineserve.desktop.ui.DesktopViewModel
import com.devwithzachary.mineserve.desktop.ui.DesktopSystemTrayManager
import com.devwithzachary.mineserve.desktop.ui.MineServeDesktopApp
import com.devwithzachary.mineserve.model.MineServeAboutData
import com.devwithzachary.mineserve.model.ServerStatus
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.CountDownLatch
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

data class CliOptions(
    val headless: Boolean = false,
    val verbose: Boolean = false,
    val port: Int = 8080,
    val bindHost: String = "0.0.0.0",
    val dataDir: File = DesktopDataPaths.defaultDataDir(),
    val resetAdmin: Boolean = false,
    val resetPassword: String? = null,
    val printService: Boolean = false,
    val showHelp: Boolean = false
)

fun parseCliArgs(args: Array<String>): CliOptions {
    var headless = false
    var verbose = false
    var port = 8080
    var bindHost = "0.0.0.0"
    var dataDir = DesktopDataPaths.defaultDataDir()
    var resetAdmin = false
    var resetPassword: String? = null
    var printService = false
    var showHelp = false

    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--headless" -> headless = true
            "--verbose", "-v" -> verbose = true
            "--port", "-p" -> {
                port = args.getOrNull(++i)?.toIntOrNull()?.coerceIn(1, 65535) ?: port
            }
            "--bind", "-b" -> {
                bindHost = args.getOrNull(++i)?.trim()?.ifBlank { bindHost } ?: bindHost
            }
            "--data-dir", "-d" -> {
                val p = args.getOrNull(++i)?.trim()
                if (!p.isNullOrBlank()) dataDir = File(p)
            }
            "--reset-admin" -> {
                resetAdmin = true
                val next = args.getOrNull(i + 1)
                if (next != null && !next.startsWith("-")) {
                    resetPassword = next
                    i++
                }
            }
            "--reset-password" -> {
                resetAdmin = true
                val next = args.getOrNull(i + 1)
                if (next != null && !next.startsWith("-")) {
                    resetPassword = next
                    i++
                }
            }
            "--print-service" -> printService = true
            "--help", "-h" -> showHelp = true
            else -> {
                if (arg.startsWith("--port=")) {
                    port = arg.substringAfter("=").toIntOrNull()?.coerceIn(1, 65535) ?: port
                } else if (arg.startsWith("--bind=")) {
                    bindHost = arg.substringAfter("=").trim().ifBlank { bindHost }
                } else if (arg.startsWith("--data-dir=")) {
                    val p = arg.substringAfter("=").trim()
                    if (p.isNotBlank()) dataDir = File(p)
                } else if (arg.startsWith("--reset-admin=")) {
                    resetAdmin = true
                    resetPassword = arg.substringAfter("=").takeIf { it.isNotBlank() }
                } else if (arg.startsWith("--reset-password=")) {
                    resetAdmin = true
                    resetPassword = arg.substringAfter("=").takeIf { it.isNotBlank() }
                }
            }
        }
        i++
    }

    return CliOptions(
        headless = headless,
        verbose = verbose,
        port = port,
        bindHost = bindHost,
        dataDir = dataDir,
        resetAdmin = resetAdmin,
        resetPassword = resetPassword,
        printService = printService,
        showHelp = showHelp
    )
}

private fun generateInitialPassword(length: Int = 14): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
    val rng = SecureRandom()
    return (1..length).map { alphabet[rng.nextInt(alphabet.length)] }.joinToString("")
}

private fun printUsage() {
    println(
        """
        MineServe ${DesktopPlatform.shortName} (v${MineServeAboutData.APP_VERSION}) - Native Minecraft Server Manager

        Usage:
          mineserve [options]

        Options:
          --headless                Run in headless daemon mode (Web Admin panel only, no GUI window)
          --verbose, -v             Print detailed HTTP, tunnel, and Minecraft server logs
          --port, -p <port>         Web Admin HTTP port (default: 8080)
          --bind, -b <host>         Bind address (default: 0.0.0.0)
          --data-dir, -d <path>     Custom data directory (default: ${DesktopDataPaths.defaultDataDir().path})
          --reset-admin [pass]      Reset primary 'admin' Owner account (random password if omitted) and exit
          --reset-password [pass]   Alias for --reset-admin (min 6 chars if custom password provided)
          --print-service           Print a ready-to-use background service config for ${DesktopPlatform.shortName}
          --help, -h                Show this help message
        """.trimIndent()
    )
}

private object FallbackAppIconPainter : Painter() {
    override val intrinsicSize = Size(64f, 64f)
    override fun DrawScope.onDraw() {
        drawRoundRect(color = Color(0xFF10B981))
    }
}

private fun loadAppLogoBufferedImage(): BufferedImage? {
    return DesktopBrandAssets.loadWindowIconBufferedImage()
}

fun main(args: Array<String>) {
    val options = parseCliArgs(args)
    CoreLog.sink = object : Logger {
        override fun d(tag: String, message: String) {
            if (options.verbose) println("D/$tag: $message")
        }
        override fun i(tag: String, message: String) = println("[$tag] $message")
        override fun w(tag: String, message: String, throwable: Throwable?) {
            System.err.println("[$tag] WARN: $message")
            throwable?.printStackTrace(System.err)
        }
        override fun e(tag: String, message: String, throwable: Throwable?) {
            System.err.println("[$tag] ERROR: $message")
            throwable?.printStackTrace(System.err)
        }
    }

    if (options.showHelp) {
        printUsage()
        return
    }
    if (options.printService) {
        print(SystemdServiceInstaller.generateForCurrentOs(dataDir = options.dataDir, port = options.port, bindHost = options.bindHost))
        return
    }

    val paths = DesktopDataPaths(options.dataDir)
    val userRepository = UserRepository(paths.rootDir)

    if (options.resetAdmin) {
        val newPass = options.resetPassword?.trim()?.takeIf { it.isNotEmpty() } ?: generateInitialPassword()
        if (newPass.length < 6) {
            System.err.println("Error: password must be at least 6 characters.")
            return
        }
        userRepository.syncPrimaryOwnerCredentials("admin", newPass)
        println("============================================================")
        println(" MineServe Admin Panel Credentials Reset")
        println("   Username: admin")
        println("   Password: $newPass")
        println("   Data Dir: ${paths.rootDir.absolutePath}")
        println("============================================================")
        if (!options.headless) {
            return
        }
    }

    val existingUsers = userRepository.listUsers()
    if (existingUsers.isEmpty()) {
        val initialPassword = generateInitialPassword()
        userRepository.ensureDefaultOwner("admin", initialPassword)
        println("============================================================")
        println(" MineServe ${DesktopPlatform.shortName} First-Run Setup")
        println(" Created default Owner account:")
        println("   Username: admin")
        println("   Password: $initialPassword")
        println(" Forgot it later? Run with --reset-admin to generate a new one.")
        println("============================================================")
    }

    val javaManager = PortableJavaManager(paths)
    val serverRepository = DesktopServerRepository(paths)
    val softwareManager = DesktopServerSoftwareManager(serverRepository, javaManager)
    val cloudManager = DesktopCloudManager(paths)
    val networkManager = DesktopNetworkManager(paths, cloudManager = cloudManager, verbose = options.verbose)
    val processManager = DesktopProcessManager(
        serverRepository = serverRepository,
        javaManager = javaManager,
        networkManager = networkManager,
        verbose = options.verbose,
        hasActiveTunnel = { serverId -> networkManager.getTunnelState(serverId) is DesktopTunnelState.Connected }
    )
    val schedulerManager = DesktopSchedulerManager(
        serverRepository = serverRepository,
        processManager = processManager,
        cloudManager = cloudManager
    )
    schedulerManager.start()

    // Auto-start any servers configured with autoStart = true, or enter standby if autoWakeOnPing = true
    serverRepository.servers.value.forEach { srv ->
        if (srv.autoStart) {
            println("[MineServe] Auto-starting server '${srv.name}' (${srv.type.displayName} ${srv.version})...")
            processManager.startServer(srv)
        } else if (srv.automationConfig.autoWakeOnPing) {
            println("[MineServe] Starting auto-wake standby listener for '${srv.name}' on port ${srv.port}...")
            processManager.enterStandby(srv)
        }
    }

    val webServer = DesktopWebServer(
        bindHost = options.bindHost,
        port = options.port,
        paths = paths,
        serverRepository = serverRepository,
        processManager = processManager,
        softwareManager = softwareManager,
        javaManager = javaManager,
        networkManager = networkManager,
        cloudManager = cloudManager,
        userRepository = userRepository,
        verbose = options.verbose
    )

    if (!webServer.start()) {
        System.err.println("Failed to start MineServe Web Admin on ${options.bindHost}:${options.port}")
        return
    }
    networkManager.webAdminPortProvider = { webServer.actualPort }

    val runHeadless = options.headless || GraphicsEnvironment.isHeadless()
    val lanIp = DesktopHostTelemetry.getLocalIpAddress() ?: "127.0.0.1"
    val mem = DesktopHostTelemetry.getMemoryInfo()
    println("")
    println("MineServe ${DesktopPlatform.shortName} Engine started (${if (runHeadless) "Headless Mode" else "Desktop GUI + Web Admin Mode"}${if (options.verbose) ", Verbose" else ""})")
    println("  Host:      ${DesktopHostTelemetry.getHostDisplayName()} (${mem.totalMb} MB RAM)")
    println("  Data Dir:  ${paths.rootDir.absolutePath}")
    println("  Local URL: http://127.0.0.1:${webServer.actualPort}")
    println("  LAN URL:   http://$lanIp:${webServer.actualPort}")
    val webFw = DesktopFirewallChecker.checkInboundPort(webServer.actualPort, isUdp = false)
    if (webFw.isActive && !webFw.isPortConfirmedOpen && webFw.fixCommand != null) {
        println("  [Firewall Warning] ${webFw.firewallName} is active. To access the Web UI from another device on LAN, run:")
        println("    ${webFw.fixCommand}")
    }
    println("")

    val trayRef = java.util.concurrent.atomic.AtomicReference<DesktopSystemTrayManager?>(null)
    val shutdownHook = Thread {
        println("\n[MineServe] Shutting down gracefully...")
        schedulerManager.stop()
        trayRef.getAndSet(null)?.stop()
        networkManager.stopAll()
        processManager.stopAllServersBlocking()
        webServer.stop()
    }
    Runtime.getRuntime().addShutdownHook(shutdownHook)

    if (runHeadless) {
        println("  Press Ctrl+C to gracefully stop all servers and exit.")
        val shutdownLatch = CountDownLatch(1)
        Runtime.getRuntime().addShutdownHook(Thread { shutdownLatch.countDown() })
        shutdownLatch.await()
        return
    }

    val viewModel = DesktopViewModel(
        paths = paths,
        serverRepository = serverRepository,
        processManager = processManager,
        softwareManager = softwareManager,
        javaManager = javaManager,
        networkManager = networkManager,
        cloudManager = cloudManager,
        userRepository = userRepository,
        webServer = webServer
    )

    val logoBufferedImage = loadAppLogoBufferedImage()
    val appIconPainter: Painter = logoBufferedImage?.let { BitmapPainter(it.toComposeImageBitmap()) } ?: FallbackAppIconPainter

    application {
        var isWindowVisible by remember { mutableStateOf(true) }
        var composeWindowRef by remember { mutableStateOf<ComposeWindow?>(null) }
        val windowState = rememberWindowState(width = 1600.dp, height = 1000.dp)
        val statuses by processManager.serverStatuses.collectAsState()
        val runningCount = statuses.values.count { it == ServerStatus.RUNNING || it == ServerStatus.STARTING }

        fun bringWindowToFrontOnWayland() {
            SwingUtilities.invokeLater {
                composeWindowRef?.apply {
                    isVisible = true
                    extendedState = Frame.NORMAL
                    isAlwaysOnTop = true
                    toFront()
                    requestFocus()
                }
            }
            Thread {
                try {
                    Thread.sleep(180)
                    SwingUtilities.invokeLater {
                        composeWindowRef?.isAlwaysOnTop = false
                    }
                } catch (_: Exception) {}
            }.start()
        }

        fun restoreDesktopWindow() {
            SwingUtilities.invokeLater {
                isWindowVisible = true
                windowState.isMinimized = false
            }
            bringWindowToFrontOnWayland()
        }

        fun hideDesktopWindow() {
            SwingUtilities.invokeLater {
                isWindowVisible = false
            }
        }

        LaunchedEffect(isWindowVisible) {
            if (isWindowVisible) {
                bringWindowToFrontOnWayland()
            }
        }

        DisposableEffect(Unit) {
            val trayManager = DesktopSystemTrayManager(
                paths = paths,
                logoImage = logoBufferedImage,
                onActivateWindow = { restoreDesktopWindow() },
                onHideWindow = { hideDesktopWindow() },
                onStartAllServers = {
                    serverRepository.servers.value.forEach { srv ->
                        if (!processManager.isServerRunning(srv.id)) {
                            processManager.startServer(srv)
                        }
                    }
                },
                onStopAllServers = { processManager.stopAllServers() },
                onOpenWebAdmin = { viewModel.openUrlInBrowser("http://127.0.0.1:${webServer.actualPort}") },
                onQuitApp = {
                    Thread({
                        schedulerManager.stop()
                        networkManager.stopAll()
                        processManager.stopAllServersBlocking()
                        webServer.stop()
                        trayRef.getAndSet(null)?.stop()
                        exitApplication()
                    }, "mineserve-quit").start()
                }
            )
            trayRef.set(trayManager)
            trayManager.start()
            onDispose {
                trayRef.compareAndSet(trayManager, null)
                trayManager.stop()
            }
        }

        Window(
            onCloseRequest = {
                if (runningCount > 0 && trayRef.get()?.isActive == true) {
                    isWindowVisible = false
                    println("[MineServe] Window hidden to system tray ($runningCount server(s) running). Left-click or right-click the tray icon to restore.")
                } else {
                    Thread({
                        schedulerManager.stop()
                        networkManager.stopAll()
                        processManager.stopAllServersBlocking()
                        webServer.stop()
                        trayRef.getAndSet(null)?.stop()
                        exitApplication()
                    }, "mineserve-close").start()
                }
            },
            visible = isWindowVisible,
            title = "MineServe v${MineServeAboutData.APP_VERSION} - ${DesktopPlatform.displayName}",
            icon = appIconPainter,
            state = windowState
        ) {
            DisposableEffect(window) {
                composeWindowRef = window
                window.minimumSize = java.awt.Dimension(1120, 720)
                if (logoBufferedImage != null) {
                    runCatching { window.iconImage = logoBufferedImage }
                }
                onDispose {}
            }
            MineServeDesktopApp(viewModel = viewModel)
        }
    }
}
