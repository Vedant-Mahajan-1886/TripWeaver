package com.example.smarttravelplanner.ui.screens.mytrips

import androidx.compose.runtime.Immutable
import com.example.smarttravelplanner.domain.model.Trip

@Immutable
sealed interface MyTripsUiState {
    data object Loading : MyTripsUiState
    data object Empty : MyTripsUiState
    data class Success(val trips: List<Trip>) : MyTripsUiState
    data class Error(val message: String) : MyTripsUiState
}
