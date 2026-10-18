package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizEngine
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.ui.vocabularyquiz.VocabularyQuizPhase
import com.ravibhaiya.quizzen.ui.vocabularyquiz.VocabularyQuizSavedState
import com.ravibhaiya.quizzen.ui.vocabularyquiz.VocabularyQuizUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The text that keeps an unfinished Vocabulary quiz on the device: round trip, timer rule and rejection of bad text. */
class VocabularyQuizSavedStateTest {

    private val config = VocabularyQuizConfig(
        modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
        wordCount = 5,
        timerSeconds = 12,
    )
    private val study = VocabularyQuizRules.FALLBACK_ENTRIES.take(5)
    private val questions = VocabularyQuizEngine.generateQuiz(study, study, config.modes, Random(3))

    private fun quizState(
        index: Int = 2,
        locked: Boolean = false,
        remaining: Int = 4,
        phase: VocabularyQuizPhase = VocabularyQuizPhase.Quiz,
    ) = VocabularyQuizUiState(
        config = config,
        phase = phase,
        studyEntries = study,
        questions = questions,
        currentIndex = index,
        attemptedCount = index + if (locked) 1 else 0,
        correctCount = index,
        wrongCount = if (locked) 1 else 0,
        timeoutCount = 0,
        selectedOption = if (locked) questions[index].options.first { !questions[index].isCorrect(it) } else null,
        remainingSeconds = remaining,
        feedback = if (locked) Feedback(FeedbackType.Incorrect, questions[index].correctAnswer) else null,
        isLocked = locked,
    )

    private val outcomes = mapOf(0 to FeedbackType.Correct, 1 to FeedbackType.Correct)

    @Test
    fun `an open question is restored with every question, the score and a full timer`() {
        val text = VocabularyQuizSavedState.encodeSession(quizState(index = 2, locked = false, remaining = 4), outcomes)
        val restored = VocabularyQuizSavedState.decodeSession(text)

        assertNotNull(restored)
        val ui = restored!!.uiState
        assertEquals(VocabularyQuizPhase.Quiz, ui.phase)
        assertEquals(config, ui.config)
        assertEquals(questions, ui.questions)
        assertEquals(study.map { it.wordDetails.word }, ui.studyEntries.map { it.wordDetails.word })
        assertEquals(2, ui.currentIndex)
        assertEquals(2, ui.attemptedCount)
        assertEquals(2, ui.correctCount)
        assertEquals(config.timerSeconds, ui.remainingSeconds) // not the 4 s that were left
        assertFalse(ui.isLocked)
        assertNull(ui.feedback)
        assertEquals(outcomes, restored.outcomes)
    }

    @Test
    fun `an answered question is restored as answered with its result and Next ready`() {
        val state = quizState(index = 3, locked = true, remaining = 2)
        val restored = VocabularyQuizSavedState.decodeSession(VocabularyQuizSavedState.encodeSession(state, outcomes))!!.uiState

        assertTrue(restored.isLocked)
        assertEquals(3, restored.currentIndex)
        assertEquals(4, restored.attemptedCount)
        assertEquals(state.selectedOption, restored.selectedOption)
        assertEquals(state.feedback, restored.feedback)
    }

    @Test
    fun `only a running quiz is written, never the study or finished phase`() {
        assertNull(VocabularyQuizSavedState.encodeSession(quizState(phase = VocabularyQuizPhase.Study), outcomes))
        assertNull(VocabularyQuizSavedState.encodeSession(quizState(phase = VocabularyQuizPhase.Finished), outcomes))
        assertNull(VocabularyQuizSavedState.encodeSession(quizState().copy(questions = emptyList()), outcomes))
    }

    @Test
    fun `summary tells the question the user will continue from`() {
        val text = VocabularyQuizSavedState.encodeSession(quizState(index = 4), outcomes)
        val info = VocabularyQuizSavedState.summarize(text)!!

        assertEquals(5, info.questionNumber)
        assertEquals(questions.size, info.totalQuestions)
        assertEquals(config, info.config)
    }

    @Test
    fun `text that cannot be a real quiz position is rejected`() {
        val good = VocabularyQuizSavedState.encodeSession(quizState(index = 2), outcomes)!!

        assertNull(VocabularyQuizSavedState.decodeSession(null))
        assertNull(VocabularyQuizSavedState.decodeSession(""))
        assertNull(VocabularyQuizSavedState.decodeSession("not a quiz"))
        assertNull(VocabularyQuizSavedState.decodeSession(good.replaceFirst("quizzen-vocabulary-session-v1", "other")))
        // the question index is outside the questions
        assertNull(VocabularyQuizSavedState.decodeSession(good.replace("index=2", "index=999")))
        // attempted does not fit the index
        assertNull(VocabularyQuizSavedState.decodeSession(good.replace("attempted=2", "attempted=7")))
        // no questions / no study words / no readable setup
        assertNull(VocabularyQuizSavedState.decodeSession(good.replace(Regex("(?m)^questions=.*$"), "questions=")))
        assertNull(VocabularyQuizSavedState.decodeSession(good.replace(Regex("(?m)^study=.*$"), "study=")))
        assertNull(VocabularyQuizSavedState.decodeSession(good.replace(Regex("(?m)^config=.*$"), "config=junk")))
        // answered but the result is missing
        val locked = VocabularyQuizSavedState.encodeSession(quizState(index = 2, locked = true), outcomes)!!
        assertNull(VocabularyQuizSavedState.decodeSession(locked.replace(Regex("(?m)^ftype=.*\\n"), "")))
    }
}
