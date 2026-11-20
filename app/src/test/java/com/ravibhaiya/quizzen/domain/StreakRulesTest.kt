package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue(StreakRules.isMilestone(StreakRules.MILESTONE_STEP * 10))
        assertTrue(StreakRules.isMilestone(StreakRules.MILESTONE_STEP * 11))
    }

    @Test
    fun badgeFor_unlocksTenDistinctBadgesInOrderFromFiveToFifty() {
        assertEquals(StreakRules.BADGE_COUNT, StreakRules.Badge.entries.size)
        val expectedOrder = listOf(
            StreakRules.Badge.Bronze,
            StreakRules.Badge.Silver,
            StreakRules.Badge.Gold,
            StreakRules.Badge.Platinum,
            StreakRules.Badge.Emerald,
            StreakRules.Badge.Sapphire,
            StreakRules.Badge.Ruby,
            StreakRules.Badge.Amethyst,
            StreakRules.Badge.Diamond,
            StreakRules.Badge.Legend,
        )
        expectedOrder.forEachIndexed { index, expectedBadge ->
            val milestone = (index + 1) * StreakRules.MILESTONE_STEP
            assertEquals(index + 1, expectedBadge.rank)
            assertEquals(expectedBadge, StreakRules.badgeFor(milestone))
        }
    }

    @Test
    fun badgeFor_keepsFinalLegendBadgeBeyondFiftyWhileMilestonesContinue() {
        assertEquals(StreakRules.Badge.Legend, StreakRules.badgeFor(50))
        assertEquals(StreakRules.Badge.Legend, StreakRules.badgeFor(55))
        assertEquals(StreakRules.Badge.Legend, StreakRules.badgeFor(60))
        assertEquals(StreakRules.Badge.Legend, StreakRules.badgeFor(100))
        assertEquals(StreakRules.Badge.Legend, StreakRules.badgeFor(250))
        assertTrue(StreakRules.isMilestone(55))
        assertTrue(StreakRules.isMilestone(60))
    }
}
