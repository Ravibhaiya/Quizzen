package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class VocabularyQuizEngineTest {

    private val entryAbundant = VocabularyEntry(
        id = "c1",
        wordDetails = WordDetails(word = "Abundant", meaningEnglish = "Plenty"),
        synonyms = listOf(
            WordDetails(word = "Plentiful", meaningEnglish = "Ample"),
            WordDetails(word = "Copious", meaningEnglish = "Profuse"),
        ),
        antonyms = listOf(
            WordDetails(word = "Scarce", meaningEnglish = "Rare"),
            WordDetails(word = "Meager", meaningEnglish = "Scanty"),
        ),
    )

    private val entryCandid = VocabularyEntry(
        id = "c2",
        wordDetails = WordDetails(word = "Candid", meaningEnglish = "Honest"),
        synonyms = listOf(
            WordDetails(word = "Frank", meaningEnglish = "Direct"),
            WordDetails(word = "Forthright", meaningEnglish = "Outspoken"),
        ),
        antonyms = listOf(
            WordDetails(word = "Deceitful", meaningEnglish = "Dishonest"),
            WordDetails(word = "Evasive", meaningEnglish = "Indirect"),
        ),
    )

    @Test
    fun `word count is clamped between 5 and 25`() {
        assertEquals(5, VocabularyQuizRules.coerceWordCount(1))
        assertEquals(5, VocabularyQuizRules.coerceWordCount(5))
        assertEquals(15, VocabularyQuizRules.coerceWordCount(15))
        assertEquals(25, VocabularyQuizRules.coerceWordCount(25))
        assertEquals(25, VocabularyQuizRules.coerceWordCount(99))
    }

    @Test
    fun `both modes cover every main word, synonym, and antonym in finite quiz`() {
        val study = listOf(entryAbundant, entryCandid)
        val modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms)
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = modes,
            random = Random(42),
        )

        // Each entry has 1 main word + 2 synonyms + 2 antonyms = 5 words -> 10 questions total.
        assertEquals(10, quiz.size)

        val expectedPrompts = setOf(
            "Abundant", "Plentiful", "Copious", "Scarce", "Meager",
            "Candid", "Frank", "Forthright", "Deceitful", "Evasive",
        )
        assertEquals(expectedPrompts, quiz.map { it.promptWord }.toSet())
    }

    @Test
    fun `two synonyms can be involved as question and correct option without the main word`() {
        val study = listOf(entryAbundant)
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms),
            random = Random(7),
        )

        // S+ has 3 words ("Abundant", "Plentiful", "Copious"). In a 3-cycle derangement,
        // one of the 3 questions is guaranteed to pair "Plentiful" and "Copious" directly!
        assertEquals(3, quiz.size)
        val synToSyn = quiz.filter { item ->
            item.promptWord != "Abundant" && item.correctAnswer != "Abundant"
        }
        assertEquals(1, synToSyn.size)
        val pair = setOf(synToSyn.single().promptWord, synToSyn.single().correctAnswer)
        assertEquals(setOf("Plentiful", "Copious"), pair)
    }

    @Test
    fun `antonyms can pair directly with synonyms as well as the main word`() {
        val study = listOf(entryAbundant)
        // Run across seeds to verify that antonyms pair with synonyms in S+
        val antonymTargets = (1..20).flatMap { seed ->
            VocabularyQuizEngine.generateQuiz(
                studyEntries = study,
                allEntries = study,
                modes = setOf(VocabularyQuizMode.Antonyms),
                random = Random(seed),
            ).filter { it.promptWord in setOf("Scarce", "Meager") }
                .map { it.correctAnswer }
        }.toSet()

        assertTrue("Expected synonyms in S+ to appear as correct answers for antonyms", "Plentiful" in antonymTargets || "Copious" in antonymTargets)
    }

    @Test
    fun `questions from the same cluster are interleaved and not placed back-to-back`() {
        val study = VocabularyQuizRules.FALLBACK_ENTRIES
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            random = Random(99),
        )

        // Since all 5 fallback entries have equal size (5 questions each), no two adjacent questions
        // should ever share the same clusterId.
        val adjacentSameCluster = quiz.zipWithNext().count { (a, b) -> a.clusterId == b.clusterId }
        assertEquals(0, adjacentSameCluster)
    }

    @Test
    fun `each question has 4 distinct options and excludes other valid answers from distractors`() {
        val study = listOf(entryAbundant, entryCandid)
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            random = Random(123),
        )

        val abundantSame = setOf("abundant", "plentiful", "copious")
        val abundantOpp = setOf("scarce", "meager")

        for (q in quiz) {
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.map { it.lowercase() }.toSet().size)
            assertTrue(q.options.any { q.isCorrect(it) })
            assertFalse(q.options.any { it.equals(q.promptWord, ignoreCase = true) })
            assertTrue(q.promptDetails.meaningEnglish.isNotBlank())
            assertEquals(4, q.optionDetails.size)
            q.options.forEach { option ->
                assertTrue(q.detailsForOption(option).meaningEnglish.isNotBlank())
            }

            // Verify no distractor is another valid answer in the abundant cluster
            if (q.clusterId == "c1") {
                val distractors = q.options.filterNot { q.isCorrect(it) }.map { it.lowercase() }
                if (q.mode == VocabularyQuizMode.Synonyms) {
                    assertTrue(distractors.none { it in abundantSame })
                } else {
                    val promptInSame = q.promptWord.lowercase() in abundantSame
                    val validTargets = if (promptInSame) abundantOpp else abundantSame
                    assertTrue(distractors.none { it in validTargets })
                }
            }
        }
    }

    @Test
    fun `starter entries provide 25 valid vocabulary clusters for Firebase seeding and full 5 to 25 word quizzes`() {
        val entries = VocabularyQuizRules.FALLBACK_ENTRIES
        assertEquals(25, entries.size)
        for (entry in entries) {
            assertTrue(VocabularyRules.canSave(entry.wordDetails, entry.synonyms, entry.antonyms))
            assertEquals(2, entry.synonyms.size)
            assertEquals(2, entry.antonyms.size)
        }
        val selected25 = VocabularyQuizRules.selectStudyWords(
            entries = entries,
            modes = VocabularyQuizMode.DEFAULT,
            wordCount = 25,
            random = Random(1),
        )
        assertEquals(25, selected25.size)
    }

    @Test
    fun `selectStudyWords prioritizes saved mistake words first and fills remaining slots with random words`() {
        val entries = VocabularyQuizRules.FALLBACK_ENTRIES
        val savedMistakes = setOf("zealous", "tranquil", "ephemeral")
        val selected = VocabularyQuizRules.selectStudyWords(
            entries = entries,
            modes = VocabularyQuizMode.DEFAULT,
            wordCount = 5,
            random = Random(42),
            mistakeWords = savedMistakes,
        )

        assertEquals(5, selected.size)
        val firstThreeWords = selected.take(3).map { it.wordDetails.word.lowercase() }.toSet()
        assertEquals(savedMistakes, firstThreeWords)
        val remainingWords = selected.drop(3).map { it.wordDetails.word.lowercase() }.toSet()
        assertEquals(2, remainingWords.size)
        assertTrue(remainingWords.none { it in savedMistakes })
    }

    @Test
    fun `VocabularyMistakeTracker saves on wrong or timeout and deletes only when all questions for that word are correct`() {
        val study = listOf(entryAbundant, entryCandid)
        val quiz = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = study,
            modes = setOf(VocabularyQuizMode.Synonyms, VocabularyQuizMode.Antonyms),
            random = Random(42),
        )
        val tracker = VocabularyMistakeTracker(questions = quiz, studyEntries = study)

        // Answer all Abundant ("c1") questions correctly, and get 1 Candid ("c2") question wrong ( followed by correct for rest of Candid).
        val abundantIndices = quiz.indices.filter { quiz[it].clusterId == "c1" }
        val candidIndices = quiz.indices.filter { quiz[it].clusterId == "c2" }
        assertEquals(5, abundantIndices.size)
        assertEquals(5, candidIndices.size)

        // Before the last Abundant question, resolving with Correct returns None
        for (idx in abundantIndices.dropLast(1)) {
            assertEquals(VocabularyMistakeAction.None, tracker.onQuestionResolved(idx, FeedbackType.Correct))
        }
        // Resolving the final Abundant question with Correct returns DeleteMastered
        val finalAbundantAction = tracker.onQuestionResolved(abundantIndices.last(), FeedbackType.Correct)
        assertTrue(finalAbundantAction is VocabularyMistakeAction.DeleteMastered)
        assertTrue("abundant" in finalAbundantAction.wordKeys)
        assertTrue("c1" in finalAbundantAction.wordKeys)

        // For Candid, first question is Incorrect -> immediately returns SaveMistake
        val firstCandidAction = tracker.onQuestionResolved(candidIndices.first(), FeedbackType.Incorrect)
        assertTrue(firstCandidAction is VocabularyMistakeAction.SaveMistake)
        assertTrue("candid" in firstCandidAction.wordKeys)
        assertTrue("c2" in firstCandidAction.wordKeys)

        // Even if all remaining Candid questions are answered Correct, it is NOT deleted in this session
        for (idx in candidIndices.drop(1)) {
            assertEquals(VocabularyMistakeAction.None, tracker.onQuestionResolved(idx, FeedbackType.Correct))
        }
    }

    @Test
    fun `test duplicate entries provide 25 matching main words with 12 identical and 13 with differences`() {
        val base = VocabularyQuizRules.FALLBACK_ENTRIES
        val dups = VocabularyQuizRules.TEST_DUPLICATE_ENTRIES
        assertEquals(25, base.size)
        assertEquals(25, dups.size)
        assertEquals(
            base.map { VocabularyRules.wordKey(it.wordDetails.word) },
            dups.map { VocabularyRules.wordKey(it.wordDetails.word) },
        )

        var identicalCount = 0
        var differentCount = 0
        for (i in base.indices) {
            val orig = base[i]
            val dup = dups[i]
            assertTrue(dup.id != orig.id)
            val mergeRes = VocabularyRules.mergeRelations(orig, dup)
            if (mergeRes.changed) {
                differentCount++
                assertTrue(mergeRes.addedSynonyms > 0 || mergeRes.addedAntonyms > 0)
            } else {
                identicalCount++
                assertEquals(orig.copy(id = dup.id), dup)
            }
        }
        assertEquals(12, identicalCount)
        assertEquals(13, differentCount)
    }
}

