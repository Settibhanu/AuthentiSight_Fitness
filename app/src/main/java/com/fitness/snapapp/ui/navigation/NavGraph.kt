package com.fitness.snapapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fitness.snapapp.ui.screens.*

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable(
            route = "workout/{exercise}",
            arguments = listOf(
                navArgument("exercise") {
                    type = NavType.StringType
                    defaultValue = "SQUATS"
                }
            )
        ) { backStackEntry ->
            WorkoutScreen(
                exercise    = backStackEntry.arguments?.getString("exercise") ?: "SQUATS",
                navController = navController
            )
        }

        composable("schedule") {
            ScheduleScreen(navController = navController)
        }

        composable("profile") {
            ProfileScreen(navController = navController)
        }

        composable("challenges") {
            ChallengeScreen(navController = navController)
        }

        composable(
            route = "tutorial/{exercise}",
            arguments = listOf(
                navArgument("exercise") {
                    type = NavType.StringType
                    defaultValue = "SQUATS"
                }
            )
        ) { backStackEntry ->
            TutorialScreen(
                exercise = backStackEntry.arguments?.getString("exercise") ?: "SQUATS",
                navController = navController
            )
        }
    }
}
