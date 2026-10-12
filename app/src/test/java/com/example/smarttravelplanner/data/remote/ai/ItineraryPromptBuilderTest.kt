package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import org.junit.Assert.*
import org.junit.Test

class ItineraryPromptBuilderTest {

    @Test
    fun allTwelveCardIdsMapToCorrectNaturalLanguagePhrases() {
        val expected = mapOf(
            "adventure" to "adventure activities",
            "relaxation" to "relaxing experiences",
            "nature_wildlife" to "nature and wildlife",
            "culture_heritage" to "culture and heritage",
            "food_cafes" to "food and cafés",
            "history_architecture" to "history and architecture",
            "shopping_city_life" to "shopping and city life",
            "photography_scenic" to "photography and scenic views",
            "road_trips" to "road trips",
            "beaches_islands" to "beaches and islands",
            "mountains_trekking" to "mountains and trekking",
            "local_offbeat" to "local and offbeat experiences"
        )

        assertEquals("Must map all 12 broad onboarding interests", 12, expected.size)

        expected.forEach { (id, expectedPhrase) ->
            val actual = ItineraryPromptBuilder.PREFERENCE_ID_TO_NATURAL_LANGUAGE[id]
            assertEquals("Mapping for ID '$id' must match exact natural language requirement", expectedPhrase, actual)
        }
    }

    @Test
    fun strongPreferencesVersusOrdinaryLikesDifferentiatedWithWeights() {
        val preferences = UserTravelPreferences(
            email = "traveler@example.com",
            onboardingStatus = OnboardingStatus.COMPLETED,
            likedInterestIds = setOf("food_cafes", "culture_heritage"),
            stronglyPreferredInterestIds = setOf("food_cafes"),
            dislikedInterestIds = emptySet(),
            preferenceWeights = mapOf(
                "food_cafes" to 2.0f,
                "culture_heritage" to 1.0f
            )
        )

        val promptContext = ItineraryPromptBuilder.formatPreferencesContext(preferences)

        // Strong preference (+2.0) checks
        assertTrue(promptContext.contains("STRONG PREFERENCES (+2.0 Weight - High Priority)"))
        assertTrue(promptContext.contains("food and cafés"))

        // Likes (+1.0) checks
        assertTrue(promptContext.contains("LIKES (+1.0 Weight - Favorable)"))
        assertTrue(promptContext.contains("culture and heritage"))

        // Ensure strong preference is prioritized and separated from ordinary likes
        val strongSection = promptContext.substringAfter("STRONG PREFERENCES").substringBefore("LIKES")
        assertTrue(strongSection.contains("food and cafés"))
        assertFalse(strongSection.contains("culture and heritage"))

        val likesSection = promptContext.substringAfter("LIKES").substringBefore("UNANSWERED")
        assertTrue(likesSection.contains("culture and heritage"))
        assertFalse(likesSection.contains("food and cafés"))
    }

    @Test
    fun dislikedAndUnansweredInterestsAreCorrectlyClassified() {
        val preferences = UserTravelPreferences(
            email = "traveler.dislike@example.com",
            onboardingStatus = OnboardingStatus.COMPLETED,
            likedInterestIds = setOf("beaches_islands"),
            stronglyPreferredInterestIds = emptySet(),
            dislikedInterestIds = setOf("adventure"),
            preferenceWeights = mapOf(
                "beaches_islands" to 1.0f,
                "adventure" to -1.0f
            )
        )

        val promptContext = ItineraryPromptBuilder.formatPreferencesContext(preferences)

        // Disliked (-1.0) soft avoidance check
        assertTrue(promptContext.contains("DISLIKES (-1.0 Weight - De-prioritize / Soft Avoidance)"))
        assertTrue(promptContext.contains("adventure activities"))
        assertTrue(promptContext.contains("Do not treat as absolute exclusions"))

        // Unanswered interests neutral baseline check
        assertTrue(promptContext.contains("UNANSWERED INTERESTS (Neutral baseline):"))
        assertTrue(promptContext.contains("mountains and trekking"))
        assertTrue(promptContext.contains("relaxing experiences"))
        assertTrue(promptContext.contains("neutral; neither favored nor avoided"))
    }

