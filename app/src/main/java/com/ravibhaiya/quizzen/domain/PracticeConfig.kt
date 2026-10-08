package com.ravibhaiya.quizzen.domain

/** Everything a practice session needs to run. Immutable; encoded into navigation arguments. */
sealed interface PracticeConfig {
    val timerSeconds: Int

    /**
     * True for quizzes with a limited set of questions (Tables, Powers & Roots, Alphabet): wrong and slow answers come back later in
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

        companion object {
            /** Digit counts offered for each number. */
            val DIGIT_OPTIONS: List<Int> = listOf(2, 3, 4, 5)
        }
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

    /**
     * Squares, cubes, square roots and cube roots. [squares] is the range of base numbers for squares and square roots,
     * [cubes] the one for cubes and cube roots (see [PowersRootsRules] for the limits).
     */
    data class PowersRoots(
        val types: Set<PowerRootType>,
        val squares: IntRange,
        val cubes: IntRange,
        override val timerSeconds: Int,
    ) : PracticeConfig {
        override val repeatsMistakes: Boolean get() = true

        fun rangeOf(type: PowerRootType): IntRange =
            if (type.family == PowerRootType.Family.Square) squares else cubes
    }

    /** Alphabet Reasoning: one [challenge] over the letters at positions [letters] (1 = A ... 26 = Z). */
    data class Alphabet(
        val challenge: AlphabetChallenge,
        val letters: IntRange,
        override val timerSeconds: Int,
    ) : PracticeConfig {
        override val repeatsMistakes: Boolean get() = true
    }
}
