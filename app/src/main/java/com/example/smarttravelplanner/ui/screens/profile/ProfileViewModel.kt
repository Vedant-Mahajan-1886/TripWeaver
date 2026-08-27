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

    fun updateProfile(firstName: String, lastName: String, phoneNumber: String, email: String) {
        val currentPrefs = (_uiState.value as? ProfileUiState.Success)?.preferences ?: UserPreferences()
        val updated = currentPrefs.copy(
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber,
            email = email
        )
        updatePreferences(updated)
    }

    fun updateTheme(themeMode: String) {
        val currentPrefs = (_uiState.value as? ProfileUiState.Success)?.preferences ?: UserPreferences()
        val updated = currentPrefs.copy(themeMode = themeMode)
        updatePreferences(updated)
    }

    fun updateComprehensiveProfile(
        gender: String,
        fullName: String,
        phone: String,
        email: String,
        homeLocation: String,
        dateOfBirth: String,
        maritalStatus: String
    ) {
        val currentPrefs = (_uiState.value as? ProfileUiState.Success)?.preferences ?: UserPreferences()
        val names = fullName.trim().split(" ", limit = 2)
        val fName = names.getOrNull(0) ?: ""
        val lName = names.getOrNull(1) ?: ""
        val updated = currentPrefs.copy(
            gender = gender,
            fullNameInput = fullName,
            firstName = if (fName.isNotBlank()) fName else currentPrefs.firstName,
            lastName = if (lName.isNotBlank()) lName else currentPrefs.lastName,
            phoneNumber = phone,
            email = email,
            homeLocation = homeLocation,
            dateOfBirth = dateOfBirth,
            maritalStatus = maritalStatus
        )
        updatePreferences(updated)
    }
}
