package com.ravibhaiya.quizzen.ui.components

import com.ravibhaiya.quizzen.domain.TimerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the Timer field for a config screen: applies [TimerInput] rules and auto-hides the
 * "max exceeded" error after [ERROR_VISIBLE_MS] (web: 2.5s).
 */
class TimerFieldState(
    private val scope: CoroutineScope,
    val defaultSeconds: Int,
) {
    private val _state = MutableStateFlow(TimerInput(defaultSeconds.toString()))
    val state: StateFlow<TimerInput> = _state.asStateFlow()

    private var errorJob: Job? = null

    fun onTextChanged(raw: String) {
        val next = _state.value.onTextChanged(raw)
        _state.value = next
        if (next.exceededMax) {
            errorJob?.cancel()
            errorJob = scope.launch {
                delay(ERROR_VISIBLE_MS)
                _state.update { it.copy(exceededMax = false) }
            }
        } else {
            errorJob?.cancel()
        }
    }

    fun onFocusLost() {
        _state.update { it.onFocusLost(defaultSeconds) }
    }

    fun resolvedSeconds(): Int = _state.value.resolveSeconds(defaultSeconds)

    private companion object {
        const val ERROR_VISIBLE_MS = 2_500L
    }
}
