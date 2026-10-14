package com.example.smarttravelplanner.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The traveller's Solo-mode switch, saved per account. */
interface SoloModeRepository {
    /** true/false = the traveller chose; null = never chosen (the app may suggest it). */
    fun observeSoloMode(): Flow<Boolean?>
    suspend fun setSoloMode(enabled: Boolean)
}

@Singleton
class DataStoreSoloModeRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository
) : SoloModeRepository {

    private fun keyFor(email: String) = booleanPreferencesKey("solo_mode_$email")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSoloMode(): Flow<Boolean?> =
        authRepository.currentUserEmail
            .map { it?.trim()?.lowercase().orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(null) else dataStore.data.map { it[keyFor(email)] }
            }
            .distinctUntilChanged()

    override suspend fun setSoloMode(enabled: Boolean) {
        val email = authRepository.currentUserEmail.first()?.trim()?.lowercase().orEmpty()
        if (email.isEmpty()) return
        dataStore.edit { it[keyFor(email)] = enabled }
    }
}
