package com.ravibhaiya.quizzen.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ravibhaiya.quizzen.data.DataStoreSettingsRepository
import com.ravibhaiya.quizzen.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide settings (haptic feedback and local Gemini API key). Scoped to the Activity. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SettingsRepository = DataStoreSettingsRepository(application)

    val hapticEnabled: StateFlow<Boolean> = repository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val geminiApiKey: StateFlow<String> = repository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, DataStoreSettingsRepository.latestCachedApiKey)

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticEnabled(enabled) }
    }

    fun setGeminiApiKey(apiKey: String) {
        viewModelScope.launch { repository.setGeminiApiKey(apiKey) }
    }
}
