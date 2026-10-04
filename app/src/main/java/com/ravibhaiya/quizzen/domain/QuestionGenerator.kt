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
        is PracticeConfig.Tables -> ProductQuestion(
            left = config.numbers.random(random).toLong(),
            right = random.nextLong(1, PracticeConfig.Tables.MULTIPLIER_MAX + 1L),
        )
        is PracticeConfig.PowersRoots -> powersRoots(config)
    }

    private fun powersRoots(config: PracticeConfig.PowersRoots): Question {
        val choices = config.types
            .map { type -> type to config.rangeOf(type) }
            // Unreachable for validated configs; keeps a corrupted config from crashing the quiz.
            .ifEmpty { PowerRootType.entries.map { it to PowersRootsRules.defaultRange(it.family) } }
        val (type, range) = choices.random(random)
        val base = random.nextInt(range.first, range.last + 1).toLong()
        return when (type) {
            PowerRootType.Squares -> PowerQuestion(base, exponent = 2)
            PowerRootType.Cubes -> PowerQuestion(base, exponent = 3)
            PowerRootType.SquareRoots -> RootQuestion(degree = 2, result = base)
            PowerRootType.CubeRoots -> RootQuestion(degree = 3, result = base)
        }
    }

    /** 1 digit -> 1..9, n digits -> 10^(n-1)..10^n - 1 (matches the web original). */
    internal fun randomWithDigits(digits: Int): Long {
        if (digits <= 1) return random.nextLong(1, 10)
        var min = 1L
        repeat(digits - 1) { min *= 10 }
        return random.nextLong(min, min * 10)
    }
}
