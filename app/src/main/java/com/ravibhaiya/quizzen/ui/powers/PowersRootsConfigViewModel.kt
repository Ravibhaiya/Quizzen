package com.ravibhaiya.quizzen.ui.powers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PowersRootsConfigUiState(
    val types: Set<PowerRootType> = emptySet(),
    /** Range of base numbers for squares and square roots (limit 30). */
    val squares: IntRange = PowersRootsRules.defaultRange(PowerRootType.Family.Square),
    /** Range of base numbers for cubes and cube roots (limit 20). */
    val cubes: IntRange = PowersRootsRules.defaultRange(PowerRootType.Family.Cube),
) {
    /** The sliders can only produce valid ranges, so a quiz can start as soon as one kind is chosen. */
    val canStart: Boolean get() = types.isNotEmpty()

    fun rangeOf(family: PowerRootType.Family): IntRange =
        if (family == PowerRootType.Family.Square) squares else cubes

    /** False only when something else is chosen but nothing of this family, so its slider has no effect right now. */
    fun isInUse(family: PowerRootType.Family): Boolean = types.isEmpty() || types.any { it.family == family }
}

class PowersRootsConfigViewModel : ViewModel() {

    private val _state = MutableStateFlow(PowersRootsConfigUiState())
    val state: StateFlow<PowersRootsConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    fun toggleType(type: PowerRootType) = _state.update {
        it.copy(types = if (type in it.types) it.types - type else it.types + type)
    }

    fun onRangeChanged(family: PowerRootType.Family, range: IntRange) = _state.update {
        val valid = PowersRootsRules.coerce(range.first, range.last, family)
        if (family == PowerRootType.Family.Square) it.copy(squares = valid) else it.copy(cubes = valid)
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

    companion object {
        const val DEFAULT_TIMER_SECONDS = 10
    }
}
