package com.ravibhaiya.quizzen.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ravibhaiya.quizzen.R
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing

/** How long the full-screen logo stays before it fades into the app. */
const val SPLASH_DURATION_MS = 1_100L

/** Wordmark width as a fraction of the screen width. */
private const val WORDMARK_WIDTH_FRACTION = 0.72f

/**
 * Full-screen Quizzen logo: the SVG's gradient + swoosh fill the whole screen (cropped, not letterboxed into a
 * square) with the wordmark centred on top. Shown right after the system splash, whose colour matches this backdrop.
 */
@Composable
fun SplashArt(modifier: Modifier = Modifier) {
    val wordmark = painterResource(R.drawable.ic_wordmark)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(600, easing = EmphasizedEasing)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Swallow touches so nothing underneath can be tapped while the logo is showing.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Image(
            painter = wordmark,
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .fillMaxWidth(WORDMARK_WIDTH_FRACTION)
                .aspectRatio(wordmark.intrinsicSize.width / wordmark.intrinsicSize.height)
                .graphicsLayer {
                    val scale = 0.9f + 0.1f * reveal.value
                    alpha = reveal.value
                    scaleX = scale
                    scaleY = scale
                },
            contentScale = ContentScale.Fit,
        )
    }
}
