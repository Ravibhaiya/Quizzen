package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.domain.FeedbackType
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.domain.ProductQuestion
import com.ravibhaiya.quizzen.domain.Question
import com.ravibhaiya.quizzen.domain.QuestionGenerator
import com.ravibhaiya.quizzen.ui.navigation.PracticeArgs
import com.ravibhaiya.quizzen.ui.navigation.Routes
import com.ravibhaiya.quizzen.ui.practice.PracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val config = PracticeConfig.Multiply(firstDigits = 2, secondDigits = 2, timerSeconds = 20)

    /** Deterministic: 12x11, then 13x11, 14x11, ... */
    private class SequenceGenerator : QuestionGenerator {
        private var n = 11L
        override fun next(config: PracticeConfig): Question = ProductQuestion(++n, 11)
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PracticeViewModel(config, SequenceGenerator())

    private val PracticeViewModel.leftOperand: Long get() = (state.value.question as ProductQuestion).left

    @Test
    fun startsWithFirstQuestionAndFullTimer() {
        val vm = viewModel()
        assertEquals(12L, vm.leftOperand)
        assertEquals(20, vm.state.value.remainingSeconds)
    }

    @Test
    fun countdownTicksOncePerSecondOnlyWhileResumed() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onResume()
        advanceTimeBy(3_100)
        assertEquals(17, vm.state.value.remainingSeconds)

        vm.onPause()
        advanceTimeBy(5_000)
        assertEquals(17, vm.state.value.remainingSeconds)
    }

    @Test
    fun correctAnswerShowsFeedbackThenAdvances() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onResume()
        vm.onAnswerChanged("132")
        vm.onCheck()
        assertEquals(FeedbackType.Correct, vm.state.value.feedback?.type)
        assertTrue(vm.state.value.isLocked)

        advanceTimeBy(PracticeViewModel.FEEDBACK_VISIBLE_MS + 1)
        assertNull(vm.state.value.feedback)
        assertTrue(vm.state.value.isLocked)

        advanceTimeBy(PracticeViewModel.SHEET_EXIT_MS + 1)
        runCurrent()
        assertFalse(vm.state.value.isLocked)
        assertEquals(13L, vm.leftOperand)
        assertEquals("", vm.state.value.answer)
        assertEquals(20, vm.state.value.remainingSeconds)
        vm.onPause() // stop the endless tick loop, otherwise runTest never becomes idle
    }

    @Test
    fun wrongOrBlankAnswerIsIncorrectAndCarriesCorrectAnswer() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCheck() // blank
        val feedback = vm.state.value.feedback
        assertNotNull(feedback)
        assertEquals(FeedbackType.Incorrect, feedback!!.type)
        assertEquals(132L, feedback.correctAnswer)
        assertEquals(1, vm.state.value.shakeCount)
    }

    @Test
    fun inputIgnoredWhileLocked() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCheck()
        vm.onAnswerChanged("999")
        vm.onCheck()
        assertEquals("", vm.state.value.answer)
        assertEquals(1, vm.state.value.shakeCount)
    }

    @Test
    fun timeoutFiresWhenCountdownReachesZero() = runTest(dispatcher) {
        val vm = PracticeViewModel(config.copy(timerSeconds = 3), SequenceGenerator())
        vm.onResume()
        advanceTimeBy(3_100)
        assertEquals(0, vm.state.value.remainingSeconds)
        assertEquals(FeedbackType.Timeout, vm.state.value.feedback?.type)
        vm.onPause() // don't let the pending feedback job restart the tick loop
    }

    @Test
    fun answerFieldKeepsDigitsOnly() {
        val vm = viewModel()
        vm.onAnswerChanged("1a2-3")
        assertEquals("123", vm.state.value.answer)
    }

    @Test
    fun navigationArgsRoundTrip() {
        val multiply = PracticeArgs.decode("multiply", 4, 3, "", 30)
        assertEquals(PracticeConfig.Multiply(4, 3, 30), multiply)

        val tables = PracticeArgs.decode("tables", null, null, "2,5,9", 10)
        assertEquals(PracticeConfig.Tables(listOf(2, 5, 9), 10), tables)
    }

    @Test
    fun powersRootsRouteRoundTrips() {
        val config = PracticeConfig.PowersRoots(
            types = setOf(PowerRootType.CubeRoots, PowerRootType.Squares),
            min = 3,
            max = 25,
            timerSeconds = 15,
        )
        val route = Routes.practice(config)
        assertEquals("practice/powers?types=sq,cbrt&min=3&max=25&seconds=15", route)

        val decoded = PracticeArgs.decode("powers", null, null, null, 15, types = "sq,cbrt", min = 3, max = 25)
        assertEquals(config, decoded)
    }

    @Test
    fun powersRootsDecodingRepairsBadArguments() {
        val decoded = PracticeArgs.decode("powers", null, null, null, 0, types = "", min = 99, max = 1)
        assertEquals(PowerRootType.entries.toSet(), (decoded as PracticeConfig.PowersRoots).types)
        assertEquals(30, decoded.min) // clamped to the allowed range
        assertEquals(30, decoded.max) // never below min
        assertEquals(20, decoded.timerSeconds) // falls back to the default
    }
}
