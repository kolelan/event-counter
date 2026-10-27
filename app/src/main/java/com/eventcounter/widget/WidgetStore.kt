package com.eventcounter.widget

import android.content.Context

enum class DigitStyle {
    SHADOW,
    PLATE,
}

data class WidgetSelection(
    val slotCount: Int,
    val eventIds: List<Long?>,
)

class WidgetStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(widgetId: Int): WidgetSelection {
        val count = prefs.getInt(countKey(widgetId), 0)
        val ids = decode(prefs.getString(slotsKey(widgetId), null))
        if (count <= 0) {
            val fallback = if (ids.isEmpty()) listOf(null) else ids
            return WidgetSelection(fallback.size, fallback)
        }
        return WidgetSelection(count, List(count) { index -> ids.getOrNull(index) })
    }

    fun write(widgetId: Int, slotCount: Int, eventIds: List<Long?>) {
        val normalized = List(slotCount) { index -> eventIds.getOrNull(index) }
        prefs.edit()
            .putInt(countKey(widgetId), slotCount)
            .putString(slotsKey(widgetId), normalized.joinToString(",") { it?.toString().orEmpty() })
            .apply()
    }

    fun readDigitStyle(widgetId: Int): DigitStyle {
        return when (prefs.getString(styleKey(widgetId), null)) {
            DigitStyle.PLATE.name -> DigitStyle.PLATE
            else -> DigitStyle.SHADOW
        }
    }

    fun writeDigitStyle(widgetId: Int, style: DigitStyle) {
        prefs.edit().putString(styleKey(widgetId), style.name).apply()
    }

    fun delete(widgetId: Int) {
        prefs.edit()
            .remove(countKey(widgetId))
            .remove(slotsKey(widgetId))
            .remove(styleKey(widgetId))
            .apply()
    }

    private fun decode(raw: String?): List<Long?> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split(",").map { token -> token.toLongOrNull() }
    }

    private fun countKey(widgetId: Int) = "count_$widgetId"

    private fun slotsKey(widgetId: Int) = "slots_$widgetId"

    private fun styleKey(widgetId: Int) = "style_$widgetId"

    private companion object {
        const val FILE = "widget-slots"
    }
}
