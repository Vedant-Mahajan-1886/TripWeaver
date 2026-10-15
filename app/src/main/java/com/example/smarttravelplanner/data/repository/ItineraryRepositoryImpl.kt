package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.local.dao.ItineraryDao
import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import com.example.smarttravelplanner.data.remote.cloud.NoTripSync
import com.example.smarttravelplanner.data.remote.cloud.TripSync
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.Itinerary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ItineraryRepositoryImpl @Inject constructor(
    private val itineraryDao: ItineraryDao,
    private val gson: Gson,
    /** The days travel inside the trip's cloud document, so a change re-uploads the trip. */
    private val tripSync: TripSync = NoTripSync
) : ItineraryRepository {

    override suspend fun saveItineraries(itineraries: List<Itinerary>) {
        itineraries.forEach { itinerary ->
            itineraryDao.insertItinerary(itinerary.toEntity())
        }
        itineraries.map { it.tripId }.distinct().forEach { tripSync.pushTrip(it) }
    }

    override fun getItinerariesForTripAsFlow(tripId: Long): Flow<List<Itinerary>> =
        itineraryDao.getItinerariesForTripAsFlow(tripId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getItinerariesForTrip(tripId: Long): List<Itinerary> =
        itineraryDao.getItinerariesForTrip(tripId).map { it.toDomain() }

    override suspend fun deleteItinerariesForTrip(tripId: Long) {
        itineraryDao.deleteItinerariesForTrip(tripId)
        tripSync.pushTrip(tripId)
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Itinerary.toEntity() = ItineraryEntity(
        id            = id,
        tripId        = tripId,
        dayNumber     = dayNumber,
        date          = date,
        activitiesJson = gson.toJson(activities)
    )

    private fun ItineraryEntity.toDomain(): Itinerary {
        val type = object : TypeToken<List<Activity>>() {}.type
        val activities: List<Activity> = try {
            gson.fromJson(activitiesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return Itinerary(
            id        = id,
            tripId    = tripId,
            dayNumber = dayNumber,
            date      = date,
            activities = activities
        )
    }
}
