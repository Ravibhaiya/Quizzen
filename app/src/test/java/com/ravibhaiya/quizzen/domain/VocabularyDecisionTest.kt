package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyDecisionTest {

    private fun firstPlan(
        entries: List<VocabularyEntry>,
        dismissedKeys: Set<String> = emptySet(),
    ): DecisionRequestPlan? = VocabularyDecisionRules.buildRequestBatches(
        entries = entries,
        dismissedKeys = dismissedKeys,
        maxQuestionsPerBatch = VocabularyDecisionRules.MAX_QUESTIONS_PER_CALL,
    ).firstOrNull()

    @Test
    fun `buildRequestBatches checks usage-based partOfSpeech and meanings for main word, synonyms, and antonyms`() {
        val entries = listOf(
            VocabularyEntry(
                id = "word_repudiate",
                wordDetails = WordDetails(
                    word = "Repudiate",
                    partOfSpeech = "Noun",
                    meaningEnglish = "Refuse to accept or be associated with",
                    exampleSentence = "He chose to repudiate the false accusations.",
                ),
                synonyms = listOf(
                    WordDetails(
                        word = "Reject",
                        partOfSpeech = "Verb",
                        meaningEnglish = "Dismiss as inadequate",
                        exampleSentence = "They reject the proposal.",
                    ),
                ),
                antonyms = listOf(
                    WordDetails(
                        word = "Embrace",
                        partOfSpeech = "Adjective",
                        meaningEnglish = "Accept or support willingly",
                        exampleSentence = "We embrace new ideas.",
                    ),
                ),
            ),
        )

        val plan = firstPlan(entries)
        assertNotNull(plan)
        // 1 main meaning + 1 main POS + 1 syn meaning + 1 syn POS + 1 ant meaning + 1 ant POS = 6 questions
        assertEquals(6, plan!!.conflictQuestions.size)

        val mainPosQ = plan.conflictQuestions.first { it.isPartOfSpeechCheck && it.relationDetails.word.isEmpty() }
        assertTrue(
            plan.requestJson.contains(
                "For word \\\"Repudiate\\\" as used with meaning \\\"Refuse to accept or be associated with\\\" and example \\\"He chose to repudiate the false accusations.\\\", is its listed part of speech \\\"Noun\\\" wrong?",
            ),
        )

        val antPosQ = plan.conflictQuestions.first { it.isPartOfSpeechCheck && it.isAntonym && it.relationDetails.word == "Embrace" }
        assertTrue(
            plan.requestJson.contains(
                "For antonym \\\"Embrace\\\" (under \\\"Repudiate\\\") as used with meaning \\\"Accept or support willingly\\\" and example \\\"We embrace new ideas.\\\", is its listed part of speech \\\"Adjective\\\" wrong?",
            ),
        )

        // When main-word POS is flagged, finding targets main word
        val mainPosResponse = """
            {
              "answers": {
                "${mainPosQ.questionId}": {
                  "type": "noul",
                  "noul": 0.94,
                  "confidence": 0.96
                }
              }
            }
        """.trimIndent()
        val mainFinding = VocabularyDecisionRules.parseResponse(mainPosResponse, plan)
            .filterIsInstance<VocabularyDecisionFinding.MeaningConflict>()
            .single()
        assertNull(mainFinding.conflictingTarget)
        assertEquals("Main word \"Repudiate\" part of speech", mainFinding.conflictingWord)
        assertTrue(mainFinding.conflictReason!!.contains("Wrong part of speech \"Noun\" for main word \"Repudiate\""))

        // When relation POS has higher probability than main POS, relation POS wins and preserves partOfSpeech
        val bothPosResponse = """
            {
              "answers": {
                "${mainPosQ.questionId}": {
                  "type": "noul",
                  "noul": 0.85,
                  "confidence": 0.88
                },
                "${antPosQ.questionId}": {
                  "type": "noul",
                  "noul": 0.98,
                  "confidence": 0.99
                }
              }
            }
        """.trimIndent()
        val relFinding = VocabularyDecisionRules.parseResponse(bothPosResponse, plan)
            .filterIsInstance<VocabularyDecisionFinding.MeaningConflict>()
            .single()
        assertNotNull(relFinding.conflictingTarget)
        assertEquals("Antonym \"Embrace\" part of speech", relFinding.conflictingWord)
        assertEquals("Adjective", relFinding.conflictingTarget!!.details.partOfSpeech)
    }

    @Test
    fun `selectCandidatePairs skips entries with different partOfSpeech while pairing entries with same partOfSpeech`() {
        val entries = listOf(
            VocabularyEntry(
                id = "word_present_noun",
                wordDetails = WordDetails(word = "Present", partOfSpeech = "Noun", meaningEnglish = "A gift"),
                createdAt = 100L,
            ),
            VocabularyEntry(
                id = "word_present_verb",
                wordDetails = WordDetails(word = "Present", partOfSpeech = "Verb", meaningEnglish = "Give formally"),
                createdAt = 200L,
            ),
            VocabularyEntry(
                id = "word_present_noun_2",
                wordDetails = WordDetails(word = "Present", partOfSpeech = "Noun", meaningEnglish = "Something offered"),
                createdAt = 300L,
            ),
        )

        val plan = firstPlan(entries)!!
        assertEquals(1, plan.pairQuestions.size)
        val pair = plan.pairQuestions.single()
        assertEquals("word_present_noun", pair.primaryId)
        assertEquals("word_present_noun_2", pair.duplicateId)
    }

    @Test
    fun `Option B skips already verified entries and entryContentDigest changes when partOfSpeech is edited`() {
        val entry = VocabularyEntry(
            id = "w1",
            wordDetails = WordDetails(word = "Candid", partOfSpeech = "Noun", meaningEnglish = "Truthful"),
            createdAt = 100L,
            updatedAt = 100L,
        )
        val editedPos = entry.copy(
            wordDetails = entry.wordDetails.copy(partOfSpeech = "Adjective"),
        )
        assertTrue(VocabularyDecisionRules.entryContentDigest(entry) != VocabularyDecisionRules.entryContentDigest(editedPos))

        // When w1 is already verified, buildRequestBatches returns empty
        assertTrue(
            VocabularyDecisionRules.buildRequestBatches(
                entries = listOf(editedPos),
                verifiedEntryIds = setOf("w1"),
            ).isEmpty(),
        )
    }

    @Test
    fun `encodeVerificationSnapshot and decodeVerificationSnapshot round-trip digests and targetPartOfSpeech`() {
        val snapshot = VocabularyDecisionRules.DecisionVerificationSnapshot(
            verifiedDigests = mapOf("w1" to 111, "w2" to 222),
            findings = listOf(
                VocabularyDecisionFinding.DuplicateOrVariant(
                    primaryId = "w1",
                    duplicateId = "w2",
                    kind = DecisionDuplicateKind.SPELLING_OR_TYPO,
                    probability = 0.95,
                    confidence = 0.97,
                ),
                VocabularyDecisionFinding.MeaningConflict(
                    entryId = "w1",
                    probability = 0.93,
                    confidence = 0.95,
                    conflictingTarget = VocabularyRules.ConflictingRelationTarget(
                        isAntonym = false,
                        index = 0,
                        details = WordDetails(
                            word = "Reject",
                            partOfSpeech = "Noun",
                            meaningEnglish = "Refuse to accept",
                            meaningHinglish = "Inkaar karna",
                            exampleSentence = "They reject the offer.",
                        ),
                        conflictingWordLabel = "Synonym \"Reject\" part of speech",
                        reason = "Wrong part of speech.",
                    ),
                ),
            ),
        )

        val encoded = VocabularyDecisionRules.encodeVerificationSnapshot(snapshot)
        val decoded = VocabularyDecisionRules.decodeVerificationSnapshot(encoded)
        assertEquals(snapshot, decoded)
    }
}
