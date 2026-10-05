package com.ravibhaiya.quizzen.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SettingsRepository {
    val hapticEnabled: Flow<Boolean>
    suspend fun setHapticEnabled(enabled: Boolean)
}

class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val dataStore = context.applicationContext.quizzenDataStore

    override val hapticEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[HAPTIC_KEY] ?: true }

    override suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_KEY] = enabled }
    }

    private companion object {
        val HAPTIC_KEY = booleanPreferencesKey("haptic_enabled")
    }
}
