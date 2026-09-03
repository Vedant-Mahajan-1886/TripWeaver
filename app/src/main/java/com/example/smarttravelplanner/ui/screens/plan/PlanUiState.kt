package com.example.smarttravelplanner.ui.screens.plan

sealed class PlanUiState {
    object Idle : PlanUiState()

    /**
     * Multi-step loading state — shows progress message to the user.
     * step examples: "Planning your itinerary...", "Saving trip..."
     */
    data class GeneratingItinerary(val step: String = "Planning your itinerary...") : PlanUiState()

    /**
     * Successfully generated. Carries the new tripId for navigation to TripDetailScreen.
     */
    data class ItineraryGenerated(val tripId: Long) : PlanUiState()

    data class Error(val message: String) : PlanUiState()
}
