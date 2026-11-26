package com.devwithzachary.mineserve.desktop

import java.io.File
import org.json.JSONObject

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

    val defaultServersDir: File get() = File(rootDir, "servers")
    val defaultRuntimesDir: File get() = File(rootDir, "runtimes/java")

    @Volatile private var customServersDir: File? = null
    @Volatile private var customRuntimesDir: File? = null

    val serversDir: File get() = (customServersDir ?: defaultServersDir).apply { mkdirs() }
    val runtimesDir: File get() = (customRuntimesDir ?: defaultRuntimesDir).apply { mkdirs() }
    val binDir: File get() = File(rootDir, "bin").apply { mkdirs() }
    val tmpDir: File get() = File(rootDir, "tmp").apply { mkdirs() }
    val serversJson: File get() = File(rootDir, "servers.json")
    val configJson: File get() = File(rootDir, "desktop_config.json")

    init {
        rootDir.mkdirs()
        loadConfiguredDirectories()
        serversDir.mkdirs()
        runtimesDir.mkdirs()
        binDir.mkdirs()
        tmpDir.mkdirs()
    }

    private fun loadConfiguredDirectories() {
        if (!configJson.exists()) return
        runCatching {
            val obj = JSONObject(configJson.readText())
            obj.optString("serversDir", "").trim().takeIf { it.isNotEmpty() }?.let { path ->
                val f = File(path)
                if (f.isAbsolute && !f.isFile) {
                    customServersDir = f
                }
            }
            obj.optString("runtimesDir", "").trim().takeIf { it.isNotEmpty() }?.let { path ->
                val f = File(path)
                if (f.isAbsolute && !f.isFile) {
                    customRuntimesDir = f
                }
            }
        }
    }

    @Synchronized
    fun updateDirectories(serversDirPath: String?, runtimesDirPath: String?): Result<Pair<File, File>> {
        val nextServers = validateDirectoryOverride(serversDirPath, "Servers directory").getOrElse {
            return Result.failure(it)
        }
        val nextRuntimes = validateDirectoryOverride(runtimesDirPath, "Java runtimes directory").getOrElse {
            return Result.failure(it)
        }

        customServersDir = nextServers
        customRuntimesDir = nextRuntimes

        val obj = runCatching {
            if (configJson.exists()) JSONObject(configJson.readText()) else JSONObject()
        }.getOrDefault(JSONObject())

        if (nextServers != null) {
            obj.put("serversDir", nextServers.absolutePath)
        } else {
            obj.remove("serversDir")
        }
        if (nextRuntimes != null) {
            obj.put("runtimesDir", nextRuntimes.absolutePath)
        } else {
            obj.remove("runtimesDir")
        }
        configJson.writeText(obj.toString(2))

        val resolvedServers = serversDir
        val resolvedRuntimes = runtimesDir
        return Result.success(resolvedServers to resolvedRuntimes)
    }

    private fun validateDirectoryOverride(rawPath: String?, label: String): Result<File?> {
        val trimmed = rawPath?.trim().orEmpty()
        if (trimmed.isEmpty()) return Result.success(null)
        val dir = File(trimmed)
        if (!dir.isAbsolute) {
            return Result.failure(IllegalArgumentException("$label must be an absolute path."))
        }
        if (dir.exists() && !dir.isDirectory) {
            return Result.failure(IllegalArgumentException("$label points to an existing file, not a directory."))
        }
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.failure(IllegalArgumentException("Could not create $label: ${dir.absolutePath}"))
        }
        if (!dir.isDirectory || !dir.canWrite()) {
            return Result.failure(IllegalArgumentException("$label is not writable: ${dir.absolutePath}"))
        }
        return Result.success(dir)
    }
}
