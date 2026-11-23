package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.core.CoreLog
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Provisions portable Adoptium Temurin OpenJDK JREs on Linux (x64 & aarch64)
 * with zero system package manager or root requirements.
 */
class PortableJavaManager(
    private val paths: LinuxDataPaths
) {
    companion object {
        private const val TAG = "PortableJavaManager"
        val SUPPORTED_VERSIONS = listOf(8, 17, 21, 25)
    }

    private val installMutex = Mutex()
    private val downloadClient by lazy {
        MineServeHttpClient.client.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .build()
    }

    private fun adoptiumArch(): String {
        val arch = (System.getProperty("os.arch") ?: "amd64").lowercase()
        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
            arch.contains("arm") -> "arm"
            else -> "x64"
        }
    }

    fun getJavaBinary(version: Int): File? {
        val versionDir = File(paths.runtimesDir, "jdk-$version")
        if (versionDir.exists()) {
            val direct = File(versionDir, "bin/java")
            if (direct.exists() && direct.canExecute()) return direct
            // Check one level deep (Temurin tarballs extract into jdk-21.0.x-jre/)
            versionDir.listFiles()?.firstOrNull { it.isDirectory }?.let { sub ->
                val nested = File(sub, "bin/java")
                if (nested.exists() && nested.canExecute()) return nested
            }
        }
        // Fallback: check current JVM if major version matches
        val currentMajor = Runtime.version().feature()
        if (currentMajor == version) {
            val sysJava = File(System.getProperty("java.home"), "bin/java")
            if (sysJava.exists() && sysJava.canExecute()) return sysJava
        }
        return null
    }

    fun isJavaInstalled(version: Int): Boolean = getJavaBinary(version) != null

    suspend fun installJava(
        version: Int,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        installMutex.withLock {
            getJavaBinary(version)?.let { return@withLock Result.success(it) }
            try {
                val arch = adoptiumArch()
                val url = "https://api.adoptium.net/v3/binary/latest/$version/ga/linux/$arch/jre/hotspot/normal/eclipse"
                onProgress("Downloading Adoptium Temurin OpenJDK $version ($arch)...")
                CoreLog.i(TAG, "Downloading portable JRE $version from $url")

                val archiveFile = File(paths.tmpDir, "temurin-jre-$version-$arch.tar.gz")
                val req = MineServeHttpClient.newRequestBuilder(url).build()
                downloadClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withLock Result.failure(IllegalStateException("Adoptium API returned HTTP ${resp.code} for Java $version ($arch)"))
                    }
                    val body = resp.body ?: return@withLock Result.failure(IllegalStateException("Empty download body"))
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        FileOutputStream(archiveFile).use { output ->
                            val buf = ByteArray(65536)
                            var downloaded = 0L
                            var lastLog = 0L
                            while (true) {
                                val r = input.read(buf)
                                if (r == -1) break
                                output.write(buf, 0, r)
                                downloaded += r
                                val now = System.currentTimeMillis()
                                if (now - lastLog > 500L) {
                                    lastLog = now
                                    val pct = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                                    onProgress("Downloading OpenJDK $version: ${downloaded / (1024 * 1024)} MB ($pct%)")
                                }
                            }
                            output.flush()
                        }
                    }
                }

                onProgress("Extracting OpenJDK $version runtime...")
                val targetDir = File(paths.runtimesDir, "jdk-$version")
                if (targetDir.exists()) targetDir.deleteRecursively()
                targetDir.mkdirs()

                // Use system tar if available (fastest & preserves permissions/symlinks cleanly on Linux)
                val tarProc = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", targetDir.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                val tarOut = tarProc.inputStream.bufferedReader().readText()
                val exit = tarProc.waitFor()
                archiveFile.delete()

                if (exit != 0) {
                    return@withLock Result.failure(IllegalStateException("Failed extracting OpenJDK $version: $tarOut"))
                }

                val javaBin = getJavaBinary(version)
                    ?: return@withLock Result.failure(IllegalStateException("Extracted OpenJDK $version but could not locate bin/java"))
                javaBin.setExecutable(true, false)
                onProgress("OpenJDK $version installed at ${javaBin.absolutePath}")
                Result.success(javaBin)
            } catch (e: Exception) {
                CoreLog.e(TAG, "Failed installing portable Java $version", e)
                Result.failure(e)
            }
        }
    }
}
