package com.devwithzachary.mineserve.api

import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Centralized OkHttpClient singleton for MineServe.
 * Reuses a single connection pool and dispatcher across all API clients
 * to enable HTTP/2 connection multiplexing, reduce socket allocations,
 * and conserve memory.
 */
object MineServeHttpClient {
    const val USER_AGENT = "MineServe-Android (https://github.com/devwithzachary/mineserve)"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun newCall(request: Request): Call = client.newCall(request)

    fun newRequestBuilder(url: String): Request.Builder = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)

    fun newGetRequest(url: String): Request = newRequestBuilder(url).build()
}
