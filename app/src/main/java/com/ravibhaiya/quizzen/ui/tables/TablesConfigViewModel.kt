package com.ravibhaiya.quizzen.ui.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TablesConfigUiState(
    val selected: Set<Int> = emptySet(),
) {
    val allSelected: Boolean get() = selected.size == PracticeConfig.Tables.AVAILABLE_NUMBERS.size
}

class TablesConfigViewModel : ViewModel() {

    private val _state = MutableStateFlow(TablesConfigUiState())
    val state: StateFlow<TablesConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    fun toggle(number: Int) = _state.update {
        it.copy(selected = if (number in it.selected) it.selected - number else it.selected + number)
    }

    /** Select everything, or clear everything if all are already selected (web: toggleSelectAll). */
    fun toggleAll() = _state.update {
        if (it.allSelected) it.copy(selected = emptySet())
        else it.copy(selected = PracticeConfig.Tables.AVAILABLE_NUMBERS.toSet())
    }

    fun buildConfig(): PracticeConfig.Tables = PracticeConfig.Tables(
        numbers = _state.value.selected.sorted(),
        timerSeconds = timer.resolvedSeconds(),
    )

    companion object {
        const val DEFAULT_TIMER_SECONDS = 10
    }
}
