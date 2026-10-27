package com.eventcounter.data.iconify

import com.eventcounter.domain.IconifyKeys
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class IconifyHit(val prefix: String, val name: String)

internal object IconifyJson {
    private val allowedLicenses = setOf(
        "Apache-2.0",
        "MIT",
        "ISC",
        "BSD-2-Clause",
        "BSD-3-Clause",
        "0BSD",
        "Unlicense",
    )

    fun parseSearch(json: String, limit: Int = 16): List<IconifyHit> {
        val root = JsonParser.parseString(json).asJsonObject
        val allowed = allowedPrefixes(root.getAsJsonObject("collections"))
        val icons = root.getAsJsonArray("icons") ?: return emptyList()
        val hits = ArrayList<IconifyHit>(limit)
        for (element in icons) {
            if (hits.size == limit) break
            val parts = element.asString.split(':', limit = 2)
            if (parts.size != 2) continue
            val prefix = parts[0]
            val name = parts[1]
            if (prefix !in allowed || IconifyKeys.key(prefix, name) == null) continue
            hits += IconifyHit(prefix, name)
        }
        return hits
    }

    fun parseCollection(json: String): Map<String, String> {
        val root = JsonParser.parseString(json).asJsonObject
        val defaultWidth = root.int("width", 16)
        val defaultHeight = root.int("height", 16)
        val bodies = LinkedHashMap<String, IconBody>()
        val icons = root.getAsJsonObject("icons")
        icons?.entrySet()?.forEach { (name, value) ->
            val item = value.asJsonObject
            val body = item.get("body")?.asString ?: return@forEach
            bodies[name] = IconBody(
                body = body,
                left = item.int("left", 0),
                top = item.int("top", 0),
                width = item.int("width", defaultWidth),
                height = item.int("height", defaultHeight),
            )
        }
        val aliases = LinkedHashMap<String, String>()
        root.getAsJsonObject("aliases")?.entrySet()?.forEach { (name, value) ->
            val parent = value.asJsonObject.get("parent")?.asString ?: return@forEach
            aliases[name] = parent
        }
        val documents = LinkedHashMap<String, String>()
        (bodies.keys + aliases.keys).forEach { name ->
            resolve(name, bodies, aliases)?.let { body ->
                documents[name] = body.toSvg()
            }
        }
        return documents
    }

    private fun allowedPrefixes(collections: JsonObject?): Set<String> {
        if (collections == null) return emptySet()
        return collections.entrySet().mapNotNull { (prefix, value) ->
            val item = value.asJsonObject
            val license = item.getAsJsonObject("license")?.get("spdx")?.asString
            val palette = item.get("palette")?.asBoolean ?: false
            if (!palette && license in allowedLicenses) prefix else null
        }.toSet()
    }

    private fun resolve(
        name: String,
        bodies: Map<String, IconBody>,
        aliases: Map<String, String>,
        seen: Set<String> = emptySet(),
    ): IconBody? {
        if (name in seen) return null
        bodies[name]?.let { return it }
        val parent = aliases[name] ?: return null
        return resolve(parent, bodies, aliases, seen + name)
    }

    private fun JsonObject.int(name: String, fallback: Int): Int {
        val value = get(name) ?: return fallback
        return if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) value.asInt else fallback
    }
}

private data class IconBody(
    val body: String,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
) {
    fun toSvg(): String {
        val boxWidth = width.coerceAtLeast(1)
        val boxHeight = height.coerceAtLeast(1)
        return """<svg xmlns="http://www.w3.org/2000/svg" viewBox="$left $top $boxWidth $boxHeight">$body</svg>"""
    }
}
