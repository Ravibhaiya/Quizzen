package com.ravibhaiya.quizzen.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ravibhaiya.quizzen.data.DataStoreSettingsRepository
import com.ravibhaiya.quizzen.data.GeminiDecisionClient
import com.ravibhaiya.quizzen.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Live status of the "Test Key" check in the Settings sheet. */
sealed interface ApiKeyTestState {
    data object Idle : ApiKeyTestState
    data object Testing : ApiKeyTestState
    data object Valid : ApiKeyTestState
    data class Invalid(val message: String) : ApiKeyTestState
}

/** App-wide settings (haptic feedback and local Gemini API key). Scoped to the Activity. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SettingsRepository = DataStoreSettingsRepository(application)
    private val decisionClient = GeminiDecisionClient(settingsRepository = repository, context = application)

    private val _apiKeyTestState = MutableStateFlow<ApiKeyTestState>(ApiKeyTestState.Idle)
    val apiKeyTestState: StateFlow<ApiKeyTestState> = _apiKeyTestState.asStateFlow()
    private var testKeyJob: Job? = null

    val hapticEnabled: StateFlow<Boolean> = repository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val geminiApiKey: StateFlow<String> = repository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, DataStoreSettingsRepository.latestCachedApiKey)

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticEnabled(enabled) }
    }

    fun setGeminiApiKey(apiKey: String) {
        clearApiKeyTestState()
        viewModelScope.launch { repository.setGeminiApiKey(apiKey) }
    }

    fun testGeminiApiKey(rawKey: String) {
        testKeyJob?.cancel()
        _apiKeyTestState.value = ApiKeyTestState.Testing
        testKeyJob = viewModelScope.launch {
            val result = decisionClient.verifyApiKey(rawKey)
            _apiKeyTestState.value = result.fold(
                onSuccess = { ApiKeyTestState.Valid },
                onFailure = { err ->
                    ApiKeyTestState.Invalid(err.message ?: "Could not verify Gemini API key.")
                },
            )
        }
    }

    fun clearApiKeyTestState() {
        testKeyJob?.cancel()
        testKeyJob = null
        _apiKeyTestState.value = ApiKeyTestState.Idle
    }
}
