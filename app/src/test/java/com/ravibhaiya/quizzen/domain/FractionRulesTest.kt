package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FractionRulesTest {

    /** The reference chart, typed in by hand: n -> whole, numerator, denominator of `100 / n` (numerator 0 = no fraction). */
    private val chart: Map<Int, Triple<Int, Int, Int>> = mapOf(
        2 to Triple(50, 0, 1), 3 to Triple(33, 1, 3), 4 to Triple(25, 0, 1), 5 to Triple(20, 0, 1),
        6 to Triple(16, 2, 3), 7 to Triple(14, 2, 7), 8 to Triple(12, 1, 2), 9 to Triple(11, 1, 9),
        10 to Triple(10, 0, 1), 11 to Triple(9, 1, 11), 12 to Triple(8, 1, 3), 13 to Triple(7, 9, 13),
        14 to Triple(7, 1, 7), 15 to Triple(6, 2, 3), 16 to Triple(6, 1, 4), 17 to Triple(5, 15, 17),
        18 to Triple(5, 5, 9), 19 to Triple(5, 5, 19), 20 to Triple(5, 0, 1), 24 to Triple(4, 1, 6),
        25 to Triple(4, 0, 1), 30 to Triple(3, 1, 3), 40 to Triple(2, 1, 2), 50 to Triple(2, 0, 1),
    )

    @Test
    fun theQuizAsksExactlyTheFractionsOfTheChart() {
        assertEquals(chart.keys.sorted(), FractionRules.DENOMINATORS)
        assertEquals(24, FractionRules.DENOMINATORS.size)
    }

    @Test
    fun everyPercentageMatchesTheChart() {
        chart.forEach { (n, expected) ->
            val percent = FractionRules.percentOf(n)
            assertEquals("1/$n", expected, Triple(percent.whole, percent.numerator, percent.denominator))
        }
    }

    @Test
    fun decimals_haveTwoPlacesAndNoTrailingZeros() {
        val expected = mapOf(
            2 to "50", 3 to "33.33", 4 to "25", 6 to "16.67", 7 to "14.29", 8 to "12.5", 9 to "11.11", 11 to "9.09",
            12 to "8.33", 13 to "7.69", 14 to "7.14", 15 to "6.67", 16 to "6.25", 17 to "5.88", 18 to "5.56",
            19 to "5.26", 24 to "4.17", 25 to "4", 30 to "3.33", 40 to "2.5", 50 to "2",
        )
        expected.forEach { (n, text) -> assertEquals("1/$n", text, FractionRules.decimalOf(n)) }
    }

    @Test
    fun percentagesThatEnd_areTheOnesWithoutEndlessDecimals() {
        val ending = setOf(2, 4, 5, 8, 10, 16, 20, 25, 40, 50)
        FractionRules.DENOMINATORS.forEach { assertEquals("1/$it", it in ending, FractionRules.endsExactly(it)) }
    }

    @Test
    fun percentText_forTheFeedbackSheet() {
        assertEquals("25%", FractionRules.percentText(4))
        assertEquals("6 1/4% = 6.25%", FractionRules.percentText(16))
        assertEquals("33 1/3% \u2248 33.33%", FractionRules.percentText(3))
        assertEquals("5 15/17% \u2248 5.88%", FractionRules.percentText(17))
    }

    @Test
    fun fractionAnswers() {
        assertTrue(FractionRules.isFractionAnswer("1/3", 3))
        assertTrue(FractionRules.isFractionAnswer(" 1 / 3 ", 3))
        assertTrue(FractionRules.isFractionAnswer("2/6", 3)) // the same value
        assertFalse(FractionRules.isFractionAnswer("1/4", 3))
        listOf("", "1", "3", "/3", "1/", "/", "3/1", "0/3", "-1/3", "1/3/3", "a/b", "1.5/3", "1/3%", "99999999999/3")
            .forEach { assertFalse("'$it'", FractionRules.isFractionAnswer(it, 3)) }
    }

    @Test
    fun percentAnswers_thatEndMustBeExact() {
        assertTrue(FractionRules.isPercentAnswer("25", 4))
        assertTrue(FractionRules.isPercentAnswer("25.0", 4))
        assertTrue(FractionRules.isPercentAnswer("25%", 4))
        assertFalse(FractionRules.isPercentAnswer("25.01", 4))
        assertFalse(FractionRules.isPercentAnswer("24.99", 4))
        assertTrue(FractionRules.isPercentAnswer("12.5", 8))
        assertTrue(FractionRules.isPercentAnswer("12.50", 8))
        assertFalse(FractionRules.isPercentAnswer("12.4", 8))
        assertFalse(FractionRules.isPercentAnswer("13", 8))
        assertTrue(FractionRules.isPercentAnswer("6.25", 16))
        assertFalse(FractionRules.isPercentAnswer("6.3", 16))
    }

    @Test
    fun percentAnswers_withEndlessDecimals_mayBeRoundedOrCut() {
        listOf("33.33", "33.3", "33.34", "33,33", "33.33%", " 33.33 ", "33.333").forEach {
            assertTrue("'$it'", FractionRules.isPercentAnswer(it, 3))
        }
        listOf("33", "34", "33.5", "33.4", "3.33", "").forEach { assertFalse("'$it'", FractionRules.isPercentAnswer(it, 3)) }
        listOf("16.67", "16.66", "16.7").forEach { assertTrue("'$it'", FractionRules.isPercentAnswer(it, 6)) }
        listOf("5.88", "5.9", "5.882").forEach { assertTrue("'$it'", FractionRules.isPercentAnswer(it, 17)) }
        assertFalse(FractionRules.isPercentAnswer("5.8", 17))
        assertFalse(FractionRules.isPercentAnswer("5", 17))
    }

    @Test
    fun percentAnswers_thatAreNotNumbersAreWrong() {
        listOf("", ".", "%", "abc", "NaN", "Infinity", "1e1", "0x10", "-25", "2 5", "25..0").forEach {
            assertFalse("'$it'", FractionRules.isPercentAnswer(it, 4))
        }
    }

    @Test
    fun everyQuestionIsRightWithItsOwnAnswer() {
        FractionRules.DENOMINATORS.forEach { n ->
            val toFraction = FractionQuestion(FractionChallenge.Fraction, n)
            assertTrue("1/$n", toFraction.isCorrect(toFraction.answerText))
            assertEquals(AnswerKind.Fraction, toFraction.answerKind)

            val toPercent = FractionQuestion(FractionChallenge.Percentage, n)
            assertTrue("1/$n", toPercent.isCorrect(FractionRules.decimalOf(n)))
            assertEquals(AnswerKind.Percent, toPercent.answerKind)
        }
    }

    @Test
    fun answerText_isWhatTheFeedbackSheetShows() {
        assertEquals("1/3", FractionQuestion(FractionChallenge.Fraction, 3).answerText)
        assertEquals("33 1/3% \u2248 33.33%", FractionQuestion(FractionChallenge.Percentage, 3).answerText)
        assertEquals("20%", FractionQuestion(FractionChallenge.Percentage, 5).answerText)
    }

    @Test
    fun wording_isNotPartOfWhichQuestionItIs() {
        val mixed = FractionQuestion(FractionChallenge.Fraction, 3, decimal = false)
        val decimal = FractionQuestion(FractionChallenge.Fraction, 3, decimal = true)
        assertEquals(mixed, decimal)
        assertEquals(mixed.hashCode(), decimal.hashCode())
        assertNotEquals(mixed, FractionQuestion(FractionChallenge.Fraction, 4))
        assertNotEquals(mixed, FractionQuestion(FractionChallenge.Percentage, 3))
    }

    @Test
    fun asked_picksEitherWordingForPercentagesWithAFraction() {
        val random = Random(1)
        val question = FractionQuestion(FractionChallenge.Fraction, 3)
        val wordings = List(60) { (question.asked(random) as FractionQuestion).decimal }.toSet()
        assertEquals(setOf(true, false), wordings)
    }

    @Test
    fun asked_leavesWholePercentagesAndTheOtherDirectionAlone() {
        val random = Random(2)
        val whole = FractionQuestion(FractionChallenge.Fraction, 4) // 25%: nothing to rewrite
        repeat(30) { assertSame(whole, whole.asked(random)) }
        val toPercent = FractionQuestion(FractionChallenge.Percentage, 3) // a fraction is shown: one way to write it
        repeat(30) { assertSame(toPercent, toPercent.asked(random)) }
        val product = ProductQuestion(3, 4)
        assertSame(product, product.asked(random))
    }

    @Test
    fun pool_hasEveryFractionOnce() {
        listOf(FractionChallenge.Fraction, FractionChallenge.Percentage).forEach { challenge ->
            val pool = QuestionPool.of(PracticeConfig.Fractions(challenge, timerSeconds = 10))!!
            assertEquals(24, pool.size)
            assertEquals(FractionRules.DENOMINATORS, pool.map { (it as FractionQuestion).denominator })
            assertTrue(pool.all { (it as FractionQuestion).challenge == challenge })
        }
    }

    @Test
    fun session_asksEveryFractionOncePerRound_andSaysWhichWordingEachTime() {
        val config = PracticeConfig.Fractions(FractionChallenge.Fraction, timerSeconds = 10)
        val session = PracticeSession(RandomQuestionGenerator(Random(3)), config, Random(3))
        val round = List(24) { session.next() as FractionQuestion }
        assertEquals(FractionRules.DENOMINATORS, round.map { it.denominator }.sorted())
        // Whole percentages are never written as decimals; the others come in both wordings over a few rounds.
        val more = round + List(72) { session.next() as FractionQuestion }
        assertTrue(more.filter { !it.percent.hasFraction }.none { it.decimal })
        assertEquals(setOf(true, false), more.filter { it.percent.hasFraction }.map { it.decimal }.toSet())
    }

    @Test
    fun aMissedQuestion_comesBackWordedAsItWasMissed() {
        val config = PracticeConfig.Fractions(FractionChallenge.Fraction, timerSeconds = 10)
        val session = PracticeSession(RandomQuestionGenerator(Random(4)), config, Random(4))
        var missed: FractionQuestion? = null
        repeat(40) {
            val question = session.next() as FractionQuestion
            if (missed == null && question.percent.hasFraction) {
                missed = question
                session.report(question, AnswerOutcome.Wrong)
            } else if (missed != null && question == missed) {
                assertEquals(missed!!.decimal, question.decimal)
            }
        }
    }
}
