package com.alturya.fluenta.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Esquinas redondeadas generosas = sensación moderna/amable (estilo Duolingo/Babbel).
// Material 3 mapea estos tokens a componentes: Card→medium/large, Button→full,
// TextField→extraSmall, etc. Suben el "look pro" sin tocar cada pantalla.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
