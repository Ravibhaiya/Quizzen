package com.ravibhaiya.quizzen.domain

/** A single practice question. [answer] is a [Long] because 5-digit x 5-digit products exceed [Int]. */
sealed interface Question {
    val answer: Long
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

internal fun power(base: Long, exponent: Int): Long {
    var value = 1L
    repeat(exponent) { value *= base }
    return value
}

enum class FeedbackType { Correct, Incorrect, Timeout }

/** Feedback shown after a question is resolved. [correctAnswer] is captured so it survives question rotation. */
data class Feedback(
    val type: FeedbackType,
    val correctAnswer: Long,
)
