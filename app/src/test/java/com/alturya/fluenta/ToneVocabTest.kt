package com.alturya.fluenta

import com.alturya.fluenta.tone.PinyinColors
import com.alturya.fluenta.tone.ToneVocab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garantiza que el entrenador de tono se alimenta de vocab REAL es→zh balanceado
 * por los 4 tonos y que el color de pinyin sigue la convención del proyecto.
 * Si esto se rompe, el alumno practicaría tonos sesgados o con colores incoherentes.
 */
class ToneVocabTest {

    @Test fun hsk1_has_at_least_six_words_per_tone() {
        val byTone = ToneVocab.HSK1.groupBy { it.tone }
        for (t in 1..4) {
            assertTrue("tono $t escaso", (byTone[t]?.size ?: 0) >= 6)
        }
    }

    @Test fun every_word_has_real_data() {
        for (w in ToneVocab.HSK1) {
            assertTrue("hanzi vacío", w.hanzi.isNotBlank())
            assertTrue("pinyin vacío", w.pinyin.isNotBlank())
            assertTrue("significado vacío", w.meaning.isNotBlank())
            assertTrue("tono fuera de rango: ${w.pinyin}", w.tone in 1..4)
        }
    }

    @Test fun session_is_balanced_across_four_tones() {
        val s = ToneVocab.session(perTone = 2, shuffle = false)
        assertEquals(8, s.size)
        val byTone = s.groupBy { it.tone }
        for (t in 1..4) assertEquals("tono $t", 2, byTone[t]?.size)
    }

    @Test fun has_real_vocab_for_levels_1_to_3_balanced() {
        for (lvl in 1..3) {
            val byTone = ToneVocab.byLevel[lvl]!!.groupBy { it.tone }
            for (t in 1..4) assertTrue("nivel $lvl tono $t escaso", (byTone[t]?.size ?: 0) >= 2)
        }
    }

    @Test fun all_is_deduped_and_spans_levels() {
        val all = ToneVocab.all
        // sin duplicados por identidad
        assertEquals(all.size, all.distinctBy { "${it.hanzi}|${it.pinyin}|${it.tone}" }.size)
        // incluye sílabas de HSK2/HSK3, no solo HSK1
        assertTrue(all.any { it.hanzi == "鱼" })  // HSK2
        assertTrue(all.any { it.hanzi == "甜" })  // HSK3
    }

    @Test fun session_can_target_a_level() {
        val s = ToneVocab.session(perTone = 2, shuffle = false, level = 2)
        assertEquals(8, s.size)
        assertTrue(s.all { it in ToneVocab.HSK2 })
    }

    @Test fun pinyin_color_follows_convention() {
        assertEquals(PinyinColors.Tone1, PinyinColors.of(1))
        assertEquals(PinyinColors.Tone2, PinyinColors.of(2))
        assertEquals(PinyinColors.Tone3, PinyinColors.of(3))
        assertEquals(PinyinColors.Tone4, PinyinColors.of(4))
        assertEquals(PinyinColors.Neutral, PinyinColors.of(0))
        assertEquals(PinyinColors.Neutral, PinyinColors.of(5))
    }

    @Test fun detects_tone_from_marked_pinyin() {
        assertEquals(1, PinyinColors.toneOfMarked("mā"))
        assertEquals(2, PinyinColors.toneOfMarked("má"))
        assertEquals(3, PinyinColors.toneOfMarked("mǎ"))
        assertEquals(4, PinyinColors.toneOfMarked("mà"))
        assertEquals(3, PinyinColors.toneOfMarked("nǐ hǎo")) // primera marca encontrada
        assertEquals(0, PinyinColors.toneOfMarked("ma"))     // neutro / sin marca
        assertEquals(PinyinColors.Tone3, PinyinColors.ofMarked("shuǐ"))
        assertEquals(PinyinColors.Neutral, PinyinColors.ofMarked("de"))
    }
}
