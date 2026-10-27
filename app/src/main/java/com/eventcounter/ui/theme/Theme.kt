package com.eventcounter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F5B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE8DC),
    onPrimaryContainer = Color(0xFF062019),
    secondary = Color(0xFF4E6358),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F6F4),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFFBFCFB),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFE2E8E4),
    onSurfaceVariant = Color(0xFF3E4944),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AD4C0),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0F4F41),
    onPrimaryContainer = Color(0xFFCDE8DC),
    secondary = Color(0xFFB4CCBF),
    onSecondary = Color(0xFF1C352C),
    background = Color(0xFF111413),
    onBackground = Color(0xFFE2E3E1),
    surface = Color(0xFF171A19),
    onSurface = Color(0xFFE2E3E1),
    surfaceVariant = Color(0xFF3E4944),
    onSurfaceVariant = Color(0xFFC0C9C3),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
)

@Composable
fun EventCounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
