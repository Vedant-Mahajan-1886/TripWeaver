package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.runtime.Immutable
import com.example.smarttravelplanner.BuildConfig
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip

enum class TripDetailViewMode {
    TIMELINE,
    VISUALIZE,
    BUDGET
}

@Immutable
data class TripDetailUiState(
    val trip: Trip? = null,
    val itineraries: List<Itinerary> = emptyList(),
    val weather: List<DayWeather> = emptyList(),
    val selectedDayIndex: Int = 0,
    val viewMode: TripDetailViewMode = TripDetailViewMode.TIMELINE,
    val selectedActivityId: Long? = null,
    val isLoading: Boolean = true,
    val isLoadingWeather: Boolean = false,
    val error: String? = null
) {
    val selectedItinerary: Itinerary?
        get() = itineraries.getOrNull(selectedDayIndex)

    val selectedDayWeather: DayWeather?
        get() = weather.getOrNull(selectedDayIndex)

    val allActivities: List<Activity>
        get() = itineraries.flatMap { it.activities }

    val totalEstimatedCost: Double
        get() = allActivities.sumOf { it.estimatedCost }

    val totalBudget: Double
        get() = trip?.budget ?: 0.0

    val remainingBudget: Double
        get() = totalBudget - totalEstimatedCost

    val isOverBudget: Boolean
        get() = totalBudget > 0 && totalEstimatedCost > totalBudget

    val categoryExpenses: Map<ActivityCategory, Double>
        get() {
            val map = mutableMapOf<ActivityCategory, Double>()
            allActivities.forEach { act ->
                map[act.category] = (map[act.category] ?: 0.0) + act.estimatedCost
            }
            return map.filterValues { it > 0.0 }
        }

    val dayExpenses: List<Pair<Int, Double>>
        get() = itineraries.map { itin ->
            itin.dayNumber to itin.activities.sumOf { it.estimatedCost }
        }

    val hasMapsApiKey: Boolean
        get() = BuildConfig.MAPS_API_KEY.isNotBlank()
}

