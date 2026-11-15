package com.ravibhaiya.quizzen.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Duplicate or variant classification returned by a `choice` question on `gemini-decision-1`.
 */
enum class DecisionDuplicateKind(val apiCode: String) {
    EXACT_DUPLICATE("exact_duplicate"),
    ROOT_OR_INFLECTED("root_or_inflected"),
    SPELLING_OR_TYPO("spelling_or_typo"),
    ;

    companion object {
        fun fromApiCode(code: String?): DecisionDuplicateKind? =
            entries.firstOrNull { it.apiCode == code }
    }
}

/**
 * A high-confidence duplicate, word-form/spelling variant, or synonym/antonym meaning conflict
 * flagged by `gemini-decision-1` for manual user review in `DatabaseNotificationsSheet`.
 */
sealed interface VocabularyDecisionFinding {
    val key: String
    val probability: Double
    val confidence: Double

    val confidencePercent: Int
        get() = (confidence.coerceIn(0.0, 1.0) * 100.0).roundToInt()

    /**
     * [duplicateId] is classified by `gemini-decision-1` as an exact duplicate, root/inflected form,
     * or spelling variant/typo of [primaryId].
     */
    data class DuplicateOrVariant(
        val primaryId: String,
        val duplicateId: String,
        val kind: DecisionDuplicateKind,
        override val probability: Double,
        override val confidence: Double,
    ) : VocabularyDecisionFinding {
        override val key: String get() = pairDismissKey(primaryId, duplicateId)
    }

    /**
     * [entryId] is flagged by a `noul` (yes/no) question on `gemini-decision-1` as having a synonym/antonym
     * meaning conflict or contradictory definition on [conflictingTarget].
     */
    data class MeaningConflict(
        val entryId: String,
        override val probability: Double,
        override val confidence: Double,
        val conflictingTarget: VocabularyRules.ConflictingRelationTarget? = null,
        val customConflictLabel: String? = null,
        val customReason: String? = null,
    ) : VocabularyDecisionFinding {
        val conflictingWord: String? get() = customConflictLabel ?: conflictingTarget?.conflictingWordLabel
        val conflictReason: String? get() = customReason ?: conflictingTarget?.reason
        override val key: String get() = conflictDismissKey(entryId)
    }

    companion object {
        fun pairDismissKey(primaryId: String, duplicateId: String): String =
            "decision_pair_${primaryId}_$duplicateId"

        fun conflictDismissKey(entryId: String): String =
            "decision_conflict_$entryId"
    }
}

data class DecisionPairQuestion(
    val questionId: String,
    val primaryId: String,
    val duplicateId: String,
    val primaryWord: String,
    val duplicateWord: String,
    val primaryMeaning: String = "",
    val duplicateMeaning: String = "",
)

data class DecisionConflictQuestion(
    val questionId: String,
    val entryId: String,
    val word: String,
    val mainMeaning: String = "",
    val isMainWordDefinition: Boolean = false,
    val isAntonym: Boolean = true,
    val relationIndex: Int = 0,
    val relationDetails: WordDetails = WordDetails(),
)

data class DecisionRequestPlan(
    val requestJson: String,
    val pairQuestions: List<DecisionPairQuestion>,
    val conflictQuestions: List<DecisionConflictQuestion>,
    val coveredEntryIds: Set<String> = emptySet(),
) {
    val totalQuestions: Int get() = pairQuestions.size + conflictQuestions.size
}

/**
 * Pure-Kotlin request builder and response parser for the Google Gemini Decisions API (`gemini-decision-1`).
 *
 * Implements **Option B** one-time smart verification:
 * - Only new or edited (unverified) words are checked for main-word meaning, synonym, and antonym conflicts,
 *   ordered with most recently updated words first.
 * - Each unverified word is compared against **all** database words (`unverified × all`) for duplicate/variant
 *   pair selection, with dedicated pair slots reserved in every batch so conflict checks never starve pair checks.
 * - Requests are split into batches of at most [BATCH_MAX_QUESTIONS] (`100`) questions per call (`<= 250` hard API cap).
 */
object VocabularyDecisionRules {

