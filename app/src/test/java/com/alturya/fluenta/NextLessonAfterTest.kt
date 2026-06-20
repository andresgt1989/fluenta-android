package com.alturya.fluenta

import com.alturya.fluenta.lesson.nextLessonAfter
import com.alturya.fluenta.network.CurriculumMapResponse
import com.alturya.fluenta.network.CurriculumUnit
import com.alturya.fluenta.network.Lesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Red de seguridad de "Siguiente lección →" (lever de retención de T3). Decide, a
 * partir del mapa de currículo REAL, a qué lección llevar al usuario tras completar
 * una, para mantener el momentum. Si esto se rompe, el botón mandaría a la lección
 * equivocada o reaparecería cuando no debe. Función pura en JVM.
 */
class NextLessonAfterTest {

    private fun lesson(id: String, n: Int, completed: Boolean) =
        Lesson(id = id, number = n, title = "L$n", type = "core", completed = completed)

    private fun map(vararg units: CurriculumUnit) =
        CurriculumMapResponse(map = units.toList(), l1 = "es", l2 = "en")

    private fun unit(id: String, n: Int, lessons: List<Lesson>) =
        CurriculumUnit(id = id, number = n, title = "U$n", description = null, lessons = lessons)

    @Test fun next_is_the_following_pending_lesson_in_same_unit() {
        val u = unit("u1", 1, listOf(
            lesson("l1", 1, true),   // recién completada
            lesson("l2", 2, false),  // la siguiente pendiente
            lesson("l3", 3, false),
        ))
        assertEquals("l2", nextLessonAfter(map(u), "l1"))
    }

    @Test fun skips_already_completed_lessons_ahead() {
        val u = unit("u1", 1, listOf(
            lesson("l1", 1, true),
            lesson("l2", 2, true),   // ya hecha, se salta
            lesson("l3", 3, false),  // primera pendiente por delante
        ))
        assertEquals("l3", nextLessonAfter(map(u), "l1"))
    }

    @Test fun crosses_into_the_next_unit_when_current_unit_is_done() {
        val u1 = unit("u1", 1, listOf(lesson("a", 1, true), lesson("b", 2, true)))
        val u2 = unit("u2", 2, listOf(lesson("c", 1, false), lesson("d", 2, false)))
        // tras 'b' (última de u1, completada) la siguiente es 'c' en u2.
        assertEquals("c", nextLessonAfter(map(u1, u2), "b"))
    }

    @Test fun null_when_nothing_pending_ahead() {
        val u = unit("u1", 1, listOf(
            lesson("l1", 1, false),
            lesson("l2", 2, true),  // recién completada, nada pendiente después
        ))
        assertNull(nextLessonAfter(map(u), "l2"))
    }

    @Test fun null_when_lesson_not_in_map() {
        val u = unit("u1", 1, listOf(lesson("l1", 1, false)))
        assertNull(nextLessonAfter(map(u), "desconocida"))
    }

    @Test fun respects_unit_and_lesson_order_not_array_order() {
        // unidades y lecciones desordenadas en el array: debe ordenar por number.
        val u2 = unit("u2", 2, listOf(lesson("c", 1, false)))
        val u1 = unit("u1", 1, listOf(lesson("b", 2, false), lesson("a", 1, true)))
        // orden real: a(u1.1) -> b(u1.2) -> c(u2.1). Tras 'a', siguiente pendiente = 'b'.
        assertEquals("b", nextLessonAfter(map(u2, u1), "a"))
    }
}
