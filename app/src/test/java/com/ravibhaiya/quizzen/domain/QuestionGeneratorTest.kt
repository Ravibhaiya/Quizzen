package com.ravibhaiya.quizzen.domain

import com.ravibhaiya.quizzen.domain.AlphabetQuestion
import com.ravibhaiya.quizzen.domain.PowerQuestion
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.ProductQuestion
import com.ravibhaiya.quizzen.domain.RootQuestion
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionGeneratorTest {

    private val generator = RandomQuestionGenerator(Random(42))

    @Test
    fun multiply_operandsHaveRequestedDigitCounts() {
        repeat(500) {
            val q = generator.next(PracticeConfig.Multiply(firstDigits = 3, secondDigits = 2, timerSeconds = 20)) as ProductQuestion
            assertTrue("left=${q.left}", q.left in 100..999)
            assertTrue("right=${q.right}", q.right in 10..99)
        }
    }

    @Test
    fun multiply_fiveByFiveDigitsDoesNotOverflow() {
        repeat(200) {
            val q = generator.next(PracticeConfig.Multiply(firstDigits = 5, secondDigits = 5, timerSeconds = 20)) as ProductQuestion
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
            val q = generator.next(PracticeConfig.Tables(numbers = chosen, timerSeconds = 10)) as ProductQuestion
            assertTrue(q.left.toInt() in chosen)
            assertTrue(q.right in 1..10)
        }
    }

    // ---- Powers & Roots: squares and square roots go up to 30, cubes and cube roots up to 20 ----

    private fun powers(
        types: Set<PowerRootType>,
        squares: IntRange = 2..30,
        cubes: IntRange = 2..20,
    ) = PracticeConfig.PowersRoots(types = types, squares = squares, cubes = cubes, timerSeconds = 10)

    @Test
    fun squares_useBasesUpToThirty() {
        var highest = 0L
        repeat(3_000) {
            val q = generator.next(powers(setOf(PowerRootType.Squares))) as PowerQuestion
            assertEquals(2, q.exponent)
            assertTrue("base=${q.base}", q.base in 2..30)
            assertEquals(q.base * q.base, q.answer)
            highest = maxOf(highest, q.base)
        }
        assertEquals(30L, highest)
    }

    @Test
    fun cubes_useBasesUpToTwenty() {
        var highest = 0L
        repeat(3_000) {
            val q = generator.next(powers(setOf(PowerRootType.Cubes))) as PowerQuestion
            assertEquals(3, q.exponent)
            assertTrue("base=${q.base}", q.base in 2..20)
            assertEquals(q.base * q.base * q.base, q.answer)
            highest = maxOf(highest, q.base)
        }
        assertEquals(20L, highest)
    }

    @Test
    fun squareRoots_showPerfectSquaresAndAnswerTheRoot() {
        repeat(3_000) {
            val q = generator.next(powers(setOf(PowerRootType.SquareRoots))) as RootQuestion
            assertEquals(2, q.degree)
            assertTrue("answer=${q.answer}", q.answer in 2..30)
            assertEquals(q.answer * q.answer, q.radicand)
        }
    }

    @Test
    fun cubeRoots_showPerfectCubesUpTo8000AndAnswerTheRoot() {
        repeat(3_000) {
            val q = generator.next(powers(setOf(PowerRootType.CubeRoots))) as RootQuestion
            assertEquals(3, q.degree)
            assertTrue("answer=${q.answer}", q.answer in 2..20)
            assertEquals(q.answer * q.answer * q.answer, q.radicand)
            assertTrue(q.radicand <= 8_000)
        }
    }

    @Test
    fun mixedSelection_asksAllFourKinds_withEachKindKeepingItsOwnLimit() {
        val seen = mutableSetOf<String>()
        var squareKindAboveTwenty = false
        repeat(6_000) {
            when (val q = generator.next(powers(PowerRootType.entries.toSet()))) {
                is PowerQuestion -> {
                    seen += "power${q.exponent}"
                    if (q.exponent == 3) assertTrue(q.base <= 20) else if (q.base > 20) squareKindAboveTwenty = true
                }
                is RootQuestion -> {
                    seen += "root${q.degree}"
                    if (q.degree == 3) assertTrue(q.answer <= 20) else if (q.answer > 20) squareKindAboveTwenty = true
                }
                is ProductQuestion, is AlphabetQuestion, is FractionQuestion -> throw AssertionError("unexpected $q")
            }
        }
        assertEquals(setOf("power2", "power3", "root2", "root3"), seen)
        assertTrue(squareKindAboveTwenty)
    }

    @Test
    fun eachKindUsesItsOwnRange() {
        repeat(2_000) {
            val q = generator.next(powers(PowerRootType.entries.toSet(), squares = 25..30, cubes = 2..5))
            val ok = when {
                q is PowerQuestion && q.exponent == 2 -> q.base in 25..30
                q is RootQuestion && q.degree == 2 -> q.answer in 25..30
                q is PowerQuestion && q.exponent == 3 -> q.base in 2..5
                q is RootQuestion && q.degree == 3 -> q.answer in 2..5
                else -> false
            }
            assertTrue("unexpected $q", ok)
        }
    }

    @Test
    fun customRangeIsRespected() {
        repeat(500) {
            val q = generator.next(powers(setOf(PowerRootType.Squares), squares = 10..12)) as PowerQuestion
            assertTrue(q.base in 10..12)
        }
    }
}
