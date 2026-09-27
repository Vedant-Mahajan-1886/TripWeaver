package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.remote.dto.ActivityDto
import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripEditRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * Asks the AI to plan ONLY the days of a trip that have no activities yet
 * (e.g. days added when the traveller extended the dates).
 *
 * Why not regenerate the whole trip? The other days may have been edited by
 * hand or reviewed by an agent — replanning them would throw that work away,
 * take longer and use more AI quota. Instead the AI is told which places are
 * already planned so the new days don't repeat them.
 */
class PlanEmptyDaysUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val aiRepository: AiRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getWeatherForTripUseCase: GetWeatherForTripUseCase,
    private val tripEditRepository: TripEditRepository
) {

    /**
     * @param onlyDays limit to these day numbers (null = every empty day)
     * @return how many days were planned (0 if there was nothing to plan)
     */
    suspend operator fun invoke(tripId: Long, onlyDays: Set<Int>? = null): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trip = tripRepository.getTripById(tripId) ?: error("Trip not found.")
                val days = itineraryRepository.getItinerariesForTrip(tripId).sortedBy { it.dayNumber }
                val targets = days.filter { it.activities.isEmpty() && (onlyDays == null || it.dayNumber in onlyDays) }
                if (targets.isEmpty()) return@runCatching 0

                val firstDate = targets.first().date.toLocalDateOrNull() ?: trip.startDate
                val lastDate = targets.last().date.toLocalDateOrNull() ?: firstDate
                val weather = getWeatherForTripUseCase.getForDestination(trip.destination, firstDate, lastDate)
                    .getOrNull()?.takeIf { it.isNotEmpty() }

                val prefs = userPreferencesRepository.userPreferences.first()
                val model = if (prefs.aiProvider.equals("LOCAL", ignoreCase = true)) {
                    prefs.ollamaModel.ifBlank { "qwen3.5:4b" }
                } else {
                    prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
                }

                val dto = aiRepository.generateItinerary(
                    destination = trip.destination,
                    days = targets.size,
                    budget = budgetShare(trip, days.size, targets.size),
                    interests = trip.interests,
                    startDate = firstDate.toString(),
                    notes = buildNotes(trip, days, targets),
                    model = model,
                    weatherForecast = weather,
                    adults = trip.adults,
                    kids = trip.kids
                ).getOrThrow()

                val generated = dto.days.sortedBy { it.dayNumber }
                check(generated.isNotEmpty()) { "The AI didn't return any days. Please try again." }

                // Fill the empty days in order with the generated days
                val filled = targets.zip(generated).associate { (target, dayDto) ->
                    target.dayNumber to dayDto.activities.mapIndexed { i, a -> a.toActivity(target.id, i) }
                }
                val updatedDays = days.map { day ->
                    filled[day.dayNumber]?.let { day.copy(activities = it) } ?: day
                }

                val planned = filled.filterValues { it.isNotEmpty() }.keys.sorted()
                tripEditRepository.saveEdits(
                    trip = trip,
                    days = updatedDays,
                    summaries = listOf("AI planned " + planned.joinToString(", ") { "Day $it" })
                ).getOrThrow()
                planned.size
            }
        }

    /** The new days' share of the total budget (0 = let the AI decide). */
    private fun budgetShare(trip: Trip, totalDays: Int, newDays: Int): Double =
        if (trip.budget <= 0 || totalDays <= 0) 0.0 else trip.budget * newDays / totalDays

    /** The traveller's notes plus what's already planned, so nothing is repeated. */
    private fun buildNotes(trip: Trip, days: List<Itinerary>, targets: List<Itinerary>): String {
        val alreadyPlanned = days.filter { it.activities.isNotEmpty() }
            .flatMap { it.activities.map { a -> a.title } }
            .distinct()
            .take(40)
        return buildString {
            if (trip.notes.isNotBlank()) appendLine(trip.notes.trim()).appendLine()
            appendLine(
                "IMPORTANT: This trip already has a plan for its other days. Plan ONLY ${targets.size} " +
                    "extra day(s), for these dates: ${targets.joinToString(", ") { it.date }}."
            )
            if (alreadyPlanned.isNotEmpty()) {
                appendLine("Do NOT repeat any of these already-planned activities or places:")
                appendLine(alreadyPlanned.joinToString("; "))
                appendLine("Suggest fresh places nearby or a short day trip instead.")
            }
        }
    }

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

    // Same mapping as GenerateTripUseCase (kept here so that file stays untouched)
    private fun ActivityDto.toActivity(itineraryId: Long, index: Int) = Activity(
        id = index.toLong(),
        itineraryId = itineraryId,
        title = title,
        description = description,
        location = address.ifBlank { placeName },
        lat = lat.takeIf { it != 0.0 },
        lng = lng.takeIf { it != 0.0 },
        startTime = startTime,
        endTime = endTime,
        category = runCatching { ActivityCategory.valueOf(category) }.getOrDefault(ActivityCategory.ATTRACTION),
        estimatedCost = estimatedCostInr,
        placeId = placeId,
        weatherNote = weatherNote
    )
}
