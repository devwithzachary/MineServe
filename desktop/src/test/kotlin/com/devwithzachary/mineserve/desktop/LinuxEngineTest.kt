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

class LinuxEngineTest {

    @Test
    fun `telemetry reads Linux memory and host info`() {
        val mem = LinuxHostTelemetry.getMemoryInfo()
        assertTrue(mem.totalMb >= 1024L)
        assertTrue(mem.maxAllocatableMb >= 1024)
        assertTrue(mem.defaultRamMb in 1024..mem.maxAllocatableMb)
        assertTrue(LinuxHostTelemetry.getHostDisplayName().isNotBlank())
    }

    @Test
    fun `server repository CRUD, properties, files, and backup retention`() = runBlocking {
        val tempRoot = Files.createTempDirectory("mineserve_linux_repo_test").toFile()
        try {
            val paths = LinuxDataPaths(tempRoot)
            val repo = LinuxServerRepository(paths)

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
        assertEquals("/srv/mineserve", opts.dataDir.path)

        val resetAuto = parseCliArgs(arrayOf("--reset-admin"))
        assertTrue(resetAuto.resetAdmin)
        assertEquals(null, resetAuto.resetPassword)

        val resetCustom = parseCliArgs(arrayOf("--reset-admin", "mySecret99"))
        assertTrue(resetCustom.resetAdmin)
        assertEquals("mySecret99", resetCustom.resetPassword)

        val fw = LinuxFirewallChecker.checkInboundPort(25565, isUdp = false)
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
        assertTrue(unit.contains("ExecStart=/usr/bin/mineserve --headless --bind 0.0.0.0 --port 9090 --data-dir /srv/mineserve"))
    }

    @Test
    fun `linux web server serves auth, system info, and server list`() {
        val tempRoot = Files.createTempDirectory("mineserve_linux_web_test").toFile()
        val paths = LinuxDataPaths(tempRoot)
        val userRepo = UserRepository(paths.rootDir)
        userRepo.ensureDefaultOwner("admin", "secret123")
        val serverRepo = LinuxServerRepository(paths)
        val javaMgr = PortableJavaManager(paths)
        val softwareMgr = LinuxServerSoftwareManager(serverRepo, javaMgr)
        val procMgr = LinuxProcessManager(serverRepo, javaMgr)
        val cloudMgr = LinuxCloudManager(paths)
        val netMgr = LinuxNetworkManager(paths, cloudManager = cloudMgr)

        val web = LinuxWebServer(
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

            // 4. Authenticated /api/system returns Linux Native info
            val sysConn = (URL("$baseUrl/api/system").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
            }
            assertEquals(200, sysConn.responseCode)
            val sysJson = JSONObject(sysConn.inputStream.bufferedReader().readText())
            assertEquals("linux", sysJson.getString("platform"))
            assertTrue(sysJson.getLong("deviceTotalRamMb") >= 1024L)

            // 5. Authenticated /api/servers returns empty array
            val srvConn = (URL("$baseUrl/api/servers").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $token")
            }
            assertEquals(200, srvConn.responseCode)
            val arr = JSONArray(srvConn.inputStream.bufferedReader().readText())
            assertEquals(0, arr.length())
            assertFalse(web.actualPort == 0)
        } finally {
            web.stop()
            tempRoot.deleteRecursively()
        }
    }
}
