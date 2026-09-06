package com.example.smarttravelplanner.ui.screens.mytrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.PhotoRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.usecase.DeleteTripUseCase
import com.example.smarttravelplanner.domain.usecase.GetTripsUseCase
import com.example.smarttravelplanner.util.DestinationImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class MyTripsViewModel @Inject constructor(
    private val getTripsUseCase: GetTripsUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val photoRepository: PhotoRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    // Thread-safe set of already requested trip IDs
    private val resolvedTripIds = ConcurrentHashMap.newKeySet<Long>()

    val uiState = getTripsUseCase()
        .map { trips ->
            if (trips.isEmpty()) MyTripsUiState.Empty
            else MyTripsUiState.Success(trips)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyTripsUiState.Loading
        )

    init {
        // Asynchronously resolve missing cover images on Dispatchers.IO without blocking or cycling UI state
        viewModelScope.launch(Dispatchers.IO) {
            getTripsUseCase().collect { trips ->
                resolveMissingCoverImages(trips)
            }
        }
    }

    private suspend fun resolveMissingCoverImages(trips: List<Trip>) {
        trips.forEach { trip ->
            if (trip.coverImageUrl.isBlank() && !DestinationImageHelper.hasLocalDrawable(trip.destination)) {
                if (resolvedTripIds.add(trip.id)) {
                    photoRepository.getDestinationPhotoUrl(trip.destination).onSuccess { photoUrl ->
                        if (photoUrl.isNotBlank()) {
                            tripRepository.updateTrip(trip.copy(coverImageUrl = photoUrl))
                        }
                    }
                }
            }
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTripUseCase(tripId)
        }
    }
}