    const val MODEL_ID = "gemini-decision-1"
    const val MIN_PROBABILITY = 0.75
    const val BATCH_MAX_QUESTIONS = 100
    const val MAX_QUESTIONS_PER_CALL = 250
    const val MIN_PAIR_SLOTS_PER_BATCH = 30
    private const val ALL_PAIRS_THRESHOLD = 100

    private const val CHOICE_DISTINCT = "distinct"

    /**
     * Deterministic content fingerprint for [entry] so unchanged words are never re-verified across sessions
     * or when new words are added, while any edit to the word, meanings, synonyms, or antonyms invalidates
     * its fingerprint and schedules a fresh verification.
     */
    fun entryContentDigest(entry: VocabularyEntry): Int {
        val clean = entry.sanitized()
        var result = clean.wordDetails.hashCode()
        result = 31 * result + clean.synonyms.hashCode()
        result = 31 * result + clean.antonyms.hashCode()
        return result
    }

    /**
     * Orders entries so canonical deterministic IDs come first, followed by older entries, so the first
     * entry in any pair is always the primary entry to keep.
     */
    fun sortEntriesForPrimarySelection(entries: List<VocabularyEntry>): List<VocabularyEntry> =
        entries.sortedWith(
            compareBy<VocabularyEntry> { entry ->
                val word = entry.wordDetails.word
                when {
                    word.isNotBlank() && entry.id == VocabularyRules.documentIdForWord(word) -> 0
                    entry.id.startsWith("seed_") -> 1
                    else -> 2
                }
            }.thenBy { if (it.createdAt > 0L) it.createdAt else Long.MAX_VALUE }
                .thenBy { it.id },
        )

    /**
     * Orders entries with the most recently updated or created words first so newly added or edited words
     * are prioritized first when building verification batches.
     */
    fun sortEntriesByRecentActivity(entries: List<VocabularyEntry>): List<VocabularyEntry> =
        entries.sortedWith(
            compareByDescending<VocabularyEntry> { it.updatedAt }
                .thenByDescending { it.createdAt }
                .thenBy { it.id },
        )

