package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.runtime.Immutable
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip

@Immutable
data class TripDetailUiState(
    val trip: Trip? = null,
    val itineraries: List<Itinerary> = emptyList(),
    val weather: List<DayWeather> = emptyList(),
    val selectedDayIndex: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingWeather: Boolean = false,
    val error: String? = null
) {
    val selectedItinerary: Itinerary?
        get() = itineraries.getOrNull(selectedDayIndex)

    val selectedDayWeather: DayWeather?
        get() = weather.getOrNull(selectedDayIndex)
}
