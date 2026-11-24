package com.devwithzachary.mineserve.util

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared dedicated-server mod filter that detects and disables client-only rendering/UI mods
 * (e.g. Sodium, Iris, ImmediatelyFast, EntityCulling, MoreCulling, Gnetum, Ixeris) and
 * byte-identical duplicate JARs in `mods/`, preventing dedicated server crashes such as
 * `NoClassDefFoundError: org/lwjgl/system/MathUtil`.
 */
object ServerModFilter {

    private val json = Json { ignoreUnknownKeys = true }

    private val KNOWN_CLIENT_ONLY_PATTERNS = listOf(
        "immediatelyfast",
        "sodium",
        "embeddium",
        "rubidium",
        "iris",
        "oculus",
        "entityculling",
        "moreculling",
        "asyncparticles",
        "particle_core",
        "particle-core",
        "betterblockentities",
        "bbe-neoforge",
        "bbe-fabric",
        "bbe-forge",
        "gnetum",
        "ixeris",
        "badoptimizations",
        "jasione",
        "indium",
        "continuity",
        "modmenu",
        "dynamicfps",
        "dynamic-fps",
        "zoomify",
        "lambdynamiclights",
        "3dskinlayers",
        "skinlayers3d",
        "entity_model_features",
        "entity_texture_features",
        "citresewn",
        "animatica",
        "fabrishot",
        "chat_heads",
        "chatheads",
        "mousewheelie",
        "controlling",
        "searchables",
        "betterf3",
        "presencefootsteps",
        "visuality",
        "wakes",
        "euphoria_patches",
        "polytone",
        "cubes-without-borders"
    )

    private val ENVIRONMENT_CLIENT_REGEX = Regex(""""environment"\s*:\s*"client"""", RegexOption.IGNORE_CASE)
    private val CLIENT_SIDE_ONLY_TOML_REGEX = Regex("""^\s*clientSideOnly\s*=\s*true\b""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    /**
     * Scans JAR files in `mods/` under [serverDir], renames any client-only or duplicate mods
     * to `.jar.disabled`, and returns the list of disabled `(fileName, reason)` pairs.
     */
    fun disableClientOnlyMods(serverDir: File): List<Pair<String, String>> {
        val modsDir = File(serverDir, "mods")
        if (!modsDir.exists() || !modsDir.isDirectory) return emptyList()
        val disabled = mutableListOf<Pair<String, String>>()

        val jars = modsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".jar", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?: emptyList()

        val seenHashes = mutableMapOf<String, String>()
        for (jar in jars) {
            val reason = detectClientOnlyReason(jar)
            if (reason != null) {
                val target = File(modsDir, "${jar.name}.disabled")
                if (target.exists()) target.delete()
                if (jar.renameTo(target)) {
                    disabled.add(jar.name to reason)
                }
                continue
            }

            val hash = sha256(jar)
            if (hash != null) {
                val existingName = seenHashes.putIfAbsent(hash, jar.name)
                if (existingName != null) {
                    val target = File(modsDir, "${jar.name}.disabled")
                    if (target.exists()) target.delete()
                    if (jar.renameTo(target)) {
                        disabled.add(jar.name to "duplicate of $existingName")
                    }
                }
            }
        }
        return disabled
    }

    fun detectClientOnlyReason(jarFile: File): String? {
        val lowerName = jarFile.name.lowercase()
        for (pattern in KNOWN_CLIENT_ONLY_PATTERNS) {
            if (lowerName.contains(pattern)) {
                return "known client-only rendering/UI mod '$pattern'"
            }
        }

        return try {
            ZipFile(jarFile).use { zip ->
                val fabricEntry = zip.getEntry("fabric.mod.json")
                if (fabricEntry != null) {
                    val text = zip.getInputStream(fabricEntry).bufferedReader(Charsets.UTF_8).readText()
                    if (ENVIRONMENT_CLIENT_REGEX.containsMatchIn(text)) {
                        return@use "fabric.mod.json declares environment=client"
                    }
                }

                val tomlEntry = zip.getEntry("META-INF/neoforge.mods.toml") ?: zip.getEntry("META-INF/mods.toml")
                if (tomlEntry != null) {
                    val toml = zip.getInputStream(tomlEntry).bufferedReader(Charsets.UTF_8).readText()
                    if (CLIENT_SIDE_ONLY_TOML_REGEX.containsMatchIn(toml)) {
                        return@use "mods.toml declares clientSideOnly=true"
                    }
                }

                val entries = zip.entries().asSequence().toList()
                for (entry in entries) {
                    if (!entry.isDirectory && entry.name.endsWith("MixinPlugin.class")) {
                        val ascii = String(zip.getInputStream(entry).readBytes(), Charsets.ISO_8859_1)
                        if (ascii.contains("org/lwjgl/")) {
                            return@use "${entry.name.substringAfterLast('/')} links directly against client-only LWJGL"
                        }
                    }
                }

                val mixinEntries = entries.filter {
                    !it.isDirectory && !it.name.contains('/') && it.name.endsWith(".mixins.json", ignoreCase = true)
                }
                for (mEntry in mixinEntries) {
                    val mObj = runCatching {
                        json.parseToJsonElement(zip.getInputStream(mEntry).bufferedReader(Charsets.UTF_8).readText()).jsonObject
                    }.getOrNull() ?: continue
                    val pluginClass = mObj["plugin"]?.jsonPrimitive?.content?.replace('.', '/')?.plus(".class").orEmpty()
                    if (pluginClass.isNotBlank() && pluginClass != ".class") {
                        val pEntry = zip.getEntry(pluginClass)
                        if (pEntry != null) {
                            val ascii = String(zip.getInputStream(pEntry).readBytes(), Charsets.ISO_8859_1)
                            if (ascii.contains("org/lwjgl/")) {
                                return@use "Mixin plugin $pluginClass references client-only LWJGL"
                            }
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sha256(file: File): String? = runCatching {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(16384)
            while (true) {
                val r = input.read(buf)
                if (r <= 0) break
                md.update(buf, 0, r)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
}
