package com.alturya.fluenta.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alturya.fluenta.ui.theme.FluentaSlate

/**
 * Botón 3D táctil — la seña de identidad visual estilo Duolingo. Tiene una "cara"
 * de color sólido sobre una "base" más oscura; al presionar, la cara baja hasta la
 * base (sensación de tecla física). Eleva el look de toda la app con un solo
 * componente reutilizable.
 *
 * Variantes: primary (relleno marca), success (verde), danger (rojo), neutral (gris).
 */
enum class FluentaButtonStyle { Primary, Success, Danger, Neutral }

@Composable
fun FluentaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FluentaButtonStyle = FluentaButtonStyle.Primary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val (face, base, content) = when (style) {
        FluentaButtonStyle.Primary -> Triple(cs.primary, cs.primary.darken(), cs.onPrimary)
        // a11y: cara verde profundizada (#22C55E daba 2.28:1 con texto blanco) -> #178640 = 4.65:1
        FluentaButtonStyle.Success -> Triple(Color(0xFF178640), Color(0xFF15803D), Color.White)
        FluentaButtonStyle.Danger  -> Triple(cs.error, cs.error.darken(), cs.onError)
        FluentaButtonStyle.Neutral -> Triple(cs.surfaceVariant, FluentaSlate.copy(alpha = 0.5f), cs.onSurface)
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 5.dp
    val offsetY by animateDpAsState(
        targetValue = if (pressed || !enabled) 0.dp else depth,
        animationSpec = spring(stiffness = 1200f),
        label = "btn3d",
    )

    val faceColor = if (enabled) face else face.copy(alpha = 0.45f)
    val baseColor = if (enabled) base else base.copy(alpha = 0.45f)

    Box(modifier = modifier.height(56.dp + depth)) {
        // Base (sombra sólida 3D) — fija al fondo
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(18.dp))
                .background(baseColor),
        )
        // Cara — se mueve hacia abajo al presionar
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.TopCenter)
                .padding(top = offsetY)
                .clip(RoundedCornerShape(18.dp))
                .background(faceColor)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    CompositionLocalProvider(LocalContentColor provides content) { leading() }
                }
                Text(
                    text,
                    color = content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

// Oscurece un color para la "base" 3D del botón.
private fun Color.darken(factor: Float = 0.78f): Color =
    Color(red * factor, green * factor, blue * factor, alpha)
