package com.example.smarttravelplanner.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    // DataStore preferences repository will be wired here
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(
        ProfileUiState.Success(UserPreferences())
    )
    val uiState = _uiState.asStateFlow()

    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Success(preferences)
            // TODO: Persist to DataStore
        }
    }
}
