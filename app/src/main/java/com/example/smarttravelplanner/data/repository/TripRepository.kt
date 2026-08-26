package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun getAllTrips(): Flow<List<Trip>>
    suspend fun getTripById(id: Long): Trip?
    suspend fun saveTrip(trip: Trip): Long
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(id: Long)
}
