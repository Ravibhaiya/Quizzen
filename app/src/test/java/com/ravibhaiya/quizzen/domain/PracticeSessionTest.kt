package com.ravibhaiya.quizzen.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionTest {

    /** Questions that are all different, so any question that shows up twice is a comeback. */
    private class UniqueQuestions : QuestionGenerator {
        private var counter = 1_000L
        override fun next(config: PracticeConfig): Question = ProductQuestion(++counter, 1)
    }

    /** A limited quiz with [size] different questions, standing in for the real list of a Tables / Powers & Roots quiz. */
    private fun pool(size: Int): List<Question> = List(size) { ProductQuestion(1_000L + it, 1) }

    private val limited = PracticeConfig.Tables(numbers = listOf(2, 3), timerSeconds = 10)
    private val unlimited = PracticeConfig.Multiply(firstDigits = 2, secondDigits = 2, timerSeconds = 10)

    /**
     * Plays [total] questions; [outcomeOf] decides how each one is answered. Returns the questions in order shown.
     * [questions] is the quiz's list of questions (null = draw at random from [generator], like Multiply).
     */
    private fun play(
        total: Int,
        seed: Int,
        config: PracticeConfig = limited,
        generator: QuestionGenerator = UniqueQuestions(),
        questions: List<Question>? = if (config.repeatsMistakes) pool(2_000) else null,
        outcomeOf: (position: Int, question: Question) -> AnswerOutcome,
    ): List<Question> {
        val session = PracticeSession(generator, config, Random(seed), questions)
        val shown = ArrayList<Question>()
        var question = session.next()
        for (position in 1..total) {
            shown += question
            session.report(question, outcomeOf(position, question))
            question = session.next()
        }
        return shown
    }

    private fun positions(shown: List<Question>, question: Question) =
        shown.indices.filter { shown[it] == question }.map { it + 1 }

    private fun assertNeverNextToEachOther(positions: List<Int>) =
        assertTrue("positions $positions", positions.zipWithNext().all { (a, b) -> b - a >= 2 })

    private fun mistakeAt(mistakePosition: Int, outcome: AnswerOutcome, seed: Int): Triple<List<Int>, Question, Int> {
        var target: Question? = null
        val shown = play(total = 60, seed = seed) { position, question ->
            if (position == mistakePosition) {
                target = question
                outcome
            } else {
                AnswerOutcome.Fast
            }
        }
        return Triple(positions(shown, target!!).filter { it != mistakePosition }, target!!, mistakePosition)
    }

    @Test
    fun wrongAnswer_bringsTheQuestionBackThreeTimes_insideTheNextTen_butNotNext() {
        for (seed in 0 until 300) {
            val (comebacks, _, at) = mistakeAt(mistakePosition = 1 + seed % 12, AnswerOutcome.Wrong, seed)
            assertEquals("seed=$seed comebacks=$comebacks", 3, comebacks.size)
            assertTrue("seed=$seed comebacks=$comebacks", comebacks.all { it in (at + 2)..(at + 10) })
            assertNeverNextToEachOther(comebacks)
        }
    }

    @Test
    fun slowAnswer_bringsTheQuestionBackTwice_insideTheNextTen_butNotNext() {
        for (seed in 0 until 300) {
            val (comebacks, _, at) = mistakeAt(mistakePosition = 1 + seed % 12, AnswerOutcome.Slow, seed)
            assertEquals("seed=$seed comebacks=$comebacks", 2, comebacks.size)
            assertTrue("seed=$seed comebacks=$comebacks", comebacks.all { it in (at + 2)..(at + 10) })
            assertNeverNextToEachOther(comebacks)
        }
    }

    @Test
    fun comebackPositionsVaryFromSessionToSession() {
        val patterns = (0 until 100).map { seed ->
            var target: Question? = null
            val shown = play(total = 30, seed = seed) { position, question ->
                if (position == 1) { target = question; AnswerOutcome.Wrong } else AnswerOutcome.Fast
            }
            positions(shown, target!!)
        }.toSet()
        assertTrue("only ${patterns.size} different patterns", patterns.size > 10)
    }

    @Test
    fun fastAnswers_neverBringAnythingBack() {
        val shown = play(total = 100, seed = 5) { _, _ -> AnswerOutcome.Fast }
        assertEquals(shown.size, shown.toSet().size)
    }

    @Test
    fun comebackAnsweredFast_keepsTheRemainingComebacks() {
        for (seed in 0 until 100) {
            var target: Question? = null
            val shown = play(total = 60, seed = seed) { position, question ->
                if (position == 1) { target = question; AnswerOutcome.Wrong } else AnswerOutcome.Fast
            }
            assertEquals("seed=$seed", 4, positions(shown, target!!).size) // the mistake + 3 comebacks
        }
    }

    @Test
    fun comebackAnsweredWrongAgain_startsAFreshSetOfThree_andDropsTheOldOnes() {
        for (seed in 0 until 200) {
            var target: Question? = null
            var firstComeback = -1
            val shown = play(total = 80, seed = seed) { position, question ->
                when {
                    position == 1 -> { target = question; AnswerOutcome.Wrong }
                    question == target && firstComeback == -1 -> { firstComeback = position; AnswerOutcome.Wrong }
                    else -> AnswerOutcome.Fast
                }
            }
            val all = positions(shown, target!!)
            val after = all.filter { it > firstComeback }
            assertEquals("seed=$seed all=$all", 3, after.size)
            assertTrue("seed=$seed all=$all", after.all { it in (firstComeback + 2)..(firstComeback + 10) })
            assertEquals("seed=$seed all=$all", 5, all.size) // mistake + first comeback + 3 new comebacks
            assertNeverNextToEachOther(all)
        }
    }

    @Test
    fun manyMistakesInARow_nothingIsLost_andNothingIsEverRepeatedImmediately() {
        for (seed in 0 until 100) {
            val mistakes = ArrayList<Question>()
            val shown = play(total = 120, seed = seed) { position, question ->
                if (position <= 5 && question !in mistakes) { mistakes += question; AnswerOutcome.Wrong } else AnswerOutcome.Fast
            }
            for (question in mistakes) {
                val at = positions(shown, question)
                assertEquals("seed=$seed $at", 4, at.size)
                assertNeverNextToEachOther(at)
            }
            assertTrue(shown.zipWithNext().all { (a, b) -> a != b })
        }
    }

    @Test
    fun withASmallPool_aQuestionIsNeverTheSameAsThePreviousOne_andNoQuestionIsLost() {
        for (poolSize in listOf(3, 5, 10, 29, 96)) {
            val random = Random(poolSize)
            val questions = pool(poolSize)
            val shown = play(total = 20_000, seed = poolSize, questions = questions) { _, _ ->
                when (random.nextInt(10)) {
                    0, 1, 2 -> AnswerOutcome.Wrong
                    3, 4 -> AnswerOutcome.Slow
                    else -> AnswerOutcome.Fast
                }
            }
            assertEquals("pool=$poolSize", 0, shown.zipWithNext().count { (a, b) -> a == b })
            assertEquals("pool=$poolSize", questions.toSet(), shown.toSet())
        }
    }

    // ---- shuffled rounds ----

    @Test
    fun everyQuestionIsAskedOncePerRound_andRoundsKeepGoingForever() {
        val questions = pool(29)
        for (seed in 0 until 40) {
            val shown = play(total = 29 * 6, seed = seed, questions = questions) { _, _ -> AnswerOutcome.Fast }
            val rounds = shown.chunked(29)
            assertTrue("seed=$seed", rounds.all { it.size == 29 && it.toSet() == questions.toSet() })
            assertTrue("two rounds in the same order, seed=$seed", rounds.zipWithNext().all { (a, b) -> a != b })
            assertTrue("repeat at a round boundary, seed=$seed", shown.zipWithNext().all { (a, b) -> a != b })
        }
    }

    @Test
    fun theOrderOfARoundIsDifferentEveryQuiz() {
        val questions = pool(29)
        val firstRounds = (0 until 40).map { seed ->
            play(total = 29, seed = seed, questions = questions) { _, _ -> AnswerOutcome.Fast }
        }.toSet()
        assertTrue("only ${firstRounds.size} different orders", firstRounds.size > 35)
    }

    @Test
    fun aMistakeIsAddedToTheOrder_andTheRestOfTheRoundIsUntouched() {
        val questions = pool(29)
        for (seed in 0 until 300) {
            val mistakePosition = 1 + seed % 20
            var mistake: Question? = null
            val shown = play(total = 29 + 3, seed = seed, questions = questions) { position, question ->
                if (position == mistakePosition) { mistake = question; AnswerOutcome.Wrong } else AnswerOutcome.Fast
            }
            val counts = shown.groupingBy { it }.eachCount()
            assertEquals("seed=$seed", 4, counts[mistake])
            assertTrue("seed=$seed", questions.all { it == mistake || counts[it] == 1 })
        }
    }

    @Test
    fun pileUp_everyQuestionOfTheRoundIsStillAskedOnce_andNoComebackIsLost() {
        val questions = pool(29)
        for (seed in 0 until 100) {
            val mistakes = ArrayList<Question>()
            val longRun = play(total = 80, seed = seed, questions = questions) { position, question ->
                if (position <= 5 && question !in mistakes) { mistakes += question; AnswerOutcome.Wrong } else AnswerOutcome.Fast
            }
            val firstRound = longRun.take(29 + 3 * mistakes.size) // the round plus the comebacks it had to make room for
            val counts = firstRound.groupingBy { it }.eachCount()
            assertTrue("seed=$seed", mistakes.all { counts[it] == 4 })
            assertTrue("seed=$seed", questions.all { it in mistakes || counts[it] == 1 })
            assertTrue(longRun.zipWithNext().all { (a, b) -> a != b })
        }
    }

    @Test
    fun unlimitedQuizzes_doNotRepeatMistakes() {
        val shown = play(total = 200, seed = 1, config = unlimited) { _, _ -> AnswerOutcome.Wrong }
        assertEquals(shown.size, shown.toSet().size)
    }

    @Test
    fun aNewSession_startsWithNothingScheduled() {
        val first = PracticeSession(UniqueQuestions(), limited, Random(1), pool(200))
        val question = first.next()
        first.report(question, AnswerOutcome.Wrong)

        val second = PracticeSession(UniqueQuestions(), limited, Random(1), pool(200))
        val shownInSecond = List(15) { second.next() }
        // The second session never saw that mistake: all its questions are fresh and different.
        assertEquals(15, shownInSecond.toSet().size)
    }
}
