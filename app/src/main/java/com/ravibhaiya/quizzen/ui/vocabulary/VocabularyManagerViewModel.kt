package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.DecisionPendingRetryException
import com.ravibhaiya.quizzen.data.GeminiDecisionClient
import com.ravibhaiya.quizzen.data.VocabularyDecisionClient
import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.data.geminiDecisionClient
import com.ravibhaiya.quizzen.data.vocabularyRepository
import com.ravibhaiya.quizzen.domain.DecisionDuplicateKind
import com.ravibhaiya.quizzen.domain.VocabularyDecisionFinding
import com.ravibhaiya.quizzen.domain.VocabularyDecisionRules
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyJsonFormat
import com.ravibhaiya.quizzen.domain.VocabularyRules
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FocusedRelationAction {
    EDIT_WORD,
    MOVE_TO_OPPOSITE_LIST,
    REMOVE_WORD,
    FULL_ENTRY_JSON,
}

data class FocusedRelationEditorContext(
    val parentEntry: VocabularyEntry,
    val target: VocabularyRules.ConflictingRelationTarget,
    val action: FocusedRelationAction = FocusedRelationAction.EDIT_WORD,
)

data class VocabularyEditorState(
    val editingId: String? = null,
    val editingWordLabel: String? = null,
    val createdAt: Long = 0L,
    val jsonText: String = "",
    val confirmText: String = "",
    val validationError: String? = null,
    val isSaving: Boolean = false,
    val focusedRelation: FocusedRelationEditorContext? = null,
) {
    val isEditing: Boolean get() = editingId != null
    val isFocusedWordMode: Boolean
        get() = focusedRelation != null && focusedRelation.action != FocusedRelationAction.FULL_ENTRY_JSON
    val canSave: Boolean
        get() = !isEditing || confirmText == VocabularyManagerViewModel.REQUIRED_EDIT_CONFIRM
}

/** Details of how one existing vocabulary word will be updated if the user approves the merge. */
data class WordMergeItem(
    val existingWord: String,
    val incomingEntry: VocabularyEntry,
    val addedSynonyms: List<String>,
    val addedAntonyms: List<String>,
    val enrichedRelations: Int = 0,
    val mainDetailsEnriched: Boolean = false,
    val limitReached: Boolean = false,
    val hasChanges: Boolean = true,
) {
    val newSynonyms: Int get() = addedSynonyms.size
    val newAntonyms: Int get() = addedAntonyms.size
}

/**
 * Shown whenever one or more incoming words already exist in the database, or when the user chooses to
 * review duplicate database documents. Nothing is written to the database until the user types `CONFIRM`
 * in capital letters and approves.
 */
data class MergePrompt(
    val items: List<WordMergeItem>,
    val newEntriesToSave: List<VocabularyEntry> = emptyList(),
    val redundantDuplicateIdsToDelete: Set<String> = emptySet(),
    val confirmText: String = "",
    val isSaving: Boolean = false,
) {
    val incoming: VocabularyEntry get() = items.first().incomingEntry
    val existingWord: String get() = items.first().existingWord
    val newSynonyms: Int get() = items.sumOf { it.newSynonyms }
    val newAntonyms: Int get() = items.sumOf { it.newAntonyms }
    val enrichedRelations: Int get() = items.sumOf { it.enrichedRelations }
    val changedItems: List<WordMergeItem> get() = items.filter { it.hasChanges }
    val canConfirm: Boolean
        get() = confirmText == VocabularyManagerViewModel.REQUIRED_EDIT_CONFIRM
}

/**
 * Represents a database-level item (for example when words are added directly to Firestore by an external AI
 * or flagged by `gemini-decision-1`) that requires the user's explicit review and approval in the Database
 * Manager notification sheet.
 */
sealed interface DatabaseApprovalRequest {
    val id: String
    val word: String

    /**
     * Multiple documents exist in the database for the same normalized main word, or `gemini-decision-1`
     * flagged a root/inflected form or spelling/typo variant.
     * Nothing is merged automatically; the user can review & approve with `CONFIRM` or delete duplicates with `DELETE`.
     */
    data class MergeDuplicateWord(
        val wordKey: String,
        override val word: String,
        val duplicateCount: Int,
        val mergeItem: WordMergeItem,
        val redundantEntries: List<VocabularyEntry>,
        val decisionKind: DecisionDuplicateKind? = null,
        val matchedWord: String? = null,
        val confidencePercent: Int? = null,
        val dismissKey: String? = null,
    ) : DatabaseApprovalRequest {
        override val id: String get() = "merge_$wordKey"
    }

    /**
     * A database document has internal relation conflicts (e.g. duplicate synonym/antonym, self-synonym, or
     * a synonym/antonym meaning conflict flagged by `gemini-decision-1`) or is missing a required English meaning.
     */
    data class EntryIssue(
        val entry: VocabularyEntry,
        val reason: String,
        val conflictingWord: String? = null,
        val isAiConflict: Boolean = false,
        val confidencePercent: Int? = null,
        val dismissKey: String? = null,
    ) : DatabaseApprovalRequest {
        override val id: String get() = if (isAiConflict) "ai_issue_${entry.id}" else "issue_${entry.id}"
        override val word: String get() = entry.wordDetails.word
    }
}

