package com.example.smarttravelplanner.ui.screens.agent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.BookingRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripEdit
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AppliedTripChange
import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.QuoteInput
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.util.BookingRules
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.util.AgentConflicts
import com.example.smarttravelplanner.domain.util.AgentReviewValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.smarttravelplanner.data.repository.QuickReplyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AgentTripUiState {
    data object Loading : AgentTripUiState
    data class Error(val message: String) : AgentTripUiState
    data class Success(
        val incoming: IncomingShare,
        val trip: Trip,
        val days: List<Itinerary>,
        val draft: ReviewDraft = ReviewDraft(),
        /** What the traveller changed after this agent's last review (empty if not reviewed yet). */
        val changesSinceReview: List<AppliedTripChange> = emptyList(),
        /** Edits the traveller made by hand after this agent's last review. */
        val editsSinceReview: List<TripEdit> = emptyList(),
        /** Quote / booking details once the traveller has chosen this agent. */
        val booking: Booking? = null,
        /** Booked, but the traveller changed the trip afterwards → agent should re-confirm. */
        val needsReconfirm: Boolean = false,
        /** A booking action is in progress. */
        val isBookingBusy: Boolean = false,
        /** The traveller's rating of this agent for this trip (after completion). */
        val review: com.example.smarttravelplanner.domain.model.agent.AgentReview? = null,
        /** Set once the review is saved; the screen then goes back. */
        val submittedDecision: ReviewDecision? = null
    ) : AgentTripUiState
}

