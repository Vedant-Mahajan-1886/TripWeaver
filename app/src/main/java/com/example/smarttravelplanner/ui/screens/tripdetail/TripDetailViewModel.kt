package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.PhotoRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.usecase.GetTripDetailUseCase
import com.example.smarttravelplanner.domain.usecase.GetWeatherForTripUseCase
import com.example.smarttravelplanner.util.DestinationImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.usecase.RegenerateActivityUseCase
import com.example.smarttravelplanner.domain.util.TripEditor
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTripDetailUseCase: GetTripDetailUseCase,
    private val getWeatherForTripUseCase: GetWeatherForTripUseCase,
    private val photoRepository: PhotoRepository,
    private val tripRepository: TripRepository,
    private val tripEditRepository: TripEditRepository,
    private val regenerateActivityUseCase: RegenerateActivityUseCase
) : ViewModel() {


    private val tripId: Long = checkNotNull(savedStateHandle["tripId"])

    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTrip()
    }

    private fun loadTrip() {
        viewModelScope.launch(Dispatchers.IO) {
            // Load the Trip object (one-shot)
            val trip = getTripDetailUseCase.getTrip(tripId)
            if (trip == null) {
                _uiState.update { it.copy(isLoading = false, error = "Trip not found") }
                return@launch
            }
            _uiState.update { it.copy(trip = trip, isLoading = false) }

            android.util.Log.d("TripDetailVM", "Trip loaded: id=${trip.id}, destination='${trip.destination}', coverImageUrl='${trip.coverImageUrl}', hasLocal=${DestinationImageHelper.hasLocalDrawable(trip.destination)}")

            // If the trip lacks an online cover photo and has no local bundled drawable, fetch from Wikipedia
            if (trip.coverImageUrl.isBlank() && !DestinationImageHelper.hasLocalDrawable(trip.destination)) {
                launch(Dispatchers.IO) {
                    android.util.Log.d("TripDetailVM", "Requesting online photo for '${trip.destination}' from Wikipedia...")
                    photoRepository.getDestinationPhotoUrl(trip.destination)
                        .onSuccess { photoUrl ->
                            android.util.Log.d("TripDetailVM", "Got photo URL for '${trip.destination}': '$photoUrl'")
                            if (photoUrl.isNotBlank()) {
                                val updatedTrip = trip.copy(coverImageUrl = photoUrl)
                                tripRepository.updateTrip(updatedTrip)
                                _uiState.update { it.copy(trip = updatedTrip) }
                            }
                        }
                        .onFailure { e ->
                            android.util.Log.e("TripDetailVM", "Error resolving photo for '${trip.destination}'", e)
                        }
                }
            }

            // Collect itineraries reactively
            launch(Dispatchers.IO) {
                getTripDetailUseCase.getItineraries(tripId).collect { itineraries ->
                    _uiState.update {
                        it.copy(
                            itineraries = itineraries,
                            // The trip may have fewer days after an edit
                            selectedDayIndex = it.selectedDayIndex
                                .coerceAtMost((itineraries.size - 1).coerceAtLeast(0))
                        )
                    }
                }
            }

            // Fetch real-time weather (Open-Meteo)
            loadWeather(
                destination = trip.destination,
                startDate   = trip.startDate,
                endDate     = trip.endDate
            )
        }
    }

    private fun loadWeather(destination: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingWeather = true) }
            getWeatherForTripUseCase.getForDestination(destination, startDate, endDate)
                .onSuccess { weather ->
                    _uiState.update { it.copy(weather = weather, isLoadingWeather = false) }
                }
                .onFailure {
                    // Weather is non-critical — silently fail
                    _uiState.update { it.copy(isLoadingWeather = false) }
                }
        }
    }

    /**
     * Re-reads the trip's details (after returning from Edit trip).
     * Weather is reloaded only if the dates changed.
     */
    fun refreshTrip() {
        val current = _uiState.value.trip ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val fresh = getTripDetailUseCase.getTrip(tripId) ?: return@launch
            if (fresh == current) return@launch
            _uiState.update { it.copy(trip = fresh) }
            if (fresh.startDate != current.startDate || fresh.endDate != current.endDate) {
                loadWeather(fresh.destination, fresh.startDate, fresh.endDate)
            }
        }
    }

    fun selectDay(index: Int) {
        _uiState.update { it.copy(selectedDayIndex = index, selectedActivityId = null) }
    }

    fun setViewMode(mode: TripDetailViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectActivity(activityId: Long?) {
        _uiState.update { it.copy(selectedActivityId = activityId) }
    }

    /**
     * Swiped right: deletes an activity stop from the specified day.
     * Records an undo snapshot so the traveller can restore it immediately.
     */
    fun deleteActivity(dayIndex: Int, activityIndex: Int) {
        val state = _uiState.value
        val trip = state.trip ?: return
        val currentItin = state.itineraries.getOrNull(dayIndex) ?: return
        val activity = currentItin.activities.getOrNull(activityIndex) ?: return

        val previousItineraries = state.itineraries
        val result = runCatching {
            TripEditor.deleteActivity(state.itineraries, currentItin.dayNumber, activityIndex)
        }.getOrNull() ?: return

        _uiState.update {
            it.copy(
                lastDeletedActivity = DeletedActivitySnapshot(
                    title = activity.title,
                    dayNumber = currentItin.dayNumber,
                    previousItineraries = previousItineraries
                ),
                userNotice = "Removed “${activity.title}”"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            tripEditRepository.saveEdits(
                trip = trip,
                days = result.days,
                summaries = listOf(result.summary)
            )
        }
    }

    /** Restores the last deleted activity to its exact position. */
    fun undoDelete() {
        val state = _uiState.value
        val snapshot = state.lastDeletedActivity ?: return
        val trip = state.trip ?: return

        _uiState.update {
            it.copy(
                lastDeletedActivity = null,
                userNotice = "Restored “${snapshot.title}”"
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            tripEditRepository.saveEdits(
                trip = trip,
                days = snapshot.previousItineraries,
                summaries = listOf("Restored “${snapshot.title}” to Day ${snapshot.dayNumber}")
            )
        }
    }

    fun dismissUndo() {
        _uiState.update { it.copy(lastDeletedActivity = null) }
    }

    /** Updates an activity stop after the user edits it. */
    fun updateActivity(dayIndex: Int, activityIndex: Int, updated: Activity) {
        val state = _uiState.value
        val trip = state.trip ?: return
        val currentItin = state.itineraries.getOrNull(dayIndex) ?: return

        val result = runCatching {
            TripEditor.updateActivity(state.itineraries, currentItin.dayNumber, activityIndex, updated)
        }.getOrElse { e ->
            _uiState.update { it.copy(userNotice = e.message ?: "Failed to update activity") }
            return
        }

        _uiState.update { it.copy(userNotice = "Saved “${updated.title}”") }

        viewModelScope.launch(Dispatchers.IO) {
            tripEditRepository.saveEdits(
                trip = trip,
                days = result.days,
                summaries = listOf(result.summary)
            )
        }
    }

    /**
     * Regenerates an activity stop using Gemini/Ollama AI.
     * Can be invoked either with optional custom instructions or with an edited base activity
     * ("use to regenerate that edited part").
     */
    fun regenerateActivity(
        dayIndex: Int,
        activityIndex: Int,
        instructions: String = "",
        baseActivity: Activity? = null,
        onComplete: ((Result<Activity>) -> Unit)? = null
    ) {
        val state = _uiState.value
        val trip = state.trip ?: return
        val currentItin = state.itineraries.getOrNull(dayIndex) ?: return
        val targetActivity = baseActivity ?: currentItin.activities.getOrNull(activityIndex) ?: return
        val otherActivities = currentItin.activities.filterIndexed { idx, _ -> idx != activityIndex }

        _uiState.update {
            it.copy(
                isRegenerating = true,
                regeneratingActivityTitle = targetActivity.title
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = regenerateActivityUseCase(
                destination = trip.destination,
                dayNumber = currentItin.dayNumber,
                itineraryId = currentItin.id,
                currentActivity = targetActivity,
                otherActivities = otherActivities,
                instructions = instructions,
                tripInterests = trip.interests
            )

            result.onSuccess { newActivity ->
                if (onComplete != null) {
                    _uiState.update {
                        it.copy(
                            isRegenerating = false,
                            regeneratingActivityTitle = null,
                            userNotice = "✨ Regenerated “${newActivity.title}”"
                        )
                    }
                    withContext(Dispatchers.Main) {
                        onComplete(Result.success(newActivity))
                    }
                } else {
                    val updateResult = runCatching {
                        TripEditor.updateActivity(
                            days = _uiState.value.itineraries,
                            dayNumber = currentItin.dayNumber,
                            index = activityIndex,
                            updated = newActivity
                        )
                    }.getOrNull()

                    if (updateResult != null) {
                        tripEditRepository.saveEdits(
                            trip = trip,
                            days = updateResult.days,
                            summaries = listOf("Regenerated stop in Day ${currentItin.dayNumber}: “${newActivity.title}”")
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isRegenerating = false,
                            regeneratingActivityTitle = null,
                            userNotice = "✨ Replaced with “${newActivity.title}”"
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isRegenerating = false,
                        regeneratingActivityTitle = null,
                        userNotice = "AI regeneration failed: ${err.message ?: "Unknown error"}"
                    )
                }
                if (onComplete != null) {
                    withContext(Dispatchers.Main) {
                        onComplete(Result.failure(err))
                    }
                }
            }
        }
    }

    fun clearUserNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }
}


