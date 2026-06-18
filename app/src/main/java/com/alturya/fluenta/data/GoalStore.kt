package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.goalDataStore by preferencesDataStore(name = "goals")

data class GoalOption(val xp: Int, val label: String, val description: String)

object GoalStore {
    val OPTIONS = listOf(
        GoalOption(25,  "5 min",  "Ligero · ideal para días ocupados"),
        GoalOption(50,  "10 min", "Regular · el hábito que funciona"),
        GoalOption(75,  "15 min", "Intensivo · progreso visible"),
        GoalOption(100, "20 min", "Máximo · habla más rápido"),
    )

    private val KEY = intPreferencesKey("daily_goal_xp")

    fun flow(context: Context): Flow<Int> =
        context.goalDataStore.data.map { it[KEY] ?: 50 }

    suspend fun set(context: Context, xp: Int) {
        context.goalDataStore.edit { it[KEY] = xp }
    }
}
