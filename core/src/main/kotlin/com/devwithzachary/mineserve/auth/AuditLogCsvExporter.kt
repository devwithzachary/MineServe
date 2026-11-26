package com.devwithzachary.mineserve.auth

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AuditLogCsvExporter {
    private val header = "Timestamp,Username,Role,Action,Target,Details,IP"

    fun toCsv(entries: List<AuditLogEntry>): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.appendLine(header)
        for (entry in entries) {
            val row = listOf(
                isoFormat.format(Date(entry.timestamp)),
                entry.username,
                entry.role,
                entry.action,
                entry.target,
                entry.details,
                entry.clientIp
            ).joinToString(",") { escapeCell(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }

    private fun escapeCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
