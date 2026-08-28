package com.devwithzachary.mineserve.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate950
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeHelper {

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = android.graphics.Color.WHITE,
        lightColor: Int = android.graphics.Color.TRANSPARENT
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun buildBedrockDeepLink(serverName: String, host: String, port: Int): String {
        val encodedName = Uri.encode(serverName)
        return "minecraft://?addExternalServer=$encodedName|$host:$port"
    }

    fun buildJavaAddress(host: String, port: Int): String {
        return if (port == 25565) host else "$host:$port"
    }

    fun buildInviteText(
        serverName: String,
        serverType: String,
        version: String,
        host: String,
        port: Int,
        isTunnel: Boolean
    ): String {
        val connectionType = if (isTunnel) "Public Online Link (No Port Forwarding needed)" else "Local Wi-Fi LAN"
        val bedrockLink = buildBedrockDeepLink(serverName, host, port)
        return """
            🎮 Join my Minecraft Server!
            
            Server: $serverName
            Engine: $serverType ($version)
            Network: $connectionType
            
            🔹 Java Edition Address: $host:$port
            🔹 Bedrock Address: $host (Port: $port)
            🔹 Bedrock 1-Tap Join Link: $bedrockLink
            
            Hosted with MineServe on Android
        """.trimIndent()
    }
}

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 190.dp,
    qrColor: Color = EmeraldLight,
    backgroundColor: Color = Slate950
) {
    val darkArgb = qrColor.toArgb()
    val bgArgb = backgroundColor.toArgb()

    val bitmap = remember(content, darkArgb, bgArgb) {
        QrCodeHelper.generateQrBitmap(content, sizePx = 512, darkColor = darkArgb, lightColor = bgArgb)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.5.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Server QR Code",
                modifier = Modifier.size(sizeDp - 24.dp)
            )
        }
    }
}
