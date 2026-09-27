package com.example.smarttravelplanner.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.remote.service.GoogleAuthHelper
import com.example.smarttravelplanner.data.repository.AuthRepository
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.data.repository.WishlistRepository
import com.example.smarttravelplanner.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    /** Number of destinations saved in the wishlist database */
    val savedCount: StateFlow<Int> = wishlistRepository.savedDestinations
        .map { it.size }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = 0
        )

    /** Reactive preferences — persisted across app kills via DataStore. Eagerly shared to prevent state loss. */
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = UserPreferences()
        )

    /** UI state for ProfileScreen, MyProfileScreen, and MainActivity theme resolution */
    val uiState: StateFlow<ProfileUiState> = preferences
        .map { ProfileUiState.Success(it) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = ProfileUiState.Success(UserPreferences())
        )

    init {
        // Auto-recover profile from Room DB if DataStore profile fields are blank for the logged-in user
        viewModelScope.launch {
            authRepository.currentUserEmail.collectLatest { loggedInEmail ->
                if (!loggedInEmail.isNullOrBlank()) {
                    val user = userDao.getUserByEmail(loggedInEmail)
                    if (user != null) {
                        userPreferencesRepository.updatePreferences { current ->
                            if (current.email.isBlank() || (current.firstName.isBlank() && current.fullNameInput.isBlank())) {
                                val names = user.fullName.trim().split(" ", limit = 2)
                                current.copy(
                                    email             = user.email,
                                    fullNameInput     = user.fullName,
                                    firstName         = names.getOrNull(0) ?: "",
                                    lastName          = names.getOrNull(1) ?: "",
                                    phoneNumber       = user.phoneNumber,
                                    profilePictureUrl = user.profilePictureUrl ?: current.profilePictureUrl,
                                    gender            = user.gender,
                                    homeLocation      = user.homeLocation,
                                    dateOfBirth       = user.dateOfBirth,
                                    maritalStatus     = user.maritalStatus,
                                    isGoogleVerified  = user.isGoogleUser
                                )
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }
    }

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
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { current ->
                current.copy(
                    firstName   = firstName,
                    lastName    = lastName,
                    phoneNumber = phoneNumber,
                    email       = email
                )
            }
        }
    }

    /**
     * Updates only theme mode in DataStore without touching or erasing any profile fields.
     */
    fun updateTheme(themeMode: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateThemeMode(themeMode)
        }
    }

    /**
     * Updates comprehensive profile in both DataStore and Room DB synchronously and atomically.
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

        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { current ->
                current.copy(
                    gender            = gender,
                    fullNameInput     = fullName,
                    firstName         = fName.ifBlank { current.firstName },
                    lastName          = lName.ifBlank { current.lastName },
                    phoneNumber       = phone,
                    email             = email.ifBlank { current.email },
                    homeLocation      = homeLocation,
                    dateOfBirth       = dateOfBirth,
                    maritalStatus     = maritalStatus,
                    profilePictureUrl = profilePictureUrl ?: current.profilePictureUrl,
                    isGoogleVerified  = isGoogleVerified ?: current.isGoogleVerified,
                    isDobLocked       = isDobLocked ?: current.isDobLocked
                )
            }

            // Sync with Room DB
            val targetEmail = email.ifBlank { preferences.value.email }
            if (targetEmail.isNotBlank()) {
                userDao.updateProfileDetails(
                    email             = targetEmail,
                    fullName          = fullName.ifBlank { preferences.value.displayFullName },
                    phoneNumber       = phone,
                    profilePictureUrl = (profilePictureUrl ?: preferences.value.profilePictureUrl).ifBlank { null },
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
        val names = googleUser.displayName.trim().split(" ", limit = 2)
        val fName = names.getOrNull(0) ?: ""
        val lName = names.getOrNull(1) ?: ""

        viewModelScope.launch {
            userPreferencesRepository.updatePreferences { current ->
                current.copy(
                    isGoogleVerified  = true,
                    profilePictureUrl = googleUser.profilePictureUri ?: current.profilePictureUrl,
                    email             = if (current.email.isBlank()) googleUser.email else current.email,
                    firstName         = if (current.firstName.isBlank()) fName else current.firstName,
                    lastName          = if (current.lastName.isBlank()) lName else current.lastName,
                    fullNameInput     = if (current.fullNameInput.isBlank()) googleUser.displayName else current.fullNameInput
                )
            }

            val targetEmail = preferences.value.email.ifBlank { googleUser.email }
            userDao.updateProfilePicture(targetEmail, googleUser.profilePictureUri)
        }
    }
}
