package com.example.smarttravelplanner.domain.model

/**
 * Represents the onboarding state for a traveller account.
 *
 * - [NOT_STARTED]: Account is newly created and has not begun preferences onboarding yet.
 * - [IN_PROGRESS]: Account started the onboarding process but has not completed or skipped.
 * - [COMPLETED]: Account finished onboarding and personalized preferences.
 * - [SKIPPED]: Account opted to skip onboarding.
 * - [LEGACY_COMPLETED]: Older account created before preferences onboarding existed; migrated without forcing onboarding.
 */
enum class OnboardingStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    LEGACY_COMPLETED;

    val isFinished: Boolean
        get() = this == COMPLETED || this == SKIPPED || this == LEGACY_COMPLETED

    companion object {
        fun fromStorage(value: String?): OnboardingStatus = when (value?.uppercase()?.trim()) {
            "NOT_STARTED" -> NOT_STARTED
            "IN_PROGRESS" -> IN_PROGRESS
            "COMPLETED" -> COMPLETED
            "SKIPPED" -> SKIPPED
            "LEGACY_COMPLETED", "LEGACY" -> LEGACY_COMPLETED
            else -> NOT_STARTED
        }
    }
}
