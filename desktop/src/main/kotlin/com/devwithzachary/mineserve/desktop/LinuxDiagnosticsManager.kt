package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.GitHubUpdateChecker
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.api.UpdateCheckResult
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.ChangelogPlatform
import com.devwithzachary.mineserve.model.MineServeAboutData
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LinuxSelfTestItem(
    val name: String,
    val passed: Boolean,
    val detail: String
)

object LinuxDiagnosticsManager {

    fun captureLogs(
        filterMode: String?,
        serverRepository: LinuxServerRepository,
        processManager: LinuxProcessManager
    ): String {
        val mode = (filterMode ?: "MINESERVE_TAGS").uppercase()
        val errorsOnly = mode == "ERRORS_ONLY"
        val sb = StringBuilder()

        sb.appendLine("=== MineServe Linux Engine Logs ($mode) ===")
        sb.appendLine("Version: v${MineServeAboutData.APP_VERSION}")
        sb.appendLine("Host: ${LinuxHostTelemetry.getHostDisplayName()}")
        sb.appendLine("------------------------------------------------------------")
        sb.appendLine(CoreLog.getCapturedLogs(errorsOnly = errorsOnly, limit = 600))

        val servers = serverRepository.servers.value
        if (servers.isNotEmpty()) {
            for (srv in servers) {
                val lines = processManager.getRecentConsoleLines(srv.id, 250)
                val filteredLines = if (errorsOnly) {
                    lines.filter {
                        it.contains("ERROR", ignoreCase = true) ||
                            it.contains("WARN", ignoreCase = true) ||
                            it.contains("Exception", ignoreCase = true)
                    }
                } else {
                    lines
                }
                if (filteredLines.isNotEmpty()) {
                    sb.appendLine()
                    sb.appendLine("=== Server Console: ${srv.name} (${srv.type.displayName} ${srv.version}) ===")
                    filteredLines.forEach { line ->
                        sb.appendLine(CoreLog.redactSensitiveText(line))
                    }
                }
            }
        }
        return sb.toString()
    }

