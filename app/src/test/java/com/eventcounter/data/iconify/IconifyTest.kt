package com.eventcounter.data.iconify

import com.eventcounter.domain.IconifyId
import com.eventcounter.domain.IconifyKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconifyKeysTest {
    @Test
    fun parse_acceptsIconifyKey() {
        assertEquals(IconifyId("mdi", "car"), IconifyKeys.parse("iconify:mdi:car"))
        assertEquals(IconifyId("fa6-solid", "oil-can"), IconifyKeys.parse("iconify:fa6-solid:oil-can"))
    }

    @Test
    fun parse_rejectsLocalAndUnsafeKeys() {
        assertNull(IconifyKeys.parse("car"))
        assertNull(IconifyKeys.parse("iconify:../:car"))
        assertNull(IconifyKeys.parse("iconify:mdi:car:extra"))
        assertNull(IconifyKeys.parse("iconify:MDI:car"))
    }
}

class IconifyJsonTest {
    @Test
    fun parseSearch_keepsPermissiveMonochromeIcons() {
        val json = """
            {
              "icons": ["mdi:oil", "fa-solid:oil-can", "logos:color", "nope"],
              "collections": {
                "mdi": {"license": {"spdx": "Apache-2.0"}, "palette": false},
                "fa-solid": {"license": {"spdx": "CC-BY-4.0"}, "palette": false},
                "logos": {"license": {"spdx": "MIT"}, "palette": true}
              }
            }
        """.trimIndent()
        assertEquals(listOf(IconifyHit("mdi", "oil")), IconifyJson.parseSearch(json))
    }

    @Test
    fun parseCollection_resolvesAlias() {
        val json = """
            {
              "prefix": "mdi",
              "width": 24,
              "height": 24,
              "icons": {"car": {"body": "<path d=\"M1\"/>"}},
              "aliases": {"auto": {"parent": "car"}}
            }
        """.trimIndent()
        val documents = IconifyJson.parseCollection(json)
        assertEquals(
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M1"/></svg>""",
            documents["auto"],
        )
    }
}

class IconifyRepositoryTest {
    @Test
    fun search_loadsOneCollectionPerPrefix() = runBlocking {
        val transport = FakeTransport(
            search = """
                {
                  "icons": ["mdi:oil", "ph:drop"],
                  "collections": {
                    "mdi": {"license": {"spdx": "Apache-2.0"}, "palette": false},
                    "ph": {"license": {"spdx": "MIT"}, "palette": false}
                  }
                }
            """.trimIndent(),
            collections = mapOf(
                "mdi" to """{"width":24,"height":24,"icons":{"oil":{"body":"<path d=\"M1\"/>"}}}""",
                "ph" to """{"width":24,"height":24,"icons":{"drop":{"body":"<path d=\"M2\"/>"}}}""",
            ),
        )
        val cache = MemoryCache()
        val repository = IconifyRepository(transport, cache, Dispatchers.Unconfined)
        val glyphs = repository.search("oil")
        assertEquals(listOf("iconify:mdi:oil", "iconify:ph:drop"), glyphs.map { it.key })
        assertEquals(listOf("search", "mdi.json", "ph.json"), transport.calls)
        repository.store(glyphs.first())
        assertTrue(cache.read(IconifyId("mdi", "oil"))!!.contains("<svg"))
    }

    @Test
    fun ensure_downloadsMissingSvgOnce() = runBlocking {
        val transport = FakeTransport(
            search = "{}",
            collections = emptyMap(),
            svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"></svg>""",
        )
        val repository = IconifyRepository(transport, MemoryCache(), Dispatchers.Unconfined)
        val first = repository.ensure("iconify:mdi:oil")
        val second = repository.ensure("iconify:mdi:oil")
        assertEquals(first, second)
        assertEquals(listOf("mdi/oil.svg"), transport.calls)
    }

    private class MemoryCache : IconifyCache {
        private val files = mutableMapOf<String, String>()
        override fun read(id: IconifyId): String? = files["${id.prefix}/${id.name}"]
        override fun write(id: IconifyId, svg: String) {
            files["${id.prefix}/${id.name}"] = svg
        }
    }

    private class FakeTransport(
        private val search: String,
        private val collections: Map<String, String>,
        private val svg: String = "",
    ) : IconifyTransport {
        val calls = mutableListOf<String>()
        override fun get(url: String): String {
            val call = when {
                url.contains("/search?") -> "search"
                url.endsWith(".svg") -> url.substringAfter("design/").substringBefore("?")
                else -> url.substringAfter("design/").substringBefore("?")
            }
            calls += call
            return when (call) {
                "search" -> search
                else -> collections[call.removeSuffix(".json")] ?: svg
            }
        }
    }
}
