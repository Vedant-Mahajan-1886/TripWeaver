package com.example.smarttravelplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.smarttravelplanner.data.local.entity.TripEditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripEditDao {

    @Insert
    suspend fun insertEdits(edits: List<TripEditEntity>)

    /** Manual edits of one trip, oldest first. */
    @Query("SELECT * FROM trip_edits WHERE tripId = :tripId ORDER BY editedAt ASC, id ASC")
    fun observeEditsForTrip(tripId: Long): Flow<List<TripEditEntity>>

    /** Manual edits on every trip that was shared with [agentEmail] (for "Trip changed" badges). */
    @Query(
        """
        SELECT * FROM trip_edits
        WHERE tripId IN (SELECT tripId FROM trip_shares WHERE agentEmail = :agentEmail)
        """
    )
    fun observeEditsOnAgentTrips(agentEmail: String): Flow<List<TripEditEntity>>

    /** Removes whole days (used when the trip's dates get shorter). */
    @Query("DELETE FROM itineraries WHERE id IN (:itineraryIds)")
    suspend fun deleteDays(itineraryIds: List<Long>)
}
