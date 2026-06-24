package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alturya.fluenta.gamification.Streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.streakDataStore by preferencesDataStore(name = "streak")

/**
 * Estado de racha que el backend AÚN no expone: mejor racha histórica y escudos
 * de racha. Se guarda client-side (cero fricción, sin backend), igual que
 * [GoalStore]/[MotivationStore]. La racha "viva" sigue viniendo del backend
 * (`UserProgress.streakDays`); aquí solo persistimos el récord y los escudos.
 */
object StreakStore {
    private val BEST = intPreferencesKey("best_streak")
    private val SHIELDS = intPreferencesKey("streak_shields")
    private val CELEBRATED = intPreferencesKey("celebrated_streak")

    /** Mejor racha registrada (el récord que el usuario intenta batir). */
    fun best(context: Context): Flow<Int> =
        context.streakDataStore.data.map { it[BEST] ?: 0 }

    /** Escudos disponibles. Por defecto 1 (perdona un día perdido). */
    fun shields(context: Context): Flow<Int> =
        context.streakDataStore.data.map { it[SHIELDS] ?: 1 }

    /** Registra la racha actual: actualiza el récord si la supera. Idempotente. */
    suspend fun record(context: Context, currentStreak: Int) {
        context.streakDataStore.edit {
            val saved = it[BEST] ?: 0
            it[BEST] = Streak.bestStreak(saved, currentStreak)
        }
    }

    /** Consume un escudo (al perdonar un día). No baja de 0. */
    suspend fun useShield(context: Context) {
        context.streakDataStore.edit {
            it[SHIELDS] = ((it[SHIELDS] ?: 1) - 1).coerceAtLeast(0)
        }
    }

    /** Mayor día de racha ya celebrado (para no repetir la celebración del hito). */
    suspend fun lastCelebrated(context: Context): Int =
        context.streakDataStore.data.first()[CELEBRATED] ?: 0

    suspend fun markCelebrated(context: Context, day: Int) {
        context.streakDataStore.edit { it[CELEBRATED] = day }
    }
}
