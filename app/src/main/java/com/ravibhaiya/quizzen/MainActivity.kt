package com.ravibhaiya.quizzen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.ui.components.SystemBarIconAppearance
import com.ravibhaiya.quizzen.ui.navigation.QuizzenNavHost
import com.ravibhaiya.quizzen.ui.settings.SettingsViewModel
import com.ravibhaiya.quizzen.ui.splash.SPLASH_DURATION_MS
import com.ravibhaiya.quizzen.ui.splash.SplashArt
import com.ravibhaiya.quizzen.ui.theme.QuizzenTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // must run before super.onCreate; swaps the launch theme for Theme.Quizzen
        // Light-only app: always use dark status/navigation bar icons, even when the system is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            QuizzenTheme {
                val settings: SettingsViewModel = viewModel()
                val hapticEnabled by settings.hapticEnabled.collectAsStateWithLifecycle()

                // Survives rotation; a fresh process start shows the logo again.
                var showSplash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(showSplash) {
                    if (showSplash) {
                        delay(SPLASH_DURATION_MS)
                        showSplash = false
                    }
                }
                SystemBarIconAppearance(darkIcons = !showSplash)

                Box(Modifier.fillMaxSize()) {
                    // The app composes underneath so it is ready the moment the logo fades out.
                    QuizzenNavHost(
                        hapticEnabled = hapticEnabled,
                        onHapticChange = settings::setHapticEnabled,
                    )
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(350)),
                    ) {
                        SplashArt()
                    }
                }
            }
        }
    }
}
