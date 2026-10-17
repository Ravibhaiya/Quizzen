package com.ravibhaiya.quizzen.domain

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

    private fun escapeJson(value: String): String = buildString {
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
