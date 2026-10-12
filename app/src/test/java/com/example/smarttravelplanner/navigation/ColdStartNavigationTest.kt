package com.example.smarttravelplanner.navigation

import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColdStartNavigationTest {

    @Test
    fun unauthenticatedOrColdStartLoadingReturnsNull() {
        // While auth state is still loading from disk
        val destination = resolveColdStartDestination(
            isLoggedIn = null,
            sessionRole = null,
            onboardingStatus = null
        )
        assertNull("Null isLoggedIn indicates loading state and must yield null destination", destination)
    }

    @Test
    fun loggedOutUserRoutesToLogin() {
        val destination = resolveColdStartDestination(
            isLoggedIn = false,
            sessionRole = null,
            onboardingStatus = null
        )
        assertEquals("login", destination)
    }

    @Test
    fun authenticatedAgentRoutesDirectlyToAgentDashboardWithoutTravellerOnboarding() {
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.AGENT,
            onboardingStatus = null // Agent does not need onboarding status
        )
        assertEquals(AGENT_DASHBOARD_ROUTE, destination)
    }

    @Test
    fun authenticatedTravellerWithNullOnboardingStatusWaitsAndDoesNotAssumeHome() {
        // CRITICAL REQUIREMENT: Cold start must NOT interpret a temporary null onboarding status as finished
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.TRAVELLER,
            onboardingStatus = null // Still loading from DataStore!
        )
        assertNull("Must wait for onboarding state to load; do NOT route to home prematurely", destination)
    }

    @Test
    fun newTravellerWithNotStartedOnboardingRoutesToOnboarding() {
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.TRAVELLER,
            onboardingStatus = OnboardingStatus.NOT_STARTED
        )
        assertEquals(ONBOARDING_PREFERENCES_ROUTE, destination)
    }

    @Test
    fun interruptedTravellerWithInProgressOnboardingRoutesToOnboarding() {
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.TRAVELLER,
            onboardingStatus = OnboardingStatus.IN_PROGRESS
        )
        assertEquals(ONBOARDING_PREFERENCES_ROUTE, destination)
    }

    @Test
    fun returningTravellerWithCompletedOnboardingRoutesToHome() {
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.TRAVELLER,
            onboardingStatus = OnboardingStatus.COMPLETED
        )
        assertEquals("home", destination)
    }

    @Test
    fun returningTravellerWithSkippedOnboardingRoutesToHome() {
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.TRAVELLER,
            onboardingStatus = OnboardingStatus.SKIPPED
        )
        assertEquals("home", destination)
    }

    @Test
    fun legacyTravellerWithLegacyCompletedOnboardingRoutesToHome() {
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = UserRole.TRAVELLER,
            onboardingStatus = OnboardingStatus.LEGACY_COMPLETED
        )
        assertEquals("home", destination)
    }

    @Test
    fun authenticatedTravellerWithRoleStillLoadingReturnsNull() {
        val destination = resolveColdStartDestination(
            isLoggedIn = true,
            sessionRole = null, // Role not yet fetched from Room
            onboardingStatus = OnboardingStatus.COMPLETED
        )
        assertNull("Must wait for sessionRole to resolve before picking start destination", destination)
    }
}
