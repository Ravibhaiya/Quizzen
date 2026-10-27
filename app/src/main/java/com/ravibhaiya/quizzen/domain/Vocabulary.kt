package com.ravibhaiya.quizzen.domain

import java.text.Normalizer

/**
 * The four details stored and displayed for a primary vocabulary word, a synonym, or an antonym.
 */
data class WordDetails(
    val word: String = "",
    val meaningEnglish: String = "",
    val meaningHinglish: String = "",
    val exampleSentence: String = "",
) {
    fun trimmed(): WordDetails = WordDetails(
        word = word.trim(),
        meaningEnglish = meaningEnglish.trim(),
        meaningHinglish = meaningHinglish.trim(),
        exampleSentence = exampleSentence.trim(),
    )

    val isBlank: Boolean
        get() = word.isBlank() &&
            meaningEnglish.isBlank() &&
            meaningHinglish.isBlank() &&
            exampleSentence.isBlank()

    val isValid: Boolean
        get() = word.isNotBlank() && meaningEnglish.isNotBlank()
}

/**
 * A complete vocabulary item with its primary word details, synonyms, and antonyms.
 */
data class VocabularyEntry(
    val id: String = "",
    val wordDetails: WordDetails = WordDetails(),
    val synonyms: List<WordDetails> = emptyList(),
    val antonyms: List<WordDetails> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    fun sanitized(): VocabularyEntry = copy(
        wordDetails = wordDetails.trimmed(),
        synonyms = synonyms.map { it.trimmed() }.filterNot { it.isBlank },
        antonyms = antonyms.map { it.trimmed() }.filterNot { it.isBlank },
    )
}

object VocabularyRules {

    /**
     * Checks whether a draft entry is valid to save:
     * - The main word must have a non-blank word and English meaning.
     * - Every non-blank synonym and antonym must also have a non-blank word and English meaning.
     */
    fun canSave(
        wordDetails: WordDetails,
        synonyms: List<WordDetails>,
        antonyms: List<WordDetails>,
    ): Boolean {
        if (!wordDetails.trimmed().isValid) return false
        val validSynonyms = synonyms.map { it.trimmed() }.filterNot { it.isBlank }.all { it.isValid }
        val validAntonyms = antonyms.map { it.trimmed() }.filterNot { it.isBlank }.all { it.isValid }
        return validSynonyms && validAntonyms
    }

    /**
     * Collapses entries that share the same word (case-insensitive) into one: the last occurrence wins for
     * main word details (inheriting blank Hinglish/example, missing id, and earlier creation time from the
     * first), while synonyms and antonyms from all occurrences are combined without duplicates.
     * Duplicate synonyms and antonyms inside an entry (same word, case-insensitive) are also collapsed,
     * keeping the first. Used before saving so one import can never create two documents for a word.
     */
    fun mergeDuplicateWords(entries: List<VocabularyEntry>): List<VocabularyEntry> {
        val byWord = LinkedHashMap<String, VocabularyEntry>()
        for (raw in entries) {
            val key = wordKey(raw.wordDetails.word)
            val cleanSynonyms = raw.synonyms
                .distinctBy { wordKey(it.word) }
                .filterNot { wordKey(it.word) == key }
                .take(MAX_RELATIONS)
            val synKeys = cleanSynonyms.map { wordKey(it.word) }.toSet()
            val cleanAntonyms = raw.antonyms
                .distinctBy { wordKey(it.word) }
                .filterNot { wordKey(it.word) == key || wordKey(it.word) in synKeys }
                .take(MAX_RELATIONS)
            val entry = raw.copy(
                synonyms = cleanSynonyms,
                antonyms = cleanAntonyms,
            )
            val previous = byWord[key]
            byWord[key] = if (previous == null) {
                entry
            } else {
                val mergedRelations = mergeRelations(previous, entry).entry
                val latestDetails = entry.wordDetails.copy(
                    meaningEnglish = entry.wordDetails.meaningEnglish.ifBlank { previous.wordDetails.meaningEnglish },
                    meaningHinglish = entry.wordDetails.meaningHinglish.ifBlank { previous.wordDetails.meaningHinglish },
                    exampleSentence = entry.wordDetails.exampleSentence.ifBlank { previous.wordDetails.exampleSentence },
                )
                mergedRelations.copy(
                    id = entry.id.ifBlank { previous.id },
                    wordDetails = latestDetails,
                    createdAt = listOf(entry.createdAt, previous.createdAt).filter { it > 0L }.minOrNull() ?: 0L,
                )
            }
        }
        return byWord.values.toList()
    }

