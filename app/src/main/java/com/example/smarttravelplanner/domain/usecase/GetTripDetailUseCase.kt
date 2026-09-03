package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTripDetailUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository
) {
    suspend fun getTrip(tripId: Long): Trip? = tripRepository.getTripById(tripId)

    fun getItineraries(tripId: Long): Flow<List<Itinerary>> =
        itineraryRepository.getItinerariesForTripAsFlow(tripId)
}
