package com.eventcounter.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

object StatusCalculator {
    fun status(event: Event, now: Instant, zone: ZoneId): EventStatus {
        if (event.isIndefinite) return EventStatus.NONE
        val redAt = event.red?.let { deadline(event.occurredAt, it, zone) }
        val yellowAt = event.yellow?.let { deadline(event.occurredAt, it, zone) }
        if (redAt != null && !now.isBefore(redAt)) return EventStatus.RED
        if (yellowAt != null && !now.isBefore(yellowAt)) return EventStatus.YELLOW
        return EventStatus.NORMAL
    }

    fun counter(event: Event, now: Instant, zone: ZoneId): CounterValue {
        return when (event.counterDirection) {
            CounterDirection.ELAPSED -> {
                val elapsed = Duration.between(event.occurredAt, now)
                val safe = if (elapsed.isNegative) Duration.ZERO else elapsed
                CounterValue(
                    direction = CounterDirection.ELAPSED,
                    text = format(safe, event.counterUnit),
                    overdue = false,
                )
            }

            CounterDirection.UNTIL_RED -> {
                val redAt = if (event.isIndefinite) {
                    null
                } else {
                    event.red?.let { deadline(event.occurredAt, it, zone) }
                }
                if (redAt == null) {
                    CounterValue(CounterDirection.UNTIL_RED, "—", overdue = false)
                } else {
                    val remaining = Duration.between(now, redAt)
                    if (remaining.isNegative) {
                        CounterValue(
                            direction = CounterDirection.UNTIL_RED,
                            text = "−${format(remaining.abs(), event.counterUnit)}",
                            overdue = true,
                        )
                    } else {
                        CounterValue(
                            direction = CounterDirection.UNTIL_RED,
                            text = format(remaining, event.counterUnit),
                            overdue = remaining.isZero,
                        )
                    }
                }
            }
        }
    }

    fun deadline(start: Instant, threshold: Threshold, zone: ZoneId): Instant {
        val zoned = start.atZone(zone)
        val end = when (threshold.unit) {
            ThresholdUnit.HOURS -> zoned.plusHours(threshold.amount)
            ThresholdUnit.DAYS -> zoned.plusDays(threshold.amount)
            ThresholdUnit.WEEKS -> zoned.plusWeeks(threshold.amount)
            ThresholdUnit.MONTHS -> zoned.plusMonths(threshold.amount)
        }
        return end.toInstant()
    }

    fun isYellowBeforeRed(
        start: Instant,
        yellow: Threshold,
        red: Threshold,
        zone: ZoneId,
    ): Boolean = deadline(start, yellow, zone).isBefore(deadline(start, red, zone))

    fun format(duration: Duration, unit: CounterUnit): String {
        val seconds = duration.seconds.coerceAtLeast(0)
        return when (unit) {
            CounterUnit.DAYS -> "${seconds / SECONDS_PER_DAY} дн."
            CounterUnit.HOURS -> "${seconds / SECONDS_PER_HOUR} ч"
            CounterUnit.MINUTES -> "${seconds / SECONDS_PER_MINUTE} мин"
            CounterUnit.SECONDS -> "$seconds с"
            CounterUnit.HOURS_MINUTES -> {
                val hours = seconds / SECONDS_PER_HOUR
                val minutes = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
                "$hours ч $minutes мин"
            }
        }
    }

    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val SECONDS_PER_DAY = 86_400L
}
