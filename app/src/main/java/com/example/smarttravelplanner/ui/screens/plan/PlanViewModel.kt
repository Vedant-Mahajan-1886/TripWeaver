package com.example.smarttravelplanner.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun generateItinerary(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        model: String = "llama3.2"
    ) {
        viewModelScope.launch {
            _uiState.value = PlanUiState.GeneratingItinerary
            aiRepository.generateItinerary(destination, days, budget, interests, model)
                .onSuccess { content -> _uiState.value = PlanUiState.ItineraryGenerated(content) }
                .onFailure { e -> _uiState.value = PlanUiState.Error(e.message ?: "AI error") }
        }
    }

    suspend fun saveTrip(trip: Trip): Long = tripRepository.saveTrip(trip)

    fun resetState() { _uiState.value = PlanUiState.Idle }
}
