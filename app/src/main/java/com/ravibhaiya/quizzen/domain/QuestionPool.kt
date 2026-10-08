package com.ravibhaiya.quizzen.domain

/** Every question a quiz can ask, for quizzes whose set of questions is small enough to list. */
object QuestionPool {

    /**
     * All questions of [config], each exactly once, or `null` when there are too many to list (Multiply) or nothing is
     * selected. Tables: every chosen table number times 1 to 10. Powers & Roots: every base number in the kind's range, for
     * every chosen kind. Alphabet: every letter of the range, once.
     */
    fun of(config: PracticeConfig): List<Question>? {
        val questions: List<Question> = when (config) {
            is PracticeConfig.Multiply -> return null
            is PracticeConfig.Tables -> config.numbers.distinct().flatMap { number ->
                (1..PracticeConfig.Tables.MULTIPLIER_MAX).map { multiplier ->
                    ProductQuestion(left = number.toLong(), right = multiplier.toLong())
                }
            }
            is PracticeConfig.Alphabet -> config.letters.map { AlphabetQuestion(config.challenge, it) }
            is PracticeConfig.PowersRoots -> config.types.flatMap { type ->
                config.rangeOf(type).map { base ->
                    when (type) {
                        PowerRootType.Squares -> PowerQuestion(base.toLong(), exponent = 2)
                        PowerRootType.Cubes -> PowerQuestion(base.toLong(), exponent = 3)
                        PowerRootType.SquareRoots -> RootQuestion(degree = 2, result = base.toLong())
                        PowerRootType.CubeRoots -> RootQuestion(degree = 3, result = base.toLong())
                    }
                }
            }
        }
        return questions.takeIf { it.isNotEmpty() }
    }
}
