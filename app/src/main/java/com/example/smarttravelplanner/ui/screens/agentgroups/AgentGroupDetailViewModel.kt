package com.example.smarttravelplanner.ui.screens.agentgroups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.GroupRepository
import com.example.smarttravelplanner.data.repository.PackageRepository
import com.example.smarttravelplanner.domain.model.agent.GroupMember
import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One group as the agent sees it: the trip, who joined, and the package it follows. */
data class GroupDetailUiState(
    val group: GroupTrip,
    val members: List<GroupMember>,
    val packageTitle: String?
)

sealed interface GroupDetailState {
    data object Loading : GroupDetailState
    /** Deleted, or not found */
    data object Missing : GroupDetailState
    data class Ready(val detail: GroupDetailUiState) : GroupDetailState
}

/** Group detail for the agent: details, member list, publish / cancel / delete. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgentGroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val packageRepository: PackageRepository
) : ViewModel() {

    private val groupId: Long = savedStateHandle["groupId"] ?: 0L

    private val packageTitle: Flow<String?> = groupRepository.observeGroup(groupId)
        .map { it?.packageId }
        .flatMapLatest { id -> if (id == null) flowOf(null) else packageRepository.observePackage(id).map { it?.title } }

    val state: StateFlow<GroupDetailState> = combine(
        groupRepository.observeGroup(groupId),
        groupRepository.observeMembers(groupId),
        packageTitle
    ) { group, members, pkgTitle ->
        if (group == null) GroupDetailState.Missing
        else GroupDetailState.Ready(GroupDetailUiState(group, members, pkgTitle))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailState.Loading)

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    /** Set after a delete so the screen can close itself. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun togglePublished(group: GroupTrip) {
        viewModelScope.launch {
            groupRepository.setPublished(group.id, !group.isPublished)
                .onSuccess {
                    _notice.value = if (group.isPublished) "Hidden \u2014 travellers can't see it"
                    else "Published \u2014 travellers can join now"
                }
                .onFailure { _notice.value = it.message }
        }
    }

    fun cancel(group: GroupTrip) {
        viewModelScope.launch {
            groupRepository.cancelGroup(group.id)
                .onSuccess { _notice.value = "Group cancelled" }
                .onFailure { _notice.value = it.message }
        }
    }

    fun delete(group: GroupTrip) {
        viewModelScope.launch {
            groupRepository.deleteGroup(group.id)
                .onSuccess { _deleted.value = true }
                .onFailure { _notice.value = it.message }
        }
    }

    fun noticeShown() {
        _notice.value = null
    }
}