    private val whitespaceRun = Regex("[\\s\\u00A0]+")
    private val hyphenOrDash = Regex("[-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015]+")
    private val edgePunctuation = Regex("^[.,;:!?\"()\\[\\]{}]+|[.,;:!?\"()\\[\\]{}]+$")
    private val nonSlugChars = Regex("[^a-z0-9]+")

    /** Most synonyms (and most antonyms) one word can hold; the same limit is enforced by `firestore.rules`. */
    const val MAX_RELATIONS = 30

    /**
     * The identity of a main word: capitalization, extra or non-breaking spaces, hyphens versus spaces,
     * surrounding punctuation, and curly versus straight apostrophes are ignored. Only ever applied to main
     * words for the uniqueness rule; synonyms and antonyms are never compared with other entries' main words.
     */
    fun wordKey(word: String): String =
        Normalizer.normalize(word, Normalizer.Form.NFC)
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .trim()
            .replace(edgePunctuation, "")
            .replace(hyphenOrDash, " ")
            .replace(whitespaceRun, " ")
            .trim()
            .lowercase()

    /**
     * Deterministic document ID derived from [wordKey] so the same main word always maps to the same
     * Firestore document ID and cannot create duplicate documents across devices or retries.
     */
    fun documentIdForWord(word: String): String {
        val key = wordKey(word)
        require(key.isNotEmpty()) { "Main word cannot be empty" }
        val slug = key.replace(nonSlugChars, "_").trim('_').take(48)
        val hash = key.fold(0x811c9dc5.toInt()) { acc, ch ->
            (acc xor ch.code) * 0x01000193
        }.toUInt().toString(16).padStart(8, '0')
        return if (slug.isNotEmpty()) "word_${slug}_$hash" else "word_$hash"
    }

    /**
     * Returns the display names of any main words that appear more than once in [entries] (compared via [wordKey]).
     */
    fun findDuplicateMainWords(entries: List<VocabularyEntry>): List<String> {
        val seen = LinkedHashMap<String, String>()
        val duplicates = LinkedHashSet<String>()
        for (entry in entries) {
            val rawWord = entry.wordDetails.word.trim()
            val key = wordKey(rawWord)
            if (key.isEmpty()) continue
            val firstWord = seen[key]
            if (firstWord != null) {
                duplicates.add(firstWord)
            } else {
                seen[key] = rawWord
            }
        }
        return duplicates.toList()
    }

    /**
     * Checks a single entry for internal synonym/antonym duplicates or conflicts so nothing is silently
     * dropped without the user's awareness. Returns a human-readable error message, or `null` if clean.
     */
    fun findEntryRelationConflict(entry: VocabularyEntry): String? {
        val label = entry.wordDetails.word.trim()
        val mainKey = wordKey(label)
        if (entry.synonyms.size > MAX_RELATIONS) {
            return "Word \"$label\" has ${entry.synonyms.size} synonyms (maximum is $MAX_RELATIONS)."
        }
        if (entry.antonyms.size > MAX_RELATIONS) {
            return "Word \"$label\" has ${entry.antonyms.size} antonyms (maximum is $MAX_RELATIONS)."
        }
        val synKeys = HashSet<String>()
        for (syn in entry.synonyms) {
            val synWord = syn.word.trim()
            val key = wordKey(synWord)
            if (key == mainKey) {
                return "Word \"$label\" cannot list itself (\"$synWord\") as a synonym."
            }
            if (!synKeys.add(key)) {
                return "Word \"$label\" has a duplicate synonym \"$synWord\"."
            }
        }
        val antKeys = HashSet<String>()
        for (ant in entry.antonyms) {
            val antWord = ant.word.trim()
            val key = wordKey(antWord)
            if (key == mainKey) {
                return "Word \"$label\" cannot list itself (\"$antWord\") as an antonym."
            }
            if (key in synKeys) {
                return "Word \"$label\" lists \"$antWord\" as both a synonym and an antonym."
            }
            if (!antKeys.add(key)) {
                return "Word \"$label\" has a duplicate antonym \"$antWord\"."
            }
        }
        return null
    }

