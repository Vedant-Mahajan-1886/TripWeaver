package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.PhotoRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.usecase.GetTripDetailUseCase
import com.example.smarttravelplanner.domain.usecase.GetWeatherForTripUseCase
import com.example.smarttravelplanner.util.DestinationImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTripDetailUseCase: GetTripDetailUseCase,
    private val getWeatherForTripUseCase: GetWeatherForTripUseCase,
    private val photoRepository: PhotoRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTrip()
    }

    private fun loadTrip() {
        viewModelScope.launch(Dispatchers.IO) {
            // Load the Trip object (one-shot)
            val trip = getTripDetailUseCase.getTrip(tripId)
            if (trip == null) {
                _uiState.update { it.copy(isLoading = false, error = "Trip not found") }
                return@launch
            }
            _uiState.update { it.copy(trip = trip, isLoading = false) }

            android.util.Log.d("TripDetailVM", "Trip loaded: id=${trip.id}, destination='${trip.destination}', coverImageUrl='${trip.coverImageUrl}', hasLocal=${DestinationImageHelper.hasLocalDrawable(trip.destination)}")

            // If the trip lacks an online cover photo and has no local bundled drawable, fetch from Wikipedia
            if (trip.coverImageUrl.isBlank() && !DestinationImageHelper.hasLocalDrawable(trip.destination)) {
                launch(Dispatchers.IO) {
                    android.util.Log.d("TripDetailVM", "Requesting online photo for '${trip.destination}' from Wikipedia...")
                    photoRepository.getDestinationPhotoUrl(trip.destination)
                        .onSuccess { photoUrl ->
                            android.util.Log.d("TripDetailVM", "Got photo URL for '${trip.destination}': '$photoUrl'")
                            if (photoUrl.isNotBlank()) {
                                val updatedTrip = trip.copy(coverImageUrl = photoUrl)
                                tripRepository.updateTrip(updatedTrip)
                                _uiState.update { it.copy(trip = updatedTrip) }
                            }
                        }
                        .onFailure { e ->
                            android.util.Log.e("TripDetailVM", "Error resolving photo for '${trip.destination}'", e)
                        }
                }
            }

            // Collect itineraries reactively
            launch(Dispatchers.IO) {
                getTripDetailUseCase.getItineraries(tripId).collect { itineraries ->
                    _uiState.update { it.copy(itineraries = itineraries) }
                }
            }

            // Fetch real-time weather (Open-Meteo)
            loadWeather(
                destination = trip.destination,
                startDate   = trip.startDate,
                endDate     = trip.endDate
            )
        }
    }

    private fun loadWeather(destination: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingWeather = true) }
            getWeatherForTripUseCase.getForDestination(destination, startDate, endDate)
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
