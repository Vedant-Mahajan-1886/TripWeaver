package com.example.smarttravelplanner.ui.screens.agentshare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.TripEdit
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.TripFeedbackItem
import com.example.smarttravelplanner.domain.model.agent.TripShare
import com.example.smarttravelplanner.domain.usecase.ApplyAgentSuggestionUseCase
import com.example.smarttravelplanner.domain.util.AgentConflicts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One agent row in the "Send to agent" sheet. */
data class AgentShareRow(
    val agent: AgentProfile,
    /** Latest share of this trip with this agent, if any (withdrawn ones count as "not shared"). */
    val share: TripShare?
) {
    val isShared: Boolean get() = share != null && share.status != ShareStatus.WITHDRAWN
}

/** One agent's offer, summarised for side-by-side comparison. */
data class AgentOffer(
    val row: AgentShareRow,
    val flaggedCount: Int,
    val appliedCount: Int,
    /** The traveller changed the trip after this agent reviewed it. */
    val reviewedBeforeLatestChanges: Boolean,
    val isLowestPrice: Boolean
) {
    val share: TripShare get() = row.share!!
    /** The agent has sent a review (it may since have been chosen or closed). */
    val hasResponded: Boolean get() = share.decision != null
}

data class SendToAgentUiState(
    val isLoading: Boolean = true,
    val rows: List<AgentShareRow> = emptyList(),
    val selectedEmails: Set<String> = emptySet(),
    val message: String = "",
    val isSending: Boolean = false,
    /** One-off feedback shown as a snackbar/toast, then cleared. */
    val notice: String? = null,
    /** Activities that more than one agent has an open suggestion for. */
    val conflicts: Set<AgentConflicts.ActivityRef> = emptySet(),
    /** Every agent that has (or had) this trip, for "Compare offers". */
    val offers: List<AgentOffer> = emptyList()
) {
    /** The agent the traveller chose, if any. */
    val chosen: AgentShareRow? get() = rows.firstOrNull { it.share?.status == ShareStatus.CONFIRMED }

    /** Agents who approved the plan and are still in the running. */
    val approvedRows: List<AgentShareRow>
        get() = rows.filter { row ->
            val share = row.share
            share != null && share.status == ShareStatus.RESPONDED && share.decision == ReviewDecision.APPROVED
        }

    val respondedOffers: List<AgentOffer> get() = offers.filter { it.hasResponded }

    /** Agents this trip is currently shared with (not withdrawn). */
    val activeShareCount: Int get() = rows.count { it.isShared }

    /** Agents who have sent their review back. */
    val respondedCount: Int get() = rows.count { it.share?.status == ShareStatus.RESPONDED }
    val canSend: Boolean get() = selectedEmails.isNotEmpty() && !isSending
}

/** One agent's review of this trip, as the traveller sees it. */
data class AgentFeedbackUiState(
    val row: AgentShareRow,
    val feedback: List<ActivityFeedback>,
    /** Feedback ids currently being applied/declined (buttons disabled meanwhile). */
    val busyIds: Set<Long> = emptySet(),
    val conflicts: Set<AgentConflicts.ActivityRef> = emptySet()
) {
    fun hasConflict(feedback: ActivityFeedback): Boolean =
        AgentConflicts.ActivityRef(feedback.dayNumber, feedback.activityTitle) in conflicts

    /** Activities the agent flagged (not simply "Available"). */
    val flagged: List<ActivityFeedback> get() = feedback.filter { it.verdict != ActivityVerdict.AVAILABLE }
    val confirmedCount: Int get() = feedback.count { it.verdict == ActivityVerdict.AVAILABLE }
}

/**
 * Traveller side of the agent feature for one trip: pick agents, add a message,
 * send, see / withdraw existing shares, and read + apply agents' feedback.
 * Scoped to the Trip Detail screen.
 */
