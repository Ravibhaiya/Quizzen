package com.ravibhaiya.quizzen.data

import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.FractionChallenge
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

        val alphabet = PracticeConfig.Alphabet(AlphabetChallenge.ReverseLetter, letters = 4..20, timerSeconds = 15)
        assertEquals(alphabet, SettingsCodec.decodeAlphabet(SettingsCodec.encode(alphabet), defaultTimer = 10))

        val fractions = PracticeConfig.Fractions(FractionChallenge.Percentage, timerSeconds = 15, range = 3..12)
        assertEquals(fractions, SettingsCodec.decodeFractions(SettingsCodec.encode(fractions), defaultTimer = 10))
    }

    @Test
    fun fractions_textIsShort_andDecodingRepairsTheTimer() {
        assertEquals("fc=fra;f=1-24;t=10", SettingsCodec.encode(PracticeConfig.Fractions(FractionChallenge.Fraction, 10)))
        assertEquals(
            "fc=per;f=4-9;t=25",
            SettingsCodec.encode(PracticeConfig.Fractions(FractionChallenge.Percentage, 25, range = 4..9)),
        )
        // A save from before the range existed has no "f": every fraction.
        assertEquals(1..24, SettingsCodec.decodeFractions("fc=per;t=10", 10)!!.range)
        // A damaged range is repaired.
        assertEquals(1..24, SettingsCodec.decodeFractions("fc=per;f=0-99;t=10", 10)!!.range)
        assertEquals(7..7, SettingsCodec.decodeFractions("fc=per;f=7-2;t=10", 10)!!.range)
        assertEquals(1..24, SettingsCodec.decodeFractions("fc=per;f=x-y;t=10", 10)!!.range)
        assertEquals(10, SettingsCodec.decodeFractions("fc=per;t=999", 10)!!.timerSeconds)
        assertEquals(10, SettingsCodec.decodeFractions("fc=per", 10)!!.timerSeconds)
        assertNull(SettingsCodec.decodeFractions("fc=unknown;t=10", 10)) // not a challenge: use the defaults
        assertNull(SettingsCodec.decodeFractions("garbage", 10))
        assertNull(SettingsCodec.decodeFractions(null, 10))
    }

    @Test
    fun alphabet_textIsShort_andDecodingRepairsWhatIsOutsideTheScreen() {
        assertEquals("ch=pos;l=1-26;t=10", SettingsCodec.encode(PracticeConfig.Alphabet(AlphabetChallenge.FindPosition, 1..26, 10)))
        val repaired = SettingsCodec.decodeAlphabet("ch=let;l=40-2;t=999", 10)!!
        assertEquals(AlphabetChallenge.FindLetter, repaired.challenge)
        assertEquals(26..26, repaired.letters)
        assertEquals(10, repaired.timerSeconds)
        assertEquals(1..26, SettingsCodec.decodeAlphabet("ch=pos;t=5", 10)!!.letters) // missing range: the whole alphabet
        assertNull(SettingsCodec.decodeAlphabet("ch=unknown;l=1-26;t=10", 10)) // not a challenge: use the defaults
        assertNull(SettingsCodec.decodeAlphabet(null, 10))
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
