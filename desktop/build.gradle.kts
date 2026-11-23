plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.devwithzachary.mineserve.desktop.MainKt")
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation("org.json:json:20240303")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
