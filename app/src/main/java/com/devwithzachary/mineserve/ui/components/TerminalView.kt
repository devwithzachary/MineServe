package com.devwithzachary.mineserve.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.mineserve.engine.TerminalEmulator
import com.devwithzachary.mineserve.ui.theme.EmeraldPrimary
import kotlin.math.abs

@Composable
fun TerminalCanvasView(
    emulator: TerminalEmulator,
    refreshTrigger: Long,
    onResizeTerminal: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 12
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }

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

    var accumulatedScrollY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        accumulatedScrollY += dragAmount.y
                        if (abs(accumulatedScrollY) >= charHeight) {
                            val lines = (accumulatedScrollY / charHeight).toInt()
                            accumulatedScrollY -= lines * charHeight
                            if (lines > 0) {
                                emulator.scrollUp(lines)
                            } else {
                                emulator.scrollDown(-lines)
                            }
                        }
                    }
                }
        ) {
            @Suppress("UNUSED_VARIABLE")
            val trigger = refreshTrigger
            val width = size.width
            val height = size.height

            val cols = (width / charWidth).toInt().coerceAtLeast(20)
            val rows = (height / charHeight).toInt().coerceAtLeast(10)

            if (emulator.cols != cols || emulator.rows != rows) {
                onResizeTerminal(cols, rows)
            }

            drawContext.canvas.nativeCanvas.apply {
                val currentRows = emulator.rows
                for (r in 0 until currentRows) {
                    val rowData = emulator.getRenderRow(r)
                    val y = r * charHeight + baselineOffset

                    var c = 0
                    while (c < rowData.size && c < emulator.cols) {
                        val cell = rowData[c]
                        val x = c * charWidth

                        // Draw Background
                        if (cell.bgColor != Color.Transparent) {
                            val bgPaint = Paint().apply {
                                color = cell.bgColor.toArgb()
                                style = Paint.Style.FILL
                            }
                            drawRect(
                                x,
                                r * charHeight,
                                x + charWidth,
                                (r + 1) * charHeight,
                                bgPaint
                            )
                        }

                        // Draw Char
                        if (cell.ch != ' ') {
                            paint.color = cell.fgColor.toArgb()
                            paint.isFakeBoldText = cell.bold
                            paint.isUnderlineText = cell.underline
                            drawText(cell.ch.toString(), x, y, paint)
                        }
                        c++
                    }
                }

                // Draw Cursor
                if (emulator.cursorVisible && emulator.scrollOffset == 0) {
                    val curX = emulator.cursorX * charWidth
                    val curY = emulator.cursorY * charHeight
                    val cursorPaint = Paint().apply {
                        color = emulator.theme.cursorColor.copy(alpha = 0.7f).toArgb()
                        style = Paint.Style.FILL
                    }
                    drawRect(
                        curX,
                        curY,
                        curX + charWidth,
                        curY + charHeight,
                        cursorPaint
                    )
                }
            }
        }

        // Scroll-to-bottom FAB indicator if user scrolled up
        if (emulator.scrollOffset > 0) {
            FloatingActionButton(
                onClick = { emulator.scrollToBottom() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(40.dp),
                containerColor = EmeraldPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Scroll to bottom",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
