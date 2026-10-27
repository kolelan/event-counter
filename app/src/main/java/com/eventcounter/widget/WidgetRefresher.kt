package com.eventcounter.widget

import androidx.glance.appwidget.updateAll

fun interface WidgetRefresher {
    suspend fun refresh()
}

class GlanceWidgetRefresher(
    private val context: android.content.Context,
) : WidgetRefresher {
    override suspend fun refresh() {
        EventGlanceWidget().updateAll(context)
    }
}
