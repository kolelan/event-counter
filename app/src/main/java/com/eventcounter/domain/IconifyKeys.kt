package com.eventcounter.domain

data class IconifyId(val prefix: String, val name: String)

object IconifyKeys {
    private const val MARKER = "iconify:"
    private val token = Regex("[a-z0-9]+(?:[-_][a-z0-9]+)*")

    fun parse(key: String): IconifyId? {
        val rest = key.removePrefix(MARKER)
        if (rest.length == key.length) return null
        val parts = rest.split(':', limit = 2)
        if (parts.size != 2) return null
        val prefix = parts[0]
        val name = parts[1]
        if (!isToken(prefix) || !isToken(name)) return null
        return IconifyId(prefix, name)
    }

    fun key(prefix: String, name: String): String? {
        val id = parse("$MARKER$prefix:$name") ?: return null
        return "$MARKER${id.prefix}:${id.name}"
    }

    fun isToken(value: String): Boolean = token.matches(value)
}
