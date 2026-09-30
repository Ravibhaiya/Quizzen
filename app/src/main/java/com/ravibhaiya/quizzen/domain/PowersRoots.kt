package com.ravibhaiya.quizzen.domain

/** The four kinds of question in Powers & Roots. [limit] is the largest base number allowed for that kind. */
enum class PowerRootType(val code: String, val family: Family, val limit: Int) {
    Squares("sq", Family.Square, PowersRootsRules.SQUARE_LIMIT),
    Cubes("cu", Family.Cube, PowersRootsRules.CUBE_LIMIT),
    SquareRoots("sqrt", Family.Square, PowersRootsRules.SQUARE_LIMIT),
    CubeRoots("cbrt", Family.Cube, PowersRootsRules.CUBE_LIMIT),
    ;

    /** Squares and square roots share one limit, cubes and cube roots another. */
    enum class Family { Square, Cube }

    companion object {
        fun fromCode(code: String): PowerRootType? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Rules for the Powers & Roots number range. The range is over the *base number*: `x` for `x^2` / `x^3`, and the answer
 * for roots (`sqrt(x^2)` = x). Squares and square roots go up to [SQUARE_LIMIT], cubes and cube roots up to [CUBE_LIMIT];
 * a range that goes above a kind's limit is simply cut off at that limit for that kind.
 */
object PowersRootsRules {
    const val SQUARE_LIMIT = 30
    const val CUBE_LIMIT = 20

    /** Smallest / largest number the Min and Max fields accept. */
    const val MIN_ALLOWED = 1
    const val MAX_ALLOWED = SQUARE_LIMIT

    const val DEFAULT_MIN = 2
    const val DEFAULT_MAX = SQUARE_LIMIT

    enum class Issue { None, InvalidNumber, MinAboveMax, BeyondCubeLimit }

    /** Result of cleaning what was typed into the Min/Max fields. */
    data class NumberInput(val text: String, val exceededLimit: Boolean)

    /** Digits only, no leading zeros, and anything above [MAX_ALLOWED] is cut to it (and flagged). Blank stays blank. */
    fun normalizeInput(raw: String): NumberInput {
        val digits = raw.filter(Char::isDigit).take(3)
        if (digits.isEmpty()) return NumberInput("", exceededLimit = false)
        val value = digits.toInt()
        return if (value > MAX_ALLOWED) {
            NumberInput(MAX_ALLOWED.toString(), exceededLimit = true)
        } else {
            NumberInput(value.toString(), exceededLimit = false)
        }
    }

    /** Numbers actually used for [type], or null when the range has nothing in it for that kind. */
    fun effectiveRange(type: PowerRootType, min: Int, max: Int): IntRange? {
        val high = minOf(max, type.limit)
        return if (min <= high) min..high else null
    }

    fun issue(types: Set<PowerRootType>, min: Int?, max: Int?): Issue {
        if (min == null || max == null) return Issue.InvalidNumber
        if (min !in MIN_ALLOWED..MAX_ALLOWED || max !in MIN_ALLOWED..MAX_ALLOWED) return Issue.InvalidNumber
        if (min > max) return Issue.MinAboveMax
        if (types.isNotEmpty() && types.none { effectiveRange(it, min, max) != null }) return Issue.BeyondCubeLimit
        return Issue.None
    }
}
