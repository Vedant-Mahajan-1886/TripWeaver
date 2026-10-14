package com.example.smarttravelplanner.ui.screens.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.GroupRepository
import com.example.smarttravelplanner.domain.model.agent.GroupTripListing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TravellerGroupState {
    data object Loading : TravellerGroupState
    /** Doesn't exist, or it is a draft I'm not part of */
    data object Missing : TravellerGroupState
    data class Ready(val listing: GroupTripListing) : TravellerGroupState
}

/** One group trip for a traveller: details, and join / leave. */
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val groupId: Long = savedStateHandle["groupId"] ?: 0L

    val state: StateFlow<TravellerGroupState> = groupRepository.observeListing(groupId)
        .map { listing -> if (listing == null) TravellerGroupState.Missing else TravellerGroupState.Ready(listing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TravellerGroupState.Loading)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun join() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            groupRepository.joinGroup(groupId)
                .onSuccess { _notice.value = "You're in! The agent can see you on the member list." }
                .onFailure { _notice.value = it.message ?: "Couldn't join this group." }
            _busy.value = false
        }
    }

    fun leave() {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            groupRepository.leaveGroup(groupId)
                .onSuccess { _notice.value = "You left the group. Your seat is free again." }
                .onFailure { _notice.value = it.message ?: "Couldn't leave this group." }
            _busy.value = false
        }
    }

    fun noticeShown() {
        _notice.value = null
    }
}
