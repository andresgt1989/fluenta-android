package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.motivationDataStore by preferencesDataStore(name = "motivation")

/** El "por qué" del usuario, capturado en onboarding. Personaliza el tono y, más
 *  adelante, el contenido. Se guarda client-side (cero fricción, sin backend). */
data class MotivationOption(val id: String, val emoji: String, val label: String)

object MotivationStore {
    val OPTIONS = listOf(
        MotivationOption("travel",  "🌍", "Viajar"),
        MotivationOption("career",  "💼", "Trabajo y carrera"),
        MotivationOption("study",   "🎓", "Estudios"),
        MotivationOption("family",  "❤️", "Familia y amigos"),
        MotivationOption("culture", "🎬", "Cultura y entretenimiento"),
        MotivationOption("fun",     "✨", "Por diversión"),
    )

    private val KEY = stringPreferencesKey("motivation_id")

    fun flow(context: Context): Flow<String?> =
        context.motivationDataStore.data.map { it[KEY] }

    suspend fun set(context: Context, id: String) {
        context.motivationDataStore.edit { it[KEY] = id }
    }
}
