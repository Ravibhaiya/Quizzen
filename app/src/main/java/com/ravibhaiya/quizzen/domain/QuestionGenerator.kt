package com.ravibhaiya.quizzen.domain

import kotlin.random.Random

fun interface QuestionGenerator {
    fun next(config: PracticeConfig): Question
}

class RandomQuestionGenerator(private val random: Random = Random.Default) : QuestionGenerator {

    override fun next(config: PracticeConfig): Question = when (config) {
        is PracticeConfig.Multiply -> Question(
            left = randomWithDigits(config.firstDigits),
            right = randomWithDigits(config.secondDigits),
        )
        is PracticeConfig.Tables -> Question(
            left = config.numbers.random(random).toLong(),
            right = random.nextLong(1, PracticeConfig.Tables.MULTIPLIER_MAX + 1L),
        )
    }

    /** 1 digit -> 1..9, n digits -> 10^(n-1)..10^n - 1 (matches the web original). */
    internal fun randomWithDigits(digits: Int): Long {
        if (digits <= 1) return random.nextLong(1, 10)
        var min = 1L
        repeat(digits - 1) { min *= 10 }
        return random.nextLong(min, min * 10)
    }
}
