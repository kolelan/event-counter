package com.eventcounter.domain

import java.time.Instant

enum class EventStatus {
    NONE,
    NORMAL,
    YELLOW,
    RED,
}

enum class CounterDirection {
    ELAPSED,
    UNTIL_RED,
}

enum class CounterUnit {
    DAYS,
    HOURS,
    MINUTES,
    SECONDS,
    HOURS_MINUTES,
}

enum class ThresholdUnit {
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
}

enum class ViewMode {
    LIST,
    GRID,
}

data class Threshold(
    val amount: Long,
    val unit: ThresholdUnit,
)

data class CounterValue(
    val direction: CounterDirection,
    val text: String,
    val overdue: Boolean,
)

data class Group(
    val id: Long = 0,
    val parentGroupId: Long? = null,
    val name: String,
    val iconKey: String,
    val sortOrder: Int = 0,
)

data class Event(
    val id: Long = 0,
    val groupId: Long? = null,
    val title: String,
    val shortDescription: String = "",
    val fullDescription: String = "",
    val tag: String = "",
    val iconKey: String,
    val occurredAt: Instant,
    val isIndefinite: Boolean = false,
    val yellow: Threshold? = null,
    val red: Threshold? = null,
    val counterDirection: CounterDirection = CounterDirection.ELAPSED,
    val counterUnit: CounterUnit = CounterUnit.DAYS,
    val sortOrder: Int = 0,
)

data class EventImage(
    val id: Long = 0,
    val eventId: Long,
    val path: String,
    val sortOrder: Int = 0,
)

data class EventDetails(
    val event: Event,
    val images: List<EventImage>,
)

data class FolderSnapshot(
    val groups: List<Group>,
    val events: List<Event>,
)

data class AppSettings(
    val viewMode: ViewMode = ViewMode.LIST,
    val aiApiKey: String = "",
    val aiBaseUrl: String = "",
)

data class GroupChoice(
    val id: Long,
    val name: String,
    val depth: Int,
)

object IconKeys {
    const val EVENT = "event"
    const val HYGIENE = "hygiene"
    const val HEALTH = "health"
    const val CAR = "car"
    const val WATER = "water"
    const val HOME = "home"
    const val FOOD = "food"
    const val PET = "pet"
    const val PLANT = "plant"
    const val FINANCE = "finance"
    const val SPORT = "sport"
    const val FOLDER = "folder"

    fun suggest(tag: String): String {
        val value = tag.lowercase()
        return when {
            value.contains("гигиен") || value.contains("щет") || value.contains("hygiene") -> HYGIENE
            value.contains("здоров") || value.contains("health") -> HEALTH
            value.contains("авто") || value.contains("масл") || value.contains("car") -> CAR
            value.contains("вода") || value.contains("фильтр") || value.contains("water") -> WATER
            value.contains("дом") || value.contains("home") -> HOME
            value.contains("еда") || value.contains("питан") || value.contains("food") -> FOOD
            value.contains("питом") || value.contains("pet") -> PET
            value.contains("растен") || value.contains("plant") -> PLANT
            value.contains("финанс") || value.contains("finance") -> FINANCE
            value.contains("спорт") || value.contains("sport") -> SPORT
            else -> EVENT
        }
    }
}

fun parseThreshold(amount: String, unit: ThresholdUnit): Threshold? {
    val value = amount.trim().toLongOrNull() ?: return null
    if (value !in 1..9999) return null
    return Threshold(value, unit)
}
