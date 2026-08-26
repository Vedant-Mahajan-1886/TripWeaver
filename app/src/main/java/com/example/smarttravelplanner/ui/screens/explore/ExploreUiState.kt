package com.example.smarttravelplanner.ui.screens.explore

import com.example.smarttravelplanner.domain.model.Destination

sealed class ExploreUiState {
    object Idle : ExploreUiState()
    object Loading : ExploreUiState()
    data class Success(val destinations: List<Destination>) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}
