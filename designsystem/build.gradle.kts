import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val excludeAndroid = providers.gradleProperty("excludeAndroid").orNull == "true"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

if (!excludeAndroid) {
    apply(plugin = libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
}

kotlin {
    jvmToolchain(21)

    jvm("desktop")

    if (!excludeAndroid) {
        (this as ExtensionAware).extensions.configure(KotlinMultiplatformAndroidLibraryTarget::class.java) {
            namespace = "com.devwithzachary.mineserve.designsystem"
            compileSdk = 36
            minSdk = 23
            androidResources.enable = true
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.compose.runtime)
            api(libs.jetbrains.compose.foundation)
            api(libs.jetbrains.compose.ui)
            api(libs.jetbrains.compose.material3)
            api(libs.jetbrains.compose.material.icons.extended)
            api(libs.jetbrains.compose.components.resources)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        if (!excludeAndroid) {
            androidMain.dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.compose.material3)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.devwithzachary.mineserve.designsystem.resources"
    generateResClass = always
}