/** A trip a traveller sent to the logged-in agent: view it and review it. */
@HiltViewModel
class AgentTripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val tripEditRepository: TripEditRepository,
    private val bookingRepository: BookingRepository,
    private val quickReplyRepository: QuickReplyRepository
) : ViewModel() {

    /** The agent's saved one-tap messages for review comments. */
    val quickReplies: StateFlow<List<String>> = quickReplyRepository.observeQuickReplies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveQuickReply(text: String) {
        viewModelScope.launch {
            quickReplyRepository.addQuickReply(text)
            _notice.value = "Saved as a quick reply"
        }
    }

    private val _notice = MutableStateFlow<String?>(null)
    /** One-off message after a booking action. */
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun noticeShown() {
        _notice.value = null
    }

    private val shareId: Long = checkNotNull(savedStateHandle["shareId"])

    private val _uiState = MutableStateFlow<AgentTripUiState>(AgentTripUiState.Loading)
    val uiState: StateFlow<AgentTripUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** @param showSpinner false = refresh in place (after a booking action) */
    fun load(showSpinner: Boolean = true) {
        viewModelScope.launch {
            if (showSpinner) _uiState.value = AgentTripUiState.Loading
            runCatching {
                val incoming = agentRepository.observeIncomingShares()
                    .map { list -> list.firstOrNull { it.share.id == shareId } }
                    .first()
                    ?: error("This trip is no longer shared with you.")

                val trip = tripRepository.getTripById(incoming.share.tripId)
                    ?: error("The traveller deleted this trip.")
                val days = itineraryRepository.getItinerariesForTrip(trip.id).sortedBy { it.dayNumber }
                val existingFeedback = agentRepository.observeFeedback(shareId).first()
                val changes = if (incoming.share.decision != null) {
                    AgentConflicts.changesSince(
                        reviewedAt = incoming.share.updatedAt,
                        myShareId = shareId,
                        all = agentRepository.observeFeedbackForTrip(trip.id).first()
                    )
                } else emptyList()
                val edits = if (incoming.share.decision != null) {
                    tripEditRepository.observeEdits(trip.id).first()
                        .filter { it.editedAt > incoming.share.updatedAt }
                } else emptyList()

                val booking = bookingRepository.observeBooking(shareId).first()
                val lastTripChangeAt = listOfNotNull(
                    tripEditRepository.observeEdits(trip.id).first().maxOfOrNull { it.editedAt },
                    agentRepository.observeFeedbackForTrip(trip.id).first()
                        .filter { it.feedback.response == SuggestionResponse.APPLIED }
                        .maxOfOrNull { it.feedback.updatedAt }
                ).maxOrNull()
                val needsReconfirm = BookingRules.needsReconfirm(
                    incoming.share.status, booking?.confirmedAt, lastTripChangeAt
                )

                // Opening the trip tells the traveller it has been viewed
                agentRepository.markViewed(shareId)
                val shown = if (incoming.share.status == ShareStatus.SENT) {
                    incoming.copy(share = incoming.share.copy(status = ShareStatus.VIEWED))
                } else incoming

                AgentTripUiState.Success(
                    incoming = shown,
                    trip = trip,
                    days = days,
                    draft = initialDraft(shown.share, existingFeedback, days),
                    changesSinceReview = changes,
                    editsSinceReview = edits,
                    booking = booking,
                    needsReconfirm = needsReconfirm,
                    review = bookingRepository.observeReviewForShare(shareId).first()
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { e ->
                _uiState.value = AgentTripUiState.Error(e.message ?: "Couldn't open this trip.")
            }
        }
    }

    // ── Booking (after the traveller chose this agent) ───────────────────────

    fun sendQuote(quote: QuoteInput) = bookingAction("Quote sent to the traveller") {
        bookingRepository.sendQuote(shareId, quote)
    }

    fun markBooked(reference: String, notes: String) = bookingAction("Marked as booked") {
        bookingRepository.markBooked(shareId, reference, notes)
    }

    fun reconfirm() = bookingAction("Booking re-confirmed") { bookingRepository.reconfirm(shareId) }

    fun cancelBooking(reason: String) = bookingAction("Cancelled \u2014 the traveller has been told") {
        bookingRepository.cancel(shareId, reason)
    }

    private fun bookingAction(successMessage: String, action: suspend () -> Result<Unit>) {
        val state = _uiState.value as? AgentTripUiState.Success ?: return
        if (state.isBookingBusy) return
        viewModelScope.launch {
            _uiState.update { (it as? AgentTripUiState.Success)?.copy(isBookingBusy = true) ?: it }
            action()
                .onSuccess {
                    _notice.value = successMessage
                    load(showSpinner = false)
                }
                .onFailure { e ->
                    _notice.value = e.message ?: "Something went wrong."
                    _uiState.update { (it as? AgentTripUiState.Success)?.copy(isBookingBusy = false) ?: it }
                }
        }
    }

    // ── Editing the review ───────────────────────────────────────────────────

    fun setVerdict(key: ActivityKey, verdict: ActivityVerdict) = updateActivity(key) { it.copy(verdict = verdict) }

    fun setActivityComment(key: ActivityKey, comment: String) =
        updateActivity(key) { it.copy(comment = comment.take(MAX_TEXT)) }

    fun setSuggestion(key: ActivityKey, suggestion: String) =
        updateActivity(key) { it.copy(suggestion = suggestion.take(MAX_TEXT)) }

    fun setOverallComment(comment: String) = updateDraft { it.copy(overallComment = comment.take(MAX_TEXT)) }

    /** Keeps digits only, max 9 (up to ₹99,99,99,999). */
    fun setPrice(text: String) = updateDraft { it.copy(priceText = text.filter(Char::isDigit).take(9)) }

    // ── Submitting ───────────────────────────────────────────────────────────

    fun submit(decision: ReviewDecision) {
        val state = _uiState.value as? AgentTripUiState.Success ?: return
        if (state.draft.isSubmitting) return

        val submission = buildSubmission(state.days, state.draft, decision)
        val problems = AgentReviewValidator.validate(submission)
        if (problems.isNotEmpty()) {
            updateDraft { it.copy(problems = problems) }
            return
        }

        viewModelScope.launch {
            updateDraft { it.copy(isSubmitting = true, problems = emptyList()) }
            agentRepository.submitReview(shareId, submission)
                .onSuccess {
                    _uiState.update { current ->
                        (current as? AgentTripUiState.Success)?.copy(
                            draft = current.draft.copy(isSubmitting = false),
                            submittedDecision = decision
                        ) ?: current
                    }
                }
                .onFailure { e ->
                    updateDraft {
                        it.copy(isSubmitting = false, problems = listOf(e.message ?: "Couldn't send the review."))
                    }
                }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateActivity(key: ActivityKey, change: (ActivityDraft) -> ActivityDraft) =
        updateDraft { draft ->
            draft.copy(
                activities = draft.activities + (key to change(draft.draftFor(key))),
                problems = emptyList()
            )
        }

    private fun updateDraft(change: (ReviewDraft) -> ReviewDraft) {
        _uiState.update { current ->
            (current as? AgentTripUiState.Success)?.copy(draft = change(current.draft)) ?: current
        }
    }

    private companion object {
        const val MAX_TEXT = 500
    }
}
