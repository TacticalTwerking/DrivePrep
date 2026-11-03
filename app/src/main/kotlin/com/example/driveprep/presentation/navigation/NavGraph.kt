package com.example.driveprep.presentation.navigation

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
        modifier = modifier
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                onNavigateToExam = { mode ->
                    navController.navigate(Route.Exam.createRoute(mode))
                },
                onNavigateToWrongBook = {
                    navController.navigate(Route.WrongBook.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Route.Statistics.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.route)
                }
            )
        }

        composable(
            route = Route.Exam.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
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
                    navController.navigate(Route.Exam.createRoute("wrong_book"))
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
    object Exam : Route("exam/{mode}") {
        fun createRoute(mode: String) = "exam/$mode"
    }
}
