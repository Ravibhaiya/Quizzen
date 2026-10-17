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

data class VocabularyUiState(
    val entries: List<VocabularyEntry> = emptyList(),
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val expandedIds: Set<String> = emptySet(),
    val editorState: VocabularyEditorState? = null,
    val pendingDelete: VocabularyEntry? = null,
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
                val now = System.currentTimeMillis()
                val entriesToSave = if (editor.isEditing) {
                    val single = parseResult.entries.first()
                    listOf(
                        single.copy(
                            id = editor.editingId.orEmpty(),
                            createdAt = if (editor.createdAt > 0L) editor.createdAt else now,
                            updatedAt = now,
                        ).sanitized(),
                    )
                } else {
                    parseResult.entries.mapIndexed { index, parsed ->
                        parsed.copy(
                            createdAt = now + index,
                            updatedAt = now + index,
                        ).sanitized()
                    }
                }

                // Close the editor sheet; cards remain closed unless the user taps the expand arrow.
                _uiState.update { it.copy(editorState = null) }
                viewModelScope.launch {
                    val result = repository.saveEntries(entriesToSave)
                    result.onFailure { err ->
                        _uiState.update { state ->
                            state.copy(
                                errorMessage = err.message ?: "Failed to save vocabulary to Firebase.",
                            )
                        }
                    }
                }
                return true
            }
        }
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
