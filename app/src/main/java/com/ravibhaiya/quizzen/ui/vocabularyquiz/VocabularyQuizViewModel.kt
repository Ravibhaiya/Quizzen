package com.ravibhaiya.quizzen.ui.vocabularyquiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.NoVocabularySessionStore
import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.data.VocabularySessionStore
import com.ravibhaiya.quizzen.data.vocabularyRepository
import com.ravibhaiya.quizzen.data.vocabularySessionStore
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyMistakeAction
import com.ravibhaiya.quizzen.domain.VocabularyMistakeTracker
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizEngine
import com.ravibhaiya.quizzen.domain.VocabularyQuizItem
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
import com.ravibhaiya.quizzen.ui.navigation.Routes
import com.ravibhaiya.quizzen.ui.navigation.VocabularyQuizArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class VocabularyQuizPhase {
    Study,
    Quiz,
    Finished,
}

data class VocabularyQuizUiState(
    val config: VocabularyQuizConfig,
    val phase: VocabularyQuizPhase = VocabularyQuizPhase.Study,
    val studyEntries: List<VocabularyEntry> = emptyList(),
    val questions: List<VocabularyQuizItem> = emptyList(),
    val currentIndex: Int = 0,
    val attemptedCount: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val timeoutCount: Int = 0,
    val selectedOption: String? = null,
    val remainingSeconds: Int = config.timerSeconds,
    val feedback: Feedback? = null,
    val isLocked: Boolean = false,
    val shakeCount: Int = 0,
    /** True while an unfinished quiz is being read from the device (resume); the screen shows nothing until it is ready. */
    val isRestoring: Boolean = false,
) {
    val currentQuestion: VocabularyQuizItem?
        get() = questions.getOrNull(currentIndex)

    val totalCount: Int
        get() = questions.size

    val isTimerLow: Boolean
        get() = remainingSeconds <= LOW_TIME_THRESHOLD_SECONDS

    val accuracyPercent: Int
        get() = if (totalCount == 0) 0 else ((correctCount * 100.0) / totalCount).toInt()

    companion object {
        const val LOW_TIME_THRESHOLD_SECONDS = 5
    }
}

