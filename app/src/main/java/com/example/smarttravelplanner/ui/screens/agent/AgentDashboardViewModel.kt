package com.example.smarttravelplanner.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.BookingRepository
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AgentDashboardUiState {
    data object Loading : AgentDashboardUiState
    data class Success(
        val profile: AgentProfile?,
        val incoming: List<IncomingShare>
    ) : AgentDashboardUiState {
        val newCount: Int get() = incoming.count { it.share.status == ShareStatus.SENT }
        val inReviewCount: Int get() = incoming.count { it.share.status == ShareStatus.VIEWED }
        val respondedCount: Int get() = incoming.count { it.share.status == ShareStatus.RESPONDED }
    }
}

@HiltViewModel
class AgentDashboardViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    init {
        // Booked trips whose end date has passed become "Completed"
        viewModelScope.launch { runCatching { bookingRepository.completeFinishedTrips() } }
    }

    private val _notice = MutableStateFlow<String?>(null)
    /** One-off message after saving the profile. */
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /** Saves agency name, city and comma-separated specialities. */
    fun saveProfile(agencyName: String, city: String, specialitiesText: String) {
        viewModelScope.launch {
            val specialities = specialitiesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            agentRepository.saveMyAgentProfile(agencyName, city, specialities)
                .onSuccess { _notice.value = "Profile updated" }
                .onFailure { _notice.value = it.message ?: "Couldn't save your profile." }
        }
    }

    fun noticeShown() {
        _notice.value = null
    }

    val uiState: StateFlow<AgentDashboardUiState> = combine(
        agentRepository.observeMyAgentProfile(),
        agentRepository.observeIncomingShares()
    ) { profile, incoming ->
        AgentDashboardUiState.Success(profile, incoming)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AgentDashboardUiState.Loading
    )
}