    /**
     * Splits all needed `gemini-decision-1` verification questions for [entries] into batches of at most
     * [maxQuestionsPerBatch] (default [BATCH_MAX_QUESTIONS] = `100`):
     * - **Conflict questions (`noul`)** are generated **only** for unverified entries (`id !in verifiedEntryIds`),
     *   ordered with the most recently changed words first ([sortEntriesByRecentActivity]).
     * - **Duplicate/variant pair questions (`choice`)** compare unverified entries against **all** entries
     *   (`unverified × all`) and receive a dedicated quota in every batch so conflict questions never starve pair checks.
     * - Each returned [DecisionRequestPlan] records [DecisionRequestPlan.coveredEntryIds]: the unverified entry IDs
     *   whose conflict and pair questions are completely finished once that batch succeeds.
     */
    fun buildRequestBatches(
        entries: List<VocabularyEntry>,
        dismissedKeys: Set<String> = emptySet(),
        verifiedEntryIds: Set<String> = emptySet(),
        maxQuestionsPerBatch: Int = BATCH_MAX_QUESTIONS,
    ): List<DecisionRequestPlan> {
        val batchCap = maxQuestionsPerBatch.coerceIn(1, MAX_QUESTIONS_PER_CALL)
        val valid = entries.filter { it.id.isNotBlank() && it.wordDetails.word.isNotBlank() }
        if (valid.isEmpty()) return emptyList()

        val unverifiedByRecent = sortEntriesByRecentActivity(
            valid.filter { it.id !in verifiedEntryIds },
        )
        if (unverifiedByRecent.isEmpty()) return emptyList()

        val allConflicts = mutableListOf<DecisionConflictQuestion>()
        for ((idx, entry) in unverifiedByRecent.withIndex()) {
            val dismissKey = VocabularyDecisionFinding.conflictDismissKey(entry.id)
            if (dismissKey in dismissedKeys) continue
            val mainWord = entry.wordDetails.word.trim()
            val mainMeaning = entry.wordDetails.meaningEnglish.trim()

            if (mainMeaning.isNotEmpty()) {
                allConflicts += DecisionConflictQuestion(
                    questionId = "conflict_${idx}_main",
                    entryId = entry.id,
                    word = mainWord,
                    mainMeaning = mainMeaning,
                    isMainWordDefinition = true,
                )
            }

            for ((synIdx, syn) in entry.synonyms.withIndex()) {
                if (syn.word.isBlank()) continue
                allConflicts += DecisionConflictQuestion(
                    questionId = "conflict_${idx}_syn_$synIdx",
                    entryId = entry.id,
                    word = mainWord,
                    mainMeaning = mainMeaning,
                    isAntonym = false,
                    relationIndex = synIdx,
                    relationDetails = syn.trimmed(),
                )
            }
            for ((antIdx, ant) in entry.antonyms.withIndex()) {
                if (ant.word.isBlank()) continue
                allConflicts += DecisionConflictQuestion(
                    questionId = "conflict_${idx}_ant_$antIdx",
                    entryId = entry.id,
                    word = mainWord,
                    mainMeaning = mainMeaning,
                    isAntonym = true,
                    relationIndex = antIdx,
                    relationDetails = ant.trimmed(),
                )
            }
        }

        val sortedForPairs = sortEntriesForPrimarySelection(valid)
        val maxTotalPairs = maxOf(ALL_PAIRS_THRESHOLD, unverifiedByRecent.size * 2)
        val allPairs = selectCandidatePairs(
            sortedEntries = sortedForPairs,
            dismissedKeys = dismissedKeys,
            maxPairs = maxTotalPairs,
            verifiedEntryIds = verifiedEntryIds,
        )

        if (allConflicts.isEmpty() && allPairs.isEmpty()) {
            return emptyList()
        }

        val unverifiedIds = unverifiedByRecent.map { it.id }.toSet()
        val remainingQuestionsPerEntry = HashMap<String, Int>()
        for (id in unverifiedIds) remainingQuestionsPerEntry[id] = 0
        for (q in allConflicts) {
            if (q.entryId in unverifiedIds) {
                remainingQuestionsPerEntry[q.entryId] = (remainingQuestionsPerEntry[q.entryId] ?: 0) + 1
            }
        }
        for (p in allPairs) {
            if (p.primaryId in unverifiedIds) {
                remainingQuestionsPerEntry[p.primaryId] = (remainingQuestionsPerEntry[p.primaryId] ?: 0) + 1
            }
            if (p.duplicateId in unverifiedIds) {
                remainingQuestionsPerEntry[p.duplicateId] = (remainingQuestionsPerEntry[p.duplicateId] ?: 0) + 1
            }
        }

        val alreadyCompletedIds = LinkedHashSet<String>()
        for ((id, count) in remainingQuestionsPerEntry) {
            if (count == 0) alreadyCompletedIds += id
        }

        val batches = mutableListOf<DecisionRequestPlan>()
        var pairCursor = 0
        var conflictCursor = 0
        val reservedPairQuota = minOf(MIN_PAIR_SLOTS_PER_BATCH, (batchCap * 30 / 100).coerceAtLeast(1))

        while (pairCursor < allPairs.size || conflictCursor < allConflicts.size) {
            val availablePairs = allPairs.size - pairCursor
            val availableConflicts = allConflicts.size - conflictCursor

            var takePairs = minOf(availablePairs, reservedPairQuota)
            var takeConflicts = minOf(availableConflicts, batchCap - takePairs)
            val leftover = batchCap - (takePairs + takeConflicts)
            if (leftover > 0) {
                if (availablePairs > takePairs) {
                    val extraPairs = minOf(availablePairs - takePairs, leftover)
                    takePairs += extraPairs
                } else if (availableConflicts > takeConflicts) {
                    val extraConflicts = minOf(availableConflicts - takeConflicts, leftover)
                    takeConflicts += extraConflicts
                }
            }

            val batchPairs = allPairs.subList(pairCursor, pairCursor + takePairs)
            val batchConflicts = allConflicts.subList(conflictCursor, conflictCursor + takeConflicts)
            pairCursor += takePairs
            conflictCursor += takeConflicts

            val newlyCovered = LinkedHashSet<String>()
            if (batches.isEmpty() && alreadyCompletedIds.isNotEmpty()) {
                newlyCovered.addAll(alreadyCompletedIds)
            }
            for (q in batchConflicts) {
                val left = (remainingQuestionsPerEntry[q.entryId] ?: 0) - 1
                remainingQuestionsPerEntry[q.entryId] = left
                if (left == 0 && q.entryId in unverifiedIds) {
                    newlyCovered += q.entryId
                }
            }
            for (p in batchPairs) {
                if (p.primaryId in unverifiedIds) {
                    val left = (remainingQuestionsPerEntry[p.primaryId] ?: 0) - 1
                    remainingQuestionsPerEntry[p.primaryId] = left
                    if (left == 0) newlyCovered += p.primaryId
                }
                if (p.duplicateId in unverifiedIds) {
                    val left = (remainingQuestionsPerEntry[p.duplicateId] ?: 0) - 1
                    remainingQuestionsPerEntry[p.duplicateId] = left
                    if (left == 0) newlyCovered += p.duplicateId
                }
            }

            val requestJson = formatRequestJson(CONCISE_DECISION_INPUT, batchPairs, batchConflicts)
            batches += DecisionRequestPlan(
                requestJson = requestJson,
                pairQuestions = batchPairs,
                conflictQuestions = batchConflicts,
                coveredEntryIds = newlyCovered,
            )
        }

        return batches
    }

