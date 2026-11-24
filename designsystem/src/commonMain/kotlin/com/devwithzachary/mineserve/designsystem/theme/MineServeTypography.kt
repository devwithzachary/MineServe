package com.devwithzachary.mineserve.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.designsystem.resources.Res
import com.devwithzachary.mineserve.designsystem.resources.jetbrains_mono_bold
import com.devwithzachary.mineserve.designsystem.resources.jetbrains_mono_regular
import com.devwithzachary.mineserve.designsystem.resources.plus_jakarta_sans_bold
import com.devwithzachary.mineserve.designsystem.resources.plus_jakarta_sans_medium
import com.devwithzachary.mineserve.designsystem.resources.plus_jakarta_sans_regular
import com.devwithzachary.mineserve.designsystem.resources.plus_jakarta_sans_semibold
import org.jetbrains.compose.resources.Font

val LocalMineServeMonoFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

@Composable
fun rememberPlusJakartaSansFontFamily(): FontFamily {
    val regular = Font(Res.font.plus_jakarta_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.plus_jakarta_sans_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.plus_jakarta_sans_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.plus_jakarta_sans_bold, FontWeight.Bold)
    return remember(regular, medium, semiBold, bold) {
        FontFamily(regular, medium, semiBold, bold)
    }
}

@Composable
fun rememberJetBrainsMonoFontFamily(): FontFamily {
    val regular = Font(Res.font.jetbrains_mono_regular, FontWeight.Normal)
    val bold = Font(Res.font.jetbrains_mono_bold, FontWeight.Bold)
    return remember(regular, bold) {
        FontFamily(regular, bold)
    }
}

/**
 * Maps all 15 Material 3 Typography roles strictly to Plus Jakarta Sans.
 * JetBrains Mono is never used on UI chrome, buttons, drawers, or tab labels.
 */
@Composable
fun rememberMineServeTypography(
    uiFontFamily: FontFamily = rememberPlusJakartaSansFontFamily()
): Typography {
    return remember(uiFontFamily) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp
            ),
            displayMedium = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp
            ),
            displaySmall = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
            titleSmall = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp
            ),
            bodySmall = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp
            ),
            labelLarge = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            labelMedium = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            ),
            labelSmall = TextStyle(
                fontFamily = uiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}
