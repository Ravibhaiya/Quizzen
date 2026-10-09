package com.ravibhaiya.quizzen.data

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ravibhaiya.quizzen.domain.PracticeConfig
import kotlinx.coroutines.flow.first

/**
 * The settings each quiz was last started with, so its setup screen opens the way the user left it. Nothing here is
 * about a running quiz: a quiz itself always starts fresh.
 *
 * `load*` returns `null` when nothing was saved yet (or the saved text is unreadable); the screen then uses its defaults.
 * [defaultTimer] is what a missing or invalid saved timer falls back to.
 */
interface QuizSettingsRepository {
    suspend fun loadMultiply(defaultTimer: Int): PracticeConfig.Multiply?
    suspend fun saveMultiply(config: PracticeConfig.Multiply)

    suspend fun loadTables(defaultTimer: Int): PracticeConfig.Tables?
    suspend fun saveTables(config: PracticeConfig.Tables)

    suspend fun loadPowersRoots(defaultTimer: Int): PracticeConfig.PowersRoots?
    suspend fun savePowersRoots(config: PracticeConfig.PowersRoots)

    suspend fun loadAlphabet(defaultTimer: Int): PracticeConfig.Alphabet?
    suspend fun saveAlphabet(config: PracticeConfig.Alphabet)

    suspend fun loadFractions(defaultTimer: Int): PracticeConfig.Fractions?
    suspend fun saveFractions(config: PracticeConfig.Fractions)
}

class DataStoreQuizSettingsRepository(context: Context) : QuizSettingsRepository {

    private val dataStore = context.applicationContext.quizzenDataStore

    override suspend fun loadMultiply(defaultTimer: Int) =
        SettingsCodec.decodeMultiply(read(MULTIPLY_KEY), defaultTimer)

    override suspend fun saveMultiply(config: PracticeConfig.Multiply) = write(MULTIPLY_KEY, SettingsCodec.encode(config))

    override suspend fun loadTables(defaultTimer: Int) =
        SettingsCodec.decodeTables(read(TABLES_KEY), defaultTimer)

    override suspend fun saveTables(config: PracticeConfig.Tables) = write(TABLES_KEY, SettingsCodec.encode(config))

    override suspend fun loadPowersRoots(defaultTimer: Int) =
        SettingsCodec.decodePowersRoots(read(POWERS_KEY), defaultTimer)

    override suspend fun savePowersRoots(config: PracticeConfig.PowersRoots) =
        write(POWERS_KEY, SettingsCodec.encode(config))

    override suspend fun loadAlphabet(defaultTimer: Int) =
        SettingsCodec.decodeAlphabet(read(ALPHABET_KEY), defaultTimer)

    override suspend fun saveAlphabet(config: PracticeConfig.Alphabet) = write(ALPHABET_KEY, SettingsCodec.encode(config))

    override suspend fun loadFractions(defaultTimer: Int) =
        SettingsCodec.decodeFractions(read(FRACTIONS_KEY), defaultTimer)

    override suspend fun saveFractions(config: PracticeConfig.Fractions) = write(FRACTIONS_KEY, SettingsCodec.encode(config))

    private suspend fun read(key: Preferences.Key<String>): String? =
        runCatching { dataStore.data.first()[key] }.getOrNull() // a damaged file just means "nothing saved"

    private suspend fun write(key: Preferences.Key<String>, value: String) {
        runCatching { dataStore.edit { it[key] = value } } // losing a saved setting must never crash the app
    }

    private companion object {
        val MULTIPLY_KEY = stringPreferencesKey("quiz_multiply")
        val TABLES_KEY = stringPreferencesKey("quiz_tables")
        val POWERS_KEY = stringPreferencesKey("quiz_powers_roots")
        val ALPHABET_KEY = stringPreferencesKey("quiz_alphabet")
        val FRACTIONS_KEY = stringPreferencesKey("quiz_fractions")
    }
}

/** The repository for ViewModels created through `viewModelFactory { initializer { ... } }`. */
internal fun CreationExtras.quizSettingsRepository(): QuizSettingsRepository =
    DataStoreQuizSettingsRepository(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
