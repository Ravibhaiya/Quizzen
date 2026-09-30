package com.ravibhaiya.quizzen.ui.navigation

import androidx.lifecycle.SavedStateHandle
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig

object Routes {
    const val HOME = "home"
    const val MULTIPLY = "multiply"
    const val TABLES = "tables"
    const val POWERS = "powers"

    const val ARG_MODE = "mode"
    const val ARG_D1 = "d1"
    const val ARG_D2 = "d2"
    const val ARG_NUMBERS = "numbers"
    const val ARG_SECONDS = "seconds"
    const val ARG_TYPES = "types"
    const val ARG_MIN = "min"
    const val ARG_MAX = "max"

    const val MODE_MULTIPLY = "multiply"
    const val MODE_TABLES = "tables"
    const val MODE_POWERS = "powers"

    const val PRACTICE = "practice/{$ARG_MODE}?$ARG_D1={$ARG_D1}&$ARG_D2={$ARG_D2}" +
        "&$ARG_NUMBERS={$ARG_NUMBERS}&$ARG_SECONDS={$ARG_SECONDS}" +
        "&$ARG_TYPES={$ARG_TYPES}&$ARG_MIN={$ARG_MIN}&$ARG_MAX={$ARG_MAX}"

    fun practice(config: PracticeConfig): String = when (config) {
        is PracticeConfig.Multiply ->
            "practice/$MODE_MULTIPLY?$ARG_D1=${config.firstDigits}&$ARG_D2=${config.secondDigits}" +
                "&$ARG_SECONDS=${config.timerSeconds}"
        is PracticeConfig.Tables ->
            "practice/$MODE_TABLES?$ARG_NUMBERS=${config.numbers.joinToString(",")}" +
                "&$ARG_SECONDS=${config.timerSeconds}"
        is PracticeConfig.PowersRoots ->
            "practice/$MODE_POWERS?$ARG_TYPES=${config.types.sortedBy { it.ordinal }.joinToString(",") { it.code }}" +
                "&$ARG_MIN=${config.min}&$ARG_MAX=${config.max}&$ARG_SECONDS=${config.timerSeconds}"
    }
}

/** Decodes a [PracticeConfig] from navigation arguments (also restored from saved state). */
object PracticeArgs {

    fun fromSavedState(handle: SavedStateHandle): PracticeConfig = decode(
        mode = handle.get<String>(Routes.ARG_MODE),
        d1 = handle.get<Int>(Routes.ARG_D1),
        d2 = handle.get<Int>(Routes.ARG_D2),
        numbers = handle.get<String>(Routes.ARG_NUMBERS),
        seconds = handle.get<Int>(Routes.ARG_SECONDS),
        types = handle.get<String>(Routes.ARG_TYPES),
        min = handle.get<Int>(Routes.ARG_MIN),
        max = handle.get<Int>(Routes.ARG_MAX),
    )

    fun decode(
        mode: String?,
        d1: Int?,
        d2: Int?,
        numbers: String?,
        seconds: Int?,
        types: String? = null,
        min: Int? = null,
        max: Int? = null,
    ): PracticeConfig {
        val timer = (seconds ?: 0).takeIf { it > 0 } ?: 20
        return when (mode) {
            Routes.MODE_TABLES -> PracticeConfig.Tables(
                numbers = numbers.orEmpty().split(',').mapNotNull { it.toIntOrNull() }
                    .ifEmpty { PracticeConfig.Tables.AVAILABLE_NUMBERS },
                timerSeconds = timer,
            )
            Routes.MODE_POWERS -> {
                val low = (min ?: PowersRootsRules.DEFAULT_MIN)
                    .coerceIn(PowersRootsRules.MIN_ALLOWED, PowersRootsRules.MAX_ALLOWED)
                PracticeConfig.PowersRoots(
                    types = types.orEmpty().split(',').mapNotNull(PowerRootType::fromCode).toSet()
                        .ifEmpty { PowerRootType.entries.toSet() },
                    min = low,
                    max = (max ?: PowersRootsRules.DEFAULT_MAX).coerceIn(low, PowersRootsRules.MAX_ALLOWED),
                    timerSeconds = timer,
                )
            }
            else -> PracticeConfig.Multiply(
                firstDigits = (d1 ?: 3).coerceIn(1, 5),
                secondDigits = (d2 ?: 2).coerceIn(1, 5),
                timerSeconds = timer,
            )
        }
    }
}
