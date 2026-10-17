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
        assertEquals("vocabulary", map["category"])
        assertEquals(2, map["schemaVersion"])
        assertEquals("resilient", map["wordLowercase"])
        // Word fields live only inside wordDetails (no duplicated top-level copies).
        assertNull(map["word"])
        assertNull(map["meaningEnglish"])
        assertNull(map["meaningHinglish"])
        assertNull(map["exampleSentence"])
        assertEquals(
            setOf("word", "meaningEnglish", "meaningHinglish", "exampleSentence"),
            (map["wordDetails"] as Map<*, *>).keys,
        )
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

    @Test
    fun `firestore mapper reads version 1 documents with duplicated top-level fields`() {
        val v1 = VocabularyFirestoreMapper.entryToMap(sampleEntry).toMutableMap<String, Any>().apply {
            put("schemaVersion", 1)
            put("word", "Resilient")
            put("meaningEnglish", "Able to withstand or recover quickly from difficult conditions")
        }
        assertEquals(sampleEntry, VocabularyFirestoreMapper.docToEntry("vocab-101", v1))
    }

    @Test
    fun `malformed synonym and antonym items are counted so they can be logged`() {
        val data = VocabularyFirestoreMapper.entryToMap(sampleEntry).toMutableMap<String, Any>().apply {
            put("synonyms", listOf(mapOf("word" to "Tough"), mapOf("word" to "  "), "not a map"))
            put("antonyms", listOf(mapOf("word" to "Fragile")))
        }
        assertEquals(2, VocabularyFirestoreMapper.malformedRelationCount(data))
        assertEquals(0, VocabularyFirestoreMapper.malformedRelationCount(null))
        assertEquals(1, VocabularyFirestoreMapper.docToEntry("x", data)!!.synonyms.size)
    }

    @Test
    fun `local cache is versioned json and survives quotes and newlines`() {
        val tricky = sampleEntry.copy(
            wordDetails = sampleEntry.wordDetails.copy(exampleSentence = "He said \"hi\"\nthen left | ; : \\"),
        )
        val encoded = VocabularyJsonCodec.encodeList(listOf(tricky))
        assertEquals(true, encoded.startsWith("{\"version\":2"))
        assertEquals(listOf(tricky), VocabularyJsonCodec.decodeList(encoded))
        assertEquals(emptyList<VocabularyEntry>(), VocabularyJsonCodec.decodeList(""))
        assertEquals(emptyList<VocabularyEntry>(), VocabularyJsonCodec.decodeList("{broken"))
    }

    @Test
    fun `local cache still reads the old pipe separated format`() {
        fun b64(v: String) = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(v.toByteArray())
        fun details(w: String, e: String, h: String, x: String) =
            listOf(w, e, h, x).joinToString(":") { b64(it) }
        val legacy = listOf(
            b64("old-1"), "10", "20",
            details("Brave", "Courageous", "Bahadur", "A brave soldier."),
            details("Bold", "Fearless", "Nidar", "A bold move."),
            details("Timid", "Shy", "Darpok", "A timid child."),
        ).joinToString("|")

        val decoded = VocabularyJsonCodec.decodeList(legacy).single()
        assertEquals("old-1", decoded.id)
        assertEquals("Brave", decoded.wordDetails.word)
        assertEquals(listOf("Bold"), decoded.synonyms.map { it.word })
        assertEquals(listOf("Timid"), decoded.antonyms.map { it.word })
        assertEquals(10L, decoded.createdAt)
        assertEquals(20L, decoded.updatedAt)
    }
}
