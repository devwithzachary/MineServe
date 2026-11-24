package com.devwithzachary.mineserve.designsystem.assets

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Desktop window and system-tray brand asset loader reading directly from the
 * centralized composeResources bundle (with /webadmin/logo.png fallback).
 */
object DesktopBrandAssets {
    private const val COMPOSE_RESOURCE_ICON_PATH =
        "/composeResources/com.devwithzachary.mineserve.designsystem.resources/drawable/brand_icon_512.png"
    private const val FALLBACK_WEBADMIN_ICON_PATH = "/webadmin/logo.png"

    fun loadWindowIconBufferedImage(): BufferedImage? {
        val classLoader = DesktopBrandAssets::class.java
        val paths = listOf(COMPOSE_RESOURCE_ICON_PATH, FALLBACK_WEBADMIN_ICON_PATH)
        for (path in paths) {
            val image = runCatching {
                classLoader.getResourceAsStream(path)?.use { ImageIO.read(it) }
            }.getOrNull()
            if (image != null) return image
        }
        return null
    }

    fun loadWindowIconPainter(): Painter? {
        return loadWindowIconBufferedImage()?.let { BitmapPainter(it.toComposeImageBitmap()) }
    }
}
