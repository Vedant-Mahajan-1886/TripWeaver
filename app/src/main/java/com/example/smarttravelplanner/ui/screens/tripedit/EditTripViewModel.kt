package com.example.smarttravelplanner.ui.screens.tripedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.usecase.PlanEmptyDaysUseCase
import com.example.smarttravelplanner.domain.util.TripEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** What the traveller is editing on the Edit trip screen. */
data class EditTripForm(
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    /** Digits only, as typed */
    val budgetText: String,
    val adults: Int,
    val kids: Int,
    val interests: List<String>,
    val notes: String
) {
    val budget: Double get() = budgetText.toDoubleOrNull() ?: 0.0
}

sealed interface EditTripUiState {
    data object Loading : EditTripUiState
    data class Error(val message: String) : EditTripUiState
    data class Editing(
        val original: Trip,
        val days: List<Itinerary>,
        val form: EditTripForm,
        val problems: List<String> = emptyList(),
        /** Set when new dates would delete days that have activities; save needs confirmation. */
        val pendingDayRemoval: TripEditor.Reshape? = null,
        val isSaving: Boolean = false,
        val saved: Boolean = false,
        /** Plan days added by longer dates with AI after saving (on by default). */
        val planNewDaysWithAi: Boolean = true,
        /** Non-null while the AI is planning; the text is shown to the traveller. */
        val aiProgress: String? = null,
        /** Set when saving worked but AI planning failed. */
        val aiFailure: String? = null,
        /** One-off message (e.g. "Planned 2 days with AI"). */
        val notice: String? = null
    ) : EditTripUiState {
        /** Days that exist but have no activities (can be filled with AI). */
        val emptyDayNumbers: List<Int> get() = days.filter { it.activities.isEmpty() }.map { it.dayNumber }

        val hasChanges: Boolean get() = TripEditor.describeDetailChanges(original, form.toTrip(original)).isNotEmpty()
    }
}

internal fun EditTripForm.toTrip(original: Trip): Trip = original.copy(
    title = title.trim(),
    startDate = startDate,
    endDate = endDate,
    budget = budget,
    adults = adults,
    kids = kids,
    interests = interests.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
    notes = notes.trim()
)

