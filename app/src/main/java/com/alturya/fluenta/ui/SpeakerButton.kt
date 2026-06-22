package com.alturya.fluenta.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.data.I18nStore

/**
 * Botón de "escuchar" accesible (a11y).
 *
 * Por qué existe: varias pantallas pintaban el ícono de audio dentro de un
 * `IconButton(Modifier.size(32.dp))`, lo que encoge el ÁREA TÁCTIL a 32dp — por
 * debajo del mínimo de 48dp (WCAG 2.5.5 / Material). Aquí el `IconButton` conserva
 * su tamaño táctil por defecto (48dp) aunque el ícono visual sea pequeño, y la
 * descripción se traduce vía [I18nStore] (por eso es a11y + i18n a la vez).
 *
 * @param contentDescription etiqueta para lectores de pantalla (traducible).
 * @param iconSize tamaño VISUAL del ícono (no afecta el área táctil de 48dp).
 */
@Composable
fun SpeakerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = I18nStore.t("a11y.play_audio", "Escuchar"),
    iconSize: Dp = 16.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
