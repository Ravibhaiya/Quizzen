package com.ravibhaiya.quizzen.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Stroke icons ported from the web original's 24x24 inline SVGs (Feather-style).
 * They are stroked black and tinted by `Icon(tint = ...)`.
 */
object QuizzenIcons {

    private fun strokeIcon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Settings: ImageVector by lazy {
        strokeIcon(
            "Settings", 2f,
            "M15 12a3 3 0 1 0-6 0a3 3 0 1 0 6 0",
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33" +
                " 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33" +
                "l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4" +
                "h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06" +
                "A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51" +
                " 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9" +
                "a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
        )
    }

    val ChevronRight: ImageVector by lazy { strokeIcon("ChevronRight", 2.4f, "M9 6l6 6-6 6") }
    val ChevronLeft: ImageVector by lazy { strokeIcon("ChevronLeft", 2.4f, "M15 6l-6 6 6 6") }

    val Calculator: ImageVector by lazy {
        strokeIcon(
            "Calculator", 2f,
            "M8 3h8a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H8a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z",
            "M8 7h8",
            "M7.5 12a1 1 0 1 0 2 0a1 1 0 1 0-2 0",
            "M11 12a1 1 0 1 0 2 0a1 1 0 1 0-2 0",
            "M14.5 12a1 1 0 1 0 2 0a1 1 0 1 0-2 0",
            "M7.5 16a1 1 0 1 0 2 0a1 1 0 1 0-2 0",
            "M11 16a1 1 0 1 0 2 0a1 1 0 1 0-2 0",
            "M14.5 16a1 1 0 1 0 2 0a1 1 0 1 0-2 0",
        )
    }

    val Globe: ImageVector by lazy {
        strokeIcon(
            "Globe", 2f,
            "M3 12a9 9 0 1 0 18 0a9 9 0 1 0-18 0",
            "M3 12h18",
            "M12 3c2.5 2.5 4 5.5 4 9s-1.5 6.5-4 9c-2.5-2.5-4-5.5-4-9s1.5-6.5 4-9z",
        )
    }

    val CheckAll: ImageVector by lazy { strokeIcon("CheckAll", 2.4f, "M2 12l4 4 6-8", "M10 12l4 4 8-10") }

    val Timer: ImageVector by lazy {
        strokeIcon("Timer", 2f, "M4 13a8 8 0 1 0 16 0a8 8 0 1 0-16 0", "M12 9v4", "M10 2h4")
    }

    val TimerBold: ImageVector by lazy {
        strokeIcon("TimerBold", 3f, "M4 13a8 8 0 1 0 16 0a8 8 0 1 0-16 0", "M12 9v4", "M10 2h4")
    }

    val ArrowRight: ImageVector by lazy { strokeIcon("ArrowRight", 2.2f, "M5 12h14", "M13 6l6 6-6 6") }

    val CheckBold: ImageVector by lazy { strokeIcon("CheckBold", 3f, "M20 6L9 17l-5-5") }
    val CloseBold: ImageVector by lazy { strokeIcon("CloseBold", 3f, "M18 6L6 18", "M6 6l12 12") }

    val Vibrate: ImageVector by lazy {
        strokeIcon(
            "Vibrate", 2f,
            "M10 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
            "M4 9v6", "M20 9v6", "M1 10v4", "M23 10v4",
        )
    }

    val Database: ImageVector by lazy {
        strokeIcon(
            "Database", 2f,
            "M4 5a8 3 0 1 0 16 0a8 3 0 1 0-16 0",
            "M4 5v6c0 1.7 3.6 3 8 3s8-1.3 8-3V5",
            "M4 11v6c0 1.7 3.6 3 8 3s8-1.3 8-3v-6",
        )
    }
}
