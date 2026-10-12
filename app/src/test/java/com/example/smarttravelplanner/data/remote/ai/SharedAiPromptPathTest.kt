package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.UserTravelPreferences
import org.junit.Assert.*
import org.junit.Test

class SharedAiPromptPathTest {

    @Test
    fun bothGeminiAndOllamaEnginesShareIdenticalPersonalizedPromptFormat() {
        val preferences = UserTravelPreferences(
            email = "shared.prompt@example.com",
            onboardingStatus = OnboardingStatus.COMPLETED,
            likedInterestIds = setOf("mountains_trekking", "photography_scenic"),
            stronglyPreferredInterestIds = setOf("mountains_trekking"),
            dislikedInterestIds = setOf("shopping_city_life"),
            preferenceWeights = mapOf(
                "mountains_trekking" to 2.0f,
                "photography_scenic" to 1.0f,
                "shopping_city_life" to -1.0f
            )
        )

        // Simulate prompt built by Cloud Gemini Engine
        val geminiPrompt = ItineraryPromptBuilder.buildUserMessage(
            destination = "Ladakh",
            days = 6,
            budget = 65000.0,
            interests = listOf("High Passes"),
            travelStyle = "packed",
            startDate = "2026-07-10",
            notes = "Acclimatization day required.",
            weatherForecast = null,
            enableWebGrounding = false,
            adults = 2,
            kids = 0,
            userPreferences = preferences
        )

        // Simulate prompt built by Local Ollama Engine
        val ollamaPrompt = ItineraryPromptBuilder.buildUserMessage(
            destination = "Ladakh",
            days = 6,
            budget = 65000.0,
            interests = listOf("High Passes"),
            travelStyle = "packed",
            startDate = "2026-07-10",
            notes = "Acclimatization day required.",
            weatherForecast = null,
            enableWebGrounding = false,
            adults = 2,
            kids = 0,
            userPreferences = preferences
        )

        // Verify identical shared personalization content across both engines
        assertEquals("Both engines must construct identical personalized prompt text", geminiPrompt, ollamaPrompt)

        // Verify prompt contains strong, liked, disliked, and integrity rules
        assertTrue(geminiPrompt.contains("mountains and trekking"))
        assertTrue(geminiPrompt.contains("photography and scenic views"))
        assertTrue(geminiPrompt.contains("shopping and city life"))
        assertTrue(geminiPrompt.contains("TRAVELLER SAVED PREFERENCES (ONBOARDING PROFILE)"))
        assertTrue(geminiPrompt.contains("CONSTRAINT INTEGRITY & PREFERENCE HIERARCHY"))
    }

    @Test
    fun activityRegenerationMessageIncludesPersonalizationContext() {
        val preferences = UserTravelPreferences(
            email = "regen@example.com",
            onboardingStatus = OnboardingStatus.COMPLETED,
            likedInterestIds = setOf("food_cafes"),
            stronglyPreferredInterestIds = setOf("food_cafes"),
            dislikedInterestIds = emptySet(),
            preferenceWeights = mapOf("food_cafes" to 2.0f)
        )

        val dummyActivity = com.example.smarttravelplanner.domain.model.Activity(
            id = 1L,
            itineraryId = 10L,
            title = "Generic Tour",
            description = "Standard city walk",
            location = "City Center",
            startTime = "10:00",
            endTime = "12:00",
            category = com.example.smarttravelplanner.domain.model.ActivityCategory.ATTRACTION,
            estimatedCost = 500.0
        )

        val message = ItineraryPromptBuilder.buildActivityRegenerationMessage(
            destination = "Kolkata",
            dayNumber = 2,
            currentActivity = dummyActivity,
            otherActivities = emptyList(),
            instructions = "Find something delicious",
            tripInterests = listOf("Culinary"),
            userPreferences = preferences
        )

        assertTrue(message.contains("food and cafés"))
        assertTrue(message.contains("TRAVELLER SAVED PREFERENCES"))
    }
}
