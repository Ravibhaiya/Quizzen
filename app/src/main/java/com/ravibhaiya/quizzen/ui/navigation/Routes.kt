package com.ravibhaiya.quizzen.ui.navigation

import androidx.lifecycle.SavedStateHandle
import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetRules
import com.ravibhaiya.quizzen.domain.FractionChallenge
import com.ravibhaiya.quizzen.domain.FractionRules
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig

object Routes {
    const val HOME = "home"
    const val MULTIPLY = "multiply"
    const val TABLES = "tables"
    const val POWERS = "powers"
    const val ALPHABET = "alphabet"
    const val FRACTIONS = "fractions"

    const val ARG_MODE = "mode"
    const val ARG_D1 = "d1"
    const val ARG_D2 = "d2"
    const val ARG_NUMBERS = "numbers"
    const val ARG_SECONDS = "seconds"
    const val ARG_TYPES = "types"
    const val ARG_SQ_MIN = "smin"
    const val ARG_SQ_MAX = "smax"
    const val ARG_CU_MIN = "cmin"
    const val ARG_CU_MAX = "cmax"
    const val ARG_CHALLENGE = "ch"
    const val ARG_LETTER_FROM = "lfrom"
    const val ARG_LETTER_TO = "lto"
    const val ARG_FRACTION_FROM = "ffrom"
    const val ARG_FRACTION_TO = "fto"

    const val MODE_MULTIPLY = "multiply"
    const val MODE_TABLES = "tables"
    const val MODE_POWERS = "powers"
    const val MODE_ALPHABET = "alphabet"
    const val MODE_FRACTIONS = "fractions"

    const val PRACTICE = "practice/{$ARG_MODE}?$ARG_D1={$ARG_D1}&$ARG_D2={$ARG_D2}" +
        "&$ARG_NUMBERS={$ARG_NUMBERS}&$ARG_SECONDS={$ARG_SECONDS}" +
        "&$ARG_TYPES={$ARG_TYPES}&$ARG_SQ_MIN={$ARG_SQ_MIN}&$ARG_SQ_MAX={$ARG_SQ_MAX}" +
        "&$ARG_CU_MIN={$ARG_CU_MIN}&$ARG_CU_MAX={$ARG_CU_MAX}" +
        "&$ARG_CHALLENGE={$ARG_CHALLENGE}&$ARG_LETTER_FROM={$ARG_LETTER_FROM}&$ARG_LETTER_TO={$ARG_LETTER_TO}" +
        "&$ARG_FRACTION_FROM={$ARG_FRACTION_FROM}&$ARG_FRACTION_TO={$ARG_FRACTION_TO}"

    fun practice(config: PracticeConfig): String = when (config) {
        is PracticeConfig.Multiply ->
            "practice/$MODE_MULTIPLY?$ARG_D1=${config.firstDigits}&$ARG_D2=${config.secondDigits}" +
                "&$ARG_SECONDS=${config.timerSeconds}"
        is PracticeConfig.Tables ->
            "practice/$MODE_TABLES?$ARG_NUMBERS=${config.numbers.joinToString(",")}" +
                "&$ARG_SECONDS=${config.timerSeconds}"
        is PracticeConfig.PowersRoots ->
            "practice/$MODE_POWERS?$ARG_TYPES=${config.types.sortedBy { it.ordinal }.joinToString(",") { it.code }}" +
                "&$ARG_SQ_MIN=${config.squares.first}&$ARG_SQ_MAX=${config.squares.last}" +
                "&$ARG_CU_MIN=${config.cubes.first}&$ARG_CU_MAX=${config.cubes.last}&$ARG_SECONDS=${config.timerSeconds}"
        is PracticeConfig.Alphabet ->
            "practice/$MODE_ALPHABET?$ARG_CHALLENGE=${AlphabetChallenge.encode(config.challenges)}" +
                "&$ARG_LETTER_FROM=${config.letters.first}&$ARG_LETTER_TO=${config.letters.last}" +
                "&$ARG_SECONDS=${config.timerSeconds}"
        // The challenge travels in the same argument as Alphabet's (the mode says which codes to read).
        is PracticeConfig.Fractions ->
            "practice/$MODE_FRACTIONS?$ARG_CHALLENGE=${FractionChallenge.encode(config.challenges)}" +
                "&$ARG_FRACTION_FROM=${config.range.first}&$ARG_FRACTION_TO=${config.range.last}" +
                "&$ARG_SECONDS=${config.timerSeconds}"
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
        squareMin = handle.get<Int>(Routes.ARG_SQ_MIN),
        squareMax = handle.get<Int>(Routes.ARG_SQ_MAX),
        cubeMin = handle.get<Int>(Routes.ARG_CU_MIN),
        cubeMax = handle.get<Int>(Routes.ARG_CU_MAX),
        challenge = handle.get<String>(Routes.ARG_CHALLENGE),
        letterFrom = handle.get<Int>(Routes.ARG_LETTER_FROM),
        letterTo = handle.get<Int>(Routes.ARG_LETTER_TO),
        fractionFrom = handle.get<Int>(Routes.ARG_FRACTION_FROM),
        fractionTo = handle.get<Int>(Routes.ARG_FRACTION_TO),
    )

