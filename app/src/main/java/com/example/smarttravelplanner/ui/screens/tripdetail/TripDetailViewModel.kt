package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.PlacesRepository
import com.example.smarttravelplanner.domain.usecase.GetTripDetailUseCase
import com.example.smarttravelplanner.domain.usecase.GetWeatherForTripUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTripDetailUseCase: GetTripDetailUseCase,
    private val getWeatherForTripUseCase: GetWeatherForTripUseCase,
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTrip()
    }

    private fun loadTrip() {
        viewModelScope.launch {
            // Load the Trip object (one-shot)
            val trip = getTripDetailUseCase.getTrip(tripId)
            if (trip == null) {
                _uiState.update { it.copy(isLoading = false, error = "Trip not found") }
                return@launch
            }
            _uiState.update { it.copy(trip = trip, isLoading = false) }

            // Collect itineraries reactively
            launch {
                getTripDetailUseCase.getItineraries(tripId).collect { itineraries ->
                    _uiState.update { it.copy(itineraries = itineraries) }
                }
            }

            // Geocode destination and fetch real-time weather (Open-Meteo)
            val coords = placesRepository.geocodeDestination(trip.destination)
                .getOrDefault(Pair(28.6139, 77.2090))
            loadWeather(
                lat       = coords.first,
                lng       = coords.second,
                startDate = trip.startDate.toString(),
                endDate   = trip.endDate.toString()
            )
        }
    }

    private fun loadWeather(lat: Double, lng: Double, startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true) }
            getWeatherForTripUseCase(lat, lng, startDate, endDate)
                .onSuccess { weather ->
                    _uiState.update { it.copy(weather = weather, isLoadingWeather = false) }
                }
                .onFailure {
                    // Weather is non-critical — silently fail
                    _uiState.update { it.copy(isLoadingWeather = false) }
                }
        }
    }

    fun selectDay(index: Int) {
        _uiState.update { it.copy(selectedDayIndex = index) }
    }
}
