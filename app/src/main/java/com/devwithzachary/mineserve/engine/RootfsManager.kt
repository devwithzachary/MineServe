package com.devwithzachary.mineserve.engine

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

sealed class RootfsSetupState {
    data object Idle : RootfsSetupState()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long, val progressPercent: Int) : RootfsSetupState()
    data class Extracting(val message: String, val progressPercent: Int = 0, val logs: List<String> = emptyList()) : RootfsSetupState()
    data class Success(val rootfsDir: File, val logs: List<String> = emptyList()) : RootfsSetupState()
    data class Error(val message: String, val logs: List<String> = emptyList()) : RootfsSetupState()
}

class RootfsManager(private val context: Context, private val pRootEngine: PRootEngine) {

    companion object {
        private const val TAG = "RootfsManager"

        // Official Ubuntu 24.04 LTS (Noble) Base images
        private const val UBUNTU_24_ARM64 = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.4-base-arm64.tar.gz"
        private const val UBUNTU_24_AMD64 = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.4-base-amd64.tar.gz"
        private const val UBUNTU_24_ARMHF = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.4-base-armhf.tar.gz"

        // Fallback 22.04 LTS (Jammy) Base images
        private const val UBUNTU_22_ARM64 = "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.5-base-arm64.tar.gz"
        private const val UBUNTU_22_AMD64 = "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.5-base-amd64.tar.gz"
        private const val UBUNTU_22_ARMHF = "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.5-base-armhf.tar.gz"
    }

    private val prefs = context.getSharedPreferences("mineserve_rootfs_prefs", Context.MODE_PRIVATE)
    val rootfsDir: File get() = pRootEngine.rootfsDir

    fun isInstalled(): Boolean = pRootEngine.isRootfsInstalled()

