package com.ravibhaiya.quizzen.domain

import com.ravibhaiya.quizzen.domain.PowersRootsRules.Issue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PowersRootsRulesTest {

    private val squares = setOf(PowerRootType.Squares)
    private val cubes = setOf(PowerRootType.Cubes, PowerRootType.CubeRoots)

    @Test
    fun limitsAreThirtyForSquaresAndTwentyForCubes() {
        assertEquals(30, PowerRootType.Squares.limit)
        assertEquals(30, PowerRootType.SquareRoots.limit)
        assertEquals(20, PowerRootType.Cubes.limit)
        assertEquals(20, PowerRootType.CubeRoots.limit)
    }

    @Test
    fun effectiveRange_cutsEachKindAtItsOwnLimit() {
        assertEquals(2..30, PowersRootsRules.effectiveRange(PowerRootType.Squares, 2, 30))
        assertEquals(2..20, PowersRootsRules.effectiveRange(PowerRootType.Cubes, 2, 30))
        assertEquals(5..12, PowersRootsRules.effectiveRange(PowerRootType.CubeRoots, 5, 12))
        assertNull(PowersRootsRules.effectiveRange(PowerRootType.CubeRoots, 21, 30))
    }

    @Test
    fun validRanges() {
        assertEquals(Issue.None, PowersRootsRules.issue(squares, 2, 30))
        assertEquals(Issue.None, PowersRootsRules.issue(cubes, 2, 30)) // cut to 20, still fine
        assertEquals(Issue.None, PowersRootsRules.issue(squares + cubes, 21, 30)) // squares still have 21..30
        assertEquals(Issue.None, PowersRootsRules.issue(emptySet(), 2, 30)) // selection is checked separately
    }

    @Test
    fun invalidRanges() {
        assertEquals(Issue.InvalidNumber, PowersRootsRules.issue(squares, null, 30))
        assertEquals(Issue.InvalidNumber, PowersRootsRules.issue(squares, 2, null))
        assertEquals(Issue.InvalidNumber, PowersRootsRules.issue(squares, 0, 30))
        assertEquals(Issue.InvalidNumber, PowersRootsRules.issue(squares, 2, 31))
        assertEquals(Issue.MinAboveMax, PowersRootsRules.issue(squares, 9, 5))
        assertEquals(Issue.BeyondCubeLimit, PowersRootsRules.issue(cubes, 21, 30))
    }

    @Test
    fun normalizeInput_keepsDigitsOnly_stripsLeadingZeros_andCapsAtThirty() {
        assertEquals(PowersRootsRules.NumberInput("", false), PowersRootsRules.normalizeInput(""))
        assertEquals(PowersRootsRules.NumberInput("", false), PowersRootsRules.normalizeInput("ab"))
        assertEquals(PowersRootsRules.NumberInput("7", false), PowersRootsRules.normalizeInput("007"))
        assertEquals(PowersRootsRules.NumberInput("0", false), PowersRootsRules.normalizeInput("0"))
        assertEquals(PowersRootsRules.NumberInput("25", false), PowersRootsRules.normalizeInput("2a5"))
        assertEquals(PowersRootsRules.NumberInput("30", false), PowersRootsRules.normalizeInput("30"))
        assertEquals(PowersRootsRules.NumberInput("30", true), PowersRootsRules.normalizeInput("31"))
        assertEquals(PowersRootsRules.NumberInput("30", true), PowersRootsRules.normalizeInput("999"))
    }

    @Test
    fun questionsKnowTheirAnswers() {
        assertEquals(784L, PowerQuestion(28, 2).answer)
        assertEquals(8_000L, PowerQuestion(20, 3).answer)
        assertEquals(28L, RootQuestion(2, 28).answer)
        assertEquals(784L, RootQuestion(2, 28).radicand)
        assertEquals(8_000L, RootQuestion(3, 20).radicand)
    }
}
