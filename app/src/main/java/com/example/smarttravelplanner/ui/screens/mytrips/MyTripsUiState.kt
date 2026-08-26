package com.example.smarttravelplanner.ui.screens.mytrips

import com.example.smarttravelplanner.domain.model.Trip

sealed class MyTripsUiState {
    object Loading : MyTripsUiState()
    data class Success(val trips: List<Trip>) : MyTripsUiState()
    data class Error(val message: String) : MyTripsUiState()
}
