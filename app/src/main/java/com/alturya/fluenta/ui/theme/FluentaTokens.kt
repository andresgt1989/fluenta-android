package com.alturya.fluenta.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tokens del kit "Claude Design" (Fluenta Language Learning App).
 *
 * Fuente de verdad: los mockups .dc.html del proyecto de diseño. Se usan en las
 * pantallas de entrada/núcleo (Onboarding, LanguageSelector, GuestLesson, Paywall)
 * para reproducir el diseño con fidelidad, conservando el botón 3D del sistema
 * ([com.alturya.fluenta.ui.FluentaButton]) y el [FluentaTheme] de Material 3.
 *
 * a11y: los botones con texto y el texto sobre color usan el verde a11y-safe del
 * tema (MaterialTheme.colorScheme.primary = #138378, 4.62:1 con blanco). Estos
 * tokens aportan los colores DECORATIVOS y de acento del kit (ink, ámbar, mint,
 * bordes, surface) que el tema no expone, sin romper el contraste WCAG AA.
 */
object FluentaTokens {
    // Marca / decoración (barras de progreso, bordes seleccionados, badges)
    val Primary = Color(0xFF10B981)        // verde marca del kit
    val PrimaryDark = Color(0xFF059669)    // relieve 3D / acentos profundos
    val Container = Color(0xFFCDEEE6)      // mint container

    // Texto
    val Ink = Color(0xFF0F2E27)            // títulos / texto fuerte (alto contraste)
    val Muted = Color(0xFF5B7268)          // texto secundario (4.7:1 sobre Surface)
    val BrandText = Color(0xFF0A6F64)      // verde de marca para texto (5.4:1 sobre Surface)

    // Superficies
    val Surface = Color(0xFFF1FAF6)        // fondo de pantalla del kit
    val Border = Color(0xFFE7EFEB)         // borde de tarjetas no seleccionadas

    // Acentos
    val Amber = Color(0xFFE08A00)          // pinyin / lectura · badge "recomendado"
    val AmberInk = Color(0xFFB36F00)       // ámbar a11y-safe para texto pequeño (≥4.5:1)
    val Coral = Color(0xFFE8554B)          // correcciones / energía
    val CoralContainer = Color(0xFFFFE1DC)

    // Feedback de acierto (burbuja "¡Bien hecho!")
    val SuccessBg = Color(0xFFD6F4E4)
    val SuccessInk = Color(0xFF0B7B53)     // 4.7:1 sobre SuccessBg

    // Gradiente hero: verde marca → mint (kit). Texto encima va en Ink, no blanco.
    val HeroGradient = Brush.verticalGradient(listOf(Primary, Container))

    // Radios del kit: card r20, chip/btn r14-16
    val CardShape = RoundedCornerShape(20.dp)
    val FieldShape = RoundedCornerShape(16.dp)
}
