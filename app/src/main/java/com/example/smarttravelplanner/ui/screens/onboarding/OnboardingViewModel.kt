package com.example.smarttravelplanner.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AuthRepository
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.CardAction
import com.example.smarttravelplanner.domain.model.DefaultPreferenceDeck
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.PreferenceCardItem
import com.example.smarttravelplanner.domain.model.PreferenceDecision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    internal var scopeOverride: CoroutineScope? = null
    private val activeScope: CoroutineScope get() = scopeOverride ?: viewModelScope

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var targetEmail: String? = null
    var isEditMode: Boolean = false

    fun startWithSessionUser(isEdit: Boolean = this.isEditMode): Job = activeScope.launch {
        this@OnboardingViewModel.isEditMode = isEdit
        val email = authRepository.currentUserEmail.first()
        if (!email.isNullOrBlank()) {
            loadPreferencesForAccount(email, isEdit)
        }
    }

    /**
     * Initializes or switches the onboarding account context.
     * Restores any previously saved decisions. In edit mode, starts at card 0 so the user
     * can review and update all cards. In first-time onboarding mode, resumes at the first unrated interest.
     */
    fun loadPreferencesForAccount(email: String, isEdit: Boolean = this.isEditMode): Job {
        this.isEditMode = isEdit
        val normalized = email.trim().lowercase()
        targetEmail = normalized

        return activeScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                userPreferencesRepository.getTravelPreferencesOnce(normalized)
            }.onSuccess { existingPrefs ->
                val deck = DefaultPreferenceDeck.cards
                val decidedIds = existingPrefs.likedInterestIds + existingPrefs.dislikedInterestIds

                // In edit mode, start at card 0 to allow reviewing and changing existing decisions.
                // In onboarding mode, resume at the first undecided card, or at card 0 if none.
                val resumeIndex = if (isEdit) {
                    0
                } else {
                    var idx = 0
                    for ((index, card) in deck.withIndex()) {
                        if (card.id in decidedIds) {
                            idx = index + 1
                        } else {
                            idx = index
                            break
                        }
                    }
                    idx.coerceIn(0, deck.size)
                }

                _uiState.update { current ->
                    current.copy(
                        deck = deck,
                        currentIndex = resumeIndex,
                        likedInterestIds = existingPrefs.likedInterestIds,
                        stronglyPreferredInterestIds = existingPrefs.stronglyPreferredInterestIds,
                        dislikedInterestIds = existingPrefs.dislikedInterestIds,
                        preferenceWeights = existingPrefs.preferenceWeights,
                        isLoading = false,
                        isActionInProgress = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * Shared action handler for gestures and buttons with deduplication against double submissions.
     */
    fun onCardAction(action: CardAction): Job? {
        val current = _uiState.value
        if (current.isActionInProgress || current.isFinishedDeck) return null

        val card = current.currentCard ?: return null
        val email = targetEmail ?: return null

        // 1. Lock actions immediately to prevent duplicate gesture submissions
        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }

        // 2. Compute updated sets & weights
        val cardId = card.id
        val prevWeight = current.preferenceWeights[cardId]
        val prevLiked = cardId in current.likedInterestIds
        val prevStrong = cardId in current.stronglyPreferredInterestIds
        val prevDisliked = cardId in current.dislikedInterestIds

        val newLiked = when (action) {
            CardAction.LIKE, CardAction.STRONG_PREFERENCE -> current.likedInterestIds + cardId
            CardAction.DISLIKE -> current.likedInterestIds - cardId
        }

        val newStrong = when (action) {
            CardAction.STRONG_PREFERENCE -> current.stronglyPreferredInterestIds + cardId
            CardAction.LIKE, CardAction.DISLIKE -> current.stronglyPreferredInterestIds - cardId
        }

        val newDisliked = when (action) {
            CardAction.DISLIKE -> current.dislikedInterestIds + cardId
            CardAction.LIKE, CardAction.STRONG_PREFERENCE -> current.dislikedInterestIds - cardId
        }

        val newWeights = current.preferenceWeights + (cardId to action.weight)
        val newHistory = current.history + PreferenceDecision(
            cardId = cardId,
            action = action,
            cardIndex = current.currentIndex,
            previousWeight = prevWeight,
            previouslyLiked = prevLiked,
            previouslyStrong = prevStrong,
            previouslyDisliked = prevDisliked
        )

        val nextIndex = current.currentIndex + 1
        val isFinalCard = nextIndex >= current.deck.size

        // 3. Persist incrementally
        return activeScope.launch {
            runCatching {
                userPreferencesRepository.updateTravelPreferences(
                    email = email,
                    liked = newLiked,
                    stronglyPreferred = newStrong,
                    disliked = newDisliked,
                    weights = newWeights
                )

                if (isFinalCard) {
                    if (!isEditMode) {
                        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)
                    }
                } else {
                    if (!isEditMode) {
                        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.IN_PROGRESS)
                    }
                }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        currentIndex = nextIndex,
                        likedInterestIds = newLiked,
                        stronglyPreferredInterestIds = newStrong,
                        dislikedInterestIds = newDisliked,
                        preferenceWeights = newWeights,
                        history = newHistory,
                        isCompleted = isFinalCard && !isEditMode,
                        isActionInProgress = false
                    )
                }
            }.onFailure { error ->
                // Persistence failed: do NOT mark complete or advance card
                _uiState.update { state ->
                    state.copy(
                        isActionInProgress = false,
                        errorMessage = error.message ?: "Failed to save preference. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Undoes the most recent decision, rolling back index and persisted state.
     */
    fun undoLastAction(): Job? {
        val current = _uiState.value
        if (!current.canUndo) return null

        val email = targetEmail ?: return null
        val lastDecision = current.history.lastOrNull() ?: return null

        _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }

        val cardId = lastDecision.cardId
        val revertedLiked = if (lastDecision.previouslyLiked) {
            current.likedInterestIds + cardId
        } else {
            current.likedInterestIds - cardId
        }
        val revertedStrong = if (lastDecision.previouslyStrong) {
            current.stronglyPreferredInterestIds + cardId
        } else {
            current.stronglyPreferredInterestIds - cardId
        }
        val revertedDisliked = if (lastDecision.previouslyDisliked) {
            current.dislikedInterestIds + cardId
        } else {
            current.dislikedInterestIds - cardId
        }
        val revertedWeights = if (lastDecision.previousWeight != null) {
            current.preferenceWeights + (cardId to lastDecision.previousWeight)
        } else {
            current.preferenceWeights - cardId
        }
        val revertedHistory = current.history.dropLast(1)
        val revertedIndex = lastDecision.cardIndex

        return activeScope.launch {
            runCatching {
                userPreferencesRepository.updateTravelPreferences(
                    email = email,
                    liked = revertedLiked,
                    stronglyPreferred = revertedStrong,
                    disliked = revertedDisliked,
                    weights = revertedWeights
                )
                if (!isEditMode) {
                    userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.IN_PROGRESS)
                }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        currentIndex = revertedIndex,
                        likedInterestIds = revertedLiked,
                        stronglyPreferredInterestIds = revertedStrong,
                        dislikedInterestIds = revertedDisliked,
                        preferenceWeights = revertedWeights,
                        history = revertedHistory,
                        isCompleted = false,
                        isActionInProgress = false
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isActionInProgress = false,
                        errorMessage = error.message ?: "Failed to undo. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Explicitly skips preferences onboarding.
     * Preserves all previously selected decisions and does not mark unanswered cards as dislikes.
     */
    fun skipOnboarding(): Job? {
        val email = targetEmail ?: return null
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        return activeScope.launch {
            runCatching {
                if (!isEditMode) {
                    userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.SKIPPED)
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isSkipped = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to skip onboarding. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Completes onboarding early with existing selections if user is on the summary screen.
     */
    fun completeOnboarding(): Job? {
        val email = targetEmail ?: return null
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        return activeScope.launch {
            runCatching {
                if (!isEditMode) {
                    userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)
                }
            }.onSuccess {
                _uiState.update { it.copy(isLoading = false, isCompleted = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to save preferences. Please try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
