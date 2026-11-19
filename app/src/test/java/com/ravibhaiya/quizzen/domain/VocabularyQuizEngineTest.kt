package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VocabularyQuizEngineTest {

    private val entryAbundant = VocabularyEntry(
        id = "c1",
        wordDetails = WordDetails(word = "Abundant", partOfSpeech = "Adjective", meaningEnglish = "Plenty"),
        synonyms = listOf(
            WordDetails(word = "Plentiful", partOfSpeech = "Adjective", meaningEnglish = "Ample"),
            WordDetails(word = "Copious", partOfSpeech = "Adjective", meaningEnglish = "Profuse"),
        ),
        antonyms = listOf(
            WordDetails(word = "Scarce", partOfSpeech = "Adjective", meaningEnglish = "Rare"),
            WordDetails(word = "Meager", partOfSpeech = "Adjective", meaningEnglish = "Scanty"),
        ),
    )

    private val entryCandid = VocabularyEntry(
        id = "c2",
        wordDetails = WordDetails(word = "Candid", partOfSpeech = "Adjective", meaningEnglish = "Honest"),
        synonyms = listOf(
            WordDetails(word = "Frank", partOfSpeech = "Adjective", meaningEnglish = "Direct"),
            WordDetails(word = "Forthright", partOfSpeech = "Adjective", meaningEnglish = "Outspoken"),
        ),
        antonyms = listOf(
            WordDetails(word = "Deceitful", partOfSpeech = "Adjective", meaningEnglish = "Dishonest"),
            WordDetails(word = "Evasive", partOfSpeech = "Adjective", meaningEnglish = "Indirect"),
        ),
    )

    @Test
    fun `word count minimum is 1 and maximum is 25, and question count minimum is 1 and maximum is 100`() {
        assertEquals(1, VocabularyQuizRules.MIN_WORDS)
        assertEquals(25, VocabularyQuizRules.MAX_WORDS)
        assertEquals(1, VocabularyQuizRules.coerceWordCount(0))
        assertEquals(1, VocabularyQuizRules.coerceWordCount(1))
        assertEquals(25, VocabularyQuizRules.coerceWordCount(30))

        assertEquals(1, VocabularyQuizRules.MIN_QUESTIONS)
        assertEquals(100, VocabularyQuizRules.MAX_QUESTIONS)
        assertEquals(1, VocabularyQuizRules.coerceQuestionCount(0))
        assertEquals(1, VocabularyQuizRules.coerceQuestionCount(1))
        assertEquals(100, VocabularyQuizRules.coerceQuestionCount(100))
        assertEquals(100, VocabularyQuizRules.coerceQuestionCount(150))
    }

    @Test
    fun `maxQuestions limits total questions fairly across studied clusters while keeping clusters contiguous`() {
        val study = VocabularyQuizRules.FALLBACK_ENTRIES.take(3) // 15 total unique questions across 3 clusters
        val cappedToSeven = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            maxQuestions = 7,
            random = Random(42),
        )
        assertEquals(7, cappedToSeven.size)
        // Every cluster stays contiguous and every studied cluster receives at least 2 questions (3 + 2 + 2)
        val runs = mutableListOf<String>()
        for (q in cappedToSeven) {
            if (runs.lastOrNull() != q.clusterId) runs += q.clusterId
        }
        assertEquals(3, runs.size)
        assertEquals(3, runs.toSet().size)

        val cappedToOne = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            maxQuestions = 1,
            random = Random(42),
        )
        assertEquals(1, cappedToOne.size)

        val cappedToHundred = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            maxQuestions = 100,
            random = Random(42),
        )
        assertEquals(15, cappedToHundred.size)
    }

    @Test
    fun `questions for each main word stay contiguous and preserve individual partOfSpeech for prompt and options`() {
        val study = VocabularyQuizRules.FALLBACK_ENTRIES.take(3)
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            random = Random(42),
        )

        assertEquals(15, quiz.size)
        val clusterRuns = mutableListOf<String>()
        for (item in quiz) {
            if (clusterRuns.lastOrNull() != item.clusterId) {
                clusterRuns += item.clusterId
            }
            assertTrue(item.clusterWord.isNotBlank())
            assertTrue(item.clusterPartOfSpeech.isNotBlank())
            assertTrue(item.promptDetails.partOfSpeech.isNotBlank())
            assertEquals(4, item.options.size)
            item.options.forEach { option ->
                assertTrue(item.detailsForOption(option).partOfSpeech.isNotBlank())
                assertTrue(item.detailsForOption(option).meaningEnglish.isNotBlank())
            }
        }
        assertEquals(3, clusterRuns.size)
        assertEquals(3, clusterRuns.toSet().size)
    }

    @Test
    fun `homograph entries with different partOfSpeech keep their own cluster partOfSpeech and meanings in quiz`() {
        val runNoun = VocabularyEntry(
            id = "run_noun",
            wordDetails = WordDetails(word = "Run", partOfSpeech = "Noun", meaningEnglish = "An act of running"),
            synonyms = listOf(
                WordDetails(word = "Dash", partOfSpeech = "Noun", meaningEnglish = "A short fast run"),
            ),
            antonyms = listOf(
                WordDetails(word = "Standstill", partOfSpeech = "Noun", meaningEnglish = "A complete stop"),
            ),
        )
        val runVerb = VocabularyEntry(
            id = "run_verb",
            wordDetails = WordDetails(word = "Run", partOfSpeech = "Verb", meaningEnglish = "Move fast on foot"),
            synonyms = listOf(
                WordDetails(word = "Sprint", partOfSpeech = "Verb", meaningEnglish = "Run at full speed"),
            ),
            antonyms = listOf(
                WordDetails(word = "Crawl", partOfSpeech = "Verb", meaningEnglish = "Move slowly on hands and knees"),
            ),
        )

        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = listOf(runNoun, runVerb),
            allEntries = listOf(runNoun, runVerb),
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            random = Random(7),
        )

        val nounClusterQuestions = quiz.filter { it.clusterId == "run_noun" }
        val verbClusterQuestions = quiz.filter { it.clusterId == "run_verb" }
        assertTrue(nounClusterQuestions.all { it.clusterPartOfSpeech == "Noun" })
        assertTrue(verbClusterQuestions.all { it.clusterPartOfSpeech == "Verb" })

        val nounPrompt = nounClusterQuestions.first { it.promptWord == "Run" }
        val verbPrompt = verbClusterQuestions.first { it.promptWord == "Run" }
        assertEquals("Noun", nounPrompt.promptDetails.partOfSpeech)
        assertEquals("An act of running", nounPrompt.promptDetails.meaningEnglish)
        assertEquals("Verb", verbPrompt.promptDetails.partOfSpeech)
        assertEquals("Move fast on foot", verbPrompt.promptDetails.meaningEnglish)
    }

    @Test
    fun `synonym and antonym questions within a cluster are mixed randomly instead of all synonyms before all antonyms`() {
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = listOf(entryAbundant),
            allEntries = listOf(entryAbundant, entryCandid),
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            random = Random(11),
        )

        assertEquals(5, quiz.size)
        val modes = quiz.map { it.mode }
        assertTrue(modes.contains(VocabularyQuizMode.Synonyms))
        assertTrue(modes.contains(VocabularyQuizMode.Antonyms))
        val transitions = modes.zipWithNext().count { (a, b) -> a != b }
        assertTrue("Expected synonyms and antonyms to be mixed rather than grouped by mode", transitions >= 2)
    }

    @Test
    fun `selectStudyWords returns empty list on empty database and switches words when excludeEntries is passed`() {
        assertTrue(
            VocabularyQuizRules.selectStudyWords(
                entries = emptyList(),
                modes = VocabularyQuizMode.DEFAULT,
                wordCount = 1,
                random = Random(1),
            ).isEmpty(),
        )

        val selected = VocabularyQuizRules.selectStudyWords(
            entries = listOf(entryAbundant, entryCandid),
            modes = VocabularyQuizMode.DEFAULT,
            wordCount = 1,
            random = Random(99),
            mistakeWords = setOf("candid"),
        )
        assertEquals(1, selected.size)
        assertEquals("Candid", selected.single().wordDetails.word)

        val reshuffled = VocabularyQuizRules.selectStudyWords(
            entries = listOf(entryAbundant, entryCandid),
            modes = VocabularyQuizMode.DEFAULT,
            wordCount = 1,
            random = Random(99),
            mistakeWords = setOf("candid"),
            excludeEntries = selected,
        )
        assertEquals(1, reshuffled.size)
        assertEquals("Abundant", reshuffled.single().wordDetails.word)
    }

    @Test
    fun `VocabularyMistakeTracker saves on wrong or timeout and deletes when all questions for a word are correct`() {
        val study = listOf(entryAbundant, entryCandid)
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            random = Random(42),
        )
        val tracker = VocabularyMistakeTracker(
            questions = quiz,
            studyEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
        )

        val abundantIndices = quiz.indices.filter { quiz[it].clusterId == "c1" }
        val candidIndices = quiz.indices.filter { quiz[it].clusterId == "c2" }

        for (idx in abundantIndices.dropLast(1)) {
            assertEquals(VocabularyMistakeAction.None, tracker.onQuestionResolved(idx, FeedbackType.Correct))
        }
        val mastered = tracker.onQuestionResolved(abundantIndices.last(), FeedbackType.Correct)
        assertTrue(mastered is VocabularyMistakeAction.DeleteMastered)
        assertTrue("c1" in mastered.wordKeys)

        val mistake = tracker.onQuestionResolved(candidIndices.first(), FeedbackType.Incorrect)
        assertTrue(mistake is VocabularyMistakeAction.SaveMistake)
        assertTrue("c2" in mistake.wordKeys)
        assertFalse(tracker.onQuestionResolved(candidIndices.last(), FeedbackType.Correct) is VocabularyMistakeAction.DeleteMastered)
    }

    @Test
    fun `VocabularyMistakeTracker does not clear weak word when questionCount tests too few questions from its cluster`() {
        val study = listOf(entryAbundant) // 5 full questions (3 S+ + 2 S-)
        val modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms)
        val shortQuiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = modes,
            maxQuestions = 2,
            random = Random(42),
        )
        assertEquals(2, shortQuiz.size)
        val shortTracker = VocabularyMistakeTracker(questions = shortQuiz, studyEntries = study, modes = modes)
        assertEquals(VocabularyMistakeAction.None, shortTracker.onQuestionResolved(0, FeedbackType.Correct))
        // Answering only 2 out of 5 questions must not clear mastery.
        assertEquals(VocabularyMistakeAction.None, shortTracker.onQuestionResolved(1, FeedbackType.Correct))

        // Answering 3 out of 5 (>= half and >= MIN_CAPPED_MASTERY_QUESTIONS) all right clears mastery.
        val mediumQuiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = modes,
            maxQuestions = 3,
            random = Random(42),
        )
        val mediumTracker = VocabularyMistakeTracker(questions = mediumQuiz, studyEntries = study, modes = modes)
        assertEquals(VocabularyMistakeAction.None, mediumTracker.onQuestionResolved(0, FeedbackType.Correct))
        assertEquals(VocabularyMistakeAction.None, mediumTracker.onQuestionResolved(1, FeedbackType.Correct))
        assertTrue(mediumTracker.onQuestionResolved(2, FeedbackType.Correct) is VocabularyMistakeAction.DeleteMastered)
    }

    @Test
    fun `homograph entries with different partOfSpeech do not collide in mistake keys or study prioritization`() {
        val presentNoun = VocabularyEntry(
            id = "present_noun",
            wordDetails = WordDetails(word = "Present", partOfSpeech = "Noun", meaningEnglish = "A gift"),
            synonyms = listOf(WordDetails(word = "Gift", partOfSpeech = "Noun", meaningEnglish = "Something given")),
        )
        val presentVerb = VocabularyEntry(
            id = "present_verb",
            wordDetails = WordDetails(word = "Present", partOfSpeech = "Verb", meaningEnglish = "To show or offer"),
            synonyms = listOf(WordDetails(word = "Offer", partOfSpeech = "Verb", meaningEnglish = "To put forward")),
        )
        val entries = listOf(presentNoun, presentVerb)
        assertEquals(setOf("present_noun"), VocabularyQuizRules.entryMistakeKeys(presentNoun))
        assertEquals(setOf("present_verb"), VocabularyQuizRules.entryMistakeKeys(presentVerb))

        val bareCounts = VocabularyQuizRules.bareWordCounts(entries)
        val nounOnlyMistakes = setOf("present_noun")
        assertTrue(VocabularyQuizRules.matchesMistakeWords(presentNoun, nounOnlyMistakes, bareCounts))
        assertFalse(VocabularyQuizRules.matchesMistakeWords(presentVerb, nounOnlyMistakes, bareCounts))

        for (seed in 1..10) {
            val selected = VocabularyQuizRules.selectStudyWords(
                entries = entries,
                modes = setOf(VocabularyQuizMode.Synonyms),
                wordCount = 1,
                random = Random(seed),
                mistakeWords = nounOnlyMistakes,
            )
            assertEquals(listOf("present_noun"), selected.map { it.id })
        }
    }
}
