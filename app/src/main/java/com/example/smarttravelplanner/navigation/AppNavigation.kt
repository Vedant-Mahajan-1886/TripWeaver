package com.example.smarttravelplanner.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.ui.components.BottomNavigationBar
import com.example.smarttravelplanner.ui.screens.agent.AgentDashboardScreen
import com.example.smarttravelplanner.ui.screens.agent.AgentTripScreen
import com.example.smarttravelplanner.ui.screens.agentgroups.AgentGroupDetailScreen
import com.example.smarttravelplanner.ui.screens.agentgroups.GroupEditorScreen
import com.example.smarttravelplanner.ui.screens.agentpackages.AgentPackagesScreen
import com.example.smarttravelplanner.ui.screens.agentpackages.PackageEditorScreen
import com.example.smarttravelplanner.ui.screens.packages.PackageDetailScreen
import com.example.smarttravelplanner.ui.screens.packages.PackagesScreen
import com.example.smarttravelplanner.ui.screens.tripedit.EditActivitiesScreen
import com.example.smarttravelplanner.ui.screens.tripedit.EditTripScreen
import com.example.smarttravelplanner.ui.screens.auth.AuthViewModel
import com.example.smarttravelplanner.ui.screens.auth.LoginScreen
import com.example.smarttravelplanner.ui.screens.explore.ExploreScreen
import com.example.smarttravelplanner.ui.screens.explore.ExploreViewModel
import com.example.smarttravelplanner.ui.screens.home.HomeScreen
import com.example.smarttravelplanner.ui.screens.mytrips.MyTripsScreen
import com.example.smarttravelplanner.ui.screens.payment.ManagePaymentsScreen
import com.example.smarttravelplanner.ui.screens.plan.PlanScreen
import com.example.smarttravelplanner.ui.screens.profile.MyProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileScreen
import com.example.smarttravelplanner.ui.screens.profile.ProfileViewModel
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.ui.screens.onboarding.OnboardingPreferencesScreen
import com.example.smarttravelplanner.ui.screens.tripdetail.TripDetailScreen
import com.example.smarttravelplanner.ui.theme.Amber


/** Landing screen for travel-agent accounts (no bottom navigation). */
internal const val AGENT_DASHBOARD_ROUTE = "agent_dashboard"

/** Personalized travel preferences onboarding route for new travellers. */
internal const val ONBOARDING_PREFERENCES_ROUTE = "onboarding_preferences"

/** Edit travel preferences route accessed from Profile screen. */
internal const val EDIT_PREFERENCES_ROUTE = "edit_preferences"

/**
 * Determines the cold-start destination based on auth state, role, and onboarding status.
 * Returns null while any necessary state is still loading.
 * Crucially, does NOT interpret a null onboarding status as finished.
 */
internal fun resolveColdStartDestination(
    isLoggedIn: Boolean?,
    sessionRole: UserRole?,
    onboardingStatus: OnboardingStatus?
): String? = when {
    isLoggedIn == false -> "login"
    isLoggedIn == true && sessionRole == UserRole.AGENT -> AGENT_DASHBOARD_ROUTE
    isLoggedIn == true && sessionRole == UserRole.TRAVELLER -> {
        when {
            onboardingStatus == null -> null // still loading; do NOT interpret as finished
            onboardingStatus.isFinished -> "home"
            else -> ONBOARDING_PREFERENCES_ROUTE
        }
    }
    else -> null // still loading auth or role
}

/** Edit a trip's details (opened from the pencil in Trip Detail's header). */
private const val EDIT_TRIP_ROUTE = "edit_trip/{tripId}"

/** Day-by-day activity editor (opened from Edit trip). */
private const val EDIT_ACTIVITIES_ROUTE = "edit_activities/{tripId}"

/** Travellers: browse published packages (optional search), and one package in full. */
private const val PACKAGES_ROUTE = "packages?query={query}"
private const val PACKAGE_DETAIL_ROUTE = "package/{packageId}"

/** Agent's own ready-made packages, and the editor (packageId 0 = new). */
private const val AGENT_PACKAGES_ROUTE = "agent_packages"
private const val AGENT_PACKAGE_EDIT_ROUTE = "agent_package_edit/{packageId}"

/** Agent's group trips: one group with its members, and the editor (groupId 0 = new). */
private const val AGENT_GROUP_ROUTE = "agent_group/{groupId}"
private const val AGENT_GROUP_EDIT_ROUTE = "agent_group_edit/{groupId}"

/** A trip sent to the agent, opened from the dashboard. */
private const val AGENT_TRIP_ROUTE = "agent_trip/{shareId}"

/** SavedStateHandle key used to pre-fill the Plan screen's destination from Explore. */
private const val PLAN_PREFILL_DESTINATION_KEY = "plan_prefill_destination"

