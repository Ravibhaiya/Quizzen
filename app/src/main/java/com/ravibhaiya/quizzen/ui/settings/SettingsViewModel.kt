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

/** App-wide settings (currently only haptic feedback). Scoped to the Activity. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SettingsRepository = DataStoreSettingsRepository(application)

    val hapticEnabled: StateFlow<Boolean> = repository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticEnabled(enabled) }
    }
}
