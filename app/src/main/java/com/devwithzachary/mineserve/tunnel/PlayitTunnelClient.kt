package com.devwithzachary.mineserve.tunnel

import android.content.Context
import android.os.Build
import android.util.Log
import com.devwithzachary.mineserve.engine.PRootConfig
import com.devwithzachary.mineserve.engine.PRootEngine
import com.devwithzachary.mineserve.model.TunnelProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayitTunnelClient(
    private val context: Context,
    private val localPort: Int = 25565,
    private val secret: String = "",
    private val onStateChanged: (TunnelState) -> Unit
) {
    companion object {
        private const val TAG = "PlayitTunnelClient"
        private const val PLAYIT_VERSION = "v0.15.26"
        private val ANSI_REGEX = Regex("\u001B\\[[0-9;]*[a-zA-Z]")
        private val CLAIM_URL_REGEX = Regex("https://playit\\.gg/claim/[a-zA-Z0-9_-]+")

        // 1. Matches: "abc.ply.gg:12345 => 127.0.0.1:25565" or "cool.joinmc.link => 127.0.0.1:25565" or "147.185.221.16:12345 => 127.0.0.1:25565"
        private val MAPPING_ARROW_REGEX = Regex("""([a-zA-Z0-9_.-]+(?::[0-9]+)?)\s*=>\s*(?:127\.0\.0\.1|localhost|0\.0\.0\.0|\[::1\]|local)""", RegexOption.IGNORE_CASE)

        // 2. Matches playit / joinmc domains: "abc.ply.gg:12345" or "xyz.gl.joinmc.link" or "myname.playit.gg:25565"
        private val DOMAIN_ADDR_REGEX = Regex("""\b([a-zA-Z0-9_.-]+\.(?:ply\.gg|joinmc\.link|playit\.gg))(?::([0-9]+))?\b""", RegexOption.IGNORE_CASE)

        // 3. Matches IP:port before arrow: "147.185.221.16:12345 =>"
        private val IP_PORT_ARROW_REGEX = Regex("""\b((?:\d{1,3}\.){3}\d{1,3}):([0-9]+)\s*=>""")

        private fun stripAnsi(text: String): String {
            return ANSI_REGEX.replace(text, "")
        }
    }

    private val pRootEngine = PRootEngine(context)
    private var tunnelJob: Job? = null
    private var process: Process? = null
    private val isExplicitlyStopped = AtomicBoolean(false)

    fun start(scope: CoroutineScope) {
        stop()
        isExplicitlyStopped.set(false)
        tunnelJob = scope.launch(Dispatchers.IO) {
            runPlayitLoop(scope)
        }
    }

    fun stop() {
        isExplicitlyStopped.set(true)
        tunnelJob?.cancel()
        tunnelJob = null
        try {
            process?.destroyForcibly()
        } catch (_: Exception) {}
        process = null
        onStateChanged(TunnelState.Disconnected)
    }

    private suspend fun runPlayitLoop(parentScope: CoroutineScope) {
        val effectiveLocalPort = if (localPort in 1..65535) localPort else 25565
        val usrLocalBin = File(pRootEngine.rootfsDir, "usr/local/bin").apply { mkdirs() }
        val playitBin = File(usrLocalBin, "playit")

        while (parentScope.isActive && !isExplicitlyStopped.get()) {
            try {
                if (isExplicitlyStopped.get()) break
                onStateChanged(TunnelState.Connecting("Initializing Playit.gg agent..."))

                // 1. Ensure binary exists inside rootfs
                if (!playitBin.exists() || playitBin.length() == 0L || !playitBin.canExecute()) {
                    onStateChanged(TunnelState.Connecting("Downloading Playit.gg agent..."))
                    downloadBinary(playitBin)
                }
                playitBin.setExecutable(true, false)

                // 2. Prepare PRoot command
                val innerCmd = mutableListOf("/usr/local/bin/playit", "--stdout")
                if (secret.isNotBlank()) {
                    innerCmd.add("--secret")
                    innerCmd.add(secret.trim())
                }
                innerCmd.add("start")

                val prootCmd = pRootEngine.buildPRootCommand(
                    command = innerCmd,
                    config = PRootConfig(
                        rootfsDir = pRootEngine.rootfsDir,
                        tmpDir = pRootEngine.tmpDir,
                        workingDir = "/root"
                    ),
                    loginUser = "root"
                )

                Log.i(TAG, "Starting Playit PRoot process: ${prootCmd.joinToString(" ")}")
                onStateChanged(TunnelState.Connecting("Connecting to Playit.gg network..."))

                val pb = ProcessBuilder(prootCmd)
                pb.directory(pRootEngine.tmpDir)
                val envMap = pRootEngine.getEnvironmentVariables("root")
                for ((k, v) in envMap) {
                    pb.environment()[k] = v
                }
                if (secret.isNotBlank()) {
                    pb.environment()["PLAYIT_SECRET"] = secret.trim()
                }
                pb.redirectErrorStream(true)

                val proc = pb.start()
                process = proc

                val reader = BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8))
                var assignedAddress: String? = null
                var hasClaimed = false

                // Parallel coroutine to query Playit REST API using the secret key
                val apiPollJob = parentScope.launch(Dispatchers.IO) {
                    while (isActive && !isExplicitlyStopped.get()) {
                        val currentSecret = findSecretKey()
                        if (currentSecret != null) {
                            val runData = fetchPlayitTunnels(currentSecret)
                            if (runData != null) {
                                hasClaimed = true
                                if (runData.tunnels.isNotEmpty()) {
                                    val matchingTunnel = runData.tunnels.firstOrNull { it.localPort == effectiveLocalPort }
                                        ?: runData.tunnels.first()

                                    val host = matchingTunnel.customDomain?.ifBlank { null }
                                        ?: matchingTunnel.assignedDomain
                                    val port = matchingTunnel.portFrom
                                    val full = if (port == 25565 && host.endsWith("joinmc.link")) host else "$host:$port"

                                    if (assignedAddress != full) {
                                        assignedAddress = full
                                        Log.i(TAG, "Playit tunnel active from REST API: $full")
                                        onStateChanged(
                                            TunnelState.Connected(
                                                publicHost = host,
                                                publicPort = port,
                                                fullAddress = full,
                                                provider = TunnelProvider.PLAYIT
                                            )
                                        )
                                    }
                                } else {
                                    if (assignedAddress == null) {
                                        onStateChanged(
                                            TunnelState.Connecting(
                                                message = "Playit online: No tunnel created on playit.gg",
                                                claimUrl = null
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        delay(2500L)
                    }
                }

                while (parentScope.isActive && !isExplicitlyStopped.get()) {
                    val rawLine = reader.readLine() ?: break
                    val line = stripAnsi(rawLine).trim()
                    Log.d(TAG, "[playit] $line")

                    // 1. Check for Claim URL
                    val claimMatch = CLAIM_URL_REGEX.find(line)
                    if (claimMatch != null && !hasClaimed && assignedAddress == null) {
                        val claimUrl = claimMatch.value
                        onStateChanged(
                            TunnelState.Connecting(
                                message = "Playit setup: Claim tunnel to connect",
                                claimUrl = claimUrl
                            )
                        )
                        continue
                    }

                    // 2. Check for Claim Authorization / Setup Complete indicators
                    if (!hasClaimed && (
                        line.contains("Loaded secret", ignoreCase = true) ||
                        line.contains("Registered agent", ignoreCase = true) ||
                        line.contains("Agent registered", ignoreCase = true) ||
                        line.contains("Authenticated with playit", ignoreCase = true) ||
                        line.contains("starting up tunnel connection", ignoreCase = true) ||
                        line.contains("tunnel running", ignoreCase = true) ||
                        line.contains("TUNNELS", ignoreCase = true) ||
                        line.contains("setup, secret written", ignoreCase = true)
                    )) {
                        hasClaimed = true
                        if (assignedAddress == null) {
                            onStateChanged(
                                TunnelState.Connecting(
                                    message = "Playit agent claimed! Loading tunnels...",
                                    claimUrl = null
                                )
                            )
                        }
                    }

                    // 3. Check for Mapping Arrow (e.g. "abc.ply.gg:12345 => 127.0.0.1:25565")
                    val arrowMatch = MAPPING_ARROW_REGEX.find(line)
                    if (arrowMatch != null) {
                        val fullHostPort = arrowMatch.groupValues[1]
                        val host = if (fullHostPort.contains(':')) fullHostPort.substringBefore(':') else fullHostPort
                        val port = if (fullHostPort.contains(':')) {
                            fullHostPort.substringAfter(':').toIntOrNull() ?: 25565
                        } else 25565

                        assignedAddress = "$host:$port"
                        hasClaimed = true
                        Log.i(TAG, "Playit public tunnel connected via arrow mapping: $assignedAddress")
                        onStateChanged(
                            TunnelState.Connected(
                                publicHost = host,
                                publicPort = port,
                                fullAddress = assignedAddress,
                                provider = TunnelProvider.PLAYIT
                            )
                        )
                        continue
                    }

                    // 4. Check for Domain Match (e.g. "subdomain.ply.gg:12345" or "foo.joinmc.link")
                    val domainMatch = DOMAIN_ADDR_REGEX.find(line)
                    if (domainMatch != null && assignedAddress == null) {
                        val host = domainMatch.groupValues[1]
                        val portStr = domainMatch.groupValues.getOrNull(2)
                        val port = if (!portStr.isNullOrBlank()) portStr.toIntOrNull() ?: 25565 else 25565
                        assignedAddress = "$host:$port"
                        hasClaimed = true
                        Log.i(TAG, "Playit public tunnel connected via domain match: $assignedAddress")
                        onStateChanged(
                            TunnelState.Connected(
                                publicHost = host,
                                publicPort = port,
                                fullAddress = assignedAddress,
                                provider = TunnelProvider.PLAYIT
                            )
                        )
                        continue
                    }

                    // 5. Check for IP:Port before arrow match
                    val ipMatch = IP_PORT_ARROW_REGEX.find(line)
                    if (ipMatch != null && assignedAddress == null) {
                        val host = ipMatch.groupValues[1]
                        val port = ipMatch.groupValues[2].toIntOrNull() ?: 25565
                        assignedAddress = "$host:$port"
                        hasClaimed = true
                        Log.i(TAG, "Playit public tunnel connected via IP mapping: $assignedAddress")
                        onStateChanged(
                            TunnelState.Connected(
                                publicHost = host,
                                publicPort = port,
                                fullAddress = assignedAddress,
                                provider = TunnelProvider.PLAYIT
                            )
                        )
                        continue
                    }

                    // 6. Check for No Tunnels notice if claimed but 0 tunnels
                    if (hasClaimed && assignedAddress == null && (
                        line.contains("0 tunnels", ignoreCase = true) ||
                        line.contains("No tunnels configured", ignoreCase = true)
                    )) {
                        onStateChanged(
                            TunnelState.Connecting(
                                message = "Playit online: No tunnel created on playit.gg",
                                claimUrl = null
                            )
                        )
                    }

                    // 7. Check for general errors
                    if (line.contains("error", ignoreCase = true) &&
                        !line.contains("loading secret", ignoreCase = true) &&
                        !line.contains("no secret", ignoreCase = true) &&
                        !line.contains("TooManyRequests", ignoreCase = true) &&
                        assignedAddress == null
                    ) {
                        Log.w(TAG, "Playit error in output: $line")
                    }
                }

                apiPollJob.cancel()

                val exitCode = proc.waitFor()
                Log.i(TAG, "Playit process exited with code $exitCode")

                if (isExplicitlyStopped.get()) break
                if (exitCode != 0) {
                    throw Exception("Playit agent exited with code $exitCode")
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Playit tunnel cancelled gracefully")
                break
            } catch (e: Exception) {
                if (isExplicitlyStopped.get() || !parentScope.isActive) {
                    Log.d(TAG, "Playit tunnel stopped intentionally")
                    break
                }
                Log.e(TAG, "Playit tunnel error: ${e.message}", e)
                onStateChanged(TunnelState.Error(e.message ?: "Playit connection failed"))
            } finally {
                try {
                    process?.destroyForcibly()
                } catch (_: Exception) {}
                process = null
            }

            if (!parentScope.isActive || isExplicitlyStopped.get()) break
            delay(5000L)
        }

        if (isExplicitlyStopped.get()) {
            onStateChanged(TunnelState.Disconnected)
        }
    }

    private suspend fun downloadBinary(targetFile: File) = withContext(Dispatchers.IO) {
        val arch = when (Build.SUPPORTED_ABIS.firstOrNull() ?: "") {
            "arm64-v8a" -> "aarch64"
            "x86_64" -> "x86_64"
            "armeabi-v7a", "armeabi" -> "armv7"
            else -> "aarch64"
        }
        val downloadUrl = "https://github.com/playit-cloud/playit-agent/releases/download/$PLAYIT_VERSION/playit-linux-$arch"
        Log.i(TAG, "Downloading Playit binary from $downloadUrl to ${targetFile.absolutePath}")

        val url = URL(downloadUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        var currentConn = conn
        var redirects = 0
        while (currentConn.responseCode in 300..399 && redirects < 5) {
            val newUrl = currentConn.getHeaderField("Location") ?: break
            currentConn.disconnect()
            currentConn = URL(newUrl).openConnection() as HttpURLConnection
            currentConn.connectTimeout = 15000
            currentConn.readTimeout = 30000
            redirects++
        }

        val tempFile = File(targetFile.parentFile, "playit.tmp")
        currentConn.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        currentConn.disconnect()

        if (tempFile.exists() && tempFile.length() > 0L) {
            tempFile.renameTo(targetFile)
            targetFile.setExecutable(true, false)
            Log.i(TAG, "Playit binary downloaded successfully (${targetFile.length()} bytes)")
        } else {
            throw Exception("Failed to download Playit agent binary")
        }
    }

    private fun findSecretKey(): String? {
        if (secret.isNotBlank()) return secret.trim()

        val paths = listOf(
            File(pRootEngine.rootfsDir, "root/.config/playit_gg/playit.toml"),
            File(pRootEngine.rootfsDir, "root/.config/playit/playit.toml"),
            File(pRootEngine.rootfsDir, "root/playit.toml"),
            File(pRootEngine.rootfsDir, "etc/playit/playit.toml")
        )

        for (file in paths) {
            if (file.exists() && file.isFile) {
                try {
                    val content = file.readText()
                    val regex = Regex("""secret(?:_key)?\s*=\s*["']([^"']+)["']""")
                    val match = regex.find(content)
                    if (match != null) {
                        return match.groupValues[1].trim()
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private suspend fun fetchPlayitTunnels(secretKey: String): PlayitRunData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.playit.gg/agents/rundata")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Agent-Key $secretKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "MineServe-Android")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write("{}".toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                return@withContext parsePlayitRunData(responseText)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Playit API query error: ${e.message}")
        }
        null
    }

    private fun parsePlayitRunData(jsonString: String): PlayitRunData? {
        try {
            val root = org.json.JSONObject(jsonString)
            if (root.optString("status") != "success") return null
            val data = root.optJSONObject("data") ?: return null
            val agentId = data.optString("agent_id")
            val accountStatus = data.optString("account_status")
            val tunnelsArray = data.optJSONArray("tunnels") ?: org.json.JSONArray()
            val tunnelsList = mutableListOf<PlayitTunnelInfo>()

            for (i in 0 until tunnelsArray.length()) {
                val item = tunnelsArray.getJSONObject(i)
                val id = item.optString("id")
                val name = item.optString("name")
                val assignedDomain = item.optString("assigned_domain")
                val customDomain = if (item.isNull("custom_domain")) null else item.optString("custom_domain")
                val proto = item.optString("proto")
                val localPort = item.optInt("local_port", 25565)

                val portObj = item.optJSONObject("port")
                val portFrom = portObj?.optInt("from", 25565) ?: 25565
                val portTo = portObj?.optInt("to", portFrom) ?: portFrom

                tunnelsList.add(
                    PlayitTunnelInfo(
                        id = id,
                        name = name,
                        assignedDomain = assignedDomain,
                        customDomain = customDomain,
                        portFrom = portFrom,
                        portTo = portTo,
                        proto = proto,
                        localPort = localPort
                    )
                )
            }

            return PlayitRunData(agentId, accountStatus, tunnelsList)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Playit API response", e)
            return null
        }
    }
}

private data class PlayitTunnelInfo(
    val id: String,
    val name: String,
    val assignedDomain: String,
    val customDomain: String?,
    val portFrom: Int,
    val portTo: Int,
    val proto: String,
    val localPort: Int
)

private data class PlayitRunData(
    val agentId: String,
    val accountStatus: String,
    val tunnels: List<PlayitTunnelInfo>
)

