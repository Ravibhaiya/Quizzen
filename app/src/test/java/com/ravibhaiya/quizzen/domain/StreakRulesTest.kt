package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StreakRulesTest {

    @Test
    fun nextStreak_incrementsOnCorrectAndResetsOnWrongOrTimeout() {
        assertEquals(1, StreakRules.nextStreak(0, FeedbackType.Correct))
        assertEquals(5, StreakRules.nextStreak(4, FeedbackType.Correct))
        assertEquals(0, StreakRules.nextStreak(4, FeedbackType.Incorrect))
        assertEquals(0, StreakRules.nextStreak(7, FeedbackType.Timeout))
    }

    @Test
    fun isMilestone_onlyTrueAtPositiveMultiplesOfMilestoneStep() {
        assertFalse(StreakRules.isMilestone(0))
        for (n in 1 until StreakRules.MILESTONE_STEP) {
            assertFalse("Expected $n not to be a milestone", StreakRules.isMilestone(n))
        }
        assertTrue(StreakRules.isMilestone(StreakRules.MILESTONE_STEP))
        assertFalse(StreakRules.isMilestone(StreakRules.MILESTONE_STEP + 1))
        assertTrue(StreakRules.isMilestone(StreakRules.MILESTONE_STEP * 2))
        assertTrue(StreakRules.isMilestone(StreakRules.MILESTONE_STEP * 3))
    }

    @Test
    fun tierFor_escalatesAcrossMilestones() {
        assertEquals(StreakRules.Tier.Momentum, StreakRules.tierFor(StreakRules.MILESTONE_STEP))
        assertEquals(StreakRules.Tier.OnFire, StreakRules.tierFor(StreakRules.MILESTONE_STEP * 2))
        assertEquals(StreakRules.Tier.Unstoppable, StreakRules.tierFor(StreakRules.MILESTONE_STEP * 3))
        assertEquals(StreakRules.Tier.Unstoppable, StreakRules.tierFor(StreakRules.MILESTONE_STEP * 5))
    }

    @Test
    fun isExtendedStreak_trueOnlyStrictlyAboveFifteen() {
        assertFalse(StreakRules.isExtendedStreak(5))
        assertFalse(StreakRules.isExtendedStreak(10))
        assertFalse(StreakRules.isExtendedStreak(StreakRules.EXTENDED_STREAK_THRESHOLD))
        assertTrue(StreakRules.isExtendedStreak(StreakRules.EXTENDED_STREAK_THRESHOLD + StreakRules.MILESTONE_STEP))
        assertTrue(StreakRules.isExtendedStreak(StreakRules.EXTENDED_STREAK_THRESHOLD + StreakRules.MILESTONE_STEP * 3))
    }

    @Test
    fun pickRandomIndex_staysInRangeAndNeverRepeatsPreviousWhenMultipleChoicesExist() {
        val rng = Random(42)
        var prev = -1
        repeat(40) {
            val next = StreakRules.pickRandomIndex(StreakRules.EXTENDED_PHRASE_COUNT, previousIndex = prev, random = rng)
            assertTrue(next in 0 until StreakRules.EXTENDED_PHRASE_COUNT)
            if (prev >= 0) {
                assertNotEquals(prev, next)
            }
            prev = next
        }
        assertEquals(0, StreakRules.pickRandomIndex(1, previousIndex = 0, random = rng))
    }
}
