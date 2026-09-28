package com.ravibhaiya.quizzen.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionGeneratorTest {

    private val generator = RandomQuestionGenerator(Random(42))

    @Test
    fun multiply_operandsHaveRequestedDigitCounts() {
        repeat(500) {
            val q = generator.next(PracticeConfig.Multiply(firstDigits = 3, secondDigits = 2, timerSeconds = 20))
            assertTrue("left=${q.left}", q.left in 100..999)
            assertTrue("right=${q.right}", q.right in 10..99)
        }
    }

    @Test
    fun multiply_fiveByFiveDigitsDoesNotOverflow() {
        repeat(200) {
            val q = generator.next(PracticeConfig.Multiply(firstDigits = 5, secondDigits = 5, timerSeconds = 20))
            assertEquals(q.left * q.right, q.answer)
            assertTrue(q.answer > Int.MAX_VALUE.toLong() / 100)
        }
    }

    @Test
    fun oneDigitRangeIsOneToNine() {
        repeat(200) {
            assertTrue(generator.randomWithDigits(1) in 1..9)
        }
    }

    @Test
    fun tables_usesOnlySelectedNumbersAndMultipliersOneToTen() {
        val chosen = listOf(7, 12, 25)
        repeat(300) {
            val q = generator.next(PracticeConfig.Tables(numbers = chosen, timerSeconds = 10))
            assertTrue(q.left.toInt() in chosen)
            assertTrue(q.right in 1..10)
        }
    }
}
