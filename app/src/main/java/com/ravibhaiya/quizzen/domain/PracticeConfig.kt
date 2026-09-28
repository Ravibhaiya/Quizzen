package com.ravibhaiya.quizzen.domain

/** Everything a practice session needs to run. Immutable; encoded into navigation arguments. */
sealed interface PracticeConfig {
    val timerSeconds: Int

    /** Random `a x b` where each operand has a fixed number of digits. */
    data class Multiply(
        val firstDigits: Int,
        val secondDigits: Int,
        override val timerSeconds: Int,
    ) : PracticeConfig

    /** `n x m` where `n` is one of the chosen table numbers and `m` is 1..[MULTIPLIER_MAX]. */
    data class Tables(
        val numbers: List<Int>,
        override val timerSeconds: Int,
    ) : PracticeConfig {
        companion object {
            const val MULTIPLIER_MAX = 10
            val AVAILABLE_NUMBERS: List<Int> = (2..31).toList()
        }
    }
}
