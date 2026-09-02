package com.devwithzachary.mineserve.ui.components.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle

object SyntaxHighlighter {

    val ColorKey = Color(0xFF67E8F9)        // Cyan 300
    val ColorString = Color(0xFF86EFAC)     // Emerald 300
    val ColorNumberBool = Color(0xFFFDE047) // Yellow 300
    val ColorComment = Color(0xFF64748B)    // Slate 500
    val ColorHeader = Color(0xFFC084FC)     // Purple 400
    val ColorDelimiter = Color(0xFF94A3B8)  // Slate 400
    val ColorDefault = Color(0xFFE2E8F0)    // Slate 200

    fun highlight(text: String, extension: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        return when (extension.lowercase()) {
            "yml", "yaml" -> highlightYaml(text)
            "json", "mcmeta" -> highlightJson(text)
            "properties", "cfg", "conf", "ini" -> highlightProperties(text)
            "toml" -> highlightToml(text)
            else -> highlightGeneric(text)
        }
    }

    private fun highlightYaml(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val lines = text.lines()
            var currentOffset = 0

            for (line in lines) {
                val trimmed = line.trimStart()
                val lineStartOffset = currentOffset

                if (trimmed.startsWith("#")) {
                    // Full line comment
                    addStyle(
                        SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic),
                        lineStartOffset,
                        lineStartOffset + line.length
                    )
                } else {
                    // Check for inline comment
                    val hashIndex = line.indexOf('#')
                    val activeLength = if (hashIndex >= 0) hashIndex else line.length

                    if (hashIndex >= 0) {
                        addStyle(
                            SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic),
                            lineStartOffset + hashIndex,
                            lineStartOffset + line.length
                        )
                    }

                    // Key-Value parsing
                    val colonIndex = line.indexOf(':')
                    if (colonIndex in 0 until activeLength) {
                        // Key
                        val keyStart = line.indexOfFirst { !it.isWhitespace() && it != '-' }
                        if (keyStart in 0..colonIndex) {
                            addStyle(
                                SpanStyle(color = ColorKey),
                                lineStartOffset + keyStart,
                                lineStartOffset + colonIndex
                            )
                        }

                        // Colon
                        addStyle(
                            SpanStyle(color = ColorDelimiter),
                            lineStartOffset + colonIndex,
                            lineStartOffset + colonIndex + 1
                        )

                        // Value
                        val valStart = colonIndex + 1
                        if (valStart < activeLength) {
                            val valueText = line.substring(valStart, activeLength).trim()
                            if (valueText.isNotBlank()) {
                                val actualValIndex = line.indexOf(valueText, valStart)
                                if (actualValIndex >= 0) {
                                    val valStyle = when {
                                        valueText.equals("true", ignoreCase = true) ||
                                                valueText.equals("false", ignoreCase = true) ||
                                                valueText.toDoubleOrNull() != null -> SpanStyle(color = ColorNumberBool)
                                        valueText.startsWith("\"") || valueText.startsWith("'") -> SpanStyle(color = ColorString)
                                        else -> SpanStyle(color = ColorString)
                                    }
                                    addStyle(
                                        valStyle,
                                        lineStartOffset + actualValIndex,
                                        lineStartOffset + actualValIndex + valueText.length
                                    )
                                }
                            }
                        }
                    } else if (trimmed.startsWith("-")) {
                        // List item bullet
                        val bulletIndex = line.indexOf('-')
                        if (bulletIndex >= 0) {
                            addStyle(
                                SpanStyle(color = ColorDelimiter),
                                lineStartOffset + bulletIndex,
                                lineStartOffset + bulletIndex + 1
                            )
                        }
                    }
                }

