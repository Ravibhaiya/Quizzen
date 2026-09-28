package com.ravibhaiya.quizzen.ui.practice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravibhaiya.quizzen.domain.Feedback
import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.domain.Question
import com.ravibhaiya.quizzen.domain.QuestionGenerator
import com.ravibhaiya.quizzen.domain.RandomQuestionGenerator
import com.ravibhaiya.quizzen.ui.navigation.PracticeArgs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeUiState(
    val question: Question,
    val remainingSeconds: Int,
    val answer: String = "",
    /** Non-null while the feedback sheet is visible. */
    val feedback: Feedback? = null,
    /** True from feedback start until the next question appears; input is ignored while locked. */
    val isLocked: Boolean = false,
    /** Increments on every incorrect answer so the UI can replay the shake animation. */
    val shakeCount: Int = 0,
) {
    val isTimerLow: Boolean get() = remainingSeconds <= LOW_TIME_THRESHOLD_SECONDS

    companion object {
        const val LOW_TIME_THRESHOLD_SECONDS = 5
    }
}

class PracticeViewModel(
    private val config: PracticeConfig,
    private val generator: QuestionGenerator = RandomQuestionGenerator(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        PracticeUiState(question = generator.next(config), remainingSeconds = config.timerSeconds),
    )
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var feedbackJob: Job? = null
    private var inForeground = false

    fun onAnswerChanged(raw: String) {
        if (_state.value.isLocked) return
        _state.update { it.copy(answer = raw.filter(Char::isDigit).take(MAX_ANSWER_DIGITS)) }
    }

    fun onCheck() {
        val current = _state.value
        if (current.isLocked) return
        val value = current.answer.toLongOrNull()
        showFeedback(if (value == current.question.answer) FeedbackType.Correct else FeedbackType.Incorrect)
    }

    /** Call when the screen is visible & interactive. Resumes the countdown without resetting it. */
    fun onResume() {
        inForeground = true
        if (!_state.value.isLocked) startTicking()
    }

    /** Call when the screen is backgrounded so the timer doesn't burn down invisibly. */
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
                    showFeedback(FeedbackType.Timeout)
                    return@launch
                }
            }
        }
    }

    private fun showFeedback(type: FeedbackType) {
        tickJob?.cancel()
        feedbackJob?.cancel()
        _state.update {
            it.copy(
                feedback = Feedback(type, it.question.answer),
                isLocked = true,
                shakeCount = if (type == FeedbackType.Incorrect) it.shakeCount + 1 else it.shakeCount,
            )
        }
        feedbackJob = viewModelScope.launch {
            delay(FEEDBACK_VISIBLE_MS)
            _state.update { it.copy(feedback = null) }
            delay(SHEET_EXIT_MS)
            _state.update {
                it.copy(
                    question = generator.next(config),
                    answer = "",
                    remainingSeconds = config.timerSeconds,
                    isLocked = false,
                )
            }
            if (inForeground) startTicking()
        }
    }

    companion object {
        const val TICK_MS = 1_000L
        const val FEEDBACK_VISIBLE_MS = 1_700L
        const val SHEET_EXIT_MS = 250L
        const val MAX_ANSWER_DIGITS = 12

        val Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = createSavedStateHandle()
                PracticeViewModel(PracticeArgs.fromSavedState(handle))
            }
        }
    }
}
