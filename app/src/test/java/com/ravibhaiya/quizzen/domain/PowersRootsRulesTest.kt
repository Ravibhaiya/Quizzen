package com.ravibhaiya.quizzen.domain

import com.ravibhaiya.quizzen.domain.PowerRootType.Family
import org.junit.Assert.assertEquals
import org.junit.Test

class PowersRootsRulesTest {

    @Test
    fun limitsAreThirtyForSquaresAndTwentyForCubes() {
        assertEquals(30, PowerRootType.Squares.limit)
        assertEquals(30, PowerRootType.SquareRoots.limit)
        assertEquals(20, PowerRootType.Cubes.limit)
        assertEquals(20, PowerRootType.CubeRoots.limit)
        assertEquals(30, PowersRootsRules.limitOf(Family.Square))
        assertEquals(20, PowersRootsRules.limitOf(Family.Cube))
    }

    @Test
    fun defaultRangesStartAtTwoAndEndAtTheLimit() {
        assertEquals(2..30, PowersRootsRules.defaultRange(Family.Square))
        assertEquals(2..20, PowersRootsRules.defaultRange(Family.Cube))
    }

    @Test
    fun coerce_keepsValidRanges() {
        assertEquals(5..12, PowersRootsRules.coerce(5, 12, Family.Cube))
        assertEquals(1..30, PowersRootsRules.coerce(1, 30, Family.Square))
        assertEquals(7..7, PowersRootsRules.coerce(7, 7, Family.Square))
    }

    @Test
    fun coerce_repairsRangesOutsideTheLimits() {
        assertEquals(2..20, PowersRootsRules.coerce(2, 30, Family.Cube)) // cubes stop at 20
        assertEquals(1..30, PowersRootsRules.coerce(-4, 99, Family.Square))
        assertEquals(20..20, PowersRootsRules.coerce(25, 30, Family.Cube)) // start pulled inside, end never before start
        assertEquals(9..9, PowersRootsRules.coerce(9, 5, Family.Square)) // end before start
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
