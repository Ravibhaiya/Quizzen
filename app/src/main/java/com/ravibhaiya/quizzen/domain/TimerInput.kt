package com.ravibhaiya.quizzen.domain

/**
 * State of the "Timer" numeric field on the configuration screens.
 *
 * Rules (from the web original): value is clamped to [MAX_SECONDS]; exceeding it flags [exceededMax]
 * so the UI can flash an error; a value of 0 or blank means "use the default".
 */
data class TimerInput(
    val text: String,
    val exceededMax: Boolean = false,
) {
    fun onTextChanged(raw: String): TimerInput {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return TimerInput(text = "")
        val value = digits.take(MAX_DIGITS).toInt()
        return if (value > MAX_SECONDS) {
            TimerInput(text = MAX_SECONDS.toString(), exceededMax = true)
        } else {
            TimerInput(text = value.toString())
        }
    }

    /** Blank fields snap back to [defaultSeconds] when focus is lost. */
    fun onFocusLost(defaultSeconds: Int): TimerInput =
        if (text.isEmpty()) TimerInput(defaultSeconds.toString()) else copy(exceededMax = false)

    /** Seconds to actually run with; 0 / blank fall back to [defaultSeconds]. */
    fun resolveSeconds(defaultSeconds: Int): Int =
        text.toIntOrNull()?.takeIf { it > 0 } ?: defaultSeconds

    companion object {
        const val MAX_SECONDS = 180
        private const val MAX_DIGITS = 4
    }
}
