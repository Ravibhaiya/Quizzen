package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalQuizzenColors = staticCompositionLocalOf { LightQuizzenColors }

/** Access extra design tokens: `MaterialTheme.quizzen.tone30`. */
val MaterialTheme.quizzen: QuizzenColors
    @Composable
    @ReadOnlyComposable
    get() = LocalQuizzenColors.current

@Composable
fun QuizzenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extras = if (darkTheme) DarkQuizzenColors else LightQuizzenColors
    CompositionLocalProvider(LocalQuizzenColors provides extras) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = QuizzenTypography,
            content = content,
        )
    }
}
