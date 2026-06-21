package com.alturya.fluenta.ui.theme

import androidx.compose.ui.graphics.Color

// Fluenta brand — TEAL palette (Claude Design kit, 2026-05-29).
// Matches mascot/icon drawables. Replaces the legacy WhatsApp-green identity.
val FluentaTeal = Color(0xFF1BB6A6)        // primary
val FluentaTealDark = Color(0xFF0E8C80)    // deep / pressed
val FluentaTealLight = Color(0xFF7FDCCF)   // bright accent (on dark)
val FluentaTealContainer = Color(0xFFD2F2EC) // primary container (light)
val FluentaTealContainerDark = Color(0xFF06302C)
// Tono profundo para el gradiente hero (coach IA). Va de primary (#138378, blanco
// 4.62:1) a este, MÁS oscuro, así que el texto/íconos blancos siguen pasando WCAG AA.
val FluentaTealDeep = Color(0xFF0C6F65)

val FluentaCoral = Color(0xFFFF6F5E)       // secondary / energy
val FluentaCoralDark = Color(0xFFE8513F)
val FluentaCoralContainer = Color(0xFFFFE0DB)

val FluentaAmber = Color(0xFFFFC24B)       // tertiary / streak / highlight
val FluentaAmberDark = Color(0xFFF0A22E)

val FluentaPurple = Color(0xFF8E6FE0)      // support (vocabulary)
val FluentaSky = Color(0xFF56A8F2)         // support (listening)

// Semantic success — stays GREEN on purpose (universal "correct answer" cue).
val FluentaSuccess = Color(0xFF22C55E)

val FluentaInk = Color(0xFF27313F)         // text on light
val FluentaCream = Color(0xFFFFF6E9)       // warm surface accent
val FluentaMist = Color(0xFFF3F4F6)        // surface variant (light)
val FluentaSlate = Color(0xFF636A77)       // muted text — a11y: 4.95:1 sobre surfaceVariant (#6B7280 quedaba en 4.39, bajo el mínimo 4.5:1)
val FluentaError = Color(0xFFDC2626)

// Dark scheme surfaces (teal-tinted night)
val FluentaNight = Color(0xFF0E1A18)
val FluentaNightSurface = Color(0xFF13201D)
val FluentaNightSurfaceVariant = Color(0xFF1E2B28)
