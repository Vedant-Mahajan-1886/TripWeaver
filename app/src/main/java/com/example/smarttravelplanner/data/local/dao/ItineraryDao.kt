package com.example.smarttravelplanner.data.local.dao

import androidx.room.*
import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItineraryDao {

    @Query("SELECT * FROM itineraries WHERE tripId = :tripId ORDER BY dayNumber ASC")
    fun getItinerariesForTrip(tripId: Long): Flow<List<ItineraryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItinerary(itinerary: ItineraryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItineraries(itineraries: List<ItineraryEntity>)

    @Delete
    suspend fun deleteItinerary(itinerary: ItineraryEntity)

    @Query("DELETE FROM itineraries WHERE tripId = :tripId")
    suspend fun deleteItinerariesForTrip(tripId: Long)
}
