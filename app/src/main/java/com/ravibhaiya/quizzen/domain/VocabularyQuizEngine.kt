package com.ravibhaiya.quizzen.domain

import kotlin.random.Random

/**
 * Dedicated Vocabulary Quiz Generator & Scheduling Algorithm (distinct from Math's infinite `PracticeSession`).
 *
 * How it works:
 * 1. **Semantic Cluster Graph Construction**:
 *    Each studied [VocabularyEntry] forms a semantic cluster with two sets:
 *    - Same-meaning set `S+ = { mainWord } U synonyms` (every pair of distinct words in `S+` are synonyms).
 *    - Opposite-meaning set `S- = antonyms` (every word in `S-` is an antonym of every word in `S+`, including both
 *      the main word and all synonyms).
 * 2. **Full-Coverage Derangement & Cross-Cluster Pairing**:
 *    - Every active word in the studied entries (main word, each synonym, and each antonym) is guaranteed to be asked
 *      as a question prompt once, making the quiz finite and ending right after all words are covered.
 *    - For `S+` in Synonym mode, words are paired using a shuffled single-cycle derangement `w_i -> w_{(i+1) mod k}`.
 *      Whenever a word has 2+ synonyms (`|S+| >= 3`), this directly creates `Synonym A -> Synonym B` questions where
 *      the main word is neither the question nor the correct option.
 *    - For Antonym questions, words in `S-` are paired across a shuffled `S+` (and vice-versa in Antonym-only mode),
 *      so antonyms pair directly with synonyms as well as with the main word.
 * 3. **Main-Word Contiguous Grouping**:
 *    - All Synonym and Antonym questions belonging to a main word (`cluster`) stay grouped together in a contiguous
 *      block so the main word remains displayed until all of its synonym and antonym questions are finished before
 *      moving on to the next main word.
 * 4. **Conflict-Free Smart Distractors**:
 *    - Excludes all words in the cluster (and across all entries) that also satisfy the asked relation with the prompt,
 *      guaranteeing a single unambiguous correct option among the 4 choices.
 *    - Injects up to 1 opposite-relation word from the same cluster as a comprehension trap when available, then fills
 *      remaining distractors from other studied words first before falling back to the wider database pool.
 */
object VocabularyQuizEngine {

    private data class SemanticCluster(
        val id: String,
        val mainWord: String,
        val partOfSpeech: String,
        val sameMeaning: List<String>,
        val oppositeMeaning: List<String>,
        val detailsByWordLower: Map<String, WordDetails> = emptyMap(),
    )

    private data class RawQuestion(
        val cluster: SemanticCluster,
        val mode: VocabularyQuizMode,
        val promptWord: String,
        val correctAnswer: String,
    )