@HiltViewModel
class EditTripViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val tripEditRepository: TripEditRepository,
    private val planEmptyDays: PlanEmptyDaysUseCase
) : ViewModel() {

    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val _uiState = MutableStateFlow<EditTripUiState>(EditTripUiState.Loading)
    val uiState: StateFlow<EditTripUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val trip = tripRepository.getTripById(tripId)
            if (trip == null) {
                _uiState.value = EditTripUiState.Error("Trip not found.")
                return@launch
            }
            val days = itineraryRepository.getItinerariesForTrip(tripId).sortedBy { it.dayNumber }
            _uiState.value = EditTripUiState.Editing(
                original = trip,
                days = days,
                form = EditTripForm(
                    title = trip.title,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    budgetText = trip.budget.toLong().takeIf { it > 0 }?.toString().orEmpty(),
                    adults = trip.adults,
                    kids = trip.kids,
                    interests = trip.interests,
                    notes = trip.notes
                )
            )
        }
    }

    // ── Editing ──────────────────────────────────────────────────────────────

    fun setTitle(value: String) = updateForm { it.copy(title = value.take(60)) }
    fun setDates(start: LocalDate, end: LocalDate) = updateForm { it.copy(startDate = start, endDate = end) }
    fun setBudget(value: String) = updateForm { it.copy(budgetText = value.filter(Char::isDigit).take(9)) }
    fun setAdults(value: Int) = updateForm { it.copy(adults = value.coerceIn(1, 20)) }
    fun setKids(value: Int) = updateForm { it.copy(kids = value.coerceIn(0, 20)) }
    fun setNotes(value: String) = updateForm { it.copy(notes = value.take(500)) }

    fun addInterest(value: String) = updateForm { form ->
        val interest = value.trim()
        if (interest.isEmpty() || form.interests.any { it.equals(interest, ignoreCase = true) }) form
        else form.copy(interests = form.interests + interest.take(30))
    }

    fun removeInterest(value: String) = updateForm { it.copy(interests = it.interests - value) }

    fun setPlanNewDaysWithAi(enabled: Boolean) =
        _uiState.update { (it as? EditTripUiState.Editing)?.copy(planNewDaysWithAi = enabled) ?: it }

    // ── AI planning for empty days ───────────────────────────────────────────

    /** Plans every existing day that has no activities (without changing anything else). */
    fun planEmptyDaysNow() {
        val state = _uiState.value as? EditTripUiState.Editing ?: return
        val empty = state.emptyDayNumbers
        if (empty.isEmpty() || state.aiProgress != null) return
        viewModelScope.launch {
            setAiProgress("Planning ${dayList(empty)} with AI\u2026")
            planEmptyDays(tripId, empty.toSet())
                .onSuccess { count ->
                    val days = itineraryRepository.getItinerariesForTrip(tripId).sortedBy { it.dayNumber }
                    _uiState.update {
                        (it as? EditTripUiState.Editing)?.copy(
                            days = days,
                            aiProgress = null,
                            notice = if (count == 0) "Nothing to plan" else "Planned $count day(s) with AI"
                        ) ?: it
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        (it as? EditTripUiState.Editing)?.copy(
                            aiProgress = null,
                            notice = "AI planning failed: ${e.message ?: "please try again"}"
                        ) ?: it
                    }
                }
        }
    }

    fun noticeShown() = _uiState.update { (it as? EditTripUiState.Editing)?.copy(notice = null) ?: it }

    /** After an AI failure dialog: the edits are saved, so leave the screen. */
    fun acknowledgeAiFailure() =
        _uiState.update { (it as? EditTripUiState.Editing)?.copy(aiFailure = null, saved = true) ?: it }

    private fun setAiProgress(text: String?) =
        _uiState.update { (it as? EditTripUiState.Editing)?.copy(aiProgress = text) ?: it }

    private fun dayList(days: List<Int>): String =
        if (days.size == 1) "Day ${days.first()}" else "Days " + days.joinToString(", ")

    // ── Saving ───────────────────────────────────────────────────────────────

    /** Validates, then saves — or asks for confirmation first if days with activities would be removed. */
    fun save() {
        val state = _uiState.value as? EditTripUiState.Editing ?: return
        if (state.isSaving) return
        val f = state.form
        val problems = TripEditor.validateDetails(f.title, f.startDate, f.endDate, f.budget, f.adults, f.kids)
        if (problems.isNotEmpty()) {
            _uiState.update { (it as? EditTripUiState.Editing)?.copy(problems = problems) ?: it }
            return
        }
        val reshape = TripEditor.reshapeDays(tripId, state.days, f.startDate, f.endDate)
        if (reshape.removedActivityCount > 0) {
            _uiState.update { (it as? EditTripUiState.Editing)?.copy(pendingDayRemoval = reshape) ?: it }
            return
        }
        persist(state, reshape)
    }

    /** The traveller accepted that some days (and their activities) will be removed. */
    fun confirmDayRemoval() {
        val state = _uiState.value as? EditTripUiState.Editing ?: return
        val reshape = state.pendingDayRemoval ?: return
        _uiState.update { (it as? EditTripUiState.Editing)?.copy(pendingDayRemoval = null) ?: it }
        persist(state, reshape)
    }

    fun cancelDayRemoval() {
        _uiState.update { (it as? EditTripUiState.Editing)?.copy(pendingDayRemoval = null) ?: it }
    }

    private fun persist(state: EditTripUiState.Editing, reshape: TripEditor.Reshape) {
        val updatedTrip = state.form.toTrip(state.original)
        val summaries = TripEditor.describeDetailChanges(state.original, updatedTrip) +
            reshape.removedDays.map { day ->
                "Removed Day ${day.dayNumber}" +
                    if (day.activities.isEmpty()) "" else " (${day.activities.size} activities)"
            }

        viewModelScope.launch {
            _uiState.update { (it as? EditTripUiState.Editing)?.copy(isSaving = true, problems = emptyList()) ?: it }
            tripEditRepository.saveEdits(
                trip = updatedTrip,
                days = reshape.days,
                removedDayIds = reshape.removedDays.map { it.id }.filter { it != 0L },
                summaries = summaries
            ).onSuccess {
                // Days added by longer dates are empty — optionally let the AI plan just those
                val oldNumbers = state.days.map { it.dayNumber }.toSet()
                val addedDays = reshape.days.map { it.dayNumber }.filter { it !in oldNumbers }
                if (state.planNewDaysWithAi && addedDays.isNotEmpty()) {
                    _uiState.update {
                        (it as? EditTripUiState.Editing)?.copy(
                            isSaving = false,
                            aiProgress = "Saved. Planning ${dayList(addedDays)} with AI\u2026"
                        ) ?: it
                    }
                    planEmptyDays(tripId, addedDays.toSet())
                        .onSuccess {
                            _uiState.update { (it as? EditTripUiState.Editing)?.copy(aiProgress = null, saved = true) ?: it }
                        }
                        .onFailure { e ->
                            _uiState.update {
                                (it as? EditTripUiState.Editing)?.copy(
                                    aiProgress = null,
                                    aiFailure = e.message ?: "Unknown error"
                                ) ?: it
                            }
                        }
                } else {
                    _uiState.update { (it as? EditTripUiState.Editing)?.copy(isSaving = false, saved = true) ?: it }
                }
            }.onFailure { e ->
                _uiState.update {
                    (it as? EditTripUiState.Editing)?.copy(
                        isSaving = false,
                        problems = listOf(e.message ?: "Couldn't save your changes.")
                    ) ?: it
                }
            }
        }
    }

    private fun updateForm(change: (EditTripForm) -> EditTripForm) {
        _uiState.update { current ->
            (current as? EditTripUiState.Editing)?.copy(form = change(current.form), problems = emptyList()) ?: current
        }
    }
}
