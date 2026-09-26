package com.example.smarttravelplanner.ui.screens.agentshare

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.TripShare
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

data class SendToAgentUiState(
    val isLoading: Boolean = true,
    val rows: List<AgentShareRow> = emptyList(),
    val selectedEmails: Set<String> = emptySet(),
    val message: String = "",
    val isSending: Boolean = false,
    /** One-off feedback shown as a snackbar/toast, then cleared. */
    val notice: String? = null
) {
    /** Agents this trip is currently shared with (not withdrawn). */
    val activeShareCount: Int get() = rows.count { it.isShared }
    val canSend: Boolean get() = selectedEmails.isNotEmpty() && !isSending
}

/**
 * Traveller side of the agent feature for one trip: pick agents, add a message,
 * send, and see / withdraw existing shares. Scoped to the Trip Detail screen.
 */
@HiltViewModel
class SendToAgentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val agentRepository: AgentRepository
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
        selection
    ) { agents, shares, sel ->
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
            notice = sel.notice
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

    private companion object {
        const val MAX_MESSAGE = 500
    }
}
