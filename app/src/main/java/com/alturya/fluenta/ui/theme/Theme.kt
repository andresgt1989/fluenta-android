package com.alturya.fluenta.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = FluentaGreen,
    onPrimary = Color.White,
    primaryContainer = FluentaGreenLight,
    onPrimaryContainer = FluentaGreenDark,
    secondary = FluentaGreenBright,
    onSecondary = Color.White,
    tertiary = FluentaAmber,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = FluentaInk,
    surface = Color.White,
    onSurface = FluentaInk,
    surfaceVariant = FluentaMist,
    onSurfaceVariant = FluentaSlate,
    error = FluentaError,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = FluentaGreenBright,
    onPrimary = FluentaGreenDark,
    primaryContainer = FluentaGreenContainerDark,
    onPrimaryContainer = FluentaGreenLight,
    secondary = FluentaGreenBright,
    onSecondary = FluentaGreenDark,
    tertiary = FluentaAmber,
    onTertiary = Color.White,
    background = FluentaNight,
    onBackground = Color(0xFFE5E7EB),
    surface = FluentaNightSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2A24),
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

@Composable
fun FluentaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Brand colors always win — no dynamic (system) color override.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
