package com.example.smarttravelplanner.ui.screens.home

import androidx.compose.runtime.Immutable
import com.example.smarttravelplanner.domain.model.Trip

@Immutable
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val trips: List<Trip>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