class VocabularyQuizViewModel(
    private val config: VocabularyQuizConfig,
    private val repository: VocabularyRepository,
    private val random: Random = Random.Default,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val sessionStore: VocabularySessionStore = NoVocabularySessionStore,
    resumeSavedQuiz: Boolean = false,
) : ViewModel() {

    private var allEntries: List<VocabularyEntry> = emptyList()
    private var savedMistakeWords: Set<String> = emptySet()
    private val restoredSnapshot = VocabularyQuizSavedState.restore(savedStateHandle, config)
    private var usedFallbackForStudy: Boolean = restoredSnapshot == null
    private var mistakeTracker: VocabularyMistakeTracker? = restoredSnapshot?.let { snapshot ->
        if (snapshot.uiState.questions.isNotEmpty()) {
            VocabularyMistakeTracker(
                questions = snapshot.uiState.questions,
                studyEntries = snapshot.uiState.studyEntries,
                initialOutcomes = snapshot.outcomes,
            )
        } else {
            null
        }
    }

    private val _state = MutableStateFlow(
        restoredSnapshot?.uiState ?: VocabularyQuizUiState(
            config = config,
            studyEntries = VocabularyQuizRules.selectStudyWords(
                entries = emptyList(),
                modes = config.modes,
                wordCount = config.wordCount,
                random = random,
            ),
            remainingSeconds = config.timerSeconds,
            isRestoring = resumeSavedQuiz && restoredSnapshot == null,
        ),
    )
    val state: StateFlow<VocabularyQuizUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var inForeground = false

    init {
        if (_state.value.isRestoring) restoreFromDevice()
        viewModelScope.launch {
            combine(repository.entries, repository.mistakeWords) { repoEntries, repoMistakes ->
                repoEntries to repoMistakes
            }.collect { (repoEntries, repoMistakes) ->
                val hadUserEligible = allEntries.any { VocabularyQuizRules.isEligible(it, config.modes) }
                val previousMistakes = savedMistakeWords
                allEntries = repoEntries
                savedMistakeWords = repoMistakes
                    .map(VocabularyQuizRules::mistakeKey)
                    .filter { it.isNotEmpty() }
                    .toSet()
                val nowHasUserEligible = repoEntries.any { VocabularyQuizRules.isEligible(it, config.modes) }
                val mistakesChangedBeforeQuiz =
                    previousMistakes != savedMistakeWords && _state.value.questions.isEmpty()

                // If the study screen initially loaded before repository emissions arrived, refresh with real user words
                // and locally saved mistake words (unless restored from process death).
                if (_state.value.phase == VocabularyQuizPhase.Study &&
                    (((usedFallbackForStudy || !hadUserEligible) && nowHasUserEligible) ||
                        (mistakesChangedBeforeQuiz && usedFallbackForStudy))
                ) {
                    if (nowHasUserEligible) usedFallbackForStudy = false
                    updateAndPersistState {
                        it.copy(
                            studyEntries = VocabularyQuizRules.selectStudyWords(
                                entries = repoEntries,
                                modes = config.modes,
                                wordCount = config.wordCount,
                                random = random,
                                mistakeWords = savedMistakeWords,
                            ),
                        )
                    }
                }
            }
        }
    }

    /**
     * Continues the unfinished quiz kept on the device (Vocabulary only): same questions, same position, same score. If it
     * cannot be read any more it is dropped and the normal Study screen is shown instead. A question that was still open
     * starts again with a full timer, one that was already answered shows its result with **Next** ready.
     */
    private fun restoreFromDevice() {
        viewModelScope.launch {
            val snapshot = VocabularyQuizSavedState.decodeSession(sessionStore.load())
            if (snapshot == null) {
                sessionStore.clear()
                _state.update { it.copy(isRestoring = false) }
                return@launch
            }
            mistakeTracker = VocabularyMistakeTracker(
                questions = snapshot.uiState.questions,
                studyEntries = snapshot.uiState.studyEntries,
                initialOutcomes = snapshot.outcomes,
            )
            usedFallbackForStudy = false
            _state.value = snapshot.uiState.copy(config = config)
            persistSnapshot()
            if (inForeground && !snapshot.uiState.isLocked) startTicking()
        }
    }

    /** Picks a new set of words to read on the Study screen, prioritizing locally saved mistake words first. */
    fun shuffleStudyWords() {
        tickJob?.cancel()
        mistakeTracker = null
        val hasUserEligible = allEntries.any { VocabularyQuizRules.isEligible(it, config.modes) }
        usedFallbackForStudy = !hasUserEligible
        val nextStudy = VocabularyQuizRules.selectStudyWords(
            entries = allEntries,
            modes = config.modes,
            wordCount = config.wordCount,
            random = random,
            mistakeWords = savedMistakeWords,
        )
        updateAndPersistState {
            it.copy(
                phase = VocabularyQuizPhase.Study,
                studyEntries = nextStudy,
                questions = emptyList(),
                currentIndex = 0,
                attemptedCount = 0,
                correctCount = 0,
                wrongCount = 0,
                timeoutCount = 0,
                selectedOption = null,
                remainingSeconds = config.timerSeconds,
                feedback = null,
                isLocked = false,
            )
        }
    }

    /** Transitions from the Study screen to the active finite quiz. */
    fun startQuiz() {
        tickJob?.cancel()
        val study = _state.value.studyEntries.ifEmpty {
            VocabularyQuizRules.selectStudyWords(
                entries = allEntries,
                modes = config.modes,
                wordCount = config.wordCount,
                random = random,
                mistakeWords = savedMistakeWords,
            )
        }
        val generated = VocabularyQuizEngine.generateQuiz(
            studyEntries = study,
            allEntries = allEntries,
            modes = config.modes,
            random = random,
        )
        if (generated.isEmpty()) return

        mistakeTracker = VocabularyMistakeTracker(
            questions = generated,
            studyEntries = study,
        )

        updateAndPersistState {
            it.copy(
                phase = VocabularyQuizPhase.Quiz,
                studyEntries = study,
                questions = generated,
                currentIndex = 0,
                attemptedCount = 0,
                correctCount = 0,
                wrongCount = 0,
                timeoutCount = 0,
                selectedOption = null,
                remainingSeconds = config.timerSeconds,
                feedback = null,
                isLocked = false,
            )
        }
        persistSession()
        if (inForeground) startTicking()
    }

    /** Restarts the quiz using the same studied words in a fresh interleaved order. */
    fun retryQuiz() = startQuiz()

    /** Returns to the Study screen to review the selected words. */
    fun backToStudy() {
        tickJob?.cancel()
        updateAndPersistState {
            it.copy(
                phase = VocabularyQuizPhase.Study,
                selectedOption = null,
                feedback = null,
                isLocked = false,
                remainingSeconds = config.timerSeconds,
            )
        }
    }

    /** Evaluates the tapped 4-option multiple-choice card. */
    fun selectOption(option: String) {
        val current = _state.value
        if (current.phase != VocabularyQuizPhase.Quiz || current.isLocked) return
        val question = current.currentQuestion ?: return
        val isRight = question.isCorrect(option)
        resolveQuestion(
            type = if (isRight) FeedbackType.Correct else FeedbackType.Incorrect,
            selected = option,
        )
    }

    /** Advances to the next MCQ question (or finishes the quiz on the last question) when the Next button is clicked. */
    fun nextQuestion() {
        val current = _state.value
        if (current.phase != VocabularyQuizPhase.Quiz || !current.isLocked) return
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.questions.size) {
            updateAndPersistState {
                it.copy(
                    phase = VocabularyQuizPhase.Finished,
                    selectedOption = null,
                    feedback = null,
                    isLocked = false,
                )
            }
            sessionStore.clear() // a finished quiz has nothing left to resume
        } else {
            updateAndPersistState {
                it.copy(
                    currentIndex = nextIndex,
                    selectedOption = null,
                    feedback = null,
                    remainingSeconds = config.timerSeconds,
                    isLocked = false,
                )
            }
            persistSession()
            if (inForeground) startTicking()
        }
    }

    fun onResume() {
        inForeground = true
        val current = _state.value
        if (current.phase == VocabularyQuizPhase.Quiz && !current.isLocked) {
            startTicking()
        }
    }

    fun onPause() {
        inForeground = false
        tickJob?.cancel()
        persistSnapshot()
        persistSession()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val next = (_state.value.remainingSeconds - 1).coerceAtLeast(0)
                updateAndPersistState { it.copy(remainingSeconds = next) }
                if (next == 0) {
                    resolveQuestion(type = FeedbackType.Timeout, selected = null)
                    return@launch
                }
            }
        }
    }

    private fun resolveQuestion(type: FeedbackType, selected: String?) {
        val current = _state.value
        val question = current.currentQuestion ?: return
        tickJob?.cancel()

        val mistakeAction = mistakeTracker?.onQuestionResolved(current.currentIndex, type)
            ?: VocabularyMistakeAction.None
        when (mistakeAction) {
            is VocabularyMistakeAction.SaveMistake -> {
                val keys = mistakeAction.wordKeys.map(VocabularyQuizRules::mistakeKey).filter { it.isNotEmpty() }.toSet()
                savedMistakeWords = savedMistakeWords + keys
                viewModelScope.launch {
                    repository.recordMistakeWords(keys)
                }
            }
            is VocabularyMistakeAction.DeleteMastered -> {
                val keys = mistakeAction.wordKeys.map(VocabularyQuizRules::mistakeKey).filter { it.isNotEmpty() }.toSet()
                savedMistakeWords = savedMistakeWords - keys
                viewModelScope.launch {
                    repository.removeMistakeWords(keys)
                }
            }
            VocabularyMistakeAction.None -> Unit
        }

        updateAndPersistState {
            it.copy(
                selectedOption = selected,
                attemptedCount = it.attemptedCount + 1,
                correctCount = if (type == FeedbackType.Correct) it.correctCount + 1 else it.correctCount,
                wrongCount = if (type == FeedbackType.Incorrect) it.wrongCount + 1 else it.wrongCount,
                timeoutCount = if (type == FeedbackType.Timeout) it.timeoutCount + 1 else it.timeoutCount,
                feedback = Feedback(type, question.correctAnswer),
                isLocked = true,
                shakeCount = if (type == FeedbackType.Incorrect) it.shakeCount + 1 else it.shakeCount,
            )
        }
        persistSession()
    }

    private inline fun updateAndPersistState(transform: (VocabularyQuizUiState) -> VocabularyQuizUiState) {
        _state.update(transform)
        persistSnapshot()
    }

    private fun persistSnapshot() {
        // While a resume is still loading, the placeholder Study state must not replace what process-death restore would use.
        if (_state.value.isRestoring) return
        VocabularyQuizSavedState.save(
            handle = savedStateHandle,
            state = _state.value,
            outcomes = mistakeTracker?.outcomesSnapshot().orEmpty(),
        )
    }

    /**
     * Keeps the unfinished quiz on the device so it can be resumed after the app is closed. Only a running **Quiz** is kept
     * (Study has nothing to lose, Finished is cleared) and not on every tick: when a quiz starts, when a question is
     * answered, when the next one opens and when the screen is paused. Nothing is written while a resume is still loading.
     */
    private fun persistSession() {
        val current = _state.value
        if (current.isRestoring || current.phase != VocabularyQuizPhase.Quiz) return
        VocabularyQuizSavedState.encodeSession(current, mistakeTracker?.outcomesSnapshot().orEmpty())
            ?.let(sessionStore::save)
    }

    companion object {
        const val TICK_MS = 1_000L

        val Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = createSavedStateHandle()
                VocabularyQuizViewModel(
                    config = VocabularyQuizArgs.fromSavedState(handle),
                    repository = vocabularyRepository(),
                    savedStateHandle = handle,
                    sessionStore = vocabularySessionStore(),
                    resumeSavedQuiz = handle.get<Int>(Routes.ARG_RESUME) == 1,
                )
            }
        }
    }
}
