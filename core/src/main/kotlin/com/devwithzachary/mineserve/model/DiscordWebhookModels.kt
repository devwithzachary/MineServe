package com.devwithzachary.mineserve.model

import kotlinx.serialization.Serializable

/**
 * Per-server Discord webhook bridge configuration.
 */
@Serializable
data class DiscordWebhookConfig(
    val enabled: Boolean = false,
    val webhookUrl: String = "",
    val notifyServerStart: Boolean = true,
    val notifyServerStop: Boolean = true,
    val notifyServerCrash: Boolean = true,
    val notifyPlayerJoin: Boolean = true,
    val notifyPlayerLeave: Boolean = true,
    val notifyBackupComplete: Boolean = true,
    val includePlayerSkins: Boolean = true
)

