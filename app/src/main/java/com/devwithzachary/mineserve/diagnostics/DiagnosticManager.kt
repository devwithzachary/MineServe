package com.devwithzachary.mineserve.diagnostics

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.devwithzachary.mineserve.BuildConfig
import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.cloud.MineServeCloudManager
import com.devwithzachary.mineserve.engine.JavaRuntimeManager
import com.devwithzachary.mineserve.engine.PRootEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class DiagnosticCheckResult(
    val name: String,
    val passed: Boolean,
    val detail: String,
    val latencyMs: Long = 0L
)

enum class LogFilterMode(val label: String) {
    MINESERVE_TAGS("MineServe Tags"),
    ERRORS_ONLY("Errors & Crashes"),
    ALL_APP("All App Logs")
}

object DiagnosticManager {
    private const val TAG = "DiagnosticManager"

    private val relevantTags = setOf(
        "PlayitTunnelClient",
        "BoreTunnelClient",
        "TunnelManager",
        "PortForwardManager",
        "ServerProcessManager",
        "PRootEngine",
        "RootfsManager",
        "WebAdminServer",
        "WebAdminManager",
        "MineServeCloudManager",
        "CloudBackupManager",
        "DiagnosticManager",
        "AndroidRuntime",
        "System.err"
    )

    private val bearerRegex = Regex("""(?i)(Bearer\s+)[A-Za-z0-9._-]+""")
    private val jwtRegex = Regex("""\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b""")
    private val webhookRegex = Regex("""https://(?:ptb\.|canary\.)?discord(?:app)?\.com/api/webhooks/[^\s"'<>]+""")
    private val secretKvRegex = Regex("""(?i)((?:playitSecret|webDavPassword|clientToken|X-Relay-Secret|Authorization|password|secret)\s*[:=]\s*)([^\s,;}"']+)""")
    private val ipv4Regex = Regex("""\b(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b""")

    fun redactSensitiveInfo(input: String): String {
        var out = input
        out = bearerRegex.replace(out) { "${it.groupValues[1]}[REDACTED_TOKEN]" }
        out = jwtRegex.replace(out, "[REDACTED_JWT]")
        out = webhookRegex.replace(out, "https://discord.com/api/webhooks/[REDACTED]")
        out = secretKvRegex.replace(out) { "${it.groupValues[1]}[REDACTED]" }
        out = ipv4Regex.replace(out) { m ->
            val a = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            val b = m.groupValues[2].toIntOrNull() ?: return@replace m.value
            val c = m.groupValues[3].toIntOrNull() ?: return@replace m.value
            val d = m.groupValues[4].toIntOrNull() ?: return@replace m.value
            if (a > 255 || b > 255 || c > 255 || d > 255) return@replace m.value
            val isLocal = (a == 127) ||
                (a == 0 && b == 0 && c == 0 && d == 0) ||
                (a == 10) ||
                (a == 192 && b == 168) ||
                (a == 172 && b in 16..31)
            if (isLocal) m.value else "[REDACTED_IP]"
        }
        return out
    }

