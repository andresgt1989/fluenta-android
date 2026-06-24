package com.alturya.fluenta

import com.alturya.fluenta.gamification.Streak
import com.alturya.fluenta.gamification.Streak.DayState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Red de seguridad de la lógica de racha (handoff Gamificación · pantalla 1).
 * Funciones puras en JVM: si la mecánica "volver mañana" se rompe, CI falla.
 */
class StreakTest {

    @Test fun nextMilestone_picksFirstStrictlyGreater() {
        assertEquals(3, Streak.nextMilestone(0))
        assertEquals(7, Streak.nextMilestone(3))   // justo EN un hito → apunta al siguiente
        assertEquals(7, Streak.nextMilestone(5))
        assertEquals(10, Streak.nextMilestone(7))
        assertEquals(30, Streak.nextMilestone(14))
    }

    @Test fun nextMilestone_beyondYear_usesYearMultiples() {
        assertEquals(730, Streak.nextMilestone(365))
        assertEquals(730, Streak.nextMilestone(400))
    }

    @Test fun milestoneProgress_isMeasuredFromPreviousMilestone() {
        // 7→10: en 7 el progreso es 0 (acaba de cruzar 7), en 10 sería 1.
        assertEquals(0f, Streak.milestoneProgress(7), 0.001f)
        assertEquals(2f / 3f, Streak.milestoneProgress(9), 0.001f)
        // Antes del primer hito: desde 0 hacia 3.
        assertEquals(1f / 3f, Streak.milestoneProgress(1), 0.001f)
    }

    @Test fun bestStreak_keepsTheMaximum() {
        assertEquals(12, Streak.bestStreak(12, 7))
        assertEquals(8, Streak.bestStreak(5, 8))
    }

    @Test fun isMilestone_onlyTrueOnDefinedMilestones() {
        assertTrue(Streak.isMilestone(7))
        assertTrue(Streak.isMilestone(10))   // 10 está en el set de la app
        assertFalse(Streak.isMilestone(8))
        assertFalse(Streak.isMilestone(0))
    }

    @Test fun isStreakMilestone_firesOncePerMilestone() {
        // Día 7 con hito 3 ya celebrado → celebra.
        assertTrue(com.alturya.fluenta.gamification.isStreakMilestone(7, lastCelebrated = 3))
        // Mismo hito ya celebrado → no repite.
        assertFalse(com.alturya.fluenta.gamification.isStreakMilestone(7, lastCelebrated = 7))
        // Día sin hito → nunca celebra.
        assertFalse(com.alturya.fluenta.gamification.isStreakMilestone(8, lastCelebrated = 3))
    }

    @Test fun atRisk_onlyWhenStreakAliveButNoActivityToday() {
        assertTrue(Streak.atRisk(streakDays = 7, todayXp = 0))
        assertFalse(Streak.atRisk(streakDays = 7, todayXp = 15)) // ya estudió hoy
        assertFalse(Streak.atRisk(streakDays = 0, todayXp = 0))  // sin racha que arriesgar
    }

    @Test fun weekTracker_marksTodayAndPastDaysCoveredByStreak() {
        // Miércoles (idx 2), racha 3, ya estudió hoy → L,M activos; X = HOY (activo).
        val w = Streak.weekTracker(todayIdx = 2, streakDays = 3, studiedToday = true)
        assertEquals(DayState.ACTIVE, w[0]) // Lun
        assertEquals(DayState.ACTIVE, w[1]) // Mar
        assertEquals(DayState.ACTIVE, w[2]) // Mié (hoy, ya estudió)
        assertEquals(DayState.FUTURE, w[3]) // Jue
        assertEquals(DayState.FUTURE, w[6]) // Dom
    }

    @Test fun weekTracker_todayPendingIsTodayState_andShortStreakLeavesGaps() {
        // Miércoles (idx 2), racha 1 sin estudiar aún hoy → la racha cubre AYER (Mar),
        // hoy queda pendiente (TODAY), Lunes vacío.
        val w = Streak.weekTracker(todayIdx = 2, streakDays = 1, studiedToday = false)
        assertEquals(DayState.EMPTY, w[0])  // Lun
        assertEquals(DayState.ACTIVE, w[1]) // Mar (ayer, cubierto por la racha)
        assertEquals(DayState.TODAY, w[2])  // Mié (hoy, aún pendiente)
    }
}
