package com.alturya.fluenta.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    // a11y: hues de marca PROFUNDIZADOS lo justo para que el texto sobre ellos pase
    // WCAG AA 4.5:1, conservando la identidad (mismo tono teal/coral, más oscuro).
    primary = Color(0xFF138378),         // teal (era #1BB6A6) — texto blanco 4.62:1 (antes 2.53)
    onPrimary = Color.White,
    primaryContainer = FluentaTealContainer,
    onPrimaryContainer = Color(0xFF0C766C), // texto sobre container claro 4.62:1 (antes 3.47)
    secondary = Color(0xFFBF5346),       // coral (era #FF6F5E) — texto blanco 4.62:1 (antes 2.73)
    onSecondary = Color.White,
    secondaryContainer = FluentaCoralContainer,
    onSecondaryContainer = Color(0xFFB33E31), // texto sobre container claro 4.64:1 (antes 2.98)
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
    primaryContainer = Color(0xFF0A5F57),  // a11y: oscurecido para que el texto claro pase 4.68:1 (antes 2.57)
    onPrimaryContainer = FluentaTealLight,
    secondary = FluentaCoral,
    onSecondary = Color(0xFF3A0E08),
    secondaryContainer = Color(0xFFB33E31), // a11y: oscurecido para texto claro 4.64:1 (antes 2.98)
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
