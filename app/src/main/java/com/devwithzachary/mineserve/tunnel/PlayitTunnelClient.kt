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
        private val CLAIM_URL_REGEX = Regex("https://playit\\.gg/claim/[a-zA-Z0-9_-]+")
        private val TUNNEL_ADDR_REGEX = Regex("([a-zA-Z0-9.-]+\\.(?:ply\\.gg|joinmc\\.link|playit\\.gg))(?::([0-9]+))?")
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

                while (parentScope.isActive && !isExplicitlyStopped.get()) {
                    val line = reader.readLine() ?: break
                    Log.d(TAG, "[playit] $line")

                    // Check for Claim URL
                    val claimMatch = CLAIM_URL_REGEX.find(line)
                    if (claimMatch != null) {
                        val claimUrl = claimMatch.value
                        onStateChanged(
                            TunnelState.Connecting(
                                message = "Playit setup: Claim tunnel to connect",
                                claimUrl = claimUrl
                            )
                        )
                    }

                    // Check for Assigned Domain / Address
                    val tunnelMatch = TUNNEL_ADDR_REGEX.find(line)
                    if (tunnelMatch != null && (line.contains("tunnel", ignoreCase = true) || line.contains("server", ignoreCase = true) || line.contains("address", ignoreCase = true) || line.contains("connected", ignoreCase = true))) {
                        val host = tunnelMatch.groupValues[1]
                        val portStr = tunnelMatch.groupValues.getOrNull(2)
                        val port = portStr?.toIntOrNull() ?: 25565
                        assignedAddress = "$host:$port"

                        Log.i(TAG, "Playit public tunnel connected: $assignedAddress")
                        onStateChanged(
                            TunnelState.Connected(
                                publicHost = host,
                                publicPort = port,
                                fullAddress = assignedAddress,
                                provider = TunnelProvider.PLAYIT
                            )
                        )
                    }

                    if (line.contains("error", ignoreCase = true) && !line.contains("loading secret", ignoreCase = true) && assignedAddress == null) {
                        Log.w(TAG, "Playit error in output: $line")
                    }
                }

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
}
