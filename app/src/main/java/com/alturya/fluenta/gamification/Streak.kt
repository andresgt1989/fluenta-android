package com.alturya.fluenta.gamification

/**
 * Lógica PURA de rachas (sin dependencias de Android) — el núcleo de la mecánica
 * "volver mañana" del handoff de gamificación (Fluenta Gamificación.dc.html · P1).
 *
 * Regla de oro: SOLO usamos señales reales del backend (`streakDays`, `todayXp`).
 * El backend ya evalúa la racha a medianoche local; aquí no inventamos historial,
 * solo derivamos honestamente lo que se puede mostrar a partir de la racha actual.
 */
object Streak {

    /** Hitos de racha en días (ordenados). El diseño celebra estos saltos. */
    val MILESTONES = listOf(3, 7, 10, 14, 30, 50, 100, 180, 365)

    /** Próximo hito a alcanzar: el primer hito estrictamente mayor que la racha
     *  actual. Pasados los 365 días seguimos en múltiplos de un año. */
    fun nextMilestone(streakDays: Int): Int =
        MILESTONES.firstOrNull { it > streakDays } ?: (((streakDays / 365) + 1) * 365)

    /** Progreso 0..1 hacia el próximo hito, midiendo desde el hito anterior. */
    fun milestoneProgress(streakDays: Int): Float {
        val next = nextMilestone(streakDays)
        val prev = MILESTONES.lastOrNull { it <= streakDays }?.takeIf { it < next } ?: 0
        val span = (next - prev).coerceAtLeast(1)
        return ((streakDays - prev).toFloat() / span).coerceIn(0f, 1f)
    }

    /** Mejor racha = máximo entre la guardada localmente y la actual. */
    fun bestStreak(saved: Int, current: Int): Int = maxOf(saved, current)

    /** ¿La racha está EN RIESGO hoy? Racha viva pero aún sin actividad hoy →
     *  dispara el chip coral y el recordatorio push del atardecer. */
    fun atRisk(streakDays: Int, todayXp: Int): Boolean = streakDays > 0 && todayXp <= 0

    /** Estado visual de cada casilla del rastreador semanal. */
    enum class DayState { ACTIVE, TODAY, FUTURE, EMPTY }

    /**
     * Rastreador semanal honesto (Lun..Dom).
     * @param todayIdx 0=Lun … 6=Dom.
     * @param streakDays racha actual.
     * @param studiedToday si ya hubo XP hoy (la racha de hoy ya cuenta).
     *
     * Marca como ACTIVE los días previos de ESTA semana que la racha alcanza a
     * cubrir; HOY es TODAY (o ACTIVE si ya estudió); el futuro queda vacío.
     */
    fun weekTracker(todayIdx: Int, streakDays: Int, studiedToday: Boolean): List<DayState> {
        val idx = todayIdx.coerceIn(0, 6)
        // Días de racha aplicables a días ANTERIORES a hoy (sin contar hoy).
        val streakBeforeToday = if (studiedToday) streakDays - 1 else streakDays
        return (0..6).map { i ->
            when {
                i == idx -> if (studiedToday) DayState.ACTIVE else DayState.TODAY
                i > idx -> DayState.FUTURE
                else -> if ((idx - i) <= streakBeforeToday) DayState.ACTIVE else DayState.EMPTY
            }
        }
    }
}
