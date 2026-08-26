package com.example.smarttravelplanner.ui.screens.home

import com.example.smarttravelplanner.domain.model.Trip

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val trips: List<Trip>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
