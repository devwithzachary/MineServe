package com.devwithzachary.mineserve.engine

import com.devwithzachary.mineserve.model.CrashDiagnosticReport
import com.devwithzachary.mineserve.model.CrashIssueType
import com.devwithzachary.mineserve.model.CrashSeverity
import com.devwithzachary.mineserve.model.QuickFixAction
import com.devwithzachary.mineserve.model.QuickFixType
import java.io.File

object CrashLogAnalyzer {

    fun analyzeServer(serverDir: File): CrashDiagnosticReport? {
        if (!serverDir.exists()) return null

        // 1. Check crash-reports folder first for latest crash report
        val crashReportsDir = File(serverDir, "crash-reports")
        if (crashReportsDir.exists() && crashReportsDir.isDirectory) {
            val latestReport = crashReportsDir.listFiles()
                ?.filter { it.isFile && (it.name.startsWith("crash-") || it.name.endsWith(".txt")) }
                ?.maxByOrNull { it.lastModified() }

            if (latestReport != null && latestReport.length() > 0) {
                try {
                    val content = latestReport.readText()
                    val parsed = parseLogContent(content, sourceFile = "crash-reports/${latestReport.name}")
                    if (parsed != null) return parsed
                } catch (_: Exception) {}
            }
        }

        // 2. Check logs/latest.log
        val latestLog = File(serverDir, "logs/latest.log")
        if (latestLog.exists() && latestLog.isFile && latestLog.length() > 0) {
            try {
                val content = latestLog.readText()
                val parsed = parseLogContent(content, sourceFile = "logs/latest.log")
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }

        // 3. Fallback: check logs/debug.log
        val debugLog = File(serverDir, "logs/debug.log")
        if (debugLog.exists() && debugLog.isFile && debugLog.length() > 0) {
            try {
                val content = debugLog.readText()
                return parseLogContent(content, sourceFile = "logs/debug.log")
            } catch (_: Exception) {}
        }

        return null
    }

    fun parseLogContent(content: String, sourceFile: String = "Console Log"): CrashDiagnosticReport? {
        if (content.isBlank()) return null

        val lines = content.lines()
        val tailLines = if (lines.size > 200) lines.takeLast(200) else lines
        val fullSnippet = tailLines.joinToString("\n")

        // 1. Incompatible Java Version (UnsupportedClassVersionError)
        val javaVerRegex = Regex("UnsupportedClassVersionError: .* has been compiled by a more recent version of the Java Runtime \\(class file version (\\d+\\.\\d+)\\), this version of the Java Runtime only recognizes class file versions up to (\\d+\\.\\d+)", RegexOption.IGNORE_CASE)
        val javaMatch = javaVerRegex.find(content)
        if (javaMatch != null || content.contains("UnsupportedClassVersionError", ignoreCase = true)) {
            val reqClassVer = javaMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 65.0
            val reqJava = when {
                reqClassVer >= 69.0 -> "25"
                reqClassVer >= 65.0 -> "21"
                reqClassVer >= 61.0 -> "17"
                else -> "17"
            }
            val snippet = extractSnippetAround(lines, "UnsupportedClassVersionError")
            return CrashDiagnosticReport(
                title = "Incompatible Java Version",
                severity = CrashSeverity.CRITICAL,
                issueType = CrashIssueType.INCOMPATIBLE_JAVA_VERSION,
                summary = "The Minecraft engine or one of its mods requires Java $reqJava, but the server is running an older Java runtime.",
                explanation = "Java class files compiled for newer Minecraft versions require OpenJDK $reqJava or newer. MineServe can automatically switch this server to Java $reqJava.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "Switch to Java $reqJava",
                        description = "Change the server Java runtime to Java $reqJava",
                        actionType = QuickFixType.CHANGE_JAVA_VERSION,
                        payload = reqJava
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        // 2. Out of Memory (OOM)
        if (content.contains("OutOfMemoryError", ignoreCase = true) ||
            content.contains("insufficient memory for the Java Runtime", ignoreCase = true) ||
            content.contains("Java heap space", ignoreCase = true)
        ) {
            val snippet = extractSnippetAround(lines, "OutOfMemoryError", "Java heap space", "insufficient memory")
            return CrashDiagnosticReport(
                title = "Out of Memory (OOM)",
                severity = CrashSeverity.CRITICAL,
                issueType = CrashIssueType.OUT_OF_MEMORY,
                summary = "The server ran out of allocated RAM heap memory and was terminated by the JVM.",
                explanation = "Minecraft servers with mods, plugins, or large view distances need more RAM. Increasing RAM allocation prevents garbage collection freezes and crashes.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "Increase RAM to 3072 MB",
                        description = "Allocate more RAM to the server process",
                        actionType = QuickFixType.INCREASE_RAM,
                        payload = "3072"
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        // 3. EULA Rejection
        if (content.contains("You need to agree to the EULA", ignoreCase = true) ||
            content.contains("eula.txt", ignoreCase = true) && content.contains("false", ignoreCase = true)
        ) {
            val snippet = extractSnippetAround(lines, "agree to the EULA", "eula.txt")
            return CrashDiagnosticReport(
                title = "Minecraft EULA Not Accepted",
                severity = CrashSeverity.CRITICAL,
                issueType = CrashIssueType.EULA_NOT_ACCEPTED,
                summary = "The Minecraft server stopped because the Mojang End User License Agreement (EULA) is not accepted.",
                explanation = "Mojang requires server owners to agree to the EULA before starting. MineServe can set eula=true in eula.txt for you.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "Accept EULA (eula=true)",
                        description = "Automatically accept the Mojang EULA in eula.txt",
                        actionType = QuickFixType.ACCEPT_EULA,
                        payload = "eula.txt"
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        // 4. Port In Use / BindException
        if (content.contains("BindException", ignoreCase = true) ||
            content.contains("FAILED TO BIND TO PORT", ignoreCase = true) ||
            content.contains("Address already in use", ignoreCase = true) ||
            content.contains("Perhaps a server is already running on that port", ignoreCase = true)
        ) {
            val snippet = extractSnippetAround(lines, "BindException", "FAILED TO BIND TO PORT", "Address already in use")
            return CrashDiagnosticReport(
                title = "Port Already in Use",
                severity = CrashSeverity.CRITICAL,
                issueType = CrashIssueType.PORT_ALREADY_IN_USE,
                summary = "The configured server port is already occupied by another running server or system process.",
                explanation = "Two servers cannot listen on the same port at the same time. Assigning an unused port resolves the conflict immediately.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "Auto Assign Open Port",
                        description = "Assign the next available port number",
                        actionType = QuickFixType.CHANGE_PORT,
                        payload = ""
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        // 5. Mod ID Conflict / Duplicate Mods / Missing Dependency
        if (content.contains("DuplicateModsFoundException", ignoreCase = true) ||
            content.contains("ModResolutionException", ignoreCase = true) ||
            content.contains("IncompatibleModException", ignoreCase = true) ||
            content.contains("Some of your mods are incompatible", ignoreCase = true) ||
            content.contains("requires version", ignoreCase = true) && content.contains("mod", ignoreCase = true)
        ) {
            val snippet = extractSnippetAround(lines, "DuplicateModsFoundException", "ModResolutionException", "incompatible", "requires version")
            return CrashDiagnosticReport(
                title = "Mod Conflict or Missing Dependency",
                severity = CrashSeverity.CRITICAL,
                issueType = CrashIssueType.MOD_CONFLICT_OR_MISSING_DEP,
                summary = "Fabric/Forge detected incompatible, duplicate, or missing required mod dependencies.",
                explanation = "Check your mods directory for duplicate version jars or missing library dependencies required by your installed mods.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "Open Mods Folder",
                        description = "Browse and manage files in the mods folder",
                        actionType = QuickFixType.OPEN_FILE_EDITOR,
                        payload = "mods"
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        // 6. Corrupted Region / Chunk
        if (content.contains("Corrupted region file", ignoreCase = true) ||
            content.contains("Chunk coordinate", ignoreCase = true) && content.contains("out of bounds", ignoreCase = true) ||
            content.contains("Wrong location!", ignoreCase = true)
        ) {
            val snippet = extractSnippetAround(lines, "Corrupted region file", "out of bounds", "Wrong location!")
            return CrashDiagnosticReport(
                title = "Corrupted World Region or Chunk",
                severity = CrashSeverity.WARNING,
                issueType = CrashIssueType.CORRUPTED_WORLD_CHUNK,
                summary = "The Minecraft world loader detected corrupted world chunk data or region coordinate errors.",
                explanation = "Restoring a recent backup or inspecting the world region files in the world folder can recover the corrupt data.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "Open World Folder",
                        description = "Explore world data and backups",
                        actionType = QuickFixType.OPEN_FILE_EDITOR,
                        payload = "world"
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        // 7. Config / YAML Syntax Error
        if (content.contains("ScannerException", ignoreCase = true) ||
            content.contains("ParserException", ignoreCase = true) ||
            content.contains("Cannot load configuration from stream", ignoreCase = true) ||
            content.contains("Invalid YAML", ignoreCase = true)
        ) {
            val snippet = extractSnippetAround(lines, "ScannerException", "ParserException", "Cannot load configuration", "Invalid YAML")
            return CrashDiagnosticReport(
                title = "Configuration Syntax Error",
                severity = CrashSeverity.WARNING,
                issueType = CrashIssueType.SYNTAX_ERROR_CONFIG,
                summary = "A plugin or server configuration file contains invalid YAML, JSON, or formatting syntax.",
                explanation = "Review the syntax error in the configuration file using the built-in syntax-highlighted editor to fix misplaced indentation or quotes.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "Open File Explorer",
                        description = "Locate and edit the configuration file",
                        actionType = QuickFixType.OPEN_FILE_EDITOR,
                        payload = ""
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        // 8. General Fatal Error / Stack Trace
        if (content.contains("Exception in thread", ignoreCase = true) ||
            content.contains("FATAL", ignoreCase = true) ||
            content.contains("CRITICAL", ignoreCase = true)
        ) {
            val snippet = extractSnippetAround(lines, "Exception in thread", "FATAL", "CRITICAL")
            return CrashDiagnosticReport(
                title = "Server Runtime Exception",
                severity = CrashSeverity.WARNING,
                issueType = CrashIssueType.UNKNOWN_CRASH,
                summary = "The Minecraft server encountered an unhandled exception during startup or execution.",
                explanation = "Inspect the log output below to identify the failing component, mod, or plugin.",
                suggestedFixes = listOf(
                    QuickFixAction(
                        label = "View Full Latest Log",
                        description = "Open logs/latest.log in Code Editor",
                        actionType = QuickFixType.OPEN_FILE_EDITOR,
                        payload = "logs/latest.log"
                    )
                ),
                logSnippet = snippet.ifBlank { fullSnippet.takeLast(1000) },
                sourceFile = sourceFile
            )
        }

        return null
    }

    private fun extractSnippetAround(lines: List<String>, vararg keywords: String): String {
        val matchingIndices = lines.mapIndexedNotNull { index, line ->
            if (keywords.any { line.contains(it, ignoreCase = true) }) index else null
        }
        if (matchingIndices.isEmpty()) return ""

        val firstMatch = matchingIndices.first()
        val start = (firstMatch - 5).coerceAtLeast(0)
        val end = (matchingIndices.last() + 15).coerceAtMost(lines.size)
        return lines.subList(start, end).joinToString("\n")
    }
}