    /**
     * Result of [checkMainWords]: [toSave] are new words (or unchanged/edited ones), [clashing] are incoming entries
     * whose main word already belongs to another saved entry, [mergedRepeats] counts repeats collapsed inside the batch.
     */
    data class WordCheck(
        val toSave: List<VocabularyEntry>,
        val clashing: List<VocabularyEntry>,
        val mergedRepeats: Int,
    ) {
        val skippedExisting: List<String> get() = clashing.map { it.wordDetails.word.trim() }
    }

    /**
     * Keeps one entry per main word. Repeats inside [incoming] are collapsed (the last one wins). An incoming entry
     * clashes when its word key belongs to a saved entry, unless it is that same entry (same id, same word), so
     * editing a word still works even if older duplicates exist. Renaming onto another word clashes.
     */
    fun checkMainWords(incoming: List<VocabularyEntry>, existing: List<VocabularyEntry>): WordCheck {
        val merged = mergeDuplicateWords(incoming)
        val keyById = existing.filter { it.id.isNotBlank() }.associate { it.id to wordKey(it.wordDetails.word) }
        val taken = existing.map { wordKey(it.wordDetails.word) }.toSet()
        val (clashing, fresh) = merged.partition { entry ->
            val key = wordKey(entry.wordDetails.word)
            val unchanged = entry.id.isNotBlank() && keyById[entry.id] == key
            key in taken && !unchanged
        }
        return WordCheck(toSave = fresh, clashing = clashing, mergedRepeats = incoming.size - merged.size)
    }

    data class MergeResult(
        val entry: VocabularyEntry,
        val addedSynonyms: Int,
        val addedAntonyms: Int,
        val skipped: Int,
        val limitReached: Boolean,
        val changed: Boolean,
        val addedSynonymWords: List<String> = emptyList(),
        val addedAntonymWords: List<String> = emptyList(),
        val enrichedRelations: Int = 0,
        val mainDetailsEnriched: Boolean = false,
    )

    data class MergeSummary(
        val wordsUpdated: Int,
        val addedSynonyms: Int,
        val addedAntonyms: Int,
        val skipped: Int,
        val limitReached: Boolean,
        val enrichedRelations: Int = 0,
    )

