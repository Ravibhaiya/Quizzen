package com.ravibhaiya.quizzen.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Plays [HapticEffect]s through the device vibrator, honoring the in-app Haptic Feedback setting.
 *
 * This deliberately uses [Vibrator] rather than `View.performHapticFeedback`: the latter is silenced by the system's
 * "touch vibration" toggle and is very weak on many phones, so most users never felt it.
 */
@Immutable
class Haptics internal constructor(
    private val vibrator: Vibrator?,
    private val enabled: Boolean,
) {
    fun perform(effect: HapticEffect) {
        if (!enabled) return
        val device = vibrator?.takeIf { it.hasVibrator() } ?: return
        val pattern = HapticPatterns.of(effect)
        device.vibrate(VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, NO_REPEAT))
    }

    fun tick() = perform(HapticEffect.Tick)
    fun click() = perform(HapticEffect.Click)
    fun heavyClick() = perform(HapticEffect.HeavyClick)
    fun success() = perform(HapticEffect.Success)
    fun error() = perform(HapticEffect.Error)
    fun timeout() = perform(HapticEffect.Timeout)

    private companion object {
        const val NO_REPEAT = -1
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val context = LocalContext.current
    return remember(context, enabled) { Haptics(context.findVibrator(), enabled) }
}

private fun Context.findVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
