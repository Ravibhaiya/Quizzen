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
            partOfSpeech = "Adjective",
            meaningEnglish = "Able to withstand or recover quickly from difficult conditions",
            meaningHinglish = "Mushkilon se jaldi ubharne wala",
            exampleSentence = "Children are often remarkably resilient.",
        ),
        synonyms = listOf(
            WordDetails(
                word = "Tough",
                partOfSpeech = "Adjective",
                meaningEnglish = "Strong enough to withstand adverse conditions",
                meaningHinglish = "Majboot",
                exampleSentence = "She had to be tough to survive.",
            ),
        ),
        antonyms = listOf(
            WordDetails(
                word = "Fragile",
                partOfSpeech = "Adjective",
                meaningEnglish = "Easily broken or damaged",
                meaningHinglish = "Nazuk",
                exampleSentence = "His confidence was still fragile.",
            ),
        ),
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_500_000L,
    )

    @Test
    fun `firestore mapper stores schema v2 fields with partOfSpeech and round-trips entry`() {
        val map = VocabularyFirestoreMapper.entryToMap(sampleEntry)

        assertEquals("vocab-101", map["id"])
        assertEquals("vocabulary", map["category"])
        assertEquals(2, map["schemaVersion"])
        assertEquals("resilient", map["wordLowercase"])
        assertNull(map["word"])
        assertEquals(
            setOf("word", "partOfSpeech", "meaningEnglish", "meaningHinglish", "exampleSentence"),
            (map["wordDetails"] as Map<*, *>).keys,
        )
        assertEquals(listOf("tough"), map["synonymWords"])
        assertEquals(listOf("fragile"), map["antonymWords"])

        val restored = VocabularyFirestoreMapper.docToEntry("vocab-101", map)
        assertNotNull(restored)
        assertEquals(sampleEntry, restored)
    }

    @Test
    fun `local json codec round-trips entries with and without partOfSpeech`() {
        val withoutPos = sampleEntry.copy(
            id = "vocab-102",
            wordDetails = sampleEntry.wordDetails.copy(partOfSpeech = ""),
            synonyms = sampleEntry.synonyms.map { it.copy(partOfSpeech = "") },
            antonyms = sampleEntry.antonyms.map { it.copy(partOfSpeech = "") },
        )

        val encoded = VocabularyJsonCodec.encodeList(listOf(sampleEntry, withoutPos))
        val decoded = VocabularyJsonCodec.decodeList(encoded)

        assertEquals(2, decoded.size)
        assertEquals(sampleEntry, decoded[0])
        assertEquals(withoutPos, decoded[1])
    }
}
