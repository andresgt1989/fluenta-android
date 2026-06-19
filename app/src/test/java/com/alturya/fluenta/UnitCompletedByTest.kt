package com.alturya.fluenta

import com.alturya.fluenta.lesson.unitCompletedBy
import com.alturya.fluenta.network.CurriculumMapResponse
import com.alturya.fluenta.network.CurriculumUnit
import com.alturya.fluenta.network.Lesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Red de seguridad de `unidad_completada` (evento de retención de T3). Decide, a
 * partir del mapa de currículo REAL del backend, si la lección recién enviada deja
 * su unidad completa. Si esta lógica se rompe, el evento de retención más caro
 * (terminar una unidad entera) se emitiría de más o de menos. Función pura en JVM.
 */
class UnitCompletedByTest {

    private fun lesson(id: String, n: Int, completed: Boolean) =
        Lesson(id = id, number = n, title = "L$n", type = "core", completed = completed)

    private fun map(vararg units: CurriculumUnit) =
        CurriculumMapResponse(map = units.toList(), l1 = "es", l2 = "en")

    private fun unit(id: String, n: Int, lessons: List<Lesson>) =
        CurriculumUnit(id = id, number = n, title = "U$n", description = null, lessons = lessons)

    @Test fun last_lesson_of_unit_completes_it() {
        val u = unit("u1", 1, listOf(
            lesson("l1", 1, true),
            lesson("l2", 2, true),
            lesson("l3", 3, true),  // la recién enviada cierra la unidad
        ))
        val res = unitCompletedBy(map(u), "l3")
        assertEquals("u1", res?.id)
    }

    @Test fun not_complete_when_a_sibling_is_pending() {
        val u = unit("u1", 1, listOf(
            lesson("l1", 1, true),
            lesson("l2", 2, false),  // todavía pendiente
            lesson("l3", 3, true),
        ))
        assertNull(unitCompletedBy(map(u), "l3"))
    }

    @Test fun lesson_not_in_any_unit_returns_null() {
        val u = unit("u1", 1, listOf(lesson("l1", 1, true)))
        assertNull(unitCompletedBy(map(u), "desconocida"))
    }

    @Test fun empty_unit_never_completes() {
        val u = unit("u1", 1, emptyList())
        assertNull(unitCompletedBy(map(u), "l1"))
    }

    @Test fun finds_the_correct_unit_among_several() {
        val u1 = unit("u1", 1, listOf(lesson("a", 1, true), lesson("b", 2, false)))
        val u2 = unit("u2", 2, listOf(lesson("c", 1, true), lesson("d", 2, true)))
        // 'd' pertenece a u2 y u2 queda completa; u1 sigue incompleta y no se toca.
        assertEquals("u2", unitCompletedBy(map(u1, u2), "d")?.id)
        assertNull(unitCompletedBy(map(u1, u2), "b"))
    }
}