    /**
     * Adds the new synonyms and antonyms of [incoming] to [existing]. The existing word, meanings and example are
     * never overwritten (a blank Hinglish meaning or example on the main word or on an existing synonym/antonym is
     * filled in). An item is skipped when it equals the main word, is already in either list (with no blank fields
     * to fill), or would push a list past [MAX_RELATIONS].
     */
    fun mergeRelations(existing: VocabularyEntry, incoming: VocabularyEntry): MergeResult {
        val clean = incoming.sanitized()
        val mainKey = wordKey(existing.wordDetails.word)
        val synKeys = existing.synonyms.map { wordKey(it.word) }.toMutableSet()
        val antKeys = existing.antonyms.map { wordKey(it.word) }.toMutableSet()
        val synonyms = existing.synonyms.toMutableList()
        val antonyms = existing.antonyms.toMutableList()
        var skipped = 0
        var enrichedRelations = 0
        var limitReached = false
        val addedSynWords = mutableListOf<String>()
        val addedAntWords = mutableListOf<String>()

        fun absorb(
            source: List<WordDetails>,
            target: MutableList<WordDetails>,
            own: MutableSet<String>,
            other: Set<String>,
            addedWords: MutableList<String>,
        ): Int {
            var added = 0
            for (item in source) {
                val key = wordKey(item.word)
                when {
                    key == mainKey || key in other -> skipped++
                    key in own -> {
                        val idx = target.indexOfFirst { wordKey(it.word) == key }
                        if (idx >= 0) {
                            val current = target[idx]
                            val updated = current.copy(
                                meaningHinglish = current.meaningHinglish.ifBlank { item.meaningHinglish },
                                exampleSentence = current.exampleSentence.ifBlank { item.exampleSentence },
                            )
                            if (updated != current) {
                                target[idx] = updated
                                enrichedRelations++
                            } else {
                                skipped++
                            }
                        } else {
                            skipped++
                        }
                    }
                    target.size >= MAX_RELATIONS -> {
                        skipped++
                        limitReached = true
                    }
                    else -> {
                        target += item
                        own += key
                        addedWords += item.word
                        added++
                    }
                }
            }
            return added
        }

        val addedSyn = absorb(clean.synonyms, synonyms, synKeys, antKeys, addedSynWords)
        val addedAnt = absorb(clean.antonyms, antonyms, antKeys, synKeys, addedAntWords)
        val mainDetails = existing.wordDetails.copy(
            meaningHinglish = existing.wordDetails.meaningHinglish.ifBlank { clean.wordDetails.meaningHinglish },
            exampleSentence = existing.wordDetails.exampleSentence.ifBlank { clean.wordDetails.exampleSentence },
        )
        val mainEnriched = mainDetails != existing.wordDetails
        return MergeResult(
            entry = existing.copy(wordDetails = mainDetails, synonyms = synonyms, antonyms = antonyms),
            addedSynonyms = addedSyn,
            addedAntonyms = addedAnt,
            skipped = skipped,
            limitReached = limitReached,
            changed = addedSyn + addedAnt > 0 || enrichedRelations > 0 || mainEnriched,
            addedSynonymWords = addedSynWords,
            addedAntonymWords = addedAntWords,
            enrichedRelations = enrichedRelations,
            mainDetailsEnriched = mainEnriched,
        )
    }

    /**
     * Case-insensitive search across the primary word, its meanings/example, and all synonyms and antonyms.
     */
    fun matchesSearch(entry: VocabularyEntry, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (entry.wordDetails.matches(q)) return true
        if (entry.synonyms.any { it.matches(q) }) return true
        if (entry.antonyms.any { it.matches(q) }) return true
        return false
    }

    private fun WordDetails.matches(query: String): Boolean =
        word.contains(query, ignoreCase = true) ||
            meaningEnglish.contains(query, ignoreCase = true) ||
            meaningHinglish.contains(query, ignoreCase = true) ||
            exampleSentence.contains(query, ignoreCase = true)
}

/**
 * Pure-Kotlin JSON parser and formatter for bulk and single-word Vocabulary JSON editing.
 *
 * Supports:
 * - A JSON array `[ { ... }, { ... } ]` for adding or importing multiple words at once.
 * - A single JSON object `{ ... }` for adding or editing a single word.
 * - A wrapper object `{ "words": [ ... ] }` or `{ "vocabulary": [ ... ] }`.
 */
object VocabularyJsonFormat {

    sealed interface ParseResult {
        data class Success(val entries: List<VocabularyEntry>) : ParseResult
        data class Error(val message: String) : ParseResult
    }

    fun formatSingle(entry: VocabularyEntry): String =
        formatEntryObject(entry.sanitized(), indent = "")

    fun formatBulkTemplate(): String = """
[
  {
    "word": "",
    "meaningEnglish": "",
    "meaningHinglish": "",
    "exampleSentence": "",
    "synonyms": [
      {
        "word": "",
        "meaningEnglish": "",
        "meaningHinglish": "",
        "exampleSentence": ""
      }
    ],
    "antonyms": [
      {
        "word": "",
        "meaningEnglish": "",
        "meaningHinglish": "",
        "exampleSentence": ""
      }
    ]
  }
]
    """.trimIndent()

    fun parse(jsonText: String, singleMode: Boolean = false): ParseResult {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Error("JSON input cannot be empty.")
        }
        val root = try {
            JsonTokenizer(trimmed).parseValue()
        } catch (e: JsonSyntaxException) {
            return ParseResult.Error(e.message ?: "Invalid JSON syntax.")
        }

