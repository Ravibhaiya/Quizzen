package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.VocabularyRules
import com.ravibhaiya.quizzen.domain.WordDetails
import com.ravibhaiya.quizzen.ui.components.STREAK_CELEBRATION_DURATION_MS
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
        initialMistakesLoaded: Boolean = true,
    ) : VocabularyRepository {
        private val _entries = MutableStateFlow(initial)
        override val entries: Flow<List<VocabularyEntry>> = _entries
        private val _mistakes = MutableStateFlow(initialMistakes)
        override val mistakeWords: Flow<Set<String>> = _mistakes
        private val _mistakesLoaded = MutableStateFlow(initialMistakesLoaded)
        override val mistakeWordsLoaded: Flow<Boolean> = _mistakesLoaded
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

        fun completeMistakesLoad(mistakes: Set<String>) {
            _mistakes.value = mistakes
            _mistakesLoaded.value = true
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
            questionCount = 10,
            timerSeconds = 10,
        )
        assertEquals("vocabulary_quiz?vmodes=syn-ant&wcount=1&qcount=10&seconds=10", Routes.vocabularyQuiz(config))
        assertEquals(config, VocabularyQuizArgs.decode("syn-ant", 1, 10, questionCount = 10))

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

    @Test
    fun `shuffleStudyWords switches to a different random word even when mistakeWords exist`() = runTest(dispatcher) {
        val entries = listOf(
            sampleEntries.first(),
            VocabularyEntry(
                id = "w2",
                wordDetails = WordDetails(word = "Swift", partOfSpeech = "Adjective", meaningEnglish = "Fast"),
                synonyms = listOf(WordDetails(word = "Rapid", partOfSpeech = "Adjective", meaningEnglish = "Quick")),
                antonyms = listOf(WordDetails(word = "Slow", partOfSpeech = "Adjective", meaningEnglish = "Sluggish")),
            ),
        )
        val repo = FakeVocabularyRepo(entries, initialMistakes = setOf("bright"))
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(wordCount = 1),
            repository = repo,
            random = Random(12),
        )
        runCurrent()

        val initialWord = vm.state.value.studyEntries.single().wordDetails.word
        vm.shuffleStudyWords()
        val secondWord = vm.state.value.studyEntries.single().wordDetails.word
        assertFalse("Expected shuffleStudyWords to change the current study word", initialWord == secondWord)

        vm.shuffleStudyWords()
        val thirdWord = vm.state.value.studyEntries.single().wordDetails.word
        assertEquals(initialWord, thirdWord)
    }

    @Test
    fun `startQuiz respects configured questionCount limit`() = runTest(dispatcher) {
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(
                modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
                wordCount = 1,
                questionCount = 2,
                timerSeconds = 10,
            ),
            repository = FakeVocabularyRepo(sampleEntries),
            random = Random(7),
        )
        runCurrent()

        vm.startQuiz()
        assertEquals(VocabularyQuizPhase.Quiz, vm.state.value.phase)
        assertEquals(2, vm.state.value.totalCount)
    }

    @Test
    fun `initial study selection prioritizes saved mistake words from repository`() = runTest(dispatcher) {
        val swiftEntry = VocabularyEntry(
            id = "w2",
            wordDetails = WordDetails(word = "Swift", partOfSpeech = "Adjective", meaningEnglish = "Fast"),
            synonyms = listOf(WordDetails(word = "Rapid", partOfSpeech = "Adjective", meaningEnglish = "Quick")),
            antonyms = listOf(WordDetails(word = "Slow", partOfSpeech = "Adjective", meaningEnglish = "Sluggish")),
        )
        val entries = listOf(sampleEntries.first(), swiftEntry)

        for (seed in 1..10) {
            val vm = VocabularyQuizViewModel(
                config = VocabularyQuizConfig(wordCount = 1),
                repository = FakeVocabularyRepo(entries, initialMistakes = setOf("swift")),
                random = Random(seed),
            )
            runCurrent()
            assertEquals(listOf("Swift"), vm.state.value.studyEntries.map { it.wordDetails.word })
        }
    }

    @Test
    fun `missed questions are tracked in outcomes and retryMissedWords quizzes only missed words`() = runTest(dispatcher) {
        val swiftEntry = VocabularyEntry(
            id = "w2",
            wordDetails = WordDetails(word = "Swift", partOfSpeech = "Adjective", meaningEnglish = "Fast"),
            synonyms = listOf(WordDetails(word = "Rapid", partOfSpeech = "Adjective", meaningEnglish = "Quick")),
            antonyms = listOf(WordDetails(word = "Slow", partOfSpeech = "Adjective", meaningEnglish = "Sluggish")),
        )
        val entries = listOf(sampleEntries.first(), swiftEntry)
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(
                modes = setOf(VocabularyQuizMode.Synonyms),
                wordCount = 2,
                questionCount = 20,
                timerSeconds = 10,
            ),
            repository = FakeVocabularyRepo(entries),
            random = Random(4),
        )
        runCurrent()
        vm.startQuiz()

        var missedClusterWord: String? = null
        repeat(vm.state.value.totalCount) {
            val q = vm.state.value.currentQuestion!!
            if (missedClusterWord == null && q.clusterWord == "Swift") {
                missedClusterWord = q.clusterWord
                val wrongOption = q.options.first { !q.isCorrect(it) }
                vm.selectOption(wrongOption)
            } else {
                vm.selectOption(q.correctAnswer)
            }
            vm.nextQuestion()
        }

        val finished = vm.state.value
        assertEquals(VocabularyQuizPhase.Finished, finished.phase)
        assertEquals(1, finished.missedQuestions.size)
        assertEquals(FeedbackType.Incorrect, finished.missedQuestions.single().second)
        assertEquals(listOf("Swift"), finished.missedStudyEntries.map { it.wordDetails.word })

        vm.retryMissedWords()
        val retryState = vm.state.value
        assertEquals(VocabularyQuizPhase.Quiz, retryState.phase)
        assertTrue(retryState.outcomes.isEmpty())
        assertEquals(listOf("Swift"), retryState.studyEntries.map { it.wordDetails.word })
        assertTrue(retryState.questions.all { it.clusterWord == "Swift" })
    }

    @Test
    fun `streak milestone triggers only on 5 consecutive correct answers and resets on wrong answer`() = runTest(dispatcher) {
        val entries = listOf(
            sampleEntries.first(),
            VocabularyEntry(
                id = "w2",
                wordDetails = WordDetails(word = "Swift", partOfSpeech = "Adjective", meaningEnglish = "Fast"),
                synonyms = listOf(
                    WordDetails(word = "Rapid", partOfSpeech = "Adjective", meaningEnglish = "Quick"),
                    WordDetails(word = "Brisk", partOfSpeech = "Adjective", meaningEnglish = "Lively"),
                ),
                antonyms = listOf(
                    WordDetails(word = "Slow", partOfSpeech = "Adjective", meaningEnglish = "Sluggish"),
                ),
            ),
        )
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(
                modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
                wordCount = 2,
                questionCount = 20,
                timerSeconds = 10,
            ),
            repository = FakeVocabularyRepo(entries),
            random = Random(9),
        )
        runCurrent()
        vm.startQuiz()
        assertTrue(vm.state.value.totalCount >= 6)

        // First 4 correct answers: no milestone event yet.
        repeat(4) { i ->
            val q = vm.state.value.currentQuestion!!
            vm.selectOption(q.correctAnswer)
            assertEquals(i + 1, vm.state.value.currentStreak)
            assertEquals(0, vm.state.value.streakMilestoneEventId)
            vm.nextQuestion()
        }

        // 5th consecutive correct answer: milestone event fires and holds Next for 3 seconds!
        val fifth = vm.state.value.currentQuestion!!
        vm.selectOption(fifth.correctAnswer)
        assertEquals(5, vm.state.value.currentStreak)
        assertEquals(5, vm.state.value.streakMilestone)
        assertEquals(1, vm.state.value.streakMilestoneEventId)
        assertTrue(vm.state.value.isStreakCelebrating)

        // Tapping Next while the 3-second celebration is active is ignored so the next timer never ticks hidden.
        vm.nextQuestion()
        assertEquals(4, vm.state.value.currentIndex)

        advanceTimeBy(STREAK_CELEBRATION_DURATION_MS + 1)
        assertFalse(vm.state.value.isStreakCelebrating)
        vm.nextQuestion()
        assertEquals(5, vm.state.value.currentIndex)

        // 6th question answered wrong: streak resets to 0.
        val sixth = vm.state.value.currentQuestion!!
        vm.selectOption(sixth.options.first { !sixth.isCorrect(it) })
        assertEquals(0, vm.state.value.currentStreak)
        assertEquals(1, vm.state.value.streakMilestoneEventId)
    }

    @Test
    fun `initial study selection waits for mistakeWordsLoaded before choosing study words`() = runTest(dispatcher) {
        val swiftEntry = VocabularyEntry(
            id = "w2",
            wordDetails = WordDetails(word = "Swift", partOfSpeech = "Adjective", meaningEnglish = "Fast"),
            synonyms = listOf(WordDetails(word = "Rapid", partOfSpeech = "Adjective", meaningEnglish = "Quick")),
        )
        val entries = listOf(sampleEntries.first(), swiftEntry)
        val repo = FakeVocabularyRepo(entries, initialMistakes = emptySet(), initialMistakesLoaded = false)
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(wordCount = 1),
            repository = repo,
            random = Random(3),
        )
        runCurrent()
        assertTrue("Study words must wait until mistake words finish loading", vm.state.value.studyEntries.isEmpty())

        repo.completeMistakesLoad(setOf("w2"))
        runCurrent()
        assertEquals(listOf("Swift"), vm.state.value.studyEntries.map { it.wordDetails.word })
    }

    @Test
    fun `corrupt saved session on resume falls back to populated Study words instead of blank screen`() = runTest(dispatcher) {
        val store = FakeVocabularySessionStore().apply { text = "corrupted-session-data" }
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(wordCount = 1),
            repository = FakeVocabularyRepo(sampleEntries),
            random = Random(5),
            sessionStore = store,
            resumeSavedQuiz = true,
        )
        runCurrent()

        assertFalse(vm.state.value.isRestoring)
        assertEquals(VocabularyQuizPhase.Study, vm.state.value.phase)
        assertEquals(1, vm.state.value.studyEntries.size)
        assertNull(store.text)
    }

    @Test
    fun `homograph missedStudyEntries only includes missed part of speech and accuracyPercent rounds properly`() = runTest(dispatcher) {
        val runNoun = VocabularyEntry(
            id = "run_noun",
            wordDetails = WordDetails(word = "Run", partOfSpeech = "Noun", meaningEnglish = "An act of running"),
            synonyms = listOf(WordDetails(word = "Dash", partOfSpeech = "Noun", meaningEnglish = "Fast run")),
        )
        val runVerb = VocabularyEntry(
            id = "run_verb",
            wordDetails = WordDetails(word = "Run", partOfSpeech = "Verb", meaningEnglish = "Move fast on foot"),
            synonyms = listOf(WordDetails(word = "Sprint", partOfSpeech = "Verb", meaningEnglish = "Run fast")),
        )
        val vm = VocabularyQuizViewModel(
            config = VocabularyQuizConfig(
                modes = setOf(VocabularyQuizMode.Synonyms),
                wordCount = 2,
                questionCount = 3,
                timerSeconds = 10,
            ),
            repository = FakeVocabularyRepo(listOf(runNoun, runVerb)),
            random = Random(1),
        )
        runCurrent()
        vm.startQuiz()
        assertEquals(3, vm.state.value.totalCount)

        var missedNoun = false
        repeat(vm.state.value.totalCount) {
            val q = vm.state.value.currentQuestion!!
            if (!missedNoun && q.clusterId == "run_noun") {
                missedNoun = true
                vm.selectOption(q.options.first { !q.isCorrect(it) })
            } else {
                vm.selectOption(q.correctAnswer)
            }
            vm.nextQuestion()
        }

        val finished = vm.state.value
        assertEquals(VocabularyQuizPhase.Finished, finished.phase)
        assertEquals(listOf("run_noun"), finished.missedStudyEntries.map { it.id })
        // 2 correct out of 3 = 66.666...% -> rounds to 67%
        assertEquals(67, finished.accuracyPercent)
    }
}
