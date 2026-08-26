package com.example.smarttravelplanner.ui.screens.profile

import com.example.smarttravelplanner.domain.model.UserPreferences

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val preferences: UserPreferences) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}
