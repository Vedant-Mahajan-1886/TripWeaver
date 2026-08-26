package com.example.smarttravelplanner.ui.screens.plan

sealed class PlanUiState {
    object Idle : PlanUiState()
    object GeneratingItinerary : PlanUiState()
    data class ItineraryGenerated(val content: String) : PlanUiState()
    data class Error(val message: String) : PlanUiState()
}
