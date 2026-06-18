package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * User preferences for sound, haptics and reminders. Defaults to ON so the app
 * feels alive out of the box; the user can silence it from the Settings screen.
 */
object SettingsStore {
    private val SFX_KEY = booleanPreferencesKey("sfx_enabled")
    private val HAPTICS_KEY = booleanPreferencesKey("haptics_enabled")
    private val REMINDERS_KEY = booleanPreferencesKey("reminders_enabled")

    fun sfxEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[SFX_KEY] ?: true }

    fun hapticsEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[HAPTICS_KEY] ?: true }

    fun remindersEnabled(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[REMINDERS_KEY] ?: true }

    suspend fun setSfx(context: Context, on: Boolean) =
        context.settingsDataStore.edit { it[SFX_KEY] = on }

    suspend fun setHaptics(context: Context, on: Boolean) =
        context.settingsDataStore.edit { it[HAPTICS_KEY] = on }

    suspend fun setReminders(context: Context, on: Boolean) =
        context.settingsDataStore.edit { it[REMINDERS_KEY] = on }
}
