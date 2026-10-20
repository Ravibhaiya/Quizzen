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
    val validationError: String? = null,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != null
}

/** A new entry whose main word already exists: shown as a "Merge" confirmation before adding only the new items. */
data class MergePrompt(
    val incoming: VocabularyEntry,
    val existingWord: String,
    val newSynonyms: Int,
    val newAntonyms: Int,
)

data class VocabularyUiState(
    val entries: List<VocabularyEntry> = emptyList(),
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val expandedIds: Set<String> = emptySet(),
    val editorState: VocabularyEditorState? = null,
    val pendingDelete: VocabularyEntry? = null,
    val mergePrompt: MergePrompt? = null,
    val noticeMessage: String? = null,
    val errorMessage: String? = null,
) {
    val filteredEntries: List<VocabularyEntry>
        get() = entries.filter { VocabularyRules.matchesSearch(it, searchQuery) }
}

class VocabularyManagerViewModel(
    private val repository: VocabularyRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabularyUiState())
    val uiState: StateFlow<VocabularyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.entries.collect { list ->
                _uiState.update { it.copy(entries = list) }
            }
        }
        viewModelScope.launch {
            repository.syncError.collect { err ->
                _uiState.update { it.copy(errorMessage = err) }
            }
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

    fun onStartAdd() {
        _uiState.update {
            it.copy(
                editorState = VocabularyEditorState(
                    editingId = null,
                    editingWordLabel = null,
                    jsonText = "",
                    validationError = null,
                ),
            )
        }
    }

    fun onStartEdit(entry: VocabularyEntry) {
        _uiState.update {
            it.copy(
                editorState = VocabularyEditorState(
                    editingId = entry.id,
                    editingWordLabel = entry.wordDetails.word,
                    createdAt = entry.createdAt,
                    jsonText = VocabularyJsonFormat.formatSingle(entry),
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
                val existing = _uiState.value.entries
                val incoming = if (editor.isEditing) {
                    parseResult.entries.map { it.copy(id = editor.editingId.orEmpty()) }
                } else {
                    parseResult.entries
                }
                val check = VocabularyRules.checkMainWords(incoming, existing)

                // Renaming a word onto another saved word is refused.
                if (editor.isEditing && check.clashing.isNotEmpty()) {
                    showEditorError(editor, "\"${check.skippedExisting.first()}\" already exists in your vocabulary.")
                    return false
                }

                // Adding one word that already exists: offer to merge its new synonyms/antonyms, or explain.
                if (!editor.isEditing && incoming.size == 1 && check.clashing.isNotEmpty()) {
                    val clash = check.clashing.first()
                    val key = VocabularyRules.wordKey(clash.wordDetails.word)
                    val target = existing.firstOrNull { VocabularyRules.wordKey(it.wordDetails.word) == key }
                        ?: return false
                    val preview = VocabularyRules.mergeRelations(target, clash)
                    if (!preview.changed) {
                        showEditorError(
                            editor,
                            "\"${target.wordDetails.word}\" already exists and there is nothing new to add.",
                        )
                        return false
                    }
                    _uiState.update {
                        it.copy(
                            mergePrompt = MergePrompt(
                                incoming = clash,
                                existingWord = target.wordDetails.word,
                                newSynonyms = preview.addedSynonyms,
                                newAntonyms = preview.addedAntonyms,
                            ),
                        )
                    }
                    return false // the editor stays open behind the merge sheet
                }

                val now = System.currentTimeMillis()
                val entriesToSave = if (editor.isEditing) {
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

                // Close the editor sheet; cards remain closed unless the user taps the expand arrow.
                _uiState.update { it.copy(editorState = null, noticeMessage = null) }
                viewModelScope.launch {
                    if (entriesToSave.isNotEmpty()) {
                        repository.saveEntries(entriesToSave).getOrElse { err ->
                            _uiState.update { state ->
                                state.copy(
                                    errorMessage = err.message ?: "Failed to save vocabulary to Firebase.",
                                )
                            }
                            return@launch
                        }
                    }
                    // Bulk import: words that already exist get only their new synonyms/antonyms added.
                    var summary: VocabularyRules.MergeSummary? = null
                    if (check.clashing.isNotEmpty()) {
                        summary = repository.mergeIntoExisting(check.clashing).getOrElse { err ->
                            _uiState.update { it.copy(errorMessage = err.message ?: "Failed to merge words.") }
                            return@launch
                        }
                    }
                    if (check.mergedRepeats > 0 || summary != null) {
                        val notice = describeResult(entriesToSave.size, check.mergedRepeats, summary)
                        _uiState.update { it.copy(noticeMessage = notice) }
                    }
                }
                return true
            }
        }
    }

    private fun showEditorError(editor: VocabularyEditorState, message: String) {
        _uiState.update { it.copy(editorState = editor.copy(validationError = message)) }
    }

    private fun describeResult(added: Int, repeats: Int, merge: VocabularyRules.MergeSummary?): String = buildString {
        if (added > 0) append("Added $added. ")
        if (repeats > 0) append("Repeated words in the list were combined ($repeats). ")
        if (merge != null) {
            if (merge.wordsUpdated > 0) {
                append("Updated ${merge.wordsUpdated} existing (")
                append("synonyms +${merge.addedSynonyms}, antonyms +${merge.addedAntonyms}). ")
            } else {
                append("Nothing new to add to existing words. ")
            }
            if (merge.limitReached) {
                append("Some items were not added: a word holds at most ${VocabularyRules.MAX_RELATIONS} ")
                append("synonyms or antonyms.")
            }
        }
    }.trim()

    fun onConfirmMerge() {
        val prompt = _uiState.value.mergePrompt ?: return
        _uiState.update { it.copy(mergePrompt = null, editorState = null, noticeMessage = null) }
        viewModelScope.launch {
            repository.mergeIntoExisting(listOf(prompt.incoming))
                .onSuccess { s -> _uiState.update { it.copy(noticeMessage = describeResult(0, 0, s)) } }
                .onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message ?: "Failed to merge words.") }
                }
        }
    }

    fun onDismissMerge() {
        _uiState.update { it.copy(mergePrompt = null) }
    }

    fun onDismissNotice() {
        _uiState.update { it.copy(noticeMessage = null) }
    }

    fun onRequestDelete(entry: VocabularyEntry) {
        _uiState.update { it.copy(pendingDelete = entry) }
    }

    fun onCancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun onConfirmDelete() {
        val target = _uiState.value.pendingDelete ?: return
        _uiState.update { state ->
            state.copy(
                pendingDelete = null,
                expandedIds = state.expandedIds - target.id,
            )
        }
        viewModelScope.launch {
            val result = repository.deleteEntry(target.id)
            result.onFailure { err ->
                _uiState.update { state ->
                    state.copy(
                        errorMessage = err.message ?: "Failed to delete word from Firebase.",
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { VocabularyManagerViewModel(vocabularyRepository()) }
        }
    }
}
