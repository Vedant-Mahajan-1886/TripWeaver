package com.example.smarttravelplanner.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarttravelplanner.ui.components.BottomNavigationBar
import com.example.smarttravelplanner.ui.screens.auth.LoginScreen
import com.example.smarttravelplanner.ui.screens.explore.ExploreScreen
import com.example.smarttravelplanner.ui.screens.home.HomeScreen
import com.example.smarttravelplanner.ui.screens.mytrips.MyTripsScreen
import com.example.smarttravelplanner.ui.screens.payment.ManagePaymentsScreen
import com.example.smarttravelplanner.ui.screens.plan.PlanScreen
import com.example.smarttravelplanner.ui.screens.profile.MyProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileViewModel
import com.example.smarttravelplanner.ui.screens.tripdetail.TripDetailScreen

@Composable
fun AppNavigation(profileViewModel: ProfileViewModel) {

    val navController = rememberNavController()

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: "login"

    // Routes where the bottom nav should be hidden
    val hiddenNavRoutes = setOf(
        "login", "profile", "my_profile", "manage_payments", "trip_detail/{tripId}"
    )
    val showBottomNav = hiddenNavRoutes.none { currentRoute.startsWith(it.substringBefore("{")) }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = "login",   // Login stub — bypasses immediately in AuthViewModel
            modifier         = Modifier.padding(innerPadding)
        ) {

            // ── Auth ──────────────────────────────────────────────────────────
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            // ── Main tabs ─────────────────────────────────────────────────────
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
                    onTripClick = { tripId ->
                        navController.navigate("trip_detail/$tripId")
                    },
                    onMenuClick = { navController.navigate("profile") }
                )
            }

            composable("plan") {
                PlanScreen(
                    onMenuClick = { navController.navigate("profile") },
                    onTripGenerated = { tripId ->
                        navController.navigate("trip_detail/$tripId") {
                            popUpTo("plan") { inclusive = false }
                        }
                    }
                )
            }

            composable("mytrips") {
                MyTripsScreen(
                    onTripClick = { tripId ->
                        navController.navigate("trip_detail/$tripId")
                    }
                )
            }

            composable("explore") {
                ExploreScreen(
                    onMenuClick = { navController.navigate("profile") }
                )
            }

            // ── Trip Detail ───────────────────────────────────────────────────
            composable(
                route     = "trip_detail/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) {
                TripDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ── Profile stack ─────────────────────────────────────────────────
            composable("profile") {
                ProfileScreen(
                    viewModel              = profileViewModel,
                    onCloseClick           = { navController.popBackStack() },
                    onNavigateToMyProfile  = { navController.navigate("my_profile") },
                    onNavigateToManagePayments = { navController.navigate("manage_payments") }
                )
            }

            composable("my_profile") {
                MyProfileScreen(onBackClick = { navController.popBackStack() })
            }

            composable("manage_payments") {
                ManagePaymentsScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
