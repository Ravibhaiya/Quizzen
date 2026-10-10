package com.ravibhaiya.quizzen.domain

import kotlin.math.abs

/**
 * The two ways Fraction & Percentage can ask. One of them is chosen per quiz; the name says what the player *answers*.
 */
enum class FractionChallenge(val code: String) {
    /** A percentage is shown, the answer is the fraction: `33⅓%` (or `33.33%`) -> `1/3`. */
    Fraction("fra"),

    /** A fraction is shown, the answer is the percentage: `1/3` -> `33.33` (the app adds the `%` sign). */
    Percentage("per"),
    ;

    companion object {
        fun fromCode(code: String?): FractionChallenge? = entries.firstOrNull { it.code == code }
    }
}

/** A percentage written as a mixed number: `5 15/17` is whole 5 and 15/17. [numerator] is 0 when there is no fractional part. */
data class MixedPercent(val whole: Int, val numerator: Int, val denominator: Int) {
    val hasFraction: Boolean get() = numerator != 0
}

/**
 * Rules for Fraction & Percentage. Only the unit fractions `1/n` of the reference chart are asked ([DENOMINATORS]); every
 * percentage is `100 / n`. Everything is derived from the denominator, so there is no table of answers to keep in sync.
 */
object FractionRules {

    /** The n of every `1/n` on the chart: 2 to 20, then 24, 25, 30, 40 and 50. */
    val DENOMINATORS: List<Int> = (2..20).toList() + listOf(24, 25, 30, 40, 50)

    /**
     * The range slider works on the *places* of the fractions in [DENOMINATORS] (1 = 1/2 ... [SIZE] = 1/50), so every position of
     * the slider is a fraction of the chart. [coerce] repairs anything that arrives from elsewhere (saved settings, route arguments).
     */
    const val FIRST = 1
    val SIZE: Int = DENOMINATORS.size

    /** Every fraction of the chart, 1/2 to 1/50. */
    val FULL: IntRange = FIRST..SIZE

    /** The n of the fraction at [position] (1 = 2, 2 = 3, ... [SIZE] = 50). */
    fun denominatorAt(position: Int): Int = DENOMINATORS[position - FIRST]

    /** The denominators of the fractions in [range] (positions), smallest n first. */
    fun denominatorsIn(range: IntRange): List<Int> = coerce(range.first, range.last).map(::denominatorAt)

    /** A valid range: inside `FIRST..SIZE`, and never ending before it starts. */
    fun coerce(from: Int, to: Int): IntRange {
        val low = from.coerceIn(FIRST, SIZE)
        return low..to.coerceIn(low, SIZE)
    }

    /**
     * How far a typed percentage may be from the exact value when that value has endless decimals (1/3 = 33.333...). It lets
     * the answer be rounded or cut: 33.3, 33.33 and 33.34 are all right for 33 1/3. Percentages that end (25, 12.5, 6.25)
     * must be typed exactly.
     */
    const val PERCENT_TOLERANCE = 0.05

    /** 100 / [denominator] as a mixed number with a reduced fractional part: 17 -> 5 15/17, 30 -> 3 1/3, 25 -> 4. */
    fun percentOf(denominator: Int): MixedPercent {
        val whole = 100 / denominator
        val rest = 100 % denominator
        if (rest == 0) return MixedPercent(whole, 0, 1)
        val divisor = gcd(rest, denominator)
        return MixedPercent(whole, rest / divisor, denominator / divisor)
    }

    /** 100 / [denominator] with two decimals (rounded half up) and no trailing zeros: 3 -> "33.33", 8 -> "12.5", 4 -> "25". */
    fun decimalOf(denominator: Int): String {
        val hundredths = (20_000 + denominator) / (2 * denominator) // 10000 / n, rounded half up
        val whole = hundredths / 100
        val cents = hundredths % 100
        return when {
            cents == 0 -> whole.toString()
            cents % 10 == 0 -> "$whole.${cents / 10}"
            else -> "$whole.${cents.toString().padStart(2, '0')}"
        }
    }

    /** True when 100 / [denominator] ends after a few decimals (2, 4, 5, 8, 10, 16, 20, 25, 40, 50 on the chart). */
    fun endsExactly(denominator: Int): Boolean {
        var rest = percentOf(denominator).denominator
        while (rest % 2 == 0) rest /= 2
        while (rest % 5 == 0) rest /= 5
        return rest == 1
    }

    /** The percentage as plain text for the feedback sheet: `25%`, `6 1/4% = 6.25%`, `33 1/3% ≈ 33.33%`. */
    fun percentText(denominator: Int): String {
        val percent = percentOf(denominator)
        if (!percent.hasFraction) return "${percent.whole}%"
        val sign = if (endsExactly(denominator)) "=" else "\u2248"
        return "${percent.whole} ${percent.numerator}/${percent.denominator}% $sign ${decimalOf(denominator)}%"
    }

    /**
     * Whether [input] (`1/3`, spaces ignored) is a fraction equal to `1/[denominator]`. Equal values count, so `2/6` is right
     * for 1/3 too; a missing slash, a zero or a negative number never is.
     */
    fun isFractionAnswer(input: String, denominator: Int): Boolean {
        val parts = input.filterNot(Char::isWhitespace).split('/')
        if (parts.size != 2 || parts.any { it.isEmpty() || it.length > MAX_PART_LENGTH || !it.all(Char::isDigit) }) return false
        val top = parts[0].toLong()
        val bottom = parts[1].toLong()
        return top > 0 && top * denominator == bottom
    }

    /**
     * Whether [input] (`33.33`, a trailing `%` and a decimal comma are tolerated) is the percentage of `1/[denominator]`. See
     * [PERCENT_TOLERANCE] for how exact it has to be.
     */
    fun isPercentAnswer(input: String, denominator: Int): Boolean {
        val text = input.trim().removeSuffix("%").trim().replace(',', '.')
        if (text.none(Char::isDigit) || !text.all { it.isDigit() || it == '.' }) return false
        val typed = text.toDoubleOrNull() ?: return false
        val allowed = if (endsExactly(denominator)) EXACT_TOLERANCE else PERCENT_TOLERANCE
        return abs(typed - 100.0 / denominator) <= allowed
    }

    private const val MAX_PART_LENGTH = 6
    private const val EXACT_TOLERANCE = 1e-9

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
