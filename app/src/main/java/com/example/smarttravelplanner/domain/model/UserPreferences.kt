package com.example.smarttravelplanner.domain.model

data class UserPreferences(
    val firstName: String = "",
    val lastName: String = "",
    val fullNameInput: String = "",
    val gender: String = "", // "Male", "Female", "Others"
    val phoneNumber: String = "",
    val phoneCountryCode: String = "+91",
    val isPhoneVerified: Boolean = false,
    val email: String = "",
    val profilePictureUrl: String = "",
    val isGoogleVerified: Boolean = false,
    val homeLocation: String = "",
    val dateOfBirth: String = "",
    val isDobLocked: Boolean = false,
    val maritalStatus: String = "", // "Married", "Single"
    val currency: String = "INR",
    val themeMode: String = "Device Theme",
    val preferredActivities: List<String> = emptyList(),
    val defaultBudget: Double = 25000.0,
    val ollamaModel: String = "qwen3.5:4b",
    // For physical device: replace with your laptop's LAN IP
    val ollamaBaseUrl: String = "http://10.0.2.2:11434/"
) {
    val displayFullName: String
        get() = when {
            fullNameInput.isNotBlank() -> fullNameInput
            firstName.isNotBlank() || lastName.isNotBlank() -> "$firstName $lastName".trim()
            email.isNotBlank() -> email.substringBefore("@")
            else -> "Traveler"
        }

    val profileCompletionPercent: Int
        get() {
            var score = 0
            if (displayFullName.isNotBlank() && displayFullName != "Traveler") score += 20
            if (gender.isNotBlank()) score += 10
            if (phoneNumber.isNotBlank()) score += 20
            if (email.isNotBlank()) score += 20
            if (homeLocation.isNotBlank()) score += 15
            if (dateOfBirth.isNotBlank()) score += 10
            if (maritalStatus.isNotBlank()) score += 5
            return score.coerceIn(0, 100)
        }
}
