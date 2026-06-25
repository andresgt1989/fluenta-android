package com.alturya.fluenta.tone

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.toneSrsDataStore by preferencesDataStore(name = "tone_srs")

/**
 * Persistencia local del SRS de tono. Guarda las [ToneCard] por idioma (l2) como JSON
 * en DataStore. El algoritmo vive en [ToneSrs] (puro); aquí solo la E/S. Espeja
 * [com.alturya.fluenta.script.HanziSrsStore]. Sync-ready para cuando el backend exponga
 * cola de repaso de tono.
 */
object ToneSrsStore {
    private val gson = Gson()
    private val listType = object : TypeToken<List<ToneCard>>() {}.type

    private fun key(l2: String) = stringPreferencesKey("tone_cards_$l2")

    fun cards(context: Context, l2: String): Flow<List<ToneCard>> =
        context.toneSrsDataStore.data.map { prefs ->
            prefs[key(l2)]?.let { runCatching { gson.fromJson<List<ToneCard>>(it, listType) }.getOrNull() } ?: emptyList()
        }

    fun due(context: Context, l2: String, nowMillis: Long): Flow<List<ToneCard>> =
        cards(context, l2).map { ToneSrs.due(it, nowMillis) }

    fun dueCount(context: Context, l2: String, nowMillis: Long): Flow<Int> =
        due(context, l2, nowMillis).map { it.size }

    /** Siembra las palabras practicadas. Idempotente: no pisa cartas ya existentes. */
    suspend fun enqueue(context: Context, l2: String, words: List<ToneWord>, nowMillis: Long = System.currentTimeMillis()) {
        if (words.isEmpty()) return
        context.toneSrsDataStore.edit { prefs ->
            val existing = prefs[key(l2)]?.let { runCatching { gson.fromJson<List<ToneCard>>(it, listType) }.getOrNull() } ?: emptyList()
            val known = existing.map { it.key }.toSet()
            val additions = words
                .map { ToneSrs.keyOf(it) to it }
                .filter { it.first !in known }
                .distinctBy { it.first }
                .map { ToneSrs.newCard(it.second, nowMillis) }
            if (additions.isNotEmpty()) prefs[key(l2)] = gson.toJson(existing + additions, listType)
        }
    }

    /** Registra el score de voz de una palabra y reprograma su carta. */
    suspend fun grade(context: Context, l2: String, cardKey: String, score: Int, nowMillis: Long = System.currentTimeMillis()) {
        context.toneSrsDataStore.edit { prefs ->
            val existing = prefs[key(l2)]?.let { runCatching { gson.fromJson<List<ToneCard>>(it, listType) }.getOrNull() } ?: return@edit
            val updated = existing.map { if (it.key == cardKey) ToneSrs.schedule(it, score, nowMillis) else it }
            prefs[key(l2)] = gson.toJson(updated, listType)
        }
    }

    /** Solo para diagnóstico/tests instrumentados. */
    suspend fun clear(context: Context, l2: String) {
        context.toneSrsDataStore.edit { it.remove(key(l2)) }
    }
}
