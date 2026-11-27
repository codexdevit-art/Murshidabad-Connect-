package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "murshidabad_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { ex ->
        Log.e("UserPreferences", "Preferences DataStore corrupted; resetting to defaults", ex)
        emptyPreferences()
    }
)

data class UserPrefsState(
    val language: String = "bn",
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true
)

class UserPreferences(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("preferred_language")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val prefsFlow: Flow<UserPrefsState> = context.applicationContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading user preferences; falling back to defaults", exception)
                emit(emptyPreferences())
            } else {
                Log.e(TAG, "Unexpected preferences read failure", exception)
                emit(emptyPreferences())
            }
        }
        .map { prefs ->
            UserPrefsState(
                language = prefs[Keys.LANGUAGE] ?: "bn",
                darkMode = prefs[Keys.DARK_MODE] ?: false,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
            )
        }

    suspend fun setLanguage(lang: String) {
        runCatching {
            context.applicationContext.dataStore.edit { it[Keys.LANGUAGE] = lang }
        }.onFailure { e ->
            Log.e(TAG, "Failed to save language preference", e)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        runCatching {
            context.applicationContext.dataStore.edit { it[Keys.DARK_MODE] = enabled }
        }.onFailure { e ->
            Log.e(TAG, "Failed to save dark mode preference", e)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        runCatching {
            context.applicationContext.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
        }.onFailure { e ->
            Log.e(TAG, "Failed to save notifications preference", e)
        }
    }

    companion object {
        private const val TAG = "UserPreferences"
    }
}
