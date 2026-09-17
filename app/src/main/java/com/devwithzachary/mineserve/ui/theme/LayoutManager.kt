package com.devwithzachary.mineserve.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Central layout manager and design tokens controlling spacing, margins,
 * card paddings, and layout dimensions across the application.
 */
object LayoutManager {
    /**
     * Vertical spacing between cards in lists and columns.
     */
    val cardSpacing: Dp = 8.dp

    /**
     * Screen-level horizontal padding on the left and right of cards.
     */
    val screenHorizontalPadding: Dp = 8.dp

    /**
     * Screen-level vertical padding for scrollable containers.
     */
    val screenVerticalPadding: Dp = 8.dp

    /**
     * Internal content padding within standard cards.
     */
    val cardPadding: Dp = 16.dp

    /**
     * Internal content padding within compact cards.
     */
    val compactCardPadding: Dp = 12.dp

    /**
     * Horizontal edge padding for scrollable tab rows.
     */
    val tabEdgePadding: Dp = 8.dp

    /**
     * Spacing between small items, tags, or chips inside rows.
     */
    val itemSpacingSmall: Dp = 4.dp

    /**
     * Medium spacing between related items in rows or forms.
     */
    val itemSpacingMedium: Dp = 8.dp

    /**
     * Large spacing between distinct groups or form sections.
     */
    val itemSpacingLarge: Dp = 12.dp

    /**
     * Corner radius for standard cards and major surfaces.
     */
    val cardCornerRadius: Dp = 16.dp

    /**
     * Corner radius for compact cards and secondary elements.
     */
    val compactCardCornerRadius: Dp = 12.dp

    /**
     * Corner radius for buttons, text fields, and chips.
     */
    val chipCornerRadius: Dp = 8.dp

    /**
     * Standard card outline stroke width.
     */
    val cardBorderWidth: Dp = 1.dp

    /**
     * Screen-level horizontal margin on the left and right of popup dialogs.
     */
    val dialogHorizontalMargin: Dp = 8.dp

    /**
     * Corner radius for popup dialog containers.
     */
    val dialogCornerRadius: Dp = 16.dp

    /**
     * Standard dialog properties ensuring dialogs span the intended width
     * without platform default outer margin restrictions.
     */
    val dialogProperties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false
    )

    /**
     * Modifier applied to popup dialogs ensuring consistent screen margins
     * controlled centrally by the LayoutManager.
     */
    val dialogModifier: Modifier
        get() = Modifier
            .fillMaxWidth()
            .padding(horizontal = dialogHorizontalMargin)
}

typealias AppLayout = LayoutManager

val LocalLayoutManager = staticCompositionLocalOf { LayoutManager }

val androidx.compose.material3.MaterialTheme.layout: LayoutManager
    @Composable
    @ReadOnlyComposable
    get() = LocalLayoutManager.current
