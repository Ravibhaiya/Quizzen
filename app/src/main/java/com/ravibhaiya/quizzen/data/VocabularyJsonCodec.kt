package com.ravibhaiya.quizzen.data

import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.WordDetails

/**
 * Versioned JSON codec for the local offline cache (`{"version":2,"entries":[...]}`). Pure Kotlin so it runs in JVM
 * unit tests without Android's stubbed `org.json`. Still reads the old v1 pipe/base64 lines so an existing cache file
 * is upgraded on the next save instead of lost.
 */
object VocabularyJsonCodec {
    private const val CACHE_VERSION = 2

    fun encodeList(entries: List<VocabularyEntry>): String = buildString {
        append("{\"version\":").append(CACHE_VERSION).append(",\"entries\":[")
        entries.forEachIndexed { index, entry ->
            if (index > 0) append(',')
            appendEntry(entry.sanitized())
        }
        append("]}")
    }

    fun decodeList(raw: String): List<VocabularyEntry> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith("{")) return decodeLegacyPipeList(trimmed)
        return runCatching { JsonReader(trimmed).readRoot() }.getOrDefault(emptyList())
    }

    private fun StringBuilder.appendEntry(entry: VocabularyEntry) {
        append('{')
        append("\"id\":"); appendJsonString(entry.id)
        append(",\"createdAt\":").append(entry.createdAt)
        append(",\"updatedAt\":").append(entry.updatedAt)
        append(",\"wordDetails\":"); appendDetails(entry.wordDetails)
        append(",\"synonyms\":[")
        entry.synonyms.forEachIndexed { i, d -> if (i > 0) append(','); appendDetails(d) }
        append("],\"antonyms\":[")
        entry.antonyms.forEachIndexed { i, d -> if (i > 0) append(','); appendDetails(d) }
        append("]}")
    }

    private fun StringBuilder.appendDetails(details: WordDetails) {
        append('{')
        append("\"word\":"); appendJsonString(details.word)
        append(",\"meaningEnglish\":"); appendJsonString(details.meaningEnglish)
        append(",\"meaningHinglish\":"); appendJsonString(details.meaningHinglish)
        append(",\"exampleSentence\":"); appendJsonString(details.exampleSentence)
        append('}')
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    append(ch)
                }
            }
        }
        append('"')
    }

    private class JsonReader(private val text: String) {
        private var pos = 0

        fun readRoot(): List<VocabularyEntry> {
            val root = readObject()
            val entries = root["entries"] as? List<*> ?: return emptyList()
            return entries.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                val stringMap = map as Map<String, Any?>
                val id = (stringMap["id"] as? String).orEmpty()
                VocabularyFirestoreMapper.docToEntry(id, stringMap)
            }
        }

        private fun readValue(): Any? {
            skipWhitespace()
            return when (text.getOrNull(pos)) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readBoolean()
                'n' -> { pos += 4; null }
                else -> readNumber()
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek('}')) { pos++; return map }
            while (pos < text.length) {
                skipWhitespace()
                val key = readString()
                skipWhitespace(); expect(':')
                map[key] = readValue()
                skipWhitespace()
                if (peek(',')) { pos++; continue }
                expect('}'); break
            }
            return map
        }

        private fun readArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek(']')) { pos++; return list }
            while (pos < text.length) {
                list += readValue()
                skipWhitespace()
                if (peek(',')) { pos++; continue }
                expect(']'); break
            }
            return list
        }

        private fun readString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < text.length) {
                when (val ch = text[pos++]) {
                    '"' -> return sb.toString()
                    '\\' -> when (val esc = text[pos++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            sb.append(text.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            error("Unterminated string")
        }

        private fun readNumber(): Long {
            val start = pos
            while (pos < text.length && (text[pos].isDigit() || text[pos] == '-')) pos++
            return text.substring(start, pos).toLongOrNull() ?: 0L
        }

        private fun readBoolean(): Boolean = if (text.startsWith("true", pos)) {
            pos += 4; true
        } else {
            pos += 5; false
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun peek(ch: Char): Boolean = pos < text.length && text[pos] == ch

        private fun expect(ch: Char) {
            skipWhitespace()
            check(pos < text.length && text[pos] == ch) { "Expected '$ch' at $pos" }
            pos++
        }
    }

    // ---- Legacy v1 pipe/base64 reader (kept so existing cache files still load) ----

    private fun decodeLegacyPipeList(raw: String): List<VocabularyEntry> =
        raw.lineSequence().filter { it.isNotBlank() }.mapNotNull(::decodeLegacyEntry).toList()

    private fun decodeLegacyEntry(line: String): VocabularyEntry? {
        val parts = line.split('|')
        if (parts.size < 6) return null
        val id = unb64(parts[0])
        val createdAt = parts[1].toLongOrNull() ?: 0L
        val updatedAt = parts[2].toLongOrNull() ?: createdAt
        val wordDetails = decodeLegacyDetails(parts[3]) ?: return null
        if (wordDetails.word.isBlank()) return null
        val synonyms = parts[4].split(';').filter { it.isNotBlank() }.mapNotNull(::decodeLegacyDetails)
        val antonyms = parts[5].split(';').filter { it.isNotBlank() }.mapNotNull(::decodeLegacyDetails)
        return VocabularyEntry(id, wordDetails, synonyms, antonyms, createdAt, updatedAt).sanitized()
    }

    private fun decodeLegacyDetails(token: String): WordDetails? {
        val fields = token.split(':')
        if (fields.size < 4) return null
        return WordDetails(unb64(fields[0]), unb64(fields[1]), unb64(fields[2]), unb64(fields[3]))
    }

    private fun unb64(encoded: String): String = runCatching {
        String(java.util.Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
    }.getOrDefault("")
}
