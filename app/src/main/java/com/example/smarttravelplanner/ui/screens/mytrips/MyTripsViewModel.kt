package com.example.smarttravelplanner.ui.screens.mytrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyTripsViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    val uiState = tripRepository.getAllTrips()
        .map<_, MyTripsUiState> { trips -> MyTripsUiState.Success(trips) }
        .catch { e -> emit(MyTripsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyTripsUiState.Loading
        )

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch { tripRepository.deleteTrip(tripId) }
    }
}
