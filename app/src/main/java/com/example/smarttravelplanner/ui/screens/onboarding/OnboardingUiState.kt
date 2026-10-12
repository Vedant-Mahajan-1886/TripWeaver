package com.example.smarttravelplanner.ui.screens.onboarding

import com.example.smarttravelplanner.domain.model.DefaultPreferenceDeck
import com.example.smarttravelplanner.domain.model.PreferenceCardItem
import com.example.smarttravelplanner.domain.model.PreferenceDecision

/**
 * UI State for the Tinder-style travel preferences onboarding deck.
 */
data class OnboardingUiState(
    val deck: List<PreferenceCardItem> = DefaultPreferenceDeck.cards,
    val currentIndex: Int = 0,
    val likedInterestIds: Set<String> = emptySet(),
    val stronglyPreferredInterestIds: Set<String> = emptySet(),
    val dislikedInterestIds: Set<String> = emptySet(),
    val preferenceWeights: Map<String, Float> = emptyMap(),
    val history: List<PreferenceDecision> = emptyList(),
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null
) {
    val currentCard: PreferenceCardItem?
        get() = deck.getOrNull(currentIndex)

    val nextCard: PreferenceCardItem?
        get() = deck.getOrNull(currentIndex + 1)

    val canUndo: Boolean
        get() = history.isNotEmpty() && !isActionInProgress && !isCompleted

    val totalCards: Int
        get() = deck.size

    val progress: Float
        get() = if (deck.isEmpty()) 0f else (currentIndex.toFloat() / deck.size.toFloat()).coerceIn(0f, 1f)

    val isFinishedDeck: Boolean
        get() = currentIndex >= deck.size
}
