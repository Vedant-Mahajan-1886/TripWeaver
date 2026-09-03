package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.remote.dto.ActivityDto
import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import java.time.LocalDate
import javax.inject.Inject

/**
 * Orchestrates the full "Generate + Save" flow:
 *  1. Calls AiRepository.generateItinerary() → structured GeneratedItineraryDto
 *  2. Saves the Trip to Room → gets a real tripId
 *  3. Maps Dto activities to domain Activity objects, assigns tripId
 *  4. Saves all Itinerary days to Room
 *  5. Returns Result<Long> (the new tripId, used for navigation to TripDetail)
 *
 * In Track B, steps before step 1 will fetch real Google Places + weather data
 * and inject them as context into the AI prompt.
 */
class GenerateTripUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository
) {
    suspend operator fun invoke(
        destination: String,
        days: Int,
        budget: Double,
        interests: List<String>,
        travelStyle: String,
        startDate: LocalDate?,
        notes: String,
        model: String
    ): Result<Long> = runCatching {

        val startDateStr = startDate?.toString() ?: ""

        // Step 1 — Generate structured itinerary from AI
        val dto = aiRepository.generateItinerary(
            destination  = destination,
            days         = days,
            budget       = budget,
            interests    = interests,
            travelStyle  = travelStyle,
            startDate    = startDateStr,
            notes        = notes,
            model        = model
        ).getOrThrow()

        // Step 2 — Build Trip domain object and persist it
        val resolvedStart = startDate ?: LocalDate.now()
        val resolvedEnd   = resolvedStart.plusDays(days.toLong())

        val trip = Trip(
            title         = "Trip to $destination",
            destination   = destination,
            startDate     = resolvedStart,
            endDate       = resolvedEnd,
            budget        = budget,
            interests     = interests,
            notes         = notes
        )
        val tripId = tripRepository.saveTrip(trip)

        // Step 3 + 4 — Map DTO → domain Itinerary and save
        val itineraries = dto.days.map { dayDto ->
            Itinerary(
                tripId    = tripId,
                dayNumber = dayDto.dayNumber,
                date      = dayDto.date.ifBlank {
                    resolvedStart.plusDays((dayDto.dayNumber - 1).toLong()).toString()
                },
                activities = dayDto.activities.mapIndexed { idx, actDto ->
                    actDto.toDomain(itineraryId = 0L, id = idx.toLong())
                }
            )
        }
        itineraryRepository.saveItineraries(itineraries)

        tripId
    }

    private fun ActivityDto.toDomain(itineraryId: Long, id: Long) = Activity(
        id            = id,
        itineraryId   = itineraryId,
        title         = title,
        description   = description,
        location      = address.ifBlank { placeName },
        lat           = lat.takeIf { it != 0.0 },
        lng           = lng.takeIf { it != 0.0 },
        startTime     = startTime,
        endTime       = endTime,
        category      = runCatching { ActivityCategory.valueOf(category) }
                            .getOrDefault(ActivityCategory.ATTRACTION),
        estimatedCost = estimatedCostInr,
        placeId       = placeId,
        weatherNote   = weatherNote
    )
}
