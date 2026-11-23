package com.devwithzachary.mineserve.desktop

import java.io.File

class LinuxDataPaths(
    val rootDir: File = defaultDataDir()
) {
    companion object {
        fun defaultDataDir(): File {
            val xdg = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            return if (xdg != null) {
                File(xdg, "mineserve")
            } else {
                File(System.getProperty("user.home"), ".local/share/mineserve")
            }
        }
    }

    val serversDir: File get() = File(rootDir, "servers").apply { mkdirs() }
    val runtimesDir: File get() = File(rootDir, "runtimes/java").apply { mkdirs() }
    val binDir: File get() = File(rootDir, "bin").apply { mkdirs() }
    val tmpDir: File get() = File(rootDir, "tmp").apply { mkdirs() }
    val serversJson: File get() = File(rootDir, "servers.json")
    val configJson: File get() = File(rootDir, "desktop_config.json")

    init {
        rootDir.mkdirs()
        serversDir.mkdirs()
        runtimesDir.mkdirs()
        binDir.mkdirs()
        tmpDir.mkdirs()
    }
}
