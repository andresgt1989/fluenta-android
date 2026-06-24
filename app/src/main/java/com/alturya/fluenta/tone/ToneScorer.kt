package com.alturya.fluenta.tone

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Motor de SCORING DE TONO por voz (diferenciador "chino genio", tarea T2).
 *
 * Es lógica PURA (sin dependencias de Android) para poder testearla en la JVM:
 * la captura por micrófono (AudioRecord) y su UI las dispara la pantalla cuando
 * llegue el mock de Claude Design; aquí vive el algoritmo que esa pantalla usará.
 *
 * Pipeline:
 *   PCM 16-bit mono ──pitchContour──▶ F0 por frame (Hz, 0 = no sonoro)
 *                   ──normalizeContour──▶ contorno sonoro re-muestreado a N puntos,
 *                                          en SEMITONOS relativos a la media del hablante
 *                                          (independiente de voz aguda/grave)
 *                   ──classify──▶ tono 1..4 detectado + parecido con cada plantilla
 *
 * Los 4 tonos del mandarín por su curva de F0 (Chao tone letters):
 *   1 (55) alto plano · 2 (35) ascendente · 3 (214) cae-y-sube · 4 (51) descendente.
 */
object ToneScorer {

    /** Rango de F0 de voz humana cantada/hablada que buscamos (Hz). */
    private const val MIN_HZ = 70.0
    private const val MAX_HZ = 450.0

    /** Nº de puntos a los que re-muestreamos el contorno para comparar. */
    const val POINTS = 9

    /** Mínimo de frames sonoros para considerar la grabación válida. */
    private const val MIN_VOICED_FRAMES = 6

    /**
     * Plantillas de cada tono como niveles de pitch Chao (1=grave .. 5=agudo)
     * a lo largo del tiempo normalizado. Se z-normalizan antes de comparar, así
     * que solo importa la FORMA de la curva, no su altura absoluta.
     */
    private val TEMPLATES: Map<Int, FloatArray> = mapOf(
        1 to floatArrayOf(5f, 5f, 5f, 5f, 5f, 5f, 5f, 5f, 5f),         // alto plano
        2 to floatArrayOf(3f, 3.1f, 3.3f, 3.6f, 4f, 4.4f, 4.7f, 4.9f, 5f), // ascendente
        3 to floatArrayOf(2.4f, 1.8f, 1.3f, 1f, 1f, 1.4f, 2.2f, 3.2f, 4f), // cae y sube
        4 to floatArrayOf(5f, 4.4f, 3.7f, 3f, 2.4f, 1.9f, 1.5f, 1.2f, 1f), // descendente
    )

    /** z-normalizadas una sola vez (media 0, desviación 1). */
    private val NORM_TEMPLATES: Map<Int, FloatArray> =
        TEMPLATES.mapValues { zNormalize(it.value) }

