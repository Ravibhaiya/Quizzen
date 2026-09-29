package com.ravibhaiya.quizzen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravibhaiya.quizzen.ui.navigation.QuizzenNavHost
import com.ravibhaiya.quizzen.ui.settings.SettingsViewModel
import com.ravibhaiya.quizzen.ui.theme.QuizzenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
                QuizzenNavHost(
                    hapticEnabled = hapticEnabled,
                    onHapticChange = settings::setHapticEnabled,
                )
            }
        }
    }
}