        val rawObjects: List<Map<String, Any?>> = when (root) {
            is List<*> -> {
                if (singleMode && root.size > 1) {
                    return ParseResult.Error("Only a single word JSON object is allowed when editing a specific word.")
                }
                if (root.isEmpty()) {
                    return ParseResult.Error("JSON array is empty. Add at least one word object.")
                }
                root.mapIndexed { index, item ->
                    @Suppress("UNCHECKED_CAST")
                    (item as? Map<String, Any?>)
                        ?: return ParseResult.Error("Item #${index + 1} in JSON array must be a JSON object.")
                }
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val map = root as Map<String, Any?>
                val nestedList = (map["words"] as? List<*>) ?: (map["vocabulary"] as? List<*>)
                if (nestedList != null) {
                    if (singleMode && nestedList.size > 1) {
                        return ParseResult.Error(
                            "Only a single word JSON object is allowed when editing a specific word.",
                        )
                    }
                    if (nestedList.isEmpty()) {
                        return ParseResult.Error("The 'words' array is empty. Add at least one word object.")
                    }
                    nestedList.mapIndexed { index, item ->
                        @Suppress("UNCHECKED_CAST")
                        (item as? Map<String, Any?>)
                            ?: return ParseResult.Error("Item #${index + 1} in 'words' must be a JSON object.")
                    }
                } else {
                    listOf(map)
                }
            }
            else -> return ParseResult.Error("JSON must be an object {...} or an array of objects [{...}].")
        }

        val entries = mutableListOf<VocabularyEntry>()
        for ((index, obj) in rawObjects.withIndex()) {
            val prefix = if (rawObjects.size > 1) "Word #${index + 1}: " else ""
            val nestedMain = obj["wordDetails"] as? Map<*, *>
            val mainSource = if (nestedMain != null) {
                @Suppress("UNCHECKED_CAST")
                nestedMain as Map<String, Any?>
            } else {
                obj
            }
            val mainDetails = extractWordDetails(mainSource)
            if (mainDetails.word.isBlank()) {
                return ParseResult.Error("${prefix}Missing or empty \"word\" field.")
            }
            if (mainDetails.meaningEnglish.isBlank()) {
                return ParseResult.Error(
                    "${prefix}Missing or empty \"meaningEnglish\" for word \"${mainDetails.word}\".",
                )
            }

            val synonyms = parseRelationList(
                raw = obj["synonyms"],
                relationName = "synonym",
                wordLabel = mainDetails.word,
            ).getOrElse { err ->
                return ParseResult.Error(prefix + err.message.orEmpty())
            }

            val antonyms = parseRelationList(
                raw = obj["antonyms"],
                relationName = "antonym",
                wordLabel = mainDetails.word,
            ).getOrElse { err ->
                return ParseResult.Error(prefix + err.message.orEmpty())
            }

            entries.add(
                VocabularyEntry(
                    id = (obj["id"] as? String).orEmpty().trim(),
                    wordDetails = mainDetails,
                    synonyms = synonyms,
                    antonyms = antonyms,
                ).sanitized(),
            )
        }

