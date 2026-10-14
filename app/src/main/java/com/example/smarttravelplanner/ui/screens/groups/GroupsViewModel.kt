package com.example.smarttravelplanner.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.GroupRepository
import com.example.smarttravelplanner.domain.model.agent.GroupTripListing
import com.example.smarttravelplanner.domain.util.GroupBrowse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** What the traveller's "Group trips" list shows. */
data class GroupsUiState(
    val listings: List<GroupTripListing>,
    /** How many groups exist before search / filters (to tell "none yet" from "no match") */
    val totalCount: Int,
    val query: String,
    val hideFull: Boolean
)

/** The traveller's list of open group trips, with search and a "hide full" filter. */
@HiltViewModel
class GroupsViewModel @Inject constructor(
    groupRepository: GroupRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val hideFull = MutableStateFlow(false)

    /** null while loading */
    val uiState: StateFlow<GroupsUiState?> = combine(
        groupRepository.observeOpenGroups(), query, hideFull
    ) { all, q, hide ->
        GroupsUiState(
            listings = GroupBrowse.filter(all, q, hide),
            totalCount = all.size,
            query = q,
            hideFull = hide
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) {
        query.value = value.take(60)
    }

    fun setHideFull(value: Boolean) {
        hideFull.value = value
    }
}
