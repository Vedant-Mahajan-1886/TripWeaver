package com.example.smarttravelplanner.domain.model

/** What kind of account a user has. Stored in the users table as the enum name. */
enum class UserRole(val label: String) {
    /** Plans trips and can send them to agents for review. */
    TRAVELLER("Traveller"),

    /** Receives travellers' itineraries and sends back feedback. */
    AGENT("Travel Agent");

    companion object {
        /** Reads a stored value; unknown or missing values fall back to [TRAVELLER]. */
        fun fromStorage(value: String?): UserRole =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: TRAVELLER
    }
}
