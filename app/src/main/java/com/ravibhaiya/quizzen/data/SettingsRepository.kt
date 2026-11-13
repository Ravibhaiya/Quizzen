package com.ravibhaiya.quizzen.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ravibhaiya.quizzen.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SettingsRepository {
    val hapticEnabled: Flow<Boolean>
    suspend fun setHapticEnabled(enabled: Boolean)

    val geminiApiKey: Flow<String>
        get() = flowOf("")

    suspend fun setGeminiApiKey(apiKey: String) {}
}

class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val dataStore = context.applicationContext.quizzenDataStore

    override val hapticEnabled: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[HAPTIC_KEY] ?: true }

    override suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_KEY] = enabled }
    }

    override val geminiApiKey: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val stored = sanitizeApiKey(prefs[GEMINI_API_KEY].orEmpty())
            latestCachedApiKey = stored
            stored
        }

    override suspend fun setGeminiApiKey(apiKey: String) {
        val cleaned = sanitizeApiKey(apiKey)
        latestCachedApiKey = cleaned
        dataStore.edit { prefs ->
            if (cleaned.isEmpty()) {
                prefs.remove(GEMINI_API_KEY)
            } else {
                prefs[GEMINI_API_KEY] = cleaned
            }
        }
    }

    companion object {
        private val HAPTIC_KEY = booleanPreferencesKey("haptic_enabled")
        private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private const val PLACEHOLDER_KEY = "YOUR_GEMINI_API_KEY"
        private const val INVALID_CONTAINER_KEY_SHA256 =
            "1238bfb6c827dbce27869dda4886e148174fd475baaaac03ff41242e45dc0f7b"

        @Volatile
        var latestCachedApiKey: String = ""
            internal set

        private fun isInvalidContainerPlaceholder(key: String): Boolean {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(key.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            return digest == INVALID_CONTAINER_KEY_SHA256
        }

        fun sanitizeApiKey(raw: String): String {
            var s = raw.trim()
                .removeSurrounding("\"")
                .removeSurrounding("'")
                .trim()
            if (s.startsWith("x-goog-api-key:", ignoreCase = true)) {
                s = s.substringAfter(":").trim()
            } else if (s.startsWith("Bearer ", ignoreCase = true)) {
                s = s.substring(7).trim()
            }
            s = s.replace("\\s+".toRegex(), "")
            if (s.isEmpty() || s == PLACEHOLDER_KEY || isInvalidContainerPlaceholder(s)) {
                return ""
            }
            return s
        }

        fun effectiveGeminiApiKey(storedKey: String = latestCachedApiKey): String {
            val fromSettings = sanitizeApiKey(storedKey)
            if (fromSettings.isNotEmpty()) {
                return fromSettings
            }
            val fromBuildConfig = sanitizeApiKey(BuildConfig.GEMINI_API_KEY)
            if (fromBuildConfig.isNotEmpty()) {
                return fromBuildConfig
            }
            return ""
        }
    }
}
