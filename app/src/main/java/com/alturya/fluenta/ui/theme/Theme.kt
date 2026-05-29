package com.alturya.fluenta.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = FluentaTeal,
    onPrimary = Color.White,
    primaryContainer = FluentaTealContainer,
    onPrimaryContainer = FluentaTealDark,
    secondary = FluentaCoral,
    onSecondary = Color.White,
    secondaryContainer = FluentaCoralContainer,
    onSecondaryContainer = FluentaCoralDark,
    tertiary = FluentaAmber,
    onTertiary = FluentaInk,
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
    primary = FluentaTealLight,
    onPrimary = FluentaTealContainerDark,
    primaryContainer = FluentaTealDark,
    onPrimaryContainer = FluentaTealLight,
    secondary = FluentaCoral,
    onSecondary = Color(0xFF3A0E08),
    secondaryContainer = FluentaCoralDark,
    onSecondaryContainer = FluentaCoralContainer,
    tertiary = FluentaAmber,
    onTertiary = FluentaInk,
    background = FluentaNight,
    onBackground = Color(0xFFE5E7EB),
    surface = FluentaNightSurface,
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = FluentaNightSurfaceVariant,
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
        shapes = AppShapes,
        content = content
    )
}
