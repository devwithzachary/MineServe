package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.api.MineServeHttpClient
import com.devwithzachary.mineserve.core.CoreLog
import com.devwithzachary.mineserve.util.FileStorageUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Provisions portable Adoptium Temurin OpenJDK JREs on Linux, macOS, and Windows
 * with zero system package manager or root requirements.
 */
class PortableJavaManager(
    private val paths: DesktopDataPaths
) {
    companion object {
        private const val TAG = "PortableJavaManager"
        val SUPPORTED_VERSIONS = listOf(8, 17, 21, 25)

        fun adoptiumOs(os: DesktopOs = DesktopPlatform.currentOs): String = when (os) {
            DesktopOs.LINUX -> "linux"
            DesktopOs.MACOS -> "mac"
            DesktopOs.WINDOWS -> "windows"
        }

        fun adoptiumDownloadUrl(
            version: Int,
            os: DesktopOs = DesktopPlatform.currentOs,
            arch: String = DesktopPlatform.normalizedArch
        ): String {
            val osSlug = adoptiumOs(os)
            // Adoptium does not publish Java 8 mac/aarch64; x64 runs via Rosetta 2 on Apple Silicon.
            val effectiveArch = if (os == DesktopOs.MACOS && arch == "aarch64" && version == 8) {
                "x64"
            } else {
                arch
            }
            return "https://api.adoptium.net/v3/binary/latest/$version/ga/$osSlug/$effectiveArch/jre/hotspot/normal/eclipse"
        }
    }

    private val installMutex = Mutex()
    private val downloadClient by lazy {
        MineServeHttpClient.client.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .build()
    }

    private fun isUsableJavaBinary(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        return DesktopPlatform.isWindows || file.canExecute()
    }

    fun getJavaBinary(version: Int): File? {
        val javaExe = DesktopPlatform.javaExecutableName
        val versionDir = File(paths.runtimesDir, "jdk-$version")
        if (versionDir.exists()) {
            val direct = File(versionDir, "bin/$javaExe")
            if (isUsableJavaBinary(direct)) return direct
            // Check one level deep (Temurin archives extract into jdk-21.0.x-jre/ or Contents/Home/ on macOS)
            versionDir.listFiles()?.firstOrNull { it.isDirectory }?.let { sub ->
                val nested = File(sub, "bin/$javaExe")
                if (isUsableJavaBinary(nested)) return nested
                val macNested = File(sub, "Contents/Home/bin/$javaExe")
                if (isUsableJavaBinary(macNested)) return macNested
            }
        }
        // Fallback: check current JVM if major version matches
        val currentMajor = Runtime.version().feature()
        if (currentMajor == version) {
            val sysJava = File(System.getProperty("java.home"), "bin/$javaExe")
            if (isUsableJavaBinary(sysJava)) return sysJava
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
                val arch = DesktopPlatform.normalizedArch
                val url = adoptiumDownloadUrl(version, DesktopPlatform.currentOs, arch)
                onProgress("Downloading Adoptium Temurin OpenJDK $version (${DesktopPlatform.shortName}/$arch)...")
                CoreLog.i(TAG, "Downloading portable JRE $version from $url")

                val ext = if (DesktopPlatform.isWindows) "zip" else "tar.gz"
                val archiveFile = File(paths.tmpDir, "temurin-jre-$version-$arch.$ext")
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

                if (DesktopPlatform.isWindows) {
                    FileStorageUtils.extractZipSafe(archiveFile, targetDir, deleteZipAfter = true).getOrThrow()
                } else {
                    // Use system tar on Linux/macOS (fast & preserves permissions/symlinks cleanly)
                    val tarProc = ProcessBuilder("tar", "-xzf", archiveFile.absolutePath, "-C", targetDir.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                    val tarOut = tarProc.inputStream.bufferedReader().readText()
                    val exit = tarProc.waitFor()
                    archiveFile.delete()

                    if (exit != 0) {
                        return@withLock Result.failure(IllegalStateException("Failed extracting OpenJDK $version: $tarOut"))
                    }
                }

                val javaBin = getJavaBinary(version)
                    ?: return@withLock Result.failure(IllegalStateException("Extracted OpenJDK $version but could not locate bin/${DesktopPlatform.javaExecutableName}"))
                if (!DesktopPlatform.isWindows) {
                    javaBin.setExecutable(true, false)
                }
                onProgress("OpenJDK $version installed at ${javaBin.absolutePath}")
                Result.success(javaBin)
            } catch (e: Exception) {
                CoreLog.e(TAG, "Failed installing portable Java $version", e)
                Result.failure(e)
            }
        }
    }
}