    suspend fun captureLogcat(
        filterMode: LogFilterMode = LogFilterMode.MINESERVE_TAGS,
        maxLines: Int = 600
    ): String = withContext(Dispatchers.IO) {
        try {
            val pid = android.os.Process.myPid()
            val process = ProcessBuilder("logcat", "-d", "-v", "threadtime", "--pid=$pid", "-t", maxLines.toString())
                .redirectErrorStream(true)
                .start()

            val lines = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readLines()
            }
            process.waitFor()

            val filtered = when (filterMode) {
                LogFilterMode.ALL_APP -> lines
                LogFilterMode.ERRORS_ONLY -> lines.filter { line ->
                    line.contains(" E ") || line.contains(" F ") ||
                        line.contains("Exception", ignoreCase = true) ||
                        line.contains("Error", ignoreCase = true)
                }
                LogFilterMode.MINESERVE_TAGS -> lines.filter { line ->
                    relevantTags.any { tag -> line.contains(tag) } ||
                        line.contains(" E ") || line.contains(" F ")
                }
            }

            if (filtered.isEmpty()) {
                "No matching logcat entries captured yet for filter: ${filterMode.label}."
            } else {
                redactSensitiveInfo(filtered.joinToString("\n"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed capturing logcat", e)
            "Failed to capture logcat: ${e.message}"
        }
    }

    suspend fun runNetworkSelfTest(context: Context): List<DiagnosticCheckResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiagnosticCheckResult>()

        // 1. Local Network & Default Gateway
        val t0 = System.currentTimeMillis()
        val localIp = findLocalIpv4Address()
        val gateway = findDefaultGateway(context)
        results.add(
            DiagnosticCheckResult(
                name = "Local Network & Gateway",
                passed = localIp != null,
                detail = if (localIp != null) "LAN IP: $localIp | Gateway: ${gateway ?: "none"}" else "No active non-loopback network interface",
                latencyMs = System.currentTimeMillis() - t0
            )
        )

        // 2. DNS Resolution (api.mineserve.app & bore.pub)
        results.add(checkDns("DNS Resolution (bore.pub)", "bore.pub"))
        results.add(checkDns("DNS Resolution (api.mineserve.app)", "api.mineserve.app"))

        // 3. Bore Relay TCP Reachability
        results.add(checkTcp("Bore Tunnel Reachability (bore.pub:7835)", "bore.pub", 7835))

        // 4. Playit.gg Control Reachability
        results.add(checkTcp("Playit.gg Reachability (api.playit.gg:443)", "api.playit.gg", 443))

        // 5. MineServe Cloud API /health
        results.add(checkCloudApiHealth())

        // 6. UPnP SSDP Discovery
        results.add(checkUpnpDiscovery())

        results
    }

    private fun checkDns(name: String, host: String): DiagnosticCheckResult {
        val start = System.currentTimeMillis()
        return try {
            val addrs = InetAddress.getAllByName(host)
            val elapsed = System.currentTimeMillis() - start
            DiagnosticCheckResult(
                name = name,
                passed = addrs.isNotEmpty(),
                detail = "Resolved ${addrs.size} record(s)",
                latencyMs = elapsed
            )
        } catch (e: Exception) {
            DiagnosticCheckResult(
                name = name,
                passed = false,
                detail = e.message ?: "DNS resolution failed",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    private fun checkTcp(name: String, host: String, port: Int): DiagnosticCheckResult {
        val start = System.currentTimeMillis()
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
            }
            DiagnosticCheckResult(
                name = name,
                passed = true,
                detail = "TCP handshake succeeded",
                latencyMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            DiagnosticCheckResult(
                name = name,
                passed = false,
                detail = e.message ?: "TCP connection failed",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    private fun checkCloudApiHealth(): DiagnosticCheckResult {
        val start = System.currentTimeMillis()
        val url = "${MineServeCloudManager.apiBaseUrl}/health"
        return try {
            val req = MineServeHttpClient.newGetRequest(url)
            MineServeHttpClient.newCall(req).execute().use { resp ->
                val elapsed = System.currentTimeMillis() - start
                DiagnosticCheckResult(
                    name = "MineServe Cloud API",
                    passed = resp.isSuccessful,
                    detail = "HTTP ${resp.code}",
                    latencyMs = elapsed
                )
            }
        } catch (e: Exception) {
            DiagnosticCheckResult(
                name = "MineServe Cloud API",
                passed = false,
                detail = e.message ?: "Unreachable",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    private fun checkUpnpDiscovery(): DiagnosticCheckResult {
        val start = System.currentTimeMillis()
        return try {
            val msg = (
                "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 1\r\n" +
                    "ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n\r\n"
                ).toByteArray(Charsets.UTF_8)

            DatagramSocket().use { sock ->
                sock.soTimeout = 1500
                val group = InetAddress.getByName("239.255.255.250")
                sock.send(DatagramPacket(msg, msg.size, group, 1900))
                val buf = ByteArray(1024)
                val reply = DatagramPacket(buf, buf.size)
                sock.receive(reply)
                val elapsed = System.currentTimeMillis() - start
                DiagnosticCheckResult(
                    name = "UPnP Router Discovery",
                    passed = true,
                    detail = "IGD router responded (${reply.address?.hostAddress ?: "local"})",
                    latencyMs = elapsed
                )
            }
        } catch (e: Exception) {
            DiagnosticCheckResult(
                name = "UPnP Router Discovery",
                passed = false,
                detail = "No UPnP IGD response (${e.javaClass.simpleName})",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    private fun findLocalIpv4Address(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { addr -> addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun findDefaultGateway(context: Context): String? {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
            val active = cm.activeNetwork ?: return null
            val props = cm.getLinkProperties(active) ?: return null
            props.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    suspend fun exportDiagnosticBundle(
        context: Context,
        logText: String,
        networkResults: List<DiagnosticCheckResult>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(dir, "mineserve_diagnostics_$stamp.zip")

            val proot = PRootEngine(context)
            val javaMgr = JavaRuntimeManager(context, proot)
            val installedJavas = listOf(8, 17, 21, 25).filter { javaMgr.isJavaInstalled(it) }
            val summary = buildString {
                appendLine("MineServe Diagnostics Bundle")
                appendLine("Generated: ${Date()}")
                appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
                appendLine("Rootfs Installed: ${proot.isRootfsInstalled()}")
                appendLine("Installed Java Runtimes: ${installedJavas.joinToString(", ").ifBlank { "none" }}")
            }

            val networkText = if (networkResults.isEmpty()) {
                "Network self-test was not run prior to export."
            } else {
                networkResults.joinToString("\n") { r ->
                    "[${if (r.passed) "PASS" else "FAIL"}] ${r.name} (${r.latencyMs}ms): ${r.detail}"
                }
            }

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                writeZipEntry(zos, "system_summary.txt", summary)
                writeZipEntry(zos, "network_selftest.txt", redactSensitiveInfo(networkText))
                writeZipEntry(zos, "logcat_redacted.txt", redactSensitiveInfo(logText))
            }
            zipFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed exporting diagnostic bundle", e)
            null
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, content: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    fun getShareIntent(context: Context, zipFile: File): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, zipFile.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating diagnostic share intent", e)
            null
        }
    }

    suspend fun saveBundleToDownloads(context: Context, zipFile: File): String? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, zipFile.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MineServe")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(zipFile).use { input -> input.copyTo(out) }
                }
                "Downloads/MineServe/${zipFile.name}"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destDir = File(downloadsDir, "MineServe").apply { mkdirs() }
                val dest = File(destDir, zipFile.name)
                zipFile.copyTo(dest, overwrite = true)
                dest.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving diagnostic bundle to Downloads", e)
            null
        }
    }
}

