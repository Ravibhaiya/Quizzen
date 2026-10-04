package com.ravibhaiya.quizzen.domain

/** The four kinds of question in Powers & Roots. [limit] is the largest base number allowed for that kind. */
enum class PowerRootType(val code: String, val family: Family, val limit: Int) {
    Squares("sq", Family.Square, PowersRootsRules.SQUARE_LIMIT),
    Cubes("cu", Family.Cube, PowersRootsRules.CUBE_LIMIT),
    SquareRoots("sqrt", Family.Square, PowersRootsRules.SQUARE_LIMIT),
    CubeRoots("cbrt", Family.Cube, PowersRootsRules.CUBE_LIMIT),
    ;

    /** Squares and square roots share one range (and limit), cubes and cube roots another. */
    enum class Family { Square, Cube }

    companion object {
        fun fromCode(code: String): PowerRootType? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Rules for the Powers & Roots number ranges. Each family has its own range of *base numbers*: `x` for `x^2` / `x^3`, and the
 * answer for roots (`sqrt(x^2)` = x). Squares and square roots go up to [SQUARE_LIMIT], cubes and cube roots up to
 * [CUBE_LIMIT]. The range sliders can only produce values inside these limits; [coerce] repairs anything that arrives from
 * elsewhere (saved navigation arguments).
 */
object PowersRootsRules {
    const val SQUARE_LIMIT = 30
    const val CUBE_LIMIT = 20

    /** Smallest number a range may start at. */
    const val MIN_ALLOWED = 1

    /** The sliders start at 2 (1 squared and cubed is not much of a question), up to the limit. */
    const val DEFAULT_MIN = 2

    fun limitOf(family: PowerRootType.Family): Int =
        if (family == PowerRootType.Family.Square) SQUARE_LIMIT else CUBE_LIMIT

    fun defaultRange(family: PowerRootType.Family): IntRange = DEFAULT_MIN..limitOf(family)

    /** A valid range for [family]: inside `MIN_ALLOWED..limit`, and never ending before it starts. */
    fun coerce(min: Int, max: Int, family: PowerRootType.Family): IntRange {
        val limit = limitOf(family)
        val low = min.coerceIn(MIN_ALLOWED, limit)
        return low..max.coerceIn(low, limit)
    }
}
