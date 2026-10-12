package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    /** Reactive stream of the current user preferences (persisted across app kills). */
    val userPreferences: Flow<UserPreferences>

    /** Persist updated preferences to DataStore. */
    suspend fun update(prefs: UserPreferences)

    /** Atomically update preferences using a transform lambda on current DataStore state. */
    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences)

    /** Update only the theme mode in DataStore without touching any profile fields. */
    suspend fun updateThemeMode(themeMode: String)

    // ── Account-Scoped Travel Preferences & Onboarding ───────────────────────

    /**
     * Reactive stream of account-specific travel preferences and onboarding status.
     * Isolated per account using namespaced DataStore keys derived from [email].
     */
    fun getTravelPreferences(email: String): Flow<UserTravelPreferences>

    /**
     * Reads account-specific travel preferences snapshot once.
     */
    suspend fun getTravelPreferencesOnce(email: String): UserTravelPreferences

    /**
     * Returns the raw persisted onboarding status for [email], or null if uninitialized / legacy.
     */
    suspend fun getRawOnboardingStatus(email: String): OnboardingStatus?

    /**
     * Initializes a fresh account's onboarding state to [OnboardingStatus.NOT_STARTED].
     */
    suspend fun initializeNewAccountOnboarding(email: String)

    /**
     * Updates the onboarding status for [email].
     */
    suspend fun updateOnboardingStatus(email: String, status: OnboardingStatus)

    /**
     * Updates the personalized travel preferences (likes, dislikes, weights) for [email].
     */
    suspend fun updateTravelPreferences(
        email: String,
        liked: Set<String>,
        stronglyPreferred: Set<String> = emptySet(),
        disliked: Set<String> = emptySet(),
        weights: Map<String, Float> = emptyMap()
    )

    /**
     * Safe, idempotent migration for older accounts created before onboarding existed.
     * If no onboarding record exists, sets status to [OnboardingStatus.LEGACY_COMPLETED]
     * without inventing preferences, routing returning users directly to Home.
     */
    suspend fun migrateLegacyAccountIfNeeded(email: String): OnboardingStatus

    /**
     * Resets transient in-memory state for [email] on logout or account switch.
     * Persisted onboarding status, liked/disliked interests, and preference weights
     * in DataStore MUST NOT be deleted.
     */
    suspend fun clearAccountState(email: String)
}
