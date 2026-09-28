package com.example.smarttravelplanner.ui.screens.agentpackages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.PackageRepository
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.domain.model.agent.PackageDay
import com.example.smarttravelplanner.domain.util.PackageRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The agent's list of packages (drafts and published). */
@HiltViewModel
class AgentPackagesViewModel @Inject constructor(
    private val packageRepository: PackageRepository
) : ViewModel() {

    val packages: StateFlow<List<AgentPackage>?> = packageRepository.observeMyPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun togglePublished(pkg: AgentPackage) {
        viewModelScope.launch {
            packageRepository.setPublished(pkg.id, !pkg.isPublished)
                .onSuccess { _notice.value = if (pkg.isPublished) "Unpublished \u2014 travellers can't see it" else "Published \u2014 travellers can see it now" }
                .onFailure { _notice.value = it.message }
        }
    }

    fun delete(pkg: AgentPackage) {
        viewModelScope.launch {
            packageRepository.deletePackage(pkg.id)
                .onSuccess { _notice.value = "Deleted \u201C${pkg.title}\u201D" }
                .onFailure { _notice.value = it.message }
        }
    }

    fun noticeShown() {
        _notice.value = null
    }
}

/** Editor state: the package being built, plus which day is selected. */
data class PackageEditorUiState(
    val isLoading: Boolean = true,
    val draft: AgentPackage = AgentPackage(
        title = "", destination = "", pricePerPerson = 0.0,
        days = listOf(PackageDay(dayNumber = 1))
    ),
    val priceText: String = "",
    val selectedDay: Int = 1,
    val problems: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false
) {
    val isNew: Boolean get() = draft.id == 0L
    val day: PackageDay? get() = draft.days.firstOrNull { it.dayNumber == selectedDay }
}

/** Create or edit one package (packageId 0 = new). Nothing is saved until Save is tapped. */
@HiltViewModel
class PackageEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val packageRepository: PackageRepository
) : ViewModel() {

    private val packageId: Long = savedStateHandle["packageId"] ?: 0L

    private val _uiState = MutableStateFlow(PackageEditorUiState())
    val uiState: StateFlow<PackageEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = if (packageId != 0L) packageRepository.observePackage(packageId).first() else null
            _uiState.update { state ->
                if (existing == null) state.copy(isLoading = false)
                else state.copy(
                    isLoading = false,
                    draft = existing.copy(days = existing.days.ifEmpty { listOf(PackageDay(1)) }),
                    priceText = existing.pricePerPerson.toLong().takeIf { it > 0 }?.toString().orEmpty()
                )
            }
        }
    }

    // ── Details ──────────────────────────────────────────────────────────────
    fun setTitle(v: String) = edit { it.copy(title = v.take(80)) }
    fun setDestination(v: String) = edit { it.copy(destination = v.take(60)) }
    fun setDescription(v: String) = edit { it.copy(description = v.take(800)) }
    fun setIncludes(v: String) = edit { it.copy(includes = v.take(400)) }
    fun setExcludes(v: String) = edit { it.copy(excludes = v.take(400)) }
    fun setBestSeason(v: String) = edit { it.copy(bestSeason = v.take(40)) }
    fun setPrice(v: String) {
        val digits = v.filter(Char::isDigit).take(8)
        _uiState.update { it.copy(priceText = digits, problems = emptyList(), draft = it.draft.copy(pricePerPerson = digits.toDoubleOrNull() ?: 0.0)) }
    }
    fun addHighlight(v: String) = edit { pkg ->
        val h = v.trim().take(40)
        if (h.isEmpty() || pkg.highlights.any { it.equals(h, true) }) pkg else pkg.copy(highlights = pkg.highlights + h)
    }
    fun removeHighlight(v: String) = edit { it.copy(highlights = it.highlights - v) }

    // ── Days ─────────────────────────────────────────────────────────────────
    fun selectDay(n: Int) = _uiState.update { it.copy(selectedDay = n) }

    fun addDay() {
        val next = _uiState.value.draft.days.size + 1
        if (next > PackageRules.MAX_DAYS) return
        edit { it.copy(days = it.days + PackageDay(dayNumber = next)) }
        _uiState.update { it.copy(selectedDay = next) }
    }

    /** Removes the last day (days stay numbered 1..n). */
    fun removeLastDay() {
        val days = _uiState.value.draft.days
        if (days.size <= 1) return
        edit { it.copy(days = it.days.dropLast(1)) }
        _uiState.update { it.copy(selectedDay = it.selectedDay.coerceAtMost(days.size - 1)) }
    }

    fun setDayTitle(v: String) = editDay { it.copy(title = v.take(50)) }

    fun addActivity(a: Activity) = editDay { it.copy(activities = it.activities + a) }
    fun updateActivity(index: Int, a: Activity) = editDay { d ->
        d.copy(activities = d.activities.toMutableList().also { if (index in it.indices) it[index] = a })
    }
    fun deleteActivity(index: Int) = editDay { d ->
        d.copy(activities = d.activities.toMutableList().also { if (index in it.indices) it.removeAt(index) })
    }
    fun moveActivity(from: Int, to: Int) = editDay { d ->
        if (from !in d.activities.indices || to !in d.activities.indices) d
        else d.copy(activities = d.activities.toMutableList().also { list -> list.add(to, list.removeAt(from)) })
    }

    // ── Save ─────────────────────────────────────────────────────────────────
    /** @param publish true = save and make it visible to travellers */
    fun save(publish: Boolean) {
        val state = _uiState.value
        if (state.isSaving) return
        val toSave = state.draft.copy(isPublished = publish)
        val problems = PackageRules.validate(toSave, forPublishing = publish)
        if (problems.isNotEmpty()) {
            _uiState.update { it.copy(problems = problems) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            packageRepository.savePackage(toSave)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, problems = listOf(e.message ?: "Couldn't save.")) } }
        }
    }

    private fun edit(change: (AgentPackage) -> AgentPackage) =
        _uiState.update { it.copy(draft = change(it.draft), problems = emptyList()) }

    private fun editDay(change: (PackageDay) -> PackageDay) {
        val n = _uiState.value.selectedDay
        edit { pkg -> pkg.copy(days = pkg.days.map { if (it.dayNumber == n) change(it) else it }) }
    }
}
