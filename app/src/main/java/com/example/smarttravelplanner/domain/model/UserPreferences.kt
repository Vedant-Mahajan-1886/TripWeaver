package com.example.smarttravelplanner.domain.model

data class UserPreferences(
    val firstName: String = "Pramendra",
    val lastName: String = "Kumar Jaiswal",
    val fullNameInput: String = "Pramendra Kumar Jaiswal",
    val gender: String = "Male", // "Male", "Female", "Others"
    val phoneNumber: String = "8477007224",
    val phoneCountryCode: String = "+91",
    val isPhoneVerified: Boolean = true,
    val email: String = "pramendraj@rediffmail.com",
    val isGoogleVerified: Boolean = false,
    val homeLocation: String = "",
    val dateOfBirth: String = "",
    val isDobLocked: Boolean = false,
    val maritalStatus: String = "", // "Married", "Single"
    val currency: String = "INR",
    val themeMode: String = "Device Theme",
    val preferredActivities: List<String> = emptyList(),
    val defaultBudget: Double = 25000.0,
    val ollamaModel: String = "llama3.2",
    // For physical device: replace with your laptop's LAN IP
    val ollamaBaseUrl: String = "http://10.0.2.2:11434/"
) {
    val displayFullName: String
        get() = if (fullNameInput.isNotBlank()) fullNameInput else "$firstName $lastName".trim()

    val profileCompletionPercent: Int
        get() {
            var score = 0
            if (displayFullName.isNotBlank()) score += 20
            if (gender.isNotBlank()) score += 10
            if (phoneNumber.isNotBlank()) score += 20
            if (email.isNotBlank()) score += 20
            if (homeLocation.isNotBlank()) score += 15
            if (dateOfBirth.isNotBlank()) score += 10
            if (maritalStatus.isNotBlank()) score += 5
            return score.coerceIn(0, 100)
        }
}
