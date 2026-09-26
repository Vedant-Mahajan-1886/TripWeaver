package com.example.smarttravelplanner.ui.screens.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AgentTripUiState {
    data object Loading : AgentTripUiState
    data class Error(val message: String) : AgentTripUiState
    data class Success(
        val incoming: IncomingShare,
        val trip: Trip,
        val days: List<Itinerary>
    ) : AgentTripUiState
}

/** A trip a traveller sent to the logged-in agent, opened from the dashboard. */
@HiltViewModel
class AgentTripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository
) : ViewModel() {

    private val shareId: Long = checkNotNull(savedStateHandle["shareId"])

    private val _uiState = MutableStateFlow<AgentTripUiState>(AgentTripUiState.Loading)
    val uiState: StateFlow<AgentTripUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AgentTripUiState.Loading
            runCatching {
                val incoming = agentRepository.observeIncomingShares()
                    .map { list -> list.firstOrNull { it.share.id == shareId } }
                    .first()
                    ?: error("This trip is no longer shared with you.")

                val trip = tripRepository.getTripById(incoming.share.tripId)
                    ?: error("The traveller deleted this trip.")
                val days = itineraryRepository.getItinerariesForTrip(trip.id).sortedBy { it.dayNumber }

                // Opening the trip tells the traveller it has been viewed
                agentRepository.markViewed(shareId)
                val shown = if (incoming.share.status == ShareStatus.SENT) {
                    incoming.copy(share = incoming.share.copy(status = ShareStatus.VIEWED))
                } else incoming

                AgentTripUiState.Success(shown, trip, days)
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { e ->
                _uiState.update { AgentTripUiState.Error(e.message ?: "Couldn't open this trip.") }
            }
        }
    }
}
