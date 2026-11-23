package com.devwithzachary.mineserve.desktop

import java.io.File

object SystemdServiceInstaller {
    fun generateServiceUnit(
        user: String = System.getProperty("user.name") ?: "mineserve",
        execPath: String = "/usr/local/bin/mineserve",
        dataDir: File = LinuxDataPaths.defaultDataDir(),
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
}
