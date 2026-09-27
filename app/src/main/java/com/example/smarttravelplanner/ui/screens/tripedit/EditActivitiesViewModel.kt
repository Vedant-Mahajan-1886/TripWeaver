package com.example.smarttravelplanner.ui.screens.tripedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.usecase.PlanEmptyDaysUseCase
import com.example.smarttravelplanner.domain.util.TripEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Undo information for the last deleted activity. */
data class UndoDelete(val title: String, val previousDays: List<Itinerary>)

data class EditActivitiesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val trip: Trip? = null,
    val days: List<Itinerary> = emptyList(),
    val selectedDayNumber: Int = 1,
    val isBusy: Boolean = false,
    /** Shown as a snackbar with an Undo action. */
    val undo: UndoDelete? = null,
    /** One-off message. */
    val notice: String? = null
) {
    val selectedDay: Itinerary? get() = days.firstOrNull { it.dayNumber == selectedDayNumber }
}

/**
 * Day-by-day activity editor: add, edit, delete (with undo), reorder and move
 * activities between days. Every change is saved straight away and written to
 * the trip's edit log.
 */
@HiltViewModel
class EditActivitiesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val tripEditRepository: TripEditRepository,
    private val planEmptyDays: PlanEmptyDaysUseCase,
    agentRepository: AgentRepository
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    /** "Agents reviewing this trip will see…" (null if never sent to an agent). */
    val agentNotice: StateFlow<String?> = agentEditNotice(agentRepository, tripId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(EditActivitiesUiState())
    val uiState: StateFlow<EditActivitiesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            if (trip == null) {
                _uiState.update { it.copy(isLoading = false, error = "Trip not found.") }
                return@launch
            }
            _uiState.update { it.copy(trip = trip) }
            // Live: changes made here (or by AI / agent suggestions) show up immediately
            itineraryRepository.getItinerariesForTripAsFlow(tripId).collect { days ->
                val sorted = days.sortedBy { it.dayNumber }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        days = sorted,
                        selectedDayNumber = state.selectedDayNumber
                            .takeIf { n -> sorted.any { it.dayNumber == n } } ?: (sorted.firstOrNull()?.dayNumber ?: 1)
                    )
                }
            }
        }
    }

    fun selectDay(dayNumber: Int) = _uiState.update { it.copy(selectedDayNumber = dayNumber) }

    // ── Changes ──────────────────────────────────────────────────────────────

    fun addActivity(activity: Activity) = applyEdit { days ->
        TripEditor.addActivity(days, currentDay(), activity)
    }

    fun updateActivity(index: Int, activity: Activity) = applyEdit { days ->
        TripEditor.updateActivity(days, currentDay(), index, activity)
    }

    fun moveUp(index: Int) = applyEdit { days -> TripEditor.moveWithinDay(days, currentDay(), index, index - 1) }

    fun moveDown(index: Int) = applyEdit { days -> TripEditor.moveWithinDay(days, currentDay(), index, index + 1) }

    fun moveToDay(index: Int, toDay: Int) = applyEdit(notice = { "Moved to Day $toDay" }) { days ->
        TripEditor.moveToDay(days, currentDay(), index, toDay)
    }

    fun deleteActivity(index: Int) {
        val before = _uiState.value.days
        val title = _uiState.value.selectedDay?.activities?.getOrNull(index)?.title ?: return
        applyEdit(afterSave = { it.copy(undo = UndoDelete(title, before)) }) { days ->
            TripEditor.deleteActivity(days, currentDay(), index)
        }
    }

    /** Puts the last deleted activity back exactly where it was. */
    fun undoDelete() {
        val state = _uiState.value
        val undo = state.undo ?: return
        val trip = state.trip ?: return
        _uiState.update { it.copy(undo = null) }
        save(trip, undo.previousDays, "Restored \u201C${undo.title}\u201D")
    }

    fun undoDismissed() = _uiState.update { it.copy(undo = null) }

    fun noticeShown() = _uiState.update { it.copy(notice = null) }

    /** Lets the AI plan the selected day if it's empty. */
    fun planSelectedDayWithAi() {
        val day = _uiState.value.selectedDay ?: return
        if (day.activities.isNotEmpty() || _uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            planEmptyDays(tripId, setOf(day.dayNumber))
                .onSuccess { _uiState.update { it.copy(isBusy = false, notice = "Day ${day.dayNumber} planned with AI") } }
                .onFailure { e -> _uiState.update { it.copy(isBusy = false, notice = "AI planning failed: ${e.message}") } }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun currentDay(): Int = _uiState.value.selectedDayNumber

    /** Runs an edit rule and saves the result; errors from the rule become a message. */
    private fun applyEdit(
        notice: ((TripEditor.Result) -> String?)? = null,
        afterSave: ((EditActivitiesUiState) -> EditActivitiesUiState)? = null,
        change: (List<Itinerary>) -> TripEditor.Result
    ) {
        val state = _uiState.value
        val trip = state.trip ?: return
        if (state.isBusy) return
        // Any new change makes an older "Undo delete" unsafe, so drop it
        _uiState.update { it.copy(undo = null) }
        val result = runCatching { change(state.days) }.getOrElse { e ->
            _uiState.update { it.copy(notice = e.message ?: "Couldn't make that change.") }
            return
        }
        save(trip, result.days, result.summary, notice?.invoke(result), afterSave)
    }

    private fun save(
        trip: Trip,
        days: List<Itinerary>,
        summary: String,
        notice: String? = null,
        afterSave: ((EditActivitiesUiState) -> EditActivitiesUiState)? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            tripEditRepository.saveEdits(trip, days, summaries = listOf(summary))
                .onSuccess {
                    _uiState.update { s ->
                        val updated = s.copy(isBusy = false, notice = notice ?: s.notice)
                        afterSave?.invoke(updated) ?: updated
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBusy = false, notice = e.message ?: "Couldn't save the change.") }
                }
        }
    }
}
