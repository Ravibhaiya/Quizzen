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
    /** Non-orange celebration accents for streak milestone tiers (10+ blue, 15+ plum-magenta, and random >15 palettes). */
    val streakBlue: Color,
    val streakBlueContainer: Color,
    val onStreakBlueContainer: Color,
    val streakMagenta: Color,
    val streakIndigo: Color,
    val streakViolet: Color,
    val streakIndigoContainer: Color,
    val onStreakIndigoContainer: Color,
    val streakCyan: Color,
    val streakTeal: Color,
    val streakCyanContainer: Color,
    val onStreakCyanContainer: Color,
    val streakCrimson: Color,
    val streakRose: Color,
    val streakCrimsonContainer: Color,
    val onStreakCrimsonContainer: Color,
    val streakEmerald: Color,
    val streakForest: Color,
    val streakEmeraldContainer: Color,
    val onStreakEmeraldContainer: Color,
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

// Streak milestone celebration accents (kept distinct from the app's primary orange).
private val StreakBlue = Color(0xFF2563EB)
private val StreakBlueContainer = Color(0xFFDBEAFE)
private val OnStreakBlueContainer = Color(0xFF1E3A8A)
private val StreakMagenta = Color(0xFFC22578)
private val StreakIndigo = Color(0xFF4F46E5)
private val StreakViolet = Color(0xFF7C3AED)
private val StreakIndigoContainer = Color(0xFFE0E7FF)
private val OnStreakIndigoContainer = Color(0xFF312E81)
private val StreakCyan = Color(0xFF0891B2)
private val StreakTeal = Color(0xFF0D9488)
private val StreakCyanContainer = Color(0xFFCFFAFE)
private val OnStreakCyanContainer = Color(0xFF164E63)
private val StreakCrimson = Color(0xFFE11D48)
private val StreakRose = Color(0xFFBE185D)
private val StreakCrimsonContainer = Color(0xFFFFE4E6)
private val OnStreakCrimsonContainer = Color(0xFF881337)
private val StreakEmerald = Color(0xFF059669)
private val StreakForest = Color(0xFF047857)
private val StreakEmeraldContainer = Color(0xFFD1FAE5)
private val OnStreakEmeraldContainer = Color(0xFF064E3B)

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
    streakBlue = StreakBlue,
    streakBlueContainer = StreakBlueContainer,
    onStreakBlueContainer = OnStreakBlueContainer,
    streakMagenta = StreakMagenta,
    streakIndigo = StreakIndigo,
    streakViolet = StreakViolet,
    streakIndigoContainer = StreakIndigoContainer,
    onStreakIndigoContainer = OnStreakIndigoContainer,
    streakCyan = StreakCyan,
    streakTeal = StreakTeal,
    streakCyanContainer = StreakCyanContainer,
    onStreakCyanContainer = OnStreakCyanContainer,
    streakCrimson = StreakCrimson,
    streakRose = StreakRose,
    streakCrimsonContainer = StreakCrimsonContainer,
    onStreakCrimsonContainer = OnStreakCrimsonContainer,
    streakEmerald = StreakEmerald,
    streakForest = StreakForest,
    streakEmeraldContainer = StreakEmeraldContainer,
    onStreakEmeraldContainer = OnStreakEmeraldContainer,
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