@HiltViewModel
class SendToAgentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository,
    private val applyAgentSuggestion: ApplyAgentSuggestionUseCase,
    tripEditRepository: TripEditRepository
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val selection = MutableStateFlow(Selection())

    private data class Selection(
        val selectedEmails: Set<String> = emptySet(),
        val message: String = "",
        val isSending: Boolean = false,
        val notice: String? = null
    )

    val uiState: StateFlow<SendToAgentUiState> = combine(
        agentRepository.observeAgents(),
        agentRepository.observeSharesForTrip(tripId),
        agentRepository.observeFeedbackForTrip(tripId),
        tripEditRepository.observeEdits(tripId),
        selection
    ) { agents, shares, tripFeedback, edits, sel ->
        val latestByAgent = shares
            .groupBy { it.agentEmail }
            .mapValues { (_, list) -> list.maxByOrNull { it.updatedAt } }

        SendToAgentUiState(
            isLoading = false,
            rows = agents.map { AgentShareRow(it, latestByAgent[it.email]) },
            // Drop selections for agents that now have the trip
            selectedEmails = sel.selectedEmails.filterTo(mutableSetOf()) { email ->
                latestByAgent[email]?.status.let { it == null || it == ShareStatus.WITHDRAWN }
            },
            message = sel.message,
            isSending = sel.isSending,
            notice = sel.notice,
            conflicts = AgentConflicts.conflictingActivities(tripFeedback),
            offers = buildOffers(agents.map { AgentShareRow(it, latestByAgent[it.email]) }, tripFeedback, edits)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SendToAgentUiState()
    )

    fun toggleAgent(email: String) = selection.update { sel ->
        val updated = if (email in sel.selectedEmails) sel.selectedEmails - email else sel.selectedEmails + email
        sel.copy(selectedEmails = updated)
    }

    fun onMessageChange(message: String) = selection.update { it.copy(message = message.take(MAX_MESSAGE)) }

    fun send() {
        val current = selection.value
        if (current.selectedEmails.isEmpty() || current.isSending) return

        viewModelScope.launch {
            selection.update { it.copy(isSending = true) }
            agentRepository.shareTrip(tripId, current.selectedEmails.toList(), current.message)
                .onSuccess { count ->
                    selection.update {
                        Selection(
                            notice = if (count == 1) "Trip sent to 1 agent" else "Trip sent to $count agents"
                        )
                    }
                }
                .onFailure { error ->
                    selection.update {
                        it.copy(isSending = false, notice = error.message ?: "Couldn't send the trip.")
                    }
                }
        }
    }

    fun withdraw(shareId: Long) {
        viewModelScope.launch {
            agentRepository.withdrawShare(shareId)
                .onSuccess { selection.update { it.copy(notice = "Trip withdrawn") } }
                .onFailure { e -> selection.update { it.copy(notice = e.message ?: "Couldn't withdraw.") } }
        }
    }

    fun noticeShown() = selection.update { it.copy(notice = null) }

    // ── Choosing an agent ────────────────────────────────────────────────────

    /** Goes ahead with one agent; the other agents' requests are closed. */
    fun chooseAgent(shareId: Long) {
        viewModelScope.launch {
            agentRepository.chooseAgent(tripId, shareId)
                .onSuccess {
                    val name = uiState.value.rows.firstOrNull { it.share?.id == shareId }
                        ?.agent?.let { it.agencyName.ifBlank { it.fullName } } ?: "this agent"
                    selection.update { it.copy(notice = "Going ahead with $name") }
                }
                .onFailure { e -> selection.update { it.copy(notice = e.message ?: "Couldn't choose this agent.") } }
        }
    }


    // ── Reading an agent's feedback ──────────────────────────────────────────

    private val openFeedbackShareId = MutableStateFlow<Long?>(null)
    private val busyFeedbackIds = MutableStateFlow<Set<Long>>(emptySet())

    /** Feedback of the agent the traveller opened, or null when the feedback sheet is closed. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val feedbackState: StateFlow<AgentFeedbackUiState?> = openFeedbackShareId
        .flatMapLatest { shareId ->
            if (shareId == null) flowOf(null)
            else combine(uiState, agentRepository.observeFeedback(shareId), busyFeedbackIds) { state, feedback, busy ->
                state.rows.firstOrNull { it.share?.id == shareId }
                    ?.let { row -> AgentFeedbackUiState(row, feedback, busy, state.conflicts) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun openFeedback(shareId: Long) {
        openFeedbackShareId.value = shareId
    }

    fun closeFeedback() {
        openFeedbackShareId.value = null
    }

    /** Applies the suggestion to the itinerary (replace or remove the activity). */
    fun applySuggestion(feedback: ActivityFeedback) = respond(feedback) {
        applyAgentSuggestion(tripId, feedback).map {
            if (feedback.verdict == ActivityVerdict.NOT_AVAILABLE) "Removed \"${feedback.activityTitle}\" from Day ${feedback.dayNumber}"
            else "Replaced with \"${feedback.suggestion}\""
        }
    }

    /** Keeps the itinerary as it is and marks the suggestion declined. */
    fun declineSuggestion(feedback: ActivityFeedback) = respond(feedback) {
        agentRepository.respondToFeedback(feedback.id, SuggestionResponse.DECLINED).map { "Suggestion declined" }
    }

    private fun respond(feedback: ActivityFeedback, action: suspend () -> Result<String>) {
        if (feedback.id in busyFeedbackIds.value) return
        viewModelScope.launch {
            busyFeedbackIds.update { it + feedback.id }
            action()
                .onSuccess { message -> selection.update { it.copy(notice = message) } }
                .onFailure { e -> selection.update { it.copy(notice = e.message ?: "Something went wrong.") } }
            busyFeedbackIds.update { it - feedback.id }
        }
    }

    private companion object {
        const val MAX_MESSAGE = 500
    }
}

/**
 * Summarises every agent that has this trip into comparable offers:
 * chosen first, then responded (approved before changes-requested, cheaper first), then pending.
 */
internal fun buildOffers(
    rows: List<AgentShareRow>,
    tripFeedback: List<TripFeedbackItem>,
    edits: List<TripEdit> = emptyList()
): List<AgentOffer> {
    val active = rows.filter { it.isShared }
    val prices = active.mapNotNull { row ->
        row.share?.takeIf { it.decision != null }?.priceEstimate
    }
    val lowest = prices.minOrNull()
    return active.map { row ->
        val share = row.share!!
        val mine = tripFeedback.filter { it.feedback.shareId == share.id }.map { it.feedback }
        AgentOffer(
            row = row,
            flaggedCount = mine.count { it.verdict != ActivityVerdict.AVAILABLE },
            appliedCount = mine.count { it.response == SuggestionResponse.APPLIED },
            reviewedBeforeLatestChanges = share.decision != null && (
                AgentConflicts.changesSince(share.updatedAt, share.id, tripFeedback).any { !it.suggestedByMe } ||
                    edits.any { it.editedAt > share.updatedAt }
                ),
            isLowestPrice = prices.size > 1 && share.priceEstimate != null && share.priceEstimate == lowest
        )
    }.sortedWith(
        // Chosen first, then responded offers (approved before changes-requested), then pending
        compareBy<AgentOffer> { if (it.share.status == ShareStatus.CONFIRMED) 0 else if (it.hasResponded) 1 else 2 }
            .thenBy { if (it.share.decision == ReviewDecision.APPROVED) 0 else 1 }
            .thenBy { it.share.priceEstimate ?: Double.MAX_VALUE }
    )
}
