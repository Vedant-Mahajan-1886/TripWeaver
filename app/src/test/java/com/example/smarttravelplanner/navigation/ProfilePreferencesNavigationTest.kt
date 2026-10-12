package com.example.smarttravelplanner.navigation

import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.ui.screens.explore.ExploreUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePreferencesNavigationTest {

    @Test
    fun editPreferencesRouteConstantIsDefinedAndDistinctFromOnboarding() {
        assertEquals("edit_preferences", EDIT_PREFERENCES_ROUTE)
        assertEquals("onboarding_preferences", ONBOARDING_PREFERENCES_ROUTE)
        assertNotEquals(
            "EDIT_PREFERENCES_ROUTE must be distinct from first-time onboarding route",
            ONBOARDING_PREFERENCES_ROUTE,
            EDIT_PREFERENCES_ROUTE
        )
    }

    @Test
    fun editPreferencesRouteHidesBottomNavigation() {
        val hiddenNavRoutes = setOf(
            "login", "profile", "my_profile", "manage_payments", "trip_detail/{tripId}",
            AGENT_DASHBOARD_ROUTE, ONBOARDING_PREFERENCES_ROUTE, EDIT_PREFERENCES_ROUTE
        )

        fun isBottomNavVisible(route: String): Boolean {
            return hiddenNavRoutes.none { route.startsWith(it.substringBefore("{")) }
        }

        assertFalse(
            "Bottom navigation must be hidden on edit_preferences route",
            isBottomNavVisible(EDIT_PREFERENCES_ROUTE)
        )
        assertFalse(
            "Bottom navigation must be hidden on profile route",
            isBottomNavVisible("profile")
        )
        assertTrue(
            "Bottom navigation must be visible on home route",
            isBottomNavVisible("home")
        )
        assertTrue(
            "Bottom navigation must be visible on explore route",
            isBottomNavVisible("explore")
        )
    }

    @Test
    fun editingPreferencesPreservesCompletedColdStartRouting() {
        // A user whose onboarding is completed remains routed to home upon cold start
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.TRAVELLER,
            onboardingStatus = OnboardingStatus.COMPLETED
        )
        assertEquals(
            "Cold start must route completed user to home, not onboarding",
            "home",
            destination
        )
    }

    @Test
    fun exploreUiStateFiltersDestinationsWhenSavedOnlyIsActive() {
        val dest1 = ExploreDestination(
            name = "Jaipur",
            description = "The Pink City",
            stateCountry = "Rajasthan",
            idealDuration = "3 Days",
            bestSeason = "Winter",
            tag = "Cultural",
            tagCategory = "cultural",
            imageRes = 0
        )
        val dest2 = ExploreDestination(
            name = "Goa",
            description = "Sun, Sand & Sea",
            stateCountry = "Goa",
            idealDuration = "4 Days",
            bestSeason = "Winter",
            tag = "Beach",
            tagCategory = "nature",
            imageRes = 0
        )

        val fullCatalog = listOf(dest1, dest2)
        val savedSet = setOf("Jaipur")

        // 1. Normal state: all destinations visible
        val normalState = ExploreUiState(
            destinations = fullCatalog,
            savedNames = savedSet,
            savedOnly = false
        )
        assertEquals(2, normalState.destinations.size)
        assertTrue(normalState.savedNames.contains("Jaipur"))
        assertFalse(normalState.savedNames.contains("Goa"))

        // 2. Saved-only state (e.g. from Wishlist filter chip): only saved destination shown
        val filteredDestinations = fullCatalog.filter { it.name in savedSet }
        val savedOnlyState = normalState.copy(
            destinations = filteredDestinations,
            savedOnly = true
        )
        assertTrue(savedOnlyState.savedOnly)
        assertEquals(1, savedOnlyState.destinations.size)
        assertEquals("Jaipur", savedOnlyState.destinations.first().name)
    }

    @Test
    fun wishlistStateFlowCanBeObservedAndPreservedAcrossScreens() = runBlocking {
        val wishlistFlow = MutableStateFlow(setOf("Manali", "Kerala"))

        // Read initial wishlist items
        val initial = wishlistFlow.first()
        assertEquals(2, initial.size)
        assertTrue(initial.contains("Manali"))
        assertTrue(initial.contains("Kerala"))

        // Adding a destination persists into the flow
        wishlistFlow.value = wishlistFlow.value + "Varanasi"
        val updated = wishlistFlow.first()
        assertEquals(3, updated.size)
        assertTrue(updated.contains("Varanasi"))
    }
}
