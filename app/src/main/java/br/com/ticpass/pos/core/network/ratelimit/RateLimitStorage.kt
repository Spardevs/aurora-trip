package br.com.ticpass.pos.core.network.ratelimit

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rate_limits")

class RateLimitStorage(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getPolicy(key: String): RateLimitPolicy? {
        val preferences = context.dataStore.data.firstOrNull() ?: return null
        val policyString = preferences[stringPreferencesKey(key)]
        return policyString?.let { json.decodeFromString(it) }
    }

    suspend fun savePolicy(key: String, policy: RateLimitPolicy) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = json.encodeToString(policy)
        }
    }

    suspend fun getLastRequestTime(key: String): Long {
        val preferences = context.dataStore.data.firstOrNull() ?: return 0L
        return preferences[longPreferencesKey("${key}_last_request")] ?: 0L
    }

    suspend fun saveLastRequestTime(key: String, timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[longPreferencesKey("${key}_last_request")] = timestamp
        }
    }
}