package com.alturya.fluenta.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Efectos de sonido del lesson player sin empaquetar assets de audio: usa el
 * ToneGenerator del sistema. Best-effort — si falla (silencio, foco de audio),
 * no rompe nada; la háptica sigue dando feedback.
 */
object Sfx {
    // Respeta el toggle de Ajustes. MainActivity lo sincroniza desde SettingsStore.
    @Volatile var enabled: Boolean = true

    // Correct: short satisfying ding
    fun correct() = play(ToneGenerator.TONE_PROP_BEEP, 120)
    // Wrong: gentle error tone (not harsh)
    fun wrong() = play(ToneGenerator.TONE_PROP_NACK, 180)
    // Lesson complete: celebratory double-beep
    fun success() {
        play(ToneGenerator.TONE_PROP_BEEP, 180)
        Handler(Looper.getMainLooper()).postDelayed({ play(ToneGenerator.TONE_PROP_BEEP2, 280) }, 200)
    }
    // Level-up notification
    fun levelUp() {
        play(ToneGenerator.TONE_PROP_BEEP, 120)
        Handler(Looper.getMainLooper()).postDelayed({ play(ToneGenerator.TONE_PROP_BEEP2, 300) }, 150)
        Handler(Looper.getMainLooper()).postDelayed({ play(ToneGenerator.TONE_PROP_ACK, 400) }, 450)
    }
    // Streak alert
    fun streakAlert() = play(ToneGenerator.TONE_PROP_NACK, 120)

    private fun play(tone: Int, durationMs: Int) {
        if (!enabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
            tg.startTone(tone, durationMs)
            Handler(Looper.getMainLooper()).postDelayed(
                { runCatching { tg.release() } },
                (durationMs + 150).toLong(),
            )
        } catch (_: Exception) {
            // Sound is optional — haptics still provide feedback
        }
    }
}
