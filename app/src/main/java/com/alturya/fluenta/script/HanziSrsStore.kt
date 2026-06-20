package com.alturya.fluenta.script

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alturya.fluenta.network.ScriptItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.hanziSrsDataStore by preferencesDataStore(name = "hanzi_srs")

/**
 * Persistencia local del SRS de caracteres. Guarda las [HanziCard] por idioma (l2)
 * como JSON en DataStore. El algoritmo vive en [HanziSrs] (puro); aquí solo está la
 * E/S, para que la lógica siga siendo testeable sin Android.
 *
 * Sync-ready: cuando exista endpoint backend de repaso de caracteres, este store se
 * vuelve la caché offline y se añade una capa de merge — sin tocar [HanziSrs].
 */
object HanziSrsStore {
    private val gson = Gson()
    private val listType = object : TypeToken<List<HanziCard>>() {}.type

    private fun key(l2: String) = stringPreferencesKey("cards_$l2")

    fun cards(context: Context, l2: String): Flow<List<HanziCard>> =
        context.hanziSrsDataStore.data.map { prefs ->
            prefs[key(l2)]?.let { runCatching { gson.fromJson<List<HanziCard>>(it, listType) }.getOrNull() } ?: emptyList()
        }

    fun due(context: Context, l2: String, nowMillis: Long): Flow<List<HanziCard>> =
        cards(context, l2).map { HanziSrs.due(it, nowMillis) }

    fun dueCount(context: Context, l2: String, nowMillis: Long): Flow<Int> =
        due(context, l2, nowMillis).map { it.size }

    /** Siembra los glifos recién aprendidos. Idempotente: no pisa cartas ya existentes. */
    suspend fun enqueue(context: Context, l2: String, items: List<ScriptItem>, nowMillis: Long = System.currentTimeMillis()) {
        if (items.isEmpty()) return
        context.hanziSrsDataStore.edit { prefs ->
            val existing = prefs[key(l2)]?.let { runCatching { gson.fromJson<List<HanziCard>>(it, listType) }.getOrNull() } ?: emptyList()
            val known = existing.map { it.glyph }.toSet()
            val additions = items
                .filter { it.glyph.isNotBlank() && it.glyph !in known }
                .map { HanziSrs.newCard(it.glyph, it.romanization, it.meaning ?: "", nowMillis) }
            if (additions.isNotEmpty()) prefs[key(l2)] = gson.toJson(existing + additions, listType)
        }
    }

    /** Registra el resultado de un repaso y reprograma la carta. */
    suspend fun grade(context: Context, l2: String, glyph: String, remembered: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        context.hanziSrsDataStore.edit { prefs ->
            val existing = prefs[key(l2)]?.let { runCatching { gson.fromJson<List<HanziCard>>(it, listType) }.getOrNull() } ?: return@edit
            val updated = existing.map { if (it.glyph == glyph) HanziSrs.schedule(it, remembered, nowMillis) else it }
            prefs[key(l2)] = gson.toJson(updated, listType)
        }
    }

    /** Solo para diagnóstico/tests instrumentados. */
    suspend fun clear(context: Context, l2: String) {
        context.hanziSrsDataStore.edit { it.remove(key(l2)) }
    }
}
