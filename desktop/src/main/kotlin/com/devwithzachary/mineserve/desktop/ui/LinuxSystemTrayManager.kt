package com.devwithzachary.mineserve.desktop.ui

import com.devwithzachary.mineserve.desktop.LinuxDataPaths
import java.awt.Color as AwtColor
import java.awt.Font
import java.awt.Image
import java.awt.MouseInfo
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

class LinuxSystemTrayManager(
    private val paths: LinuxDataPaths,
    private val logoImage: BufferedImage?,
    private val onActivateWindow: () -> Unit,
    private val onHideWindow: () -> Unit,
    private val onStartAllServers: () -> Unit,
    private val onStopAllServers: () -> Unit,
    private val onOpenWebAdmin: () -> Unit,
    private val onQuitApp: () -> Unit
) {
    @Volatile private var sniProcess: Process? = null
    @Volatile private var awtTrayIcon: TrayIcon? = null
    @Volatile private var stopped: Boolean = false

    val isActive: Boolean
        get() = (sniProcess?.isAlive == true) || (awtTrayIcon != null)

    fun start() {
        stopped = false
        Thread({
            if (stopped) return@Thread
            if (tryStartNativeDbusSniTray()) {
                return@Thread
            }
            if (!stopped) {
                SwingUtilities.invokeLater {
                    if (!stopped) startFallbackAwtTray()
                }
            }
        }, "MineServe-Tray-Init").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        stopped = true
        runCatching {
            sniProcess?.destroyForcibly()
            sniProcess = null
        }
        awtTrayIcon?.let { icon ->
            runCatching { SystemTray.getSystemTray().remove(icon) }
            awtTrayIcon = null
        }
    }

    private fun tryStartNativeDbusSniTray(): Boolean {
        return try {
            val trayDir = File(paths.tmpDir, "tray-sni").apply { mkdirs() }
            val iconThemeDir = File(trayDir, "hicolor/64x64/apps").apply { mkdirs() }
            val rootIconFile = File(trayDir, "mineserve.png")
            val themedIconFile = File(iconThemeDir, "mineserve.png")

            if (logoImage != null) {
                ImageIO.write(logoImage, "png", rootIconFile)
                ImageIO.write(logoImage, "png", themedIconFile)
            }

            val scriptFile = File(trayDir, "mineserve_sni_tray.py")
            scriptFile.writeText(SNI_PYTHON_SCRIPT)

            val proc = ProcessBuilder(
                "python3",
                "-u",
                scriptFile.absolutePath,
                trayDir.absolutePath,
                rootIconFile.absolutePath
            )
                .redirectErrorStream(false)
                .start()

            // Drain stderr continuously so the helper never blocks on pipe buffers
            Thread({
                runCatching {
                    BufferedReader(InputStreamReader(proc.errorStream, Charsets.UTF_8)).use { errReader ->
                        while (errReader.readLine() != null) { /* discard */ }
                    }
                }
            }, "MineServe-SNI-Tray-Stderr").apply {
                isDaemon = true
                start()
            }

            val readyFuture = CompletableFuture<String?>()
            val reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
            Thread({
                try {
                    val first = reader.readLine()
                    readyFuture.complete(first)
                    if (first != "SNI_READY") return@Thread
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        when (line?.trim()) {
                            "CMD:ACTIVATE" -> onActivateWindow()
                            "CMD:HIDE" -> onHideWindow()
                            "CMD:START_ALL" -> onStartAllServers()
                            "CMD:STOP_ALL" -> onStopAllServers()
                            "CMD:OPEN_WEB" -> onOpenWebAdmin()
                            "CMD:QUIT" -> onQuitApp()
                        }
                    }
                } catch (_: Exception) {
                    readyFuture.complete(null)
                }
            }, "MineServe-SNI-Tray-Reader").apply {
                isDaemon = true
                start()
            }

            // Wait up to 3 seconds for StatusNotifierWatcher registration
            val firstLine = runCatching { readyFuture.get(3, TimeUnit.SECONDS) }.getOrNull()
            if (stopped || firstLine != "SNI_READY") {
                proc.destroyForcibly()
                return false
            }

            sniProcess = proc
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun startFallbackAwtTray() {
        val img = logoImage ?: return
        if (!SystemTray.isSupported()) return
        runCatching {
            val sysTray = SystemTray.getSystemTray()
            val traySize = sysTray.trayIconSize
            val scaledImg = img.getScaledInstance(
                traySize.width.coerceAtLeast(24),
                traySize.height.coerceAtLeast(24),
                Image.SCALE_SMOOTH
            )

            val swingMenu = JPopupMenu().apply {
                background = AwtColor(17, 24, 39)
                border = BorderFactory.createLineBorder(AwtColor(31, 41, 55), 1)

                fun addDarkItem(title: String, fg: AwtColor = AwtColor.WHITE, action: () -> Unit) {
                    val item = JMenuItem(title).apply {
                        background = AwtColor(17, 24, 39)
                        foreground = fg
                        font = Font("SansSerif", Font.BOLD, 13)
                        border = BorderFactory.createEmptyBorder(6, 12, 6, 12)
                        addActionListener { action() }
                    }
                    add(item)
                }

                addDarkItem("Show MineServe Desktop", AwtColor(110, 231, 183)) { onActivateWindow() }
                addDarkItem("Hide to System Tray") { onHideWindow() }
                addSeparator()
                addDarkItem("Start All Servers") { onStartAllServers() }
                addDarkItem("Stop All Running Servers") { onStopAllServers() }
                addDarkItem("Open Web Admin in Browser") { onOpenWebAdmin() }
                addSeparator()
                addDarkItem("Quit MineServe", AwtColor(252, 165, 165)) { onQuitApp() }
            }

            val icon = TrayIcon(scaledImg, "MineServe - Linux Desktop").apply {
                isImageAutoSize = true
                addActionListener { onActivateWindow() }
                addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) {
                        if (e.button == MouseEvent.BUTTON1) {
                            onActivateWindow()
                        } else if (e.button == MouseEvent.BUTTON3 || e.isPopupTrigger) {
                            SwingUtilities.invokeLater {
                                val loc = MouseInfo.getPointerInfo()?.location
                                if (loc != null) {
                                    swingMenu.location = loc
                                    swingMenu.invoker = swingMenu
                                    swingMenu.isVisible = true
                                }
                            }
                        }
                    }
                })
            }
            sysTray.add(icon)
            awtTrayIcon = icon
        }
    }

    companion object {
        private val SNI_PYTHON_SCRIPT = """
import sys
import os
import dbus
import dbus.service
import dbus.mainloop.glib
from gi.repository import GLib

ICON_THEME_DIR = sys.argv[1]
ICON_PNG_PATH = sys.argv[2]

def emit(cmd):
    sys.stdout.write(cmd + "\n")
    sys.stdout.flush()

class DbusMenu(dbus.service.Object):
    def __init__(self, bus):
        super().__init__(bus, "/MenuBar")
        self.items = [
            (1, {"label": dbus.String("Show MineServe Desktop", variant_level=1), "enabled": dbus.Boolean(True, variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
            (2, {"label": dbus.String("Hide Window to Tray", variant_level=1), "enabled": dbus.Boolean(True, variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
            (3, {"type": dbus.String("separator", variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
            (4, {"label": dbus.String("Start All Servers", variant_level=1), "enabled": dbus.Boolean(True, variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
            (5, {"label": dbus.String("Stop All Running Servers", variant_level=1), "enabled": dbus.Boolean(True, variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
            (6, {"label": dbus.String("Open Web Admin in Browser", variant_level=1), "enabled": dbus.Boolean(True, variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
            (7, {"type": dbus.String("separator", variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
            (8, {"label": dbus.String("Quit MineServe", variant_level=1), "enabled": dbus.Boolean(True, variant_level=1), "visible": dbus.Boolean(True, variant_level=1)}),
        ]

    @dbus.service.method("org.freedesktop.DBus.Properties", in_signature="ss", out_signature="v")
    def Get(self, interface, prop):
        return self.GetAll(interface).get(prop, dbus.String("", variant_level=1))

    @dbus.service.method("org.freedesktop.DBus.Properties", in_signature="s", out_signature="a{sv}")
    def GetAll(self, interface):
        return dbus.Dictionary({
            "Version": dbus.UInt32(3, variant_level=1),
            "TextDirection": dbus.String("ltr", variant_level=1),
            "Status": dbus.String("normal", variant_level=1),
            "IconThemePath": dbus.Array([dbus.String(ICON_THEME_DIR)], signature="s", variant_level=1),
        }, signature="sv")

    @dbus.service.method("com.canonical.dbusmenu", in_signature="iias", out_signature="u(ia{sv}av)")
    def GetLayout(self, parent_id, recursion_depth, property_names):
        if parent_id == 0:
            children = dbus.Array([], signature="v")
            for item_id, props in self.items:
                child_struct = dbus.Struct(
                    (dbus.Int32(item_id), dbus.Dictionary(props, signature="sv"), dbus.Array([], signature="v")),
                    signature="ia{sv}av",
                    variant_level=1
                )
                children.append(child_struct)
            root_props = dbus.Dictionary({"children-display": dbus.String("submenu", variant_level=1)}, signature="sv")
            return (dbus.UInt32(1), (dbus.Int32(0), root_props, children))
        for item_id, props in self.items:
            if item_id == parent_id:
                return (dbus.UInt32(1), (dbus.Int32(item_id), dbus.Dictionary(props, signature="sv"), dbus.Array([], signature="v")))
        return (dbus.UInt32(1), (dbus.Int32(parent_id), dbus.Dictionary({}, signature="sv"), dbus.Array([], signature="v")))

    @dbus.service.method("com.canonical.dbusmenu", in_signature="aias", out_signature="a(ia{sv})")
    def GetGroupProperties(self, ids, property_names):
        res = dbus.Array([], signature="(ia{sv})")
        for item_id, props in self.items:
            if not ids or item_id in ids:
                res.append((dbus.Int32(item_id), dbus.Dictionary(props, signature="sv")))
        return res

    @dbus.service.method("com.canonical.dbusmenu", in_signature="is", out_signature="v")
    def GetProperty(self, item_id, name):
        for i_id, props in self.items:
            if i_id == item_id and name in props:
                return props[name]
        return dbus.String("", variant_level=1)

    @dbus.service.method("com.canonical.dbusmenu", in_signature="isvu", out_signature="")
    def Event(self, item_id, event_id, data, timestamp):
        if event_id == "clicked":
            self._handle_click(int(item_id))

    @dbus.service.method("com.canonical.dbusmenu", in_signature="a(isvu)", out_signature="ai")
    def EventGroup(self, events):
        for item_id, event_id, data, timestamp in events:
            if event_id == "clicked":
                self._handle_click(int(item_id))
        return dbus.Array([], signature="i")

    @dbus.service.method("com.canonical.dbusmenu", in_signature="i", out_signature="b")
    def AboutToShow(self, item_id):
        return dbus.Boolean(False)

    @dbus.service.method("com.canonical.dbusmenu", in_signature="ai", out_signature="aiai")
    def AboutToShowGroup(self, ids):
        return (dbus.Array([], signature="i"), dbus.Array([], signature="i"))

    def _handle_click(self, item_id):
        if item_id == 1:
            emit("CMD:ACTIVATE")
        elif item_id == 2:
            emit("CMD:HIDE")
        elif item_id == 4:
            emit("CMD:START_ALL")
        elif item_id == 5:
            emit("CMD:STOP_ALL")
        elif item_id == 6:
            emit("CMD:OPEN_WEB")
        elif item_id == 8:
            emit("CMD:QUIT")

class StatusNotifierItem(dbus.service.Object):
    def __init__(self, bus):
        super().__init__(bus, "/StatusNotifierItem")

    @dbus.service.method("org.freedesktop.DBus.Properties", in_signature="ss", out_signature="v")
    def Get(self, interface, prop):
        return self.GetAll(interface).get(prop, dbus.String("", variant_level=1))

    @dbus.service.method("org.freedesktop.DBus.Properties", in_signature="s", out_signature="a{sv}")
    def GetAll(self, interface):
        tooltip = dbus.Struct(
            (
                dbus.String("mineserve"),
                dbus.Array([], signature="(iiay)"),
                dbus.String("MineServe - Linux Desktop"),
                dbus.String("Left-click to show window, right-click for quick actions")
            ),
            signature="sa(iiay)ss",
            variant_level=1
        )
        return dbus.Dictionary({
            "Category": dbus.String("ApplicationStatus", variant_level=1),
            "Id": dbus.String("mineserve", variant_level=1),
            "Title": dbus.String("MineServe", variant_level=1),
            "Status": dbus.String("Active", variant_level=1),
            "WindowId": dbus.Int32(0, variant_level=1),
            "IconName": dbus.String("mineserve", variant_level=1),
            "IconThemePath": dbus.String(ICON_THEME_DIR, variant_level=1),
            "ItemIsMenu": dbus.Boolean(False, variant_level=1),
            "Menu": dbus.ObjectPath("/MenuBar", variant_level=1),
            "ToolTip": tooltip,
        }, signature="sv")

    @dbus.service.method("org.kde.StatusNotifierItem", in_signature="ii", out_signature="")
    def Activate(self, x, y):
        emit("CMD:ACTIVATE")

    @dbus.service.method("org.kde.StatusNotifierItem", in_signature="ii", out_signature="")
    def SecondaryActivate(self, x, y):
        emit("CMD:ACTIVATE")

    @dbus.service.method("org.kde.StatusNotifierItem", in_signature="ii", out_signature="")
    def ContextMenu(self, x, y):
        emit("CMD:ACTIVATE")

    @dbus.service.method("org.kde.StatusNotifierItem", in_signature="is", out_signature="")
    def Scroll(self, delta, orientation):
        pass

def main():
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    bus = dbus.SessionBus()
    bus_name_str = f"org.kde.StatusNotifierItem-{os.getpid()}-1"
    bus_name = dbus.service.BusName(bus_name_str, bus)
    menu = DbusMenu(bus)
    item = StatusNotifierItem(bus)

    watcher = bus.get_object("org.kde.StatusNotifierWatcher", "/StatusNotifierWatcher")
    watcher_iface = dbus.Interface(watcher, "org.kde.StatusNotifierWatcher")
    watcher_iface.RegisterStatusNotifierItem(bus_name_str)

    emit("SNI_READY")
    loop = GLib.MainLoop()
    loop.run()

if __name__ == "__main__":
    main()
""".trimIndent()
    }
}
