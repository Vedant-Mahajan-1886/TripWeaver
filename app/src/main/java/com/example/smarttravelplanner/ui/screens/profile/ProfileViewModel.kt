package com.example.smarttravelplanner.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.remote.service.GoogleAuthHelper
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDao: UserDao
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

    /**
     * Updates comprehensive profile in both DataStore and Room DB synchronously.
     */
    fun updateComprehensiveProfile(
        gender: String,
        fullName: String,
        phone: String,
        email: String,
        homeLocation: String,
        dateOfBirth: String,
        maritalStatus: String,
        profilePictureUrl: String? = null,
        isGoogleVerified: Boolean? = null,
        isDobLocked: Boolean? = null
    ) {
        val names = fullName.trim().split(" ", limit = 2)
        val fName = names.getOrNull(0) ?: ""
        val lName = names.getOrNull(1) ?: ""
        val current = preferences.value
        val updated = current.copy(
            gender            = gender,
            fullNameInput     = fullName,
            firstName         = fName.ifBlank { current.firstName },
            lastName          = lName.ifBlank { current.lastName },
            phoneNumber       = phone,
            email             = email,
            homeLocation      = homeLocation,
            dateOfBirth       = dateOfBirth,
            maritalStatus     = maritalStatus,
            profilePictureUrl = profilePictureUrl ?: current.profilePictureUrl,
            isGoogleVerified  = isGoogleVerified ?: current.isGoogleVerified,
            isDobLocked       = isDobLocked ?: current.isDobLocked
        )
        updatePreferences(updated)

        // Sync with Room DB
        viewModelScope.launch {
            val targetEmail = if (email.isNotBlank()) email else current.email
            if (targetEmail.isNotBlank()) {
                userDao.updateProfileDetails(
                    email             = targetEmail,
                    fullName          = if (fullName.isNotBlank()) fullName else updated.displayFullName,
                    phoneNumber       = phone,
                    profilePictureUrl = updated.profilePictureUrl.ifBlank { null },
                    gender            = gender,
                    homeLocation      = homeLocation,
                    dateOfBirth       = dateOfBirth,
                    maritalStatus     = maritalStatus
                )
            }
        }
    }

    /**
     * Links and verifies Google account from MyProfileScreen.
     */
    fun linkGoogleAccount(googleUser: GoogleAuthHelper.GoogleUser) {
        val current = preferences.value
        val names = googleUser.displayName.trim().split(" ", limit = 2)
        val fName = names.getOrNull(0) ?: ""
        val lName = names.getOrNull(1) ?: ""

        val updated = current.copy(
            isGoogleVerified  = true,
            profilePictureUrl = googleUser.profilePictureUri ?: current.profilePictureUrl,
            email             = if (current.email.isBlank()) googleUser.email else current.email,
            firstName         = if (current.firstName.isBlank()) fName else current.firstName,
            lastName          = if (current.lastName.isBlank()) lName else current.lastName,
            fullNameInput     = if (current.fullNameInput.isBlank()) googleUser.displayName else current.fullNameInput
        )
        updatePreferences(updated)

        viewModelScope.launch {
            val targetEmail = if (current.email.isNotBlank()) current.email else googleUser.email
            userDao.updateProfilePicture(targetEmail, updated.profilePictureUrl)
        }
    }
}
