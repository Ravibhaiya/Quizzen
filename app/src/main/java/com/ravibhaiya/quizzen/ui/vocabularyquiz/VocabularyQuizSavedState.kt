package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.lifecycle.SavedStateHandle
import com.ravibhaiya.quizzen.data.VocabularyJsonCodec
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizItem
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.WordDetails
import java.util.Base64

internal data class RestoredVocabularyQuizSnapshot(
    val uiState: VocabularyQuizUiState,
    val outcomes: Map<Int, FeedbackType>,
)

/**
 * Pure-Kotlin saved-state persistence for [VocabularyQuizViewModel] using [SavedStateHandle].
 * Ensures that if Android kills the app in the background during a Vocabulary Study or Quiz session,
 * the active phase, studied words, generated questions, current question index, scores, remaining timer,
 * and per-word mistake tracking are all preserved seamlessly.
 */
internal object VocabularyQuizSavedState {
    private const val KEY_HAS_SNAPSHOT = "vocab_quiz_has_snapshot"
    private const val KEY_PHASE = "vocab_quiz_phase"
    private const val KEY_STUDY_JSON = "vocab_quiz_study_json"
    private const val KEY_QUESTIONS = "vocab_quiz_questions"
    private const val KEY_CURRENT_INDEX = "vocab_quiz_current_index"
    private const val KEY_ATTEMPTED = "vocab_quiz_attempted"
    private const val KEY_CORRECT = "vocab_quiz_correct"
    private const val KEY_WRONG = "vocab_quiz_wrong"
    private const val KEY_TIMEOUT = "vocab_quiz_timeout"
    private const val KEY_SELECTED_OPTION = "vocab_quiz_selected_option"
    private const val KEY_REMAINING_SECONDS = "vocab_quiz_remaining_seconds"
    private const val KEY_FEEDBACK_TYPE = "vocab_quiz_feedback_type"
    private const val KEY_FEEDBACK_ANSWER = "vocab_quiz_feedback_answer"
    private const val KEY_IS_LOCKED = "vocab_quiz_is_locked"
    private const val KEY_OUTCOMES = "vocab_quiz_outcomes"

    fun save(
        handle: SavedStateHandle,
        state: VocabularyQuizUiState,
        outcomes: Map<Int, FeedbackType>,
    ) {
        handle[KEY_HAS_SNAPSHOT] = true
        handle[KEY_PHASE] = state.phase.name
        handle[KEY_STUDY_JSON] = VocabularyJsonCodec.encodeList(state.studyEntries)
        handle[KEY_QUESTIONS] = encodeQuestions(state.questions)
        handle[KEY_CURRENT_INDEX] = state.currentIndex
        handle[KEY_ATTEMPTED] = state.attemptedCount
        handle[KEY_CORRECT] = state.correctCount
        handle[KEY_WRONG] = state.wrongCount
        handle[KEY_TIMEOUT] = state.timeoutCount
        handle[KEY_SELECTED_OPTION] = state.selectedOption
        handle[KEY_REMAINING_SECONDS] = state.remainingSeconds
        handle[KEY_FEEDBACK_TYPE] = state.feedback?.type?.name
        handle[KEY_FEEDBACK_ANSWER] = state.feedback?.correctAnswer
        handle[KEY_IS_LOCKED] = state.isLocked
        handle[KEY_OUTCOMES] = encodeOutcomes(outcomes)
    }

