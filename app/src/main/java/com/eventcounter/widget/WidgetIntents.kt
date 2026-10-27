package com.eventcounter.widget

import android.content.Context
import android.content.Intent
import com.eventcounter.MainActivity
import com.eventcounter.ui.widget.WidgetConfigureActivity

object WidgetIntents {
    const val EXTRA_EVENT_ID = "com.eventcounter.eventId"

    fun openEvent(context: Context, eventId: Long): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    fun configure(context: Context, widgetId: Int): Intent {
        return Intent(context, WidgetConfigureActivity::class.java).apply {
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun eventId(intent: Intent?): Long? {
        if (intent == null || !intent.hasExtra(EXTRA_EVENT_ID)) return null
        val id = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        intent.removeExtra(EXTRA_EVENT_ID)
        return id.takeIf { it >= 0L }
    }
}

fun slotCountOf(providerClassName: String?): Int = when {
    providerClassName?.endsWith("ThreeEventWidgetReceiver") == true -> 3
    providerClassName?.endsWith("FiveEventWidgetReceiver") == true -> 5
    else -> 1
}
