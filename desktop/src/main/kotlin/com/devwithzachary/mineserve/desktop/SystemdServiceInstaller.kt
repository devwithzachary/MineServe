package com.devwithzachary.mineserve.desktop

import java.io.File

object SystemdServiceInstaller {
    fun generateServiceUnit(
        user: String = System.getProperty("user.name") ?: "mineserve",
        execPath: String = "/usr/bin/mineserve",
        dataDir: File = DesktopDataPaths.defaultDataDir(),
        port: Int = 8080,
        bindHost: String = "0.0.0.0"
    ): String {
        return """
            [Unit]
            Description=MineServe Headless Minecraft Server Manager
            After=network-online.target
            Wants=network-online.target

            [Service]
            Type=simple
            User=$user
            WorkingDirectory=${dataDir.absolutePath}
            ExecStart=$execPath --headless --bind $bindHost --port $port --data-dir ${dataDir.absolutePath}
            Restart=on-failure
            RestartSec=5
            TimeoutStopSec=45
            LimitNOFILE=65536

            [Install]
            WantedBy=multi-user.target
        """.trimIndent() + "\n"
    }

    fun generateLaunchdPlist(
        execPath: String = "/Applications/MineServe.app/Contents/MacOS/MineServe",
        dataDir: File = DesktopDataPaths.defaultDataDirFor(DesktopOs.MACOS, System.getProperty("user.home") ?: ".", System.getenv()),
        port: Int = 8080,
        bindHost: String = "0.0.0.0"
    ): String {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
              <key>Label</key>
              <string>com.devwithzachary.mineserve</string>
              <key>ProgramArguments</key>
              <array>
                <string>$execPath</string>
                <string>--headless</string>
                <string>--bind</string>
                <string>$bindHost</string>
                <string>--port</string>
                <string>$port</string>
                <string>--data-dir</string>
                <string>${dataDir.absolutePath}</string>
              </array>
              <key>WorkingDirectory</key>
              <string>${dataDir.absolutePath}</string>
              <key>RunAtLoad</key>
              <true/>
              <key>KeepAlive</key>
              <true/>
              <key>StandardOutPath</key>
              <string>${dataDir.absolutePath}/mineserve.log</string>
              <key>StandardErrorPath</key>
              <string>${dataDir.absolutePath}/mineserve.err.log</string>
            </dict>
            </plist>
        """.trimIndent() + "\n"
    }

    fun generateWindowsStartupScript(
        execPath: String = "C:\\Program Files\\MineServe\\MineServe.exe",
        dataDir: File = DesktopDataPaths.defaultDataDirFor(DesktopOs.WINDOWS, System.getProperty("user.home") ?: ".", System.getenv()),
        port: Int = 8080,
        bindHost: String = "0.0.0.0"
    ): String {
        return """
            @echo off
            REM Save as mineserve-headless.bat or register via Windows Task Scheduler (schtasks)
            "$execPath" --headless --bind $bindHost --port $port --data-dir "${dataDir.absolutePath}"
        """.trimIndent() + "\n"
    }

    fun generateForCurrentOs(
        dataDir: File = DesktopDataPaths.defaultDataDir(),
        port: Int = 8080,
        bindHost: String = "0.0.0.0"
    ): String = when (DesktopPlatform.currentOs) {
        DesktopOs.LINUX -> generateServiceUnit(dataDir = dataDir, port = port, bindHost = bindHost)
        DesktopOs.MACOS -> generateLaunchdPlist(dataDir = dataDir, port = port, bindHost = bindHost)
        DesktopOs.WINDOWS -> generateWindowsStartupScript(dataDir = dataDir, port = port, bindHost = bindHost)
    }
}
