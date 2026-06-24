package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Tope de frecuencia del feedback in-app (handoff "Fluenta Feedback", sección
// "Datos & comportamiento"):
//   - Tras opinar en una pantalla, NO se vuelve a preguntar en ~72 h.
//   - El CSAT 0-10 se pide como máximo 1 vez cada 7 días.
// Persistimos el timestamp del último envío por pantalla (y del último CSAT) en
// DataStore para que el tope sobreviva entre sesiones (un `remember` en memoria
// no basta: al reabrir la app volvería a preguntar y abrumaría).
private val Context.feedbackDs by preferencesDataStore(name = "fluenta_feedback")

object FeedbackStore {
    private const val SCREEN_COOLDOWN_MS = 72L * 60 * 60 * 1000   // 72 h
    private const val CSAT_COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000 // 7 días
    private const val CSAT_KEY = "csat_last"

    // ¿Se puede pedir feedback en esta pantalla? false si se opinó hace < 72 h.
    suspend fun canAskScreen(context: Context, screen: String): Boolean {
        val last = context.feedbackDs.data.first()[longPreferencesKey("screen_$screen")] ?: 0L
        return System.currentTimeMillis() - last >= SCREEN_COOLDOWN_MS
    }

    suspend fun markScreen(context: Context, screen: String) {
        context.feedbackDs.edit { it[longPreferencesKey("screen_$screen")] = System.currentTimeMillis() }
    }

    // ¿Se puede pedir el CSAT 0-10? false si se respondió hace < 7 días.
    suspend fun canAskCsat(context: Context): Boolean {
        val last = context.feedbackDs.data.first()[longPreferencesKey(CSAT_KEY)] ?: 0L
        return System.currentTimeMillis() - last >= CSAT_COOLDOWN_MS
    }

    suspend fun markCsat(context: Context) {
        context.feedbackDs.edit { it[longPreferencesKey(CSAT_KEY)] = System.currentTimeMillis() }
    }
}
