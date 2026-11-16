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
    fun `word count minimum is 1 and maximum is 25`() {
        assertEquals(1, VocabularyQuizRules.MIN_WORDS)
        assertEquals(25, VocabularyQuizRules.MAX_WORDS)
        assertEquals(1, VocabularyQuizRules.coerceWordCount(0))
        assertEquals(1, VocabularyQuizRules.coerceWordCount(1))
        assertEquals(25, VocabularyQuizRules.coerceWordCount(30))
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
    fun `selectStudyWords returns empty list on empty database and prioritizes mistake words when present`() {
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
        val tracker = VocabularyMistakeTracker(questions = quiz, studyEntries = study)

        val abundantIndices = quiz.indices.filter { quiz[it].clusterId == "c1" }
        val candidIndices = quiz.indices.filter { quiz[it].clusterId == "c2" }

        for (idx in abundantIndices.dropLast(1)) {
            assertEquals(VocabularyMistakeAction.None, tracker.onQuestionResolved(idx, FeedbackType.Correct))
        }
        val mastered = tracker.onQuestionResolved(abundantIndices.last(), FeedbackType.Correct)
        assertTrue(mastered is VocabularyMistakeAction.DeleteMastered)
        assertTrue("abundant" in mastered.wordKeys)

        val mistake = tracker.onQuestionResolved(candidIndices.first(), FeedbackType.Incorrect)
        assertTrue(mistake is VocabularyMistakeAction.SaveMistake)
        assertTrue("candid" in mistake.wordKeys)
        assertFalse(tracker.onQuestionResolved(candidIndices.last(), FeedbackType.Correct) is VocabularyMistakeAction.DeleteMastered)
    }
}
