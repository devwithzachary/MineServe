package com.devwithzachary.mineserve.ui.components

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.engine.TerminalChar
import com.devwithzachary.mineserve.engine.TerminalEmulator
import com.devwithzachary.mineserve.ui.theme.EmeraldDark
import com.devwithzachary.mineserve.ui.theme.EmeraldLight
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import com.devwithzachary.mineserve.ui.theme.ObsidianCard
import com.devwithzachary.mineserve.ui.theme.ObsidianCardBorder
import com.devwithzachary.mineserve.ui.theme.Slate400
import kotlin.math.max
import kotlin.math.min

@Composable
fun TerminalCanvasView(
    emulator: TerminalEmulator,
    refreshTrigger: Long,
    onResizeTerminal: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 12
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    var selectionStart by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (row, col)
    var selectionEnd by remember { mutableStateOf<Pair<Int, Int>?>(null) }   // (row, col)
    var accumulatedScrollY by remember { mutableFloatStateOf(0f) }
    var localScrollTick by remember { mutableLongStateOf(0L) }

    val paint = remember(fontSizePx) {
        Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = fontSizePx
            isAntiAlias = true
        }
    }

    val fontMetrics = paint.fontMetrics
    val charWidth = paint.measureText("W").coerceAtLeast(1f)
    val charHeight = (fontMetrics.bottom - fontMetrics.top).coerceAtLeast(1f)
    val baselineOffset = -fontMetrics.top

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(emulator.theme.defaultBg)
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val cols = max(20, (widthPx / charWidth).toInt())
        val rows = max(5, (heightPx / charHeight).toInt())

        LaunchedEffect(cols, rows) {
            if (emulator.cols != cols || emulator.rows != rows) {
                onResizeTerminal(cols, rows)
            }
        }

        val selStart = selectionStart
        val selEnd = selectionEnd
        val hasSelection = selStart != null && selEnd != null

        // Normalized linear selection bounds: (fromR, fromC) <= (toR, toC)
        val (fromR, fromC, toR, toC) = remember(selStart, selEnd, cols) {
            if (selStart != null && selEnd != null) {
                val startLinear = selStart.first * cols + selStart.second
                val endLinear = selEnd.first * cols + selEnd.second
                if (startLinear <= endLinear) {
                    listOf(selStart.first, selStart.second, selEnd.first, selEnd.second)
                } else {
                    listOf(selEnd.first, selEnd.second, selStart.first, selStart.second)
                }
            } else {
                listOf(0, 0, 0, 0)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cols, rows) {
                    // Smooth vertical drag gestures for scrolling through terminal log history
                    detectDragGestures(
                        onDragStart = {
                            accumulatedScrollY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedScrollY += dragAmount.y
                            val threshold = charHeight * 0.75f
                            if (accumulatedScrollY > threshold) {
                                val lines = (accumulatedScrollY / charHeight).toInt().coerceAtLeast(1)
                                emulator.scrollUp(lines)
                                accumulatedScrollY %= charHeight
                                localScrollTick++
                            } else if (accumulatedScrollY < -threshold) {
                                val lines = (-accumulatedScrollY / charHeight).toInt().coerceAtLeast(1)
                                emulator.scrollDown(lines)
                                accumulatedScrollY %= charHeight
                                localScrollTick++
                            }
                        }
                    )
                }
                .pointerInput(cols, rows) {
                    detectTapGestures(
                        onTap = {
                            if (selectionStart != null || selectionEnd != null) {
                                selectionStart = null
                                selectionEnd = null
                            }
                        },
                        onLongPress = { offset ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            val c = (offset.x / charWidth).toInt().coerceIn(0, cols - 1)
                            val r = (offset.y / charHeight).toInt().coerceIn(0, rows - 1)
                            val wordRange = emulator.getWordAt(r, c)
                            selectionStart = Pair(r, wordRange.first)
                            selectionEnd = Pair(r, wordRange.second)
                        }
                    )
                }
        ) {
            // Terminal Screen & Selection Highlighting Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE")
                val trigger = refreshTrigger + localScrollTick

                val theme = emulator.theme
                drawRect(color = theme.defaultBg)

                val curX = emulator.cursorX
                val curY = emulator.cursorY
                val cursorVisible = emulator.cursorVisible
                val isScrolledBack = emulator.scrollOffset > 0

                val renderRows = min(rows, emulator.rows)
                val nativeCanvas = drawContext.canvas.nativeCanvas

                for (r in 0 until renderRows) {
                    val rowY = r * charHeight
                    val rowChars = emulator.getRenderRow(r)
                    val actualCols = min(rowChars.size, cols)

                    for (c in 0 until actualCols) {
                        val cell: TerminalChar = rowChars[c]
                        val cellX = c * charWidth

                        // Linear multi-line selection check
                        val isSelected = hasSelection && when {
                            r < fromR || r > toR -> false
                            fromR == toR -> c in fromC..toC
                            r == fromR -> c >= fromC
                            r == toR -> c <= toC
                            else -> true
                        }

                        // 1. Draw Selection or Background Color
                        if (isSelected) {
                            paint.color = theme.selectionColor.toArgb()
                            nativeCanvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                        } else if (cell.bgColor != Color.Transparent) {
                            paint.color = cell.bgColor.toArgb()
                            nativeCanvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                        }

                        // 2. Draw Cursor or Foreground Character
                        if (!isScrolledBack && cursorVisible && r == curY && c == curX) {
                            paint.color = theme.cursorColor.toArgb()
                            nativeCanvas.drawRect(cellX, rowY, cellX + charWidth, rowY + charHeight, paint)
                            paint.color = theme.defaultBg.toArgb()
                        } else if (isSelected) {
                            paint.color = theme.defaultFg.toArgb()
                        } else {
                            paint.color = cell.fgColor.toArgb()
                        }

                        paint.isFakeBoldText = cell.bold
                        paint.isUnderlineText = cell.underline

                        if (cell.ch != ' ') {
                            nativeCanvas.drawText(
                                cell.ch.toString(),
                                cellX,
                                rowY + baselineOffset,
                                paint
                            )
                        }
                    }
                }
            }

            // Draggable Text Selection Handles
            if (hasSelection) {
                var dragStartAnchor by remember { mutableStateOf(Offset.Zero) }
                var dragEndAnchor by remember { mutableStateOf(Offset.Zero) }

                // Start Selection Handle (top-left anchor)
                val startHandlePos = Offset(fromC * charWidth, (fromR + 1) * charHeight)
                TerminalSelectionHandle(
                    position = startHandlePos,
                    isStart = true,
                    handleColor = EmeraldPrimary,
                    onDragStart = {
                        dragStartAnchor = Offset(fromC * charWidth + charWidth * 0.5f, fromR * charHeight + charHeight * 0.5f)
                    },
                    onDrag = { dragDelta ->
                        val curPixelX = dragStartAnchor.x + dragDelta.x
                        val curPixelY = dragStartAnchor.y + dragDelta.y
                        val newR = (curPixelY / charHeight).toInt().coerceIn(0, rows - 1)
                        val newC = (curPixelX / charWidth).toInt().coerceIn(0, cols - 1)
                        if (selectionStart?.first != newR || selectionStart?.second != newC) {
                            try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            selectionStart = Pair(newR, newC)
                        }
                    },
                    onDragEnd = {}
                )

                // End Selection Handle (bottom-right anchor)
                val endHandlePos = Offset((toC + 1) * charWidth, (toR + 1) * charHeight)
                TerminalSelectionHandle(
                    position = endHandlePos,
                    isStart = false,
                    handleColor = EmeraldPrimary,
                    onDragStart = {
                        dragEndAnchor = Offset(toC * charWidth + charWidth * 0.5f, toR * charHeight + charHeight * 0.5f)
                    },
                    onDrag = { dragDelta ->
                        val curPixelX = dragEndAnchor.x + dragDelta.x
                        val curPixelY = dragEndAnchor.y + dragDelta.y
                        val newR = (curPixelY / charHeight).toInt().coerceIn(0, rows - 1)
                        val newC = (curPixelX / charWidth).toInt().coerceIn(0, cols - 1)
                        if (selectionEnd?.first != newR || selectionEnd?.second != newC) {
                            try { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Exception) {}
                            selectionEnd = Pair(newR, newC)
                        }
                    },
                    onDragEnd = {}
                )
            }

            // Floating Selection Toolbar
            AnimatedVisibility(
                visible = hasSelection,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ObsidianCard,
                    shadowElevation = 8.dp,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Copy Selected Text
                        FilledTonalButton(
                            onClick = {
                                val s = selectionStart
                                val e = selectionEnd
                                if (s != null && e != null) {
                                    val text = emulator.getSelectedText(s.first, s.second, e.first, e.second)
                                    if (text.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(text))
                                    }
                                }
                                selectionStart = null
                                selectionEnd = null
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Selection",
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        // 2. Select All Screen Text
                        TextButton(
                            onClick = {
                                selectionStart = Pair(0, 0)
                                selectionEnd = Pair(rows - 1, cols - 1)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                modifier = Modifier.size(16.dp),
                                tint = EmeraldLight
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select All", fontSize = 12.sp, color = EmeraldLight)
                        }

                        // 3. Share Selected Text
                        IconButton(
                            onClick = {
                                val s = selectionStart
                                val e = selectionEnd
                                if (s != null && e != null) {
                                    val text = emulator.getSelectedText(s.first, s.second, e.first, e.second)
                                    if (text.isNotEmpty()) {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, text)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, "Share Console Logs")
                                        context.startActivity(shareIntent)
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Selection",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // 4. Dismiss / Clear Selection
                        IconButton(
                            onClick = {
                                selectionStart = null
                                selectionEnd = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear Selection",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Floating "Scroll to Bottom" Button when scrolled up into history
            val currentScrollOffset = remember(refreshTrigger, localScrollTick) { emulator.scrollOffset }
            if (currentScrollOffset > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                emulator.scrollToBottom()
                                localScrollTick++
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Scroll to Bottom",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Scroll to Bottom ($currentScrollOffset)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draggable touch handle placed at the beginning or end of selected terminal text.
 */
@Composable
private fun TerminalSelectionHandle(
    position: Offset,
    isStart: Boolean,
    handleColor: Color = EmeraldPrimary,
    onDragStart: () -> Unit,
    onDrag: (dragDelta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val handleTouchSize = 44.dp
    val handleTouchSizePx = with(LocalDensity.current) { handleTouchSize.toPx() }

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .offset(
                x = with(LocalDensity.current) { (position.x - (if (isStart) handleTouchSizePx * 0.75f else handleTouchSizePx * 0.25f)).toDp() },
                y = with(LocalDensity.current) { position.y.toDp() }
            )
            .size(handleTouchSize)
            .pointerInput(isStart) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulated = Offset.Zero
                        currentOnDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                        currentOnDrag(dragAccumulated)
                    },
                    onDragEnd = {
                        currentOnDragEnd()
                    },
                    onDragCancel = {
                        currentOnDragEnd()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radius = w * 0.32f
            val anchorX = if (isStart) w * 0.75f else w * 0.25f
            val anchorY = 0f
            val circleCenterX = w * 0.5f
            val circleCenterY = h * 0.55f

            val path = Path().apply {
                if (isStart) {
                    moveTo(anchorX, anchorY)
                    lineTo(anchorX, circleCenterY)
                    arcTo(
                        rect = Rect(circleCenterX - radius, circleCenterY - radius, circleCenterX + radius, circleCenterY + radius),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = 270f,
                        forceMoveTo = false
                    )
                    close()
                } else {
                    moveTo(anchorX, anchorY)
                    lineTo(anchorX, circleCenterY)
                    arcTo(
                        rect = Rect(circleCenterX - radius, circleCenterY - radius, circleCenterX + radius, circleCenterY + radius),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -270f,
                        forceMoveTo = false
                    )
                    close()
                }
            }
            drawPath(path, color = handleColor)
        }
    }
}
