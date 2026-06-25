package com.alturya.fluenta.tone

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * SRS de TONO (chino genio, T2): conecta el SCORE de voz (0-100 de [ToneScorer]) con la
 * repetición espaciada, para que los tonos que te salen mal vuelvan pronto y los que
 * dominas se espacien. Motor PURO y testeable en JVM (la persistencia vive en
 * [ToneSrsStore]); misma familia SM-2-lite que el SRS de caracteres [com.alturya.fluenta.script.HanziSrs]
 * pero graduado por score continuo en vez de acierto/fallo binario.
 */
data class ToneCard(
    val key: String,               // identidad estable: "hanzi|pinyin|tono"
    val hanzi: String,
    val pinyin: String,
    val tone: Int,
    val meaning: String = "",
    val ease: Double = 2.5,        // factor de facilidad (SM-2), acotado a [1.3, 3.0]
    val intervalDays: Int = 0,
    val reps: Int = 0,             // aciertos consecutivos
    val lapses: Int = 0,           // fallos tras haberlo aprendido
    val bestScore: Int = 0,        // mejor score de voz logrado (para mostrar progreso)
    val dueAtMillis: Long = 0L,
    val lastReviewedAt: Long = 0L,
)

object ToneSrs {
    const val DAY_MS = 86_400_000L
    const val RELEARN_MS = 10 * 60 * 1000L
    const val MIN_EASE = 1.3
    const val MAX_EASE = 3.0
    /** Score mínimo de voz para considerar el tono "producido bien". */
    const val PASS_SCORE = 60

    fun keyOf(w: ToneWord): String = "${w.hanzi}|${w.pinyin}|${w.tone}"

    /** Reconstruye la palabra entrenable desde una carta (para armar el mazo de repaso). */
    fun toWord(card: ToneCard): ToneWord = ToneWord(card.hanzi, card.pinyin, card.tone, card.meaning)

    /**
     * Mazo de REPASO desde las cartas vencidas (ya ordenadas por due): hasta [max]
     * palabras válidas (con hanzi y tono 1..4). Puro/testeable; la pantalla decide si
     * usarlo o caer a una sesión nueva cuando no hay suficiente vencido.
     */
    fun reviewDeck(dueCards: List<ToneCard>, max: Int = 8): List<ToneWord> =
        dueCards.asSequence()
            .filter { it.hanzi.isNotBlank() && it.tone in 1..4 }
            .map { toWord(it) }
            .take(max)
            .toList()

    /** Carta nueva desde una palabra: vencida de inmediato (entra a la cola hoy). */
    fun newCard(w: ToneWord, nowMillis: Long): ToneCard =
        ToneCard(key = keyOf(w), hanzi = w.hanzi, pinyin = w.pinyin, tone = w.tone, meaning = w.meaning, dueAtMillis = nowMillis)

    /**
     * Reprograma la carta tras una grabación puntuada. [score] 0-100 del motor de voz.
     * < [PASS_SCORE] → re-aprendizaje en caliente; ≥ → se espacia, con bonus de facilidad
     * proporcional a lo bien que salió (60 = mínimo, 100 = máximo). Función pura.
     */
    fun schedule(card: ToneCard, score: Int, nowMillis: Long): ToneCard {
        val best = max(card.bestScore, score)
        if (score < PASS_SCORE) {
            return card.copy(
                reps = 0,
                lapses = card.lapses + 1,
                intervalDays = 0,
                ease = max(MIN_EASE, card.ease - 0.2),
                bestScore = best,
                dueAtMillis = nowMillis + RELEARN_MS,
                lastReviewedAt = nowMillis,
            )
        }
        val newReps = card.reps + 1
        val newInterval = when (card.reps) {
            0 -> 1
            1 -> 3
            else -> max(1L, (card.intervalDays * card.ease).roundToLong()).toInt()
        }
        // Bonus de facilidad escalado por el score dentro de la banda de acierto [PASS..100].
        val quality = (score - PASS_SCORE).toDouble() / (100 - PASS_SCORE) // 0..1
        val easeDelta = 0.05 + 0.15 * quality                              // +0.05 (justo) .. +0.20 (perfecto)
        return card.copy(
            reps = newReps,
            intervalDays = newInterval,
            ease = min(MAX_EASE, card.ease + easeDelta),
            bestScore = best,
            dueAtMillis = nowMillis + newInterval * DAY_MS,
            lastReviewedAt = nowMillis,
        )
    }

    fun isDue(card: ToneCard, nowMillis: Long): Boolean = card.dueAtMillis <= nowMillis

    /** Cola de repaso: las vencidas, primero las más atrasadas. */
    fun due(cards: List<ToneCard>, nowMillis: Long): List<ToneCard> =
        cards.filter { isDue(it, nowMillis) }.sortedBy { it.dueAtMillis }
}