data class VocabularyUiState(
    val entries: List<VocabularyEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val expandedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isNotificationsOpen: Boolean = false,
    val isDecisionRunning: Boolean = false,
    val isDecisionPendingRetry: Boolean = false,
    val decisionRetryRemainingSeconds: Int = 0,
    val decisionError: String? = null,
    val decisionFindings: List<VocabularyDecisionFinding> = emptyList(),
    val dismissedDecisionKeys: Set<String> = emptySet(),
    val editorState: VocabularyEditorState? = null,
    val pendingDelete: List<VocabularyEntry> = emptyList(),
    val deleteConfirmText: String = "",
    val mergePrompt: MergePrompt? = null,
    val noticeMessage: String? = null,
    val errorMessage: String? = null,
) {
    val filteredEntries: List<VocabularyEntry>
        get() = entries.filter { VocabularyRules.matchesSearch(it, searchQuery) }

    val allFilteredSelected: Boolean
        get() = filteredEntries.isNotEmpty() && filteredEntries.all { it.id in selectedIds }

    val canConfirmDelete: Boolean
        get() = pendingDelete.isNotEmpty() && deleteConfirmText == VocabularyManagerViewModel.REQUIRED_DELETE_CONFIRM

    val duplicateDatabaseWords: List<String>
        get() {
            val validIds = entries.map { it.id }.toSet()
            return decisionFindings
                .filterIsInstance<VocabularyDecisionFinding.DuplicateOrVariant>()
                .filter { it.primaryId in validIds && it.duplicateId in validIds }
                .mapNotNull { finding ->
                    entries.firstOrNull { it.id == finding.primaryId }
                        ?.let { VocabularyRules.wordKey(it.wordDetails.word) }
                        ?.takeIf { it.isNotEmpty() }
                }
                .distinct()
        }

    val pendingApprovalRequests: List<DatabaseApprovalRequest>
        get() = buildDatabaseApprovalRequests(entries, decisionFindings, dismissedDecisionKeys)

    val pendingApprovalCount: Int
        get() = pendingApprovalRequests.size
}

/**
 * Builds Approval Requests exclusively from [decisionFindings] returned by `gemini-decision-1`
 * (zero offline/local heuristic checks).
 */
internal fun buildDatabaseApprovalRequests(
    entries: List<VocabularyEntry>,
    decisionFindings: List<VocabularyDecisionFinding> = emptyList(),
    dismissedDecisionKeys: Set<String> = emptySet(),
): List<DatabaseApprovalRequest> {
    if (decisionFindings.isEmpty()) return emptyList()
    val requests = mutableListOf<DatabaseApprovalRequest>()
    val entryById = entries.filter { it.id.isNotBlank() }.associateBy { it.id }

    for (finding in decisionFindings) {
        if (finding.key in dismissedDecisionKeys) continue
        when (finding) {
            is VocabularyDecisionFinding.DuplicateOrVariant -> {
                val primary = entryById[finding.primaryId] ?: continue
                val dup = entryById[finding.duplicateId] ?: continue
                val res = VocabularyRules.mergeRelations(primary, dup)
                val mergeItem = WordMergeItem(
                    existingWord = primary.wordDetails.word,
                    incomingEntry = res.entry.copy(id = primary.id),
                    addedSynonyms = res.addedSynonymWords,
                    addedAntonyms = res.addedAntonymWords,
                    enrichedRelations = res.enrichedRelations,
                    mainDetailsEnriched = res.mainDetailsEnriched,
                    limitReached = res.limitReached,
                    hasChanges = res.changed,
                )
                val isSameSurfaceWord = primary.wordDetails.word.trim()
                    .equals(dup.wordDetails.word.trim(), ignoreCase = true)
                requests += DatabaseApprovalRequest.MergeDuplicateWord(
                    wordKey = finding.key,
                    word = primary.wordDetails.word,
                    duplicateCount = 2,
                    mergeItem = mergeItem,
                    redundantEntries = listOf(dup),
                    decisionKind = finding.kind,
                    matchedWord = if (isSameSurfaceWord) null else dup.wordDetails.word,
                    confidencePercent = finding.confidencePercent,
                    dismissKey = if (finding.kind == DecisionDuplicateKind.EXACT_DUPLICATE && !res.changed) {
                        null
                    } else {
                        finding.key
                    },
                )
            }
            is VocabularyDecisionFinding.MeaningConflict -> {
                val entry = entryById[finding.entryId] ?: continue
                val target = if (finding.customConflictLabel != null) {
                    finding.conflictingTarget
                } else {
                    finding.conflictingTarget ?: defaultEntryConflictTarget(entry)
                }
                val conflictingWord = finding.conflictingWord ?: target?.conflictingWordLabel
                val reason = finding.conflictReason ?: target?.reason
                    ?: "Gemini Decision 1 flagged a definition meaning conflict in \"${entry.wordDetails.word.trim()}\"."
                requests += DatabaseApprovalRequest.EntryIssue(
                    entry = entry,
                    reason = reason,
                    conflictingWord = conflictingWord,
                    isAiConflict = true,
                    confidencePercent = finding.confidencePercent,
                    dismissKey = finding.key,
                )
            }
        }
    }
    return requests
}

