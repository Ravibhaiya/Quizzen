package com.ravibhaiya.quizzen.data

import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class VocabularyPersistenceTest {

    private val sampleEntry = VocabularyEntry(
        id = "vocab-101",
        wordDetails = WordDetails(
            word = "Resilient",
            meaningEnglish = "Able to withstand or recover quickly from difficult conditions",
            meaningHinglish = "Mushkilon se jaldi ubharne wala",
            exampleSentence = "Children are often remarkably resilient.",
        ),
        synonyms = listOf(
            WordDetails(
                word = "Tough",
                meaningEnglish = "Strong enough to withstand adverse conditions",
                meaningHinglish = "Majboot",
                exampleSentence = "She had to be tough to survive.",
            ),
            WordDetails(
                word = "Adaptable",
                meaningEnglish = "Able to adjust to new conditions",
                meaningHinglish = "Paristithi ke anusaar dhalne wala",
                exampleSentence = "An adaptable workforce.",
            ),
        ),
        antonyms = listOf(
            WordDetails(
                word = "Fragile",
                meaningEnglish = "Easily broken or damaged",
                meaningHinglish = "Nazuk",
                exampleSentence = "His confidence was still fragile.",
            ),
        ),
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_500_000L,
    )

    @Test
    fun `firestore mapper stores future-proof schema fields and round-trips entry`() {
        val map = VocabularyFirestoreMapper.entryToMap(sampleEntry)

        assertEquals("vocab-101", map["id"])
        assertEquals(1, map["schemaVersion"])
        assertEquals("vocabulary", map["category"])
        assertEquals("Resilient", map["word"])
        assertEquals("resilient", map["wordLowercase"])
        assertEquals(listOf("tough", "adaptable"), map["synonymWords"])
        assertEquals(listOf("fragile"), map["antonymWords"])
        assertEquals(1_700_000_000_000L, map["createdAt"])
        assertEquals(1_700_000_500_000L, map["updatedAt"])

        val restored = VocabularyFirestoreMapper.docToEntry("vocab-101", map)
        assertNotNull(restored)
        assertEquals(sampleEntry, restored)
    }

    @Test
    fun `firestore mapper reads legacy or top-level flat document fields gracefully`() {
        val flatMap = mapOf<String, Any>(
            "word" to "Eloquent",
            "meaningEnglish" to "Fluent or persuasive in speaking or writing",
            "meaningHinglish" to "Prabhavshali vakta",
            "exampleSentence" to "An eloquent speech.",
            "updatedAt" to 1_700_001_000_000L,
        )

        val restored = VocabularyFirestoreMapper.docToEntry("flat-1", flatMap)
        assertNotNull(restored)
        assertEquals("Eloquent", restored!!.wordDetails.word)
        assertEquals("Fluent or persuasive in speaking or writing", restored.wordDetails.meaningEnglish)
        assertEquals("Prabhavshali vakta", restored.wordDetails.meaningHinglish)
        assertEquals("An eloquent speech.", restored.wordDetails.exampleSentence)
        assertEquals(1_700_001_000_000L, restored.createdAt)
        assertEquals(1_700_001_000_000L, restored.updatedAt)

        assertNull(VocabularyFirestoreMapper.docToEntry("invalid", mapOf("word" to "   ")))
        assertNull(VocabularyFirestoreMapper.docToEntry("null-doc", null))
    }

    @Test
    fun `local json codec round-trips list of vocabulary entries`() {
        val encoded = VocabularyJsonCodec.encodeList(listOf(sampleEntry))
        val decoded = VocabularyJsonCodec.decodeList(encoded)

        assertEquals(1, decoded.size)
        assertEquals(sampleEntry, decoded.single())
    }
}
