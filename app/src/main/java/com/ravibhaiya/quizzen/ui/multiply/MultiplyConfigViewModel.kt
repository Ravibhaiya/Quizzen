package com.ravibhaiya.quizzen.ui.multiply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.QuizSettingsRepository
import com.ravibhaiya.quizzen.data.quizSettingsRepository
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MultiplyConfigUiState(
    val firstDigits: Int = 3,
    val secondDigits: Int = 2,
    /** False until the last-used setting has been read, so the screen can wait instead of flashing the defaults. */
    val loaded: Boolean = false,
)

class MultiplyConfigViewModel(
    private val repository: QuizSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MultiplyConfigUiState())
    val state: StateFlow<MultiplyConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    private var edited = false

    init {
        viewModelScope.launch {
            val saved = repository.loadMultiply(DEFAULT_TIMER_SECONDS)
            // Anything the user already touched while this was loading wins over the saved value.
            if (saved != null && !edited) {
                _state.update { it.copy(firstDigits = saved.firstDigits, secondDigits = saved.secondDigits) }
                timer.set(saved.timerSeconds)
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    fun selectFirstDigits(digits: Int) {
        edited = true
        _state.update { it.copy(firstDigits = digits) }
    }

    fun selectSecondDigits(digits: Int) {
        edited = true
        _state.update { it.copy(secondDigits = digits) }
    }

    fun onTimerChanged(raw: String) {
        edited = true
        timer.onTextChanged(raw)
    }

    fun buildConfig(): PracticeConfig.Multiply = PracticeConfig.Multiply(
        firstDigits = _state.value.firstDigits,
        secondDigits = _state.value.secondDigits,
        timerSeconds = timer.resolvedSeconds(),
    )

    /** The config for Start; it is also remembered as this quiz's last-used setting. */
    fun startQuiz(): PracticeConfig.Multiply {
        val config = buildConfig()
        viewModelScope.launch { repository.saveMultiply(config) }
        return config
    }

    companion object {
        const val DEFAULT_TIMER_SECONDS = 20

        val Factory = viewModelFactory {
            initializer { MultiplyConfigViewModel(quizSettingsRepository()) }
        }
    }
}
