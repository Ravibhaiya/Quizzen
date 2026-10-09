package com.ravibhaiya.quizzen.ui.fractions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.QuizSettingsRepository
import com.ravibhaiya.quizzen.data.quizSettingsRepository
import com.ravibhaiya.quizzen.domain.FractionChallenge
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FractionsConfigUiState(
    /** What the player answers in: a fraction (the question is a percentage) or a percentage (the question is a fraction). */
    val challenge: FractionChallenge = FractionChallenge.Fraction,
    /** False until the last-used setting has been read, so the screen can wait instead of flashing the defaults. */
    val loaded: Boolean = false,
)

class FractionsConfigViewModel(
    private val repository: QuizSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FractionsConfigUiState())
    val state: StateFlow<FractionsConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    private var edited = false

    init {
        viewModelScope.launch {
            val saved = repository.loadFractions(DEFAULT_TIMER_SECONDS)
            // Anything the user already touched while this was loading wins over the saved value.
            if (saved != null && !edited) {
                _state.update { it.copy(challenge = saved.challenge) }
                timer.set(saved.timerSeconds)
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    fun selectChallenge(challenge: FractionChallenge) {
        edited = true
        _state.update { it.copy(challenge = challenge) }
    }

    fun onTimerChanged(raw: String) {
        edited = true
        timer.onTextChanged(raw)
    }

    fun buildConfig(): PracticeConfig.Fractions =
        PracticeConfig.Fractions(challenge = _state.value.challenge, timerSeconds = timer.resolvedSeconds())

    /** The config for Start; it is also remembered as this quiz's last-used setting. */
    fun startQuiz(): PracticeConfig.Fractions {
        val config = buildConfig()
        viewModelScope.launch { repository.saveFractions(config) }
        return config
    }

    companion object {
        const val DEFAULT_TIMER_SECONDS = 10

        val Factory = viewModelFactory {
            initializer { FractionsConfigViewModel(quizSettingsRepository()) }
        }
    }
}
