package com.alturya.fluenta.tone

import androidx.compose.ui.graphics.Color

/**
 * Color por tono del pinyin — REUTILIZABLE en todo el chino (tarea T2).
 * Convención del proyecto: 1=rojo · 2=verde · 3=azul · 4=morado · neutro=gris.
 * Es el mismo código de color que usan HelloChinese/Pleco, así el alumno asocia
 * color↔tono de forma consistente en cualquier pantalla (trainer, lectura, kit).
 */
object PinyinColors {
    val Tone1 = Color(0xFFD7263D) // rojo  · alto plano
    val Tone2 = Color(0xFF1FA463) // verde · ascendente
    val Tone3 = Color(0xFF2A7DE1) // azul  · cae y sube
    val Tone4 = Color(0xFF8B43D7) // morado· descendente
    val Neutral = Color(0xFF9AA39E) // gris · tono neutro (轻声)

    /** Color del tono 1..4; cualquier otro (0/5/neutro) → gris. */
    fun of(tone: Int): Color = when (tone) {
        1 -> Tone1
        2 -> Tone2
        3 -> Tone3
        4 -> Tone4
        else -> Neutral
    }
}