    fun generateQuiz(
        studyEntries: List<VocabularyEntry>,
        allEntries: List<VocabularyEntry> = studyEntries,
        modes: Set<VocabularyQuizMode>,
        random: Random = Random.Default,
        maxQuestions: Int? = null,
    ): List<VocabularyQuizItem> {
        if (studyEntries.isEmpty() || modes.isEmpty()) return emptyList()

        val clusters = studyEntries.mapIndexedNotNull { index, entry ->
            toCluster(entry, fallbackId = "cluster_$index")
        }
        if (clusters.isEmpty()) return emptyList()

        val allSourceEntries = studyEntries + allEntries + VocabularyQuizRules.FALLBACK_ENTRIES
        val allClusters = allSourceEntries
            .mapIndexedNotNull { index, entry -> toCluster(entry, fallbackId = "all_$index") }
        val detailsByWord = buildWordDetailsLookup(allSourceEntries)

        val clusterBatches = clusters.shuffled(random)
            .mapNotNull { cluster ->
                val questions = buildQuestionsForCluster(cluster, modes, random)
                if (questions.isEmpty()) null else shuffleClusterQuestions(questions, random)
            }
        if (clusterBatches.isEmpty()) return emptyList()

        val orderedRaw = selectQuestionsWithQuota(
            clusterBatches = clusterBatches,
            maxQuestions = maxQuestions,
            random = random,
        )
        if (orderedRaw.isEmpty()) return emptyList()

        val studiedWordPool = clusters.flatMap { it.sameMeaning + it.oppositeMeaning }.distinctBy { it.lowercase() }
        val globalWordPool = allClusters.flatMap { it.sameMeaning + it.oppositeMeaning }.distinctBy { it.lowercase() }

        return orderedRaw.map { raw ->
            val options = buildOptions(
                raw = raw,
                allClusters = allClusters,
                studiedWordPool = studiedWordPool,
                globalWordPool = globalWordPool,
                random = random,
            )
            val promptLower = raw.promptWord.lowercase()
            val promptDetails = raw.cluster.detailsByWordLower[promptLower]
                ?: detailsByWord[promptLower]
                ?: WordDetails(word = raw.promptWord)
            val optionDetails = options.map { option ->
                val optLower = option.lowercase()
                raw.cluster.detailsByWordLower[optLower]
                    ?: detailsByWord[optLower]
                    ?: WordDetails(word = option)
            }
            val clusterPos = raw.cluster.partOfSpeech.ifBlank {
                detailsByWord[raw.cluster.mainWord.lowercase()]?.partOfSpeech.orEmpty()
            }
            VocabularyQuizItem(
                clusterId = raw.cluster.id,
                mode = raw.mode,
                promptWord = raw.promptWord,
                correctAnswer = raw.correctAnswer,
                options = options,
                promptDetails = promptDetails,
                optionDetails = optionDetails,
                clusterWord = raw.cluster.mainWord,
                clusterPartOfSpeech = clusterPos,
            )
        }
    }

    private fun buildWordDetailsLookup(entries: List<VocabularyEntry>): Map<String, WordDetails> {
        val map = LinkedHashMap<String, WordDetails>()
        fun register(details: WordDetails) {
            val clean = details.trimmed()
            val key = clean.word.lowercase()
            if (key.isEmpty()) return
            val existing = map[key]
            if (existing == null) {
                map[key] = clean
            } else {
                map[key] = WordDetails(
                    word = existing.word.ifBlank { clean.word },
                    partOfSpeech = existing.partOfSpeech.ifBlank { clean.partOfSpeech },
                    meaningEnglish = existing.meaningEnglish.ifBlank { clean.meaningEnglish },
                    meaningHinglish = existing.meaningHinglish.ifBlank { clean.meaningHinglish },
                    exampleSentence = existing.exampleSentence.ifBlank { clean.exampleSentence },
                )
            }
        }
        for (entry in entries) {
            val cleanEntry = entry.sanitized()
            register(cleanEntry.wordDetails)
            cleanEntry.synonyms.forEach(::register)
            cleanEntry.antonyms.forEach(::register)
        }
        return map
    }

    private fun toCluster(entry: VocabularyEntry, fallbackId: String): SemanticCluster? {
        val clean = entry.sanitized()
        val main = clean.wordDetails.word.trim()
        if (main.isEmpty()) return null

        val same = buildList {
            add(main)
            clean.synonyms.mapTo(this) { it.word.trim() }
        }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }

        val sameLower = same.map { it.lowercase() }.toSet()
        val opposite = clean.antonyms
            .map { it.word.trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in sameLower }
            .distinctBy { it.lowercase() }

