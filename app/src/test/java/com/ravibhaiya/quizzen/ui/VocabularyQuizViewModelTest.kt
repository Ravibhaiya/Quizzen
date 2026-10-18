package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.navigation.Routes
import com.ravibhaiya.quizzen.ui.navigation.VocabularyQuizArgs
import com.ravibhaiya.quizzen.ui.vocabularyquiz.VocabularyQuizPhase
import com.ravibhaiya.quizzen.ui.vocabularyquiz.VocabularyQuizViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyQuizViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeVocabularyRepo(
        initial: List<VocabularyEntry>,
        initialMistakes: Set<String> = emptySet(),
    ) : VocabularyRepository {
        override val entries: Flow<List<VocabularyEntry>> = MutableStateFlow(initial)
        private val _mistakes = MutableStateFlow(initialMistakes)
        override val mistakeWords: Flow<Set<String>> = _mistakes
        override val syncError: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry> = Result.success(entry)
        override suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>> = Result.success(entries)
        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun recordMistakeWords(wordKeys: Set<String>) {
            _mistakes.value = _mistakes.value + wordKeys
        }
        override suspend fun removeMistakeWords(wordKeys: Set<String>) {
            _mistakes.value = _mistakes.value - wordKeys
        }
        override fun clearSyncError() = Unit

        val currentMistakes: Set<String>
            get() = _mistakes.value
    }

    private val sampleEntries = listOf(
        VocabularyEntry(
            id = "w1",
            wordDetails = WordDetails(word = "Bright", meaningEnglish = "Shining"),
            synonyms = listOf(
                WordDetails(word = "Radiant", meaningEnglish = "Glowing"),
                WordDetails(word = "Luminous", meaningEnglish = "Shining"),
            ),
            antonyms = listOf(
                WordDetails(word = "Dim", meaningEnglish = "Faint"),
            ),
        ),
        VocabularyEntry(
            id = "w2",
            wordDetails = WordDetails(word = "Swift", meaningEnglish = "Fast"),
            synonyms = listOf(
                WordDetails(word = "Rapid", meaningEnglish = "Quick"),
            ),
            antonyms = listOf(
                WordDetails(word = "Sluggish", meaningEnglish = "Slow"),
            ),
        ),
    )

    @Test
    fun `vocabulary route and args round trip and repair invalid values`() {
        val config = VocabularyQuizConfig(
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            wordCount = 15,
            timerSeconds = 12,
        )
        val route = Routes.vocabularyQuiz(config)
        assertEquals("vocabulary_quiz?vmodes=syn-ant&wcount=15&seconds=12", route)

        val decoded = VocabularyQuizArgs.decode("syn-ant", 15, 12)
        assertEquals(config, decoded)

        val repaired = VocabularyQuizArgs.decode("invalid", 99, 0)
        assertEquals(VocabularyQuizMode.DEFAULT, repaired.modes)
        assertEquals(VocabularyQuizRules.MAX_WORDS, repaired.wordCount)
        assertEquals(VocabularyQuizRules.DEFAULT_TIMER_SECONDS, repaired.timerSeconds)
    }

    @Test
    fun `starts in Study phase with selected words, then runs finite Quiz and ends in Finished phase`() = runTest(dispatcher) {
        val config = VocabularyQuizConfig(
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            wordCount = 5,
            timerSeconds = 10,
        )
        val vm = VocabularyQuizViewModel(
            config = config,
            repository = FakeVocabularyRepo(sampleEntries),
            random = Random(11),
        )
        runCurrent()

        // 1. Study Phase
        assertEquals(VocabularyQuizPhase.Study, vm.state.value.phase)
        assertEquals(2, vm.state.value.studyEntries.size)

        // 2. Start Quiz
        vm.startQuiz()
        assertEquals(VocabularyQuizPhase.Quiz, vm.state.value.phase)
        val total = vm.state.value.totalCount
        // w1 has 1 main + 2 syn + 1 ant = 4 words; w2 has 1 main + 1 syn + 1 ant = 3 words -> 7 total questions
        assertEquals(7, total)
        assertEquals(0, vm.state.value.attemptedCount)

        // 3. Answer all 7 questions and verify it stays on the current question until nextQuestion() is called
        repeat(total) { index ->
            val currentQ = vm.state.value.currentQuestion
            assertNotNull(currentQ)
            assertEquals(index, vm.state.value.currentIndex)
            assertEquals(index, vm.state.value.attemptedCount)
            assertFalse(vm.state.value.isLocked)

            // Calling nextQuestion() before answering is a no-op
            vm.nextQuestion()
            assertEquals(index, vm.state.value.currentIndex)

            val chosen = if (index == 0) {
                currentQ!!.options.first { !currentQ.isCorrect(it) }
            } else {
                currentQ!!.correctAnswer
            }
            vm.selectOption(chosen)

            assertEquals(index + 1, vm.state.value.attemptedCount)
            assertTrue(vm.state.value.isLocked)
            assertEquals(
                if (index == 0) FeedbackType.Incorrect else FeedbackType.Correct,
                vm.state.value.feedback?.type,
            )

            // Verify meanings are available on the prompt word and all 4 options
            assertTrue(currentQ.promptDetails.meaningEnglish.isNotBlank())
            currentQ.options.forEach { opt ->
                assertTrue(currentQ.detailsForOption(opt).meaningEnglish.isNotBlank())
            }

            // Does NOT auto-advance even after waiting several seconds
            advanceTimeBy(5_000L)
            runCurrent()
            assertEquals(VocabularyQuizPhase.Quiz, vm.state.value.phase)
            assertEquals(index, vm.state.value.currentIndex)
            assertTrue(vm.state.value.isLocked)

            // Clicking Next advances to the next question (or finishes after the last one)
            vm.nextQuestion()
        }

        // 4. Quiz ends when Next is clicked after all words are attempted
        assertEquals(VocabularyQuizPhase.Finished, vm.state.value.phase)
        assertEquals(total, vm.state.value.attemptedCount)
        assertEquals(total - 1, vm.state.value.correctCount)
        assertEquals(1, vm.state.value.wrongCount)
    }

    @Test
    fun `timeout locks question to show meanings and waits for nextQuestion click`() = runTest(dispatcher) {
        val config = VocabularyQuizConfig(
            modes = setOf(VocabularyQuizMode.Synonyms),
            wordCount = 5,
            timerSeconds = 3,
        )
        val vm = VocabularyQuizViewModel(
            config = config,
            repository = FakeVocabularyRepo(sampleEntries),
            random = Random(5),
        )
        runCurrent()
        vm.startQuiz()
        vm.onResume()

        advanceTimeBy(3_100)
        assertEquals(0, vm.state.value.remainingSeconds)
        assertEquals(FeedbackType.Timeout, vm.state.value.feedback?.type)
        assertTrue(vm.state.value.isLocked)
        assertEquals(0, vm.state.value.currentIndex)
        assertEquals(1, vm.state.value.attemptedCount)
        assertEquals(1, vm.state.value.timeoutCount)

        // Remains on question 0 until Next is clicked
        advanceTimeBy(5_000)
        assertEquals(0, vm.state.value.currentIndex)
        assertTrue(vm.state.value.isLocked)

        vm.nextQuestion()
        assertEquals(1, vm.state.value.currentIndex)
        assertFalse(vm.state.value.isLocked)
        assertEquals(3, vm.state.value.remainingSeconds)
        vm.onPause()
    }

    @Test
    fun `wrong answer saves word locally, chooses it next time, and automatically deletes it when all its questions are answered right`() = runTest(dispatcher) {
        val repo = FakeVocabularyRepo(VocabularyQuizRules.FALLBACK_ENTRIES)
        val config = VocabularyQuizConfig(
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            wordCount = 5,
            timerSeconds = 10,
        )

        // Session 1: User gets a question wrong on the first word cluster ("missedClusterId"), answers everything else right
        val vm1 = VocabularyQuizViewModel(
            config = config,
            repository = repo,
            random = Random(21),
        )
        runCurrent()
        vm1.startQuiz()

        val firstQ = vm1.state.value.currentQuestion!!
        val missedClusterId = firstQ.clusterId
        val missedWordLower = firstQ.clusterWord.lowercase()

        repeat(vm1.state.value.totalCount) { idx ->
            val q = vm1.state.value.currentQuestion!!
            val chosen = if (idx == 0) {
                q.options.first { !q.isCorrect(it) }
            } else {
                q.correctAnswer
            }
            vm1.selectOption(chosen)
            runCurrent()
            vm1.nextQuestion()
        }

        // Verify the missed word is saved locally in repository.mistakeWords
        assertTrue(missedWordLower in repo.currentMistakes || missedClusterId.lowercase() in repo.currentMistakes)

        // Session 2: Starting a new quiz prioritizes the saved mistake word in studyEntries
        val vm2 = VocabularyQuizViewModel(
            config = config,
            repository = repo,
            random = Random(99),
        )
        runCurrent()
        assertTrue(
            "Expected saved mistake word '$missedWordLower' to be chosen for the next quiz",
            vm2.state.value.studyEntries.any { it.wordDetails.word.equals(missedWordLower, ignoreCase = true) },
        )

        // Answer EVERY question in Session 2 correctly -> the saved mistake word is automatically deleted!
        vm2.startQuiz()
        repeat(vm2.state.value.totalCount) {
            val q = vm2.state.value.currentQuestion!!
            vm2.selectOption(q.correctAnswer)
            runCurrent()
            vm2.nextQuestion()
        }

        assertFalse(missedWordLower in repo.currentMistakes)
        assertFalse(missedClusterId.lowercase() in repo.currentMistakes)
    }
}

