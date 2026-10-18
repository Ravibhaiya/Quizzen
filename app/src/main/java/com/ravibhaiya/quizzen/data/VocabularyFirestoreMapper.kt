package com.ravibhaiya.quizzen.data

import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails

/**
 * Pure mapping helpers between [VocabularyEntry] and Firestore document maps so schema mapping is
 * unit-testable on the JVM without requiring an Android runtime or Firebase emulator.
 *
 * Schema v2: word text, meanings, and example sentence live only inside `wordDetails` (never duplicated at the top
 * level); top-level fields are only document metadata and lowercase search keys (`wordLowercase`, `synonymWords`,
 * `antonymWords`). `docToEntry` still reads legacy v1 / flat documents that had `word` at the top level.
 */
object VocabularyFirestoreMapper {
    private const val SCHEMA_VERSION = 2
    private const val CATEGORY_VOCABULARY = "vocabulary"

    fun entryToMap(entry: VocabularyEntry): Map<String, Any> {
        val clean = entry.sanitized()
        return mapOf(
            "id" to clean.id,
            "category" to CATEGORY_VOCABULARY,
            "schemaVersion" to SCHEMA_VERSION,
            "wordLowercase" to clean.wordDetails.word.lowercase(),
            "wordDetails" to detailsToMap(clean.wordDetails),
            "synonyms" to clean.synonyms.map(::detailsToMap),
            "antonyms" to clean.antonyms.map(::detailsToMap),
            "synonymWords" to clean.synonyms.map { it.word.lowercase() },
            "antonymWords" to clean.antonyms.map { it.word.lowercase() },
            "createdAt" to clean.createdAt,
            "updatedAt" to clean.updatedAt,
        )
    }

    fun detailsToMap(details: WordDetails): Map<String, String> = mapOf(
        "word" to details.word.trim(),
        "meaningEnglish" to details.meaningEnglish.trim(),
        "meaningHinglish" to details.meaningHinglish.trim(),
        "exampleSentence" to details.exampleSentence.trim(),
    )

    fun docToEntry(docId: String, data: Map<String, Any?>?): VocabularyEntry? {
        if (data == null) return null

        val wordDetailsMap = data["wordDetails"] as? Map<*, *>
        val mainDetails = if (wordDetailsMap != null) {
            mapToDetails(wordDetailsMap)
        } else {
            WordDetails(
                word = (data["word"] as? String).orEmpty().trim(),
                meaningEnglish = (data["meaningEnglish"] as? String).orEmpty().trim(),
                meaningHinglish = (data["meaningHinglish"] as? String).orEmpty().trim(),
                exampleSentence = (data["exampleSentence"] as? String).orEmpty().trim(),
            )
        }

        if (mainDetails.word.isBlank()) return null

        val synonyms = (data["synonyms"] as? List<*>)
            ?.mapNotNull { item -> (item as? Map<*, *>)?.let(::mapToDetails) }
            ?.filter { it.word.isNotBlank() }
            .orEmpty()

        val antonyms = (data["antonyms"] as? List<*>)
            ?.mapNotNull { item -> (item as? Map<*, *>)?.let(::mapToDetails) }
            ?.filter { it.word.isNotBlank() }
            .orEmpty()

        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: updatedAt
        val resolvedId = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: docId

        return VocabularyEntry(
            id = resolvedId,
            wordDetails = mainDetails,
            synonyms = synonyms,
            antonyms = antonyms,
            createdAt = createdAt,
            updatedAt = updatedAt,
        ).sanitized()
    }

    /** Count of synonym/antonym list items that were not valid maps or had a blank `word` (for logging). */
    fun malformedRelationCount(data: Map<String, Any?>?): Int {
        if (data == null) return 0
        fun badIn(raw: Any?): Int = (raw as? List<*>)?.count { item ->
            val map = item as? Map<*, *> ?: return@count true
            mapToDetails(map).word.isBlank()
        } ?: 0
        return badIn(data["synonyms"]) + badIn(data["antonyms"])
    }

    private fun mapToDetails(map: Map<*, *>): WordDetails = WordDetails(
        word = (map["word"] as? String).orEmpty().trim(),
        meaningEnglish = (map["meaningEnglish"] as? String).orEmpty().trim(),
        meaningHinglish = (map["meaningHinglish"] as? String).orEmpty().trim(),
        exampleSentence = (map["exampleSentence"] as? String).orEmpty().trim(),
    )
}
