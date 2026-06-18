package com.alturya.fluenta

import com.alturya.fluenta.home.DailyMissions
import com.alturya.fluenta.home.MissionAction
import com.alturya.fluenta.network.UserProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Red de seguridad de las misiones diarias (motor de retención). Funciones puras
 * en JVM: verifican que las misiones se derivan correctamente de señales REALES
 * del backend, sin datos falsos. Si la lógica de progreso se rompe, CI falla.
 */
class DailyMissionsTest {

    private fun progress(todayXp: Int = 0, cardsDueToday: Int = 0) = UserProgress(
        streakDays = 3,
        totalXp = 500,
        completedLessons = 10,
        todayXp = todayXp,
        dailyGoalXp = 50,
        dailyGoalPct = 0,
        cardsDueToday = cardsDueToday,
        l1 = "es",
        l2 = "en",
        level = "A2",
        levelSystem = "cefr",
    )

    private fun build(p: UserProgress?, goalXp: Int = 50) =
        DailyMissions.build(p, goalXp, "racha", "meta", "repaso")

    @Test fun null_progress_yields_no_missions() {
        assertTrue(build(null).isEmpty())
    }

    @Test fun fresh_day_nothing_done_except_clear_review() {
        // Sin XP hoy y sin tarjetas vencidas: racha y meta pendientes, repaso ya limpio.
        val m = build(progress(todayXp = 0, cardsDueToday = 0))
        assertEquals(3, m.size)
        val byId = m.associateBy { it.id }
        assertFalse(byId.getValue("streak").done)
        assertFalse(byId.getValue("goal").done)
        assertTrue(byId.getValue("review").done)   // 0 vencidas = despejado
        assertEquals(1, DailyMissions.completedCount(m))
        assertFalse(DailyMissions.allDone(m))
    }

    @Test fun any_xp_protects_streak_mission() {
        val m = build(progress(todayXp = 5)).associateBy { it.id }
        assertTrue(m.getValue("streak").done)        // cualquier XP > 0
        assertFalse(m.getValue("goal").done)         // 5 de 50 aún no
        assertEquals(10, m.getValue("goal").pct)
    }

    @Test fun goal_caps_at_target_and_100pct() {
        val m = build(progress(todayXp = 80), goalXp = 50).associateBy { it.id }
        val goal = m.getValue("goal")
        assertTrue(goal.done)
        assertEquals(50, goal.current)               // se topa en la meta
        assertEquals(100, goal.pct)
    }

    @Test fun pending_review_is_not_done() {
        val m = build(progress(cardsDueToday = 7)).associateBy { it.id }
        assertFalse(m.getValue("review").done)
        assertEquals(MissionAction.Review, m.getValue("review").action)
    }

    @Test fun all_done_when_goal_hit_and_review_clear() {
        val m = build(progress(todayXp = 60, cardsDueToday = 0))
        assertTrue(DailyMissions.allDone(m))
        assertEquals(3, DailyMissions.completedCount(m))
    }

    @Test fun zero_goal_does_not_crash_pct() {
        val m = build(progress(todayXp = 10), goalXp = 0).associateBy { it.id }
        // target se fuerza a >=1, así que no hay división por cero.
        assertEquals(100, m.getValue("goal").pct)
    }
}
