package com.eventcounter.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.eventcounter.domain.CounterDirection
import com.eventcounter.domain.CounterUnit
import com.eventcounter.domain.CounterValue
import com.eventcounter.domain.EventStatus
import com.eventcounter.domain.ThresholdUnit
import com.eventcounter.ui.icons.AppIcons

@Composable
fun statusColor(status: EventStatus): Color {
    val dark = isSystemInDarkTheme()
    return when (status) {
        EventStatus.YELLOW -> if (dark) Color(0xFFFFD56A) else Color(0xFF8A5A00)
        EventStatus.RED -> MaterialTheme.colorScheme.error
        EventStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
        EventStatus.NORMAL -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun eventContainerColor(status: EventStatus): Color {
    val dark = isSystemInDarkTheme()
    return when (status) {
        EventStatus.YELLOW -> if (dark) Color(0xFF3F3416) else Color(0xFFFFF1CC)
        EventStatus.RED -> if (dark) Color(0xFF3F1D1D) else Color(0xFFFDE8E6)
        EventStatus.NONE -> MaterialTheme.colorScheme.surface
        EventStatus.NORMAL -> MaterialTheme.colorScheme.surface
    }
}

fun EventStatus.label(): String = when (this) {
    EventStatus.NONE -> "Без срока"
    EventStatus.NORMAL -> "В норме"
    EventStatus.YELLOW -> "Скоро срок"
    EventStatus.RED -> "Срок вышел"
}

fun ThresholdUnit.shortLabel(): String = when (this) {
    ThresholdUnit.HOURS -> "часы"
    ThresholdUnit.DAYS -> "дни"
    ThresholdUnit.WEEKS -> "недели"
    ThresholdUnit.MONTHS -> "месяцы"
}

fun CounterUnit.shortLabel(): String = when (this) {
    CounterUnit.DAYS -> "дни"
    CounterUnit.HOURS -> "часы"
    CounterUnit.MINUTES -> "минуты"
    CounterUnit.SECONDS -> "секунды"
    CounterUnit.HOURS_MINUTES -> "часы и минуты"
}

@Composable
fun AppIconGlyph(
    iconKey: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val icon = AppIcons.byKey(iconKey)
    Icon(
        imageVector = icon.image,
        contentDescription = icon.label,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun CounterLabel(
    counter: CounterValue,
    status: EventStatus,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val color = if (counter.overdue) MaterialTheme.colorScheme.error else statusColor(status)
    val stopwatch = counter.direction == CounterDirection.ELAPSED
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (stopwatch) Icons.Outlined.Timer else Icons.Outlined.HourglassBottom,
            contentDescription = if (stopwatch) "Секундомер" else "Таймер",
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = counter.text,
            color = color,
            style = style.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}
