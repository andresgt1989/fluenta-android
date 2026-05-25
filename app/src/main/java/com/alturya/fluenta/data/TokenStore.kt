package com.alturya.fluenta.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fluenta_prefs")

object TokenStore {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val PHONE_KEY = stringPreferencesKey("phone")

    fun getToken(context: Context): Flow<String?> =
        context.dataStore.data.map { it[TOKEN_KEY] }

    fun getPhone(context: Context): Flow<String?> =
        context.dataStore.data.map { it[PHONE_KEY] }

    suspend fun save(context: Context, token: String, phone: String) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            it[PHONE_KEY] = phone
        }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}