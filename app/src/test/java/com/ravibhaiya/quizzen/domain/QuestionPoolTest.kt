package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionPoolTest {

    @Test
    fun tables_isEveryChosenNumberTimesOneToTen_eachOnce() {
        val pool = QuestionPool.of(PracticeConfig.Tables(numbers = listOf(2, 3), timerSeconds = 10))!!
        assertEquals(20, pool.size)
        assertEquals(20, pool.toSet().size)
        assertTrue(pool.all { it is ProductQuestion && it.left in 2..3 && it.right in 1..10 })
    }

    @Test
    fun alphabet_isEveryLetterOfTheRange_eachOnce() {
        val pool = QuestionPool.of(PracticeConfig.Alphabet(AlphabetChallenge.FindLetter, letters = 5..9, timerSeconds = 10))!!
        assertEquals((5..9).map { AlphabetQuestion(AlphabetChallenge.FindLetter, it) }, pool)
        assertEquals(26, QuestionPool.of(PracticeConfig.Alphabet(AlphabetChallenge.FindPosition, AlphabetRules.FULL, 10))!!.size)
    }

    @Test
    fun powersRoots_isEveryBaseOfEveryChosenKind_eachOnce() {
        val config = PracticeConfig.PowersRoots(PowerRootType.entries.toSet(), squares = 2..30, cubes = 2..20, timerSeconds = 10)
        val pool = QuestionPool.of(config)!!
        assertEquals(29 + 29 + 19 + 19, pool.size) // squares, square roots, cubes, cube roots
        assertEquals(pool.size, pool.toSet().size)
        assertTrue(pool.all { it.answer > 0 })
    }

    @Test
    fun powersRoots_usesTheRangeOfEachKind() {
        val config = PracticeConfig.PowersRoots(
            types = setOf(PowerRootType.Squares, PowerRootType.CubeRoots),
            squares = 25..27,
            cubes = 4..5,
            timerSeconds = 10,
        )
        assertEquals(
            setOf<Question>(
                PowerQuestion(25, 2), PowerQuestion(26, 2), PowerQuestion(27, 2),
                RootQuestion(3, 4), RootQuestion(3, 5),
            ),
            QuestionPool.of(config)!!.toSet(),
        )
    }

    @Test
    fun noListWhenThereAreTooManyQuestionsOrNothingIsSelected() {
        assertNull(QuestionPool.of(PracticeConfig.Multiply(3, 2, 10)))
        assertNull(QuestionPool.of(PracticeConfig.Tables(numbers = emptyList(), timerSeconds = 10)))
        assertNull(QuestionPool.of(PracticeConfig.PowersRoots(emptySet(), 2..30, 2..20, 10)))
    }
}
