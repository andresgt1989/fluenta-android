package com.alturya.fluenta

import com.alturya.fluenta.util.isRtl
import com.alturya.fluenta.util.levelIndex
import com.alturya.fluenta.util.levelLabel
import com.alturya.fluenta.util.levelSystemName
import com.alturya.fluenta.util.flag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Red de seguridad: funciones puras de etiquetas de nivel/idioma. Corren en JVM
 * (sin emulador) y bloquean regresiones en CI. Si algo de esto se rompe, el test falla.
 */
class LevelLabelsTest {

    @Test fun rtl_languages_are_detected() {
        assertTrue(isRtl("ar")); assertTrue(isRtl("he")); assertTrue(isRtl("fa")); assertTrue(isRtl("ur"))
        assertTrue(isRtl("AR"))            // case-insensitive
        assertFalse(isRtl("es")); assertFalse(isRtl("en")); assertFalse(isRtl(null))
    }

    @Test fun level_system_name_maps_correctly() {
        assertEquals("JLPT", levelSystemName("jlpt"))
        assertEquals("HSK", levelSystemName("hsk"))
        assertEquals("TOPIK", levelSystemName("topik"))
        assertEquals("CEFR", levelSystemName("cefr"))
        assertEquals("CEFR", levelSystemName(null))   // default
    }

    @Test fun cefr_label_is_uppercased() {
        assertEquals("B1", levelLabel("b1", "cefr"))
        assertEquals("A2", levelLabel("a2", null))
    }

    @Test fun jlpt_and_hsk_labels_use_their_ladder() {
        // b1 = índice 2 en la escala CEFR → 3er peldaño de cada sistema
        assertTrue(levelLabel("b1", "jlpt").startsWith("JLPT"))
        assertTrue(levelLabel("b1", "hsk").startsWith("HSK"))
        assertTrue(levelLabel("b1", "topik").startsWith("TOPIK"))
    }

    @Test fun level_index_orders_cefr() {
        assertEquals(0, levelIndex("a1"))
        assertEquals(4, levelIndex("c1"))      // c1 es el nivel MÁS ALTO soportado hoy
        // GAP UNICORNIO: el backend topa en c1 — C2 (maestría) aún NO existe.
        // Este test documenta el contrato actual; cuando el backend agregue c2, sube a 5.
        assertEquals(-1, levelIndex("c2"))
        assertEquals(-1, levelIndex(null))     // desconocido → nudge "descubre tu nivel"
        assertEquals(-1, levelIndex("zzz"))
    }

    @Test fun flags_exist_for_core_languages() {
        // No deben estar vacíos para los idiomas principales
        listOf("en", "es", "pt", "fr", "de", "ja", "zh", "ar").forEach {
            assertTrue("falta bandera para $it", flag(it).isNotBlank())
        }
    }
}
