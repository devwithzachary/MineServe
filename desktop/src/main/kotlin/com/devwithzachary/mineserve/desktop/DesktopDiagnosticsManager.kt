package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.GitHubUpdateChecker
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.api.UpdateCheckResult
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.model.ChangelogPlatform
import com.devwithzachary.mineserve.model.MineServeAboutData
import com.devwithzachary.mineserve.model.MineServeWhatsNewCatalog
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

data class DesktopSelfTestItem(
    val name: String,
    val passed: Boolean,
    val detail: String
)

object DesktopDiagnosticsManager {

    fun captureLogs(
        filterMode: String?,
        serverRepository: DesktopServerRepository,
        processManager: DesktopProcessManager
    ): String {
        val mode = (filterMode ?: "MINESERVE_TAGS").uppercase()
        val errorsOnly = mode == "ERRORS_ONLY"
        val sb = StringBuilder()

        sb.appendLine("=== MineServe ${DesktopPlatform.shortName} Engine Logs ($mode) ===")
        sb.appendLine("Version: v${MineServeAboutData.APP_VERSION}")
        sb.appendLine("Host: ${DesktopHostTelemetry.getHostDisplayName()}")
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

    suspend fun runNetworkSelfTest(): List<DesktopSelfTestItem> = withContext(Dispatchers.IO) {
        val checks = mutableListOf<DesktopSelfTestItem>()

        // 1. Local LAN IP & Gateway
        val lanIp = DesktopHostTelemetry.getLocalIpAddress()
        val gwIp = DesktopHostTelemetry.getDefaultGatewayIp()
        checks.add(
            DesktopSelfTestItem(
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
            DesktopSelfTestItem(
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
            DesktopSelfTestItem(
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
            DesktopSelfTestItem(
                name = "MineServe Cloud API (api.mineserve.app)",
                passed = cloudHealth.isSuccess,
                detail = cloudHealth.getOrElse { "Check failed: ${it.message}" }
            )
        )

        // 5. Linux Host Firewall Status
        val fw = DesktopFirewallChecker.checkInboundPort(25565, isUdp = false)
        checks.add(
            DesktopSelfTestItem(
                name = "Host Inbound Firewall",
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
        paths: DesktopDataPaths,
        filterMode: String?,
        serverRepository: DesktopServerRepository,
        processManager: DesktopProcessManager
    ): File = withContext(Dispatchers.IO) {
        val logs = captureLogs(filterMode, serverRepository, processManager)
        val checks = runNetworkSelfTest()
        val mem = DesktopHostTelemetry.getMemoryInfo()
        val outZip = File(paths.tmpDir, "mineserve-${DesktopPlatform.platformKey}-diagnostics-${System.currentTimeMillis()}.zip")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outZip))).use { zos ->
            fun writeEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            val sysSummary = buildString {
                appendLine("MineServe ${DesktopPlatform.shortName} Diagnostic Bundle")
                appendLine("Version: v${MineServeAboutData.APP_VERSION}")
                appendLine("Host OS: ${DesktopHostTelemetry.getHostDisplayName()}")
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

    private val prefsLock = Any()

    private fun readDesktopUiJson(paths: DesktopDataPaths): JSONObject = synchronized(prefsLock) {
        val file = File(paths.rootDir, "desktop_ui.json")
        if (!file.exists()) return JSONObject()
        return runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
    }

    private fun writeDesktopUiJson(paths: DesktopDataPaths, updater: (JSONObject) -> Unit) = synchronized(prefsLock) {
        val file = File(paths.rootDir, "desktop_ui.json")
        val obj = if (file.exists()) runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject()) else JSONObject()
        updater(obj)
        runCatching { file.writeText(obj.toString(2)) }
    }

    fun isShowWhatsNewOnUpdateEnabled(paths: DesktopDataPaths): Boolean {
        return readDesktopUiJson(paths).optBoolean("showWhatsNewOnUpdate", true)
    }

    fun setShowWhatsNewOnUpdateEnabled(paths: DesktopDataPaths, enabled: Boolean) {
        writeDesktopUiJson(paths) { it.put("showWhatsNewOnUpdate", enabled) }
    }

    fun getLastSeenWhatsNewVersion(paths: DesktopDataPaths): String {
        return readDesktopUiJson(paths).optString("lastSeenWhatsNewVersion", "")
    }

    fun markWhatsNewSeen(paths: DesktopDataPaths, majorVersion: String = MineServeWhatsNewCatalog.CURRENT_MAJOR_VERSION) {
        writeDesktopUiJson(paths) { it.put("lastSeenWhatsNewVersion", majorVersion) }
    }

    fun shouldAutoShowWhatsNew(paths: DesktopDataPaths, hasExistingServers: Boolean): Boolean {
        val major = MineServeWhatsNewCatalog.CURRENT_MAJOR_VERSION
        if (!hasExistingServers) {
            // Brand-new installation: mark current major release seen so it doesn't pop up
            if (getLastSeenWhatsNewVersion(paths) != major) {
                markWhatsNewSeen(paths, major)
            }
            return false
        }
        return isShowWhatsNewOnUpdateEnabled(paths) && getLastSeenWhatsNewVersion(paths) != major
    }

    fun buildWhatsNewJson(
        platform: ChangelogPlatform,
        showOnUpdateEnabled: Boolean,
        lastSeenVersion: String,
        shouldAutoShow: Boolean
    ): JSONObject {
        val release = MineServeWhatsNewCatalog.getForPlatform(platform)
        val featuresArr = JSONArray()
        release.features.forEach { f ->
            featuresArr.put(
                JSONObject()
                    .put("badge", f.badge)
                    .put("title", f.title)
                    .put("summary", f.summary)
                    .put("stepsToFind", JSONArray(f.stepsToFind))
                    .put("iconKey", f.iconKey)
            )
        }
        return JSONObject()
            .put("majorVersion", release.majorVersion)
            .put("displayVersion", release.displayVersion)
            .put("headline", release.headline)
            .put("subtitle", release.subtitle)
            .put("features", featuresArr)
            .put("showOnUpdateEnabled", showOnUpdateEnabled)
            .put("lastSeenVersion", lastSeenVersion)
            .put("shouldAutoShow", shouldAutoShow)
    }
}
