package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyRulesTest {

    @Test
    fun `canSave requires main word and meaning in English`() {
        assertFalse(
            VocabularyRules.canSave(
                wordDetails = WordDetails(word = "", meaningEnglish = "Quick"),
                synonyms = emptyList(),
                antonyms = emptyList(),
            ),
        )
        assertFalse(
            VocabularyRules.canSave(
                wordDetails = WordDetails(word = "Swift", meaningEnglish = "   "),
                synonyms = emptyList(),
                antonyms = emptyList(),
            ),
        )
        assertTrue(
            VocabularyRules.canSave(
                wordDetails = WordDetails(word = "Swift", meaningEnglish = "Moving very fast"),
                synonyms = emptyList(),
                antonyms = emptyList(),
            ),
        )
    }

    @Test
    fun `canSave validates non-blank synonyms and antonyms while ignoring blank rows`() {
        val main = WordDetails(
            word = "Benevolent",
            meaningEnglish = "Well meaning and kindly",
            meaningHinglish = "Dayalu aur paropkari",
            exampleSentence = "She was a benevolent leader.",
        )

        assertFalse(
            VocabularyRules.canSave(
                wordDetails = main,
                synonyms = listOf(WordDetails(word = "Kind", meaningEnglish = "")),
                antonyms = emptyList(),
            ),
        )

        assertFalse(
            VocabularyRules.canSave(
                wordDetails = main,
                synonyms = emptyList(),
                antonyms = listOf(WordDetails(word = "", meaningEnglish = "Cruel")),
            ),
        )

        assertTrue(
            VocabularyRules.canSave(
                wordDetails = main,
                synonyms = listOf(
                    WordDetails(),
                    WordDetails(
                        word = "Compassionate",
                        meaningEnglish = "Feeling or showing sympathy",
                        meaningHinglish = "Dard samajhne wala",
                        exampleSentence = "He is compassionate towards animals.",
                    ),
                ),
                antonyms = listOf(
                    WordDetails(
                        word = "Malevolent",
                        meaningEnglish = "Having or showing a wish to do evil",
                        meaningHinglish = "Bura chahne wala",
                        exampleSentence = "The villain had a malevolent smile.",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `sanitized trims fields and drops completely blank synonyms and antonyms`() {
        val entry = VocabularyEntry(
            id = "1",
            wordDetails = WordDetails(
                word = "  Candid ",
                meaningEnglish = " Truthful and straightforward ",
                meaningHinglish = " Saaf dil se bolne wala ",
                exampleSentence = " Let me be candid with you. ",
            ),
            synonyms = listOf(
                WordDetails(),
                WordDetails(word = " Frank ", meaningEnglish = " Open and honest "),
            ),
            antonyms = listOf(
                WordDetails(word = " Secretive ", meaningEnglish = " Inclined to conceal "),
                WordDetails(word = "  ", meaningEnglish = "  "),
            ),
        ).sanitized()

        assertEquals("Candid", entry.wordDetails.word)
        assertEquals("Truthful and straightforward", entry.wordDetails.meaningEnglish)
        assertEquals("Saaf dil se bolne wala", entry.wordDetails.meaningHinglish)
        assertEquals("Let me be candid with you.", entry.wordDetails.exampleSentence)
        assertEquals(1, entry.synonyms.size)
        assertEquals("Frank", entry.synonyms.first().word)
        assertEquals(1, entry.antonyms.size)
        assertEquals("Secretive", entry.antonyms.first().word)
    }

    @Test
    fun `matchesSearch checks main word, meanings, synonyms, and antonyms`() {
        val entry = VocabularyEntry(
            id = "1",
            wordDetails = WordDetails(
                word = "Ephemeral",
                meaningEnglish = "Lasting for a very short time",
                meaningHinglish = "Thode samay ke liye rehne wala",
                exampleSentence = "Fame can be ephemeral.",
            ),
            synonyms = listOf(
                WordDetails(
                    word = "Transient",
                    meaningEnglish = "Lasting only for a short time",
                    meaningHinglish = "Kshanik",
                    exampleSentence = "A transient mood.",
                ),
            ),
            antonyms = listOf(
                WordDetails(
                    word = "Permanent",
                    meaningEnglish = "Lasting forever",
                    meaningHinglish = "Sthayi",
                    exampleSentence = "A permanent solution.",
                ),
            ),
        )

        assertTrue(VocabularyRules.matchesSearch(entry, ""))
        assertTrue(VocabularyRules.matchesSearch(entry, "ephem"))
        assertTrue(VocabularyRules.matchesSearch(entry, "thode samay"))
        assertTrue(VocabularyRules.matchesSearch(entry, "transient"))
        assertTrue(VocabularyRules.matchesSearch(entry, "sthayi"))
        assertFalse(VocabularyRules.matchesSearch(entry, "ubiquitous"))
    }

    @Test
    fun `VocabularyJsonFormat parses bulk array and single object and round-trips`() {
        val bulkJson = """
            [
              {
                "word": "Diligent",
                "meaningEnglish": "Hard-working",
                "meaningHinglish": "Mehanti",
                "exampleSentence": "She is diligent.",
                "synonyms": [
                  {
                    "word": "Industrious",
                    "meaningEnglish": "Assiduous",
                    "meaningHinglish": "Parishrami",
                    "exampleSentence": "An industrious student."
                  }
                ],
                "antonyms": [
                  {
                    "word": "Lazy",
                    "meaningEnglish": "Idle",
                    "meaningHinglish": "Aalsi",
                    "exampleSentence": "He was lazy."
                  }
                ]
              },
              {
                "word": "Lucid",
                "meaningEnglish": "Expressed clearly",
                "meaningHinglish": "Spasht",
                "exampleSentence": "A lucid explanation."
              }
            ]
        """.trimIndent()

        val parsed = VocabularyJsonFormat.parse(bulkJson, singleMode = false)
        assertTrue(parsed is VocabularyJsonFormat.ParseResult.Success)
        val entries = (parsed as VocabularyJsonFormat.ParseResult.Success).entries
        assertEquals(2, entries.size)
        assertEquals("Diligent", entries[0].wordDetails.word)
        assertEquals("Industrious", entries[0].synonyms.single().word)
        assertEquals("Lazy", entries[0].antonyms.single().word)
        assertEquals("Lucid", entries[1].wordDetails.word)

        // Single mode rejects multi-word array
        val singleRejected = VocabularyJsonFormat.parse(bulkJson, singleMode = true)
        assertTrue(singleRejected is VocabularyJsonFormat.ParseResult.Error)

        // Single word formatting round-trip
        val formattedSingle = VocabularyJsonFormat.formatSingle(entries[0])
        val reparsedSingle = VocabularyJsonFormat.parse(formattedSingle, singleMode = true)
        assertTrue(reparsedSingle is VocabularyJsonFormat.ParseResult.Success)
        val singleEntry = (reparsedSingle as VocabularyJsonFormat.ParseResult.Success).entries.single()
        assertEquals(entries[0].wordDetails, singleEntry.wordDetails)
        assertEquals(entries[0].synonyms, singleEntry.synonyms)
        assertEquals(entries[0].antonyms, singleEntry.antonyms)
    }

    @Test
    fun `mergeDuplicateWords keeps one entry per word and the last one wins`() {
        val first = VocabularyEntry(
            id = "a",
            wordDetails = WordDetails(word = "Diligent", meaningEnglish = "Hard-working"),
            createdAt = 5L,
        )
        val other = VocabularyEntry(wordDetails = WordDetails(word = "Lazy", meaningEnglish = "Idle"))
        val repeat = VocabularyEntry(
            wordDetails = WordDetails(word = "  diligent ", meaningEnglish = "Industrious"),
            createdAt = 9L,
        )

        val merged = VocabularyRules.mergeDuplicateWords(listOf(first, other, repeat))

        assertEquals(listOf("diligent ", "Lazy").map { it.trim().lowercase() }, merged.map { it.wordDetails.word.trim().lowercase() })
        assertEquals("Industrious", merged[0].wordDetails.meaningEnglish)
        assertEquals("a", merged[0].id)
        assertEquals(5L, merged[0].createdAt)
    }

    @Test
    fun `mergeDuplicateWords drops repeated synonyms and antonyms inside an entry`() {
        val entry = VocabularyEntry(
            wordDetails = WordDetails(word = "Quick", meaningEnglish = "Fast"),
            synonyms = listOf(
                WordDetails(word = "Fast", meaningEnglish = "Rapid"),
                WordDetails(word = "FAST", meaningEnglish = "Rapid again"),
                WordDetails(word = "Swift", meaningEnglish = "Speedy"),
            ),
            antonyms = listOf(
                WordDetails(word = "Slow", meaningEnglish = "Not fast"),
                WordDetails(word = "slow", meaningEnglish = "Not fast"),
            ),
        )

        val merged = VocabularyRules.mergeDuplicateWords(listOf(entry)).single()

        assertEquals(listOf("Fast", "Swift"), merged.synonyms.map { it.word })
        assertEquals(listOf("Slow"), merged.antonyms.map { it.word })
    }
}
