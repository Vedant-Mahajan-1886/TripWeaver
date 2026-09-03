package com.example.smarttravelplanner.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /** Reactive preferences — persisted across app kills via DataStore. */
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    /** UI state for ProfileScreen, MyProfileScreen, and MainActivity theme resolution */
    val uiState: StateFlow<ProfileUiState> = userPreferencesRepository.userPreferences
        .map { ProfileUiState.Success(it) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState.Success(UserPreferences())
        )

    // ── Update helpers ────────────────────────────────────────────────────────

    fun updatePreferences(updated: UserPreferences) {
        viewModelScope.launch { userPreferencesRepository.update(updated) }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        email: String
    ) {
        val current = preferences.value
        updatePreferences(
            current.copy(
                firstName   = firstName,
                lastName    = lastName,
                phoneNumber = phoneNumber,
                email       = email
            )
        )
    }

    fun updateTheme(themeMode: String) {
        updatePreferences(preferences.value.copy(themeMode = themeMode))
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
        val names = fullName.trim().split(" ", limit = 2)
        val fName = names.getOrNull(0) ?: ""
        val lName = names.getOrNull(1) ?: ""
        val current = preferences.value
        updatePreferences(
            current.copy(
                gender        = gender,
                fullNameInput = fullName,
                firstName     = fName.ifBlank { current.firstName },
                lastName      = lName.ifBlank { current.lastName },
                phoneNumber   = phone,
                email         = email,
                homeLocation  = homeLocation,
                dateOfBirth   = dateOfBirth,
                maritalStatus = maritalStatus
            )
        )
    }
}
