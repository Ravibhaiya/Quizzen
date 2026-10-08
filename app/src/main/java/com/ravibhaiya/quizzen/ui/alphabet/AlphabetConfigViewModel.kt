package com.ravibhaiya.quizzen.ui.alphabet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.QuizSettingsRepository
import com.ravibhaiya.quizzen.data.quizSettingsRepository
import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlphabetConfigUiState(
    val challenge: AlphabetChallenge = AlphabetChallenge.FindPosition,
    /** Positions of the first and last letter (1 = A ... 26 = Z). */
    val letters: IntRange = AlphabetRules.FULL,
    /** False until the last-used setting has been read, so the screen can wait instead of flashing the defaults. */
    val loaded: Boolean = false,
)

class AlphabetConfigViewModel(
    private val repository: QuizSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AlphabetConfigUiState())
    val state: StateFlow<AlphabetConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    private var edited = false

    init {
        viewModelScope.launch {
            val saved = repository.loadAlphabet(DEFAULT_TIMER_SECONDS)
            // Anything the user already touched while this was loading wins over the saved value.
            if (saved != null && !edited) {
                _state.update { it.copy(challenge = saved.challenge, letters = saved.letters) }
                timer.set(saved.timerSeconds)
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    fun selectChallenge(challenge: AlphabetChallenge) {
        edited = true
        _state.update { it.copy(challenge = challenge) }
    }

    /** The slider can only produce valid ranges; [AlphabetRules.coerce] is the safety net. */
    fun onLettersChanged(range: IntRange) {
        edited = true
        _state.update { it.copy(letters = AlphabetRules.coerce(range.first, range.last)) }
    }

    fun onTimerChanged(raw: String) {
        edited = true
        timer.onTextChanged(raw)
    }

    fun buildConfig(): PracticeConfig.Alphabet {
        val current = _state.value
        return PracticeConfig.Alphabet(
            challenge = current.challenge,
            letters = current.letters,
            timerSeconds = timer.resolvedSeconds(),
        )
    }

    /** The config for Start; it is also remembered as this quiz's last-used setting. */
    fun startQuiz(): PracticeConfig.Alphabet {
        val config = buildConfig()
        viewModelScope.launch { repository.saveAlphabet(config) }
        return config
    }

    companion object {
        const val DEFAULT_TIMER_SECONDS = 10

        val Factory = viewModelFactory {
            initializer { AlphabetConfigViewModel(quizSettingsRepository()) }
        }
    }
}
