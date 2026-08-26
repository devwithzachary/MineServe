package com.devwithzachary.mineserve.model

import kotlinx.serialization.Serializable

@Serializable
enum class ServerType(
    val displayName: String,
    val description: String,
    val defaultJavaVersion: Int,
    val supportsPlugins: Boolean,
    val supportsMods: Boolean
) {
    PAPER(
        displayName = "PaperMC",
        description = "High-performance Minecraft server fork designed for plugins and speed",
        defaultJavaVersion = 21,
        supportsPlugins = true,
        supportsMods = false
    ),
    PURPUR(
        displayName = "Purpur",
        description = "Drop-in replacement for Paper with extensive gameplay customization",
        defaultJavaVersion = 21,
        supportsPlugins = true,
        supportsMods = false
    ),
    FOLIA(
        displayName = "Folia",
        description = "Regionized multithreading server software for high player counts",
        defaultJavaVersion = 21,
        supportsPlugins = true,
        supportsMods = false
    ),
    VANILLA(
        displayName = "Vanilla",
        description = "Official Mojang Minecraft server software (Standard)",
        defaultJavaVersion = 21,
        supportsPlugins = false,
        supportsMods = false
    ),
    FABRIC(
        displayName = "Fabric",
        description = "Lightweight, modular modding toolchain & server",
        defaultJavaVersion = 21,
        supportsPlugins = false,
        supportsMods = true
    ),
    NEOFORGE(
        displayName = "NeoForge",
        description = "Community-driven modern modding API and server",
        defaultJavaVersion = 21,
        supportsPlugins = false,
        supportsMods = true
    ),
    BEDROCK_GEYSER(
        displayName = "Paper + Geyser (Cross-Play)",
        description = "Java Paper server with GeyserMC to allow Mobile/Console/PC cross-play",
        defaultJavaVersion = 21,
        supportsPlugins = true,
        supportsMods = false
    ),
    CUSTOM(
        displayName = "Custom JAR",
        description = "Provide your own custom server.jar executable",
        defaultJavaVersion = 21,
        supportsPlugins = true,
        supportsMods = true
    )
}

@Serializable
enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}
