package com.eventcounter.data.iconify

import com.eventcounter.domain.IconifyId
import com.eventcounter.domain.IconifyKeys
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class IconifyGlyph(
    val key: String,
    val svg: String,
)

internal fun interface IconifyTransport {
    fun get(url: String): String
}

internal interface IconifyCache {
    fun read(id: IconifyId): String?
    fun write(id: IconifyId, svg: String)
}

internal class HttpIconifyTransport : IconifyTransport {
    override fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "*/*")
            setRequestProperty("User-Agent", "EventCounter")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { reader ->
                val buffer = CharArray(8_192)
                val out = StringBuilder()
                while (out.length < MAX_BYTES) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, MAX_BYTES - out.length))
                    if (count < 0) break
                    out.append(buffer, 0, count)
                }
                out.toString()
            }.orEmpty()
            if (code !in 200..299) error("Iconify HTTP $code")
            return text
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MAX_BYTES = 256 * 1024
    }
}

internal class FileIconifyCache(private val directory: java.io.File) : IconifyCache {
    constructor(context: android.content.Context) : this(java.io.File(context.filesDir, "iconify"))

    override fun read(id: IconifyId): String? {
        val file = file(id)
        if (!file.isFile) return null
        val text = file.readText()
        return text.takeIf { it.isSvg() }
    }

    override fun write(id: IconifyId, svg: String) {
        if (!svg.isSvg()) return
        val file = file(id)
        file.parentFile?.mkdirs()
        file.writeText(svg)
    }

    private fun file(id: IconifyId): java.io.File {
        require(IconifyKeys.isToken(id.prefix) && IconifyKeys.isToken(id.name))
        return java.io.File(directory, "${id.prefix}--${id.name}.svg")
    }
}

class IconifyRepository internal constructor(
    private val transport: IconifyTransport,
    private val cache: IconifyCache,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val gate = Mutex()

    suspend fun search(query: String): List<IconifyGlyph> = withContext(dispatcher) {
        val trimmed = query.trim().take(40)
        if (trimmed.length < 2) return@withContext emptyList()
        val hits = IconifyJson.parseSearch(transport.get(searchUrl(trimmed)))
        val documents = LinkedHashMap<String, String>()
        hits.groupBy { it.prefix }.forEach { (prefix, group) ->
            val names = group.map { it.name }.distinct().joinToString(",")
            val body = runCatching { transport.get(collectionUrl(prefix, names)) }.getOrNull() ?: return@forEach
            val parsed = runCatching { IconifyJson.parseCollection(body) }.getOrDefault(emptyMap())
            parsed.forEach { (name, svg) ->
                val key = IconifyKeys.key(prefix, name) ?: return@forEach
                documents[key] = svg
            }
        }
        val glyphs = hits.mapNotNull { hit ->
            val key = IconifyKeys.key(hit.prefix, hit.name) ?: return@mapNotNull null
            documents[key]?.let { IconifyGlyph(key, it) }
        }
        if (hits.isNotEmpty() && glyphs.isEmpty()) error("Iconify icons unavailable")
        glyphs
    }

    fun cached(key: String): String? {
        val id = IconifyKeys.parse(key) ?: return null
        return cache.read(id)
    }

    suspend fun store(glyph: IconifyGlyph) = withContext(dispatcher) {
        val id = IconifyKeys.parse(glyph.key) ?: return@withContext
        if (!glyph.svg.isSvg()) return@withContext
        gate.withLock { cache.write(id, glyph.svg) }
    }

    suspend fun ensure(key: String): String? = withContext(dispatcher) {
        val id = IconifyKeys.parse(key) ?: return@withContext null
        cache.read(id)?.let { return@withContext it }
        gate.withLock {
            cache.read(id)?.let { return@withLock it }
            val svg = runCatching { transport.get(svgUrl(id)) }.getOrNull()?.takeIf { it.isSvg() }
                ?: return@withLock null
            cache.write(id, svg)
            svg
        }
    }

    private fun searchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        return "$ROOT/search?query=$encoded&limit=32"
    }

    private fun collectionUrl(prefix: String, names: String): String = "$ROOT/$prefix.json?icons=$names"

    private fun svgUrl(id: IconifyId): String = "$ROOT/${id.prefix}/${id.name}.svg"

    private companion object {
        const val ROOT = "https://api.iconify.design"
    }
}

private fun String.isSvg(): Boolean = contains("<svg", ignoreCase = true) && length <= 256 * 1024
