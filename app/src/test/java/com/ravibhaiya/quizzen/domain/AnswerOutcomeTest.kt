package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerOutcomeTest {

    private fun outcome(correct: Boolean, remaining: Int, total: Int) = AnswerOutcome.classify(correct, remaining, total)

    @Test
    fun slow_meansLessThanFortyPercentOfTheTimerWasLeft() {
        assertEquals(AnswerOutcome.Fast, outcome(true, remaining = 10, total = 10)) // instant
        assertEquals(AnswerOutcome.Fast, outcome(true, remaining = 4, total = 10)) // exactly 40% is not below 40%
        assertEquals(AnswerOutcome.Slow, outcome(true, remaining = 3, total = 10))
        assertEquals(AnswerOutcome.Fast, outcome(true, remaining = 8, total = 20))
        assertEquals(AnswerOutcome.Slow, outcome(true, remaining = 7, total = 20))
        assertEquals(AnswerOutcome.Slow, outcome(true, remaining = 1, total = 180))
        assertEquals(AnswerOutcome.Fast, outcome(true, remaining = 1, total = 1))
    }

    @Test
    fun wrongAnswersAndTimeOuts_areWrongWhateverTheTime() {
        assertEquals(AnswerOutcome.Wrong, outcome(false, remaining = 10, total = 10))
        assertEquals(AnswerOutcome.Wrong, outcome(false, remaining = 5, total = 10))
        assertEquals(AnswerOutcome.Wrong, outcome(false, remaining = 0, total = 10)) // time-out
    }

    @Test
    fun theRepeatCountsAreThreeForWrongAndTwoForSlow() {
        assertEquals(3, PracticeSession.WRONG_REPEATS)
        assertEquals(2, PracticeSession.SLOW_REPEATS)
        assertEquals(10, PracticeSession.WINDOW)
    }
}
