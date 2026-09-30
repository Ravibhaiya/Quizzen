package com.ravibhaiya.quizzen.ui.powers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PowersRootsConfigUiState(
    val types: Set<PowerRootType> = emptySet(),
    val minText: String = PowersRootsRules.DEFAULT_MIN.toString(),
    val maxText: String = PowersRootsRules.DEFAULT_MAX.toString(),
    /** True for a moment after the user typed a number above the allowed maximum. */
    val limitExceeded: Boolean = false,
) {
    val min: Int? get() = minText.toIntOrNull()
    val max: Int? get() = maxText.toIntOrNull()

    val issue: PowersRootsRules.Issue get() = PowersRootsRules.issue(types, min, max)

    val canStart: Boolean get() = types.isNotEmpty() && issue == PowersRootsRules.Issue.None

    /** A blank field is just being edited, so it blocks Start without showing an error message. */
    val showIssue: Boolean
        get() = issue != PowersRootsRules.Issue.None && minText.isNotEmpty() && maxText.isNotEmpty()
}

class PowersRootsConfigViewModel : ViewModel() {

    private val _state = MutableStateFlow(PowersRootsConfigUiState())
    val state: StateFlow<PowersRootsConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    private var limitJob: Job? = null

    fun toggleType(type: PowerRootType) = _state.update {
        it.copy(types = if (type in it.types) it.types - type else it.types + type)
    }

    fun onMinChanged(raw: String) = onNumberChanged(raw) { text -> copy(minText = text) }

    fun onMaxChanged(raw: String) = onNumberChanged(raw) { text -> copy(maxText = text) }

    /** An empty or zero field snaps back to its default when the user leaves it. */
    fun onMinFocusLost() = _state.update {
        if ((it.min ?: 0) < PowersRootsRules.MIN_ALLOWED) it.copy(minText = PowersRootsRules.DEFAULT_MIN.toString()) else it
    }

    fun onMaxFocusLost() = _state.update {
        if ((it.max ?: 0) < PowersRootsRules.MIN_ALLOWED) it.copy(maxText = PowersRootsRules.DEFAULT_MAX.toString()) else it
    }

    fun buildConfig(): PracticeConfig.PowersRoots {
        val current = _state.value
        return PracticeConfig.PowersRoots(
            types = current.types,
            min = current.min ?: PowersRootsRules.DEFAULT_MIN,
            max = current.max ?: PowersRootsRules.DEFAULT_MAX,
            timerSeconds = timer.resolvedSeconds(),
        )
    }

    private fun onNumberChanged(raw: String, transform: PowersRootsConfigUiState.(String) -> PowersRootsConfigUiState) {
        val input = PowersRootsRules.normalizeInput(raw)
        _state.update { it.transform(input.text).copy(limitExceeded = input.exceededLimit) }
        limitJob?.cancel()
        if (input.exceededLimit) {
            limitJob = viewModelScope.launch {
                delay(LIMIT_MESSAGE_MS)
                _state.update { it.copy(limitExceeded = false) }
            }
        }
    }

    companion object {
        const val DEFAULT_TIMER_SECONDS = 10
        private const val LIMIT_MESSAGE_MS = 2_500L
    }
}
