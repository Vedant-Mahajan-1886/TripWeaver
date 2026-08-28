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

import com.example.smarttravelplanner.ui.screens.payment.ManagePaymentsScreen
import com.example.smarttravelplanner.ui.screens.profile.MyProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileViewModel

@Composable
fun AppNavigation(profileViewModel: ProfileViewModel) {

    val navController = rememberNavController()

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: "home"

    val fullScreenRoutes = setOf("profile", "my_profile", "manage_payments")

    Scaffold(
        bottomBar = {
            if (currentRoute !in fullScreenRoutes) {
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
                    },
                    onMenuClick = {
                        navController.navigate("profile")
                    }
                )
            }

            composable("plan") {
                PlanScreen(
                    onMenuClick = {
                        navController.navigate("profile")
                    }
                )
            }

            composable("mytrips") { MyTripsScreen() }

            composable("explore") {
                ExploreScreen(
                    onMenuClick = {
                        navController.navigate("profile")
                    }
                )
            }

            composable("profile") {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onCloseClick = {
                        navController.popBackStack()
                    },
                    onNavigateToMyProfile = {
                        navController.navigate("my_profile")
                    },
                    onNavigateToManagePayments = {
                        navController.navigate("manage_payments")
                    }
                )
            }

            composable("my_profile") {
                MyProfileScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("manage_payments") {
                ManagePaymentsScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}