package com.devwithzachary.mineserve.ui.screens.whatsnew

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.devwithzachary.mineserve.model.ChangelogPlatform
import com.devwithzachary.mineserve.model.MineServeWhatsNewCatalog

data class WhatsNewFeature(
    val badge: String,
    val title: String,
    val summary: String,
    val stepsToFind: List<String>,
    val icon: ImageVector
)

data class WhatsNewRelease(
    val majorVersion: String,
    val displayVersion: String,
    val headline: String,
    val subtitle: String,
    val features: List<WhatsNewFeature>
)

private fun resolveIcon(iconKey: String): ImageVector = when (iconKey) {
    "palette" -> Icons.Default.Palette
    "security" -> Icons.Default.Security
    "web" -> Icons.Default.Dns
    "mods" -> Icons.Default.Extension
    "cloud" -> Icons.Default.Cloud
    "map" -> Icons.Default.Public
    else -> Icons.Default.Terminal
}

val CURRENT_WHATS_NEW_RELEASE: WhatsNewRelease =
    MineServeWhatsNewCatalog.getForPlatform(ChangelogPlatform.ANDROID).let { shared ->
        WhatsNewRelease(
            majorVersion = shared.majorVersion,
            displayVersion = shared.displayVersion,
            headline = shared.headline,
            subtitle = shared.subtitle,
            features = shared.features.map { f ->
                WhatsNewFeature(
                    badge = f.badge,
                    title = f.title,
                    summary = f.summary,
                    stepsToFind = f.stepsToFind,
                    icon = resolveIcon(f.iconKey)
                )
            }
        )
    }
