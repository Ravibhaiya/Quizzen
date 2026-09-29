package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalQuizzenColors = staticCompositionLocalOf { QuizzenExtraColors }

/** Access extra design tokens: `MaterialTheme.quizzen.tone30`. */
val MaterialTheme.quizzen: QuizzenColors
    @Composable
    @ReadOnlyComposable
    get() = LocalQuizzenColors.current

/** Light-only theme: the app deliberately ignores the system dark-mode setting. */
@Composable
fun QuizzenTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalQuizzenColors provides QuizzenExtraColors) {
        MaterialTheme(
            colorScheme = QuizzenColorScheme,
            typography = QuizzenTypography,
            content = content,
        )
    }
}
