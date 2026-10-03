package com.ravibhaiya.quizzen.domain

/** Everything a practice session needs to run. Immutable; encoded into navigation arguments. */
sealed interface PracticeConfig {
    val timerSeconds: Int

    /**
     * True for quizzes with a limited set of questions (Tables, Powers & Roots): wrong and slow answers come back later in
     * the same session (see [PracticeSession]). Multiply draws from far too many questions for that.
     */
    val repeatsMistakes: Boolean

    /** Random `a x b` where each operand has a fixed number of digits. */
    data class Multiply(
        val firstDigits: Int,
        val secondDigits: Int,
        override val timerSeconds: Int,
    ) : PracticeConfig {
        override val repeatsMistakes: Boolean get() = false
    }

    /** `n x m` where `n` is one of the chosen table numbers and `m` is 1..[MULTIPLIER_MAX]. */
    data class Tables(
        val numbers: List<Int>,
        override val timerSeconds: Int,
    ) : PracticeConfig {
        override val repeatsMistakes: Boolean get() = true

        companion object {
            const val MULTIPLIER_MAX = 10
            val AVAILABLE_NUMBERS: List<Int> = (2..31).toList()
        }
    }

    /** Squares, cubes, square roots and cube roots for base numbers in `min..max` (see [PowersRootsRules]). */
    data class PowersRoots(
        val types: Set<PowerRootType>,
        val min: Int,
        val max: Int,
        override val timerSeconds: Int,
    ) : PracticeConfig {
        override val repeatsMistakes: Boolean get() = true
    }
}
