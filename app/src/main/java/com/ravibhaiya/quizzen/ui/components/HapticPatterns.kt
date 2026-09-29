package com.ravibhaiya.quizzen.ui.components

/** Every distinct sensation the app can produce. One effect per kind of user action (see docs/BEHAVIOR_SPEC.md). */
enum class HapticEffect {
    /** Light tick: selecting an option, toggling a table number, switching tabs, placeholder taps. */
    Tick,

    /** Medium click: opening things, Back, Select All. */
    Click,

    /** Firm single thump: Start. */
    HeavyClick,

    /** Two rising taps: correct answer. */
    Success,

    /** Three hard buzzes: wrong answer. */
    Error,

    /** One long, softer buzz: time is up. */
    Timeout,
}

/**
 * A vibration waveform: `timings[i]` milliseconds at `amplitudes[i]` (0 = pause, 1..255 = strength).
 * Plain data so it can be unit-tested on the JVM; [Haptics] turns it into a `VibrationEffect`.
 *
 * Waveforms are used on every API level instead of the platform's predefined effects because budget phones often
 * implement those weakly or not at all, which made haptics feel absent.
 */
class HapticPattern(val timings: LongArray, val amplitudes: IntArray) {
    init {
        require(timings.isNotEmpty() && timings.size == amplitudes.size) { "timings and amplitudes must match" }
        require(timings.all { it > 0 }) { "every segment needs a positive duration" }
        require(amplitudes.all { it in 0..255 }) { "amplitude must be within 0..255" }
        require(amplitudes.any { it > 0 }) { "a pattern must vibrate at least once" }
    }

    val totalMillis: Long get() = timings.sum()

    fun contentEquals(other: HapticPattern): Boolean =
        timings.contentEquals(other.timings) && amplitudes.contentEquals(other.amplitudes)
}

internal object HapticPatterns {
    private val tick = HapticPattern(longArrayOf(18), intArrayOf(100))
    private val click = HapticPattern(longArrayOf(28), intArrayOf(165))
    private val heavyClick = HapticPattern(longArrayOf(45), intArrayOf(255))
    private val success = HapticPattern(longArrayOf(24, 55, 34), intArrayOf(150, 0, 235))
    private val error = HapticPattern(longArrayOf(70, 55, 70, 55, 110), intArrayOf(255, 0, 255, 0, 255))
    private val timeout = HapticPattern(longArrayOf(420), intArrayOf(190))

    fun of(effect: HapticEffect): HapticPattern = when (effect) {
        HapticEffect.Tick -> tick
        HapticEffect.Click -> click
        HapticEffect.HeavyClick -> heavyClick
        HapticEffect.Success -> success
        HapticEffect.Error -> error
        HapticEffect.Timeout -> timeout
    }
}
