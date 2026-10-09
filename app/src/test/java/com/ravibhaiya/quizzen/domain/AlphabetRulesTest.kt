package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetRulesTest {

    private fun ask(challenge: AlphabetChallenge, position: Int) = AlphabetQuestion(challenge, position)

    @Test
    fun letters_areCountedFromAToZ() {
        assertEquals('A', AlphabetRules.letterOf(1))
        assertEquals('M', AlphabetRules.letterOf(13))
        assertEquals('Z', AlphabetRules.letterOf(26))
    }

    @Test
    fun opposite_countsFromTheOtherEnd() {
        assertEquals(26, AlphabetRules.opposite(1))
        assertEquals(25, AlphabetRules.opposite(2))
        assertEquals(14, AlphabetRules.opposite(13)) // M <-> N
        (1..26).forEach { assertEquals(it, AlphabetRules.opposite(AlphabetRules.opposite(it))) }
    }

    @Test
    fun findPosition_showsALetter_andWantsItsNumber() {
        val question = ask(AlphabetChallenge.FindPosition, 3)
        assertEquals("C", question.shown)
        assertEquals(AnswerKind.Number, question.answerKind)
        assertEquals("3", question.answerText)
        assertTrue(question.isCorrect("3"))
        assertFalse(question.isCorrect("C"))
        assertFalse(question.isCorrect(""))
    }

    @Test
    fun findLetter_showsANumber_andWantsTheLetter() {
        val question = ask(AlphabetChallenge.FindLetter, 3)
        assertEquals("3", question.shown)
        assertEquals(AnswerKind.Letter, question.answerKind)
        assertEquals("C", question.answerText)
        assertTrue(question.isCorrect("C"))
        assertTrue(question.isCorrect(" c "))
        assertFalse(question.isCorrect("3"))
        assertFalse(question.isCorrect(""))
    }

    @Test
    fun reverseLetter_showsALetter_andWantsTheOppositeLetter() {
        assertEquals("Z", ask(AlphabetChallenge.ReverseLetter, 1).answerText)
        assertEquals("A", ask(AlphabetChallenge.ReverseLetter, 26).answerText)
        val question = ask(AlphabetChallenge.ReverseLetter, 3)
        assertEquals("C", question.shown)
        assertEquals("X", question.answerText)
        assertTrue(question.isCorrect("x"))
        assertFalse(question.isCorrect("C"))
    }

    @Test
    fun numberQuestions_stillCheckExactIntegers() {
        assertTrue(ProductQuestion(12, 11).isCorrect("132"))
        assertFalse(ProductQuestion(12, 11).isCorrect("131"))
        assertFalse(ProductQuestion(12, 11).isCorrect(""))
        assertEquals("784", PowerQuestion(28, 2).answerText)
    }

    @Test
    fun coerce_keepsTheRangeInsideTheAlphabet() {
        assertEquals(1..26, AlphabetRules.coerce(0, 99))
        assertEquals(26..26, AlphabetRules.coerce(40, 2))
        assertEquals(5..9, AlphabetRules.coerce(5, 9))
        assertEquals(7..7, AlphabetRules.coerce(7, 3)) // never ends before it starts
    }

    @Test
    fun challengeCodes_roundTrip() {
        AlphabetChallenge.entries.forEach { assertEquals(it, AlphabetChallenge.fromCode(it.code)) }
        assertEquals(null, AlphabetChallenge.fromCode("nope"))
        assertEquals(null, AlphabetChallenge.fromCode(null))
    }
}
