package com.devwithzachary.mineserve.engine

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class JavaInstallState {
    data class Progress(val version: Int, val message: String) : JavaInstallState()
    data class Success(val version: Int, val javaPath: String) : JavaInstallState()
    data class Error(val version: Int, val errorMessage: String) : JavaInstallState()
}

class JavaRuntimeManager(
    private val context: Context,
    private val pRootEngine: PRootEngine
) {
    companion object {
        private const val TAG = "JavaRuntimeManager"
        private val installMutex = Mutex()
    }

    fun isJavaInstalled(version: Int = 21): Boolean {
        val rootfs = pRootEngine.rootfsDir
        if (!rootfs.exists()) return false

        val verJava = File(rootfs, "usr/bin/java-$version")
        if (verJava.exists()) return true

        val jvmDir = File(rootfs, "usr/lib/jvm")
        if (jvmDir.exists()) {
            val matching = jvmDir.listFiles()?.any {
                (it.name.contains("-$version-") || it.name.contains("java-$version") || it.name.contains("jdk-$version")) &&
                File(it, "bin/java").exists()
            } ?: false
            if (matching) return true
        }

        val standardJava = File(rootfs, "usr/bin/java")
        val altJava = File(rootfs, "bin/java")
        if ((standardJava.exists() || altJava.exists()) && version == 21) {
            return true
        }

        return false
    }

    fun getJavaExecutablePath(version: Int = 21): String {
        val rootfs = pRootEngine.rootfsDir

        val verJava = File(rootfs, "usr/bin/java-$version")
        if (verJava.exists()) {
            return "/usr/bin/java-$version"
        }

        val jvmDir = File(rootfs, "usr/lib/jvm")
        if (jvmDir.exists()) {
            val matching = jvmDir.listFiles()?.firstOrNull {
                (it.name.contains("-$version-") || it.name.contains("java-$version") || it.name.contains("jdk-$version")) &&
                File(it, "bin/java").exists()
            }
            if (matching != null) {
                return "/usr/lib/jvm/${matching.name}/bin/java"
            }
        }

        return "/usr/bin/java"
    }

    fun installJava(version: Int = 21): Flow<JavaInstallState> = flow {
        installMutex.withLock {
            if (isJavaInstalled(version)) {
                emit(JavaInstallState.Success(version, getJavaExecutablePath(version)))
                return@withLock
            }

            emit(JavaInstallState.Progress(version, "Preparing OpenJDK $version runtime environment..."))

            try {
                val rootfsDir = pRootEngine.rootfsDir

                // Clean host-side lock files before starting PRoot
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

                // 1. Policy-rc.d stub to prevent systemd service daemon errors in PRoot
                val policyRcd = File(rootfsDir, "usr/sbin/policy-rc.d")
                if (!policyRcd.exists()) {
                    try {
                        policyRcd.parentFile?.mkdirs()
                        policyRcd.writeText("#!/bin/sh\nexit 101\n")
                        policyRcd.setExecutable(true, false)
                        policyRcd.setReadable(true, false)
                    } catch (_: Exception) {}
                }

                // 2. Systemd and Dpkg stubs
                val stubs = listOf(
                    "usr/bin/systemd-tmpfiles",
                    "bin/systemd-tmpfiles",
                    "usr/bin/systemd-sysusers",
                    "bin/systemd-sysusers",
                    "usr/bin/systemd-detect-virt",
                    "bin/systemd-detect-virt",
                    "usr/sbin/dpkg-preconfigure"
                )
                for (stubPath in stubs) {
                    val stubFile = File(rootfsDir, stubPath)
                    try {
                        stubFile.parentFile?.mkdirs()
                        stubFile.writeText("#!/bin/sh\nexit 0\n")
                        stubFile.setExecutable(true, false)
                        stubFile.setReadable(true, false)
                    } catch (_: Exception) {}
                }

                val pkgName = when (version) {
                    8 -> "openjdk-8-jre-headless"
                    17 -> "openjdk-17-jre-headless"
                    25 -> "openjdk-25-jre-headless"
                    else -> "openjdk-21-jre-headless"
                }

                emit(JavaInstallState.Progress(version, "Updating apt package index and installing $pkgName..."))

                val installScript = buildString {
                    append("export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin; ")
                    append("killall -9 apt-get dpkg 2>/dev/null || true; ")
                    append("rm -f /var/lib/dpkg/lock /var/lib/dpkg/lock-frontend /var/lib/apt/lists/lock /var/cache/apt/archives/lock /var/lib/dpkg/updates/* /usr/bin/*.dpkg-new /usr/lib/*.dpkg-new 2>/dev/null; ")
                    append("chmod 755 /usr /usr/local /usr/bin /usr/sbin /etc 2>/dev/null; ")
                    append("chmod -R 777 /var/lib/dpkg /var/lib/apt /var/cache/apt /tmp /var/tmp /.l2s 2>/dev/null; ")
                    append("mkdir -p /etc/dpkg/dpkg.cfg.d && echo force-unsafe-io > /etc/dpkg/dpkg.cfg.d/00-mineserve && echo force-overwrite >> /etc/dpkg/dpkg.cfg.d/00-mineserve && echo force-confold >> /etc/dpkg/dpkg.cfg.d/00-mineserve && echo force-confdef >> /etc/dpkg/dpkg.cfg.d/00-mineserve; ")
                    append("mkdir -p /etc/apt/apt.conf.d && echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/99mineserve && echo 'Acquire::http::Pipeline-Depth \"0\";' >> /etc/apt/apt.conf.d/99mineserve && echo 'Acquire::http::No-Cache \"true\";' >> /etc/apt/apt.conf.d/99mineserve && echo 'Acquire::PDiffs \"false\";' >> /etc/apt/apt.conf.d/99mineserve && echo 'Acquire::ForceIPv4 \"true\";' >> /etc/apt/apt.conf.d/99mineserve; ")
                    append("chmod 666 /var/lib/dpkg/status* 2>/dev/null; ")
                    append("dpkg --configure -a --force-confold --force-confdef 2>/dev/null; ")
                    append("apt-get update -y && apt-get install -y --no-install-recommends $pkgName curl wget ca-certificates-java")
                }

                val cmd = pRootEngine.buildPRootCommand(
                    command = listOf("/bin/sh", "-c", installScript),
                    loginUser = "root"
                )

                val pb = ProcessBuilder(cmd)
                pb.directory(rootfsDir)

                val env = pb.environment()
                env.putAll(pRootEngine.getEnvironmentVariables("root"))
                env["PROOT_NO_SECCOMP"] = "1"
                env["PROOT_FORCE_SETID"] = "1"
                env["PROOT_LINK2SYMLINK"] = "1"
                env["TMPDIR"] = "/tmp"
                env["TMP"] = "/tmp"
                env["DEBIAN_FRONTEND"] = "noninteractive"
                env["DEBIAN_PRIORITY"] = "critical"
                env["UCF_FORCE_CONFFOLD"] = "1"
                env["NEEDRESTART_MODE"] = "a"

                pb.redirectErrorStream(true)
                val process = pb.start()

                try {
                    process.outputStream.close()
                } catch (_: Exception) {}

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: break
                    Log.d(TAG, "[Java $version Install] $currentLine")
                    emit(JavaInstallState.Progress(version, currentLine))
                }

                val exitCode = process.waitFor()
                reader.close()

                if (exitCode == 0 || isJavaInstalled(version)) {
                    emit(JavaInstallState.Success(version, getJavaExecutablePath(version)))
                } else {
                    emit(JavaInstallState.Error(version, "Java installation exited with code $exitCode"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed installing Java $version", e)
                emit(JavaInstallState.Error(version, e.localizedMessage ?: "Unknown installation error"))
            }
        }
    }.flowOn(Dispatchers.IO)
}
