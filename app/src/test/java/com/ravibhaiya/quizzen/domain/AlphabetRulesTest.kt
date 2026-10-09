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
    fun pick_aLetterBeforeOrAfterStretchesTheRange() {
        assertEquals(3..9, AlphabetRules.pick(5..9, 3))
        assertEquals(5..12, AlphabetRules.pick(5..9, 12))
        assertEquals(1..26, AlphabetRules.pick(5..26, 1))
    }

    @Test
    fun pick_anEndOfALongerRange_startsOverWithJustThatLetter() {
        assertEquals(5..5, AlphabetRules.pick(5..9, 5))
        assertEquals(9..9, AlphabetRules.pick(5..9, 9))
        assertEquals(1..1, AlphabetRules.pick(1..26, 1))
        assertEquals(26..26, AlphabetRules.pick(1..26, 26))
    }

    @Test
    fun pick_aSingleLetter_staysUntilAnotherLetterStretchesIt() {
        assertEquals(5..5, AlphabetRules.pick(5..5, 5))
        assertEquals(5..9, AlphabetRules.pick(5..5, 9))
        assertEquals(2..5, AlphabetRules.pick(5..5, 2))
    }

    @Test
    fun pick_aLetterInside_movesTheNearerEnd() {
        assertEquals(7..20, AlphabetRules.pick(5..20, 7)) // nearer the start
        assertEquals(5..18, AlphabetRules.pick(5..20, 18)) // nearer the end
        assertEquals(10..20, AlphabetRules.pick(5..20, 10)) // 5 away from the start, 10 from the end: the start moves
        assertEquals(5..26, AlphabetRules.pick(5..9, 99))
    }

    @Test
    fun challengeCodes_roundTrip() {
        AlphabetChallenge.entries.forEach { assertEquals(it, AlphabetChallenge.fromCode(it.code)) }
        assertEquals(null, AlphabetChallenge.fromCode("nope"))
        assertEquals(null, AlphabetChallenge.fromCode(null))
    }
}
