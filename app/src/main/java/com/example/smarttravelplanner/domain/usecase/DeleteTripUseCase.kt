package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.data.repository.TripRepository
import javax.inject.Inject

class DeleteTripUseCase @Inject constructor(
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository
) {
    suspend operator fun invoke(tripId: Long) {
        itineraryRepository.deleteItinerariesForTrip(tripId)
        tripRepository.deleteTrip(tripId)
    }
}
