package com.ravibhaiya.quizzen.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Thin wrapper over [View.performHapticFeedback] that honors the in-app Haptic Feedback setting. */
@Immutable
class Haptics(private val view: View, private val enabled: Boolean) {

    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)

    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP,
    )

    fun reject() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
    )

    private fun perform(constant: Int) {
        if (enabled) view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val view = LocalView.current
    return remember(view, enabled) { Haptics(view, enabled) }
}
