package com.ravibhaiya.quizzen.domain

import kotlin.random.Random

fun interface QuestionGenerator {
    fun next(config: PracticeConfig): Question
}

class RandomQuestionGenerator(private val random: Random = Random.Default) : QuestionGenerator {

    override fun next(config: PracticeConfig): Question = when (config) {
        is PracticeConfig.Multiply -> ProductQuestion(
            left = randomWithDigits(config.firstDigits),
            right = randomWithDigits(config.secondDigits),
        )
        // Limited quizzes are normally played from a shuffled round (see PracticeSession); a plain pick from the same list
        // keeps this generator complete.
        is PracticeConfig.Tables, is PracticeConfig.PowersRoots ->
            requireNotNull(QuestionPool.of(config)) { "nothing to ask: $config" }.random(random)
    }

    /** 1 digit -> 1..9, n digits -> 10^(n-1)..10^n - 1 (matches the web original). */
    internal fun randomWithDigits(digits: Int): Long {
        if (digits <= 1) return random.nextLong(1, 10)
        var min = 1L
        repeat(digits - 1) { min *= 10 }
        return random.nextLong(min, min * 10)
    }
}