    private const val CONCISE_DECISION_INPUT =
        "Evaluate the English vocabulary word pairs and synonym/antonym relations below."

    /**
     * Selects up to [maxPairs] candidate pairs `(primary, candidate)` to evaluate with `gemini-decision-1`.
     * Only pairs where at least one word is unverified (`id !in verifiedEntryIds`) are considered so old
     * verified pairs are never re-checked while every new or edited word is compared against the entire
     * vocabulary (`unverified × all`).
     */
    internal fun selectCandidatePairs(
        sortedEntries: List<VocabularyEntry>,
        dismissedKeys: Set<String>,
        maxPairs: Int,
        verifiedEntryIds: Set<String> = emptySet(),
    ): List<DecisionPairQuestion> {
        if (maxPairs <= 0 || sortedEntries.size < 2) return emptyList()
        data class ScoredPair(
            val primary: VocabularyEntry,
            val duplicate: VocabularyEntry,
            val score: Int,
            val latestActivity: Long,
        )

        val allPairs = mutableListOf<ScoredPair>()
        for (i in 0 until sortedEntries.size) {
            val primary = sortedEntries[i]
            val pKey = VocabularyRules.wordKey(primary.wordDetails.word)
            if (pKey.isEmpty()) continue
            val primaryUnverified = primary.id !in verifiedEntryIds
            for (j in i + 1 until sortedEntries.size) {
                val dup = sortedEntries[j]
                if (!primaryUnverified && dup.id in verifiedEntryIds) continue
                val dKey = VocabularyRules.wordKey(dup.wordDetails.word)
                if (dKey.isEmpty()) continue
                val dismissKey = VocabularyDecisionFinding.pairDismissKey(primary.id, dup.id)
                if (dismissKey in dismissedKeys) continue
                val score = similarityPriority(primary, pKey, dup, dKey)
                val activity = maxOf(primary.updatedAt, dup.updatedAt, primary.createdAt, dup.createdAt)
                allPairs += ScoredPair(
                    primary = primary,
                    duplicate = dup,
                    score = score,
                    latestActivity = activity,
                )
            }
        }

        val limit = maxPairs.coerceAtLeast(1)
        val chosen = if (sortedEntries.size <= 8 && allPairs.size <= limit) {
            allPairs
        } else {
            val plausible = allPairs
                .filter { it.score > 0 }
                .sortedWith(
                    compareByDescending<ScoredPair> { it.score }
                        .thenByDescending { it.latestActivity },
                )
            if (plausible.isNotEmpty()) {
                plausible.take(limit)
            } else {
                allPairs
                    .sortedByDescending { it.latestActivity }
                    .take(limit.coerceAtMost(15))
            }
        }

        return chosen.mapIndexed { idx, pair ->
            DecisionPairQuestion(
                questionId = "pair_$idx",
                primaryId = pair.primary.id,
                duplicateId = pair.duplicate.id,
                primaryWord = pair.primary.wordDetails.word.trim(),
                duplicateWord = pair.duplicate.wordDetails.word.trim(),
                primaryMeaning = pair.primary.wordDetails.meaningEnglish.trim(),
                duplicateMeaning = pair.duplicate.wordDetails.meaningEnglish.trim(),
            )
        }
    }

