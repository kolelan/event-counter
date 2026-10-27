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
): List<WidgetSlot> {
    val byId = events.associateBy { it.id }
    return eventIds.map { id ->
        val event = id?.let(byId::get)
        if (event == null || event.archivedAt != null) {
            emptyWidgetSlot()
        } else {
            val counter = StatusCalculator.counter(event, now, zone)
            WidgetSlot(
                eventId = event.id,
                title = event.title,
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

private fun emptyWidgetSlot() = WidgetSlot(
    eventId = null,
    title = WIDGET_EMPTY_TITLE,
    iconKey = IconKeys.EVENT,
    counterText = "",
    status = EventStatus.NONE,
    opensEvent = false,
)
