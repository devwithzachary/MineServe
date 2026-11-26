import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
}

val appVersionName = Regex("""versionName\s*=\s*"([^"]+)"""")
    .find(rootProject.file("app/build.gradle.kts").readText())
    ?.groupValues
    ?.getOrNull(1)
    ?: "1.8.1"

val hostOsName = System.getProperty("os.name").lowercase()
val isMacOs = hostOsName.contains("mac") || hostOsName.contains("darwin")
val isWindows = hostOsName.contains("win")

compose.desktop {
    application {
        mainClass = "com.devwithzachary.mineserve.desktop.MainKt"
        nativeDistributions {
            when {
                isMacOs -> targetFormats(TargetFormat.Dmg)
                isWindows -> targetFormats(TargetFormat.Msi, TargetFormat.Exe)
                else -> targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            }
            packageName = if (isMacOs || isWindows) "MineServe" else "mineserve"
            val numericPackageVersion = appVersionName.substringBefore("-").ifBlank { "1.0.0" }
            packageVersion = numericPackageVersion
            description = "MineServe - Cross-Platform Minecraft Server Manager"
            vendor = "DevWithZachary"
            modules("java.instrument", "java.management", "java.naming", "java.prefs", "java.security.jgss", "jdk.management", "jdk.unsupported")
            linux {
                iconFile.set(rootProject.file("designsystem/src/commonMain/composeResources/drawable/brand_icon_512.png"))
            }
            macOS {
                bundleID = "com.devwithzachary.mineserve"
                dockName = "MineServe"
                iconFile.set(rootProject.file("packaging/macos/mineserve.icns"))
            }
            windows {
                menuGroup = "MineServe"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "7e53f74b-2d49-4f7c-8a45-4c8d1f61b901"
                iconFile.set(rootProject.file("packaging/windows/mineserve.ico"))
            }
        }
    }
}

abstract class InstallDesktopDistTask : DefaultTask() {
    @get:InputFile
    abstract val mainJar: RegularFileProperty

    @get:InputFiles
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val installDir: DirectoryProperty

    @TaskAction
    fun install() {
        val root = installDir.get().asFile
        val libDir = File(root, "lib").apply {
            deleteRecursively()
            mkdirs()
        }
        val binDir = File(root, "bin").apply { mkdirs() }

        val jarFile = mainJar.get().asFile
        jarFile.copyTo(File(libDir, jarFile.name), overwrite = true)

        runtimeClasspath.files.forEach { file ->
            file.copyTo(File(libDir, file.name), overwrite = true)
        }

        val script = """
            #!/usr/bin/env bash
            set -e
            SCRIPT_PATH="${'$'}(readlink -f "${'$'}0" 2>/dev/null || realpath "${'$'}0" 2>/dev/null || printf '%s' "${'$'}0")"
            APP_HOME="${'$'}(cd "${'$'}(dirname "${'$'}SCRIPT_PATH")/.." && pwd)"
            exec java -cp "${'$'}APP_HOME/lib/*" com.devwithzachary.mineserve.desktop.MainKt "${'$'}@"
        """.trimIndent() + "\n"

        listOf("desktop", "mineserve").forEach { name ->
            val bin = File(binDir, name)
            bin.writeText(script)
            bin.setExecutable(true, false)
        }

        val batScript = """
            @echo off
            setlocal
            set "APP_HOME=%~dp0.."
            java -cp "%APP_HOME%\lib\*" com.devwithzachary.mineserve.desktop.MainKt %*
        """.trimIndent().replace("\n", "\r\n") + "\r\n"
        File(binDir, "mineserve.bat").writeText(batScript)
    }
}

tasks.register<InstallDesktopDistTask>("installDist") {
    mainJar.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    runtimeClasspath.from(configurations.getByName("runtimeClasspath"))
    installDir.set(layout.buildDirectory.dir("install/desktop"))
}

// Reuse the single source of truth Web UI assets from :app
tasks.named<ProcessResources>("processResources") {
    from(rootProject.file("app/src/main/assets/webadmin")) {
        into("webadmin")
    }
    from(rootProject.file("designsystem/src/commonMain/composeResources/drawable/brand_icon_512.png")) {
        into("webadmin")
        rename { "logo.png" }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":designsystem"))
    implementation(compose.desktop.currentOs)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation("org.json:json:20240303")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
