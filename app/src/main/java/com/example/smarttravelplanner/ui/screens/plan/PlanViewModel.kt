package com.example.smarttravelplanner.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.usecase.GenerateTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val generateTripUseCase: GenerateTripUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /** User's Ollama settings loaded from DataStore */
    val userPreferences = userPreferencesRepository.userPreferences
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.example.smarttravelplanner.domain.model.UserPreferences()
        )

    fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String = "balanced",
        startDate: LocalDate? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val model = userPreferences.value.ollamaModel

            _uiState.value = PlanUiState.GeneratingItinerary("Planning your itinerary with AI...")

            generateTripUseCase(
                destination  = destination,
                days         = days,
                budget       = budget,
                interests    = interests,
                travelStyle  = travelStyle,
                startDate    = startDate,
                notes        = notes,
                model        = model
            ).onSuccess { tripId ->
                _uiState.value = PlanUiState.ItineraryGenerated(tripId)
            }.onFailure { e ->
                _uiState.value = PlanUiState.Error(
                    e.message ?: "Something went wrong. Make sure Ollama is running."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = PlanUiState.Idle
    }
}
