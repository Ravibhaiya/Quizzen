package com.ravibhaiya.quizzen.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ravibhaiya.quizzen.R

/**
 * Plus Jakarta Sans as five static fonts (one file per weight).
 *
 * Static files are used instead of the single variable font because loading the variable font crashed the app on some
 * devices/emulators ("Font$Builder ... maybe invalid font data"). `OptionalLocal` is a safety net: if a file ever fails to
 * load, Compose falls back to the system font instead of throwing and killing the app.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
)

/**
 * Type scale. Sizes are the web original's rem values x 16 (see docs/DESIGN_SYSTEM.md).
 *
 * Role -> usage:
 *  displayLarge   49.6sp  practice question
 *  headlineLarge  24.8sp  app name, hero title, screen titles
 *  headlineMedium 20.8sp  settings sheet title
 *  titleLarge     19.2sp  section titles
 *  titleMedium    17.6sp  primary button label, "Timer" label
 *  titleSmall     16.96sp tile title
 *  bodyLarge      16.8sp  section label
 *  bodyMedium     16.3sp  chips, settings rows
 *  labelLarge     15.2sp  timer chip, select-all
 *  labelSmall     10.56sp timer unit
 */
internal val QuizzenTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 49.6.sp, letterSpacing = (-0.02).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 24.8.sp, letterSpacing = (-0.02).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 20.8.sp, letterSpacing = (-0.02).em,
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 19.2.sp, letterSpacing = (-0.01).em,
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.6.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 16.96.sp, lineHeight = 20.sp, letterSpacing = (-0.01).em,
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal,
        fontSize = 16.8.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.3.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 15.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 10.56.sp, letterSpacing = 0.05.em,
    ),
)
