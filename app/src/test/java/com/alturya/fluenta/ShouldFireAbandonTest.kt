package com.alturya.fluenta

import com.alturya.fluenta.lesson.LessonPlayerState
import com.alturya.fluenta.lesson.shouldFireAbandon
import com.alturya.fluenta.network.LessonSubmitResponse
import com.alturya.fluenta.network.PlayableExercise
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Red de seguridad de `leccion_abandonada` (evento de retención de T3). Garantiza
 * que el abandono se emite SOLO cuando la lección estaba en curso y no se emitió
 * ya otro evento de salida — es decir, exclusión mutua con leccion_completada y
 * un único disparo por lección. Si esta lógica se rompe, la tasa de abandono se
 * contaría de más (falsos abandonos al cargar/rotar) o de menos. Función pura.
 */
class ShouldFireAbandonTest {

    private fun ex(i: Int) = PlayableExercise(index = i, kind = "multiple_choice")

    private fun inProgress() = LessonPlayerState(
        loading = false,
        exercises = listOf(ex(0), ex(1)),
        currentIndex = 1,
        result = null,
    )

    @Test fun fires_when_in_progress_and_not_yet_tracked() {
        assertTrue(shouldFireAbandon(inProgress(), alreadyTracked = false))
    }

    @Test fun does_not_fire_if_an_outcome_was_already_tracked() {
        // Ya se emitió complete o abandon antes: nunca un segundo evento.
        assertFalse(shouldFireAbandon(inProgress(), alreadyTracked = true))
    }

    @Test fun does_not_fire_when_lesson_already_completed() {
        val completed = inProgress().copy(
            result = LessonSubmitResponse(
                correctCount = 2, total = 2, scorePct = 100, passed = true,
                xpEarned = 10, results = emptyList(), newStreakDays = 1,
            ),
        )
        assertFalse(shouldFireAbandon(completed, alreadyTracked = false))
    }

    @Test fun does_not_fire_while_still_loading() {
        assertFalse(shouldFireAbandon(LessonPlayerState(loading = true), alreadyTracked = false))
    }

    @Test fun does_not_fire_before_any_exercise_is_loaded() {
        val empty = LessonPlayerState(loading = false, exercises = emptyList(), result = null)
        assertFalse(shouldFireAbandon(empty, alreadyTracked = false))
    }
}
