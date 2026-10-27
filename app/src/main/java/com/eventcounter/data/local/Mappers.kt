package com.eventcounter.data.local

import com.eventcounter.domain.CounterDirection
import com.eventcounter.domain.CounterUnit
import com.eventcounter.domain.Event
import com.eventcounter.domain.EventImage
import com.eventcounter.domain.Group
import com.eventcounter.domain.Threshold
import com.eventcounter.domain.ThresholdUnit
import java.time.Instant

fun GroupEntity.toDomain(): Group = Group(
    id = id,
    parentGroupId = parentGroupId,
    name = name,
    iconKey = iconKey,
    sortOrder = sortOrder,
)

fun Group.toEntity(): GroupEntity = GroupEntity(
    id = id,
    parentGroupId = parentGroupId,
    name = name,
    iconKey = iconKey,
    sortOrder = sortOrder,
)

fun EventEntity.toDomain(): Event = Event(
    id = id,
    groupId = groupId,
    title = title,
    shortDescription = shortDescription,
    fullDescription = fullDescription,
    tag = tag,
    iconKey = iconKey,
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMillis),
    isIndefinite = isIndefinite,
    yellow = threshold(yellowAmount, yellowUnit),
    red = threshold(redAmount, redUnit),
    counterDirection = CounterDirection.valueOf(counterDirection),
    counterUnit = CounterUnit.valueOf(counterUnit),
    sortOrder = sortOrder,
)

fun Event.toEntity(): EventEntity = EventEntity(
    id = id,
    groupId = groupId,
    title = title,
    shortDescription = shortDescription,
    fullDescription = fullDescription,
    tag = tag,
    iconKey = iconKey,
    occurredAtEpochMillis = occurredAt.toEpochMilli(),
    isIndefinite = isIndefinite,
    yellowAmount = yellow?.amount,
    yellowUnit = yellow?.unit?.name,
    redAmount = red?.amount,
    redUnit = red?.unit?.name,
    counterDirection = counterDirection.name,
    counterUnit = counterUnit.name,
    sortOrder = sortOrder,
)

fun EventImageEntity.toDomain(): EventImage = EventImage(
    id = id,
    eventId = eventId,
    path = path,
    sortOrder = sortOrder,
)

private fun threshold(amount: Long?, unit: String?): Threshold? {
    if (amount == null || unit == null || amount <= 0L) return null
    val parsed = runCatching { ThresholdUnit.valueOf(unit) }.getOrNull() ?: return null
    return Threshold(amount, parsed)
}
