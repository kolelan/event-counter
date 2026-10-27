package com.eventcounter.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as dayNightColor
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.action.clickable
import com.eventcounter.EventCounterApp
import com.eventcounter.domain.EventStatus
import com.eventcounter.domain.WidgetSlot
import com.eventcounter.domain.buildWidgetSlots
import com.eventcounter.ui.icons.AppIcons
import java.time.Instant
import java.time.ZoneId

class EventGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val selection = WidgetStore(context).read(widgetId)
        val container = (context.applicationContext as EventCounterApp).container
        val events = container.repository.getEvents(selection.eventIds.filterNotNull())
        val slots = buildWidgetSlots(
            eventIds = selection.eventIds,
            events = events,
            now = Instant.now(),
            zone = ZoneId.systemDefault(),
        )
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(8.dp),
                ) {
                    slots.forEachIndexed { index, slot ->
                        if (index > 0) {
                            Spacer(GlanceModifier.height(6.dp))
                        }
                        SlotRow(context = context, widgetId = widgetId, slot = slot)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SlotRow(context: Context, widgetId: Int, slot: WidgetSlot) {
    val color = slotColor(slot.status)
    val icon = remember(slot.iconKey) { AppIcons.byKey(slot.iconKey).image.raster() }
    val action = if (slot.opensEvent && slot.eventId != null) {
        actionStartActivity(WidgetIntents.openEvent(context, slot.eventId))
    } else {
        actionStartActivity(WidgetIntents.configure(context, widgetId))
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(action)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(28.dp)
                .cornerRadius(2.dp)
                .background(color),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Image(
            provider = ImageProvider(icon),
            contentDescription = AppIcons.byKey(slot.iconKey).label,
            modifier = GlanceModifier.size(20.dp),
            colorFilter = ColorFilter.tint(color),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = slot.title,
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (slot.counterText.isNotBlank()) {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = slot.counterText,
                maxLines = 1,
                style = TextStyle(
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

private fun slotColor(status: EventStatus): ColorProvider = when (status) {
    EventStatus.YELLOW -> dayNightColor(day = Color(0xFF8A5A00), night = Color(0xFFFFD56A))
    EventStatus.RED -> dayNightColor(day = Color(0xFFBA1A1A), night = Color(0xFFFFB4AB))
    EventStatus.NORMAL -> dayNightColor(day = Color(0xFF1F6F5B), night = Color(0xFF9AD4C0))
    EventStatus.NONE -> dayNightColor(day = Color(0xFF3E4944), night = Color(0xFFC0C9C3))
}

class OneEventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        deleteSlots(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class ThreeEventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        deleteSlots(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class FiveEventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        deleteSlots(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

private fun deleteSlots(context: Context, appWidgetIds: IntArray) {
    val store = WidgetStore(context)
    appWidgetIds.forEach(store::delete)
}
