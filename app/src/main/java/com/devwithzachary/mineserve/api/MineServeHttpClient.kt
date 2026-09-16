package com.devwithzachary.mineserve.api

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Centralized OkHttpClient singleton for MineServe.
 * Reuses a single connection pool and dispatcher across all API clients
 * to enable HTTP/2 connection multiplexing, reduce socket allocations,
 * and conserve memory.
 */
object MineServeHttpClient {
    const val USER_AGENT = "MineServe-Android (https://github.com/devwithzachary/mineserve)"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val DEFAULT_FALLBACK_VERSIONS: List<String> = listOf(
        "1.21.4", "1.21.3", "1.21.1", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"
    )
}
