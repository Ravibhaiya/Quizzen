package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.QuizSettingsRepository
import com.ravibhaiya.quizzen.data.quizSettingsRepository
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizMode
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.ui.components.TimerFieldState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VocabularyConfigUiState(
    /** Selected quiz modes (Synonyms, Antonyms, or both). Never empty. */
    val modes: Set<VocabularyQuizMode> = VocabularyQuizMode.DEFAULT,
    /** Number of words to study and attempt (5..25). */
    val wordCount: Int = VocabularyQuizRules.DEFAULT_WORDS,
    /** False until the last-used setting has been read from DataStore. */
    val loaded: Boolean = false,
)

class VocabularyConfigViewModel(
    private val repository: QuizSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VocabularyConfigUiState())
    val state: StateFlow<VocabularyConfigUiState> = _state.asStateFlow()

    val timer = TimerFieldState(viewModelScope, defaultSeconds = DEFAULT_TIMER_SECONDS)

    private var edited = false

    init {
        viewModelScope.launch {
            val saved = repository.loadVocabulary(DEFAULT_TIMER_SECONDS)
            if (saved != null && !edited) {
                _state.update {
                    it.copy(
                        modes = saved.modes,
                        wordCount = VocabularyQuizRules.coerceWordCount(saved.wordCount),
                    )
                }
                timer.set(saved.timerSeconds)
            }
            _state.update { it.copy(loaded = true) }
        }
    }

    /** Toggles [mode] while ensuring at least one mode remains selected. */
    fun toggleMode(mode: VocabularyQuizMode) {
        edited = true
        _state.update { state ->
            val chosen = state.modes
            when {
                mode !in chosen -> state.copy(modes = chosen + mode)
                chosen.size > 1 -> state.copy(modes = chosen - mode)
                else -> state
            }
        }
    }

    /** Updates the number of words to attempt, clamped to `5..25`. */
    fun onWordCountChanged(count: Int) {
        edited = true
        _state.update { it.copy(wordCount = VocabularyQuizRules.coerceWordCount(count)) }
    }

    fun onTimerChanged(raw: String) {
        edited = true
        timer.onTextChanged(raw)
    }

    fun buildConfig(): VocabularyQuizConfig {
        val current = _state.value
        return VocabularyQuizConfig(
            modes = current.modes,
            wordCount = current.wordCount,
            timerSeconds = timer.resolvedSeconds(),
        )
    }

    fun startQuiz(): VocabularyQuizConfig {
        val config = buildConfig()
        viewModelScope.launch { repository.saveVocabulary(config) }
        return config
    }

    companion object {
        const val DEFAULT_TIMER_SECONDS = VocabularyQuizRules.DEFAULT_TIMER_SECONDS

        val Factory = viewModelFactory {
            initializer { VocabularyConfigViewModel(quizSettingsRepository()) }
        }
    }
}
