package com.ravibhaiya.quizzen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerInputTest {

    private val empty = TimerInput("20")

    @Test
    fun acceptsValuesUpToMax() {
        val result = empty.onTextChanged("180")
        assertEquals("180", result.text)
        assertFalse(result.exceededMax)
    }

    @Test
    fun clampsAboveMaxAndFlagsError() {
        val result = empty.onTextChanged("181")
        assertEquals("180", result.text)
        assertTrue(result.exceededMax)
    }

    @Test
    fun stripsNonDigitsAndLeadingZeros() {
        assertEquals("7", empty.onTextChanged("007").text)
        assertEquals("15", empty.onTextChanged("1a5").text)
    }

    @Test
    fun blankStaysBlankWhileEditingThenRestoresDefaultOnFocusLost() {
        val blank = empty.onTextChanged("")
        assertEquals("", blank.text)
        assertEquals("20", blank.onFocusLost(defaultSeconds = 20).text)
    }

    @Test
    fun zeroOrBlankResolveToDefault() {
        assertEquals(20, TimerInput("0").resolveSeconds(20))
        assertEquals(10, TimerInput("").resolveSeconds(10))
        assertEquals(45, TimerInput("45").resolveSeconds(20))
    }
}
