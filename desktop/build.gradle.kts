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

compose.desktop {
    application {
        mainClass = "com.devwithzachary.mineserve.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            packageName = "mineserve"
            packageVersion = "1.8.0"
            description = "MineServe - Native Linux Minecraft Server Manager"
            vendor = "DevWithZachary"
            modules("java.instrument", "java.management", "java.naming", "java.prefs", "java.security.jgss", "jdk.unsupported")
            linux {
                iconFile.set(rootProject.file("app/src/main/res/drawable/ic_launcher_logo.png"))
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
    from(rootProject.file("app/src/main/res/drawable/ic_launcher_logo.png")) {
        into("webadmin")
        rename { "logo.png" }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation("org.json:json:20240303")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