    private fun similarityPriority(
        a: VocabularyEntry,
        aKey: String,
        b: VocabularyEntry,
        bKey: String,
    ): Int {
        if (aKey == bKey) return 100
        var score = 0
        val commonPrefix = aKey.commonPrefixWith(bKey).length
        if (commonPrefix >= 4) score += 40 + commonPrefix * 2
        else if (commonPrefix >= 3) score += 25
        val dist = editDistanceBounded(aKey, bKey, limit = 3)
        if (dist <= 2) score += 50 - dist * 10
        else if (dist == 3) score += 20
        if (aKey.startsWith(bKey) || bKey.startsWith(aKey)) score += 35
        val aSynAnt = (a.synonyms + a.antonyms).map { VocabularyRules.wordKey(it.word) }.toSet()
        val bSynAnt = (b.synonyms + b.antonyms).map { VocabularyRules.wordKey(it.word) }.toSet()
        if (bKey in aSynAnt || aKey in bSynAnt) score += 25
        if (aSynAnt.intersect(bSynAnt).isNotEmpty()) score += 15
        val aMeaningWords = VocabularyRules.wordKey(a.wordDetails.meaningEnglish)
            .split(" ")
            .filter { it.length >= 5 && it !in COMMON_MEANING_STOPWORDS }
            .toSet()
        val bMeaningWords = VocabularyRules.wordKey(b.wordDetails.meaningEnglish)
            .split(" ")
            .filter { it.length >= 5 && it !in COMMON_MEANING_STOPWORDS }
            .toSet()
        if (aMeaningWords.intersect(bMeaningWords).isNotEmpty()) score += 20
        return score
    }

    private val COMMON_MEANING_STOPWORDS = setOf(
        "something",
        "someone",
        "having",
        "showing",
        "making",
        "state",
        "quality",
        "person",
        "action",
        "things",
    )