        return ParseResult.Success(entries)
    }

    private fun parseRelationList(
        raw: Any?,
        relationName: String,
        wordLabel: String,
    ): Result<List<WordDetails>> {
        if (raw == null) return Result.success(emptyList())
        val list = raw as? List<*>
            ?: return Result.failure(
                IllegalArgumentException("\"${relationName}s\" for \"$wordLabel\" must be a JSON array."),
            )
        val result = mutableListOf<WordDetails>()
        for ((i, item) in list.withIndex()) {
            @Suppress("UNCHECKED_CAST")
            val map = item as? Map<String, Any?>
                ?: return Result.failure(
                    IllegalArgumentException(
                        "${relationName.replaceFirstChar { it.uppercase() }} #${i + 1} for \"$wordLabel\" must be a JSON object.",
                    ),
                )
            val details = extractWordDetails(map)
            if (details.isBlank) continue
            if (details.word.isBlank()) {
                return Result.failure(
                    IllegalArgumentException(
                        "${relationName.replaceFirstChar { it.uppercase() }} #${i + 1} for \"$wordLabel\" is missing \"word\".",
                    ),
                )
            }
            if (details.meaningEnglish.isBlank()) {
                return Result.failure(
                    IllegalArgumentException(
                        "${relationName.replaceFirstChar { it.uppercase() }} \"${details.word}\" for \"$wordLabel\" is missing \"meaningEnglish\".",
                    ),
                )
            }
            result.add(details)
        }
        return Result.success(result)
    }

    private fun extractWordDetails(map: Map<String, Any?>): WordDetails {
        val word = stringField(map, "word", "Word", "term")
        val meaningEnglish = stringField(
            map,
            "meaningEnglish",
            "meaning_english",
            "englishMeaning",
            "meaningInEnglish",
            "meaning",
        )
        val meaningHinglish = stringField(
            map,
            "meaningHinglish",
            "meaning_hinglish",
            "hinglishMeaning",
            "meaningInHinglish",
            "hinglish",
        )
        val exampleSentence = stringField(
            map,
            "exampleSentence",
            "example_sentence",
            "example",
            "sentence",
        )
        return WordDetails(
            word = word,
            meaningEnglish = meaningEnglish,
            meaningHinglish = meaningHinglish,
            exampleSentence = exampleSentence,
        ).trimmed()
    }

    private fun stringField(map: Map<String, Any?>, vararg keys: String): String {
        for (key in keys) {
            val direct = map[key]
            if (direct is String && direct.isNotBlank()) return direct.trim()
        }
        val lowerMap = map.entries.associate { it.key.lowercase() to it.value }
        for (key in keys) {
            val matched = lowerMap[key.lowercase()]
            if (matched is String && matched.isNotBlank()) return matched.trim()
        }
        return ""
    }

    private fun formatEntryObject(entry: VocabularyEntry, indent: String): String {
        val nextIndent = "$indent  "
        val sb = StringBuilder()
        sb.append(indent).append("{\n")
        sb.append(nextIndent).append("\"word\": ").append(escapeJson(entry.wordDetails.word)).append(",\n")
        sb.append(nextIndent).append("\"meaningEnglish\": ").append(escapeJson(entry.wordDetails.meaningEnglish)).append(",\n")
        sb.append(nextIndent).append("\"meaningHinglish\": ").append(escapeJson(entry.wordDetails.meaningHinglish)).append(",\n")
        sb.append(nextIndent).append("\"exampleSentence\": ").append(escapeJson(entry.wordDetails.exampleSentence)).append(",\n")
        sb.append(nextIndent).append("\"synonyms\": ").append(formatDetailsList(entry.synonyms, nextIndent)).append(",\n")
        sb.append(nextIndent).append("\"antonyms\": ").append(formatDetailsList(entry.antonyms, nextIndent)).append("\n")
        sb.append(indent).append("}")
        return sb.toString()
    }

    private fun formatDetailsList(items: List<WordDetails>, indent: String): String {
        if (items.isEmpty()) return "[]"
        val itemIndent = "$indent  "
        val fieldIndent = "$itemIndent  "
        val sb = StringBuilder("[\n")
        items.forEachIndexed { index, d ->
            sb.append(itemIndent).append("{\n")
            sb.append(fieldIndent).append("\"word\": ").append(escapeJson(d.word)).append(",\n")
            sb.append(fieldIndent).append("\"meaningEnglish\": ").append(escapeJson(d.meaningEnglish)).append(",\n")
            sb.append(fieldIndent).append("\"meaningHinglish\": ").append(escapeJson(d.meaningHinglish)).append(",\n")
            sb.append(fieldIndent).append("\"exampleSentence\": ").append(escapeJson(d.exampleSentence)).append("\n")
            sb.append(itemIndent).append("}")
            if (index < items.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append(indent).append("]")
        return sb.toString()
    }

    /** Parses any JSON text into Map / List / String / Number / Boolean / null, or returns null if invalid. */
    internal fun parseTree(text: String): Any? = try {
        JsonTokenizer(text.trim()).parseValue()
    } catch (e: JsonSyntaxException) {
        null
    }

    internal fun escapeJson(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch < ' ') {
                        append(String.format("\\u%04x", ch.code))
                    } else {
                        append(ch)
                    }
                }
            }
        }
        append('"')
    }

    private class JsonSyntaxException(message: String) : Exception(message)

    private class JsonTokenizer(private val src: String) {
        private var pos = 0

        fun parseValue(): Any? {
            skipWhitespace()
            val v = readValue()
            skipWhitespace()
            if (pos < src.length) {
                throw JsonSyntaxException("Unexpected trailing character '${src[pos]}' at position $pos.")
            }
            return v
        }

        private fun readValue(): Any? {
            skipWhitespace()
            if (pos >= src.length) throw JsonSyntaxException("Unexpected end of JSON.")
            return when (val ch = src[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readBoolean()
                'n' -> readNull()
                '-', in '0'..'9' -> readNumber()
                else -> throw JsonSyntaxException("Unexpected character '$ch' at position $pos.")
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            skipWhitespace()
            val map = LinkedHashMap<String, Any?>()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                if (peek() != '"') {
                    throw JsonSyntaxException("Expected quoted property name at position $pos.")
                }
                val key = readString()
                skipWhitespace()
                expect(':')
                val value = readValue()
                map[key] = value
                skipWhitespace()
                when (val next = peek()) {
                    ',' -> {
                        pos++
                        skipWhitespace()
                        if (peek() == '}') {
                            pos++
                            return map
                        }
                    }
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw JsonSyntaxException(
                        "Expected ',' or '}' in object at position $pos, found '${next ?: "EOF"}'.",
                    )
                }
            }
        }

        private fun readArray(): List<Any?> {
            expect('[')
            skipWhitespace()
            val list = ArrayList<Any?>()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(readValue())
                skipWhitespace()
                when (val next = peek()) {
                    ',' -> {
                        pos++
                        skipWhitespace()
                        if (peek() == ']') {
                            pos++
                            return list
                        }
                    }
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw JsonSyntaxException(
                        "Expected ',' or ']' in array at position $pos, found '${next ?: "EOF"}'.",
                    )
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < src.length) {
                when (val ch = src[pos++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) throw JsonSyntaxException("Unterminated escape sequence.")
                        when (val esc = src[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) {
                                    throw JsonSyntaxException("Invalid unicode escape at position $pos.")
                                }
                                val hex = src.substring(pos, pos + 4)
                                val code = hex.toIntOrNull(16)
                                    ?: throw JsonSyntaxException("Invalid unicode hex '$hex' at position $pos.")
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> throw JsonSyntaxException("Invalid escape character '\\$esc' at position $pos.")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            throw JsonSyntaxException("Unterminated string literal in JSON.")
        }

        private fun readBoolean(): Boolean {
            if (src.startsWith("true", pos)) {
                pos += 4
                return true
            }
            if (src.startsWith("false", pos)) {
                pos += 5
                return false
            }
            throw JsonSyntaxException("Invalid boolean token at position $pos.")
        }

        private fun readNull(): Any? {
            if (src.startsWith("null", pos)) {
                pos += 4
                return null
            }
            throw JsonSyntaxException("Invalid null token at position $pos.")
        }

        private fun readNumber(): Number {
            val start = pos
            if (peek() == '-') pos++
            while (pos < src.length && src[pos] in '0'..'9') pos++
            if (pos < src.length && src[pos] == '.') {
                pos++
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            if (pos < src.length && (src[pos] == 'e' || src[pos] == 'E')) {
                pos++
                if (pos < src.length && (src[pos] == '+' || src[pos] == '-')) pos++
                while (pos < src.length && src[pos] in '0'..'9') pos++
            }
            val token = src.substring(start, pos)
            return token.toLongOrNull()
                ?: token.toDoubleOrNull()
                ?: throw JsonSyntaxException("Invalid number '$token' at position $start.")
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) {
                pos++
            }
        }

        private fun peek(): Char? = if (pos < src.length) src[pos] else null

        private fun expect(expected: Char) {
            if (pos >= src.length || src[pos] != expected) {
                throw JsonSyntaxException("Expected '$expected' at position $pos.")
            }
            pos++
        }
    }
}
