package com.eventcounter.domain

import java.time.Instant
import java.time.ZoneId

const val WIDGET_EMPTY_TITLE = "Выберите событие"

data class WidgetSlot(
    val eventId: Long?,
    val title: String,
    val iconKey: String,
    val counterText: String,
    val status: EventStatus,
    val opensEvent: Boolean,
)

fun buildWidgetSlots(
    eventIds: List<Long?>,
    events: List<Event>,
    now: Instant,
    zone: ZoneId,
    counterUnit: CounterUnit? = null,
    showTitle: Boolean = true,
    customTitles: List<String> = emptyList(),
): List<WidgetSlot> {
    val byId = events.associateBy { it.id }
    return eventIds.mapIndexed { index, id ->
        val event = id?.let(byId::get)
        if (event == null || event.archivedAt != null) {
            emptyWidgetSlot()
        } else {
            val shown = if (counterUnit == null) event else event.copy(counterUnit = counterUnit)
            val custom = customTitles.getOrNull(index).orEmpty()
            val counter = StatusCalculator.counter(shown, now, zone)
            WidgetSlot(
                eventId = event.id,
                title = widgetDisplayTitle(event.title, custom, showTitle),
                iconKey = event.iconKey,
                counterText = counter.text,
                status = StatusCalculator.status(event, now, zone),
                opensEvent = true,
            )
        }
    }
}

fun eventsForSlot(
    events: List<Event>,
    selectedIds: List<Long?>,
    slotIndex: Int,
): List<Event> {
    val taken = selectedIds.mapIndexedNotNull { index, id ->
        if (index == slotIndex) null else id
    }.toSet()
    return events
        .filter { it.archivedAt == null && it.id != 0L && it.id !in taken }
        .sortedWith(compareBy<Event> { it.title.lowercase() }.thenByDescending { it.occurredAt })
}

fun uniqueSlotIds(eventIds: List<Long?>): List<Long?> {
    val seen = mutableSetOf<Long>()
    return eventIds.map { id ->
        if (id != null && seen.add(id)) id else null
    }
}

fun widgetDisplayTitle(eventTitle: String, customTitle: String, showTitle: Boolean): String {
    if (!showTitle) return ""
    val custom = customTitle.trim()
    return custom.ifEmpty { eventTitle }
}

fun cellCounterLabel(counterText: String): String {
    val number = counterText.substringBefore(' ').trim()
    return number.ifBlank { counterText }
}

fun cellCounterTextSizeRatio(label: String): Float = when (label.length) {
    0, 1, 2 -> 0.58f
    3 -> 0.44f
    4 -> 0.34f
    else -> 0.26f
}

private fun emptyWidgetSlot() = WidgetSlot(
    eventId = null,
    title = WIDGET_EMPTY_TITLE,
    iconKey = IconKeys.EVENT,
    counterText = "",
    status = EventStatus.NONE,
    opensEvent = false,
)
