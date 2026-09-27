package com.example.smarttravelplanner.data.repository

import androidx.room.withTransaction
import com.example.smarttravelplanner.data.local.dao.TripEditDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.local.entity.TripEditEntity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TripEditRepositoryImpl @Inject constructor(
    private val database: TravelDatabase,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val tripEditDao: TripEditDao
) : TripEditRepository {

    override suspend fun saveEdits(
        trip: Trip,
        days: List<Itinerary>,
        removedDayIds: List<Long>,
        summaries: List<String>
    ): Result<Unit> = runCatching {
        require(days.all { it.tripId == trip.id }) { "These days belong to a different trip." }
        val now = System.currentTimeMillis()
        database.withTransaction {
            tripRepository.updateTrip(trip)
            if (removedDayIds.isNotEmpty()) tripEditDao.deleteDays(removedDayIds)
            itineraryRepository.saveItineraries(days)
            if (summaries.isNotEmpty()) {
                tripEditDao.insertEdits(
                    summaries.map { TripEditEntity(tripId = trip.id, summary = it, editedAt = now) }
                )
            }
        }
    }

    override fun observeEdits(tripId: Long): Flow<List<TripEdit>> =
        tripEditDao.observeEditsForTrip(tripId).map { rows -> rows.map { TripEdit(it.summary, it.editedAt) } }
}
