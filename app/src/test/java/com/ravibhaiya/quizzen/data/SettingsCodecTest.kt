package com.ravibhaiya.quizzen.data

import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PracticeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCodecTest {

    @Test
    fun everyQuizRoundTrips() {
        val multiply = PracticeConfig.Multiply(firstDigits = 4, secondDigits = 3, timerSeconds = 45)
        assertEquals(multiply, SettingsCodec.decodeMultiply(SettingsCodec.encode(multiply), defaultTimer = 20))

        val tables = PracticeConfig.Tables(numbers = listOf(2, 5, 9, 31), timerSeconds = 12)
        assertEquals(tables, SettingsCodec.decodeTables(SettingsCodec.encode(tables), defaultTimer = 10))

        val powers = PracticeConfig.PowersRoots(
            types = setOf(PowerRootType.CubeRoots, PowerRootType.Squares),
            squares = 3..25,
            cubes = 4..18,
            timerSeconds = 15,
        )
        assertEquals(powers, SettingsCodec.decodePowersRoots(SettingsCodec.encode(powers), defaultTimer = 10))
    }

    @Test
    fun theSavedTextIsShortAndReadable() {
        val powers = PracticeConfig.PowersRoots(setOf(PowerRootType.CubeRoots, PowerRootType.Squares), 3..25, 4..18, 15)
        assertEquals("ty=sq,cbrt;sq=3-25;cu=4-18;t=15", SettingsCodec.encode(powers))
        assertEquals("d1=3;d2=2;t=20", SettingsCodec.encode(PracticeConfig.Multiply(3, 2, 20)))
        assertEquals("n=2,5;t=10", SettingsCodec.encode(PracticeConfig.Tables(listOf(2, 5), 10)))
    }

    @Test
    fun nothingSavedOrUnreadableText_givesNull() {
        assertNull(SettingsCodec.decodeMultiply(null, 20))
        assertNull(SettingsCodec.decodeMultiply("", 20))
        assertNull(SettingsCodec.decodeMultiply("garbage", 20))
        assertNull(SettingsCodec.decodeTables("not;valid", 10))
        assertNull(SettingsCodec.decodePowersRoots("   ", 10))
    }

    @Test
    fun multiply_repairsTheTimer_butNotImpossibleDigits() {
        assertEquals(20, SettingsCodec.decodeMultiply("d1=3;d2=2;t=999", 20)!!.timerSeconds)
        assertEquals(20, SettingsCodec.decodeMultiply("d1=3;d2=2;t=0", 20)!!.timerSeconds)
        assertEquals(20, SettingsCodec.decodeMultiply("d1=3;d2=2", 20)!!.timerSeconds)
        assertNull(SettingsCodec.decodeMultiply("d1=9;d2=3;t=20", 20)) // 9 digits is not an option: use the defaults
    }

    @Test
    fun tables_dropsNumbersThatAreNotOnTheScreen() {
        val decoded = SettingsCodec.decodeTables("n=1,2,2,40,abc,7;t=10", 10)!!
        assertEquals(listOf(2, 7), decoded.numbers)
    }

    @Test
    fun powersRoots_repairsRangesAndDropsUnknownTypes() {
        val decoded = SettingsCodec.decodePowersRoots("ty=sq,zzz,cu;sq=40-1;cu=2-30;t=5", 10)!!
        assertEquals(setOf(PowerRootType.Squares, PowerRootType.Cubes), decoded.types)
        assertEquals(30..30, decoded.squares) // pulled inside the limit, never ending before it starts
        assertEquals(2..20, decoded.cubes) // cubes stop at 20
        assertEquals(5, decoded.timerSeconds)
    }

    @Test
    fun powersRoots_missingPartsFallBackToTheDefaults() {
        val decoded = SettingsCodec.decodePowersRoots("ty=;sq=x;cu=;t=7", 10)
        assertNotNull(decoded)
        decoded!!
        assertTrue(decoded.types.isEmpty())
        assertEquals(2..30, decoded.squares)
        assertEquals(2..20, decoded.cubes)
        assertEquals(7, decoded.timerSeconds)
    }
}
