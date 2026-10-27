package com.ravibhaiya.quizzen.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.data.vocabularyRepository
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyJsonFormat
import com.ravibhaiya.quizzen.domain.VocabularyRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VocabularyEditorState(
    val editingId: String? = null,
    val editingWordLabel: String? = null,
    val createdAt: Long = 0L,
    val jsonText: String = "",
    val confirmText: String = "",
    val validationError: String? = null,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != null
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
 * Represents a database-level item (for example when words are added directly to Firestore by an external AI)
 * that requires the user's explicit review and approval in the Database Manager notification sheet.
 */
sealed interface DatabaseApprovalRequest {
    val id: String
    val word: String

    /**
     * Multiple documents exist in the database for the same normalized main word.
     * Nothing is merged automatically; the user can review & approve with `CONFIRM` or delete duplicates with `DELETE`.
     */
    data class MergeDuplicateWord(
        val wordKey: String,
        override val word: String,
        val duplicateCount: Int,
        val mergeItem: WordMergeItem,
        val redundantEntries: List<VocabularyEntry>,
    ) : DatabaseApprovalRequest {
        override val id: String get() = "merge_$wordKey"
    }

    /**
     * A database document has internal relation conflicts (e.g. duplicate synonym/antonym or self-synonym)
     * or is missing a required English meaning.
     */
    data class EntryIssue(
        val entry: VocabularyEntry,
        val reason: String,
    ) : DatabaseApprovalRequest {
        override val id: String get() = "issue_${entry.id}"
        override val word: String get() = entry.wordDetails.word
    }
}

data class VocabularyUiState(
    val entries: List<VocabularyEntry> = emptyList(),
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val expandedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isNotificationsOpen: Boolean = false,
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
        get() = VocabularyRules.findDuplicateMainWords(entries)

    val pendingApprovalRequests: List<DatabaseApprovalRequest>
        get() = buildDatabaseApprovalRequests(entries)

    val pendingApprovalCount: Int
        get() = pendingApprovalRequests.size
}

internal fun buildDatabaseApprovalRequests(entries: List<VocabularyEntry>): List<DatabaseApprovalRequest> {
    val requests = mutableListOf<DatabaseApprovalRequest>()
    val grouped = entries.groupBy { VocabularyRules.wordKey(it.wordDetails.word) }
        .filter { (key, list) -> key.isNotEmpty() && list.size > 1 }

    for ((key, group) in grouped) {
        val sortedGroup = group.sortedBy { entry ->
            when {
                entry.id == VocabularyRules.documentIdForWord(entry.wordDetails.word) -> 0
                entry.id.startsWith("seed_") -> 1
                else -> 2
            }
        }
        val primary = sortedGroup.first()
        val duplicates = sortedGroup.drop(1)
        var acc = primary
        var combinedSyn = emptyList<String>()
        var combinedAnt = emptyList<String>()
        var enrichedRel = 0
        var mainEnriched = false
        var limit = false
        var anyChanged = false
        for (dup in duplicates) {
            val res = VocabularyRules.mergeRelations(acc, dup)
            acc = res.entry
            combinedSyn = combinedSyn + res.addedSynonymWords
            combinedAnt = combinedAnt + res.addedAntonymWords
            enrichedRel += res.enrichedRelations
            mainEnriched = mainEnriched || res.mainDetailsEnriched
            limit = limit || res.limitReached
            anyChanged = anyChanged || res.changed
        }
        val mergeItem = WordMergeItem(
            existingWord = primary.wordDetails.word,
            incomingEntry = acc.copy(id = primary.id),
            addedSynonyms = combinedSyn,
            addedAntonyms = combinedAnt,
            enrichedRelations = enrichedRel,
            mainDetailsEnriched = mainEnriched,
            limitReached = limit,
            hasChanges = anyChanged,
        )
        requests += DatabaseApprovalRequest.MergeDuplicateWord(
            wordKey = key,
            word = primary.wordDetails.word,
            duplicateCount = group.size,
            mergeItem = mergeItem,
            redundantEntries = duplicates,
        )
    }

    for (entry in entries) {
        val conflict = VocabularyRules.findEntryRelationConflict(entry)
            ?: if (!VocabularyRules.canSave(entry.wordDetails, entry.synonyms, entry.antonyms)) {
                "Word \"${entry.wordDetails.word}\" is missing a required English meaning."
            } else {
                null
            }
        if (conflict != null) {
            requests += DatabaseApprovalRequest.EntryIssue(entry = entry, reason = conflict)
        }
    }
    return requests
}

class VocabularyManagerViewModel(
    private val repository: VocabularyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.entries.collect { list ->
                val validIds = list.map { it.id }.toSet()
                _uiState.update { state ->
                    val nextSelected = state.selectedIds.intersect(validIds)
                    state.copy(
                        entries = list,
                        selectedIds = nextSelected,
                        isSelectionMode = state.isSelectionMode && (state.selectedIds.isEmpty() || nextSelected.isNotEmpty()),
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.syncError.collect { err ->
                _uiState.update { it.copy(errorMessage = err) }
            }
        }
    }

    fun onOpenNotifications() {
        _uiState.update { it.copy(isNotificationsOpen = true) }
    }

    fun onDismissNotifications() {
        _uiState.update { it.copy(isNotificationsOpen = false) }
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
                ),
            )
        }
    }

    fun onDismissEditor() {
        _uiState.update { it.copy(editorState = null) }
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

                // 5. All words are brand-new (or a confirmed single-word edit): save to database.
                _uiState.update { it.copy(editorState = null, noticeMessage = null) }
                viewModelScope.launch {
                    if (newEntriesToSave.isNotEmpty()) {
                        repository.saveEntries(newEntriesToSave).getOrElse { err ->
                            _uiState.update { state ->
                                state.copy(
                                    errorMessage = err.message ?: "Failed to save vocabulary to Firebase.",
                                )
                            }
                            return@launch
                        }
                    }
                }
                return true
            }
        }
    }

    private fun showEditorError(editor: VocabularyEditorState, message: String) {
        _uiState.update { it.copy(editorState = editor.copy(validationError = message)) }
    }

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
        if (!prompt.canConfirm) return false
        _uiState.update { it.copy(mergePrompt = null, editorState = null, noticeMessage = null) }
        viewModelScope.launch {
            if (prompt.newEntriesToSave.isNotEmpty()) {
                repository.saveEntries(prompt.newEntriesToSave).getOrElse { err ->
                    _uiState.update { state ->
                        state.copy(errorMessage = err.message ?: "Failed to save vocabulary to Firebase.")
                    }
                    return@launch
                }
            }
            var summary: VocabularyRules.MergeSummary? = null
            val incomingToMerge = prompt.changedItems.map { it.incomingEntry }
            if (incomingToMerge.isNotEmpty()) {
                summary = repository.mergeIntoExisting(incomingToMerge).getOrElse { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: "Failed to merge words.") }
                    return@launch
                }
            }
            if (prompt.redundantDuplicateIdsToDelete.isNotEmpty()) {
                repository.deleteEntries(prompt.redundantDuplicateIdsToDelete).getOrElse { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: "Failed to clean up duplicate entries.") }
                    return@launch
                }
            }
            val notice = describeResult(
                added = prompt.newEntriesToSave.size,
                merge = summary,
                deletedDuplicates = prompt.redundantDuplicateIdsToDelete.size,
            )
            if (notice.isNotEmpty()) {
                _uiState.update { it.copy(noticeMessage = notice) }
            }
        }
        return true
    }

    fun onRequestMergeDatabaseDuplicates(targetWordKey: String? = null) {
        val mergeRequests = _uiState.value.pendingApprovalRequests
            .filterIsInstance<DatabaseApprovalRequest.MergeDuplicateWord>()
            .filter { it.mergeItem.hasChanges }
            .let { list ->
                if (targetWordKey != null) list.filter { it.wordKey == targetWordKey } else list
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
        _uiState.update { it.copy(mergePrompt = null) }
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
            val result = repository.deleteEntries(targetIds)
            result.onFailure { err ->
                _uiState.update { state ->
                    state.copy(
                        errorMessage = err.message ?: "Failed to delete word from Firebase.",
                    )
                }
            }
        }
        return true
    }

    companion object {
        const val REQUIRED_DELETE_CONFIRM = "DELETE"
        const val REQUIRED_EDIT_CONFIRM = "CONFIRM"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { VocabularyManagerViewModel(vocabularyRepository()) }
        }
    }
}