        return SemanticCluster(
            id = clean.id.ifBlank { "${fallbackId}_${main.lowercase()}" },
            mainWord = main,
            partOfSpeech = clean.wordDetails.partOfSpeech.trim(),
            sameMeaning = same,
            oppositeMeaning = opposite,
            detailsByWordLower = buildWordDetailsLookup(listOf(clean)),
        )
    }

    /** Total number of unique questions [entry] produces under [modes] when not capped by `questionCount`. */
    fun fullQuestionCountFor(entry: VocabularyEntry, modes: Set<VocabularyQuizMode>): Int {
        val cluster = toCluster(entry, fallbackId = "count") ?: return 0
        val wantSynonyms = VocabularyQuizMode.Synonyms in modes && cluster.sameMeaning.size >= 2
        val wantAntonyms = VocabularyQuizMode.Antonyms in modes &&
            cluster.sameMeaning.isNotEmpty() &&
            cluster.oppositeMeaning.isNotEmpty()
        return when {
            wantSynonyms && wantAntonyms -> cluster.sameMeaning.size + cluster.oppositeMeaning.size
            wantSynonyms -> cluster.sameMeaning.size
            wantAntonyms -> cluster.sameMeaning.size + cluster.oppositeMeaning.size
            else -> 0
        }
    }

    private fun buildQuestionsForCluster(
        cluster: SemanticCluster,
        modes: Set<VocabularyQuizMode>,
        random: Random,
    ): List<RawQuestion> {
        val wantSynonyms = VocabularyQuizMode.Synonyms in modes && cluster.sameMeaning.size >= 2
        val wantAntonyms = VocabularyQuizMode.Antonyms in modes &&
            cluster.sameMeaning.isNotEmpty() &&
            cluster.oppositeMeaning.isNotEmpty()

        val questions = mutableListOf<RawQuestion>()

        if (wantSynonyms && wantAntonyms) {
            // Every word in S+ is asked once via a single-cycle derangement (covering Main->Syn, Syn->Syn, Syn->Main).
            val cycle = cluster.sameMeaning.shuffled(random)
            for (i in cycle.indices) {
                val prompt = cycle[i]
                val target = cycle[(i + 1) % cycle.size]
                questions += RawQuestion(
                    cluster = cluster,
                    mode = VocabularyQuizMode.Synonyms,
                    promptWord = prompt,
                    correctAnswer = target,
                )
            }
            // Every word in S- is asked once as an Antonym question paired across a shuffled S+ (including synonyms!).
            val targets = cluster.sameMeaning.shuffled(random)
            val shuffledOpposites = cluster.oppositeMeaning.shuffled(random)
            for (i in shuffledOpposites.indices) {
                val antPrompt = shuffledOpposites[i]
                val target = targets[i % targets.size]
                questions += RawQuestion(
                    cluster = cluster,
                    mode = VocabularyQuizMode.Antonyms,
                    promptWord = antPrompt,
                    correctAnswer = target,
                )
            }
        } else if (wantSynonyms) {
            val cycle = cluster.sameMeaning.shuffled(random)
            for (i in cycle.indices) {
                val prompt = cycle[i]
                val target = cycle[(i + 1) % cycle.size]
                questions += RawQuestion(
                    cluster = cluster,
                    mode = VocabularyQuizMode.Synonyms,
                    promptWord = prompt,
                    correctAnswer = target,
                )
            }
        } else if (wantAntonyms) {
            // In Antonym-only mode, every word in S+ asks for an antonym in S-, and every word in S- asks for an antonym in S+.
            val shuffledSame = cluster.sameMeaning.shuffled(random)
            val shuffledOpposite = cluster.oppositeMeaning.shuffled(random)
            for (i in shuffledSame.indices) {
                questions += RawQuestion(
                    cluster = cluster,
                    mode = VocabularyQuizMode.Antonyms,
                    promptWord = shuffledSame[i],
                    correctAnswer = shuffledOpposite[i % shuffledOpposite.size],
                )
            }
            for (i in shuffledOpposite.indices) {
                questions += RawQuestion(
                    cluster = cluster,
                    mode = VocabularyQuizMode.Antonyms,
                    promptWord = shuffledOpposite[i],
                    correctAnswer = shuffledSame[i % shuffledSame.size],
                )
            }
        }

        return questions
    }

    /**
     * Shuffles all Synonym and Antonym questions of a single main-word cluster together randomly. When both modes are
     * present and the random shuffle happens to leave all questions of one mode in a single contiguous block before all
     * questions of the other mode, swaps the boundary question so Synonym and Antonym questions are genuinely mixed.
     */
    private fun shuffleClusterQuestions(
        questions: List<RawQuestion>,
        random: Random,
    ): List<RawQuestion> {
        if (questions.size <= 1) return questions
        val shuffled = questions.shuffled(random).toMutableList()
        val hasSyn = shuffled.any { it.mode == VocabularyQuizMode.Synonyms }
        val hasAnt = shuffled.any { it.mode == VocabularyQuizMode.Antonyms }
        if (hasSyn && hasAnt && shuffled.size >= 3) {
            val transitions = shuffled.zipWithNext().count { (a, b) -> a.mode != b.mode }
            if (transitions == 1) {
                val boundary = shuffled.zipWithNext().indexOfFirst { (a, b) -> a.mode != b.mode }
                if (boundary >= 0 && boundary + 1 < shuffled.size) {
                    val temp = shuffled[boundary]
                    shuffled[boundary] = shuffled[boundary + 1]
                    shuffled[boundary + 1] = temp
                }
            }
        }
        return shuffled
    }

    /**
     * Enforces [maxQuestions] across shuffled cluster batches while keeping every cluster's questions contiguous and
     * ensuring each studied word contributes questions before any single word takes extra slots.
     */
    private fun selectQuestionsWithQuota(
        clusterBatches: List<List<RawQuestion>>,
        maxQuestions: Int?,
        random: Random,
    ): List<RawQuestion> {
        val totalAvailable = clusterBatches.sumOf { it.size }
        val limit = maxQuestions?.let { VocabularyQuizRules.coerceQuestionCount(it) } ?: totalAvailable
        if (limit >= totalAvailable) {
            return clusterBatches.flatten()
        }

        val quotas = IntArray(clusterBatches.size)
        var remaining = limit
        while (remaining > 0) {
            var assignedInPass = false
            for (i in clusterBatches.indices) {
                if (remaining == 0) break
                if (quotas[i] < clusterBatches[i].size) {
                    quotas[i]++
                    remaining--
                    assignedInPass = true
                }
            }
            if (!assignedInPass) break
        }

        val selected = mutableListOf<RawQuestion>()
        for (i in clusterBatches.indices) {
            val quota = quotas[i]
            if (quota <= 0) continue
            val batch = clusterBatches[i]
            if (quota >= batch.size) {
                selected += batch
            } else {
                selected += pickMixedSubset(batch, quota, random)
            }
        }
        return selected
    }

    private fun pickMixedSubset(
        batch: List<RawQuestion>,
        quota: Int,
        random: Random,
    ): List<RawQuestion> {
        val taken = batch.take(quota).toMutableList()
        if (quota >= 2) {
            val remaining = batch.drop(quota)
            if (taken.none { it.mode == VocabularyQuizMode.Synonyms }) {
                val syn = remaining.firstOrNull { it.mode == VocabularyQuizMode.Synonyms }
                if (syn != null) taken[taken.lastIndex] = syn
            } else if (taken.none { it.mode == VocabularyQuizMode.Antonyms }) {
                val ant = remaining.firstOrNull { it.mode == VocabularyQuizMode.Antonyms }
                if (ant != null) taken[taken.lastIndex] = ant
            }
            return shuffleClusterQuestions(taken, random)
        }
        return taken
    }

    private fun buildOptions(
        raw: RawQuestion,
        allClusters: List<SemanticCluster>,
        studiedWordPool: List<String>,
        globalWordPool: List<String>,
        random: Random,
    ): List<String> {
        val promptLower = raw.promptWord.lowercase()
        val correctLower = raw.correctAnswer.lowercase()

        // Any word that is ALSO a valid answer for (promptWord, mode) in any cluster must be excluded from distractors.
        val forbiddenLower = mutableSetOf(promptLower, correctLower)
        val trapCandidates = mutableListOf<String>()

        for (cluster in allClusters) {
            val inSame = cluster.sameMeaning.any { it.equals(raw.promptWord, ignoreCase = true) }
            val inOpposite = cluster.oppositeMeaning.any { it.equals(raw.promptWord, ignoreCase = true) }
            if (!inSame && !inOpposite) continue
            val isActiveCluster = cluster.id == raw.cluster.id

            when (raw.mode) {
                VocabularyQuizMode.Synonyms -> {
                    if (inSame) {
                        cluster.sameMeaning.forEach { forbiddenLower += it.lowercase() }
                        if (isActiveCluster) cluster.oppositeMeaning.forEach { trapCandidates += it }
                    } else {
                        cluster.oppositeMeaning.forEach { forbiddenLower += it.lowercase() }
                        if (isActiveCluster) cluster.sameMeaning.forEach { trapCandidates += it }
                    }
                }
                VocabularyQuizMode.Antonyms -> {
                    if (inSame) {
                        cluster.oppositeMeaning.forEach { forbiddenLower += it.lowercase() }
                        if (isActiveCluster) cluster.sameMeaning.forEach { trapCandidates += it }
                    } else {
                        cluster.sameMeaning.forEach { forbiddenLower += it.lowercase() }
                        if (isActiveCluster) cluster.oppositeMeaning.forEach { trapCandidates += it }
                    }
                }
            }
        }

        val distractors = mutableListOf<String>()

        // 1. Add up to 1 opposite-relation word from the same cluster as a smart comprehension trap.
        val validTraps = trapCandidates
            .filter { it.lowercase() !in forbiddenLower }
            .distinctBy { it.lowercase() }
            .shuffled(random)
        if (validTraps.isNotEmpty()) {
            val trap = validTraps.first()
            distractors += trap
            forbiddenLower += trap.lowercase()
        }

        // 2. Fill remaining distractors from other studied words in this quiz session first.
        val studiedCandidates = studiedWordPool
            .filter { it.lowercase() !in forbiddenLower }
            .shuffled(random)
        for (candidate in studiedCandidates) {
            if (distractors.size >= 3) break
            distractors += candidate
            forbiddenLower += candidate.lowercase()
        }

        // 3. Fall back to the wider vocabulary pool if the studied pool was small.
        if (distractors.size < 3) {
            val globalCandidates = globalWordPool
                .filter { it.lowercase() !in forbiddenLower }
                .shuffled(random)
            for (candidate in globalCandidates) {
                if (distractors.size >= 3) break
                distractors += candidate
                forbiddenLower += candidate.lowercase()
            }
        }

        return (distractors.take(3) + raw.correctAnswer).shuffled(random)
    }
}

