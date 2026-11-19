package com.ravibhaiya.quizzen.domain

import java.text.Normalizer

/**
 * The details stored and displayed for a primary vocabulary word, a synonym, or an antonym.
 */
data class WordDetails(
    val word: String = "",
    val partOfSpeech: String = "",
    val meaningEnglish: String = "",
    val meaningHinglish: String = "",
    val exampleSentence: String = "",
) {
    fun trimmed(): WordDetails = WordDetails(
        word = word.trim(),
        partOfSpeech = partOfSpeech.trim(),
        meaningEnglish = meaningEnglish.trim(),
        meaningHinglish = meaningHinglish.trim(),
        exampleSentence = exampleSentence.trim(),
    )

    val isBlank: Boolean
        get() = word.isBlank() &&
            partOfSpeech.isBlank() &&
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
     * Collapses entries that share the same word and part of speech ([wordIdentityKey], case-insensitive) into one:
     * the last occurrence wins for main word details (inheriting blank part of speech/Hinglish/example, missing id,
     * and earlier creation time from the first), while synonyms and antonyms from all occurrences are combined
     * without duplicates. Duplicate synonyms and antonyms inside an entry (same [wordIdentityKey]) are also collapsed,
     * keeping the first. Entries or relations with the same word but different parts of speech stay separate.
     */
    fun mergeDuplicateWords(entries: List<VocabularyEntry>): List<VocabularyEntry> {
        val byWord = LinkedHashMap<String, VocabularyEntry>()
        for (raw in entries) {
            val key = wordIdentityKey(raw.wordDetails)
            val cleanSynonyms = raw.synonyms
                .distinctBy { wordIdentityKey(it) }
                .filterNot { wordIdentityKey(it) == key }
                .take(MAX_RELATIONS)
            val synKeys = cleanSynonyms.map { wordIdentityKey(it) }.toSet()
            val cleanAntonyms = raw.antonyms
                .distinctBy { wordIdentityKey(it) }
                .filterNot { wordIdentityKey(it) == key || wordIdentityKey(it) in synKeys }
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
                    partOfSpeech = entry.wordDetails.partOfSpeech.ifBlank { previous.wordDetails.partOfSpeech },
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

    /** Maximum length for a word field (`wordDetails.word`, synonym/antonym `word`); enforced by `firestore.rules`. */
    const val MAX_WORD_LENGTH = 120

    /** Maximum length for an optional part of speech label; enforced by `firestore.rules`. */
    const val MAX_PART_OF_SPEECH_LENGTH = 60

    /** Maximum length for an English or Hinglish meaning; enforced by `firestore.rules`. */
    const val MAX_MEANING_LENGTH = 500

    /** Maximum length for an example sentence; enforced by `firestore.rules`. */
    const val MAX_EXAMPLE_LENGTH = 600

    private fun firestoreStringSize(value: String): Int =
        maxOf(value.length, value.toByteArray(Charsets.UTF_8).size)

    /**
     * Checks that the main word and all synonyms/antonyms stay within the character/UTF-8 byte limits
     * enforced by `firestore.rules` ([MAX_WORD_LENGTH], [MAX_PART_OF_SPEECH_LENGTH], [MAX_MEANING_LENGTH],
     * [MAX_EXAMPLE_LENGTH]).
     * Returns a clear human-readable error message, or `null` if all fields are within limits.
     */
    fun validateFieldLengths(entry: VocabularyEntry): String? {
        val clean = entry.sanitized()
        val mainLabel = clean.wordDetails.word.ifBlank { "Main word" }
        validateDetailsLengths(clean.wordDetails, "Word \"$mainLabel\"")?.let { return it }
        for ((idx, syn) in clean.synonyms.withIndex()) {
            val synLabel = syn.word.ifBlank { "#${idx + 1}" }
            validateDetailsLengths(syn, "Synonym \"$synLabel\" in \"$mainLabel\"")?.let { return it }
        }
        for ((idx, ant) in clean.antonyms.withIndex()) {
            val antLabel = ant.word.ifBlank { "#${idx + 1}" }
            validateDetailsLengths(ant, "Antonym \"$antLabel\" in \"$mainLabel\"")?.let { return it }
        }
        return null
    }

    private fun validateDetailsLengths(details: WordDetails, subjectLabel: String): String? {
        val wordLen = firestoreStringSize(details.word)
        if (wordLen > MAX_WORD_LENGTH) {
            return "$subjectLabel is too long ($wordLen/$MAX_WORD_LENGTH characters)."
        }
        val posLen = firestoreStringSize(details.partOfSpeech)
        if (posLen > MAX_PART_OF_SPEECH_LENGTH) {
            return "Part of speech for ${subjectLabel.replaceFirstChar { it.lowercase() }} is too long ($posLen/$MAX_PART_OF_SPEECH_LENGTH characters)."
        }
        val engLen = firestoreStringSize(details.meaningEnglish)
        if (engLen > MAX_MEANING_LENGTH) {
            return "English meaning for ${subjectLabel.replaceFirstChar { it.lowercase() }} is too long ($engLen/$MAX_MEANING_LENGTH characters)."
        }
        val hinLen = firestoreStringSize(details.meaningHinglish)
        if (hinLen > MAX_MEANING_LENGTH) {
            return "Hinglish meaning for ${subjectLabel.replaceFirstChar { it.lowercase() }} is too long ($hinLen/$MAX_MEANING_LENGTH characters)."
        }
        val exLen = firestoreStringSize(details.exampleSentence)
        if (exLen > MAX_EXAMPLE_LENGTH) {
            return "Example sentence for ${subjectLabel.replaceFirstChar { it.lowercase() }} is too long ($exLen/$MAX_EXAMPLE_LENGTH characters)."
        }
        return null
    }

    /**
     * Result of [prepareEntriesForSave]: [entries] have assigned document IDs and timestamps, and
     * [createOnlyIds] contains the IDs of entries that had no ID before saving and must be created fresh.
     */
    data class PreparedSaveBatch(
        val entries: List<VocabularyEntry>,
        val createOnlyIds: Set<String>,
    )

    /**
     * Prepares [entries] for saving against [existing] database entries:
     * - Only entries with a blank `id` (`entry.id.isBlank()`) are marked in [PreparedSaveBatch.createOnlyIds].
     * - Editing an existing word (`entry.id.isNotBlank()`) keeps its document ID and is not create-only.
     * - If a new entry (`entry.id.isBlank()`) has a default [documentIdForWord] that is already used by an
     *   entry in [existing] (for example, because an older entry was originally created under that spelling
     *   and later renamed to a corrected spelling), a disambiguated ID (`${baseId}_2`, `${baseId}_3`, ...)
     *   is assigned so re-adding the old spelling never overwrites the renamed word.
     */
    fun prepareEntriesForSave(
        entries: List<VocabularyEntry>,
        existing: List<VocabularyEntry>,
        now: Long,
    ): PreparedSaveBatch {
        val existingById = existing.filter { it.id.isNotBlank() }.associateBy { it.id }
        val allocatedNewIds = HashSet<String>()
        val createOnlyIds = LinkedHashSet<String>()
        val prepared = entries.mapIndexed { index, raw ->
            val clean = raw.sanitized()
            val isNew = clean.id.isBlank()
            val assignedId = if (!isNew) {
                clean.id
            } else {
                val key = wordIdentityKey(clean.wordDetails)
                val baseId = documentIdForWord(clean.wordDetails.word)
                var candidate = baseId
                var suffix = 2
                while (
                    candidate in allocatedNewIds ||
                    (existingById[candidate] != null && wordIdentityKey(existingById.getValue(candidate).wordDetails) != key)
                ) {
                    candidate = "${baseId}_$suffix"
                    suffix++
                }
                allocatedNewIds += candidate
                createOnlyIds += candidate
                candidate
            }
            val created = if (clean.createdAt > 0L) clean.createdAt else (now + index)
            clean.copy(
                id = assignedId,
                createdAt = created,
                updatedAt = now + index,
            )
        }
        return PreparedSaveBatch(entries = prepared, createOnlyIds = createOnlyIds)
    }

    /**
     * Normalized surface form of a word or part of speech: capitalization, extra or non-breaking spaces,
     * hyphens versus spaces, surrounding punctuation, and curly versus straight apostrophes are ignored.
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
     * Uniqueness key combining [wordKey] and normalized [partOfSpeech] so two entries or relations with the
     * same word but different parts of speech (e.g. `"Run"` as `"Noun"` vs `"Run"` as `"Verb"`) are not
     * treated as duplicates.
     */
    fun wordIdentityKey(details: WordDetails): String =
        wordIdentityKey(details.word, details.partOfSpeech)

    fun wordIdentityKey(word: String, partOfSpeech: String = ""): String {
        val wKey = wordKey(word)
        if (wKey.isEmpty()) return ""
        val posKey = wordKey(partOfSpeech)
        return if (posKey.isEmpty()) wKey else "$wKey#$posKey"
    }

    /**
     * Returns true when [a] and [b] have different parts of speech and should not be treated as duplicates:
     * - For the same normalized word (`wordKey(a.word) == wordKey(b.word)`), any difference in normalized
     *   `partOfSpeech` makes them distinct entries.
     * - For different surface words, two entries have different parts of speech when both specify a non-blank
     *   `partOfSpeech` and their normalized `partOfSpeech` values differ.
     */
    fun hasDifferentPartOfSpeech(a: WordDetails, b: WordDetails): Boolean {
        val posA = wordKey(a.partOfSpeech)
        val posB = wordKey(b.partOfSpeech)
        if (posA == posB) return false
        return if (wordKey(a.word) == wordKey(b.word)) {
            true
        } else {
            posA.isNotEmpty() && posB.isNotEmpty()
        }
    }

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
     * Returns the display names of any main words that appear more than once in [entries] with the same
     * [wordIdentityKey] (entries with the same word but different `partOfSpeech` are allowed).
     */
    fun findDuplicateMainWords(entries: List<VocabularyEntry>): List<String> {
        val seen = LinkedHashMap<String, String>()
        val duplicates = LinkedHashSet<String>()
        for (entry in entries) {
            val rawWord = entry.wordDetails.word.trim()
            val key = wordIdentityKey(entry.wordDetails)
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
     * Checks a single entry for field length limits or internal synonym/antonym duplicates or conflicts
     * so nothing is silently dropped or rejected with a cryptic permission error. Returns a human-readable
     * error message, or `null` if clean.
     */
    fun findEntryRelationConflict(entry: VocabularyEntry): String? {
        validateFieldLengths(entry)?.let { return it }
        val label = entry.wordDetails.word.trim()
        val mainKey = wordIdentityKey(entry.wordDetails)
        if (entry.synonyms.size > MAX_RELATIONS) {
            return "Word \"$label\" has ${entry.synonyms.size} synonyms (maximum is $MAX_RELATIONS)."
        }
        if (entry.antonyms.size > MAX_RELATIONS) {
            return "Word \"$label\" has ${entry.antonyms.size} antonyms (maximum is $MAX_RELATIONS)."
        }
        val synKeys = HashSet<String>()
        for (syn in entry.synonyms) {
            val synWord = syn.word.trim()
            val key = wordIdentityKey(syn)
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
            val key = wordIdentityKey(ant)
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
     * Pinpoints the exact synonym or antonym item inside [entry] that causes a relation conflict or
     * semantic meaning conflict so the editor can open focused on just that single word.
     */
    data class ConflictingRelationTarget(
        val isAntonym: Boolean,
        val index: Int,
        val details: WordDetails,
        val conflictingWordLabel: String,
        val reason: String,
    ) {
        val word: String get() = details.word.trim()
    }

    /**
     * Returns the exact synonym or antonym target where a deterministic relation conflict occurs in [entry],
     * or `null` if no relation word is in conflict.
     */
    fun findEntryRelationConflictTarget(entry: VocabularyEntry): ConflictingRelationTarget? {
        val label = entry.wordDetails.word.trim()
        val mainKey = wordIdentityKey(entry.wordDetails)
        val synKeys = HashSet<String>()
        for ((index, syn) in entry.synonyms.withIndex()) {
            val synWord = syn.word.trim()
            val key = wordIdentityKey(syn)
            if (key == mainKey) {
                return ConflictingRelationTarget(
                    isAntonym = false,
                    index = index,
                    details = syn,
                    conflictingWordLabel = "Synonym \"$synWord\"",
                    reason = "Word \"$label\" cannot list itself (\"$synWord\") as a synonym.",
                )
            }
            if (!synKeys.add(key)) {
                return ConflictingRelationTarget(
                    isAntonym = false,
                    index = index,
                    details = syn,
                    conflictingWordLabel = "Synonym \"$synWord\"",
                    reason = "Word \"$label\" has a duplicate synonym \"$synWord\".",
                )
            }
            if (!syn.trimmed().isValid) {
                return ConflictingRelationTarget(
                    isAntonym = false,
                    index = index,
                    details = syn,
                    conflictingWordLabel = "Synonym \"$synWord\"",
                    reason = "Synonym \"$synWord\" in \"$label\" is missing a required English meaning.",
                )
            }
        }
        val antKeys = HashSet<String>()
        for ((index, ant) in entry.antonyms.withIndex()) {
            val antWord = ant.word.trim()
            val key = wordIdentityKey(ant)
            if (key == mainKey) {
                return ConflictingRelationTarget(
                    isAntonym = true,
                    index = index,
                    details = ant,
                    conflictingWordLabel = "Antonym \"$antWord\"",
                    reason = "Word \"$label\" cannot list itself (\"$antWord\") as an antonym.",
                )
            }
            if (key in synKeys) {
                return ConflictingRelationTarget(
                    isAntonym = true,
                    index = index,
                    details = ant,
                    conflictingWordLabel = "Synonym & Antonym \"$antWord\"",
                    reason = "Word \"$label\" lists \"$antWord\" as both a synonym and an antonym.",
                )
            }
            if (!antKeys.add(key)) {
                return ConflictingRelationTarget(
                    isAntonym = true,
                    index = index,
                    details = ant,
                    conflictingWordLabel = "Antonym \"$antWord\"",
                    reason = "Word \"$label\" has a duplicate antonym \"$antWord\".",
                )
            }
            if (!ant.trimmed().isValid) {
                return ConflictingRelationTarget(
                    isAntonym = true,
                    index = index,
                    details = ant,
                    conflictingWordLabel = "Antonym \"$antWord\"",
                    reason = "Antonym \"$antWord\" in \"$label\" is missing a required English meaning.",
                )
            }
        }
        return null
    }

    /**
     * Result of [checkMainWords]: [toSave] are new words (or unchanged/edited ones), [clashing] are incoming entries
     * whose main word and part of speech already belong to another saved entry, [mergedRepeats] counts repeats
     * collapsed inside the batch.
     */
    data class WordCheck(
        val toSave: List<VocabularyEntry>,
        val clashing: List<VocabularyEntry>,
        val mergedRepeats: Int,
    ) {
        val skippedExisting: List<String> get() = clashing.map { it.wordDetails.word.trim() }
    }

    /**
     * Keeps one entry per main word and part of speech ([wordIdentityKey]). Repeats inside [incoming] are collapsed
     * (the last one wins). An incoming entry clashes when its [wordIdentityKey] belongs to a saved entry, unless it is
     * that same entry (same id, same identity key), so editing a word still works even if older duplicates exist.
     * Entries with the same word but different parts of speech do not clash and are saved separately.
     */
    fun checkMainWords(incoming: List<VocabularyEntry>, existing: List<VocabularyEntry>): WordCheck {
        val merged = mergeDuplicateWords(incoming)
        val keyById = existing.filter { it.id.isNotBlank() }.associate { it.id to wordIdentityKey(it.wordDetails) }
        val taken = existing.map { wordIdentityKey(it.wordDetails) }.toSet()
        val (clashing, fresh) = merged.partition { entry ->
            val key = wordIdentityKey(entry.wordDetails)
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
     * never overwritten (a blank part of speech, Hinglish meaning, or example on the main word or on an existing
     * synonym/antonym is filled in). An item is skipped when it equals the main word, is already in either list
     * with the same [wordIdentityKey] (and no blank fields to fill), or would push a list past [MAX_RELATIONS].
     */
    fun mergeRelations(existing: VocabularyEntry, incoming: VocabularyEntry): MergeResult {
        val clean = incoming.sanitized()
        val mainWordKey = wordKey(existing.wordDetails.word)
        val mainPosKey = wordKey(existing.wordDetails.partOfSpeech)
        val synonyms = existing.synonyms.toMutableList()
        val antonyms = existing.antonyms.toMutableList()
        var skipped = 0
        var enrichedRelations = 0
        var limitReached = false
        val addedSynWords = mutableListOf<String>()
        val addedAntWords = mutableListOf<String>()

        fun sameRelationSlot(a: WordDetails, b: WordDetails): Boolean {
            if (wordKey(a.word) != wordKey(b.word)) return false
            val posA = wordKey(a.partOfSpeech)
            val posB = wordKey(b.partOfSpeech)
            return posA.isEmpty() || posB.isEmpty() || posA == posB
        }

        fun matchesMainWord(item: WordDetails): Boolean {
            if (wordKey(item.word) != mainWordKey) return false
            val itemPosKey = wordKey(item.partOfSpeech)
            return mainPosKey.isEmpty() || itemPosKey.isEmpty() || mainPosKey == itemPosKey
        }

        fun absorb(
            source: List<WordDetails>,
            target: MutableList<WordDetails>,
            other: List<WordDetails>,
            addedWords: MutableList<String>,
        ): Int {
            var added = 0
            for (item in source) {
                val idxInOwn = target.indexOfFirst { sameRelationSlot(it, item) }
                val inOther = other.any { sameRelationSlot(it, item) }
                when {
                    matchesMainWord(item) || inOther -> skipped++
                    idxInOwn >= 0 -> {
                        val current = target[idxInOwn]
                        val updated = current.copy(
                            partOfSpeech = current.partOfSpeech.ifBlank { item.partOfSpeech },
                            meaningHinglish = current.meaningHinglish.ifBlank { item.meaningHinglish },
                            exampleSentence = current.exampleSentence.ifBlank { item.exampleSentence },
                        )
                        if (updated != current) {
                            target[idxInOwn] = updated
                            enrichedRelations++
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
                        addedWords += item.word
                        added++
                    }
                }
            }
            return added
        }

        val addedSyn = absorb(clean.synonyms, synonyms, antonyms, addedSynWords)
        val addedAnt = absorb(clean.antonyms, antonyms, synonyms, addedAntWords)
        val mainDetails = existing.wordDetails.copy(
            partOfSpeech = existing.wordDetails.partOfSpeech.ifBlank { clean.wordDetails.partOfSpeech },
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
     * Case-insensitive search across the primary word, its part of speech/meanings/example, and all synonyms and antonyms.
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
            partOfSpeech.contains(query, ignoreCase = true) ||
            meaningEnglish.contains(query, ignoreCase = true) ||
            meaningHinglish.contains(query, ignoreCase = true) ||
            exampleSentence.contains(query, ignoreCase = true)
}