    // ─────────────────────────────────────────────────────────────────────
    // 1 · Detección de pitch (F0) por autocorrelación
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Estima F0 por frame con autocorrelación normalizada. Devuelve Hz por frame;
     * 0f = frame no sonoro (silencio/ruido). Frame por defecto ~40 ms con 50% de salto.
     */
    fun pitchContour(
        pcm: ShortArray,
        sampleRate: Int,
        frameSize: Int = (sampleRate * 0.04).toInt(),
        hop: Int = (sampleRate * 0.02).toInt(),
    ): FloatArray {
        if (pcm.size < frameSize || frameSize <= 0 || hop <= 0) return FloatArray(0)
        val minLag = (sampleRate / MAX_HZ).toInt().coerceAtLeast(1)
        val maxLag = (sampleRate / MIN_HZ).toInt().coerceAtMost(frameSize - 1)
        if (maxLag <= minLag) return FloatArray(0)

        val frames = ((pcm.size - frameSize) / hop) + 1
        val out = FloatArray(frames)
        val buf = DoubleArray(frameSize)

        for (f in 0 until frames) {
            val start = f * hop
            var energy = 0.0
            for (i in 0 until frameSize) {
                val s = pcm[start + i].toDouble()
                buf[i] = s
                energy += s * s
            }
            val rms = sqrt(energy / frameSize)
            if (rms < 250.0) { out[f] = 0f; continue } // umbral de silencio (16-bit)

            // r(0) para normalizar
            val r0 = energy
            var bestLag = -1
            var bestCorr = 0.0
            var lag = minLag
            while (lag <= maxLag) {
                var sum = 0.0
                var i = 0
                while (i < frameSize - lag) { sum += buf[i] * buf[i + lag]; i++ }
                val norm = sum / r0
                if (norm > bestCorr) { bestCorr = norm; bestLag = lag }
                lag++
            }
            // periodicidad clara → sonoro
            out[f] = if (bestLag > 0 && bestCorr > 0.45) (sampleRate.toDouble() / bestLag).toFloat() else 0f
        }
        return out
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2 · Normalización del contorno (sonoro, semitonos, re-muestreado)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Toma un contorno F0 en Hz, descarta lo no sonoro, lo pasa a SEMITONOS relativos
     * a su media (independiente del registro del hablante) y lo re-muestrea a [points].
     * Devuelve null si no hay suficientes frames sonoros.
     */
    fun normalizeContour(hz: FloatArray, points: Int = POINTS): FloatArray? {
        val voiced = hz.filter { it > 0f }
        if (voiced.size < MIN_VOICED_FRAMES) return null

        // mediana como referencia robusta del registro del hablante
        val ref = voiced.sorted()[voiced.size / 2].toDouble()
        // semitonos relativos: 12*log2(f/ref)
        val semis = hz.map { if (it > 0f) 12.0 * ln(it / ref) / ln(2.0) else Double.NaN }

        // re-muestrear los puntos SONOROS a [points] respetando su orden temporal
        val voicedSeq = semis.filter { !it.isNaN() }
        return resample(voicedSeq, points)
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3 · Clasificación contra las plantillas de tono
    // ─────────────────────────────────────────────────────────────────────

    /** Resultado de clasificar un contorno: tono más probable y parecido (0..1) con cada uno. */
    data class ToneResult(val tone: Int, val scores: Map<Int, Float>)

    /**
     * Clasifica un contorno ya normalizado (en semitonos, [POINTS] puntos) comparándolo
     * con las 4 plantillas por correlación de forma. Devuelve el tono ganador y el
     * parecido normalizado 0..1 con cada plantilla.
     */
    fun classify(norm: FloatArray): ToneResult {
        val z = zNormalize(norm)
        val scores = NORM_TEMPLATES.mapValues { (_, tpl) ->
            ((correlation(z, tpl) + 1f) / 2f).coerceIn(0f, 1f) // [-1,1] → [0,1]
        }
        val best = scores.maxByOrNull { it.value }!!.key
        return ToneResult(best, scores)
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4 · Pipeline completo: puntuar la grabación contra el tono esperado
    // ─────────────────────────────────────────────────────────────────────

    /**
     * @param voiced false si la grabación no tenía voz suficiente para puntuar.
     * @param score 0..100 = parecido con el tono ESPERADO (lo que el usuario debía producir).
     * @param detectedTone el tono que más se parece a lo que dijo (para feedback "sonó a 2.º").
     */
    data class ToneScore(
        val expectedTone: Int,
        val detectedTone: Int,
        val score: Int,
        val correct: Boolean,
        val voiced: Boolean,
        val contour: FloatArray,
    )

    /** No sonoro / grabación inválida. */
    fun emptyScore(expectedTone: Int) =
        ToneScore(expectedTone, 0, 0, false, false, FloatArray(0))

    /** Pipeline completo desde PCM crudo. */
    fun score(pcm: ShortArray, sampleRate: Int, expectedTone: Int): ToneScore {
        val contour = normalizeContour(pitchContour(pcm, sampleRate)) ?: return emptyScore(expectedTone)
        return scoreContour(contour, expectedTone)
    }

    /** Igual que [score] pero partiendo de un contorno ya normalizado (útil para tests). */
    fun scoreContour(contour: FloatArray, expectedTone: Int): ToneScore {
        val result = classify(contour)
        val match = result.scores[expectedTone] ?: 0f
        // mapear [0..1] → 0..100 con piso suave: forma muy distinta no baja de 0,
        // pero una correlación negativa (curva opuesta) sí debe puntuar bajo.
        val score = (match * 100f).toInt().coerceIn(0, 100)
        return ToneScore(
            expectedTone = expectedTone,
            detectedTone = result.tone,
            score = score,
            correct = result.tone == expectedTone,
            voiced = true,
            contour = contour,
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // utilidades numéricas
    // ─────────────────────────────────────────────────────────────────────

    /** Re-muestrea [src] (en orden) a [n] puntos por interpolación lineal. */
    private fun resample(src: List<Double>, n: Int): FloatArray {
        if (src.isEmpty()) return FloatArray(n)
        if (src.size == 1) return FloatArray(n) { src[0].toFloat() }
        val out = FloatArray(n)
        for (i in 0 until n) {
            val pos = i.toDouble() * (src.size - 1) / (n - 1)
            val lo = pos.toInt()
            val hi = (lo + 1).coerceAtMost(src.size - 1)
            val frac = pos - lo
            out[i] = (src[lo] * (1 - frac) + src[hi] * frac).toFloat()
        }
        return out
    }

    /** Media 0, desviación 1. Si la curva es plana, queda en ceros (forma plana válida). */
    private fun zNormalize(v: FloatArray): FloatArray {
        if (v.isEmpty()) return v
        val mean = v.average()
        var varSum = 0.0
        for (x in v) varSum += (x - mean) * (x - mean)
        val std = sqrt(varSum / v.size)
        if (std < 1e-6) return FloatArray(v.size) // plano
        return FloatArray(v.size) { ((v[it] - mean) / std).toFloat() }
    }

    /** Correlación de Pearson entre dos vectores ya z-normalizados o no. [-1,1]. */
    private fun correlation(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        if (n == 0) return 0f
        val az = zNormalize(a.copyOf(n))
        val bz = zNormalize(b.copyOf(n))
        // caso plano: si ambos planos → 1; si uno plano y otro no → 0
        val aFlat = az.all { abs(it) < 1e-6f }
        val bFlat = bz.all { abs(it) < 1e-6f }
        if (aFlat && bFlat) return 1f
        if (aFlat || bFlat) return 0f
        var dot = 0.0
        for (i in 0 until n) dot += az[i] * bz[i]
        return (dot / n).toFloat().coerceIn(-1f, 1f)
    }
}