    private fun editDistanceBounded(a: String, b: String, limit: Int): Int {
        if (abs(a.length - b.length) > limit) return limit + 1
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            var rowMin = curr[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,
                    curr[j - 1] + 1,
                    prev[j - 1] + cost,
                )
                if (curr[j] < rowMin) rowMin = curr[j]
            }
            if (rowMin > limit) return limit + 1
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }

    private fun formatRequestJson(
        inputDigest: String,
        pairQuestions: List<DecisionPairQuestion>,
        conflictQuestions: List<DecisionConflictQuestion>,
    ): String = buildString {
        append("{\n")
        append("  \"model\": ").append(VocabularyJsonFormat.escapeJson(MODEL_ID)).append(",\n")
        append("  \"input\": ").append(VocabularyJsonFormat.escapeJson(inputDigest)).append(",\n")
        append("  \"questions\": {\n")

        val questionBlocks = mutableListOf<String>()
        for (q in pairQuestions) {
            val instruction = if (q.primaryMeaning.isNotEmpty() || q.duplicateMeaning.isNotEmpty()) {
                "Compare \"${q.primaryWord}\" (\"${q.primaryMeaning}\") vs \"${q.duplicateWord}\" (\"${q.duplicateMeaning}\"):"
            } else {
                "Compare \"${q.primaryWord}\" vs \"${q.duplicateWord}\":"
            }
            val block = buildString {
                append("    ").append(VocabularyJsonFormat.escapeJson(q.questionId)).append(": {\n")
                append("      \"type\": \"choice\",\n")
                append("      \"instructions\": ").append(VocabularyJsonFormat.escapeJson(instruction)).append(",\n")
                append("      \"criteria\": {\n")
                append("        \"distinct\": \"Different words\",\n")
                append("        \"exact_duplicate\": \"Same word or spacing/hyphen variant\",\n")
                append("        \"root_or_inflected\": \"Same root inflected/plural/derived form (abating/abate, forsook/forsake, phenomena/phenomenon, tenacity/tenacious)\",\n")
                append("        \"spelling_or_typo\": \"British/American spelling variant or typo (colour/color, manoeuvre/maneuver, accomodate/accommodate)\"\n")
                append("      }\n")
                append("    }")
            }
            questionBlocks += block
        }

        for (q in conflictQuestions) {
            val relWord = q.relationDetails.word.trim()
            val relMeaning = q.relationDetails.meaningEnglish.trim()
            val instruction = when {
                q.isMainWordDefinition ->
                    "For vocabulary headword \"${q.word}\", is its listed meaning \"${q.mainMeaning}\" factually wrong, opposite, or the definition of a different word?"
                q.isAntonym ->
                    "In \"${q.word}\" (\"${q.mainMeaning}\"), is listed antonym \"$relWord\" (\"$relMeaning\") a conflict (e.g., a synonym mistakenly listed under antonyms, or \"$relMeaning\" is the wrong definition of \"$relWord\")?"
                else ->
                    "In \"${q.word}\" (\"${q.mainMeaning}\"), is listed synonym \"$relWord\" (\"$relMeaning\") a conflict (e.g., an antonym mistakenly listed under synonyms, or \"$relMeaning\" is the wrong definition of \"$relWord\")?"
            }
            val block = buildString {
                append("    ").append(VocabularyJsonFormat.escapeJson(q.questionId)).append(": {\n")
                append("      \"type\": \"noul\",\n")
                append("      \"instructions\": ").append(VocabularyJsonFormat.escapeJson(instruction)).append("\n")
                append("    }")
            }
            questionBlocks += block
        }

        append(questionBlocks.joinToString(",\n"))
        append("\n  }\n")
        append("}")
    }

    /**
     * Parses the JSON response from `POST /v1beta/decisions` (`gemini-decision-1`) and returns all
     * findings whose probability meets or exceeds [minProbability].
     */
    fun parseResponse(
        responseJson: String,
        plan: DecisionRequestPlan,
        minProbability: Double = MIN_PROBABILITY,
    ): List<VocabularyDecisionFinding> {
        val root = VocabularyJsonFormat.parseTree(responseJson) as? Map<*, *>
            ?: throw IllegalArgumentException("Invalid JSON response from $MODEL_ID.")
        val answers = root["answers"] as? Map<*, *>
            ?: throw IllegalArgumentException("Missing 'answers' object in $MODEL_ID response.")

        val findings = mutableListOf<VocabularyDecisionFinding>()

        for (q in plan.pairQuestions) {
            val ans = answers[q.questionId] as? Map<*, *> ?: continue
            val chosen = ans["choice"] as? String ?: continue
            if (chosen == CHOICE_DISTINCT) continue
            val kind = DecisionDuplicateKind.fromApiCode(chosen) ?: continue
            val probabilities = ans["probabilities"] as? Map<*, *>
            val prob = (probabilities?.get(chosen) as? Number)?.toDouble()
                ?: (ans["confidence"] as? Number)?.toDouble()
                ?: 0.0
            val confidence = (ans["confidence"] as? Number)?.toDouble() ?: prob
            if (prob >= minProbability) {
                findings += VocabularyDecisionFinding.DuplicateOrVariant(
                    primaryId = q.primaryId,
                    duplicateId = q.duplicateId,
                    kind = kind,
                    probability = prob,
                    confidence = confidence,
                )
            }
        }

        val conflictByEntryId = LinkedHashMap<String, VocabularyDecisionFinding.MeaningConflict>()
        for (q in plan.conflictQuestions) {
            val ans = answers[q.questionId] as? Map<*, *> ?: continue
            val noulProb = (ans["noul"] as? Number)?.toDouble()
                ?: (ans["probability"] as? Number)?.toDouble()
                ?: continue
            val confidence = (ans["confidence"] as? Number)?.toDouble() ?: noulProb
            if (noulProb >= minProbability) {
                val candidate = if (q.isMainWordDefinition) {
                    VocabularyDecisionFinding.MeaningConflict(
                        entryId = q.entryId,
                        probability = noulProb,
                        confidence = confidence,
                        conflictingTarget = null,
                        customConflictLabel = "Main word \"${q.word}\" definition",
                        customReason = "Wrong definition for main word \"${q.word}\": listed meaning \"${q.mainMeaning}\" was flagged by $MODEL_ID as factually incorrect.",
                    )
                } else {
                    val relLabel = if (q.isAntonym) "Antonym" else "Synonym"
                    val relWord = q.relationDetails.word.trim()
                    val relMeaning = q.relationDetails.meaningEnglish.trim()
                    val target = if (relWord.isNotEmpty()) {
                        VocabularyRules.ConflictingRelationTarget(
                            isAntonym = q.isAntonym,
                            index = q.relationIndex,
                            details = q.relationDetails,
                            conflictingWordLabel = "$relLabel \"$relWord\"",
                            reason = "Conflict in ${relLabel.lowercase()} \"$relWord\" (\"$relMeaning\"): flagged by $MODEL_ID as contradicting or misdefining main word \"${q.word}\" (\"${q.mainMeaning}\").",
                        )
                    } else {
                        null
                    }
                    VocabularyDecisionFinding.MeaningConflict(
                        entryId = q.entryId,
                        probability = noulProb,
                        confidence = confidence,
                        conflictingTarget = target,
                    )
                }
                val previous = conflictByEntryId[q.entryId]
                val keepPreviousMainConflict = previous?.customConflictLabel != null && !q.isMainWordDefinition
                if (!keepPreviousMainConflict && (previous == null || q.isMainWordDefinition || candidate.probability > previous.probability)) {
                    conflictByEntryId[q.entryId] = candidate
                }
            }
        }
        findings.addAll(conflictByEntryId.values)

        return findings
    }

    /**
     * Persisted snapshot of already-verified entry fingerprints ([verifiedDigests]: `entryId -> entryContentDigest`)
     * and active [findings] so verified words are never re-checked across app restarts or rate-limit retries.
     */
    data class DecisionVerificationSnapshot(
        val verifiedDigests: Map<String, Int> = emptyMap(),
        val findings: List<VocabularyDecisionFinding> = emptyList(),
    )

    fun encodeVerificationSnapshot(snapshot: DecisionVerificationSnapshot): String = buildString {
        append("{\n")
        append("  \"verifiedDigests\": {\n")
        val digestEntries = snapshot.verifiedDigests.entries.toList()
        for ((i, kv) in digestEntries.withIndex()) {
            append("    ").append(VocabularyJsonFormat.escapeJson(kv.key)).append(": ").append(kv.value)
            if (i < digestEntries.lastIndex) append(",")
            append("\n")
        }
        append("  },\n")
        append("  \"findings\": [\n")
        for ((i, f) in snapshot.findings.withIndex()) {
            when (f) {
                is VocabularyDecisionFinding.DuplicateOrVariant -> {
                    append("    {\n")
                    append("      \"type\": \"pair\",\n")
                    append("      \"primaryId\": ").append(VocabularyJsonFormat.escapeJson(f.primaryId)).append(",\n")
                    append("      \"duplicateId\": ").append(VocabularyJsonFormat.escapeJson(f.duplicateId)).append(",\n")
                    append("      \"kind\": ").append(VocabularyJsonFormat.escapeJson(f.kind.apiCode)).append(",\n")
                    append("      \"probability\": ").append(f.probability).append(",\n")
                    append("      \"confidence\": ").append(f.confidence).append("\n")
                    append("    }")
                }
                is VocabularyDecisionFinding.MeaningConflict -> {
                    append("    {\n")
                    append("      \"type\": \"conflict\",\n")
                    append("      \"entryId\": ").append(VocabularyJsonFormat.escapeJson(f.entryId)).append(",\n")
                    append("      \"probability\": ").append(f.probability).append(",\n")
                    append("      \"confidence\": ").append(f.confidence)
                    if (f.customConflictLabel != null) {
                        append(",\n      \"customConflictLabel\": ").append(VocabularyJsonFormat.escapeJson(f.customConflictLabel))
                    }
                    if (f.customReason != null) {
                        append(",\n      \"customReason\": ").append(VocabularyJsonFormat.escapeJson(f.customReason))
                    }
                    val t = f.conflictingTarget
                    if (t != null) {
                        append(",\n      \"targetIsAntonym\": ").append(t.isAntonym)
                        append(",\n      \"targetIndex\": ").append(t.index)
                        append(",\n      \"targetWord\": ").append(VocabularyJsonFormat.escapeJson(t.details.word))
                        append(",\n      \"targetMeaningEnglish\": ").append(VocabularyJsonFormat.escapeJson(t.details.meaningEnglish))
                        append(",\n      \"targetMeaningHinglish\": ").append(VocabularyJsonFormat.escapeJson(t.details.meaningHinglish))
                        append(",\n      \"targetExampleSentence\": ").append(VocabularyJsonFormat.escapeJson(t.details.exampleSentence))
                        append(",\n      \"targetLabel\": ").append(VocabularyJsonFormat.escapeJson(t.conflictingWordLabel))
                        append(",\n      \"targetReason\": ").append(VocabularyJsonFormat.escapeJson(t.reason))
                    }
                    append("\n    }")
                }
            }
            if (i < snapshot.findings.lastIndex) append(",")
            append("\n")
        }
        append("  ]\n")
        append("}")
    }

    fun decodeVerificationSnapshot(jsonText: String): DecisionVerificationSnapshot {
        val root = VocabularyJsonFormat.parseTree(jsonText) as? Map<*, *>
            ?: return DecisionVerificationSnapshot()
        val digestsMap = LinkedHashMap<String, Int>()
        val rawDigests = root["verifiedDigests"] as? Map<*, *>
        if (rawDigests != null) {
            for ((k, v) in rawDigests) {
                val id = (k as? String)?.trim().orEmpty()
                val num = (v as? Number)?.toInt()
                if (id.isNotEmpty() && num != null) {
                    digestsMap[id] = num
                }
            }
        }
        val findingsList = mutableListOf<VocabularyDecisionFinding>()
        val rawFindings = root["findings"] as? List<*>
        if (rawFindings != null) {
            for (item in rawFindings) {
                val map = item as? Map<*, *> ?: continue
                when (map["type"] as? String) {
                    "pair" -> {
                        val primaryId = (map["primaryId"] as? String)?.trim().orEmpty()
                        val duplicateId = (map["duplicateId"] as? String)?.trim().orEmpty()
                        val kind = DecisionDuplicateKind.fromApiCode(map["kind"] as? String)
                        val prob = (map["probability"] as? Number)?.toDouble() ?: continue
                        val conf = (map["confidence"] as? Number)?.toDouble() ?: prob
                        if (primaryId.isNotEmpty() && duplicateId.isNotEmpty() && kind != null) {
                            findingsList += VocabularyDecisionFinding.DuplicateOrVariant(
                                primaryId = primaryId,
                                duplicateId = duplicateId,
                                kind = kind,
                                probability = prob,
                                confidence = conf,
                            )
                        }
                    }
                    "conflict" -> {
                        val entryId = (map["entryId"] as? String)?.trim().orEmpty()
                        val prob = (map["probability"] as? Number)?.toDouble() ?: continue
                        val conf = (map["confidence"] as? Number)?.toDouble() ?: prob
                        if (entryId.isEmpty()) continue
                        val customLabel = map["customConflictLabel"] as? String
                        val customReason = map["customReason"] as? String
                        val targetWord = map["targetWord"] as? String
                        val target = if (!targetWord.isNullOrBlank()) {
                            VocabularyRules.ConflictingRelationTarget(
                                isAntonym = (map["targetIsAntonym"] as? Boolean) ?: true,
                                index = (map["targetIndex"] as? Number)?.toInt() ?: 0,
                                details = WordDetails(
                                    word = targetWord,
                                    meaningEnglish = (map["targetMeaningEnglish"] as? String).orEmpty(),
                                    meaningHinglish = (map["targetMeaningHinglish"] as? String).orEmpty(),
                                    exampleSentence = (map["targetExampleSentence"] as? String).orEmpty(),
                                ),
                                conflictingWordLabel = (map["targetLabel"] as? String).orEmpty(),
                                reason = (map["targetReason"] as? String).orEmpty(),
                            )
                        } else {
                            null
                        }
                        findingsList += VocabularyDecisionFinding.MeaningConflict(
                            entryId = entryId,
                            probability = prob,
                            confidence = conf,
                            conflictingTarget = target,
                            customConflictLabel = customLabel,
                            customReason = customReason,
                        )
                    }
                }
            }
        }
        return DecisionVerificationSnapshot(
            verifiedDigests = digestsMap,
            findings = findingsList,
        )
    }
}
