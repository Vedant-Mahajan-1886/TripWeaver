package com.example.smarttravelplanner.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.DestinationRepository
import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.util.DestinationFilter
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
                    val cities = destinationRepository.getDepartureCities()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            departureCities = cities,
                            selectedCity = state.selectedCity
                                .takeIf { it in cities } ?: cities.firstOrNull().orEmpty()
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

    fun onCitySelected(city: String) {
        // The departure city doesn't filter the list yet; it will drive
        // distance/"nearest first" sorting in a later step.
        _uiState.update { it.copy(selectedCity = city) }
    }

    fun clearFilters() = updateFilters {
        it.copy(searchQuery = "", selectedCategory = DestinationFilter.ALL)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun updateFilters(transform: (ExploreUiState) -> ExploreUiState) {
        _uiState.update { transform(it).withFilteredDestinations() }
    }

    private fun ExploreUiState.withFilteredDestinations(): ExploreUiState =
        copy(
            destinations = DestinationFilter.apply(
                destinations = allDestinations,
                query = searchQuery,
                category = selectedCategory
            )
        )
}
