package com.example.smarttravelplanner.domain.model

/**
 * Account-scoped personalized travel preferences.
 * Kept strictly isolated per account (keyed by normalized email).
 *
 * @param email Normalized email of the account owning these preferences.
 * @param onboardingStatus Current onboarding stage for this account.
 * @param likedInterestIds Category/interest identifiers swiped right (liked).
 * @param stronglyPreferredInterestIds High-affinity travel preferences (super liked / prioritized).
 * @param dislikedInterestIds Category/interest identifiers swiped left (disliked / avoided).
 * @param preferenceWeights Optional future preference weight mappings (e.g. interestId -> weight).
 */
data class UserTravelPreferences(
    val email: String,
    val onboardingStatus: OnboardingStatus = OnboardingStatus.NOT_STARTED,
    val likedInterestIds: Set<String> = emptySet(),
    val stronglyPreferredInterestIds: Set<String> = emptySet(),
    val dislikedInterestIds: Set<String> = emptySet(),
    val preferenceWeights: Map<String, Float> = emptyMap()
)
