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
) {
    val totalQuestions: Int get() = pairQuestions.size + conflictQuestions.size
}

/**
 * Pure-Kotlin request builder and response parser for the Google Gemini Decisions API (`gemini-decision-1`).
 *
 * Evaluates up to [MAX_QUESTIONS_PER_CALL] structured `choice` and `noul` (yes/no) questions in a single
 * forward pass with zero output tokens.
 */
object VocabularyDecisionRules {

    const val MODEL_ID = "gemini-decision-1"
    const val MIN_PROBABILITY = 0.75
    const val MAX_QUESTIONS_PER_CALL = 250
    private const val ALL_PAIRS_THRESHOLD = 100

    private const val CHOICE_DISTINCT = "distinct"

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
     * Builds a single batched `gemini-decision-1` request plan for [entries], skipping any pairs or
     * conflicts whose keys are in [dismissedKeys]. Returns `null` when there are no questions to ask.
     */
    fun buildRequestPlan(
        entries: List<VocabularyEntry>,
        dismissedKeys: Set<String> = emptySet(),
    ): DecisionRequestPlan? {
        val valid = sortEntriesForPrimarySelection(
            entries.filter { it.id.isNotBlank() && it.wordDetails.word.isNotBlank() },
        )
        if (valid.isEmpty()) return null

        val conflictQuestions = mutableListOf<DecisionConflictQuestion>()
        for ((idx, entry) in valid.withIndex()) {
            if (conflictQuestions.size >= MAX_QUESTIONS_PER_CALL) break
            val dismissKey = VocabularyDecisionFinding.conflictDismissKey(entry.id)
            if (dismissKey in dismissedKeys) continue
            val mainWord = entry.wordDetails.word.trim()
            val mainMeaning = entry.wordDetails.meaningEnglish.trim()

            if (mainMeaning.isNotEmpty()) {
                conflictQuestions += DecisionConflictQuestion(
                    questionId = "conflict_${idx}_main",
                    entryId = entry.id,
                    word = mainWord,
                    mainMeaning = mainMeaning,
                    isMainWordDefinition = true,
                )
            }

            for ((synIdx, syn) in entry.synonyms.withIndex()) {
                if (conflictQuestions.size >= MAX_QUESTIONS_PER_CALL) break
                if (syn.word.isBlank()) continue
                conflictQuestions += DecisionConflictQuestion(
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
                if (conflictQuestions.size >= MAX_QUESTIONS_PER_CALL) break
                if (ant.word.isBlank()) continue
                conflictQuestions += DecisionConflictQuestion(
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

        val remainingBudget = (MAX_QUESTIONS_PER_CALL - conflictQuestions.size).coerceAtLeast(0)
        val candidatePairs = selectCandidatePairs(valid, dismissedKeys, remainingBudget)
        if (conflictQuestions.isEmpty() && candidatePairs.isEmpty()) return null

        val requestJson = formatRequestJson(CONCISE_DECISION_INPUT, candidatePairs, conflictQuestions)
        return DecisionRequestPlan(
            requestJson = requestJson,
            pairQuestions = candidatePairs,
            conflictQuestions = conflictQuestions,
        )
    }

    private const val CONCISE_DECISION_INPUT =
        "Evaluate the English vocabulary word pairs and synonym/antonym relations below."

    /**
     * Selects up to [maxPairs] candidate pairs `(primary, candidate)` to evaluate with `gemini-decision-1`.
     * For very small sets (`<= 8` words) fitting inside [ALL_PAIRS_THRESHOLD] and [maxPairs], every pair is
     * sent; for larger vocabularies, pairs with orthographic, root-stem, or meaning similarity are prioritized
     * so payloads stay fast and comfortably within the 50K TPM limit.
     */
    internal fun selectCandidatePairs(
        sortedEntries: List<VocabularyEntry>,
        dismissedKeys: Set<String>,
        maxPairs: Int,
    ): List<DecisionPairQuestion> {
        if (maxPairs <= 0 || sortedEntries.size < 2) return emptyList()
        data class ScoredPair(
            val primary: VocabularyEntry,
            val duplicate: VocabularyEntry,
            val score: Int,
        )

        val allPairs = mutableListOf<ScoredPair>()
        for (i in 0 until sortedEntries.size) {
            val primary = sortedEntries[i]
            val pKey = VocabularyRules.wordKey(primary.wordDetails.word)
            if (pKey.isEmpty()) continue
            for (j in i + 1 until sortedEntries.size) {
                val dup = sortedEntries[j]
                val dKey = VocabularyRules.wordKey(dup.wordDetails.word)
                if (dKey.isEmpty()) continue
                val dismissKey = VocabularyDecisionFinding.pairDismissKey(primary.id, dup.id)
                if (dismissKey in dismissedKeys) continue
                val score = similarityPriority(primary, pKey, dup, dKey)
                allPairs += ScoredPair(primary = primary, duplicate = dup, score = score)
            }
        }

        val limit = maxPairs.coerceAtMost(ALL_PAIRS_THRESHOLD)
        val chosen = if (sortedEntries.size <= 8 && allPairs.size <= limit) {
            allPairs
        } else {
            val plausible = allPairs.filter { it.score > 0 }.sortedByDescending { it.score }
            if (plausible.isNotEmpty()) plausible.take(limit) else allPairs.take(limit.coerceAtMost(15))
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
}
