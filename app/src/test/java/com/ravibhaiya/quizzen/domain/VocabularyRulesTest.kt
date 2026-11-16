package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyRulesTest {

    @Test
    fun `canSave validates main word, synonyms, and antonyms while ignoring blank rows`() {
        val main = WordDetails(
            word = "Ephemeral",
            partOfSpeech = "Adjective",
            meaningEnglish = "Lasting for a very short time",
            meaningHinglish = "Kshanik",
            exampleSentence = "Fame is ephemeral.",
        )

        assertFalse(VocabularyRules.canSave(WordDetails(word = "", meaningEnglish = "Valid"), emptyList(), emptyList()))
        assertFalse(VocabularyRules.canSave(WordDetails(word = "Valid", meaningEnglish = "  "), emptyList(), emptyList()))
        assertFalse(
            VocabularyRules.canSave(
                wordDetails = main,
                synonyms = listOf(WordDetails(word = "Transient", meaningEnglish = "")),
                antonyms = emptyList(),
            ),
        )
        assertFalse(
            VocabularyRules.canSave(
                wordDetails = main,
                synonyms = emptyList(),
                antonyms = listOf(WordDetails(word = "", meaningEnglish = "Permanent")),
            ),
        )
        assertTrue(
            VocabularyRules.canSave(
                wordDetails = main,
                synonyms = listOf(
                    WordDetails(),
                    WordDetails(word = "Transient", partOfSpeech = "Adjective", meaningEnglish = "Short-lived"),
                ),
                antonyms = listOf(
                    WordDetails(word = "Permanent", partOfSpeech = "Adjective", meaningEnglish = "Lasting forever"),
                ),
            ),
        )
    }

    @Test
    fun `wordKey and wordIdentityKey normalize surface forms and distinguish different parts of speech`() {
        assertEquals("don't", VocabularyRules.wordKey(" Don\u2019t "))
        assertEquals("hard working", VocabularyRules.wordKey("Hard-working."))
        assertEquals("run", VocabularyRules.wordIdentityKey("Run", ""))
        assertEquals("run#noun", VocabularyRules.wordIdentityKey("Run", "Noun"))
        assertEquals("run#verb", VocabularyRules.wordIdentityKey(" run ", " VERB "))

        val runNoun = WordDetails(word = "Run", partOfSpeech = "Noun", meaningEnglish = "A spell of running")
        val runVerb = WordDetails(word = "Run", partOfSpeech = "Verb", meaningEnglish = "Move fast on foot")
        val runNounLower = WordDetails(word = "run", partOfSpeech = "noun", meaningEnglish = "Act of running")
        val runBlank = WordDetails(word = "Run", partOfSpeech = "", meaningEnglish = "Unspecified run")

        assertTrue(VocabularyRules.hasDifferentPartOfSpeech(runNoun, runVerb))
        assertTrue(VocabularyRules.hasDifferentPartOfSpeech(runNoun, runBlank))
        assertFalse(VocabularyRules.hasDifferentPartOfSpeech(runNoun, runNounLower))
    }

    @Test
    fun `JSON formatting always emits partOfSpeech for main word, synonyms, and antonyms and round-trips`() {
        val template = VocabularyJsonFormat.formatBulkTemplate()
        assertEquals(3, "\"partOfSpeech\": \"\"".toRegex().findAll(template).count())

        val detailsJson = VocabularyJsonFormat.formatWordDetails(
            WordDetails(word = "Candid", partOfSpeech = "", meaningEnglish = "Honest"),
        )
        assertTrue(detailsJson.contains("\"partOfSpeech\": \"\""))

        val entry = VocabularyEntry(
            id = "e1",
            wordDetails = WordDetails(
                word = "Repudiate",
                partOfSpeech = "Verb",
                meaningEnglish = "Refuse to accept",
                meaningHinglish = "Nakaarna",
                exampleSentence = "He chose to repudiate the claim.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Reject",
                    partOfSpeech = "Verb",
                    meaningEnglish = "Dismiss as inadequate",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Embrace",
                    partOfSpeech = "",
                    meaningEnglish = "Accept willingly",
                ),
            ),
        )

        val formatted = VocabularyJsonFormat.formatSingle(entry)
        assertEquals(3, "\"partOfSpeech\":".toRegex().findAll(formatted).count())
        assertTrue(formatted.contains("\"partOfSpeech\": \"Verb\""))
        assertTrue(formatted.contains("\"partOfSpeech\": \"\""))

        val parsed = VocabularyJsonFormat.parse(formatted, singleMode = true)
        assertTrue(parsed is VocabularyJsonFormat.ParseResult.Success)
        val roundTripped = (parsed as VocabularyJsonFormat.ParseResult.Success).entries.single()
        assertEquals(entry.wordDetails, roundTripped.wordDetails)
        assertEquals(entry.synonyms, roundTripped.synonyms)
        assertEquals(entry.antonyms, roundTripped.antonyms)
    }

    @Test
    fun `same word with different partOfSpeech is allowed in JSON input, relations, checkMainWords, and document IDs`() {
        val presentNoun = VocabularyEntry(
            wordDetails = WordDetails(word = "Present", partOfSpeech = "Noun", meaningEnglish = "A gift"),
            synonyms = listOf(
                WordDetails(word = "Light", partOfSpeech = "Noun", meaningEnglish = "Illumination"),
                WordDetails(word = "Light", partOfSpeech = "Adjective", meaningEnglish = "Not heavy"),
            ),
        )
        val presentVerb = VocabularyEntry(
            wordDetails = WordDetails(word = "Present", partOfSpeech = "Verb", meaningEnglish = "Give formally"),
            synonyms = listOf(
                WordDetails(word = "Award", partOfSpeech = "Verb", meaningEnglish = "Grant officially"),
            ),
        )

        // 1. Relations with the same word but different partOfSpeech inside one entry are allowed
        assertNull(VocabularyRules.findEntryRelationConflict(presentNoun))

        // 2. Main words with the same spelling but different partOfSpeech in one JSON input are not duplicates
        assertTrue(VocabularyRules.findDuplicateMainWords(listOf(presentNoun, presentVerb)).isEmpty())

        // 3. Main words with the same spelling AND same partOfSpeech ARE caught as duplicates
        val duplicateNoun = VocabularyEntry(
            wordDetails = WordDetails(word = " present ", partOfSpeech = "noun", meaningEnglish = "Another gift"),
        )
        assertEquals(listOf("Present"), VocabularyRules.findDuplicateMainWords(listOf(presentNoun, duplicateNoun)))

        // 4. Saving both entries assigns disambiguated document IDs without clashing
        val savedNoun = VocabularyRules.prepareEntriesForSave(listOf(presentNoun), emptyList(), now = 100L).entries
        val checkVerb = VocabularyRules.checkMainWords(listOf(presentVerb), savedNoun)
        assertTrue(checkVerb.clashing.isEmpty())
        assertEquals(1, checkVerb.toSave.size)

        val savedVerbBatch = VocabularyRules.prepareEntriesForSave(checkVerb.toSave, savedNoun, now = 200L)
        val baseId = VocabularyRules.documentIdForWord("Present")
        assertEquals(baseId, savedNoun.single().id)
        assertEquals("${baseId}_2", savedVerbBatch.entries.single().id)
        assertEquals(setOf("${baseId}_2"), savedVerbBatch.createOnlyIds)
    }

    @Test
    fun `checkMainWords and mergeRelations merge same-POS entries while preserving existing meanings`() {
        val existing = VocabularyEntry(
            id = "word_abate",
            wordDetails = WordDetails(word = "Abate", partOfSpeech = "Verb", meaningEnglish = "Become less intense"),
            synonyms = listOf(WordDetails(word = "Subside", partOfSpeech = "", meaningEnglish = "Die down")),
            antonyms = listOf(WordDetails(word = "Intensify", partOfSpeech = "Verb", meaningEnglish = "Become stronger")),
        )
        val incomingSamePos = VocabularyEntry(
            wordDetails = WordDetails(
                word = "ABATE",
                partOfSpeech = "verb",
                meaningEnglish = "Different meaning",
                meaningHinglish = "Kam hona",
                exampleSentence = "The storm abated.",
            ),
            synonyms = listOf(
                WordDetails(word = "Subside", partOfSpeech = "Verb", meaningEnglish = "Die down", meaningHinglish = "Shant hona"),
                WordDetails(word = "Wane", partOfSpeech = "Verb", meaningEnglish = "Decrease in vigor"),
            ),
        )

        val check = VocabularyRules.checkMainWords(listOf(incomingSamePos), listOf(existing))
        assertEquals(1, check.clashing.size)
        assertTrue(check.toSave.isEmpty())

        val merged = VocabularyRules.mergeRelations(existing, incomingSamePos)
        assertTrue(merged.changed)
        assertTrue(merged.mainDetailsEnriched)
        assertEquals(1, merged.addedSynonyms)
        assertEquals(1, merged.enrichedRelations)
        assertEquals("Become less intense", merged.entry.wordDetails.meaningEnglish)
        assertEquals("Kam hona", merged.entry.wordDetails.meaningHinglish)
        assertEquals("Verb", merged.entry.synonyms.first { it.word == "Subside" }.partOfSpeech)
        assertEquals(listOf("Subside", "Wane"), merged.entry.synonyms.map { it.word })
    }

    @Test
    fun `validateFieldLengths and findEntryRelationConflict enforce firestore limits and catch true relation duplicates`() {
        val selfSyn = VocabularyEntry(
            wordDetails = WordDetails(word = "Abate", partOfSpeech = "Verb", meaningEnglish = "Lessen"),
            synonyms = listOf(WordDetails(word = "abate", partOfSpeech = "Verb", meaningEnglish = "Lessen")),
        )
        assertEquals(
            "Word \"Abate\" cannot list itself (\"abate\") as a synonym.",
            VocabularyRules.findEntryRelationConflict(selfSyn),
        )

        val crossConflict = VocabularyEntry(
            wordDetails = WordDetails(word = "Abate", partOfSpeech = "Verb", meaningEnglish = "Lessen"),
            synonyms = listOf(WordDetails(word = "Reduce", partOfSpeech = "Verb", meaningEnglish = "Make smaller")),
            antonyms = listOf(WordDetails(word = "reduce", partOfSpeech = "verb", meaningEnglish = "Make smaller")),
        )
        assertEquals(
            "Word \"Abate\" lists \"reduce\" as both a synonym and an antonym.",
            VocabularyRules.findEntryRelationConflict(crossConflict),
        )

        val tooLongPos = "p".repeat(VocabularyRules.MAX_PART_OF_SPEECH_LENGTH + 5)
        val posEntry = VocabularyEntry(
            wordDetails = WordDetails(word = "Abate", partOfSpeech = tooLongPos, meaningEnglish = "Lessen"),
        )
        val posError = VocabularyRules.findEntryRelationConflict(posEntry)
        assertTrue(posError!!.contains("Part of speech for word \"Abate\" is too long"))
    }
}