@Composable
fun AppNavigation(
    profileViewModel: ProfileViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    exploreViewModel: ExploreViewModel = hiltViewModel()
) {

    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle(initialValue = null)
    val sessionRole by authViewModel.sessionRole.collectAsStateWithLifecycle()
    val sessionOnboardingStatus by authViewModel.sessionOnboardingStatus.collectAsStateWithLifecycle()

    // Pick the start screen once, at app launch: login, Home (traveller) or Agent Dashboard.
    // Kept in remembered state so later login/logout changes never rebuild the NavGraph.
    val startDestinationState = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(isLoggedIn, sessionRole, sessionOnboardingStatus) {
        if (startDestinationState.value == null) {
            startDestinationState.value = resolveColdStartDestination(
                isLoggedIn = isLoggedIn,
                sessionRole = sessionRole,
                onboardingStatus = sessionOnboardingStatus
            )
        }
    }

    val startDestination = startDestinationState.value
    if (startDestination == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Amber,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }
        return
    }

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: startDestination

    // Routes where the bottom nav should be hidden
    val hiddenNavRoutes = setOf(
        "login", "profile", "my_profile", "manage_payments", "trip_detail/{tripId}",
        AGENT_DASHBOARD_ROUTE, AGENT_TRIP_ROUTE, EDIT_TRIP_ROUTE, EDIT_ACTIVITIES_ROUTE,
        AGENT_PACKAGES_ROUTE, AGENT_PACKAGE_EDIT_ROUTE, PACKAGES_ROUTE, PACKAGE_DETAIL_ROUTE,
        ONBOARDING_PREFERENCES_ROUTE, EDIT_PREFERENCES_ROUTE
    )
    val isImeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
    val isNavRoute = hiddenNavRoutes.none { currentRoute.startsWith(it.substringBefore("{")) }

    Scaffold(
        bottomBar = {
            if (!isImeVisible) {
                AnimatedVisibility(
                    visible = isNavRoute,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
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
        }
    ) { innerPadding ->

        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {

            // ── Auth ──────────────────────────────────────────────────────────
            composable(
                route = "login",
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = { isNewAccount ->
                        if (isNewAccount) {
                            navController.navigate(ONBOARDING_PREFERENCES_ROUTE) {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    onAgentLoginSuccess = {
                        navController.navigate(AGENT_DASHBOARD_ROUTE) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            // ── Onboarding Preferences ─────────────────────────────────────────
            composable(ONBOARDING_PREFERENCES_ROUTE) {
                val userPrefs by profileViewModel.preferences.collectAsStateWithLifecycle()
                OnboardingPreferencesScreen(
                    userEmail = userPrefs.email,
                    onComplete = {
                        navController.navigate("home") {
                            popUpTo(ONBOARDING_PREFERENCES_ROUTE) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate("home") {
                            popUpTo(ONBOARDING_PREFERENCES_ROUTE) { inclusive = true }
                        }
                    }
                )
            }

            // ── Travel agent ──────────────────────────────────────────────────
            composable(AGENT_DASHBOARD_ROUTE) {
                AgentDashboardScreen(
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenShare = { shareId -> navController.navigate("agent_trip/$shareId") },
                    onEditPackage = { packageId -> navController.navigate("agent_package_edit/$packageId") },
                    onOpenGroup = { groupId -> navController.navigate("agent_group/$groupId") },
                    onEditGroup = { groupId -> navController.navigate("agent_group_edit/$groupId") }
                )
            }

            composable(
                route = AGENT_TRIP_ROUTE,
                arguments = listOf(navArgument("shareId") { type = NavType.LongType })
            ) {
                AgentTripScreen(onBack = { navController.popBackStack() })
            }

            // ── Packages for travellers ───────────────────────────────────────
            composable(
                route = PACKAGES_ROUTE,
                arguments = listOf(navArgument("query") { type = NavType.StringType; defaultValue = "" })
            ) {
                PackagesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPackage = { id -> navController.navigate("package/$id") }
                )
            }
            composable(
                route = PACKAGE_DETAIL_ROUTE,
                arguments = listOf(navArgument("packageId") { type = NavType.LongType })
            ) {
                PackageDetailScreen(
                    onBack = { navController.popBackStack() },
                    onTripCreated = { tripId ->
                        navController.navigate("trip_detail/$tripId") {
                            popUpTo(PACKAGE_DETAIL_ROUTE) { inclusive = true }
                        }
                    }
                )
            }

            // ── Agent packages ────────────────────────────────────────────────
            composable(AGENT_PACKAGES_ROUTE) {
                AgentPackagesScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { packageId -> navController.navigate("agent_package_edit/$packageId") }
                )
            }
            composable(
                route = AGENT_PACKAGE_EDIT_ROUTE,
                arguments = listOf(navArgument("packageId") { type = NavType.LongType })
            ) {
                PackageEditorScreen(onBack = { navController.popBackStack() })
            }

            // ── Agent group trips ─────────────────────────────────────────────
            composable(
                route = AGENT_GROUP_ROUTE,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) {
                AgentGroupDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { groupId -> navController.navigate("agent_group_edit/$groupId") }
                )
            }
            composable(
                route = AGENT_GROUP_EDIT_ROUTE,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) {
                GroupEditorScreen(onBack = { navController.popBackStack() })
            }

            // ── Main tabs ─────────────────────────────────────────────────────
            composable(
                route = "home",
                enterTransition = {
                    if (initialState.destination.route?.startsWith("login") == true) {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeIn(animationSpec = tween(durationMillis = 300))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route?.startsWith("login") == true) {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeIn(animationSpec = tween(durationMillis = 300))
                    }
                },
                exitTransition = {
                    if (targetState.destination.route?.startsWith("login") == true) {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                        ) + scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeOut(animationSpec = tween(durationMillis = 300))
                    }
                },
                popExitTransition = {
                    if (targetState.destination.route?.startsWith("login") == true) {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            targetOffset = { fullWidth -> (fullWidth * 0.35f).toInt() }
                        ) + scaleOut(
                            targetScale = 0.92f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        )
                    } else {
                        fadeOut(animationSpec = tween(durationMillis = 300))
                    }
                }
            ) {
                HomeScreen(
                    onPlanWithAiClick = {
                        navController.navigate("plan") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onExploreClick = {
                        exploreViewModel.clearSelectedDestination()
                        navController.navigate("explore") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onDestinationClick = { destinationName ->
                        exploreViewModel.selectDestinationByName(destinationName)
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

            composable("plan") { planEntry ->
                // Destination handed over from Explore → "Plan a trip here"
                val prefillDestination by planEntry.savedStateHandle
                    .getStateFlow<String?>(PLAN_PREFILL_DESTINATION_KEY, null)
                    .collectAsStateWithLifecycle()

                PlanScreen(
                    prefillDestination = prefillDestination,
                    onPrefillConsumed = {
                        planEntry.savedStateHandle[PLAN_PREFILL_DESTINATION_KEY] = null
                    },
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
                    },
                    onMenuClick = { navController.navigate("profile") }
                )
            }

            composable("explore") {
                ExploreScreen(
                    viewModel = exploreViewModel,
                    onMenuClick = { navController.navigate("profile") },
                    onPlanTrip = { destinationName ->
                        // Same options as the bottom-nav "Plan" tab so the tab stays highlighted
                        navController.navigate("plan") {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(PLAN_PREFILL_DESTINATION_KEY, destinationName)
                    },
                    onOpenPackage = { id -> navController.navigate("package/$id") },
                    onBrowsePackages = { query -> navController.navigate("packages?query=${android.net.Uri.encode(query)}") }
                )
            }

            // ── Trip Detail ───────────────────────────────────────────────────
            composable(
                route     = "trip_detail/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { entry ->
                val tripId = entry.arguments?.getLong("tripId") ?: 0L
                TripDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onEditClick = { navController.navigate("edit_trip/$tripId") }
                )
            }

            // ── Trip editing ──────────────────────────────────────────────────
            composable(
                route = EDIT_TRIP_ROUTE,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) {
                val tripId = it.arguments?.getLong("tripId") ?: 0L
                EditTripScreen(
                    onBack = { navController.popBackStack() },
                    onEditActivities = { navController.navigate("edit_activities/$tripId") }
                )
            }

            composable(
                route = EDIT_ACTIVITIES_ROUTE,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) {
                EditActivitiesScreen(onBack = { navController.popBackStack() })
            }

            // ── Profile stack ─────────────────────────────────────────────────
            composable("profile") {
                ProfileScreen(
                    viewModel              = profileViewModel,
                    onCloseClick           = { navController.popBackStack() },
                    onNavigateToMyProfile  = { navController.navigate("my_profile") },
                    onNavigateToManagePayments = { navController.navigate("manage_payments") },
                    onNavigateToPreferences = { navController.navigate(EDIT_PREFERENCES_ROUTE) },
                    onNavigateToWishlist   = {
                        exploreViewModel.filterBySavedOnly()
                        val popped = navController.popBackStack("explore", inclusive = false)
                        if (!popped) {
                            navController.navigate("explore") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onLogoutClick          = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("my_profile") {
                MyProfileScreen(
                    viewModel = profileViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("manage_payments") {
                ManagePaymentsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(EDIT_PREFERENCES_ROUTE) {
                val userPrefs by profileViewModel.preferences.collectAsStateWithLifecycle()
                OnboardingPreferencesScreen(
                    userEmail = userPrefs.email,
                    isEditMode = true,
                    onComplete = { navController.popBackStack() },
                    onSkip = { navController.popBackStack() }
                )
            }
        }
    }
}
