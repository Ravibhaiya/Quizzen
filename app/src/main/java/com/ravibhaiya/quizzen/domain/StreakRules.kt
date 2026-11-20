package com.ravibhaiya.quizzen.domain

/**
 * Pure-Kotlin streak rules shared by Math Practice and Language (Vocabulary) quizzes.
 *
 * A celebratory animation triggers only when the user answers [MILESTONE_STEP] consecutive
 * questions correctly (`5`, `10`, `15`, `20`, ...) — never on every single correct answer —
 * and any wrong answer or timeout resets the streak to `0`.
 *
 * Milestones unlock [BADGE_COUNT] (`10`) progressive rank badges from [Badge.Bronze] (`5`)
 * up to [Badge.Legend] (`50`). Beyond `50` (`55`, `60`, ...), the final [Badge.Legend] badge
 * continues to appear while the displayed streak number keeps increasing.
 */
object StreakRules {
    /** Number of consecutive correct answers between celebratory milestone animations. */
    const val MILESTONE_STEP = 5

    /** Total number of progressive streak badges (`5` through `50`). */
    const val BADGE_COUNT = 10

    /** Progressive streak badges unlocked every [MILESTONE_STEP] consecutive correct answers. */
    enum class Badge(val rank: Int) {
        /** Milestone 1 (`5` in a row). */
        Bronze(1),
        /** Milestone 2 (`10` in a row). */
        Silver(2),
        /** Milestone 3 (`15` in a row). */
        Gold(3),
        /** Milestone 4 (`20` in a row). */
        Platinum(4),
        /** Milestone 5 (`25` in a row). */
        Emerald(5),
        /** Milestone 6 (`30` in a row). */
        Sapphire(6),
        /** Milestone 7 (`35` in a row). */
        Ruby(7),
        /** Milestone 8 (`40` in a row). */
        Amethyst(8),
        /** Milestone 9 (`45` in a row). */
        Diamond(9),
        /** Milestone 10 and beyond (`50`, `55`, `60`, ... in a row). */
        Legend(10),
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

    /**
     * Maps a milestone [streak] count to its progressive [Badge]:
     * `5 -> Bronze`, `10 -> Silver`, ..., `50 -> Legend`, and `55+ -> Legend`.
     */
    fun badgeFor(streak: Int): Badge {
        val index = ((streak / MILESTONE_STEP) - 1).coerceIn(0, Badge.entries.lastIndex)
        return Badge.entries[index]
    }
}
