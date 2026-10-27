package com.eventcounter.widget

import android.appwidget.AppWidgetManager
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
import androidx.glance.layout.ContentScale
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
import com.eventcounter.domain.CounterUnit
import com.eventcounter.domain.EventStatus
import com.eventcounter.domain.WidgetSlot
import com.eventcounter.domain.buildWidgetSlots
import com.eventcounter.domain.cellCounterLabel
import com.eventcounter.ui.icons.AppIcons
import java.time.Instant
import java.time.ZoneId

class EventGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)?.provider?.className
        val cell = isOneCell(provider)
        val store = WidgetStore(context)
        val selection = store.read(widgetId)
        val digitStyle = store.readDigitStyle(widgetId)
        val showTitle = store.readShowTitle(widgetId, defaultVisible = !cell)
        val customTitles = store.readCustomTitles(widgetId, selection.eventIds.size)
        val container = (context.applicationContext as EventCounterApp).container
        val events = container.repository.getEvents(selection.eventIds.filterNotNull())
        val slots = buildWidgetSlots(
            eventIds = selection.eventIds,
            events = events,
            now = Instant.now(),
            zone = ZoneId.systemDefault(),
            counterUnit = if (cell) CounterUnit.DAYS else null,
            showTitle = showTitle,
            customTitles = customTitles,
        )
        provideContent {
            GlanceTheme {
                if (cell) {
                    OneCell(
                        context = context,
                        widgetId = widgetId,
                        slot = slots.first(),
                        digitStyle = digitStyle,
                    )
                } else {
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
}

@androidx.compose.runtime.Composable
private fun SlotRow(context: Context, widgetId: Int, slot: WidgetSlot) {
    val color = slotColor(slot.status)
    val icon = remember(slot.iconKey) { AppIcons.byKey(slot.iconKey).image.raster() }
    val action = slotAction(context, widgetId, slot)
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
        if (slot.title.isNotBlank()) {
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
        } else {
            Spacer(GlanceModifier.defaultWeight())
        }
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

@androidx.compose.runtime.Composable
private fun OneCell(context: Context, widgetId: Int, slot: WidgetSlot, digitStyle: DigitStyle) {
    val color = slotColor(slot.status)
    val icon = remember(slot.iconKey) { AppIcons.byKey(slot.iconKey).image.raster(sizePx = 288) }
    val label = cellCounterLabel(slot.counterText)
    val number = remember(label, slot.title, slot.status, digitStyle) {
        if (slot.opensEvent && label.isNotBlank()) {
            cellCounterBitmap(label, slot.status, digitStyle, title = slot.title)
        } else {
            null
        }
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(cellBackground(slot.status))
            .cornerRadius(16.dp)
            .clickable(slotAction(context, widgetId, slot)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = AppIcons.byKey(slot.iconKey).label,
            modifier = GlanceModifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(color),
        )
        if (number != null) {
            Image(
                provider = ImageProvider(number),
                contentDescription = label,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(
                text = "Выберите",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

private fun slotAction(context: Context, widgetId: Int, slot: WidgetSlot) =
    if (slot.opensEvent && slot.eventId != null) {
        actionStartActivity(WidgetIntents.openEvent(context, slot.eventId))
    } else {
        actionStartActivity(WidgetIntents.configure(context, widgetId))
    }

private fun slotColor(status: EventStatus): ColorProvider = when (status) {
    EventStatus.YELLOW -> dayNightColor(day = Color(0xFF8A5A00), night = Color(0xFFFFD56A))
    EventStatus.RED -> dayNightColor(day = Color(0xFFBA1A1A), night = Color(0xFFFFB4AB))
    EventStatus.NORMAL -> dayNightColor(day = Color(0xFF1F6F5B), night = Color(0xFF9AD4C0))
    EventStatus.NONE -> dayNightColor(day = Color(0xFF3E4944), night = Color(0xFFC0C9C3))
}

private fun cellBackground(status: EventStatus): ColorProvider = when (status) {
    EventStatus.YELLOW -> dayNightColor(day = Color(0xFFFFF1CC), night = Color(0xFF3F3416))
    EventStatus.RED -> dayNightColor(day = Color(0xFFFDE8E6), night = Color(0xFF3F1D1D))
    EventStatus.NORMAL -> dayNightColor(day = Color(0xFFFBFCFB), night = Color(0xFF171A19))
    EventStatus.NONE -> dayNightColor(day = Color(0xFFFBFCFB), night = Color(0xFF171A19))
}

class OneEventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        deleteSlots(context, appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }
}

class OneCellWidgetReceiver : GlanceAppWidgetReceiver() {
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
