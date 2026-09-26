package com.example.smarttravelplanner.data.local.dao

import androidx.room.*
import com.example.smarttravelplanner.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    /** Trips owned by one account, newest first. */
    @Query("SELECT * FROM trips WHERE ownerEmail = :ownerEmail ORDER BY createdAt DESC")
    fun getTripsForOwner(ownerEmail: String): Flow<List<TripEntity>>

    /**
     * Assigns trips created before ownership existed (ownerEmail = '')
     * to [ownerEmail]. Returns how many trips were claimed.
     */
    @Query("UPDATE trips SET ownerEmail = :ownerEmail WHERE ownerEmail = ''")
    suspend fun claimUnownedTrips(ownerEmail: String): Int

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)
}