                currentOffset += line.length + 1
            }
        }
    }

    private fun highlightJson(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)

            // Keys: "key":
            val keyRegex = Regex("\"([^\"]+)\"\\s*:")
            keyRegex.findAll(text).forEach { match ->
                val range = match.groups[1]?.range
                if (range != null) {
                    addStyle(SpanStyle(color = ColorKey), range.first, range.last + 1)
                }
            }

            // String values: : "value"
            val stringValRegex = Regex(":\\s*\"([^\"]*)\"")
            stringValRegex.findAll(text).forEach { match ->
                val range = match.groups[1]?.range
                if (range != null) {
                    addStyle(SpanStyle(color = ColorString), range.first, range.last + 1)
                }
            }

            // Numbers, Booleans, Null: :\s*(-?\d+(\.\d+)?|true|false|null)
            val numBoolRegex = Regex(":\\s*(-?\\d+(\\.\\d+)?|true|false|null)")
            numBoolRegex.findAll(text).forEach { match ->
                val range = match.groups[1]?.range
                if (range != null) {
                    addStyle(SpanStyle(color = ColorNumberBool), range.first, range.last + 1)
                }
            }

            // Structural brackets
            val bracketRegex = Regex("[{}\\[\\],]")
            bracketRegex.findAll(text).forEach { match ->
                addStyle(SpanStyle(color = ColorDelimiter), match.range.first, match.range.last + 1)
            }
        }
    }

    private fun highlightProperties(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val lines = text.lines()
            var currentOffset = 0

            for (line in lines) {
                val trimmed = line.trimStart()
                val lineStartOffset = currentOffset

                if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    addStyle(
                        SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic),
                        lineStartOffset,
                        lineStartOffset + line.length
                    )
                } else {
                    val eqIndex = line.indexOfAny(charArrayOf('=', ':'))
                    if (eqIndex >= 0) {
                        // Key
                        val keyStart = line.indexOfFirst { !it.isWhitespace() }
                        if (keyStart in 0..eqIndex) {
                            addStyle(
                                SpanStyle(color = ColorKey),
                                lineStartOffset + keyStart,
                                lineStartOffset + eqIndex
                            )
                        }

                        // Equal sign
                        addStyle(
                            SpanStyle(color = ColorDelimiter),
                            lineStartOffset + eqIndex,
                            lineStartOffset + eqIndex + 1
                        )

                        // Value
                        val valStart = eqIndex + 1
                        if (valStart < line.length) {
                            val valueText = line.substring(valStart).trim()
                            val actualValIndex = line.indexOf(valueText, valStart)
                            if (actualValIndex >= 0 && valueText.isNotBlank()) {
                                val valStyle = when {
                                    valueText.equals("true", ignoreCase = true) ||
                                            valueText.equals("false", ignoreCase = true) ||
                                            valueText.toDoubleOrNull() != null -> SpanStyle(color = ColorNumberBool)
                                    else -> SpanStyle(color = ColorString)
                                }
                                addStyle(
                                    valStyle,
                                    lineStartOffset + actualValIndex,
                                    lineStartOffset + actualValIndex + valueText.length
                                )
                            }
                        }
                    }
                }
                currentOffset += line.length + 1
            }
        }
    }

    private fun highlightToml(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            val lines = text.lines()
            var currentOffset = 0

            for (line in lines) {
                val trimmed = line.trimStart()
                val lineStartOffset = currentOffset

                if (trimmed.startsWith("#")) {
                    addStyle(
                        SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic),
                        lineStartOffset,
                        lineStartOffset + line.length
                    )
                } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    // Table header [section]
                    addStyle(
                        SpanStyle(color = ColorHeader),
                        lineStartOffset,
                        lineStartOffset + line.length
                    )
                } else {
                    val eqIndex = line.indexOf('=')
                    if (eqIndex >= 0) {
                        val keyStart = line.indexOfFirst { !it.isWhitespace() }
                        if (keyStart in 0..eqIndex) {
                            addStyle(
                                SpanStyle(color = ColorKey),
                                lineStartOffset + keyStart,
                                lineStartOffset + eqIndex
                            )
                        }
                        addStyle(
                            SpanStyle(color = ColorDelimiter),
                            lineStartOffset + eqIndex,
                            lineStartOffset + eqIndex + 1
                        )
                        val valStart = eqIndex + 1
                        if (valStart < line.length) {
                            val valueText = line.substring(valStart).trim()
                            val actualValIndex = line.indexOf(valueText, valStart)
                            if (actualValIndex >= 0 && valueText.isNotBlank()) {
                                val valStyle = when {
                                    valueText.equals("true", ignoreCase = true) ||
                                            valueText.equals("false", ignoreCase = true) ||
                                            valueText.toDoubleOrNull() != null -> SpanStyle(color = ColorNumberBool)
                                    else -> SpanStyle(color = ColorString)
                                }
                                addStyle(
                                    valStyle,
                                    lineStartOffset + actualValIndex,
                                    lineStartOffset + actualValIndex + valueText.length
                                )
                            }
                        }
                    }
                }
                currentOffset += line.length + 1
            }
        }
    }

    private fun highlightGeneric(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(color = ColorDefault), 0, text.length)
        }
    }
}