    suspend fun runNetworkSelfTest(): List<LinuxSelfTestItem> = withContext(Dispatchers.IO) {
        val checks = mutableListOf<LinuxSelfTestItem>()

        // 1. Local LAN IP & Gateway
        val lanIp = LinuxHostTelemetry.getLocalIpAddress()
        val gwIp = LinuxHostTelemetry.getDefaultGatewayIp()
        checks.add(
            LinuxSelfTestItem(
                name = "Local LAN & Default Gateway",
                passed = !lanIp.isNullOrBlank(),
                detail = "LAN IP: ${lanIp ?: "Not detected"} | Gateway: ${gwIp ?: "Unknown"}"
            )
        )

        // 2. DNS Resolution (bore.pub & api.mineserve.app)
        val dnsRes = runCatching {
            val boreIp = InetAddress.getByName("bore.pub").hostAddress
            val cloudIp = InetAddress.getByName("api.mineserve.app").hostAddress
            "bore.pub -> $boreIp, api.mineserve.app -> $cloudIp"
        }
        checks.add(
            LinuxSelfTestItem(
                name = "DNS Resolution (Tunnel & Cloud Relays)",
                passed = dnsRes.isSuccess,
                detail = dnsRes.getOrElse { "DNS lookup failed: ${it.message}" }
            )
        )

        // 3. Bore TCP Control Port Reachability
        val boreTcp = runCatching {
            Socket().use { sock ->
                sock.connect(InetSocketAddress("bore.pub", 7835), 4000)
            }
            "Connected to bore.pub:7835 TCP control port"
        }
        checks.add(
            LinuxSelfTestItem(
                name = "Bore Public Tunnel Relay (bore.pub:7835)",
                passed = boreTcp.isSuccess,
                detail = boreTcp.getOrElse { "Unreachable: ${it.message}" }
            )
        )

        // 4. MineServe Cloud API Health
        val cloudHealth = runCatching {
            val req = MineServeHttpClient.newRequestBuilder("https://api.mineserve.app/health").get().build()
            MineServeHttpClient.client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) "HTTP ${resp.code} OK from api.mineserve.app"
                else throw IllegalStateException("HTTP ${resp.code}")
            }
        }
        checks.add(
            LinuxSelfTestItem(
                name = "MineServe Cloud API (api.mineserve.app)",
                passed = cloudHealth.isSuccess,
                detail = cloudHealth.getOrElse { "Check failed: ${it.message}" }
            )
        )

        // 5. Linux Host Firewall Status
        val fw = LinuxFirewallChecker.checkInboundPort(25565, isUdp = false)
        checks.add(
            LinuxSelfTestItem(
                name = "Linux Host Inbound Firewall",
                passed = !fw.isActive || fw.isPortConfirmedOpen,
                detail = if (!fw.isActive) {
                    "No active inbound firewall detected"
                } else if (fw.isPortConfirmedOpen) {
                    "${fw.firewallName} active (port 25565 confirmed open)"
                } else {
                    "${fw.firewallName} active - use a tunnel or open port (${fw.fixCommand ?: ""})"
                }
            )
        )

        checks
    }

    suspend fun exportDiagnosticZip(
        paths: LinuxDataPaths,
        filterMode: String?,
        serverRepository: LinuxServerRepository,
        processManager: LinuxProcessManager
    ): File = withContext(Dispatchers.IO) {
        val logs = captureLogs(filterMode, serverRepository, processManager)
        val checks = runNetworkSelfTest()
        val mem = LinuxHostTelemetry.getMemoryInfo()
        val outZip = File(paths.tmpDir, "mineserve-linux-diagnostics-${System.currentTimeMillis()}.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outZip))).use { zos ->
            fun writeEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val sysSummary = buildString {
                appendLine("MineServe Linux Diagnostic Bundle")
                appendLine("Version: v${MineServeAboutData.APP_VERSION}")
                appendLine("Host OS: ${LinuxHostTelemetry.getHostDisplayName()}")
                appendLine("Java Runtime: ${System.getProperty("java.version")} (${System.getProperty("java.vendor")})")
                appendLine("Host Memory: ${mem.totalMb} MB total (${mem.availableMb} MB available)")
                appendLine("Data Directory: ${paths.rootDir.absolutePath}")
                appendLine("Configured Servers: ${serverRepository.servers.value.size}")
            }
            writeEntry("system_info.txt", sysSummary)

            val selfTestReport = checks.joinToString("\n") { c ->
                "[${if (c.passed) "PASS" else "WARN"}] ${c.name}: ${c.detail}"
            }
            writeEntry("network_selftest.txt", selfTestReport)
            writeEntry("mineserve_logs.txt", logs)
        }
        outZip
    }

    fun buildAboutJson(platform: ChangelogPlatform, isPlayStore: Boolean): JSONObject {
        val links = JSONObject()
            .put("website", MineServeAboutData.WEBSITE_URL)
            .put("githubRepo", MineServeAboutData.GITHUB_REPO_URL)
            .put("githubIssues", MineServeAboutData.GITHUB_ISSUES_URL)
            .put("githubReleases", MineServeAboutData.GITHUB_RELEASES_URL)
            .put("discord", MineServeAboutData.DISCORD_URL)
            .put("privacy", MineServeAboutData.PRIVACY_URL)
            .put("license", MineServeAboutData.LICENSE_NAME)
        if (!isPlayStore) {
            links.put("patreon", MineServeAboutData.PATREON_URL)
            links.put("buyMeACoffee", MineServeAboutData.BUYMEACOFFEE_URL)
        }

        val supportersArr = JSONArray()
        if (!isPlayStore) {
            MineServeAboutData.SUPPORTERS.forEach { s ->
                supportersArr.put(JSONObject().put("name", s.name).put("tier", s.tier))
            }
        }

        val creditsArr = JSONArray()
        MineServeAboutData.getCreditsForPlatform(platform).forEach { c ->
            creditsArr.put(
                JSONObject()
                    .put("name", c.name)
                    .put("category", c.category.name)
                    .put("categoryLabel", c.category.label)
                    .put("description", c.description)
                    .put("url", c.url)
                    .put("license", c.license)
                    .put("tag", c.tag)
            )
        }

        val changelogArr = JSONArray()
        MineServeAboutData.getChangelogForPlatform(platform).forEach { entry ->
            changelogArr.put(
                JSONObject()
                    .put("version", entry.version)
                    .put("date", entry.date)
                    .put("initialExpanded", entry.initialExpanded)
                    .put("highlights", JSONArray(entry.highlights))
            )
        }

        return JSONObject()
            .put("version", MineServeAboutData.APP_VERSION)
            .put("versionCode", MineServeAboutData.VERSION_CODE)
            .put("platform", platform.name.lowercase())
            .put("isPlayStore", isPlayStore)
            .put("links", links)
            .put("supporters", supportersArr)
            .put("credits", creditsArr)
            .put("changelog", changelogArr)
    }

    suspend fun checkGitHubUpdatesJson(currentVersion: String = MineServeAboutData.APP_VERSION): JSONObject {
        val checker = GitHubUpdateChecker()
        return when (val res = checker.checkLatestRelease(currentVersion)) {
            is UpdateCheckResult.UpToDate -> JSONObject()
                .put("status", "UP_TO_DATE")
                .put("currentVersion", currentVersion)
                .put("latestTag", res.latestTag.ifBlank { "v$currentVersion" })
                .put("htmlUrl", MineServeAboutData.GITHUB_RELEASES_URL)
            is UpdateCheckResult.UpdateAvailable -> JSONObject()
                .put("status", "UPDATE_AVAILABLE")
                .put("currentVersion", currentVersion)
                .put("latestTag", res.release.tagName)
                .put("releaseName", res.release.name)
                .put("releaseNotes", res.release.body)
                .put("htmlUrl", res.release.htmlUrl)
                .put("publishedAt", res.release.publishedAt)
            is UpdateCheckResult.Error -> JSONObject()
                .put("status", "ERROR")
                .put("currentVersion", currentVersion)
                .put("error", res.message)
                .put("htmlUrl", MineServeAboutData.GITHUB_RELEASES_URL)
        }
    }
}
