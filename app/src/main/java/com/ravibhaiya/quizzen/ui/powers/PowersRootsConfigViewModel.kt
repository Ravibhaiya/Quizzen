package com.ravibhaiya.quizzen.ui.powers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.QuizSettingsRepository
import com.ravibhaiya.quizzen.data.quizSettingsRepository
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PowersRootsConfigUiState(
    val types: Set<PowerRootType> = emptySet(),
    /** Range of base numbers for squares and square roots (limit 30). */
    val squares: IntRange = PowersRootsRules.defaultRange(PowerRootType.Family.Square),
    /** Range of base numbers for cubes and cube roots (limit 20). */
    val cubes: IntRange = PowersRootsRules.defaultRange(PowerRootType.Family.Cube),
    /** False until the last-used setting has been read, so the screen can wait instead of flashing the defaults. */
    val loaded: Boolean = false,
) {
    /** The sliders can only produce valid ranges, so a quiz can start as soon as one kind is chosen. */
    val canStart: Boolean get() = types.isNotEmpty()

    fun rangeOf(family: PowerRootType.Family): IntRange =
        if (family == PowerRootType.Family.Square) squares else cubes

    /** False only when something else is chosen but nothing of this family, so its slider has no effect right now. */
    fun isInUse(family: PowerRootType.Family): Boolean = types.isEmpty() || types.any { it.family == family }
}

class PowersRootsConfigViewModel(
    private val repository: QuizSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PowersRootsConfigUiState())
    val state: StateFlow<PowersRootsConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    private var edited = false

    init {
        viewModelScope.launch {
            val saved = repository.loadPowersRoots(DEFAULT_TIMER_SECONDS)
            // Anything the user already touched while this was loading wins over the saved value.
            if (saved != null && !edited) {
                _state.update { it.copy(types = saved.types, squares = saved.squares, cubes = saved.cubes) }
                timer.set(saved.timerSeconds)
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    fun toggleType(type: PowerRootType) {
        edited = true
        _state.update { it.copy(types = if (type in it.types) it.types - type else it.types + type) }
    }

    fun onRangeChanged(family: PowerRootType.Family, range: IntRange) {
        edited = true
        _state.update {
            val valid = PowersRootsRules.coerce(range.first, range.last, family)
            if (family == PowerRootType.Family.Square) it.copy(squares = valid) else it.copy(cubes = valid)
        }
    }

    fun onTimerChanged(raw: String) {
        edited = true
        timer.onTextChanged(raw)
    }

    fun buildConfig(): PracticeConfig.PowersRoots {
        val current = _state.value
        return PracticeConfig.PowersRoots(
            types = current.types,
            squares = current.squares,
            cubes = current.cubes,
            timerSeconds = timer.resolvedSeconds(),
        )
    }

    /** The config for Start; it is also remembered as this quiz's last-used setting. */
    fun startQuiz(): PracticeConfig.PowersRoots {
        val config = buildConfig()
        viewModelScope.launch { repository.savePowersRoots(config) }
        return config
    }

    companion object {
        const val DEFAULT_TIMER_SECONDS = 10

        val Factory = viewModelFactory {
            initializer { PowersRootsConfigViewModel(quizSettingsRepository()) }
        }
    }
}
