package com.ravibhaiya.quizzen.ui.vocabularyquiz

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.data.VocabularyRepository
import com.ravibhaiya.quizzen.data.vocabularyRepository
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.VocabularyEntry
import com.ravibhaiya.quizzen.domain.VocabularyMistakeAction
import com.ravibhaiya.quizzen.domain.VocabularyMistakeTracker
import com.ravibhaiya.quizzen.domain.VocabularyQuizConfig
import com.ravibhaiya.quizzen.domain.VocabularyQuizEngine
import com.ravibhaiya.quizzen.domain.VocabularyQuizItem
import com.ravibhaiya.quizzen.domain.VocabularyQuizRules
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
) : ViewModel() {

    private var allEntries: List<VocabularyEntry> = emptyList()
    private var savedMistakeWords: Set<String> = emptySet()
    private var usedFallbackForStudy: Boolean = true
    private var mistakeTracker: VocabularyMistakeTracker? = null

    private val _state = MutableStateFlow(
        VocabularyQuizUiState(
            config = config,
            studyEntries = VocabularyQuizRules.selectStudyWords(
                entries = emptyList(),
                modes = config.modes,
                wordCount = config.wordCount,
                random = random,
            ),
            remainingSeconds = config.timerSeconds,
        ),
    )
    val state: StateFlow<VocabularyQuizUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var inForeground = false

    init {
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
                // and locally saved mistake words.
                if (_state.value.phase == VocabularyQuizPhase.Study &&
                    (((usedFallbackForStudy || !hadUserEligible) && nowHasUserEligible) || mistakesChangedBeforeQuiz)
                ) {
                    if (nowHasUserEligible) usedFallbackForStudy = false
                    _state.update {
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
        _state.update {
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

        _state.update {
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
        if (inForeground) startTicking()
    }

    /** Restarts the quiz using the same studied words in a fresh interleaved order. */
    fun retryQuiz() = startQuiz()

    /** Returns to the Study screen to review the selected words. */
    fun backToStudy() {
        tickJob?.cancel()
        _state.update {
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
            _state.update {
                it.copy(
                    phase = VocabularyQuizPhase.Finished,
                    selectedOption = null,
                    feedback = null,
                    isLocked = false,
                )
            }
        } else {
            _state.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedOption = null,
                    feedback = null,
                    remainingSeconds = config.timerSeconds,
                    isLocked = false,
                )
            }
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
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                val next = (_state.value.remainingSeconds - 1).coerceAtLeast(0)
                _state.update { it.copy(remainingSeconds = next) }
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

        _state.update {
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
    }

    companion object {
        const val TICK_MS = 1_000L

        val Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = createSavedStateHandle()
                VocabularyQuizViewModel(
                    config = VocabularyQuizArgs.fromSavedState(handle),
                    repository = vocabularyRepository(),
                )
            }
        }
    }
}
