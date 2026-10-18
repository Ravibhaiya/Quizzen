package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.NoVocabularySessionStore
import com.ravibhaiya.quizzen.data.QuizSettingsRepository
import com.ravibhaiya.quizzen.data.VocabularySessionStore
import com.ravibhaiya.quizzen.data.quizSettingsRepository
import com.ravibhaiya.quizzen.data.vocabularySessionStore
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
    /** An unfinished quiz kept on the device (the app was closed or left mid-quiz), or null when there is none. */
    val resume: VocabularyResumeInfo? = null,
    /** False until the last-used setting (and any unfinished quiz) has been read from the device. */
    val loaded: Boolean = false,
)

class VocabularyConfigViewModel(
    private val repository: QuizSettingsRepository,
    private val sessionStore: VocabularySessionStore = NoVocabularySessionStore,
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
            val resume = readSavedQuiz()
            _state.update { it.copy(resume = resume, loaded = true) }
        }
    }

    /** Looks again for an unfinished quiz; the screen calls it whenever it comes back to the front (e.g. after a quiz). */
    fun refreshSavedQuiz() {
        viewModelScope.launch {
            val resume = readSavedQuiz()
            _state.update { if (it.loaded) it.copy(resume = resume) else it }
        }
    }

    /** Throws the unfinished quiz away ("Discard"): the next quiz starts from the setup below. */
    fun discardSavedQuiz() {
        sessionStore.clear()
        _state.update { it.copy(resume = null) }
    }

    /** An unreadable stored quiz is dropped, so it can never block the screen. */
    private suspend fun readSavedQuiz(): VocabularyResumeInfo? {
        val text = sessionStore.load() ?: return null
        val info = VocabularyQuizSavedState.summarize(text)
        if (info == null) sessionStore.clear()
        return info
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
            initializer { VocabularyConfigViewModel(quizSettingsRepository(), vocabularySessionStore()) }
        }
    }
}
