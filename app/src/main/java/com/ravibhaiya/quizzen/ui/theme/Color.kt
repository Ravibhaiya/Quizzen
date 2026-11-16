package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Tokens that Material 3's [ColorScheme] has no slot for. Read via `MaterialTheme.quizzen`. */
@Immutable
data class QuizzenColors(
    /** End stop of the signature primary -> tone30 gradient (and the tint of hero/logo shadows). */
    val tone30: Color,
    val success: Color,
    val error: Color,
    /** Background of the "Time's Up" sheet. */
    val timeout: Color,
    /** Accent and badge colors that visually distinguish Synonyms (same meaning) from Antonyms (opposite meaning). */
    val synonym: Color,
    val synonymContainer: Color,
    val onSynonymContainer: Color,
    val antonym: Color,
    val antonymContainer: Color,
    val onAntonymContainer: Color,
)

// Palette of the app. It is light-only by design (no dark palette).
private val Primary = Color(0xFFEA7A31)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFFCE1CB)
private val OnPrimaryContainer = Color(0xFF7A3B0E)
private val Surface = Color(0xFFFFFAF6)
private val SurfaceContainer = Color(0xFFFBEEE3)
private val SurfaceContainerHigh = Color(0xFFF5E1CE)
private val OnSurface = Color(0xFF211710)
private val OnSurfaceVariant = Color(0xFF5C4A3B)
private val Tone30 = Color(0xFFB85E1E)

// Feedback colors.
private val Success = Color(0xFF1FAE6A)
private val Error = Color(0xFFE14C4C)
// Sky blue: sits with the green/red sheets at a similar brightness (white text contrast ~2.9:1, like the green one).
private val Timeout = Color(0xFF2E9FE0)

// Vocabulary relation tokens: Emerald-Teal for Synonyms vs Plum-Violet for Antonyms.
private val SynonymAccent = Color(0xFF15806A)
private val SynonymContainer = Color(0xFFD6F3EC)
private val OnSynonymContainer = Color(0xFF0B4A3E)
private val AntonymAccent = Color(0xFF7B3FA0)
private val AntonymContainer = Color(0xFFEDE0FA)
private val OnAntonymContainer = Color(0xFF421861)

// Only Material's `inversePrimary` slot needs this extra color.
private val InversePrimary = Color(0xFFF4B178)

internal val QuizzenExtraColors = QuizzenColors(
    tone30 = Tone30,
    success = Success,
    error = Error,
    timeout = Timeout,
    synonym = SynonymAccent,
    synonymContainer = SynonymContainer,
    onSynonymContainer = OnSynonymContainer,
    antonym = AntonymAccent,
    antonymContainer = AntonymContainer,
    onAntonymContainer = OnAntonymContainer,
)

/**
 * Every Material role is set explicitly so no component (Snackbar, dividers, sheets...) can fall back to the
 * baseline purple palette. Roles without a dedicated token reuse the design's own tokens.
 */
internal val QuizzenColorScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Primary,
    onSecondary = OnPrimary,
    secondaryContainer = PrimaryContainer,
    onSecondaryContainer = OnPrimaryContainer,
    tertiary = Tone30,
    onTertiary = OnPrimary,
    tertiaryContainer = PrimaryContainer,
    onTertiaryContainer = OnPrimaryContainer,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Primary,
    inverseSurface = OnSurface,
    inverseOnSurface = Surface,
    error = Error,
    onError = Color.White,
    errorContainer = Error,
    onErrorContainer = Color.White,
    outline = OnSurfaceVariant,
    outlineVariant = SurfaceContainerHigh,
    scrim = Color.Black,
    surfaceBright = Surface,
    surfaceDim = SurfaceContainerHigh,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHigh,
)
