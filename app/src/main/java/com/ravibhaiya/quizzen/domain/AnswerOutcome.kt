package com.ravibhaiya.quizzen.domain

/** How an answer went, as far as the mistake-repeat rules are concerned. */
enum class AnswerOutcome {
    /** Correct and quick: nothing to repeat. */
    Fast,

    /** Correct but slow: the question comes back [PracticeSession.SLOW_REPEATS] times. */
    Slow,

    /** Wrong answer, or the time ran out: the question comes back [PracticeSession.WRONG_REPEATS] times. */
    Wrong,
    ;

    companion object {
        /**
         * An answer is slow when it was given with less than 40% of the timer left (the countdown on screen), i.e. it took
         * more than 60% of the time. With a 10 s timer, answering with 4 s left is still fast; with 3 s left it is slow.
         * A wrong answer or a time-out is [Wrong] whatever the time was.
         */
        fun classify(correct: Boolean, remainingSeconds: Int, totalSeconds: Int): AnswerOutcome = when {
            !correct -> Wrong
            remainingSeconds * 5 < totalSeconds * 2 -> Slow // remaining / total < 0.4, in whole numbers
            else -> Fast
        }
    }
}
