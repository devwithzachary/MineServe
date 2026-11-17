package com.devwithzachary.mineserve.core

interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Platform-agnostic logging facade for the `:core` module.
 * On Android, [sink] is bound to `android.util.Log` at app startup.
 */
object CoreLog : Logger {
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

    override fun d(tag: String, message: String) = sink.d(tag, message)
    override fun i(tag: String, message: String) = sink.i(tag, message)
    override fun w(tag: String, message: String, throwable: Throwable?) = sink.w(tag, message, throwable)
    override fun e(tag: String, message: String, throwable: Throwable?) = sink.e(tag, message, throwable)

    fun d(tag: String, message: String, throwable: Throwable?): Int {
        sink.d(tag, "$message${throwable?.let { " ($it)" } ?: ""}")
        return 0
    }

    fun w(tag: String, throwable: Throwable): Int {
        sink.w(tag, throwable.message ?: "", throwable)
        return 0
    }
}

