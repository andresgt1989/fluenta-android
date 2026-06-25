package com.alturya.fluenta

import com.alturya.fluenta.tone.ToneCard
import com.alturya.fluenta.tone.ToneSrs
import com.alturya.fluenta.tone.ToneWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Red de seguridad del SRS de tono (T2): conecta el score de voz con la repetición
 * espaciada. Si se rompe, los tonos mal producidos no volverían a tiempo (retención =
 * la brecha real al unicornio). Motor puro → testeable en JVM.
 */
class ToneSrsTest {
    private val now = 1_000_000_000_000L
    private val ma = ToneWord("妈", "ma", 1, "mamá")

    @Test fun new_card_is_due_now() {
        val c = ToneSrs.newCard(ma, now)
        assertTrue(ToneSrs.isDue(c, now))
        assertEquals("妈|ma|1", c.key)
        assertEquals(0, c.reps)
    }

    @Test fun passing_score_spaces_the_card_into_the_future() {
        val c = ToneSrs.schedule(ToneSrs.newCard(ma, now), score = 85, nowMillis = now)
        assertEquals(1, c.reps)
        assertEquals(1, c.intervalDays)       // primer acierto → 1 día
        assertTrue("debe vencer en el futuro", c.dueAtMillis > now)
        assertEquals(85, c.bestScore)
    }

    @Test fun failing_score_relearns_soon_and_counts_lapse() {
        val learned = ToneSrs.schedule(ToneSrs.schedule(ToneSrs.newCard(ma, now), 90, now), 90, now)
        assertTrue(learned.reps >= 2)
        val failed = ToneSrs.schedule(learned, score = 40, nowMillis = now)
        assertEquals(0, failed.reps)
        assertEquals(1, failed.lapses)
        assertEquals(0, failed.intervalDays)
        // vuelve en minutos, no días
        assertTrue(failed.dueAtMillis - now <= ToneSrs.RELEARN_MS)
        assertTrue("la facilidad baja al fallar", failed.ease < learned.ease)
    }

    @Test fun higher_score_grows_ease_more_than_a_bare_pass() {
        val base = ToneSrs.newCard(ma, now)
        val perfect = ToneSrs.schedule(base, score = 100, nowMillis = now)
        val barely = ToneSrs.schedule(base, score = 60, nowMillis = now)
        assertTrue("100 sube más la facilidad que 60", perfect.ease > barely.ease)
        assertEquals(100, perfect.bestScore)
    }

    @Test fun best_score_keeps_the_maximum() {
        var c = ToneSrs.schedule(ToneSrs.newCard(ma, now), 95, now)
        c = ToneSrs.schedule(c, 70, now) // peor intento no baja el récord
        assertEquals(95, c.bestScore)
    }

    @Test fun review_deck_rebuilds_words_from_due_cards() {
        val cards = listOf(
            ToneCard("妈|ma|1", "妈", "ma", 1, "mamá", dueAtMillis = now - 100),
            ToneCard("好|hao|3", "好", "hao", 3, "bueno", dueAtMillis = now - 50),
            ToneCard("bad||0", "", "", 0, dueAtMillis = now), // inválida: se descarta
        )
        val deck = ToneSrs.reviewDeck(cards, max = 8)
        assertEquals(2, deck.size)
        assertEquals("妈", deck[0].hanzi)
        assertEquals(1, deck[0].tone)
        assertTrue(deck.none { it.hanzi.isBlank() })
    }

    @Test fun review_deck_respects_max() {
        val many = (1..20).map { ToneCard("k$it", "字", "zi", 4, dueAtMillis = now - it) }
        assertEquals(5, ToneSrs.reviewDeck(many, max = 5).size)
    }

    @Test fun due_returns_only_overdue_sorted() {
        val a = ToneCard("a|a|1", "啊", "a", 1, dueAtMillis = now - 2000)
        val b = ToneCard("b|b|2", "波", "bo", 2, dueAtMillis = now - 5000)
        val future = ToneCard("c|c|3", "草", "cao", 3, dueAtMillis = now + 9999)
        val due = ToneSrs.due(listOf(a, b, future), now)
        assertEquals(listOf("b|b|2", "a|a|1"), due.map { it.key }) // más atrasada primero
        assertFalse(due.any { it.key == "c|c|3" })
    }
}
