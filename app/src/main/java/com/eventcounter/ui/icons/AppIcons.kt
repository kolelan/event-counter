package com.eventcounter.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.ui.graphics.vector.ImageVector
import com.eventcounter.domain.IconKeys

data class AppIcon(
    val key: String,
    val label: String,
    val image: ImageVector,
)

object AppIcons {
    val all: List<AppIcon> = listOf(
        AppIcon(IconKeys.HYGIENE, "Гигиена", Icons.Outlined.CleaningServices),
        AppIcon(IconKeys.HEALTH, "Здоровье", Icons.Outlined.HealthAndSafety),
        AppIcon(IconKeys.WATER, "Вода", Icons.Outlined.WaterDrop),
        AppIcon(IconKeys.CAR, "Авто", Icons.Outlined.DirectionsCar),
        AppIcon(IconKeys.HOME, "Дом", Icons.Outlined.Home),
        AppIcon(IconKeys.FOOD, "Еда", Icons.Outlined.Restaurant),
        AppIcon(IconKeys.PET, "Питомцы", Icons.Outlined.Pets),
        AppIcon(IconKeys.PLANT, "Растения", Icons.Outlined.Yard),
        AppIcon(IconKeys.FINANCE, "Финансы", Icons.Outlined.Payments),
        AppIcon(IconKeys.SPORT, "Спорт", Icons.Outlined.FitnessCenter),
        AppIcon(IconKeys.EVENT, "Событие", Icons.Outlined.Event),
        AppIcon(IconKeys.FOLDER, "Группа", Icons.Outlined.Folder),
    )

    fun byKey(key: String): AppIcon = all.firstOrNull { it.key == key } ?: all.first { it.key == IconKeys.EVENT }
}
