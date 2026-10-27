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
    fun `mergeDuplicateWords keeps one entry per word and the last one wins while combining relations`() {
        val first = VocabularyEntry(
            id = "a",
            wordDetails = WordDetails(
                word = "Diligent",
                meaningEnglish = "Hard-working",
                meaningHinglish = "Mehanti",
            ),
            synonyms = listOf(WordDetails(word = "Assiduous", meaningEnglish = "Careful")),
            antonyms = listOf(WordDetails(word = "Lazy", meaningEnglish = "Idle")),
            createdAt = 5L,
        )
        val other = VocabularyEntry(wordDetails = WordDetails(word = "Lazy", meaningEnglish = "Idle"))
        val repeat = VocabularyEntry(
            wordDetails = WordDetails(word = "  diligent ", meaningEnglish = "Industrious"),
            synonyms = listOf(WordDetails(word = "Tireless", meaningEnglish = "Persistent")),
            antonyms = listOf(WordDetails(word = "Slothful", meaningEnglish = "Inactive")),
            createdAt = 9L,
        )

        val merged = VocabularyRules.mergeDuplicateWords(listOf(first, other, repeat))

        assertEquals(listOf("diligent ", "Lazy").map { it.trim().lowercase() }, merged.map { it.wordDetails.word.trim().lowercase() })
        assertEquals("Industrious", merged[0].wordDetails.meaningEnglish)
        assertEquals("Mehanti", merged[0].wordDetails.meaningHinglish)
        assertEquals(listOf("Assiduous", "Tireless"), merged[0].synonyms.map { it.word })
        assertEquals(listOf("Lazy", "Slothful"), merged[0].antonyms.map { it.word })
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

    private fun entry(
        word: String,
        id: String = "",
        synonyms: List<String> = emptyList(),
        antonyms: List<String> = emptyList(),
        hinglish: String = "",
        example: String = "",
    ) = VocabularyEntry(
        id = id,
        wordDetails = WordDetails(word = word, meaningEnglish = "meaning", meaningHinglish = hinglish, exampleSentence = example),
        synonyms = synonyms.map { WordDetails(word = it, meaningEnglish = "meaning") },
        antonyms = antonyms.map { WordDetails(word = it, meaningEnglish = "meaning") },
    )

    private val abate = entry("abate", id = "a1", synonyms = listOf("reduce", "lessen"), antonyms = listOf("increase"))

    @Test
    fun `wordKey ignores case, extra spaces, hyphens, edge punctuation, and curly apostrophes`() {
        assertEquals("don't", VocabularyRules.wordKey(" Don\u2019t "))
        assertEquals("give up", VocabularyRules.wordKey("Give  \u00A0up"))
        assertEquals("hard working", VocabularyRules.wordKey("Hard-working"))
        assertEquals("hard working", VocabularyRules.wordKey("hard\u2013working."))
        assertEquals("diligent", VocabularyRules.wordKey(" \"Diligent!\" "))
    }

    @Test
    fun `documentIdForWord produces identical deterministic IDs for equivalent word keys`() {
        val id1 = VocabularyRules.documentIdForWord("Hard-working")
        val id2 = VocabularyRules.documentIdForWord("  hard working. ")
        assertEquals(id1, id2)
        assertTrue(id1.startsWith("word_hard_working_"))
    }

    @Test
    fun `findDuplicateMainWords and findEntryRelationConflict catch intra-JSON duplicates and conflicts`() {
        val dupes = VocabularyRules.findDuplicateMainWords(
            listOf(
                entry("Abandon"),
                entry("Brisk"),
                entry("  abandon. "),
            ),
        )
        assertEquals(listOf("Abandon"), dupes)

        val selfSyn = entry("Abate", synonyms = listOf("abate"))
        assertEquals("Word \"Abate\" cannot list itself (\"abate\") as a synonym.", VocabularyRules.findEntryRelationConflict(selfSyn))

        val dupSyn = entry("Abate", synonyms = listOf("Reduce", " reduce "))
        assertEquals("Word \"Abate\" has a duplicate synonym \"reduce\".", VocabularyRules.findEntryRelationConflict(dupSyn))

        val crossConflict = entry("Abate", synonyms = listOf("Reduce"), antonyms = listOf("reduce"))
        assertEquals("Word \"Abate\" lists \"reduce\" as both a synonym and an antonym.", VocabularyRules.findEntryRelationConflict(crossConflict))
    }

    @Test
    fun `checkMainWords flags the same main word in any capitalization or spacing`() {
        val check = VocabularyRules.checkMainWords(listOf(entry(" ABATE ")), listOf(abate))
        assertEquals(1, check.clashing.size)
        assertTrue(check.toSave.isEmpty())
    }

    @Test
    fun `checkMainWords never compares synonyms or antonyms of other entries`() {
        val asSynonym = VocabularyRules.checkMainWords(listOf(entry("Reduce")), listOf(abate))
        val asAntonym = VocabularyRules.checkMainWords(listOf(entry("increase")), listOf(abate))
        assertTrue(asSynonym.clashing.isEmpty() && asSynonym.toSave.size == 1)
        assertTrue(asAntonym.clashing.isEmpty() && asAntonym.toSave.size == 1)
    }

    @Test
    fun `checkMainWords lets an entry keep its own word even when an older duplicate exists`() {
        val saved = listOf(entry("abate", id = "a1"), entry("abate", id = "a2"))
        val check = VocabularyRules.checkMainWords(listOf(entry("abate", id = "a1", hinglish = "kam karna")), saved)
        assertTrue(check.clashing.isEmpty())
        assertEquals(1, check.toSave.size)
    }

    @Test
    fun `checkMainWords refuses renaming an entry onto another entry's word`() {
        val saved = listOf(entry("abate", id = "a1"), entry("abet", id = "a2"))
        val check = VocabularyRules.checkMainWords(listOf(entry("abet", id = "a1")), saved)
        assertEquals(1, check.clashing.size)
    }

    @Test
    fun `checkMainWords on a bulk list splits new, existing and repeated words`() {
        val incoming = listOf(
            entry("abandon"),
            entry("abate"),
            entry("abhor"),
            entry("Abandon", synonyms = listOf("leave")),
        )
        val check = VocabularyRules.checkMainWords(incoming, listOf(abate))
        assertEquals(listOf("Abandon", "abhor"), check.toSave.map { it.wordDetails.word })
        assertEquals(listOf("abate"), check.skippedExisting)
        assertEquals(1, check.mergedRepeats)
    }

    @Test
    fun `mergeRelations adds only new synonyms and antonyms and keeps the entry id`() {
        val incoming = entry("abate", synonyms = listOf("Reduce", "subside"), antonyms = listOf("intensify"))
        val result = VocabularyRules.mergeRelations(abate, incoming)
        assertEquals(listOf("reduce", "lessen", "subside"), result.entry.synonyms.map { it.word })
        assertEquals(listOf("increase", "intensify"), result.entry.antonyms.map { it.word })
        assertEquals(listOf("subside"), result.addedSynonymWords)
        assertEquals(listOf("intensify"), result.addedAntonymWords)
        assertEquals(1, result.addedSynonyms)
        assertEquals(1, result.addedAntonyms)
        assertEquals(1, result.skipped)
        assertEquals("a1", result.entry.id)
        assertTrue(result.changed)
    }

    @Test
    fun `mergeRelations fills blank Hinglish and example on existing synonyms and antonyms without overwriting`() {
        val existingWithBlankSynonym = VocabularyEntry(
            id = "a1",
            wordDetails = WordDetails(word = "abate", meaningEnglish = "become less"),
            synonyms = listOf(
                WordDetails(word = "reduce", meaningEnglish = "make smaller", meaningHinglish = "", exampleSentence = ""),
            ),
        )
        val incomingWithDetails = VocabularyEntry(
            wordDetails = WordDetails(word = "abate", meaningEnglish = "different english"),
            synonyms = listOf(
                WordDetails(
                    word = "Reduce",
                    meaningEnglish = "ignored english",
                    meaningHinglish = "Kam karna",
                    exampleSentence = "Reduce the speed.",
                ),
            ),
        )

        val result = VocabularyRules.mergeRelations(existingWithBlankSynonym, incomingWithDetails)
        assertTrue(result.changed)
        assertEquals(0, result.addedSynonyms)
        assertEquals(1, result.enrichedRelations)
        val updatedSyn = result.entry.synonyms.single()
        assertEquals("make smaller", updatedSyn.meaningEnglish)
        assertEquals("Kam karna", updatedSyn.meaningHinglish)
        assertEquals("Reduce the speed.", updatedSyn.exampleSentence)
    }

    @Test
    fun `mergeRelations skips the main word itself and a word already in the other list`() {
        val incoming = entry("abate", synonyms = listOf("ABATE", "increase", "fresh"), antonyms = listOf("fresh", "decrease"))
        val result = VocabularyRules.mergeRelations(abate, incoming)
        assertEquals(listOf("reduce", "lessen", "fresh"), result.entry.synonyms.map { it.word })
        assertEquals(listOf("increase", "decrease"), result.entry.antonyms.map { it.word })
    }

    @Test
    fun `mergeRelations never lets a list grow past the limit`() {
        val full = entry("big", id = "b", synonyms = (1..VocabularyRules.MAX_RELATIONS).map { "s$it" })
        val result = VocabularyRules.mergeRelations(full, entry("big", synonyms = listOf("extra"), antonyms = listOf("small")))
        assertEquals(VocabularyRules.MAX_RELATIONS, result.entry.synonyms.size)
        assertTrue(result.limitReached)
        assertEquals(0, result.addedSynonyms)
        assertEquals(1, result.addedAntonyms)
    }

    @Test
    fun `mergeRelations fills blank Hinglish and example but never overwrites`() {
        val blank = entry("abate", id = "a1")
        val filled = VocabularyRules.mergeRelations(blank, entry("abate", hinglish = "kam karna", example = "The storm abated."))
        assertEquals("kam karna", filled.entry.wordDetails.meaningHinglish)
        assertEquals("The storm abated.", filled.entry.wordDetails.exampleSentence)
        assertTrue(filled.changed)

        val kept = VocabularyRules.mergeRelations(
            entry("abate", id = "a1", hinglish = "keep me", example = "keep"),
            entry("abate", hinglish = "other", example = "other"),
        )
        assertEquals("keep me", kept.entry.wordDetails.meaningHinglish)
        assertFalse(kept.changed)
    }

    @Test
    fun `mergeRelations reports no change when everything is already present`() {
        val result = VocabularyRules.mergeRelations(abate, entry("abate", synonyms = listOf("reduce"), antonyms = listOf("increase")))
        assertFalse(result.changed)
        assertEquals(2, result.skipped)
    }
}
