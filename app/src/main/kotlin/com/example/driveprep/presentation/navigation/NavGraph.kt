package com.example.driveprep.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.driveprep.presentation.exam.ExamScreen
import com.example.driveprep.presentation.home.HomeScreen
import com.example.driveprep.presentation.settings.SettingsScreen
import com.example.driveprep.presentation.statistics.StatisticsScreen
import com.example.driveprep.presentation.wrongbook.WrongBookScreen

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier,
        // 传统 Activity 式转场:前进时新页从右滑入,返回时旧页向右滑出
        enterTransition = {
            slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300))
        }
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                onNavigateToExam = { mode, subject ->
                    navController.navigate(Route.Exam.createRoute(mode, subject)) {
                        launchSingleTop = true
                    }
                },
                onNavigateToWrongBook = {
                    navController.navigate(Route.WrongBook.route) { launchSingleTop = true }
                },
                onNavigateToStatistics = {
                    navController.navigate(Route.Statistics.route) { launchSingleTop = true }
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.route) { launchSingleTop = true }
                }
            )
        }

        composable(
            route = Route.Exam.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("subject") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "mock_exam"
            ExamScreen(
                mode = mode,
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Route.WrongBook.route) {
            WrongBookScreen(
                onNavigateBack = { navController.navigateUp() },
                onStartReview = {
                    navController.navigate(Route.Exam.createRoute("wrong_book")) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Route.Statistics.route) {
            StatisticsScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(Route.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}

sealed class Route(val route: String) {
    object Home : Route("home")
    object WrongBook : Route("wrong_book")
    object Statistics : Route("statistics")
    object Settings : Route("settings")
    object Exam : Route("exam/{mode}?subject={subject}") {
        fun createRoute(mode: String, subject: Int = 1) = "exam/$mode?subject=$subject"
    }
}
