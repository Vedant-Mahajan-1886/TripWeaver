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
import com.example.smarttravelplanner.data.repository.SoloModeRepository
import com.example.smarttravelplanner.domain.util.SoloDetector
import com.example.smarttravelplanner.domain.util.SoloModeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getTripsUseCase: GetTripsUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val soloModeRepository: SoloModeRepository
) : ViewModel() {

    /** Solo-mode switch + "you usually travel solo" nudge, from the saved choice and the trips. */
    val soloState: StateFlow<SoloModeState> = combine(
        getTripsUseCase(),
        soloModeRepository.observeSoloMode()
    ) { trips, savedChoice -> SoloDetector.state(savedChoice, trips) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SoloModeState(enabled = false, showNudge = false, detectedSolo = false)
        )

    fun setSoloMode(enabled: Boolean) {
        viewModelScope.launch { soloModeRepository.setSoloMode(enabled) }
    }

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
