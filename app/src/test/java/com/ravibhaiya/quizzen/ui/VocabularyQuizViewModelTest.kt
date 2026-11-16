package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.VocabularyRules
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
import org.junit.Assert.assertNull
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
        private val _entries = MutableStateFlow(initial)
        override val entries: Flow<List<VocabularyEntry>> = _entries
        private val _mistakes = MutableStateFlow(initialMistakes)
        override val mistakeWords: Flow<Set<String>> = _mistakes
        override val syncError: Flow<String?> = MutableStateFlow(null)
        override suspend fun saveEntry(entry: VocabularyEntry): Result<VocabularyEntry> = Result.success(entry)
        override suspend fun saveEntries(entries: List<VocabularyEntry>): Result<List<VocabularyEntry>> = Result.success(entries)
        override suspend fun mergeIntoExisting(incoming: List<VocabularyEntry>): Result<VocabularyRules.MergeSummary> =
            Result.success(VocabularyRules.MergeSummary(0, 0, 0, 0, false))
        override suspend fun deleteEntry(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun recordMistakeWords(wordKeys: Set<String>) {
            _mistakes.value = _mistakes.value + wordKeys
        }
        override suspend fun removeMistakeWords(wordKeys: Set<String>) {
            _mistakes.value = _mistakes.value - wordKeys
        }
        override fun clearSyncError() = Unit

        fun setEntries(next: List<VocabularyEntry>) {
            _entries.value = next
        }
    }

    private val sampleEntries = listOf(
        VocabularyEntry(
            id = "w1",
            wordDetails = WordDetails(word = "Bright", partOfSpeech = "Adjective", meaningEnglish = "Shining"),
            synonyms = listOf(
                WordDetails(word = "Radiant", partOfSpeech = "Adjective", meaningEnglish = "Glowing"),
            ),
            antonyms = listOf(
                WordDetails(word = "Dim", partOfSpeech = "Adjective", meaningEnglish = "Faint"),
            ),
        ),
    )

    @Test
    fun `1-word quiz runs through Study, Quiz, and Finished phases and clears session on completion`() = runTest(dispatcher) {
        val config = VocabularyQuizConfig(
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            wordCount = 1,
            timerSeconds = 10,
        )
        assertEquals("vocabulary_quiz?vmodes=syn-ant&wcount=1&seconds=10", Routes.vocabularyQuiz(config))
        assertEquals(config, VocabularyQuizArgs.decode("syn-ant", 1, 10))

        val store = FakeVocabularySessionStore()
        val vm = VocabularyQuizViewModel(
            config = config,
            repository = FakeVocabularyRepo(sampleEntries),
            random = Random(7),
            sessionStore = store,
        )
        runCurrent()

        assertEquals(VocabularyQuizPhase.Study, vm.state.value.phase)
        assertEquals(1, vm.state.value.studyEntries.size)

        vm.startQuiz()
        assertEquals(VocabularyQuizPhase.Quiz, vm.state.value.phase)
        assertEquals(3, vm.state.value.totalCount)
        assertNotNull(store.text)

        repeat(vm.state.value.totalCount) { idx ->
            val q = vm.state.value.currentQuestion!!
            assertEquals("Bright", q.clusterWord)
            assertEquals("Adjective", q.clusterPartOfSpeech)
            vm.selectOption(q.correctAnswer)
            assertTrue(vm.state.value.isLocked)
            assertEquals(FeedbackType.Correct, vm.state.value.feedback?.type)
            assertEquals(idx + 1, vm.state.value.attemptedCount)
            vm.nextQuestion()
        }

        assertEquals(VocabularyQuizPhase.Finished, vm.state.value.phase)
        assertEquals(3, vm.state.value.correctCount)
        assertNull(store.text)
    }

    @Test
    fun `empty database shows empty Study list and deleting all words clears studyEntries`() = runTest(dispatcher) {
        val repo = FakeVocabularyRepo(sampleEntries)
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(wordCount = 1),
            repository = repo,
            random = Random(3),
        )
        runCurrent()
        assertEquals(1, vm.state.value.studyEntries.size)

        repo.setEntries(emptyList())
        runCurrent()
        assertTrue(vm.state.value.studyEntries.isEmpty())
    }

    @Test
    fun `timeout locks question and waits for nextQuestion`() = runTest(dispatcher) {
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(modes = setOf(VocabularyQuizMode.Synonyms), wordCount = 1, timerSeconds = 3),
            repository = FakeVocabularyRepo(sampleEntries),
            random = Random(5),
        )
        runCurrent()
        vm.startQuiz()
        vm.onResume()

        advanceTimeBy(3_100)
        assertTrue(vm.state.value.isLocked)
        assertEquals(FeedbackType.Timeout, vm.state.value.feedback?.type)
        assertEquals(0, vm.state.value.currentIndex)

        vm.nextQuestion()
        assertEquals(1, vm.state.value.currentIndex)
        assertFalse(vm.state.value.isLocked)
        vm.onPause()
    }
}
