package com.example.smarttravelplanner.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smarttravelplanner.ui.components.BottomNavigationBar
import com.example.smarttravelplanner.ui.screens.explore.ExploreScreen
import com.example.smarttravelplanner.ui.screens.home.HomeScreen
import com.example.smarttravelplanner.ui.screens.mytrips.MyTripsScreen
import com.example.smarttravelplanner.ui.screens.plan.PlanScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: "home"

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("home") {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {

            composable("home") {
                HomeScreen(
                    onPlanWithAiClick = {
                        navController.navigate("plan") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onExploreClick = {
                        navController.navigate("explore") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMyTripsClick = {
                        navController.navigate("mytrips") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("plan") { PlanScreen() }

            composable("mytrips") { MyTripsScreen() }

            composable("explore") { ExploreScreen() }
        }
    }
}