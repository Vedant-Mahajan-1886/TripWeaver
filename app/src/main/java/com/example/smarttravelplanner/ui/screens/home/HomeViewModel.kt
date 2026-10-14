package com.example.smarttravelplanner.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.domain.usecase.DeleteTripUseCase
import com.example.smarttravelplanner.domain.usecase.GetSoloModeStateUseCase
import com.example.smarttravelplanner.domain.usecase.GetTripsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.smarttravelplanner.data.repository.GroupRepository
import com.example.smarttravelplanner.data.repository.SoloModeRepository
import com.example.smarttravelplanner.domain.model.agent.GroupTripListing
import com.example.smarttravelplanner.domain.util.GroupBrowse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.smarttravelplanner.domain.util.SoloModeState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getTripsUseCase: GetTripsUseCase,
    getSoloModeStateUseCase: GetSoloModeStateUseCase,
    private val deleteTripUseCase: DeleteTripUseCase,
    private val soloModeRepository: SoloModeRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    /** Solo-mode switch + "you usually travel solo" nudge, from the saved choice and the trips. */
    val soloState: StateFlow<SoloModeState> = getSoloModeStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SoloModeState(enabled = false, showNudge = false, detectedSolo = false)
        )

    /**
     * Group trips to suggest on Home. Empty unless Solo mode is on. Full groups are left out
     * (unless I joined), and groups I joined come first.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val soloGroups: StateFlow<List<GroupTripListing>> = soloState
        .map { it.enabled }
        .distinctUntilChanged()
        .flatMapLatest { enabled ->
            if (!enabled) flowOf(emptyList())
            else groupRepository.observeOpenGroups().map { all -> GroupBrowse.filter(all, query = "", hideFull = true).take(6) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
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
