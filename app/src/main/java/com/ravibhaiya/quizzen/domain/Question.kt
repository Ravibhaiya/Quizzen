package com.ravibhaiya.quizzen.domain

/** A single multiplication question. Uses [Long] because 5-digit x 5-digit products exceed [Int]. */
data class Question(
    val left: Long,
    val right: Long,
) {
    val answer: Long get() = left * right
}

enum class FeedbackType { Correct, Incorrect, Timeout }

/** Feedback shown after a question is resolved. [correctAnswer] is captured so it survives question rotation. */
data class Feedback(
    val type: FeedbackType,
    val correctAnswer: Long,
)
