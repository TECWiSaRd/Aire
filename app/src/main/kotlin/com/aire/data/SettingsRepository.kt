package com.aire.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages user settings for Aire, including AI configuration and appearance.
 */
class SettingsRepository(private val context: Context) {

    private val KEY_API_KEY = stringPreferencesKey("anthropic_api_key")
    private val KEY_MODEL = stringPreferencesKey("ai_model")
    private val KEY_APPEARANCE = stringPreferencesKey("appearance")

    val anthropicApiKey: Flow<String?> = context.dataStore.data.map { it[KEY_API_KEY] }
    val aiModel: Flow<String> = context.dataStore.data.map { it[KEY_MODEL] ?: "claude-3-5-haiku-latest" }
    val appearance: Flow<String> = context.dataStore.data.map { it[KEY_APPEARANCE] ?: "System" }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setModel(model: String) {
        context.dataStore.edit { it[KEY_MODEL] = model }
    }

    suspend fun setAppearance(appearance: String) {
        context.dataStore.edit { it[KEY_APPEARANCE] = appearance }
    }
}
