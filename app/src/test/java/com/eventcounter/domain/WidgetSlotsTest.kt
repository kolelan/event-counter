package com.eventcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class WidgetSlotsTest {
    private val zone = ZoneOffset.UTC
    private val start = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun activeEvent_showsTitleCounterAndStatus() {
        val event = event(id = 7, title = "Масло")
        val slots = buildWidgetSlots(
            eventIds = listOf(7L),
            events = listOf(event),
            now = Instant.parse("2026-01-11T00:00:00Z"),
            zone = zone,
        )
        assertEquals("Масло", slots.single().title)
        assertEquals("10 дн.", slots.single().counterText)
        assertEquals(EventStatus.NORMAL, slots.single().status)
        assertTrue(slots.single().opensEvent)
    }

    @Test
    fun yellowEvent_keepsCounterAndStatus() {
        val event = event(
            id = 4,
            title = "Фильтр",
            yellow = Threshold(5, ThresholdUnit.DAYS),
            red = Threshold(20, ThresholdUnit.DAYS),
        )
        val slot = buildWidgetSlots(
            eventIds = listOf(4L),
            events = listOf(event),
            now = Instant.parse("2026-01-10T00:00:00Z"),
            zone = zone,
        ).single()
        assertEquals("9 дн.", slot.counterText)
        assertEquals(EventStatus.YELLOW, slot.status)
    }

    @Test
    fun cellCounter_showsDaysEvenWhenEventUsesHours() {
        val event = event(
            id = 8,
            title = "Масло",
            yellow = Threshold(5, ThresholdUnit.DAYS),
            red = Threshold(20, ThresholdUnit.DAYS),
        ).copy(counterUnit = CounterUnit.HOURS)
        val slot = buildWidgetSlots(
            eventIds = listOf(8L),
            events = listOf(event),
            now = Instant.parse("2026-01-11T00:00:00Z"),
            zone = zone,
            counterUnit = CounterUnit.DAYS,
        ).single()
        assertEquals("10 дн.", slot.counterText)
        assertEquals(EventStatus.YELLOW, slot.status)
    }

    @Test
    fun cellLabel_keepsNumberAndShrinksLongValues() {
        assertEquals("355", cellCounterLabel("355 дн."))
        assertEquals("−12", cellCounterLabel("−12 дн."))
        assertTrue(cellCounterTextSizeRatio("355") < cellCounterTextSizeRatio("9"))
        assertTrue(cellCounterTextSizeRatio("−355") < cellCounterTextSizeRatio("355"))
    }

    @Test
    fun archivedMissingAndEmpty_askToChoose() {
        val archived = event(id = 3, title = "Масло", archivedAt = Instant.parse("2026-02-01T00:00:00Z"))
        val slots = buildWidgetSlots(
            eventIds = listOf(3L, null, 99L),
            events = listOf(archived),
            now = Instant.parse("2026-03-01T00:00:00Z"),
            zone = zone,
        )
        assertEquals(3, slots.size)
        assertTrue(slots.all { it.title == WIDGET_EMPTY_TITLE && !it.opensEvent && it.counterText.isEmpty() })
    }

    @Test
    fun slotChoices_skipTakenAndArchived() {
        val events = listOf(
            event(id = 1, title = "Фильтр"),
            event(id = 2, title = "Масло"),
            event(id = 3, title = "Щётка"),
            event(id = 4, title = "Скрытое", archivedAt = start),
        )
        val choices = eventsForSlot(events, selectedIds = listOf(2L, null, 1L), slotIndex = 1)
        assertEquals(listOf("Щётка"), choices.map { it.title })
    }

    @Test
    fun uniqueSlotIds_dropsRepeat() {
        assertEquals(listOf(1L, null, 2L), uniqueSlotIds(listOf(1L, 1L, 2L)))
    }

    private fun event(
        id: Long,
        title: String,
        yellow: Threshold? = Threshold(60, ThresholdUnit.DAYS),
        red: Threshold? = Threshold(90, ThresholdUnit.DAYS),
        archivedAt: Instant? = null,
    ) = Event(
        id = id,
        title = title,
        iconKey = IconKeys.CAR,
        occurredAt = start,
        yellow = yellow,
        red = red,
        archivedAt = archivedAt,
    )
}
