package com.ravibhaiya.quizzen.ui.powers

import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowerRootType.Family
import com.ravibhaiya.quizzen.ui.powers.LimitCardModel.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitCardModelTest {

    private fun cards(types: Set<PowerRootType>, min: Int? = 2, max: Int? = 30) = limitCardModels(types, min, max)
    private fun List<LimitCardModel>.square() = single { it.family == Family.Square }
    private fun List<LimitCardModel>.cube() = single { it.family == Family.Cube }

    @Test
    fun nothingSelected_showsBothLimits_andNothingIsFaded() {
        val result = cards(emptySet())
        assertEquals(30, result.square().limit)
        assertEquals(20, result.cube().limit)
        assertTrue(result.all { it.status == Status.Idle && !it.dimmed && it.used == null && !it.cut })
    }

    @Test
    fun squaresSelected_usesTheRange_andFadesTheCubeCard() {
        val result = cards(setOf(PowerRootType.Squares))
        assertEquals(Status.Using, result.square().status)
        assertEquals(2..30, result.square().used)
        assertFalse(result.square().cut)
        assertEquals(Status.Idle, result.cube().status)
        assertTrue(result.cube().dimmed)
    }

    @Test
    fun cubesWithMaxThirty_areCutAtTwenty() {
        val result = cards(setOf(PowerRootType.Cubes))
        assertEquals(Status.Using, result.cube().status)
        assertEquals(2..20, result.cube().used)
        assertTrue(result.cube().cut)
        assertTrue(result.square().dimmed)
    }

    @Test
    fun cubesWithMaxWithinTheLimit_areNotCut() {
        val cube = cards(setOf(PowerRootType.CubeRoots), min = 5, max = 12).cube()
        assertEquals(5..12, cube.used)
        assertFalse(cube.cut)
    }

    @Test
    fun mixedSelection_showsEachKindWithItsOwnRange() {
        val result = cards(PowerRootType.entries.toSet())
        assertEquals(2..30, result.square().used)
        assertEquals(2..20, result.cube().used)
        assertTrue(result.none { it.dimmed })
    }

    @Test
    fun cubesWithMinAboveTwenty_haveNoNumbers() {
        val result = cards(PowerRootType.entries.toSet(), min = 25, max = 30)
        assertEquals(Status.NoNumbers, result.cube().status)
        assertNull(result.cube().used)
        assertEquals(25..30, result.square().used)
    }

    @Test
    fun invalidRange_fallsBackToLimitsOnly() {
        for ((min, max) in listOf<Pair<Int?, Int?>>(null to 30, 2 to null, 0 to 30, 2 to 31, 9 to 5)) {
            val result = cards(PowerRootType.entries.toSet(), min, max)
            assertTrue("min=$min max=$max", result.all { it.status == Status.Idle && it.used == null })
        }
    }
}
