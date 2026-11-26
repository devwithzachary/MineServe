package com.devwithzachary.mineserve.desktop

import com.devwithzachary.mineserve.model.ChangelogPlatform
import java.awt.Desktop
import java.io.File
import java.net.URI

enum class DesktopOs {
    LINUX,
    MACOS,
    WINDOWS
}

object DesktopPlatform {
    fun detectOsFromName(osName: String): DesktopOs {
        val lower = osName.lowercase()
        return when {
            lower.contains("mac") || lower.contains("darwin") -> DesktopOs.MACOS
            lower.contains("win") -> DesktopOs.WINDOWS
            else -> DesktopOs.LINUX
        }
    }

    val currentOs: DesktopOs by lazy {
        detectOsFromName(System.getProperty("os.name") ?: "Linux")
    }

    val isLinux: Boolean get() = currentOs == DesktopOs.LINUX
    val isMacOs: Boolean get() = currentOs == DesktopOs.MACOS
    val isWindows: Boolean get() = currentOs == DesktopOs.WINDOWS

    val normalizedArch: String
        get() {
            val arch = (System.getProperty("os.arch") ?: "amd64").lowercase()
            return when {
                arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
                arch.contains("arm") -> "arm"
                else -> "x64"
            }
        }

    val platformKey: String
        get() = when (currentOs) {
            DesktopOs.LINUX -> "linux"
            DesktopOs.MACOS -> "macos"
            DesktopOs.WINDOWS -> "windows"
        }

    val shortName: String
        get() = when (currentOs) {
            DesktopOs.LINUX -> "Linux"
            DesktopOs.MACOS -> "macOS"
            DesktopOs.WINDOWS -> "Windows"
        }

    val displayName: String
        get() = "$shortName Desktop"

    val changelogPlatform: ChangelogPlatform
        get() = when (currentOs) {
            DesktopOs.LINUX -> ChangelogPlatform.LINUX
            DesktopOs.MACOS -> ChangelogPlatform.MACOS
            DesktopOs.WINDOWS -> ChangelogPlatform.WINDOWS
        }

    val javaExecutableName: String
        get() = if (isWindows) "java.exe" else "java"

    val bedrockBinaryName: String
        get() = if (isWindows) "bedrock_server.exe" else "bedrock_server"

    fun openFile(file: File): Boolean {
        val openedViaAwt = runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file)
                true
            } else {
                false
            }
        }.getOrDefault(false)
        if (openedViaAwt) return true

        return runCatching {
            val cmd = when (currentOs) {
                DesktopOs.MACOS -> listOf("open", file.absolutePath)
                DesktopOs.WINDOWS -> listOf("explorer.exe", file.absolutePath)
                DesktopOs.LINUX -> listOf("xdg-open", file.absolutePath)
            }
            ProcessBuilder(cmd).start()
            true
        }.getOrDefault(false)
    }

    fun openUrl(url: String): Boolean {
        val openedViaAwt = runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                true
            } else {
                false
            }
        }.getOrDefault(false)
        if (openedViaAwt) return true

        return runCatching {
            val cmd = when (currentOs) {
                DesktopOs.MACOS -> listOf("open", url)
                DesktopOs.WINDOWS -> listOf("cmd", "/c", "start", "", url)
                DesktopOs.LINUX -> listOf("xdg-open", url)
            }
            ProcessBuilder(cmd).start()
            true
        }.getOrDefault(false)
    }
}
