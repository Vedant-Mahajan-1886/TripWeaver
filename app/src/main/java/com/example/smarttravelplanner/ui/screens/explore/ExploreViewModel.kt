package com.example.smarttravelplanner.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.DestinationRepository
import com.example.smarttravelplanner.domain.model.DepartureCity
import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.ExploreSortOrder
import com.example.smarttravelplanner.domain.util.DestinationFilter
import com.example.smarttravelplanner.domain.util.GeoDistance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    /** Unfiltered catalog; [ExploreUiState.destinations] is derived from this. */
    private var allDestinations: List<ExploreDestination> = emptyList()

    /** "From" cities with coordinates; the UI only needs their names. */
    private var departureCities: List<DepartureCity> = emptyList()

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadDestinations()
    }

    fun loadDestinations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            destinationRepository.getDestinations()
                .onSuccess { destinations ->
                    allDestinations = destinations
                    departureCities = destinationRepository.getDepartureCities()
                    val cityNames = departureCities.map { it.name }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            departureCities = cityNames,
                            selectedCity = state.selectedCity
                                .takeIf { it in cityNames } ?: cityNames.firstOrNull().orEmpty()
                        ).withFilteredDestinations()
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Couldn't load destinations."
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) = updateFilters { it.copy(searchQuery = query) }

    fun onCategorySelected(categoryId: String) =
        updateFilters { it.copy(selectedCategory = categoryId) }

    fun onCitySelected(city: String) = updateFilters { it.copy(selectedCity = city) }

    fun onSortOrderSelected(order: ExploreSortOrder) = updateFilters { it.copy(sortOrder = order) }

    fun onDestinationClick(destination: ExploreDestination) {
        _uiState.update { it.copy(selectedDestination = destination) }
    }

    fun onDetailsDismissed() {
        _uiState.update { it.copy(selectedDestination = null) }
    }

    fun clearFilters() = updateFilters {
        it.copy(searchQuery = "", selectedCategory = DestinationFilter.ALL)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateFilters(transform: (ExploreUiState) -> ExploreUiState) {
        _uiState.update { transform(it).withFilteredDestinations() }
    }

    /** Re-derives the visible list: filter → distances from the "From" city → optional sort. */
    private fun ExploreUiState.withFilteredDestinations(): ExploreUiState {
        val filtered = DestinationFilter.apply(
            destinations = allDestinations,
            query = searchQuery,
            category = selectedCategory
        )
        val origin = this@ExploreViewModel.departureCities.firstOrNull { it.name == selectedCity }?.location
        val distances = origin?.let { GeoDistance.distancesFrom(it, allDestinations) }.orEmpty()

        val ordered = when (sortOrder) {
            ExploreSortOrder.RECOMMENDED -> filtered
            ExploreSortOrder.NEAREST     -> GeoDistance.sortByDistance(filtered, distances)
        }
        return copy(destinations = ordered, distancesKm = distances)
    }
}