internal fun defaultEntryConflictTarget(
    entry: VocabularyEntry,
): VocabularyRules.ConflictingRelationTarget? {
    val mainWord = entry.wordDetails.word.trim()
    val firstAnt = entry.antonyms.firstOrNull()
    if (firstAnt != null) {
        val w = firstAnt.word.trim()
        return VocabularyRules.ConflictingRelationTarget(
            isAntonym = true,
            index = 0,
            details = firstAnt,
            conflictingWordLabel = "Antonym \"$w\"",
            reason = "Gemini Decision 1 flagged a meaning conflict in \"$mainWord\" on antonym \"$w\".",
        )
    }
    val firstSyn = entry.synonyms.firstOrNull()
    if (firstSyn != null) {
        val w = firstSyn.word.trim()
        return VocabularyRules.ConflictingRelationTarget(
            isAntonym = false,
            index = 0,
            details = firstSyn,
            conflictingWordLabel = "Synonym \"$w\"",
            reason = "Gemini Decision 1 flagged a meaning conflict in \"$mainWord\" on synonym \"$w\".",
        )
    }
    return null
}

class VocabularyManagerViewModel(
    private val repository: VocabularyRepository,
    private val decisionClient: VocabularyDecisionClient = GeminiDecisionClient(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()
    private var lastAnalyzedSignature: Int? = null
    private var lastAnalyzedEntryDigests: Map<String, Int> = emptyMap()
    private var pendingRetryJob: Job? = null

    init {
        viewModelScope.launch {
            repository.isLoading.collect { loading ->
                _uiState.update { it.copy(isLoading = loading) }
            }
        }
        viewModelScope.launch {
            repository.entries.collect { list ->
                val validIds = list.map { it.id }.toSet()
                val currentDigests = list.associate { entry ->
                    entry.id to Triple(entry.wordDetails, entry.synonyms, entry.antonyms).hashCode()
                }
                val onlyDeletedEntries = lastAnalyzedEntryDigests.isNotEmpty() &&
                    currentDigests.isNotEmpty() &&
                    currentDigests.size < lastAnalyzedEntryDigests.size &&
                    currentDigests.all { (id, hash) -> lastAnalyzedEntryDigests[id] == hash }

                _uiState.update { state ->
                    val nextSelected = state.selectedIds.intersect(validIds)
                    val nextFindings = state.decisionFindings.filter { finding ->
                        when (finding) {
                            is VocabularyDecisionFinding.DuplicateOrVariant ->
                                finding.primaryId in validIds && finding.duplicateId in validIds
                            is VocabularyDecisionFinding.MeaningConflict ->
                                finding.entryId in validIds
                        }
                    }
                    state.copy(
                        entries = list,
                        selectedIds = nextSelected,
                        isSelectionMode = state.isSelectionMode && (state.selectedIds.isEmpty() || nextSelected.isNotEmpty()),
                        decisionFindings = nextFindings,
                    )
                }
                val sig = entriesSignature(list)
                if (onlyDeletedEntries) {
                    lastAnalyzedSignature = sig
                    lastAnalyzedEntryDigests = currentDigests
                } else if (_uiState.value.mergePrompt?.isSaving != true &&
                    !_uiState.value.isDecisionPendingRetry &&
                    decisionClient.isConfigured &&
                    list.isNotEmpty() &&
                    sig != lastAnalyzedSignature
                ) {
                    onRunGeminiDecisionCheck()
                }
            }
        }
        viewModelScope.launch {
            repository.syncError.collect { err ->
                _uiState.update { it.copy(errorMessage = err) }
            }
        }
        viewModelScope.launch {
            decisionClient.apiKeyChanges.collect { key ->
                val current = _uiState.value
                if (key.isNotBlank() && current.entries.isNotEmpty()) {
                    val sig = entriesSignature(current.entries)
                    if (sig != lastAnalyzedSignature || current.decisionError != null || current.isDecisionPendingRetry) {
                        onRunGeminiDecisionCheck()
                    }
                }
            }
        }
    }

    private fun entriesSignature(list: List<VocabularyEntry>): Int =
        list.map { Triple(it.id, it.wordDetails, it.synonyms to it.antonyms) }.hashCode()

    fun onOpenNotifications() {
        _uiState.update { it.copy(isNotificationsOpen = true) }
        val current = _uiState.value
        val sig = entriesSignature(current.entries)
        if (current.entries.isNotEmpty() &&
            sig != lastAnalyzedSignature &&
            !current.isDecisionRunning &&
            !current.isDecisionPendingRetry
        ) {
            onRunGeminiDecisionCheck()
        }
    }

    fun onDismissNotifications() {
        _uiState.update { it.copy(isNotificationsOpen = false) }
    }

    fun onRunGeminiDecisionCheck() {
        val current = _uiState.value
        if (current.isDecisionRunning || current.entries.isEmpty()) return
        pendingRetryJob?.cancel()
        pendingRetryJob = null
        val targetEntries = current.entries
        val targetSignature = entriesSignature(targetEntries)
        val targetDigests = targetEntries.associate { entry ->
            entry.id to Triple(entry.wordDetails, entry.synonyms, entry.antonyms).hashCode()
        }
        _uiState.update {
            it.copy(
                isDecisionRunning = true,
                isDecisionPendingRetry = false,
                decisionRetryRemainingSeconds = 0,
                decisionError = null,
            )
        }
        viewModelScope.launch {
            decisionClient.analyze(targetEntries, current.dismissedDecisionKeys)
                .onSuccess { findings ->
                    lastAnalyzedSignature = targetSignature
                    lastAnalyzedEntryDigests = targetDigests
                    _uiState.update { state ->
                        val validIds = state.entries.map { it.id }.toSet()
                        val filtered = findings.filter { finding ->
                            when (finding) {
                                is VocabularyDecisionFinding.DuplicateOrVariant ->
                                    finding.primaryId in validIds && finding.duplicateId in validIds
                                is VocabularyDecisionFinding.MeaningConflict ->
                                    finding.entryId in validIds
                            }
                        }
                        state.copy(
                            isDecisionRunning = false,
                            isDecisionPendingRetry = false,
                            decisionRetryRemainingSeconds = 0,
                            decisionFindings = filtered,
                            decisionError = null,
                        )
                    }
                    val latestEntries = _uiState.value.entries
                    if (latestEntries.isNotEmpty() && entriesSignature(latestEntries) != targetSignature) {
                        onRunGeminiDecisionCheck()
                    }
                }
                .onFailure { err ->
                    val retrySeconds = when {
                        err is DecisionPendingRetryException -> err.retrySeconds
                        isRetryableDecisionError(err.message.orEmpty()) ->
                            DecisionPendingRetryException.DEFAULT_RETRY_SECONDS
                        else -> 0
                    }
                    val message = err.message ?: "gemini-decision-1 check failed."
                    if (retrySeconds > 0) {
                        schedulePendingDecisionRetry(retrySeconds, message)
                    } else {
                        _uiState.update {
                            it.copy(
                                isDecisionRunning = false,
                                isDecisionPendingRetry = false,
                                decisionRetryRemainingSeconds = 0,
                                decisionError = message,
                            )
                        }
                    }
                }
        }
    }

    private fun isRetryableDecisionError(message: String): Boolean {
        val lower = message.lowercase()
        return "rate limit" in lower ||
            "quota" in lower ||
            "resource_exhausted" in lower ||
            "429" in lower ||
            "50k tpm" in lower ||
            "timed out" in lower ||
            "temporarily busy" in lower
    }

    private fun schedulePendingDecisionRetry(retrySeconds: Int, message: String) {
        pendingRetryJob?.cancel()
        val totalSeconds = retrySeconds.coerceAtLeast(1)
        _uiState.update {
            it.copy(
                isDecisionRunning = false,
                isDecisionPendingRetry = true,
                decisionRetryRemainingSeconds = totalSeconds,
                decisionError = message,
            )
        }
        pendingRetryJob = viewModelScope.launch {
            for (remaining in (totalSeconds - 1) downTo 1) {
                delay(1_000L)
                _uiState.update { state ->
                    if (!state.isDecisionPendingRetry) return@launch
                    state.copy(decisionRetryRemainingSeconds = remaining)
                }
            }
            delay(1_000L)
            if (_uiState.value.isDecisionPendingRetry) {
                onRunGeminiDecisionCheck()
            }
        }
    }

    fun onDismissDecisionFinding(dismissKey: String) {
        _uiState.update { state ->
            state.copy(dismissedDecisionKeys = state.dismissedDecisionKeys + dismissKey)
        }
    }

    fun onDismissDecisionError() {
        pendingRetryJob?.cancel()
        pendingRetryJob = null
        _uiState.update {
            it.copy(
                isDecisionPendingRetry = false,
                decisionRetryRemainingSeconds = 0,
                decisionError = null,
            )
        }
    }

    fun onToggleSearch() {
        _uiState.update { state ->
            if (state.isSearchActive) {
                state.copy(isSearchActive = false, searchQuery = "")
            } else {
                state.copy(isSearchActive = true)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onDismissError() {
        repository.clearSyncError()
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onToggleExpand(id: String) {
        _uiState.update { state ->
            val next = if (id in state.expandedIds) state.expandedIds - id else state.expandedIds + id
            state.copy(expandedIds = next)
        }
    }

    fun onToggleSelectionMode() {
        _uiState.update { state ->
            if (state.isSelectionMode) {
                state.copy(isSelectionMode = false, selectedIds = emptySet())
            } else {
                state.copy(isSelectionMode = true, selectedIds = emptySet())
            }
        }
    }

    fun onLongPressWord(id: String) {
        _uiState.update { state ->
            if (!state.isSelectionMode) {
                state.copy(
                    isSelectionMode = true,
                    selectedIds = setOf(id),
                )
            } else {
                val next = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
                state.copy(
                    isSelectionMode = next.isNotEmpty(),
                    selectedIds = next,
                )
            }
        }
    }

    fun onToggleSelectWord(id: String) {
        _uiState.update { state ->
            val next = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(
                isSelectionMode = next.isNotEmpty(),
                selectedIds = next,
            )
        }
    }

    fun onSelectAllFiltered() {
        _uiState.update { state ->
            val visibleIds = state.filteredEntries.map { it.id }.toSet()
            if (visibleIds.isEmpty()) return@update state
            val next = if (visibleIds.all { it in state.selectedIds }) {
                state.selectedIds - visibleIds
            } else {
                state.selectedIds + visibleIds
            }
            state.copy(
                isSelectionMode = true,
                selectedIds = next,
            )
        }
    }

    fun onStartAdd() {
        _uiState.update {
            it.copy(
                editorState = VocabularyEditorState(
                    editingId = null,
                    editingWordLabel = null,
                    jsonText = "",
                    confirmText = "",
                    validationError = null,
                ),
            )
        }
    }

    fun onStartEdit(entry: VocabularyEntry) {
        _uiState.update {
            it.copy(
                isNotificationsOpen = false,
                editorState = VocabularyEditorState(
                    editingId = entry.id,
                    editingWordLabel = entry.wordDetails.word,
                    createdAt = entry.createdAt,
                    jsonText = VocabularyJsonFormat.formatSingle(entry),
                    confirmText = "",
                    validationError = null,
                    focusedRelation = null,
                ),
            )
        }
    }

    /**
     * Opens the editor directly focused on the exact conflicting synonym or antonym inside [entry]
     * so the user can edit just those 4 fields, move it between synonyms/antonyms, or remove it without
     * scrolling through the entire parent entry JSON.
     */
    fun onStartEditIssue(entry: VocabularyEntry) {
        val finding = _uiState.value.decisionFindings
            .filterIsInstance<VocabularyDecisionFinding.MeaningConflict>()
            .firstOrNull { it.entryId == entry.id }
        if (finding != null && finding.conflictingTarget == null && finding.customConflictLabel != null) {
            onStartEdit(entry)
            return
        }
        val aiTarget = finding?.conflictingTarget
        val target = VocabularyRules.findEntryRelationConflictTarget(entry)
            ?: aiTarget
            ?: defaultEntryConflictTarget(entry)
        if (target == null) {
            onStartEdit(entry)
            return
        }
        _uiState.update {
            it.copy(
                isNotificationsOpen = false,
                editorState = VocabularyEditorState(
                    editingId = entry.id,
                    editingWordLabel = entry.wordDetails.word,
                    createdAt = entry.createdAt,
                    jsonText = VocabularyJsonFormat.formatWordDetails(target.details),
                    confirmText = "",
                    validationError = null,
                    focusedRelation = FocusedRelationEditorContext(
                        parentEntry = entry,
                        target = target,
                        action = FocusedRelationAction.EDIT_WORD,
                    ),
                ),
            )
        }
    }

    fun onSelectFocusedRelationAction(action: FocusedRelationAction) {
        _uiState.update { state ->
            val editor = state.editorState ?: return@update state
            val focused = editor.focusedRelation ?: return@update state
            if (focused.action == action) return@update state
            val updatedJson = when {
                action == FocusedRelationAction.FULL_ENTRY_JSON ->
                    VocabularyJsonFormat.formatSingle(focused.parentEntry)
                focused.action == FocusedRelationAction.FULL_ENTRY_JSON ->
                    VocabularyJsonFormat.formatWordDetails(focused.target.details)
                else -> editor.jsonText
            }
            state.copy(
                editorState = editor.copy(
                    jsonText = updatedJson,
                    validationError = null,
                    focusedRelation = focused.copy(action = action),
                ),
            )
        }
    }

    fun onDismissEditor() {
        // While a save is running the result must stay visible: closing now would hide a failure.
        _uiState.update { if (it.editorState?.isSaving == true) it else it.copy(editorState = null) }
    }

    fun onUpdateJsonText(json: String) {
        _uiState.update { state ->
            val editor = state.editorState ?: return@update state
            state.copy(
                editorState = editor.copy(
                    jsonText = json,
                    validationError = null,
                ),
            )
        }
    }

    fun onUpdateEditConfirmText(text: String) {
        _uiState.update { state ->
            val editor = state.editorState ?: return@update state
            state.copy(
                editorState = editor.copy(
                    confirmText = text,
                    validationError = null,
                ),
            )
        }
    }

    fun onInsertSampleTemplate() {
        _uiState.update { state ->
            val editor = state.editorState ?: return@update state
            state.copy(
                editorState = editor.copy(
                    jsonText = VocabularyJsonFormat.formatBulkTemplate(),
                    validationError = null,
                ),
            )
        }
    }

    fun onSaveEntry(): Boolean {
        val editor = _uiState.value.editorState ?: return false
        if (editor.isSaving) return false

        if (editor.isEditing && editor.confirmText != REQUIRED_EDIT_CONFIRM) {
            showEditorError(editor, "Type $REQUIRED_EDIT_CONFIRM in capital letters to save changes.")
            return false
        }

        val focused = editor.focusedRelation
        if (focused != null && focused.action != FocusedRelationAction.FULL_ENTRY_JSON) {
            return saveFocusedRelationEdit(editor, focused)
        }

        val parseResult = VocabularyJsonFormat.parse(
            jsonText = editor.jsonText,
            singleMode = editor.isEditing,
        )
        when (parseResult) {
            is VocabularyJsonFormat.ParseResult.Error -> {
                _uiState.update { state ->
                    state.copy(
                        editorState = editor.copy(validationError = parseResult.message),
                    )
                }
                return false
            }
            is VocabularyJsonFormat.ParseResult.Success -> {
                // 1. Refuse internal synonym/antonym duplicates or conflicts so nothing is dropped silently.
                for (entry in parseResult.entries) {
                    val relationConflict = VocabularyRules.findEntryRelationConflict(entry)
                    if (relationConflict != null) {
                        showEditorError(editor, relationConflict)
                        return false
                    }
                }

                // 2. Refuse repeated main words inside the same JSON input so nothing is combined automatically.
                val duplicatesInInput = VocabularyRules.findDuplicateMainWords(parseResult.entries)
                if (duplicatesInInput.isNotEmpty()) {
                    val listed = duplicatesInInput.joinToString(", ") { "\"$it\"" }
                    showEditorError(
                        editor,
                        "Duplicate word(s) in JSON input: $listed. Keep only one entry per word.",
                    )
                    return false
                }

                val existing = _uiState.value.entries
                val incoming = if (editor.isEditing) {
                    parseResult.entries.map { it.copy(id = editor.editingId.orEmpty()) }
                } else {
                    parseResult.entries
                }
                val check = VocabularyRules.checkMainWords(incoming, existing)

                // 3. Renaming a word onto another saved word is refused.
                if (editor.isEditing && check.clashing.isNotEmpty()) {
                    showEditorError(editor, "\"${check.skippedExisting.first()}\" already exists in your vocabulary.")
                    return false
                }

                val now = System.currentTimeMillis()
                val newEntriesToSave = if (editor.isEditing) {
                    val single = check.toSave.first()
                    listOf(
                        single.copy(
                            id = editor.editingId.orEmpty(),
                            createdAt = if (editor.createdAt > 0L) editor.createdAt else now,
                            updatedAt = now,
                        ).sanitized(),
                    )
                } else {
                    check.toSave.mapIndexed { index, parsed ->
                        parsed.copy(
                            createdAt = now + index,
                            updatedAt = now + index,
                        ).sanitized()
                    }
                }

                // 4. Adding 1 or more words where any word already exists: NEVER auto-merge!
                //    Inspect all clashing words and require explicit user approval first.
                if (!editor.isEditing && check.clashing.isNotEmpty()) {
                    val existingByKey = existing.associateBy { VocabularyRules.wordKey(it.wordDetails.word) }
                    val mergeItems = check.clashing.mapNotNull { clash ->
                        val key = VocabularyRules.wordKey(clash.wordDetails.word)
                        val target = existingByKey[key] ?: return@mapNotNull null
                        val preview = VocabularyRules.mergeRelations(target, clash)
                        WordMergeItem(
                            existingWord = target.wordDetails.word,
                            incomingEntry = clash,
                            addedSynonyms = preview.addedSynonymWords,
                            addedAntonyms = preview.addedAntonymWords,
                            enrichedRelations = preview.enrichedRelations,
                            mainDetailsEnriched = preview.mainDetailsEnriched,
                            limitReached = preview.limitReached,
                            hasChanges = preview.changed,
                        )
                    }
                    if (mergeItems.none { it.hasChanges }) {
                        val message = if (mergeItems.size == 1 && newEntriesToSave.isEmpty()) {
                            "\"${mergeItems.first().existingWord}\" already exists and there is nothing new to add."
                        } else {
                            val names = mergeItems.joinToString(", ") { "\"${it.existingWord}\"" }
                            "Already exists with nothing new to add: $names. Remove duplicate word(s) before saving."
                        }
                        showEditorError(editor, message)
                        return false
                    }
                    _uiState.update {
                        it.copy(
                            mergePrompt = MergePrompt(
                                items = mergeItems,
                                newEntriesToSave = newEntriesToSave,
                                confirmText = "",
                            ),
                        )
                    }
                    return false // editor stays open behind the approval sheet until user confirms
                }

                // 5. All words are brand-new (or a confirmed single-word edit): save to the database. Saving needs
                //    internet and the editor stays open until the server confirmed it, so nothing the user typed is
                //    lost when it fails.
                _uiState.update { state ->
                    state.copy(editorState = editor.copy(isSaving = true, validationError = null))
                }
                viewModelScope.launch {
                    repository.saveEntries(newEntriesToSave)
                        .onSuccess { saved ->
                            _uiState.update { state ->
                                state.copy(
                                    editorState = null,
                                    noticeMessage = describeSaved(saved.size, editor.isEditing),
                                )
                            }
                        }
                        .onFailure { err ->
                            failEditor(err.message ?: "Failed to save vocabulary to Firebase.")
                        }
                }
                return true
            }
        }
    }

    private fun saveFocusedRelationEdit(
        editor: VocabularyEditorState,
        focused: FocusedRelationEditorContext,
    ): Boolean {
        val parent = focused.parentEntry
        val target = focused.target
        val synonyms = parent.synonyms.toMutableList()
        val antonyms = parent.antonyms.toMutableList()

        val updatedDetails = if (focused.action == FocusedRelationAction.REMOVE_WORD) {
            target.details
        } else {
            VocabularyJsonFormat.parseWordDetails(editor.jsonText).getOrElse { err ->
                showEditorError(editor, err.message ?: "Invalid word JSON.")
                return false
            }
        }

        when (focused.action) {
            FocusedRelationAction.EDIT_WORD -> {
                if (target.isAntonym) {
                    if (target.index !in antonyms.indices) {
                        showEditorError(editor, "Target antonym could not be found in \"${parent.wordDetails.word}\".")
                        return false
                    }
                    antonyms[target.index] = updatedDetails
                } else {
                    if (target.index !in synonyms.indices) {
                        showEditorError(editor, "Target synonym could not be found in \"${parent.wordDetails.word}\".")
                        return false
                    }
                    synonyms[target.index] = updatedDetails
                }
            }
            FocusedRelationAction.MOVE_TO_OPPOSITE_LIST -> {
                if (target.isAntonym) {
                    if (target.index in antonyms.indices) antonyms.removeAt(target.index)
                    synonyms += updatedDetails
                } else {
                    if (target.index in synonyms.indices) synonyms.removeAt(target.index)
                    antonyms += updatedDetails
                }
            }
            FocusedRelationAction.REMOVE_WORD -> {
                if (target.isAntonym) {
                    if (target.index in antonyms.indices) antonyms.removeAt(target.index)
                } else {
                    if (target.index in synonyms.indices) synonyms.removeAt(target.index)
                }
            }
            FocusedRelationAction.FULL_ENTRY_JSON -> return false
        }

        val now = System.currentTimeMillis()
        val updatedEntry = parent.copy(
            id = editor.editingId.orEmpty().ifBlank { parent.id },
            synonyms = synonyms,
            antonyms = antonyms,
            createdAt = if (editor.createdAt > 0L) editor.createdAt else parent.createdAt,
            updatedAt = now,
        ).sanitized()

        val relationConflict = VocabularyRules.findEntryRelationConflict(updatedEntry)
        if (relationConflict != null) {
            showEditorError(editor, relationConflict)
            return false
        }

        val noticeText = when (focused.action) {
            FocusedRelationAction.EDIT_WORD ->
                "Updated \"${updatedDetails.word}\" in \"${updatedEntry.wordDetails.word}\"."
            FocusedRelationAction.MOVE_TO_OPPOSITE_LIST -> {
                val dest = if (target.isAntonym) "synonyms" else "antonyms"
                "Moved \"${updatedDetails.word}\" to $dest in \"${updatedEntry.wordDetails.word}\"."
            }
            FocusedRelationAction.REMOVE_WORD ->
                "Removed \"${target.word}\" from \"${updatedEntry.wordDetails.word}\"."
            FocusedRelationAction.FULL_ENTRY_JSON ->
                describeSaved(1, edited = true)
        }

        _uiState.update { state ->
            state.copy(editorState = editor.copy(isSaving = true, validationError = null))
        }
        viewModelScope.launch {
            repository.saveEntries(listOf(updatedEntry))
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            editorState = null,
                            noticeMessage = noticeText,
                        )
                    }
                }
                .onFailure { err ->
                    failEditor(err.message ?: "Failed to save vocabulary to Firebase.")
                }
        }
        return true
    }

    private fun showEditorError(editor: VocabularyEditorState, message: String) {
        _uiState.update { it.copy(editorState = editor.copy(validationError = message)) }
    }

    /** A save failed: keep the editor (and the typed JSON) open and show why. */
    private fun failEditor(message: String) {
        _uiState.update { state ->
            val current = state.editorState
            if (current != null) {
                state.copy(editorState = current.copy(isSaving = false, validationError = message))
            } else {
                state.copy(errorMessage = message)
            }
        }
    }

    /** A merge failed: close only the approval sheet and show why (inside the editor if it is open). */
    private fun failMerge(message: String) {
        _uiState.update { state ->
            val editor = state.editorState
            if (editor != null) {
                state.copy(mergePrompt = null, editorState = editor.copy(validationError = message))
            } else {
                state.copy(mergePrompt = null, errorMessage = message)
            }
        }
    }

    private fun describeSaved(count: Int, edited: Boolean): String =
        if (edited) "Word updated in the database." else "Added $count new word(s) to the database."

    private fun describeResult(added: Int, merge: VocabularyRules.MergeSummary?, deletedDuplicates: Int = 0): String =
        buildString {
            if (added > 0) append("Added $added new word(s). ")
            if (merge != null) {
                if (merge.wordsUpdated > 0) {
                    append("Updated ${merge.wordsUpdated} existing (")
                    append("synonyms +${merge.addedSynonyms}, antonyms +${merge.addedAntonyms}")
                    if (merge.enrichedRelations > 0) {
                        append(", filled details on ${merge.enrichedRelations}")
                    }
                    append("). ")
                } else if (deletedDuplicates == 0) {
                    append("Nothing new to add to existing words. ")
                }
                if (merge.limitReached) {
                    append("Some items were not added: a word holds at most ${VocabularyRules.MAX_RELATIONS} ")
                    append("synonyms or antonyms. ")
                }
            }
            if (deletedDuplicates > 0) {
                append("Removed $deletedDuplicates duplicate database entry(s).")
            }
        }.trim()

    fun onUpdateMergeConfirmText(text: String) {
        _uiState.update { state ->
            val prompt = state.mergePrompt ?: return@update state
            state.copy(mergePrompt = prompt.copy(confirmText = text))
        }
    }

    fun onConfirmMerge(): Boolean {
        val prompt = _uiState.value.mergePrompt ?: return false
        if (!prompt.canConfirm || prompt.isSaving) return false
        // The approval sheet (and the editor behind it) stay open until the server confirmed everything.
        _uiState.update { it.copy(mergePrompt = prompt.copy(isSaving = true)) }
        viewModelScope.launch {
            if (prompt.newEntriesToSave.isNotEmpty()) {
                val saveFailure = repository.saveEntries(prompt.newEntriesToSave).exceptionOrNull()
                if (saveFailure != null) {
                    failMerge(saveFailure.message ?: "Failed to save vocabulary to Firebase.")
                    return@launch
                }
            }
            var summary: VocabularyRules.MergeSummary? = null
            val incomingToMerge = prompt.changedItems.map { it.incomingEntry }
            if (incomingToMerge.isNotEmpty()) {
                summary = repository.mergeIntoExisting(incomingToMerge).getOrElse { err ->
                    failMerge(err.message ?: "Failed to merge words.")
                    return@launch
                }
            }
            if (prompt.redundantDuplicateIdsToDelete.isNotEmpty()) {
                val deleteFailure = repository.deleteEntries(prompt.redundantDuplicateIdsToDelete).exceptionOrNull()
                if (deleteFailure != null) {
                    failMerge(deleteFailure.message ?: "Failed to clean up duplicate entries.")
                    return@launch
                }
            }
            val notice = describeResult(
                added = prompt.newEntriesToSave.size,
                merge = summary,
                deletedDuplicates = prompt.redundantDuplicateIdsToDelete.size,
            )
            if (prompt.newEntriesToSave.isEmpty()) {
                val postMergeEntries = _uiState.value.entries
                lastAnalyzedSignature = entriesSignature(postMergeEntries)
                lastAnalyzedEntryDigests = postMergeEntries.associate { entry ->
                    entry.id to Triple(entry.wordDetails, entry.synonyms, entry.antonyms).hashCode()
                }
            }
            _uiState.update {
                it.copy(
                    mergePrompt = null,
                    editorState = null,
                    noticeMessage = if (notice.isEmpty()) null else notice,
                )
            }
        }
        return true
    }

    fun onRequestMergeDatabaseDuplicates(targetWordKey: String? = null) {
        val mergeRequests = _uiState.value.pendingApprovalRequests
            .filterIsInstance<DatabaseApprovalRequest.MergeDuplicateWord>()
            .filter { it.mergeItem.hasChanges }
            .let { list ->
                if (targetWordKey != null) {
                    list.filter {
                        it.wordKey == targetWordKey || VocabularyRules.wordKey(it.word) == targetWordKey
                    }
                } else {
                    list
                }
            }
        if (mergeRequests.isEmpty()) return

        val mergeItems = mergeRequests.map { it.mergeItem }
        val redundantIds = mergeRequests
            .flatMap { req -> req.redundantEntries.map { it.id } }
            .filter { it.isNotBlank() }
            .toSet()

        _uiState.update {
            it.copy(
                isNotificationsOpen = false,
                mergePrompt = MergePrompt(
                    items = mergeItems,
                    newEntriesToSave = emptyList(),
                    redundantDuplicateIdsToDelete = redundantIds,
                    confirmText = "",
                ),
            )
        }
    }

    fun onRequestDeleteDuplicateEntries(entries: List<VocabularyEntry>) {
        if (entries.isEmpty()) return
        _uiState.update {
            it.copy(
                isNotificationsOpen = false,
                pendingDelete = entries,
                deleteConfirmText = "",
            )
        }
    }

    fun onDismissMerge() {
        _uiState.update { if (it.mergePrompt?.isSaving == true) it else it.copy(mergePrompt = null) }
    }

    fun onDismissNotice() {
        _uiState.update { it.copy(noticeMessage = null) }
    }

    fun onRequestDelete(entry: VocabularyEntry) {
        _uiState.update {
            it.copy(
                isNotificationsOpen = false,
                pendingDelete = listOf(entry),
                deleteConfirmText = "",
            )
        }
    }

    fun onRequestDeleteSelected() {
        _uiState.update { state ->
            val targets = state.entries.filter { it.id in state.selectedIds }
            if (targets.isEmpty()) return@update state
            state.copy(
                pendingDelete = targets,
                deleteConfirmText = "",
            )
        }
    }

    fun onUpdateDeleteConfirmText(text: String) {
        _uiState.update { it.copy(deleteConfirmText = text) }
    }

    fun onCancelDelete() {
        _uiState.update {
            it.copy(
                pendingDelete = emptyList(),
                deleteConfirmText = "",
            )
        }
    }

    fun onConfirmDelete(): Boolean {
        val current = _uiState.value
        val targets = current.pendingDelete
        if (targets.isEmpty() || current.deleteConfirmText != REQUIRED_DELETE_CONFIRM) {
            return false
        }
        val targetIds = targets.map { it.id }.toSet()
        _uiState.update { state ->
            state.copy(
                pendingDelete = emptyList(),
                deleteConfirmText = "",
                expandedIds = state.expandedIds - targetIds,
                selectedIds = state.selectedIds - targetIds,
                isSelectionMode = false,
            )
        }
        viewModelScope.launch {
            // The words leave the list only after the server confirmed the delete (it needs internet).
            repository.deleteEntries(targetIds)
                .onSuccess {
                    _uiState.update { it.copy(noticeMessage = "Deleted ${targetIds.size} word(s) from the database.") }
                }
                .onFailure { err ->
                    _uiState.update { state ->
                        state.copy(errorMessage = err.message ?: "Failed to delete word from Firebase.")
                    }
                }
        }
        return true
    }

    companion object {
        const val REQUIRED_DELETE_CONFIRM = "DELETE"
        const val REQUIRED_EDIT_CONFIRM = "CONFIRM"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { VocabularyManagerViewModel(vocabularyRepository(), geminiDecisionClient()) }
        }
    }
}
