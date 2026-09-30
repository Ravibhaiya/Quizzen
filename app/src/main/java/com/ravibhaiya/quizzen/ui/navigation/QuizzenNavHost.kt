package com.ravibhaiya.quizzen.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ravibhaiya.quizzen.domain.PowersRootsRules
import com.ravibhaiya.quizzen.ui.components.EmphasizedEasing
import com.ravibhaiya.quizzen.ui.home.HomeScreen
import com.ravibhaiya.quizzen.ui.multiply.MultiplyConfigScreen
import com.ravibhaiya.quizzen.ui.powers.PowersRootsConfigScreen
import com.ravibhaiya.quizzen.ui.practice.PracticeScreen
import com.ravibhaiya.quizzen.ui.tables.TablesConfigScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp

@Composable
fun QuizzenNavHost(
    hapticEnabled: Boolean,
    onHapticChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val slidePx = with(LocalDensity.current) { 16.dp.roundToPx() }

    // Web `pageIn`: fade + 16px rise over 380ms with the emphasized ease. Outgoing screen just fades quickly.
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        enterTransition = {
            fadeIn(tween(380, easing = EmphasizedEasing)) +
                slideInVertically(tween(380, easing = EmphasizedEasing)) { slidePx }
        },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = {
            fadeIn(tween(380, easing = EmphasizedEasing)) +
                slideInVertically(tween(380, easing = EmphasizedEasing)) { slidePx }
        },
        popExitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                hapticEnabled = hapticEnabled,
                onHapticChange = onHapticChange,
                onOpenMultiply = { navController.navigate(Routes.MULTIPLY) },
                onOpenTables = { navController.navigate(Routes.TABLES) },
                onOpenPowers = { navController.navigate(Routes.POWERS) },
            )
        }
        composable(Routes.MULTIPLY) {
            MultiplyConfigScreen(
                hapticEnabled = hapticEnabled,
                onBack = { navController.popBackStack() },
                onStart = { config -> navController.navigate(Routes.practice(config)) },
            )
        }
        composable(Routes.TABLES) {
            TablesConfigScreen(
                hapticEnabled = hapticEnabled,
                onBack = { navController.popBackStack() },
                onStart = { config -> navController.navigate(Routes.practice(config)) },
            )
        }
        composable(Routes.POWERS) {
            PowersRootsConfigScreen(
                hapticEnabled = hapticEnabled,
                onBack = { navController.popBackStack() },
                onStart = { config -> navController.navigate(Routes.practice(config)) },
            )
        }
        composable(
            route = Routes.PRACTICE,
            arguments = listOf(
                navArgument(Routes.ARG_MODE) { type = NavType.StringType },
                navArgument(Routes.ARG_D1) { type = NavType.IntType; defaultValue = 3 },
                navArgument(Routes.ARG_D2) { type = NavType.IntType; defaultValue = 2 },
                navArgument(Routes.ARG_NUMBERS) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_SECONDS) { type = NavType.IntType; defaultValue = 20 },
                navArgument(Routes.ARG_TYPES) { type = NavType.StringType; defaultValue = "" },
                navArgument(Routes.ARG_MIN) { type = NavType.IntType; defaultValue = PowersRootsRules.DEFAULT_MIN },
                navArgument(Routes.ARG_MAX) { type = NavType.IntType; defaultValue = PowersRootsRules.DEFAULT_MAX },
            ),
        ) {
            PracticeScreen(
                hapticEnabled = hapticEnabled,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
