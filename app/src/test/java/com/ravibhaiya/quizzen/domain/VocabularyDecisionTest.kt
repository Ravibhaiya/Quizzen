package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyDecisionTest {

    @Test
    fun `buildRequestPlan creates gemini-decision-1 payload with choice and per-relation noul questions and parseResponse extracts exact conflicting word`() {
        val entries = listOf(
            VocabularyEntry(
                id = "word_abate_1",
                wordDetails = WordDetails(word = "Abate", meaningEnglish = "Become less intense"),
                synonyms = listOf(WordDetails(word = "Subside", meaningEnglish = "Die down")),
                createdAt = 100L,
            ),
            VocabularyEntry(
                id = "word_abating_2",
                wordDetails = WordDetails(word = "Abating", meaningEnglish = "Becoming less intense"),
                synonyms = listOf(WordDetails(word = "Waning", meaningEnglish = "Decreasing")),
                createdAt = 200L,
            ),
            VocabularyEntry(
                id = "word_candid_3",
                wordDetails = WordDetails(word = "Candid", meaningEnglish = "Truthful and straightforward"),
                synonyms = listOf(WordDetails(word = "Forthright", meaningEnglish = "Direct and outspoken")),
                antonyms = listOf(WordDetails(word = "Frank", meaningEnglish = "Truthful and straightforward")),
                createdAt = 300L,
            ),
        )

        val plan = VocabularyDecisionRules.buildRequestPlan(entries)
        assertNotNull(plan)
        assertTrue(plan!!.requestJson.contains("\"model\": \"gemini-decision-1\""))
        assertEquals(3, plan.pairQuestions.size)
        // 3 main-word definition checks + 4 relation conflict checks = 7 conflict questions
        assertEquals(7, plan.conflictQuestions.size)

        val abatingPair = plan.pairQuestions.first {
            it.primaryId == "word_abate_1" && it.duplicateId == "word_abating_2"
        }
        val abateMainConflict = plan.conflictQuestions.first {
            it.entryId == "word_abate_1" && it.isMainWordDefinition
        }
        val candidAntonymConflict = plan.conflictQuestions.first {
            it.entryId == "word_candid_3" && !it.isMainWordDefinition && it.isAntonym && it.relationDetails.word == "Frank"
        }

        val responseJson = """
            {
              "answers": {
                "${abatingPair.questionId}": {
                  "type": "choice",
                  "choice": "root_or_inflected",
                  "probabilities": {
                    "distinct": 0.02,
                    "exact_duplicate": 0.01,
                    "root_or_inflected": 0.96,
                    "spelling_or_typo": 0.01
                  },
                  "confidence": 0.97
                },
                "${abateMainConflict.questionId}": {
                  "type": "noul",
                  "noul": 0.95,
                  "confidence": 0.96
                },
                "${candidAntonymConflict.questionId}": {
                  "type": "noul",
                  "noul": 0.98,
                  "confidence": 0.98
                }
              }
            }
        """.trimIndent()

        val findings = VocabularyDecisionRules.parseResponse(responseJson, plan)
        assertEquals(3, findings.size)

        val dupFinding = findings.filterIsInstance<VocabularyDecisionFinding.DuplicateOrVariant>().single()
        assertEquals("word_abate_1", dupFinding.primaryId)
        assertEquals("word_abating_2", dupFinding.duplicateId)
        assertEquals(DecisionDuplicateKind.ROOT_OR_INFLECTED, dupFinding.kind)
        assertEquals(97, dupFinding.confidencePercent)

        val mainConflictFinding = findings.filterIsInstance<VocabularyDecisionFinding.MeaningConflict>()
            .first { it.entryId == "word_abate_1" }
        assertEquals("Main word \"Abate\" definition", mainConflictFinding.conflictingWord)
        assertNull(mainConflictFinding.conflictingTarget)

        val conflictFinding = findings.filterIsInstance<VocabularyDecisionFinding.MeaningConflict>()
            .first { it.entryId == "word_candid_3" }
        assertEquals("word_candid_3", conflictFinding.entryId)
        assertEquals(98, conflictFinding.confidencePercent)
        assertEquals("Antonym \"Frank\"", conflictFinding.conflictingWord)
        assertNotNull(conflictFinding.conflictingTarget)
        assertEquals("Frank", conflictFinding.conflictingTarget!!.word)
        assertTrue(conflictFinding.conflictingTarget!!.isAntonym)
        assertEquals(0, conflictFinding.conflictingTarget!!.index)

        // Dismissing all pair & conflict keys leaves nothing to ask
        val allDismissed = plan.pairQuestions.map {
            VocabularyDecisionFinding.pairDismissKey(it.primaryId, it.duplicateId)
        }.toSet() + plan.conflictQuestions.map {
            VocabularyDecisionFinding.conflictDismissKey(it.entryId)
        }.toSet()
        assertNull(VocabularyDecisionRules.buildRequestPlan(entries, allDismissed))
    }

    @Test
    fun `buildRequestPlan distinguishes synonym vs antonym noul instructions and pre-filters candidate pairs on 25-word sets`() {
        val unrelatedWords = listOf(
            "Abacus", "Bramble", "Citadel", "Drought", "Eclipse", "Fjord",
            "Gazebo", "Harbor", "Igloo", "Jasmine", "Keystone", "Lantern",
            "Mosaic", "Nectar", "Orchid", "Prism", "Quartz", "Rivulet",
            "Saffron", "Tundra", "Umbra", "Velvet", "Zephyr",
        )
        val entries = unrelatedWords.mapIndexed { i, w ->
            VocabularyEntry(
                id = "word_${w.lowercase()}",
                wordDetails = WordDetails(
                    word = w,
                    meaningEnglish = "Meaningof${w.lowercase()}",
                ),
                synonyms = listOf(WordDetails(word = "AliasFor$w", meaningEnglish = "Senseof${w.lowercase()}")),
                createdAt = (i + 1).toLong(),
            )
        } + listOf(
            VocabularyEntry(
                id = "word_accommodate",
                wordDetails = WordDetails(word = "Accommodate", meaningEnglish = "Provide lodging"),
                synonyms = listOf(WordDetails(word = "House", meaningEnglish = "Give shelter")),
                antonyms = listOf(WordDetails(word = "Turn away", meaningEnglish = "Refuse entry")),
                createdAt = 100L,
            ),
            VocabularyEntry(
                id = "word_accomodate",
                wordDetails = WordDetails(word = "Accomodate", meaningEnglish = "Misspelled duplicate"),
                createdAt = 200L,
            ),
        )

        val plan = VocabularyDecisionRules.buildRequestPlan(entries)!!
        assertTrue(plan.totalQuestions <= VocabularyDecisionRules.MAX_QUESTIONS_PER_CALL)
        // Candidate-pair filtering avoids sending all 300 pairs while keeping plausible typo pairs.
        assertTrue(plan.pairQuestions.size <= 5)
        assertTrue(
            plan.pairQuestions.any {
                it.primaryId == "word_accommodate" && it.duplicateId == "word_accomodate"
            },
        )
        assertTrue(plan.requestJson.contains("listed antonym \\\"Turn away\\\""))
        assertTrue(plan.requestJson.contains("listed synonym \\\"House\\\""))
    }
}
