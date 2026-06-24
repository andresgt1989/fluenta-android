package com.alturya.fluenta

import com.alturya.fluenta.tone.ToneScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/**
 * Red de seguridad del SCORING DE TONO por voz (diferenciador "chino genio", T2).
 * El motor decide si el usuario produjo el tono correcto (1 plano · 2 sube · 3 cae-sube
 * · 4 baja) a partir de su contorno de pitch. Si esto se rompe, el feedback de
 * pronunciación mentiría al alumno. Funciones puras → testeables en JVM sin micrófono.
 */
class ToneScorerTest {

    // ── clasificación a partir de contornos sintéticos (semitonos relativos) ──

    @Test fun classifies_tone1_flat() {
        val flat = FloatArray(ToneScorer.POINTS) { 2f }
        assertEquals(1, ToneScorer.classify(flat).tone)
    }

    @Test fun classifies_tone2_rising() {
        // sube monótono, valores absolutos distintos a la plantilla (la forma manda)
        val rising = FloatArray(ToneScorer.POINTS) { -4f + it * 1f }
        assertEquals(2, ToneScorer.classify(rising).tone)
    }

    @Test fun classifies_tone3_dip() {
        val dip = floatArrayOf(0f, -2f, -4f, -5f, -5f, -3f, 0f, 2f, 3f)
        assertEquals(3, ToneScorer.classify(dip).tone)
    }

    @Test fun classifies_tone4_falling() {
        val falling = FloatArray(ToneScorer.POINTS) { 6f - it * 1.5f }
        assertEquals(4, ToneScorer.classify(falling).tone)
    }

    // ── scoring contra el tono esperado ──

    @Test fun matching_tone_scores_high() {
        val rising = FloatArray(ToneScorer.POINTS) { -4f + it * 1f }
        val s = ToneScorer.scoreContour(rising, expectedTone = 2)
        assertTrue("esperaba score alto, fue ${s.score}", s.score >= 80)
        assertTrue(s.correct)
        assertTrue(s.voiced)
    }

    @Test fun opposite_tone_scores_low() {
        // produjo ascendente (2) pero se esperaba descendente (4): curva opuesta
        val rising = FloatArray(ToneScorer.POINTS) { -4f + it * 1f }
        val s = ToneScorer.scoreContour(rising, expectedTone = 4)
        assertTrue("esperaba score bajo, fue ${s.score}", s.score <= 25)
        assertFalse(s.correct)
        assertEquals(2, s.detectedTone)
    }

    // ── normalización: descarta grabaciones sin voz suficiente ──

    @Test fun normalize_returns_null_when_too_few_voiced() {
        val mostlySilent = FloatArray(40) { 0f }.also { it[0] = 200f; it[1] = 201f }
        assertNull(ToneScorer.normalizeContour(mostlySilent))
    }

    @Test fun normalize_is_speaker_independent() {
        // misma FORMA (sube una octava) en registro grave y agudo → contorno equivalente
        val low = FloatArray(20) { 100f + it * 3f }
        val high = FloatArray(20) { 200f + it * 6f }
        val nl = ToneScorer.normalizeContour(low)!!
        val nh = ToneScorer.normalizeContour(high)!!
        for (i in nl.indices) {
            assertEquals("punto $i", nl[i].toDouble(), nh[i].toDouble(), 0.5)
        }
    }

    // ── pipeline completo desde PCM sintético ──

    @Test fun pitchContour_detects_pure_tone_frequency() {
        val sr = 16000
        val pcm = sine(sr, durSec = 0.5, freqAt = { 200.0 })
        val voiced = ToneScorer.pitchContour(pcm, sr).filter { it > 0f }
        assertTrue("no detectó frames sonoros", voiced.size > 5)
        val avg = voiced.average()
        assertEquals("F0 estimada $avg", 200.0, avg, 8.0)
    }

    @Test fun full_pipeline_scores_a_rising_sweep_as_tone2() {
        val sr = 16000
        // barrido 150→260 Hz = tono ascendente (2.º)
        val pcm = sine(sr, durSec = 0.6, freqAt = { t -> 150.0 + 110.0 * t / 0.6 })
        val s = ToneScorer.score(pcm, sr, expectedTone = 2)
        assertTrue(s.voiced)
        assertEquals(2, s.detectedTone)
        assertTrue("score ${s.score}", s.score >= 70)
    }

    @Test fun full_pipeline_scores_a_falling_sweep_as_tone4() {
        val sr = 16000
        val pcm = sine(sr, durSec = 0.6, freqAt = { t -> 260.0 - 110.0 * t / 0.6 })
        val s = ToneScorer.score(pcm, sr, expectedTone = 4)
        assertEquals(4, s.detectedTone)
        assertTrue("score ${s.score}", s.score >= 70)
    }

    @Test fun silence_is_not_voiced() {
        val sr = 16000
        val silence = ShortArray(sr / 2) { 0 }
        val s = ToneScorer.score(silence, sr, expectedTone = 1)
        assertFalse(s.voiced)
        assertEquals(0, s.score)
    }

    /** Genera PCM mono 16-bit de un seno cuya frecuencia varía con el tiempo (s). */
    private fun sine(sampleRate: Int, durSec: Double, freqAt: (t: Double) -> Double): ShortArray {
        val n = (sampleRate * durSec).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            phase += 2 * PI * freqAt(t) / sampleRate
            out[i] = (sin(phase) * 9000.0).toInt().toShort()
        }
        return out
    }
}
