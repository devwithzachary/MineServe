package com.devwithzachary.mineserve.net.discord

import android.util.Log
import com.devwithzachary.mineserve.model.DiscordWebhookConfig
import com.devwithzachary.mineserve.model.MinecraftServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Pure-OkHttp Discord Webhook event bridge for server lifecycle, player, and backup events.
 */
object DiscordWebhookClient {
    private const val TAG = "DiscordWebhookClient"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val VALID_WEBHOOK_PREFIXES = listOf(
        "https://discord.com/api/webhooks/",
        "https://discordapp.com/api/webhooks/",
        "https://canary.discord.com/api/webhooks/",
        "https://ptb.discord.com/api/webhooks/"
    )

    fun isValidWebhookUrl(url: String): Boolean {
        val trimmed = url.trim()
        return VALID_WEBHOOK_PREFIXES.any { trimmed.startsWith(it, ignoreCase = true) } &&
            trimmed.length > "https://discord.com/api/webhooks/".length
    }

    suspend fun sendTestMessage(webhookUrl: String, serverName: String): Result<Unit> {
        val cleanUrl = webhookUrl.trim()
        if (!isValidWebhookUrl(cleanUrl)) {
            return Result.failure(IllegalArgumentException("Enter a valid Discord webhook URL (https://discord.com/api/webhooks/...)."))
        }
        val payload = buildEmbedPayload(
            title = "MineServe Webhook Connected",
            description = "Test notification from **$serverName**. Server lifecycle, player, and backup events will appear here.",
            color = 0x10B981,
            serverName = serverName
        )
        return postPayload(cleanUrl, payload)
    }

    fun notifyServerStart(server: MinecraftServer) {
        val cfg = server.discordWebhookConfig
        if (!canSend(cfg) || !cfg.notifyServerStart) return
        val payload = buildEmbedPayload(
            title = "Server Online",
            description = "**${server.name}** is now online and accepting players.",
            color = 0x10B981,
            serverName = server.name,
            fields = listOf(
                "Software" to "${server.type.displayName} ${server.version}",
                "Port" to server.port.toString()
            )
        )
        dispatchAsync(cfg.webhookUrl, payload)
    }

    fun notifyServerStop(server: MinecraftServer) {
        val cfg = server.discordWebhookConfig
        if (!canSend(cfg) || !cfg.notifyServerStop) return
        val payload = buildEmbedPayload(
            title = "Server Offline",
            description = "**${server.name}** has shut down cleanly.",
            color = 0x64748B,
            serverName = server.name
        )
        dispatchAsync(cfg.webhookUrl, payload)
    }

    fun notifyServerCrash(server: MinecraftServer, detail: String? = null) {
        val cfg = server.discordWebhookConfig
        if (!canSend(cfg) || !cfg.notifyServerCrash) return
        val extra = if (!detail.isNullOrBlank()) "\n\nDetail: `$detail`" else ""
        val payload = buildEmbedPayload(
            title = "Server Crash Detected",
            description = "**${server.name}** stopped unexpectedly.$extra",
            color = 0xEF4444,
            serverName = server.name
        )
        dispatchAsync(cfg.webhookUrl, payload)
    }

    fun notifyPlayerJoin(server: MinecraftServer, playerName: String, onlineCount: Int) {
        val cfg = server.discordWebhookConfig
        if (!canSend(cfg) || !cfg.notifyPlayerJoin) return
        val encoded = runCatching { URLEncoder.encode(playerName, "UTF-8") }.getOrDefault(playerName)
        val thumb = if (cfg.includePlayerSkins) "https://mc-heads.net/avatar/$encoded/64" else null
        val payload = buildEmbedPayload(
            title = "Player Joined",
            description = "**$playerName** joined **${server.name}** ($onlineCount online).",
            color = 0x38BDF8,
            serverName = server.name,
            thumbnailUrl = thumb
        )
        dispatchAsync(cfg.webhookUrl, payload)
    }

    fun notifyPlayerLeave(server: MinecraftServer, playerName: String, onlineCount: Int) {
        val cfg = server.discordWebhookConfig
        if (!canSend(cfg) || !cfg.notifyPlayerLeave) return
        val encoded = runCatching { URLEncoder.encode(playerName, "UTF-8") }.getOrDefault(playerName)
        val thumb = if (cfg.includePlayerSkins) "https://mc-heads.net/avatar/$encoded/64" else null
        val payload = buildEmbedPayload(
            title = "Player Left",
            description = "**$playerName** left **${server.name}** ($onlineCount online).",
            color = 0xF59E0B,
            serverName = server.name,
            thumbnailUrl = thumb
        )
        dispatchAsync(cfg.webhookUrl, payload)
    }

    fun notifyBackupCompleted(server: MinecraftServer, backupKind: String, fileName: String? = null) {
        val cfg = server.discordWebhookConfig
        if (!canSend(cfg) || !cfg.notifyBackupComplete) return
        val fields = if (!fileName.isNullOrBlank()) listOf("Archive" to fileName) else emptyList()
        val payload = buildEmbedPayload(
            title = "$backupKind Completed",
            description = "A **$backupKind** snapshot finished for **${server.name}**.",
            color = 0x8B5CF6,
            serverName = server.name,
            fields = fields
        )
        dispatchAsync(cfg.webhookUrl, payload)
    }

    private fun canSend(cfg: DiscordWebhookConfig): Boolean =
        cfg.enabled && isValidWebhookUrl(cfg.webhookUrl)

    private fun dispatchAsync(webhookUrl: String, payload: String) {
        val cleanUrl = webhookUrl.trim()
        scope.launch {
            postPayload(cleanUrl, payload).onFailure { err ->
                Log.w(TAG, "Discord webhook delivery failed: ${err.message}")
            }
        }
    }

    private suspend fun postPayload(webhookUrl: String, payload: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = payload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .header("User-Agent", "MineServe-Webhook/1.6.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errText = response.body?.string()?.take(200) ?: ""
                    throw IllegalStateException("Discord returned HTTP ${response.code}: $errText")
                }
            }
        }
    }

    private fun buildEmbedPayload(
        title: String,
        description: String,
        color: Int,
        serverName: String,
        thumbnailUrl: String? = null,
        fields: List<Pair<String, String>> = emptyList()
    ): String {
        val embed = JSONObject()
            .put("title", title)
            .put("description", description)
            .put("color", color)
            .put("timestamp", isoUtcNow())
            .put("footer", JSONObject().put("text", "MineServe | $serverName"))

        if (!thumbnailUrl.isNullOrBlank()) {
            embed.put("thumbnail", JSONObject().put("url", thumbnailUrl))
        }
        if (fields.isNotEmpty()) {
            val fieldsArr = JSONArray()
            for ((name, value) in fields) {
                fieldsArr.put(
                    JSONObject()
                        .put("name", name)
                        .put("value", value)
                        .put("inline", true)
                )
            }
            embed.put("fields", fieldsArr)
        }

        return JSONObject()
            .put("username", "MineServe")
            .put("embeds", JSONArray().put(embed))
            .toString()
    }

    private fun isoUtcNow(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}

