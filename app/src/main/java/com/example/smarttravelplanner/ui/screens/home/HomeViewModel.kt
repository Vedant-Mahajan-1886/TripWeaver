package com.example.smarttravelplanner.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.domain.usecase.DeleteTripUseCase
import com.example.smarttravelplanner.domain.usecase.GetTripsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getTripsUseCase: GetTripsUseCase,
    private val deleteTripUseCase: DeleteTripUseCase
) : ViewModel() {

    val uiState = getTripsUseCase()
        .map { trips ->
            if (trips.isEmpty()) HomeUiState.Success(emptyList())
            else HomeUiState.Success(trips)
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch { deleteTripUseCase(tripId) }
    }
}
