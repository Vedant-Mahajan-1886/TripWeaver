package com.example.smarttravelplanner.ui.screens.agentgroups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.GroupRepository
import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The agent's list of group trips (drafts, published, cancelled). */
@HiltViewModel
class AgentGroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    /** null while loading */
    val groups: StateFlow<List<GroupTrip>?> = groupRepository.observeMyGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

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

    fun noticeShown() {
        _notice.value = null
    }
}
