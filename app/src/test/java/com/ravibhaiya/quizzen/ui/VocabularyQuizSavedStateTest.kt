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

class VocabularyQuizSavedStateTest {

    private val config = VocabularyQuizConfig(
        modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
        wordCount = 2,
        timerSeconds = 12,
    )
    private val study = VocabularyQuizRules.FALLBACK_ENTRIES.take(2)
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
    fun `encodeSession and decodeSession round-trip questions, partOfSpeech details, and full timer`() {
        val text = VocabularyQuizSavedState.encodeSession(quizState(index = 2, locked = false, remaining = 4), outcomes)
        val restored = VocabularyQuizSavedState.decodeSession(text)

        assertNotNull(restored)
        val ui = restored!!.uiState
        assertEquals(VocabularyQuizPhase.Quiz, ui.phase)
        assertEquals(config, ui.config)
        assertEquals(questions, ui.questions)
        assertEquals(2, ui.currentIndex)
        assertEquals(config.timerSeconds, ui.remainingSeconds)
        assertFalse(ui.isLocked)
        assertEquals(outcomes, restored.outcomes)
        assertTrue(ui.questions.all { it.clusterPartOfSpeech.isNotBlank() })
    }

    @Test
    fun `locked question restores as locked and non-Quiz phases return null`() {
        val lockedState = quizState(index = 1, locked = true, remaining = 2)
        val restored = VocabularyQuizSavedState.decodeSession(
            VocabularyQuizSavedState.encodeSession(lockedState, outcomes),
        )!!.uiState
        assertTrue(restored.isLocked)
        assertEquals(lockedState.selectedOption, restored.selectedOption)
        assertEquals(lockedState.feedback, restored.feedback)

        assertNull(VocabularyQuizSavedState.encodeSession(quizState(phase = VocabularyQuizPhase.Study), outcomes))
        assertNull(VocabularyQuizSavedState.encodeSession(quizState(phase = VocabularyQuizPhase.Finished), outcomes))
        assertNull(VocabularyQuizSavedState.decodeSession("corrupted_session_data"))
    }
}
