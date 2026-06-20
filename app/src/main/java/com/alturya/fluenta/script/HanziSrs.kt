package com.alturya.fluenta.script

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * SRS de caracteres (local, en carril T3). Motor PURO y testeable en JVM: dada una
 * carta y el resultado del repaso, devuelve la carta reprogramada. Sin dependencias
 * de Android para que la lógica de retención —lo que de verdad mueve la aguja— tenga
 * red de seguridad de tests, no sea "decorativa".
 *
 * Diseñado sync-ready: cuando el backend exponga una cola de repaso de caracteres,
 * basta con sincronizar [HanziCard] sin reescribir el algoritmo. Variante SM-2-lite:
 * intervalos en días, factor de facilidad acotado, y un re-aprendizaje corto al fallar.
 */
data class HanziCard(
    val glyph: String,
    val romanization: String = "",
    val meaning: String = "",
    val ease: Double = 2.5,        // factor de facilidad (SM-2), acotado a [1.3, 3.0]
    val intervalDays: Int = 0,     // intervalo vigente en días
    val reps: Int = 0,             // repasos correctos consecutivos
    val lapses: Int = 0,           // veces que se falló tras haberlo aprendido
    val dueAtMillis: Long = 0L,    // epoch ms en que toca repasar (recién creada => ya vencida)
    val lastReviewedAt: Long = 0L,
)

object HanziSrs {
    const val DAY_MS = 86_400_000L
    // Al fallar, la carta vuelve pronto (mismo día) para re-aprender en caliente.
    const val RELEARN_MS = 10 * 60 * 1000L
    const val MIN_EASE = 1.3
    const val MAX_EASE = 3.0

    /** Carta nueva a partir de un glifo aprendido: vencida de inmediato (se repasa hoy). */
    fun newCard(glyph: String, romanization: String = "", meaning: String = "", nowMillis: Long): HanziCard =
        HanziCard(glyph = glyph, romanization = romanization, meaning = meaning, dueAtMillis = nowMillis)

    /**
     * Reprograma la carta tras un repaso. [remembered] = el usuario recordó/trazó bien.
     * Función pura: no toca persistencia ni reloj — el llamador pasa [nowMillis].
     */
    fun schedule(card: HanziCard, remembered: Boolean, nowMillis: Long): HanziCard {
        if (!remembered) {
            return card.copy(
                reps = 0,
                lapses = card.lapses + 1,
                intervalDays = 0,
                ease = max(MIN_EASE, card.ease - 0.2),
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
        val newEase = min(MAX_EASE, card.ease + 0.15)
        return card.copy(
            reps = newReps,
            intervalDays = newInterval,
            ease = newEase,
            dueAtMillis = nowMillis + newInterval * DAY_MS,
            lastReviewedAt = nowMillis,
        )
    }

    fun isDue(card: HanziCard, nowMillis: Long): Boolean = card.dueAtMillis <= nowMillis

    /** Cola de repaso: las vencidas, primero las más atrasadas. */
    fun due(cards: List<HanziCard>, nowMillis: Long): List<HanziCard> =
        cards.filter { isDue(it, nowMillis) }.sortedBy { it.dueAtMillis }
}
