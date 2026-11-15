package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyDecisionTest {
    /** The first request the app would send (the app itself sends every batch, in order). */
    private fun firstPlan(
        entries: List<VocabularyEntry>,
        dismissedKeys: Set<String> = emptySet(),
    ): DecisionRequestPlan? = VocabularyDecisionRules.buildRequestBatches(
        entries = entries,
        dismissedKeys = dismissedKeys,
        maxQuestionsPerBatch = VocabularyDecisionRules.MAX_QUESTIONS_PER_CALL,
    ).firstOrNull()


    @Test
    fun `buildRequestBatches creates gemini-decision-1 payload with choice and per-relation noul questions and parseResponse extracts exact conflicting word`() {
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

        val plan = firstPlan(entries)
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
        assertNull(firstPlan(entries, allDismissed))
    }

    @Test
    fun `buildRequestBatches distinguishes synonym vs antonym noul instructions and pre-filters candidate pairs on 25-word sets`() {
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

        val plan = firstPlan(entries)!!
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

    @Test
    fun `Option B skips meaning, synonym and antonym checks on verified words while still comparing unverified words against all existing words`() {
        val verifiedOld = listOf(
            VocabularyEntry(
                id = "word_accommodate",
                wordDetails = WordDetails(word = "Accommodate", meaningEnglish = "Provide lodging"),
                synonyms = listOf(WordDetails(word = "Lodge", meaningEnglish = "Give shelter")),
                antonyms = listOf(WordDetails(word = "Evict", meaningEnglish = "Expel")),
                createdAt = 100L,
                updatedAt = 100L,
            ),
            VocabularyEntry(
                id = "word_color",
                wordDetails = WordDetails(word = "Color", meaningEnglish = "Visual hue"),
                synonyms = listOf(WordDetails(word = "Tint", meaningEnglish = "Shade")),
                createdAt = 110L,
                updatedAt = 110L,
            ),
            VocabularyEntry(
                id = "word_colour_old",
                wordDetails = WordDetails(word = "Colour", meaningEnglish = "British spelling of color"),
                createdAt = 120L,
                updatedAt = 120L,
            ),
        )
        val newUnverified = VocabularyEntry(
            id = "word_accomodate_new",
            wordDetails = WordDetails(word = "Accomodate", meaningEnglish = "Provide room for"),
            synonyms = listOf(WordDetails(word = "House", meaningEnglish = "Provide shelter")),
            createdAt = 500L,
            updatedAt = 500L,
        )

        val verifiedIds = verifiedOld.map { it.id }.toSet()
        val batches = VocabularyDecisionRules.buildRequestBatches(
            entries = verifiedOld + newUnverified,
            verifiedEntryIds = verifiedIds,
        )
        assertEquals(1, batches.size)
        val batch = batches.single()

        // 1. Conflict checks (main meaning + synonyms + antonyms) are generated ONLY for the new unverified word
        assertTrue(batch.conflictQuestions.all { it.entryId == "word_accomodate_new" })
        assertEquals(2, batch.conflictQuestions.size) // 1 main + 1 synonym

        // 2. Duplicate pair check compares the new unverified word against the old verified word ("Accommodate" vs "Accomodate"),
        //    and does NOT re-compare the two already-verified words ("Color" vs "Colour")
        assertTrue(
            batch.pairQuestions.any {
                it.primaryId == "word_accommodate" && it.duplicateId == "word_accomodate_new"
            },
        )
        assertTrue(
            batch.pairQuestions.none {
                it.primaryId in verifiedIds && it.duplicateId in verifiedIds
            },
        )
        assertEquals(setOf("word_accomodate_new"), batch.coveredEntryIds)
    }

    @Test
    fun `buildRequestBatches chunks large databases into 100-question batches with dedicated pair slots and newest words first`() {
        // 80 words each with 2 synonyms and 1 antonym = 320 conflict questions (> 250 old cap) + typo pairs
        val entries = (1..80).map { i ->
            VocabularyEntry(
                id = "word_$i",
                wordDetails = WordDetails(word = "TermNumber$i", meaningEnglish = "Distinctmeaning$i"),
                synonyms = listOf(
                    WordDetails(word = "SynA$i", meaningEnglish = "SynmeaningA$i"),
                    WordDetails(word = "SynB$i", meaningEnglish = "SynmeaningB$i"),
                ),
                antonyms = listOf(
                    WordDetails(word = "AntA$i", meaningEnglish = "AntmeaningA$i"),
                ),
                createdAt = i.toLong(),
                updatedAt = i.toLong(),
            )
        } + listOf(
            VocabularyEntry(
                id = "word_newest_typo",
                wordDetails = WordDetails(word = "TermNumber80Typo", meaningEnglish = "Distinctmeaning80"),
                createdAt = 999L,
                updatedAt = 999L,
            ),
        )

        val batches = VocabularyDecisionRules.buildRequestBatches(entries)
        assertTrue(batches.size >= 4)
        assertTrue(batches.all { it.totalQuestions <= VocabularyDecisionRules.BATCH_MAX_QUESTIONS })

        // First batch must include pair questions (never starved by the 321 conflict questions)
        // and must prioritize the newest word ("word_newest_typo") first!
        val firstBatch = batches.first()
        assertTrue(firstBatch.pairQuestions.isNotEmpty())
        assertEquals("word_newest_typo", firstBatch.conflictQuestions.first().entryId)

        // Across all batches, every entry is covered
        val allCovered = batches.flatMap { it.coveredEntryIds }.toSet()
        assertEquals(entries.map { it.id }.toSet(), allCovered)
    }

    @Test
    fun `encodeVerificationSnapshot and decodeVerificationSnapshot round-trip digests and findings`() {
        val original = VocabularyDecisionRules.DecisionVerificationSnapshot(
            verifiedDigests = mapOf("w1" to 12345, "w2" to -67890),
            findings = listOf(
                VocabularyDecisionFinding.DuplicateOrVariant(
                    primaryId = "w1",
                    duplicateId = "w2",
                    kind = DecisionDuplicateKind.SPELLING_OR_TYPO,
                    probability = 0.96,
                    confidence = 0.98,
                ),
                VocabularyDecisionFinding.MeaningConflict(
                    entryId = "w1",
                    probability = 0.91,
                    confidence = 0.94,
                    conflictingTarget = VocabularyRules.ConflictingRelationTarget(
                        isAntonym = true,
                        index = 1,
                        details = WordDetails(word = "Frank", meaningEnglish = "Honest"),
                        conflictingWordLabel = "Antonym \"Frank\"",
                        reason = "Listed under antonyms by mistake.",
                    ),
                ),
            ),
        )
        val encoded = VocabularyDecisionRules.encodeVerificationSnapshot(original)
        val decoded = VocabularyDecisionRules.decodeVerificationSnapshot(encoded)
        assertEquals(original, decoded)
    }
}
