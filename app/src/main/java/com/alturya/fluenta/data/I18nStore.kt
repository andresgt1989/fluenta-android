package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alturya.fluenta.network.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.i18nDataStore by preferencesDataStore(name = "fluenta_i18n")

/**
 * Local cache of UI strings keyed by interface language.
 * Strings come from `GET /api/i18n/ui?lang=xx` (backend translates once, caches 30d in Redis).
 * App caches the JSON map locally 24h; refresh on app start.
 *
 * Usage: `I18nStore.get(context, "home.greeting", lang = "hi")` → "नमस्ते 👋"
 * Falls back to the key itself if not loaded yet (fail-safe for cold start).
 */
object I18nStore {
    private val LANG_KEY = stringPreferencesKey("interface_lang")
    private val STRINGS_KEY = stringPreferencesKey("strings_json")
    private val FETCHED_AT_KEY = stringPreferencesKey("fetched_at_ms")
    private val TTL_MS = 24L * 60L * 60L * 1000L

    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    @Volatile
    private var memCache: Map<String, String> = emptyMap()
    @Volatile
    private var memLang: String = "es"

    fun currentLangFlow(context: Context): Flow<String> =
        context.i18nDataStore.data.map { it[LANG_KEY] ?: "es" }

    suspend fun setLang(context: Context, lang: String) {
        context.i18nDataStore.edit { it[LANG_KEY] = lang }
    }

    /** Read a string by key. Returns the key itself if not yet loaded. */
    fun t(key: String, fallback: String = key): String =
        memCache[key] ?: fallback

    /**
     * Load strings for `lang` from cache if fresh, otherwise refetch from backend.
     * Always populates memCache, even on network error (with whatever is cached).
     */
    suspend fun ensureLoaded(context: Context, lang: String) {
        if (memLang == lang && memCache.isNotEmpty()) return

        // Read from DataStore
        val prefs = context.i18nDataStore.data.first()
        val cachedLang = prefs[LANG_KEY]
        val cachedJson = prefs[STRINGS_KEY]
        val fetchedAt = prefs[FETCHED_AT_KEY]?.toLongOrNull() ?: 0L
        val now = System.currentTimeMillis()
        val fresh = (now - fetchedAt) < TTL_MS

        if (cachedLang == lang && cachedJson != null && fresh) {
            runCatching {
                memCache = gson.fromJson(cachedJson, mapType) ?: emptyMap()
                memLang = lang
            }
            return
        }

        // Refetch from backend
        runCatching {
            val resp = ApiClient.api.getUiStrings(lang)
            val strings = resp.strings ?: emptyMap()
            memCache = strings
            memLang = lang
            val json = gson.toJson(strings)
            context.i18nDataStore.edit {
                it[LANG_KEY] = lang
                it[STRINGS_KEY] = json
                it[FETCHED_AT_KEY] = now.toString()
            }
        }.onFailure {
            // Network failed; use stale cache if available so UI isn't broken
            if (cachedJson != null) {
                runCatching {
                    memCache = gson.fromJson(cachedJson, mapType) ?: emptyMap()
                    memLang = cachedLang ?: lang
                }
            }
        }
    }
}
