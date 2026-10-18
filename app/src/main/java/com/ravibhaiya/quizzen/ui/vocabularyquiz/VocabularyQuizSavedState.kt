package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.lifecycle.SavedStateHandle
import com.ravibhaiya.quizzen.data.SettingsCodec
import com.ravibhaiya.quizzen.data.VocabularyJsonCodec
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizItem
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.domain.WordDetails
import java.util.Base64

internal data class RestoredVocabularyQuizSnapshot(
    val uiState: VocabularyQuizUiState,
    val outcomes: Map<Int, FeedbackType>,
)

/** What the setup screen shows about a stored unfinished quiz: how it was configured and where the user stopped. */
data class VocabularyResumeInfo(
    val config: VocabularyQuizConfig,
    /** 1-based number of the question the user will continue from. */
    val questionNumber: Int,
    val totalQuestions: Int,
)

/**
 * Pure-Kotlin saved-state persistence for [VocabularyQuizViewModel] using [SavedStateHandle].
 * Ensures that if Android kills the app in the background during a Vocabulary Study or Quiz session,
 * the active phase, studied words, generated questions, current question index, scores, remaining timer,
 * and per-word mistake tracking are all preserved seamlessly.
 *
 * The same pieces can also be written as one text ([encodeSession] / [decodeSession]) for `VocabularySessionStore`, so an
 * unfinished **Quiz** survives the app being closed (not only process death). That text is self-contained: it carries the
 * quiz setup and every generated question with its meanings, so it still works if the words were edited or deleted since.
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
    private const val SESSION_HEADER = "quizzen-vocabulary-session-v1"

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

        return buildSnapshot(
            config = config,
            phase = phase,
            studyEntries = VocabularyJsonCodec.decodeList(handle.get<String>(KEY_STUDY_JSON).orEmpty()),
            questions = decodeQuestions(handle.get<String>(KEY_QUESTIONS).orEmpty()),
            currentIndex = handle.get<Int>(KEY_CURRENT_INDEX),
            attemptedCount = handle.get<Int>(KEY_ATTEMPTED),
            correctCount = handle.get<Int>(KEY_CORRECT),
            wrongCount = handle.get<Int>(KEY_WRONG),
            timeoutCount = handle.get<Int>(KEY_TIMEOUT),
            selectedOption = handle.get<String>(KEY_SELECTED_OPTION),
            remainingSeconds = handle.get<Int>(KEY_REMAINING_SECONDS),
            feedbackTypeName = handle.get<String>(KEY_FEEDBACK_TYPE),
            feedbackAnswer = handle.get<String>(KEY_FEEDBACK_ANSWER),
            isLocked = handle.get<Boolean>(KEY_IS_LOCKED),
            outcomes = decodeOutcomes(handle.get<String>(KEY_OUTCOMES).orEmpty()),
        )
    }

    /**
     * The text kept on the device for an unfinished **Quiz** (`null` for the Study and Finished phases: there is nothing to
     * resume there). The timer is not stored: the screen starts the question again with a full timer (see [decodeSession]).
     */
    fun encodeSession(state: VocabularyQuizUiState, outcomes: Map<Int, FeedbackType>): String? {
        if (state.phase != VocabularyQuizPhase.Quiz || state.questions.isEmpty()) return null
        return buildList {
            add(SESSION_HEADER)
            add("config=${SettingsCodec.encode(state.config)}")
            add("study=${b64(VocabularyJsonCodec.encodeList(state.studyEntries))}")
            add("questions=${b64(encodeQuestions(state.questions))}")
            add("index=${state.currentIndex}")
            add("attempted=${state.attemptedCount}")
            add("correct=${state.correctCount}")
            add("wrong=${state.wrongCount}")
            add("timeout=${state.timeoutCount}")
            state.selectedOption?.let { add("selected=${b64(it)}") }
            add("locked=${if (state.isLocked) 1 else 0}")
            state.feedback?.let {
                add("ftype=${it.type.name}")
                add("fanswer=${b64(it.correctAnswer)}")
            }
            add("outcomes=${encodeOutcomes(outcomes)}")
        }.joinToString("\n")
    }

    /**
     * Reads the text written by [encodeSession]. Never trusts it: anything unreadable or that cannot be a real quiz position
     * (wrong header, no questions, counts that do not match the question index, a locked question without its feedback)
     * gives `null`, which means "nothing to resume".
     *
     * A question that was still open (not answered yet) restarts with a **full timer**; one that was already answered is
     * restored as answered (result and meanings visible, **Next** enabled).
     */
    fun decodeSession(text: String?): RestoredVocabularyQuizSnapshot? {
        if (text.isNullOrBlank()) return null
        val lines = text.lines()
        if (lines.firstOrNull()?.trim() != SESSION_HEADER) return null
        val values = lines.drop(1).mapNotNull { line ->
            line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()

        val config = SettingsCodec.decodeVocabulary(values["config"], VocabularyQuizRules.DEFAULT_TIMER_SECONDS)
            ?: return null
        val questions = decodeQuestions(unb64(values["questions"].orEmpty()))
        if (questions.isEmpty()) return null
        val index = values["index"]?.toIntOrNull() ?: return null
        val attempted = values["attempted"]?.toIntOrNull() ?: return null
        val isLocked = values["locked"] == "1"
        if (index !in questions.indices || attempted != index + if (isLocked) 1 else 0) return null

        val studyEntries = VocabularyJsonCodec.decodeList(unb64(values["study"].orEmpty()))
        if (studyEntries.isEmpty()) return null

        val snapshot = buildSnapshot(
            config = config,
            phase = VocabularyQuizPhase.Quiz,
            studyEntries = studyEntries,
            questions = questions,
            currentIndex = index,
            attemptedCount = attempted,
            correctCount = values["correct"]?.toIntOrNull(),
            wrongCount = values["wrong"]?.toIntOrNull(),
            timeoutCount = values["timeout"]?.toIntOrNull(),
            selectedOption = values["selected"]?.let(::unb64),
            remainingSeconds = config.timerSeconds,
            feedbackTypeName = values["ftype"],
            feedbackAnswer = values["fanswer"]?.let(::unb64),
            isLocked = isLocked,
            outcomes = decodeOutcomes(values["outcomes"].orEmpty()),
        ) ?: return null
        if (isLocked && snapshot.uiState.feedback == null) return null
        return snapshot
    }

    /** The short description of a stored quiz for the setup screen, or `null` when [text] cannot be resumed. */
    fun summarize(text: String?): VocabularyResumeInfo? =
        decodeSession(text)?.let { snapshot ->
            VocabularyResumeInfo(
                config = snapshot.uiState.config,
                questionNumber = snapshot.uiState.currentIndex + 1,
                totalQuestions = snapshot.uiState.totalCount,
            )
        }

    /** Applies the same repairs (non-negative counts, index and timer inside their limits) to every source of a snapshot. */
    private fun buildSnapshot(
        config: VocabularyQuizConfig,
        phase: VocabularyQuizPhase,
        studyEntries: List<VocabularyEntry>,
        questions: List<VocabularyQuizItem>,
        currentIndex: Int?,
        attemptedCount: Int?,
        correctCount: Int?,
        wrongCount: Int?,
        timeoutCount: Int?,
        selectedOption: String?,
        remainingSeconds: Int?,
        feedbackTypeName: String?,
        feedbackAnswer: String?,
        isLocked: Boolean?,
        outcomes: Map<Int, FeedbackType>,
    ): RestoredVocabularyQuizSnapshot? {
        if (phase != VocabularyQuizPhase.Study && questions.isEmpty()) return null

        val feedback = if (feedbackTypeName != null && feedbackAnswer != null) {
            FeedbackType.entries.firstOrNull { it.name == feedbackTypeName }?.let { Feedback(it, feedbackAnswer) }
        } else {
            null
        }

        return RestoredVocabularyQuizSnapshot(
            uiState = VocabularyQuizUiState(
                config = config,
                phase = phase,
                studyEntries = studyEntries,
                questions = questions,
                currentIndex = (currentIndex ?: 0).coerceIn(0, (questions.size - 1).coerceAtLeast(0)),
                attemptedCount = (attemptedCount ?: 0).coerceAtLeast(0),
                correctCount = (correctCount ?: 0).coerceAtLeast(0),
                wrongCount = (wrongCount ?: 0).coerceAtLeast(0),
                timeoutCount = (timeoutCount ?: 0).coerceAtLeast(0),
                selectedOption = selectedOption,
                remainingSeconds = (remainingSeconds ?: config.timerSeconds).coerceIn(0, config.timerSeconds),
                feedback = feedback,
                isLocked = isLocked ?: false,
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
