package com.devwithzachary.mineserve.designsystem.assets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.devwithzachary.mineserve.designsystem.resources.Res
import com.devwithzachary.mineserve.designsystem.resources.brand_icon_512
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Centralized brand asset references backed by composeResources.
 * Replacing the designer files in composeResources updates Android, Desktop,
 * Linux packages, and WebUI in one commit.
 */
object MineServeBrandAssets {
    val BrandIcon512: DrawableResource = Res.drawable.brand_icon_512
    const val BRAND_LOGO_SVG_PATH: String = "files/brand_logo.svg"

    @Composable
    fun brandIconPainter(): Painter = painterResource(BrandIcon512)
}
