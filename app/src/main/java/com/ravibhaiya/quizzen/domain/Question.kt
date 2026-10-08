package com.ravibhaiya.quizzen.domain

/** What the player types: digits, or one letter (Alphabet Reasoning). */
enum class AnswerKind { Number, Letter }

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
    }
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
