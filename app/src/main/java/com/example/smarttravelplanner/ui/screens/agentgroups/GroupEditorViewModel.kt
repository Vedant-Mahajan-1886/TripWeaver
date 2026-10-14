package com.example.smarttravelplanner.ui.screens.agentgroups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.GroupRepository
import com.example.smarttravelplanner.data.repository.PackageRepository
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import com.example.smarttravelplanner.domain.util.GroupRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Editor state: the group being built plus the agent's packages to link to. */
data class GroupEditorUiState(
    val isLoading: Boolean = true,
    val draft: GroupTrip = GroupTrip(
        title = "", destination = "",
        startDate = LocalDate.now().plusDays(30),
        endDate = LocalDate.now().plusDays(34),
        pricePerPerson = 0.0,
        totalSeats = 8
    ),
    val priceText: String = "",
    /** The agent's own packages (a group can follow one) */
    val packages: List<AgentPackage> = emptyList(),
    val problems: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false
) {
    val isNew: Boolean get() = draft.id == 0L
    val hasMembers: Boolean get() = draft.seatsTaken > 0

    /** Fewest seats allowed: nobody who joined can lose their seat. */
    val minSeats: Int get() = maxOf(GroupRules.MIN_SEATS, draft.seatsTaken)
}

/** Create or edit one group trip (groupId 0 = new). Nothing is saved until Save is tapped. */
@HiltViewModel
class GroupEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    private val packageRepository: PackageRepository
) : ViewModel() {

    private val groupId: Long = savedStateHandle["groupId"] ?: 0L

    private val _uiState = MutableStateFlow(GroupEditorUiState())
    val uiState: StateFlow<GroupEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val packages = packageRepository.observeMyPackages().first()
            val existing = if (groupId != 0L) groupRepository.observeGroup(groupId).first() else null
            _uiState.update { state ->
                if (existing == null) state.copy(isLoading = false, packages = packages)
                else state.copy(
                    isLoading = false,
                    packages = packages,
                    draft = existing,
                    priceText = existing.pricePerPerson.toLong().takeIf { it > 0 }?.toString().orEmpty()
                )
            }
        }
    }

    // -- Details --------------------------------------------------------------
    fun setTitle(v: String) = edit { it.copy(title = v.take(80)) }
    fun setDestination(v: String) = edit { it.copy(destination = v.take(60)) }
    fun setMeetingPoint(v: String) = edit { it.copy(meetingPoint = v.take(120)) }
    fun setDescription(v: String) = edit { it.copy(description = v.take(800)) }

    fun setPrice(v: String) {
        val digits = v.filter(Char::isDigit).take(8)
        _uiState.update {
            it.copy(priceText = digits, problems = emptyList(), draft = it.draft.copy(pricePerPerson = digits.toDoubleOrNull() ?: 0.0))
        }
    }

    // -- Dates and seats ------------------------------------------------------

    /** Moving the start date later also moves the end date so the trip keeps its length. */
    fun setStartDate(date: LocalDate) = edit { g ->
        val length = g.nights
        g.copy(startDate = date, endDate = if (g.endDate.isBefore(date)) date.plusDays(length) else g.endDate)
    }

    fun setEndDate(date: LocalDate) = edit { it.copy(endDate = date) }

    fun changeSeats(by: Int) = edit { g ->
        val min = maxOf(GroupRules.MIN_SEATS, g.seatsTaken)
        g.copy(totalSeats = (g.totalSeats + by).coerceIn(min, GroupRules.MAX_SEATS))
    }

    // -- Package link ---------------------------------------------------------

    /** Links the group to one of the agent's packages (null = none). Fills empty fields from it. */
    fun choosePackage(pkg: AgentPackage?) {
        _uiState.update { state ->
            val g = state.draft
            if (pkg == null) {
                state.copy(draft = g.copy(packageId = null), problems = emptyList())
            } else {
                val price = if (g.pricePerPerson <= 0) pkg.pricePerPerson else g.pricePerPerson
                state.copy(
                    draft = g.copy(
                        packageId = pkg.id,
                        destination = g.destination.ifBlank { pkg.destination },
                        title = g.title.ifBlank { pkg.title },
                        pricePerPerson = price
                    ),
                    priceText = price.toLong().takeIf { it > 0 }?.toString().orEmpty(),
                    problems = emptyList()
                )
            }
        }
    }

    // -- Save -----------------------------------------------------------------

    /** @param publish true = save and make it visible to travellers */
    fun save(publish: Boolean) {
        val state = _uiState.value
        if (state.isSaving) return
        val toSave = state.draft.copy(isPublished = publish)
        val problems = GroupRules.validate(toSave, LocalDate.now())
        if (problems.isNotEmpty()) {
            _uiState.update { it.copy(problems = problems) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            groupRepository.saveGroup(toSave)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, problems = listOf(e.message ?: "Couldn't save.")) } }
        }
    }

    private fun edit(change: (GroupTrip) -> GroupTrip) =
        _uiState.update { it.copy(draft = change(it.draft), problems = emptyList()) }
}