/**
 * Action emitted by [VocabularyMistakeTracker] as questions in a Vocabulary quiz session are resolved.
 */
sealed interface VocabularyMistakeAction {
    val wordKeys: Set<String>

    data object None : VocabularyMistakeAction {
        override val wordKeys: Set<String> = emptySet()
    }

    data class SaveMistake(override val wordKeys: Set<String>) : VocabularyMistakeAction
    data class DeleteMastered(override val wordKeys: Set<String>) : VocabularyMistakeAction
}

/**
 * Pure-Kotlin per-session tracker that monitors every question belonging to each studied [VocabularyEntry] cluster:
 * - If any question for a word is answered wrong or times out, emits [VocabularyMistakeAction.SaveMistake] immediately
 *   so the word is saved locally even if the user leaves the quiz early.
 * - When the last question for a word in the quiz session is resolved, **all** questions for that word were answered
 *   correctly, and the session tested enough of that word's full cluster (all questions for clusters up to
 *   [MIN_CAPPED_MASTERY_QUESTIONS], or at least half of a larger cluster when capped by `questionCount`), emits
 *   [VocabularyMistakeAction.DeleteMastered] so the word is automatically removed from local mistake storage.
 */
class VocabularyMistakeTracker(
    private val questions: List<VocabularyQuizItem>,
    studyEntries: List<VocabularyEntry> = emptyList(),
    modes: Set<VocabularyQuizMode> = VocabularyQuizMode.DEFAULT,
    initialOutcomes: Map<Int, FeedbackType> = emptyMap(),
) {
    private val questionIndicesByCluster: Map<String, List<Int>> =
        questions.indices.groupBy { index -> questions[index].clusterId }

    private val requiredCorrectByCluster: Map<String, Int> = buildMap {
        studyEntries.forEachIndexed { index, entry ->
            val clean = entry.sanitized()
            val main = clean.wordDetails.word.trim()
            if (main.isNotEmpty()) {
                val clusterId = clean.id.ifBlank { "cluster_${index}_${main.lowercase()}" }
                val fullCount = VocabularyQuizEngine.fullQuestionCountFor(clean, modes)
                if (fullCount > 0) {
                    val required = minOf(
                        fullCount,
                        maxOf(MIN_CAPPED_MASTERY_QUESTIONS, (fullCount + 1) / 2),
                    )
                    put(clusterId, required)
                }
            }
        }
    }

    private val keysByCluster: Map<String, Set<String>> = buildMap {
        studyEntries.forEachIndexed { index, entry ->
            val clean = entry.sanitized()
            val main = clean.wordDetails.word.trim()
            if (main.isNotEmpty()) {
                val clusterId = clean.id.ifBlank { "cluster_${index}_${main.lowercase()}" }
                put(clusterId, VocabularyQuizRules.entryMistakeKeys(clean))
            }
        }
        for (item in questions) {
            val existing = get(item.clusterId).orEmpty()
            if (existing.isEmpty()) {
                val fallback = buildSet {
                    if (item.clusterId.isNotBlank()) {
                        add(VocabularyQuizRules.mistakeKey(item.clusterId))
                    } else if (item.clusterWord.isNotBlank()) {
                        add(
                            VocabularyRules.wordIdentityKey(
                                WordDetails(word = item.clusterWord, partOfSpeech = item.clusterPartOfSpeech),
                            ),
                        )
                    }
                }
                if (fallback.isNotEmpty()) put(item.clusterId, fallback)
            }
        }
    }

    private val outcomesByIndex = LinkedHashMap<Int, FeedbackType>(initialOutcomes)

    /** Snapshot of resolved question outcomes so quiz progress can be saved and restored across process death. */
    fun outcomesSnapshot(): Map<Int, FeedbackType> = LinkedHashMap(outcomesByIndex)

    fun onQuestionResolved(questionIndex: Int, outcome: FeedbackType): VocabularyMistakeAction {
        val question = questions.getOrNull(questionIndex) ?: return VocabularyMistakeAction.None
        outcomesByIndex[questionIndex] = outcome
        val clusterId = question.clusterId
        val keys = keysByCluster[clusterId].orEmpty()
        if (keys.isEmpty()) return VocabularyMistakeAction.None

        if (outcome == FeedbackType.Incorrect || outcome == FeedbackType.Timeout) {
            return VocabularyMistakeAction.SaveMistake(keys)
        }

        val clusterIndices = questionIndicesByCluster[clusterId].orEmpty()
        val allResolved = clusterIndices.isNotEmpty() && clusterIndices.all { it in outcomesByIndex }
        if (!allResolved) return VocabularyMistakeAction.None

        val requiredCount = requiredCorrectByCluster[clusterId] ?: clusterIndices.size
        val hasEnoughCoverage = clusterIndices.size >= requiredCount
        val allCorrect = clusterIndices.all { outcomesByIndex[it] == FeedbackType.Correct }
        return if (allCorrect && hasEnoughCoverage) {
            VocabularyMistakeAction.DeleteMastered(keys)
        } else {
            VocabularyMistakeAction.None
        }
    }

    companion object {
        /** Minimum number of tested questions required to clear a weak word when `questionCount` caps its cluster. */
        const val MIN_CAPPED_MASTERY_QUESTIONS = 3
    }
}
