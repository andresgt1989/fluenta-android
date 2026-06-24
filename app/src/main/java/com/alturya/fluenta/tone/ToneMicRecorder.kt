package com.alturya.fluenta.tone

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Captura de voz para el scoring de tono: graba PCM 16-bit mono a 16 kHz y se lo
 * entrega a [ToneScorer]. Aislado de la UI a propósito; la pantalla solo llama
 * [record] dentro de una corrutina. Devuelve null si no hay permiso/hardware o
 * si no se capturó voz suficiente, para que la UI muestre "no te oí".
 */
object ToneMicRecorder {
    const val SAMPLE_RATE = ToneScorerSampleRate

    /** Graba ~[durationMs] ms de PCM mono. null si falla (sin permiso, hw o muy corto). */
    suspend fun record(durationMs: Int = 1600): ShortArray? = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return@withContext null
        val total = SAMPLE_RATE * durationMs / 1000

        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf, total * 2),
            )
        } catch (e: SecurityException) {
            return@withContext null // sin permiso RECORD_AUDIO
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return@withContext null }

        val out = ShortArray(total)
        var read = 0
        try {
            rec.startRecording()
            while (read < total) {
                val n = rec.read(out, read, total - read)
                if (n <= 0) break
                read += n
            }
        } catch (e: Exception) {
            rec.release(); return@withContext null
        } finally {
            try { rec.stop() } catch (_: Exception) {}
        }
        rec.release()
        // exige al menos medio segundo capturado para intentar puntuar
        if (read < total / 2) null else out.copyOf(read)
    }
}

/** Tasa de muestreo compartida con [ToneScorer] (evita un import circular de constantes). */
const val ToneScorerSampleRate = 16000
