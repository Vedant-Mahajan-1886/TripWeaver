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
        val PROFILE_PICTURE_URL = stringPreferencesKey("profile_picture_url")
        val IS_GOOGLE_VERIFIED  = booleanPreferencesKey("is_google_verified")
        val HOME_LOCATION       = stringPreferencesKey("home_location")
        val DATE_OF_BIRTH       = stringPreferencesKey("date_of_birth")
        val IS_DOB_LOCKED       = booleanPreferencesKey("is_dob_locked")
        val MARITAL_STATUS      = stringPreferencesKey("marital_status")
        val CURRENCY            = stringPreferencesKey("currency")
        val THEME_MODE          = stringPreferencesKey("theme_mode")
        val DEFAULT_BUDGET      = doublePreferencesKey("default_budget")
        val AI_PROVIDER         = stringPreferencesKey("ai_provider")
        val CLOUD_MODEL         = stringPreferencesKey("cloud_model")
        val ENABLE_WEB_GROUNDING = booleanPreferencesKey("enable_web_grounding")
        val CLOUD_API_KEY       = stringPreferencesKey("cloud_api_key")
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
        .map { prefs -> prefs.toUserPreferences() }

    // ── Write ─────────────────────────────────────────────────────────────────
    override suspend fun update(prefs: UserPreferences) {
        dataStore.edit { p ->
            p.writeUserPreferences(prefs)
        }
    }

    override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { p ->
            val current = p.toUserPreferences()
            val updated = transform(current)
            p.writeUserPreferences(updated)
        }
    }

    override suspend fun updateThemeMode(themeMode: String) {
        dataStore.edit { p ->
            p[Keys.THEME_MODE] = themeMode
        }
    }

    // ── Serialization Helpers ────────────────────────────────────────────────
    private fun Preferences.toUserPreferences(): UserPreferences {
        return UserPreferences(
            firstName           = this[Keys.FIRST_NAME]          ?: "",
            lastName            = this[Keys.LAST_NAME]           ?: "",
            fullNameInput       = this[Keys.FULL_NAME_INPUT]     ?: "",
            gender              = this[Keys.GENDER]              ?: "",
            phoneNumber         = this[Keys.PHONE_NUMBER]        ?: "",
            phoneCountryCode    = this[Keys.PHONE_COUNTRY_CODE]  ?: "+91",
            isPhoneVerified     = this[Keys.IS_PHONE_VERIFIED]   ?: false,
            email               = this[Keys.EMAIL]               ?: "",
            profilePictureUrl   = this[Keys.PROFILE_PICTURE_URL] ?: "",
            isGoogleVerified    = this[Keys.IS_GOOGLE_VERIFIED]  ?: false,
            homeLocation        = this[Keys.HOME_LOCATION]       ?: "",
            dateOfBirth         = this[Keys.DATE_OF_BIRTH]       ?: "",
            isDobLocked         = this[Keys.IS_DOB_LOCKED]       ?: false,
            maritalStatus       = this[Keys.MARITAL_STATUS]      ?: "",
            currency            = this[Keys.CURRENCY]            ?: "INR",
            themeMode           = this[Keys.THEME_MODE]          ?: "Device Theme",
            defaultBudget       = this[Keys.DEFAULT_BUDGET]      ?: 25000.0,
            aiProvider          = this[Keys.AI_PROVIDER]         ?: "CLOUD",
            cloudModel          = this[Keys.CLOUD_MODEL]         ?: "gemini-3.1-flash-lite",
            enableWebGrounding  = this[Keys.ENABLE_WEB_GROUNDING] ?: false,
            cloudApiKey         = this[Keys.CLOUD_API_KEY]       ?: "",
            ollamaModel         = this[Keys.OLLAMA_MODEL]        ?: "qwen3.5:4b",
            ollamaBaseUrl       = this[Keys.OLLAMA_BASE_URL]     ?: "http://10.0.2.2:11434/",
            preferredActivities = this[Keys.PREFERRED_ACTIVITIES]
                ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    private fun MutablePreferences.writeUserPreferences(prefs: UserPreferences) {
        this[Keys.FIRST_NAME]           = prefs.firstName
        this[Keys.LAST_NAME]            = prefs.lastName
        this[Keys.FULL_NAME_INPUT]      = prefs.fullNameInput
        this[Keys.GENDER]               = prefs.gender
        this[Keys.PHONE_NUMBER]         = prefs.phoneNumber
        this[Keys.PHONE_COUNTRY_CODE]   = prefs.phoneCountryCode
        this[Keys.IS_PHONE_VERIFIED]    = prefs.isPhoneVerified
        this[Keys.EMAIL]                = prefs.email
        this[Keys.PROFILE_PICTURE_URL]  = prefs.profilePictureUrl
        this[Keys.IS_GOOGLE_VERIFIED]   = prefs.isGoogleVerified
        this[Keys.HOME_LOCATION]        = prefs.homeLocation
        this[Keys.DATE_OF_BIRTH]        = prefs.dateOfBirth
        this[Keys.IS_DOB_LOCKED]        = prefs.isDobLocked
        this[Keys.MARITAL_STATUS]       = prefs.maritalStatus
        this[Keys.CURRENCY]             = prefs.currency
        this[Keys.THEME_MODE]           = prefs.themeMode
        this[Keys.DEFAULT_BUDGET]       = prefs.defaultBudget
        this[Keys.AI_PROVIDER]          = prefs.aiProvider
        this[Keys.CLOUD_MODEL]          = prefs.cloudModel
        this[Keys.ENABLE_WEB_GROUNDING] = prefs.enableWebGrounding
        this[Keys.CLOUD_API_KEY]        = prefs.cloudApiKey
        this[Keys.OLLAMA_MODEL]         = prefs.ollamaModel
        this[Keys.OLLAMA_BASE_URL]      = prefs.ollamaBaseUrl
        this[Keys.PREFERRED_ACTIVITIES] = prefs.preferredActivities.joinToString(",")
    }
}
