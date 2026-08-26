package com.devwithzachary.mineserve.engine

import androidx.compose.ui.graphics.Color

data class TerminalTheme(
    val name: String,
    val defaultFg: Color,
    val defaultBg: Color,
    val cursorColor: Color,
    val ansiColors: List<Color>
) {
    companion object {
        val OBSIDIAN = TerminalTheme(
            name = "Obsidian Dark",
            defaultFg = Color(0xFFE2E8F0),
            defaultBg = Color(0xFF0F172A),
            cursorColor = Color(0xFF10B981), // Emerald accent
            ansiColors = listOf(
                Color(0xFF0F172A), // 0: Black
                Color(0xFFEF4444), // 1: Red
                Color(0xFF10B981), // 2: Green
                Color(0xFFF59E0B), // 3: Yellow
                Color(0xFF3B82F6), // 4: Blue
                Color(0xFFA855F7), // 5: Magenta
                Color(0xFF06B6D4), // 6: Cyan
                Color(0xFFE2E8F0), // 7: White
                Color(0xFF64748B), // 8: Bright Black
                Color(0xFFF87171), // 9: Bright Red
                Color(0xFF34D399), // 10: Bright Green
                Color(0xFFFBBF24), // 11: Bright Yellow
                Color(0xFF60A5FA), // 12: Bright Blue
                Color(0xFFC084FC), // 13: Bright Magenta
                Color(0xFF22D3EE), // 14: Bright Cyan
                Color(0xFFFFFFFF)  // 15: Bright White
            )
        )

        val DRACULA = TerminalTheme(
            name = "Dracula",
            defaultFg = Color(0xFFF8F8F2),
            defaultBg = Color(0xFF282A36),
            cursorColor = Color(0xFF50FA7B),
            ansiColors = listOf(
                Color(0xFF21222C), Color(0xFFFF5555), Color(0xFF50FA7B), Color(0xFFF1FA8C),
                Color(0xFFBD93F9), Color(0xFFFF79C6), Color(0xFF8BE9FD), Color(0xFFF8F8F2),
                Color(0xFF6272A4), Color(0xFFFF6E6E), Color(0xFF69FF94), Color(0xFFFFFFA5),
                Color(0xFFD6ACFF), Color(0xFFFF92DF), Color(0xFFA4FFFF), Color(0xFFFFFFFF)
            )
        )
    }
}

data class TerminalChar(
    val ch: Char = ' ',
    val fgColor: Color = Color(0xFFE0E0E0),
    val bgColor: Color = Color.Transparent,
    val bold: Boolean = false,
    val underline: Boolean = false,
    val reverse: Boolean = false
)

