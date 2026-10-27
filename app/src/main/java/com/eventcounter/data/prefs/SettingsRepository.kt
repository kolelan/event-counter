package com.eventcounter.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eventcounter.domain.AppSettings
import com.eventcounter.domain.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            AppSettings(
                viewMode = prefs[Keys.VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
                    ?: ViewMode.LIST,
                aiApiKey = prefs[Keys.AI_API_KEY].orEmpty(),
                aiBaseUrl = prefs[Keys.AI_BASE_URL].orEmpty(),
            )
        }

    suspend fun setViewMode(mode: ViewMode) {
        dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setAiApiKey(value: String) {
        dataStore.edit { it[Keys.AI_API_KEY] = value }
    }

    suspend fun setAiBaseUrl(value: String) {
        dataStore.edit { it[Keys.AI_BASE_URL] = value }
    }

    private object Keys {
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
    }
}