    @Test
    fun preservationOfHardTripConstraints() {
        val preferences = UserTravelPreferences(
            email = "traveler.constraints@example.com",
            onboardingStatus = OnboardingStatus.COMPLETED,
            likedInterestIds = setOf("shopping_city_life"),
            stronglyPreferredInterestIds = setOf("shopping_city_life"),
            dislikedInterestIds = setOf("nature_wildlife"),
            preferenceWeights = mapOf("shopping_city_life" to 2.0f, "nature_wildlife" to -1.0f)
        )

        val userMessage = ItineraryPromptBuilder.buildUserMessage(
            destination = "Manali",
            days = 4,
            budget = 50000.0,
            interests = listOf("Snow Valley Sightseeing"),
            travelStyle = "relaxed",
            startDate = "2026-11-01",
            notes = "Need wheelchair accessible paths for elderly grandmother.",
            weatherForecast = null,
            enableWebGrounding = false,
            adults = 2,
            kids = 1,
            userPreferences = preferences
        )

        // Verify explicit constraints are prominent and present
        assertTrue(userMessage.contains("DESTINATION: Manali"))
        assertTrue(userMessage.contains("DURATION: 4 Days"))
        assertTrue(userMessage.contains("TOTAL TRIP BUDGET: ₹50000 INR"))
        assertTrue(userMessage.contains("TRAVELERS: 2 Adults, 1 Kids"))
        assertTrue(userMessage.contains("CORE INTERESTS: Snow Valley Sightseeing"))
        assertTrue(userMessage.contains("Need wheelchair accessible paths for elderly grandmother."))

        // Verify constraint integrity instruction in prompt
        assertTrue(userMessage.contains("CONSTRAINT INTEGRITY & PREFERENCE HIERARCHY:"))
        assertTrue(userMessage.contains("take STRICT precedence and must NEVER be overridden by these preferences"))
        assertTrue(userMessage.contains("satisfy those explicit requirements first"))
    }

    @Test
    fun emptyPreferencesAndNullPreferencesDoNotInjectPreferencesContext() {
        val nullResult = ItineraryPromptBuilder.formatPreferencesContext(null)
        assertEquals("", nullResult)

        val emptyPrefs = UserTravelPreferences(
            email = "legacy@example.com",
            onboardingStatus = OnboardingStatus.LEGACY_COMPLETED,
            likedInterestIds = emptySet(),
            stronglyPreferredInterestIds = emptySet(),
            dislikedInterestIds = emptySet(),
            preferenceWeights = emptyMap()
        )
        val emptyResult = ItineraryPromptBuilder.formatPreferencesContext(emptyPrefs)
        assertEquals("", emptyResult)

        val messageWithoutPrefs = ItineraryPromptBuilder.buildUserMessage(
            destination = "Goa",
            days = 3,
            budget = 30000.0,
            interests = listOf("Beaches"),
            travelStyle = "relaxed",
            startDate = "2026-12-01",
            notes = "",
            weatherForecast = null,
            userPreferences = null
        )
        assertFalse(messageWithoutPrefs.contains("TRAVELLER SAVED PREFERENCES (ONBOARDING PROFILE)"))
    }

    @Test
    fun skippedUserWithPartialSelectionsPreservesSavedChoices() {
        // User swiped 1 card before clicking "Skip this"
        val skippedWithPartialChoice = UserTravelPreferences(
            email = "skipped@example.com",
            onboardingStatus = OnboardingStatus.SKIPPED,
            likedInterestIds = setOf("relaxation"),
            stronglyPreferredInterestIds = emptySet(),
            dislikedInterestIds = emptySet(),
            preferenceWeights = mapOf("relaxation" to 1.0f)
        )

        val promptContext = ItineraryPromptBuilder.formatPreferencesContext(skippedWithPartialChoice)
        assertTrue("Skipped user with saved choice should still benefit", promptContext.contains("relaxing experiences"))
        assertTrue("Remaining unanswered interests should be neutral", promptContext.contains("adventure activities"))
    }
}
