package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.ui.components.HapticEffect
import com.ravibhaiya.quizzen.ui.components.HapticPattern
import com.ravibhaiya.quizzen.ui.components.HapticPatterns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPatternsTest {

    @Test
    fun everyEffectHasItsOwnDistinctPattern() {
        val effects = HapticEffect.entries
        for (a in effects) for (b in effects) {
            if (a != b) {
                assertFalse("$a and $b feel identical", HapticPatterns.of(a).contentEquals(HapticPatterns.of(b)))
            }
        }
    }

    @Test
    fun feedbackEffectsAreClearlyDistinguishable() {
        val success = HapticPatterns.of(HapticEffect.Success)
        val error = HapticPatterns.of(HapticEffect.Error)
        val timeout = HapticPatterns.of(HapticEffect.Timeout)
        // Success = 2 pulses, Error = 3 pulses, Timeout = 1 long pulse.
        assertEquals(2, success.amplitudes.count { it > 0 })
        assertEquals(3, error.amplitudes.count { it > 0 })
        assertEquals(1, timeout.amplitudes.count { it > 0 })
        assertTrue(timeout.timings.max() > error.timings.max())
    }

    @Test
    fun strengthGrowsFromTickToClickToHeavyClick() {
        fun peak(e: HapticEffect) = HapticPatterns.of(e).amplitudes.max()
        assertTrue(peak(HapticEffect.Tick) < peak(HapticEffect.Click))
        assertTrue(peak(HapticEffect.Click) < peak(HapticEffect.HeavyClick))
    }

    @Test
    fun invalidPatternsAreRejected() {
        assertRejected { HapticPattern(longArrayOf(10, 10), intArrayOf(100)) }
        assertRejected { HapticPattern(longArrayOf(0), intArrayOf(100)) }
        assertRejected { HapticPattern(longArrayOf(10), intArrayOf(300)) }
        assertRejected { HapticPattern(longArrayOf(10), intArrayOf(0)) }
    }

    private fun assertRejected(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
