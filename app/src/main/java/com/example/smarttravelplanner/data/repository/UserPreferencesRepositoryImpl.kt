package com.example.smarttravelplanner.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.smarttravelplanner.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    // ── DataStore keys ────────────────────────────────────────────────────────
    private object Keys {
        val FIRST_NAME          = stringPreferencesKey("first_name")
        val LAST_NAME           = stringPreferencesKey("last_name")
        val FULL_NAME_INPUT     = stringPreferencesKey("full_name_input")
        val GENDER              = stringPreferencesKey("gender")
        val PHONE_NUMBER        = stringPreferencesKey("phone_number")
        val PHONE_COUNTRY_CODE  = stringPreferencesKey("phone_country_code")
        val IS_PHONE_VERIFIED   = booleanPreferencesKey("is_phone_verified")
        val EMAIL               = stringPreferencesKey("email")
        val IS_GOOGLE_VERIFIED  = booleanPreferencesKey("is_google_verified")
        val HOME_LOCATION       = stringPreferencesKey("home_location")
        val DATE_OF_BIRTH       = stringPreferencesKey("date_of_birth")
        val IS_DOB_LOCKED       = booleanPreferencesKey("is_dob_locked")
        val MARITAL_STATUS      = stringPreferencesKey("marital_status")
        val CURRENCY            = stringPreferencesKey("currency")
        val THEME_MODE          = stringPreferencesKey("theme_mode")
        val DEFAULT_BUDGET      = doublePreferencesKey("default_budget")
        val OLLAMA_MODEL        = stringPreferencesKey("ollama_model")
        val OLLAMA_BASE_URL     = stringPreferencesKey("ollama_base_url")
        val PREFERRED_ACTIVITIES = stringPreferencesKey("preferred_activities") // comma-separated
    }

    // ── Read ─────────────────────────────────────────────────────────────────
    override val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { e ->
            // If DataStore file is corrupted, emit empty preferences
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            UserPreferences(
                firstName          = prefs[Keys.FIRST_NAME]         ?: "Pramendra",
                lastName           = prefs[Keys.LAST_NAME]          ?: "Kumar Jaiswal",
                fullNameInput      = prefs[Keys.FULL_NAME_INPUT]    ?: "Pramendra Kumar Jaiswal",
                gender             = prefs[Keys.GENDER]             ?: "Male",
                phoneNumber        = prefs[Keys.PHONE_NUMBER]       ?: "8477007224",
                phoneCountryCode   = prefs[Keys.PHONE_COUNTRY_CODE] ?: "+91",
                isPhoneVerified    = prefs[Keys.IS_PHONE_VERIFIED]  ?: true,
                email              = prefs[Keys.EMAIL]              ?: "pramendraj@rediffmail.com",
                isGoogleVerified   = prefs[Keys.IS_GOOGLE_VERIFIED] ?: false,
                homeLocation       = prefs[Keys.HOME_LOCATION]      ?: "",
                dateOfBirth        = prefs[Keys.DATE_OF_BIRTH]      ?: "",
                isDobLocked        = prefs[Keys.IS_DOB_LOCKED]      ?: false,
                maritalStatus      = prefs[Keys.MARITAL_STATUS]     ?: "",
                currency           = prefs[Keys.CURRENCY]           ?: "INR",
                themeMode          = prefs[Keys.THEME_MODE]         ?: "Device Theme",
                defaultBudget      = prefs[Keys.DEFAULT_BUDGET]     ?: 25000.0,
                ollamaModel        = prefs[Keys.OLLAMA_MODEL]       ?: "llama3.2",
                ollamaBaseUrl      = prefs[Keys.OLLAMA_BASE_URL]    ?: "http://10.0.2.2:11434/",
                preferredActivities = prefs[Keys.PREFERRED_ACTIVITIES]
                    ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            )
        }

    // ── Write ─────────────────────────────────────────────────────────────────
    override suspend fun update(prefs: UserPreferences) {
        dataStore.edit { p ->
            p[Keys.FIRST_NAME]           = prefs.firstName
            p[Keys.LAST_NAME]            = prefs.lastName
            p[Keys.FULL_NAME_INPUT]      = prefs.fullNameInput
            p[Keys.GENDER]               = prefs.gender
            p[Keys.PHONE_NUMBER]         = prefs.phoneNumber
            p[Keys.PHONE_COUNTRY_CODE]   = prefs.phoneCountryCode
            p[Keys.IS_PHONE_VERIFIED]    = prefs.isPhoneVerified
            p[Keys.EMAIL]                = prefs.email
            p[Keys.IS_GOOGLE_VERIFIED]   = prefs.isGoogleVerified
            p[Keys.HOME_LOCATION]        = prefs.homeLocation
            p[Keys.DATE_OF_BIRTH]        = prefs.dateOfBirth
            p[Keys.IS_DOB_LOCKED]        = prefs.isDobLocked
            p[Keys.MARITAL_STATUS]       = prefs.maritalStatus
            p[Keys.CURRENCY]             = prefs.currency
            p[Keys.THEME_MODE]           = prefs.themeMode
            p[Keys.DEFAULT_BUDGET]       = prefs.defaultBudget
            p[Keys.OLLAMA_MODEL]         = prefs.ollamaModel
            p[Keys.OLLAMA_BASE_URL]      = prefs.ollamaBaseUrl
            p[Keys.PREFERRED_ACTIVITIES] = prefs.preferredActivities.joinToString(",")
        }
    }
}
