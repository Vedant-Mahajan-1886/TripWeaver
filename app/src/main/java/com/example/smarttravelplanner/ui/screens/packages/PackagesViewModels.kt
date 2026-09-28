package com.example.smarttravelplanner.ui.screens.packages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.BookingRepository
import com.example.smarttravelplanner.data.repository.PackageRepository
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.AgentRating
import com.example.smarttravelplanner.domain.model.agent.PackageListing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Browse published packages, with a search box (pre-filled from Explore's destination). */
@HiltViewModel
class PackagesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val packageRepository: PackageRepository
) : ViewModel() {

    private val _query = MutableStateFlow(savedStateHandle.get<String>("query").orEmpty())
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val packages: StateFlow<List<PackageListing>?> = _query
        .flatMapLatest { packageRepository.observePublishedPackages(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(q: String) {
        _query.value = q.take(40)
    }
}

/** Used inside Explore's destination sheet: all published packages (filtered in the UI). */
@HiltViewModel
class PackagePreviewViewModel @Inject constructor(
    packageRepository: PackageRepository
) : ViewModel() {
    val packages: StateFlow<List<PackageListing>> = packageRepository.observePublishedPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class PackageDetailUiState(
    val isLoading: Boolean = true,
    val pkg: AgentPackage? = null,
    val agent: AgentProfile? = null,
    val rating: AgentRating? = null,
    val isChoosing: Boolean = false,
    val error: String? = null,
    /** Trip created from the package; the screen opens it. */
    val createdTripId: Long? = null
)

/** One package in full, and "Choose this package". */
@HiltViewModel
class PackageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val packageRepository: PackageRepository,
    agentRepository: AgentRepository,
    bookingRepository: BookingRepository
) : ViewModel() {

    private val packageId: Long = checkNotNull(savedStateHandle["packageId"])
    private val choosing = MutableStateFlow(Triple<Boolean, String?, Long?>(false, null, null))

    val uiState: StateFlow<PackageDetailUiState> = combine(
        packageRepository.observePackage(packageId),
        agentRepository.observeAgents(),
        bookingRepository.observeRatings(),
        choosing
    ) { pkg, agents, ratings, (busy, error, tripId) ->
        PackageDetailUiState(
            isLoading = false,
            pkg = pkg,
            agent = pkg?.let { p -> agents.firstOrNull { it.email == p.agentEmail } },
            rating = pkg?.let { ratings[it.agentEmail] },
            isChoosing = busy,
            error = error,
            createdTripId = tripId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PackageDetailUiState())

    fun choose(startDate: LocalDate, adults: Int, kids: Int) {
        if (choosing.value.first) return
        viewModelScope.launch {
            choosing.value = Triple(true, null, null)
            packageRepository.choosePackage(packageId, startDate, adults, kids)
                .onSuccess { tripId -> choosing.value = Triple(false, null, tripId) }
                .onFailure { e -> choosing.value = Triple(false, e.message ?: "Couldn't book this package.", null) }
        }
    }

    fun errorShown() {
        choosing.value = choosing.value.copy(second = null)
    }
}
