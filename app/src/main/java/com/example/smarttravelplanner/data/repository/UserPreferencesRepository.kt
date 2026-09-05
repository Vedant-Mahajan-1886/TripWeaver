package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    /** Reactive stream of the current user preferences (persisted across app kills). */
    val userPreferences: Flow<UserPreferences>

    /** Persist updated preferences to DataStore. */
    suspend fun update(prefs: UserPreferences)

    /** Atomically update preferences using a transform lambda on current DataStore state. */
    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences)

    /** Update only the theme mode in DataStore without touching any profile fields. */
    suspend fun updateThemeMode(themeMode: String)
}
