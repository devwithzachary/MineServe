package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.auth.UserRepository
import com.devwithzachary.mineserve.model.ServerType
import com.devwithzachary.mineserve.util.ServerModFilter
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopEngineTest {

    @Test
    fun `telemetry reads Linux memory and host info`() {
        val mem = DesktopHostTelemetry.getMemoryInfo()
        assertTrue(mem.totalMb >= 1024L)
        assertTrue(mem.maxAllocatableMb >= 1024)
        assertTrue(mem.defaultRamMb in 1024..mem.maxAllocatableMb)
        assertTrue(DesktopHostTelemetry.getHostDisplayName().isNotBlank())
    }

    @Test
    fun `server repository CRUD, properties, files, and backup retention`() = runBlocking {
        val tempRoot = Files.createTempDirectory("mineserve_linux_repo_test").toFile()
        try {
            val paths = DesktopDataPaths(tempRoot)
            val repo = DesktopServerRepository(paths)

            val created = repo.createServer(
                name = "Linux Survival",
                type = ServerType.PAPER,
                version = "26.1.2",
                port = 25565,
                ramMb = 2048,
                motd = "Hello Linux"
            )
            assertNotNull(repo.getServer(created.id))
            assertEquals(25, created.javaVersion)
            assertEquals("Hello Linux", repo.loadServerProperties(created.id).motd)

            // File operations
            assertTrue(repo.createDirectory(created.id, "", "world"))
            assertTrue(repo.createFile(created.id, "world", "level.txt", "seed=12345"))
            assertEquals("seed=12345", repo.readFile(created.id, "world/level.txt"))
            assertTrue(repo.renameFileOrDirectory(created.id, "world/level.txt", "level_renamed.txt"))
            assertEquals("seed=12345", repo.readFile(created.id, "world/level_renamed.txt"))

            // Backup retention policy (keep max 2)
            repeat(4) {
                Thread.sleep(15)
                assertTrue(repo.createBackup(created.id, worldOnly = true, retentionLimit = 2).isSuccess)
            }
            assertEquals(2, repo.listBackups(created.id).size)

            assertTrue(repo.deleteServer(created.id))
            assertEquals(0, repo.servers.value.size)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `cli parser and systemd service unit generation`() {
        val opts = parseCliArgs(arrayOf("--headless", "--verbose", "--port", "9090", "--bind", "127.0.0.1", "--data-dir", "/srv/mineserve"))
        assertTrue(opts.headless)
        assertTrue(opts.verbose)
        assertEquals(9090, opts.port)
        assertEquals("127.0.0.1", opts.bindHost)
        assertEquals(File("/srv/mineserve").path, opts.dataDir.path)

        val resetAuto = parseCliArgs(arrayOf("--reset-admin"))
        assertTrue(resetAuto.resetAdmin)
        assertEquals(null, resetAuto.resetPassword)

        val resetCustom = parseCliArgs(arrayOf("--reset-admin", "mySecret99"))
        assertTrue(resetCustom.resetAdmin)
        assertEquals("mySecret99", resetCustom.resetPassword)

        val fw = DesktopFirewallChecker.checkInboundPort(25565, isUdp = false)
        assertNotNull(fw.firewallName)

        val tempModsRoot = Files.createTempDirectory("mineserve_modfilter_test").toFile()
        try {
            val modsDir = File(tempModsRoot, "mods").apply { mkdirs() }
            File(modsDir, "ImmediatelyFast-NeoForge-1.17.1+26.3.jar").writeText("client-mod")
            File(modsDir, "[钠] sodium-neoforge-0.9.3.jar").writeText("client-mod-2")
            File(modsDir, "lithium-neoforge-0.26.3.jar").writeText("server-mod")
            val disabled = ServerModFilter.disableClientOnlyMods(tempModsRoot)
            assertEquals(2, disabled.size)
            assertTrue(File(modsDir, "ImmediatelyFast-NeoForge-1.17.1+26.3.jar.disabled").exists())
            assertTrue(File(modsDir, "[钠] sodium-neoforge-0.9.3.jar.disabled").exists())
            assertTrue(File(modsDir, "lithium-neoforge-0.26.3.jar").exists())
        } finally {
            tempModsRoot.deleteRecursively()
        }

        val unit = SystemdServiceInstaller.generateServiceUnit(
            user = "mc",
            execPath = "/usr/bin/mineserve",
            dataDir = File("/srv/mineserve"),
            port = 9090,
            bindHost = "0.0.0.0"
        )
        assertTrue(unit.contains("User=mc"))
        assertTrue(unit.contains("ExecStart=/usr/bin/mineserve --headless --bind 0.0.0.0 --port 9090 --data-dir ${File("/srv/mineserve").absolutePath}"))
    }

    @Test
    fun `linux web server serves auth, system info, and server list`() {
        val tempRoot = Files.createTempDirectory("mineserve_linux_web_test").toFile()
        val paths = DesktopDataPaths(tempRoot)
        val userRepo = UserRepository(paths.rootDir)
        userRepo.ensureDefaultOwner("admin", "secret123")
        val serverRepo = DesktopServerRepository(paths)
        val javaMgr = PortableJavaManager(paths)
        val softwareMgr = DesktopServerSoftwareManager(serverRepo, javaMgr)
        val procMgr = DesktopProcessManager(serverRepo, javaMgr)
        val cloudMgr = DesktopCloudManager(paths)
        val netMgr = DesktopNetworkManager(paths, cloudManager = cloudMgr)

        val web = DesktopWebServer(
            bindHost = "127.0.0.1",
            port = 0, // ephemeral free port
            paths = paths,
            serverRepository = serverRepo,
            processManager = procMgr,
            softwareManager = softwareMgr,
            javaManager = javaMgr,
            networkManager = netMgr,
            cloudManager = cloudMgr,
            userRepository = userRepo
        )
        try {
            assertTrue(web.start())
            val baseUrl = "http://127.0.0.1:${web.actualPort}"

            // 1. Static index.html loads
            val indexConn = (URL("$baseUrl/").openConnection() as HttpURLConnection)
            assertEquals(200, indexConn.responseCode)
            val html = indexConn.inputStream.bufferedReader().readText()
            assertTrue(html.contains("MineServe"))

            // 2. Unauthenticated /api/me returns 401
            val unauthConn = (URL("$baseUrl/api/me").openConnection() as HttpURLConnection)
            assertEquals(401, unauthConn.responseCode)

            // 3. Login returns bearer token
            val loginConn = (URL("$baseUrl/api/login").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.write("""{"username":"admin","password":"secret123"}""".toByteArray())
            }
            assertEquals(200, loginConn.responseCode)
            val loginJson = JSONObject(loginConn.inputStream.bufferedReader().readText())
            val token = loginJson.getString("token")
            assertTrue(token.isNotBlank())

            // 4. Authenticated /api/system returns Desktop Native info
            val sysConn = (URL("$baseUrl/api/system").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
            }
            assertEquals(200, sysConn.responseCode)
            val sysJson = JSONObject(sysConn.inputStream.bufferedReader().readText())
            assertEquals(DesktopPlatform.platformKey, sysJson.getString("platform"))
            assertTrue(sysJson.getBoolean("isDesktopHost"))
            assertTrue(sysJson.getLong("deviceTotalRamMb") >= 1024L)

            // 5. Authenticated /api/servers returns empty array
            val srvConn = (URL("$baseUrl/api/servers").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
            }
            assertEquals(200, srvConn.responseCode)
            val arr = JSONArray(srvConn.inputStream.bufferedReader().readText())
            assertEquals(0, arr.length())
            assertFalse(web.actualPort == 0)

            // 6. Sibling-directory traversal rejected in DesktopServerRepository
            val paperSrv = serverRepo.createServer("Survival", ServerType.PAPER, "1.21.4", 25565, 2048, "MOTD")
            val siblingDir = File(paths.serversDir, "${paperSrv.id}_evil").apply { mkdirs() }
            File(siblingDir, "secret.txt").writeText("secret")
            assertTrue(serverRepo.listDirectory(paperSrv.id, "../${paperSrv.id}_evil").isEmpty())
            assertFalse(serverRepo.writeFile("../${paperSrv.id}_evil", "pwn.txt", "bad"))

            // 7. Binary upload cannot target `..` to overwrite panel_users.json
            val origUsersBytes = File(paths.rootDir, "panel_users.json").readBytes()
            val badUpload = (URL("$baseUrl/api/servers/../files/upload?name=panel_users.json").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                outputStream.write("""[]""".toByteArray())
            }
            assertTrue(badUpload.responseCode in listOf(400, 404))
            assertEquals(String(origUsersBytes), File(paths.rootDir, "panel_users.json").readText())

            // 8. GET /delete is rejected with 405 and unknown action returns 404
            val getDelConn = (URL("$baseUrl/api/servers/${paperSrv.id}/delete").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
            }
            assertEquals(405, getDelConn.responseCode)
            assertNotNull(serverRepo.getServer(paperSrv.id))

            val unknownConn = (URL("$baseUrl/api/servers/${paperSrv.id}/does_not_exist").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
            }
            assertEquals(404, unknownConn.responseCode)

            // 9. Scheduled task toggle & delete operate cleanly without duplicating tasks
            val addTaskConn = (URL("$baseUrl/api/servers/${paperSrv.id}/automation/tasks/add").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                outputStream.write("""{"name":"Nightly","taskType":"RESTART","frequency":"INTERVAL","intervalMinutes":60}""".toByteArray())
            }
            assertEquals(200, addTaskConn.responseCode)
            val addedTask = serverRepo.getServer(paperSrv.id)!!.automationConfig.scheduledTasks.single()
            assertTrue(addedTask.enabled)

            val toggleConn = (URL("$baseUrl/api/servers/${paperSrv.id}/automation/tasks/toggle").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                outputStream.write("""{"taskId":"${addedTask.id}"}""".toByteArray())
            }
            assertEquals(200, toggleConn.responseCode)
            val afterToggle = serverRepo.getServer(paperSrv.id)!!.automationConfig.scheduledTasks
            assertEquals(1, afterToggle.size)
            assertFalse(afterToggle.single().enabled)

            val delTaskConn = (URL("$baseUrl/api/servers/${paperSrv.id}/automation/tasks/delete").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                outputStream.write("""{"taskId":"${addedTask.id}"}""".toByteArray())
            }
            assertEquals(200, delTaskConn.responseCode)
            assertEquals(0, serverRepo.getServer(paperSrv.id)!!.automationConfig.scheduledTasks.size)

            // 10. Players endpoint reads whitelist.json & ops.json from disk
            val srvDir = serverRepo.getServerDirectory(paperSrv.id)
            File(srvDir, "whitelist.json").writeText("""[{"uuid":"00000000-0000-0000-0000-000000000001","name":"Steve"}]""")
            File(srvDir, "ops.json").writeText("""[{"uuid":"00000000-0000-0000-0000-000000000001","name":"Steve","level":4}]""")
            val playersConn = (URL("$baseUrl/api/servers/${paperSrv.id}/players").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
            }
            assertEquals(200, playersConn.responseCode)
            val playersJson = JSONObject(playersConn.inputStream.bufferedReader().readText())
            assertEquals("Steve", playersJson.getJSONArray("whitelist").getJSONObject(0).getString("name"))
            assertEquals(4, playersJson.getJSONArray("ops").getJSONObject(0).getInt("level"))

            // 11. Velocity proxy config persists routes, writes velocity.toml, and binds backend loopback
            val proxySrv = serverRepo.createServer("LobbyProxy", ServerType.VELOCITY, "3.4.0", 25577, 1024, "Proxy")
            val proxySaveConn = (URL("$baseUrl/api/servers/${proxySrv.id}/proxy").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                outputStream.write("""{"onlineMode":true,"linkedServerIds":["${paperSrv.id}"],"bindBackendsLoopback":true}""".toByteArray())
            }
            assertEquals(200, proxySaveConn.responseCode)
            val proxyDir = serverRepo.getServerDirectory(proxySrv.id)
            val velocityToml = File(proxyDir, "velocity.toml").readText()
            assertTrue(velocityToml.contains("""127.0.0.1:${paperSrv.port}"""))
            assertTrue(File(proxyDir, "forwarding.secret").exists())
            val backendProps = serverRepo.loadServerProperties(paperSrv.id)
            assertEquals("127.0.0.1", backendProps.serverIp)
            assertFalse(backendProps.onlineMode)
        } finally {
            web.stop()
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `DesktopDataPaths updateDirectories persists custom paths and resets cleanly`() {
        val tempRoot = Files.createTempDirectory("mineserve-paths-test").toFile()
        try {
            val customServers = File(tempRoot, "custom-servers")
            val customRuntimes = File(tempRoot, "custom-runtimes")
            val regularFile = File(tempRoot, "not-a-dir").apply { writeText("x") }

            val paths = DesktopDataPaths(tempRoot)
            assertTrue(paths.updateDirectories("relative/path", null).isFailure)
            assertTrue(paths.updateDirectories(regularFile.absolutePath, null).isFailure)

            val updated = paths.updateDirectories(customServers.absolutePath, customRuntimes.absolutePath)
            assertTrue(updated.isSuccess)
            assertEquals(customServers.absolutePath, paths.serversDir.absolutePath)
            assertEquals(customRuntimes.absolutePath, paths.runtimesDir.absolutePath)

            // Reload from desktop_config.json
            val reloaded = DesktopDataPaths(tempRoot)
            assertEquals(customServers.absolutePath, reloaded.serversDir.absolutePath)
            assertEquals(customRuntimes.absolutePath, reloaded.runtimesDir.absolutePath)

            // Blank resets to defaults
            val reset = reloaded.updateDirectories("", "")
            assertTrue(reset.isSuccess)
            assertEquals(reloaded.defaultServersDir.absolutePath, reloaded.serversDir.absolutePath)
            assertEquals(reloaded.defaultRuntimesDir.absolutePath, reloaded.runtimesDir.absolutePath)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `AuditLogCsvExporter escapes cells and formats ISO timestamps`() {
        val entries = listOf(
            com.devwithzachary.mineserve.auth.AuditLogEntry(
                id = "audit-1",
                timestamp = 0L,
                username = "alex_mod",
                role = "MODERATOR",
                action = "CONSOLE_CMD",
                target = "srv-1",
                details = "say \"hello\", world",
                clientIp = "127.0.0.1"
            )
        )
        val csv = com.devwithzachary.mineserve.auth.AuditLogCsvExporter.toCsv(entries).trim().lines()
        assertEquals(2, csv.size)
        assertEquals("Timestamp,Username,Role,Action,Target,Details,IP", csv[0])
        assertEquals(
            """"1970-01-01T00:00:00.000Z","alex_mod","MODERATOR","CONSOLE_CMD","srv-1","say ""hello"", world","127.0.0.1"""",
            csv[1]
        )
    }

    @Test
    fun `DesktopPlatform detects OS names and default data paths across Linux, macOS, and Windows`() {
        assertEquals(DesktopOs.LINUX, DesktopPlatform.detectOsFromName("Linux"))
        assertEquals(DesktopOs.MACOS, DesktopPlatform.detectOsFromName("Mac OS X"))
        assertEquals(DesktopOs.MACOS, DesktopPlatform.detectOsFromName("Darwin"))
        assertEquals(DesktopOs.WINDOWS, DesktopPlatform.detectOsFromName("Windows 11"))

        assertEquals(
            File("/home/zach/.local/share/mineserve").path,
            DesktopDataPaths.defaultDataDirFor(DesktopOs.LINUX, "/home/zach", emptyMap()).path
        )
        assertEquals(
            File("/custom/xdg/mineserve").path,
            DesktopDataPaths.defaultDataDirFor(DesktopOs.LINUX, "/home/zach", mapOf("XDG_DATA_HOME" to "/custom/xdg")).path
        )
        assertEquals(
            File("/Users/zach/Library/Application Support/MineServe").path,
            DesktopDataPaths.defaultDataDirFor(DesktopOs.MACOS, "/Users/zach", emptyMap()).path
        )
        assertEquals(
            File("C:/Users/zach/AppData/Local/MineServe").path,
            DesktopDataPaths.defaultDataDirFor(DesktopOs.WINDOWS, "C:/Users/zach", emptyMap()).path
        )
        assertEquals(
            File("D:/LocalAppData/MineServe").path,
            DesktopDataPaths.defaultDataDirFor(DesktopOs.WINDOWS, "C:/Users/zach", mapOf("LOCALAPPDATA" to "D:/LocalAppData")).path
        )
    }

    @Test
    fun `PortableJavaManager builds Adoptium URLs including macOS arm64 Java 8 Rosetta fallback`() {
        assertEquals(
            "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jre/hotspot/normal/eclipse",
            PortableJavaManager.adoptiumDownloadUrl(21, DesktopOs.LINUX, "x64")
        )
        assertEquals(
            "https://api.adoptium.net/v3/binary/latest/21/ga/mac/aarch64/jre/hotspot/normal/eclipse",
            PortableJavaManager.adoptiumDownloadUrl(21, DesktopOs.MACOS, "aarch64")
        )
        assertEquals(
            "https://api.adoptium.net/v3/binary/latest/8/ga/mac/x64/jre/hotspot/normal/eclipse",
            PortableJavaManager.adoptiumDownloadUrl(8, DesktopOs.MACOS, "aarch64")
        )
        assertEquals(
            "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse",
            PortableJavaManager.adoptiumDownloadUrl(21, DesktopOs.WINDOWS, "x64")
        )
    }

    @Test
    fun `SystemdServiceInstaller generates launchd plist, Windows startup script, and host telemetry runs safely`() {
        val plist = SystemdServiceInstaller.generateLaunchdPlist(
            execPath = "/Applications/MineServe.app/Contents/MacOS/MineServe",
            dataDir = File("/Users/zach/Library/Application Support/MineServe"),
            port = 8080,
            bindHost = "0.0.0.0"
        )
        assertTrue(plist.contains("<string>com.devwithzachary.mineserve</string>"))
        assertTrue(plist.contains("<string>--headless</string>"))

        val bat = SystemdServiceInstaller.generateWindowsStartupScript(
            execPath = "C:\\Program Files\\MineServe\\MineServe.exe",
            dataDir = File("C:/Users/zach/AppData/Local/MineServe"),
            port = 8080,
            bindHost = "0.0.0.0"
        )
        assertTrue(bat.contains("--headless --bind 0.0.0.0 --port 8080"))

        val selfPid = ProcessHandle.current().pid()
        assertTrue(DesktopHostTelemetry.getProcessCpuPercent(selfPid) >= 0f)
        assertTrue(DesktopHostTelemetry.getProcessRssMb(selfPid) >= 0L)
    }
}
