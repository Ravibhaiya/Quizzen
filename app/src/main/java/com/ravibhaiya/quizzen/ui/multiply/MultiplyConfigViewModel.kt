package com.ravibhaiya.quizzen.ui.multiply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MultiplyConfigUiState(
    val firstDigits: Int = 3,
    val secondDigits: Int = 2,
)

class MultiplyConfigViewModel : ViewModel() {

    private val _state = MutableStateFlow(MultiplyConfigUiState())
    val state: StateFlow<MultiplyConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    fun selectFirstDigits(digits: Int) = _state.update { it.copy(firstDigits = digits) }

    fun selectSecondDigits(digits: Int) = _state.update { it.copy(secondDigits = digits) }

    fun buildConfig(): PracticeConfig.Multiply = PracticeConfig.Multiply(
        firstDigits = _state.value.firstDigits,
        secondDigits = _state.value.secondDigits,
        timerSeconds = timer.resolvedSeconds(),
    )

    companion object {
        const val DEFAULT_TIMER_SECONDS = 20
        val DIGIT_OPTIONS = listOf(2, 3, 4, 5)
    }
}
