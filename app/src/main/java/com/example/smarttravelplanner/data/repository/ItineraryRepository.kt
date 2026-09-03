package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.Itinerary
import kotlinx.coroutines.flow.Flow

interface ItineraryRepository {
    /** Persist a list of itinerary days (and their activities) for a trip. */
    suspend fun saveItineraries(itineraries: List<Itinerary>)

    /** Reactive stream of itinerary days for a trip, ordered by day number. */
    fun getItinerariesForTripAsFlow(tripId: Long): Flow<List<Itinerary>>

    /** One-shot fetch of itinerary days (used for display/export). */
    suspend fun getItinerariesForTrip(tripId: Long): List<Itinerary>

    /** Delete all itinerary days for a trip (called before re-generating). */
    suspend fun deleteItinerariesForTrip(tripId: Long)
}
