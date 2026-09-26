package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.entity.TripEntity
import com.example.smarttravelplanner.domain.model.Trip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val authRepository: AuthRepository
) : TripRepository {

    /**
     * Trips of the logged-in user only. Switches automatically when a different
     * account logs in, and is empty when nobody is logged in.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllTrips(): Flow<List<Trip>> =
        authRepository.currentUserEmail
            .map { it.normalizedEmail() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    flow {
                        // Trips from before ownership existed go to the first user who logs in
                        tripDao.claimUnownedTrips(email)
                        emitAll(tripDao.getTripsForOwner(email))
                    }
                }
            }
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTripById(id: Long): Trip? =
        tripDao.getTripById(id)?.toDomain()

    override suspend fun saveTrip(trip: Trip): Long {
        val owner = trip.ownerEmail.ifBlank { currentEmail() }
        return tripDao.insertTrip(trip.copy(ownerEmail = owner).toEntity())
    }

    override suspend fun updateTrip(trip: Trip) {
        // Never lose the owner when a screen updates a trip it loaded earlier
        val owner = trip.ownerEmail.ifBlank {
            tripDao.getTripById(trip.id)?.ownerEmail.orEmpty().ifBlank { currentEmail() }
        }
        tripDao.updateTrip(trip.copy(ownerEmail = owner).toEntity())
    }

    override suspend fun deleteTrip(id: Long) =
        tripDao.deleteTripById(id)

    private suspend fun currentEmail(): String =
        authRepository.currentUserEmail.first().normalizedEmail()

    private fun String?.normalizedEmail(): String = this?.trim()?.lowercase().orEmpty()

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun TripEntity.toDomain() = Trip(
        id = id,
        title = title,
        destination = destination,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        budget = budget,
        adults = adults,
        kids = kids,
        interests = interests,
        notes = notes,
        coverImageUrl = coverImageUrl,
        createdAt = createdAt,
        ownerEmail = ownerEmail
    )

    private fun Trip.toEntity() = TripEntity(
        id = id,
        title = title,
        destination = destination,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        budget = budget,
        adults = adults,
        kids = kids,
        interests = interests,
        notes = notes,
        coverImageUrl = coverImageUrl,
        createdAt = createdAt,
        ownerEmail = ownerEmail
    )
}
