package com.ravibhaiya.quizzen.domain

import kotlin.random.Random

/**
 * Pure-Kotlin streak rules shared by Math Practice and Language (Vocabulary) quizzes.
 *
 * A celebratory animation triggers only when the user answers [MILESTONE_STEP] consecutive
 * questions correctly (`5`, `10`, `15`, `20`, ...) — never on every single correct answer —
 * and any wrong answer or timeout resets the streak to `0`.
 * Beyond [EXTENDED_STREAK_THRESHOLD] (`20`, `25`, `30`, ...), celebrations randomly pick a
 * pre-made celebratory phrase and a distinct non-orange color palette while displaying the streak count.
 */
object StreakRules {
    /** Number of consecutive correct answers between celebratory milestone animations. */
    const val MILESTONE_STEP = 5

    /** Highest fixed-tier milestone (`15`); milestones strictly above this use random phrases and colors. */
    const val EXTENDED_STREAK_THRESHOLD = MILESTONE_STEP * 3

    /** Number of pre-made celebratory phrases available for streaks beyond [EXTENDED_STREAK_THRESHOLD]. */
    const val EXTENDED_PHRASE_COUNT = 8

    /** Number of distinct non-orange color palettes available for streaks beyond [EXTENDED_STREAK_THRESHOLD]. */
    const val EXTENDED_PALETTE_COUNT = 6

    enum class Tier {
        /** First milestone (`5` in a row). */
        Momentum,
        /** Second milestone (`10` in a row). */
        OnFire,
        /** Third milestone and beyond (`15`, `20`, ... in a row). */
        Unstoppable,
    }

    /** Returns the updated consecutive correct count after resolving a question with [outcome]. */
    fun nextStreak(currentStreak: Int, outcome: FeedbackType): Int = when (outcome) {
        FeedbackType.Correct -> currentStreak.coerceAtLeast(0) + 1
        FeedbackType.Incorrect,
        FeedbackType.Timeout,
        -> 0
    }

    /** True only when [streak] lands on an exact positive multiple of [MILESTONE_STEP]. */
    fun isMilestone(streak: Int): Boolean = streak > 0 && streak % MILESTONE_STEP == 0

    /** True when [streak] exceeds the 15-streak threshold (`20`, `25`, `30`, ...). */
    fun isExtendedStreak(streak: Int): Boolean = streak > EXTENDED_STREAK_THRESHOLD

    /** Maps a milestone [streak] count to its celebratory visual tier. */
    fun tierFor(streak: Int): Tier = when {
        streak >= EXTENDED_STREAK_THRESHOLD -> Tier.Unstoppable
        streak >= MILESTONE_STEP * 2 -> Tier.OnFire
        else -> Tier.Momentum
    }

    /**
     * Picks a random index in `0 until count`, avoiding [previousIndex] when `count > 1` so
     * back-to-back milestones after 15 always feel fresh.
     */
    fun pickRandomIndex(
        count: Int,
        previousIndex: Int = -1,
        random: Random = Random.Default,
    ): Int {
        if (count <= 1) return 0
        if (previousIndex !in 0 until count) return random.nextInt(count)
        val raw = random.nextInt(count - 1)
        return if (raw >= previousIndex) raw + 1 else raw
    }
}
