package com.ravibhaiya.quizzen.domain

/** The three ways Alphabet Reasoning can ask. One of them is chosen per quiz. */
enum class AlphabetChallenge(val code: String) {
    /** A letter is shown, the answer is its place in the alphabet: `C` -> `3`. */
    FindPosition("pos"),

    /** A place is shown, the answer is the letter: `3` -> `C`. */
    FindLetter("let"),

    /** A letter is shown, the answer is the letter at the same place counted from the other end: `C` -> `X`. */
    ReverseLetter("rev"),
    ;

    companion object {
        fun fromCode(code: String?): AlphabetChallenge? = entries.firstOrNull { it.code == code }
    }
}

/**
 * Rules for Alphabet Reasoning. Letters are handled as *positions* 1..26 (A = 1, Z = 26); the letter range of a quiz is a range of
 * positions. [coerce] repairs anything that arrives from elsewhere (saved settings, navigation arguments).
 */
object AlphabetRules {
    const val FIRST = 1
    const val SIZE = 26

    /** A to Z. */
    val FULL: IntRange = FIRST..SIZE

    fun letterOf(position: Int): Char = 'A' + (position - FIRST)

    /** The position of the same letter counted from the other end: A (1) <-> Z (26), B (2) <-> Y (25). */
    fun opposite(position: Int): Int = SIZE + FIRST - position

    /** A valid range: inside `FIRST..SIZE`, and never ending before it starts. */
    fun coerce(from: Int, to: Int): IntRange {
        val low = from.coerceIn(FIRST, SIZE)
        return low..to.coerceIn(low, SIZE)
    }
}
