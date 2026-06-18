package com.alturya.fluenta.home

import com.alturya.fluenta.network.UserProgress

/**
 * Misiones diarias (estilo "daily quests" de Duolingo) — el motor de retención
 * client-side. TODAS se derivan de señales REALES del backend (`UserProgress`),
 * nunca de datos inventados: el backend resetea `todayXp`/`cardsDueToday` cada día,
 * así que las misiones se reinician solas sin lógica de fechas en el cliente.
 *
 * Tres ejes distintos, escalera pedagógica de hábito:
 *  1. Consistencia — "preséntate hoy" (protege la racha).
 *  2. Volumen      — "alcanza tu meta diaria".
 *  3. Retención    — "despeja tu repaso espaciado" (lo de mayor valor a largo plazo).
 */
enum class MissionAction { Lesson, Goal, Review }

data class DailyMission(
    val id: String,
    val title: String,
    val emoji: String,
    val current: Int,
    val target: Int,
    val action: MissionAction,
) {
    val done: Boolean get() = current >= target
    val pct: Int get() = if (target <= 0) 100 else ((current.toLong() * 100 / target).toInt()).coerceIn(0, 100)
}

object DailyMissions {

    /** Construye las misiones del día a partir del progreso real + la meta local del usuario. */
    fun build(
        progress: UserProgress?,
        goalXp: Int,
        titleStreak: String,
        titleGoal: String,
        titleReview: String,
    ): List<DailyMission> {
        if (progress == null) return emptyList()
        val today = progress.todayXp.coerceAtLeast(0)
        val goal = goalXp.coerceAtLeast(1)
        val due = progress.cardsDueToday.coerceAtLeast(0)
        return listOf(
            // Preséntate: con practicar (cualquier XP) ya proteges la racha.
            DailyMission("streak", titleStreak, "🔥", current = if (today > 0) 1 else 0, target = 1, action = MissionAction.Lesson),
            // Meta diaria: volumen real de XP de hoy contra la meta elegida.
            DailyMission("goal", titleGoal, "🎯", current = today.coerceAtMost(goal), target = goal, action = MissionAction.Goal),
            // Repaso SRS: cumplido cuando no quedan tarjetas vencidas hoy.
            DailyMission("review", titleReview, "🧠", current = if (due == 0) 1 else 0, target = 1, action = MissionAction.Review),
        )
    }

    fun completedCount(missions: List<DailyMission>): Int = missions.count { it.done }

    fun allDone(missions: List<DailyMission>): Boolean = missions.isNotEmpty() && missions.all { it.done }
}