class TerminalEmulator(
    var cols: Int = 80,
    var rows: Int = 24,
    val maxScrollback: Int = 2000
) {
    private var primaryGrid = Array(rows) { Array(cols) { TerminalChar() } }
    private var altGrid = Array(rows) { Array(cols) { TerminalChar() } }
    var grid = primaryGrid
        private set

    val scrollback = mutableListOf<Array<TerminalChar>>()
    var scrollOffset = 0
        private set

    fun scrollUp(lines: Int = 1) {
        if (scrollback.isNotEmpty()) {
            scrollOffset = (scrollOffset + lines).coerceIn(0, scrollback.size)
        }
    }

    fun scrollDown(lines: Int = 1) {
        scrollOffset = (scrollOffset - lines).coerceIn(0, scrollback.size)
    }

    fun scrollToBottom() {
        scrollOffset = 0
    }

    fun getRenderRow(r: Int): Array<TerminalChar> {
        if (scrollOffset == 0 || scrollback.isEmpty()) {
            return if (r < grid.size) grid[r] else Array(cols) { TerminalChar() }
        }
        val totalHistory = scrollback.size
        val targetIndex = (totalHistory + r) - scrollOffset
        return when {
            targetIndex < 0 -> Array(cols) { TerminalChar() }
            targetIndex < totalHistory -> scrollback[targetIndex]
            else -> {
                val gridIndex = targetIndex - totalHistory
                if (gridIndex < grid.size) grid[gridIndex] else Array(cols) { TerminalChar() }
            }
        }
    }

    var cursorX = 0
        private set
    var cursorY = 0
        private set
    var cursorVisible = true
        private set
    var appCursorKeys = false
        private set

    private var savedCursorX = 0
    private var savedCursorY = 0

    var scrollTop = 0
        private set
    var scrollBottom = rows - 1
        private set

    var theme: TerminalTheme = TerminalTheme.OBSIDIAN
        private set

    private var currentFg: Color = theme.defaultFg
    private var currentBg: Color = Color.Transparent
    private var isBold = false
    private var isUnderline = false
    private var isReverse = false
    private var inAltBuffer = false

    private enum class State { NORMAL, ESCAPE, CSI, OSC, CHARSET }
    private var state = State.NORMAL
    private val csiParams = StringBuilder()
    private val ansiColors = Array(16) { i -> theme.ansiColors.getOrElse(i) { Color.White } }

    fun applyTheme(newTheme: TerminalTheme) {
        theme = newTheme
        for (i in 0 until 16) {
            if (i < newTheme.ansiColors.size) {
                ansiColors[i] = newTheme.ansiColors[i]
            }
        }
        resetSgr()
    }

    fun resize(newCols: Int, newRows: Int) {
        if (newCols <= 0 || newRows <= 0) return
        val oldCols = cols
        val oldRows = rows
        cols = newCols
        rows = newRows

        primaryGrid = Array(rows) { r ->
            Array(cols) { c ->
                if (r < oldRows && c < oldCols) grid[r][c] else TerminalChar()
            }
        }
        altGrid = Array(rows) { Array(cols) { TerminalChar() } }
        grid = if (inAltBuffer) altGrid else primaryGrid

        scrollTop = 0
        scrollBottom = rows - 1

        cursorX = cursorX.coerceIn(0, cols - 1)
        cursorY = cursorY.coerceIn(0, rows - 1)
    }

    @Synchronized
    fun appendBytes(buffer: ByteArray, length: Int) {
        val text = String(buffer, 0, length, Charsets.UTF_8)
        for (ch in text) {
            processChar(ch)
        }
    }

    private fun processChar(ch: Char) {
        when (state) {
            State.NORMAL -> {
                when (ch) {
                    '\u001B' -> state = State.ESCAPE
                    '\r' -> cursorX = 0
                    '\n' -> lineFeed()
                    '\b' -> if (cursorX > 0) cursorX--
                    '\t' -> cursorX = (((cursorX / 8) + 1) * 8).coerceAtMost(cols - 1)
                    '\u0007' -> {}
                    else -> {
                        if (ch >= ' ') {
                            if (cursorX >= cols) {
                                cursorX = 0
                                lineFeed()
                            }
                            if (cursorY < rows && cursorX < cols) {
                                grid[cursorY][cursorX] = TerminalChar(
                                    ch = ch,
                                    fgColor = currentFg,
                                    bgColor = currentBg,
                                    bold = isBold,
                                    underline = isUnderline,
                                    reverse = isReverse
                                )
                                cursorX++
                            }
                        }
                    }
                }
            }
            State.ESCAPE -> {
                when (ch) {
                    '[' -> {
                        csiParams.clear()
                        state = State.CSI
                    }
                    ']' -> state = State.OSC
                    '(' -> state = State.CHARSET
                    '7' -> { savedCursorX = cursorX; savedCursorY = cursorY; state = State.NORMAL }
                    '8' -> { cursorX = savedCursorX.coerceIn(0, cols - 1); cursorY = savedCursorY.coerceIn(0, rows - 1); state = State.NORMAL }
                    'M' -> { reverseIndex(); state = State.NORMAL }
                    '=' -> { appCursorKeys = true; state = State.NORMAL }
                    '>' -> { appCursorKeys = false; state = State.NORMAL }
                    else -> state = State.NORMAL
                }
            }
            State.CSI -> {
                if (ch in '0'..'9' || ch == ';' || ch == '?' || ch == '>') {
                    csiParams.append(ch)
                } else {
                    executeCsi(ch)
                    state = State.NORMAL
                }
            }
            State.OSC -> {
                if (ch == '\u0007' || ch == '\u001B') {
                    state = State.NORMAL
                }
            }
            State.CHARSET -> {
                state = State.NORMAL
            }
        }
    }

    private fun lineFeed() {
        if (cursorY == scrollBottom) {
            scrollUpGrid()
        } else if (cursorY < rows - 1) {
            cursorY++
        }
    }

    private fun reverseIndex() {
        if (cursorY == scrollTop) {
            scrollDownGrid()
        } else if (cursorY > 0) {
            cursorY--
        }
    }

    private fun scrollUpGrid() {
        if (scrollTop == 0 && scrollBottom == rows - 1) {
            if (!inAltBuffer) {
                scrollback.add(grid[0].copyOf())
                if (scrollback.size > maxScrollback) {
                    scrollback.removeAt(0)
                }
            }
        }
        for (r in scrollTop until scrollBottom) {
            grid[r] = grid[r + 1]
        }
        grid[scrollBottom] = Array(cols) { TerminalChar() }
    }

    private fun scrollDownGrid() {
        for (r in scrollBottom downTo scrollTop + 1) {
            grid[r] = grid[r - 1]
        }
        grid[scrollTop] = Array(cols) { TerminalChar() }
    }

    private fun executeCsi(command: Char) {
        val raw = csiParams.toString()
        val isPrivate = raw.startsWith("?")
        val paramStr = if (isPrivate) raw.substring(1) else raw
        val parts = paramStr.split(';').mapNotNull { it.toIntOrNull() }

        when (command) {
            'A' -> { val n = parts.getOrElse(0) { 1 }.coerceAtLeast(1); cursorY = (cursorY - n).coerceAtLeast(scrollTop) }
            'B' -> { val n = parts.getOrElse(0) { 1 }.coerceAtLeast(1); cursorY = (cursorY + n).coerceAtMost(scrollBottom) }
            'C' -> { val n = parts.getOrElse(0) { 1 }.coerceAtLeast(1); cursorX = (cursorX + n).coerceAtMost(cols - 1) }
            'D' -> { val n = parts.getOrElse(0) { 1 }.coerceAtLeast(1); cursorX = (cursorX - n).coerceAtLeast(0) }
            'H', 'f' -> {
                val row = parts.getOrElse(0) { 1 } - 1
                val col = parts.getOrElse(1) { 1 } - 1
                cursorY = row.coerceIn(0, rows - 1)
                cursorX = col.coerceIn(0, cols - 1)
            }
            'J' -> {
                val mode = parts.getOrElse(0) { 0 }
                when (mode) {
                    0 -> {
                        for (c in cursorX until cols) grid[cursorY][c] = TerminalChar()
                        for (r in cursorY + 1 until rows) grid[r] = Array(cols) { TerminalChar() }
                    }
                    1 -> {
                        for (r in 0 until cursorY) grid[r] = Array(cols) { TerminalChar() }
                        for (c in 0..cursorX) grid[cursorY][c] = TerminalChar()
                    }
                    2, 3 -> {
                        for (r in 0 until rows) grid[r] = Array(cols) { TerminalChar() }
                        if (mode == 3) scrollback.clear()
                    }
                }
            }
            'K' -> {
                val mode = parts.getOrElse(0) { 0 }
                when (mode) {
                    0 -> for (c in cursorX until cols) grid[cursorY][c] = TerminalChar()
                    1 -> for (c in 0..cursorX) grid[cursorY][c] = TerminalChar()
                    2 -> grid[cursorY] = Array(cols) { TerminalChar() }
                }
            }
            'm' -> {
                if (parts.isEmpty()) resetSgr()
                else {
                    var idx = 0
                    while (idx < parts.size) {
                        when (val code = parts[idx]) {
                            0 -> resetSgr()
                            1 -> isBold = true
                            4 -> isUnderline = true
                            7 -> isReverse = true
                            22 -> isBold = false
                            24 -> isUnderline = false
                            27 -> isReverse = false
                            in 30..37 -> currentFg = ansiColors[code - 30]
                            39 -> currentFg = theme.defaultFg
                            in 40..47 -> currentBg = ansiColors[code - 40]
                            49 -> currentBg = Color.Transparent
                            in 90..97 -> currentFg = ansiColors[code - 90 + 8]
                            in 100..107 -> currentBg = ansiColors[code - 100 + 8]
                            38 -> {
                                if (idx + 2 < parts.size && parts[idx + 1] == 5) {
                                    val colorIdx = parts[idx + 2]
                                    currentFg = get256Color(colorIdx)
                                    idx += 2
                                } else if (idx + 4 < parts.size && parts[idx + 1] == 2) {
                                    currentFg = Color(parts[idx + 2], parts[idx + 3], parts[idx + 4])
                                    idx += 4
                                }
                            }
                            48 -> {
                                if (idx + 2 < parts.size && parts[idx + 1] == 5) {
                                    val colorIdx = parts[idx + 2]
                                    currentBg = get256Color(colorIdx)
                                    idx += 2
                                } else if (idx + 4 < parts.size && parts[idx + 1] == 2) {
                                    currentBg = Color(parts[idx + 2], parts[idx + 3], parts[idx + 4])
                                    idx += 4
                                }
                            }
                        }
                        idx++
                    }
                }
            }
            'r' -> {
                val top = parts.getOrElse(0) { 1 } - 1
                val bottom = parts.getOrElse(1) { rows } - 1
                scrollTop = top.coerceIn(0, rows - 1)
                scrollBottom = bottom.coerceIn(scrollTop, rows - 1)
                cursorX = 0
                cursorY = 0
            }
            'h' -> {
                if (isPrivate) {
                    when (parts.getOrElse(0) { 0 }) {
                        25 -> cursorVisible = true
                        1049 -> {
                            inAltBuffer = true
                            grid = altGrid
                            cursorX = 0; cursorY = 0
                        }
                    }
                }
            }
            'l' -> {
                if (isPrivate) {
                    when (parts.getOrElse(0) { 0 }) {
                        25 -> cursorVisible = false
                        1049 -> {
                            inAltBuffer = false
                            grid = primaryGrid
                        }
                    }
                }
            }
        }
    }

    private fun resetSgr() {
        currentFg = theme.defaultFg
        currentBg = Color.Transparent
        isBold = false
        isUnderline = false
        isReverse = false
    }

    private fun get256Color(index: Int): Color {
        if (index in 0..15) return ansiColors[index]
        if (index in 16..231) {
            val i = index - 16
            val r = (i / 36) * 51
            val g = ((i % 36) / 6) * 51
            val b = (i % 6) * 51
            return Color(r, g, b)
        }
        if (index in 232..255) {
            val v = (index - 232) * 10 + 8
            return Color(v, v, v)
        }
        return theme.defaultFg
    }

    fun getAllText(): String = buildString {
        for (row in scrollback) {
            appendLine(row.joinToString("") { it.ch.toString() }.trimEnd())
        }
        for (r in 0 until rows) {
            appendLine(grid[r].joinToString("") { it.ch.toString() }.trimEnd())
        }
    }

    fun getVisibleText(): String = buildString {
        for (r in 0 until rows) {
            appendLine(getRenderRow(r).joinToString("") { it.ch.toString() }.trimEnd())
        }
    }

    fun getSelectedText(startRow: Int, startCol: Int, endRow: Int, endCol: Int): String {
        return getVisibleText()
    }
}
