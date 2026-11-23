package com.devwithzachary.mineserve.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

data class CoreLogEntry(
    val timestamp: Long,
    val level: String,
    val tag: String,
    val message: String
)

/**
 * Platform-agnostic logging facade for the `:core` module with an in-memory
 * diagnostic ring buffer and automatic secret/token redaction.
 */
object CoreLog : Logger {
    private const val MAX_BUFFER_LINES = 1500
    private val ringBuffer = ArrayDeque<CoreLogEntry>(MAX_BUFFER_LINES)
    private val bearerRegex = Regex("""(?i)(Bearer\s+)[A-Za-z0-9._~+/=-]+""")
    private val webhookRegex = Regex("""https://(?:ptb\.|canary\.)?discord(?:app)?\.com/api/webhooks/\d+/[A-Za-z0-9_-]+""")
    private val secretKeyRegex = Regex("""(?i)((?:token|secret|password|authorization)["'\s:=]+)([^\s"',}]+)""")

    @Volatile
    var sink: Logger = object : Logger {
        override fun d(tag: String, message: String) = println("D/$tag: $message")
        override fun i(tag: String, message: String) = println("I/$tag: $message")
        override fun w(tag: String, message: String, throwable: Throwable?) {
            println("W/$tag: $message${throwable?.let { " ($it)" } ?: ""}")
        }
        override fun e(tag: String, message: String, throwable: Throwable?) {
            System.err.println("E/$tag: $message${throwable?.let { " ($it)" } ?: ""}")
        }
    }

    private fun record(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) {
            "$message (${throwable.javaClass.simpleName}: ${throwable.message})"
        } else {
            message
        }
        synchronized(ringBuffer) {
            if (ringBuffer.size >= MAX_BUFFER_LINES) {
                ringBuffer.removeFirst()
            }
            ringBuffer.addLast(
                CoreLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    tag = tag,
                    message = fullMsg
                )
            )
        }
    }

    override fun d(tag: String, message: String) {
        record("D", tag, message)
        sink.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        record("I", tag, message)
        sink.i(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        record("W", tag, message, throwable)
        sink.w(tag, message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        record("E", tag, message, throwable)
        sink.e(tag, message, throwable)
    }

    fun d(tag: String, message: String, throwable: Throwable?): Int {
        record("D", tag, message, throwable)
        sink.d(tag, "$message${throwable?.let { " ($it)" } ?: ""}")
        return 0
    }

    fun w(tag: String, throwable: Throwable): Int {
        record("W", tag, throwable.message ?: "", throwable)
        sink.w(tag, throwable.message ?: "", throwable)
        return 0
    }

    fun redactSensitiveText(input: String): String {
        return input
            .replace(bearerRegex, "$1[REDACTED_TOKEN]")
            .replace(webhookRegex, "https://discord.com/api/webhooks/[REDACTED]")
            .replace(secretKeyRegex, "$1[REDACTED]")
    }

    fun getCapturedLogs(
        errorsOnly: Boolean = false,
        limit: Int = 600
    ): String {
        val entries = synchronized(ringBuffer) {
            ringBuffer.toList()
        }
        val filtered = if (errorsOnly) {
            entries.filter { it.level == "W" || it.level == "E" }
        } else {
            entries
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val slice = if (filtered.size > limit) filtered.takeLast(limit) else filtered
        if (slice.isEmpty()) {
            return if (errorsOnly) "(No warnings or errors recorded in this session)" else "(No log entries recorded yet)"
        }
        return slice.joinToString("\n") { e ->
            redactSensitiveText("${fmt.format(Date(e.timestamp))} ${e.level}/${e.tag}: ${e.message}")
        }
    }
}