    private fun getArchRootfsUrls(): List<String> {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> listOf(UBUNTU_24_ARM64, UBUNTU_22_ARM64)
            abi.contains("x86_64") -> listOf(UBUNTU_24_AMD64, UBUNTU_22_AMD64)
            abi.contains("armeabi") || abi.contains("armv7") -> listOf(UBUNTU_24_ARMHF, UBUNTU_22_ARMHF)
            else -> listOf(UBUNTU_24_ARM64, UBUNTU_22_ARM64)
        }
    }

    suspend fun getStorageUsedMb(): Long = withContext(Dispatchers.IO) {
        val targets = listOf(rootfsDir, File(context.filesDir, "servers"))
        var totalBytes = 0L

        for (target in targets) {
            if (!target.exists()) continue
            try {
                java.nio.file.Files.walkFileTree(
                    target.toPath(),
                    object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                        override fun visitFile(file: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                            try {
                                totalBytes += attrs.size()
                            } catch (_: Exception) {}
                            return java.nio.file.FileVisitResult.CONTINUE
                        }

                        override fun visitFileFailed(file: java.nio.file.Path, exc: java.io.IOException?): java.nio.file.FileVisitResult {
                            // Safely ignore unreadable files / broken symlinks
                            return java.nio.file.FileVisitResult.CONTINUE
                        }

                        override fun preVisitDirectory(dir: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                            return java.nio.file.FileVisitResult.CONTINUE
                        }
                    }
                )
            } catch (_: Exception) {
                // Fallback: iterative folder traversal
                val queue = ArrayDeque<File>()
                queue.add(target)
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    val children = try { current.listFiles() } catch (_: Exception) { null }
                    if (children != null) {
                        for (child in children) {
                            if (child.isDirectory) {
                                queue.add(child)
                            } else {
                                try { totalBytes += child.length() } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        }

        (totalBytes / (1024 * 1024)).coerceAtLeast(1L)
    }

    fun setupRootfs(): Flow<RootfsSetupState> = channelFlow {
        send(RootfsSetupState.Downloading(0L, 100L, 0))
        val archiveFile = File(context.cacheDir, "ubuntu_base.tar.gz")
        val urlsToTry = getArchRootfsUrls()

        val logList = mutableListOf<String>()
        fun emitLog(msg: String) {
            logList.add(msg)
            Log.d(TAG, msg)
        }

        try {
            if (!rootfsDir.exists()) {
                rootfsDir.mkdirs()
            }

            var downloaded = false
            var downloadError: String? = null

            for (downloadUrl in urlsToTry) {
                try {
                    emitLog("Connecting to Ubuntu mirror: $downloadUrl...")
                    val url = URL(downloadUrl)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20000
                        readTimeout = 20000
                        instanceFollowRedirects = true
                        setRequestProperty("User-Agent", "MineServe-Android")
                        connect()
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val fileLength = connection.contentLength.toLong()
                        val inputStream = BufferedInputStream(connection.inputStream, 65536)
                        val outputStream = FileOutputStream(archiveFile)

                        val buffer = ByteArray(65536)
                        var totalRead = 0L
                        var read: Int
                        var lastProgressUpdate = 0L

                        while (inputStream.read(buffer).also { read = it } != -1) {
                            totalRead += read
                            outputStream.write(buffer, 0, read)

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate > 100) {
                                lastProgressUpdate = now
                                val percent = if (fileLength > 0) ((totalRead * 100) / fileLength).toInt() else 0
                                send(RootfsSetupState.Downloading(totalRead, fileLength, percent))
                            }
                        }

                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        downloaded = true
                        emitLog("Downloaded Ubuntu rootfs (${totalRead / (1024 * 1024)} MB)")
                        break
                    } else {
                        emitLog("Mirror returned HTTP ${connection.responseCode}, trying next mirror...")
                    }
                } catch (e: Exception) {
                    emitLog("Mirror failed: ${e.message}, trying fallback...")
                    downloadError = e.message
                }
            }

            if (!downloaded || !archiveFile.exists() || archiveFile.length() == 0L) {
                throw IllegalStateException("Failed to download Ubuntu rootfs archive: $downloadError")
            }

            emitLog("Extracting Ubuntu rootfs structure...")
            send(RootfsSetupState.Extracting("Extracting Ubuntu filesystem...", 0, logList.toList()))

            if (rootfsDir.exists()) {
                rootfsDir.deleteRecursively()
            }
            rootfsDir.mkdirs()

            extractTarGzInJava(archiveFile, rootfsDir) { status, percent ->
                send(RootfsSetupState.Extracting(status, percent, logList.toList()))
            }
            archiveFile.delete()

            emitLog("Configuring DNS resolvers, APT sandbox, and system permissions...")
            send(RootfsSetupState.Extracting("Configuring system files and permissions...", 95, logList.toList()))
            configureSystemFiles()

            emitLog("Rootfs setup completed successfully!")
            send(RootfsSetupState.Success(rootfsDir, logList.toList()))

        } catch (e: Exception) {
            Log.e(TAG, "Error installing rootfs", e)
            emitLog("Installation error: ${e.message}")
            send(RootfsSetupState.Error(e.localizedMessage ?: "Failed to install Ubuntu rootfs", logList.toList()))
        }
    }.flowOn(Dispatchers.IO)

    fun ensureSystemFilesConfigured() {
        configureSystemFiles()
    }

    private fun configureSystemFiles() {
        try {
            val etcDir = File(rootfsDir, "etc").apply { if (!exists()) mkdirs() }
            
            // DNS configuration
            val resolvConf = File(etcDir, "resolv.conf")
            resolvConf.writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\nnameserver 1.0.0.1\n")

            // Hosts
            val hosts = File(etcDir, "hosts")
            if (!hosts.exists() || hosts.length() == 0L) {
                hosts.writeText("127.0.0.1 localhost mineserve\n::1 localhost ip6-localhost ip6-loopback\n")
            }

            // Timezone configuration (sync with host device local timezone)
            val deviceTz = try { java.util.TimeZone.getDefault().id.takeIf { it.isNotBlank() } ?: "UTC" } catch (_: Exception) { "UTC" }
            val timezoneFile = File(etcDir, "timezone")
            timezoneFile.writeText("$deviceTz\n")

            val zoneinfoFile = File(rootfsDir, "usr/share/zoneinfo/$deviceTz")
            val localtimeFile = File(etcDir, "localtime")
            if (zoneinfoFile.exists()) {
                try {
                    localtimeFile.delete()
                    zoneinfoFile.copyTo(localtimeFile, overwrite = true)
                } catch (_: Exception) {}
            }

            // User & Group configuration for mineserve unprivileged user (UID/GID 1000) and _apt user
            val passwdFile = File(etcDir, "passwd")
            if (passwdFile.exists()) {
                val lines = passwdFile.readLines().toMutableList()
                if (lines.none { it.startsWith("root:") }) {
                    lines.add(0, "root:x:0:0:root:/root:/bin/bash")
                }
                if (lines.none { it.startsWith("_apt:") }) {
                    lines.add("_apt:x:42:65534::/nonexistent:/usr/sbin/nologin")
                }
                if (lines.none { it.startsWith("mineserve:") }) {
                    val has1000 = lines.any { it.contains(":1000:1000:") }
                    if (has1000) {
                        val index = lines.indexOfFirst { it.contains(":1000:1000:") }
                        lines[index] = "mineserve:x:1000:1000:MineServe Server User:/home/mineserve:/bin/bash"
                    } else {
                        lines.add("mineserve:x:1000:1000:MineServe Server User:/home/mineserve:/bin/bash")
                    }
                }
                passwdFile.writeText(lines.joinToString("\n") + "\n")
            } else {
                passwdFile.writeText("root:x:0:0:root:/root:/bin/bash\n_apt:x:42:65534::/nonexistent:/usr/sbin/nologin\nmineserve:x:1000:1000:MineServe Server User:/home/mineserve:/bin/bash\n")
            }

            val groupFile = File(etcDir, "group")
            if (groupFile.exists()) {
                val lines = groupFile.readLines().toMutableList()
                if (lines.none { it.startsWith("root:") }) {
                    lines.add(0, "root:x:0:")
                }
                if (lines.none { it.startsWith("nogroup:") }) {
                    lines.add("nogroup:x:65534:")
                }
                if (lines.none { it.startsWith("mineserve:") }) {
                    val has1000 = lines.any { it.contains(":1000:") }
                    if (has1000) {
                        val index = lines.indexOfFirst { it.contains(":1000:") }
                        lines[index] = "mineserve:x:1000:"
                    } else {
                        lines.add("mineserve:x:1000:")
                    }
                }
                groupFile.writeText(lines.joinToString("\n") + "\n")
            } else {
                groupFile.writeText("root:x:0:\nnogroup:x:65534:\nmineserve:x:1000:\n")
            }

            val shadowFile = File(etcDir, "shadow")
            if (shadowFile.exists()) {
                val lines = shadowFile.readLines().toMutableList()
                if (lines.none { it.startsWith("mineserve:") }) {
                    lines.add("mineserve:*:19000:0:99999:7:::")
                    shadowFile.writeText(lines.joinToString("\n") + "\n")
                }
            } else {
                shadowFile.writeText("root:*:19000:0:99999:7:::\nmineserve:*:19000:0:99999:7:::\n")
            }

            // Clean any stale APT or DPKG lock files left behind
            val staleLocks = listOf(
                File(rootfsDir, "var/lib/dpkg/lock"),
                File(rootfsDir, "var/lib/dpkg/lock-frontend"),
                File(rootfsDir, "var/lib/apt/lists/lock"),
                File(rootfsDir, "var/cache/apt/archives/lock")
            )
            for (lf in staleLocks) {
                try {
                    if (lf.exists()) lf.delete()
                } catch (_: Exception) {}
            }

            // Create and permit /home/mineserve
            val homeMineserve = File(rootfsDir, "home/mineserve").apply { if (!exists()) mkdirs() }
            homeMineserve.setReadable(true, false)
            homeMineserve.setWritable(true, false)
            homeMineserve.setExecutable(true, false)

            // Temp directories with open permissions
            val tmpDir = File(rootfsDir, "tmp").apply { if (!exists()) mkdirs() }
            tmpDir.setReadable(true, false)
            tmpDir.setWritable(true, false)
            tmpDir.setExecutable(true, false)

            val varTmpDir = File(rootfsDir, "var/tmp").apply { if (!exists()) mkdirs() }
            varTmpDir.setReadable(true, false)
            varTmpDir.setWritable(true, false)
            varTmpDir.setExecutable(true, false)

            // Servers directory for isolated Minecraft instances
            val serversDir = File(rootfsDir, "servers").apply { if (!exists()) mkdirs() }
            serversDir.setReadable(true, false)
            serversDir.setWritable(true, false)
            serversDir.setExecutable(true, false)

            // APT sandboxing for PRoot
            val aptConfigDir = File(etcDir, "apt/apt.conf.d").apply { if (!exists()) mkdirs() }
            val aptConfig = File(aptConfigDir, "99mineserve")
            aptConfig.writeText(
                "APT::Sandbox::User \"root\";\n" +
                "Acquire::http::Pipeline-Depth \"0\";\n" +
                "Acquire::http::No-Cache \"true\";\n" +
                "Acquire::BrokenProxy \"true\";\n" +
                "Acquire::PDiffs \"false\";\n" +
                "Acquire::ForceIPv4 \"true\";\n"
            )

            // DPKG configuration
            val dpkgConfigDir = File(etcDir, "dpkg/dpkg.cfg.d").apply { if (!exists()) mkdirs() }
            val dpkgConfig = File(dpkgConfigDir, "00-mineserve")
            dpkgConfig.writeText("force-unsafe-io\nforce-overwrite\nforce-confold\nforce-confdef\n")

            // Policy-rc.d to prevent service start failures during apt-get in PRoot
            val policyRcd = File(rootfsDir, "usr/sbin/policy-rc.d").apply { parentFile?.mkdirs() }
            policyRcd.writeText("#!/bin/sh\nexit 101\n")
            policyRcd.setExecutable(true, false)
            policyRcd.setReadable(true, false)

            // Ensure /bin/sh and /bin/bash exist and are executable
            val binDir = File(rootfsDir, "bin").apply { if (!exists()) mkdirs() }
            val usrBinDir = File(rootfsDir, "usr/bin").apply { if (!exists()) mkdirs() }

            val binSh = File(binDir, "sh")
            val binBash = File(binDir, "bash")
            val usrBinSh = File(usrBinDir, "sh")
            val usrBinBash = File(usrBinDir, "bash")

            if (!binSh.exists() && usrBinSh.exists()) {
                try { android.system.Os.symlink("/usr/bin/sh", binSh.absolutePath) } catch (_: Exception) {}
            }
            if (!binBash.exists() && usrBinBash.exists()) {
                try { android.system.Os.symlink("/usr/bin/bash", binBash.absolutePath) } catch (_: Exception) {}
            }

            binSh.setExecutable(true, false)
            binBash.setExecutable(true, false)
            usrBinSh.setExecutable(true, false)
            usrBinBash.setExecutable(true, false)

            fixAllPermissions(rootfsDir)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure system files", e)
        }
    }

    private fun fixAllPermissions(dir: File) {
        try {
            dir.listFiles()?.forEach { file ->
                file.setReadable(true, false)
                if (file.isDirectory) {
                    file.setWritable(true, false)
                    file.setExecutable(true, false)
                    fixAllPermissions(file)
                } else {
                    val name = file.name
                    val path = file.absolutePath
                    if (path.contains("/bin/") || path.contains("/sbin/") || path.contains("/lib/") || path.contains("/libexec/") || name.endsWith(".so") || name.contains(".so.") || name.endsWith(".sh")) {
                        file.setExecutable(true, false)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun extractTarGzInJava(
        tarGzFile: File,
        targetDir: File,
        onProgress: (suspend (String, Int) -> Unit)? = null
    ) {
        val gzipIn = GZIPInputStream(BufferedInputStream(tarGzFile.inputStream(), 65536))
        val buffer = ByteArray(512)
        var longName: String? = null
        var extractedFiles = 0
        var lastUpdate = System.currentTimeMillis()

        while (true) {
            var bytesRead = 0
            while (bytesRead < 512) {
                val r = gzipIn.read(buffer, bytesRead, 512 - bytesRead)
                if (r == -1) break
                bytesRead += r
            }
            if (bytesRead < 512) break

            var isEmpty = true
            for (i in 0 until 512) {
                if (buffer[i] != 0.toByte()) {
                    isEmpty = false
                    break
                }
            }
            if (isEmpty) break

            val rawName = String(buffer, 0, 100, Charsets.US_ASCII).trimEnd('\u0000', ' ')
            val prefix = String(buffer, 345, 155, Charsets.US_ASCII).trimEnd('\u0000', ' ')
            val typeFlag = buffer[156].toInt().toChar()
            val size = parseOctal(buffer, 124, 12)

            var entryName = longName ?: if (prefix.isNotEmpty()) "$prefix/$rawName" else rawName
            longName = null

            if (entryName.isEmpty()) {
                skipBytes(gzipIn, size)
                continue
            }

            if (typeFlag == 'L') {
                val nameBytes = ByteArray(size.toInt())
                readFully(gzipIn, nameBytes)
                longName = String(nameBytes, Charsets.UTF_8).trimEnd('\u0000', ' ', '\n', '\r')
                val remainder = (512 - (size % 512)) % 512
                if (remainder > 0) skipBytes(gzipIn, remainder)
                continue
            }

            if (entryName.startsWith("/")) {
                entryName = entryName.substring(1)
            }

            val destFile = File(targetDir, entryName)
            extractedFiles++

            val now = System.currentTimeMillis()
            if (onProgress != null && now - lastUpdate > 100) {
                lastUpdate = now
                val fileName = entryName.takeLast(30)
                val percent = ((extractedFiles * 100) / 12000).coerceIn(0, 95)
                kotlinx.coroutines.runBlocking {
                    onProgress("Extracting: $fileName", percent)
                }
            }

            when (typeFlag) {
                '5' -> { // Directory
                    destFile.mkdirs()
                    destFile.setReadable(true, false)
                    destFile.setWritable(true, false)
                    destFile.setExecutable(true, false)
                    skipBytes(gzipIn, (512 - (size % 512)) % 512)
                }

                '0', '\u0000' -> { // Regular file
                    destFile.parentFile?.apply {
                        mkdirs()
                        setReadable(true, false)
                        setWritable(true, false)
                        setExecutable(true, false)
                    }
                    FileOutputStream(destFile).use { out ->
                        copyBytes(gzipIn, out, size)
                    }
                    destFile.setReadable(true, false)
                    val mode = parseOctal(buffer, 100, 8)
                    val isExec = entryName.contains("bin/") ||
                            entryName.endsWith(".sh") ||
                            entryName.contains("lib/") ||
                            entryName.contains(".so") ||
                            entryName.contains("libexec/") ||
                            (mode and 73L != 0L) // 0111 in octal
                    if (isExec) {
                        destFile.setExecutable(true, false)
                    }
                    val remainder = (512 - (size % 512)) % 512
                    if (remainder > 0) skipBytes(gzipIn, remainder)
                }

                '1', '2' -> { // Symlink / Hardlink
                    destFile.parentFile?.mkdirs()
                    val rawLink = String(buffer, 157, 100, Charsets.US_ASCII).trimEnd('\u0000', ' ')
                    if (rawLink.isNotEmpty()) {
                        val isTopLevel = (destFile.parentFile?.absolutePath == targetDir.absolutePath)
                        val isAbsoluteRootfsPath = rawLink.startsWith("usr/") || rawLink.startsWith("etc/") || rawLink.startsWith("var/") || rawLink.startsWith("opt/")
                        val linkTarget = if (!isTopLevel && isAbsoluteRootfsPath) "/$rawLink" else rawLink
                        try {
                            if (destFile.exists()) {
                                destFile.delete()
                            }
                            android.system.Os.symlink(linkTarget, destFile.absolutePath)
                        } catch (e: Exception) {
                            Log.w(TAG, "Symlink creation for ${destFile.name} -> $linkTarget: ${e.message}")
                        }
                    }
                    val remainder = (512 - (size % 512)) % 512
                    if (remainder > 0) skipBytes(gzipIn, remainder)
                }

                else -> {
                    val remainder = (512 - (size % 512)) % 512
                    skipBytes(gzipIn, size + remainder)
                }
            }
        }
        gzipIn.close()
    }

    private fun parseOctal(buffer: ByteArray, offset: Int, length: Int): Long {
        var result = 0L
        val end = offset + length
        for (i in offset until end) {
            val b = buffer[i].toInt() and 0xFF
            if (b == 0 || b == ' '.code) continue
            if (b in '0'.code..'7'.code) {
                result = (result shl 3) + (b - '0'.code)
            }
        }
        return result
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var read = 0
        while (read < buffer.size) {
            val r = input.read(buffer, read, buffer.size - read)
            if (r == -1) break
            read += r
        }
    }

    private fun copyBytes(input: InputStream, output: FileOutputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(16384)
        while (remaining > 0) {
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, toRead)
            if (r == -1) break
            output.write(buf, 0, r)
            remaining -= r
        }
    }

    private fun skipBytes(input: InputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(16384)
        while (remaining > 0) {
            val toRead = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, toRead)
            if (r == -1) break
            remaining -= r
        }
    }
}
