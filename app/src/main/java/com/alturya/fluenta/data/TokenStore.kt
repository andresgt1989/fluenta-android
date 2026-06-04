package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fluenta_prefs")

object TokenStore {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val PHONE_KEY = stringPreferencesKey("phone")
    private val FCM_KEY = stringPreferencesKey("fcm_token")
    private val ONBOARDING_DONE_KEY = booleanPreferencesKey("onboarding_done")
    private val CHOSEN_L1_KEY = stringPreferencesKey("chosen_l1")
    private val CHOSEN_L2_KEY = stringPreferencesKey("chosen_l2")

    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[TOKEN_KEY] }

    // Play-first onboarding: whether the user has seen the first-run flow, and the
    // language pair they picked there (applied to their account right after login).
    fun getOnboardingDone(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_DONE_KEY] ?: false }

    fun getChosenL2(context: Context): Flow<String?> =
        context.dataStore.data.map { it[CHOSEN_L2_KEY] }

    // Atomic first-run routing read: (authToken, onboardingDone) from a single
    // DataStore emission, so the start-destination decision has no token/flag race.
    fun getStartState(context: Context): Flow<Pair<String?, Boolean>> =
        context.dataStore.data.map { it[TOKEN_KEY] to (it[ONBOARDING_DONE_KEY] ?: false) }

    suspend fun saveOnboardingChoice(context: Context, l1: String, l2: String) {
        context.dataStore.edit {
            it[CHOSEN_L1_KEY] = l1
            it[CHOSEN_L2_KEY] = l2
            it[ONBOARDING_DONE_KEY] = true
        }
    }

    fun getPhone(context: Context): Flow<String?> =
        context.dataStore.data.map { it[PHONE_KEY] }

    fun getFcmToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[FCM_KEY] }

    suspend fun save(context: Context, token: String, phone: String) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            it[PHONE_KEY] = phone
        }
    }

    suspend fun saveFcmToken(context: Context, fcmToken: String) {
        context.dataStore.edit { it[FCM_KEY] = fcmToken }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}