package com.ravibhaiya.quizzen.ui.tables

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

data class TablesConfigUiState(
    val selected: Set<Int> = emptySet(),
    /** False until the last-used setting has been read, so the screen can wait instead of flashing the defaults. */
    val loaded: Boolean = false,
) {
    val allSelected: Boolean get() = selected.size == PracticeConfig.Tables.AVAILABLE_NUMBERS.size
}

class TablesConfigViewModel(
    private val repository: QuizSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TablesConfigUiState())
    val state: StateFlow<TablesConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    private var edited = false

    init {
        viewModelScope.launch {
            val saved = repository.loadTables(DEFAULT_TIMER_SECONDS)
            if (saved != null && !edited) {
                _state.update { it.copy(selected = saved.numbers.toSet()) }
                timer.set(saved.timerSeconds)
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    fun toggle(number: Int) {
        edited = true
        _state.update {
            it.copy(selected = if (number in it.selected) it.selected - number else it.selected + number)
        }
    }

    /** Select everything, or clear everything if all are already selected (web: toggleSelectAll). */
    fun toggleAll() {
        edited = true
        _state.update {
            if (it.allSelected) it.copy(selected = emptySet())
            else it.copy(selected = PracticeConfig.Tables.AVAILABLE_NUMBERS.toSet())
        }
    }

    fun onTimerChanged(raw: String) {
        edited = true
        timer.onTextChanged(raw)
    }

    fun buildConfig(): PracticeConfig.Tables = PracticeConfig.Tables(
        numbers = _state.value.selected.sorted(),
        timerSeconds = timer.resolvedSeconds(),
    )

    /** The config for Start; it is also remembered as this quiz's last-used setting. */
    fun startQuiz(): PracticeConfig.Tables {
        val config = buildConfig()
        viewModelScope.launch { repository.saveTables(config) }
        return config
    }

    companion object {
        const val DEFAULT_TIMER_SECONDS = 10

        val Factory = viewModelFactory {
            initializer { TablesConfigViewModel(quizSettingsRepository()) }
        }
    }
}
