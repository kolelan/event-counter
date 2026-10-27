package com.eventcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class StatusCalculatorTest {
    private val zone = ZoneOffset.UTC
    private val start = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun indefinite_hasNoStatus() {
        val event = event(indefinite = true, yellow = Threshold(60, ThresholdUnit.DAYS), red = Threshold(90, ThresholdUnit.DAYS))
        val later = Instant.parse("2026-06-01T00:00:00Z")
        assertEquals(EventStatus.NONE, StatusCalculator.status(event, later, zone))
    }

    @Test
    fun beforeYellow_isNormal() {
        val event = event(yellow = Threshold(2, ThresholdUnit.MONTHS), red = Threshold(3, ThresholdUnit.MONTHS))
        val now = Instant.parse("2026-01-15T00:00:00Z")
        assertEquals(EventStatus.NORMAL, StatusCalculator.status(event, now, zone))
    }

    @Test
    fun afterYellow_beforeRed_isYellow() {
        val event = event(yellow = Threshold(2, ThresholdUnit.MONTHS), red = Threshold(3, ThresholdUnit.MONTHS))
        val now = Instant.parse("2026-03-31T12:00:00Z")
        assertEquals(EventStatus.YELLOW, StatusCalculator.status(event, now, zone))
    }

    @Test
    fun threeMonths_turnsRedOnAprilFirst() {
        val event = event(yellow = Threshold(2, ThresholdUnit.MONTHS), red = Threshold(3, ThresholdUnit.MONTHS))
        val now = Instant.parse("2026-04-01T00:00:00Z")
        assertEquals(EventStatus.RED, StatusCalculator.status(event, now, zone))
    }

    @Test
    fun onlyRed_staysNormalUntilThreshold() {
        val event = event(red = Threshold(10, ThresholdUnit.DAYS))
        val now = start.plusSeconds(3 * 86_400)
        assertEquals(EventStatus.NORMAL, StatusCalculator.status(event, now, zone))
    }

    @Test
    fun yellowWithoutRed_neverTurnsRed() {
        val event = event(yellow = Threshold(1, ThresholdUnit.DAYS))
        val now = start.plusSeconds(5 * 86_400)
        assertEquals(EventStatus.YELLOW, StatusCalculator.status(event, now, zone))
    }

    @Test
    fun elapsedDays_floor() {
        val event = event(unit = CounterUnit.DAYS)
        val now = start.plusSeconds(2 * 86_400)
        val counter = StatusCalculator.counter(event, now, zone)
        assertEquals("2 дн.", counter.text)
        assertFalse(counter.overdue)
        assertEquals(CounterDirection.ELAPSED, counter.direction)
    }

    @Test
    fun futureEvent_elapsedIsZero() {
        val event = event()
        val now = start.minusSeconds(3_600)
        assertEquals("0 дн.", StatusCalculator.counter(event, now, zone).text)
    }

    @Test
    fun hoursAndMinutes() {
        val event = event(unit = CounterUnit.HOURS_MINUTES)
        val now = start.plusSeconds(2 * 3_600 + 5 * 60)
        assertEquals("2 ч 5 мин", StatusCalculator.counter(event, now, zone).text)
    }

    @Test
    fun secondsAndHoursFloor() {
        val seconds = event(unit = CounterUnit.SECONDS)
        assertEquals("90 с", StatusCalculator.counter(seconds, start.plusSeconds(90), zone).text)
        val hours = event(unit = CounterUnit.HOURS)
        assertEquals("1 ч", StatusCalculator.counter(hours, start.plusSeconds(90 * 60), zone).text)
    }

    @Test
    fun untilRed_remainingAndOverdue() {
        val event = event(
            red = Threshold(3, ThresholdUnit.DAYS),
            direction = CounterDirection.UNTIL_RED,
            unit = CounterUnit.DAYS,
        )
        val remaining = StatusCalculator.counter(event, start.plusSeconds(2 * 86_400), zone)
        assertEquals("1 дн.", remaining.text)
        assertFalse(remaining.overdue)

        val overdue = StatusCalculator.counter(event, start.plusSeconds(4 * 86_400), zone)
        assertEquals("−1 дн.", overdue.text)
        assertTrue(overdue.overdue)
    }

    @Test
    fun untilRed_withoutThreshold_isDash() {
        val indefinite = event(indefinite = true, direction = CounterDirection.UNTIL_RED)
        assertEquals("—", StatusCalculator.counter(indefinite, start.plusSeconds(86_400), zone).text)
        val noRed = event(direction = CounterDirection.UNTIL_RED)
        assertEquals("—", StatusCalculator.counter(noRed, start, zone).text)
    }

    @Test
    fun suggestIcon_fromTag() {
        assertEquals(IconKeys.HYGIENE, IconKeys.suggest("Гигиена"))
        assertEquals(IconKeys.CAR, IconKeys.suggest("замена масла"))
        assertEquals(IconKeys.WATER, IconKeys.suggest("фильтр воды"))
        assertEquals(IconKeys.EVENT, IconKeys.suggest("другое"))
    }

    private fun event(
        at: Instant = start,
        indefinite: Boolean = false,
        yellow: Threshold? = null,
        red: Threshold? = null,
        direction: CounterDirection = CounterDirection.ELAPSED,
        unit: CounterUnit = CounterUnit.DAYS,
    ) = Event(
        title = "Щётка",
        iconKey = IconKeys.HYGIENE,
        occurredAt = at,
        isIndefinite = indefinite,
        yellow = yellow,
        red = red,
        counterDirection = direction,
        counterUnit = unit,
    )
}
