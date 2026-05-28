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
    fun correct() = play(ToneGenerator.TONE_PROP_ACK, 140)
    fun wrong() = play(ToneGenerator.TONE_SUP_ERROR, 250)
    fun success() = play(ToneGenerator.TONE_PROP_BEEP2, 400)

    private fun play(tone: Int, durationMs: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            tg.startTone(tone, durationMs)
            Handler(Looper.getMainLooper()).postDelayed(
                { runCatching { tg.release() } },
                (durationMs + 120).toLong(),
            )
        } catch (_: Exception) {
            // sonido es opcional; nunca debe tumbar la lección
        }
    }
}