    fun restore(
        handle: SavedStateHandle,
        config: VocabularyQuizConfig,
    ): RestoredVocabularyQuizSnapshot? {
        if (handle.get<Boolean>(KEY_HAS_SNAPSHOT) != true) return null
        val phaseName = handle.get<String>(KEY_PHASE) ?: return null
        val phase = VocabularyQuizPhase.entries.firstOrNull { it.name == phaseName } ?: return null

        val studyEntries = VocabularyJsonCodec.decodeList(handle.get<String>(KEY_STUDY_JSON).orEmpty())
        val questions = decodeQuestions(handle.get<String>(KEY_QUESTIONS).orEmpty())
        if (phase != VocabularyQuizPhase.Study && questions.isEmpty()) return null

        val currentIndex = (handle.get<Int>(KEY_CURRENT_INDEX) ?: 0)
            .coerceIn(0, (questions.size - 1).coerceAtLeast(0))
        val attemptedCount = (handle.get<Int>(KEY_ATTEMPTED) ?: 0).coerceAtLeast(0)
        val correctCount = (handle.get<Int>(KEY_CORRECT) ?: 0).coerceAtLeast(0)
        val wrongCount = (handle.get<Int>(KEY_WRONG) ?: 0).coerceAtLeast(0)
        val timeoutCount = (handle.get<Int>(KEY_TIMEOUT) ?: 0).coerceAtLeast(0)
        val selectedOption = handle.get<String>(KEY_SELECTED_OPTION)
        val remainingSeconds = (handle.get<Int>(KEY_REMAINING_SECONDS) ?: config.timerSeconds)
            .coerceIn(0, config.timerSeconds)
        val isLocked = handle.get<Boolean>(KEY_IS_LOCKED) ?: false

        val feedbackTypeName = handle.get<String>(KEY_FEEDBACK_TYPE)
        val feedbackAnswer = handle.get<String>(KEY_FEEDBACK_ANSWER)
        val feedback = if (feedbackTypeName != null && feedbackAnswer != null) {
            val type = FeedbackType.entries.firstOrNull { it.name == feedbackTypeName }
            type?.let { Feedback(it, feedbackAnswer) }
        } else {
            null
        }

        val outcomes = decodeOutcomes(handle.get<String>(KEY_OUTCOMES).orEmpty())

        return RestoredVocabularyQuizSnapshot(
            uiState = VocabularyQuizUiState(
                config = config,
                phase = phase,
                studyEntries = studyEntries,
                questions = questions,
                currentIndex = currentIndex,
                attemptedCount = attemptedCount,
                correctCount = correctCount,
                wrongCount = wrongCount,
                timeoutCount = timeoutCount,
                selectedOption = selectedOption,
                remainingSeconds = remainingSeconds,
                feedback = feedback,
                isLocked = isLocked,
            ),
            outcomes = outcomes,
        )
    }

    private fun encodeOutcomes(outcomes: Map<Int, FeedbackType>): String =
        outcomes.entries.joinToString(",") { (idx, type) -> "$idx:${type.name}" }

    private fun decodeOutcomes(raw: String): Map<Int, FeedbackType> {
        if (raw.isBlank()) return emptyMap()
        val result = LinkedHashMap<Int, FeedbackType>()
        for (token in raw.split(',')) {
            val parts = token.split(':')
            if (parts.size != 2) continue
            val idx = parts[0].toIntOrNull() ?: continue
            val type = FeedbackType.entries.firstOrNull { it.name == parts[1] } ?: continue
            result[idx] = type
        }
        return result
    }

    private fun encodeQuestions(questions: List<VocabularyQuizItem>): String =
        questions.joinToString("\n") { q ->
            listOf(
                b64(q.clusterId),
                q.mode.code,
                b64(q.promptWord),
                b64(q.correctAnswer),
                q.options.joinToString(";") { b64(it) },
                encodeDetails(q.promptDetails),
                q.optionDetails.joinToString(";") { encodeDetails(it) },
                b64(q.clusterWord),
            ).joinToString("|")
        }

    private fun decodeQuestions(raw: String): List<VocabularyQuizItem> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeQuestionLine)
            .toList()
    }

    private fun decodeQuestionLine(line: String): VocabularyQuizItem? {
        val parts = line.split('|')
        if (parts.size < 8) return null
        val clusterId = unb64(parts[0])
        val mode = VocabularyQuizMode.fromCode(parts[1]) ?: return null
        val promptWord = unb64(parts[2])
        val correctAnswer = unb64(parts[3])
        val options = parts[4].split(';').filter { it.isNotBlank() }.map(::unb64)
        if (promptWord.isBlank() || correctAnswer.isBlank() || options.isEmpty()) return null
        val promptDetails = decodeDetails(parts[5]) ?: WordDetails(word = promptWord)
        val optionDetails = parts[6].split(';').filter { it.isNotBlank() }.mapNotNull(::decodeDetails)
        val clusterWord = unb64(parts[7])
        return VocabularyQuizItem(
            clusterId = clusterId,
            mode = mode,
            promptWord = promptWord,
            correctAnswer = correctAnswer,
            options = options,
            promptDetails = promptDetails,
            optionDetails = optionDetails.ifEmpty { options.map { WordDetails(word = it) } },
            clusterWord = clusterWord,
        )
    }

    private fun encodeDetails(details: WordDetails): String =
        listOf(
            b64(details.word),
            b64(details.meaningEnglish),
            b64(details.meaningHinglish),
            b64(details.exampleSentence),
        ).joinToString(":")

    private fun decodeDetails(raw: String): WordDetails? {
        val parts = raw.split(':')
        if (parts.size < 4) return null
        return WordDetails(
            word = unb64(parts[0]),
            meaningEnglish = unb64(parts[1]),
            meaningHinglish = unb64(parts[2]),
            exampleSentence = unb64(parts[3]),
        )
    }

    private fun b64(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun unb64(encoded: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    }.getOrDefault("")
}