    fun decode(
        mode: String?,
        d1: Int?,
        d2: Int?,
        numbers: String?,
        seconds: Int?,
        types: String? = null,
        squareMin: Int? = null,
        squareMax: Int? = null,
        cubeMin: Int? = null,
        cubeMax: Int? = null,
        challenge: String? = null,
        letterFrom: Int? = null,
        letterTo: Int? = null,
        fractionFrom: Int? = null,
        fractionTo: Int? = null,
    ): PracticeConfig {
        val timer = (seconds ?: 0).takeIf { it > 0 } ?: 20
        return when (mode) {
            Routes.MODE_TABLES -> PracticeConfig.Tables(
                numbers = numbers.orEmpty().split(',').mapNotNull { it.toIntOrNull() }
                    .ifEmpty { PracticeConfig.Tables.AVAILABLE_NUMBERS },
                timerSeconds = timer,
            )
            Routes.MODE_POWERS -> {
                val square = PowerRootType.Family.Square
                val cube = PowerRootType.Family.Cube
                PracticeConfig.PowersRoots(
                    types = types.orEmpty().split(',').mapNotNull(PowerRootType::fromCode).toSet()
                        .ifEmpty { PowerRootType.entries.toSet() },
                    squares = PowersRootsRules.coerce(
                        squareMin ?: PowersRootsRules.DEFAULT_MIN,
                        squareMax ?: PowersRootsRules.SQUARE_LIMIT,
                        square,
                    ),
                    cubes = PowersRootsRules.coerce(
                        cubeMin ?: PowersRootsRules.DEFAULT_MIN,
                        cubeMax ?: PowersRootsRules.CUBE_LIMIT,
                        cube,
                    ),
                    timerSeconds = timer,
                )
            }
            Routes.MODE_ALPHABET -> PracticeConfig.Alphabet(
                challenges = AlphabetChallenge.decode(challenge).ifEmpty { setOf(AlphabetChallenge.FindPosition) },
                letters = AlphabetRules.coerce(letterFrom ?: AlphabetRules.FIRST, letterTo ?: AlphabetRules.SIZE),
                timerSeconds = timer,
            )
            Routes.MODE_FRACTIONS -> PracticeConfig.Fractions(
                challenges = FractionChallenge.decode(challenge).ifEmpty { setOf(FractionChallenge.Fraction) },
                timerSeconds = timer,
                range = FractionRules.coerce(fractionFrom ?: FractionRules.FIRST, fractionTo ?: FractionRules.SIZE),
            )
            else -> PracticeConfig.Multiply(
                firstDigits = (d1 ?: 3).coerceIn(1, 5),
                secondDigits = (d2 ?: 2).coerceIn(1, 5),
                timerSeconds = timer,
            )
        }
    }
}
