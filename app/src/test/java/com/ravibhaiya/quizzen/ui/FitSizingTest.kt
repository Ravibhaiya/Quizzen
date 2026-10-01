package com.ravibhaiya.quizzen.ui

import com.ravibhaiya.quizzen.ui.components.fitFontSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FitSizingTest {

    /** A text that is exactly 10 px wide per sp of font size. */
    private val tenPerSp: (Float) -> Float = { size -> size * 10f }

    @Test
    fun keepsTheMaximumWhenItFits() {
        assertEquals(49.6f, fitFontSize(49.6f, 24f, 1.6f, availableWidth = 1_000f, widthAt = tenPerSp), 0.001f)
    }

    @Test
    fun picksTheLargestSizeOnTheGridThatFits() {
        // grid: 24.8, 24.3, 23.8, ... ; 10 px per sp and 235 px available => the largest size <= 23.5 is 23.3 (24.8 - 3 * 0.5)
        val size = fitFontSize(24.8f, 14f, 0.5f, availableWidth = 235f, widthAt = tenPerSp)
        assertEquals(23.3f, size, 0.001f)
        assertTrue(size * 10f <= 235f)
        assertTrue((size + 0.5f) * 10f > 235f) // one step larger would not fit
    }

    @Test
    fun neverGoesBelowTheMinimum() {
        assertEquals(24f, fitFontSize(49.6f, 24f, 1.6f, availableWidth = 10f, widthAt = tenPerSp), 0.001f)
    }

    @Test
    fun measuresOnlyAFewTimes() {
        var calls = 0
        fitFontSize(49.6f, 24f, 1.6f, availableWidth = 300f) { size -> calls++; size * 10f }
        assertTrue("measured $calls times", calls <= 8)
    }

    @Test
    fun matchesAnExhaustiveLinearSearch() {
        for (available in 100..600 step 7) {
            val expected = generateSequence(0) { it + 1 }
                .map { i -> (24.8f - i * 0.5f).coerceAtLeast(14f) }
                .first { it * 10f <= available || it == 14f }
            val actual = fitFontSize(24.8f, 14f, 0.5f, available.toFloat(), tenPerSp)
            assertEquals("available=$available", expected, actual, 0.001f)
        }
    }
}
