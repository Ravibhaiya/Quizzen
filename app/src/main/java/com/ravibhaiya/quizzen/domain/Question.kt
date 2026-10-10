package com.ravibhaiya.quizzen.domain

import kotlin.random.Random

/**
 * What the player types: digits, one letter (Alphabet Reasoning), a fraction such as `1/3`, or a percentage such as `33.33`
 * (Fraction & Percentage; the `%` sign is added by the screen).
 */
enum class AnswerKind { Number, Letter, Fraction, Percent }

/**
 * A single practice question. [answer] is a [Long] because 5-digit x 5-digit products exceed [Int]. For questions whose answer
 * is a letter, [answer] is the letter's place in the alphabet and [answerText] is the letter itself.
 */
sealed interface Question {
    val answer: Long

    val answerKind: AnswerKind get() = AnswerKind.Number

    /** The correct answer as the player sees and types it: digits, or a single capital letter. */
    val answerText: String get() = answer.toString()

    /** Whether [input] (what the player typed) is the correct answer. A blank or unreadable input is wrong. */
    fun isCorrect(input: String): Boolean = when (answerKind) {
        AnswerKind.Number -> input.toLongOrNull() == answer
        AnswerKind.Letter -> input.trim().equals(answerText, ignoreCase = true)
        AnswerKind.Fraction, AnswerKind.Percent -> false // these questions check the answer themselves (see FractionQuestion)
    }

    /**
     * The question as it is put to the player this time. Most questions are always asked the same way, so this is the question
     * itself; a question that can be worded in more than one way (see [FractionQuestion]) picks one of them with [random].
     */
    fun asked(random: Random): Question = this
}

/** `left x right` (Multiply and Tables). */
data class ProductQuestion(
    val left: Long,
    val right: Long,
) : Question {
    override val answer: Long get() = left * right
}

/** `base` raised to `exponent` (Squares: 2, Cubes: 3). */
data class PowerQuestion(
    val base: Long,
    val exponent: Int,
) : Question {
    override val answer: Long get() = power(base, exponent)
}

/** The [degree]-th root of a perfect power; the [answer] is [result] and the number shown is [radicand]. */
data class RootQuestion(
    val degree: Int,
    val result: Long,
) : Question {
    val radicand: Long get() = power(result, degree)
    override val answer: Long get() = result
}

/** Alphabet Reasoning: [position] is the place (1..26) of the letter the question is about. What is shown depends on [challenge]. */
data class AlphabetQuestion(
    val challenge: AlphabetChallenge,
    val position: Int,
) : Question {
    /** What is on the screen: a number for [AlphabetChallenge.FindLetter], otherwise the letter. */
    val shown: String
        get() = if (challenge == AlphabetChallenge.FindLetter) position.toString() else AlphabetRules.letterOf(position).toString()

    override val answer: Long
        get() = (if (challenge == AlphabetChallenge.ReverseLetter) AlphabetRules.opposite(position) else position).toLong()

    override val answerKind: AnswerKind
        get() = if (challenge == AlphabetChallenge.FindPosition) AnswerKind.Number else AnswerKind.Letter

    override val answerText: String
        get() = if (answerKind == AnswerKind.Number) answer.toString() else AlphabetRules.letterOf(answer.toInt()).toString()
}

/**
 * Fraction & Percentage: the unit fraction `1/denominator` and its percentage. [challenge] decides which of the two is shown and
 * which one is typed. For [FractionChallenge.Fraction] a percentage with a fractional part can be written as a mixed number
 * (`33 1/3%`) or as a decimal ([decimal], `33.33%`); the wording is picked per asking by [asked].
 *
 * [decimal] is only wording, not part of what is asked, so it is left out of equality: the same question in another wording is
 * still the same question for rounds and comebacks. [answer] is the denominator.
 */
data class FractionQuestion(
    val challenge: FractionChallenge,
    val denominator: Int,
    val decimal: Boolean = false,
) : Question {
    val percent: MixedPercent get() = FractionRules.percentOf(denominator)

    /** True when the question is a percentage with a fractional part written as a mixed number (drawn as a stacked fraction). */
    val showsMixedNumber: Boolean
        get() = challenge == FractionChallenge.Fraction && !decimal && percent.hasFraction

    override val answer: Long get() = denominator.toLong()

    override val answerKind: AnswerKind
        get() = if (challenge == FractionChallenge.Fraction) AnswerKind.Fraction else AnswerKind.Percent

    override val answerText: String
        get() = if (challenge == FractionChallenge.Fraction) "1/$denominator" else FractionRules.percentText(denominator)

    override fun isCorrect(input: String): Boolean = when (challenge) {
        FractionChallenge.Fraction -> FractionRules.isFractionAnswer(input, denominator)
        FractionChallenge.Percentage -> FractionRules.isPercentAnswer(input, denominator)
    }

    override fun asked(random: Random): Question =
        if (challenge == FractionChallenge.Fraction && percent.hasFraction) copy(decimal = random.nextBoolean()) else this

    override fun equals(other: Any?): Boolean =
        other is FractionQuestion && other.challenge == challenge && other.denominator == denominator

    override fun hashCode(): Int = 31 * challenge.hashCode() + denominator
}

internal fun power(base: Long, exponent: Int): Long {
    var value = 1L
    repeat(exponent) { value *= base }
    return value
}

enum class FeedbackType { Correct, Incorrect, Timeout }

/** Feedback shown after a question is resolved. [correctAnswer] (see [Question.answerText]) is captured so it survives question rotation. */
data class Feedback(
    val type: FeedbackType,
    val correctAnswer: String,
)
