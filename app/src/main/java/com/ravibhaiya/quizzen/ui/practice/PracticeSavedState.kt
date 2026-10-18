package com.ravibhaiya.quizzen.ui.practice

import androidx.lifecycle.SavedStateHandle
import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetQuestion
import com.ravibhaiya.quizzen.domain.AlphabetRules
import com.ravibhaiya.quizzen.domain.FractionChallenge
import com.ravibhaiya.quizzen.domain.FractionQuestion
import com.ravibhaiya.quizzen.domain.PowerQuestion
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.domain.ProductQuestion
import com.ravibhaiya.quizzen.domain.Question
import com.ravibhaiya.quizzen.domain.RootQuestion

/**
 * Pure-Kotlin saved-state persistence for [PracticeViewModel] using [SavedStateHandle].
 * Preserves the current [Question], typed [PracticeUiState.answer], and [PracticeUiState.remainingSeconds]
 * if Android kills the app in the background during a practice session.
 */
internal object PracticeSavedState {
    private const val KEY_HAS_SNAPSHOT = "practice_has_snapshot"
    private const val KEY_QUESTION = "practice_question"
    private const val KEY_ANSWER = "practice_answer"
    private const val KEY_REMAINING_SECONDS = "practice_remaining_seconds"

    fun save(
        handle: SavedStateHandle,
        state: PracticeUiState,
    ) {
        handle[KEY_HAS_SNAPSHOT] = true
        handle[KEY_QUESTION] = encodeQuestion(state.question)
        handle[KEY_ANSWER] = state.answer
        handle[KEY_REMAINING_SECONDS] = state.remainingSeconds
    }

    fun restore(
        handle: SavedStateHandle,
        config: PracticeConfig,
    ): PracticeUiState? {
        if (handle.get<Boolean>(KEY_HAS_SNAPSHOT) != true) return null
        val question = decodeQuestion(handle.get<String>(KEY_QUESTION).orEmpty()) ?: return null
        val remaining = (handle.get<Int>(KEY_REMAINING_SECONDS) ?: config.timerSeconds)
            .coerceIn(1, config.timerSeconds)
        val answer = handle.get<String>(KEY_ANSWER).orEmpty()
        return PracticeUiState(
            question = question,
            remainingSeconds = remaining,
            answer = answer,
        )
    }

    internal fun encodeQuestion(question: Question): String = when (question) {
        is ProductQuestion -> "prod:${question.left}:${question.right}"
        is PowerQuestion -> "pow:${question.base}:${question.exponent}"
        is RootQuestion -> "root:${question.degree}:${question.result}"
        is AlphabetQuestion -> "alpha:${question.challenge.code}:${question.position}"
        is FractionQuestion -> "frac:${question.challenge.code}:${question.denominator}:${question.decimal}"
    }

    internal fun decodeQuestion(raw: String): Question? {
        val parts = raw.split(':')
        if (parts.size < 3) return null
        return when (parts[0]) {
            "prod" -> {
                val left = parts[1].toLongOrNull() ?: return null
                val right = parts[2].toLongOrNull() ?: return null
                ProductQuestion(left, right)
            }
            "pow" -> {
                val base = parts[1].toLongOrNull() ?: return null
                val exp = parts[2].toIntOrNull() ?: return null
                PowerQuestion(base, exp)
            }
            "root" -> {
                val degree = parts[1].toIntOrNull() ?: return null
                val result = parts[2].toLongOrNull() ?: return null
                RootQuestion(degree, result)
            }
            "alpha" -> {
                val challenge = AlphabetChallenge.fromCode(parts[1]) ?: return null
                val pos = (parts[2].toIntOrNull() ?: return null).coerceIn(AlphabetRules.FIRST, AlphabetRules.SIZE)
                AlphabetQuestion(challenge, pos)
            }
            "frac" -> {
                val challenge = FractionChallenge.fromCode(parts[1]) ?: return null
                val denom = parts[2].toIntOrNull() ?: return null
                val decimal = parts.getOrNull(3)?.toBooleanStrictOrNull() ?: false
                FractionQuestion(challenge, denom, decimal)
            }
            else -> null
        }
    }
}
