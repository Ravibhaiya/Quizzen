package com.ravibhaiya.quizzen.data

import com.ravibhaiya.quizzen.domain.AlphabetChallenge
import com.ravibhaiya.quizzen.domain.AlphabetRules
import com.ravibhaiya.quizzen.domain.FractionChallenge
import com.ravibhaiya.quizzen.domain.FractionRules
import com.ravibhaiya.quizzen.domain.PowerRootType
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.domain.PracticeConfig
import com.ravibhaiya.quizzen.domain.TimerInput

/**
 * Turns the last-used settings of a quiz into one short string and back, e.g. `d1=3;d2=2;t=20`. Pure Kotlin so it can be
 * unit tested. Decoding never trusts the text: anything missing, malformed or outside what the screen allows is repaired or
 * replaced by the default, and a string that cannot be read at all gives `null` (use the defaults).
 */
object SettingsCodec {

    fun encode(config: PracticeConfig.Multiply): String =
        "d1=${config.firstDigits};d2=${config.secondDigits};t=${config.timerSeconds}"

    fun encode(config: PracticeConfig.Tables): String =
        "n=${config.numbers.joinToString(",")};t=${config.timerSeconds}"

    fun encode(config: PracticeConfig.PowersRoots): String =
        "ty=${config.types.sortedBy { it.ordinal }.joinToString(",") { it.code }};" +
            "sq=${config.squares.first}-${config.squares.last};cu=${config.cubes.first}-${config.cubes.last};" +
            "t=${config.timerSeconds}"

    fun encode(config: PracticeConfig.Alphabet): String =
        "ch=${config.challenge.code};l=${config.letters.first}-${config.letters.last};t=${config.timerSeconds}"

    fun encode(config: PracticeConfig.Fractions): String =
        "fc=${config.challenge.code};f=${config.range.first}-${config.range.last};t=${config.timerSeconds}"

    fun decodeMultiply(text: String?, defaultTimer: Int): PracticeConfig.Multiply? {
        val values = parse(text) ?: return null
        val options = PracticeConfig.Multiply.DIGIT_OPTIONS
        return PracticeConfig.Multiply(
            firstDigits = values["d1"]?.toIntOrNull()?.takeIf { it in options } ?: return null,
            secondDigits = values["d2"]?.toIntOrNull()?.takeIf { it in options } ?: return null,
            timerSeconds = timer(values["t"], defaultTimer),
        )
    }

    fun decodeTables(text: String?, defaultTimer: Int): PracticeConfig.Tables? {
        val values = parse(text) ?: return null
        val numbers = values["n"].orEmpty().split(',')
            .mapNotNull { it.toIntOrNull() }
            .filter { it in PracticeConfig.Tables.AVAILABLE_NUMBERS }
            .distinct()
            .sorted()
        return PracticeConfig.Tables(numbers = numbers, timerSeconds = timer(values["t"], defaultTimer))
    }

    fun decodePowersRoots(text: String?, defaultTimer: Int): PracticeConfig.PowersRoots? {
        val values = parse(text) ?: return null
        val square = PowerRootType.Family.Square
        val cube = PowerRootType.Family.Cube
        return PracticeConfig.PowersRoots(
            types = values["ty"].orEmpty().split(',').mapNotNull(PowerRootType::fromCode).toSet(),
            squares = range(values["sq"], square),
            cubes = range(values["cu"], cube),
            timerSeconds = timer(values["t"], defaultTimer),
        )
    }

    fun decodeAlphabet(text: String?, defaultTimer: Int): PracticeConfig.Alphabet? {
        val values = parse(text) ?: return null
        val letters = values["l"].orEmpty().split('-')
        return PracticeConfig.Alphabet(
            challenge = AlphabetChallenge.fromCode(values["ch"]) ?: return null,
            letters = AlphabetRules.coerce(
                from = letters.getOrNull(0)?.toIntOrNull() ?: AlphabetRules.FIRST,
                to = letters.getOrNull(1)?.toIntOrNull() ?: AlphabetRules.SIZE,
            ),
            timerSeconds = timer(values["t"], defaultTimer),
        )
    }

    fun decodeFractions(text: String?, defaultTimer: Int): PracticeConfig.Fractions? {
        val values = parse(text) ?: return null
        val places = values["f"].orEmpty().split('-')
        return PracticeConfig.Fractions(
            challenge = FractionChallenge.fromCode(values["fc"]) ?: return null,
            timerSeconds = timer(values["t"], defaultTimer),
            // A save from before the range existed has no "f": every fraction.
            range = FractionRules.coerce(
                from = places.getOrNull(0)?.toIntOrNull() ?: FractionRules.FIRST,
                to = places.getOrNull(1)?.toIntOrNull() ?: FractionRules.SIZE,
            ),
        )
    }

    private fun parse(text: String?): Map<String, String>? {
        if (text.isNullOrBlank()) return null
        val pairs = text.split(';').map { it.split('=', limit = 2) }
        if (pairs.any { it.size != 2 }) return null
        return pairs.associate { it[0] to it[1] }
    }

    private fun timer(text: String?, default: Int): Int =
        text?.toIntOrNull()?.takeIf { it in 1..TimerInput.MAX_SECONDS } ?: default

    private fun range(text: String?, family: PowerRootType.Family): IntRange {
        val parts = text.orEmpty().split('-')
        val low = parts.getOrNull(0)?.toIntOrNull() ?: PowersRootsRules.DEFAULT_MIN
        val high = parts.getOrNull(1)?.toIntOrNull() ?: PowersRootsRules.limitOf(family)
        return PowersRootsRules.coerce(low, high, family)
    }
}
