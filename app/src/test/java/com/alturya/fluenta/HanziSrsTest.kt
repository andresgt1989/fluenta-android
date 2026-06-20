package com.alturya.fluenta

import com.alturya.fluenta.script.HanziSrs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Red de seguridad del motor SRS de caracteres (T3). Verifica la programación pura:
 * progresión de intervalos al acertar, re-aprendizaje y castigo de ease al fallar,
 * cotas de ease, y selección de la cola de vencidas.
 */
class HanziSrsTest {

    private val now = 1_000_000_000_000L
    private fun card(glyph: String = "好") = HanziSrs.newCard(glyph, "hǎo", "bueno", now)

    @Test fun new_card_is_due_immediately() {
        assertTrue(HanziSrs.isDue(card(), now))
    }

    @Test fun first_correct_schedules_one_day() {
        val c = HanziSrs.schedule(card(), remembered = true, nowMillis = now)
        assertEquals(1, c.intervalDays)
        assertEquals(1, c.reps)
        assertEquals(now + HanziSrs.DAY_MS, c.dueAtMillis)
        assertFalse(HanziSrs.isDue(c, now))
    }

    @Test fun second_correct_schedules_three_days() {
        var c = HanziSrs.schedule(card(), remembered = true, nowMillis = now)
        c = HanziSrs.schedule(c, remembered = true, nowMillis = c.dueAtMillis)
        assertEquals(3, c.intervalDays)
        assertEquals(2, c.reps)
    }

    @Test fun third_correct_multiplies_by_ease() {
        var c = HanziSrs.schedule(card(), remembered = true, nowMillis = now)        // 1d, ease 2.65
        c = HanziSrs.schedule(c, remembered = true, nowMillis = c.dueAtMillis)        // 3d, ease 2.80
        val easeBefore = c.ease
        c = HanziSrs.schedule(c, remembered = true, nowMillis = c.dueAtMillis)        // 3 * ease
        assertEquals(Math.round(3 * easeBefore).toInt(), c.intervalDays)
        assertEquals(3, c.reps)
    }

    @Test fun fail_resets_reps_and_relearns_same_session() {
        var c = HanziSrs.schedule(card(), remembered = true, nowMillis = now)
        c = HanziSrs.schedule(c, remembered = true, nowMillis = c.dueAtMillis)
        val failAt = c.dueAtMillis
        c = HanziSrs.schedule(c, remembered = false, nowMillis = failAt)
        assertEquals(0, c.reps)
        assertEquals(0, c.intervalDays)
        assertEquals(1, c.lapses)
        assertEquals(failAt + HanziSrs.RELEARN_MS, c.dueAtMillis)
    }

    @Test fun ease_is_bounded() {
        // Muchos aciertos no superan MAX_EASE
        var c = card()
        repeat(20) { c = HanziSrs.schedule(c, remembered = true, nowMillis = c.dueAtMillis) }
        assertTrue(c.ease <= HanziSrs.MAX_EASE)
        // Muchos fallos no bajan de MIN_EASE
        repeat(20) { c = HanziSrs.schedule(c, remembered = false, nowMillis = c.dueAtMillis) }
        assertTrue(c.ease >= HanziSrs.MIN_EASE)
    }

    @Test fun due_returns_overdue_first() {
        val a = HanziSrs.newCard("一", "yī", "uno", now - 5_000)
        val b = HanziSrs.newCard("二", "èr", "dos", now - 50_000)
        val future = HanziSrs.newCard("三", "sān", "tres", now + HanziSrs.DAY_MS)
        val due = HanziSrs.due(listOf(a, b, future), now)
        assertEquals(listOf("二", "一"), due.map { it.glyph })
    }
}
